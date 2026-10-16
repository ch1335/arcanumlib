package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AdjustmentHelper;
import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts.AttackingContext;
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
        return AdjustmentHelper.getAttributeWithAdjusted(instance, attribute, () -> original.call(instance, attribute), new AttackingContext(instance, target));
    }
}