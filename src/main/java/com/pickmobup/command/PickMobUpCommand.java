package com.pickmobup.command;

import com.pickmobup.PickMobUpPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class PickMobUpCommand implements CommandExecutor, TabCompleter {

    private final PickMobUpPlugin plugin;

    public PickMobUpCommand(PickMobUpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("pickmobup.admin")) {
                sendKey(sender, "no-permission");
                return true;
            }
            plugin.reloadAll();
            sendKey(sender, "reload-success");
            return true;
        }
        sendKey(sender, "unknown-command");
        return true;
    }

    private void sendKey(CommandSender sender, String key) {
        if (sender instanceof Player) {
            plugin.lang().send((Player) sender, key);
        } else {
            String prefix = plugin.lang().raw("prefix");
            sender.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix + plugin.lang().raw(key)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("pickmobup.admin")) {
            return Collections.singletonList("reload");
        }
        return Collections.emptyList();
    }
}
