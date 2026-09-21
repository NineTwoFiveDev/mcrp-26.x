package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.economy.EconomyManager;
import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.printer.PrinterManager;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PrinterCommand implements CommandExecutor {

    private static final int REACH = 5;

    private final PrinterManager printerManager;
    private final JobManager jobManager;
    private final EconomyManager economy;

    public PrinterCommand(PrinterManager printerManager, JobManager jobManager, EconomyManager economy) {
        this.printerManager = printerManager;
        this.jobManager = jobManager;
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.error(sender, "Only players can manage money printers.");
            return true;
        }
        if (args.length == 0) {
            Msg.error(sender, "Usage: /printer <buy|sell|confiscate>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "buy" -> handleBuy(player);
            case "sell" -> handleSell(player);
            case "confiscate" -> handleConfiscate(player);
            default -> Msg.error(sender, "Unknown subcommand. Use buy, sell or confiscate.");
        }
        return true;
    }

    private void handleBuy(Player player) {
        Block solid = player.getTargetBlockExact(REACH);
        if (solid == null) {
            Msg.error(player, "Look at a block to place a printer on top of it.");
            return;
        }
        Block spot = solid.getRelative(BlockFace.UP);
        var result = printerManager.buy(player, spot);
        switch (result) {
            case SUCCESS -> Msg.success(player, "Placed a money printer for " + economy.format(printerManager.getPrice()) + ".");
            case NOT_A_SPOT -> Msg.error(player, "There isn't a clear, empty spot there to place a printer.");
            case ALREADY_A_PRINTER -> Msg.error(player, "There's already a printer there.");
            case WRONG_JOB -> Msg.error(player, "Your job can't place money printers.");
            case LIMIT_REACHED -> Msg.error(player, "You already have the maximum number of printers.");
            case INSUFFICIENT_FUNDS -> Msg.error(player, "You don't have enough money.");
            default -> Msg.error(player, "Couldn't place the printer.");
        }
    }

    private void handleSell(Player player) {
        Block target = player.getTargetBlockExact(REACH);
        var result = printerManager.sell(player, target);
        switch (result) {
            case SUCCESS -> Msg.success(player, "Sold the printer back.");
            case NOT_A_PRINTER -> Msg.error(player, "You must be looking at a money printer.");
            case NOT_THE_OWNER -> Msg.error(player, "You don't own that printer.");
            default -> Msg.error(player, "Couldn't sell that printer.");
        }
    }

    private void handleConfiscate(Player player) {
        if (!jobManager.canActAsPolice(player)) {
            Msg.error(player, "Only police can confiscate money printers.");
            return;
        }
        Block target = player.getTargetBlockExact(REACH);
        var result = printerManager.confiscate(player, target);
        switch (result) {
            case SUCCESS -> Msg.success(player, "Confiscated the printer.");
            case NOT_A_PRINTER -> Msg.error(player, "You must be looking at a money printer.");
            default -> Msg.error(player, "Couldn't confiscate that printer.");
        }
    }
}
