package com.chen1335.arcanumlib.modules.attribute.events;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.util.Map;

public class LivingGetAttributeAdjustmentEvent extends Event {
    private final Holder<Attribute> attribute;

    private final LivingEntity owner;
    private final IContext context;
    private final Map<AttributeModifier.Operation, MutableDouble> modifiers = Map.of(
            AttributeModifier.Operation.ADD_VALUE, new MutableDouble(0),
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE, new MutableDouble(0),
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, new MutableDouble(1)
    );

    public LivingGetAttributeAdjustmentEvent(Holder<Attribute> attributeHolder, LivingEntity owner, IContext context) {
        this.attribute = attributeHolder;
        this.owner = owner;
        this.context = context;
    }

    public MutableDouble getByOperation(AttributeModifier.Operation operation) {
        return modifiers.get(operation);
    }

    public IContext getContext() {
        return context;
    }

    public Holder<Attribute> getAttribute() {
        return attribute;
    }

    public LivingEntity getOwner() {
        return owner;
    }

    public interface IContext {

    }

    public static Map<AttributeModifier.Operation, MutableDouble> post(Holder<Attribute> attributeHolder, LivingEntity owner, IContext context) {
        return NeoForge.EVENT_BUS.post(new LivingGetAttributeAdjustmentEvent(attributeHolder, owner, context)).modifiers;
    }
}
