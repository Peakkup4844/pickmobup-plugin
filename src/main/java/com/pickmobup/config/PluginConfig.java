package com.pickmobup.config;

import com.pickmobup.message.MessageChannel;
import com.pickmobup.mount.MountMode;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Typed view over config.yml. Reloaded via {@link #load()}.
 */
public class PluginConfig {

    public enum FilterMode {WHITELIST, BLACKLIST}

    private final JavaPlugin plugin;

    private MountMode mountMode;
    private boolean disableAi;
    private boolean invulnerable;
    private FilterMode filterMode;
    private final Set<EntityType> filterList = new HashSet<>();
    private boolean allowCarryPlayers;
    private final Set<String> allowedWorlds = new HashSet<>();
    private long tapThresholdMs;
    private boolean throwEnabled;
    private double maxPowerBlocks;
    private double velocityPerBlock;
    private double upwardBoost;
    private boolean slowFalling;
    private int chargeCycleTicks;
    private int chargeUpdateTicks;
    private int barSegments;
    private int slowFallingGroundGraceTicks;
    private int slowFallingMaxTicks;
    private String soundPickup;
    private String soundPlace;
    private String soundThrow;
    private MessageChannel messageOutput;
    private MessageChannel chargeOutput;
    private BarColor bossbarColor;
    private BarStyle bossbarStyle;
    private long bossbarFeedbackTicks;
    private boolean debug;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        FileConfiguration c = plugin.getConfig();

        this.mountMode = parseEnum(c.getString("mount-mode", "PASSENGER"), MountMode.class, MountMode.PASSENGER);
        this.disableAi = c.getBoolean("disable-ai-while-carried", true);
        this.invulnerable = c.getBoolean("invulnerable-while-carried", true);

        this.filterMode = parseEnum(c.getString("entity-filter.mode", "BLACKLIST"), FilterMode.class, FilterMode.BLACKLIST);
        this.filterList.clear();
        for (String raw : c.getStringList("entity-filter.list")) {
            EntityType type = parseEntityType(raw);
            if (type != null) {
                filterList.add(type);
            }
        }

        this.allowCarryPlayers = c.getBoolean("allow-carry-players", false);

        this.allowedWorlds.clear();
        for (String w : c.getStringList("allowed-worlds")) {
            allowedWorlds.add(w.toLowerCase(Locale.ROOT));
        }

        this.tapThresholdMs = Math.max(0L, c.getLong("controls.sneak-tap-threshold-ms", 250L));

        this.throwEnabled = c.getBoolean("throw.enabled", true);
        this.maxPowerBlocks = Math.max(0.0, c.getDouble("throw.max-power-blocks", 30.0));
        this.velocityPerBlock = Math.max(0.0, c.getDouble("throw.velocity-per-block", 0.12));
        this.upwardBoost = c.getDouble("throw.upward-boost", 0.2);
        this.slowFalling = c.getBoolean("throw.slow-falling", true);
        this.chargeCycleTicks = Math.max(2, c.getInt("throw.charge.cycle-ticks", 40));
        this.chargeUpdateTicks = Math.max(1, c.getInt("throw.charge.update-ticks", 2));
        this.barSegments = Math.max(1, c.getInt("throw.charge.bar-segments", 30));
        this.slowFallingGroundGraceTicks = Math.max(0, c.getInt("slow-falling-ground-grace-ticks", 10));
        this.slowFallingMaxTicks = Math.max(20, c.getInt("throw.slow-falling-max-ticks", 600));

        this.soundPickup = emptyToNull(c.getString("sounds.pickup", ""));
        this.soundPlace = emptyToNull(c.getString("sounds.place", ""));
        this.soundThrow = emptyToNull(c.getString("sounds.throw", ""));

        this.messageOutput = parseEnum(c.getString("messages.output", "CHAT"), MessageChannel.class, MessageChannel.CHAT);
        this.chargeOutput = parseEnum(c.getString("messages.charge-output", "ACTIONBAR"), MessageChannel.class, MessageChannel.ACTIONBAR);
        this.bossbarColor = parseEnum(c.getString("messages.bossbar.color", "YELLOW"), BarColor.class, BarColor.YELLOW);
        this.bossbarStyle = parseEnum(c.getString("messages.bossbar.style", "SOLID"), BarStyle.class, BarStyle.SOLID);
        this.bossbarFeedbackTicks = Math.max(1L, c.getLong("messages.bossbar.feedback-duration-ticks", 40L));

        this.debug = c.getBoolean("debug", false);
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> type, E def) {
        if (value == null) {
            return def;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("ค่าไม่ถูกต้อง '" + value + "' สำหรับ " + type.getSimpleName() + ", ใช้ค่าเริ่มต้น " + def);
            return def;
        }
    }

    private EntityType parseEntityType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return EntityType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("ไม่รู้จัก EntityType ใน entity-filter.list: " + raw);
            return null;
        }
    }

    private String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    // ---- accessors -------------------------------------------------------

    public MountMode mountMode() {
        return mountMode;
    }

    public boolean disableAi() {
        return disableAi;
    }

    public boolean invulnerable() {
        return invulnerable;
    }

    public boolean allowCarryPlayers() {
        return allowCarryPlayers;
    }

    public long tapThresholdMs() {
        return tapThresholdMs;
    }

    public boolean throwEnabled() {
        return throwEnabled;
    }

    public double maxPowerBlocks() {
        return maxPowerBlocks;
    }

    public double velocityPerBlock() {
        return velocityPerBlock;
    }

    public double upwardBoost() {
        return upwardBoost;
    }

    public boolean slowFalling() {
        return slowFalling;
    }

    public int chargeCycleTicks() {
        return chargeCycleTicks;
    }

    public int chargeUpdateTicks() {
        return chargeUpdateTicks;
    }

    public int barSegments() {
        return barSegments;
    }

    public int slowFallingGroundGraceTicks() {
        return slowFallingGroundGraceTicks;
    }

    public int slowFallingMaxTicks() {
        return slowFallingMaxTicks;
    }

    public MessageChannel messageOutput() {
        return messageOutput;
    }

    public MessageChannel chargeOutput() {
        return chargeOutput;
    }

    public BarColor bossbarColor() {
        return bossbarColor;
    }

    public BarStyle bossbarStyle() {
        return bossbarStyle;
    }

    public long bossbarFeedbackTicks() {
        return bossbarFeedbackTicks;
    }

    public boolean debug() {
        return debug;
    }

    public boolean isWorldAllowed(String worldName) {
        return allowedWorlds.isEmpty() || allowedWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    public boolean isTypeAllowed(EntityType type) {
        boolean inList = filterList.contains(type);
        return filterMode == FilterMode.WHITELIST ? inList : !inList;
    }

    public void playSound(Player player, String which) {
        String key;
        switch (which) {
            case "pickup":
                key = soundPickup;
                break;
            case "place":
                key = soundPlace;
                break;
            case "throw":
                key = soundThrow;
                break;
            default:
                key = null;
        }
        if (key == null) {
            return;
        }
        try {
            Location loc = player.getLocation();
            player.playSound(loc, key, 1.0f, 1.0f);
        } catch (Throwable ignored) {
            // Invalid sound key; silently skip.
        }
    }
}
