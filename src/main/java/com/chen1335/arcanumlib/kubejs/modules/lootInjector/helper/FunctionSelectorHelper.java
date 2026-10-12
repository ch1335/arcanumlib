package com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper;

import dev.latvian.mods.kubejs.typings.Info;
import org.jetbrains.annotations.Nullable;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.FunctionSelector;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * {@code FunctionSelector} 的脚本工厂：把 json 里的这个类别写成一行 Java 调用。
 *
 * <p>脚本侧（已注册为绑定 {@code FunctionSelectorHelper}）用法例如：
 * <pre>{@code FunctionSelectorHelper.any}</pre>
 */
public final class FunctionSelectorHelper {

    private FunctionSelectorHelper() {
    }

    /** 对应记录 {@code FunctionSelector.Any}（json 类型见该类的注册项）。 */
    @Info("Matches every function (the default).")
    public static FunctionSelector any() {
        return new FunctionSelector.Any();
    }

    /** 对应记录 {@code FunctionSelector.ByIndex}（json 类型见该类的注册项）。 */
    @Info("Matches the function at that index, starting from 0.")
    public static FunctionSelector byIndex(int index) {
        return new FunctionSelector.ByIndex(index);
    }

    /** 对应记录 {@code FunctionSelector.ByType}（json 类型见该类的注册项）。 */
    @Info("Matches functions of that json type, such as minecraft:set_count.")
    public static FunctionSelector byType(ResourceLocation functionType) {
        return new FunctionSelector.ByType(functionType);
    }

    /** 对应记录 {@code FunctionSelector.HasCondition}（json 类型见该类的注册项）。 */
    @Info("Matches functions that carry a condition of that type.")
    public static FunctionSelector hasCondition(ResourceLocation conditionType) {
        return new FunctionSelector.HasCondition(conditionType);
    }

    /** 对应记录 {@code FunctionSelector.Not}（json 类型见该类的注册项）。 */
    @Info("Inverts the wrapped selector.")
    public static FunctionSelector not(FunctionSelector child) {
        return new FunctionSelector.Not(child);
    }

    /** 对应记录 {@code FunctionSelector.AnyOf}（json 类型见该类的注册项）。 */
    @Info("Matches when at least one of the listed selectors matches.")
    public static FunctionSelector anyOf(List<FunctionSelector> children) {
        return new FunctionSelector.AnyOf(children);
    }

    /** 对应记录 {@code FunctionSelector.AllOf}（json 类型见该类的注册项）。 */
    @Info("Matches when every listed selector matches.")
    public static FunctionSelector allOf(List<FunctionSelector> children) {
        return new FunctionSelector.AllOf(children);
    }

    /** 对应记录 {@code FunctionSelector.Match}（json 类型见该类的注册项）。 */
    @Info("Matches functions whose json contains the given values (subset match).")
    public static FunctionSelector match(Dynamic<?> function, boolean strict) {
        return new FunctionSelector.Match(function, strict);
    }

    /** 对应记录 {@code FunctionSelector.Custom}（json 类型见该类的注册项）。 */
    @Info("Hands the decision to a listener registered for that custom_id.")
    public static FunctionSelector custom(String id, @Nullable CompoundTag data) {
        return new FunctionSelector.Custom(id, Optional.ofNullable(data));
    }
}
