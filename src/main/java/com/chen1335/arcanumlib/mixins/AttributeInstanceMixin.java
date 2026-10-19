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
import com.chen1335.arcanumlib.ArcanumLib;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.ArrayList;
import java.util.Collection;
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

    /**
     * 三种与原版同名的运算：都在 {@code calculateValue} 里"修饰符被遍历"的那一刻注入一条合成修饰符。
     *
     * <p>好处是基准值、遍历顺序、累加/相乘方式全部交给原版，不需要 {@code @Local} 抓局部变量，
     * 也不会改动 {@code d0} 造成重复计入。
     *
     * <p>三种运算的中性值都是 0；
     * {@code ADD_MULTIPLIED_TOTAL} 的数值按分数合并（{@code 0.5} 即 +50%，多条按 (1+a)(1+b)-1 连乘）；
     */
    @WrapOperation(method = "calculateValue",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/attributes/AttributeInstance;getModifiersOrEmpty(Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;)Ljava/util/Collection;"))
    private Collection<AttributeModifier> arcanumlib$withAdjustments(AttributeInstance instance, AttributeModifier.Operation operation,
                                                                     Operation<Collection<AttributeModifier>> original) {
        Collection<AttributeModifier> modifiers = original.call(instance, operation);
        List<AttributeAdjustment> adjustments = this.arcanumlib$calculatedAdjustments;
        if (adjustments == null) {
            return modifiers;
        }

        AttributeAdjustment.Operation adjusted = switch (operation) {
            case ADD_VALUE -> AttributeAdjustment.Operation.ADD_VALUE;
            case ADD_MULTIPLIED_BASE -> AttributeAdjustment.Operation.ADD_MULTIPLIED_BASE;
            case ADD_MULTIPLIED_TOTAL -> AttributeAdjustment.Operation.ADD_MULTIPLIED_TOTAL;
        };

        // 三种运算的中性值都是 0；合并规则由 Operation 自己给：
        // 加法的相加，ADD_MULTIPLIED_TOTAL 按分数连乘（(1+a)(1+b)-1），结果仍是分数，正好喂给原版 d1 *= 1 + amount
        double amount = 0.0D;
        for (AttributeAdjustment adjustment : adjustments) {
            amount = adjusted.get(amount, adjustment.get(adjusted));
        }
        if (amount == 0.0D) {
            return modifiers;                                  // 全是中性值，别动原集合
        }

        List<AttributeModifier> merged = new ArrayList<>(modifiers);
        merged.add(new AttributeModifier(ArcanumLib.id("temporary_adjustment"), amount, operation));
        return merged;
    }

    @ModifyArg(method = "calculateValue",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/attributes/Attribute;sanitizeValue(D)D"),
            index = 0)
    private double arcanumlib$afterFinalStages(double total) {
        List<AttributeAdjustment> adjustments = arcanumlib$calculatedAdjustments;
        if (adjustments == null) {
            return total;
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

