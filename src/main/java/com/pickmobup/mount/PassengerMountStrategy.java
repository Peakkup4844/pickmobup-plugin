package com.pickmobup.mount;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Real vanilla passenger: the entity becomes a passenger of the player, so it
 * rides on the player's head and follows automatically. Works on Spigot, Paper
 * and Folia without any extra dependency.
 */
public class PassengerMountStrategy implements MountStrategy {

    @Override
    public MountMode mode() {
        return MountMode.PASSENGER;
    }

    @Override
    public void mount(Player carrier, Entity entity) {
        // Make sure the entity is not riding/being ridden by something else.
        if (entity.getVehicle() != null) {
            entity.leaveVehicle();
        }
        carrier.addPassenger(entity);
    }

    @Override
    public void dismount(Player carrier, Entity entity) {
        carrier.removePassenger(entity);
    }

    @Override
    public boolean isMounted(Player carrier, Entity entity) {
        // Bukkit silently ejects passengers on world change / teleport; if the
        // entity is no longer riding the carrier the carry has effectively ended.
        return carrier.equals(entity.getVehicle());
    }
}
