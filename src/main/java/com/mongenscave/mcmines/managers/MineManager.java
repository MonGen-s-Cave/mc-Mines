package com.mongenscave.mcmines.managers;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.config.Config;
import com.mongenscave.mcmines.data.BlockData;
import com.mongenscave.mcmines.models.Mine;
import com.mongenscave.mcmines.utils.LoggerUtils;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class MineManager {
    @Getter private static MineManager instance;
    private final ConcurrentHashMap<String, Mine> mines = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MyScheduledTask> resetTasks = new ConcurrentHashMap<>();
    private static final McMines plugin = McMines.getInstance();
    private Config minesConfig;

    public MineManager() {
        instance = this;
        initializeConfig();
        loadMines();
    }

    private void initializeConfig() {
        try {
            minesConfig = new Config(
                    new File(plugin.getDataFolder(), "mines.yml"),
                    plugin.getResource("mines.yml"),
                    GeneralSettings.builder().setUseDefaults(false).build(),
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setKeepAll(true).build()
            );
        } catch (Exception e) {
            LoggerUtils.error("Failed to initialize mines.yml: " + e.getMessage());
        }
    }

    public void loadMines() {
        mines.clear();

        if (minesConfig == null) return;

        if (minesConfig.get("mines") == null || minesConfig.getSection("mines") == null) {
            LoggerUtils.info("No mines section found in configuration. Starting with empty mines list.");
            return;
        }

        Set<String> mineNames = minesConfig.getSection("mines").getRoutesAsStrings(false);

        for (String mineName : mineNames) {
            try {
                Map<String, Object> mineSection = minesConfig.getSection("mines." + mineName).getStringRouteMappedValues(false);
                Mine mine = Mine.loadFromConfig(mineName, mineSection);
                mines.put(mineName, mine);

                if (mine.isValidMineArea()) startResetTask(mine);
                LoggerUtils.info("Loaded mine: " + mineName);
            } catch (Exception e) {
                LoggerUtils.error("Failed to load mine '" + mineName + "': " + e.getMessage());
            }
        }

        LoggerUtils.info("Loaded " + mines.size() + " mines");
    }

    public void saveMines() {
        if (minesConfig == null) return;

        try {
            minesConfig.set("mines", null);

            for (Mine mine : mines.values()) {
                Map<String, Object> mineSection = new HashMap<>();
                mine.saveToConfig(mineSection);
                minesConfig.set("mines." + mine.getName(), mineSection);
            }

            minesConfig.save();
            LoggerUtils.info("Saved " + mines.size() + " mines to mines.yml");
        } catch (Exception e) {
            LoggerUtils.error("Failed to save mines: " + e.getMessage());
        }
    }

    public void createMine(@NotNull String name, int resetAfter) {
        Mine mine = new Mine(name, resetAfter);
        mines.put(name, mine);
        saveMines();
        LoggerUtils.info("Created new mine: " + name);
    }

    public boolean deleteMine(@NotNull String name) {
        Mine mine = mines.remove(name);
        if (mine != null) {
            MyScheduledTask task = resetTasks.remove(name);
            if (task != null) task.cancel();
            saveMines();
            LoggerUtils.info("Deleted mine: " + name);
            return true;
        }
        return false;
    }

    @Nullable
    public Mine getMine(@NotNull String name) {
        return mines.get(name);
    }

    @NotNull
    public Collection<Mine> getAllMines() {
        return mines.values();
    }

    @NotNull
    public Set<String> getMineNames() {
        return mines.keySet();
    }

    public void resetMine(@NotNull String name) {
        Mine mine = getMine(name);
        if (mine == null || !mine.isValidMineArea()) {
            LoggerUtils.error("Cannot reset mine '" + name + "': mine not found or invalid area");
            return;
        }

        resetMine(mine);
    }

    public void resetMine(@NotNull Mine mine) {
        if (!mine.isValidMineArea()) {
            LoggerUtils.error("Cannot reset mine '" + mine.getName() + "': invalid mine area");
            return;
        }

        Location pos1 = mine.getMineAreaPos1();
        Location pos2 = mine.getMineAreaPos2();

        if (pos1.getWorld() == null || pos2.getWorld() == null || !pos1.getWorld().equals(pos2.getWorld())) {
            LoggerUtils.error("Cannot reset mine '" + mine.getName() + "': invalid world");
            return;
        }

        int minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        int maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        int minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        int maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        int minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());
        int maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());

        List<BlockData> blockDataList = mine.getBlockDataList();
        if (blockDataList.isEmpty()) {
            LoggerUtils.warn("Mine '" + mine.getName() + "' has no block data configured");
            return;
        }

        int totalChance = blockDataList.stream().mapToInt(BlockData::chance).sum();
        if (totalChance <= 0) {
            LoggerUtils.error("Mine '" + mine.getName() + "' has invalid total chance: " + totalChance);
            return;
        }

        int blocksReset = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location blockLocation = new Location(pos1.getWorld(), x, y, z);
                    Material material = selectRandomMaterial(blockDataList, totalChance);

                    if (material != null) {
                        blockLocation.getBlock().setType(material);
                        blocksReset++;
                    }
                }
            }
        }

        LoggerUtils.info("Reset mine '" + mine.getName() + "' - " + blocksReset + " blocks changed");

        startResetTask(mine);
    }

    @Nullable
    private Material selectRandomMaterial(@NotNull List<BlockData> blockDataList, int totalChance) {
        int randomValue = ThreadLocalRandom.current().nextInt(totalChance);
        int currentChance = 0;

        for (BlockData blockData : blockDataList) {
            currentChance += blockData.chance();
            if (randomValue < currentChance) {
                try {
                    return Material.valueOf(blockData.material().toUpperCase());
                } catch (IllegalArgumentException e) {
                    LoggerUtils.error("Invalid material: " + blockData.material());
                    return null;
                }
            }
        }

        try {
            return Material.valueOf(blockDataList.getFirst().material().toUpperCase());
        } catch (Exception e) {
            LoggerUtils.error("Failed to select material from block data list");
            return null;
        }
    }

    private void startResetTask(@NotNull Mine mine) {
        MyScheduledTask existingTask = resetTasks.remove(mine.getName());
        if (existingTask != null) existingTask.cancel();

        if (mine.getResetAfter() <= 0) {
            LoggerUtils.warn("Mine '" + mine.getName() + "' has invalid reset time: " + mine.getResetAfter());
            return;
        }

        MyScheduledTask task = plugin.getScheduler().runTaskLater(() -> resetMine(mine), mine.getResetAfter() * 20L);
        resetTasks.put(mine.getName(), task);
    }

    public void updateMine(@NotNull Mine mine) {
        mines.put(mine.getName(), mine);
        saveMines();

        if (mine.isValidMineArea()) startResetTask(mine);
    }

    public void shutdown() {
        resetTasks.values().forEach(MyScheduledTask::cancel);
        resetTasks.clear();

        saveMines();
        LoggerUtils.info("Mine manager shutdown complete");
    }

    public void resetAllMines() {
        for (Mine mine : mines.values()) {
            if (mine.isValidMineArea()) {
                resetMine(mine);
            }
        }
        LoggerUtils.info("Reset all mines");
    }
}