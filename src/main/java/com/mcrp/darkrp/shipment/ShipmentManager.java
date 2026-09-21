package com.mcrp.darkrp.shipment;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.model.Job;
import com.mcrp.darkrp.model.PlayerRecord;
import com.mcrp.darkrp.model.ShipmentType;
import com.mcrp.darkrp.storage.DataStore;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Buyable weapon/item shipments: a chest placed in the world and filled with
 * a configured item stack, gated by job where configured.
 */
public class ShipmentManager {

    public static final NamespacedKey OWNER_KEY = new NamespacedKey("darkrp", "shipment_owner");
    public static final NamespacedKey TYPE_KEY = new NamespacedKey("darkrp", "shipment_type");

    private final Plugin plugin;
    private final EconomyManager economy;
    private final JobManager jobManager;
    private final DataStore dataStore;
    private final File file;
    private final Map<String, ShipmentType> types = new LinkedHashMap<>();

    public ShipmentManager(Plugin plugin, EconomyManager economy, JobManager jobManager, DataStore dataStore) {
        this.plugin = plugin;
        this.economy = economy;
        this.jobManager = jobManager;
        this.dataStore = dataStore;
        this.file = new File(plugin.getDataFolder(), "shipments.yml");
    }

    public void load() {
        if (!file.exists()) {
            plugin.saveResource("shipments.yml", false);
        }
        types.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("shipments");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            Material material = Material.matchMaterial(s.getString("item", "STONE"));
            if (material == null) {
                plugin.getLogger().warning("Shipment '" + id + "' has an invalid item material, skipping.");
                continue;
            }
            ShipmentType type = new ShipmentType(
                    id,
                    s.getString("display-name", id),
                    material,
                    Math.max(1, s.getInt("amount", 1)),
                    s.getDouble("price", 0.0),
                    s.getStringList("allowed-jobs")
            );
            types.put(id.toLowerCase(), type);
        }
        plugin.getLogger().info("Loaded " + types.size() + " shipment type(s).");
    }

    public Collection<ShipmentType> getTypes() {
        return types.values();
    }

    public ShipmentType getType(String id) {
        return id == null ? null : types.get(id.toLowerCase());
    }

    public boolean canBuy(Player player, ShipmentType type) {
        if (type.getAllowedJobs() == null || type.getAllowedJobs().isEmpty()) {
            return true;
        }
        PlayerRecord record = dataStore.get(player.getUniqueId());
        Job job = record != null ? jobManager.getCurrentJob(record) : null;
        return job != null && type.getAllowedJobs().stream().anyMatch(id -> id.equalsIgnoreCase(job.getId()));
    }

    public enum Result {
        SUCCESS, UNKNOWN_TYPE, NOT_A_SPOT, WRONG_JOB, INSUFFICIENT_FUNDS, NOT_A_SHIPMENT
    }

    public Result buy(Player player, Block placementSpot, String typeId) {
        ShipmentType type = getType(typeId);
        if (type == null) {
            return Result.UNKNOWN_TYPE;
        }
        if (placementSpot == null || placementSpot.getType() != Material.AIR) {
            return Result.NOT_A_SPOT;
        }
        if (!canBuy(player, type)) {
            return Result.WRONG_JOB;
        }
        if (!economy.has(player, type.getPrice())) {
            return Result.INSUFFICIENT_FUNDS;
        }
        economy.withdraw(player, type.getPrice());

        placementSpot.setType(Material.CHEST);
        if (placementSpot.getState() instanceof Chest chest) {
            chest.getInventory().addItem(new ItemStack(type.getItem(), type.getAmount()));
            PersistentDataContainer pdc = chest.getPersistentDataContainer();
            pdc.set(OWNER_KEY, PersistentDataType.STRING, player.getUniqueId().toString());
            pdc.set(TYPE_KEY, PersistentDataType.STRING, type.getId());
            chest.update();
        }
        return Result.SUCCESS;
    }

    public boolean isShipment(Block block) {
        return block != null && block.getState() instanceof Chest chest
                && chest.getPersistentDataContainer().has(OWNER_KEY, PersistentDataType.STRING);
    }

    public Result confiscate(Player police, Block block) {
        if (!isShipment(block)) {
            return Result.NOT_A_SHIPMENT;
        }
        Chest chest = (Chest) block.getState();
        String ownerStr = chest.getPersistentDataContainer().get(OWNER_KEY, PersistentDataType.STRING);
        chest.getInventory().clear();
        block.setType(Material.AIR);
        if (ownerStr != null) {
            OfflinePlayer owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerStr));
            if (owner.isOnline() && owner.getPlayer() != null) {
                Msg.error(owner.getPlayer(), "Your shipment was confiscated by " + police.getName() + ".");
            }
        }
        return Result.SUCCESS;
    }
}
