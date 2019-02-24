package me.arboriginal.PlayerRider;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

// TODO: Find a way not to hide the duck POV with rider legs (players have new hitbox)

public class PlayerRider extends JavaPlugin implements Listener {
  public static FileConfiguration config;
  public static Sound             sound;
  public static Float             volume;
  public static PRcooldown        cooldown;

  // -----------------------------------------------------------------------------------------------
  // JavaPlugin methods
  // -----------------------------------------------------------------------------------------------

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (command.getName().equalsIgnoreCase("prider-reload")) {
      reloadConfig();
      userMessage(sender, "reload");

      return true;
    }

    return super.onCommand(sender, command, label, args);
  }

  @Override
  public void onDisable() {
    super.onDisable();

    HandlerList.unregisterAll((JavaPlugin) this);
  }

  @Override
  public void onEnable() {
    cooldown = new PRcooldown();
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

  // -----------------------------------------------------------------------------------------------
  // Listener methods
  // -----------------------------------------------------------------------------------------------

  private boolean duckAllowed(Player duck, int passengersCount) {
    ConfigurationSection section = config.getConfigurationSection("max_riders");

    for (String group : section.getKeys(false)) {
      if ((group.equals("default") || duck.hasPermission("playerrider.duck." + group))
          && config.getInt("max_riders." + group) >= passengersCount)
        return true;
    }

    return false;
  }

  @EventHandler
  public void onEntityDamage(EntityDamageEvent event) {
    if (event.isCancelled()) return;

    Entity injured = event.getEntity();

    if (!isPlayer(injured)) return;

    if (config.getBoolean("prevent_suffocation")
        && event.getCause() == DamageCause.SUFFOCATION
        && isPlayer(injured.getVehicle())) {
      event.setCancelled(true);
      return;
    }

    if (config.getBoolean("getoff_when_hurt") && isPlayer(injured.getVehicle())) {
      injured.leaveVehicle();
      return;
    }

    if (config.getBoolean("eject_when_hurt") && injured.getPassengers().size() > 0) {
      injured.eject();
      return;
    }
  }

  @EventHandler
  public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
    Entity injured = event.getEntity();

    if (!isPlayer(injured)) return;

    if (config.getBoolean("prevent_hit_rider") && isPlayer(event.getDamager())) {
      Entity vehicle = injured.getVehicle();

      if (vehicle != null && event.getDamager().equals(vehicle)) {
        event.setCancelled(true);
        return;
      }
    }
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    // https://www.spigotmc.org/threads/how-would-i-stop-an-event-from-being-called-twice.135234/#post-1434104
    if (event.getHand() == EquipmentSlot.OFF_HAND) return;

    if (!config.getBoolean("boost_allowed")) return;

    Player player = event.getPlayer();

    if (!playerAllowed(player, "whip")
        || player.getLocation().getPitch() < config.getDouble("boost_maxPitch")
        || !isPlayer(player.getVehicle()))
      return;

    ItemStack item = player.getInventory().getItemInMainHand();

    if (!config.getStringList("allowed_items.whip").contains(item.getType().toString())) return;

    Player duck = (Player) player.getVehicle();

    if (cooldown.isActive("whip.perform", player, duck, true)) return;

    int amplifier = config.getInt("boost_amplifier"), duration = config.getInt("boost_duration");

    if (amplifier > 0 && duration > 0) {
      if (sound != null) {
        player.playSound(player.getLocation(), sound, volume, (float) config.getDouble("boost_whip_pitch"));
        duck.playSound(duck.getLocation(), sound, volume, (float) config.getDouble("boost_whip_pitch"));
      }

      duck.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier, false, false, false), true);
    }

    consume(player, item, "whip");
    alert("whip", player, duck);
    cooldown.set("whip.perform", player, duck);
  }

  @EventHandler
  public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    // https://www.spigotmc.org/threads/how-would-i-stop-an-event-from-being-called-twice.135234/#post-1434104
    if (event.getHand() == EquipmentSlot.OFF_HAND || !isPlayer(event.getRightClicked())) return;

    Player player = event.getPlayer(), duck = (Player) event.getRightClicked();

    if (player.getPassengers().contains(duck)) {
      if (player.getLocation().getPitch() < config.getDouble("eject_maxPitch")
          && player.hasPermission("playerrider.eject")
          && !cooldown.isActive("eject.perform", player, true)) {
        player.eject();
        alert("eject", duck, player);
        cooldown.clear("eject.perform", player);
      }

      return;
    }

    if (!playerAllowed(player, "ride") || !duck.hasPermission("playerrider.duck") || player.isInsideVehicle()) return;

    ItemStack item = player.getInventory().getItemInMainHand();

    if (!config.getStringList("allowed_items.ride").contains(item.getType().toString())) return;

    List<Entity> riders = duck.getPassengers();
    int          count  = 0;

    while (riders.size() == 1 && isPlayer(riders.get(0))) {
      duck = (Player) riders.get(0);
      count++;

      if (duck.equals(player)) {
        return;
      }

      riders = duck.getPassengers();
    }

    if (riders.size() > 0) return;

    if (!duckAllowed(duck, count + 1)) {
      userMessage(player, "tooMany", player, duck);
      return;
    }

    if (duck.addPassenger(player)) {
      consume(player, item, "ride");
      alert("ride", player, duck);
      cooldown.set("ride.perform", player, duck);
      cooldown.set("eject.perform", duck);
    }
    else {
      userMessage(player, "failed", player, duck);
    }
  }

  // -----------------------------------------------------------------------------------------------
  // Custom methods
  // -----------------------------------------------------------------------------------------------

  private void alert(String key, Player player, Player duck) {
    if (!cooldown.isActive(key + ".alertPlayer", player, duck)) {
      userMessage(player, "player." + key, player, duck);
      cooldown.set(key + ".alertPlayer", player, duck);
    }

    if (!cooldown.isActive(key + ".alertDuck", player, duck)) {
      userMessage(duck, "duck." + key, player, duck);
      cooldown.set(key + ".alertDuck", player, duck);
    }

    if (!cooldown.isActive(key + ".broadcast", player, duck)) {
      broadcast(key, player, duck);
      cooldown.set(key + ".broadcast", player, duck);
    }
  }

  private void broadcast(String key, CommandSender player, CommandSender duck) {
    String message = config.getString("broadcast." + key);

    if (!message.isEmpty()) {
      getServer().broadcastMessage(prepareMessage(message, player, duck));
    }
  }

  private void consume(Player player, ItemStack item, String key) {
    if (config.getBoolean("consume_items." + key)
        && !player.hasPermission("playerrider." + key + ".keepitem")
        && item.getType() != Material.AIR)
      item.setAmount(item.getAmount() - 1);
  }

  private boolean isPlayer(Entity entity) {
    return (entity instanceof Player) && !entity.hasMetadata("NPC");
  }

  private boolean playerAllowed(Player player, String key) {
    return player.hasPermission("playerrider." + key) || player.hasPermission("playerrider." + key + ".keepitem");
  }

  private static String prepareMessage(String message) {
    return prepareMessage(message, null, null);
  }

  private static String prepareMessage(String message, CommandSender player, CommandSender duck) {
    message = message.replace("{prefix}", config.getString("prefix"));

    if (player != null) message = message.replace("{player}", player.getName());
    if (duck != null) message = message.replace("{duck}", duck.getName());

    return ChatColor.translateAlternateColorCodes('&', message);
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

  protected static void warn(String key, Player player, Player duck, int delay) {
    String[] parts = key.split("\\.");

    if (parts.length != 2 || !parts[1].equals("perform")) return;

    String message = PlayerRider.config.getString("warn_player_when_cooldown." + parts[0]);

    if (message.isEmpty()) return;

    ((Player) player).spigot().sendMessage(ChatMessageType.ACTION_BAR,
        new TextComponent(prepareMessage(message.replace("{delay}", "" + delay), player, duck)));
  }
}
