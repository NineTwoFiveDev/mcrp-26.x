package com.mcrp.darkrp.listeners;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.model.Job;
import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final EconomyManager economy;
    private final JobManager jobManager;

    public PlayerJoinListener(EconomyManager economy, JobManager jobManager) {
        this.economy = economy;
        this.jobManager = jobManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerRecord record = economy.record(event.getPlayer());
        record.setName(event.getPlayer().getName());
        if (record.getJobId() == null || jobManager.getJob(record.getJobId()) == null) {
            Job def = jobManager.getDefaultJob();
            if (def != null) {
                record.setJobId(def.getId());
            }
        }
        Job job = jobManager.getCurrentJob(record);
        jobManager.applyTabListName(event.getPlayer(), job);
        Msg.success(event.getPlayer(), "Welcome back! You are a <white>" + (job != null ? job.getDisplayName() : "Citizen")
                + "</white><green> with " + economy.format(record.getBalance()) + "<green>.");
    }
}
