package com.mongenscave.mcmines.block;

import org.bukkit.Location;

public interface BlockPlatform {
    String namespace();
    boolean isEnabled();
    void place(String id, Location loc) throws Exception;
}