package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.util.Msg;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Classic DarkRP /advert: broadcast a short message to everyone, usually at
 * a small cost, on a per-player cooldown.
 */
public class AdvertCommand implements CommandExecutor {

    private final Plugin plugin;
    private final EconomyManager economy;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public AdvertCommand(Plugin plugin, EconomyManager economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.error(sender, "Only players can advertise.");
            return true;
        }
        if (args.length == 0) {
            Msg.error(sender, "Usage: /advert <message>");
            return true;
        }
        long cooldownSeconds = plugin.getConfig().getLong("advert.cooldown-seconds", 60);
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null) {
            long remaining = (last + cooldownSeconds * 1000L - System.currentTimeMillis()) / 1000L;
            if (remaining > 0) {
                Msg.error(sender, "You must wait " + remaining + "s before advertising again.");
                return true;
            }
        }
        double cost = plugin.getConfig().getDouble("advert.cost", 25.0);
        if (cost > 0 && !economy.withdraw(player, cost)) {
            Msg.error(sender, "Advertising costs " + economy.format(cost) + " and you don't have enough.");
            return true;
        }

        String message = String.join(" ", args);
        Component broadcast = Msg.parse("<gold><bold>[Advert]</bold></gold> <white>" + player.getName() + "</white><gray>:</gray> ")
                .append(Component.text(message));
        Bukkit.broadcast(broadcast);

        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        return true;
    }
}
