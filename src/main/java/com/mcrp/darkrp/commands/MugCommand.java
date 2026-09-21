package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.crime.MugManager;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MugCommand implements CommandExecutor {

    private final MugManager mugManager;

    public MugCommand(MugManager mugManager) {
        this.mugManager = mugManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player mugger)) {
            Msg.error(sender, "Only players can mug other players.");
            return true;
        }
        if (args.length < 1) {
            Msg.error(sender, "Usage: /mug <player>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.error(sender, "That player is not online.");
            return true;
        }
        mugManager.attemptMug(mugger, target);
        return true;
    }
}
