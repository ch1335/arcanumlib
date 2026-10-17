package com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment;

import com.chen1335.arcanumlib.modules.attribute.events.LivingGetAttributeAdjustmentEvent;
import com.chen1335.arcanumlib.registry.ArcanumAttachments;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

/**
 * 一次“应用临时调整”的作用域：打开时触发一次事件并挂好监听方给出的全部调整，关闭时按引用全部移除。
 *
 * <p>相比每读一条属性就触发一次事件，用法是：
 * <pre>{@code
 * try (AdjustmentScope scope = AdjustmentScope.open(new LivingDamageContext(target, source, damage))) {
 *     double armor = scope.get(target, Attributes.ARMOR);
 *     double toughness = scope.get(target, Attributes.ARMOR_TOUGHNESS);
 * }
 * }</pre>
 *
 * <p>注意：本类只负责“注册 / 移除”调整；把调整计入数值的是属性计算阶段的读取钩子。
 */
public final class AdjustmentScope implements AutoCloseable {

    /** 本次已挂上的调整；按引用保存，关闭时逐个移除（同一属性多次读取也不会串）。 */
    private final List<Applied> applied = new ArrayList<>();

    private boolean closed = false;

    private AdjustmentScope() {
    }

    /**
     * 触发一次 {@link LivingGetAttributeAdjustmentEvent}，并把监听方写入的全部（实体, 属性）调整挂进对应属性表。
     *
     * <p>监听方没给出的实体 / 属性不会有任何改动。
     */
    public static AdjustmentScope open(LivingGetAttributeAdjustmentEvent.IContext context) {
        AdjustmentScope scope = new AdjustmentScope();
        Map<LivingEntity, Map<Holder<Attribute>, AttributeAdjustment>> posted =
                LivingGetAttributeAdjustmentEvent.post(context);

        try {
            for (Map.Entry<LivingEntity, Map<Holder<Attribute>, AttributeAdjustment>> byEntity : posted.entrySet()) {
                LivingEntity owner = byEntity.getKey();
                for (Map.Entry<Holder<Attribute>, AttributeAdjustment> byAttribute : byEntity.getValue().entrySet()) {
                    Holder<Attribute> attribute = byAttribute.getKey();
                    AttributeAdjustment adjustment = byAttribute.getValue();
                    List<AttributeAdjustment> list = owner.getData(ArcanumAttachments.ENTITY_DATA.get())
                            .adjustmentData.getModifier(attribute);
                    list.add(adjustment);
                    scope.applied.add(new Applied(list, adjustment));
                }
            }
        } catch (RuntimeException e) {
            // 中途失败也要把已经挂上的摘掉，别留在列表里
            scope.close();
            throw e;
        }

        return scope;
    }

    /** 读取该实体的属性值（调整已在 {@link #open} 时挂好）。 */
    public double get(LivingEntity owner, Holder<Attribute> attribute) {
        return owner.getAttributeValue(attribute);
    }

    /**
     * 同上，但读取动作由调用方给出（例如 mixin 里要保留其它模组对原始调用的包装，
     * 只能把 {@code original.call(...)} 作为供应者传进来）。
     */
    public double get(LivingEntity owner, Holder<Attribute> attribute, DoubleSupplier supplier) {
        return supplier.getAsDouble();
    }

    /** 不取值：只让 {@code runnable} 在调整生效期间执行。 */
    public void run(LivingEntity owner, Holder<Attribute> attribute, Runnable runnable) {
        runnable.run();
    }

    /** 移除本次挂上的全部调整；可重复调用。 */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;
        // 逆序移除，与挂上的顺序相反
        for (int i = this.applied.size() - 1; i >= 0; i--) {
            Applied entry = this.applied.get(i);
            entry.list().remove(entry.adjustment());
        }
        this.applied.clear();
    }

    private record Applied(List<AttributeAdjustment> list, AttributeAdjustment adjustment) {
    }
}