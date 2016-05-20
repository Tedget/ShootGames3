package de.tedget.shootgames.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import de.tedget.shootgames.ShootGames;

public class SpawnPoints implements CommandExecutor, Listener{

	public static HashMap<String, Integer> tasks = new HashMap<String, Integer>();
	
	ShootGames plugin;
	
	public SpawnPoints(ShootGames shootgames) {
	    this.plugin = shootgames;
	    shootgames.getServer().getPluginManager().registerEvents(this, shootgames);
	  }
	
	private YamlConfiguration config;
	private File configFile;
	
	public YamlConfiguration getConfig() {
	    return this.config;
	  }
	  
	  public void save() {
	    try
	    {
	      this.config.save(this.configFile);
	    }
	    catch (IOException e) {
	      e.printStackTrace();
	    }
	  }
	  
	  public void load() {
	    this.configFile = new File("plugins/ShootGames/spawnpoints.yml");
	    if (!this.configFile.exists()) {
	      try {
	        this.configFile.createNewFile();
	      }
	      catch (IOException e) {
	        e.printStackTrace();
	      }
	    }
	    this.config = new YamlConfiguration();
	    try {
	      this.config.load(this.configFile);
	    }
	    catch (IOException e) {
	      e.printStackTrace();
	    }
	    catch (InvalidConfigurationException e) {
	      e.printStackTrace();
	    }
	  }
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e){
		Player p = e.getPlayer();
		World w = Bukkit.getWorld("lobby");
		if (w == null){
			WorldCreator wc = new WorldCreator("lobby");
			wc.generateStructures(false);
			wc.type(WorldType.FLAT);
		}
		int players = Bukkit.getOnlinePlayers().length;
		//TODO
		/*if (ArenaHandler.getArena(p.getArena()).isRunning(){
		      p.kickPlayer("§cThe game is already running.");
		  }
		  else{*/
		if (players >= 10){
			p.kickPlayer("§cThe arena is full!");
		}
		else{
			p.teleport(w.getSpawnLocation());
			if (players >= 8){
				StartGame();
			}
		}
	}
	
	int zeit = 0;
	
	public void StartGame(){
		if (tasks.containsKey("1")){
			System.out.println("§cGame is already starting.");
		}
		else{
			tasks.put("1", Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
				public void run() {
					int players = Bukkit.getOnlinePlayers().length;
					if (players >= 8){
						if (zeit >= 30){
							Bukkit.broadcastMessage("§6Spiel startet!");
							for (Player p : Bukkit.getOnlinePlayers()){
								spawn(p);
							}
							Integer timerID = tasks.remove("1");
							if (timerID != null) {
								Bukkit.getScheduler().cancelTask(timerID);
							}
						}
						else{
							zeit++;
							int noch = 30-zeit;
							Bukkit.broadcastMessage("§7Noch §6"+noch+"§7 sekunden.");
						}
					}
					else{
						Bukkit.broadcastMessage("§cEs sind zu wenig Spieler online.");
						Integer timerID = tasks.remove("1");
						if (timerID != null) {
							Bukkit.getScheduler().cancelTask(timerID);
						}
					}
				}
			}, 20L, 20L).getTaskId());
		}
	}
	
	public void spawn(Player p){
		Random random = new Random();
		int i = config.getKeys(false).size();
		int point = random.nextInt(i)+1;
		Location l = (Location) config.get(""+point);
		if (l != null){
			p.teleport(l);
			//TODO p.giveWeapons();
		}
		else{
			p.sendMessage("§4Error!§c meh irgendwas stimmt mit der config nicht ._.");
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("addpoint")){
			if (sender instanceof Player){
				Player p = (Player) sender;
				Location l = p.getLocation();
				int i = config.getKeys(false).size();
				int add = i+1;
				config.set(""+add, l);
			}
			else{
				sender.sendMessage("§cYou have to be a player!");
			}
		}
		return true;
	}

}
