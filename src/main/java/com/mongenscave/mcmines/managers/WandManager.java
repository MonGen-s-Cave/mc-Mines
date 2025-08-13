package com.mongenscave.mcmines.managers;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.identifiers.SelectionMode;
import com.mongenscave.mcmines.data.MenuController;
import com.mongenscave.mcmines.gui.models.MineEditorMenu;
import com.mongenscave.mcmines.identifiers.keys.MessageKeys;
import com.mongenscave.mcmines.models.Mine;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("all")
public final class WandManager implements Listener {

    public static final String WAND_PDC_KEY = "selector_wand";

    private final McMines plugin = McMines.getInstance();
    private final NamespacedKey wandKey = new NamespacedKey(plugin, WAND_PDC_KEY);
    private final NamespacedKey ownerKey = new NamespacedKey(plugin, "selector_owner");

    private final MineManager mineManager = plugin.getMineManager();

    private final Map<UUID, Session> sessions = new HashMap<>();
    private final Map<UUID, MyScheduledTask> previewTasks = new HashMap<>();

    private record Session(UUID playerId,
                           String mineName,
                           SelectionMode mode,
                           boolean reopenEditor,
                           @Nullable Location pos1,
                           @Nullable Location pos2) {
        Session withPos1(Location l) { return new Session(playerId, mineName, mode, reopenEditor, l, pos2); }
        Session withPos2(Location l) { return new Session(playerId, mineName, mode, reopenEditor, pos1, l); }
    }

    public void startSession(@NotNull Player player, @NotNull Mine mine, @NotNull SelectionMode mode, boolean reopenEditor) {
        cancelSession(player, false);
        sessions.put(player.getUniqueId(), new Session(player.getUniqueId(), mine.getName(), mode, reopenEditor, null, null));
        giveWand(player);
        switch (mode) {
            case MINE_AREA -> player.sendMessage(MessageKeys.WAND_START_MINE.getMessage());
            case ENTRANCE_AREA -> player.sendMessage(MessageKeys.WAND_START_ENTRANCE.getMessage());
        }
        player.sendMessage(MessageKeys.WAND_HINT_SAVE.getMessage());
    }

    public void cancelSession(@NotNull Player player, boolean notify) {
        stopPreview(player.getUniqueId());
        sessions.remove(player.getUniqueId());
        removeWand(player);
        if (notify) player.sendMessage(MessageKeys.WAND_CANCELLED.getMessage());
    }

    private void giveWand(@NotNull Player player) {
        removeWand(player);
        ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = wand.getItemMeta();
        meta.setDisplayName(MessageKeys.WAND_ITEM_NAME.getMessage());
        meta.setLore(List.of(
                MessageKeys.WAND_ITEM_LORE_1.getMessage(),
                MessageKeys.WAND_ITEM_LORE_2.getMessage(),
                MessageKeys.WAND_ITEM_LORE_3.getMessage()
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        wand.setItemMeta(meta);

        PlayerInventory inv = player.getInventory();
        if (inv.firstEmpty() != -1) {
            inv.addItem(wand);
        } else if (inv.getItemInMainHand().getType() == Material.AIR) {
            inv.setItemInMainHand(wand);
        } else {
            player.sendMessage(MessageKeys.WAND_NO_SPACE.getMessage());
            sessions.remove(player.getUniqueId());
        }
    }

    private void removeWand(@NotNull Player player) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack it = inv.getItem(i);
            if (isWandOfPlayer(it, player)) inv.clear(i);
        }
    }

    private boolean isWand(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte tag = meta.getPersistentDataContainer().get(wandKey, PersistentDataType.BYTE);
        return tag != null && tag == (byte) 1;
    }

