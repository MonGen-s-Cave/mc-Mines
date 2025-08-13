package com.mongenscave.mcmines.identifiers.keys;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.item.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public enum ItemKeys {

    MINE_SELECTOR_BACK("mine-selector.items.back"),
    MINE_SELECTOR_FORWARD("mine-selector.items.forward"),
    MINE_SELECTOR_RELOAD("mine-selector.items.reload"),
    MINE_SELECTOR_RESET_ALL("mine-selector.items.reset-all"),
    MINE_SELECTOR_CREATE("mine-selector.items.create"),

    MINE_EDITOR_BACK("mine-editor.items.back"),
    MINE_EDITOR_RENAME("mine-editor.items.rename"),
    MINE_EDITOR_SET_RESET("mine-editor.mine.set-reset-time"),
    MINE_EDITOR_SET_PERMISSION("mine-editor.items.set-permission"),
    MINE_EDITOR_BLOCKS("mine-editor.items.blocks"),
    MINE_EDITOR_RESET("mine-editor.items.reset"),
    MINE_EDITOR_AREA_WAND("mine-editor.items.area-wand"),
    MINE_EDITOR_ENTRANCE_WAND("mine-editor.items.entrance-wand");

    private final String path;

    ItemKeys(@NotNull final String path) { this.path = path; }

    public int getSlot() {
        return McMines.getInstance().getGuis().getInt(path + ".slot");
    }

    public ItemStack getItem() {
        return ItemFactory.createItemFromString(path, McMines.getInstance().getGuis()).orElse(null);
    }
}