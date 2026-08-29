package com.pickmobup.config;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Loads lang.yml and renders messages. Uses legacy '&' colour codes and sends
 * action bars through the BungeeCord chat API which is available on both Spigot
 * and Paper across all supported versions.
 */
public class Lang {

    private final JavaPlugin plugin;
    private FileConfiguration messages;
    private String prefix = "";

    public Lang(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "lang.yml");
        if (!file.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(file);
        this.prefix = raw("prefix");
    }

    public String raw(String key) {
        return messages.getString(key, key);
    }

    /** Apply replacements (pairs of placeholder, value) to a template. */
    private String apply(String template, String... replacements) {
        String out = template;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            out = out.replace(replacements[i], replacements[i + 1]);
        }
        return out;
    }

    private String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /** Render a message (with '&' colours resolved) without the prefix. */
    public String render(String key, String... replacements) {
        return color(apply(raw(key), replacements));
    }

    public void send(Player player, String key, String... replacements) {
        String msg = apply(prefix + raw(key), replacements);
        player.sendMessage(color(msg));
    }

    public void actionBar(Player player, String key, String... replacements) {
        String msg = apply(raw(key), replacements);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(color(msg)));
    }

    /** Build a charge bar string (still containing '&' codes) from a 0..1 fraction. */
    public String buildBar(double fraction, int segments) {
        int filled = (int) Math.round(Math.max(0.0, Math.min(1.0, fraction)) * segments);
        String symbol = raw("charge-bar-symbol");
        String filledColor = raw("charge-bar-filled-color");
        String emptyColor = raw("charge-bar-empty-color");

        StringBuilder sb = new StringBuilder();
        sb.append(filledColor);
        for (int i = 0; i < filled; i++) {
            sb.append(symbol);
        }
        sb.append(emptyColor);
        for (int i = filled; i < segments; i++) {
            sb.append(symbol);
        }
        return sb.toString();
    }
}
