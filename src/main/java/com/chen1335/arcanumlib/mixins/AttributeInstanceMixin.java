package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.mixinsAPI.IAttributeInstanceExtension;
import com.chen1335.arcanumlib.modules.attribute.events.LivingAttributeChangeEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(AttributeInstance.class)
public abstract class AttributeInstanceMixin implements IAttributeInstanceExtension {

    @Shadow
    public abstract double getValue();

    /** 上一次广播出去的数值，用来判断"值真的变了"。 */
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

    /**
     * 所有改变数值的操作最后都会走到 {@code setDirty()}：改基础值、增 / 删 / 改 modifier、
     * {@code replaceFrom}、{@code load} —— 只钩这一处就覆盖全部。
     *
     * <p>锚在它的 RETURN：此时 {@code dirty} 已被置为 true，{@code getValue()} 会重新计算，拿到的是新值。
     */
    @Inject(method = "setDirty", at = @At("RETURN"))
    private void onAttributeValueChanged(CallbackInfo ci) {
        double value = getValue();
        // 只在值真的变了、且已知 owner 时广播，避免同一数值反复触发
        if (arcanumlib$owner != null && arcanumlib$cachedValue != value) {
            LivingAttributeChangeEvent.post(arcanumlib$owner, (AttributeInstance) (Object) this, value);
            arcanumlib$cachedValue = value;
        }
    }
}