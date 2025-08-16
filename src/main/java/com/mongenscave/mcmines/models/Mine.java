package com.mongenscave.mcmines.models;

import com.mongenscave.mcmines.data.BlockData;
import com.mongenscave.mcmines.data.ResetData;
import com.mongenscave.mcmines.identifiers.ResetDirection;
import com.mongenscave.mcmines.identifiers.ResetType;
import com.mongenscave.mcmines.utils.LocationUtils;
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

    public void saveToConfig(@NotNull Map<String, Object> section) {
        List<Map<String, Object>> blockDataMaps = Collections.synchronizedList(new ArrayList<>());
        for (BlockData blockData : blockDataList) {
            Map<String, Object> blockMap = Map.of(
                    "material", blockData.material(),
                    "chance", blockData.chance()
            );

            blockDataMaps.add(blockMap);
        }

        section.put("block-data", blockDataMaps);
        section.put("reset-after", resetAfter);

        if (mineAreaPos1 != null) section.put("mine-area.pos1", LocationUtils.serialize(mineAreaPos1));
        if (mineAreaPos2 != null) section.put("mine-area.pos2", LocationUtils.serialize(mineAreaPos2));
        if (entranceAreaPos1 != null) section.put("entrance-area.pos1", LocationUtils.serialize(entranceAreaPos1));
        if (entranceAreaPos2 != null) section.put("entrance-area.pos2", LocationUtils.serialize(entranceAreaPos2));
        if (entrancePermission != null && !entrancePermission.isEmpty()) section.put("entrance-permission", entrancePermission);

        section.put("visual-reset.enabled", visualResetEnabled);
        section.put("visual-reset.type", resetType.name());
        section.put("visual-reset.direction", resetDirection.name());
        section.put("visual-reset.blocks-per-tick", blocksPerTick);
        section.put("visual-reset.tick-period", tickPeriod);

        section.put("visual-reset.particle.type", particleType.name());
        section.put("visual-reset.particle.count", particleCount);
        section.put("visual-reset.particle.offset-x", particleOffsetX);
        section.put("visual-reset.particle.offset-y", particleOffsetY);
        section.put("visual-reset.particle.offset-z", particleOffsetZ);
        section.put("visual-reset.particle.speed", particleSpeed);

        section.put("visual-reset.sound.type", soundType.name());
        section.put("visual-reset.sound.volume", soundVolume);
        section.put("visual-reset.sound.pitch-start", soundPitchStart);
        section.put("visual-reset.sound.pitch-end", soundPitchEnd);
        section.put("visual-reset.sound.every-placement", soundEveryPlacement);
    }

    @NotNull
    public static Mine loadFromConfig(@NotNull String name, @NotNull Map<String, Object> section) {
        Mine mine = new Mine(name, 0);

        if (section.containsKey("block-data") && section.get("block-data") instanceof List<?> blockDataList) {
            for (Object obj : blockDataList) {
                if (obj instanceof Map<?, ?> blockMap) {
                    String material = (String) blockMap.get("material");
                    int chance = (Integer) blockMap.get("chance");
                    mine.blockDataList.add(new BlockData(material, chance));
                }
            }
        }

        mine.resetAfter = (Integer) section.getOrDefault("reset-after", 300);

        if (section.containsKey("mine-area.pos1")) mine.mineAreaPos1 = LocationUtils.deserialize((String) section.get("mine-area.pos1"));
        if (section.containsKey("mine-area.pos2")) mine.mineAreaPos2 = LocationUtils.deserialize((String) section.get("mine-area.pos2"));
        if (section.containsKey("entrance-area.pos1")) mine.entranceAreaPos1 = LocationUtils.deserialize((String) section.get("entrance-area.pos1"));
        if (section.containsKey("entrance-area.pos2")) mine.entranceAreaPos2 = LocationUtils.deserialize((String) section.get("entrance-area.pos2"));

        mine.entrancePermission = (String) section.get("entrance-permission");

        mine.visualResetEnabled = (Boolean) section.getOrDefault("visual-reset.enabled", false);
        mine.resetType = parseResetType((String) section.getOrDefault("visual-reset.type", "SWEEP"));
        mine.resetDirection = parseResetDirection((String) section.getOrDefault("visual-reset.direction", "WEST_EAST"));
        mine.blocksPerTick = (Integer) section.getOrDefault("visual-reset.blocks-per-tick", 120);
        mine.tickPeriod = (Integer) section.getOrDefault("visual-reset.tick-period", 2);

        mine.particleType = parseParticle((String) section.getOrDefault("visual-reset.particle.type", "ENCHANTMENT_TABLE"));
        mine.particleCount = (Integer) section.getOrDefault("visual-reset.particle.count", 2);
        mine.particleOffsetX = (Double) section.getOrDefault("visual-reset.particle.offset-x", 0.0);
        mine.particleOffsetY = (Double) section.getOrDefault("visual-reset.particle.offset-y", 0.0);
        mine.particleOffsetZ = (Double) section.getOrDefault("visual-reset.particle.offset-z", 0.0);
        mine.particleSpeed = (Double) section.getOrDefault("visual-reset.particle.speed", 0.0);

        mine.soundType = parseSound((String) section.getOrDefault("visual-reset.sound.type", "BLOCK_AMETHYST_BLOCK_CHIME"));
        mine.soundVolume = parseFloat(section.getOrDefault("visual-reset.sound.volume", 0.6), 0.6f);
        mine.soundPitchStart = parseFloat(section.getOrDefault("visual-reset.sound.pitch-start", 0.95), 0.95f);
        mine.soundPitchEnd = parseFloat(section.getOrDefault("visual-reset.sound.pitch-end", 1.35), 1.35f);
        mine.soundEveryPlacement = (Integer) section.getOrDefault("visual-reset.sound.every-placement", 40);

        return mine;
    }

    private static float parseFloat(Object obj, float defaultValue) {
        return switch (obj) {
            case Float v -> v;
            case Double v -> v.floatValue();
            case Integer i -> i.floatValue();
            case null, default -> defaultValue;
        };
    }

    private static ResetType parseResetType(String raw) {
        try {
            return ResetType.valueOf(String.valueOf(raw).toUpperCase());
        } catch (Exception ignored) {
            return ResetType.SWEEP;
        }
    }

    private static ResetDirection parseResetDirection(String raw) {
        try {
            return ResetDirection.valueOf(String.valueOf(raw).toUpperCase());
        } catch (Exception ignored) {
            return ResetDirection.WEST_EAST;
        }
    }

    private static Particle parseParticle(String raw) {
        try {
            return Particle.valueOf(String.valueOf(raw).toUpperCase());
        } catch (Exception ignored) {
            return Particle.ENCHANT;
        }
    }

    private static Sound parseSound(String raw) {
        try {
            return Sound.valueOf(String.valueOf(raw).toUpperCase());
        } catch (Exception ignored) {
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
