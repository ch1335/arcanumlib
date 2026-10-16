package com.chen1335.arcanumlib.mixins.apothic_attributes;

import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AdjustmentHelper;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.DamageContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.shadowsoffire.apothic_attributes.api.ALCombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让 ApothicAttributes 的护甲 / 保护公式内的属性读取同样吃属性修正。
 *
 * <p>该类读过四个属性（护甲压碎、护甲穿透、保护压碎、保护穿透），全部是攻击方身上的，
 * 上下文用 {@link DamageContext}（受伤方、伤害来源与本次伤害数值都由该方法拿到）。
 *
 * <p>锚点说明：字节码里这四处调用的 owner 是局部变量声明的静态类型 {@code LivingEntity}
 * （不是接收者的运行时类型），所以 target 必须按 {@code LivingEntity} 写。
 * {@code @At} 不写 ordinal 时四个调用点都会被包住。
 *
 * <p>包在 {@code ...mixins.apothic_attributes.} 下：由 {@code ArcanumLibMixinPlugin} 负责在
 * ApothicAttributes 缺席时整段跳过，不会因找不到目标类而报错。
 */

@Mixin(ALCombatRules.class)
public class ALCombatRulesMixin {

    @WrapOperation(
            method = {"getDamageAfterProtection", "getDamageAfterArmor"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"
            )
    )
    private static double arcanumlib$adjustAttribute(LivingEntity instance, Holder<Attribute> attribute, Operation<Double> original,
                                                     @Local(argsOnly = true) LivingEntity target,
                                                     @Local(argsOnly = true) DamageSource source,
                                                     @Local(argsOnly = true, ordinal = 0) float amount) {
        return AdjustmentHelper.getAttributeWithAdjusted(instance, attribute, () -> original.call(instance, attribute),
                new DamageContext(target, source, amount));
    }
}