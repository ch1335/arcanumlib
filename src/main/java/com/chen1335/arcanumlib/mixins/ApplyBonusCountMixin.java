package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.events.EnchantedLootEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * apply_bonus（时运/抢夺加掉落数量）用的也是物品版 getItemEnchantmentLevel，而且只看 TOOL。
 * 取到等级后只发事件，改动逻辑交给监听器。
 */
@Mixin(ApplyBonusCount.class)
public class ApplyBonusCountMixin {

    @WrapOperation(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getItemEnchantmentLevel(Lnet/minecraft/core/Holder;Lnet/minecraft/world/item/ItemStack;)I"))
    private int arcanumlib$fireEnchantedLoot(Holder<Enchantment> holder, ItemStack tool,
                                             Operation<Integer> original,
                                             @Local(argsOnly = true) LootContext context) {
        return EnchantedLootEvent.post(holder, context, original.call(holder, tool));
    }
}
