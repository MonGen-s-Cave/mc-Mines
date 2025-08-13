package com.mongenscave.mcmines.identifiers.keys;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.config.Config;
import com.mongenscave.mcmines.processor.MessageProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum MenuKeys {
    MENU_MINE_SELECTOR_TITLE("mine-selector.title"),
    MENU_MINE_SELECTOR_SIZE("mine-selector.size"),

    MENU_MINE_EDITOR_TITLE("mine-editor.title"),
    MENU_MINE_EDITOR_SIZE("mine-editor.size");

    private static final Config config = McMines.getInstance().getGuis();
    private final String path;

    MenuKeys(@NotNull String path) { this.path = path; }

    public static @NotNull String getString(@NotNull String path) {
        return config.getString(path);
    }

    public @NotNull String getString() {
        return MessageProcessor.process(config.getString(path));
    }

    public boolean getBoolean() {
        return config.getBoolean(path);
    }

    public int getInt() {
        return config.getInt(path);
    }

    public List<String> getList() {
        return config.getList(path);
    }
}