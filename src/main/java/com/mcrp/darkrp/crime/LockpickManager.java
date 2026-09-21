package com.mcrp.darkrp.crime;

import com.mcrp.darkrp.door.DoorManager;
import com.mcrp.darkrp.model.DoorRecord;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Lets a player carrying the configured lockpick item channel-attempt to
 * unlock someone else's locked door, as an alternative to a police warrant.
 */
public class LockpickManager {

    private final Plugin plugin;
    private final DoorManager doorManager;

    private final Set<UUID> activePickers = new HashSet<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public LockpickManager(Plugin plugin, DoorManager doorManager) {
        this.plugin = plugin;
        this.doorManager = doorManager;
    }

    public Material lockpickItem() {
        Material material = Material.matchMaterial(plugin.getConfig().getString("lockpick.item", "TRIPWIRE_HOOK"));
        return material != null ? material : Material.TRIPWIRE_HOOK;
    }

    public boolean isHoldingLockpick(Player player) {
        return player.getInventory().getItemInMainHand().getType() == lockpickItem();
    }

    public void attemptPick(Player thief, Block door) {
        DoorRecord record = doorManager.getRecordFor(door);
        if (record == null || !record.isLocked()) {
            Msg.error(thief, "This door isn't locked.");
            return;
        }
        if (record.isOwnedBy(thief.getUniqueId())) {
            Msg.error(thief, "You already have access to this door.");
            return;
        }
        if (activePickers.contains(thief.getUniqueId())) {
            Msg.error(thief, "You're already picking a lock.");
            return;
        }
        long cooldownSeconds = plugin.getConfig().getLong("lockpick.cooldown-seconds", 45);
        Long last = cooldowns.get(thief.getUniqueId());
        if (last != null) {
            long remaining = (last + cooldownSeconds * 1000L - System.currentTimeMillis()) / 1000L;
            if (remaining > 0) {
                Msg.error(thief, "Your lockpick needs " + remaining + "s to reset before you can try again.");
                return;
            }
        }

        activePickers.add(thief.getUniqueId());
        int channelSeconds = plugin.getConfig().getInt("lockpick.channel-seconds", 4);
        Msg.raw(thief, "<yellow>Picking the lock... hold still for " + channelSeconds + "s.</yellow>");

        runChannelTick(thief.getUniqueId(), door, channelSeconds);
    }

    private void runChannelTick(UUID thiefId, Block door, int ticksLeft) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player thief = Bukkit.getPlayer(thiefId);
            if (thief == null || !thief.isOnline()) {
                activePickers.remove(thiefId);
                return;
            }
            DoorRecord record = doorManager.getRecordFor(door);
            if (record == null || !record.isLocked()) {
                activePickers.remove(thiefId);
                Msg.error(thief, "The lock changed before you could finish.");
                return;
            }
            double maxDistance = plugin.getConfig().getDouble("lockpick.max-distance", 4.0);
            if (!thief.getWorld().equals(door.getWorld()) || thief.getLocation().distance(door.getLocation().add(0.5, 0.5, 0.5)) > maxDistance) {
                activePickers.remove(thiefId);
                Msg.error(thief, "You moved too far from the door.");
                return;
            }
            if (ticksLeft <= 0) {
                finishPick(thief, door, record);
                activePickers.remove(thiefId);
                return;
            }
            runChannelTick(thiefId, door, ticksLeft - 1);
        }, 20L);
    }

    private void finishPick(Player thief, Block door, DoorRecord record) {
        cooldowns.put(thief.getUniqueId(), System.currentTimeMillis());
        double chance = plugin.getConfig().getDouble("lockpick.success-chance", 0.5);
        if (ThreadLocalRandom.current().nextDouble() < chance) {
            doorManager.forceUnlock(door);
            Msg.success(thief, "You picked the lock!");
            if (record.getOwner() != null) {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(record.getOwner());
                if (owner.isOnline() && owner.getPlayer() != null) {
                    Msg.error(owner.getPlayer(), "One of your doors was just picked open!");
                }
            }
        } else {
            Msg.error(thief, "You failed to pick the lock.");
        }
    }
}
