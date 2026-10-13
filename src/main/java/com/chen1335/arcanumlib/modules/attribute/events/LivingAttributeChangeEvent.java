package com.chen1335.arcanumlib.modules.attribute.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 实体的属性值发生变化后触发（加/改 modifier、改基础值之后）。
 * 能拿到实体、发生变化的那条属性实例，以及变化后的数值。在 NeoForge.EVENT_BUS 上监听。
 *
 * <p>只读事件：不可取消，也改不了数值。
 */
public class LivingAttributeChangeEvent extends Event {
    private final LivingEntity livingEntity;
    private final AttributeInstance attributeInstance;

    /** 变化后的属性值（已把 modifier 计入）。 */
    private final double newValue;

    public LivingAttributeChangeEvent(LivingEntity livingEntity, AttributeInstance attributeInstance, double newValue) {
        this.livingEntity = livingEntity;
        this.attributeInstance = attributeInstance;
        this.newValue = newValue;
    }

    /** 属性所属的实体。 */
    public LivingEntity getLivingEntity() {
        return livingEntity;
    }

    /** 发生变化的那条属性实例：属性类型、基础值、modifier 列表都在里面。 */
    public AttributeInstance getAttributeInstance() {
        return attributeInstance;
    }

    /** 变化后的属性值。 */
    public double getNewValue() {
        return newValue;
    }

    /** 在 NeoForge 事件总线上广播一次。 */
    public static void post(LivingEntity livingEntity, AttributeInstance attributeInstance, double newValue) {
        NeoForge.EVENT_BUS.post(new LivingAttributeChangeEvent(livingEntity, attributeInstance, newValue));
    }
}