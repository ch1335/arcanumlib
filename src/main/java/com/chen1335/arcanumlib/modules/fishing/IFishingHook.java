package com.chen1335.arcanumlib.modules.fishing;

import com.chen1335.arcanumlib.modules.fishing.fishMediaTypes.IFishMediaType;
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

    default boolean canFishAt(Level level, LivingEntity owner, BlockPos blockPos, FluidState fluidState) {
        return getFishMediaType().isAvailableFluid(level, owner, this, blockPos, fluidState);
    }

    default IFishMediaType getFishMediaType() {
        return FishMediaType.WATER;
    }

    default LootTable getLootTable(ServerLevel level, BlockPos blockPos, FluidState fluidState) {
        return level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
    }

    void setFishSpeed(float fishSpeed);

    float getFishSpeed();

    void setFishLuck(float fishLuck);

    float getFishLuck();

    default LootParams buildLootParams(LivingEntity owner, ItemStack rod, ItemStack bait) {
        LootParams.Builder builder = FishingHooks.buildLootParams(owner, this, rod, bait);
        this.modifyLootParams(builder, owner, rod, bait);
        return builder.create(LootContextParamSets.FISHING);
    }

    default void modifyLootParams(LootParams.Builder builder, LivingEntity owner, ItemStack rod, ItemStack bait) {
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
