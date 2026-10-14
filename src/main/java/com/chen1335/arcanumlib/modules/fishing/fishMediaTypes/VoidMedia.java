package com.chen1335.arcanumlib.modules.fishing.fishMediaTypes;

import com.chen1335.arcanumlib.modules.fishing.IFishingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

public class VoidMedia implements IFishMediaType {
    @Override
    public boolean isAvailableFluid(Level level, LivingEntity owner, IFishingHook hook, BlockPos blockPos, FluidState fluidState) {
        return blockPos.getY() <= level.getMinBuildHeight();
    }

    @Override
    public boolean isOpenWaterFluid(BlockPos pos, FluidState fluidState) {
        return true;
    }

    @Override
    public boolean isFloatingInFluid(FluidState fluidState, float fluidHeight) {
        return true;
    }
}
