package com.pickmobup.listener;

import com.pickmobup.carry.CarryManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class InteractListener implements Listener {

    private final CarryManager carryManager;

    public InteractListener(CarryManager carryManager) {
        this.carryManager = carryManager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return; // avoid firing twice (off-hand)
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }
        if (carryManager.isCarrying(player.getUniqueId())) {
            return;
        }
        if (carryManager.attemptPickup(player, event.getRightClicked())) {
            event.setCancelled(true);
        }
    }
}
