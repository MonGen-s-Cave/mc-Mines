package com.mongenscave.mcmines.identifiers.keys;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.config.Config;
import com.mongenscave.mcmines.processor.MessageProcessor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public enum MessageKeys {
    RELOAD("messages.reload"),

    NO_PERMISSION("messages.no-permission"),
    PLAYER_REQUIRED("messages.player-required"),

    PROMPT_CREATE_NAME_START("messages.prompt.create.name.start"),
    PROMPT_CREATE_NAME_INVALID("messages.prompt.create.name.invalid"),
    PROMPT_CREATE_NAME_EXISTS("messages.prompt.create.name.exists"),
    PROMPT_CREATE_RESET_START("messages.prompt.create.reset.start"),
    PROMPT_CREATE_RESET_INVALID("messages.prompt.create.reset.invalid-number"),
    PROMPT_CREATE_RESET_RANGE("messages.prompt.create.reset.out-of-range"),
    PROMPT_CREATE_SUCCESS("messages.prompt.create.success"),

    PROMPT_RENAME_START("messages.prompt.editor.rename.start"),
    PROMPT_RENAME_INVALID("messages.prompt.editor.rename.invalid"),
    PROMPT_RENAME_EXISTS("messages.prompt.editor.rename.exists"),
    PROMPT_RENAME_SUCCESS("messages.prompt.editor.rename.success"),

    PROMPT_SET_RESET_START("messages.prompt.editor.set-reset.start"),
    PROMPT_SET_RESET_RANGE("messages.prompt.editor.set-reset.range"),
    PROMPT_SET_RESET_SUCCESS("messages.prompt.editor.set-reset.success"),
    PROMPT_SET_RESET_INVALID("messages.prompt.editor.set-reset.invalid"),

    PROMPT_SET_PERMISSION_START("messages.prompt.editor.set-permission.start"),
    PROMPT_SET_PERMISSION_CLEARED("messages.prompt.editor.set-permission.cleared"),
    PROMPT_SET_PERMISSION_SET("messages.prompt.editor.set-permission.set"),

    PROMPT_BLOCK_ADD_ID("messages.prompt.editor.blocks.add.id"),
    PROMPT_BLOCK_ADD_INVALID("messages.prompt.editor.blocks.add.invalid"),
    PROMPT_BLOCK_ADD_CHANCE("messages.prompt.editor.blocks.add.chance"),
    PROMPT_BLOCK_ADD_CHANCE_INVALID("messages.prompt.editor.blocks.add.chance-invalid"),
    PROMPT_BLOCK_ADD_CHANCE_RANGE("messages.prompt.editor.blocks.add.chance-range"),
    BLOCKS_REMOVED("messages.prompt.editor.blocks.removed"),
    BLOCKS_ADDED("messages.prompt.editor.blocks.added"),

    WAND_START_MINE("messages.wand.start-mine"),
    WAND_START_ENTRANCE("messages.wand.start-entrance"),
    WAND_POS1_SET("messages.wand.pos1"),
    WAND_POS2_SET("messages.wand.pos2"),
    WAND_DONE_MINE("messages.wand.done-mine"),
    WAND_DONE_ENTRANCE("messages.wand.done-entrance"),
    WAND_CANCELLED("messages.wand.cancelled"),
    WAND_NO_SPACE("messages.wand.no-space"),

    WAND_ITEM_NAME("messages.wand.item.name"),
    WAND_ITEM_LORE_1("messages.wand.item.lore1"),
    WAND_ITEM_LORE_2("messages.wand.item.lore2"),
    WAND_HINT_SAVE("messages.wand.hint-save"),
    WAND_NEED_BOTH("messages.wand.need-both"),
    WAND_ITEM_LORE_3("messages.wand.item.lore3"),

    ENTRANCE_NO_PERMISSION("messages.entrance.no-permission"),
    ACTIONBAR_MINE_STATUS("messages.actionbar.mine-status");

    private static final Config language = McMines.getInstance().getLanguage();
    private final String path;

    MessageKeys(@NotNull String path) { this.path = path; }

    public @NotNull String getMessage() {
        return MessageProcessor.process(language.getString(path)).replace("%prefix%", MessageProcessor.process(language.getString("prefix")));
    }

    public @NotNull String with(@NotNull Object... kv) {
        String msg = getMessage();

        for (int i = 0; i + 1 < kv.length; i += 2) {
            String k = String.valueOf(kv[i]);
            String v = String.valueOf(kv[i + 1]);
            msg = msg.replace("{" + k + "}", v);
        }

        return msg;
    }
}