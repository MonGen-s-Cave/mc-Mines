package com.mongenscave.mcmines.managers;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.block.BlockPlatforms;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    public synchronized void renameMine(@NotNull Mine mine, @NotNull String newName) {
        String oldName = mine.getName();
        if (oldName.equalsIgnoreCase(newName)) return;

        if (getMine(newName) != null) {
            throw new IllegalArgumentException("Mine already exists: " + newName);
        }

        int resetAfter = mine.getResetAfter();
        Location minePos1 = mine.getMineAreaPos1();
        Location minePos2 = mine.getMineAreaPos2();
        Location entrancePos1 = mine.getEntranceAreaPos1();
        Location entrancePos2 = mine.getEntranceAreaPos2();
        String entrancePerm = mine.getEntrancePermission();

        List<BlockData> blocks = new ArrayList<>(mine.getBlockDataList());

        deleteMine(oldName);

        createMine(newName, resetAfter);
        Mine renamed = getMine(newName);
        if (renamed == null) {
            createMine(oldName, resetAfter);
            Mine restored = getMine(oldName);
            if (restored != null) {
                if (minePos1 != null) restored.setMineAreaPos1(minePos1);
                if (minePos2 != null) restored.setMineAreaPos2(minePos2);
                if (entrancePos1 != null) restored.setEntranceAreaPos1(entrancePos1);
                if (entrancePos2 != null) restored.setEntranceAreaPos2(entrancePos2);
                restored.setEntrancePermission(entrancePerm);

                if (!restored.getBlockDataList().isEmpty()) {
                    for (BlockData existing : new ArrayList<>(restored.getBlockDataList())) {
                        restored.removeBlockData(existing.material());
                    }
                }
                for (BlockData bd : blocks) restored.addBlockData(bd.material(), bd.chance());
                updateMine(restored);
            }
            throw new IllegalStateException("Failed to create renamed mine: " + newName);
        }

        if (minePos1 != null) renamed.setMineAreaPos1(minePos1);
        if (minePos2 != null) renamed.setMineAreaPos2(minePos2);
        if (entrancePos1 != null) renamed.setEntranceAreaPos1(entrancePos1);
        if (entrancePos2 != null) renamed.setEntranceAreaPos2(entrancePos2);
        renamed.setEntrancePermission(entrancePerm);

        if (!renamed.getBlockDataList().isEmpty()) {
            for (BlockData existing : new ArrayList<>(renamed.getBlockDataList())) {
                renamed.removeBlockData(existing.material());
            }
        }
        for (BlockData bd : blocks) {
            renamed.addBlockData(bd.material(), bd.chance());
        }

        updateMine(renamed);
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

    @SuppressWarnings("all")
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

        var platforms = McMines.getInstance().getBlockPlatforms();
        record Weighted(BlockPlatforms.Placement p, int w) {}
        List<Weighted> weighted = new java.util.ArrayList<>();
        int total = 0;

        for (BlockData bd : blockDataList) {
            try {
                if (bd.chance() > 0) {
                    var placement = platforms.resolveForReset(bd.material());
                    weighted.add(new Weighted(placement, bd.chance()));
                    total += bd.chance();
                }
            } catch (Exception ex) {
                LoggerUtils.error("Skipping invalid block: " + bd.material() + " (" + ex.getMessage() + ")");
            }
        }

        if (total <= 0 || weighted.isEmpty()) {
            LoggerUtils.error("Mine '" + mine.getName() + "' has invalid total chance: " + total);
            return;
        }

        final int n = weighted.size();
        final int[] prefix = new int[n];
        for (int i = 0, sum = 0; i < n; i++) { sum += weighted.get(i).w; prefix[i] = sum; }

        var world = pos1.getWorld();
        var rng = java.util.concurrent.ThreadLocalRandom.current();
        final Location loc = new Location(world, 0, 0, 0);
        int blocksReset = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    int r = rng.nextInt(total);
                    int idx = java.util.Arrays.binarySearch(prefix, r + 1);
                    if (idx < 0) idx = -idx - 1;

                    loc.set(x, y, z);
                    try {
                        weighted.get(idx).p.placeAt(loc);
                        blocksReset++;
                    } catch (Throwable t) {
                        LoggerUtils.error("Failed to place block at " + x + "," + y + "," + z + ": " + t.getMessage());
                    }
                }
            }
        }

        LoggerUtils.info("Reset mine '" + mine.getName() + "' - " + blocksReset + " blocks changed");
        startResetTask(mine);
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