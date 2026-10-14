package com.chen1335.arcanumlib.modules.holder;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.registry.ArcanumDataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * EntityHolderItem 不该以掉落物形态存在于世界里：一旦有 ItemEntity 带着它进入世界，
 * 就拒绝这个实体加入，并在它的位置直接生成被包装的实体。
 */
@EventBusSubscriber(modid = ArcanumLib.MODID)
public final class EntityHolderEvents {
    private EntityHolderEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // 读档时世界里的旧掉落物也会走这个事件，必须排除，否则每次进存档都会多刷一批
        if (event.loadedFromDisk()) {
            return;
        }
        if (!(event.getEntity() instanceof ItemEntity itemEntity) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ItemStack stack = itemEntity.getItem();
        if (!EntityHolderItem.isHolder(stack)) {
            return;
        }

        EntityHolderPayload payload = EntityHolderItem.payloadOf(stack);
        event.setCanceled(true);

        if (payload != null) {
            // position() 取当前位置，getDeltaMovement() 把掉落物的动量一起带过去
            EntityHolderHandler.spawn(level, payload, itemEntity.position(), itemEntity.getDeltaMovement().multiply(1.2,1.2,1.2));
        }
    }
}