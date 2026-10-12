package com.chen1335.arcanumlib.modules.loot.lootInjector.operator;

import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOps;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.PoolSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.TableCustomOpEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.FunctionSelector;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import java.util.Optional;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;

import java.util.ArrayList;
import java.util.List;

/**
 * 表级操作：作用于整张表，注入器 json 顶层的 {@code ops} 就是这一级。
 *
 * <p>作用域 op（{@link ModifyPool}）把子 ops 依次应用到每个命中的池上；当前池若被前面的子 op
 * 删掉，剩下的子 op 立刻停止（游标语义）。
 */
public interface TableOperator {

    Codec<TableOperator> CODEC = LootInjectorOps.TABLE_OPERATORS.byNameCodec()
            .dispatch("type", TableOperator::getType, TableOperatorType::codec);

    TableOperatorType getType();

    void apply(LootTable table, HolderLookup.Provider registries);

    /** 追加一个池。 */
    record AddPool(LootPool pool) implements TableOperator {
        @Override
        public TableOperatorType getType() {
            return LootInjectorOps.TABLE_ADD_POOL.get();
        }
        public static final MapCodec<AddPool> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootPool.CODEC.fieldOf("pool").forGetter(AddPool::pool)).apply(instance, AddPool::new));
        @Override
        public void apply(LootTable table, HolderLookup.Provider registries) {
            table.pools.add(this.pool);
        }
    }

    /** 作用域：命中的每个池依次应用一遍子 ops。 */
    record ModifyPool(PoolSelector poolSelector, List<PoolOperator> ops) implements TableOperator {
        @Override
        public TableOperatorType getType() {
            return LootInjectorOps.TABLE_MODIFY_POOL.get();
        }
        public static final MapCodec<ModifyPool> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PoolSelector.CODEC.optionalFieldOf("poolSelector", new PoolSelector.Any())
                        .forGetter(ModifyPool::poolSelector),
                PoolOperator.CODEC.listOf().optionalFieldOf("ops", List.of()).forGetter(ModifyPool::ops))
                .apply(instance, ModifyPool::new));
        @Override
        public void apply(LootTable table, HolderLookup.Provider registries) {
            // 快照遍历：子 op 可能删池，直接按下标走会跳项
            for (LootPool pool : List.copyOf(table.pools)) {
                int index = table.pools.indexOf(pool);
                if (index < 0 || !this.poolSelector.test(index, pool, registries)) {
                    continue;
                }

                for (PoolOperator op : this.ops) {
                    int current = table.pools.indexOf(pool);
                    if (current < 0) {
                        break;                                  // 当前池已被前面的子 op 删掉
                    }
                    op.apply(table, current, pool, registries);
                }
            }
        }
    }

    /** 直接替换表上的函数列表（不是追加）。 */
    record SetFunction(List<LootItemFunction> functions) implements TableOperator {
        @Override
        public TableOperatorType getType() {
            return LootInjectorOps.TABLE_SET_FUNCTION.get();
        }
        public static final MapCodec<SetFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootItemFunctions.ROOT_CODEC.listOf().fieldOf("functions").forGetter(SetFunction::functions))
                .apply(instance, SetFunction::new));
        @Override
        public void apply(LootTable table, HolderLookup.Provider registries) {
            table.functions = List.copyOf(this.functions);
            table.compositeFunction = LootItemFunctions.compose(table.functions);   // 掷骰真正走的是它
        }
    }

    /** 往表上的函数列表追加。 */
    record AddFunction(List<LootItemFunction> functions) implements TableOperator {
        @Override
        public TableOperatorType getType() {
            return LootInjectorOps.TABLE_ADD_FUNCTION.get();
        }
        public static final MapCodec<AddFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootItemFunctions.ROOT_CODEC.listOf().fieldOf("functions").forGetter(AddFunction::functions))
                .apply(instance, AddFunction::new));
        @Override
        public void apply(LootTable table, HolderLookup.Provider registries) {
            if (this.functions.isEmpty()) {
                return;
            }

            List<LootItemFunction> merged = new ArrayList<>(table.functions);
            merged.addAll(this.functions);

            table.functions = merged;
            table.compositeFunction = LootItemFunctions.compose(merged);
        }
    }

    /** 按函数类型 id 摘掉表上的函数。 */
    record RemoveFunction(List<ResourceLocation> functionTypes) implements TableOperator {
        @Override
        public TableOperatorType getType() {
            return LootInjectorOps.TABLE_REMOVE_FUNCTION.get();
        }
        public static final MapCodec<RemoveFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.listOf().fieldOf("functionTypes").forGetter(RemoveFunction::functionTypes))
                .apply(instance, RemoveFunction::new));
        @Override
        public void apply(LootTable table, HolderLookup.Provider registries) {
            List<LootItemFunction> kept = table.functions.stream()
                    .filter(function -> !this.functionTypes.contains(
                            BuiltInRegistries.LOOT_FUNCTION_TYPE.getKey(function.getType())))
                    .toList();
            if (kept.size() == table.functions.size()) {
                return;                                     // 一个都没摘掉就别动
            }

            table.functions = kept;
            table.compositeFunction = LootItemFunctions.compose(kept);
        }
    }

    /** 作用域：对表函数列表里命中的每个函数应用子 ops，可以再往里钻到函数自带的条件。 */
    record ModifyFunction(FunctionSelector functionSelector, List<FunctionOperator> ops) implements TableOperator {
        @Override
        public TableOperatorType getType() {
            return LootInjectorOps.TABLE_MODIFY_FUNCTION.get();
        }
        public static final MapCodec<ModifyFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FunctionSelector.CODEC.optionalFieldOf("functionSelector", new FunctionSelector.Any())
                        .forGetter(ModifyFunction::functionSelector),
                FunctionOperator.CODEC.listOf().optionalFieldOf("ops", List.of()).forGetter(ModifyFunction::ops))
                .apply(instance, ModifyFunction::new));
        @Override
        public void apply(LootTable table, HolderLookup.Provider registries) {
            // 函数列表可能来自不可变列表，增删前先换一份可变的
            if (!(table.functions instanceof ArrayList)) {
                table.functions = new ArrayList<>(table.functions);
            }
            List<LootItemFunction> functions = table.functions;

            // 快照遍历：子 op 可能删函数，直接按下标走会跳项
            for (LootItemFunction function : List.copyOf(functions)) {
                int index = functions.indexOf(function);
                if (index < 0 || !this.functionSelector.test(index, function, registries)) {
                    continue;
                }

                for (FunctionOperator op : this.ops) {
                    int current = functions.indexOf(function);
                    if (current < 0) {
                        break;                                  // 当前函数已被前面的子 op 删掉
                    }
                    op.apply(functions, current, registries);
                }
            }

            table.compositeFunction = LootItemFunctions.compose(functions);   // 掷骰真正走的是它
        }
    }
    /** 自定义：json 里只给一个 id，改动交给 TableCustomOpEvent 的监听者。 */
    record TableCustom(String id, Optional<CompoundTag> data) implements TableOperator {
        /** 只给 id 的简写：不带附加数据。 */
        public TableCustom(String id) {
            this(id, Optional.empty());
        }
        @Override
        public TableOperatorType getType() {
            return LootInjectorOps.TABLE_CUSTOM.get();
        }
        public static final MapCodec<TableCustom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("custom_id").forGetter(TableCustom::id),
                LootInjectorOps.CUSTOM_DATA.forGetter(TableCustom::data)).apply(instance, TableCustom::new));
        @Override
        public void apply(LootTable table, HolderLookup.Provider registries) {
            TableCustomOpEvent.post(this.id, this.data.orElseGet(CompoundTag::new), table, registries);
        }
    }
}
