package com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment;

import com.chen1335.arcanumlib.modules.attribute.events.LivingGetAttributeAdjustmentEvent;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.function.DoubleSupplier;

/**
 * 单属性场景的便捷入口：内部就是 {@link AdjustmentScope} 的一行封装。
 *
 * <p>一次结算里要读多条属性（或想自己控制作用域）时，直接用
 * {@code try (AdjustmentScope scope = AdjustmentScope.open(context)) { … }}，事件只会触发一次。
 */
public class AdjustmentHelper {

    /** 读取该属性，返回包含监听方临时调整的数值。 */
    public static double getAttributeWithAdjusted(LivingEntity owner, Holder<Attribute> attribute,
                                                  LivingGetAttributeAdjustmentEvent.IContext context) {
        try (AdjustmentScope scope = AdjustmentScope.open(context)) {
            return scope.get(owner, attribute);
        }
    }

    /**
     * 同上，但读取动作由调用方给出（mixin 里要保留其它模组对同一调用的包装时用它）。
     */
    public static double getAttributeWithAdjusted(LivingEntity owner, Holder<Attribute> attribute,
                                                  DoubleSupplier supplier,
                                                  LivingGetAttributeAdjustmentEvent.IContext context) {
        try (AdjustmentScope scope = AdjustmentScope.open(context)) {
            return scope.get(owner, attribute, supplier);
        }
    }

    /** 不取值：只让 {@code runnable} 在调整生效期间执行。 */
    public static void runWhileAdjusted(LivingEntity owner, Holder<Attribute> attribute, Runnable runnable,
                                        LivingGetAttributeAdjustmentEvent.IContext context) {
        try (AdjustmentScope scope = AdjustmentScope.open(context)) {
            scope.run(owner, attribute, runnable);
        }
    }
}