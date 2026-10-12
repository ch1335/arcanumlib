package com.chen1335.arcanumlib.modules.loot.lootInjector.operator;

import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOps;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.ConditionSelector;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数级操作：作用于"当前函数"，只能出现在 {@code modify_function} 作用域的子 {@code ops} 里。
 *
 * <p>叶子暂时只有"删"和"换"。通用的"改参数"做不出来——各函数的参数字段都是各自私有的，
 * 那种需求交给 {@code *_custom} 按类型自己处理。
 */
public interface FunctionOperator {

    Codec<FunctionOperator> CODEC = LootInjectorOps.FUNCTION_OPERATORS.byNameCodec()
            .dispatch("type", FunctionOperator::getType, FunctionOperatorType::codec);

    FunctionOperatorType getType();

    /** functions 已经是可变列表，index 指向"当前函数"。 */
    void apply(List<LootItemFunction> functions, int index, HolderLookup.Provider registries);

    /** 摘掉"当前函数"。 */
    record RemoveFunction() implements FunctionOperator {
        @Override
        public FunctionOperatorType getType() {
            return LootInjectorOps.FUNCTION_OPERATOR_REMOVE.get();
        }
        public static final MapCodec<RemoveFunction> CODEC = MapCodec.unit(RemoveFunction::new);
        @Override
        public void apply(List<LootItemFunction> functions, int index, HolderLookup.Provider registries) {
            functions.remove(index);
        }
    }

    /** 把"当前函数"换成新函数；每处都重新解码一份独立实例。 */
    record ReplaceFunction(LootItemFunction function) implements FunctionOperator {
        @Override
        public FunctionOperatorType getType() {
            return LootInjectorOps.FUNCTION_OPERATOR_REPLACE.get();
        }
        public static final MapCodec<ReplaceFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootItemFunctions.ROOT_CODEC.fieldOf("function").forGetter(ReplaceFunction::function))
                .apply(instance, ReplaceFunction::new));
        @Override
        public void apply(List<LootItemFunction> functions, int index, HolderLookup.Provider registries) {
            // 函数里可能有附魔这类数据包注册表引用，必须用 RegistryOps
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
            JsonElement json = LootItemFunctions.ROOT_CODEC.encodeStart(ops, this.function).getOrThrow();

            functions.set(index, LootItemFunctions.ROOT_CODEC.parse(ops, json).getOrThrow());
        }
    }

    /**
     * 作用域：钻进"当前函数"自带的条件里，对每个命中的条件应用子 ops。
     * 非条件型函数（例如 set_count）没有条件可改，直接跳过。
     */
    record ModifyCondition(ConditionSelector conditionSelector,
                           List<ConditionOperator> ops) implements FunctionOperator {
        @Override
        public FunctionOperatorType getType() {
            return LootInjectorOps.FUNCTION_OPERATOR_MODIFY_CONDITION.get();
        }
        public static final MapCodec<ModifyCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ConditionSelector.CODEC.optionalFieldOf("conditionSelector", new ConditionSelector.Any())
                        .forGetter(ModifyCondition::conditionSelector),
                ConditionOperator.CODEC.listOf().optionalFieldOf("ops", List.of()).forGetter(ModifyCondition::ops))
                .apply(instance, ModifyCondition::new));
        @Override
        public void apply(List<LootItemFunction> functions, int index, HolderLookup.Provider registries) {
            if (!(functions.get(index) instanceof LootItemConditionalFunction function)) {
                return;
            }

            // 条件列表可能来自不可变列表，增删前先换一份可变的
            if (!(function.predicates instanceof ArrayList)) {
                function.predicates = new ArrayList<>(function.predicates);
            }
            List<LootItemCondition> conditions = function.predicates;

            // 快照遍历：子 op 可能删条件，直接按下标走会跳项
            for (LootItemCondition condition : List.copyOf(conditions)) {
                int at = conditions.indexOf(condition);
                if (at < 0 || !this.conditionSelector.test(at, condition, registries)) {
                    continue;
                }

                for (ConditionOperator op : this.ops) {
                    int current = conditions.indexOf(condition);
                    if (current < 0) {
                        break;                                  // 当前条件已被前面的子 op 删掉
                    }
                    op.apply(conditions, current, registries);
                }
            }

            function.compositePredicates = Util.allOf(conditions);   // 结算真正走的是它
        }
    }
}
