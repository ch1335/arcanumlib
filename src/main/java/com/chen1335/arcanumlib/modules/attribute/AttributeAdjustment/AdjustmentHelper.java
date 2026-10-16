package com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment;

import com.chen1335.arcanumlib.modules.attribute.events.LivingGetAttributeAdjustmentEvent;
import com.chen1335.arcanumlib.registry.ArcanumAttachments;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.List;
import java.util.function.DoubleSupplier;

/**
 * 属性修正的读取入口：三个方法都是「注册调整 → 执行 → 按引用移除调整」的同一套流程，
 * 区别只在“谁来读 / 谁来做”。
 *
 * <p>调整由监听方在事件里写入；移除用的是按引用（{@code remove(post)}），因此同一属性被重入读取时
 * 弹出的一定是自己那一条。
 */
public class AdjustmentHelper {

    /** 读取该属性，返回包含监听方临时调整的数值。 */
    public static double getAttributeWithAdjusted(LivingEntity owner, Holder<Attribute> attribute, LivingGetAttributeAdjustmentEvent.IContext context) {
        List<AttributeAdjustment> adjustments = owner.getData(ArcanumAttachments.ENTITY_DATA.get()).adjustmentData.getModifier(attribute);
        AttributeAdjustment post = LivingGetAttributeAdjustmentEvent.post(attribute, owner, context);
        double value;
        try {
            adjustments.add(post);
            value = owner.getAttributeValue(attribute);
        } finally {
            adjustments.remove(post);
        }
        return value;
    }

    /**
     * 同上，但读取动作由调用方给出（例如 {@code mixin} 里要保留其它模组对同一调用的包装，
     * 只能把 {@code original.call(...)} 作为供应者传进来）。
     */
    public static double getAttributeWithAdjusted(LivingEntity owner, Holder<Attribute> attribute, DoubleSupplier supplier, LivingGetAttributeAdjustmentEvent.IContext context) {
        List<AttributeAdjustment> adjustments = owner.getData(ArcanumAttachments.ENTITY_DATA.get()).adjustmentData.getModifier(attribute);
        AttributeAdjustment post = LivingGetAttributeAdjustmentEvent.post(attribute, owner, context);
        double value;
        try {
            adjustments.add(post);
            value = supplier.getAsDouble();
        } finally {
            adjustments.remove(post);
        }
        return value;
    }

    /** 不取值：只让 {@code runnable} 在调整生效期间执行。 */
    public static void runWhileAdjusted(LivingEntity owner, Holder<Attribute> attribute, Runnable runnable, LivingGetAttributeAdjustmentEvent.IContext context) {
        List<AttributeAdjustment> adjustments = owner.getData(ArcanumAttachments.ENTITY_DATA.get()).adjustmentData.getModifier(attribute);
        AttributeAdjustment post = LivingGetAttributeAdjustmentEvent.post(attribute, owner, context);
        try {
            adjustments.add(post);
            runnable.run();
        } finally {
            adjustments.remove(post);
        }
    }
}
