package com.pickmobup.carry;

import com.pickmobup.PickMobUpPlugin;
import com.pickmobup.config.PluginConfig;
import com.pickmobup.message.MessageService;
import com.pickmobup.mount.MountMode;
import com.pickmobup.mount.MountStrategy;
import com.pickmobup.mount.PacketMountStrategy;
import com.pickmobup.mount.PassengerMountStrategy;
import com.pickmobup.util.EntityUtil;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CarryManager {

    private final PickMobUpPlugin plugin;
    private final PlatformScheduler scheduler;
    private final MessageService messages;

    /** carrier UUID -> session */
    private final Map<UUID, CarrySession> sessions = new ConcurrentHashMap<>();
    /** entity UUIDs currently claimed, so the same entity can't be grabbed twice (also covers PACKET mode). */
    private final Set<UUID> claimed = ConcurrentHashMap.newKeySet();

    private final PassengerMountStrategy passengerStrategy = new PassengerMountStrategy();
    private MountStrategy packetStrategy; // lazily created only if PacketEvents present
    private boolean warnedPacketMissing;

    public CarryManager(PickMobUpPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.getFoliaLib().getScheduler();
        this.messages = plugin.messages();
    }

    public boolean isCarrying(UUID id) {
        return sessions.containsKey(id);
    }

    private PluginConfig cfg() {
        return plugin.config();
    }

    private long msToTicks(long ms) {
        return Math.max(1L, ms / 50L);
    }

    // ---- pickup ----------------------------------------------------------

    /**
     * Try to pick up the target. Returns true if the interaction should be
     * cancelled. We only cancel (and message) once we are confident the player
     * intends a pickup; otherwise we stay silent so normal interactions
     * (villager trading, etc.) are never broken.
     */
    public boolean attemptPickup(Player player, Entity target) {
        if (!(target instanceof LivingEntity) || target.equals(player)) {
            return false;
        }

        PluginConfig cfg = cfg();

        // Silent rejections: not our concern -> let vanilla handle the click.
        if (!player.hasPermission("pickmobup.use")) {
            return false;
        }
        if (!cfg.isWorldAllowed(player.getWorld().getName())) {
            return false;
        }
        if (target instanceof Player
                && (!cfg.allowCarryPlayers() || !player.hasPermission("pickmobup.carryplayers"))) {
            return false;
        }
        if (!cfg.isTypeAllowed(target.getType())) {
            return false;
        }

        // From here we treat the click as a deliberate pickup attempt.
        if (isCarrying(player.getUniqueId())) {
            messages.feedback(player, "already-carrying");
            return true;
        }

        // Atomic claim guards against duplicate/concurrent pickups (incl. PACKET mode).
        if (!target.getPassengers().isEmpty() || target.getVehicle() != null
                || !claimed.add(target.getUniqueId())) {
            messages.feedback(player, "entity-already-carried");
            return true;
        }

        CarrySession session = new CarrySession(player.getUniqueId(), target);
        session.strategy = resolveStrategy();
        session.ignoreNextSneakRelease = true;

        if (cfg.disableAi() && !(target instanceof Player)) {
            LivingEntity le = (LivingEntity) target;
            session.aiChanged = true;
            session.prevAi = le.hasAI();
            le.setAI(false);
        }
        if (cfg.invulnerable()) {
            session.invulnChanged = true;
            session.prevInvuln = target.isInvulnerable();
            target.setInvulnerable(true);
        }

        try {
            session.strategy.mount(player, target);
        } catch (Throwable t) {
            restoreState(session);
            claimed.remove(target.getUniqueId());
            messages.feedback(player, "cannot-carry-entity");
            plugin.getLogger().warning("อุ้มไม่สำเร็จ: " + t.getMessage());
            return true;
        }

        sessions.put(player.getUniqueId(), session);
        startMaintenance(player, session);

        messages.feedback(player, "pickup-success", "%entity%", EntityUtil.displayName(target));
        cfg.playSound(player, "pickup");
        return true;
    }

    private MountStrategy resolveStrategy() {
        if (cfg().mountMode() == MountMode.PACKET) {
            if (plugin.isPacketEventsAvailable()) {
                if (packetStrategy == null) {
                    packetStrategy = new PacketMountStrategy();
                }
                return packetStrategy;
            }
            if (!warnedPacketMissing) {
                warnedPacketMissing = true;
                plugin.getLogger().warning("mount-mode=PACKET แต่ไม่พบปลั๊กอิน PacketEvents - fallback เป็น PASSENGER");
            }
        }
        return passengerStrategy;
    }

    // ---- sneak handling --------------------------------------------------

    public void onSneakPress(Player player) {
        CarrySession s = sessions.get(player.getUniqueId());
        if (s == null) {
            return;
        }
        // Schedule entering charge mode once the tap-threshold elapses.
        // Use the Runnable overload so we get a cancellable WrappedTask back.
        long delay = msToTicks(cfg().tapThresholdMs());
        s.pendingChargeTask = scheduler.runAtEntityLater(player, () -> {
            s.pendingChargeTask = null;
            if (sessions.get(player.getUniqueId()) != s) {
                return;
            }
            if (!player.isOnline() || !player.isSneaking()) {
                return;
            }
            beginCharge(player, s);
        }, delay);
    }

    public void onSneakRelease(Player player) {
        CarrySession s = sessions.get(player.getUniqueId());
        if (s == null) {
            return;
        }
        if (s.ignoreNextSneakRelease) {
            s.ignoreNextSneakRelease = false;
            return;
        }
        if (s.charging) {
            throwEntity(player, s);
            return;
        }
        // Released before threshold -> tap -> place.
        if (s.pendingChargeTask != null) {
            s.pendingChargeTask.cancel();
            s.pendingChargeTask = null;
        }
        place(player, s);
    }

    private void beginCharge(Player player, CarrySession s) {
        if (!cfg().throwEnabled()) {
            return; // a later release will simply place the entity.
        }
        s.charging = true;
        s.chargeStartMillis = System.currentTimeMillis();
        long period = cfg().chargeUpdateTicks();
        s.chargeTask = scheduler.runAtEntityTimer(player, () -> {
            if (sessions.get(player.getUniqueId()) != s || !s.charging || !player.isOnline()) {
                if (s.chargeTask != null) {
                    s.chargeTask.cancel();
                }
                return;
            }
            double frac = chargeFraction(s);
            messages.charge(player, frac, (int) Math.round(frac * 100));
        }, 0L, period);
    }

    private double chargeFraction(CarrySession s) {
        long elapsed = System.currentTimeMillis() - s.chargeStartMillis;
        long cycleMs = (long) cfg().chargeCycleTicks() * 50L;
        if (cycleMs <= 0) {
            return 1.0;
        }
        double phase = (elapsed % cycleMs) / (double) cycleMs; // 0..1
        // Triangle wave: 0 -> 1 -> 0
        return phase < 0.5 ? phase * 2.0 : (1.0 - phase) * 2.0;
    }

    // ---- place / throw ---------------------------------------------------

    public void place(Player player, CarrySession s) {
        Entity e = s.entity;
        endSession(player, s);

        try {
            s.strategy.dismount(player, e);
        } catch (Throwable ignored) {
        }
        restoreState(s);

        Location loc = player.getLocation();
        scheduler.runAtEntity(e, task -> {
            if (e.isValid()) {
                EntityUtil.teleport(e, loc);
            }
        });

        messages.feedback(player, "place-success");
        cfg().playSound(player, "place");
    }

    public void throwEntity(Player player, CarrySession s) {
        if (!cfg().throwEnabled()) {
            messages.feedback(player, "throw-disabled");
            place(player, s);
            return;
        }

        double frac = chargeFraction(s);
        Entity e = s.entity;
        endSession(player, s);

        try {
            s.strategy.dismount(player, e);
        } catch (Throwable ignored) {
        }
        restoreState(s);

        double power = frac * cfg().maxPowerBlocks();
        Vector dir = player.getEyeLocation().getDirection().normalize();
        final Vector velocity = dir.multiply(power * cfg().velocityPerBlock());
        velocity.setY(velocity.getY() + cfg().upwardBoost());

        final Location launch = player.getEyeLocation();
        final boolean applySlow = cfg().slowFalling();
        scheduler.runAtEntity(e, task -> {
            if (!e.isValid()) {
                return;
            }
            // Launch from the carrier's position (matters for PACKET mode where the
            // real entity lags behind). teleportAsync finishes a tick later and wipes
            // velocity, so the throw must be applied AFTER the teleport completes --
            // otherwise the entity just drops off the player's head with no momentum.
            EntityUtil.teleport(e, launch).whenComplete((ok, ex) -> applyThrow(e, velocity, applySlow));
        });

        messages.feedback(player, "throw-success", "%power%", String.format("%.1f", power));
        cfg().playSound(player, "throw");
    }

    /** Apply the throw velocity (and slow falling) on the entity's region thread. */
    private void applyThrow(Entity e, Vector velocity, boolean applySlow) {
        scheduler.runAtEntity(e, task -> {
            if (!e.isValid()) {
                return;
            }
            e.setVelocity(velocity);
            if (applySlow && e instanceof LivingEntity) {
                LivingEntity le = (LivingEntity) e;
                int maxTicks = cfg().slowFallingMaxTicks();
                le.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOW_FALLING, maxTicks, 0, false, false, false));
                scheduleLandingRemoval(le, maxTicks);
            }
        });
    }

    private void scheduleLandingRemoval(LivingEntity entity, int maxTicks) {
        final int grace = cfg().slowFallingGroundGraceTicks();
        final long period = 5L;
        final long[] elapsed = {0L};
        final WrappedTask[] ref = new WrappedTask[1];
        ref[0] = scheduler.runAtEntityTimer(entity, () -> {
            elapsed[0] += period;
            boolean landed = entity.isValid() && elapsed[0] >= grace && entity.isOnGround();
            boolean timedOut = elapsed[0] >= maxTicks;
            if (!entity.isValid() || landed || timedOut) {
                if (entity.isValid()) {
                    entity.removePotionEffect(PotionEffectType.SLOW_FALLING);
                }
                if (ref[0] != null) {
                    ref[0].cancel();
                }
            }
        }, period, period);
    }

    // ---- maintenance & cleanup ------------------------------------------

    private void startMaintenance(Player player, CarrySession s) {
        final long period = 20L; // 1 second
        s.maintenanceTask = scheduler.runAtEntityTimer(player, () -> {
            CarrySession current = sessions.get(player.getUniqueId());
            if (current != s) {
                if (s.maintenanceTask != null) {
                    s.maintenanceTask.cancel();
                }
                return;
            }
            Entity e = s.entity;
            if (!player.isOnline() || e == null || e.isDead() || !e.isValid()) {
                abort(player, s);
                return;
            }
            // Detect a mount broken externally (carrier changed world / was teleported,
            // or the passenger was ejected by another plugin) so we restore the entity's
            // AI/invulnerability and release the claim instead of orphaning it.
            if (!s.strategy.isMounted(player, e)) {
                abort(player, s);
                return;
            }
            try {
                s.strategy.maintain(player, e);
            } catch (Throwable ignored) {
            }
            if (s.strategy.needsFollow()) {
                Location follow = player.getLocation();
                scheduler.runAtEntity(e, task -> {
                    if (e.isValid()) {
                        EntityUtil.teleport(e, follow);
                    }
                });
            }
        }, period, period);
    }

    /** Carried entity became invalid / carrier left while maintenance was running. */
    private void abort(Player player, CarrySession s) {
        Entity e = s.entity;
        endSession(player, s);
        if (e != null && e.isValid() && player != null && player.isOnline()) {
            try {
                s.strategy.dismount(player, e);
            } catch (Throwable ignored) {
            }
        }
        restoreState(s);
    }

    /** Player quit / died while carrying: drop the entity where they are. */
    public void drop(Player player) {
        CarrySession s = sessions.get(player.getUniqueId());
        if (s == null) {
            return;
        }
        Entity e = s.entity;
        endSession(player, s);
        messages.clear(player);
        try {
            s.strategy.dismount(player, e);
        } catch (Throwable ignored) {
        }
        restoreState(s);
        if (e != null && e.isValid()) {
            Location loc = player.getLocation();
            scheduler.runAtEntity(e, task -> {
                if (e.isValid()) {
                    EntityUtil.teleport(e, loc);
                }
            });
        }
    }

    /** Plugin disable: best-effort synchronous cleanup of everything. */
    public void dropAll() {
        for (CarrySession s : sessions.values()) {
            cancelTasks(s);
            if (s.entity != null) {
                claimed.remove(s.entity.getUniqueId());
            }
            Player carrier = plugin.getServer().getPlayer(s.carrierId);
            try {
                if (carrier != null && s.entity != null) {
                    s.strategy.dismount(carrier, s.entity);
                }
            } catch (Throwable ignored) {
            }
            restoreState(s);
        }
        sessions.clear();
        claimed.clear();
        messages.clearAll();
    }

    /** Remove the session, cancel its tasks and release the entity claim. */
    private void endSession(Player player, CarrySession s) {
        sessions.remove(s.carrierId, s);
        cancelTasks(s);
        if (player != null) {
            messages.clearCharge(player);
        }
        if (s.entity != null) {
            claimed.remove(s.entity.getUniqueId());
        }
    }

    private void cancelTasks(CarrySession s) {
        if (s.pendingChargeTask != null) {
            s.pendingChargeTask.cancel();
            s.pendingChargeTask = null;
        }
        if (s.chargeTask != null) {
            s.chargeTask.cancel();
            s.chargeTask = null;
        }
        if (s.maintenanceTask != null) {
            s.maintenanceTask.cancel();
            s.maintenanceTask = null;
        }
    }

    private void restoreState(CarrySession s) {
        Entity e = s.entity;
        if (e == null) {
            return;
        }
        if (s.aiChanged && e instanceof LivingEntity) {
            try {
                ((LivingEntity) e).setAI(s.prevAi);
            } catch (Throwable ignored) {
            }
            s.aiChanged = false;
        }
        if (s.invulnChanged) {
            try {
                e.setInvulnerable(s.prevInvuln);
            } catch (Throwable ignored) {
            }
            s.invulnChanged = false;
        }
    }
}
