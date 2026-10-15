package com.chen1335.arcanumlib.modules.attribute;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.common.attachmentTypes.EntityData;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AttributeAdjustment;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.DamageContext;
import com.chen1335.arcanumlib.modules.attribute.events.LivingGetAttributeAdjustmentEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

/**
 * 属性挂载：自定义属性必须挂到实体类型上，否则 {@code LivingEntity#getAttribute} 返回 null，
 * 提供方就加不上 modifier，也就不会有任何同步。
 */
@EventBusSubscriber(modid = ArcanumLib.MODID)
public class AttributeEvents {

    // LOWEST：ApothicAttributes 在 NORMAL 也会注册这条属性，基础值由后注册者决定，所以必须排在它之后
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ArcanumAttributes.SEEKING);
        event.add(EntityType.PLAYER, ArcanumAttributes.FORTUNE);
        event.add(EntityType.PLAYER, ArcanumAttributes.LOOTING);
        event.add(EntityType.PLAYER, ArcanumAttributes.BONUS_LOOT);

        for (EntityType<? extends LivingEntity> type : event.getTypes()) {
            event.add(type, ArcanumAttributes.DAMAGE_MULTIPLIER);
            event.add(type, ArcanumAttributes.DAMAGE_TAKEN_MULTIPLIER);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttributeModification(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        Entity sourceEntity = source.getEntity();
        LivingEntity target = event.getEntity();
        DamageContext context = new DamageContext(target, source);
        if (sourceEntity instanceof LivingEntity attacker) {
            AttributeAdjustment modifier = LivingGetAttributeAdjustmentEvent.post(ArcanumAttributes.DAMAGE_MULTIPLIER, attacker, context);
            List<AttributeAdjustment> modifierList = EntityData.get(attacker).adjustmentData.getModifier(ArcanumAttributes.DAMAGE_MULTIPLIER);

            modifierList.add(modifier);
            try {
                event.setAmount(event.getAmount() * (float) attacker.getAttributeValue(ArcanumAttributes.DAMAGE_MULTIPLIER));
            } finally {
                // 按位置弹出：按值移除在重入（自伤 / 反弹）时会删掉另一条相等的调整
                modifierList.removeLast();
            }

        }
        AttributeAdjustment modifier = LivingGetAttributeAdjustmentEvent.post(ArcanumAttributes.DAMAGE_TAKEN_MULTIPLIER, target, context);
        List<AttributeAdjustment> modifierList = EntityData.get(target).adjustmentData.getModifier(ArcanumAttributes.DAMAGE_TAKEN_MULTIPLIER);

        modifierList.add(modifier);
        try {
            event.setAmount(event.getAmount() * (float) event.getEntity().getAttributeValue(ArcanumAttributes.DAMAGE_TAKEN_MULTIPLIER));
        } finally {
            modifierList.removeLast();
        }
        if (event.getSource().getEntity() instanceof Player player) {
            player.sendSystemMessage(Component.literal(String.valueOf(event.getAmount())));
        }
    }


}