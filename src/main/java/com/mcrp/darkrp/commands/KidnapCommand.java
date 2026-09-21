package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.crime.KidnapManager;
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

public class KidnapCommand implements CommandExecutor, TabCompleter {

    private final KidnapManager kidnapManager;

    public KidnapCommand(KidnapManager kidnapManager) {
        this.kidnapManager = kidnapManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player kidnapper)) {
            Msg.error(sender, "Only players can kidnap other players.");
            return true;
        }
        if (args.length < 2) {
            Msg.error(sender, "Usage: /kidnap <player> <ransom>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.error(sender, "That player is not online.");
            return true;
        }
        double ransom;
        try {
            ransom = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            Msg.error(sender, "Invalid ransom amount.");
            return true;
        }
        kidnapManager.attemptKidnap(kidnapper, target, ransom);
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
