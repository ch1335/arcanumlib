package com.chen1335.arcanumlib.modules.fishing.fishMediaTypes;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.fishing.IFishingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class LavaMedia implements IFishMediaType {
    public static final ResourceKey<LootTable> LAVA_FISHING = ResourceKey.create(Registries.LOOT_TABLE, ArcanumLib.id("gameplay/fishing/lava"));
    @Override
    public boolean isAvailableFluid(Level level, LivingEntity owner, IFishingHook hook, BlockPos blockPos, FluidState fluidState) {
        return fluidState.is(FluidTags.LAVA);
    }

    @Override
    public boolean isOpenWaterFluid(Level level, BlockPos pos, FluidState fluidState) {
        return fluidState.is(FluidTags.LAVA) && fluidState.isSource();
    }

    @Override
    public LootTable getLootTable(ServerLevel level, BlockPos blockPos, FluidState fluidState) {
       return level.getServer().reloadableRegistries().getLootTable(LAVA_FISHING);
    }

    @Override
    public ParticleOptions getBubbleParticle() {
        return ParticleTypes.LAVA;
    }

    @Override
    public ParticleOptions getFishingParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    public ParticleOptions getSplashParticle() {
        return ParticleTypes.LAVA;
    }

    @Override
    public SoundEvent getSplashSound() {
        return SoundEvents.LAVA_POP;
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
