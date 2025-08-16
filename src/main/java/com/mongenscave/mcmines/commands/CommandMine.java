package com.mongenscave.mcmines.commands;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.data.MenuController;
import com.mongenscave.mcmines.gui.models.MineSelectorMenu;
import com.mongenscave.mcmines.managers.MineManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command("mine")
public class CommandMine {
    private static final McMines plugin = McMines.getInstance();
    private static final MineManager mineManager = plugin.getMineManager();

    //@CommandPlaceholder
    //@CommandPermission("mcmines.help")
    //public void help(@NotNull CommandSender sender) {
    //}

    @Subcommand("reload")
    @CommandPermission("mcmines.reload")
    public void reload(@NotNull CommandSender sender) {
        plugin.getConfiguration().reload();
        plugin.getLanguage().reload();
        plugin.getHooks().reload();
        plugin.getGuis().reload();
        mineManager.reloadConfiguration();
        if (!mineManager.getAllMines().isEmpty()) mineManager.resetAllMines();

        sender.sendMessage(Component.text("McMines configuration reloaded successfully!", NamedTextColor.GREEN));
    }

    @Subcommand("editor")
    @CommandPermission("mcmines.edit")
    public void openEditor(@NotNull Player player) {
        new MineSelectorMenu(MenuController.getMenuUtils(player)).open();
    }
}