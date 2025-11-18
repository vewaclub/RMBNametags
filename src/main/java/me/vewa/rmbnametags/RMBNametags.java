package me.vewa.rmbnametags;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.potion.PotionEffectType;

import org.bstats.bukkit.Metrics;
import me.clip.placeholderapi.PlaceholderAPI;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RMBNametags extends JavaPlugin implements Listener {

    private enum DisplayLocation {
        ACTIONBAR, SUBTITLE;

        static DisplayLocation fromConfig(String raw) {
            if (raw == null) return ACTIONBAR;
            return switch (raw.trim().toUpperCase(Locale.ROOT)) {
                case "SUBTITLE" -> SUBTITLE;
                default -> ACTIONBAR;
            };
        }
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");

    private Scoreboard board;
    private Team hiddenNamesTeam;

    private int displayTimeSeconds;
    private String nameFormat;
    private boolean respectInvisibility;            // dont show invisible players name
    private DisplayLocation displayLocation;        // where to show nickname

    private boolean HAS_PLACEHOLDER = false;

    private final Map<UUID, String> hiddenPlayers = new HashMap<>(); // hide player with custom text or not
    private @Nullable String hideNameFormat;
    private boolean showHiddenPlayers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        // bStats
        new Metrics(this, 22888);

        // events and reloadcommand
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("rmbnametags") != null) {
            getCommand("rmbnametags").setExecutor(new ReloadCommand(this));
        }

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            HAS_PLACEHOLDER = true;
            getLogger().info("PlaceholderAPI expansion registered!");
        }

        board = Bukkit.getScoreboardManager().getNewScoreboard();
        hiddenNamesTeam = board.registerNewTeam("hiddenNames");
        hiddenNamesTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        hiddenNamesTeam.setCanSeeFriendlyInvisibles(false);

        // hide online players nicknames
        for (Player player : Bukkit.getOnlinePlayers()) {
            hidePlayerName(player);
        }

        getLogger().info(() -> String.format(
                "Config loaded: display-time=%ds, format=%s, respect-invisibility=%s, display-location=%s, show-hidden=%s",
                displayTimeSeconds, nameFormat, respectInvisibility, displayLocation, showHiddenPlayers));
    }

    @Override
    public void onDisable() {
        board.getTeam(hiddenNamesTeam.getName()).unregister();
    }

    public void loadConfig() {
        reloadConfig();
        FileConfiguration cfg = getConfig();
        displayTimeSeconds   = Math.max(0, cfg.getInt("display-time", 3));
        nameFormat           = cfg.getString("name-format", "&6{PLAYER_NAME}");
        hideNameFormat       = cfg.getString("hide-name-format", "&a&lUnknown");
        respectInvisibility  = cfg.getBoolean("respect-invisibility", true);
        displayLocation      = DisplayLocation.fromConfig(cfg.getString("display-location", "actionbar"));
        showHiddenPlayers    = cfg.getBoolean("show-hidden-players", true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        hidePlayerName(event.getPlayer());
    }

    private void hidePlayerName(Player player) {
        if (!hiddenNamesTeam.hasEntry(player.getName())) {
            hiddenNamesTeam.addEntry(player.getName());
        }
        player.setScoreboard(board);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Player clickedPlayer)) return;

        Player viewer = event.getPlayer();

        if (isPlayerHidden(clickedPlayer)) {
            if (!showHiddenPlayers || (respectInvisibility && isEffectivelyInvisible(clickedPlayer))) {
                return;
            }
            showHiddenPlayerName(viewer, clickedPlayer);
            return;
        }

        if (respectInvisibility && isEffectivelyInvisible(clickedPlayer) || viewer.isSneaking()) return;

        showConfigured(viewer, clickedPlayer);
    }


    private boolean isEffectivelyInvisible(Player player) {
        return player.hasPotionEffect(PotionEffectType.INVISIBILITY) || player.isInvisible();
    }

    private void showHiddenPlayerName(Player viewer, Player target) {
        String customName = getHiddenPlayerName(target);
        String displayText;

        if (customName != null) {
            // кастомное имя
            displayText = colorize(customName);
        } else {
            // формат из конфига
            if (hideNameFormat == null || hideNameFormat.isEmpty()) {
                return; // Не показываем ничего, если формат не задан
            }
            displayText = colorize(hideNameFormat.replace("{PLAYER_NAME}", target.getName()));

            if (HAS_PLACEHOLDER) {
                displayText = PlaceholderAPI.setPlaceholders(target, displayText);
                displayText = PlaceholderAPI.setRelationalPlaceholders(viewer, target, displayText);
            }
        }

        showText(viewer, displayText);
    }

    // show nickname according to display-location
    private void showConfigured(Player viewer, Player target) {
        String formatted = colorize(nameFormat.replace("{PLAYER_NAME}", target.getName()));

        if (HAS_PLACEHOLDER) {
            formatted = PlaceholderAPI.setPlaceholders(target, formatted); // support Placeholders
            formatted = PlaceholderAPI.setRelationalPlaceholders(viewer,target,formatted); // and rel placeholders
        }

        showText(viewer, formatted);
    }


    // method for show text
    private void showText(Player viewer, String text) {
        switch (displayLocation) {
            case SUBTITLE:
                int stay = Math.max(1, displayTimeSeconds) * 20;
                viewer.sendTitle("", text, 0, stay, 10);
                break;

            case ACTIONBAR:
            default:
                viewer.sendActionBar(text);
                if (displayTimeSeconds > 0) {
                    new BukkitRunnable() {
                        @Override public void run() { viewer.sendActionBar(""); }
                    }.runTaskLater(this, displayTimeSeconds * 20L);
                }
                break;
        }
    }


    // methods for hidden players - now not usage
    public boolean hidePlayer(Player player) {
        return hidePlayer(player, null);
    }

    public boolean hidePlayer(Player player, String customName) {
        // Null means using format from config
        hiddenPlayers.put(player.getUniqueId(), Objects.requireNonNullElse(customName, ""));
        return true;
    }

    public boolean showPlayer(Player player) {
        return hiddenPlayers.remove(player.getUniqueId()) != null;
    }

    public boolean isPlayerHidden(Player player) {
        return hiddenPlayers.containsKey(player.getUniqueId());
    }

    public String getHiddenPlayerName(Player player) {
        String customName = hiddenPlayers.get(player.getUniqueId());
        // Если пустая строка - возвращаем null (использовать формат из конфига)
        return (customName != null && !customName.isEmpty()) ? customName : null;
    }

    // nor usage for now
    public Map<UUID, String> getHiddenPlayers() {
        return new HashMap<>(hiddenPlayers);
    }

    // HEX support
    private String colorize(String input) {
        Matcher m = HEX_PATTERN.matcher(input);
        StringBuilder buf = new StringBuilder();
        while (m.find()) {
            String color = m.group(1);
            m.appendReplacement(buf, ChatColor.of("#" + color).toString());
        }
        m.appendTail(buf);
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', buf.toString());
    }
}
