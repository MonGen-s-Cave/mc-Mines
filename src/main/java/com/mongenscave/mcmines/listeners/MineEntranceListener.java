package com.mongenscave.mcmines.listeners;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.identifiers.keys.MessageKeys;
import com.mongenscave.mcmines.managers.MineManager;
import com.mongenscave.mcmines.models.Mine;
import com.mongenscave.mcmines.processor.MessageProcessor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MineEntranceListener implements Listener {
    private final McMines plugin = McMines.getInstance();
    private final MineManager mineManager = plugin.getMineManager();

    private final Map<UUID, String> viewingMineByPlayer = new HashMap<>();
    private final Map<UUID, MyScheduledTask> actionbarTasks = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(@NotNull PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        Player player = event.getPlayer();
        Location to = event.getTo();

        for (Mine mine : mineManager.getAllMines()) {
            if (mine.getEntranceAreaPos1() == null || mine.getEntranceAreaPos2() == null) continue;

            if (isInside(to, mine.getEntranceAreaPos1(), mine.getEntranceAreaPos2())) {
                String requiredPerm = mine.getEntrancePermission();
                if (requiredPerm != null && !requiredPerm.isEmpty() && !player.hasPermission(requiredPerm)) {
                    event.setTo(event.getFrom());
                    player.sendMessage(MessageKeys.ENTRANCE_NO_PERMISSION.getMessage().replace("{permission}", requiredPerm));
                    stopActionbar(player.getUniqueId());
                    return;
                }
            }
        }

        Mine nearest = findNearestEntranceMineAt(to);

        if (nearest == null) {
            stopActionbar(player.getUniqueId());
            return;
        }

        ensureActionbar(player, nearest);
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        stopActionbar(event.getPlayer().getUniqueId());
    }

    private void ensureActionbar(@NotNull Player player, @NotNull Mine mine) {
        UUID id = player.getUniqueId();
        String current = viewingMineByPlayer.get(id);
        if (mine.getName().equalsIgnoreCase(current) && actionbarTasks.containsKey(id)) return;

        stopActionbar(id);

        viewingMineByPlayer.put(id, mine.getName());
        MyScheduledTask task = plugin.getScheduler().runTaskTimer(() -> {
            Player p = plugin.getServer().getPlayer(id);
            if (p == null || !p.isOnline()) {
                stopActionbar(id);
                return;
            }

            Mine m = mineManager.getMine(mine.getName());
            if (m == null || m.getEntranceAreaPos1() == null || m.getEntranceAreaPos2() == null) {
                stopActionbar(id);
                return;
            }

            Location loc = p.getLocation();
            if (!isInside(loc, m.getEntranceAreaPos1(), m.getEntranceAreaPos2())) {
                stopActionbar(id);
                return;
            }

            int secs = mineManager.getSecondsUntilReset(m);
            String countdown = secs < 0 ? "?" : formatHMS(secs);

            double rem = mineManager.getRemainingPercent(m, 5000);
            String remaining = rem < 0 ? "?" : String.format("%.1f%%", rem);

            String line = MessageKeys.ACTIONBAR_MINE_STATUS
                    .with("mine", m.getName(),
                            "reset_countdown", countdown,
                            "remaining_percent", remaining);

            p.sendActionBar(line);
        }, 0L, 20L);

        actionbarTasks.put(id, task);
    }

    private void stopActionbar(@NotNull UUID playerId) {
        MyScheduledTask t = actionbarTasks.remove(playerId);
        if (t != null) t.cancel();
        viewingMineByPlayer.remove(playerId);
    }

    private Mine findNearestEntranceMineAt(@NotNull Location at) {
        Mine nearest = null;
        double bestDist2 = Double.MAX_VALUE;

        for (Mine mine : mineManager.getAllMines()) {
            Location a = mine.getEntranceAreaPos1();
            Location b = mine.getEntranceAreaPos2();
            if (a == null || b == null) continue;
            if (!sameWorld(at, a) || !sameWorld(at, b)) continue;

            if (isInside(at, a, b)) {
                Location center = boxCenter(a, b);
                double d2 = center.toVector().distanceSquared(at.toVector());
                if (d2 < bestDist2) {
                    bestDist2 = d2;
                    nearest = mine;
                }
            }
        }
        return nearest;
    }

    private static boolean sameWorld(@NotNull Location x, @NotNull Location y) {
        return x.getWorld() != null && x.getWorld().equals(y.getWorld());
    }

    @NotNull
    private static Location boxCenter(@NotNull Location a, @NotNull Location b) {
        int minX = Math.min(a.getBlockX(), b.getBlockX());
        int maxX = Math.max(a.getBlockX(), b.getBlockX()) + 1;
        int minY = Math.min(a.getBlockY(), b.getBlockY());
        int maxY = Math.max(a.getBlockY(), b.getBlockY()) + 1;
        int minZ = Math.min(a.getBlockZ(), b.getBlockZ());
        int maxZ = Math.max(a.getBlockZ(), b.getBlockZ()) + 1;

        double cx = (minX + maxX) / 2.0;
        double cy = (minY + maxY) / 2.0;
        double cz = (minZ + maxZ) / 2.0;

        return new Location(a.getWorld(), cx, cy, cz);
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

    @NotNull
    private static String formatHMS(int totalSeconds) {
        int s = Math.max(0, totalSeconds);
        int h = s / 3600; s %= 3600;
        int m = s / 60;   s %= 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }
}
