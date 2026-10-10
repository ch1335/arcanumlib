package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.events.EnchantedLootEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * table_bonus（按等级查概率表）用的是物品版 getItemEnchantmentLevel，只看 TOOL，
 * descriptor 与下面两个实体版的不同，所以必须单独挂一个。
 */
@Mixin(BonusLevelTableCondition.class)
public class BonusLevelTableConditionMixin {

    @WrapOperation(method = "test(Lnet/minecraft/world/level/storage/loot/LootContext;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getItemEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemStack;)I"))
    private int arcanumlib$fireEnchantedLoot(Holder<Enchantment> holder, ItemStack tool,
                                             Operation<Integer> original,
                                             @Local(argsOnly = true) LootContext context) {
        return EnchantedLootEvent.post(holder, context, original.call(holder, tool));
    }
}
