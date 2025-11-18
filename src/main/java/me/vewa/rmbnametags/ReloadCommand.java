package me.vewa.rmbnametags;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReloadCommand implements CommandExecutor, TabCompleter {
    private final RMBNametags plugin;

    public ReloadCommand(RMBNametags plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("rmbnametags.reload")) {
                    sendMessage(sender, "No-Permission");
                    return true;
                }
                plugin.loadConfig();
                sendMessage(sender, "Reload");
                break;

            case "hide":
                handleHideShow(sender, args, true);
                break;

            case "show":
                handleHideShow(sender, args, false);
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void handleHideShow(CommandSender sender, String[] args, boolean hide) {
        if (!sender.hasPermission("rmbnametags.hide-show")) {
            sendMessage(sender, "No-Permission");
            return;
        }

        Player targetPlayer;
        String customName = null;

        if (args.length > 1) {
            // if player included
            if (!sender.hasPermission("rmbnametags.hide-show.other")) {
                sendMessage(sender, "No-Permission");
                return;
            }

            targetPlayer = Bukkit.getPlayer(args[1]);
            if (targetPlayer == null) {
                sendMessage(sender, "Player-Not-Found");
                return;
            }

            // check if custom name
            if (args.length > 2 && hide) {
                if (!sender.hasPermission("rmbnametags.hide.custom")) {
                    sendMessage(sender, "No-Permission");
                    return;
                }
                // build another text for custom name
                StringBuilder nameBuilder = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    if (!nameBuilder.isEmpty()) nameBuilder.append(" ");
                    nameBuilder.append(args[i]);
                }
                customName = nameBuilder.toString();
            }
        } else {
            // if without player - set himself
            if (!(sender instanceof Player)) {
                sendMessage(sender, "Console-Must-Specify-Player");
                return;
            }
            targetPlayer = (Player) sender;
        }

        // task
        boolean success;
        String messageKey;
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", targetPlayer.getName());

        if (hide) {
            success = plugin.hidePlayer(targetPlayer, customName);
            if (success) {
                if (customName != null) {
                    messageKey = (targetPlayer == sender) ? "Hide-Custom-Self" : "Hide-Custom-Other";
                    placeholders.put("%custom_name%", customName);
                } else {
                    messageKey = (targetPlayer == sender) ? "Hide-Self" : "Hide-Other";
                }
            } else {
                messageKey = "Already-Hidden";
            }
        } else {
            success = plugin.showPlayer(targetPlayer);
            messageKey = (targetPlayer == sender) ? "Show-Self" : "Show-Other";
            if (!success) {
                messageKey = "Already-Visible";
            }
        }

        sendMessage(sender, messageKey, placeholders);

        // Messages
        if (success && targetPlayer != sender) {
            if (hide) {
                if (customName != null) {
                    targetPlayer.sendMessage(colorize(plugin.getConfig().getString("Messages.Hide-Custom-Other-Target", "&aYour nickname is now hidden with custom name: %custom_name%").replace("%custom_name%", customName)));
                } else {
                    sendMessage(targetPlayer, "Hide-Self");
                }
            } else {
                sendMessage(targetPlayer, "Show-Self");
            }
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("reload");
            completions.add("hide");
            completions.add("show");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("hide") || args[0].equalsIgnoreCase("show"))) {
            if (sender.hasPermission("rmbnametags.hide-show.other")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .toList());
            }
        }
        // for args.length >= 3 just text for custom name

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }

    private void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, new HashMap<>());
    }

    private void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        String path = "Messages." + key;
        String message = plugin.getConfig().getString(path);
        if (message == null) {
            // if a message is null -> use default
            message = getDefaultMessage(key);
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace(entry.getKey(), entry.getValue());
        }
        sender.sendMessage(colorize(message));
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(colorize("&6=== RMBNametags Help ==="));
        sender.sendMessage(colorize("&e/rmbnametags reload &7- Reload config"));
        sender.sendMessage(colorize("&e/rmbnametags hide [player] [custom_name] &7- Hide nickname"));
        sender.sendMessage(colorize("&e/rmbnametags show [player] &7- Show nickname"));
        sender.sendMessage(colorize("&6Examples:"));
        sender.sendMessage(colorize("&7/rmbnametags hide &8- hide your nickname"));
        sender.sendMessage(colorize("&7/rmbnametags hide Mr_Milota &8- hide player's nickname"));
        sender.sendMessage(colorize("&7/rmbnametags hide Mr_Milota Admin &8- hide with custom name"));
    }

    private String getDefaultMessage(String key) {
        return switch (key) {
            case "No-Permission" -> "&cYou don't have permission to use this command!";
            case "Player-Not-Found" -> "&cPlayer not found or offline!";
            case "Console-Specify-Player" -> "&cConsole must specify a player!";
            case "Hide-Self" -> "&aYour nickname is now hidden";
            case "Hide-Other" -> "&aNickname of player %player% is now hidden";
            case "Show-Self" -> "&aYour nickname is now visible";
            case "Show-Other" -> "&aNickname of player %player% is now visible";
            case "Already-Hidden" -> "&cNickname of player %player% is already hidden";
            case "Already-Visible" -> "&cNickname of player %player% is already visible";
            case "Hide-Custom-Self" -> "&aYour nickname is now hidden with custom name: %custom_name%";
            case "Hide-Custom-Other" -> "&aNickname of player %player% is now hidden with custom name: %custom_name%";
            default -> "&cMessage not found: " + key;
        };
    }
}