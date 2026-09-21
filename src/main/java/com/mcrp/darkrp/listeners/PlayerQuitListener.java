package com.mcrp.darkrp.listeners;

import com.mcrp.darkrp.storage.DataStore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final DataStore dataStore;

    public PlayerQuitListener(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        dataStore.save();
    }
}
