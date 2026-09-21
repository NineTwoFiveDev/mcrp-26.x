package com.mcrp.darkrp.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses simple "MATERIAL:AMOUNT" kit definitions from config into ItemStacks.
 */
public final class ItemUtil {

    private ItemUtil() {
    }

    public static List<ItemStack> parseKit(Plugin plugin, List<String> definitions) {
        List<ItemStack> items = new ArrayList<>();
        if (definitions == null) {
            return items;
        }
        for (String def : definitions) {
            ItemStack item = parseSingle(def);
            if (item == null) {
                plugin.getLogger().warning("Invalid kit item definition: '" + def + "' (expected MATERIAL:AMOUNT)");
                continue;
            }
            items.add(item);
        }
        return items;
    }

    public static ItemStack parseSingle(String def) {
        if (def == null || def.isBlank()) {
            return null;
        }
        String[] parts = def.split(":", 2);
        Material material = Material.matchMaterial(parts[0].trim());
        if (material == null) {
            return null;
        }
        int amount = 1;
        if (parts.length > 1) {
            try {
                amount = Math.max(1, Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ignored) {
                amount = 1;
            }
        }
        return new ItemStack(material, amount);
    }
}
