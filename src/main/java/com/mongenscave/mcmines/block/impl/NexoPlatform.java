package com.mongenscave.mcmines.block.impl;

import com.mongenscave.mcmines.block.BlockPlatform;
import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class NexoPlatform implements BlockPlatform {

    @NotNull @Contract(pure = true) @Override
    public String namespace() {
        return "nexo";
    }

    @Override
    public boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("Nexo");
    }

    @Override
    public void place(String id, Location loc) {
        NexoBlocks.place(id, loc);
    }

    @Override
    public Optional<ItemStack> icon(String id) {
        if (!isEnabled()) return Optional.empty();
        try {
            ItemBuilder builder = NexoItems.itemFromId(id);
            if (builder == null) return Optional.empty();

            ItemStack stack = builder.getFinalItemStack();
            if (stack == null || stack.getType().isAir()) return Optional.empty();

            return Optional.of(stack.clone());
        }  catch (Throwable t) {
            return Optional.empty();
        }
    }
}