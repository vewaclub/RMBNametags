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

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RMBNametags extends JavaPlugin implements Listener {

    private enum DisplayLocation {
        ACTIONBAR, SUBTITLE;

        static DisplayLocation fromConfig(String raw) {
            if (raw == null) return ACTIONBAR;
            switch (raw.trim().toUpperCase(Locale.ROOT)) {
                case "SUBTITLE": return SUBTITLE;
                case "ACTIONBAR":
                default: return ACTIONBAR;
            }
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
                "Config loaded: display-time=%ds, format=%s, respect-invisibility=%s, display-location=%s",
                displayTimeSeconds, nameFormat, respectInvisibility, displayLocation));
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
        respectInvisibility  = cfg.getBoolean("respect-invisibility", true);
        displayLocation      = DisplayLocation.fromConfig(cfg.getString("display-location", "actionbar"));
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

        if (respectInvisibility && isEffectivelyInvisible(clickedPlayer) || event.getPlayer().isSneaking()) return;

        showConfigured(event.getPlayer(), clickedPlayer);
    }


    private boolean isEffectivelyInvisible(Player player) {
        return player.hasPotionEffect(PotionEffectType.INVISIBILITY) || player.isInvisible();
    }

    // show nickname according to display-location
    private void showConfigured(Player viewer, Player target) {
        String formatted = colorize(nameFormat.replace("{PLAYER_NAME}", target.getName()));

        if (HAS_PLACEHOLDER) {
            formatted = PlaceholderAPI.setPlaceholders(target, formatted); // support Placeholders
            formatted = PlaceholderAPI.setRelationalPlaceholders(viewer,target,formatted);
        }

        switch (displayLocation) {
            case SUBTITLE:
                int stay = Math.max(1, displayTimeSeconds) * 20;
                viewer.sendTitle("", formatted, 0, stay, 10);
                break;

            case ACTIONBAR:
            default:

                viewer.sendActionBar(formatted);
                if (displayTimeSeconds > 0) {
                    new BukkitRunnable() {
                        @Override public void run() { viewer.sendActionBar(""); }
                    }.runTaskLater(this, displayTimeSeconds * 20L);
                }
                break;
        }
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
