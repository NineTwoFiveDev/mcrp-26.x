package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.crime.JailManager;
import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.storage.DataStore;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReleaseCommand implements CommandExecutor {

    private final JailManager jailManager;
    private final JobManager jobManager;
    private final DataStore dataStore;

    public ReleaseCommand(JailManager jailManager, JobManager jobManager, DataStore dataStore) {
        this.jailManager = jailManager;
        this.jobManager = jobManager;
        this.dataStore = dataStore;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!jobManager.canActAsPolice(sender)) {
            Msg.error(sender, "Only police can release players from jail.");
            return true;
        }
        if (args.length < 1) {
            Msg.error(sender, "Usage: /release <player>");
            return true;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            Msg.error(sender, "That player is not online.");
            return true;
        }
        var record = dataStore.get(target.getUniqueId());
        if (record == null || !record.isJailed()) {
            Msg.error(sender, target.getName() + " isn't jailed.");
            return true;
        }
        jailManager.release(target);
        Msg.success(sender, "You released " + target.getName() + " from jail.");
        return true;
    }
}
