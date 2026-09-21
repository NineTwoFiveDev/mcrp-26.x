package com.mcrp.darkrp.printer;

import com.mcrp.darkrp.model.PrinterRecord;
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

public class PrinterInteractListener implements Listener {

    private final PrinterManager printerManager;

    public PrinterInteractListener(PrinterManager printerManager) {
        this.printerManager = printerManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !printerManager.isPrinter(block) || !event.getPlayer().isSneaking()) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        PrinterRecord record = printerManager.getRecord(block);
        if (record == null) {
            return;
        }
        OfflinePlayer owner = Bukkit.getOfflinePlayer(record.getOwner());
        Msg.raw(player, "<dark_gray>---- <gold>Money Printer</gold> ----</dark_gray>");
        Msg.raw(player, "<gray>Owner: <white>" + (owner.getName() != null ? owner.getName() : "Unknown") + "</white></gray>");
        if (record.getOwner().equals(player.getUniqueId())) {
            Msg.raw(player, "<click:run_command:'/printer sell'><red>[Sell]</red></click>");
        }
        Msg.raw(player, "<dark_gray>-----------------------</dark_gray>");
    }
}
