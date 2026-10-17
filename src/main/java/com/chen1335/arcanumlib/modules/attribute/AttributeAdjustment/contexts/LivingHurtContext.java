package com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts;

import com.chen1335.arcanumlib.modules.attribute.events.LivingGetAttributeAdjustmentEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

public record LivingHurtContext(LivingEntity target, DamageSource source,
                                float damage) implements LivingGetAttributeAdjustmentEvent.IContext {
}
