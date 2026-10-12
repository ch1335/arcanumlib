package com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper;

import dev.latvian.mods.kubejs.typings.Info;
import org.jetbrains.annotations.Nullable;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.FunctionOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.ConditionOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.EntryOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.ConditionSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.EntrySelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.FunctionSelector;
import java.util.List;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * {@code EntryOperator} 的脚本工厂：把 json 里的这个类别写成一行 Java 调用。
 *
 * <p>脚本侧（已注册为绑定 {@code EntryOperatorHelper}）用法例如：
 * <pre>{@code EntryOperatorHelper.removeEntry}</pre>
 */
public final class EntryOperatorHelper {

    private EntryOperatorHelper() {
    }

    /** 对应记录 {@code EntryOperator.RemoveEntry}（json 类型见该类的注册项）。 */
    @Info("Deletes the current entry.")
    public static EntryOperator removeEntry() {
        return new EntryOperator.RemoveEntry();
    }

    /** 对应记录 {@code EntryOperator.ReplaceEntry}（json 类型见该类的注册项）。 */
    @Info("Replaces the current entry with a new one (a fresh instance per site).")
    public static EntryOperator replaceEntry(LootPoolEntryContainer entry) {
        return new EntryOperator.ReplaceEntry(entry);
    }

    /** 对应记录 {@code EntryOperator.SetWeight}（json 类型见该类的注册项）。 */
    @Info("Sets the current entry's weight; a negative value leaves it alone; composite entries are skipped.")
    public static EntryOperator setWeight(int weight) {
        return new EntryOperator.SetWeight(weight);
    }

    /** 对应记录 {@code EntryOperator.SetQuality}（json 类型见该类的注册项）。 */
    @Info("Sets the current entry's quality; a negative value leaves it alone; composite entries are skipped.")
    public static EntryOperator setQuality(int quality) {
        return new EntryOperator.SetQuality(quality);
    }

    /** 对应记录 {@code EntryOperator.SetCondition}（json 类型见该类的注册项）。 */
    @Info("Replaces the current entry's whole condition list.")
    public static EntryOperator setCondition(List<LootItemCondition> conditions) {
        return new EntryOperator.SetCondition(conditions);
    }

    /** 对应记录 {@code EntryOperator.AddCondition}（json 类型见该类的注册项）。 */
    @Info("Appends conditions to the current entry.")
    public static EntryOperator addCondition(List<LootItemCondition> conditions) {
        return new EntryOperator.AddCondition(conditions);
    }

    /** 对应记录 {@code EntryOperator.RemoveCondition}（json 类型见该类的注册项）。 */
    @Info("Removes the current entry's conditions whose type id is in the list.")
    public static EntryOperator removeCondition(List<ResourceLocation> conditionTypes) {
        return new EntryOperator.RemoveCondition(conditionTypes);
    }

    /** 对应记录 {@code EntryOperator.SetFunction}（json 类型见该类的注册项）。 */
    @Info("Replaces the current entry's whole function list (composite entries are skipped).")
    public static EntryOperator setFunction(List<LootItemFunction> functions) {
        return new EntryOperator.SetFunction(functions);
    }

    /** 对应记录 {@code EntryOperator.AddFunction}（json 类型见该类的注册项）。 */
    @Info("Appends functions to the current entry (composite entries are skipped).")
    public static EntryOperator addFunction(List<LootItemFunction> functions) {
        return new EntryOperator.AddFunction(functions);
    }

    /** 对应记录 {@code EntryOperator.RemoveFunction}（json 类型见该类的注册项）。 */
    @Info("Removes the current entry's functions whose type id is in the list (composite entries are skipped).")
    public static EntryOperator removeFunction(List<ResourceLocation> functionTypes) {
        return new EntryOperator.RemoveFunction(functionTypes);
    }

    /** 对应记录 {@code EntryOperator.ModifyFunction}（json 类型见该类的注册项）。 */
    @Info("Runs the child function ops on every matched function of the current entry.")
    public static EntryOperator modifyFunction(FunctionSelector functionSelector, List<FunctionOperator> ops) {
        return new EntryOperator.ModifyFunction(functionSelector, ops);
    }

    /** 对应记录 {@code EntryOperator.ModifyCondition}（json 类型见该类的注册项）。 */
    @Info("Runs the child condition ops on every matched condition of the current entry.")
    public static EntryOperator modifyCondition(ConditionSelector conditionSelector, List<ConditionOperator> ops) {
        return new EntryOperator.ModifyCondition(conditionSelector, ops);
    }

    /** 对应记录 {@code EntryOperator.EntryCustom}（json 类型见该类的注册项）。 */
    @Info("Hands the current entry to a listener registered for that custom_id.")
    public static EntryOperator entryCustom(String id, @Nullable CompoundTag data) {
        return new EntryOperator.EntryCustom(id, Optional.ofNullable(data));
    }

    /** 对应记录 {@code EntryOperator.ModifyChild}（json 类型见该类的注册项）。 */
    @Info("Runs the child ops on every matched child of the current composite entry (only alternatives / group / sequence); nest it to go deeper.")
    public static EntryOperator modifyChild(EntrySelector entrySelector, List<EntryOperator> ops) {
        return new EntryOperator.ModifyChild(entrySelector, ops);
    }

    /** 对应记录 {@code EntryOperator.AddChild}（json 类型见该类的注册项）。 */
    @Info("Inserts a branch into the current composite entry (appended when no index is given).")
    public static EntryOperator addChild(LootPoolEntryContainer entry, @Nullable Integer index) {
        return new EntryOperator.AddChild(entry, Optional.ofNullable(index));
    }

    /** 对应记录 {@code EntryOperator.RemoveChild}（json 类型见该类的注册项）。 */
    @Info("Removes every matched child of the current composite entry.")
    public static EntryOperator removeChild(EntrySelector entrySelector) {
        return new EntryOperator.RemoveChild(entrySelector);
    }

    /** 对应记录 {@code EntryOperator.MoveChild}（json 类型见该类的注册项）。 */
    @Info("Moves a child of the current composite entry to first / last / the given index; the order of alternatives is its priority.")
    public static EntryOperator moveChild(EntrySelector entrySelector, String position, int index) {
        return new EntryOperator.MoveChild(entrySelector, position, index);
    }
}
