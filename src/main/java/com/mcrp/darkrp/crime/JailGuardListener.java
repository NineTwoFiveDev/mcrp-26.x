package com.mcrp.darkrp.crime;

import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.storage.DataStore;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Prevents jailed players from escaping via ender pearls or other teleports
 * (commands, other plugins) while a jail sentence is active.
 */
public class JailGuardListener implements Listener {

    private final DataStore dataStore;
    private final JailManager jailManager;

    public JailGuardListener(DataStore dataStore, JailManager jailManager) {
        this.dataStore = dataStore;
        this.jailManager = jailManager;
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (jailManager.isInternalTeleport(player)) {
            return;
        }
        PlayerRecord record = dataStore.get(player.getUniqueId());
        if (record != null && record.isJailed()) {
            event.setCancelled(true);
            Msg.error(player, "You can't teleport while jailed.");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getItem() == null || event.getItem().getType() != Material.ENDER_PEARL) {
            return;
        }
        Player player = event.getPlayer();
        PlayerRecord record = dataStore.get(player.getUniqueId());
        if (record != null && record.isJailed()) {
            event.setCancelled(true);
            Msg.error(player, "You can't use ender pearls while jailed.");
        }
    }
}
