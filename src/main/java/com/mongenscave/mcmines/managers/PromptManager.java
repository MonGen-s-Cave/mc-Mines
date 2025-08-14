package com.mongenscave.mcmines.managers;

import com.mongenscave.mcmines.McMines;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class PromptManager implements Listener {
    private static final Map<UUID, BiConsumer<Player, String>> PROMPTS = new ConcurrentHashMap<>();

    public static void init() {
        Bukkit.getPluginManager().registerEvents(new PromptManager(), McMines.getInstance());
    }

    public static void request(@NotNull Player p, @NotNull String askMessage, @NotNull BiConsumer<Player, String> onReply) {
        p.sendMessage(askMessage);
        PROMPTS.put(p.getUniqueId(), onReply);
    }

    public static boolean has(@NotNull Player p) { return PROMPTS.containsKey(p.getUniqueId()); }
    public static void clear(@NotNull Player p) { PROMPTS.remove(p.getUniqueId()); }

    @EventHandler
    @SuppressWarnings("all")
    public void onChat(@NotNull AsyncPlayerChatEvent e) {
        var handler = PROMPTS.remove(e.getPlayer().getUniqueId());
        if (handler != null) {
            e.setCancelled(true);
            Bukkit.getScheduler().runTask(McMines.getInstance(),
                    () -> handler.accept(e.getPlayer(), e.getMessage()));
        }
    }
}