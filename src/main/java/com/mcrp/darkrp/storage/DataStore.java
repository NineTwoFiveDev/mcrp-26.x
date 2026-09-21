package com.mcrp.darkrp.storage;

import com.mcrp.darkrp.model.PlayerRecord;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * In-memory cache of {@link PlayerRecord}s, backed by a single players.yml file.
 */
public class DataStore {

    private final Plugin plugin;
    private final File file;
    private final Map<UUID, PlayerRecord> players = new ConcurrentHashMap<>();

    public DataStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
    }

    public void load() {
        players.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }
            var path = "players." + key + ".";
            String name = yaml.getString(path + "name", "");
            double balance = yaml.getDouble(path + "balance", 0.0);
            String jobId = yaml.getString(path + "job", null);
            PlayerRecord record = new PlayerRecord(uuid, name, balance, jobId);
            record.setJailedUntilMillis(yaml.getLong(path + "jailed-until", 0));
            record.setPreJailLocation(yaml.getString(path + "pre-jail-location", null));
            record.setWanted(yaml.getBoolean(path + "wanted", false));
            record.setWantedReason(yaml.getString(path + "wanted-reason", ""));
            record.setWantedExpiryMillis(yaml.getLong(path + "wanted-expiry", 0));
            record.setBounty(yaml.getDouble(path + "bounty", 0.0));
            players.put(uuid, record);
        }
        plugin.getLogger().info("Loaded " + players.size() + " player record(s).");
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PlayerRecord record : players.values()) {
            String path = "players." + record.getUuid() + ".";
            yaml.set(path + "name", record.getName());
            yaml.set(path + "balance", record.getBalance());
            yaml.set(path + "job", record.getJobId());
            yaml.set(path + "jailed-until", record.getJailedUntilMillis());
            yaml.set(path + "pre-jail-location", record.getPreJailLocation());
            yaml.set(path + "wanted", record.isWanted());
            yaml.set(path + "wanted-reason", record.getWantedReason());
            yaml.set(path + "wanted-expiry", record.getWantedExpiryMillis());
            yaml.set(path + "bounty", record.getBounty());
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save players.yml", e);
        }
    }

    public PlayerRecord getOrCreate(UUID uuid, String name, double startingBalance, String defaultJobId) {
        return players.computeIfAbsent(uuid, id -> new PlayerRecord(id, name, startingBalance, defaultJobId));
    }

    public PlayerRecord get(UUID uuid) {
        return players.get(uuid);
    }

    public Map<UUID, PlayerRecord> all() {
        return players;
    }
}
