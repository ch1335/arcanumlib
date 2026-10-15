package com.chen1335.arcanumlib.modules.fishing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

import java.util.Map;

public interface IFishingRod {
    default void modifyHook(LivingEntity owner, IFishingHook hook, ItemStack rod) {

    }

    default void modifyLootParams(LivingEntity owner, IFishingHook hook, ItemStack rod, Map<LootContextParam<?>, Object> params) {

    }

    default void consumeBait(LivingEntity owner, IFishingHook hook, ItemStack rod, ItemStack bait) {
        if (bait.getItem() instanceof IFishBait fishBait) {
            fishBait.consumeBait(owner, hook, rod, bait);
        } else {
            bait.shrink(1);
        }
    }

    default float getLureRate(IFishingHook fishingHook, Level level, ItemStack rod, BlockPos blockPos, boolean isRaining, boolean canSeeSky, float lureRate) {
        return lureRate;
    }

    default float getHookRate(IFishingHook fishingHook, Level level, ItemStack rod, BlockPos blockPos, boolean isRaining, boolean canSeeSky, float hookRate) {
        return hookRate;
    }
}
