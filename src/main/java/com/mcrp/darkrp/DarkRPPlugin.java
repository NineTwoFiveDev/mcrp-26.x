package com.mcrp.darkrp;

import com.mcrp.darkrp.commands.ArrestCommand;
import com.mcrp.darkrp.commands.DarkRPAdminCommand;
import com.mcrp.darkrp.commands.DoorCommand;
import com.mcrp.darkrp.commands.JobsCommand;
import com.mcrp.darkrp.commands.MoneyCommand;
import com.mcrp.darkrp.commands.MugCommand;
import com.mcrp.darkrp.commands.PayCommand;
import com.mcrp.darkrp.commands.ReleaseCommand;
import com.mcrp.darkrp.commands.SetJobCommand;
import com.mcrp.darkrp.commands.UnwantedCommand;
import com.mcrp.darkrp.commands.WantedCommand;
import com.mcrp.darkrp.crime.JailGuardListener;
import com.mcrp.darkrp.crime.JailManager;
import com.mcrp.darkrp.crime.MugManager;
import com.mcrp.darkrp.crime.WantedManager;
import com.mcrp.darkrp.door.DoorInteractListener;
import com.mcrp.darkrp.door.DoorManager;
import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.job.JobMenu;
import com.mcrp.darkrp.job.JobMenuListener;
import com.mcrp.darkrp.listeners.PlayerJoinListener;
import com.mcrp.darkrp.listeners.PlayerQuitListener;
import com.mcrp.darkrp.storage.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class DarkRPPlugin extends JavaPlugin {

    private DataStore dataStore;
    private EconomyManager economyManager;
    private JobManager jobManager;
    private DoorManager doorManager;
    private WantedManager wantedManager;
    private JailManager jailManager;
    private MugManager mugManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dataStore = new DataStore(this);
        dataStore.load();

        economyManager = new EconomyManager(this, dataStore);

        jobManager = new JobManager(this, dataStore, economyManager);
        jobManager.load();

        doorManager = new DoorManager(this, economyManager);
        doorManager.load();

        wantedManager = new WantedManager(this, dataStore);
        jailManager = new JailManager(this, dataStore);
        mugManager = new MugManager(this, dataStore, economyManager, jobManager);

        JobMenu jobMenu = new JobMenu(this, jobManager, dataStore);

        registerCommands(jobMenu);
        registerListeners();

        jobManager.startSalaryTask();
        wantedManager.startExpiryTask();
        jailManager.startTickTask();

        long autosaveTicks = 5L * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            dataStore.save();
            doorManager.save();
        }, autosaveTicks, autosaveTicks);

        getLogger().info("DarkRP enabled.");
    }

    @Override
    public void onDisable() {
        if (jobManager != null) {
            jobManager.stopSalaryTask();
        }
        if (wantedManager != null) {
            wantedManager.stopExpiryTask();
        }
        if (jailManager != null) {
            jailManager.stopTickTask();
        }
        if (dataStore != null) {
            dataStore.save();
        }
        if (doorManager != null) {
            doorManager.save();
        }
        getLogger().info("DarkRP disabled.");
    }

    private void registerCommands(JobMenu jobMenu) {
        getCommand("money").setExecutor(new MoneyCommand(economyManager));
        getCommand("pay").setExecutor(new PayCommand(economyManager));
        getCommand("jobs").setExecutor(new JobsCommand(jobMenu));
        getCommand("setjob").setExecutor(new SetJobCommand(jobManager));
        getCommand("door").setExecutor(new DoorCommand(doorManager, economyManager));
        getCommand("wanted").setExecutor(new WantedCommand(wantedManager, jobManager));
        getCommand("unwanted").setExecutor(new UnwantedCommand(wantedManager, jobManager));
        getCommand("arrest").setExecutor(new ArrestCommand(jailManager, jobManager));
        getCommand("release").setExecutor(new ReleaseCommand(jailManager, jobManager, dataStore));
        getCommand("mug").setExecutor(new MugCommand(mugManager));
        getCommand("darkrp").setExecutor(new DarkRPAdminCommand(this, economyManager, jobManager, doorManager));
    }

    private void registerListeners() {
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerJoinListener(economyManager, jobManager), this);
        pm.registerEvents(new PlayerQuitListener(dataStore), this);
        pm.registerEvents(new JobMenuListener(jobManager), this);
        pm.registerEvents(new DoorInteractListener(doorManager, economyManager), this);
        pm.registerEvents(new JailGuardListener(dataStore, jailManager), this);
    }
}
