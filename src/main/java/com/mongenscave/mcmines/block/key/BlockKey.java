package com.mongenscave.mcmines.block.key;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record BlockKey(boolean vanilla, String raw, String namespace, String id) {

    public static @NotNull BlockKey parse(@NotNull String input) {
        String s = input.trim();
        if (s.isEmpty()) throw new IllegalArgumentException("Empty material");

        int idx = s.indexOf(':');
        if (idx < 0) return new BlockKey(true, s, null, null);

        String ns = s.substring(0, idx).toLowerCase();
        String id = s.substring(idx + 1);

        if (id.isEmpty()) throw new IllegalArgumentException("Missing id part");
        return new BlockKey(false, s, ns, id);
    }

    @NotNull
    @Contract(pure = true)
    public String storeKey() {
        return vanilla ? raw.toUpperCase() : (namespace + ":" + id);
    }
}