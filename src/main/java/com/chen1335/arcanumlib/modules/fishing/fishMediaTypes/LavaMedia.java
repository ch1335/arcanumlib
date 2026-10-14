package com.chen1335.arcanumlib.modules.fishing.fishMediaTypes;

import com.chen1335.arcanumlib.modules.fishing.IFishingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

public class LavaMedia implements IFishMediaType{
    @Override
    public boolean isAvailableFluid(Level level, LivingEntity owner, IFishingHook hook, BlockPos blockPos, FluidState fluidState) {
        return fluidState.is(FluidTags.LAVA);
    }

    @Override
    public boolean isOpenWaterFluid(Level level, BlockPos pos, FluidState fluidState) {
        return fluidState.is(FluidTags.LAVA) && fluidState.isSource();
    }
}
