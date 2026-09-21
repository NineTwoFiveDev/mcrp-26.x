package com.mcrp.darkrp.crime;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.storage.DataStore;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Player-funded bounties: place cash on another player's head, collected by
 * whoever kills them in PvP or arrests them (see BountyDeathListener and
 * ArrestCommand).
 */
public class BountyManager {

    private final DataStore dataStore;
    private final EconomyManager economy;

    public BountyManager(DataStore dataStore, EconomyManager economy) {
        this.dataStore = dataStore;
        this.economy = economy;
    }

    public boolean placeBounty(Player placer, OfflinePlayer target, double amount) {
        if (amount <= 0) {
            Msg.error(placer, "The bounty must be a positive amount.");
            return false;
        }
        if (placer.getUniqueId().equals(target.getUniqueId())) {
            Msg.error(placer, "You can't place a bounty on yourself.");
            return false;
        }
        if (!economy.has(placer, amount)) {
            Msg.error(placer, "You don't have enough money.");
            return false;
        }
        economy.withdraw(placer, amount);
        PlayerRecord record = economy.record(target);
        record.setBounty(record.getBounty() + amount);
        Bukkit.broadcast(Msg.parse("<gold><bold>BOUNTY:</bold></gold> <white>" + placer.getName()
                + "</white> <gray>placed a</gray> <gold>" + economy.format(record.getBounty())
                + "</gold> <gray>bounty on</gray> <white>" + target.getName() + "</white>"));
        return true;
    }

    public double getBounty(OfflinePlayer target) {
        PlayerRecord record = dataStore.get(target.getUniqueId());
        return record != null ? record.getBounty() : 0.0;
    }

    /** Pays out and clears the target's bounty to the collector, if there is one. Returns the amount paid (0 if none). */
    public double payout(OfflinePlayer target, Player collector) {
        PlayerRecord record = dataStore.get(target.getUniqueId());
        if (record == null || record.getBounty() <= 0) {
            return 0.0;
        }
        double amount = record.getBounty();
        record.setBounty(0);
        economy.deposit(collector, amount);
        Bukkit.broadcast(Msg.parse("<gold>" + collector.getName() + " collected the " + economy.format(amount)
                + " bounty on " + target.getName() + "!</gold>"));
        return amount;
    }

    public List<PlayerRecord> topBounties(int limit) {
        return dataStore.all().values().stream()
                .filter(record -> record.getBounty() > 0)
                .sorted(Comparator.comparingDouble(PlayerRecord::getBounty).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
