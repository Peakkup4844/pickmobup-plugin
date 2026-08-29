package com.pickmobup.listener;

import com.pickmobup.carry.CarryManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class CleanupListener implements Listener {

    private final CarryManager carryManager;

    public CleanupListener(CarryManager carryManager) {
        this.carryManager = carryManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        carryManager.drop(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        carryManager.drop(event.getEntity());
    }
}
