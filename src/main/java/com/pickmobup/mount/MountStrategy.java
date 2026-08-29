package com.pickmobup.mount;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Abstraction over "how an entity is shown riding on the player's head".
 */
public interface MountStrategy {

    MountMode mode();

    /** Attach the entity to the carrier. Called on the carrier's region thread. */
    void mount(Player carrier, Entity entity);

    /** Detach the entity from the carrier. Called on the carrier's region thread. */
    void dismount(Player carrier, Entity entity);

    /** Periodic upkeep (e.g. re-broadcasting packets to new viewers). May be a no-op. */
    default void maintain(Player carrier, Entity entity) {
    }

    /** Whether this strategy needs the real entity to be kept following the carrier server-side. */
    default boolean needsFollow() {
        return false;
    }

    /**
     * Whether the carrier→entity attachment still holds. Polled during maintenance
     * so a mount broken externally (e.g. the carrier changed world / was teleported,
     * or another plugin ejected the passenger) is detected and cleaned up instead of
     * leaving the entity orphaned with its AI disabled. Visual strategies that never
     * create a real attachment should keep returning {@code true}.
     */
    default boolean isMounted(Player carrier, Entity entity) {
        return true;
    }
}
