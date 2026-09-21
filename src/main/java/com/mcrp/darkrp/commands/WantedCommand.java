package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.crime.WantedManager;
import com.mcrp.darkrp.job.JobManager;
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

public class WantedCommand implements CommandExecutor, TabCompleter {

    private final WantedManager wantedManager;
    private final JobManager jobManager;

    public WantedCommand(WantedManager wantedManager, JobManager jobManager) {
        this.wantedManager = wantedManager;
        this.jobManager = jobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!jobManager.canActAsPolice(sender)) {
            Msg.error(sender, "Only police can mark players as wanted.");
            return true;
        }
        if (args.length < 1) {
            Msg.error(sender, "Usage: /wanted <player> [reason]");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.error(sender, "That player is not online.");
            return true;
        }
        String reason = args.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)) : null;
        wantedManager.setWanted(target, reason, wantedManager.defaultDurationSeconds());
        Msg.success(sender, target.getName() + " has been marked as wanted.");
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
