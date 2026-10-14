package com.chen1335.arcanumlib.mixins;

import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 绕过原版对「空 stack」的读取守卫。
 *
 * <p>0 数量的战利品标记在原版眼里是空的，而 {@code ItemStack} 的两个读取入口都会因此撒谎：
 * <ul>
 *   <li>{@code getItem()} → {@code isEmpty() ? Items.AIR : this.item}；</li>
 *   <li>{@code getComponents()} → {@code isEmpty() ? DataComponentMap.EMPTY : this.components}。</li>
 * </ul>
 * 标记必须能读到自己的物品与组件，所以这里直接访问那两个私有字段。
 */
@Mixin(ItemStack.class)
public interface ItemStackAccessor {
    @Accessor("item")
    Item arcanumlib$item();

    @Accessor("components")
    PatchedDataComponentMap arcanumlib$components();
}