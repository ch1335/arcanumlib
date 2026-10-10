package com.chen1335.arcanumlib.modules.loot.lootInjector;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.EntryCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.EntryConditionType;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.PoolCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.PoolConditionType;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.TableCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.TableConditionType;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.EntryOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.EntryOperatorType;
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
 * 注入操作的类型注册表，按作用层级分成三套：表级 / 池级 / 条目级。
 *
 * <p>同一个 id（例如 {@code set_function}）在三级各注册一份，json 里靠嵌套层级区分——
 * 写错层级会在解析期直接报"这一层没有这个 op"。别的模组用
 * {@code DeferredRegister.create(REGISTRY_KEY, 自己的 modid)} 就能加自己的 op。
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

    // ---- 表选择器：同一套做法，别的模组也能注册 ----

    public static final ResourceKey<Registry<TableConditionType>> TABLE_CONDITION_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("table_condition"));

    public static final Registry<TableConditionType> TABLE_CONDITIONS = new RegistryBuilder<>(TABLE_CONDITION_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<TableConditionType> TABLE_CONDITION_TYPES =
            DeferredRegister.create(TABLE_CONDITION_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<TableConditionType, TableConditionType> TABLE_ALL =
            TABLE_CONDITION_TYPES.register("all", () -> new TableConditionType(TableCondition.All.CODEC));
    public static final DeferredHolder<TableConditionType, TableConditionType> TABLE_BY_ID =
            TABLE_CONDITION_TYPES.register("id", () -> new TableConditionType(TableCondition.ById.CODEC));
    public static final DeferredHolder<TableConditionType, TableConditionType> TABLE_NAMESPACE =
            TABLE_CONDITION_TYPES.register("namespace", () -> new TableConditionType(TableCondition.Namespace.CODEC));
    public static final DeferredHolder<TableConditionType, TableConditionType> TABLE_PATH_PREFIX =
            TABLE_CONDITION_TYPES.register("path_prefix", () -> new TableConditionType(TableCondition.PathPrefix.CODEC));
    public static final DeferredHolder<TableConditionType, TableConditionType> TABLE_ENTITY_TAG =
            TABLE_CONDITION_TYPES.register("entity_tag", () -> new TableConditionType(TableCondition.EntityTagTable.CODEC));
    public static final DeferredHolder<TableConditionType, TableConditionType> TABLE_BLOCK_TAG =
            TABLE_CONDITION_TYPES.register("block_tag", () -> new TableConditionType(TableCondition.BlockTagTable.CODEC));
    public static final DeferredHolder<TableConditionType, TableConditionType> TABLE_NOT =
            TABLE_CONDITION_TYPES.register("not", () -> new TableConditionType(TableCondition.Not.CODEC));
    public static final DeferredHolder<TableConditionType, TableConditionType> TABLE_ANY_OF =
            TABLE_CONDITION_TYPES.register("any_of", () -> new TableConditionType(TableCondition.AnyOf.CODEC));
    public static final DeferredHolder<TableConditionType, TableConditionType> TABLE_ALL_OF =
            TABLE_CONDITION_TYPES.register("all_of", () -> new TableConditionType(TableCondition.AllOf.CODEC));
    /** 只带一个 id，判断交给 CustomTableConditionEvent 的监听者。 */
    public static final DeferredHolder<TableConditionType, TableConditionType> CUSTOM_TABLE =
            TABLE_CONDITION_TYPES.register("custom", () -> new TableConditionType(TableCondition.Custom.CODEC));

    // ---- 池选择器 ----

    public static final ResourceKey<Registry<PoolConditionType>> POOL_CONDITION_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("pool_condition"));

    public static final Registry<PoolConditionType> POOL_CONDITIONS = new RegistryBuilder<>(POOL_CONDITION_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<PoolConditionType> POOL_CONDITION_TYPES =
            DeferredRegister.create(POOL_CONDITION_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<PoolConditionType, PoolConditionType> POOL_ANY =
            POOL_CONDITION_TYPES.register("any", () -> new PoolConditionType(PoolCondition.Any.CODEC));
    public static final DeferredHolder<PoolConditionType, PoolConditionType> POOL_BY_INDEX =
            POOL_CONDITION_TYPES.register("index", () -> new PoolConditionType(PoolCondition.ByIndex.CODEC));
    public static final DeferredHolder<PoolConditionType, PoolConditionType> POOL_BY_NAME =
            POOL_CONDITION_TYPES.register("name", () -> new PoolConditionType(PoolCondition.ByName.CODEC));
    public static final DeferredHolder<PoolConditionType, PoolConditionType> POOL_HAS_ENTRY =
            POOL_CONDITION_TYPES.register("has_entry", () -> new PoolConditionType(PoolCondition.HasEntry.CODEC));
    public static final DeferredHolder<PoolConditionType, PoolConditionType> POOL_TOTAL_WEIGHT =
            POOL_CONDITION_TYPES.register("total_weight",
                    () -> new PoolConditionType(PoolCondition.TotalWeightInRange.CODEC));
    public static final DeferredHolder<PoolConditionType, PoolConditionType> POOL_NOT =
            POOL_CONDITION_TYPES.register("not", () -> new PoolConditionType(PoolCondition.Not.CODEC));
    public static final DeferredHolder<PoolConditionType, PoolConditionType> POOL_ANY_OF =
            POOL_CONDITION_TYPES.register("any_of", () -> new PoolConditionType(PoolCondition.AnyOf.CODEC));
    public static final DeferredHolder<PoolConditionType, PoolConditionType> POOL_ALL_OF =
            POOL_CONDITION_TYPES.register("all_of", () -> new PoolConditionType(PoolCondition.AllOf.CODEC));
    /** 只带一个 id，判断交给 CustomPoolConditionEvent 的监听者。 */
    public static final DeferredHolder<PoolConditionType, PoolConditionType> CUSTOM_POOL =
            POOL_CONDITION_TYPES.register("custom", () -> new PoolConditionType(PoolCondition.Custom.CODEC));

    // ---- 条目选择器 ----

    public static final ResourceKey<Registry<EntryConditionType>> ENTRY_CONDITION_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("entry_condition"));

    public static final Registry<EntryConditionType> ENTRY_CONDITIONS = new RegistryBuilder<>(ENTRY_CONDITION_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<EntryConditionType> ENTRY_CONDITION_TYPES =
            DeferredRegister.create(ENTRY_CONDITION_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_ANY =
            ENTRY_CONDITION_TYPES.register("any", () -> new EntryConditionType(EntryCondition.Any.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_BY_INDEX =
            ENTRY_CONDITION_TYPES.register("index", () -> new EntryConditionType(EntryCondition.ByIndex.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_IS_ITEM =
            ENTRY_CONDITION_TYPES.register("item", () -> new EntryConditionType(EntryCondition.IsItem.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_IS_TAG =
            ENTRY_CONDITION_TYPES.register("tag", () -> new EntryConditionType(EntryCondition.IsTag.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_IS_ITEM_WITH_TAG =
            ENTRY_CONDITION_TYPES.register("item_with_tag",
                    () -> new EntryConditionType(EntryCondition.IsItemWithTag.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_WEIGHT_SHARE =
            ENTRY_CONDITION_TYPES.register("weight_share",
                    () -> new EntryConditionType(EntryCondition.WeightShareInRange.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_IS_TYPE =
            ENTRY_CONDITION_TYPES.register("entry_type", () -> new EntryConditionType(EntryCondition.IsType.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_WEIGHT_IN_RANGE =
            ENTRY_CONDITION_TYPES.register("weight_in_range",
                    () -> new EntryConditionType(EntryCondition.WeightInRange.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_HAS_CONDITION =
            ENTRY_CONDITION_TYPES.register("has_condition",
                    () -> new EntryConditionType(EntryCondition.HasCondition.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_HAS_FUNCTION =
            ENTRY_CONDITION_TYPES.register("has_function",
                    () -> new EntryConditionType(EntryCondition.HasFunction.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_HAS_CHILD =
            ENTRY_CONDITION_TYPES.register("has_child", () -> new EntryConditionType(EntryCondition.HasChild.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_NOT =
            ENTRY_CONDITION_TYPES.register("not", () -> new EntryConditionType(EntryCondition.Not.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_ANY_OF =
            ENTRY_CONDITION_TYPES.register("any_of", () -> new EntryConditionType(EntryCondition.AnyOf.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_ALL_OF =
            ENTRY_CONDITION_TYPES.register("all_of", () -> new EntryConditionType(EntryCondition.AllOf.CODEC));
    /** 只带一个 id，判断交给 CustomEntryConditionEvent 的监听者。 */
    public static final DeferredHolder<EntryConditionType, EntryConditionType> CUSTOM_ENTRY =
            ENTRY_CONDITION_TYPES.register("custom", () -> new EntryConditionType(EntryCondition.Custom.CODEC));

    private LootInjectorOps() {
    }
}
