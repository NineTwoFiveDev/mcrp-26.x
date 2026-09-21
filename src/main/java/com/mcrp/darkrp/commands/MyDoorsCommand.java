package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.door.DoorManager;
import com.mcrp.darkrp.model.DoorRecord;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class MyDoorsCommand implements CommandExecutor {

    private final DoorManager doorManager;

    public MyDoorsCommand(DoorManager doorManager) {
        this.doorManager = doorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.error(sender, "Only players can list their doors.");
            return true;
        }
        List<DoorRecord> owned = doorManager.getDoorsOwnedBy(player.getUniqueId());
        if (owned.isEmpty()) {
            Msg.raw(player, "<gray>You don't own any doors.</gray>");
            return true;
        }
        Msg.raw(player, "<dark_gray>---- <gold>Your Doors</gold> (" + owned.size() + ") ----</dark_gray>");
        for (DoorRecord record : owned) {
            boolean primary = player.getUniqueId().equals(record.getOwner());
            String where = record.getBlockKeys().iterator().next();
            String[] parts = where.split(":");
            String location = parts.length >= 4
                    ? parts[0] + " (" + parts[1] + ", " + parts[2] + ", " + parts[3] + ")"
                    : where;
            Msg.raw(player, "<white>" + location + "</white> <gray>-</gray> "
                    + (primary ? "<green>owner</green>" : "<yellow>co-owner</yellow>") + " <gray>-</gray> "
                    + (record.isLocked() ? "<red>locked" : "<green>unlocked") + "</gray>");
        }
        return true;
    }
}
