package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.mixinsAPI.IAttributeInstanceExtension;
import com.chen1335.arcanumlib.mixinsAPI.IAttributeMapExtension;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AttributeMap.class)
public class AttributeMapMixin implements IAttributeMapExtension {
    @Unique
    private LivingEntity arcanumlib$owner = null;

    @Override
    public @Nullable LivingEntity arcanumlib$getOwner() {
        return arcanumlib$owner;
    }

    @Override
    public void arcanumlib$setOwner(LivingEntity owner) {
        arcanumlib$owner = owner;
    }

    @ModifyReturnValue(method = "lambda$getInstance$1", at = @At("RETURN"))
    private AttributeInstance arcanumlib$bindInstanceOwner(AttributeInstance instance) {
        if (instance != null && this.arcanumlib$owner != null) {
            ((IAttributeInstanceExtension) instance).arcanumlib$setOwner(this.arcanumlib$owner);
        }

        return instance;
    }
}