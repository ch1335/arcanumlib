package com.chen1335.arcanumlib.modules.fishing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public class FishingHooks {
    public static LootParams.Builder buildLootParams(LivingEntity owner, IFishingHook hook, ItemStack rod, ItemStack bait) {
        Entity entityHook = (Entity) hook;
        AttributeMap attributes = owner.getAttributes();
        return new LootParams.Builder((ServerLevel) entityHook.level())
                .withParameter(LootContextParams.ORIGIN, entityHook.position())
                .withParameter(LootContextParams.TOOL, rod)
                .withParameter(LootContextParams.THIS_ENTITY, entityHook)
                .withParameter(LootContextParams.ATTACKING_ENTITY, owner)
                .withLuck((float) (hook.getFishLuck() + (attributes.hasAttribute(Attributes.LUCK) ? attributes.getValue(Attributes.LUCK) : 0)));
    }

    public static float getLureRate(IFishingHook fishingHook, Level level, BlockPos blockPos, boolean isRaining, boolean canSeeSky) {
        return fishingHook.getLureRate(fishingHook, level, blockPos, isRaining, canSeeSky);
    }

    public static float getHookRate(IFishingHook fishingHook, Level level, BlockPos blockPos, boolean isRaining, boolean canSeeSky) {
        return fishingHook.getHookRate(fishingHook, level, blockPos, isRaining, canSeeSky);
    }



}
