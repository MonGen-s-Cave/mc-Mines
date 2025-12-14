package com.mongenscave.mcmines.identifiers.keys;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.config.Config;
import com.mongenscave.mcmines.processor.MessageProcessor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public enum HookKeys {
    NEXO("hooks.register.Nexo"),
    ITEMSADDER("hooks.register.ItemsAdder"),
    ORAXEN("hooks.register.Oraxen");

    private static final Config config = McMines.getInstance().getHooks();
    private final String path;

    HookKeys(@NotNull String path) { this.path = path; }

    public boolean getBoolean() {
        return config.getBoolean(path);
    }
}