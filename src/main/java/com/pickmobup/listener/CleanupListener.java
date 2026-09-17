package com.pickmobup.listener;

import com.pickmobup.carry.CarryManager;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

public class CleanupListener implements Listener {

    private final CarryManager carryManager;

    public CleanupListener(CarryManager carryManager) {
        this.carryManager = carryManager;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        carryManager.drop(event.getPlayer());
        // The quitting player may itself be carried: restore it before its data is saved.
        carryManager.releaseCarried(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        carryManager.drop(event.getEntity());
    }

    // Entities saved mid-carry (e.g. left behind in an unloading chunk in PACKET mode,
    // or a server crash) come back frozen/invulnerable: undo that when they load.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            carryManager.recoverIfOrphaned(entity);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        carryManager.recoverIfOrphaned(event.getPlayer());
    }
}
