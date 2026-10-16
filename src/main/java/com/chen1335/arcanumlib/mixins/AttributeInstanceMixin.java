package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.common.attachmentTypes.EntityData;
import com.chen1335.arcanumlib.mixinsAPI.IAttributeInstanceExtension;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AttributeAdjustment;
import com.chen1335.arcanumlib.modules.attribute.events.LivingAttributeChangeEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

import java.util.List;

@Mixin(AttributeInstance.class)
public abstract class AttributeInstanceMixin implements IAttributeInstanceExtension {

    @Shadow
    public abstract double getValue();

    @Shadow
    public abstract Holder<Attribute> getAttribute();

    @Shadow
    protected abstract double calculateValue();

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

    /**
     * 当前生效的临时调整列表；没有就在返回 null，让三个钩子直接跳过。
     *
     * <p>必须用 {@code getModifierOrNull}：属性计算是超热路径，{@code getModifier} 会顺手建空列表，
     * 等于每次重算都往每个实体的表里塞一条空记录。
     */
    @Unique
    @Nullable
    private List<AttributeAdjustment> arcanumlib$adjustments() {
        return arcanumlib$owner == null
                ? null
                : EntityData.get(arcanumlib$owner).adjustmentData.getModifierOrNull(getAttribute());
    }


    @ModifyExpressionValue(method = "calculateValue",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getBaseValue()D"))
    private double arcanumlib$afterAddValue(double base) {
        List<AttributeAdjustment> adjustments = arcanumlib$adjustments();
        if (adjustments == null) {
            return base;
        }
        for (AttributeAdjustment adjustment : adjustments) {
            base += adjustment.get(AttributeModifier.Operation.ADD_VALUE);
        }
        return base;
    }


    @ModifyVariable(method = "calculateValue",
            at = @At(value = "INVOKE", ordinal = 2,
                    target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getModifiersOrEmpty(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)Ljava/util/Collection;"),
            name = "d1")
    private double arcanumlib$afterAddMultipliedBase(double total, @Local(name = "d0") double d0) {
        List<AttributeAdjustment> adjustments = arcanumlib$adjustments();
        if (adjustments == null) {
            return total;
        }
        for (AttributeAdjustment adjustment : adjustments) {
            total += d0 * adjustment.get(AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        }
        return total;
    }


    @ModifyArg(method = "calculateValue",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/attributes/Attribute;sanitizeValue(D)D"),
            index = 0)
    private double arcanumlib$afterAddMultipliedTotal(double total) {
        List<AttributeAdjustment> adjustments = arcanumlib$adjustments();
        if (adjustments == null) {
            return total;
        }
        for (AttributeAdjustment adjustment : adjustments) {
            total *= adjustment.get(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
        return total;
    }

    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void arcanumlib$getValue(CallbackInfoReturnable<Double> cir) {
        List<AttributeAdjustment> adjustments = arcanumlib$adjustments();
        if (adjustments != null && !adjustments.isEmpty()) {
            cir.setReturnValue(calculateValue());
        }
    }
}