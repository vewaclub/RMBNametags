package me.vewa.rmbnametags;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Fixes tamed wolves having their nametags hidden by inheriting
 * their owner's scoreboard team (which has NAME_TAG_VISIBILITY=NEVER).
 */
public class WolfNametagManager implements Listener {

    private static final String WOLF_TEAM_NAME = "rmb_wolves";

    private final JavaPlugin plugin;
    private final Scoreboard board;
    private Team wolfTeam;

    public WolfNametagManager(JavaPlugin plugin, Scoreboard board) {
        this.plugin = plugin;
        this.board  = board;
        setupTeam();
    }

    private void setupTeam() {
        wolfTeam = board.getTeam(WOLF_TEAM_NAME);
        if (wolfTeam == null) {
            wolfTeam = board.registerNewTeam(WOLF_TEAM_NAME);
        }
        wolfTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        wolfTeam.setCanSeeFriendlyInvisibles(false);
    }

    public void cleanup() {
        Team t = board.getTeam(WOLF_TEAM_NAME);
        if (t != null) t.unregister();
    }

    private void registerWolf(Wolf wolf) {
        // only tamed wolves with a named nametag matter here.
        if (!wolf.isTamed()) return;
        if (wolf.customName() == null && (wolf.getCustomName() == null || wolf.getCustomName().isEmpty())) return;

        String uuidEntry = wolf.getUniqueId().toString();
        if (!wolfTeam.hasEntry(uuidEntry)) {
            wolfTeam.addEntry(uuidEntry);
        }
    }

    private void unregisterWolf(Wolf wolf) {
        wolfTeam.removeEntry(wolf.getUniqueId().toString());
    }

    /**
     * Called when a wolf is successfully tamed
     * One tick so the wolf's owner and custom name are fully set
     * before checking isTamed() / getCustomName()
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> registerWolf(wolf), 1L);
    }

    /**
     * Catch already tamed wolves that load from disk with existing nametags
     * EntitiesLoadEvent called after the chunk's entities are fully initialised
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        for (var entity : event.getEntities()) {
            if (entity instanceof Wolf wolf) {
                registerWolf(wolf);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        for (var entity : event.getChunk().getEntities()) {
            if (entity instanceof Wolf wolf) {
                registerWolf(wolf);
            }
        }
    }

    /**
     * Called when a player uses any item on an entity
     * We check if the item is a nametag with a name, and the entity is a dog
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerNameTag(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Wolf wolf)) return;
        if (!wolf.isTamed()) return;

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (item.getType() != Material.NAME_TAG) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> registerWolf(wolf), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Wolf wolf) {
            unregisterWolf(wolf);
        }
    }

    public void reRegisterWolvesOwnedBy(Player owner) {
        // Scan all loaded worlds for wolves owned by this player
        for (var world : Bukkit.getWorlds()) {
            for (var entity : world.getEntitiesByClass(Wolf.class)) {
                if (owner.getUniqueId().equals(entity.getOwnerUniqueId())) {
                    registerWolf(entity);
                }
            }
        }
    }
}