package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.job.JobManager;
import com.mcrp.darkrp.model.ShipmentType;
import com.mcrp.darkrp.shipment.ShipmentManager;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.stream.Collectors;

public class ShipmentCommand implements CommandExecutor {

    private static final int REACH = 5;

    private final ShipmentManager shipmentManager;
    private final JobManager jobManager;

    public ShipmentCommand(ShipmentManager shipmentManager, JobManager jobManager) {
        this.shipmentManager = shipmentManager;
        this.jobManager = jobManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.error(sender, "Only players can manage shipments.");
            return true;
        }
        if (args.length == 0) {
            Msg.error(sender, "Usage: /shipment <buy <type>|list|confiscate>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "list" -> handleList(player);
            case "buy" -> handleBuy(player, args);
            case "confiscate" -> handleConfiscate(player);
            default -> Msg.error(sender, "Unknown subcommand. Use buy, list or confiscate.");
        }
        return true;
    }

    private void handleList(Player player) {
        var types = shipmentManager.getTypes();
        if (types.isEmpty()) {
            Msg.error(player, "No shipment types are configured.");
            return;
        }
        Msg.raw(player, "<dark_gray>---- <gold>Shipments</gold> ----</dark_gray>");
        for (ShipmentType type : types) {
            Msg.raw(player, "<click:run_command:'/shipment buy " + type.getId() + "'><yellow>" + type.getDisplayName()
                    + "</yellow></click> <gray>- $" + type.getPrice() + " (" + type.getAmount() + "x " + type.getItem().name() + ")</gray>");
        }
    }

    private void handleBuy(Player player, String[] args) {
        if (args.length < 2) {
            Msg.error(player, "Usage: /shipment buy <type>");
            return;
        }
        Block solid = player.getTargetBlockExact(REACH);
        Block spot = solid != null ? solid.getRelative(BlockFace.UP) : null;
        var result = shipmentManager.buy(player, spot, args[1]);
        switch (result) {
            case SUCCESS -> Msg.success(player, "Placed a shipment of " + args[1] + ".");
            case UNKNOWN_TYPE -> {
                String ids = shipmentManager.getTypes().stream().map(ShipmentType::getId).collect(Collectors.joining(", "));
                Msg.error(player, "Unknown shipment type. Available: " + ids);
            }
            case NOT_A_SPOT -> Msg.error(player, "There isn't a clear, empty spot there to place a shipment.");
            case WRONG_JOB -> Msg.error(player, "Your job can't buy that shipment.");
            case INSUFFICIENT_FUNDS -> Msg.error(player, "You don't have enough money.");
            default -> Msg.error(player, "Couldn't place that shipment.");
        }
    }

    private void handleConfiscate(Player player) {
        if (!jobManager.canActAsPolice(player)) {
            Msg.error(player, "Only police can confiscate shipments.");
            return;
        }
        Block target = player.getTargetBlockExact(REACH);
        var result = shipmentManager.confiscate(player, target);
        switch (result) {
            case SUCCESS -> Msg.success(player, "Confiscated the shipment.");
            case NOT_A_SHIPMENT -> Msg.error(player, "You must be looking at a shipment crate.");
            default -> Msg.error(player, "Couldn't confiscate that shipment.");
        }
    }
}
