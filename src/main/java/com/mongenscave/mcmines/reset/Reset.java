package com.mongenscave.mcmines.reset;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.identifiers.ResetDirection;
import com.mongenscave.mcmines.models.Mine;
import com.mongenscave.mcmines.data.ResetData;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class Reset {
    protected final McMines plugin;
    protected final Consumer<Mine> onFinish;
    protected final Map<String, MyScheduledTask> running = new ConcurrentHashMap<>();

    protected Reset(@NotNull McMines plugin, @NotNull Consumer<Mine> onFinish) {
        this.plugin = plugin;
        this.onFinish = onFinish;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("visual-reset.enabled", false);
    }

    public final void cancel(@NotNull String mineName) {
        MyScheduledTask t = running.remove(mineName);
        if (t != null) t.cancel();
    }

    public final void shutdown() {
        running.values().forEach(MyScheduledTask::cancel);
        running.clear();
    }

    public final void resetVisual(@NotNull Mine mine) {
        if (!isEnabled()) return;

        ResetData settings = mine.getResetSettings();
        doResetVisual(mine, settings);
    }

    protected abstract void doResetVisual(@NotNull Mine mine, @NotNull ResetData settings);
}