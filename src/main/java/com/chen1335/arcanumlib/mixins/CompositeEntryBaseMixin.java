package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.CompositeChildrenAccess;
import net.minecraft.world.level.storage.loot.entries.ComposableEntryContainer;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * 让本模组能安全地改复合条目的 children。
 *
 * <p>Mixin 的代码会被合并进目标类，所以这里能直接读写 private final 的 {@code composedChildren}、
 * 调用 protected 的 {@code compose}，不需要任何 AT（AT 还要额外给三个子类的覆写提权，得不偿失）。
 */
@Mixin(CompositeEntryBase.class)
public abstract class CompositeEntryBaseMixin implements CompositeChildrenAccess {

    @Mutable
    @Final
    @Shadow
    public List<LootPoolEntryContainer> children;

    @Shadow
    @Final
    @Mutable
    private ComposableEntryContainer composedChildren;

    @Shadow
    protected abstract ComposableEntryContainer compose(List<? extends ComposableEntryContainer> children);

    @Override
    public void arcanumlib$replaceChildren(List<LootPoolEntryContainer> newChildren) {
        this.children = newChildren;
        this.composedChildren = this.compose(newChildren);
    }
}