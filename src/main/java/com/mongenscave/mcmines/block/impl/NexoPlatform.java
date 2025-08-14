package com.mongenscave.mcmines.block.impl;

import com.mongenscave.mcmines.block.BlockPlatform;
import com.nexomc.nexo.api.NexoBlocks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class NexoPlatform implements BlockPlatform {

    @NotNull
    @Contract(pure = true)
    @Override
    public String namespace() {
        return "nexo";
    }

    @Override
    public boolean isEnabled() {
        return !Bukkit.getPluginManager().isPluginEnabled("Nexo");
    }

    @Override
    public void place(String id, Location loc) {
        NexoBlocks.place(id, loc);
    }
}