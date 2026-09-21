package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MoneyCommand implements CommandExecutor {

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
}
