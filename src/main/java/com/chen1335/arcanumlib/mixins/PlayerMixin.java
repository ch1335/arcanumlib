package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.modules.attribute.AttributeHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 攻击伤害的临时调整：包住 {@link Player#attack(Entity)} 里那一次
 * {@code this.getAttributeValue(Attributes.ATTACK_DAMAGE)} 调用，实现在
 * {@link AttributeHooks#beforeGetAttackDamage}，这里只负责包住调用并保证弹出。
 *
 * <p>锚点用 {@code ordinal = 0}：该方法里 {@code getAttributeValue} 一共只出现两次，
 * 第一次就是 ATTACK_DAMAGE（第二次是 SWEEPING_DAMAGE_RATIO）。
 *
 * <p>调整只在这次读取期间生效，所以后续用这个 {@code float f} 参与的计算不受影响；
 * 那之后 {@code DamageSource} 才被创建，因此上下文只能给攻击者与目标（{@code AttackingContext}）。
 */
@Mixin(Player.class)
public class PlayerMixin {

    @WrapOperation(
            method = "attack(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(
                    value = "INVOKE",
                    ordinal = 0,
                    target = "Lnet/minecraft/world/entity/player/Player;getAttributeValue(Lnet/minecraft/core/Holder;)D"
            )
    )
    private double arcanumlib$adjustAttackDamage(Player instance, Holder<Attribute> attribute, Operation<Double> original, @Local(argsOnly = true) Entity target) {
        AttributeHooks.beforeGetAttackDamage(instance, target);
        try {
            return original.call(instance, attribute);
        } finally {
            AttributeHooks.afterGetAttackDamage(instance);
        }
    }
}