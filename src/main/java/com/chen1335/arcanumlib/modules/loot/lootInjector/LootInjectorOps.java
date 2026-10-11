package com.chen1335.arcanumlib.modules.loot.lootInjector;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.EntrySelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.EntrySelectorType;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.PoolSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.PoolSelectorType;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.TableSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.ConditionSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.ConditionSelectorType;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.FunctionSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.FunctionSelectorType;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.TableSelectorType;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.EntryOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.ConditionOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.ConditionOperatorType;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.EntryOperatorType;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.FunctionOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.FunctionOperatorType;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.PoolOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.PoolOperatorType;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.TableOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.TableOperatorType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
* 注入操作的类型注册表：表级 / 池级 / 条目级三套，同一 id（如 {@code set_function}）三级各注册一份，json 靠嵌套层级区分。
* 写错层级会在解析期报"这一层没有这个 op"；别的模组用 {@code DeferredRegister.create(REGISTRY_KEY, 自己的 modid)} 加自己的 op。
*/
public final class LootInjectorOps {

    // ---- 表级操作 ----

    public static final ResourceKey<Registry<TableOperatorType>> TABLE_OPERATOR_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("table_operator"));

    // 只在服务端解析数据包时用得到，不需要同步给客户端
    public static final Registry<TableOperatorType> TABLE_OPERATORS = new RegistryBuilder<>(TABLE_OPERATOR_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<TableOperatorType> TABLE_OPERATOR_TYPES =
            DeferredRegister.create(TABLE_OPERATOR_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<TableOperatorType, TableOperatorType> TABLE_ADD_POOL =
            TABLE_OPERATOR_TYPES.register("add_pool", () -> new TableOperatorType(TableOperator.AddPool.CODEC));
    public static final DeferredHolder<TableOperatorType, TableOperatorType> TABLE_MODIFY_POOL =
            TABLE_OPERATOR_TYPES.register("modify_pool", () -> new TableOperatorType(TableOperator.ModifyPool.CODEC));
    public static final DeferredHolder<TableOperatorType, TableOperatorType> TABLE_SET_FUNCTION =
            TABLE_OPERATOR_TYPES.register("set_function", () -> new TableOperatorType(TableOperator.SetFunction.CODEC));
    public static final DeferredHolder<TableOperatorType, TableOperatorType> TABLE_ADD_FUNCTION =
            TABLE_OPERATOR_TYPES.register("add_function", () -> new TableOperatorType(TableOperator.AddFunction.CODEC));
    public static final DeferredHolder<TableOperatorType, TableOperatorType> TABLE_REMOVE_FUNCTION =
            TABLE_OPERATOR_TYPES.register("remove_function",
                    () -> new TableOperatorType(TableOperator.RemoveFunction.CODEC));
    public static final DeferredHolder<TableOperatorType, TableOperatorType> TABLE_CUSTOM =
            TABLE_OPERATOR_TYPES.register("table_custom", () -> new TableOperatorType(TableOperator.TableCustom.CODEC));
    public static final DeferredHolder<TableOperatorType, TableOperatorType> TABLE_MODIFY_FUNCTION =
            TABLE_OPERATOR_TYPES.register("modify_function",
                    () -> new TableOperatorType(TableOperator.ModifyFunction.CODEC));

    // ---- 池级操作 ----

    public static final ResourceKey<Registry<PoolOperatorType>> POOL_OPERATOR_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("pool_operator"));

    public static final Registry<PoolOperatorType> POOL_OPERATORS = new RegistryBuilder<>(POOL_OPERATOR_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<PoolOperatorType> POOL_OPERATOR_TYPES =
            DeferredRegister.create(POOL_OPERATOR_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<PoolOperatorType, PoolOperatorType> POOL_REMOVE_POOL =
            POOL_OPERATOR_TYPES.register("remove_pool", () -> new PoolOperatorType(PoolOperator.RemovePool.CODEC));
    public static final DeferredHolder<PoolOperatorType, PoolOperatorType> POOL_ADD_ENTRY =
            POOL_OPERATOR_TYPES.register("add_entry", () -> new PoolOperatorType(PoolOperator.AddEntry.CODEC));
    public static final DeferredHolder<PoolOperatorType, PoolOperatorType> POOL_MODIFY_ENTRY =
            POOL_OPERATOR_TYPES.register("modify_entry", () -> new PoolOperatorType(PoolOperator.ModifyEntry.CODEC));
    public static final DeferredHolder<PoolOperatorType, PoolOperatorType> POOL_SET_CONDITION =
            POOL_OPERATOR_TYPES.register("set_condition", () -> new PoolOperatorType(PoolOperator.SetCondition.CODEC));
    public static final DeferredHolder<PoolOperatorType, PoolOperatorType> POOL_ADD_CONDITION =
            POOL_OPERATOR_TYPES.register("add_condition", () -> new PoolOperatorType(PoolOperator.AddCondition.CODEC));
    public static final DeferredHolder<PoolOperatorType, PoolOperatorType> POOL_REMOVE_CONDITION =
            POOL_OPERATOR_TYPES.register("remove_condition",
                    () -> new PoolOperatorType(PoolOperator.RemoveCondition.CODEC));
    public static final DeferredHolder<PoolOperatorType, PoolOperatorType> POOL_SET_FUNCTION =
            POOL_OPERATOR_TYPES.register("set_function", () -> new PoolOperatorType(PoolOperator.SetFunction.CODEC));
    public static final DeferredHolder<PoolOperatorType, PoolOperatorType> POOL_ADD_FUNCTION =
            POOL_OPERATOR_TYPES.register("add_function", () -> new PoolOperatorType(PoolOperator.AddFunction.CODEC));
    public static final DeferredHolder<PoolOperatorType, PoolOperatorType> POOL_REMOVE_FUNCTION =
            POOL_OPERATOR_TYPES.register("remove_function",
                    () -> new PoolOperatorType(PoolOperator.RemoveFunction.CODEC));
    public static final DeferredHolder<PoolOperatorType, PoolOperatorType> POOL_CUSTOM =
            POOL_OPERATOR_TYPES.register("pool_custom", () -> new PoolOperatorType(PoolOperator.PoolCustom.CODEC));
    public static final DeferredHolder<PoolOperatorType, PoolOperatorType> POOL_MODIFY_FUNCTION =
            POOL_OPERATOR_TYPES.register("modify_function",
                    () -> new PoolOperatorType(PoolOperator.ModifyFunction.CODEC));
    public static final DeferredHolder<PoolOperatorType, PoolOperatorType> POOL_MODIFY_CONDITION =
            POOL_OPERATOR_TYPES.register("modify_condition",
                    () -> new PoolOperatorType(PoolOperator.ModifyCondition.CODEC));

    // ---- 条目级操作 ----

    public static final ResourceKey<Registry<EntryOperatorType>> ENTRY_OPERATOR_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("entry_operator"));

    public static final Registry<EntryOperatorType> ENTRY_OPERATORS = new RegistryBuilder<>(ENTRY_OPERATOR_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<EntryOperatorType> ENTRY_OPERATOR_TYPES =
            DeferredRegister.create(ENTRY_OPERATOR_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<EntryOperatorType, EntryOperatorType> ENTRY_REMOVE_ENTRY =
            ENTRY_OPERATOR_TYPES.register("remove_entry", () -> new EntryOperatorType(EntryOperator.RemoveEntry.CODEC));
    public static final DeferredHolder<EntryOperatorType, EntryOperatorType> ENTRY_REPLACE_ENTRY =
            ENTRY_OPERATOR_TYPES.register("replace_entry", () -> new EntryOperatorType(EntryOperator.ReplaceEntry.CODEC));
    public static final DeferredHolder<EntryOperatorType, EntryOperatorType> ENTRY_SET_WEIGHT =
            ENTRY_OPERATOR_TYPES.register("set_weight", () -> new EntryOperatorType(EntryOperator.SetWeight.CODEC));
    public static final DeferredHolder<EntryOperatorType, EntryOperatorType> ENTRY_SET_QUALITY =
            ENTRY_OPERATOR_TYPES.register("set_quality", () -> new EntryOperatorType(EntryOperator.SetQuality.CODEC));
    public static final DeferredHolder<EntryOperatorType, EntryOperatorType> ENTRY_SET_CONDITION =
            ENTRY_OPERATOR_TYPES.register("set_condition", () -> new EntryOperatorType(EntryOperator.SetCondition.CODEC));
    public static final DeferredHolder<EntryOperatorType, EntryOperatorType> ENTRY_ADD_CONDITION =
            ENTRY_OPERATOR_TYPES.register("add_condition", () -> new EntryOperatorType(EntryOperator.AddCondition.CODEC));
    public static final DeferredHolder<EntryOperatorType, EntryOperatorType> ENTRY_REMOVE_CONDITION =
            ENTRY_OPERATOR_TYPES.register("remove_condition",
                    () -> new EntryOperatorType(EntryOperator.RemoveCondition.CODEC));
    public static final DeferredHolder<EntryOperatorType, EntryOperatorType> ENTRY_SET_FUNCTION =
            ENTRY_OPERATOR_TYPES.register("set_function", () -> new EntryOperatorType(EntryOperator.SetFunction.CODEC));
    public static final DeferredHolder<EntryOperatorType, EntryOperatorType> ENTRY_ADD_FUNCTION =
            ENTRY_OPERATOR_TYPES.register("add_function", () -> new EntryOperatorType(EntryOperator.AddFunction.CODEC));
    public static final DeferredHolder<EntryOperatorType, EntryOperatorType> ENTRY_REMOVE_FUNCTION =
            ENTRY_OPERATOR_TYPES.register("remove_function",
                    () -> new EntryOperatorType(EntryOperator.RemoveFunction.CODEC));
    public static final DeferredHolder<EntryOperatorType, EntryOperatorType> ENTRY_CUSTOM =
            ENTRY_OPERATOR_TYPES.register("entry_custom", () -> new EntryOperatorType(EntryOperator.EntryCustom.CODEC));
    public static final DeferredHolder<EntryOperatorType, EntryOperatorType> ENTRY_MODIFY_FUNCTION =
            ENTRY_OPERATOR_TYPES.register("modify_function",
                    () -> new EntryOperatorType(EntryOperator.ModifyFunction.CODEC));
    public static final DeferredHolder<EntryOperatorType, EntryOperatorType> ENTRY_MODIFY_CONDITION =
            ENTRY_OPERATOR_TYPES.register("modify_condition",
                    () -> new EntryOperatorType(EntryOperator.ModifyCondition.CODEC));

    // ---- 函数级操作 ----

    public static final ResourceKey<Registry<FunctionOperatorType>> FUNCTION_OPERATOR_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("function_operator"));

    public static final Registry<FunctionOperatorType> FUNCTION_OPERATORS = new RegistryBuilder<>(FUNCTION_OPERATOR_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<FunctionOperatorType> FUNCTION_OPERATOR_TYPES =
            DeferredRegister.create(FUNCTION_OPERATOR_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<FunctionOperatorType, FunctionOperatorType> FUNCTION_OPERATOR_REMOVE =
            FUNCTION_OPERATOR_TYPES.register("remove_function",
                    () -> new FunctionOperatorType(FunctionOperator.RemoveFunction.CODEC));
    public static final DeferredHolder<FunctionOperatorType, FunctionOperatorType> FUNCTION_OPERATOR_REPLACE =
            FUNCTION_OPERATOR_TYPES.register("replace_function",
                    () -> new FunctionOperatorType(FunctionOperator.ReplaceFunction.CODEC));
    /** 作用域：钻进"当前函数"自带的条件。 */
    public static final DeferredHolder<FunctionOperatorType, FunctionOperatorType> FUNCTION_OPERATOR_MODIFY_CONDITION =
            FUNCTION_OPERATOR_TYPES.register("modify_condition",
                    () -> new FunctionOperatorType(FunctionOperator.ModifyCondition.CODEC));

    // ---- 条件级操作 ----

    public static final ResourceKey<Registry<ConditionOperatorType>> CONDITION_OPERATOR_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("condition_operator"));

    public static final Registry<ConditionOperatorType> CONDITION_OPERATORS = new RegistryBuilder<>(CONDITION_OPERATOR_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<ConditionOperatorType> CONDITION_OPERATOR_TYPES =
            DeferredRegister.create(CONDITION_OPERATOR_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<ConditionOperatorType, ConditionOperatorType> CONDITION_OPERATOR_REMOVE =
            CONDITION_OPERATOR_TYPES.register("remove_condition",
                    () -> new ConditionOperatorType(ConditionOperator.RemoveCondition.CODEC));
    public static final DeferredHolder<ConditionOperatorType, ConditionOperatorType> CONDITION_OPERATOR_REPLACE =
            CONDITION_OPERATOR_TYPES.register("replace_condition",
                    () -> new ConditionOperatorType(ConditionOperator.ReplaceCondition.CODEC));

    // ---- 表选择器：同一套做法，别的模组也能注册 ----

    public static final ResourceKey<Registry<TableSelectorType>> TABLE_SELECTOR_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("table_selector"));

    public static final Registry<TableSelectorType> TABLE_SELECTORS = new RegistryBuilder<>(TABLE_SELECTOR_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<TableSelectorType> TABLE_SELECTOR_TYPES =
            DeferredRegister.create(TABLE_SELECTOR_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<TableSelectorType, TableSelectorType> TABLE_ALL =
            TABLE_SELECTOR_TYPES.register("all", () -> new TableSelectorType(TableSelector.All.CODEC));
    public static final DeferredHolder<TableSelectorType, TableSelectorType> TABLE_BY_ID =
            TABLE_SELECTOR_TYPES.register("id", () -> new TableSelectorType(TableSelector.ById.CODEC));
    public static final DeferredHolder<TableSelectorType, TableSelectorType> TABLE_NAMESPACE =
            TABLE_SELECTOR_TYPES.register("namespace", () -> new TableSelectorType(TableSelector.Namespace.CODEC));
    public static final DeferredHolder<TableSelectorType, TableSelectorType> TABLE_PATH_PREFIX =
            TABLE_SELECTOR_TYPES.register("path_prefix", () -> new TableSelectorType(TableSelector.PathPrefix.CODEC));
    public static final DeferredHolder<TableSelectorType, TableSelectorType> TABLE_ENTITY_TAG =
            TABLE_SELECTOR_TYPES.register("entity_tag", () -> new TableSelectorType(TableSelector.EntityTagTable.CODEC));
    public static final DeferredHolder<TableSelectorType, TableSelectorType> TABLE_BLOCK_TAG =
            TABLE_SELECTOR_TYPES.register("block_tag", () -> new TableSelectorType(TableSelector.BlockTagTable.CODEC));
    /** 按实体类型 / 方块类型直接匹配，不走标签（对照 entity_tag / block_tag）。 */
    public static final DeferredHolder<TableSelectorType, TableSelectorType> TABLE_ENTITY_TYPE =
            TABLE_SELECTOR_TYPES.register("entity_type", () -> new TableSelectorType(TableSelector.ByEntityType.CODEC));
    public static final DeferredHolder<TableSelectorType, TableSelectorType> TABLE_BLOCK_TYPE =
            TABLE_SELECTOR_TYPES.register("block_type", () -> new TableSelectorType(TableSelector.ByBlockType.CODEC));
    /** 表里任意池满足给定的池选择器。 */
    public static final DeferredHolder<TableSelectorType, TableSelectorType> TABLE_HAS_POOL =
            TABLE_SELECTOR_TYPES.register("has_pool", () -> new TableSelectorType(TableSelector.HasPool.CODEC));
    public static final DeferredHolder<TableSelectorType, TableSelectorType> TABLE_NOT =
            TABLE_SELECTOR_TYPES.register("not", () -> new TableSelectorType(TableSelector.Not.CODEC));
    public static final DeferredHolder<TableSelectorType, TableSelectorType> TABLE_ANY_OF =
            TABLE_SELECTOR_TYPES.register("any_of", () -> new TableSelectorType(TableSelector.AnyOf.CODEC));
    public static final DeferredHolder<TableSelectorType, TableSelectorType> TABLE_ALL_OF =
            TABLE_SELECTOR_TYPES.register("all_of", () -> new TableSelectorType(TableSelector.AllOf.CODEC));
    /** 只带一个 id，判断交给 CustomTableSelectorEvent 的监听者。 */
    public static final DeferredHolder<TableSelectorType, TableSelectorType> CUSTOM_TABLE =
            TABLE_SELECTOR_TYPES.register("custom", () -> new TableSelectorType(TableSelector.Custom.CODEC));

    // ---- 池选择器 ----

    public static final ResourceKey<Registry<PoolSelectorType>> POOL_SELECTOR_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("pool_selector"));

    public static final Registry<PoolSelectorType> POOL_SELECTORS = new RegistryBuilder<>(POOL_SELECTOR_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<PoolSelectorType> POOL_SELECTOR_TYPES =
            DeferredRegister.create(POOL_SELECTOR_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<PoolSelectorType, PoolSelectorType> POOL_ANY =
            POOL_SELECTOR_TYPES.register("any", () -> new PoolSelectorType(PoolSelector.Any.CODEC));
    public static final DeferredHolder<PoolSelectorType, PoolSelectorType> POOL_BY_INDEX =
            POOL_SELECTOR_TYPES.register("index", () -> new PoolSelectorType(PoolSelector.ByIndex.CODEC));
    public static final DeferredHolder<PoolSelectorType, PoolSelectorType> POOL_BY_NAME =
            POOL_SELECTOR_TYPES.register("name", () -> new PoolSelectorType(PoolSelector.ByName.CODEC));
    public static final DeferredHolder<PoolSelectorType, PoolSelectorType> POOL_HAS_ENTRY =
            POOL_SELECTOR_TYPES.register("has_entry", () -> new PoolSelectorType(PoolSelector.HasEntry.CODEC));
    public static final DeferredHolder<PoolSelectorType, PoolSelectorType> POOL_TOTAL_WEIGHT =
            POOL_SELECTOR_TYPES.register("total_weight",
                    () -> new PoolSelectorType(PoolSelector.TotalWeightInRange.CODEC));
    public static final DeferredHolder<PoolSelectorType, PoolSelectorType> POOL_NOT =
            POOL_SELECTOR_TYPES.register("not", () -> new PoolSelectorType(PoolSelector.Not.CODEC));
    public static final DeferredHolder<PoolSelectorType, PoolSelectorType> POOL_ANY_OF =
            POOL_SELECTOR_TYPES.register("any_of", () -> new PoolSelectorType(PoolSelector.AnyOf.CODEC));
    public static final DeferredHolder<PoolSelectorType, PoolSelectorType> POOL_ALL_OF =
            POOL_SELECTOR_TYPES.register("all_of", () -> new PoolSelectorType(PoolSelector.AllOf.CODEC));
    /** 只带一个 id，判断交给 CustomPoolSelectorEvent 的监听者。 */
    public static final DeferredHolder<PoolSelectorType, PoolSelectorType> CUSTOM_POOL =
            POOL_SELECTOR_TYPES.register("custom", () -> new PoolSelectorType(PoolSelector.Custom.CODEC));

    // ---- 条目选择器 ----

    public static final ResourceKey<Registry<EntrySelectorType>> ENTRY_SELECTOR_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("entry_selector"));

    public static final Registry<EntrySelectorType> ENTRY_SELECTORS = new RegistryBuilder<>(ENTRY_SELECTOR_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<EntrySelectorType> ENTRY_SELECTOR_TYPES =
            DeferredRegister.create(ENTRY_SELECTOR_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> ENTRY_ANY =
            ENTRY_SELECTOR_TYPES.register("any", () -> new EntrySelectorType(EntrySelector.Any.CODEC));
    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> ENTRY_BY_INDEX =
            ENTRY_SELECTOR_TYPES.register("index", () -> new EntrySelectorType(EntrySelector.ByIndex.CODEC));
    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> ENTRY_IS_ITEM =
            ENTRY_SELECTOR_TYPES.register("item", () -> new EntrySelectorType(EntrySelector.IsItem.CODEC));
    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> ENTRY_IS_TAG =
            ENTRY_SELECTOR_TYPES.register("tag", () -> new EntrySelectorType(EntrySelector.IsTag.CODEC));
    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> ENTRY_IS_ITEM_WITH_TAG =
            ENTRY_SELECTOR_TYPES.register("item_with_tag",
                    () -> new EntrySelectorType(EntrySelector.IsItemWithTag.CODEC));
    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> ENTRY_WEIGHT_SHARE =
            ENTRY_SELECTOR_TYPES.register("weight_share",
                    () -> new EntrySelectorType(EntrySelector.WeightShareInRange.CODEC));
    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> ENTRY_IS_TYPE =
            ENTRY_SELECTOR_TYPES.register("entry_type", () -> new EntrySelectorType(EntrySelector.IsType.CODEC));
    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> ENTRY_WEIGHT_IN_RANGE =
            ENTRY_SELECTOR_TYPES.register("weight_in_range",
                    () -> new EntrySelectorType(EntrySelector.WeightInRange.CODEC));
    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> ENTRY_HAS_CONDITION =
            ENTRY_SELECTOR_TYPES.register("has_condition",
                    () -> new EntrySelectorType(EntrySelector.HasCondition.CODEC));
    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> ENTRY_HAS_FUNCTION =
            ENTRY_SELECTOR_TYPES.register("has_function",
                    () -> new EntrySelectorType(EntrySelector.HasFunction.CODEC));
    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> ENTRY_HAS_CHILD =
            ENTRY_SELECTOR_TYPES.register("has_child", () -> new EntrySelectorType(EntrySelector.HasChild.CODEC));
    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> ENTRY_NOT =
            ENTRY_SELECTOR_TYPES.register("not", () -> new EntrySelectorType(EntrySelector.Not.CODEC));
    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> ENTRY_ANY_OF =
            ENTRY_SELECTOR_TYPES.register("any_of", () -> new EntrySelectorType(EntrySelector.AnyOf.CODEC));
    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> ENTRY_ALL_OF =
            ENTRY_SELECTOR_TYPES.register("all_of", () -> new EntrySelectorType(EntrySelector.AllOf.CODEC));
    /** 只带一个 id，判断交给 CustomEntrySelectorEvent 的监听者。 */
    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> CUSTOM_ENTRY =
            ENTRY_SELECTOR_TYPES.register("custom", () -> new EntrySelectorType(EntrySelector.Custom.CODEC));
    // ---- 函数选择器 ----

    public static final ResourceKey<Registry<FunctionSelectorType>> FUNCTION_SELECTOR_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("function_selector"));

    public static final Registry<FunctionSelectorType> FUNCTION_SELECTORS = new RegistryBuilder<>(FUNCTION_SELECTOR_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<FunctionSelectorType> FUNCTION_SELECTOR_TYPES =
            DeferredRegister.create(FUNCTION_SELECTOR_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<FunctionSelectorType, FunctionSelectorType> FUNCTION_ANY =
            FUNCTION_SELECTOR_TYPES.register("any", () -> new FunctionSelectorType(FunctionSelector.Any.CODEC));
    public static final DeferredHolder<FunctionSelectorType, FunctionSelectorType> FUNCTION_BY_INDEX =
            FUNCTION_SELECTOR_TYPES.register("index", () -> new FunctionSelectorType(FunctionSelector.ByIndex.CODEC));
    public static final DeferredHolder<FunctionSelectorType, FunctionSelectorType> FUNCTION_BY_TYPE =
            FUNCTION_SELECTOR_TYPES.register("function_type",
                    () -> new FunctionSelectorType(FunctionSelector.ByType.CODEC));
    public static final DeferredHolder<FunctionSelectorType, FunctionSelectorType> FUNCTION_HAS_CONDITION =
            FUNCTION_SELECTOR_TYPES.register("has_condition",
                    () -> new FunctionSelectorType(FunctionSelector.HasCondition.CODEC));
    public static final DeferredHolder<FunctionSelectorType, FunctionSelectorType> FUNCTION_NOT =
            FUNCTION_SELECTOR_TYPES.register("not", () -> new FunctionSelectorType(FunctionSelector.Not.CODEC));
    public static final DeferredHolder<FunctionSelectorType, FunctionSelectorType> FUNCTION_ANY_OF =
            FUNCTION_SELECTOR_TYPES.register("any_of", () -> new FunctionSelectorType(FunctionSelector.AnyOf.CODEC));
    public static final DeferredHolder<FunctionSelectorType, FunctionSelectorType> FUNCTION_ALL_OF =
            FUNCTION_SELECTOR_TYPES.register("all_of", () -> new FunctionSelectorType(FunctionSelector.AllOf.CODEC));
    /** 只带一个 id，判断交给 CustomFunctionSelectorEvent 的监听者。 */
    public static final DeferredHolder<FunctionSelectorType, FunctionSelectorType> FUNCTION_CUSTOM =
            FUNCTION_SELECTOR_TYPES.register("custom", () -> new FunctionSelectorType(FunctionSelector.Custom.CODEC));
    public static final DeferredHolder<FunctionSelectorType, FunctionSelectorType> FUNCTION_MATCH =
            FUNCTION_SELECTOR_TYPES.register("match", () -> new FunctionSelectorType(FunctionSelector.Match.CODEC));

    // ---- 条件选择器 ----

    public static final ResourceKey<Registry<ConditionSelectorType>> CONDITION_SELECTOR_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("condition_selector"));

    public static final Registry<ConditionSelectorType> CONDITION_SELECTORS = new RegistryBuilder<>(CONDITION_SELECTOR_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<ConditionSelectorType> CONDITION_SELECTOR_TYPES =
            DeferredRegister.create(CONDITION_SELECTOR_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<ConditionSelectorType, ConditionSelectorType> CONDITION_ANY =
            CONDITION_SELECTOR_TYPES.register("any", () -> new ConditionSelectorType(ConditionSelector.Any.CODEC));
    public static final DeferredHolder<ConditionSelectorType, ConditionSelectorType> CONDITION_BY_INDEX =
            CONDITION_SELECTOR_TYPES.register("index", () -> new ConditionSelectorType(ConditionSelector.ByIndex.CODEC));
    public static final DeferredHolder<ConditionSelectorType, ConditionSelectorType> CONDITION_BY_TYPE =
            CONDITION_SELECTOR_TYPES.register("condition_type",
                    () -> new ConditionSelectorType(ConditionSelector.ByType.CODEC));
    public static final DeferredHolder<ConditionSelectorType, ConditionSelectorType> CONDITION_NOT =
            CONDITION_SELECTOR_TYPES.register("not", () -> new ConditionSelectorType(ConditionSelector.Not.CODEC));
    public static final DeferredHolder<ConditionSelectorType, ConditionSelectorType> CONDITION_ANY_OF =
            CONDITION_SELECTOR_TYPES.register("any_of", () -> new ConditionSelectorType(ConditionSelector.AnyOf.CODEC));
    public static final DeferredHolder<ConditionSelectorType, ConditionSelectorType> CONDITION_ALL_OF =
            CONDITION_SELECTOR_TYPES.register("all_of", () -> new ConditionSelectorType(ConditionSelector.AllOf.CODEC));
    /** 只带一个 id，判断交给 CustomConditionSelectorEvent 的监听者。 */
    public static final DeferredHolder<ConditionSelectorType, ConditionSelectorType> CONDITION_CUSTOM =
            CONDITION_SELECTOR_TYPES.register("custom", () -> new ConditionSelectorType(ConditionSelector.Custom.CODEC));
    public static final DeferredHolder<ConditionSelectorType, ConditionSelectorType> CONDITION_MATCH =
            CONDITION_SELECTOR_TYPES.register("match", () -> new ConditionSelectorType(ConditionSelector.Match.CODEC));

    private LootInjectorOps() {
    }
}
