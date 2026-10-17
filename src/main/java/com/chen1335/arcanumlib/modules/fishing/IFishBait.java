package com.chen1335.arcanumlib.modules.fishing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

import java.util.Map;

public interface IFishBait {
    default void modifyHook(LivingEntity owner, IFishingHook hook, ItemStack bait) {

    }

    default void modifyLootParams(LivingEntity owner, IFishingHook hook, ItemStack bait, Map<LootContextParam<?>, Object> params) {

    }

    default void consumeBait(LivingEntity owner, IFishingHook hook, ItemStack rod, ItemStack bite) {
        bite.shrink(1);
    }

    default float getLureRate(IFishingHook fishingHook, Level level, ItemStack bait, BlockPos blockPos, boolean isRaining, boolean canSeeSky, float lureRate) {
        return lureRate;
    }

    default float getHookRate(IFishingHook fishingHook, Level level, ItemStack bait, BlockPos blockPos, boolean isRaining, boolean canSeeSky, float hookRate) {
        return hookRate;
    }
}
