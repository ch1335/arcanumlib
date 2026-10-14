package com.chen1335.arcanumlib.modules.fishing.fishMediaTypes;

import com.chen1335.arcanumlib.modules.fishing.IFishingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class Dirt implements IFishMediaType{
    @Override
    public boolean isAvailableFluid(Level level, LivingEntity owner, IFishingHook hook, BlockPos blockPos, FluidState fluidState) {
        BlockState blockState = level.getBlockState(blockPos.below(1));
        return blockState.is(Blocks.DIRT);
    }

    @Override
    public float getFluidHeight(Level level, BlockPos blockPos, FluidState fluidState) {
        return 0.01F;
    }

    @Override
    public boolean isOpenWaterFluid(Level level, BlockPos pos, FluidState fluidState) {
        return true;
    }
}
