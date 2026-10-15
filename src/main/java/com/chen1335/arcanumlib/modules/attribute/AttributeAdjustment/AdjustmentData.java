package com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdjustmentData {
    private final Map<Holder<Attribute>, List<Map<AttributeModifier.Operation, MutableDouble>>> runtime = new HashMap<>();

    public List<Map<AttributeModifier.Operation, MutableDouble>> getModifier(Holder<Attribute> attribute) {
      return   runtime.computeIfAbsent(attribute, attributeHolder -> new ArrayList<>());
    }
}
