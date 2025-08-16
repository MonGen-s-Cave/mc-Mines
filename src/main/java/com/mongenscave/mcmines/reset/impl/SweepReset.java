package com.mongenscave.mcmines.reset.impl;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.block.BlockPlatforms;
import com.mongenscave.mcmines.data.BlockData;
import com.mongenscave.mcmines.data.ResetData;
import com.mongenscave.mcmines.identifiers.ResetDirection;
import com.mongenscave.mcmines.models.Mine;
import com.mongenscave.mcmines.reset.Reset;
import com.mongenscave.mcmines.utils.LoggerUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class SweepReset extends Reset {
    public SweepReset(@NotNull McMines plugin, @NotNull Consumer<Mine> onFinish) {
        super(plugin, onFinish);
    }

    @Override
    protected void doResetVisual(@NotNull Mine mine, @NotNull ResetData settings) {
        Location a = mine.getMineAreaPos1();
        Location b = mine.getMineAreaPos2();
        if (a == null || b == null) return;
        World world = a.getWorld();
        if (world == null || !world.equals(b.getWorld())) return;

        int minX = Math.min(a.getBlockX(), b.getBlockX());
        int maxX = Math.max(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY());
        int maxY = Math.max(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        int maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

        long totalBlocks =
                (long) (maxX - minX + 1) *
                        (long) (maxY - minY + 1) *
                        (long) (maxZ - minZ + 1);

        List<BlockData> input = mine.getBlockDataList();
        if (input.isEmpty()) {
            LoggerUtils.warn("Mine '" + mine.getName() + "' has no block data configured");
            return;
        }

        var platforms = McMines.getInstance().getBlockPlatforms();
        record Weighted(BlockPlatforms.Placement placement, int weight) {}
        List<Weighted> weighted = new ArrayList<>();
        int totalWeight = 0;

        for (BlockData bd : input) {
            try {
                if (bd.chance() > 0) {
                    var placement = platforms.resolveForReset(bd.material());
                    weighted.add(new Weighted(placement, bd.chance()));
                    totalWeight += bd.chance();
                }
            } catch (Exception ex) {
                LoggerUtils.error("Skipping invalid block: " + bd.material() + " (" + ex.getMessage() + ")");
            }
        }
        if (totalWeight <= 0 || weighted.isEmpty()) {
            LoggerUtils.error("Mine '" + mine.getName() + "' has invalid total chance: " + totalWeight);
            return;
        }

        int n = weighted.size();
        int[] prefix = new int[n];
        for (int i = 0, sum = 0; i < n; i++) { sum += weighted.get(i).weight; prefix[i] = sum; }

        AxisIterator it = new AxisIterator(minX, maxX, minY, maxY, minZ, maxZ, settings.direction());
        cancel(mine.getName());

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Location cursor = new Location(world, 0, 0, 0);
        AtomicLong placed = new AtomicLong(0);

        int finalTotalWeight = totalWeight;
        MyScheduledTask task = plugin.getScheduler().runTaskTimer(() -> {
            int work = 0;

            while (work < settings.blocksPerTick() && it.hasNext()) {
                it.nextInto(cursor);

                int r = rng.nextInt(finalTotalWeight);
                int idx = Arrays.binarySearch(prefix, r + 1);
                if (idx < 0) idx = -idx - 1;

                try {
                    weighted.get(idx).placement.placeAt(cursor);

                    var p = settings.particle();
                    if (p.count() > 0) {
                        world.spawnParticle(
                                p.type(),
                                cursor.getX() + 0.5, cursor.getY() + 0.5, cursor.getZ() + 0.5,
                                p.count(), p.offsetX(), p.offsetY(), p.offsetZ(), p.speed()
                        );
                    }

                    long now = placed.incrementAndGet();
                    var s = settings.sound();
                    if (now % s.everyPlacement() == 0) {
                        float progress = totalBlocks > 0 ? (float) Math.min(1.0, now / (double) totalBlocks) : 0f;
                        float pitch = s.pitchStart() + (s.pitchEnd() - s.pitchStart()) * progress;
                        world.playSound(cursor, s.type(), s.volume(), pitch);
                    }
                } catch (Throwable t) {
                    LoggerUtils.error("Failed to place block at " +
                            cursor.getBlockX() + "," + cursor.getBlockY() + "," + cursor.getBlockZ() + ": " + t.getMessage());
                }
                work++;
            }

            if (!it.hasNext()) {
                cancel(mine.getName());
                LoggerUtils.info("Reset (visual) mine '" + mine.getName() + "' - " + placed.get() + " blocks changed");
                onFinish.accept(mine);
            }
        }, 0L, settings.tickPeriod());

        running.put(mine.getName(), task);
    }

    private static final class AxisIterator {
        private final int minX, maxX, minY, maxY, minZ, maxZ;
        private final ResetDirection dir;
        private int x, y, z;
        private boolean hasNext;

        AxisIterator(int minX, int maxX, int minY, int maxY, int minZ, int maxZ, @NotNull ResetDirection dir) {
            this.minX = minX; this.maxX = maxX;
            this.minY = minY; this.maxY = maxY;
            this.minZ = minZ; this.maxZ = maxZ;
            this.dir = dir;
            switch (dir) {
                case WEST_EAST -> { x = minX; y = minY; z = minZ; }
                case EAST_WEST -> { x = maxX; y = minY; z = minZ; }
                case NORTH_SOUTH -> { z = minZ; y = minY; x = minX; }
                case SOUTH_NORTH -> { z = maxZ; y = minY; x = minX; }
                case DOWN_UP -> { y = minY; x = minX; z = minZ; }
                case UP_DOWN -> { y = maxY; x = minX; z = minZ; }
            }
            hasNext = true;
        }

        boolean hasNext() { return hasNext; }

        void nextInto(@NotNull Location out) {
            out.set(x, y, z);
            advance();
        }

        private void advance() {
            switch (dir) {
                case WEST_EAST -> stepXYZ(+1);
                case EAST_WEST -> stepXYZ(-1);
                case NORTH_SOUTH -> stepZXY(+1);
                case SOUTH_NORTH -> stepZXY(-1);
                case DOWN_UP -> stepYXZ(+1);
                case UP_DOWN -> stepYXZ(-1);
            }
        }

        private void stepXYZ(int xDir) {
            y++;
            if (y > maxY) { y = minY; z++; }
            if (z > maxZ) { z = minZ; x += xDir; }
            if (xDir > 0 ? x > maxX : x < minX) hasNext = false;
        }

        private void stepZXY(int zDir) {
            y++;
            if (y > maxY) { y = minY; x++; }
            if (x > maxX) { x = minX; z += zDir; }
            if (zDir > 0 ? z > maxZ : z < minZ) hasNext = false;
        }

        private void stepYXZ(int yDir) {
            x++;
            if (x > maxX) { x = minX; z++; }
            if (z > maxZ) { z = minZ; y += yDir; }
            if (yDir > 0 ? y > maxY : y < minY) hasNext = false;
        }
    }
}