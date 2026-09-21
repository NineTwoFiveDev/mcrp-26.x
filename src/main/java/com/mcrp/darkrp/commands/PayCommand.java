package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.util.Completions;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final EconomyManager economy;

    public PayCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.error(sender, "Only players can pay other players.");
            return true;
        }
        if (args.length < 2) {
            Msg.error(sender, "Usage: /pay <player> <amount>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.error(sender, "That player is not online.");
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            Msg.error(sender, "You can't pay yourself.");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            Msg.error(sender, "Invalid amount.");
            return true;
        }
        if (amount <= 0) {
            Msg.error(sender, "Amount must be positive.");
            return true;
        }
        if (!economy.transfer(player, target, amount)) {
            Msg.error(sender, "You don't have enough money.");
            return true;
        }
        Msg.success(player, "You paid " + target.getName() + " " + economy.format(amount) + ".");
        Msg.success(target, player.getName() + " paid you " + economy.format(amount) + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Completions.onlinePlayerNames(args[0]);
        }
        return Collections.emptyList();
    }
}
