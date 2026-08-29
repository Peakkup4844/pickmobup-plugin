package com.pickmobup.message;

import com.pickmobup.PickMobUpPlugin;
import com.pickmobup.config.Lang;
import com.pickmobup.config.PluginConfig;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import org.bukkit.Bukkit;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Routes feedback messages and the charge meter to the channel chosen in
 * config.yml (chat / action bar / boss bar / none).
 *
 * <p>Boss bars are stateful, so they are tracked per player:
 * <ul>
 *   <li>a single live <b>charge</b> bar while the player holds sneak, and</li>
 *   <li>a transient <b>feedback</b> bar that auto-hides after a configurable delay.</li>
 * </ul>
 * All boss bar mutations happen on the player's region thread (callers already
 * run on it), keeping this safe on Folia.
 */
public class MessageService {

    private final PickMobUpPlugin plugin;
    private final PlatformScheduler scheduler;

    private final Map<UUID, BossBar> chargeBars = new ConcurrentHashMap<>();
    private final Map<UUID, FeedbackBar> feedbackBars = new ConcurrentHashMap<>();

    private static final class FeedbackBar {
        final BossBar bar;
        WrappedTask removalTask;

        FeedbackBar(BossBar bar) {
            this.bar = bar;
        }
    }

    public MessageService(PickMobUpPlugin plugin) {
        this.plugin = plugin;
        this.scheduler = plugin.getFoliaLib().getScheduler();
    }

    private PluginConfig cfg() {
        return plugin.config();
    }

    private Lang lang() {
        return plugin.lang();
    }

    // ---- feedback messages ----------------------------------------------

    /** Show a one-off feedback message on the configured output channel. */
    public void feedback(Player player, String key, String... replacements) {
        switch (cfg().messageOutput()) {
            case CHAT:
                lang().send(player, key, replacements);
                break;
            case ACTIONBAR:
                lang().actionBar(player, key, replacements);
                break;
            case BOSSBAR:
                showFeedbackBar(player, lang().render(key, replacements));
                break;
            case NONE:
            default:
                break;
        }
    }

    private void showFeedbackBar(Player player, String title) {
        UUID id = player.getUniqueId();
        FeedbackBar fb = feedbackBars.get(id);
        if (fb == null) {
            BossBar bar = Bukkit.createBossBar(title, cfg().bossbarColor(), cfg().bossbarStyle());
            bar.addPlayer(player);
            fb = new FeedbackBar(bar);
            feedbackBars.put(id, fb);
        } else if (fb.removalTask != null) {
            fb.removalTask.cancel();
            fb.removalTask = null;
        }

        fb.bar.setColor(cfg().bossbarColor());
        fb.bar.setStyle(cfg().bossbarStyle());
        fb.bar.setTitle(title);
        fb.bar.setProgress(1.0);
        fb.bar.setVisible(true);

        final FeedbackBar ref = fb;
        ref.removalTask = scheduler.runAtEntityLater(player, () -> {
            if (feedbackBars.remove(id, ref)) {
                removeBar(ref.bar);
            }
        }, Math.max(1L, cfg().bossbarFeedbackTicks()));
    }

    // ---- charge meter ----------------------------------------------------

    /** Update the live charge meter (called every charge tick). */
    public void charge(Player player, double fraction, int percent) {
        switch (cfg().chargeOutput()) {
            case BOSSBAR:
                BossBar bar = chargeBars.computeIfAbsent(player.getUniqueId(), id -> {
                    BossBar b = Bukkit.createBossBar(" ", cfg().bossbarColor(), cfg().bossbarStyle());
                    b.addPlayer(player);
                    return b;
                });
                bar.setColor(cfg().bossbarColor());
                bar.setStyle(cfg().bossbarStyle());
                bar.setProgress(clamp01(fraction));
                bar.setTitle(lang().render("charge-bar",
                        "%bar%", lang().buildBar(fraction, cfg().barSegments()),
                        "%percent%", String.valueOf(percent)));
                bar.setVisible(true);
                break;
            case NONE:
                break;
            case CHAT:
            case ACTIONBAR:
            default:
                // A live meter only makes sense on the action bar; CHAT falls back to it.
                lang().actionBar(player, "charge-bar",
                        "%bar%", lang().buildBar(fraction, cfg().barSegments()),
                        "%percent%", String.valueOf(percent));
                break;
        }
    }

    /** Remove the live charge bar (on throw / place / abort). */
    public void clearCharge(Player player) {
        BossBar bar = chargeBars.remove(player.getUniqueId());
        if (bar != null) {
            removeBar(bar);
        }
    }

    // ---- cleanup ---------------------------------------------------------

    /** Remove every boss bar belonging to a single player. */
    public void clear(Player player) {
        clearCharge(player);
        FeedbackBar fb = feedbackBars.remove(player.getUniqueId());
        if (fb != null) {
            if (fb.removalTask != null) {
                fb.removalTask.cancel();
            }
            removeBar(fb.bar);
        }
    }

    /** Remove all tracked boss bars (plugin disable). */
    public void clearAll() {
        for (BossBar bar : chargeBars.values()) {
            removeBar(bar);
        }
        chargeBars.clear();
        for (FeedbackBar fb : feedbackBars.values()) {
            if (fb.removalTask != null) {
                fb.removalTask.cancel();
            }
            removeBar(fb.bar);
        }
        feedbackBars.clear();
    }

    private void removeBar(BossBar bar) {
        try {
            bar.removeAll();
            bar.setVisible(false);
        } catch (Throwable ignored) {
            // Best effort.
        }
    }

    private static double clamp01(double v) {
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }
}
