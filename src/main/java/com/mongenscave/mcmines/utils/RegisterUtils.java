package com.mongenscave.mcmines.utils;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.commands.CommandMine;
import com.mongenscave.mcmines.listeners.MenuListener;
import com.mongenscave.mcmines.listeners.MineEntranceListener;
import lombok.experimental.UtilityClass;
import revxrsal.commands.bukkit.BukkitLamp;

@UtilityClass
public class RegisterUtils {
    private static final McMines plugin = McMines.getInstance();

    public void registerListeners() {
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(), plugin);
        plugin.getServer().getPluginManager().registerEvents(new MineEntranceListener(), plugin);
    }

    public void registerCommands() {
        var lamp = BukkitLamp.builder(plugin)
                .build();

        lamp.register(new CommandMine());
    }
}
