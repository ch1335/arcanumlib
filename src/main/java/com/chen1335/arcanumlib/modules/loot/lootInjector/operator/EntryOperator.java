package com.chen1335.arcanumlib.modules.loot.lootInjector.operator;

import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOps;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.EntryCustomOpEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.ConditionSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.FunctionSelector;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.ArrayList;
import java.util.List;

/**
 * 条目级操作：作用于"当前条目"，只能出现在 {@link PoolOperator.ModifyEntry} 的子 {@code ops} 里。
 * 条件只在作用域上写，叶子 op 不再自带 poolSelector / entrySelector。
 */
public interface EntryOperator {

    Codec<EntryOperator> CODEC = LootInjectorOps.ENTRY_OPERATORS.byNameCodec()
            .dispatch("type", EntryOperator::getType, EntryOperatorType::codec);

    EntryOperatorType getType();

    /** entries 已经是可变列表，index 指向"当前条目"。 */
    void apply(LootPool pool, List<LootPoolEntryContainer> entries, int index, HolderLookup.Provider registries);

    /** 摘掉"当前条目"。 */
    record RemoveEntry() implements EntryOperator {
        @Override
        public EntryOperatorType getType() {
            return LootInjectorOps.ENTRY_REMOVE_ENTRY.get();
        }
        public static final MapCodec<RemoveEntry> CODEC = MapCodec.unit(RemoveEntry::new);
        @Override
        public void apply(LootPool pool, List<LootPoolEntryContainer> entries, int index,
                          HolderLookup.Provider registries) {
            entries.remove(index);
        }
    }

