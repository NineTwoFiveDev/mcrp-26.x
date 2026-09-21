package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.job.JobMenu;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class JobsCommand implements CommandExecutor {

    private final JobMenu jobMenu;

    public JobsCommand(JobMenu jobMenu) {
        this.jobMenu = jobMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.error(sender, "Only players can open the job menu.");
            return true;
        }
        jobMenu.open(player);
        return true;
    }
}
