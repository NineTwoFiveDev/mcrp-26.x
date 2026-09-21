package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.util.Completions;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class MoneyCommand implements CommandExecutor, TabCompleter {

    private final EconomyManager economy;

    public MoneyCommand(EconomyManager economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                Msg.error(sender, "Console must specify a player: /money <player>");
                return true;
            }
            Msg.success(player, "Your balance: " + economy.format(economy.getBalance(player)));
            return true;
        }

        if (!sender.hasPermission("darkrp.admin")) {
            Msg.error(sender, "You don't have permission to check other players' balances.");
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            Msg.error(sender, "That player has never joined this server.");
            return true;
        }
        Msg.success(sender, target.getName() + "'s balance: " + economy.format(economy.getBalance(target)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("darkrp.admin")) {
            return Completions.onlinePlayerNames(args[0]);
        }
        return Collections.emptyList();
    }
}
