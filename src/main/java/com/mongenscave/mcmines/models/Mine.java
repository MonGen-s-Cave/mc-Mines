package com.mongenscave.mcmines.models;

import com.mongenscave.mcmines.data.BlockData;
import com.mongenscave.mcmines.utils.LocationUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
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

        return mine;
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
}
