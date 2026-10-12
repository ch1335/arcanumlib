package com.chen1335.arcanumlib.modules.loot.lootInjector.operator;

import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

import java.util.List;

/**
 * 由 {@code CompositeEntryBaseMixin} 实现：换掉复合条目的 children，并重算预先组合出来的容器。
 *
 * <p>{@code CompositeEntryBase} 在构造时就执行 {@code composedChildren = compose(children)}，
 * 掷骰真正走的是 {@code composedChildren}；只替换 {@code children} 字段不会生效。
 */
public interface CompositeChildrenAccess {

    /** 替换 children 并同步重算 composedChildren。 */
    void arcanumlib$replaceChildren(List<LootPoolEntryContainer> newChildren);
}