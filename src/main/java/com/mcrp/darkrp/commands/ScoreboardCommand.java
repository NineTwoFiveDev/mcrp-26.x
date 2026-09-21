package com.mcrp.darkrp.commands;

import com.mcrp.darkrp.hud.ScoreboardManager;
import com.mcrp.darkrp.util.Msg;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScoreboardCommand implements CommandExecutor {

    private final ScoreboardManager scoreboardManager;

    public ScoreboardCommand(ScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Msg.error(sender, "Only players can toggle the scoreboard.");
            return true;
        }
        scoreboardManager.toggle(player);
        Msg.success(player, scoreboardManager.isHidden(player) ? "Scoreboard hidden." : "Scoreboard shown.");
        return true;
    }
}
