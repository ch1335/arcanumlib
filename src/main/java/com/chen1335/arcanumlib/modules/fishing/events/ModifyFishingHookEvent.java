package com.chen1335.arcanumlib.modules.fishing.events;

import com.chen1335.arcanumlib.modules.fishing.IFishingHook;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

public class ModifyFishingHookEvent extends Event {
    private final LivingEntity livingEntity;
    private final ItemStack rod;
    private final IFishingHook hook;
    private final ItemStack bait;

    public ModifyFishingHookEvent(LivingEntity livingEntity, ItemStack rod, IFishingHook hook, ItemStack bait) {
        this.livingEntity = livingEntity;
        this.rod = rod;
        this.hook = hook;
        this.bait = bait;
    }

    public ItemStack getBait() {
        return bait;
    }

    public IFishingHook getHook() {
        return hook;
    }

    public ItemStack getRod() {
        return rod;
    }

    public LivingEntity getLivingEntity() {
        return livingEntity;
    }

    public static void post(LivingEntity livingEntity, ItemStack rod, IFishingHook hook, ItemStack bait) {
        NeoForge.EVENT_BUS.post(new ModifyFishingHookEvent(livingEntity, rod, hook, bait));
    }
}
