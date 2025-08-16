package com.mongenscave.mcmines.data;

import com.mongenscave.mcmines.identifiers.ResetDirection;
import org.bukkit.Particle;
import org.bukkit.Sound;

public record ResetData(
        ResetDirection direction,
        int blocksPerTick,
        int tickPeriod,
        ParticleSettings particle,
        SoundSettings sound
) {
    public record ParticleSettings(
            Particle type,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double speed
    ) {
        public static Particle parse(String name) {
            try {
                return Particle.valueOf(String.valueOf(name).toUpperCase());
            }
            catch (Exception ignored) {
                return Particle.SMOKE;
            }
        }
    }

    public record SoundSettings(
            Sound type,
            float volume,
            float pitchStart,
            float pitchEnd,
            int everyPlacement
    ) {
        public static Sound parse(String name) {
            try {
                return Sound.valueOf(String.valueOf(name).toUpperCase());
            }
            catch (Exception ignored) {
                return Sound.BLOCK_COPPER_BULB_PLACE;
            }
        }
    }
}