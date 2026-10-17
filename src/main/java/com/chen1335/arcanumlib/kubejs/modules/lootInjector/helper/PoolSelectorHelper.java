package com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper;

import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.EntrySelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.PoolSelector;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * {@code PoolSelector} 的脚本工厂：把 json 里的这个类别写成一行 Java 调用。
 *
 * <p>脚本侧（已注册为绑定 {@code PoolSelectorHelper}）用法例如：
 * <pre>{@code PoolSelectorHelper.any}</pre>
 */
public final class PoolSelectorHelper {

    private PoolSelectorHelper() {
    }

    /** 对应记录 {@code PoolSelector.Any}（json 类型见该类的注册项）。 */
    @Info("Matches every pool (the default).")
    public static PoolSelector any() {
        return new PoolSelector.Any();
    }

    /** 对应记录 {@code PoolSelector.ByIndex}（json 类型见该类的注册项）。 */
    @Info("Matches the pool at that index, starting from 0.")
    public static PoolSelector byIndex(int index) {
        return new PoolSelector.ByIndex(index);
    }

    /** 对应记录 {@code PoolSelector.ByName}（json 类型见该类的注册项）。 */
    @Info("Matches the pool that has the given name.")
    public static PoolSelector byName(String name) {
        return new PoolSelector.ByName(name);
    }

    /** 对应记录 {@code PoolSelector.HasEntry}（json 类型见该类的注册项）。 */
    @Info("Matches pools that have an entry matching the given entry selector.")
    public static PoolSelector hasEntry(EntrySelector entrySelector) {
        return new PoolSelector.HasEntry(entrySelector);
    }

    /** 对应记录 {@code PoolSelector.TotalWeightInRange}（json 类型见该类的注册项）。 */
    @Info("Matches pools whose total weight falls inside the closed interval.")
    public static PoolSelector totalWeightInRange(int min, int max) {
        return new PoolSelector.TotalWeightInRange(min, max);
    }

    /** 对应记录 {@code PoolSelector.Not}（json 类型见该类的注册项）。 */
    @Info("Inverts the wrapped selector.")
    public static PoolSelector not(PoolSelector child) {
        return new PoolSelector.Not(child);
    }

    /** 对应记录 {@code PoolSelector.AnyOf}（json 类型见该类的注册项）。 */
    @Info("Matches when at least one of the listed selectors matches.")
    public static PoolSelector anyOf(List<PoolSelector> children) {
        return new PoolSelector.AnyOf(children);
    }

    /** 对应记录 {@code PoolSelector.AllOf}（json 类型见该类的注册项）。 */
    @Info("Matches when every listed selector matches.")
    public static PoolSelector allOf(List<PoolSelector> children) {
        return new PoolSelector.AllOf(children);
    }

    /** 对应记录 {@code PoolSelector.Custom}（json 类型见该类的注册项）。 */
    @Info("Hands the decision to a listener registered for that custom_id.")
    public static PoolSelector custom(String id, @Nullable CompoundTag data) {
        return new PoolSelector.Custom(id, Optional.ofNullable(data));
    }
}
