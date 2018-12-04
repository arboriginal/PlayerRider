package me.arboriginal.PlayerRider;

import java.util.HashMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PRcooldown {
  public static HashMap<String, Long>   cooldowns;
  public static HashMap<String, String> warnings;

  public PRcooldown() {
    cooldowns = new HashMap<String, Long>() {
      private static final long serialVersionUID = 1L;
    };
  }

  public String id(String key, CommandSender player, CommandSender duck) {
    return key + "..." + player.getName() + "." + ((duck == null) ? ".." : duck.getName());
  }

  public boolean isActive(String key, Player player) {
    return isActive(key, player, null, false);
  }

  public boolean isActive(String key, Player player, boolean warn) {
    return isActive(key, player, null, warn);
  }

  public boolean isActive(String key, Player player, Player duck) {
    return isActive(key, player, duck, false);
  }

  public boolean isActive(String key, Player player, Player duck, boolean warn) {
    Long now = getCurrentTime(), next = get(key, player, duck);

    if (next > now) {
      if (warn) PlayerRider.warn(key, player, duck, (int) Math.max(1, Math.floor((next - now) / 1000)));

      return true;
    }

    return false;
  }

  public Long getCurrentTime() {
    return System.currentTimeMillis();
  }

  public Long get(String key, Player player, Player duck) {
    key = id(key, player, duck);

    return cooldowns.containsKey(key) ? cooldowns.get(key) : 0;
  }

  public void clear(String key, Player player) {
    clear(key, player, null);
  }

  public void clear(String key, Player player, Player duck) {
    cooldowns.remove(id(key, player, duck));
  }

  public void set(String key, Player player) {
    set(key, player, null);
  }

  public void set(String key, Player player, Player duck) {
    int delay = PlayerRider.config.getInt("cooldowns." + key) * 1000;

    if (delay > 0) {
      cooldowns.put(id(key, player, duck), getCurrentTime() + delay);
    }
  }
}
