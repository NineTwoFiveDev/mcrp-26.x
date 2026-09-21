package com.mcrp.darkrp.job;

import com.mcrp.darkrp.model.Job;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class JobMenuListener implements Listener {

    private final JobManager jobManager;

    public JobMenuListener(JobManager jobManager) {
        this.jobManager = jobManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof JobMenuHolder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        String jobId = meta.getPersistentDataContainer().get(JobMenu.JOB_ID_KEY, PersistentDataType.STRING);
        if (jobId == null) {
            return;
        }
        Job job = jobManager.getJob(jobId);
        if (job == null) {
            return;
        }
        jobManager.trySetJob(player, job);
        player.closeInventory();
    }
}
