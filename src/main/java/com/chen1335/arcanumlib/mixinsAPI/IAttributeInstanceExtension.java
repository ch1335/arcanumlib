package com.chen1335.arcanumlib.mixinsAPI;

import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;

public interface IAttributeInstanceExtension {
    @Nullable
    LivingEntity arcanumlib$getOwner();

    void arcanumlib$setOwner(LivingEntity owner);
}
