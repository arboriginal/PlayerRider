package com.github.arboriginal.PlayerRider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.potion.PotionEffectType;

class PROptions {
    BarColor bossbar_color;
    BarStyle bossbar_style;
    boolean  boost_allowed, bossbar_enabled, effects_ridden_enabled, effects_whipped_enabled,
            eject_when_hurt, getoff_when_hurt, prevent_hit_rider, prevent_suffocation;
    double   boost_maxPitch, bossbar_pct, eject_maxPitch, hide_rider_maxPitch;
    float    boost_whip_pitch, boost_whip_volume;
    int      boost_amplifier, boost_duration;
    Sound    boost_whip_sound;
    String   bossbar_title, file_not_writable_err, potion_effect_err;

    List<PREffect> effects_ridden, effects_whipped;
    List<String>   allowed_items__ride, allowed_items__whip, disabled_worlds;
    Set<String>    max_riders_groups;

    // Constructor methods ----------------------------------------------------------------------------------------------

    PROptions() {
        // Bossbar attributes values
        bossbar_color = (BarColor) getEnumValue(BarColor.class, "bossbar_on_ridden_player.color");
        bossbar_style = (BarStyle) getEnumValue(BarStyle.class, "bossbar_on_ridden_player.style");
        // Effects list values
        effects_ridden  = getEffects("affect_ridden_player.effects");
        effects_whipped = getEffects("affect_whipped_player.effects");
        // Boolean values
        boost_allowed           = PR.config.getBoolean("boost_allowed");
        eject_when_hurt         = PR.config.getBoolean("eject_when_hurt");
        getoff_when_hurt        = PR.config.getBoolean("getoff_when_hurt");
        prevent_hit_rider       = PR.config.getBoolean("prevent_hit_rider");
        prevent_suffocation     = PR.config.getBoolean("prevent_suffocation");
        bossbar_enabled         = (bossbar_color == null || bossbar_style == null) ? null
                : PR.config.getBoolean("bossbar_on_ridden_player.enabled");
        effects_ridden_enabled  = effects_ridden.isEmpty() ? false
                : PR.config.getBoolean("affect_ridden_player.enabled");
        effects_whipped_enabled = effects_ridden.isEmpty() ? false
                : PR.config.getBoolean("affect_whipped_player.enabled");
        // Double values
        boost_maxPitch      = PR.config.getDouble("boost_maxPitch");
        bossbar_pct         = PR.config.getDouble("bossbar_on_ridden_player.progress");
        eject_maxPitch      = PR.config.getDouble("eject_maxPitch");
        hide_rider_maxPitch = PR.config.getDouble("hide_rider_maxPitch");
        // Float values
        boost_whip_pitch  = (float) PR.config.getDouble("boost_whip_pitch");
        boost_whip_volume = (float) PR.config.getDouble("boost_whip_volume");
        // Integer values
        boost_amplifier = PR.config.getInt("boost_amplifier");
        boost_duration  = PR.config.getInt("boost_duration");
        // Sound values
        boost_whip_sound = (boost_whip_volume > 0) ? (Sound) getEnumValue(Sound.class, "boost_whip_sound") : null;
        // String values
        bossbar_title         = PR.config.getString("bossbar_on_ridden_player.title");
        file_not_writable_err = PRUtils.prepareMessage(PR.config.getString("fileErr"));
        potion_effect_err     = PRUtils.prepareMessage(PR.config.getString("effectErr"));
        // String list & set values
        allowed_items__ride = PR.config.getStringList("allowed_items.ride");
        allowed_items__whip = PR.config.getStringList("allowed_items.whip");
        disabled_worlds     = PR.config.getStringList("disabled_worlds");
        max_riders_groups   = PR.config.getConfigurationSection("max_riders").getKeys(false);
    }

    // Package classes --------------------------------------------------------------------------------------------------

    class PREffect { // @formatter:off
        PotionEffectType type; int init, inc, max, duration;
        PREffect(PotionEffectType a, int b, int c, int d, int e) { type = a; init = b; inc = c; max = d; duration = e; }
    } // @formatter:on

    // Private methods --------------------------------------------------------------------------------------------------

    private List<PREffect> getEffects(String path) {
        List<PREffect> effects = new ArrayList<PREffect>();
        // @formatter:off
        for (Object effect : PR.config.getList(path)) {
            if (!(effect instanceof Map)) continue; Map<?, ?> e = (Map<?, ?>) effect;
            if (e.containsKey("type") && e.containsKey("init") && e.containsKey("inc")
             && e.containsKey("max")  && e.containsKey("duration")) try {
                 effects.add(new PREffect(PotionEffectType.getByName((String) e.get("type")),
                     (Integer) e.get("init"), (Integer) e.get("inc"),
                     (Integer) e.get("max"),  (Integer) e.get("duration")));
            }
            catch (Exception er) { Bukkit.getLogger().warning("Invalid effect in " + path + ", ignored!"); }
        }
        // @formatter:on
        return effects;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Enum<?> getEnumValue(Class<?> enumClass, String path) {
        String value = PR.config.getString(path);
        if (value.isEmpty()) return null;

        try {
            return Enum.valueOf((Class<Enum>) enumClass, value);
        }
        catch (Exception e) {
            Bukkit.getLogger().warning(PRUtils.prepareMessage(PR.config.getString("sndErr")));
            return null;
        }
    }
}
