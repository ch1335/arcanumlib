package com.chen1335.arcanumlib.modules.attribute;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.common.attachmentTypes.EntityData;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.DamageContext;
import com.chen1335.arcanumlib.modules.attribute.events.LivingGetAttributeAdjustmentEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.util.List;
import java.util.Map;

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
        if (sourceEntity instanceof LivingEntity attacker) {
            AttributeHooks.beforeGetDamageMultiplier(attacker, target, source);
            Map<AttributeModifier.Operation, MutableDouble> modifier = LivingGetAttributeAdjustmentEvent.post(ArcanumAttributes.DAMAGE_MULTIPLIER, attacker, new DamageContext(target, source));
            List<Map<AttributeModifier.Operation, MutableDouble>> modifierList = EntityData.get(attacker).adjustmentData.getModifier(ArcanumAttributes.DAMAGE_MULTIPLIER);

            modifierList.add(modifier);
            event.setAmount(event.getAmount() * (float) attacker.getAttributeValue(ArcanumAttributes.DAMAGE_MULTIPLIER));
            modifierList.remove(modifier);

            AttributeHooks.afterGetDamageMultiplier(attacker, target, source);
        }
        AttributeHooks.beforeGetDamageTakenMultiplier(sourceEntity, target, source);
        Map<AttributeModifier.Operation, MutableDouble> modifier = LivingGetAttributeAdjustmentEvent.post(ArcanumAttributes.DAMAGE_TAKEN_MULTIPLIER, target, new DamageContext(target, source));
        List<Map<AttributeModifier.Operation, MutableDouble>> modifierList = EntityData.get(target).adjustmentData.getModifier(ArcanumAttributes.DAMAGE_TAKEN_MULTIPLIER);

        modifierList.add(modifier);
        event.setAmount(event.getAmount() * (float) event.getEntity().getAttributeValue(ArcanumAttributes.DAMAGE_TAKEN_MULTIPLIER));
        modifierList.remove(modifier);

        AttributeHooks.afterGetDamageTakenMultiplier(sourceEntity, target, source);
    }


}
