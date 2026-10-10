package com.chen1335.arcanumlib.modules.loot.lootInjector.operator;

import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOps;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.EntryCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.PoolCustomOpEvent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
 * 池级操作：作用于"当前池"，只能出现在 {@link TableOperator.ModifyPool} 的子 {@code ops} 里。
 *
 * <p>作用域 op（{@link ModifyEntry}）把子 ops 依次应用到每个命中的条目上；当前条目若被前面的子 op
 * 删掉，剩下的子 op 立刻停止（游标语义）。
 */
public interface PoolOperator {

    Codec<PoolOperator> CODEC = LootInjectorOps.POOL_OPERATORS.byNameCodec()
            .dispatch("type", PoolOperator::getType, PoolOperatorType::codec);

    PoolOperatorType getType();

    void apply(LootTable table, int poolIndex, LootPool pool, HolderLookup.Provider registries);

    /** 池里的 entries 可能来自不可变列表，增删前先换一份可变的。 */
    static List<LootPoolEntryContainer> mutableEntries(LootPool pool) {
        if (!(pool.entries instanceof ArrayList)) {
            pool.entries = new ArrayList<>(pool.entries);
        }

        return pool.entries;
    }

    /** 摘掉"当前池"。 */
    record RemovePool() implements PoolOperator {
        @Override
        public PoolOperatorType getType() {
            return LootInjectorOps.POOL_REMOVE_POOL.get();
        }
        public static final MapCodec<RemovePool> CODEC = MapCodec.unit(RemovePool::new);
        @Override
        public void apply(LootTable table, int poolIndex, LootPool pool, HolderLookup.Provider registries) {
            table.pools.remove(poolIndex);
        }
    }

