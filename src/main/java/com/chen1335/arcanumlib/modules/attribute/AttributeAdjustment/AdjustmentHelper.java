package com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment;

import com.chen1335.arcanumlib.modules.attribute.events.LivingGetAttributeAdjustmentEvent;
import com.chen1335.arcanumlib.registry.ArcanumAttachments;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.List;
import java.util.function.DoubleSupplier;

public class AdjustmentHelper {
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
}