    /**
     * 把"当前条目"换成新条目。每处都重新解码一份独立实例：直接复用同一个对象的话，
     * 后续改动会一起改到所有位置。
     */
    record ReplaceEntry(LootPoolEntryContainer entry) implements EntryOperator {
        @Override
        public EntryOperatorType getType() {
            return LootInjectorOps.ENTRY_REPLACE_ENTRY.get();
        }
        public static final MapCodec<ReplaceEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootPoolEntries.CODEC.fieldOf("entry").forGetter(ReplaceEntry::entry))
                .apply(instance, ReplaceEntry::new));
        @Override
        public void apply(LootPool pool, List<LootPoolEntryContainer> entries, int index,
                          HolderLookup.Provider registries) {
            // 条目里可能有附魔这类数据包注册表引用，必须用 RegistryOps
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
            JsonElement json = LootPoolEntries.CODEC.encodeStart(ops, this.entry).getOrThrow();

            entries.set(index, LootPoolEntries.CODEC.parse(ops, json).getOrThrow());
        }
    }

    /** 设置"当前条目"的权重；负数视为不改（沿用旧 modify_entry 的语义）。复合条目没有权重，跳过。 */
    record SetWeight(int weight) implements EntryOperator {
        @Override
        public EntryOperatorType getType() {
            return LootInjectorOps.ENTRY_SET_WEIGHT.get();
        }
        public static final MapCodec<SetWeight> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("weight").forGetter(SetWeight::weight)).apply(instance, SetWeight::new));
        @Override
        public void apply(LootPool pool, List<LootPoolEntryContainer> entries, int index,
                          HolderLookup.Provider registries) {
            if (this.weight >= 0 && entries.get(index) instanceof LootPoolSingletonContainer singleton) {
                singleton.weight = this.weight;
            }
        }
    }

    /** 设置"当前条目"的品质；负数视为不改。复合条目没有品质，跳过。 */
    record SetQuality(int quality) implements EntryOperator {
        @Override
        public EntryOperatorType getType() {
            return LootInjectorOps.ENTRY_SET_QUALITY.get();
        }
        public static final MapCodec<SetQuality> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("quality").forGetter(SetQuality::quality)).apply(instance, SetQuality::new));
        @Override
        public void apply(LootPool pool, List<LootPoolEntryContainer> entries, int index,
                          HolderLookup.Provider registries) {
            if (this.quality >= 0 && entries.get(index) instanceof LootPoolSingletonContainer singleton) {
                singleton.quality = this.quality;
            }
        }
    }

    /** 直接替换"当前条目"的条件列表（不是追加）。 */
    record SetCondition(List<LootItemCondition> conditions) implements EntryOperator {
        @Override
        public EntryOperatorType getType() {
            return LootInjectorOps.ENTRY_SET_CONDITION.get();
        }
        public static final MapCodec<SetCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootItemCondition.DIRECT_CODEC.listOf().fieldOf("conditions").forGetter(SetCondition::conditions))
                .apply(instance, SetCondition::new));
        @Override
        public void apply(LootPool pool, List<LootPoolEntryContainer> entries, int index,
                          HolderLookup.Provider registries) {
            LootPoolEntryContainer entry = entries.get(index);
            entry.conditions = List.copyOf(this.conditions);
            entry.compositeCondition = Util.allOf(entry.conditions);    // 判定真正走的是它
        }
    }

    /** 往"当前条目"的条件列表追加。 */
    record AddCondition(List<LootItemCondition> conditions) implements EntryOperator {
        @Override
        public EntryOperatorType getType() {
            return LootInjectorOps.ENTRY_ADD_CONDITION.get();
        }
        public static final MapCodec<AddCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootItemCondition.DIRECT_CODEC.listOf().fieldOf("conditions").forGetter(AddCondition::conditions))
                .apply(instance, AddCondition::new));
        @Override
        public void apply(LootPool pool, List<LootPoolEntryContainer> entries, int index,
                          HolderLookup.Provider registries) {
            if (this.conditions.isEmpty()) {
                return;
            }

            LootPoolEntryContainer entry = entries.get(index);
            List<LootItemCondition> merged = new ArrayList<>(entry.conditions);
            merged.addAll(this.conditions);

            entry.conditions = merged;
            entry.compositeCondition = Util.allOf(merged);
        }
    }

    /** 按条件类型 id 摘掉"当前条目"身上的条件。 */
    record RemoveCondition(List<ResourceLocation> conditionTypes) implements EntryOperator {
        @Override
        public EntryOperatorType getType() {
            return LootInjectorOps.ENTRY_REMOVE_CONDITION.get();
        }
        public static final MapCodec<RemoveCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.listOf().fieldOf("conditionTypes")
                        .forGetter(RemoveCondition::conditionTypes)).apply(instance, RemoveCondition::new));
        @Override
        public void apply(LootPool pool, List<LootPoolEntryContainer> entries, int index,
                          HolderLookup.Provider registries) {
            LootPoolEntryContainer entry = entries.get(index);
            List<LootItemCondition> kept = entry.conditions.stream()
                    .filter(condition -> !this.conditionTypes.contains(
                            BuiltInRegistries.LOOT_CONDITION_TYPE.getKey(condition.getType())))
                    .toList();
            if (kept.size() == entry.conditions.size()) {
                return;                                     // 一个都没摘掉就别动
            }

            entry.conditions = kept;
            entry.compositeCondition = Util.allOf(kept);
        }
    }

    /** 直接替换"当前条目"的函数列表（不是追加）；复合条目没有函数，跳过。 */
    record SetFunction(List<LootItemFunction> functions) implements EntryOperator {
        @Override
        public EntryOperatorType getType() {
            return LootInjectorOps.ENTRY_SET_FUNCTION.get();
        }
        public static final MapCodec<SetFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootItemFunctions.ROOT_CODEC.listOf().fieldOf("functions").forGetter(SetFunction::functions))
                .apply(instance, SetFunction::new));
        @Override
        public void apply(LootPool pool, List<LootPoolEntryContainer> entries, int index,
                          HolderLookup.Provider registries) {
            if (!(entries.get(index) instanceof LootPoolSingletonContainer singleton)) {
                return;
            }

            singleton.functions = List.copyOf(this.functions);
            singleton.compositeFunction = LootItemFunctions.compose(singleton.functions);   // 结算真正走的是它
        }
    }

    /** 往"当前条目"的函数列表追加；复合条目没有函数，跳过。 */
    record AddFunction(List<LootItemFunction> functions) implements EntryOperator {
        @Override
        public EntryOperatorType getType() {
            return LootInjectorOps.ENTRY_ADD_FUNCTION.get();
        }
        public static final MapCodec<AddFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootItemFunctions.ROOT_CODEC.listOf().fieldOf("functions").forGetter(AddFunction::functions))
                .apply(instance, AddFunction::new));
        @Override
        public void apply(LootPool pool, List<LootPoolEntryContainer> entries, int index,
                          HolderLookup.Provider registries) {
            if (this.functions.isEmpty() || !(entries.get(index) instanceof LootPoolSingletonContainer singleton)) {
                return;
            }

            List<LootItemFunction> merged = new ArrayList<>(singleton.functions);
            merged.addAll(this.functions);

            singleton.functions = merged;
            singleton.compositeFunction = LootItemFunctions.compose(merged);
        }
    }

    /** 按函数类型 id 摘掉"当前条目"身上的函数；复合条目没有函数，跳过。 */
    record RemoveFunction(List<ResourceLocation> functionTypes) implements EntryOperator {
        @Override
        public EntryOperatorType getType() {
            return LootInjectorOps.ENTRY_REMOVE_FUNCTION.get();
        }
        public static final MapCodec<RemoveFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.listOf().fieldOf("functionTypes").forGetter(RemoveFunction::functionTypes))
                .apply(instance, RemoveFunction::new));
        @Override
        public void apply(LootPool pool, List<LootPoolEntryContainer> entries, int index,
                          HolderLookup.Provider registries) {
            if (!(entries.get(index) instanceof LootPoolSingletonContainer singleton)) {
                return;
            }

            List<LootItemFunction> kept = singleton.functions.stream()
                    .filter(function -> !this.functionTypes.contains(
                            BuiltInRegistries.LOOT_FUNCTION_TYPE.getKey(function.getType())))
                    .toList();
            if (kept.size() == singleton.functions.size()) {
                return;
            }

            singleton.functions = kept;
            singleton.compositeFunction = LootItemFunctions.compose(kept);
        }
    }

    /** 作用域：对"当前条目"函数列表里命中的每个函数跑子 ops；复合条目没有函数，跳过。 */
    record ModifyFunction(FunctionSelector functionSelector,
                          List<FunctionOperator> ops) implements EntryOperator {
        @Override
        public EntryOperatorType getType() {
            return LootInjectorOps.ENTRY_MODIFY_FUNCTION.get();
        }
        public static final MapCodec<ModifyFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FunctionSelector.CODEC.optionalFieldOf("functionSelector", new FunctionSelector.Any())
                        .forGetter(ModifyFunction::functionSelector),
                FunctionOperator.CODEC.listOf().optionalFieldOf("ops", List.of()).forGetter(ModifyFunction::ops))
                .apply(instance, ModifyFunction::new));
        @Override
        public void apply(LootPool pool, List<LootPoolEntryContainer> entries, int index,
                          HolderLookup.Provider registries) {
            if (!(entries.get(index) instanceof LootPoolSingletonContainer singleton)) {
                return;
            }

            if (!(singleton.functions instanceof ArrayList)) {
                singleton.functions = new ArrayList<>(singleton.functions);
            }
            List<LootItemFunction> functions = singleton.functions;

            for (LootItemFunction function : List.copyOf(functions)) {
                int at = functions.indexOf(function);
                if (at < 0 || !this.functionSelector.test(at, function, registries)) {
                    continue;
                }

                for (FunctionOperator op : this.ops) {
                    int current = functions.indexOf(function);
                    if (current < 0) {
                        break;
                    }
                    op.apply(functions, current, registries);
                }
            }

            singleton.compositeFunction = LootItemFunctions.compose(functions);
        }
    }

    /** 作用域：对"当前条目"条件里命中的每个条件跑子 ops。 */
    record ModifyCondition(ConditionSelector conditionSelector,
                           List<ConditionOperator> ops) implements EntryOperator {
        @Override
        public EntryOperatorType getType() {
            return LootInjectorOps.ENTRY_MODIFY_CONDITION.get();
        }
        public static final MapCodec<ModifyCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ConditionSelector.CODEC.optionalFieldOf("conditionSelector", new ConditionSelector.Any())
                        .forGetter(ModifyCondition::conditionSelector),
                ConditionOperator.CODEC.listOf().optionalFieldOf("ops", List.of()).forGetter(ModifyCondition::ops))
                .apply(instance, ModifyCondition::new));
        @Override
        public void apply(LootPool pool, List<LootPoolEntryContainer> entries, int index,
                          HolderLookup.Provider registries) {
            LootPoolEntryContainer entry = entries.get(index);

            if (!(entry.conditions instanceof ArrayList)) {
                entry.conditions = new ArrayList<>(entry.conditions);
            }
            List<LootItemCondition> conditions = entry.conditions;

            for (LootItemCondition condition : List.copyOf(conditions)) {
                int at = conditions.indexOf(condition);
                if (at < 0 || !this.conditionSelector.test(at, condition, registries)) {
                    continue;
                }

                for (ConditionOperator op : this.ops) {
                    int current = conditions.indexOf(condition);
                    if (current < 0) {
                        break;
                    }
                    op.apply(conditions, current, registries);
                }
            }

            entry.compositeCondition = Util.allOf(conditions);
        }
    }
    /** 自定义：json 里只给一个 id，改动交给 EntryCustomOpEvent 的监听者。 */
    record EntryCustom(String id) implements EntryOperator {
        @Override
        public EntryOperatorType getType() {
            return LootInjectorOps.ENTRY_CUSTOM.get();
        }
        public static final MapCodec<EntryCustom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("custom_id").forGetter(EntryCustom::id)).apply(instance, EntryCustom::new));
        @Override
        public void apply(LootPool pool, List<LootPoolEntryContainer> entries, int index,
                          HolderLookup.Provider registries) {
            EntryCustomOpEvent.post(this.id, pool, entries, index, registries);
        }
    }
}
