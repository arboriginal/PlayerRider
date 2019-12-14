package com.github.arboriginal.PlayerRider;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.spigotmc.event.entity.EntityDismountEvent;

class PRListener implements Listener {
    private PR plugin;

    // Constructor methods ----------------------------------------------------------------------------------------------

    public PRListener(PR pr) {
        plugin = pr;
    }

    // Listener methods -------------------------------------------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    private void onEntityDamage(EntityDamageEvent event) {
        Entity injured = event.getEntity();
        if (!PRUtils.isPlayer(injured)) return;

        if (PR.options.prevent_suffocation
                && event.getCause() == DamageCause.SUFFOCATION && PRUtils.isPlayer(injured.getVehicle())) {
            event.setCancelled(true);
            return;
        }

        if (PR.options.getoff_when_hurt && PRUtils.isPlayer(injured.getVehicle())) {
            injured.leaveVehicle();
            return;
        }

        if (PR.options.eject_when_hurt && injured.getPassengers().size() > 0) {
            injured.eject();
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        Entity injured = event.getEntity();

        if (!PRUtils.isPlayer(injured)) return;

        if (PR.options.prevent_hit_rider && PRUtils.isPlayer(event.getDamager())) {
            Entity vehicle = injured.getVehicle();

            if (vehicle != null && event.getDamager().equals(vehicle)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    private void onEntityDismount(EntityDismountEvent event) {
        Entity duck = event.getDismounted();
        if (!(duck instanceof Player)) return;

        Entity player = event.getEntity();
        if (!(player instanceof Player)) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                preventBadLocation(player, duck);
            }
        }.runTask(plugin);

        if (!((Player) duck).canSee(((Player) player))) ((Player) duck).showPlayer(plugin, ((Player) player));

        KeyedBossBar bossbar = Bukkit.getBossBar(bossbarKey(((Player) duck)));
        if (bossbar != null) bossbar.removeAll();

        if (PR.options.effects_ridden_enabled)
            PRUtils.effectsClear(((Player) duck), PR.options.effects_ridden);
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        // https://www.spigotmc.org/threads/how-would-i-stop-an-event-from-being-called-twice.135234/#post-1434104
        if (event.getHand() == EquipmentSlot.OFF_HAND || !PR.options.boost_allowed) return;

        Player player = event.getPlayer();
        // @formatter:off
        if (!PRUtils.playerAllowed(player, "whip") || player.getLocation().getPitch() < PR.options.boost_maxPitch
         || !PRUtils.isPlayer(player.getVehicle())) return;
        // @formatter:on
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!PR.options.allowed_items__whip.contains(item.getType().toString())) return;

        Player duck = (Player) player.getVehicle();
        if (PR.cooldown.isActive("whip.perform", player, duck, true)) return;

        if (PR.options.boost_amplifier > 0 && PR.options.boost_duration > 0) {
            if (PR.options.boost_whip_sound != null) {
                player.playSound(player.getLocation(),
                        PR.options.boost_whip_sound, PR.options.boost_whip_volume, PR.options.boost_whip_pitch);
                duck.playSound(duck.getLocation(),
                        PR.options.boost_whip_sound, PR.options.boost_whip_volume, PR.options.boost_whip_pitch);
            }

            duck.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PR.options.boost_duration,
                    PR.options.boost_amplifier, false, false, false), true);
        }

        PRUtils.consume(player, item, "whip");
        PRUtils.alert("whip", player, duck);
        PR.cooldown.set("whip.perform", player, duck);

        if (PR.options.effects_whipped_enabled) PRUtils.effectsApply(duck, PR.options.effects_whipped);
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // https://www.spigotmc.org/threads/how-would-i-stop-an-event-from-being-called-twice.135234/#post-1434104
        if (event.getHand() == EquipmentSlot.OFF_HAND || !PRUtils.isPlayer(event.getRightClicked())) return;

        Player player = event.getPlayer();

        if (!PR.options.disabled_worlds.isEmpty() && PR.options.disabled_worlds.contains(player.getWorld().getName()))
            return;

        Player duck = (Player) event.getRightClicked();

        if (player.getPassengers().contains(duck)) {
            if (player.getLocation().getPitch() < PR.options.eject_maxPitch && player.hasPermission("playerrider.eject")
                    && !PR.cooldown.isActive("eject.perform", player, true)) {
                player.eject();
                PRUtils.alert("eject", duck, player);
                PR.cooldown.clear("eject.perform", player);
            }

            return;
        }

        if (player.isInsideVehicle() || !PRUtils.playerAllowed(player, "ride")
                || !PRUtils.isRidable(duck) || !PRUtils.rideIsActivated(duck))
            return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!PR.options.allowed_items__ride.contains(item.getType().toString())) return;

        List<Entity> riders = duck.getPassengers();
        int          count  = 0;

        while (riders.size() == 1 && PRUtils.isPlayer(riders.get(0))) {
            count++;
            duck = (Player) riders.get(0);
            if (duck.equals(player)) return;
            riders = duck.getPassengers();
        }

        if (riders.size() > 0) return;

        if (!PRUtils.duckAllowed(duck, count + 1)) {
            PRUtils.userMessage(player, "tooMany", player, duck);
            return;
        }

        if (duck.addPassenger(player)) {
            PRUtils.consume(player, item, "ride");
            PRUtils.alert("ride", player, duck);
            PR.cooldown.set("ride.perform", player, duck);
            PR.cooldown.set("eject.perform", duck);

            if (!PRUtils.canSeeRider(duck) && duck.getPassengers().get(0).equals(player))
                duck.hidePlayer(plugin, player);

            if (PR.options.bossbar_enabled) {
                KeyedBossBar bossbar = Bukkit.createBossBar(bossbarKey(duck),
                        PR.options.bossbar_title.replace("{player}", player.getName()),
                        PR.options.bossbar_color, PR.options.bossbar_style);

                bossbar.addPlayer(duck);
                bossbar.setProgress(PR.options.bossbar_pct);
                bossbar.setVisible(true);
            }

            if (PR.options.effects_ridden_enabled && !duck.isInsideVehicle())
                PRUtils.effectsApply(duck, PR.options.effects_ridden);
        }
        else PRUtils.userMessage(player, "failed", player, duck);
    }

    @EventHandler(ignoreCancelled = true)
    private void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getPassengers().isEmpty()) return;

        Entity rider = player.getPassengers().get(0);
        if (!(rider instanceof Player)) return;

        if (PR.options.effects_ridden_enabled) PRUtils.effectsApply(((Player) player), PR.options.effects_ridden);
        if (PR.options.hide_rider_maxPitch == 0) return;

        if (PRUtils.canSeeRider(player)) player.showPlayer(plugin, ((Player) rider));
        else player.hidePlayer(plugin, ((Player) rider));
    }

    // Helper methods ---------------------------------------------------------------------------------------------------

    private NamespacedKey bossbarKey(Player player) {
        return new NamespacedKey(plugin, "PlayerRider." + player.getUniqueId());
    }

    private void preventBadLocation(Entity player, Entity from) {
        Block block = player.getLocation().getBlock();
        if (!block.isPassable() || !block.getRelative(BlockFace.UP).isPassable()) {
            Entity root = from;
            while (root.isInsideVehicle()) root = root.getVehicle();
            player.teleport(root.getLocation());
        }
    }
}
