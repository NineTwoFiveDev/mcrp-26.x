package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.model.Job;
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
import java.util.stream.Collectors;

public class SetJobCommand implements CommandExecutor, TabCompleter {

    private final JobManager jobManager;

    public SetJobCommand(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            Msg.error(sender, "Usage: /setjob <player> <jobId>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.error(sender, "That player is not online.");
            return true;
        }
        Job job = jobManager.getJob(args[1]);
        if (job == null) {
            List<String> ids = jobManager.getJobs().stream().map(Job::getId).collect(Collectors.toList());
            Msg.error(sender, "Unknown job. Available: " + String.join(", ", ids));
            return true;
        }
        jobManager.setJob(target, job);
        Msg.success(sender, "Set " + target.getName() + "'s job to " + job.getId() + ".");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Completions.onlinePlayerNames(args[0]);
        }
        if (args.length == 2) {
            List<String> ids = jobManager.getJobs().stream().map(Job::getId).collect(Collectors.toList());
            return Completions.filter(ids, args[1]);
        }
        return Collections.emptyList();
    }
}
