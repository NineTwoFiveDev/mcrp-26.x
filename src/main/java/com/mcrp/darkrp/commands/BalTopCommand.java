package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class BalTopCommand implements CommandExecutor {

    private static final int DEFAULT_LIMIT = 10;

    private final EconomyManager economy;

    public BalTopCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        int limit = DEFAULT_LIMIT;
        if (args.length > 0) {
            try {
                limit = Math.max(1, Math.min(25, Integer.parseInt(args[0])));
            } catch (NumberFormatException ignored) {
                // fall back to the default
            }
        }

        List<PlayerRecord> top = economy.topBalances(limit);
        if (top.isEmpty()) {
            Msg.raw(sender, "<gray>Nobody has any recorded balance yet.</gray>");
            return true;
        }
        Msg.raw(sender, "<dark_gray>---- <gold>Richest Players</gold> ----</dark_gray>");
        int rank = 1;
        for (PlayerRecord record : top) {
            String name = record.getName() == null || record.getName().isBlank() ? "Unknown" : record.getName();
            Msg.raw(sender, "<yellow>" + rank + ".</yellow> <white>" + name + "</white> <gray>-</gray> <green>"
                    + economy.format(record.getBalance()) + "</green>");
            rank++;
        }
        return true;
    }
}
