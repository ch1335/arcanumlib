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
 *
 * <p><b>为什么注入私有方法 {@code getRandomItems(LootContext)}</b>：它是 {@code LootTable} 上
 * 唯一调用 {@code CommonHooks#modifyLoot} 的方法，因此是「一次战利品表摇取」的精确落点。
 * 注入更上层的 public 重载会让递归调用的内层也命中，而注入 {@code modifyLoot} 则会波及
 * 所有战利品表 —— 包括嵌套表与非实体来源。这里只对「整表的顶层摇取」生效。
 *
 * <p><b>为什么方法描述符要写全</b>：{@code LootTable} 上有多个 {@code getRandomItems} 重载，
 * 只写方法名无法定位，Mixin 会在应用阶段报重载歧义。
 */
@Mixin(LootTable.class)
public abstract class LootTableBonusLootMixin {

    @ModifyReturnValue(
            method = "getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at = @At("RETURN")
    )
    private ObjectArrayList<ItemStack> arcanumlib$bonusLoot(ObjectArrayList<ItemStack> original, LootContext context) {
        return BonusLoot.append((LootTable) (Object) this, original, context);
    }
}
