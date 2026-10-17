package com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper;

import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.ConditionSelector;
import com.mojang.serialization.Dynamic;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * {@code ConditionSelector} 的脚本工厂：把 json 里的这个类别写成一行 Java 调用。
 *
 * <p>脚本侧（已注册为绑定 {@code ConditionSelectorHelper}）用法例如：
 * <pre>{@code ConditionSelectorHelper.any}</pre>
 */
public final class ConditionSelectorHelper {

    private ConditionSelectorHelper() {
    }

    /** 对应记录 {@code ConditionSelector.Any}（json 类型见该类的注册项）。 */
    @Info("Matches every condition (the default).")
    public static ConditionSelector any() {
        return new ConditionSelector.Any();
    }

    /** 对应记录 {@code ConditionSelector.ByIndex}（json 类型见该类的注册项）。 */
    @Info("Matches the condition at that index, starting from 0.")
    public static ConditionSelector byIndex(int index) {
        return new ConditionSelector.ByIndex(index);
    }

    /** 对应记录 {@code ConditionSelector.ByType}（json 类型见该类的注册项）。 */
    @Info("Matches conditions of that json type, such as minecraft:random_chance.")
    public static ConditionSelector byType(ResourceLocation conditionType) {
        return new ConditionSelector.ByType(conditionType);
    }

    /** 对应记录 {@code ConditionSelector.Not}（json 类型见该类的注册项）。 */
    @Info("Inverts the wrapped selector.")
    public static ConditionSelector not(ConditionSelector child) {
        return new ConditionSelector.Not(child);
    }

    /** 对应记录 {@code ConditionSelector.AnyOf}（json 类型见该类的注册项）。 */
    @Info("Matches when at least one of the listed selectors matches.")
    public static ConditionSelector anyOf(List<ConditionSelector> children) {
        return new ConditionSelector.AnyOf(children);
    }

    /** 对应记录 {@code ConditionSelector.AllOf}（json 类型见该类的注册项）。 */
    @Info("Matches when every listed selector matches.")
    public static ConditionSelector allOf(List<ConditionSelector> children) {
        return new ConditionSelector.AllOf(children);
    }

    /** 对应记录 {@code ConditionSelector.Match}（json 类型见该类的注册项）。 */
    @Info("Matches conditions whose json contains the given values (subset match).")
    public static ConditionSelector match(Dynamic<?> condition, boolean strict) {
        return new ConditionSelector.Match(condition, strict);
    }

    /** 对应记录 {@code ConditionSelector.Custom}（json 类型见该类的注册项）。 */
    @Info("Hands the decision to a listener registered for that custom_id.")
    public static ConditionSelector custom(String id, @Nullable CompoundTag data) {
        return new ConditionSelector.Custom(id, Optional.ofNullable(data));
    }
}
