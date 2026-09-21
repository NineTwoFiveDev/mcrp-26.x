package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.crime.BountyManager;
import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.util.Completions;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BountyCommand implements CommandExecutor, TabCompleter {

    private static final int DEFAULT_LIMIT = 10;

    private final BountyManager bountyManager;
    private final EconomyManager economy;

    public BountyCommand(BountyManager bountyManager, EconomyManager economy) {
        this.bountyManager = bountyManager;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("list")) {
            handleList(sender);
            return true;
        }
        if (!(sender instanceof Player placer)) {
            Msg.error(sender, "Only players can place bounties.");
            return true;
        }
        if (args.length < 2) {
            Msg.error(sender, "Usage: /bounty <player> <amount>  or  /bounty list");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.error(sender, "That player is not online.");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            Msg.error(sender, "Invalid amount.");
            return true;
        }
        if (bountyManager.placeBounty(placer, target, amount)) {
            Msg.success(placer, "Placed a " + economy.format(amount) + " bounty on " + target.getName() + ".");
        }
        return true;
    }

    private void handleList(CommandSender sender) {
        List<PlayerRecord> top = bountyManager.topBounties(DEFAULT_LIMIT);
        if (top.isEmpty()) {
            Msg.raw(sender, "<gray>Nobody has a bounty right now.</gray>");
            return;
        }
        Msg.raw(sender, "<dark_gray>---- <gold>Bounties</gold> ----</dark_gray>");
        for (PlayerRecord record : top) {
            String name = record.getName() == null || record.getName().isBlank() ? "Unknown" : record.getName();
            Msg.raw(sender, "<white>" + name + "</white> <gray>-</gray> <gold>" + economy.format(record.getBounty()) + "</gold>");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(Completions.onlinePlayerNames(args[0]));
            options.addAll(Completions.filter(List.of("list"), args[0]));
            return options;
        }
        return Collections.emptyList();
    }
}
