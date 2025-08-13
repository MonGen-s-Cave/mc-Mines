package com.mongenscave.mcmines.listeners;

import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.managers.MineManager;
import com.mongenscave.mcmines.models.Mine;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

public final class MineEntranceListener implements Listener {
    private final MineManager mineManager = McMines.getInstance().getMineManager();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        var player = event.getPlayer();
        var to = event.getTo();

        for (Mine mine : mineManager.getAllMines()) {
            if (mine.getEntranceAreaPos1() == null || mine.getEntranceAreaPos2() == null) continue;

            String requiredPerm = mine.getEntrancePermission();
            if (requiredPerm == null || requiredPerm.isEmpty()) continue;

            if (isInside(to, mine.getEntranceAreaPos1(), mine.getEntranceAreaPos2())) {
                if (!player.hasPermission(requiredPerm)) {
                    event.setTo(event.getFrom());
                    player.sendMessage("§cNincs jogosultságod belépni ebbe a bányába! (§7" + requiredPerm + "§c)");
                }
                break;
            }
        }
    }

    private boolean isInside(@NotNull Location loc, @NotNull Location a, @NotNull Location b) {
        if (a.getWorld() == null || b.getWorld() == null || loc.getWorld() == null) return false;
        if (!a.getWorld().equals(loc.getWorld())) return false;
        if (!b.getWorld().equals(loc.getWorld())) return false;

        int minX = Math.min(a.getBlockX(), b.getBlockX());
        int maxX = Math.max(a.getBlockX(), b.getBlockX());
        int minY = Math.min(a.getBlockY(), b.getBlockY());
        int maxY = Math.max(a.getBlockY(), b.getBlockY());
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        int maxZ = Math.max(a.getBlockZ(), b.getBlockZ());

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }
}
