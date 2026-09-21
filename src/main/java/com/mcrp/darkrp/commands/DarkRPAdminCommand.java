package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.door.DoorManager;
import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class DarkRPAdminCommand implements CommandExecutor {

    private final Plugin plugin;
    private final EconomyManager economy;
    private final JobManager jobManager;
    private final DoorManager doorManager;

    public DarkRPAdminCommand(Plugin plugin, EconomyManager economy, JobManager jobManager, DoorManager doorManager) {
        this.plugin = plugin;
        this.economy = economy;
        this.jobManager = jobManager;
        this.doorManager = doorManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            Msg.error(sender, "Usage: /darkrp <reload|setbalance|give> [args]");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "setbalance" -> handleSetBalance(sender, args);
            case "give" -> handleGive(sender, args);
            default -> Msg.error(sender, "Unknown subcommand. Use reload, setbalance or give.");
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        economy.reload();
        jobManager.load();
        jobManager.startSalaryTask();
        doorManager.reloadConfig();
        Msg.success(sender, "DarkRP configuration reloaded.");
    }

    private void handleSetBalance(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Msg.error(sender, "Usage: /darkrp setbalance <player> <amount>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        double amount = parseAmount(sender, args[2]);
        if (Double.isNaN(amount)) {
            return;
        }
        economy.record(target).setBalance(amount);
        Msg.success(sender, "Set " + target.getName() + "'s balance to " + economy.format(amount) + ".");
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 3) {
            Msg.error(sender, "Usage: /darkrp give <player> <amount>");
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        double amount = parseAmount(sender, args[2]);
        if (Double.isNaN(amount)) {
            return;
        }
        economy.deposit(target, amount);
        Msg.success(sender, "Gave " + target.getName() + " " + economy.format(amount) + ".");
    }

    private double parseAmount(CommandSender sender, String raw) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            Msg.error(sender, "Invalid amount.");
            return Double.NaN;
        }
    }
}
