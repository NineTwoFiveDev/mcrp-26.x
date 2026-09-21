package com.mcrp.darkrp.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Serializes/deserializes Locations to a compact string form for storage in YAML.
 */
public final class LocUtil {

    private LocUtil() {
    }

    /** Block-precision key, ignoring yaw/pitch. Used to identify a specific block (e.g. a door). */
    public static String blockKey(Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    public static String serialize(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ()
                + ":" + loc.getYaw() + ":" + loc.getPitch();
    }

    public static Location deserialize(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String[] p = s.split(":");
        if (p.length < 4) {
            return null;
        }
        World world = Bukkit.getWorld(p[0]);
        if (world == null) {
            return null;
        }
        double x = Double.parseDouble(p[1]);
        double y = Double.parseDouble(p[2]);
        double z = Double.parseDouble(p[3]);
        float yaw = p.length > 4 ? Float.parseFloat(p[4]) : 0f;
        float pitch = p.length > 5 ? Float.parseFloat(p[5]) : 0f;
        return new Location(world, x, y, z, yaw, pitch);
    }
}
