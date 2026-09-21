package com.mcrp.darkrp.job;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.model.Job;
import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.storage.DataStore;
import com.mcrp.darkrp.util.ItemUtil;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads job definitions and handles job switching, whitelist/slot checks and salary payouts.
 */
public class JobManager {

    private final Plugin plugin;
    private final DataStore dataStore;
    private final EconomyManager economy;
    private final Map<String, Job> jobs = new LinkedHashMap<>();
    private final File file;
    private BukkitTask salaryTask;

    public JobManager(Plugin plugin, DataStore dataStore, EconomyManager economy) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "jobs.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("jobs.yml", false);
        }
        jobs.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("jobs");
        if (section == null) {
            plugin.getLogger().warning("jobs.yml has no 'jobs' section - no jobs loaded.");
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            Job job = new Job(
                    id,
                    s.getString("display-name", id),
                    s.getString("description", ""),
                    s.getDouble("salary", 0.0),
                    s.getInt("max-slots", -1),
                    s.getBoolean("police", false),
                    s.getBoolean("whitelisted", false),
                    s.getStringList("kit")
            );
            jobs.put(id.toLowerCase(), job);
        }
        plugin.getLogger().info("Loaded " + jobs.size() + " job(s).");
    }

    public Collection<Job> getJobs() {
        return jobs.values();
    }

    public Job getJob(String id) {
        if (id == null) {
            return null;
        }
        return jobs.get(id.toLowerCase());
    }

    public Job getDefaultJob() {
        Job job = getJob(economy.getDefaultJobId());
        return job != null ? job : jobs.values().stream().findFirst().orElse(null);
    }

    public Job getCurrentJob(PlayerRecord record) {
        Job job = getJob(record.getJobId());
        return job != null ? job : getDefaultJob();
    }

    public boolean isPolice(Player player) {
        PlayerRecord record = dataStore.get(player.getUniqueId());
        if (record == null) {
            return false;
        }
        Job job = getCurrentJob(record);
        return job != null && job.isPolice();
    }

    public boolean canActAsPolice(org.bukkit.command.CommandSender sender) {
        if (sender.hasPermission("darkrp.admin") || sender.hasPermission("darkrp.police")) {
            return true;
        }
        return sender instanceof Player player && isPolice(player);
    }

    public int countOnlineInJob(Job job) {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerRecord r = dataStore.get(p.getUniqueId());
            if (r != null && job.getId().equalsIgnoreCase(r.getJobId())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Attempts to switch a player's job, enforcing whitelist permission and slot limits.
     * Sends feedback messages to the player itself.
     */
    public boolean trySetJob(Player player, Job job) {
        if (job.isWhitelisted() && !player.hasPermission(job.whitelistPermission()) && !player.hasPermission("darkrp.admin")) {
            Msg.error(player, "You are not whitelisted for that job.");
            return false;
        }
        if (job.getMaxSlots() >= 0 && countOnlineInJob(job) >= job.getMaxSlots()) {
            PlayerRecord record = dataStore.get(player.getUniqueId());
            if (record == null || !job.getId().equalsIgnoreCase(record.getJobId())) {
                Msg.error(player, "That job is full right now.");
                return false;
            }
        }
        setJob(player, job);
        return true;
    }

    /** Applies the job without any permission/slot checks (used by admin commands). */
    public void setJob(Player player, Job job) {
        PlayerRecord record = economy.record(player);
        record.setJobId(job.getId());
        for (ItemStack item : ItemUtil.parseKit(plugin, job.getKit())) {
            player.getInventory().addItem(item.clone());
        }
        applyTabListName(player, job);
        Msg.success(player, "You are now a <white>" + job.getDisplayName() + "</white><green>.");
    }

    /** Prefixes the player's tab-list name with their job's colored display name. */
    public void applyTabListName(Player player, Job job) {
        if (job == null) {
            return;
        }
        player.playerListName(Msg.parse(job.getDisplayName() + " <white>" + player.getName() + "</white>"));
    }

    public void startSalaryTask() {
        stopSalaryTask();
        long intervalTicks = plugin.getConfig().getLong("salary.interval-seconds", 300) * 20L;
        salaryTask = Bukkit.getScheduler().runTaskTimer(plugin, this::paySalaries, intervalTicks, intervalTicks);
    }

    public void stopSalaryTask() {
        if (salaryTask != null) {
            salaryTask.cancel();
            salaryTask = null;
        }
    }

    private void paySalaries() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerRecord record = dataStore.get(player.getUniqueId());
            if (record == null || record.isJailed()) {
                continue;
            }
            Job job = getCurrentJob(record);
            if (job == null || job.getSalary() <= 0) {
                continue;
            }
            economy.deposit(player, job.getSalary());
            Msg.raw(player, "<gray>[Salary] <green>+" + economy.format(job.getSalary()) + "</green> as <white>" + job.getDisplayName() + "</white></gray>");
        }
    }
}
