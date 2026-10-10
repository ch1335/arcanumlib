package com.chen1335.arcanumlib.modules.attribute;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.events.EnchantedLootEvent;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 时运／抢夺属性各按同名附魔的等级生效：取到等级后直接加上属性值，再交给原版继续算。
 *
 * <p>读值必须走 {@code getInstance} 并判空——原版 {@code getAttributeValue} 对"没挂到该实体上"的属性是
 * 【抛异常】而不是返回 0，而这里拿到的可能是压根没挂这两条属性的生物。
 */
@EventBusSubscriber(modid = ArcanumLib.MODID)
public class EnchantedLootEvents {

    @SubscribeEvent
    public static void onEnchantedLoot(EnchantedLootEvent event) {
        Holder<Attribute> attribute = attributeFor(event.getEnchantment());
        if (attribute == null) {
            return;
        }

        // 实体掉落时责任人是击杀者，方块掉落时是挖矿的玩家（那时 ATTACKING_ENTITY 为空）
        LootContext context = event.getContext();
        Entity source = context.getParamOrNull(LootContextParams.ATTACKING_ENTITY);
        if (source == null) {
            source = context.getParamOrNull(LootContextParams.THIS_ENTITY);
        }
        if (!(source instanceof LivingEntity living)) {
            return;
        }

        AttributeInstance instance = living.getAttributes().getInstance(attribute);
        if (instance == null) {
            return;
        }

        int bonus = (int) Math.round(instance.getValue());
        if (bonus > 0) {
            event.setLevel(event.getLevel() + bonus);
        }
    }

    private static Holder<Attribute> attributeFor(Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.FORTUNE)) {
            return ArcanumAttributes.FORTUNE;
        }

        return enchantment.is(Enchantments.LOOTING) ? ArcanumAttributes.LOOTING : null;
    }
}
