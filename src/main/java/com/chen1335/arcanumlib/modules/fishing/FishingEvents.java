package com.chen1335.arcanumlib.modules.fishing;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.fishing.events.LivingGetFishBaitEvent;
import com.chen1335.arcanumlib.modules.fishing.events.ModifyFishingHookEvent;
import com.chen1335.arcanumlib.registry.ArcanumAttachments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;


@EventBusSubscriber(modid = ArcanumLib.MODID)
public class FishingEvents {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void EntityJoinLevelEvent(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof IFishingHook fishingHook && fishingHook.getOwner() instanceof LivingEntity owner) {
            FishingHookData data = fishingHook.getData(ArcanumAttachments.FISHING_HOOK_DATA);
            if (!data.isModified()) {
                data.setModified(true);
                ItemStack rod = data.getRod();
                if (rod.getItem() instanceof IFishingRod rodItem) {
                    ItemStack bait = LivingGetFishBaitEvent.post(owner, rod, fishingHook, ItemStack.EMPTY).getBait();
                    data.setBait(bait);
                    rodItem.consumeBait(owner, fishingHook, rod, bait);
                    rodItem.modifyHook(owner, fishingHook, rod, bait);
                    ModifyFishingHookEvent.post(owner, rod, fishingHook, bait);
                }
            }
        }
    }
}
