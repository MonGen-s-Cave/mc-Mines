package com.mongenscave.mcmines.block.impl;

import com.mongenscave.mcmines.block.BlockPlatform;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.api.OraxenItems;
import io.th0rgal.oraxen.items.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class OraxenPlatform implements BlockPlatform {

    @NotNull @Contract(pure = true) @Override
    public String namespace() {
        return "oraxen";
    }

    @Override
    public boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("Oraxen");
    }

    @Override
    public void place(String id, Location loc) {
        OraxenBlocks.place(id, loc);
    }

    @Override
    public Optional<ItemStack> icon(String id) {
        if (!isEnabled()) return Optional.empty();
        try {
            ItemBuilder builder = OraxenItems.getItemById(id);
            if (builder == null) return Optional.empty();

            ItemStack stack = builder.build();
            if (stack == null || stack.getType().isAir()) return Optional.empty();

            return Optional.of(stack.clone());
        }  catch (Throwable t) {
            return Optional.empty();
        }
    }
}