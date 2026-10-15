package com.chen1335.arcanumlib.modules.attribute;

import com.chen1335.arcanumlib.common.attachmentTypes.EntityData;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AdjustmentData;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AttributeAdjustment;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.AttackingContext;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.DamageContext;
import com.chen1335.arcanumlib.modules.attribute.events.LivingGetAttributeAdjustmentEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

import javax.annotation.Nullable;
import java.util.List;

public class AttributeHooks {

    /**
     * 护甲与护甲韧性的临时调整：在读取这两个属性（原版的护甲减免逻辑）之前注册。
     *
     * <p>顺序是"先 {@code post} 让监听方填值 → 再注册进属性表"：调整只有进了 {@code adjustmentData}，
     * 属性计算才会算上它。两个属性共用同一个 {@link DamageContext}，少一次分配。
     *
     * <p>读完必须调用 {@link #afterGetArmorAndToughness} 弹出（那边按位置 {@code removeLast()}，重入安全）。
     */
    public static void beforeGetArmorAndToughness(LivingEntity entity, DamageSource source) {
        AdjustmentData data = EntityData.get(entity).adjustmentData;
        DamageContext context = new DamageContext(entity, source);
        AttributeAdjustment armor = LivingGetAttributeAdjustmentEvent.post(Attributes.ARMOR, entity, context);
        AttributeAdjustment toughness = LivingGetAttributeAdjustmentEvent.post(Attributes.ARMOR_TOUGHNESS, entity, context);
        data.getModifier(Attributes.ARMOR).add(armor);
        data.getModifier(Attributes.ARMOR_TOUGHNESS).add(toughness);
    }

    /**
     * 与 {@link #beforeGetArmorAndToughness} 配对的收尾：按位置弹出（逆序），重入时弹出的是自己那一条。
     *
     * <p>用只读查询 + 空判断而不是 {@code getModifier}：万一配对关系被破坏（例如注册阶段抛异常），
     * 这里不应该再抛异常把真正的错误盖掉。
     */
    public static void afterGetArmorAndToughness(LivingEntity entity) {
        AdjustmentData data = EntityData.get(entity).adjustmentData;
        popLastAdjustment(data.getModifierOrNull(Attributes.ARMOR_TOUGHNESS));
        popLastAdjustment(data.getModifierOrNull(Attributes.ARMOR));
    }

    /**
     * 攻击伤害的临时调整：在 {@code Player#attack} 读取 ATTACK_DAMAGE 之前注册。
     *
     * <p>上下文用 {@link AttackingContext}：那一刻 {@code DamageSource} 还没创建，
     * 手上只有攻击者与被打的目标。
     */
    public static void beforeGetAttackDamage(LivingEntity attacker, Entity target) {
        AdjustmentData data = EntityData.get(attacker).adjustmentData;
        AttributeAdjustment adjustment = LivingGetAttributeAdjustmentEvent.post(
                Attributes.ATTACK_DAMAGE, attacker, new AttackingContext(attacker, target));
        data.getModifier(Attributes.ATTACK_DAMAGE).add(adjustment);
    }

    /** 与 {@link #beforeGetAttackDamage} 配对的收尾：按位置弹出。 */
    public static void afterGetAttackDamage(LivingEntity attacker) {
        popLastAdjustment(EntityData.get(attacker).adjustmentData.getModifierOrNull(Attributes.ATTACK_DAMAGE));
    }

    private static void popLastAdjustment(@Nullable List<AttributeAdjustment> adjustments) {
        if (adjustments != null && !adjustments.isEmpty()) {
            adjustments.removeLast();
        }
    }
}