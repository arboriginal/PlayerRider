package me.arboriginal.PlayerRider;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PlayerRider extends JavaPlugin implements Listener {
	protected FileConfiguration	config;

	// -----------------------------------------------------------------------------------------------
	// JavaPlugin related methods
	// -----------------------------------------------------------------------------------------------

	@Override
	public void onEnable() {
		config = getConfig();

		if (!config.contains("message")) {
			config.set("message", "<player> is riding <duck>");
			saveConfig();
		}

		if (!config.contains("ejectmessage")) {
			config.set("ejectmessage", "<duck> has ejected <player>");
			saveConfig();
		}

		getServer().getPluginManager().registerEvents(this, this);
	}

	// -----------------------------------------------------------------------------------------------
	// Listener related methods
	// -----------------------------------------------------------------------------------------------

	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Player) {
			Player player = event.getPlayer();

			if (!duckEjectPassenger(player, event.getRightClicked()) && playerCanRide(player)) {
				Player vehicle = getVehicle(player);

				if (vehicle == null) {
					vehicle = (Player) event.getRightClicked();
					Player duck = getRootVehicle(vehicle);

					if (duck.hasPermission("playerrider.beridden")) {
						getLastPassenger(vehicle).setPassenger(player);
						alertPlayers(player, duck, "message");
					}
				}
				else {
					vehicle.eject();
				}
			}
		}
	}

	// -----------------------------------------------------------------------------------------------
	// Custom methods
	// -----------------------------------------------------------------------------------------------

	private boolean playerCanRide(Player player) {
		return player.hasPermission("playerrider.ride") && player.getPassenger() == null;
	}

	private boolean duckEjectPassenger(Player duck, Entity passenger) {
		if (duck.hasPermission("playerrider.eject")) {
			if (passenger.equals(duck.getPassenger())) {
				duck.eject();
				alertPlayers((Player) passenger, duck, "ejectmessage");
				
				return true;
			}
		}

		return false;
	}

	private Player getRootVehicle(Player vehicle) {
		while (getVehicle(vehicle) != null) {
			vehicle = (Player) getVehicle(vehicle);
		}

		return vehicle;
	}

	private Player getLastPassenger(Player vehicle) {
		while (vehicle.getPassenger() != null && vehicle.getPassenger() instanceof Player) {
			vehicle = (Player) vehicle.getPassenger();
		}

		return vehicle;
	}

	private Player getVehicle(Player player) {
		for (Player onlinePlayer : getServer().getOnlinePlayers()) {
			Entity passenger = onlinePlayer.getPassenger();

			if (passenger instanceof Player && passenger.getEntityId() == player.getEntityId()) {
				return onlinePlayer;
			}
		}

		return null;
	}

	private void alertPlayers(Player player, Player duck, String key) {
		String message = config.getString(key);

		if (!message.isEmpty()) {
			getServer().broadcastMessage(message.replace("<player>", player.getName()).replace("<duck>", duck.getName()));
		}
	}
}
