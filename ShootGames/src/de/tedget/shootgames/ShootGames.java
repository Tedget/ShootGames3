package de.tedget.shootgames;

import de.tedget.shootgames.command.ShootGamesCommand;
import de.tedget.shootgames.config.GrenadesLoader;
import de.tedget.shootgames.config.GunsLoader;
import de.tedget.shootgames.config.SpawnPoints;
import de.tedget.shootgames.listener.WeaponListener;
import de.tedget.shootgames.util.WeaponUtil;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.configuration.Configuration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class ShootGames extends JavaPlugin
{
  Logger log = Logger.getLogger("Minecraft");
  public WeaponUtil wu;
  public WeaponListener wl;
  public ShootGamesCommand mwCE = new ShootGamesCommand(this);
  public SpawnPoints sppt = new SpawnPoints(this);
  public GunsLoader gl;
  public GrenadesLoader grl;
  public boolean messagesEnabled;
  public boolean headshotMessage;
  public boolean fullmagStart;
  public boolean worldLimit;
  public boolean customDeath;
  public boolean noPvpDisabled;
  public boolean disabledMessage;
  public boolean hasWorldGuard;
  public boolean blockDamage;
  public boolean autoReload;
  public boolean creativeUnlimited;
  public boolean headshotEffect;
  public String noMagazine;
  public String weaponReloaded;
  public String headshotShooter;
  public String headshotVictim;
  public String death;
  public String disabled;
  public String prefix = "§8§l[§7§lShoot§b§lGames§8§l] §r";
  public List<String> worlds;
  public boolean knifeEnabled;
  public int knifeDamage;
  public int knifeBackstabDamage;
  public ItemStack knifeIte;

  public void onEnable() {
    if (!manageConfigs()) {
      return;
    }
    initializeStuff();

    registerStuff();

    this.log.log(Level.INFO, "[ShootGames] Plugin Version " + getDescription().getVersion() + " activated!");
  }

  public void onDisable() {
    this.log.log(Level.INFO, "[ShootGames] Plugin Deaktiviert");
  }

  public void loadConfig() {
    if (new File("plugins/ShootGames/config.yml").exists()) {
      this.log.log(Level.INFO, "[ShootGames] config.yml ist geloadet");
    } else {
      saveDefaultConfig();
      this.log.log(Level.INFO, "[ShootGames] Neu config.yml ist created.");
    }
  }

  public boolean manageConfigs() {
    loadConfig();
    try {
      this.gl = new GunsLoader(this);
    } catch (Exception e) {
      this.log.log(Level.WARNING, "[ShootGames] Error occurred while loading guns.yml! Plugin will disable!");
      e.printStackTrace();
      getServer().getPluginManager().disablePlugin(this);
      return false;
    }
    this.log.log(Level.INFO, "[ShootGames] guns.yml successfully loaded.");
    try {
      this.grl = new GrenadesLoader(this);
    } catch (Exception e) {
      this.log.log(Level.WARNING, "[ShootGames] Error occurred while loading grenades.yml! Plugin will disable!");
      getServer().getPluginManager().disablePlugin(this);
      return false;
    }
    this.log.log(Level.INFO, "[ShootGames] grenades.yml successfully loaded.");
    return true;
  }

  public void initializeStuff() {
	
    Configuration config = getConfig();

    this.messagesEnabled = config.getBoolean("MessageOptions.Enabled");
    this.headshotMessage = config.getBoolean("MessageOptions.HeadshotMessage");
    this.customDeath = config.getBoolean("MessageOptions.CustomDeathMessage");
    this.disabledMessage = config.getBoolean("MessageOptions.DisabledMessage");
    this.noMagazine = config.getString("Messages.NoMagazine").replace("&", "§");
    this.weaponReloaded = config.getString("Messages.WeaponReloaded").replace("&", "§");
    this.headshotShooter = config.getString("Messages.HeadshotShooter").replace("&", "§");
    this.headshotVictim = config.getString("Messages.HeadshotVictim").replace("&", "§");
    this.death = config.getString("Messages.Death").replace("&", "§");
    this.disabled = config.getString("Messages.Disabled").replace("&", "§");

    this.fullmagStart = config.getBoolean("General.FullMagazineStart");
    this.worldLimit = config.getBoolean("General.WorldLimit.Enabled");
    this.noPvpDisabled = config.getBoolean("General.DisableInNonPvpAreas");
    this.blockDamage = config.getBoolean("General.BlockDamage");
    this.autoReload = config.getBoolean("General.AutoReload");
    this.creativeUnlimited = config.getBoolean("General.CreativeUnlimitedAmmo");
    this.headshotEffect = config.getBoolean("General.HeadshotEffect");
    this.worlds = config.getStringList("General.WorldLimit.Worlds");
    this.hasWorldGuard = worldGuardInstalled();
    if (this.hasWorldGuard) {
      this.log.log(Level.INFO, "[ShootGames] WorldGuard and WorldEdit have been detected!");
    }

    this.knifeEnabled = config.getBoolean("Knife.Enabled");
    this.knifeDamage = config.getInt("Knife.Damage");
    this.knifeBackstabDamage = config.getInt("Knife.BackstabDamage");
    this.knifeIte = new WeaponUtil(this).getItem(config.getString("Knife.Item"));
  }

  public boolean worldGuardInstalled() {
    return (getWorldGuard() != null) && (getWorldEdit() != null);
  }

  public WorldGuardPlugin getWorldGuard() {
    Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
    if ((plugin == null) || (!(plugin instanceof WorldGuardPlugin))) {
      return null;
    }
    return (WorldGuardPlugin)plugin;
  }

  public WorldEditPlugin getWorldEdit() {
    Plugin plugin = getServer().getPluginManager().getPlugin("WorldEdit");
    if ((plugin == null) || (!(plugin instanceof WorldEditPlugin))) {
      return null;
    }
    return (WorldEditPlugin)plugin;
  }

  public Configuration getGuns() {
    this.gl.load();
    return this.gl.getConfig();
  }

  public Configuration getGrenades() {
    this.grl.load();
    return this.grl.getConfig();
  }

  public void registerStuff() {
    this.wu = new WeaponUtil(this);
    this.wl = new WeaponListener(this);
    this.sppt = new SpawnPoints(this);
    getCommand("sg").setExecutor(this.mwCE);
    getCommand("addpoint").setExecutor(this.sppt);
    sppt.load();
  }
}
