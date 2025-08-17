package com.mongenscave.mcmines.block.impl;

import com.mongenscave.mcmines.block.BlockPlatform;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.libraries.nbt.CompoundTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class CraftEnginePlatform implements BlockPlatform {

    @NotNull @Contract(pure = true) @Override
    public String namespace() {
        return "craftengine";
    }

    @Override
    public boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("CraftEngine");
    }

    @Override
    public void place(String id, Location loc) {
        Key blockKey = Key.of(id);
        CompoundTag properties = new CompoundTag();
        CraftEngineBlocks.place(loc, blockKey, properties, false);
    }

    @Override
    public Optional<ItemStack> icon(String id) {
        if (!isEnabled()) return Optional.empty();
        try {
            Key itemKey = Key.of(id);
            CustomItem<ItemStack> customItem = CraftEngineItems.byId(itemKey);
            if (customItem == null) return Optional.empty();

            ItemStack stack = customItem.buildItemStack();
            if (stack == null || stack.getType().isAir()) return Optional.empty();

            return Optional.of(stack.clone());
        } catch (Throwable t) {
            return Optional.empty();
        }
    }
}