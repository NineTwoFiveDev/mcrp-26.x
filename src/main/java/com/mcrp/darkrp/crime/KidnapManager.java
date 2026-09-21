package com.mcrp.darkrp.crime;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.model.Job;
import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.storage.DataStore;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implements a DarkRP-style kidnapping: restrain a nearby player and demand a
 * ransom from anyone willing to pay it. The victim is released on payment,
 * on a police/admin override, or automatically after a timeout.
 */
public class KidnapManager {

    private static final class Session {
        final UUID kidnapper;
        final double ransom;
        final Location capturePoint;
        final long expiryMillis;

        Session(UUID kidnapper, double ransom, Location capturePoint, long expiryMillis) {
            this.kidnapper = kidnapper;
            this.ransom = ransom;
            this.capturePoint = capturePoint;
            this.expiryMillis = expiryMillis;
        }
    }

    private final Plugin plugin;
    private final DataStore dataStore;
    private final EconomyManager economy;
    private final JobManager jobManager;

    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private BukkitTask tickTask;

    public KidnapManager(Plugin plugin, DataStore dataStore, EconomyManager economy, JobManager jobManager) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        this.economy = economy;
        this.jobManager = jobManager;
    }

    public boolean isKidnapped(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void attemptKidnap(Player kidnapper, Player target, double ransom) {
        if (kidnapper.getUniqueId().equals(target.getUniqueId())) {
            Msg.error(kidnapper, "You can't kidnap yourself.");
            return;
        }
        double maxDistance = plugin.getConfig().getDouble("kidnap.max-distance", 3.0);
        if (!kidnapper.getWorld().equals(target.getWorld()) || kidnapper.getLocation().distance(target.getLocation()) > maxDistance) {
            Msg.error(kidnapper, "You need to be closer to " + target.getName() + " to kidnap them.");
            return;
        }
        if (plugin.getConfig().getBoolean("kidnap.police-immune", true)) {
            PlayerRecord targetRecord = dataStore.get(target.getUniqueId());
            Job targetJob = targetRecord != null ? jobManager.getCurrentJob(targetRecord) : null;
            if (targetJob != null && targetJob.isPolice()) {
                Msg.error(kidnapper, "You can't kidnap a police officer.");
                return;
            }
        }
        if (isKidnapped(target)) {
            Msg.error(kidnapper, target.getName() + " is already kidnapped.");
            return;
        }
        if (ransom <= 0) {
            Msg.error(kidnapper, "The ransom must be a positive amount.");
            return;
        }
        long cooldownSeconds = plugin.getConfig().getLong("kidnap.cooldown-seconds", 45);
        Long last = cooldowns.get(kidnapper.getUniqueId());
        if (last != null) {
            long remaining = (last + cooldownSeconds * 1000L - System.currentTimeMillis()) / 1000L;
            if (remaining > 0) {
                Msg.error(kidnapper, "You must wait " + remaining + "s before kidnapping again.");
                return;
            }
        }

        int timeoutSeconds = plugin.getConfig().getInt("kidnap.timeout-seconds", 120);
        sessions.put(target.getUniqueId(), new Session(kidnapper.getUniqueId(), ransom, target.getLocation(),
                System.currentTimeMillis() + timeoutSeconds * 1000L));
        cooldowns.put(kidnapper.getUniqueId(), System.currentTimeMillis());

        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, timeoutSeconds * 20, 250, false, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, timeoutSeconds * 20, 250, false, false, false));

        Bukkit.broadcast(Msg.parse("<dark_red><bold>KIDNAPPED:</bold></dark_red> <white>" + target.getName()
                + "</white> <gray>has been kidnapped! Pay the </gray><gold>" + economy.format(ransom)
                + "</gold><gray> ransom with </gray><click:suggest_command:'/payransom " + target.getName()
                + "'><yellow>/payransom " + target.getName() + "</yellow></click>"));
    }

    public void payRansom(Player payer, Player target) {
        Session session = sessions.get(target.getUniqueId());
        if (session == null) {
            Msg.error(payer, target.getName() + " isn't being held for ransom.");
            return;
        }
        if (!economy.has(payer, session.ransom)) {
            Msg.error(payer, "You don't have enough money to pay the ransom.");
            return;
        }
        OfflinePlayer kidnapper = Bukkit.getOfflinePlayer(session.kidnapper);
        economy.withdraw(payer, session.ransom);
        economy.deposit(kidnapper, session.ransom);
        release(target);
        Bukkit.broadcast(Msg.parse("<green>" + payer.getName() + " paid " + target.getName() + "'s ransom. They've been released.</green>"));
    }

    public boolean freeKidnap(Player target) {
        if (!isKidnapped(target)) {
            return false;
        }
        release(target);
        return true;
    }

    private void release(Player target) {
        sessions.remove(target.getUniqueId());
        target.removePotionEffect(PotionEffectType.SLOWNESS);
        target.removePotionEffect(PotionEffectType.WEAKNESS);
    }

    public void startTickTask() {
        stopTickTask();
        double radius = plugin.getConfig().getDouble("kidnap.radius", 5.0);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            for (UUID victimId : Map.copyOf(sessions).keySet()) {
                Session session = sessions.get(victimId);
                if (session == null) {
                    continue;
                }
                Player victim = Bukkit.getPlayer(victimId);
                if (victim == null || !victim.isOnline()) {
                    continue;
                }
                if (now >= session.expiryMillis) {
                    release(victim);
                    Msg.raw(victim, "<gray>Your kidnapping timed out - you've been released.</gray>");
                    continue;
                }
                if (victim.getWorld().equals(session.capturePoint.getWorld())
                        && victim.getLocation().distance(session.capturePoint) > radius) {
                    victim.teleport(session.capturePoint);
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
