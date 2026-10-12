package com.chen1335.arcanumlib.kubejs.helper;

import dev.latvian.mods.kubejs.typings.Info;
import org.jetbrains.annotations.Nullable;
import dev.latvian.mods.kubejs.typings.Info;
import org.jetbrains.annotations.Nullable;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.EntryOperator;
import dev.latvian.mods.kubejs.typings.Info;
import org.jetbrains.annotations.Nullable;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.FunctionOperator;
import dev.latvian.mods.kubejs.typings.Info;
import org.jetbrains.annotations.Nullable;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.ConditionOperator;
import dev.latvian.mods.kubejs.typings.Info;
import org.jetbrains.annotations.Nullable;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.PoolOperator;
import dev.latvian.mods.kubejs.typings.Info;
import org.jetbrains.annotations.Nullable;
import dev.latvian.mods.kubejs.typings.Info;
import org.jetbrains.annotations.Nullable;
import dev.latvian.mods.kubejs.typings.Info;
import org.jetbrains.annotations.Nullable;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.ConditionSelector;
import dev.latvian.mods.kubejs.typings.Info;
import org.jetbrains.annotations.Nullable;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.EntrySelector;
import dev.latvian.mods.kubejs.typings.Info;
import org.jetbrains.annotations.Nullable;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.FunctionSelector;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * {@code PoolOperator} 的脚本工厂：把 json 里的这个类别写成一行 Java 调用。
 *
 * <p>脚本侧（已注册为绑定 {@code PoolOperatorHelper}）用法例如：
 * <pre>{@code PoolOperatorHelper.removePool}</pre>
 */
public final class PoolOperatorHelper {

    private PoolOperatorHelper() {
    }

    /** 对应记录 {@code PoolOperator.RemovePool}（json 类型见该类的注册项）。 */
    @Info("Deletes the current pool.")
    public static PoolOperator removePool() {
        return new PoolOperator.RemovePool();
    }

    /** 对应记录 {@code PoolOperator.AddEntry}（json 类型见该类的注册项）。 */
    @Info("Appends an entry to the current pool.")
    public static PoolOperator addEntry(LootPoolEntryContainer entry) {
        return new PoolOperator.AddEntry(entry);
    }

    /** 对应记录 {@code PoolOperator.ModifyEntry}（json 类型见该类的注册项）。 */
    @Info("Runs the child entry ops on every matched entry of the current pool.")
    public static PoolOperator modifyEntry(EntrySelector entrySelector, List<EntryOperator> ops) {
        return new PoolOperator.ModifyEntry(entrySelector, ops);
    }

    /** 对应记录 {@code PoolOperator.SetCondition}（json 类型见该类的注册项）。 */
    @Info("Replaces the pool's whole condition list.")
    public static PoolOperator setCondition(List<LootItemCondition> conditions) {
        return new PoolOperator.SetCondition(conditions);
    }

    /** 对应记录 {@code PoolOperator.AddCondition}（json 类型见该类的注册项）。 */
    @Info("Appends conditions to the pool.")
    public static PoolOperator addCondition(List<LootItemCondition> conditions) {
        return new PoolOperator.AddCondition(conditions);
    }

    /** 对应记录 {@code PoolOperator.RemoveCondition}（json 类型见该类的注册项）。 */
    @Info("Removes the pool's conditions whose type id is in the list.")
    public static PoolOperator removeCondition(List<ResourceLocation> conditionTypes) {
        return new PoolOperator.RemoveCondition(conditionTypes);
    }

    /** 对应记录 {@code PoolOperator.SetFunction}（json 类型见该类的注册项）。 */
    @Info("Replaces the pool's whole function list.")
    public static PoolOperator setFunction(List<LootItemFunction> functions) {
        return new PoolOperator.SetFunction(functions);
    }

    /** 对应记录 {@code PoolOperator.AddFunction}（json 类型见该类的注册项）。 */
    @Info("Appends functions to the pool.")
    public static PoolOperator addFunction(List<LootItemFunction> functions) {
        return new PoolOperator.AddFunction(functions);
    }

    /** 对应记录 {@code PoolOperator.RemoveFunction}（json 类型见该类的注册项）。 */
    @Info("Removes the pool's functions whose type id is in the list.")
    public static PoolOperator removeFunction(List<ResourceLocation> functionTypes) {
        return new PoolOperator.RemoveFunction(functionTypes);
    }

    /** 对应记录 {@code PoolOperator.ModifyFunction}（json 类型见该类的注册项）。 */
    @Info("Runs the child function ops on every matched function of the current pool.")
    public static PoolOperator modifyFunction(FunctionSelector functionSelector, List<FunctionOperator> ops) {
        return new PoolOperator.ModifyFunction(functionSelector, ops);
    }

    /** 对应记录 {@code PoolOperator.ModifyCondition}（json 类型见该类的注册项）。 */
    @Info("Runs the child condition ops on every matched condition of the current pool.")
    public static PoolOperator modifyCondition(ConditionSelector conditionSelector, List<ConditionOperator> ops) {
        return new PoolOperator.ModifyCondition(conditionSelector, ops);
    }

    /** 对应记录 {@code PoolOperator.PoolCustom}（json 类型见该类的注册项）。 */
    @Info("Hands the current pool to a listener registered for that custom_id.")
    public static PoolOperator poolCustom(String id, @Nullable CompoundTag data) {
        return new PoolOperator.PoolCustom(id, Optional.ofNullable(data));
    }
}
