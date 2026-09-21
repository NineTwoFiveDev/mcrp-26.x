package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.door.DoorManager;
import com.mcrp.darkrp.door.DoorMenu;
import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.util.Completions;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class DoorCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("buy", "sell", "lock", "unlock", "add", "kick", "price", "info");

    private static final int REACH = 6;

    private final DoorManager doorManager;
    private final EconomyManager economy;

    public DoorCommand(DoorManager doorManager, EconomyManager economy) {
        this.doorManager = doorManager;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.error(sender, "Only players can manage doors.");
            return true;
        }
        if (args.length == 0) {
            Msg.error(sender, "Usage: /door <buy|sell|lock|unlock|add|kick|price|info> [player|amount]");
            return true;
        }
        Block target = doorManager.getTargetDoor(player, REACH);
        if (target == null) {
            Msg.error(sender, "You must be looking at a door.");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "buy" -> handleResult(player, doorManager.buy(player, target), "You bought this door.");
            case "sell" -> handleResult(player, doorManager.sell(player, target), "You sold this door back.");
            case "lock" -> handleResult(player, doorManager.toggleLock(player, target, true), "Door locked.");
            case "unlock" -> handleResult(player, doorManager.toggleLock(player, target, false), "Door unlocked.");
            case "add" -> handleOwnerChange(player, target, args, true);
            case "kick" -> handleOwnerChange(player, target, args, false);
            case "price" -> handlePrice(player, target, args);
            case "info" -> DoorMenu.send(player, doorManager, economy, target);
            default -> Msg.error(sender, "Unknown door subcommand. Use buy, sell, lock, unlock, add, kick, price or info.");
        }
        return true;
    }

    private void handleOwnerChange(Player player, Block target, String[] args, boolean add) {
        if (args.length < 2) {
            Msg.error(player, "Usage: /door " + (add ? "add" : "kick") + " <player>");
            return;
        }
        OfflinePlayer other = Bukkit.getOfflinePlayer(args[1]);
        if (!other.hasPlayedBefore() && !other.isOnline()) {
            Msg.error(player, "That player has never joined this server.");
            return;
        }
        var result = add ? doorManager.addOwner(player, target, other) : doorManager.kickOwner(player, target, other);
        handleResult(player, result, (add ? "Added " : "Removed ") + other.getName() + " as a co-owner.");
    }

    private void handlePrice(Player player, Block target, String[] args) {
        if (args.length < 2) {
            Msg.error(player, "Usage: /door price <amount> (0 to take it off the market)");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            Msg.error(player, "Invalid amount.");
            return;
        }
        var result = doorManager.setForSalePrice(player, target, amount);
        String successMsg = amount <= 0 ? "This door is no longer for sale."
                : "This door is now for sale at " + economy.format(amount) + ".";
        handleResult(player, result, successMsg);
    }

    private void handleResult(Player player, DoorManager.Result result, String successMessage) {
        switch (result) {
            case SUCCESS -> Msg.success(player, successMessage);
            case NOT_A_DOOR -> Msg.error(player, "That's not a door.");
            case TOO_LARGE -> Msg.error(player, "This door is connected to too many others to be owned as one unit.");
            case ALREADY_OWNED -> Msg.error(player, "This door is already owned by someone else.");
            case ALREADY_YOURS -> Msg.error(player, "You already own this door.");
            case NOT_FOR_SALE -> Msg.error(player, "This door isn't for sale.");
            case INSUFFICIENT_FUNDS -> Msg.error(player, "You don't have enough money.");
            case NOT_OWNED -> Msg.error(player, "Nobody owns this door.");
            case NOT_YOUR_DOOR -> Msg.error(player, "You don't own this door.");
            case NOT_THE_OWNER -> Msg.error(player, "Only the primary owner can do that.");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Completions.filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("kick"))) {
            return Completions.onlinePlayerNames(args[1]);
        }
        return Collections.emptyList();
    }
}
