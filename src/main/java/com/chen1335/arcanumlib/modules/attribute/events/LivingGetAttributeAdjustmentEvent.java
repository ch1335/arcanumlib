package com.chen1335.arcanumlib.modules.attribute.events;

import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AttributeAdjustment;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

public class LivingGetAttributeAdjustmentEvent extends Event {
    private final Holder<Attribute> attribute;

    private final LivingEntity owner;
    private final IContext context;
    private final AttributeInstance instance;
    /**
     * 本次要附加的临时调整；监听方往里加，算完由调用方整份取出。
     */
    private final AttributeAdjustment adjustment = new AttributeAdjustment();

    public LivingGetAttributeAdjustmentEvent(Holder<Attribute> attributeHolder, LivingEntity owner, IContext context) {
        this.instance = owner.getAttributes().getInstance(attributeHolder);
        this.attribute = attributeHolder;
        this.owner = owner;
        this.context = context;
    }

    /**
     * 临时调整的载体：按运算读写 {@link AttributeAdjustment#add} / {@link AttributeAdjustment#get}。
     */
    public AttributeAdjustment getAdjustment() {
        return adjustment;
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

    public AttributeInstance getInstance() {
        return instance;
    }

    public interface IContext {

    }

    public static AttributeAdjustment post(Holder<Attribute> attributeHolder, LivingEntity owner, IContext context) {
        return NeoForge.EVENT_BUS.post(new LivingGetAttributeAdjustmentEvent(attributeHolder, owner, context)).adjustment;
    }
}
