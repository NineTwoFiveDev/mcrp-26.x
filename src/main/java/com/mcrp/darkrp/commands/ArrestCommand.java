package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.crime.JailManager;
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

public class ArrestCommand implements CommandExecutor, TabCompleter {

    private static final double MAX_ARREST_DISTANCE = 6.0;

    private final JailManager jailManager;
    private final JobManager jobManager;

    public ArrestCommand(JailManager jailManager, JobManager jobManager) {
        this.jailManager = jailManager;
        this.jobManager = jobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!jobManager.canActAsPolice(sender)) {
            Msg.error(sender, "Only police can arrest players.");
            return true;
        }
        if (args.length < 1) {
            Msg.error(sender, "Usage: /arrest <player> [seconds]");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.error(sender, "That player is not online.");
            return true;
        }
        if (sender instanceof Player police) {
            if (police.getUniqueId().equals(target.getUniqueId())) {
                Msg.error(sender, "You can't arrest yourself.");
                return true;
            }
            if (!police.getWorld().equals(target.getWorld()) || police.getLocation().distance(target.getLocation()) > MAX_ARREST_DISTANCE) {
                Msg.error(sender, target.getName() + " is too far away to arrest.");
                return true;
            }
        }
        int seconds = jailManager.defaultTimeSeconds();
        if (args.length > 1) {
            try {
                seconds = Math.max(5, Integer.parseInt(args[1]));
            } catch (NumberFormatException e) {
                Msg.error(sender, "Invalid seconds value, using default.");
            }
        }
        jailManager.jail(target, seconds);
        Msg.success(sender, "You arrested " + target.getName() + " for " + seconds + " seconds.");
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
