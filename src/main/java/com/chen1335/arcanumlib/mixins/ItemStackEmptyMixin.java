package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.modules.holder.EntityHolderItem;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让 EntityHolderItem 无论数量是多少都算「空」。
 *
 * <p>原版 {@code isEmpty()} 是 {@code count <= 0}，所以一个 count=1 的 holder 在背包 / 容器里
 * 仍然会被当成真实物品：会被计数、被扣除、被存盘。这里把它钉成永远为空，于是：
 * <ul>
 *   <li>漏斗 / 容器把它当空格，不会真的写进去（{@code LootTable.fill} 的 isEmpty 分支也会直接写 EMPTY）；</li>
 *   <li>玩家背包不会留下它，掉落物也会被原版当空丢弃；</li>
 *   <li>数量永远不会被"移除"到某个中间值 —— 它从一开始就不参与数量语义。</li>
 * </ul>
 *
 * <p>注意：本模组读 holder 必须走 {@code EntityHolderItem.isHolder/payloadOf}（内部用 accessor 直接读字段），
 * 因为 {@code getItem()} 对空 stack 返回 {@code Items.AIR}、{@code get(...)} 返回 null。
 */
@Mixin(ItemStack.class)
public class ItemStackEmptyMixin {
    @ModifyReturnValue(method = "isEmpty", at = @At("RETURN"))
    private boolean arcanumlib$holderIsAlwaysEmpty(boolean original) {
        return original || ((ItemStackAccessor) (Object) this).arcanumlib$item() instanceof EntityHolderItem;
    }
}