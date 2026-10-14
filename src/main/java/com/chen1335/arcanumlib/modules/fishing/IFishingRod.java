package com.chen1335.arcanumlib.modules.fishing;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

import java.util.Map;

public interface IFishingRod {
    default void modifyHook(LivingEntity owner, IFishingHook hook, ItemStack rod, ItemStack bait) {
        if (bait.getItem() instanceof IFishBait fishBait) {
            fishBait.modifyHook(owner, hook, rod, bait);
        }
    }

    default void modifyLootParams(LivingEntity owner, IFishingHook hook, ItemStack rod, ItemStack bait, Map<LootContextParam<?>, Object> params) {
        if (bait.getItem() instanceof IFishBait fishBait) {
            fishBait.modifyLootParams(owner, hook, rod, params);
        }
    }

    default void consumeBait(LivingEntity owner, IFishingHook hook, ItemStack rod, ItemStack bait) {
        if (bait.getItem() instanceof IFishBait fishBait) {
            fishBait.consumeBait(owner, hook, rod, bait);
        } else {
            bait.shrink(1);
        }
    }

}
