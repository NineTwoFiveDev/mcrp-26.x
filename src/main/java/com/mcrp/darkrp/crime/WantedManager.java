package com.mcrp.darkrp.crime;

import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.storage.DataStore;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Tracks wanted status (with expiry) for players.
 */
public class WantedManager {

    private final Plugin plugin;
    private final DataStore dataStore;
    private BukkitTask expiryTask;

    public WantedManager(Plugin plugin, DataStore dataStore) {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    public int defaultDurationSeconds() {
        return plugin.getConfig().getInt("wanted.default-duration-seconds", 600);
    }

    public void setWanted(Player target, String reason, int durationSeconds) {
        PlayerRecord record = dataStore.get(target.getUniqueId());
        if (record == null) {
            return;
        }
        record.setWanted(true);
        record.setWantedReason(reason == null || reason.isBlank() ? "No reason given" : reason);
        record.setWantedExpiryMillis(System.currentTimeMillis() + durationSeconds * 1000L);
        Bukkit.broadcast(Msg.parse("<red><bold>WANTED:</bold></red> <white>" + target.getName()
                + "</white> <gray>- " + record.getWantedReason() + "</gray>"));
    }

    public void clearWanted(Player target) {
        PlayerRecord record = dataStore.get(target.getUniqueId());
        if (record == null) {
            return;
        }
        record.setWanted(false);
        record.setWantedReason("");
        record.setWantedExpiryMillis(0);
    }

    public boolean isWanted(Player player) {
        PlayerRecord record = dataStore.get(player.getUniqueId());
        return record != null && record.isWanted();
    }

    public void startExpiryTask() {
        stopExpiryTask();
        expiryTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerRecord record = dataStore.get(player.getUniqueId());
                if (record == null) {
                    continue;
                }
                if (record.getWantedExpiryMillis() > 0 && record.getWantedExpiryMillis() <= now && record.isWanted()) {
                    clearWanted(player);
                    Msg.success(player, "Your wanted status has expired.");
                } else if (record.getWantedExpiryMillis() > 0 && record.getWantedExpiryMillis() <= now) {
                    // already cleared flag but stale expiry timestamp; normalize
                    record.setWantedExpiryMillis(0);
                }
            }
        }, 20L, 20L);
    }

    public void stopExpiryTask() {
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
    }
}
