package com.mongenscave.mcmines.gui.models;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.data.MenuController;
import com.mongenscave.mcmines.gui.PaginatedMenu;
import com.mongenscave.mcmines.identifiers.keys.MessageKeys;
import com.mongenscave.mcmines.managers.PromptManager;
import com.mongenscave.mcmines.identifiers.keys.ItemKeys;
import com.mongenscave.mcmines.identifiers.keys.MenuKeys;
import com.mongenscave.mcmines.item.ItemFactory;
import com.mongenscave.mcmines.managers.MineManager;
import com.mongenscave.mcmines.models.Mine;
import com.mongenscave.mcmines.processor.MessageProcessor;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("all")
public final class MineSelectorMenu extends PaginatedMenu {
    private static final MineManager manager = McMines.getInstance().getMineManager();

    private final Map<Integer, ItemKeys> slotToItemKeyMap = new ConcurrentHashMap<>();
    private final Map<Integer, String> slotToMineMap = new ConcurrentHashMap<>();
    private List<Integer> availableSlots;

    public MineSelectorMenu(@NotNull MenuController menuController) {
        super(menuController);
    }

    @Override
    public void handleMenu(final @NotNull InventoryClickEvent event) {
        event.setCancelled(true);
        var clicked = event.getCurrentItem();
        int slot = event.getSlot();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemKeys key = slotToItemKeyMap.get(slot);
        if (key != null) {
            switch (key) {
                case MINE_SELECTOR_BACK -> {
                    if (page > 0) {
                        page--;
                        setMenuItems();
                    } else {
                        menuController.owner().closeInventory();
                    }
                }

                case MINE_SELECTOR_FORWARD -> {
                    if (hasNextPage()) {
                        page++;
                        setMenuItems();
                    }
                }

                case MINE_SELECTOR_RELOAD -> {
                    McMines.getInstance().getConfiguration().reload();
                    McMines.getInstance().getLanguage().reload();
                    McMines.getInstance().getHooks().reload();
                    McMines.getInstance().getGuis().reload();

                    manager.loadMines();
                    manager.resetAllMines();

                    menuController.owner().sendMessage(Component.text("Configuration reloaded!"));
                    page = 0;

                    open();
                }

                case MINE_SELECTOR_RESET_ALL -> {
                    manager.resetAllMines();
                    menuController.owner().sendMessage(Component.text("All mines reset."));
                }

                case MINE_SELECTOR_CREATE -> {
                    final var p = menuController.owner();
                    p.closeInventory();

                    PromptManager.request(p, MessageKeys.PROMPT_CREATE_NAME_START.getMessage(), (player, nameMsg) -> {
                        String name = nameMsg == null ? "" : nameMsg.trim();
                        if (!name.matches("[A-Za-z0-9_\\-]{1,32}")) {
                            player.sendMessage(MessageKeys.PROMPT_CREATE_NAME_INVALID.getMessage());
                            new MineSelectorMenu(MenuController.getMenuUtils(player)).open();
                            return;
                        }
                        if (manager.getMine(name) != null) {
                            player.sendMessage(MessageKeys.PROMPT_CREATE_NAME_EXISTS.with("name", name));
                            new MineSelectorMenu(MenuController.getMenuUtils(player)).open();
                            return;
                        }

                        PromptManager.request(player, MessageKeys.PROMPT_CREATE_RESET_START.getMessage(), (player2, timeMsg) -> {
                            int seconds = 300;
                            String s = timeMsg == null ? "" : timeMsg.trim();

                            if (!s.isEmpty() && !s.equals("-")) {
                                try { seconds = Integer.parseInt(s); }
                                catch (NumberFormatException ex) {
                                    player2.sendMessage(MessageKeys.PROMPT_CREATE_RESET_INVALID.getMessage());
                                    new MineSelectorMenu(MenuController.getMenuUtils(player2)).open();
                                    return;
                                }
                            }

                            if (seconds <= 0 || seconds > 86400) {
                                player2.sendMessage(MessageKeys.PROMPT_CREATE_RESET_RANGE.getMessage());
                                new MineSelectorMenu(MenuController.getMenuUtils(player2)).open();
                                return;
                            }

                            manager.createMine(name, seconds);
                            player2.sendMessage(MessageKeys.PROMPT_CREATE_SUCCESS.with("name", name, "seconds", seconds));

                            Mine created = manager.getMine(name);
                            if (created != null) {
                                new MineEditorMenu(MenuController.getMenuUtils(player2), created).open();
                            } else {
                                new MineSelectorMenu(MenuController.getMenuUtils(player2)).open();
                            }
                        });
                    });
                }
                default -> {}
            }
            return;
        }

        String mineName = slotToMineMap.get(slot);
        if (mineName != null) {
            ClickType clickType = event.getClick();
            if (clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP) {
                boolean deleted = manager.deleteMine(mineName);
                if (deleted) {
                    menuController.owner().sendMessage(Component.text("Bánya törölve: " + mineName));
                } else {
                    menuController.owner().sendMessage(Component.text("A bánya nem található: " + mineName));
                }
                page = 0;
                open();
                return;
            }

            Mine mine = manager.getMine(mineName);
            if (mine != null) new MineEditorMenu(menuController, mine).open();
        }
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        slotToItemKeyMap.clear();
        slotToMineMap.clear();

        ItemFactory.setItemsForMenu("mine-selector.items", inventory);

        setMenuItem(ItemKeys.MINE_SELECTOR_BACK);
        setMenuItem(ItemKeys.MINE_SELECTOR_FORWARD);
        setMenuItem(ItemKeys.MINE_SELECTOR_RELOAD);
        setMenuItem(ItemKeys.MINE_SELECTOR_RESET_ALL);
        setMenuItem(ItemKeys.MINE_SELECTOR_CREATE);

        findAvailableSlots();

        List<String> mines = new ArrayList<>(manager.getMineNames());
        mines.sort(String.CASE_INSENSITIVE_ORDER);

        if (!mines.isEmpty() && !availableSlots.isEmpty()) {
            int perPage = availableSlots.size();
            int start = page * perPage;
            int end = Math.min(start + perPage, mines.size());

            for (int i = start; i < end; i++) {
                int slotIndex = i - start;
                if (slotIndex >= availableSlots.size()) break;

                String name = mines.get(i);
                int slot = availableSlots.get(slotIndex);

                ItemStack icon = MineItemFactory.buildMineIcon(name, manager.getMine(name));
                inventory.setItem(slot, icon);
                slotToMineMap.put(slot, name);
            }
        }
    }

