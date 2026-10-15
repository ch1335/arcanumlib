package com.chen1335.arcanumlib.modules.attribute;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class AttributeHooks {
    public static void beforeGetDamageMultiplier(LivingEntity attacker, Entity entity, DamageSource source) {
    }

    public static void afterGetDamageMultiplier(LivingEntity attacker, Entity entity, DamageSource source) {
    }

    public static void beforeGetDamageTakenMultiplier(Entity attacker, Entity entity, DamageSource source) {
    }

    public static void afterGetDamageTakenMultiplier(Entity attacker, Entity entity, DamageSource source) {
    }
}
