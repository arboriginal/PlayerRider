package com.github.arboriginal.PlayerRider;

import java.io.IOException;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import com.github.arboriginal.PlayerRider.PROptions.PREffect;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

class PRUtils {
    static void alert(String key, Player player, Player duck) {
        if (!PR.cooldown.isActive(key + ".alertPlayer", player, duck)) {
            userMessage(player, "player." + key, player, duck);
            PR.cooldown.set(key + ".alertPlayer", player, duck);
        }

        if (!PR.cooldown.isActive(key + ".alertDuck", player, duck)) {
            userMessage(duck, "duck." + key, player, duck);
            PR.cooldown.set(key + ".alertDuck", player, duck);
        }

        if (!PR.cooldown.isActive(key + ".broadcast", player, duck)) {
            broadcast(key, player, duck);
            PR.cooldown.set(key + ".broadcast", player, duck);
        }
    }

    static void broadcast(String key, CommandSender player, CommandSender duck) {
        String message = PR.config.getString("broadcast." + key);
        if (!message.isEmpty()) Bukkit.broadcastMessage(prepareMessage(message, player, duck));
    }

    static boolean canSeeRider(Player player) {
        return PR.options.hide_rider_maxPitch != 0 && player.getLocation().getPitch() < PR.options.hide_rider_maxPitch;
    }

    static void consume(Player player, ItemStack item, String key) {
        if (PR.config.getBoolean("consume_items." + key) && !player.hasPermission("playerrider." + key + ".keepitem")
                && item.getType() != Material.AIR)
            item.setAmount(item.getAmount() - 1);
    }

    static boolean dataSave() {
        try {
            if (!PR.file.exists()) PR.file.createNewFile();
            PR.users.save(PR.file);
            return true;
        }
        catch (IOException e) {
            Bukkit.getLogger().severe(PR.options.file_not_writable_err);
            return false;
        }
    }

    static boolean duckAllowed(Player duck, int passengersCount) {
        for (String group : PR.options.max_riders_groups)
            if ((group.equals("default") || duck.hasPermission("playerrider.duck." + group))
                    && PR.config.getInt("max_riders." + group) >= passengersCount)
                return true;
        return false;
    }

    static void effectsClear(Player player, List<PREffect> effects) {
        for (PREffect effect : effects) player.removePotionEffect(effect.type);
    }

    static void effectsApply(Player player, List<PREffect> effects) {
        for (PREffect effect : effects) {
            int amplifier = effectAmplifier(player, effect);
            if (amplifier < 0) continue;

            try {
                player.addPotionEffect(
                        new PotionEffect(effect.type, effect.duration, amplifier, false, false, false), true);
            }
            catch (Exception e) {
                Bukkit.getLogger().warning(PR.options.potion_effect_err.replace("type", effect.type.toString()));
            }
        }
    }

    static int effectAmplifier(Player player, PREffect effect) {
        int count = player.getPassengers().size();
        if (count == 0) return 0;
        if (count == 1) return effect.init;
        int value = effect.init + (count - 1) * effect.inc;
        return (effect.inc < 0) ? Math.min(effect.max, value) : Math.max(value, effect.max);
    }

    static boolean isPlayer(Entity entity) {
        return (entity instanceof Player) && !entity.hasMetadata("NPC");
    }

    static boolean playerAllowed(Player player, String key) {
        return player.hasPermission("playerrider." + key) || player.hasPermission("playerrider." + key + ".keepitem");
    }

    static String prepareMessage(String message) {
        return prepareMessage(message, null, null);
    }

    static String prepareMessage(String message, CommandSender player, CommandSender duck) {
        message = message.replace("{prefix}", PR.config.getString("prefix"));
        // @formatter:off
        if (player != null) message = message.replace("{player}", player.getName());
        if (duck   != null) message = message.replace("{duck}",     duck.getName());
        // @formatter:on
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    static String rideKey(Player duck) {
        return duck.getUniqueId() + ".ridable";
    }

    static boolean rideIsActivated(Player player) {
        if (!player.hasPermission("playerrider.duck")) return false;
        String key = rideKey(player);
        return !PR.users.contains(key) || PR.users.getBoolean(key);
    }

    static String rideToggle(Player player) {
        String  key    = rideKey(player);
        boolean status = !PR.users.contains(key) || PR.users.getBoolean(key);

        PR.users.set(key, !status);
        if (!dataSave()) return "toggleErr";

        return status ? "statusOff" : "statusOn";
    }

    static void userMessage(CommandSender sender, String key) {
        userMessage(sender, key, null, null);
    }

    static void userMessage(CommandSender sender, String key, CommandSender player, CommandSender duck) {
        String message = PR.config.getString(key);
        if (!message.isEmpty()) sender.sendMessage(prepareMessage(message, player, duck));
    }

    static void warn(String key, Player player, Player duck, int delay) {
        String[] parts = key.split("\\.");
        if (parts.length != 2 || !parts[1].equals("perform")) return;

        String message = PR.config.getString("warn_player_when_cooldown." + parts[0]);
        if (message.isEmpty()) return;

        ((Player) player).spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(prepareMessage(message.replace("{delay}", "" + delay), player, duck)));
    }
}
