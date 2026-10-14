package com.chen1335.arcanumlib.modules.fishing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public interface IFishingHook extends TraceableEntity, IAttachmentHolder {

    default boolean canFishAt(Level level,LivingEntity owner, BlockPos blockPos, FluidState fluidState) {
        return FishMediaType.WATER.isAvailableFluid(level, owner,this, blockPos, fluidState);
    }

    default LootTable getLootTable(ServerLevel level, BlockPos blockPos, FluidState fluidState) {
        return level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
    }

    void setFishSpeed(int fishSpeed);

    int getFishSpeed();

    void setFishLuck(int fishLuck);

    int getFishLuck();

    default LootParams buildLootParams(LivingEntity owner, ItemStack rod, ItemStack bait) {
        return FishingHooks.buildLootParams(owner, this, rod, bait).create(LootContextParamSets.FISHING);
    }

    default ParticleOptions getBubbleParticle() {
        return ParticleTypes.BUBBLE;
    }

    default ParticleOptions getFishingParticle() {
        return ParticleTypes.FISHING;
    }

    default ParticleOptions getSplashParticle() {
        return ParticleTypes.SPLASH;
    }
}
