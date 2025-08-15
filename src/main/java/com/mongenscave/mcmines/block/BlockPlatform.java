package com.mongenscave.mcmines.block;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

@SuppressWarnings("all")
public interface BlockPlatform {
    String namespace();
    boolean isEnabled();
    void place(String id, Location loc) throws Exception;
    default Optional<ItemStack> icon(String id) { return Optional.empty(); }
}