    private boolean isWandOfPlayer(@Nullable ItemStack item, @NotNull Player player) {
        if (!isWand(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String owner = meta.getPersistentDataContainer().get(ownerKey, PersistentDataType.STRING);
        return owner != null && owner.equals(player.getUniqueId().toString());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        if (!isWandOfPlayer(event.getItem(), player)) return;

        Session session = sessions.get(player.getUniqueId());
        if (session == null) {
            removeWand(player);
            return;
        }

        Action action = event.getAction();

        if (player.isSneaking() && (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR)) {
            event.setCancelled(true);
            if (session.pos1 == null || session.pos2 == null) {
                player.sendMessage(MessageKeys.WAND_NEED_BOTH.getMessage());
                return;
            }
            applyAndFinish(player, session);
            return;
        }

        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) return;

        if (event.getClickedBlock() == null) return;
        event.setCancelled(true);

        if (action == Action.LEFT_CLICK_BLOCK) {
            sessions.put(player.getUniqueId(), session.withPos1(event.getClickedBlock().getLocation()));
            player.sendMessage(MessageKeys.WAND_POS1_SET.getMessage());
        } else {
            sessions.put(player.getUniqueId(), session.withPos2(event.getClickedBlock().getLocation()));
            player.sendMessage(MessageKeys.WAND_POS2_SET.getMessage());
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
        startPreviewIfNeeded(player.getUniqueId());
        player.sendMessage(MessageKeys.WAND_HINT_SAVE.getMessage());
    }

    @EventHandler(ignoreCancelled = true)
    public void onWandDrop(PlayerDropItemEvent event) {
        if (isWandOfPlayer(event.getItemDrop().getItemStack(), event.getPlayer())) {
            event.setCancelled(true);
            cancelSession(event.getPlayer(), true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cancelSession(event.getPlayer(), false);
    }

    private void startPreviewIfNeeded(@NotNull UUID playerId) {
        if (previewTasks.containsKey(playerId)) return;
        MyScheduledTask task = plugin.getScheduler().runTaskTimer(() -> {
            Session s = sessions.get(playerId);
            if (s == null) {
                stopPreview(playerId);
                return;
            }
            if (s.pos1 == null || s.pos2 == null) return;
            Player p = plugin.getServer().getPlayer(playerId);
            if (p == null || !p.isOnline()) {
                stopPreview(playerId);
                return;
            }
            drawCuboidFrameFor(p, s.pos1, s.pos2, 0.5);
        }, 0L, 5L);
        previewTasks.put(playerId, task);
    }

    private void stopPreview(@NotNull UUID playerId) {
        MyScheduledTask t = previewTasks.remove(playerId);
        if (t != null) t.cancel();
    }

    private void applyAndFinish(@NotNull Player player, @NotNull Session session) {
        stopPreview(player.getUniqueId());
        Mine mine = mineManager.getMine(session.mineName);
        if (mine != null) {
            switch (session.mode) {
                case MINE_AREA -> {
                    mine.setMineAreaPos1(session.pos1);
                    mine.setMineAreaPos2(session.pos2);
                    mineManager.updateMine(mine);
                    player.sendMessage(MessageKeys.WAND_DONE_MINE.getMessage());
                }
                case ENTRANCE_AREA -> {
                    mine.setEntranceAreaPos1(session.pos1);
                    mine.setEntranceAreaPos2(session.pos2);
                    mineManager.updateMine(mine);
                    player.sendMessage(MessageKeys.WAND_DONE_ENTRANCE.getMessage());
                }
            }
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.0f);
        removeWand(player);
        sessions.remove(player.getUniqueId());

        if (session.reopenEditor && mine != null) {
            var controller = MenuController.getMenuUtils(player);
            new MineEditorMenu(controller, mine).open();
        }
    }

    private void drawCuboidFrameFor(@NotNull Player player,
                                    @NotNull Location a,
                                    @NotNull Location b,
                                    double step) {
        World world = a.getWorld();
        if (world == null || b.getWorld() == null || !world.equals(b.getWorld())) return;

        int minX = Math.min(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        int maxX = Math.max(a.getBlockX(), b.getBlockX()) + 1;
        int maxY = Math.max(a.getBlockY(), b.getBlockY()) + 1;
        int maxZ = Math.max(a.getBlockZ(), b.getBlockZ()) + 1;

        Location c000 = new Location(world, minX, minY, minZ);
        Location c100 = new Location(world, maxX, minY, minZ);
        Location c010 = new Location(world, minX, maxY, minZ);
        Location c110 = new Location(world, maxX, maxY, minZ);
        Location c001 = new Location(world, minX, minY, maxZ);
        Location c101 = new Location(world, maxX, minY, maxZ);
        Location c011 = new Location(world, minX, maxY, maxZ);
        Location c111 = new Location(world, maxX, maxY, maxZ);

        spawnLine(player, c000, c100, step);
        spawnLine(player, c000, c010, step);
        spawnLine(player, c000, c001, step);

        spawnLine(player, c111, c101, step);
        spawnLine(player, c111, c110, step);
        spawnLine(player, c111, c011, step);

        spawnLine(player, c100, c110, step);
        spawnLine(player, c100, c101, step);

        spawnLine(player, c010, c110, step);
        spawnLine(player, c010, c011, step);

        spawnLine(player, c001, c011, step);
        spawnLine(player, c001, c101, step);
    }

    private void spawnLine(@NotNull Player player,
                           @NotNull Location from,
                           @NotNull Location to,
                           double step) {
        Vector start = from.toVector();
        Vector end = to.toVector();
        Vector diff = end.clone().subtract(start);
        double length = diff.length();
        if (length == 0) return;

        Vector dir = diff.multiply(1.0 / length);
        int points = Math.max(2, (int) Math.ceil(length / step));

        for (int i = 0; i <= points; i++) {
            Vector point = start.clone().add(dir.clone().multiply(i * step));
            player.spawnParticle(Particle.SCRAPE, point.getX(), point.getY(), point.getZ(), 1, 0, 0, 0, 0);
        }
    }
}