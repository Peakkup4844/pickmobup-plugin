package com.pickmobup;

import com.pickmobup.carry.CarryManager;
import com.pickmobup.command.PickMobUpCommand;
import com.pickmobup.config.Lang;
import com.pickmobup.config.PluginConfig;
import com.pickmobup.listener.CleanupListener;
import com.pickmobup.listener.InteractListener;
import com.pickmobup.listener.SneakListener;
import com.pickmobup.message.MessageService;
import com.tcoded.folialib.FoliaLib;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class PickMobUpPlugin extends JavaPlugin {

    private FoliaLib foliaLib;
    private PluginConfig pluginConfig;
    private Lang lang;
    private MessageService messages;
    private CarryManager carryManager;
    private boolean packetEventsAvailable;

    @Override
    public void onEnable() {
        this.foliaLib = new FoliaLib(this);

        saveDefaultConfig();
        this.pluginConfig = new PluginConfig(this);
        this.lang = new Lang(this);
        reloadAll();

        PluginManager pm = getServer().getPluginManager();
        this.packetEventsAvailable = pm.getPlugin("packetevents") != null || pm.getPlugin("PacketEvents") != null;

        this.messages = new MessageService(this);
        this.carryManager = new CarryManager(this);

        pm.registerEvents(new InteractListener(carryManager), this);
        pm.registerEvents(new SneakListener(carryManager), this);
        pm.registerEvents(new CleanupListener(carryManager), this);

        PickMobUpCommand command = new PickMobUpCommand(this);
        if (getCommand("pickmobup") != null) {
            getCommand("pickmobup").setExecutor(command);
            getCommand("pickmobup").setTabCompleter(command);
        }

        getLogger().info("PickMobUp เปิดใช้งานแล้ว (รองรับ Folia). PacketEvents=" + packetEventsAvailable
                + ", mount-mode=" + pluginConfig.mountMode());
    }

    @Override
    public void onDisable() {
        if (carryManager != null) {
            carryManager.dropAll();
        }
    }

    public void reloadAll() {
        reloadConfig();
        pluginConfig.load();
        lang.load();
    }

    public FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public PluginConfig config() {
        return pluginConfig;
    }

    public Lang lang() {
        return lang;
    }

    public MessageService messages() {
        return messages;
    }

    public boolean isPacketEventsAvailable() {
        return packetEventsAvailable;
    }
}
