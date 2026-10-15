package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.common.attachmentTypes.EntityData;
import com.chen1335.arcanumlib.mixinsAPI.IAttributeInstanceExtension;
import com.chen1335.arcanumlib.modules.attribute.events.LivingAttributeChangeEvent;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.apache.commons.lang3.mutable.MutableDouble;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Map;

@Debug(export = true)
@Mixin(AttributeInstance.class)
public abstract class AttributeInstanceMixin implements IAttributeInstanceExtension {

    @Shadow
    public abstract double getValue();

    @Shadow
    public abstract Holder<Attribute> getAttribute();

    @Unique
    private double arcanumlib$cachedValue;

    @Unique
    @Nullable
    private LivingEntity arcanumlib$owner = null;

    @Override
    public @Nullable LivingEntity arcanumlib$getOwner() {
        return arcanumlib$owner;
    }

    @Override
    public void arcanumlib$setOwner(LivingEntity owner) {
        arcanumlib$owner = owner;
    }


    @Inject(method = "setDirty", at = @At("RETURN"))
    private void onAttributeValueChanged(CallbackInfo ci) {
        double value = getValue();
        // 只在值真的变了、且已知 owner 时广播，避免同一数值反复触发
        if (arcanumlib$owner != null && arcanumlib$cachedValue != value) {
            LivingAttributeChangeEvent.post(arcanumlib$owner, (AttributeInstance) (Object) this, value);
            arcanumlib$cachedValue = value;
        }
    }

    @ModifyVariable(method = "calculateValue",
            at = @At(value = "INVOKE", ordinal = 1,
                    target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getModifiersOrEmpty(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)Ljava/util/Collection;"),
            ordinal = 1)
    private double arcanumlib$afterAddValue(double total) {
        if (arcanumlib$owner != null) {
            for (Map<AttributeModifier.Operation, MutableDouble> operationMutableDoubleMap : EntityData.get(arcanumlib$owner).adjustmentData.getModifier(getAttribute())) {
                total += operationMutableDoubleMap.get(AttributeModifier.Operation.ADD_VALUE).getValue();
            }
        }
        return total;
    }

    @ModifyVariable(method = "calculateValue",
            at = @At(value = "INVOKE", ordinal = 2,
                    target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getModifiersOrEmpty(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)Ljava/util/Collection;"),
            ordinal = 1)
    private double arcanumlib$afterAddMultipliedBase(double total, @Local(name = "d0") double d0) {
        if (arcanumlib$owner != null) {
            for (Map<AttributeModifier.Operation, MutableDouble> operationMutableDoubleMap : EntityData.get(arcanumlib$owner).adjustmentData.getModifier(getAttribute())) {
                total += d0 * operationMutableDoubleMap.get(AttributeModifier.Operation.ADD_MULTIPLIED_BASE).getValue();
            }
        }
        return total;
    }

    @ModifyVariable(method = "calculateValue",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/attributes/Attribute;sanitizeValue(D)D"),
            ordinal = 1)
    private double arcanumlib$afterAddMultipliedTotal(double total) {
        if (arcanumlib$owner != null) {
            for (Map<AttributeModifier.Operation, MutableDouble> operationMutableDoubleMap : EntityData.get(arcanumlib$owner).adjustmentData.getModifier(getAttribute())) {
                total *= 1 * operationMutableDoubleMap.get(AttributeModifier.Operation.ADD_MULTIPLIED_BASE).getValue();
            }
        }
        return total;
    }

}