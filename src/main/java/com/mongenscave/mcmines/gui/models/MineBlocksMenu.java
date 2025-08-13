package com.mongenscave.mcmines.gui.models;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.data.BlockData;
import com.mongenscave.mcmines.data.MenuController;
import com.mongenscave.mcmines.gui.Menu;
import com.mongenscave.mcmines.identifiers.keys.ItemKeys;
import com.mongenscave.mcmines.managers.MineManager;
import com.mongenscave.mcmines.managers.PromptManager;
import com.mongenscave.mcmines.models.Mine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MineBlocksMenu extends Menu {
    private final Mine mine;
    private static final MineManager mm = McMines.getInstance().getMineManager();

    private final Map<Integer, ItemKeys> slotToKey = new ConcurrentHashMap<>();
    private final Map<Integer, String> slotToBlock = new ConcurrentHashMap<>();
    private List<Integer> freeSlots;

    public MineBlocksMenu(@NotNull MenuController menuController, @NotNull Mine mine) {
        super(menuController);
        this.mine = mine;
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent e) {
        e.setCancelled(true);
        var slot = e.getSlot();
        var item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        var player = menuController.owner();

        // Ha már van blokk ebben a slotban (szerkesztés)
        String blockKey = slotToBlock.get(slot);
        if (blockKey != null) {
            Optional<BlockData> opt = mine.getBlockDataList().stream()
                    .filter(bd -> bd.material().equalsIgnoreCase(blockKey)).findFirst();
            if (opt.isEmpty()) return;

            BlockData bd = opt.get();

            // Middle click vagy Q = törlés
            if (e.getClick() == ClickType.MIDDLE || e.getClick() == ClickType.DROP) {
                mine.removeBlockData(bd.material());
                mm.updateMine(mine);
                player.sendMessage(Component.text("Eltávolítva: " + bd.material()).color(NamedTextColor.GREEN));
                setMenuItems();
                return;
            }

            // Esély módosítás
            int delta = 0;
            if (e.getClick() == ClickType.LEFT) delta = +1;
            else if (e.getClick() == ClickType.RIGHT) delta = -1;
            else if (e.getClick() == ClickType.SHIFT_LEFT) delta = +5;
            else if (e.getClick() == ClickType.SHIFT_RIGHT) delta = -5;

            if (delta != 0) {
                int newChance = Math.max(1, Math.min(100, bd.chance() + delta));
                mine.removeBlockData(bd.material());
                mine.addBlockData(bd.material(), newChance);
                mm.updateMine(mine);
                setMenuItems();
            }
            return;
        }

        // Ha üres slot, akkor új blokk hozzáadása
        // Az inventory-ban lévő item alapján
        Material clickedMaterial = item.getType();
        String materialName = clickedMaterial.name();

        player.closeInventory();

        PromptManager.request(player,
                "Írd be az esélyt % a(z) " + materialName + " blokkhoz (1..100):",
                (p, msg) -> {
                    try {
                        int chance = Integer.parseInt(msg.trim());
                        addOrError(p, materialName, chance);
                    } catch (NumberFormatException ex) {
                        p.sendMessage(Component.text("Érvénytelen esély! Csak számot adj meg.").color(NamedTextColor.RED));
                    }
                });
    }

    private void addOrError(org.bukkit.entity.Player player, String material, int chance) {
        if (chance <= 0 || chance > 100) {
            player.sendMessage(Component.text("Az esélynek 1 és 100 között kell lennie!").color(NamedTextColor.RED));
            return;
        }

        final String storeKey;
        try {
            storeKey = McMines.getInstance().getBlockPlatforms().normalizeForStore(material);
        } catch (Exception ex) {
            player.sendMessage(Component.text("Érvénytelen material: " + material + " (" + ex.getMessage() + ")")
                    .color(NamedTextColor.RED));
            return;
        }

        // Meglévő eltávolítása és új hozzáadása
        mine.removeBlockData(storeKey);
        mine.addBlockData(storeKey, chance);
        mm.updateMine(mine);

        player.sendMessage(Component.text("Hozzáadva: " + storeKey + " - " + chance + "%")
                .color(NamedTextColor.GREEN));

        // Menu újranyitása
        new MineBlocksMenu(menuController, mine).open();
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        slotToKey.clear();
        slotToBlock.clear();

        findAvailableSlots();

        // Blokkok listázása esély szerint rendezve
        List<BlockData> list = new ArrayList<>(mine.getBlockDataList());
        list.sort(Comparator.comparingInt(BlockData::chance).reversed());

        for (int i = 0; i < list.size() && i < inventory.getSize(); i++) {
            BlockData bd = list.get(i);
            ItemStack icon = BlockIconFactory.iconFor(bd);
            inventory.setItem(i, icon);
            slotToBlock.put(i, bd.material());
        }
    }

    private void findAvailableSlots() {
        // Ez már nem kell, mivel közvetlenül a slotokba rakjuk a blokkokat
        freeSlots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                freeSlots.add(i);
            }
        }
    }

    @Override
    public String getMenuName() {
        return "Blokkok — " + mine.getName();
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public int getMenuTick() {
        return 40;
    }

    private static final class BlockIconFactory {
        static ItemStack iconFor(BlockData bd) {
            Material mat = guessIcon(bd.material());
            var itemStack = new ItemStack(mat);
            ItemMeta meta = itemStack.getItemMeta();

            if (meta != null) {
                meta.displayName(Component.text(bd.material()).color(NamedTextColor.YELLOW));
                meta.lore(Arrays.asList(
                        Component.text("Esély: " + bd.chance() + "%").color(NamedTextColor.GREEN),
                        Component.text(""),
                        Component.text("Bal klik: +1%").color(NamedTextColor.GRAY),
                        Component.text("Jobb klik: -1%").color(NamedTextColor.GRAY),
                        Component.text("Shift + Bal: +5%").color(NamedTextColor.GRAY),
                        Component.text("Shift + Jobb: -5%").color(NamedTextColor.GRAY),
                        Component.text("Középső/Q: Törlés").color(NamedTextColor.RED),
                        Component.text(""),
                        Component.text("Üres helyre klikkelj új blokk hozzáadásához!").color(NamedTextColor.AQUA)
                ));
                itemStack.setItemMeta(meta);
            }

            return itemStack;
        }

        static Material guessIcon(String key) {
            if (!key.contains(":")) {
                Material material = Material.matchMaterial(key);
                return material != null ? material : Material.STONE;
            }

            // Custom plugin támogatás
            if (key.startsWith("nexo:")) return Material.NETHER_STAR;
            if (key.startsWith("oraxen:")) return Material.ENDER_EYE;
            if (key.startsWith("itemsadder:")) return Material.NAME_TAG;

            return Material.PAPER;
        }
    }
}