package com.chen1335.arcanumlib.modules.fishing.fishMediaTypes;

import com.chen1335.arcanumlib.modules.fishing.IFishingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public interface IFishMediaType {
    boolean isAvailableFluid(Level level, LivingEntity owner, IFishingHook hook, BlockPos blockPos, FluidState fluidState);

    boolean isOpenWaterFluid(Level level, BlockPos pos, FluidState fluidState);
    // ------------------------------------------------------------------
    // 表现：粒子与声音（默认一律沿用原版，换水体类型时按需覆盖）
    // ------------------------------------------------------------------

    /**
     * 引鱼时绕着浮标飞的气泡粒子。原版：{@code ParticleTypes.BUBBLE}。
     */
    default ParticleOptions getBubbleParticle() {
        return ParticleTypes.BUBBLE;
    }

    /**
     * 引鱼时绕着浮标飞出的钓鱼粒子。原版：{@code ParticleTypes.FISHING}。
     */
    default ParticleOptions getFishingParticle() {
        return ParticleTypes.FISHING;
    }

    /**
     * 引鱼倒计时末尾的水花粒子。原版：{@code ParticleTypes.SPLASH}。
     */
    default ParticleOptions getSplashParticle() {
        return ParticleTypes.SPLASH;
    }

    /**
     * 该水体类型摇取用的战利品表。默认即原版钓鱼表 {@code BuiltInLootTables.FISHING}；
     * 岩浆 / 虚空之类想要自己的表，覆盖这里即可（不需要动鱼钩本身）。
     */
    default LootTable getLootTable(ServerLevel level, BlockPos blockPos, FluidState fluidState) {
        return level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
    }

    /**
     * 第一次引鱼的等待区间下限（刻）。原版：100。
     */
    default int getMinLureTicks(IFishingHook fishingHook, Level level, BlockPos blockPos) {
        return 100;
    }

    /**
     * 第一次引鱼的等待区间上限（刻）。原版：600。
     */
    default int getMaxLureTicks(IFishingHook fishingHook, Level level, BlockPos blockPos) {
        return 600;
    }

    /**
     * 咬钩时的水花声。原版：{@code SoundEvents.FISHING_BOBBER_SPLASH}
     * （音量与音高由调用方决定，这里只提供音效本身）。
     */
    default SoundEvent getSplashSound() {
        return SoundEvents.FISHING_BOBBER_SPLASH;
    }


    default boolean isFloatingInFluid(Level level, BlockPos blockPos, FluidState fluidState, float fluidHeight) {
        return fluidHeight > 0.0F;
    }

    default boolean isFishingFluidBlock(IFishingHook fishingHook, BlockPos pos, BlockState state) {
        Entity owner = fishingHook.getOwner();
        return owner instanceof LivingEntity living && fishingHook.canFishAt(living.level(), living, pos, state.getFluidState());
    }

    /**
     * 开阔水面判定：以 {@code pos} 为中心取 5×4×5 的柱体（y -1..2），逐层要求"整层同为水上或水下"，
     * 且不能出现无效层。逐块分类见 {@link #getOpenWaterTypeForBlock}，与原版 {@code FishingHook#calculateOpenWater} 一致。
     *
     * <p>放在这里是为了让水体类型自己决定"什么算开阔"（水/岩浆/虚空各有各的规矩）；
     * {@code SimpleFishingHook} 只保留同名方法转发过来。
     */
    default boolean calculateOpenWater(Level level, IFishingHook hook, BlockPos pos) {
        OpenWaterType waterType = OpenWaterType.INVALID;
        for (int y = -1; y <= 2; ++y) {
            OpenWaterType layerType = this.getOpenWaterTypeForArea(level, hook, pos.offset(-2, y, -2), pos.offset(2, y, 2));
            switch (layerType) {
                case ABOVE_WATER:
                    if (waterType != OpenWaterType.INVALID) {
                        break;
                    }
                    return false;
                case INSIDE_WATER:
                    if (waterType != OpenWaterType.ABOVE_WATER) {
                        break;
                    }
                    return false;
                case INVALID:
                    return false;
            }
            waterType = layerType;
        }
        return true;
    }

    /**
     * 一片区域内所有方块的水面类型必须一致，否则视为无效。
     */
    default OpenWaterType getOpenWaterTypeForArea(Level level, IFishingHook hook, BlockPos firstPos, BlockPos secondPos) {
        return BlockPos.betweenClosedStream(firstPos, secondPos)
                .map(pos -> this.getOpenWaterTypeForBlock(level, hook, pos))
                .reduce((first, second) -> first == second ? first : OpenWaterType.INVALID)
                .orElse(OpenWaterType.INVALID);
    }

    /**
     * 单个方块的水面类型：空气 / 睡莲算"水上"，开阔的静止流体算"水下"，其余无效。
     */
    default OpenWaterType getOpenWaterTypeForBlock(Level level, IFishingHook hook, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && !state.is(Blocks.LILY_PAD)) {
            FluidState fluidState = state.getFluidState();
            return this.isOpenWaterFluid(level, pos, fluidState) && state.getCollisionShape(level, pos).isEmpty()
                    ? OpenWaterType.INSIDE_WATER : OpenWaterType.INVALID;
        }
        return OpenWaterType.ABOVE_WATER;
    }

    default float getFluidHeight(Level level, BlockPos blockPos, FluidState fluidState) {
        return fluidState.getHeight(level, blockPos);
    }


    default float getLureRate(IFishingHook fishingHook, Level level, BlockPos blockPos, boolean isRaining, boolean canSeeSky) {
        float rate = geBaseLureRate();
        if (isRaining) {
            rate += 0.25F;
        }
        if (!canSeeSky) {
            rate -= 0.5F;
        }
        return rate;
    }

    default float getHookRate(IFishingHook fishingHook, Level level, BlockPos blockPos, boolean isRaining, boolean canSeeSky) {
        float rate = getBaseHookRate();
        if (isRaining) {
            rate += 0.25F;
        }
        if (!canSeeSky) {
            rate -= 0.5F;
        }
        return rate;
    }

    default float geBaseLureRate() {
        return 1;
    }

    default float getBaseHookRate() {
        return 1;
    }

    /**
     * 开阔水面判定用的单方块分类。各水体类型共用，所以放在接口里。
     */
    enum OpenWaterType {
        /**
         * 空气或睡莲：在水面之上。
         */
        ABOVE_WATER,
        /**
         * 静止且无碰撞的开阔流体：在水面之下。
         */
        INSIDE_WATER,
        /**
         * 其余一切（含水下障碍）：整片区域判为不开阔。
         */
        INVALID
    }
}
