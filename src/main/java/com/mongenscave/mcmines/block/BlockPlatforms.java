package com.mongenscave.mcmines.block;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.block.impl.NexoPlatform;
import com.mongenscave.mcmines.block.key.BlockKey;
import com.mongenscave.mcmines.utils.LoggerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockPlatforms {

    public interface Placement {
        void placeAt(Location loc);
        String storeKey();
    }

    private final Map<String, BlockPlatform> platforms = new ConcurrentHashMap<>();

    public BlockPlatforms() {
        if (McMines.getInstance().getHooks().getBoolean("hooks.register.Nexo")) {
            LoggerUtils.info("\u001B[32m [Hook] Nexo successfully enabled.\u001B[0m");
            register(new NexoPlatform());
        }
    }

    public void register(BlockPlatform p) { platforms.put(p.namespace(), p); }

    @NotNull
    public Placement resolveForReset(String rawKey) {
        BlockKey key = BlockKey.parse(rawKey);

        if (key.vanilla()) {
            final Material mat = matchMaterialFast(key.raw());
            return new Placement() {
                @Override
                public void placeAt(Location loc) {
                    if (loc.getBlock().getType() != mat) loc.getBlock().setType(mat, false);
                }

                @Override
                public String storeKey() {
                    return key.storeKey();
                }
            };
        }

        BlockPlatform p = platforms.get(key.namespace());
        if (p == null || p.isEnabled()) throw new IllegalStateException("Platform not available: " + key.namespace());

        return new Placement() {
            @Override
            public void placeAt(Location loc) {
                try { p.place(key.id(), loc); }
                catch (Exception e) { throw new RuntimeException(e); }
            }

            @Override public String storeKey() { return key.storeKey(); }
        };
    }

    @NotNull
    public String normalizeForStore(String input) {
        BlockKey key = BlockKey.parse(input);

        if (key.vanilla()) matchMaterialFast(key.raw());
        else {
            BlockPlatform p = platforms.get(key.namespace());
            if (p == null || p.isEnabled()) throw new IllegalStateException("Platform not available: " + key.namespace());
        }

        return key.storeKey();
    }

    @NotNull
    private static Material matchMaterialFast(String name) {
        Material m = Material.matchMaterial(name);

        if (m == null) m = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        if (m == null) throw new IllegalArgumentException("Unknown vanilla material: " + name);

        return m;
    }
}