package com.mcrp.darkrp.crime;

import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.storage.DataStore;
import com.mcrp.darkrp.util.LocUtil;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles arresting/jailing players: teleporting to the jail cell, counting down,
 * and releasing them (back to where they were arrested, or early via /release).
 */
public class JailManager {

    private final Plugin plugin;
    private final DataStore dataStore;
    /** UUIDs currently being teleported by this manager, so the teleport guard doesn't block us. */
    private final Set<java.util.UUID> internalTeleport = new HashSet<>();
    private BukkitTask tickTask;

    public JailManager(Plugin plugin, DataStore dataStore) {
        this.plugin = plugin;
        this.dataStore = dataStore;
    }

    public int defaultTimeSeconds() {
        return plugin.getConfig().getInt("jail.default-time-seconds", 60);
    }

    public Location jailLocation() {
        String worldName = plugin.getConfig().getString("jail.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        double x = plugin.getConfig().getDouble("jail.x", 0.5);
        double y = plugin.getConfig().getDouble("jail.y", 65.0);
        double z = plugin.getConfig().getDouble("jail.z", 0.5);
        float yaw = (float) plugin.getConfig().getDouble("jail.yaw", 0.0);
        float pitch = (float) plugin.getConfig().getDouble("jail.pitch", 0.0);
        return new Location(world, x, y, z, yaw, pitch);
    }

    public boolean isInternalTeleport(Player player) {
        return internalTeleport.contains(player.getUniqueId());
    }

    public void jail(Player target, int seconds) {
        PlayerRecord record = dataStore.get(target.getUniqueId());
        if (record == null) {
            return;
        }
        record.setPreJailLocation(LocUtil.serialize(target.getLocation()));
        record.setJailedUntilMillis(System.currentTimeMillis() + seconds * 1000L);
        teleportInternally(target, jailLocation());
        Msg.error(target, "You have been jailed for " + seconds + " seconds.");
    }

    public void release(Player target) {
        PlayerRecord record = dataStore.get(target.getUniqueId());
        if (record == null) {
            return;
        }
        record.setJailedUntilMillis(0);
        Location back = LocUtil.deserialize(record.getPreJailLocation());
        if (back == null || back.getWorld() == null) {
            back = target.getWorld().getSpawnLocation();
        }
        teleportInternally(target, back);
        Msg.success(target, "You have been released from jail.");
    }

    private void teleportInternally(Player player, Location location) {
        internalTeleport.add(player.getUniqueId());
        player.teleport(location);
        Bukkit.getScheduler().runTask(plugin, () -> internalTeleport.remove(player.getUniqueId()));
    }

    public void startTickTask() {
        stopTickTask();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                PlayerRecord record = dataStore.get(player.getUniqueId());
                if (record == null || record.getJailedUntilMillis() <= 0) {
                    continue;
                }
                long remainingMs = record.getJailedUntilMillis() - System.currentTimeMillis();
                if (remainingMs <= 0) {
                    release(player);
                } else {
                    player.sendActionBar(Msg.parse("<red>Jailed: <white>" + (remainingMs / 1000 + 1) + "s</white> remaining</red>"));
                }
            }
        }, 20L, 20L);
    }

    public void stopTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }
}
