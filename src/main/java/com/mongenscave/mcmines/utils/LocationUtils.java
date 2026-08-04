package com.mongenscave.mcmines.utils;

import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class LocationUtils {
    @NotNull
    public String serialize(@NotNull Location location) {
        return location.getWorld().getName() + "," +
                location.getBlockX() + "," +
                location.getBlockY() + "," +
                location.getBlockZ();
    }

    @Nullable
    public Location deserialize(@NotNull String serializedLocation) {
        String[] parts = serializedLocation.split(",");
        if (parts.length != 4) return null;

        try {
            return new Location(
                    Bukkit.getWorld(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3])
            );
        } catch (Exception exception) {
            LoggerUtils.error("Could not deserialize location: " + serializedLocation);
            return null;
        }
    }

    @NotNull
    public String serializeExact(@NotNull Location location) {
        return location.getWorld().getName() + "," +
                location.getX() + "," +
                location.getY() + "," +
                location.getZ() + "," +
                location.getYaw() + "," +
                location.getPitch();
    }

    @Nullable
    public Location deserializeExact(@NotNull String serializedLocation) {
        String[] parts = serializedLocation.split(",");
        if (parts.length != 6) return null;

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) {
            LoggerUtils.error("Unknown world in location: " + serializedLocation);
            return null;
        }

        try {
            return new Location(
                    world,
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5])
            );
        } catch (Exception exception) {
            LoggerUtils.error("Could not deserialize location: " + serializedLocation);
            return null;
        }
    }

    public boolean isInsideCuboid(@NotNull Location loc, @Nullable Location a, @Nullable Location b) {
        if (a == null || b == null) return false;

        World world = loc.getWorld();
        if (world == null || !world.equals(a.getWorld()) || !world.equals(b.getWorld())) return false;

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return x >= Math.min(a.getBlockX(), b.getBlockX()) && x <= Math.max(a.getBlockX(), b.getBlockX())
                && y >= Math.min(a.getBlockY(), b.getBlockY()) && y <= Math.max(a.getBlockY(), b.getBlockY())
                && z >= Math.min(a.getBlockZ(), b.getBlockZ()) && z <= Math.max(a.getBlockZ(), b.getBlockZ());
    }
}
