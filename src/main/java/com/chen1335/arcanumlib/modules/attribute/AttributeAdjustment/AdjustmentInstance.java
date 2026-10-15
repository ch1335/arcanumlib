package com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.util.Map;

public class AdjustmentInstance {
    private final Map<AttributeModifier.Operation, MutableDouble> modifiers = Map.of(
            AttributeModifier.Operation.ADD_VALUE, new MutableDouble(0),
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, new MutableDouble(0),
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, new MutableDouble(0)
    );


}
