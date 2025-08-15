package com.mongenscave.mcmines.gui.models;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.data.BlockData;
import com.mongenscave.mcmines.data.MenuController;
import com.mongenscave.mcmines.gui.Menu;
import com.mongenscave.mcmines.identifiers.keys.MenuKeys;
import com.mongenscave.mcmines.identifiers.keys.MessageKeys;
import com.mongenscave.mcmines.item.ItemFactory;
import com.mongenscave.mcmines.managers.MineManager;
import com.mongenscave.mcmines.managers.PromptManager;
import com.mongenscave.mcmines.models.Mine;
import com.mongenscave.mcmines.processor.MessageProcessor;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("all")
public final class MineBlocksMenu extends Menu {
    private final Mine mine;
    private static final MineManager mm = McMines.getInstance().getMineManager();

    private final Map<Integer, String> slotToBlock = new ConcurrentHashMap<>();
    private List<Integer> availableSlots;

    public MineBlocksMenu(@NotNull MenuController menuController, @NotNull Mine mine) {
        super(menuController);
        this.mine = mine;
    }

    @Override
    public void handleMenu(@NotNull InventoryClickEvent e) {
        e.setCancelled(true);

        int slot = e.getSlot();
        String existingKey = slotToBlock.get(slot);
        var player = menuController.owner();

        if (existingKey != null) {
            Optional<BlockData> opt = mine.getBlockDataList().stream()
                    .filter(bd -> bd.material().equalsIgnoreCase(existingKey)).findFirst();
            if (opt.isEmpty()) return;

            BlockData bd = opt.get();

            if (e.getClick() == ClickType.MIDDLE || e.getClick() == ClickType.DROP) {
                mine.removeBlockData(bd.material());
                mm.updateMine(mine);
                player.sendMessage(MessageKeys.BLOCKS_REMOVED.with("key", bd.material()));
                setMenuItems();
                return;
            }

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

        ItemStack clicked = e.getCurrentItem();
        if (clicked != null && clicked.getType() != Material.AIR) return;

        player.closeInventory();

        PromptManager.request(player,
                MessageKeys.PROMPT_BLOCK_ADD_ID.getMessage(),
                (p, keyMsg) -> {
                    String rawKey = keyMsg == null ? "" : keyMsg.trim();
                    if (rawKey.isEmpty()) {
                        p.sendMessage(MessageKeys.PROMPT_BLOCK_ADD_INVALID.with("input", "", "error", "empty"));
                        new MineBlocksMenu(MenuController.getMenuUtils(p), mine).open();
                        return;
                    }

                    final String storeKey;
                    try {
                        storeKey = McMines.getInstance().getBlockPlatforms().normalizeForStore(rawKey);
                    } catch (Exception ex) {
                        p.sendMessage(MessageKeys.PROMPT_BLOCK_ADD_INVALID.with("input", rawKey, "error", ex.getMessage()));
                        new MineBlocksMenu(MenuController.getMenuUtils(p), mine).open();
                        return;
                    }

                    PromptManager.request(p,
                            MessageKeys.PROMPT_BLOCK_ADD_CHANCE.with("key", storeKey),
                            (p2, chanceMsg) -> {
                                try {
                                    int chance = Integer.parseInt(chanceMsg.trim());
                                    if (chance <= 0 || chance > 100) {
                                        p2.sendMessage(MessageKeys.PROMPT_BLOCK_ADD_CHANCE_RANGE.getMessage());
                                        new MineBlocksMenu(MenuController.getMenuUtils(p2), mine).open();
                                        return;
                                    }

                                    mine.removeBlockData(storeKey);
                                    mine.addBlockData(storeKey, chance);
                                    mm.updateMine(mine);

                                    p2.sendMessage(MessageKeys.BLOCKS_ADDED.with("key", storeKey, "chance", chance));
                                } catch (NumberFormatException ex) {
                                    p2.sendMessage(MessageKeys.PROMPT_BLOCK_ADD_CHANCE_INVALID.getMessage());
                                }
                                new MineBlocksMenu(MenuController.getMenuUtils(p2), mine).open();
                            });
                });
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        slotToBlock.clear();

        ItemFactory.setItemsForMenu("mine-blocks.items", inventory);
        findAvailableSlots();

        List<BlockData> list = new ArrayList<>(mine.getBlockDataList());
        list.sort(Comparator.comparingInt(BlockData::chance).reversed());

        int perPage = availableSlots.size();
        int count = Math.min(perPage, list.size());

        for (int i = 0; i < count; i++) {
            BlockData bd = list.get(i);
            int slot = availableSlots.get(i);
            ItemStack icon = buildIcon(bd);
            inventory.setItem(slot, icon);
            slotToBlock.put(slot, bd.material());
        }
    }

    private void findAvailableSlots() {
        availableSlots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) availableSlots.add(i);
        }
    }

    private ItemStack buildIcon(BlockData bd) {
        var guis = McMines.getInstance().getGuis();
        var template = guis.getSection("mine-blocks.block-template");

        ItemStack base = McMines.getInstance().getBlockPlatforms()
                .iconFor(bd.material())
                .orElse(new ItemStack(Material.STONE));

        if (template != null) {
            String displayName = template.getString("name", "");
            var loreLines = template.getStringList("lore");

            base.editMeta(meta -> {
                if (displayName != null && !displayName.isEmpty()) {
                    meta.setDisplayName(MessageProcessor.process(apply(displayName, bd)));
                }
                if (loreLines != null && !loreLines.isEmpty()) {
                    List<String> out = new ArrayList<>(loreLines.size());
                    for (String line : loreLines) out.add(MessageProcessor.process(apply(line, bd)));
                    meta.setLore(out);
                } else if (meta.getLore() == null || meta.getLore().isEmpty()) {
                    meta.setLore(List.of(
                            MessageProcessor.process("&7Chance: &a{chance}%".replace("{chance}", String.valueOf(bd.chance()))),
                            MessageProcessor.process("&8Left: +1 &8| Right: -1"),
                            MessageProcessor.process("&8Shift+Left: +5 &8| Shift+Right: -5"),
                            MessageProcessor.process("&cMiddle/Q: Remove")
                    ));
                }
                if (meta.getDisplayName() == null || meta.getDisplayName().isEmpty()) {
                    meta.setDisplayName(MessageProcessor.process("&e{block_key}".replace("{block_key}", bd.material())));
                }
            });
            return base;
        }

        base.editMeta(meta -> {
            if (meta.getDisplayName() == null || meta.getDisplayName().isEmpty()) {
                meta.setDisplayName(MessageProcessor.process("&e{block_key}".replace("{block_key}", bd.material())));
            }
            if (meta.getLore() == null || meta.getLore().isEmpty()) {
                meta.setLore(List.of(
                        MessageProcessor.process("&7Chance: &a{chance}%".replace("{chance}", String.valueOf(bd.chance()))),
                        MessageProcessor.process("&8Left: +1 &8| Right: -1"),
                        MessageProcessor.process("&8Shift+Left: +5 &8| Shift+Right: -5"),
                        MessageProcessor.process("&cMiddle/Q: Remove")
                ));
            }
        });
        return base;
    }

    private static String apply(String s, BlockData bd) {
        return s.replace("{block_key}", bd.material())
                .replace("{chance}", String.valueOf(bd.chance()));
    }

    @Override public String getMenuName() {
        return MenuKeys.MENU_MINE_BLOCKS_TITLE.getString().replace("{mine}", mine.getName());
    }
    @Override public int getSlots() {
        return MenuKeys.MENU_MINE_BLOCKS_SIZE.getInt();
    }
    @Override public int getMenuTick() {
        return 20;
    }
}