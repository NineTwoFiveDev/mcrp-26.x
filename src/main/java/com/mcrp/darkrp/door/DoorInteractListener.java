package com.mcrp.darkrp.door;

import com.mcrp.darkrp.crime.WarrantManager;
import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.model.DoorRecord;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Set;

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
            sendMenu(player, block);
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

    private void sendMenu(Player player, Block block) {
        Set<Block> group = doorManager.findGroup(block);
        if (group == null) {
            return;
        }
        DoorRecord record = doorManager.getRecord(group);

        Msg.raw(player, "<dark_gray>------- <gold>Door</gold> -------</dark_gray>");

        if (record == null || record.getOwner() == null) {
            double price = doorManager.priceFor(group);
            Msg.raw(player, "<gray>This door is unowned.</gray>");
            Msg.raw(player, "<click:run_command:'/door buy'><green>[Buy for " + economy.format(price) + "]</green></click>");
            return;
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(record.getOwner());
        String ownerName = owner.getName() != null ? owner.getName() : "Unknown";

        if (record.isOwnedBy(player.getUniqueId())) {
            boolean isPrimaryOwner = player.getUniqueId().equals(record.getOwner());
            Msg.raw(player, "<gray>You own this door.</gray> <dark_gray>(" + (record.isLocked() ? "<red>locked" : "<green>unlocked") + "</dark_gray>)");
            if (isPrimaryOwner) {
                Msg.raw(player, "<click:run_command:'/door sell'><red>[Sell]</red></click>  "
                        + "<click:run_command:'/door " + (record.isLocked() ? "unlock" : "lock") + "'><yellow>[" + (record.isLocked() ? "Unlock" : "Lock") + "]</yellow></click>");
                Msg.raw(player, "<gray>Use /door add <player>, /door kick <player> and /door price <amount> to manage co-owners and resale.</gray>");
            } else {
                Msg.raw(player, "<click:run_command:'/door " + (record.isLocked() ? "unlock" : "lock") + "'><yellow>[" + (record.isLocked() ? "Unlock" : "Lock") + "]</yellow></click>");
            }
        } else {
            Msg.raw(player, "<gray>Owned by <white>" + ownerName + "</white>.</gray> <dark_gray>(" + (record.isLocked() ? "<red>locked" : "<green>unlocked") + "</dark_gray>)");
            if (record.getForSalePrice() > 0) {
                Msg.raw(player, "<click:run_command:'/door buy'><green>[Buy for " + economy.format(record.getForSalePrice()) + "]</green></click>");
            }
        }
        Msg.raw(player, "<dark_gray>-------------------</dark_gray>");
    }
}
