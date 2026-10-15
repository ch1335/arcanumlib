package com.chen1335.arcanumlib.modules.fishing.fishMediaTypes;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.fishing.IFishingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootTable;

public class DirtMedia implements IFishMediaType {
    public static final ResourceKey<LootTable> DIRT_FISHING = ResourceKey.create(Registries.LOOT_TABLE, ArcanumLib.id("gameplay/fishing/dirt"));

    @Override
    public boolean isAvailableFluid(Level level, LivingEntity owner, IFishingHook hook, BlockPos blockPos, FluidState fluidState) {
        BlockState blockState = level.getBlockState(blockPos.below(1));
        return blockState.is(Blocks.DIRT);
    }

    @Override
    public LootTable getLootTable(ServerLevel level, BlockPos blockPos, FluidState fluidState) {
        return level.getServer().reloadableRegistries().getLootTable(DIRT_FISHING);
    }

    @Override
    public float getFluidHeight(Level level, BlockPos blockPos, FluidState fluidState) {
        return 0.01F;
    }

    @Override
    public boolean isOpenWaterFluid(Level level, BlockPos pos, FluidState fluidState) {
        return true;
    }

    @Override
    public float getLureRate(IFishingHook fishingHook, Level level, BlockPos blockPos, boolean isRaining, boolean canSeeSky) {
        return geBaseLureRate();
    }

    @Override
    public float getHookRate(IFishingHook fishingHook, Level level, BlockPos blockPos, boolean isRaining, boolean canSeeSky) {
        return getBaseHookRate();
    }
}
