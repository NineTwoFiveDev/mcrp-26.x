package com.mcrp.darkrp.economy;

import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.storage.DataStore;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Balance operations on top of the DataStore. Self-contained economy (no Vault dependency).
 */
public class EconomyManager {

    private final Plugin plugin;
    private final DataStore dataStore;
    private double startingBalance;
    private String currencySymbol;
    private String defaultJobId;

    public EconomyManager(Plugin plugin, DataStore dataStore) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        reload();
    }

    public void reload() {
        this.startingBalance = plugin.getConfig().getDouble("starting-balance", 500.0);
        this.currencySymbol = plugin.getConfig().getString("currency-symbol", "$");
        this.defaultJobId = plugin.getConfig().getString("default-job", "citizen");
    }

    public String getDefaultJobId() {
        return defaultJobId;
    }

    public PlayerRecord record(OfflinePlayer player) {
        return dataStore.getOrCreate(player.getUniqueId(), player.getName(), startingBalance, defaultJobId);
    }

    public PlayerRecord record(UUID uuid, String name) {
        return dataStore.getOrCreate(uuid, name, startingBalance, defaultJobId);
    }

    public double getBalance(OfflinePlayer player) {
        return record(player).getBalance();
    }

    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    public void deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return;
        }
        PlayerRecord r = record(player);
        r.setBalance(r.getBalance() + amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) {
            return true;
        }
        PlayerRecord r = record(player);
        if (r.getBalance() < amount) {
            return false;
        }
        r.setBalance(r.getBalance() - amount);
        return true;
    }

    public boolean transfer(OfflinePlayer from, OfflinePlayer to, double amount) {
        if (amount <= 0) {
            return false;
        }
        if (!withdraw(from, amount)) {
            return false;
        }
        deposit(to, amount);
        return true;
    }

    public String format(double amount) {
        return currencySymbol + String.format("%,.2f", amount);
    }

    /** The richest known players (online or offline), highest balance first. */
    public List<PlayerRecord> topBalances(int limit) {
        return dataStore.all().values().stream()
                .sorted(Comparator.comparingDouble(PlayerRecord::getBalance).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
