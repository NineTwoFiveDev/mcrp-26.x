package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.crime.WarrantManager;
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

public class WarrantCommand implements CommandExecutor, TabCompleter {

    private final WarrantManager warrantManager;
    private final JobManager jobManager;

    public WarrantCommand(WarrantManager warrantManager, JobManager jobManager) {
        this.warrantManager = warrantManager;
        this.jobManager = jobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!jobManager.canActAsPolice(sender)) {
            Msg.error(sender, "Only police can issue warrants.");
            return true;
        }
        if (args.length < 1) {
            Msg.error(sender, "Usage: /warrant <player> [seconds]");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.error(sender, "That player is not online.");
            return true;
        }
        int seconds = warrantManager.defaultDurationSeconds();
        if (args.length > 1) {
            try {
                seconds = Math.max(30, Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                Msg.error(sender, "Invalid seconds value, using default.");
            }
        }
        warrantManager.issue(target, seconds);
        Msg.success(sender, "Issued a " + seconds + "s warrant for " + target.getName()
                + ". Police can now bypass their locked doors.");
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
