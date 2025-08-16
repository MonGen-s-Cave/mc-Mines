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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class WaveReset extends Reset {
    public WaveReset(@NotNull McMines mcMines, @NotNull Consumer<Mine> onFinish) {
        super(mcMines, onFinish);
    }

    @Override
    protected void doResetVisual(@NotNull Mine mine, @NotNull ResetData data) {
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

        long totalBlocks = (long) (maxX - minX + 1) * (long) (maxY - minY + 1) * (long) (maxZ - minZ + 1);

        List<BlockData> input = mine.getBlockDataList();

        if (input.isEmpty()) return;

        var platforms = McMines.getInstance().getBlockPlatforms();
        record Weighted(BlockPlatforms.Placement placement, int weight) {}

        List<Weighted> weighted = Collections.synchronizedList(new ArrayList<>());
        int totalWeight = 0;

        for (BlockData bd : input) {
            try {
                if (bd.chance() > 0) {
                    var placement = platforms.resolveForReset(bd.material());
                    weighted.add(new Weighted(placement, bd.chance()));
                    totalWeight += bd.chance();
                }
            } catch (Exception exception) {
                LoggerUtils.error(exception.getMessage());
            }
        }

        if (totalWeight <= 0 || weighted.isEmpty()) return;

        int n = weighted.size();
        int[] prefix = new int[n];

        for (int i = 0, sum = 0; i < n; i++) {
            sum += weighted.get(i).weight();
            prefix[i] = sum;
        }

        List<Location> allPositions = Collections.synchronizedList(new ArrayList<>());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    allPositions.add(new Location(world, x, y, z));
                }
            }
        }

        WaveComparator waveComparator = new WaveComparator(minX, maxX, minY, maxY, minZ, maxZ, data.direction());
        allPositions.sort(waveComparator);

        cancel(mine.getName());

        ThreadLocalRandom random = ThreadLocalRandom.current();
        AtomicLong placed = new AtomicLong(0);
        int[] currentIndex = {0};

        int finalTotalWeight = totalWeight;

        MyScheduledTask task = plugin.getScheduler().runTaskTimer(() -> {
           int work = 0;

           while (work < data.blocksPerTick() && currentIndex[0] < allPositions.size()) {
               Location pos = allPositions.get(currentIndex[0]);
               currentIndex[0]++;

               int r = random.nextInt(finalTotalWeight);
               int idx = Arrays.binarySearch(prefix, r + 1);
               if (idx < 0) idx = -idx - 1;

               try {
                   weighted.get(idx).placement.placeAt(pos);

                   var p = data.particle();
                   if (p.count() > 0) {
                       world.spawnParticle(
                               p.type(),
                               pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                               p.count(), p.offsetX(), p.offsetY(), p.offsetZ(), p.speed()
                       );
                   }

                   long now = placed.incrementAndGet();
                   var s = data.sound();

                   if (now % s.everyPlacement() == 0) {
                       float progress = totalBlocks > 0 ? (float) Math.min(1.0, now / (double) totalBlocks) : 0f;
                       float pitch = s.pitchStart() + (s.pitchEnd() - s.pitchStart()) * progress;
                       world.playSound(pos, s.type(), s.volume(), pitch);
                   }
               } catch (Throwable ignored) {}
               work++;
           }

            if (currentIndex[0] >= allPositions.size()) {
                cancel(mine.getName());
                onFinish.accept(mine);
           }
        }, 0L, data.tickPeriod());

        running.put(mine.getName(), task);
    }

    private static final class WaveComparator implements Comparator<Location> {
        private final int minX, maxX, minY, maxY, minZ, maxZ;
        private final ResetDirection direction;
        private final double centerX, centerY, centerZ;
        private final double maxDistance;

        WaveComparator(int minX, int maxX, int minY, int maxY, int minZ, int maxZ, ResetDirection direction) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.direction = direction;

            this.centerX = (minX + maxX) / 2.0;
            this.centerY = (minY + maxY) / 2.0;
            this.centerZ = (minZ + maxZ) / 2.0;

            double maxDx = Math.max(Math.abs(minX - centerX), Math.abs(maxX - centerX));
            double maxDy = Math.max(Math.abs(minY - centerY), Math.abs(maxY - centerY));
            double maxDz = Math.max(Math.abs(minZ - centerZ), Math.abs(maxZ - centerZ));
            this.maxDistance = Math.sqrt(maxDx * maxDx + maxDy * maxDy + maxDz * maxDz);
        }

        @Override
        public int compare(Location a, Location b) {
            double priorityA = calculateWavePrioirty(a);
            double priorityB = calculateWavePrioirty(b);

            return Double.compare(priorityA, priorityB);
        }

        private double calculateWavePrioirty(@NotNull Location location) {
            double x = location.getX();
            double y = location.getY();
            double z = location.getZ();

            // amp = 2.0
            // freq = 0.5
            // speed = 1.0

            double dx = x - centerX;
            double dy = y - centerY;
            double dz = z - centerZ;
            double distanceFromCenter = Math.sqrt(dx * dx + dy * dy + dz * dz);
            double normalizedDistance = maxDistance > 0 ? distanceFromCenter / maxDistance : 0;
            double wavePhase = normalizedDistance * 0.5 * 2 * Math.PI;
            double waveOffset = Math.sin(wavePhase) * 2.0;

            double basePriority = switch (direction) {
                case WEST_EAST -> x - minX;
                case EAST_WEST -> maxX - x;
                case NORTH_SOUTH -> z - minZ;
                case SOUTH_NORTH -> maxZ - z;
                case DOWN_UP -> y - minY;
                case UP_DOWN -> maxY - y;
            };

            double secondaryPriority = switch (direction) {
                case WEST_EAST, EAST_WEST -> (z - minZ) * 0.1 + (y - minY) * 0.05;
                case NORTH_SOUTH, SOUTH_NORTH -> (x - minX) * 0.1 + (y - minY) * 0.05;
                case DOWN_UP, UP_DOWN -> (x - minX) * 0.1 + (z - minZ) * 0.05;
            };

            return basePriority + waveOffset + secondaryPriority;
        }
    }
}
