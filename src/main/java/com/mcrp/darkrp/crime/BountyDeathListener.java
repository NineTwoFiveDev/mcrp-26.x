package com.mcrp.darkrp.crime;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Pays out a player's bounty to whoever kills them in PvP.
 */
public class BountyDeathListener implements Listener {

    private final BountyManager bountyManager;

    public BountyDeathListener(BountyManager bountyManager) {
        this.bountyManager = bountyManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }
        bountyManager.payout(victim, killer);
    }
}
