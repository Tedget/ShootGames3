package de.tedget.shootgames.listener;

import de.tedget.shootgames.ShootGames;
import de.tedget.shootgames.util.WeaponUtil;
import de.tedget.shootgames.weapons.Grenade;
import de.tedget.shootgames.weapons.Gun;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import java.util.List;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffectType;

public class WeaponListener
  implements Listener {
  ShootGames plugin;
  
  public WeaponListener(ShootGames shootgames) {
    this.plugin = shootgames;
    shootgames.getServer().getPluginManager().registerEvents(this, shootgames);
  }
  
  @EventHandler(priority=EventPriority.HIGH)
  public void onPlayerInteract(PlayerInteractEvent event) {
    Action action = event.getAction();
    if ((action != Action.RIGHT_CLICK_AIR) && (action != Action.RIGHT_CLICK_BLOCK) && (action != Action.LEFT_CLICK_AIR) && (action != Action.LEFT_CLICK_BLOCK)) {
      return;
    }
    Player p = event.getPlayer();
    if ((this.plugin.worldLimit) && 
      (!this.plugin.worlds.contains(p.getWorld().getName()))) {
      return;
    }
    String weapon = this.plugin.wu.getWeaponName(p.getItemInHand());
    if (weapon == null) {
      return;
    }
    if ((!p.hasPermission("shootgames.use." + weapon)) && (!p.hasPermission("shootgames.use.all"))) {
      return;
    }
    if (this.plugin.noPvpDisabled) {
      if (!p.getWorld().getPVP())
      {
        if (this.plugin.disabledMessage) {
          p.sendMessage(this.plugin.disabled);
        }
        return;
      }
      if (this.plugin.hasWorldGuard) {
        RegionManager rm = this.plugin.getWorldGuard().getRegionManager(p.getWorld());
        if (!rm.getApplicableRegions(p.getLocation()).allows(DefaultFlag.PVP))
        {
          if (this.plugin.disabledMessage) {
            p.sendMessage(this.plugin.disabled);
          }
          return;
        }
      }
    }
    boolean aiming = false;
    if ((action == Action.LEFT_CLICK_AIR) || (action == Action.LEFT_CLICK_BLOCK)) {
      aiming = true;
    }
    event.setCancelled(true);
    boolean gun = this.plugin.wu.isGun(weapon);
    if (aiming) {
      if (!gun) {
        return;
      }
      Gun g = new Gun(weapon, p, this.plugin, null);
      g.scope();
      return;
    }
    if (gun) {
      Gun g = new Gun(weapon, p, this.plugin, p.getItemInHand());
      if (p.isSneaking())
      {
        g.startReloading();
        return;
      }
      g.shoot();
      return;
    }
    Grenade gr = new Grenade(weapon, p, this.plugin);
    gr.throwGrenade();
  }
  
  @EventHandler(priority=EventPriority.NORMAL)
  public void onProjectileHit(ProjectileHitEvent event) {
    Projectile pr = event.getEntity();
    Entity shooter = (Entity) pr.getShooter();
    if (!(shooter instanceof Player)) {
      return;
    }
    Player p = (Player)shooter;
    if (pr.getMetadata("WeaponName").size() == 0) {
      return;
    }
    String weapon = (String)((MetadataValue)pr.getMetadata("WeaponName").get(0)).value();
    Gun g = new Gun(weapon, p, this.plugin, null);
    if (pr.getType() != EntityType.fromName(g.getBullet())) {
      return;
    }
    if (g.willExplode()) {
      Location loc = pr.getLocation();
      loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 5.0F, false, this.plugin.blockDamage);
      List<Entity> elist = pr.getNearbyEntities(g.getExplosionRange(), g.getExplosionRange(), g.getExplosionRange());
      for (int t = 0; t < elist.size(); t++)
      {
        Entity n = (Entity)elist.get(t);
        boolean damage = true;
        if ((n instanceof Player)) {
          if ((this.plugin.noPvpDisabled) && (this.plugin.hasWorldGuard)) {
            RegionManager rm = this.plugin.getWorldGuard().getRegionManager(p.getWorld());
            if (!rm.getApplicableRegions(n.getLocation()).allows(DefaultFlag.PVP)) {
              damage = false;
            }
          }
          if ((((Player)n).getName().equalsIgnoreCase(p.getName())) && (g.hasSelfImmunity())) {
            damage = false;
          }
        }
        if ((this.plugin.wu.isValidEntity(n)) && (damage)) {
          ((LivingEntity)n).setMetadata("DamagerWeaponName", new FixedMetadataValue(this.plugin, g.getName()));
          ((LivingEntity)n).damage(g.getExplosionDamage(), p);
        }
      }
    }
    g.playHitEffect(pr.getLocation());
    if (pr.getType() == EntityType.ARROW) {
      pr.remove();
    }
  }
  
  @EventHandler(priority=EventPriority.NORMAL)
  public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if (!this.plugin.wu.isValidEntity(event.getEntity())) {
      return;
    }
    LivingEntity e = (LivingEntity)event.getEntity();
    if (event.getCause() != EntityDamageEvent.DamageCause.PROJECTILE) {
      if ((this.plugin.knifeEnabled) && (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) && ((event.getDamager() instanceof Player)))
      {
        Player d = (Player)event.getDamager();
        ItemStack h = d.getItemInHand();
        if ((h.getTypeId() == this.plugin.knifeIte.getTypeId()) && (h.getData().getData() == this.plugin.knifeIte.getData().getData()))
        {
          int damage = this.plugin.knifeDamage;
          if (this.plugin.wu.isBackstab(d, e)) {
            damage = this.plugin.knifeBackstabDamage;
          }
          e.setMetadata("DamagerWeaponName", new FixedMetadataValue(this.plugin, "Knife"));
          event.setDamage(damage);
          return;
        }
      }
      return;
    }
    Projectile pr = (Projectile)event.getDamager();
    Entity shooter = pr.getShooter();
    if (!(shooter instanceof Player)) {
      return;
    }
    Player p = (Player)shooter;
    if (pr.getMetadata("WeaponName").size() == 0) {
      return;
    }
    String weapon = (String)((MetadataValue)pr.getMetadata("WeaponName").get(0)).value();
    Gun g = new Gun(weapon, p, this.plugin, null);
    if ((e instanceof Player))
    {
      if ((this.plugin.noPvpDisabled) && (this.plugin.hasWorldGuard))
      {
        RegionManager rm = this.plugin.getWorldGuard().getRegionManager(p.getWorld());
        if (!rm.getApplicableRegions(e.getLocation()).allows(DefaultFlag.PVP))
        {
          e.setMetadata("Headshot", new FixedMetadataValue(this.plugin, Boolean.valueOf(false)));
          return;
        }
      }
      if ((((Player)e).getName().equalsIgnoreCase(p.getName())) && (g.hasSelfImmunity())) {
        return;
      }
    }
    int damage = g.getDamage();
    if (this.plugin.wu.isHeadshot(pr, e)) {
      if ((e instanceof Player)) {
        if (this.plugin.headshotMessage) {
          Player ep = (Player)e;
          p.sendMessage(this.plugin.headshotShooter.replace("%player%", ep.getName()));
          ep.sendMessage(this.plugin.headshotVictim.replace("%player%", p.getName()));
        }
        e.setMetadata("Headshot", new FixedMetadataValue(this.plugin, Boolean.valueOf(true)));
      }
      if (this.plugin.headshotEffect) {
        e.getWorld().playEffect(e.getEyeLocation(), Effect.STEP_SOUND, 55);
      }
      damage += g.getHeadshotBonus();
    }
    else if ((e instanceof Player)) {
      e.setMetadata("Headshot", new FixedMetadataValue(this.plugin, Boolean.valueOf(false))); }
    event.setDamage(damage);
  }
  
  @EventHandler(priority=EventPriority.NORMAL)
  public void onPlayerPickupItem(PlayerPickupItemEvent event) {
    ItemStack i = event.getItem().getItemStack();
    if (!i.hasItemMeta()) {
      return;
    }
    ItemMeta im = i.getItemMeta();
    if (!im.hasDisplayName()) {
      return;
    }
    String name = im.getDisplayName();
    if (!this.plugin.wu.isGrenade(name)) {
      return;
    }
    event.setCancelled(true);
  }
  
  @EventHandler(priority=EventPriority.NORMAL)
  public void onEntityDamage(EntityDamageEvent event) {
    if (event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
      return;
    }
    Entity e = event.getEntity();
    if (e.getMetadata("DamagerWeaponName").size() == 0) {
      return;
    }
    String weapon = (String)((MetadataValue)e.getMetadata("DamagerWeaponName").get(0)).value();
    if (weapon.equalsIgnoreCase("None")) {
      return;
    }
    event.setCancelled(true);
    e.setMetadata("DamagerWeaponName", new FixedMetadataValue(this.plugin, "None"));
  }
  
  @EventHandler(priority=EventPriority.NORMAL)
  public void onPlayerItemHeld(PlayerItemHeldEvent event) {
    Player p = event.getPlayer();
    if ((p.getMetadata("Aiming").size() > 0) && (((Boolean)((MetadataValue)p.getMetadata("Aiming").get(0)).value()).booleanValue())) {
      p.removePotionEffect(PotionEffectType.SPEED);
      p.setMetadata("Aiming", new FixedMetadataValue(this.plugin, Boolean.valueOf(false)));
    }
    int slot = event.getNewSlot();
    ItemStack i = p.getInventory().getItem(slot);
    if (i == null) {
      return;
    }
    String weapon = this.plugin.wu.getWeaponName(i);
    if (weapon == null) {
      return;
    }
    if ((!p.hasPermission("shootgames.use." + weapon)) && (!p.hasPermission("shootgames.use.all"))) {
      return;
    }
    boolean gun = this.plugin.wu.isGun(weapon);
    p.getWorld().playSound(p.getLocation(), Sound.BAT_TAKEOFF, 0.5F, 5.0F);
    if (gun) {
      Gun g = new Gun(weapon, p, this.plugin, i);
      g.refreshItem(i);
      return;
    }
    if (weapon.equalsIgnoreCase("Knife")) {
      p.getInventory().setItem(slot, this.plugin.wu.rename(i, "§b§oKnife"));
    }
    else {
      Grenade gr = new Grenade(weapon, p, this.plugin);
      gr.refreshItem();
      return;
    }
  }
  
  @EventHandler(priority=EventPriority.HIGH)
  public void onPlayerDeath(PlayerDeathEvent event) {
    if (!this.plugin.customDeath) {
      return;
    }
    Player p = event.getEntity();
    if (!(p.getLastDamageCause() instanceof EntityDamageByEntityEvent)) {
      return;
    }
    EntityDamageByEntityEvent e = (EntityDamageByEntityEvent)p.getLastDamageCause();
    if (e.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
      Projectile pr = (Projectile)e.getDamager();
      if (!(pr.getShooter() instanceof Player)) {
        return;
      }
      Player k = (Player)pr.getShooter();
      if (pr.getMetadata("WeaponName").size() == 0) {
        return;
      }
      String weapon = (String)((MetadataValue)pr.getMetadata("WeaponName").get(0)).value();
      String addition = "";
      if (p.getMetadata("Headshot").size() > 0) {
        boolean headshot = ((Boolean)((MetadataValue)p.getMetadata("Headshot").get(0)).value()).booleanValue();
        if (headshot) {
          addition = "§4✛";
        }
      }
      event.setDeathMessage(this.plugin.death.replace("%killer%", k.getName()).replace("%player%", p.getName() + addition).replace("%weapon%", weapon));
      pr.removeMetadata("WeaponName", this.plugin);
    } else {
      if (!(e.getDamager() instanceof Player)) {
        return;
      }
      Player k = (Player)e.getDamager();
      if (p.getMetadata("DamagerWeaponName").size() == 0) {
        return;
      }
      String weapon = (String)((MetadataValue)p.getMetadata("DamagerWeaponName").get(0)).value();
      event.setDeathMessage(this.plugin.death.replace("%killer%", k.getName()).replace("%player%", p.getName()).replace("%weapon%", weapon));
      k.removeMetadata("WeaponName", this.plugin);
    }
  }
  
  @EventHandler(priority=EventPriority.NORMAL)
  public void onPlayerEggThrow(PlayerEggThrowEvent event) {
    Projectile pr = event.getEgg();
    if (pr.getMetadata("WeaponName").size() == 0) {
      return;
    }
    event.setHatching(false);
  }
}

