package com.mcrp.darkrp.crime;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.model.Job;
import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.storage.DataStore;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implements the classic DarkRP "mug" mechanic: stand near a target and channel
 * for a few seconds to steal a percentage of their cash. The channel breaks if
 * either party moves out of range.
 */
public class MugManager {

    private final Plugin plugin;
    private final DataStore dataStore;
    private final EconomyManager economy;
    private final JobManager jobManager;

    private final Set<UUID> activeMuggers = new HashSet<>();
    private final Set<UUID> activeTargets = new HashSet<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public MugManager(Plugin plugin, DataStore dataStore, EconomyManager economy, JobManager jobManager) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        this.economy = economy;
        this.jobManager = jobManager;
    }

    public void attemptMug(Player mugger, Player target) {
        if (mugger.getUniqueId().equals(target.getUniqueId())) {
            Msg.error(mugger, "You can't mug yourself.");
            return;
        }
        double maxDistance = plugin.getConfig().getDouble("mug.max-distance", 3.0);
        if (!mugger.getWorld().equals(target.getWorld()) || mugger.getLocation().distance(target.getLocation()) > maxDistance) {
            Msg.error(mugger, "You need to be closer to " + target.getName() + " to mug them.");
            return;
        }
        if (plugin.getConfig().getBoolean("mug.police-immune", true)) {
            PlayerRecord targetRecord = dataStore.get(target.getUniqueId());
            Job targetJob = targetRecord != null ? jobManager.getCurrentJob(targetRecord) : null;
            if (targetJob != null && targetJob.isPolice()) {
                Msg.error(mugger, "You can't mug a police officer.");
                return;
            }
        }
        if (activeMuggers.contains(mugger.getUniqueId())) {
            Msg.error(mugger, "You are already mugging someone.");
            return;
        }
        if (activeTargets.contains(target.getUniqueId())) {
            Msg.error(mugger, target.getName() + " is already being mugged.");
            return;
        }
        long cooldownSeconds = plugin.getConfig().getLong("mug.cooldown-seconds", 30);
        Long last = cooldowns.get(mugger.getUniqueId());
        if (last != null) {
            long remaining = (last + cooldownSeconds * 1000L - System.currentTimeMillis()) / 1000L;
            if (remaining > 0) {
                Msg.error(mugger, "You must wait " + remaining + "s before mugging again.");
                return;
            }
        }

        activeMuggers.add(mugger.getUniqueId());
        activeTargets.add(target.getUniqueId());
        cooldowns.put(mugger.getUniqueId(), System.currentTimeMillis());

        int channelSeconds = plugin.getConfig().getInt("mug.channel-seconds", 5);
        Msg.raw(mugger, "<yellow>Mugging " + target.getName() + "... stay close for " + channelSeconds + "s.</yellow>");
        Msg.error(target, mugger.getName() + " is trying to mug you! Move away to escape.");

        runChannelTick(mugger.getUniqueId(), target.getUniqueId(), channelSeconds, maxDistance);
    }

    private void runChannelTick(UUID muggerId, UUID targetId, int ticksLeft, double maxDistance) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player mugger = Bukkit.getPlayer(muggerId);
            Player target = Bukkit.getPlayer(targetId);
            if (mugger == null || target == null || !mugger.isOnline() || !target.isOnline()) {
                endSession(muggerId, targetId);
                return;
            }
            if (!mugger.getWorld().equals(target.getWorld()) || mugger.getLocation().distance(target.getLocation()) > maxDistance) {
                endSession(muggerId, targetId);
                Msg.error(mugger, "Mug failed - " + target.getName() + " got away.");
                Msg.success(target, "You escaped the mugging attempt!");
                return;
            }
            if (ticksLeft <= 0) {
                finishMug(mugger, target);
                endSession(muggerId, targetId);
                return;
            }
            runChannelTick(muggerId, targetId, ticksLeft - 1, maxDistance);
        }, 20L);
    }

    private void finishMug(Player mugger, Player target) {
        double balance = economy.getBalance(target);
        int minPct = plugin.getConfig().getInt("mug.min-steal-percent", 10);
        int maxPct = plugin.getConfig().getInt("mug.max-steal-percent", 30);
        int pct = ThreadLocalRandom.current().nextInt(minPct, maxPct + 1);
        double amount = Math.round(balance * (pct / 100.0) * 100.0) / 100.0;
        if (amount <= 0) {
            Msg.error(mugger, target.getName() + " had nothing worth stealing.");
            return;
        }
        economy.withdraw(target, amount);
        economy.deposit(mugger, amount);
        Msg.success(mugger, "You mugged " + target.getName() + " for " + economy.format(amount) + "!");
        Msg.error(target, mugger.getName() + " mugged you for " + economy.format(amount) + "!");
    }

    private void endSession(UUID muggerId, UUID targetId) {
        activeMuggers.remove(muggerId);
        activeTargets.remove(targetId);
    }
}
