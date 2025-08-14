package com.mongenscave.mcmines.gui.models;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.identifiers.SelectionMode;
import com.mongenscave.mcmines.data.MenuController;
import com.mongenscave.mcmines.gui.Menu;
import com.mongenscave.mcmines.identifiers.keys.ItemKeys;
import com.mongenscave.mcmines.identifiers.keys.MenuKeys;
import com.mongenscave.mcmines.identifiers.keys.MessageKeys;
import com.mongenscave.mcmines.item.ItemFactory;
import com.mongenscave.mcmines.managers.MineManager;
import com.mongenscave.mcmines.managers.PromptManager;
import com.mongenscave.mcmines.managers.WandManager;
import com.mongenscave.mcmines.models.Mine;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("all")
public final class MineEditorMenu extends Menu {
    private static final MineManager mineManager = McMines.getInstance().getMineManager();

    private final Mine mine;
    private final Map<Integer, ItemKeys> slotToItemKeyMap = new ConcurrentHashMap<>();

    public MineEditorMenu(@NotNull MenuController menuController, @NotNull Mine mine) {
        super(menuController);
        this.mine = mine;
    }

    @Override
    public void handleMenu(final @NotNull InventoryClickEvent event) {
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        int slot = event.getSlot();
        ItemKeys clickedKey = slotToItemKeyMap.get(slot);
        if (clickedKey == null) return;

        switch (clickedKey) {
            case MINE_EDITOR_BACK -> new MineSelectorMenu(menuController).open();

            case MINE_EDITOR_AREA_WAND -> {
                menuController.owner().closeInventory();
                WandManager wand = McMines.getInstance().getWandManager();
                wand.startSession(menuController.owner(), mine, SelectionMode.MINE_AREA, true);
            }

            case MINE_EDITOR_ENTRANCE_WAND -> {
                menuController.owner().closeInventory();
                WandManager wand = McMines.getInstance().getWandManager();
                wand.startSession(menuController.owner(), mine, SelectionMode.ENTRANCE_AREA, true);
            }

            case MINE_EDITOR_RENAME -> {
                menuController.owner().closeInventory();
                PromptManager.request(menuController.owner(), MessageKeys.PROMPT_RENAME_START.getMessage(), (player, input) -> {
                    String newName = input == null ? "" : input.trim();
                    if (!newName.matches("[A-Za-z0-9_\\-]{1,32}")) {
                        player.sendMessage(MessageKeys.PROMPT_RENAME_INVALID.getMessage());
                        new MineEditorMenu(MenuController.getMenuUtils(player), mine).open();
                        return;
                    }
                    if (mineManager.getMine(newName) != null) {
                        player.sendMessage(MessageKeys.PROMPT_RENAME_EXISTS.with("name", newName));
                        new MineEditorMenu(MenuController.getMenuUtils(player), mine).open();
                        return;
                    }

                    try {
                        mineManager.renameMine(mine, newName);
                        player.sendMessage(MessageKeys.PROMPT_RENAME_SUCCESS.with("name", newName));
                        Mine renamed = mineManager.getMine(newName);
                        new MineEditorMenu(MenuController.getMenuUtils(player), renamed != null ? renamed : mine).open();
                    } catch (Exception ex) {
                        player.sendMessage(Component.text("Rename failed: " + ex.getMessage()));
                        new MineEditorMenu(MenuController.getMenuUtils(player), mine).open();
                    }
                });
            }

            case MINE_EDITOR_SET_RESET -> {
                menuController.owner().closeInventory();
                menuController.owner().sendMessage(MessageKeys.PROMPT_SET_RESET_START.getMessage());

                PromptManager.request(menuController.owner(), null, (player, input) -> {
                    try {
                        int seconds = Integer.parseInt(input.trim());
                        if (seconds <= 0 || seconds > 86400) player.sendMessage(MessageKeys.PROMPT_SET_RESET_RANGE.getMessage());
                        else {
                            mine.setResetAfter(seconds);
                            mineManager.updateMine(mine);
                            player.sendMessage(MessageKeys.PROMPT_SET_RESET_SUCCESS.with("seconds", seconds));
                        }
                    } catch (NumberFormatException ex) {
                        player.sendMessage(MessageKeys.PROMPT_SET_RESET_INVALID.getMessage());
                    }
                    new MineEditorMenu(MenuController.getMenuUtils(player), mine).open();
                });
            }

            case MINE_EDITOR_SET_PERMISSION -> {
                menuController.owner().closeInventory();

                PromptManager.request(menuController.owner(), MessageKeys.PROMPT_SET_PERMISSION_START.getMessage(), (player, input) -> {
                    String raw = (input == null) ? "" : input.trim();
                    if (raw.isEmpty() || raw.equals("-")) {
                        mine.setEntrancePermission(null);
                        mineManager.updateMine(mine);
                        player.sendMessage(MessageKeys.PROMPT_SET_PERMISSION_CLEARED.getMessage());
                    } else {
                        mine.setEntrancePermission(raw);
                        mineManager.updateMine(mine);
                        player.sendMessage(MessageKeys.PROMPT_SET_PERMISSION_SET.with("permission", raw));
                    }
                    new MineEditorMenu(MenuController.getMenuUtils(player), mine).open();
                });
            }

            case MINE_EDITOR_BLOCKS -> new MineBlocksMenu(menuController, mine).open();

            case MINE_EDITOR_RESET -> {
                mineManager.resetMine(mine);
                menuController.owner().sendMessage(Component.text("Mine '" + mine.getName() + "' reset."));
                updateMenuItems();
            }

            default -> {}
        }
    }

    @Override
    public void setMenuItems() {
        inventory.clear();
        slotToItemKeyMap.clear();

        ItemFactory.setItemsForMenu("mine-editor.items", inventory);

        setMenuItem(ItemKeys.MINE_EDITOR_BACK);

        setMenuItem(ItemKeys.MINE_EDITOR_AREA_WAND);
        setMenuItem(ItemKeys.MINE_EDITOR_ENTRANCE_WAND);

        setMenuItem(ItemKeys.MINE_EDITOR_RENAME);

        setMenuItem(ItemKeys.MINE_EDITOR_SET_RESET);
        setMenuItem(ItemKeys.MINE_EDITOR_SET_PERMISSION);
        setMenuItem(ItemKeys.MINE_EDITOR_BLOCKS);
        setMenuItem(ItemKeys.MINE_EDITOR_RESET);
    }

    private void setMenuItem(@NotNull ItemKeys itemKey) {
        ItemStack item = itemKey.getItem();
        if (item == null) return;

        int slot = itemKey.getSlot();
        if (slot >= 0 && slot < inventory.getSize()) {
            inventory.setItem(slot, item);
            slotToItemKeyMap.put(slot, itemKey);
        }
    }

    @Override public String getMenuName() {
        return MenuKeys.MENU_MINE_EDITOR_TITLE.getString().replace("{mine}", mine.getName());
    }
    @Override public int getSlots() {
        return MenuKeys.MENU_MINE_EDITOR_SIZE.getInt();
    }
    @Override public int getMenuTick() {
        return 20;
    }
}