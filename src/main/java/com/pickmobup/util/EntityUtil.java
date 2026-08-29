package com.pickmobup.util;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityUtil {

    private EntityUtil() {
    }

    // Cache of teleportAsync(Location) per runtime entity class. A class maps to
    // null once we know it has no such method (Spigot), avoiding repeated lookups.
    private static final Map<Class<?>, Method> ASYNC_CACHE = new ConcurrentHashMap<>();
    private static final Method ABSENT;

    static {
        Method marker;
        try {
            marker = Object.class.getMethod("toString");
        } catch (NoSuchMethodException e) {
            marker = null;
        }
        ABSENT = marker;
    }

    /**
     * Teleport an entity, preferring Paper/Folia's region-safe {@code teleportAsync}
     * when available and falling back to the synchronous Bukkit teleport otherwise.
     * Must be called on the entity's owning region thread.
     *
     * <p>Returns a future that completes once the teleport has actually landed.
     * This matters for anything that must run <em>after</em> the move (e.g. setting
     * velocity for a throw): {@code teleportAsync} finishes a tick later and resets
     * the entity's velocity, so callers must wait for completion before applying it.
     */
    public static CompletableFuture<Boolean> teleport(Entity entity, Location location) {
        Method async = ASYNC_CACHE.computeIfAbsent(entity.getClass(), clazz -> {
            try {
                return clazz.getMethod("teleportAsync", Location.class);
            } catch (NoSuchMethodException e) {
                return ABSENT; // Spigot: no teleportAsync
            }
        });

        if (async != null && async != ABSENT) {
            try {
                Object res = async.invoke(entity, location);
                if (res instanceof CompletableFuture) {
                    @SuppressWarnings("unchecked")
                    CompletableFuture<Boolean> future = (CompletableFuture<Boolean>) res;
                    return future;
                }
            } catch (Throwable t) {
                // Fall through to synchronous teleport.
            }
        }
        try {
            boolean ok = entity.teleport(location);
            return CompletableFuture.completedFuture(ok);
        } catch (Throwable ignored) {
            return CompletableFuture.completedFuture(false);
        }
    }

    public static String displayName(Entity entity) {
        if (entity instanceof Player) {
            return ((Player) entity).getName();
        }
        return prettify(entity.getType().name());
    }

    private static String prettify(String raw) {
        String[] parts = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
