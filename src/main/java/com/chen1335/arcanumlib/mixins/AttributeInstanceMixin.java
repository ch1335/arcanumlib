package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.common.attachmentTypes.EntityData;
import com.chen1335.arcanumlib.mixinsAPI.IAttributeInstanceExtension;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AdjustmentScope;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AttributeAdjustment;
import com.chen1335.arcanumlib.modules.attribute.events.LivingAttributeChangeEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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

    /** {@code calculateValue} 期间解析好的那一份；三个阶段钩子直接复用它，省掉各查一次。 */
    @Unique
    @Nullable
    private List<AttributeAdjustment> arcanumlib$calculatedAdjustments = null;

    /**
     * 当前生效的临时调整列表；没有就返回 null，让三个钩子直接跳过。
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

    /** 计算开始时解析一次；没有活跃作用域就直接置空，三个阶段钩子都能早退。 */
    @Inject(method = "calculateValue", at = @At("HEAD"))
    private void arcanumlib$resolveAdjustments(CallbackInfoReturnable<Double> cir) {
        this.arcanumlib$calculatedAdjustments = AdjustmentScope.isActive() ? arcanumlib$adjustments() : null;
    }

    @ModifyExpressionValue(method = "calculateValue",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getBaseValue()D"))
    private double arcanumlib$afterAddValue(double base) {
        List<AttributeAdjustment> adjustments = arcanumlib$calculatedAdjustments;
        if (adjustments == null) {
            return base;
        }
        for (AttributeAdjustment adjustment : adjustments) {
            base += adjustment.get(AttributeAdjustment.Operation.ADD_VALUE);
        }
        return base;
    }

    @ModifyVariable(method = "calculateValue",
            at = @At(value = "INVOKE", ordinal = 2,
                    target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getModifiersOrEmpty(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)Ljava/util/Collection;"),
            name = "d1")
    private double arcanumlib$afterAddMultipliedBase(double total, @Local(name = "d0") double d0) {
        List<AttributeAdjustment> adjustments = arcanumlib$calculatedAdjustments;
        if (adjustments == null) {
            return total;
        }
        for (AttributeAdjustment adjustment : adjustments) {
            total += d0 * adjustment.get(AttributeAdjustment.Operation.ADD_MULTIPLIED_BASE);
        }
        return total;
    }

    /**
     * 总倍率、{@code ADD_FINAL} 与 {@code SET_VALUE} 三个阶段的临时调整：直接改 {@code sanitizeValue(d1)} 的入参。
     *
     * <p>对每条调整依次执行：先乘总倍率，再加 {@code ADD_FINAL}，最后若给出了 {@code SET_VALUE}（非 {@code NaN}）则覆盖。
     * 因此 {@code SET_VALUE} 与顺序有关：后出现的调整会继续作用在其结果上。时机在 {@code sanitizeValue} 之前，上下限照旧生效。
     */
    @ModifyArg(method = "calculateValue",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/attributes/Attribute;sanitizeValue(D)D"),
            index = 0)
    private double arcanumlib$afterAddMultipliedTotal(double total) {
        List<AttributeAdjustment> adjustments = arcanumlib$calculatedAdjustments;
        if (adjustments == null) {
            return total;
        }
        for (AttributeAdjustment adjustment : adjustments) {
            total *= adjustment.get(AttributeAdjustment.Operation.ADD_MULTIPLIED_TOTAL);
        }
        for (AttributeAdjustment adjustment : adjustments) {
            total += adjustment.get(AttributeAdjustment.Operation.ADD_FINAL);
        }
        for (AttributeAdjustment adjustment : adjustments) {
            double setValue = adjustment.get(AttributeAdjustment.Operation.SET_VALUE);
            if (!Double.isNaN(setValue)) {
                total = setValue;
            }
        }
        return total;
    }

    @Inject(method = "getValue", at = @At("HEAD"), cancellable = true)
    private void arcanumlib$getValue(CallbackInfoReturnable<Double> cir) {
        // 没有活跃作用域 ⇒ 一定没有临时调整，跳过那次查询
        if (!AdjustmentScope.isActive()) {
            return;
        }

        List<AttributeAdjustment> adjustments = arcanumlib$adjustments();
        if (adjustments != null && !adjustments.isEmpty()) {
            cir.setReturnValue(calculateValue());
        }
    }
}
