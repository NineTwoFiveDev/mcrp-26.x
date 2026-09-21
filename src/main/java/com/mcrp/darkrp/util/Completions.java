package com.mcrp.darkrp.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Shared helpers for building tab-complete suggestion lists.
 */
public final class Completions {

    private Completions() {
    }

    public static List<String> onlinePlayerNames(String partial) {
        String needle = partial == null ? "" : partial.toLowerCase();
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(needle)) {
                names.add(player.getName());
            }
        }
        return names;
    }

    public static List<String> filter(Collection<String> options, String partial) {
        String needle = partial == null ? "" : partial.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(needle)) {
                result.add(option);
            }
        }
        return result;
    }
}
