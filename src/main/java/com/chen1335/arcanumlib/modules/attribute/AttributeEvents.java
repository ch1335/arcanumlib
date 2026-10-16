package com.chen1335.arcanumlib.modules.attribute;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AdjustmentHelper;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.DamageContext;
import com.chen1335.arcanumlib.modules.attribute.events.LivingGetAttributeAdjustmentEvent;
import com.chen1335.arcanumlib.registry.ArcanumTags;
import dev.shadowsoffire.apothic_attributes.api.ALObjects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;


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

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void LivingIncomingDamageEvent(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        Entity sourceEntity = source.getEntity();
        LivingEntity target = event.getEntity();
        DamageContext context = new DamageContext(target, source, event.getAmount());
        // 带 IGNORE_DAMAGE_MULTIPLIER 标签的伤害类型不吃攻击方的 DAMAGE_MULTIPLIER
        if (sourceEntity instanceof LivingEntity attacker && !source.is(ArcanumTags.Damage.IGNORE_DAMAGE_MULTIPLIER)) {
            event.setAmount(event.getAmount() * (float) AdjustmentHelper.getAttributeWithAdjusted(
                    attacker, ArcanumAttributes.DAMAGE_MULTIPLIER, context));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void LivingDamageEvent$Pre(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        LivingEntity target = event.getEntity();
        DamageContext context = new DamageContext(target, source, event.getNewDamage());
        event.setNewDamage(event.getNewDamage() * (float) AdjustmentHelper.getAttributeWithAdjusted(
                target, ArcanumAttributes.DAMAGE_TAKEN_MULTIPLIER, context));
    }
}