package com.chen1335.arcanumlib.modules.fishing;

import com.chen1335.arcanumlib.modules.fishing.fishMediaTypes.*;

import java.util.HashMap;
import java.util.Map;

public class FishMediaTypes {
    private static final Map<String, IFishMediaType> MAP = new HashMap<>();
    public static final IFishMediaType WATER = getOrCreate("water", new WaterMedia());

    public static final IFishMediaType LAVA = getOrCreate("lava", new LavaMedia());

    public static final IFishMediaType VOID = getOrCreate("void", new VoidMedia());
    public static final IFishMediaType DIRT = getOrCreate("dirt", new Dirt());

    public static IFishMediaType getOrCreate(String id, IFishMediaType type) {
        return MAP.computeIfAbsent(id, s -> type);
    }
}
