package com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper;

import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.FunctionOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.PoolOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.TableOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.FunctionSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.PoolSelector;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * {@code TableOperator} 的脚本工厂：把 json 里的这个类别写成一行 Java 调用。
 *
 * <p>脚本侧（已注册为绑定 {@code TableOperatorHelper}）用法例如：
 * <pre>{@code TableOperatorHelper.addPool}</pre>
 */
public final class TableOperatorHelper {

    private TableOperatorHelper() {
    }

    /** 对应记录 {@code TableOperator.AddPool}（json 类型见该类的注册项）。 */
    @Info("Adds a new pool to the table.")
    public static TableOperator addPool(LootPool pool) {
        return new TableOperator.AddPool(pool);
    }

    /** 对应记录 {@code TableOperator.ModifyPool}（json 类型见该类的注册项）。 */
    @Info("Runs the child ops on every matched pool of the table.")
    public static TableOperator modifyPool(PoolSelector poolSelector, List<PoolOperator> ops) {
        return new TableOperator.ModifyPool(poolSelector, ops);
    }

    /** 对应记录 {@code TableOperator.SetFunction}（json 类型见该类的注册项）。 */
    @Info("Replaces the table's whole function list.")
    public static TableOperator setFunction(List<LootItemFunction> functions) {
        return new TableOperator.SetFunction(functions);
    }

    /** 对应记录 {@code TableOperator.AddFunction}（json 类型见该类的注册项）。 */
    @Info("Appends functions to the table.")
    public static TableOperator addFunction(List<LootItemFunction> functions) {
        return new TableOperator.AddFunction(functions);
    }

    /** 对应记录 {@code TableOperator.RemoveFunction}（json 类型见该类的注册项）。 */
    @Info("Removes the table's functions whose type id is in the list.")
    public static TableOperator removeFunction(List<ResourceLocation> functionTypes) {
        return new TableOperator.RemoveFunction(functionTypes);
    }

    /** 对应记录 {@code TableOperator.ModifyFunction}（json 类型见该类的注册项）。 */
    @Info("Runs the child function ops on every matched function of the table.")
    public static TableOperator modifyFunction(FunctionSelector functionSelector, List<FunctionOperator> ops) {
        return new TableOperator.ModifyFunction(functionSelector, ops);
    }

    /** 对应记录 {@code TableOperator.TableCustom}（json 类型见该类的注册项）。 */
    @Info("Hands the table to a listener registered for that custom_id.")
    public static TableOperator tableCustom(String id, @Nullable CompoundTag data) {
        return new TableOperator.TableCustom(id, Optional.ofNullable(data));
    }
}
