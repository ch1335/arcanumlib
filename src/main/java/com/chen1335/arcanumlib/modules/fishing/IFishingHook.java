package com.chen1335.arcanumlib.modules.fishing;

import com.chen1335.arcanumlib.modules.fishing.fishMediaTypes.IFishMediaType;
import com.chen1335.arcanumlib.registry.ArcanumAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public interface IFishingHook extends TraceableEntity, IAttachmentHolder {

    default boolean canFishAt(Level level, LivingEntity owner, BlockPos blockPos, FluidState fluidState) {
        return getFishMediaType().isAvailableFluid(level, owner, this, blockPos, fluidState);
    }

    default IFishMediaType getFishMediaType() {
        return FishMediaTypes.WATER;
    }

    /**
     * 摇取用的战利品表，取自当前水体类型（默认即原版钓鱼表）。
     */
    default LootTable getLootTable(ServerLevel level, BlockPos blockPos, FluidState fluidState) {
        return getFishMediaType().getLootTable(level, blockPos, fluidState);
    }

    void setFishSpeed(float fishSpeed);

    float getFishSpeed();

    void setFishLuck(float fishLuck);

    float getFishLuck();

    boolean isBiting();

    boolean isOpenWaterFishing();

    default float getLureRate(IFishingHook fishingHook, Level level, BlockPos blockPos, boolean isRaining, boolean canSeeSky) {
        float lureRate = getFishMediaType().getLureRate(this, level, blockPos, isRaining, canSeeSky);
        FishingHookData data = fishingHook.getData(ArcanumAttachments.FISHING_HOOK_DATA);
        ItemStack bait = data.getBait();
        if (bait.getItem() instanceof IFishBait fishBait) {
            lureRate = fishBait.getLureRate(fishingHook, level, bait, blockPos, isRaining, canSeeSky, lureRate);
        }
        ItemStack rod = data.getRod();
        if (rod.getItem() instanceof IFishingRod fishingRod) {
            lureRate = fishingRod.getLureRate(fishingHook, level, rod, blockPos, isRaining, canSeeSky, lureRate);
        }
        return lureRate;
    }

    default float getHookRate(IFishingHook fishingHook, Level level, BlockPos blockPos, boolean isRaining, boolean canSeeSky) {
        float hookRate = getFishMediaType().getHookRate(this, level, blockPos, isRaining, canSeeSky);
        FishingHookData data = fishingHook.getData(ArcanumAttachments.FISHING_HOOK_DATA);
        ItemStack bait = data.getBait();
        if (bait.getItem() instanceof IFishBait fishBait) {
            hookRate = fishBait.getHookRate(fishingHook, level, bait, blockPos, isRaining, canSeeSky, hookRate);
        }
        ItemStack rod = data.getRod();
        if (rod.getItem() instanceof IFishingRod fishingRod) {
            hookRate = fishingRod.getHookRate(fishingHook, level, rod, blockPos, isRaining, canSeeSky, hookRate);
        }
        return hookRate;
    }

    default LootParams buildLootParams(LivingEntity owner, ItemStack rod, ItemStack bait) {
        LootParams.Builder builder = FishingHooks.buildLootParams(owner, this, rod, bait);
        this.modifyLootParams(builder, owner, rod, bait);
        return builder.create(LootContextParamSets.FISHING);
    }

    default void modifyLootParams(LootParams.Builder builder, LivingEntity owner, ItemStack rod, ItemStack bait) {
    }

    /**
     * 气泡粒子，取自当前水体类型（默认与原版一致）。
     */
    default ParticleOptions getBubbleParticle() {
        return getFishMediaType().getBubbleParticle();
    }

    /**
     * 钓鱼粒子，取自当前水体类型（默认与原版一致）。
     */
    default ParticleOptions getFishingParticle() {
        return getFishMediaType().getFishingParticle();
    }

    /**
     * 水花粒子，取自当前水体类型（默认与原版一致）。
     */
    default ParticleOptions getSplashParticle() {
        return getFishMediaType().getSplashParticle();
    }

    /**
     * 咬钩水花声，取自当前水体类型（默认与原版一致）。
     */
    default SoundEvent getSplashSound() {
        return getFishMediaType().getSplashSound();
    }
}
