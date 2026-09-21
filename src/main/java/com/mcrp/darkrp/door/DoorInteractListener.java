package com.mcrp.darkrp.door;

import com.mcrp.darkrp.crime.WarrantManager;
import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.model.DoorRecord;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class DoorInteractListener implements Listener {

    private final DoorManager doorManager;
    private final EconomyManager economy;
    private final JobManager jobManager;
    private final WarrantManager warrantManager;

    public DoorInteractListener(DoorManager doorManager, EconomyManager economy, JobManager jobManager, WarrantManager warrantManager) {
        this.doorManager = doorManager;
        this.economy = economy;
        this.jobManager = jobManager;
        this.warrantManager = warrantManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !doorManager.isDoor(block)) {
            return;
        }
        Player player = event.getPlayer();

        if (player.isSneaking()) {
            event.setCancelled(true);
            DoorMenu.send(player, doorManager, economy, block);
            return;
        }

        DoorRecord record = doorManager.getRecordFor(block);
        boolean locked = record != null && record.isLocked() && !record.isOwnedBy(player.getUniqueId());
        if (!locked) {
            return;
        }
        boolean warrantBypass = record.getOwner() != null
                && warrantManager.hasWarrant(record.getOwner())
                && jobManager.canActAsPolice(player);
        if (warrantBypass) {
            Msg.raw(player, "<yellow>Bypassing lock under an active warrant.</yellow>");
            return;
        }
        event.setCancelled(true);
        Msg.error(player, "This door is locked.");
    }
}
