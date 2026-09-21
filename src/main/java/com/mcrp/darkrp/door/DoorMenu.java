package com.mcrp.darkrp.door;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.model.DoorRecord;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * Renders the door buy/sell/lock chat menu, shared by the sneak-right-click
 * interaction and the explicit "/door info" command.
 */
public final class DoorMenu {

    private DoorMenu() {
    }

    public static void send(Player player, DoorManager doorManager, EconomyManager economy, Block block) {
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
