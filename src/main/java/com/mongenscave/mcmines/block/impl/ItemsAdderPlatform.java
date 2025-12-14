package com.mongenscave.mcmines.block.impl;

import com.mongenscave.mcmines.block.BlockPlatform;
import dev.lone.itemsadder.api.CustomBlock;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class ItemsAdderPlatform implements BlockPlatform {

    @NotNull @Contract(pure = true) @Override
    public String namespace() {
        return "itemsadder";
    }

    @Override
    public boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("ItemsAdder");
    }

    @Override
    public void place(String id, Location loc) {
        CustomBlock.place(id, loc);
    }

    @Override
    public Optional<ItemStack> icon(String id) {
        if (!isEnabled()) return Optional.empty();
        try {
            CustomStack builder = CustomStack.getInstance(id);
            if (builder == null) return Optional.empty();

            ItemStack stack = builder.getItemStack();
            if (stack == null || stack.getType().isAir()) return Optional.empty();

            return Optional.of(stack.clone());
        }  catch (Throwable t) {
            return Optional.empty();
        }
    }
}