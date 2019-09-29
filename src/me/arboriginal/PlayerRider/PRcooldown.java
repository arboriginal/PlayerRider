package me.arboriginal.PlayerRider;

import java.util.HashMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class PRcooldown {
  private HashMap<String, Long> cooldowns;

  // Constructor methods ----------------------------------------------------------------------------------------------

  PRcooldown() {
    cooldowns = new HashMap<String, Long>() {
      private static final long serialVersionUID = 1L;
    };
  }

  // Package methods --------------------------------------------------------------------------------------------------

  void clear(String key, Player player) {
    clear(key, player, null);
  }

  boolean isActive(String key, Player player, boolean warn) {
    return isActive(key, player, null, warn);
  }

  boolean isActive(String key, Player player, Player duck) {
    return isActive(key, player, duck, false);
  }

  boolean isActive(String key, Player player, Player duck, boolean warn) {
    Long now = getCurrentTime(), next = get(key, player, duck);

    if (next > now) {
      if (warn) PRUtils.warn(key, player, duck, (int) Math.max(1, Math.floor((next - now) / 1000)));

      return true;
    }

    return false;
  }

  void set(String key, Player player) {
    set(key, player, null);
  }

  void set(String key, Player player, Player duck) {
    int delay = PR.config.getInt("cooldowns." + key) * 1000;

    if (delay > 0) {
      cooldowns.put(id(key, player, duck), getCurrentTime() + delay);
    }
  }

  // Private methods --------------------------------------------------------------------------------------------------

  private void clear(String key, Player player, Player duck) {
    cooldowns.remove(id(key, player, duck));
  }

  private Long get(String key, Player player, Player duck) {
    key = id(key, player, duck);

    return cooldowns.containsKey(key) ? cooldowns.get(key) : 0;
  }

  private Long getCurrentTime() {
    return System.currentTimeMillis();
  }

  private String id(String key, CommandSender player, CommandSender duck) {
    return key + "..." + player.getName() + "." + ((duck == null) ? ".." : duck.getName());
  }
}
