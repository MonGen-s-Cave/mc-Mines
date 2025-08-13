package com.mongenscave.mcmines.block.impl;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.block.BlockPlatform;
import com.nexomc.nexo.NexoPlugin;
import com.nexomc.nexo.api.NexoBlocks;
import org.bukkit.Location;

public final class NexoPlatform implements BlockPlatform {

    @Override
    public String namespace() {
        return "nexo";
    }

    @Override
    public boolean isEnabled() {
        return McMines.getInstance().getServer().getPluginManager().isPluginEnabled(NexoPlugin.instance());
    }

    @Override
    public void place(String id, Location loc) {
        NexoBlocks.place(id, loc);
    }
}