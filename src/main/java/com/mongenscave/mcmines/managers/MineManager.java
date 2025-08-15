package com.mongenscave.mcmines.managers;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.block.BlockPlatforms;
import com.mongenscave.mcmines.config.Config;
import com.mongenscave.mcmines.data.BlockData;
import com.mongenscave.mcmines.models.Mine;
import com.mongenscave.mcmines.reset.ResetManager;
import com.mongenscave.mcmines.reset.model.DefaultSweepVisualReset;
import com.mongenscave.mcmines.utils.LoggerUtils;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class MineManager {
    @Getter private static MineManager instance;

    private final ConcurrentHashMap<String, Mine> mines = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MyScheduledTask> resetTasks = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> resetDueAtMillis = new ConcurrentHashMap<>();

    private static final class CachedPercent {
        final double percent;
        final long   timestamp;
        CachedPercent(double p, long ts) { this.percent = p; this.timestamp = ts; }
    }
    private final ConcurrentHashMap<String, CachedPercent> fillCache = new ConcurrentHashMap<>();

    private static final McMines plugin = McMines.getInstance();
    private Config minesConfig;

    private final ResetManager visualResetter =
            new DefaultSweepVisualReset(
                    McMines.getInstance(),
                    mine -> { fillCache.remove(mine.getName()); startResetTask(mine); }
            );

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
        resetTasks.values().forEach(MyScheduledTask::cancel);
        resetTasks.clear();
        resetDueAtMillis.clear();
        fillCache.clear();

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
        if (mine.isValidMineArea()) startResetTask(mine);
        LoggerUtils.info("Created new mine: " + name);
    }

    public boolean deleteMine(@NotNull String name) {
        Mine mine = mines.remove(name);

        if (mine != null) {
            MyScheduledTask task = resetTasks.remove(name);
            if (task != null) task.cancel();
            resetDueAtMillis.remove(name);
            fillCache.remove(name);
            visualResetter.cancel(name);
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

        Long due = resetDueAtMillis.get(oldName);
        int remainingSecs = -1;
        if (due != null) {
            long diffMs = due - System.currentTimeMillis();
            if (diffMs > 0) remainingSecs = (int) ((diffMs + 999) / 1000L);
            else remainingSecs = 0;
        }

        Mine renamed = getRenamed(mine, newName);

        visualResetter.cancel(oldName);

        MyScheduledTask t = resetTasks.remove(oldName);
        if (t != null) t.cancel();
        resetDueAtMillis.remove(oldName);
        fillCache.remove(oldName);

        mines.remove(oldName);
        mines.put(newName, renamed);

        saveMines();

        if (renamed.isValidMineArea()) {
            if (remainingSecs > 0) {
                long newDueAt = System.currentTimeMillis() + remainingSecs * 1000L;
                resetDueAtMillis.put(newName, newDueAt);
                MyScheduledTask nt = plugin.getScheduler()
                        .runTaskLater(() -> resetMine(renamed), remainingSecs * 20L);
                resetTasks.put(newName, nt);
            } else {
                startResetTask(renamed);
            }
        }
    }

    @NotNull
    private static Mine getRenamed(@NotNull Mine mine, @NotNull String newName) {
        Mine renamed = new Mine(newName, mine.getResetAfter());
        renamed.setMineAreaPos1(mine.getMineAreaPos1());
        renamed.setMineAreaPos2(mine.getMineAreaPos2());
        renamed.setEntranceAreaPos1(mine.getEntranceAreaPos1());
        renamed.setEntranceAreaPos2(mine.getEntranceAreaPos2());
        renamed.setEntrancePermission(mine.getEntrancePermission());

        for (BlockData bd : new ArrayList<>(mine.getBlockDataList())) {
            renamed.addBlockData(bd.material(), bd.chance());
        }
        return renamed;
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

        if (visualResetter.isEnabled()) {
            visualResetter.resetVisual(mine);
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
        var rng = ThreadLocalRandom.current();
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
        fillCache.remove(mine.getName());
        startResetTask(mine);
    }

    private void startResetTask(@NotNull Mine mine) {
        MyScheduledTask existingTask = resetTasks.remove(mine.getName());
        if (existingTask != null) existingTask.cancel();

        if (mine.getResetAfter() <= 0) {
            LoggerUtils.warn("Mine '" + mine.getName() + "' has invalid reset time: " + mine.getResetAfter());
            resetDueAtMillis.remove(mine.getName());
            return;
        }

        long dueAt = System.currentTimeMillis() + mine.getResetAfter() * 1000L;
        resetDueAtMillis.put(mine.getName(), dueAt);

        MyScheduledTask task = plugin.getScheduler().runTaskLater(() -> resetMine(mine), mine.getResetAfter() * 20L);
        resetTasks.put(mine.getName(), task);
    }

    public void updateMine(@NotNull Mine mine) {
        mines.put(mine.getName(), mine);
        saveMines();

        if (mine.isValidMineArea()) startResetTask(mine);
        else {
            MyScheduledTask t = resetTasks.remove(mine.getName());
            if (t != null) t.cancel();
            resetDueAtMillis.remove(mine.getName());
            fillCache.remove(mine.getName());
        }
    }

    public void shutdown() {
        resetTasks.values().forEach(MyScheduledTask::cancel);
        resetTasks.clear();
        resetDueAtMillis.clear();
        fillCache.clear();

        visualResetter.shutdown();
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

    public int getSecondsUntilReset(@NotNull Mine mine) {
        Long due = resetDueAtMillis.get(mine.getName());
        if (due == null) return -1;
        long diff = due - System.currentTimeMillis();
        if (diff <= 0) return 0;
        return (int) ((diff + 999) / 1000L);
    }

    public double getRemainingPercent(@NotNull Mine mine, long cacheMillis) {
        String key = mine.getName();
        long now = System.currentTimeMillis();

        CachedPercent cached = fillCache.get(key);
        if (cached != null && (now - cached.timestamp) < cacheMillis) {
            return cached.percent;
        }

        double percent = computeRemainingPercentNow(mine);
        if (percent >= 0) {
            fillCache.put(key, new CachedPercent(percent, now));
        }
        return percent;
    }

    private double computeRemainingPercentNow(@NotNull Mine mine) {
        Location a = mine.getMineAreaPos1();
        Location b = mine.getMineAreaPos2();
        if (a == null || b == null) return -1;
        World w1 = a.getWorld();
        World w2 = b.getWorld();
        if (w1 == null || !w1.equals(w2)) return -1;

        int minX = Math.min(a.getBlockX(), b.getBlockX());
        int maxX = Math.max(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY());
        int maxY = Math.max(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        int maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

        long volume = (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        if (volume <= 0) return -1;

        long nonAir = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Material type = w1.getBlockAt(x, y, z).getType();
                    if (type != Material.AIR && type != Material.CAVE_AIR && type != Material.VOID_AIR) {
                        nonAir++;
                    }
                }
            }
        }

        double percent = (nonAir * 100.0) / volume;
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        return percent;
    }
}
