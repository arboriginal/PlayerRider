package me.arboriginal.PlayerRider;

import java.io.File;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public class PR extends JavaPlugin {
  static File              file;
  static FileConfiguration config;
  static PROptions         options;
  static PRcooldown        cooldown;
  static YamlConfiguration users;

  // JavaPlugin methods -----------------------------------------------------------------------------------------------

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (command.getName().equalsIgnoreCase("prider-reload")) {
      reloadConfig();
      PRUtils.userMessage(sender, "reload");

      return true;
    }

    if (command.getName().equalsIgnoreCase("prider-toggle")) {
      if (sender instanceof Player)
        PRUtils.userMessage(sender, PRUtils.rideToggle((Player) sender));
      else
        PRUtils.userMessage(sender, "toggleWarn");
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
    reloadConfig();

    cooldown = new PRcooldown();
    users    = new YamlConfiguration();
    file     = new File(getDataFolder(), "usersPreferences.yml");

    if (file.exists())
      users = YamlConfiguration.loadConfiguration(file);
    else
      PRUtils.dataSave();

    getServer().getPluginManager().registerEvents(new PRListener(this), this);
  }

  @Override
  public void reloadConfig() {
    super.reloadConfig();

    saveDefaultConfig();
    config = getConfig();
    config.options().copyDefaults(true);
    saveConfig();

    options = new PROptions();
  }
}
