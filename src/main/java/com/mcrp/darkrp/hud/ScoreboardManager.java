package com.mcrp.darkrp.hud;

import com.mcrp.darkrp.crime.WantedManager;
import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.model.Job;
import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.storage.DataStore;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Renders a live sidebar scoreboard (job, balance, wanted status) for every
 * online player. Players can hide it for themselves with /scoreboard.
 */
public class ScoreboardManager {

    private static final String OBJECTIVE_NAME = "darkrp";

    private final Plugin plugin;
    private final EconomyManager economy;
    private final JobManager jobManager;
    private final WantedManager wantedManager;
    private final DataStore dataStore;
    private final Set<UUID> hidden = new HashSet<>();
    private BukkitTask updateTask;

    public ScoreboardManager(Plugin plugin, EconomyManager economy, JobManager jobManager,
                              WantedManager wantedManager, DataStore dataStore) {
        this.plugin = plugin;
        this.economy = economy;
        this.jobManager = jobManager;
        this.wantedManager = wantedManager;
        this.dataStore = dataStore;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("scoreboard.enabled", true);
    }

    public boolean isHidden(Player player) {
        return hidden.contains(player.getUniqueId());
    }

    public void toggle(Player player) {
        if (hidden.remove(player.getUniqueId())) {
            update(player);
        } else {
            hidden.add(player.getUniqueId());
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void update(Player player) {
        if (!isEnabled() || isHidden(player)) {
            return;
        }
        PlayerRecord record = dataStore.get(player.getUniqueId());
        if (record == null) {
            return;
        }
        Job job = jobManager.getCurrentJob(record);

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY,
                ChatColor.GOLD + "" + ChatColor.BOLD + "DarkRP");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GRAY + "Job: " + ChatColor.WHITE + (job != null ? Msg.toLegacy(job.getDisplayName()) : "Citizen"));
        lines.add(ChatColor.GRAY + "Balance: " + ChatColor.GREEN + economy.format(record.getBalance()));
        lines.add(wantedManager.isWanted(player)
                ? ChatColor.RED + "" + ChatColor.BOLD + "WANTED"
                : ChatColor.GRAY + "Status: " + ChatColor.GREEN + "Clean");

        int score = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            objective.getScore(uniqueEntry(lines.get(i), i)).setScore(score--);
        }

        player.setScoreboard(board);
    }

    /** Pads a line with invisible color-reset codes so duplicate-looking lines still get unique scoreboard entries. */
    private String uniqueEntry(String line, int index) {
        StringBuilder padded = new StringBuilder(line);
        for (int i = 0; i < index; i++) {
            padded.append(ChatColor.RESET);
        }
        String entry = padded.toString();
        return entry.length() > 40 ? entry.substring(0, 40) : entry;
    }

    public void startUpdateTask() {
        stopUpdateTask();
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                update(player);
            }
        }, 20L, 40L);
    }

    public void stopUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
    }
}
