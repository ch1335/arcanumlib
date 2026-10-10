package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.events.EnchantedLootEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 和 EnchantedCountIncreaseFunctionMixin 一对：这个盯的是「用附魔等级加掉落概率」的谓词。
 * 取到等级后同样只发事件，逻辑交给监听器。
 */
@Mixin(LootItemRandomChanceWithEnchantedBonusCondition.class)
public class LootItemRandomChanceWithEnchantedBonusConditionMixin {

    @WrapOperation(method = "test(Lnet/minecraft/world/level/storage/loot/LootContext;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/entity/LivingEntity;)I"))
    private int arcanumlib$fireEnchantedLoot(Holder<Enchantment> holder, LivingEntity entity,
                                             Operation<Integer> original,
                                             @Local(argsOnly = true) LootContext context) {
        return EnchantedLootEvent.post(holder, context, original.call(holder, entity));
    }
}
