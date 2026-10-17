package com.chen1335.arcanumlib.modules.attribute;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.common.attachmentTypes.PlayerData;
import com.chen1335.arcanumlib.mixins.AttributeAccessor;
import com.chen1335.arcanumlib.modules.attribute.events.LivingAttributeChangeEvent;
import com.chen1335.arcanumlib.registry.ArcanumAttachments;
import com.chen1335.arcanumlib.registry.ArcanumTags;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Objects;


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
            event.add(type, ArcanumAttributes.ALL_ATTRIBUTE_MULTIPLIER);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void LivingIncomingDamageEvent(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        Entity sourceEntity = source.getEntity();
        // 带 IGNORE_DAMAGE_MULTIPLIER 标签的伤害类型不吃攻击方的 DAMAGE_MULTIPLIER
        if (sourceEntity instanceof LivingEntity attacker && !source.is(ArcanumTags.Damage.IGNORE_DAMAGE_MULTIPLIER)) {
            event.setAmount(event.getAmount() * (float) attacker.getAttributeValue(ArcanumAttributes.DAMAGE_MULTIPLIER));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void LivingDamageEvent$Pre(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        event.setNewDamage(event.getNewDamage() * (float) target.getAttributeValue(ArcanumAttributes.DAMAGE_TAKEN_MULTIPLIER));
    }


    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void PlayerTickEvent$Post(PlayerTickEvent.Post event) {
        PlayerData data = event.getEntity().getData(ArcanumAttachments.PLAYER_DATA);
        data.tick(event.getEntity());
    }

    public static final ResourceLocation ALL_ATTRIBUTE_MULTIPLIER_ID = ArcanumLib.id("all_attribute_multiplier");

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void LivingAttributeChangeEvent(LivingAttributeChangeEvent event) {
        if (event.getAttributeInstance().getAttribute().value() == ArcanumAttributes.ALL_ATTRIBUTE_MULTIPLIER.value()) {
            double attributeValue = event.getLivingEntity().getAttributeValue(ArcanumAttributes.ALL_ATTRIBUTE_MULTIPLIER) - 1D;
            for (AttributeInstance value : event.getLivingEntity().getAttributes().supplier.instances.values()) {
                Holder<Attribute> attribute = value.getAttribute();
                if (attribute.value() != ArcanumAttributes.ALL_ATTRIBUTE_MULTIPLIER.value() && ((AttributeAccessor) attribute.value()).arcanumlib$geSsentiment() == Attribute.Sentiment.POSITIVE && !attribute.is(ArcanumTags.Attribute.IGNORE_ATTRIBUTE_MULTIPLIER)) {
                    AttributeInstance instance = Objects.requireNonNull(event.getLivingEntity().getAttributes().getInstance(attribute));
                    instance.removeModifier(ALL_ATTRIBUTE_MULTIPLIER_ID);
                    if (attributeValue != 0) {
                        instance.addTransientModifier(new AttributeModifier(ALL_ATTRIBUTE_MULTIPLIER_ID, attributeValue, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    }
                }
            }
        }
    }
}