package com.mongenscave.mcmines.reset;

import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import com.mongenscave.mcmines.McMines;
import com.mongenscave.mcmines.reset.model.ResetDirection;
import com.mongenscave.mcmines.models.Mine;
import com.mongenscave.mcmines.reset.model.ResetSettings;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public abstract class ResetManager {

    protected final McMines plugin;
    protected final Consumer<Mine> onFinish;
    protected final Map<String, MyScheduledTask> running = new ConcurrentHashMap<>();

    protected ResetManager(@NotNull McMines plugin, @NotNull Consumer<Mine> onFinish) {
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
        doResetVisual(mine, readSettings(plugin.getConfig()));
    }

    protected abstract void doResetVisual(@NotNull Mine mine, @NotNull ResetSettings settings);

    protected ResetSettings readSettings(@NotNull FileConfiguration config) {
        ResetDirection dir = parseDirection(config.getString("visual-reset.direction", "WEST_EAST"));
        int blocksPerTick = Math.max(1, config.getInt("visual-reset.blocks-per-tick", 120));
        int tickPeriod = Math.max(1, config.getInt("visual-reset.tick-period", 2));

        ResetSettings.ParticleSettings particle = new ResetSettings.ParticleSettings(
                ResetSettings.ParticleSettings.parse(config.getString("visual-reset.particle.name", "ENCHANTMENT_TABLE")),
                Math.max(0, config.getInt("visual-reset.particle.count", 2)),
                config.getDouble("visual-reset.particle.offset-x", 0.0),
                config.getDouble("visual-reset.particle.offset-y", 0.0),
                config.getDouble("visual-reset.particle.offset-z", 0.0),
                config.getDouble("visual-reset.particle.speed", 0.0)
        );

        ResetSettings.SoundSettings sound = new ResetSettings.SoundSettings(
                ResetSettings.SoundSettings.parse(config.getString("visual-reset.sound.name", "BLOCK_AMETHYST_BLOCK_CHIME")),
                (float) config.getDouble("visual-reset.sound.volume", 0.6D),
                (float) config.getDouble("visual-reset.sound.pitch-start", 0.95D),
                (float) config.getDouble("visual-reset.sound.pitch-end", 1.35D),
                Math.max(1, config.getInt("visual-reset.sound.every-placement", 40))
        );

        return new ResetSettings(dir, blocksPerTick, tickPeriod, particle, sound);
    }

    private ResetDirection parseDirection(String raw) {
        try { return ResetDirection.valueOf(String.valueOf(raw).toUpperCase()); }
        catch (Exception ignored) { return ResetDirection.WEST_EAST; }
    }
}