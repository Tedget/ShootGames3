package de.tedget.shootgames.command;

import de.tedget.shootgames.ShootGames;
import de.tedget.shootgames.util.WeaponUtil;
import de.tedget.shootgames.weapons.Grenade;
import de.tedget.shootgames.weapons.Gun;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class ShootGamesCommand
  implements CommandExecutor {
	ShootGames plugin;
  
  public ShootGamesCommand(ShootGames ShootGames) {
    this.plugin = ShootGames;
  }
  
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (cmd.getName().equalsIgnoreCase("sg"))
    {
      if (args.length == 0)
      {
        sender.sendMessage(ChatColor.RED + "Invalid usage!" + "\n" + ChatColor.GOLD + "/sg help");
        return true;
      }
      if (args[0].equalsIgnoreCase("reload")) {
        if (args.length != 1) {
          sender.sendMessage(ChatColor.RED + "Invalid usage!" + "\n" + ChatColor.GOLD + "/sg reload");
          return true;
        }
        if (!sender.hasPermission("shootgames.reload")) {
          sender.sendMessage(ChatColor.RED + "You don't have permission for this command!");
          return true;
        }
        this.plugin.reloadConfig();
        this.plugin.initializeStuff();
        if ((sender instanceof Player)) {
          sender.sendMessage("§6§o[Shootgames] Config has been reloaded.");
          return true;
        }
        sender.sendMessage("CONSOLE: §6Shootgames config reloaded.");
        for (Player p : Bukkit.getOnlinePlayers()) {
          if (p.hasPermission("McWeapons.reload")) {
            p.sendMessage("§7§o[CONSOLE: §6§oShootgames config has been reloaded.§7§o]");
          }
        }
        return true;
      }
      if (args[0].equalsIgnoreCase("list"))  {
        if (args.length != 1) {
          sender.sendMessage(ChatColor.RED + "Invalid usage!" + "\n" + ChatColor.GOLD + "/sg list");
          return true;
        }
        if (!sender.hasPermission("shootgames.list")) {
          sender.sendMessage(ChatColor.RED + "You don't have permission for this command!");
          return true;
        }
        sender.sendMessage(this.plugin.prefix + "§9List of weapons: \n §4• §c§lGuns:§r" + this.plugin.wu.getGunList() + "\n §4• §6§lGrenades:§r" + this.plugin.wu.getGrenadeList());
        return true;
      }
      if (args[0].equalsIgnoreCase("info")) {
        if (args.length != 2) {
          sender.sendMessage(ChatColor.RED + "Invalid usage!" + "\n" + ChatColor.GOLD + "/sg info <weapon>");
          return true;
        }
        if (!sender.hasPermission("shootgames.info")) {
          sender.sendMessage(ChatColor.RED + "You don't have permission for this command!");
          return true;
        }
        String weapon = this.plugin.wu.getWeaponByName(args[1]);
        if (weapon == null) {
          sender.sendMessage(this.plugin.prefix + "§cThat weapon doesn't exist!");
          return true;
        }
        sender.sendMessage(this.plugin.prefix + "§9Detailed information about §b" + weapon + "§9:" + this.plugin.wu.getWeaponInformations(weapon));
        return true;
      }
      if (args[0].equalsIgnoreCase("give")) {
        if (!(sender instanceof Player)) {
          sender.sendMessage(ChatColor.RED + "Command can't be run as console!");
          return true;
        }
        Player p = (Player)sender;
        if (args.length < 2) {
          p.sendMessage(ChatColor.RED + "Invalid usage!" + "\n" + ChatColor.GOLD + "/sg give <weapon>");
          return true;
        }
        if (!sender.hasPermission("shootgames.give")) {
          p.sendMessage(ChatColor.RED + "You don't have permission for this command!");
          return true;
        }
        String wstr = "";
        for (int i = 1; i <= args.length - 1; i++) {
          if (wstr.length() == 0) {
            wstr = wstr + args[i];
          } else {
            wstr = wstr + " " + args[i];
          }
        }
        String weapon = this.plugin.wu.getWeaponByName(wstr);
        if (weapon == null) {
          if (wstr.equalsIgnoreCase("Knife")) {
            if (!this.plugin.wu.hasEnoughSpace(p)) {
              p.sendMessage(this.plugin.prefix + "§cYou don't have enough space!");
              return true;
            }
            p.getInventory().addItem(new ItemStack[] { this.plugin.wu.rename(this.plugin.knifeIte, "§b§oKnife") });
            p.sendMessage(this.plugin.prefix + "§eHere's your knife!");
            return true;
          }
          p.sendMessage(this.plugin.prefix + "§cThat weapon doesn't exist!");
          return true;
        }
        if (!this.plugin.wu.hasEnoughSpace(p)) {
          p.sendMessage(this.plugin.prefix + "§cYou don't have enough space!");
          return true;
        }
        if (this.plugin.wu.isGun(weapon)) {
          Gun g = new Gun(weapon, p, this.plugin, null);
          p.getInventory().addItem(new ItemStack[] { g.getGunItem() });
          g.refreshItem(g.getGunItem());
        } else {
          Grenade gr = new Grenade(weapon, p, this.plugin);
          p.getInventory().addItem(new ItemStack[] { gr.getGrenadeItem() });
          gr.refreshItem();
        }
        p.sendMessage(this.plugin.prefix + "§eHere's your weapon supply!");
        return true;
      }
      if (args[0].equalsIgnoreCase("ammo")) {
        if (!(sender instanceof Player))
        {
          sender.sendMessage(ChatColor.RED + "Command can't be run as console!");
          return true;
        }
        Player p = (Player)sender;
        if (args.length < 2) {
          p.sendMessage(ChatColor.RED + "Invalid usage!" + "\n" + ChatColor.GOLD + "/sg ammo <weapon>");
          return true;
        }
        if (!sender.hasPermission("shootgames.ammo")) {
          p.sendMessage(ChatColor.RED + "You don't have permission for this command!");
          return true;
        }
        String wstr = "";
        for (int i = 1; i <= args.length - 1; i++) {
          if (wstr.length() == 0) {
            wstr = wstr + args[i];
          } else {
            wstr = wstr + " " + args[i];
          }
        }
        String weapon = this.plugin.wu.getWeaponByName(wstr);
        if (weapon == null) {
          p.sendMessage(this.plugin.prefix + "§cThat weapon doesn't exist!");
          return true;
        }
        if (!this.plugin.wu.isGun(weapon)) {
          p.sendMessage(this.plugin.prefix + "§cGrenades don't have ammo!");
          return true;
        }
        if (!this.plugin.wu.hasEnoughSpace(p)) {
          p.sendMessage(this.plugin.prefix + "§cYou don't have enough space!");
          return true;
        }
        Gun g = new Gun(weapon, p, this.plugin, null);
        ItemStack ammo = g.getAmmoItem();
        ammo.setAmount(64);
        p.getInventory().addItem(new ItemStack[] { ammo });
        p.sendMessage(this.plugin.prefix + "§eHere's your ammo supply!");
        return true;
      }
    }
    return false;
  }
}
