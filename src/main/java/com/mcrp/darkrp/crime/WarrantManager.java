package com.mcrp.darkrp.crime;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks police-issued warrants: a temporary authorization for police to bypass
 * a specific player's locked doors (e.g. during a raid).
 */
public class WarrantManager {

    private final Plugin plugin;
    private final Map<UUID, Long> warrants = new HashMap<>();

    public WarrantManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public int defaultDurationSeconds() {
        return plugin.getConfig().getInt("warrant.default-duration-seconds", 300);
    }

    public void issue(Player target, int durationSeconds) {
        warrants.put(target.getUniqueId(), System.currentTimeMillis() + durationSeconds * 1000L);
    }

    public boolean hasWarrant(UUID target) {
        Long expiry = warrants.get(target);
        if (expiry == null) {
            return false;
        }
        if (expiry <= System.currentTimeMillis()) {
            warrants.remove(target);
            return false;
        }
        return true;
    }

    public long remainingSeconds(UUID target) {
        Long expiry = warrants.get(target);
        if (expiry == null) {
            return 0;
        }
        return Math.max(0, (expiry - System.currentTimeMillis()) / 1000L);
    }
}
