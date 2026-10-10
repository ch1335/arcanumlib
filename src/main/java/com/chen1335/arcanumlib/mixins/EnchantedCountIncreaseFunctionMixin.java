package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.events.EnchantedLootEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 只盯「用附魔等级加掉落数量」这一个函数，取到等级后发事件，实际逻辑交给监听器。
 * 不要改成注入 EnchantmentHelper —— 那会被原版高频调用，代价太大。
 */
@Mixin(EnchantedCountIncreaseFunction.class)
public class EnchantedCountIncreaseFunctionMixin {

    @WrapOperation(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int arcanumlib$fireEnchantedLoot(Holder<Enchantment> holder, LivingEntity entity,
                                             Operation<Integer> original,
                                             @Local(argsOnly = true) LootContext context) {
        return EnchantedLootEvent.post(holder, context, original.call(holder, entity));
    }
}
