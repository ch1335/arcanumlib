package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.modules.loot.bonusLoot.BonusLoot;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 给符合条件的战利品表追加额外掉落，逻辑见 {@link BonusLoot}。
 */
@Mixin(LootTable.class)
public abstract class LootTableMixin {

    @ModifyReturnValue(
            method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("RETURN")
    )
    private ObjectArrayList<ItemStack> arcanumlib$bonusLoot(ObjectArrayList<ItemStack> original, LootContext context) {
        return BonusLoot.append((LootTable) (Object) this, original, context);
    }
}
