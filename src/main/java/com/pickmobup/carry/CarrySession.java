package com.pickmobup.carry;

import com.pickmobup.mount.MountStrategy;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.entity.Entity;

import java.util.UUID;

/**
 * Per-player state while an entity is being carried.
 */
public class CarrySession {

    public final UUID carrierId;
    public final Entity entity;
    public MountStrategy strategy;

    /** The sneak release immediately following pickup must not place the entity. */
    public boolean ignoreNextSneakRelease;

    public boolean charging;
    public long chargeStartMillis;

    public WrappedTask pendingChargeTask;
    public WrappedTask chargeTask;
    public WrappedTask maintenanceTask;

    // Saved entity state, restored on release.
    public boolean aiChanged;
    public boolean prevAi;
    public boolean invulnChanged;
    public boolean prevInvuln;

    public CarrySession(UUID carrierId, Entity entity) {
        this.carrierId = carrierId;
        this.entity = entity;
    }
}
