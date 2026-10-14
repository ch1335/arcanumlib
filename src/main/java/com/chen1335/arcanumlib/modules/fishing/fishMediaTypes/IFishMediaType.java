package com.chen1335.arcanumlib.modules.fishing.fishMediaTypes;

import com.chen1335.arcanumlib.modules.fishing.IFishingHook;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public interface IFishMediaType {
    boolean isAvailableFluid(Level level, LivingEntity owner, IFishingHook hook, BlockPos blockPos, FluidState fluidState);

    boolean isOpenWaterFluid(BlockPos pos, FluidState fluidState);

    default boolean isFloatingInFluid(FluidState fluidState, float fluidHeight) {
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
            return this.isOpenWaterFluid(pos, fluidState) && state.getCollisionShape(level, pos).isEmpty()
                    ? OpenWaterType.INSIDE_WATER : OpenWaterType.INVALID;
        }
        return OpenWaterType.ABOVE_WATER;
    }

    /**
     * 开阔水面判定用的单方块分类。各水体类型共用，所以放在接口里。
     */
    enum OpenWaterType {
        /** 空气或睡莲：在水面之上。 */
        ABOVE_WATER,
        /** 静止且无碰撞的开阔流体：在水面之下。 */
        INSIDE_WATER,
        /** 其余一切（含水下障碍）：整片区域判为不开阔。 */
        INVALID
    }
}
