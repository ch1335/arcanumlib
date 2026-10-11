package com.chen1335.arcanumlib.modules.loot.lootInjector.operator;

import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOps;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * 条件级操作：作用于"当前条件"，只能出现在 {@code modify_condition} 作用域的子 {@code ops} 里。
 * <p>改完（无论哪一层）都必须由所在作用域重算复合体，否则判定时改动不生效。
 */
public interface ConditionOperator {

    Codec<ConditionOperator> CODEC = LootInjectorOps.CONDITION_OPERATORS.byNameCodec()
            .dispatch("type", ConditionOperator::getType, ConditionOperatorType::codec);

    ConditionOperatorType getType();

    /** conditions 已经是可变列表，index 指向"当前条件"。 */
    void apply(List<LootItemCondition> conditions, int index, HolderLookup.Provider registries);

    /** 摘掉"当前条件"。 */
    record RemoveCondition() implements ConditionOperator {
        @Override
        public ConditionOperatorType getType() {
            return LootInjectorOps.CONDITION_OPERATOR_REMOVE.get();
        }
        public static final MapCodec<RemoveCondition> CODEC = MapCodec.unit(RemoveCondition::new);
        @Override
        public void apply(List<LootItemCondition> conditions, int index, HolderLookup.Provider registries) {
            conditions.remove(index);
        }
    }

    /** 把"当前条件"换成新条件；每处都重新解码一份独立实例。 */
    record ReplaceCondition(LootItemCondition condition) implements ConditionOperator {
        @Override
        public ConditionOperatorType getType() {
            return LootInjectorOps.CONDITION_OPERATOR_REPLACE.get();
        }
        public static final MapCodec<ReplaceCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootItemCondition.DIRECT_CODEC.fieldOf("condition").forGetter(ReplaceCondition::condition))
                .apply(instance, ReplaceCondition::new));
        @Override
        public void apply(List<LootItemCondition> conditions, int index, HolderLookup.Provider registries) {
            RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
            JsonElement json = LootItemCondition.DIRECT_CODEC.encodeStart(ops, this.condition).getOrThrow();

            conditions.set(index, LootItemCondition.DIRECT_CODEC.parse(ops, json).getOrThrow());
        }
    }
}
