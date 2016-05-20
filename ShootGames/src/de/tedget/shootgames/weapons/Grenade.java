package de.tedget.shootgames.weapons;

import de.tedget.shootgames.ShootGames;
import de.tedget.shootgames.util.WeaponUtil;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class Grenade {
  private ItemStack grenIte;
  private List<String> lore = new ArrayList();
  private boolean smoke;
  private int damage;
  private List<String> effects;
  private int range;
  private long cooldown;
  private int expDelay;
  private boolean selfImmunity;
  private String name;
  private String index;
  private Player holder;
  private String hname;
  private ShootGames plugin;
  private Configuration config;
  private long cooldownMillis;
  private boolean sticky;
  
  public Grenade(String name, Player holder, ShootGames ShootGames) {
    this.name = name;
    this.index = name;
    this.holder = holder;
    this.hname = holder.getName();
    this.plugin = ShootGames;
    this.config = this.plugin.getGrenades();
    initialize();
  }
  
  private void initialize() {
    this.grenIte = getItem(this.config.getString(this.index + ".General.Item"));
    prepareGrenadeItem();
    this.selfImmunity = this.config.getBoolean(this.index + ".General.SelfImmunity");
    for (String l : this.config.getStringList(this.index + ".General.Lore")) {
      this.lore.add(l.replace("&", "§"));
    }
    this.cooldown = this.config.getLong(this.index + ".General.Cooldown");
    
    this.expDelay = this.config.getInt(this.index + ".Explosion.Delay");
    this.damage = this.config.getInt(this.index + ".Explosion.Damage");
    this.smoke = this.config.getBoolean(this.index + ".Explosion.Smoke");
    this.range = this.config.getInt(this.index + ".Explosion.Range");
    
    this.effects = this.config.getStringList(this.index + ".Ability.Effects");
    this.sticky = this.config.getBoolean(this.index + ".Ability.Sticky");
    
    this.cooldownMillis = getCooldownMillis();
  }
  
  private long getCooldownMillis() {
    if (this.holder.getMetadata("WeaponCooldown." + this.name).size() > 0) {
      return ((Long)((MetadataValue)this.holder.getMetadata("WeaponCooldown." + this.name).get(0)).value()).longValue();
    }
    return System.currentTimeMillis();
  }
  
  private boolean hasUnlimited() {
    return (this.holder.getGameMode() == GameMode.CREATIVE) && (this.plugin.creativeUnlimited);
  }
  
  public void throwGrenade() {
    if (this.cooldownMillis > System.currentTimeMillis()) {
      return;
    }
    this.holder.setMetadata("WeaponCooldown." + this.name, new FixedMetadataValue(this.plugin, Long.valueOf(System.currentTimeMillis() + this.cooldown)));
    Location loc = this.holder.getLocation();
    if (!hasUnlimited()) {
      removeGrenade();
    }
    this.holder.getWorld().playSound(this.holder.getLocation(), Sound.LAVA_POP, 1.0F, 5.0F);
    Item ite = loc.getWorld().dropItem(this.holder.getEyeLocation(), this.grenIte);
    ite.setVelocity(loc.getDirection().multiply(1.1D));
    if (this.sticky) {
      observeStickyGrenade(ite);
    }
    observeGrenade(ite, this.expDelay);
  }
  
  public void observeGrenade(final Item ite, final int remDelay) {
    if (ite.isDead()) {
      return;
    }
    this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
      public void run() {
        int newDelay = remDelay - 1;
        if (newDelay <= 0) {
          Grenade.this.explode(ite);
        } else {
          if (remDelay == 1) {
            ite.getWorld().playSound(ite.getLocation(), Sound.FUSE, 1.0F, 5.0F);
          }
          Grenade.this.observeGrenade(ite, newDelay);
        }
      }
    }, 20L);
  }
  
  private void removeGrenade() {
    ItemStack hand = this.holder.getItemInHand();
    int amount = hand.getAmount();
    amount--;
    if (amount == 0) {
      this.holder.setItemInHand(new ItemStack(0));
      return;
    }
    hand.setAmount(amount);
    this.holder.setItemInHand(hand);
  }
  
  public void observeStickyGrenade(final Item ite) {
    if (ite.isDead()) {
      return;
    }
    final String fHname = this.hname;
    this.plugin.getServer().getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
      public void run()  {
        if (!ite.isDead()) {
          boolean gotTarget = false;
          List<Entity> elist = ite.getNearbyEntities(0.2D, 0.2D, 0.2D);
          if (elist.size() >= 1) {
            Entity target = (Entity)elist.get(0);
            if ((target instanceof Player)) {
              if (!((Player)target).getName().equals(fHname)) {
                target.setPassenger(ite);
                gotTarget = true;
              }
            } else if (Grenade.this.plugin.wu.isValidEntity(target)) {
              target.setPassenger(ite);
              gotTarget = true;
            }
          }
          if (!gotTarget) {
            Grenade.this.observeStickyGrenade(ite);
          }
        }
      }
    }, 1L);
  }
  
  private void explode(Item ite) {
    Location loc = ite.getLocation();
    ite.remove();
    loc.getWorld().createExplosion(loc, 0.0F);
    List<Entity> elist = ite.getNearbyEntities(this.range, this.range, this.range);
    for (int t = 0; t < elist.size(); t++) {
      Entity n = (Entity)elist.get(t);
      boolean damage = true;
      if ((n instanceof Player)) {
        if ((this.plugin.noPvpDisabled) && (this.plugin.hasWorldGuard)) {
          RegionManager rm = this.plugin.getWorldGuard().getRegionManager(ite.getWorld());
          if (!rm.getApplicableRegions(n.getLocation()).allows(DefaultFlag.PVP)) {
            damage = false;
          }
        }
        if ((((Player)n).getName().equalsIgnoreCase(this.hname)) && (this.selfImmunity)) {
          damage = false;
        }
      }
      if ((this.plugin.wu.isValidEntity(n)) && (damage)) {
        ((LivingEntity)n).setMetadata("DamagerWeaponName", new FixedMetadataValue(this.plugin, this.name));
        if (this.damage > 0) {
          ((LivingEntity)n).damage(this.damage, this.holder);
        }
        if (this.effects.size() > 0) {
          addPotionEffects(n);
        }
      }
    }
    if (this.smoke) {
      loc.getWorld().createExplosion(loc.getX(), loc.getY(), loc.getZ(), 5.0F, false, this.plugin.blockDamage);
    }
  }
  
  private void addPotionEffects(Entity e) {
    for (String pstr : this.effects) {
      String[] split = pstr.split(",");
      String id = split[0];
      int duration = Integer.parseInt(split[1]);
      PotionEffectType type = PotionEffectType.getByName(id.toUpperCase());
      if (type != null) {
        ((LivingEntity)e).addPotionEffect(new PotionEffect(type, duration * 20, 5));
      }
    }
  }
  
  public void refreshItem() {
    String name = "§c§o" + this.name;
    for (Iterator localIterator = getGrenadeInstances().iterator(); localIterator.hasNext();) {
      int a = ((Integer)localIterator.next()).intValue();
      ItemStack i = this.holder.getInventory().getItem(a);
      this.holder.getInventory().setItem(a, this.plugin.wu.rename(this.plugin.wu.setLore(i, this.lore), name));
    }
    this.holder.updateInventory();
  }
  
  private List<Integer> getGrenadeInstances() {
    List<Integer> slots = new ArrayList();
    for (int i = 0; i <= 35; i++) {
      ItemStack is = this.holder.getInventory().getItem(i);
      if ((is != null) && 
        (is.getTypeId() == this.grenIte.getTypeId()) && (is.getData().getData() == this.grenIte.getData().getData())) {
        slots.add(Integer.valueOf(i));
      }
    }
    return slots;
  }
  
  private void prepareGrenadeItem() {
    this.grenIte.setAmount(64);
    ItemMeta im = this.grenIte.getItemMeta();
    im.setDisplayName(this.name);
    this.grenIte.setItemMeta(im);
  }
  
  private ItemStack getItem(String istr) {
    String[] split = istr.split(",");
    int id = Integer.parseInt(split[0]);
    byte data = 0;
    if (split.length == 2) {
      data = Byte.parseByte(split[1]);
    }
    return new ItemStack(id, 1, data);
  }
  
  public ItemStack getGrenadeItem() {
    return this.grenIte;
  }
  
  public boolean willSmoke() {
    return this.smoke;
  }
  
  public int getDamage() {
    return this.damage;
  }
  
  public List<String> getEffects() {
    return this.effects;
  }
  
  public int getRange() {
    return this.range;
  }
  
  public long getCooldown() {
    return this.cooldown;
  }
  
  public int getExplosionDelay(){
    return this.expDelay;
  }
  
  public String getName() {
    return this.name;
  }
  
  public Player getHolder() {
    return this.holder;
  }
  
  public String getHolderName() {
    return this.hname;
  }
  
  public boolean isSticky() {
    return this.sticky;
  }
  
  public boolean hasSelfImmunity() {
    return this.selfImmunity;
  }
}
