package com.pickmobup.listener;

import com.pickmobup.carry.CarryManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class SneakListener implements Listener {

    private final CarryManager carryManager;

    public SneakListener(CarryManager carryManager) {
        this.carryManager = carryManager;
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (!carryManager.isCarrying(player.getUniqueId())) {
            return;
        }
        if (event.isSneaking()) {
            carryManager.onSneakPress(player);
        } else {
            carryManager.onSneakRelease(player);
        }
    }
}
