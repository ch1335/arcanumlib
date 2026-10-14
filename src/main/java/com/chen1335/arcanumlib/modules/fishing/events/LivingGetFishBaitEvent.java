package com.chen1335.arcanumlib.modules.fishing.events;

import com.chen1335.arcanumlib.modules.fishing.IFishingHook;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

public class LivingGetFishBaitEvent extends Event {
    private final LivingEntity livingEntity;
    private final ItemStack rod;
    private final IFishingHook hook;
    private ItemStack bait;

    public LivingGetFishBaitEvent(LivingEntity livingEntity, ItemStack rod, IFishingHook hook, ItemStack bait) {
        this.livingEntity = livingEntity;
        this.rod = rod;
        this.hook = hook;
        this.bait = bait;
    }

    public LivingEntity getLivingEntity() {
        return livingEntity;
    }

    public ItemStack getRod() {
        return rod;
    }


    public IFishingHook getHook() {
        return hook;
    }

    public ItemStack getBait() {
        return bait;
    }

    public void setBait(ItemStack bait) {
        this.bait = bait;
    }

    public static LivingGetFishBaitEvent post(LivingEntity livingEntity, ItemStack rod, IFishingHook hook, ItemStack bait) {
        return NeoForge.EVENT_BUS.post(new LivingGetFishBaitEvent(livingEntity, rod, hook, bait));
    }
}
