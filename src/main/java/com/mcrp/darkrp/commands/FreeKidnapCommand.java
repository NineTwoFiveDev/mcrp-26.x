package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.crime.KidnapManager;
import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FreeKidnapCommand implements CommandExecutor {

    private final KidnapManager kidnapManager;
    private final JobManager jobManager;

    public FreeKidnapCommand(KidnapManager kidnapManager, JobManager jobManager) {
        this.kidnapManager = kidnapManager;
        this.jobManager = jobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!jobManager.canActAsPolice(sender)) {
            Msg.error(sender, "Only police can free a kidnapped player without paying.");
            return true;
        }
        if (args.length < 1) {
            Msg.error(sender, "Usage: /freekidnap <player>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.error(sender, "That player is not online.");
            return true;
        }
        if (!kidnapManager.freeKidnap(target)) {
            Msg.error(sender, target.getName() + " isn't kidnapped.");
            return true;
        }
        Msg.success(sender, "You freed " + target.getName() + ".");
        return true;
    }
}
