package me.vewa.rmbnametags;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReloadCommand implements CommandExecutor, TabCompleter {
    private final RMBNametags plugin;

    public ReloadCommand(RMBNametags plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("Messages.Reload")));
            plugin.loadConfig();
        } else commandSender.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("Messages.Not-Reload")));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("reload");
        }
        return List.of();
    }
}