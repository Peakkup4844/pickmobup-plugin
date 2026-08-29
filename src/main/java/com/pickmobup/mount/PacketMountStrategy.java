package com.pickmobup.mount;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * Visual-only mount driven by PacketEvents. The entity is NOT a real server-side
 * passenger; instead a "set passengers" packet is sent to nearby clients so they
 * render the entity riding the carrier. The real entity is kept following the
 * carrier by {@code CarryManager} (see {@link #needsFollow()}).
 *
 * This class only loads when PacketEvents is present, so its imports are safe.
 */
public class PacketMountStrategy implements MountStrategy {

    @Override
    public MountMode mode() {
        return MountMode.PACKET;
    }

    @Override
    public void mount(Player carrier, Entity entity) {
        broadcast(carrier, new int[]{entity.getEntityId()});
    }

    @Override
    public void dismount(Player carrier, Entity entity) {
        broadcast(carrier, new int[0]);
    }

    @Override
    public void maintain(Player carrier, Entity entity) {
        // Re-send so players who came into view also see the mount.
        broadcast(carrier, new int[]{entity.getEntityId()});
    }

    @Override
    public boolean needsFollow() {
        return true;
    }

    private void broadcast(Player carrier, int[] passengers) {
        WrapperPlayServerSetPassengers packet =
                new WrapperPlayServerSetPassengers(carrier.getEntityId(), passengers);
        for (Player viewer : carrier.getWorld().getPlayers()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(viewer, packet);
        }
    }
}
