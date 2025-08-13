package com.mongenscave.mcmines.gui.models;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.data.BlockData;
import com.mongenscave.mcmines.data.common.MenuController;
import com.mongenscave.mcmines.gui.Menu;
import com.mongenscave.mcmines.identifiers.keys.ItemKeys;
import com.mongenscave.mcmines.managers.MineManager;
import com.mongenscave.mcmines.models.Mine;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
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

    @Override public void handleMenu(@NotNull InventoryClickEvent e) {
        e.setCancelled(true);
        var slot = e.getSlot();
        var item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;

        ItemKeys key = slotToKey.get(slot);
        if (key != null) {
//            switch (key) {
//                case BLOCKS_BACK -> new MineEditorMenu(menuController, mine).open();
//                case BLOCKS_ADD -> {
//                    var p = menuController.owner();
//                    PromptManager.request(p, "Type material and chance (e.g. 'STONE 25' or 'nexo:copper_ore 15'):",
//                            (player, msg) -> {
//                                String[] parts = msg.trim().split("\\s+");
//                                if (parts.length == 0) { player.sendMessage(Component.text("Missing args.")); return; }
//                                String material = parts[0];
//                                Integer chance = null;
//                                if (parts.length >= 2) { try { chance = Integer.parseInt(parts[1]); } catch (NumberFormatException ignored) {} }
//                                if (chance == null) {
//                                    PromptManager.request(player, "Type chance % (1..100):", (pp, s) -> {
//                                        try { int c = Integer.parseInt(s); addOrError(pp, material, c); }
//                                        catch (NumberFormatException ex) { pp.sendMessage(Component.text("Invalid chance.")); }
//                                    });
//                                } else addOrError(player, material, chance);
//                            });
//                    menuController.owner().closeInventory();
//                }
//                default -> {}
//            }
            return;
        }

        String blockKey = slotToBlock.get(slot);
        if (blockKey == null) return;

        Optional<BlockData> opt = mine.getBlockDataList().stream()
                .filter(bd -> bd.material().equalsIgnoreCase(blockKey)).findFirst();
        if (opt.isEmpty()) return;

        BlockData bd = opt.get();

        int delta = 0;
        if (e.isLeftClick() && !e.isShiftClick()) delta = +1;
        else if (e.isRightClick() && !e.isShiftClick()) delta = -1;
        else if (e.isLeftClick()) delta = +5;
        else if (e.isRightClick()) delta = -5;

        if (e.getClick().isMouseClick() || e.getClick().isKeyboardClick()) {
            mine.removeBlockData(bd.material());
            mm.updateMine(mine);
            menuController.owner().sendMessage(Component.text("Removed " + bd.material()));
            setMenuItems();
            return;
        }

        if (delta != 0) {
            int newC = Math.max(1, Math.min(100, bd.chance() + delta));
            mine.removeBlockData(bd.material());
            mine.addBlockData(bd.material(), newC);
            mm.updateMine(mine);
            setMenuItems();
        }
    }

    private void addOrError(org.bukkit.entity.Player player, String material, int chance) {
        if (chance <= 0 || chance > 100) { player.sendMessage(Component.text("Chance must be 1..100")); return; }

        final String storeKey;
        try {
            storeKey = McMines.getInstance().getBlockPlatforms().normalizeForStore(material);
        } catch (Exception ex) {
            player.sendMessage(Component.text("Invalid material: " + material + " (" + ex.getMessage() + ")"));
            return;
        }

        mine.removeBlockData(storeKey);
        mine.addBlockData(storeKey, chance);
        mm.updateMine(mine);
        player.sendMessage(Component.text("Added " + storeKey + " with " + chance + "%"));
        new MineBlocksMenu(menuController, mine).open();
    }

    @Override public void setMenuItems() {
        inventory.clear();
        slotToKey.clear();
        slotToBlock.clear();

//        setFixed(ItemKeys.BLOCKS_BACK);
//        setFixed(ItemKeys.BLOCKS_ADD);

        findAvailableSlots();

        List<BlockData> list = new ArrayList<>(mine.getBlockDataList());
        list.sort(Comparator.comparingInt(BlockData::chance).reversed());

        for (int i = 0; i < list.size() && i < freeSlots.size(); i++) {
            BlockData bd = list.get(i);
            int slot = freeSlots.get(i);
            ItemStack icon = BlockIconFactory.iconFor(bd);
            inventory.setItem(slot, icon);
            slotToBlock.put(slot, bd.material());
        }
    }

    private void setFixed(@NotNull ItemKeys key) {
        ItemStack item = key.getItem(); if (item == null) return;
        int slot = key.getSlot();
        inventory.setItem(slot, item);
        slotToKey.put(slot, key);
    }

    private void findAvailableSlots() {
        freeSlots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) freeSlots.add(i);
        }
    }

    @Override public String getMenuName() { return "Blocks — " + mine.getName(); }
    @Override public int getSlots() { return 54; }
    @Override public int getMenuTick() { return 40; }

    private static final class BlockIconFactory {
        static ItemStack iconFor(BlockData bd) {
            Material mat = guessIcon(bd.material());
            var is = new ItemStack(mat);
            var im = is.getItemMeta();
            im.setDisplayName(bd.material());
            im.setLore(List.of(
                    "Chance: " + bd.chance() + "%",
                    "LMB +1%  | RMB -1%",
                    "Shift+LMB +5% | Shift+RMB -5%",
                    "Middle/Q: Remove"
            ));
            is.setItemMeta(im);
            return is;
        }
        static Material guessIcon(String key) {
            if (!key.contains(":")) {
                var m = Material.matchMaterial(key);
                return m != null ? m : Material.STONE;
            }
            if (key.startsWith("nexo:")) return Material.NETHER_STAR;
            if (key.startsWith("oraxen:")) return Material.ENDER_EYE;
            if (key.startsWith("itemsadder:")) return Material.NAME_TAG;
            return Material.PAPER;
        }
    }
}
