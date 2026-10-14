package com.chen1335.arcanumlib.modules.fishing;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class FishMediaType {
    private static final Map<String, FishMediaType> MAP = new HashMap<>();
    public static final FishMediaType WATER = getOrCreate("water", context -> context.fluidState.is(FluidTags.WATER));

    public static final FishMediaType LAVA = getOrCreate("lava", context -> context.fluidState.is(FluidTags.LAVA));

    public static final FishMediaType VOID = getOrCreate("void", context -> context.blockPos.getY() <= context.level.getMinBuildHeight());

    private final Predicate<Context> fluidStatePredicate;

    public FishMediaType(Predicate<Context> fluidStatePredicate) {
        this.fluidStatePredicate = fluidStatePredicate;
    }

    public static FishMediaType getOrCreate(String id, Predicate<Context> predicate) {
        return MAP.computeIfAbsent(id, s -> new FishMediaType(predicate));
    }

    public boolean isAvailableFluid(Level level, LivingEntity owner, IFishingHook hook, BlockPos blockPos, FluidState fluidState) {
        return fluidStatePredicate.test(new Context(level, owner,hook, blockPos, fluidState));
    }


    public record Context(Level level, LivingEntity owner, IFishingHook hook, BlockPos blockPos, FluidState fluidState){

    }
}
