package com.mcrp.darkrp.printer;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.model.Job;
import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.model.PrinterRecord;
import com.mcrp.darkrp.storage.DataStore;
import com.mcrp.darkrp.util.LocUtil;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Buyable money printers: a marker block placed in the world that periodically
 * pays its owner, with a chance of "busting" (being destroyed) each cycle.
 */
public class PrinterManager {

    private final Plugin plugin;
    private final EconomyManager economy;
    private final JobManager jobManager;
    private final DataStore dataStore;
    private final File file;
    private final Map<String, PrinterRecord> printers = new ConcurrentHashMap<>();

    private double price;
    private Material markerMaterial;
    private double payoutPerCycle;
    private double bustChance;
    private int maxPerPlayer;
    private int resalePercentage;
    private List<String> allowedJobs;

    private BukkitTask tickTask;

    public PrinterManager(Plugin plugin, EconomyManager economy, JobManager jobManager, DataStore dataStore) {
        this.plugin = plugin;
        this.economy = economy;
        this.jobManager = jobManager;
        this.dataStore = dataStore;
        this.file = new File(plugin.getDataFolder(), "printers.yml");
        reloadConfig();
    }

    public void reloadConfig() {
        this.price = plugin.getConfig().getDouble("printer.price", 750.0);
        Material mat = Material.matchMaterial(plugin.getConfig().getString("printer.marker-material", "IRON_BLOCK"));
        this.markerMaterial = mat != null ? mat : Material.IRON_BLOCK;
        this.payoutPerCycle = plugin.getConfig().getDouble("printer.payout-per-cycle", 35.0);
        this.bustChance = plugin.getConfig().getDouble("printer.bust-chance", 0.05);
        this.maxPerPlayer = plugin.getConfig().getInt("printer.max-per-player", 3);
        this.resalePercentage = plugin.getConfig().getInt("printer.resale-percentage", 40);
        this.allowedJobs = plugin.getConfig().getStringList("printer.allowed-jobs");
    }

    public double getPrice() {
        return price;
    }

    public Material getMarkerMaterial() {
        return markerMaterial;
    }

    // ----- persistence -----

    public void load() {
        printers.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        var section = yaml.getConfigurationSection("printers");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String path = key + ".";
            String ownerStr = section.getString(path + "owner", "");
            if (ownerStr == null || ownerStr.isBlank()) {
                continue;
            }
            UUID owner = UUID.fromString(ownerStr);
            double recordPrice = section.getDouble(path + "price", price);
            printers.put(key, new PrinterRecord(key, owner, recordPrice));
        }
        plugin.getLogger().info("Loaded " + printers.size() + " money printer(s).");
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PrinterRecord record : printers.values()) {
            String path = "printers." + record.getBlockKey() + ".";
            yaml.set(path + "owner", record.getOwner().toString());
            yaml.set(path + "price", record.getPrice());
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save printers.yml", e);
        }
    }

    // ----- lookups -----

    public boolean isPrinter(Block block) {
        return block != null && block.getType() == markerMaterial && printers.containsKey(LocUtil.blockKey(block));
    }

    public PrinterRecord getRecord(Block block) {
        return block == null ? null : printers.get(LocUtil.blockKey(block));
    }

    public int countOwnedBy(UUID uuid) {
        int count = 0;
        for (PrinterRecord record : printers.values()) {
            if (record.getOwner().equals(uuid)) {
                count++;
            }
        }
        return count;
    }

    public boolean canBuy(Player player) {
        if (allowedJobs == null || allowedJobs.isEmpty()) {
            return true;
        }
        PlayerRecord record = dataStore.get(player.getUniqueId());
        Job job = record != null ? jobManager.getCurrentJob(record) : null;
        return job != null && allowedJobs.stream().anyMatch(id -> id.equalsIgnoreCase(job.getId()));
    }

    // ----- actions -----

    public enum Result {
        SUCCESS, NOT_A_SPOT, ALREADY_A_PRINTER, LIMIT_REACHED, WRONG_JOB, INSUFFICIENT_FUNDS,
        NOT_A_PRINTER, NOT_THE_OWNER
    }

    public Result buy(Player player, Block placementSpot) {
        if (placementSpot == null || placementSpot.getType() != Material.AIR) {
            return Result.NOT_A_SPOT;
        }
        if (isPrinter(placementSpot)) {
            return Result.ALREADY_A_PRINTER;
        }
        if (!canBuy(player)) {
            return Result.WRONG_JOB;
        }
        if (countOwnedBy(player.getUniqueId()) >= maxPerPlayer) {
            return Result.LIMIT_REACHED;
        }
        if (!economy.has(player, price)) {
            return Result.INSUFFICIENT_FUNDS;
        }
        economy.withdraw(player, price);
        placementSpot.setType(markerMaterial);
        String key = LocUtil.blockKey(placementSpot);
        printers.put(key, new PrinterRecord(key, player.getUniqueId(), price));
        save();
        return Result.SUCCESS;
    }

    public Result sell(Player player, Block block) {
        PrinterRecord record = getRecord(block);
        if (record == null) {
            return Result.NOT_A_PRINTER;
        }
        if (!record.getOwner().equals(player.getUniqueId())) {
            return Result.NOT_THE_OWNER;
        }
        double refund = record.getPrice() * (resalePercentage / 100.0);
        economy.deposit(player, refund);
        removePrinter(block, record);
        save();
        return Result.SUCCESS;
    }

    public Result confiscate(Player police, Block block) {
        PrinterRecord record = getRecord(block);
        if (record == null) {
            return Result.NOT_A_PRINTER;
        }
        removePrinter(block, record);
        save();
        OfflinePlayer owner = Bukkit.getOfflinePlayer(record.getOwner());
        if (owner.isOnline() && owner.getPlayer() != null) {
            Msg.error(owner.getPlayer(), "Your money printer was confiscated by " + police.getName() + ".");
        }
        return Result.SUCCESS;
    }

    private void removePrinter(Block block, PrinterRecord record) {
        if (block.getType() == markerMaterial) {
            block.setType(Material.AIR);
        }
        printers.remove(record.getBlockKey());
    }

    // ----- income tick -----

    public void startTickTask() {
        stopTickTask();
        long intervalTicks = plugin.getConfig().getLong("printer.interval-seconds", 60) * 20L;
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, intervalTicks, intervalTicks);
    }

    public void stopTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    private void tick() {
        boolean changed = false;
        for (PrinterRecord record : List.copyOf(printers.values())) {
            String[] p = record.getBlockKey().split(":");
            World world = Bukkit.getWorld(p[0]);
            if (world == null) {
                continue;
            }
            int x = Integer.parseInt(p[1]);
            int z = Integer.parseInt(p[3]);
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            Block block = LocUtil.blockFromKey(record.getBlockKey());
            OfflinePlayer owner = Bukkit.getOfflinePlayer(record.getOwner());
            if (ThreadLocalRandom.current().nextDouble() < bustChance) {
                block.getWorld().playSound(block.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                removePrinter(block, record);
                changed = true;
                if (owner.isOnline() && owner.getPlayer() != null) {
                    Msg.error(owner.getPlayer(), "Your money printer overheated and was destroyed!");
                }
            } else {
                economy.deposit(owner, payoutPerCycle);
                if (owner.isOnline() && owner.getPlayer() != null) {
                    block.getWorld().playSound(block.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 0.6f, 1.4f);
                }
            }
        }
        if (changed) {
            save();
        }
    }
}