    /** 往"当前池"追加一个条目。 */
    record AddEntry(LootPoolEntryContainer entry) implements PoolOperator {
        @Override
        public PoolOperatorType getType() {
            return LootInjectorOps.POOL_ADD_ENTRY.get();
        }
        public static final MapCodec<AddEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootPoolEntries.CODEC.fieldOf("entry").forGetter(AddEntry::entry)).apply(instance, AddEntry::new));
        @Override
        public void apply(LootTable table, int poolIndex, LootPool pool, HolderLookup.Provider registries) {
            mutableEntries(pool).add(this.entry);
        }
    }

    /** 作用域：命中的每个条目依次跑一遍子 ops。 */
    record ModifyEntry(EntryCondition entryCondition, List<EntryOperator> ops) implements PoolOperator {
        @Override
        public PoolOperatorType getType() {
            return LootInjectorOps.POOL_MODIFY_ENTRY.get();
        }
        public static final MapCodec<ModifyEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                EntryCondition.CODEC.optionalFieldOf("entryCondition", new EntryCondition.Any())
                        .forGetter(ModifyEntry::entryCondition),
                EntryOperator.CODEC.listOf().optionalFieldOf("ops", List.of()).forGetter(ModifyEntry::ops))
                .apply(instance, ModifyEntry::new));
        @Override
        public void apply(LootTable table, int poolIndex, LootPool pool, HolderLookup.Provider registries) {
            List<LootPoolEntryContainer> entries = mutableEntries(pool);

            // 快照遍历：子 op 可能删条目，直接按下标走会跳项
            for (LootPoolEntryContainer entry : List.copyOf(entries)) {
                int index = entries.indexOf(entry);
                if (index < 0 || !this.entryCondition.test(index, entry, pool)) {
                    continue;
                }

                for (EntryOperator op : this.ops) {
                    int current = entries.indexOf(entry);
                    if (current < 0) {
                        break;                                  // 当前条目已被前面的子 op 删掉
                    }
                    op.apply(pool, entries, current, registries);
                }
            }
        }
    }

    /** 直接替换池上的条件列表（不是追加）。 */
    record SetCondition(List<LootItemCondition> conditions) implements PoolOperator {
        @Override
        public PoolOperatorType getType() {
            return LootInjectorOps.POOL_SET_CONDITION.get();
        }
        public static final MapCodec<SetCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootItemCondition.DIRECT_CODEC.listOf().fieldOf("conditions").forGetter(SetCondition::conditions))
                .apply(instance, SetCondition::new));
        @Override
        public void apply(LootTable table, int poolIndex, LootPool pool, HolderLookup.Provider registries) {
            pool.conditions = List.copyOf(this.conditions);
            pool.compositeCondition = Util.allOf(pool.conditions);      // 判定真正走的是它
        }
    }

    /** 往池上的条件列表追加。 */
    record AddCondition(List<LootItemCondition> conditions) implements PoolOperator {
        @Override
        public PoolOperatorType getType() {
            return LootInjectorOps.POOL_ADD_CONDITION.get();
        }
        public static final MapCodec<AddCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootItemCondition.DIRECT_CODEC.listOf().fieldOf("conditions").forGetter(AddCondition::conditions))
                .apply(instance, AddCondition::new));
        @Override
        public void apply(LootTable table, int poolIndex, LootPool pool, HolderLookup.Provider registries) {
            if (this.conditions.isEmpty()) {
                return;
            }

            List<LootItemCondition> merged = new ArrayList<>(pool.conditions);
            merged.addAll(this.conditions);

            pool.conditions = merged;
            pool.compositeCondition = Util.allOf(merged);
        }
    }

    /** 按条件类型 id 摘掉池上的条件。 */
    record RemoveCondition(List<ResourceLocation> conditionTypes) implements PoolOperator {
        @Override
        public PoolOperatorType getType() {
            return LootInjectorOps.POOL_REMOVE_CONDITION.get();
        }
        public static final MapCodec<RemoveCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.listOf().fieldOf("conditionTypes")
                        .forGetter(RemoveCondition::conditionTypes)).apply(instance, RemoveCondition::new));
        @Override
        public void apply(LootTable table, int poolIndex, LootPool pool, HolderLookup.Provider registries) {
            List<LootItemCondition> kept = pool.conditions.stream()
                    .filter(condition -> !this.conditionTypes.contains(
                            BuiltInRegistries.LOOT_CONDITION_TYPE.getKey(condition.getType())))
                    .toList();
            if (kept.size() == pool.conditions.size()) {
                return;                                     // 一个都没摘掉就别动
            }

            pool.conditions = kept;
            pool.compositeCondition = Util.allOf(kept);
        }
    }

    /** 直接替换池上的函数列表（不是追加）。 */
    record SetFunction(List<LootItemFunction> functions) implements PoolOperator {
        @Override
        public PoolOperatorType getType() {
            return LootInjectorOps.POOL_SET_FUNCTION.get();
        }
        public static final MapCodec<SetFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootItemFunctions.ROOT_CODEC.listOf().fieldOf("functions").forGetter(SetFunction::functions))
                .apply(instance, SetFunction::new));
        @Override
        public void apply(LootTable table, int poolIndex, LootPool pool, HolderLookup.Provider registries) {
            pool.functions = List.copyOf(this.functions);
            pool.compositeFunction = LootItemFunctions.compose(pool.functions);
        }
    }

    /** 往池上的函数列表追加。 */
    record AddFunction(List<LootItemFunction> functions) implements PoolOperator {
        @Override
        public PoolOperatorType getType() {
            return LootInjectorOps.POOL_ADD_FUNCTION.get();
        }
        public static final MapCodec<AddFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootItemFunctions.ROOT_CODEC.listOf().fieldOf("functions").forGetter(AddFunction::functions))
                .apply(instance, AddFunction::new));
        @Override
        public void apply(LootTable table, int poolIndex, LootPool pool, HolderLookup.Provider registries) {
            if (this.functions.isEmpty()) {
                return;
            }

            List<LootItemFunction> merged = new ArrayList<>(pool.functions);
            merged.addAll(this.functions);

            pool.functions = merged;
            pool.compositeFunction = LootItemFunctions.compose(merged);
        }
    }

    /** 按函数类型 id 摘掉池上的函数。 */
    record RemoveFunction(List<ResourceLocation> functionTypes) implements PoolOperator {
        @Override
        public PoolOperatorType getType() {
            return LootInjectorOps.POOL_REMOVE_FUNCTION.get();
        }
        public static final MapCodec<RemoveFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.listOf().fieldOf("functionTypes").forGetter(RemoveFunction::functionTypes))
                .apply(instance, RemoveFunction::new));
        @Override
        public void apply(LootTable table, int poolIndex, LootPool pool, HolderLookup.Provider registries) {
            List<LootItemFunction> kept = pool.functions.stream()
                    .filter(function -> !this.functionTypes.contains(
                            BuiltInRegistries.LOOT_FUNCTION_TYPE.getKey(function.getType())))
                    .toList();
            if (kept.size() == pool.functions.size()) {
                return;
            }

            pool.functions = kept;
            pool.compositeFunction = LootItemFunctions.compose(kept);
        }
    }

    /** 自定义：json 里只给一个 id，改动交给 PoolCustomOpEvent 的监听者。 */
    record PoolCustom(String id) implements PoolOperator {
        @Override
        public PoolOperatorType getType() {
            return LootInjectorOps.POOL_CUSTOM.get();
        }
        public static final MapCodec<PoolCustom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("custom_id").forGetter(PoolCustom::id)).apply(instance, PoolCustom::new));
        @Override
        public void apply(LootTable table, int poolIndex, LootPool pool, HolderLookup.Provider registries) {
            PoolCustomOpEvent.post(this.id, table, poolIndex, pool);
        }
    }
}
