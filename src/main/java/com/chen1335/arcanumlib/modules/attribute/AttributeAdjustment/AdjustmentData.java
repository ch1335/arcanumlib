package com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdjustmentData {
    private final Map<Holder<Attribute>, List<AttributeAdjustment>> runtime = new HashMap<>();

    /** 取（必要时创建）该属性的临时调整列表：写入方用。 */
    public List<AttributeAdjustment> getModifier(Holder<Attribute> attribute) {
        return runtime.computeIfAbsent(attribute, attributeHolder -> new ArrayList<>());
    }

    /**
     * 只读查询：没有就返回 null，不会像 {@link #getModifier} 那样顺手建一个空列表。
     * 属性计算是超热路径（每次 getValue 重算都会问一遍），读取方必须走这里。
     */
    @Nullable
    public List<AttributeAdjustment> getModifierOrNull(Holder<Attribute> attribute) {
        return runtime.get(attribute);
    }
}