    private void findAvailableSlots() {
        availableSlots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) availableSlots.add(i);
        }
    }

    private boolean hasNextPage() {
        List<String> mines = new ArrayList<>(manager.getMineNames());
        int perPage = (availableSlots == null) ? 0 : availableSlots.size();
        return perPage > 0 && (page + 1) * perPage < mines.size();
    }

    private void setMenuItem(@NotNull ItemKeys itemKey) {
        ItemStack item = itemKey.getItem();
        if (item == null) return;
        int slot = itemKey.getSlot();
        inventory.setItem(slot, item);
        slotToItemKeyMap.put(slot, itemKey);
    }

    @Override public String getMenuName() {
        return MenuKeys.MENU_MINE_SELECTOR_TITLE.getString();
    }

    @Override public int getSlots() {
        return MenuKeys.MENU_MINE_SELECTOR_SIZE.getInt();
    }

    @Override public int getMenuTick() {
        return 20;
    }

    private static final class MineItemFactory {
        static ItemStack buildMineIcon(String name, Mine mine) {
            var guis = McMines.getInstance().getGuis();
            var template = guis.getSection("mine-selector.mine-item");

            if (template == null) {
                ItemStack is = new ItemStack(Material.PAPER);
                var im = is.getItemMeta();

                im.setDisplayName(MessageProcessor.process("» " + name));

                var lore = new ArrayList<String>();
                lore.add(MessageProcessor.process("&7Reset: &f" + (mine != null ? mine.getResetAfter() : "?") + "s"));
                lore.add(MessageProcessor.process("&7Area valid: &f" + (mine != null && mine.isValidMineArea())));
                lore.add(MessageProcessor.process("&7Blocks: &f" + (mine != null ? mine.getBlockDataList().size() : 0)));

                im.setLore(lore);
                is.setItemMeta(im);

                return is;
            }

            var baseOpt = ItemFactory.buildItem(template, "mine-selector.mine-item");
            ItemStack is = baseOpt.orElseGet(() -> new ItemStack(Material.PAPER)).clone();

            is.editMeta(meta -> {
                String displayName = meta.getDisplayName();
                if (displayName != null && !displayName.isEmpty()) {
                    meta.setDisplayName(MessageProcessor.process(apply(displayName, name, mine)));
                }

                var lore = meta.getLore();
                if (lore != null && !lore.isEmpty()) {
                    List<String> out = new ArrayList<>(lore.size());
                    for (String line : lore) out.add(MessageProcessor.process(apply(line, name, mine)));
                    meta.setLore(out);
                }
            });

            return is;
        }

        private static String apply(String s, String name, Mine mine) {
            String reset = (mine != null ? String.valueOf(mine.getResetAfter()) : "?");
            String area  = String.valueOf(mine != null && mine.isValidMineArea());
            String count = String.valueOf(mine != null ? mine.getBlockDataList().size() : 0);
            return s.replace("{mine_name}", name)
                    .replace("{reset_seconds}", reset)
                    .replace("{area_valid}", area)
                    .replace("{blocks_count}", count);
        }
    }
}