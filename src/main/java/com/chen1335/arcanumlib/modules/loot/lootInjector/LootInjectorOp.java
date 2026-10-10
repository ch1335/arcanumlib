package com.chen1335.arcanumlib.modules.loot.lootInjector;

import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.EntryCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.PoolCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomInjectorOpEvent;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.ArrayList;
import java.util.List;

/**
 * 注入操作：对一张已经确定要处理的表执行。
 * 陷阱：conditions/functions 在构造器里已合成 composite，只换列表不重算复合体【改动完全不生效也不报错】；entries 没有复合体，换成可变列表即可。
 */
public interface LootInjectorOp {

    /** 按注册表名分发，所以别的模组注册进 {@link LootInjectorOps#REGISTRY_KEY} 就能加自己的类型。 */
    Codec<LootInjectorOp> CODEC = LootInjectorOps.REGISTRY.byNameCodec()
            .dispatch("type", LootInjectorOp::getType, LootInjectorOpType::codec);

    LootInjectorOpType getType();

    void apply(LootTable table, HolderLookup.Provider registries);

    /** 新增一个池，追加在最后。 */
    record AddPool(LootPool pool) implements LootInjectorOp {
        @Override
        public LootInjectorOpType getType() {
            return LootInjectorOps.ADD_POOL.get();
        }
        public static final MapCodec<AddPool> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootPool.CODEC.fieldOf("pool").forGetter(AddPool::pool)).apply(instance, AddPool::new));
        @Override
        public void apply(LootTable table, HolderLookup.Provider registries) {
            table.pools.add(this.pool);
        }
    }

    /** 删除所有命中的池。 */
    record RemovePool(PoolCondition poolCondition) implements LootInjectorOp {
        @Override
        public LootInjectorOpType getType() {
            return LootInjectorOps.REMOVE_POOL.get();
        }
        public static final MapCodec<RemovePool> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PoolCondition.CODEC.optionalFieldOf("poolCondition", new PoolCondition.Any())
                        .forGetter(RemovePool::poolCondition)).apply(instance, RemovePool::new));
        @Override
        public void apply(LootTable table, HolderLookup.Provider registries) {
            for (int i = table.pools.size() - 1; i >= 0; i--) {
                if (this.poolCondition.test(i, table.pools.get(i))) {
                    table.pools.remove(i);
                }
            }
        }
    }

    /** 往所有命中的池里追加一个条目。 */
    record AddEntry(PoolCondition poolCondition, LootPoolEntryContainer entry) implements LootInjectorOp {
        @Override
        public LootInjectorOpType getType() {
            return LootInjectorOps.ADD_ENTRY.get();
        }
        public static final MapCodec<AddEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PoolCondition.CODEC.optionalFieldOf("poolCondition", new PoolCondition.Any())
                        .forGetter(AddEntry::poolCondition),
                LootPoolEntries.CODEC.fieldOf("entry").forGetter(AddEntry::entry))
                .apply(instance, AddEntry::new));
        @Override
        public void apply(LootTable table, HolderLookup.Provider registries) {
            // 用下标循环：indexOf 是 O(n²)，而且同一个池实例出现两次时会拿错下标
            for (int p = 0; p < table.pools.size(); p++) {
                LootPool pool = table.pools.get(p);
                if (this.poolCondition.test(p, pool)) {
                    mutableEntries(pool).add(this.entry);
                }
            }
        }
    }

    /** 删除所有命中的条目。 */
    record RemoveEntry(PoolCondition poolCondition, EntryCondition entryCondition) implements LootInjectorOp {
        @Override
        public LootInjectorOpType getType() {
            return LootInjectorOps.REMOVE_ENTRY.get();
        }
        public static final MapCodec<RemoveEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PoolCondition.CODEC.optionalFieldOf("poolCondition", new PoolCondition.Any())
                        .forGetter(RemoveEntry::poolCondition),
                EntryCondition.CODEC.optionalFieldOf("entryCondition", new EntryCondition.Any())
                        .forGetter(RemoveEntry::entryCondition))
                .apply(instance, RemoveEntry::new));
        @Override
        public void apply(LootTable table, HolderLookup.Provider registries) {
            for (int p = 0; p < table.pools.size(); p++) {
                LootPool pool = table.pools.get(p);
                if (!this.poolCondition.test(p, pool)) {
                    continue;
                }

                List<LootPoolEntryContainer> entries = mutableEntries(pool);
                for (int i = entries.size() - 1; i >= 0; i--) {
                    if (this.entryCondition.test(i, entries.get(i))) {
                        entries.remove(i);
                    }
                }
            }
        }
    }

    /** 改条目的条件／函数／权重／品质。追加条件与函数时必须重建 composite，否则掷骰时不起作用。 */
    record ModifyEntry(PoolCondition poolCondition,
                       EntryCondition entryCondition,
                       List<LootItemCondition> addConditions,
                       List<LootItemFunction> addFunctions,
                       int setWeight,
                       int setQuality) implements LootInjectorOp {
        @Override
        public LootInjectorOpType getType() {
            return LootInjectorOps.MODIFY_ENTRY.get();
        }
        public static final MapCodec<ModifyEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PoolCondition.CODEC.optionalFieldOf("poolCondition", new PoolCondition.Any())
                        .forGetter(ModifyEntry::poolCondition),
                EntryCondition.CODEC.optionalFieldOf("entryCondition", new EntryCondition.Any())
                        .forGetter(ModifyEntry::entryCondition),
                LootItemCondition.DIRECT_CODEC.listOf().optionalFieldOf("add_conditions", List.of())
                        .forGetter(ModifyEntry::addConditions),
                LootItemFunctions.ROOT_CODEC.listOf().optionalFieldOf("add_functions", List.of())
                        .forGetter(ModifyEntry::addFunctions),
                Codec.INT.optionalFieldOf("set_weight", -1).forGetter(ModifyEntry::setWeight),
                Codec.INT.optionalFieldOf("set_quality", -1).forGetter(ModifyEntry::setQuality)
        ).apply(instance, ModifyEntry::new));
        @Override
        public void apply(LootTable table, HolderLookup.Provider registries) {
            for (int p = 0; p < table.pools.size(); p++) {
                LootPool pool = table.pools.get(p);
                if (!this.poolCondition.test(p, pool)) {
                    continue;
                }

                for (int i = 0; i < pool.entries.size(); i++) {
                    LootPoolEntryContainer entry = pool.entries.get(i);
                    if (this.entryCondition.test(i, entry)) {
                        this.modify(entry);
                    }
                }
            }
        }

        private void modify(LootPoolEntryContainer entry) {
            if (!this.addConditions.isEmpty()) {
                List<LootItemCondition> conditions = new ArrayList<>(entry.conditions);
                conditions.addAll(this.addConditions);

                entry.conditions = conditions;
                entry.compositeCondition = Util.allOf(conditions);          // 必须重算
            }

            if (!(entry instanceof LootPoolSingletonContainer singleton)) {
                return;
            }

            if (!this.addFunctions.isEmpty()) {
                List<LootItemFunction> functions = new ArrayList<>(singleton.functions);
                functions.addAll(this.addFunctions);

                singleton.functions = functions;
                singleton.compositeFunction = LootItemFunctions.compose(functions);   // 必须重算
            }

            if (this.setWeight >= 0) {
                singleton.weight = this.setWeight;
            }

            if (this.setQuality >= 0) {
                singleton.quality = this.setQuality;
            }
        }
    }

    /** 池里的 entries 可能来自不可变列表，增删前先换一份可变的。 */
    private static List<LootPoolEntryContainer> mutableEntries(LootPool pool) {
        if (!(pool.entries instanceof ArrayList)) {
            pool.entries = new ArrayList<>(pool.entries);
        }

        return pool.entries;
    }

    /** 把所有命中的条目【就地】换成新条目。 */
    record ReplaceEntry(PoolCondition poolCondition, EntryCondition entryCondition,
                        LootPoolEntryContainer entry) implements LootInjectorOp {
        @Override
        public LootInjectorOpType getType() {
            return LootInjectorOps.REPLACE_ENTRY.get();
        }
        public static final MapCodec<ReplaceEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PoolCondition.CODEC.optionalFieldOf("poolCondition", new PoolCondition.Any())
                        .forGetter(ReplaceEntry::poolCondition),
                EntryCondition.CODEC.optionalFieldOf("entryCondition", new EntryCondition.Any())
                        .forGetter(ReplaceEntry::entryCondition),
                LootPoolEntries.CODEC.fieldOf("entry").forGetter(ReplaceEntry::entry))
                .apply(instance, ReplaceEntry::new));
        @Override
        public void apply(LootTable table, HolderLookup.Provider registries) {
            // 每处都解出一份独立实例：直接复用同一个对象的话，后续 ModifyEntry 改权重/条件会一起改到所有位置。
            // 必须用 RegistryOps——条目里可能有附魔这类数据包注册表引用，JsonOps 会直接报 "Can't access registry"。
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);

            for (int p = 0; p < table.pools.size(); p++) {
                LootPool pool = table.pools.get(p);
                if (!this.poolCondition.test(p, pool)) {
                    continue;
                }

                // 就地 set 而不是先删后加：保位置（下标会被后面的条件读到），也保证每个原条目只判一次
                List<LootPoolEntryContainer> entries = mutableEntries(pool);
                for (int i = 0; i < entries.size(); i++) {
                    if (this.entryCondition.test(i, entries.get(i))) {
                        entries.set(i, copy(ops));
                    }
                }
            }
        }

        /** 走一次 codec 往返拿新实例；encode 与 decode 用同一套 RegistryOps。 */
        private LootPoolEntryContainer copy(RegistryOps<JsonElement> ops) {
            JsonElement json = LootPoolEntries.CODEC.encodeStart(ops, this.entry).getOrThrow();
            return LootPoolEntries.CODEC.parse(ops, json).getOrThrow();
        }
    }
    /** 自定义操作：json 里只给一个 id，具体怎么改表交给监听 CustomInjectorOpEvent 的代码。 */
    record Custom(String id) implements LootInjectorOp {
        public static final MapCodec<Custom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("custom_id").forGetter(Custom::id)).apply(instance, Custom::new));

        @Override
        public LootInjectorOpType getType() {
            return LootInjectorOps.CUSTOM_OP.get();
        }

        @Override
        public void apply(LootTable table, HolderLookup.Provider registries) {
            CustomInjectorOpEvent.post(this.id, table);
        }
    }
}
