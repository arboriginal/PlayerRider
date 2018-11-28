package me.arboriginal.PlayerRider;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

// TODO: Find a way not to hide the duck POV with rider legs (players have new hitbox)

public class PlayerRider extends JavaPlugin implements Listener {
  protected FileConfiguration config;
  public static Sound         sound;
  public static Float         volume;

  // -----------------------------------------------------------------------------------------------
  // JavaPlugin methods
  // -----------------------------------------------------------------------------------------------

  @Override
  public void onEnable() {
    reloadConfig();

    getServer().getPluginManager().registerEvents(this, this);
  }

  @Override
  public void reloadConfig() {
    super.reloadConfig();

    saveDefaultConfig();
    config = getConfig();
    config.options().copyDefaults(true);
    saveConfig();

    sound  = null;
    volume = (float) config.getDouble("boost_whip_volume");

    if (volume > 0 && !config.getString("boost_whip_sound").isEmpty()) {
      try {
        sound = Sound.valueOf(config.getString("boost_whip_sound"));
      }
      catch (Exception e) {
        getLogger().warning(prepareMessage(config.getString("sndErr")));
      }
    }
  }

  @Override
  public void onDisable() {
    super.onDisable();

    HandlerList.unregisterAll((JavaPlugin) this);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (command.getName().equalsIgnoreCase("prider-reload")) {
      reloadConfig();
      userMessage(sender, "reload");

      return true;
    }

    return super.onCommand(sender, command, label, args);
  }

  // -----------------------------------------------------------------------------------------------
  // Listener methods
  // -----------------------------------------------------------------------------------------------

  @EventHandler
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    // https://www.spigotmc.org/threads/how-would-i-stop-an-event-from-being-called-twice.135234/#post-1434104
    if (event.getHand() == EquipmentSlot.OFF_HAND) return;

    Entity duck = event.getRightClicked();

    if (!(duck instanceof Player) || !duck.hasPermission("playerrider.duck")) return;

    Player player = event.getPlayer();

    if (player.getPassengers().contains(duck)) {
      if (player.getLocation().getPitch() < config.getDouble("eject_maxPitch")
          && player.hasPermission("playerrider.eject")) {
        player.eject();

        alert("eject", duck, player);
      }

      return;
    }

    if (!player.hasPermission("playerrider.ride") || player.isInsideVehicle()
        || !config.getStringList("allowed_items.ride")
            .contains(player.getInventory().getItemInMainHand().getType().toString()))
      return;

    List<Entity> riders = duck.getPassengers();

    while (riders.size() == 1 && riders.get(0) instanceof Player) {
      duck = riders.get(0);

      if (duck.equals(player)) {
        return;
      }

      riders = duck.getPassengers();
    }

    if (riders.size() == 0) {
      duck.addPassenger(player);

      alert("ride", player, duck);
    }
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    // https://www.spigotmc.org/threads/how-would-i-stop-an-event-from-being-called-twice.135234/#post-1434104
    if (event.getHand() == EquipmentSlot.OFF_HAND) return;

    if (!config.getBoolean("boost_allowed")) return;

    Player player = event.getPlayer();

    if (!player.hasPermission("playerrider.whip")
        || player.getLocation().getPitch() < config.getDouble("boost_maxPitch")
        || !(player.getVehicle() instanceof Player)
        || !config.getStringList("allowed_items.whip")
            .contains(player.getInventory().getItemInMainHand().getType().toString()))
      return;

    Player duck = (Player) player.getVehicle();

    duck.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, config.getInt("boost_duration"),
        config.getInt("boost_amplifier"), false, false, false), true);

    if (sound != null) {
      player.playSound(player.getLocation(), sound, volume, (float) config.getDouble("boost_whip_pitch"));
      duck.playSound(duck.getLocation(), sound, volume, (float) config.getDouble("boost_whip_pitch"));
    }

  }

  @EventHandler
  public void onEntityDamage(EntityDamageEvent event) {
    Entity injured = event.getEntity();

    if (!(injured instanceof Player)) return;

    if (config.getBoolean("prevent_suffocation")
        && event.getCause() == DamageCause.SUFFOCATION
        && event.getEntity().getVehicle() instanceof Player) {
      event.setCancelled(true);
      return;
    }

    if (config.getBoolean("getoff_when_hurt") && injured.getVehicle() instanceof Player) {
      injured.leaveVehicle();
      return;
    }

    if (config.getBoolean("eject_when_hurt") && injured.getPassengers().size() > 0) {
      injured.eject();
      return;
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Custom methods
  // -----------------------------------------------------------------------------------------------

  private void alert(String key, CommandSender player, CommandSender duck) {
    userMessage(player, "player." + key, player, duck);
    userMessage(duck, "duck." + key, player, duck);

    broadcast(key, player, duck);
  }

  private void userMessage(CommandSender sender, String key) {
    userMessage(sender, key, null, null);
  }

  private void userMessage(CommandSender sender, String key, CommandSender player, CommandSender duck) {
    String message = config.getString(key);

    if (!message.isEmpty()) {
      sender.sendMessage(prepareMessage(message, player, duck));
    }
  }

  private void broadcast(String key, CommandSender player, CommandSender duck) {
    String message = config.getString("broadcast." + key);

    if (!message.isEmpty()) {
      getServer().broadcastMessage(prepareMessage(message, player, duck));
    }
  }

  private String prepareMessage(String message) {
    return prepareMessage(message, null, null);
  }

  private String prepareMessage(String message, CommandSender player, CommandSender duck) {
    message = message.replace("{prefix}", config.getString("prefix"));

    if (player != null) message = message.replace("{player}", player.getName());
    if (duck != null) message = message.replace("{duck}", duck.getName());

    return ChatColor.translateAlternateColorCodes('&', message);
  }
}
