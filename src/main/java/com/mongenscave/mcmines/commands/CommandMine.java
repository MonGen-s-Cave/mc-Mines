package com.mongenscave.mcmines.commands;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.data.BlockData;
import com.mongenscave.mcmines.managers.MineManager;
import com.mongenscave.mcmines.models.Mine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.orphan.OrphanCommand;

import java.util.Set;

@Command("mine")
public class CommandMine {
    private static final McMines plugin = McMines.getInstance();
    private static final MineManager mineManager = plugin.getMineManager();

    @CommandPlaceholder
    @CommandPermission("mcmines.help")
    public void help(@NotNull CommandSender sender) {
        sender.sendMessage(Component.text("=== McMines Commands ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/mine help - Show this help", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine list - List all mines", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine info <mine> - Show mine information", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine create <name> [resetTime] - Create a new mine", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine delete <name> - Delete a mine", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine reset <name> - Reset a mine", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine resetall - Reset all mines", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine setpos1 <name> - Set mine area position 1", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine setpos2 <name> - Set mine area position 2", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine setentrance1 <name> - Set entrance area position 1", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine setentrance2 <name> - Set entrance area position 2", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine addblock <name> <material> <chance> - Add block to mine", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine removeblock <name> <material> - Remove block from mine", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine setpermission <name> [permission] - Set entrance permission", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine setresettime <name> <seconds> - Set reset time", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/mine reload - Reload configuration", NamedTextColor.YELLOW));
    }

    @Subcommand("reload")
    @CommandPermission("mcmines.reload")
    public void reload(@NotNull CommandSender sender) {
        plugin.getConfiguration().reload();
        plugin.getLanguage().reload();
        mineManager.loadMines();
        mineManager.resetAllMines();

        sender.sendMessage(Component.text("McMines configuration reloaded!", NamedTextColor.GREEN));
    }

    @Subcommand("list")
    @CommandPermission("mcmines.list")
    public void list(@NotNull CommandSender sender) {
        Set<String> mineNames = mineManager.getMineNames();

        if (mineNames.isEmpty()) {
            sender.sendMessage(Component.text("No mines found!", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== Mines (" + mineNames.size() + ") ===", NamedTextColor.GOLD));
        for (String mineName : mineNames) {
            Mine mine = mineManager.getMine(mineName);
            String status = mine != null && mine.isValidMineArea() ? "ACTIVE" : "INACTIVE";
            NamedTextColor statusColor = mine != null && mine.isValidMineArea() ? NamedTextColor.GREEN : NamedTextColor.RED;

            sender.sendMessage(Component.text("- " + mineName + " (", NamedTextColor.YELLOW)
                    .append(Component.text(status, statusColor))
                    .append(Component.text(")", NamedTextColor.YELLOW)));
        }
    }

    @Subcommand("info")
    @CommandPermission("mcmines.info")
    public void info(@NotNull CommandSender sender, @NotNull String mineName) {
        Mine mine = mineManager.getMine(mineName);
        if (mine == null) {
            sender.sendMessage(Component.text("Mine '" + mineName + "' not found!", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== Mine: " + mineName + " ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Reset Time: " + mine.getResetAfter() + " seconds", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("Mine Area Valid: " + (mine.isValidMineArea() ? "Yes" : "No"),
                mine.isValidMineArea() ? NamedTextColor.GREEN : NamedTextColor.RED));
        sender.sendMessage(Component.text("Entrance Area Valid: " + (mine.isValidEntranceArea() ? "Yes" : "No"),
                mine.isValidEntranceArea() ? NamedTextColor.GREEN : NamedTextColor.RED));

        if (mine.getEntrancePermission() != null) {
            sender.sendMessage(Component.text("Entrance Permission: " + mine.getEntrancePermission(), NamedTextColor.YELLOW));
        }

        sender.sendMessage(Component.text("Block Data:", NamedTextColor.AQUA));
        if (mine.getBlockDataList().isEmpty()) {
            sender.sendMessage(Component.text("  No blocks configured", NamedTextColor.RED));
        } else {
            for (BlockData blockData : mine.getBlockDataList()) {
                sender.sendMessage(Component.text("  - " + blockData.material() + ": " + blockData.chance() + "%", NamedTextColor.WHITE));
            }
        }
    }

    @Subcommand("create")
    @CommandPermission("mcmines.create")
    public void create(@NotNull CommandSender sender, @NotNull String name, @Optional @Default("300") int resetTime) {
        if (mineManager.getMine(name) != null) {
            sender.sendMessage(Component.text("Mine '" + name + "' already exists!", NamedTextColor.RED));
            return;
        }

        if (resetTime <= 0) {
            sender.sendMessage(Component.text("Reset time must be greater than 0!", NamedTextColor.RED));
            return;
        }

        mineManager.createMine(name, resetTime);
        sender.sendMessage(Component.text("Mine '" + name + "' created with " + resetTime + " second reset time!", NamedTextColor.GREEN));
        sender.sendMessage(Component.text("Use /mine setpos1 and /mine setpos2 to set the mine area.", NamedTextColor.YELLOW));
    }

    @Subcommand("delete")
    @CommandPermission("mcmines.delete")
    public void delete(@NotNull CommandSender sender, @NotNull String name) {
        if (!mineManager.deleteMine(name)) {
            sender.sendMessage(Component.text("Mine '" + name + "' not found!", NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("Mine '" + name + "' deleted!", NamedTextColor.GREEN));
    }

    @Subcommand("reset")
    @CommandPermission("mcmines.reset")
    public void reset(@NotNull CommandSender sender, @NotNull String name) {
        Mine mine = mineManager.getMine(name);
        if (mine == null) {
            sender.sendMessage(Component.text("Mine '" + name + "' not found!", NamedTextColor.RED));
            return;
        }

        if (!mine.isValidMineArea()) {
            sender.sendMessage(Component.text("Mine '" + name + "' has invalid area! Set positions first.", NamedTextColor.RED));
            return;
        }

        mineManager.resetMine(mine);
        sender.sendMessage(Component.text("Mine '" + name + "' has been reset!", NamedTextColor.GREEN));
    }

    @Subcommand("resetall")
    @CommandPermission("mcmines.reset.all")
    public void resetAll(@NotNull CommandSender sender) {
        mineManager.resetAllMines();
        sender.sendMessage(Component.text("All mines have been reset!", NamedTextColor.GREEN));
    }

    @Subcommand("setpos1")
    @CommandPermission("mcmines.setpos")
    public void setPos1(@NotNull Player player, @NotNull String name) {
        Mine mine = mineManager.getMine(name);
        if (mine == null) {
            player.sendMessage(Component.text("Mine '" + name + "' not found!", NamedTextColor.RED));
            return;
        }

        Location location = player.getLocation();
        mine.setMineAreaPos1(location);
        mineManager.updateMine(mine);

        player.sendMessage(Component.text("Mine area position 1 set for '" + name + "' at " +
                location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ(), NamedTextColor.GREEN));
    }

    @Subcommand("setpos2")
    @CommandPermission("mcmines.setpos")
    public void setPos2(@NotNull Player player, @NotNull String name) {
        Mine mine = mineManager.getMine(name);
        if (mine == null) {
            player.sendMessage(Component.text("Mine '" + name + "' not found!", NamedTextColor.RED));
            return;
        }

        Location location = player.getLocation();
        mine.setMineAreaPos2(location);
        mineManager.updateMine(mine);

        player.sendMessage(Component.text("Mine area position 2 set for '" + name + "' at " +
                location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ(), NamedTextColor.GREEN));
    }

    @Subcommand("setentrance1")
    @CommandPermission("mcmines.setpos")
    public void setEntrance1(@NotNull Player player, @NotNull String name) {
        Mine mine = mineManager.getMine(name);
        if (mine == null) {
            player.sendMessage(Component.text("Mine '" + name + "' not found!", NamedTextColor.RED));
            return;
        }

        Location location = player.getLocation();
        mine.setEntranceAreaPos1(location);
        mineManager.updateMine(mine);

        player.sendMessage(Component.text("Entrance area position 1 set for '" + name + "' at " +
                location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ(), NamedTextColor.GREEN));
    }

    @Subcommand("setentrance2")
    @CommandPermission("mcmines.setpos")
    public void setEntrance2(@NotNull Player player, @NotNull String name) {
        Mine mine = mineManager.getMine(name);
        if (mine == null) {
            player.sendMessage(Component.text("Mine '" + name + "' not found!", NamedTextColor.RED));
            return;
        }

        Location location = player.getLocation();
        mine.setEntranceAreaPos2(location);
        mineManager.updateMine(mine);

        player.sendMessage(Component.text("Entrance area position 2 set for '" + name + "' at " +
                location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ(), NamedTextColor.GREEN));
    }

    @Subcommand("addblock")
    @CommandPermission("mcmines.edit")
    public void addBlock(@NotNull CommandSender sender, @NotNull String name, @NotNull String material, int chance) {
        Mine mine = mineManager.getMine(name);
        if (mine == null) {
            sender.sendMessage(Component.text("Mine '" + name + "' not found!", NamedTextColor.RED));
            return;
        }

        // Validate material
        try {
            Material.valueOf(material.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid material: " + material, NamedTextColor.RED));
            return;
        }

        if (chance <= 0 || chance > 100) {
            sender.sendMessage(Component.text("Chance must be between 1 and 100!", NamedTextColor.RED));
            return;
        }

        // Remove existing block data with same material
        mine.removeBlockData(material.toUpperCase());
        mine.addBlockData(material.toUpperCase(), chance);
        mineManager.updateMine(mine);

        sender.sendMessage(Component.text("Added " + material.toUpperCase() + " with " + chance + "% chance to mine '" + name + "'!", NamedTextColor.GREEN));
    }

    @Subcommand("removeblock")
    @CommandPermission("mcmines.edit")
    public void removeBlock(@NotNull CommandSender sender, @NotNull String name, @NotNull String material) {
        Mine mine = mineManager.getMine(name);
        if (mine == null) {
            sender.sendMessage(Component.text("Mine '" + name + "' not found!", NamedTextColor.RED));
            return;
        }

        mine.removeBlockData(material.toUpperCase());
        mineManager.updateMine(mine);

        sender.sendMessage(Component.text("Removed " + material.toUpperCase() + " from mine '" + name + "'!", NamedTextColor.GREEN));
    }

    @Subcommand("setpermission")
    @CommandPermission("mcmines.edit")
    public void setPermission(@NotNull CommandSender sender, @NotNull String name, @Optional String permission) {
        Mine mine = mineManager.getMine(name);
        if (mine == null) {
            sender.sendMessage(Component.text("Mine '" + name + "' not found!", NamedTextColor.RED));
            return;
        }

        mine.setEntrancePermission(permission);
        mineManager.updateMine(mine);

        if (permission == null || permission.isEmpty()) {
            sender.sendMessage(Component.text("Entrance permission removed from mine '" + name + "'!", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Entrance permission set to '" + permission + "' for mine '" + name + "'!", NamedTextColor.GREEN));
        }
    }

    @Subcommand("setresettime")
    @CommandPermission("mcmines.edit")
    public void setResetTime(@NotNull CommandSender sender, @NotNull String name, int seconds) {
        Mine mine = mineManager.getMine(name);
        if (mine == null) {
            sender.sendMessage(Component.text("Mine '" + name + "' not found!", NamedTextColor.RED));
            return;
        }

        if (seconds <= 0) {
            sender.sendMessage(Component.text("Reset time must be greater than 0!", NamedTextColor.RED));
            return;
        }

        mine.setResetAfter(seconds);
        mineManager.updateMine(mine);

        sender.sendMessage(Component.text("Reset time set to " + seconds + " seconds for mine '" + name + "'!", NamedTextColor.GREEN));
    }
}