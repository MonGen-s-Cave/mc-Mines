package com.mongenscave.mcmines.models;

import com.mongenscave.mcmines.data.BlockData;
import com.mongenscave.mcmines.data.ResetData;
import com.mongenscave.mcmines.identifiers.ResetDirection;
import com.mongenscave.mcmines.identifiers.ResetType;
import com.mongenscave.mcmines.utils.LocationUtils;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Mine {
    @NotNull private String name;
    @NotNull private List<BlockData> blockDataList = Collections.synchronizedList(new ArrayList<>());
    private int resetAfter;
    @Nullable private Location mineAreaPos1;
    @Nullable private Location mineAreaPos2;
    @Nullable private Location entranceAreaPos1;
    @Nullable private Location entranceAreaPos2;
    @Nullable private String entrancePermission;

    private boolean visualResetEnabled = false;
    @NotNull private ResetType resetType = ResetType.SWEEP;
    @NotNull private ResetDirection resetDirection = ResetDirection.WEST_EAST;
    private int blocksPerTick = 120;
    private int tickPeriod = 2;

    @NotNull private Particle particleType = Particle.ENCHANT;
    private int particleCount = 2;
    private double particleOffsetX = 0.0;
    private double particleOffsetY = 0.0;
    private double particleOffsetZ = 0.0;
    private double particleSpeed = 0.0;

    @NotNull private Sound soundType = Sound.BLOCK_AMETHYST_BLOCK_CHIME;
    private float soundVolume = 0.6f;
    private float soundPitchStart = 0.95f;
    private float soundPitchEnd = 1.35f;
    private int soundEveryPlacement = 40;

    public Mine(@NotNull String name, int resetAfter) {
        this.name = name;
        this.resetAfter = resetAfter;
        this.blockDataList = new ArrayList<>();
    }

    public void saveToConfig(@NotNull Section section) {
        List<Map<String, Object>> blockDataMaps = Collections.synchronizedList(new ArrayList<>());
        for (BlockData blockData : blockDataList) {
            Map<String, Object> blockMap = Map.of(
                    "material", blockData.material(),
                    "chance", blockData.chance()
            );
            blockDataMaps.add(blockMap);
        }
        section.set("block-data", blockDataMaps);
        section.set("reset-after", resetAfter);

        if (mineAreaPos1 != null || mineAreaPos2 != null) {
            Section mineAreaSection = section.createSection("mine-area");

            if (mineAreaPos1 != null) mineAreaSection.set("pos1", LocationUtils.serialize(mineAreaPos1));
            if (mineAreaPos2 != null) mineAreaSection.set("pos2", LocationUtils.serialize(mineAreaPos2));
        }

        if (entranceAreaPos1 != null || entranceAreaPos2 != null) {
            Section entranceAreaSection = section.createSection("entrance-area");

            if (entranceAreaPos1 != null) entranceAreaSection.set("pos1", LocationUtils.serialize(entranceAreaPos1));
            if (entranceAreaPos2 != null) entranceAreaSection.set("pos2", LocationUtils.serialize(entranceAreaPos2));
        }

        if (entrancePermission != null && !entrancePermission.isEmpty()) section.set("entrance-permission", entrancePermission);

        Section visualResetSection = section.createSection("visual-reset");
        visualResetSection.set("enabled", visualResetEnabled);
        visualResetSection.set("type", resetType.name());
        visualResetSection.set("direction", resetDirection.name());
        visualResetSection.set("blocks-per-tick", blocksPerTick);
        visualResetSection.set("tick-period", tickPeriod);

        Section particleSection = visualResetSection.createSection("particle");
        particleSection.set("type", particleType.name());
        particleSection.set("count", particleCount);
        particleSection.set("offset-x", particleOffsetX);
        particleSection.set("offset-y", particleOffsetY);
        particleSection.set("offset-z", particleOffsetZ);
        particleSection.set("speed", particleSpeed);

        Section soundSection = visualResetSection.createSection("sound");
        soundSection.set("type", soundType.name());
        soundSection.set("volume", soundVolume);
        soundSection.set("pitch-start", soundPitchStart);
        soundSection.set("pitch-end", soundPitchEnd);
        soundSection.set("every-placement", soundEveryPlacement);
    }

    @NotNull
    public static Mine loadFromConfig(@NotNull String name, @NotNull Section section) {
        Mine mine = new Mine(name, 0);

        if (section.contains("block-data")) {
            List<?> blockDataList = section.getList("block-data");

            if (blockDataList != null) {
                for (Object obj : blockDataList) {
                    if (obj instanceof Map<?, ?> blockMap) {
                        Object materialObj = blockMap.get("material");
                        Object chanceObj = blockMap.get("chance");

                        if (materialObj instanceof String material && chanceObj instanceof Integer chance && chance > 0) mine.blockDataList.add(new BlockData(material, chance));
                    }
                }
            }
        }

        mine.resetAfter = section.getInt("reset-after", 300);

        if (section.contains("mine-area")) {
            Section mineAreaSection = section.getSection("mine-area");
            if (mineAreaSection != null) {
                String pos1Str = mineAreaSection.getString("pos1");
                String pos2Str = mineAreaSection.getString("pos2");

                if (pos1Str != null) mine.mineAreaPos1 = LocationUtils.deserialize(pos1Str);
                if (pos2Str != null) mine.mineAreaPos2 = LocationUtils.deserialize(pos2Str);
            }
        }

        if (section.contains("entrance-area")) {
            Section entranceAreaSection = section.getSection("entrance-area");
            if (entranceAreaSection != null) {
                String pos1Str = entranceAreaSection.getString("pos1");
                String pos2Str = entranceAreaSection.getString("pos2");

                if (pos1Str != null) mine.entranceAreaPos1 = LocationUtils.deserialize(pos1Str);
                if (pos2Str != null) mine.entranceAreaPos2 = LocationUtils.deserialize(pos2Str);
            }
        }

        mine.entrancePermission = section.getString("entrance-permission");

        if (section.contains("visual-reset")) {
            Section visualResetSection = section.getSection("visual-reset");
            if (visualResetSection != null) {
                mine.visualResetEnabled = visualResetSection.getBoolean("enabled", false);
                mine.resetType = parseResetType(visualResetSection.getString("type", "SWEEP"));
                mine.resetDirection = parseResetDirection(visualResetSection.getString("direction", "WEST_EAST"));
                mine.blocksPerTick = visualResetSection.getInt("blocks-per-tick", 120);
                mine.tickPeriod = visualResetSection.getInt("tick-period", 2);

                if (visualResetSection.contains("particle")) {
                    Section particleSection = visualResetSection.getSection("particle");
                    if (particleSection != null) {
                        mine.particleType = parseParticle(particleSection.getString("type", "ENCHANT"));
                        mine.particleCount = particleSection.getInt("count", 2);
                        mine.particleOffsetX = particleSection.getDouble("offset-x", 0.0);
                        mine.particleOffsetY = particleSection.getDouble("offset-y", 0.0);
                        mine.particleOffsetZ = particleSection.getDouble("offset-z", 0.0);
                        mine.particleSpeed = particleSection.getDouble("speed", 0.0);
                    }
                }

                if (visualResetSection.contains("sound")) {
                    Section soundSection = visualResetSection.getSection("sound");

                    if (soundSection != null) {
                        mine.soundType = parseSound(soundSection.getString("type", "BLOCK_AMETHYST_BLOCK_CHIME"));
                        mine.soundVolume = soundSection.getFloat("volume", 0.6f);
                        mine.soundPitchStart = soundSection.getFloat("pitch-start", 0.95f);
                        mine.soundPitchEnd = soundSection.getFloat("pitch-end", 1.35f);
                        mine.soundEveryPlacement = soundSection.getInt("every-placement", 40);
                    }
                }
            }
        }

        return mine;
    }

    private static ResetType parseResetType(String raw) {
        if (raw == null || raw.isEmpty()) return ResetType.SWEEP;

        try {
            return ResetType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return ResetType.SWEEP;
        }
    }

    private static ResetDirection parseResetDirection(String raw) {
        if (raw == null || raw.isEmpty()) return ResetDirection.WEST_EAST;

        try {
            return ResetDirection.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return ResetDirection.WEST_EAST;
        }
    }

    private static Particle parseParticle(String raw) {
        if (raw == null || raw.isEmpty()) return Particle.ENCHANT;

        try {
            return Particle.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return Particle.ENCHANT;
        }
    }

    private static Sound parseSound(String raw) {
        if (raw == null || raw.isEmpty()) return Sound.BLOCK_AMETHYST_BLOCK_CHIME;

        try {
            return Sound.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return Sound.BLOCK_AMETHYST_BLOCK_CHIME;
        }
    }

    public boolean isValidMineArea() {
        return mineAreaPos1 != null && mineAreaPos2 != null;
    }

    public boolean isValidEntranceArea() {
        return entranceAreaPos1 != null && entranceAreaPos2 != null;
    }

    public void addBlockData(@NotNull String material, int chance) {
        blockDataList.add(new BlockData(material, chance));
    }

    public void removeBlockData(@NotNull String material) {
        blockDataList.removeIf(blockData -> blockData.material().equals(material));
    }

    @NotNull
    public ResetData getResetSettings() {
        ResetData.ParticleSettings particle = new ResetData.ParticleSettings(particleType, particleCount, particleOffsetX, particleOffsetY, particleOffsetZ, particleSpeed);
        ResetData.SoundSettings sound = new ResetData.SoundSettings(soundType, soundVolume, soundPitchStart, soundPitchEnd, soundEveryPlacement);

        return new ResetData(resetDirection, blocksPerTick, tickPeriod, particle, sound);
    }
}