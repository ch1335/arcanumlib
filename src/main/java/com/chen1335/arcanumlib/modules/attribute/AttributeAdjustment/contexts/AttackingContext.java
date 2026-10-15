package com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts;

import com.chen1335.arcanumlib.modules.attribute.events.LivingGetAttributeAdjustmentEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public record AttackingContext(LivingEntity attacker, Entity target) implements LivingGetAttributeAdjustmentEvent.IContext{
}
