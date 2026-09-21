package com.mcrp.darkrp.door;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.model.DoorRecord;
import com.mcrp.darkrp.util.LocUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Door;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Handles door ownership: locating door groups (double doors), buying/selling,
 * locking and owner management, all persisted to doors.yml.
 */
public class DoorManager {

    private static final BlockFace[] HORIZONTAL = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};

    private final Plugin plugin;
    private final EconomyManager economy;
    private final File file;
    private final Map<String, DoorRecord> doors = new ConcurrentHashMap<>();

    private double defaultPrice;
    private int resalePercentage;
    private int maxGroupSize;
    private Map<String, Double> priceOverrides = Map.of();

    public DoorManager(Plugin plugin, EconomyManager economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.file = new File(plugin.getDataFolder(), "doors.yml");
        reloadConfig();
    }

    public void reloadConfig() {
        this.defaultPrice = plugin.getConfig().getDouble("door.default-price", 250.0);
        this.resalePercentage = plugin.getConfig().getInt("door.resale-percentage", 50);
        this.maxGroupSize = Math.max(1, plugin.getConfig().getInt("door.max-group-size", 4));
        ConfigurationSection prices = plugin.getConfig().getConfigurationSection("door.prices");
        Map<String, Double> overrides = new ConcurrentHashMap<>();
        if (prices != null) {
            for (String key : prices.getKeys(false)) {
                overrides.put(key.toUpperCase(), prices.getDouble(key));
            }
        }
        this.priceOverrides = overrides;
    }

    // ----- persistence -----

    public void load() {
        doors.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("doors");
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            String path = id + ".";
            List<String> blockKeys = section.getStringList(path + "blocks");
            String ownerStr = section.getString(path + "owner", "");
            UUID owner = ownerStr == null || ownerStr.isBlank() ? null : UUID.fromString(ownerStr);
            double price = section.getDouble(path + "price", defaultPrice);
            DoorRecord record = new DoorRecord(id, new HashSet<>(blockKeys), owner, price);
            record.setLocked(section.getBoolean(path + "locked", false));
            record.setForSalePrice(section.getDouble(path + "for-sale-price", 0.0));
            for (String co : section.getStringList(path + "coowners")) {
                try {
                    record.getCoOwners().add(UUID.fromString(co));
                } catch (IllegalArgumentException ignored) {
                }
            }
            doors.put(id, record);
        }
        plugin.getLogger().info("Loaded " + doors.size() + " owned door(s).");
    }

    public synchronized void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (DoorRecord record : doors.values()) {
            String path = "doors." + record.getId() + ".";
            yaml.set(path + "blocks", new ArrayList<>(record.getBlockKeys()));
            yaml.set(path + "owner", record.getOwner() == null ? "" : record.getOwner().toString());
            yaml.set(path + "coowners", record.getCoOwners().stream().map(UUID::toString).collect(Collectors.toList()));
            yaml.set(path + "locked", record.isLocked());
            yaml.set(path + "price", record.getPrice());
            yaml.set(path + "for-sale-price", record.getForSalePrice());
        }
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save doors.yml", e);
        }
    }

    // ----- group detection -----

    public boolean isDoor(Block block) {
        return block != null && Tag.DOORS.isTagged(block.getType());
    }

    /** Finds the door block the player is currently looking at, within range, or null. */
    public Block getTargetDoor(Player player, int maxDistance) {
        Block target = player.getTargetBlockExact(maxDistance);
        return isDoor(target) ? target : null;
    }

    /** Returns the connected set of door blocks (bottom halves only), or null if not a door. */
    public Set<Block> findGroup(Block clicked) {
        if (!isDoor(clicked)) {
            return null;
        }
        Block bottom = normalizeBottom(clicked);
        Set<Block> visited = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        visited.add(bottom);
        queue.add(bottom);
        while (!queue.isEmpty()) {
            Block cur = queue.poll();
            for (BlockFace face : HORIZONTAL) {
                Block neighbor = cur.getRelative(face);
                if (visited.contains(neighbor) || !isDoor(neighbor)) {
                    continue;
                }
                Block neighborBottom = normalizeBottom(neighbor);
                if (neighborBottom.getY() != bottom.getY() || visited.contains(neighborBottom)) {
                    continue;
                }
                visited.add(neighborBottom);
                queue.add(neighborBottom);
            }
        }
        return visited;
    }

    private Block normalizeBottom(Block doorBlock) {
        BlockData data = doorBlock.getBlockData();
        if (data instanceof Door door && door.getHalf() == Bisected.Half.TOP) {
            return doorBlock.getRelative(BlockFace.DOWN);
        }
        return doorBlock;
    }

    public String keyFor(Set<Block> group) {
        TreeSet<String> sorted = new TreeSet<>();
        for (Block b : group) {
            sorted.add(LocUtil.blockKey(b));
        }
        return String.join("|", sorted);
    }

    public DoorRecord getRecord(Set<Block> group) {
        return doors.get(keyFor(group));
    }

    public double priceFor(Set<Block> group) {
        double total = 0;
        for (Block b : group) {
            total += priceOverrides.getOrDefault(b.getType().name(), defaultPrice);
        }
        return total;
    }

    // ----- actions -----

    public enum Result {
        SUCCESS, NOT_A_DOOR, TOO_LARGE, ALREADY_OWNED, ALREADY_YOURS, NOT_FOR_SALE,
        INSUFFICIENT_FUNDS, NOT_OWNED, NOT_YOUR_DOOR, NOT_THE_OWNER
    }

    public Result buy(Player player, Block clicked) {
        Set<Block> group = findGroup(clicked);
        if (group == null) {
            return Result.NOT_A_DOOR;
        }
        if (group.size() > maxGroupSize) {
            return Result.TOO_LARGE;
        }
        DoorRecord record = getRecord(group);
        if (record != null && record.getOwner() != null) {
            if (record.isOwnedBy(player.getUniqueId())) {
                return Result.ALREADY_YOURS;
            }
            if (record.getForSalePrice() <= 0) {
                return Result.ALREADY_OWNED;
            }
            OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(record.getOwner());
            double price = record.getForSalePrice();
            if (!economy.has(player, price)) {
                return Result.INSUFFICIENT_FUNDS;
            }
            economy.withdraw(player, price);
            economy.deposit(ownerPlayer, price);
            record.setOwner(player.getUniqueId());
            record.getCoOwners().clear();
            record.setForSalePrice(0);
            record.setPrice(price);
            save();
            return Result.SUCCESS;
        }
        double price = priceFor(group);
        if (!economy.has(player, price)) {
            return Result.INSUFFICIENT_FUNDS;
        }
        economy.withdraw(player, price);
        String key = keyFor(group);
        Set<String> blockKeys = group.stream().map(LocUtil::blockKey).collect(Collectors.toSet());
        DoorRecord newRecord = new DoorRecord(key, blockKeys, player.getUniqueId(), price);
        doors.put(key, newRecord);
        save();
        return Result.SUCCESS;
    }

    public Result sell(Player player, Block clicked) {
        Set<Block> group = findGroup(clicked);
        if (group == null) {
            return Result.NOT_A_DOOR;
        }
        DoorRecord record = getRecord(group);
        if (record == null || record.getOwner() == null) {
            return Result.NOT_OWNED;
        }
        if (!player.getUniqueId().equals(record.getOwner())) {
            return Result.NOT_THE_OWNER;
        }
        double refund = record.getPrice() * (resalePercentage / 100.0);
        economy.deposit(player, refund);
        doors.remove(record.getId());
        save();
        return Result.SUCCESS;
    }

    public Result toggleLock(Player player, Block clicked, boolean locked) {
        Set<Block> group = findGroup(clicked);
        if (group == null) {
            return Result.NOT_A_DOOR;
        }
        DoorRecord record = getRecord(group);
        if (record == null || record.getOwner() == null) {
            return Result.NOT_OWNED;
        }
        if (!record.isOwnedBy(player.getUniqueId())) {
            return Result.NOT_YOUR_DOOR;
        }
        record.setLocked(locked);
        if (locked) {
            closeGroup(group);
        }
        save();
        return Result.SUCCESS;
    }

    public Result addOwner(Player player, Block clicked, OfflinePlayer target) {
        Set<Block> group = findGroup(clicked);
        if (group == null) {
            return Result.NOT_A_DOOR;
        }
        DoorRecord record = getRecord(group);
        if (record == null || record.getOwner() == null) {
            return Result.NOT_OWNED;
        }
        if (!player.getUniqueId().equals(record.getOwner())) {
            return Result.NOT_THE_OWNER;
        }
        record.getCoOwners().add(target.getUniqueId());
        save();
        return Result.SUCCESS;
    }

    public Result kickOwner(Player player, Block clicked, OfflinePlayer target) {
        Set<Block> group = findGroup(clicked);
        if (group == null) {
            return Result.NOT_A_DOOR;
        }
        DoorRecord record = getRecord(group);
        if (record == null || record.getOwner() == null) {
            return Result.NOT_OWNED;
        }
        if (!player.getUniqueId().equals(record.getOwner())) {
            return Result.NOT_THE_OWNER;
        }
        record.getCoOwners().remove(target.getUniqueId());
        save();
        return Result.SUCCESS;
    }

    public Result setForSalePrice(Player player, Block clicked, double price) {
        Set<Block> group = findGroup(clicked);
        if (group == null) {
            return Result.NOT_A_DOOR;
        }
        DoorRecord record = getRecord(group);
        if (record == null || record.getOwner() == null) {
            return Result.NOT_OWNED;
        }
        if (!player.getUniqueId().equals(record.getOwner())) {
            return Result.NOT_THE_OWNER;
        }
        record.setForSalePrice(Math.max(0, price));
        save();
        return Result.SUCCESS;
    }

    public boolean isLockedFor(Player player, Block clicked) {
        Set<Block> group = findGroup(clicked);
        if (group == null) {
            return false;
        }
        DoorRecord record = getRecord(group);
        return record != null && record.isLocked() && !record.isOwnedBy(player.getUniqueId());
    }

    private void closeGroup(Set<Block> group) {
        for (Block bottom : group) {
            for (Block b : new Block[]{bottom, bottom.getRelative(BlockFace.UP)}) {
                BlockData data = b.getBlockData();
                if (data instanceof Openable openable) {
                    openable.setOpen(false);
                    b.setBlockData(data, false);
                }
            }
        }
    }
}
