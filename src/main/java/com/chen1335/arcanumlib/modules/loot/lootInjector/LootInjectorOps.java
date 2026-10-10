package com.chen1335.arcanumlib.modules.loot.lootInjector;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.EntryCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.EntryConditionType;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.PoolCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.PoolConditionType;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.TableCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.TableConditionType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;

/** 注入操作的类型注册表；其他模组 DeferredRegister.create(REGISTRY_KEY, 自己的 modid) 就能加新 op。 */
public final class LootInjectorOps {
    public static final ResourceKey<Registry<LootInjectorOpType>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("loot_injector_op"));

    // 只在服务端解析数据包时用得到，不需要同步给客户端
    public static final Registry<LootInjectorOpType> REGISTRY = new RegistryBuilder<>(REGISTRY_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<LootInjectorOpType> OP_TYPES =
            DeferredRegister.create(REGISTRY_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<LootInjectorOpType, LootInjectorOpType> ADD_POOL =
            OP_TYPES.register("add_pool", () -> new LootInjectorOpType(LootInjectorOp.AddPool.CODEC));
    public static final DeferredHolder<LootInjectorOpType, LootInjectorOpType> REMOVE_POOL =
            OP_TYPES.register("remove_pool", () -> new LootInjectorOpType(LootInjectorOp.RemovePool.CODEC));
    public static final DeferredHolder<LootInjectorOpType, LootInjectorOpType> ADD_ENTRY =
            OP_TYPES.register("add_entry", () -> new LootInjectorOpType(LootInjectorOp.AddEntry.CODEC));
    public static final DeferredHolder<LootInjectorOpType, LootInjectorOpType> REMOVE_ENTRY =
            OP_TYPES.register("remove_entry", () -> new LootInjectorOpType(LootInjectorOp.RemoveEntry.CODEC));
    public static final DeferredHolder<LootInjectorOpType, LootInjectorOpType> MODIFY_ENTRY =
            OP_TYPES.register("modify_entry", () -> new LootInjectorOpType(LootInjectorOp.ModifyEntry.CODEC));
    public static final DeferredHolder<LootInjectorOpType, LootInjectorOpType> REPLACE_ENTRY =
            OP_TYPES.register("replace", () -> new LootInjectorOpType(LootInjectorOp.ReplaceEntry.CODEC));
    /** 只带一个 id，实际改动交给 CustomInjectorOpEvent 的监听者。 */
    public static final DeferredHolder<LootInjectorOpType, LootInjectorOpType> CUSTOM_OP =
            OP_TYPES.register("custom", () -> new LootInjectorOpType(LootInjectorOp.Custom.CODEC));

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
            ENTRY_CONDITION_TYPES.register("item_with_tag", () -> new EntryConditionType(EntryCondition.IsItemWithTag.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_IS_TYPE =
            ENTRY_CONDITION_TYPES.register("entry_type", () -> new EntryConditionType(EntryCondition.IsType.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_WEIGHT_AT_LEAST =
            ENTRY_CONDITION_TYPES.register("weight_at_least", () -> new EntryConditionType(EntryCondition.WeightAtLeast.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_HAS_CONDITION =
            ENTRY_CONDITION_TYPES.register("has_condition", () -> new EntryConditionType(EntryCondition.HasCondition.CODEC));
    public static final DeferredHolder<EntryConditionType, EntryConditionType> ENTRY_HAS_FUNCTION =
            ENTRY_CONDITION_TYPES.register("has_function", () -> new EntryConditionType(EntryCondition.HasFunction.CODEC));
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
