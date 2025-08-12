package com.mongenscave.mcmines.utils;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.commands.CommandMine;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.orphan.Orphans;

@UtilityClass
public class RegisterUtils {
    private static final McMines plugin = McMines.getInstance();

    public void registerCommands() {
        var lamp = BukkitLamp.builder(plugin)
                .build();

        lamp.register(new CommandMine());
    }
}
