package com.chen1335.arcanumlib.modules.fishing;

import com.chen1335.arcanumlib.modules.fishing.fishMediaTypes.IFishMediaType;
import com.chen1335.arcanumlib.modules.fishing.fishMediaTypes.LavaMedia;
import com.chen1335.arcanumlib.modules.fishing.fishMediaTypes.VoidMedia;
import com.chen1335.arcanumlib.modules.fishing.fishMediaTypes.WaterMedia;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class FishMediaType {
    private static final Map<String, IFishMediaType> MAP = new HashMap<>();
    public static final IFishMediaType WATER = getOrCreate("water", new WaterMedia());

    public static final IFishMediaType LAVA = getOrCreate("lava", new LavaMedia());

    public static final IFishMediaType VOID = getOrCreate("void", new VoidMedia());


    public static IFishMediaType getOrCreate(String id, IFishMediaType type) {
        return MAP.computeIfAbsent(id, s -> type);
    }
}
