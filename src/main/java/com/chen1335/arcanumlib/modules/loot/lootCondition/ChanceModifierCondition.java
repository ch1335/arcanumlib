package com.chen1335.arcanumlib.modules.loot.lootCondition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.List;

/**
 * 从 baseChance 起算（缺省 1.0），依次过一遍所有加工器，再用最终概率做一次随机判定。
 * 基础概率放在条件自己的字段而不是链首：链负责加工、起点由字段给。概率不夹紧，>1 必过、≤0 必不过。
 */
public record ChanceModifierCondition(double baseChance, List<ChanceModifier> modifiers) implements LootItemCondition {

    public static final MapCodec<ChanceModifierCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.DOUBLE.optionalFieldOf("base_chance", 1.0D).forGetter(ChanceModifierCondition::baseChance),
            ChanceModifier.CODEC.listOf().optionalFieldOf("chance_modifiers", List.<ChanceModifier>of())
                    .forGetter(ChanceModifierCondition::modifiers)
    ).apply(instance, ChanceModifierCondition::new));

    public static LootItemCondition.Builder builder(double baseChance, ChanceModifier... modifiers) {
        return () -> new ChanceModifierCondition(baseChance, List.of(modifiers));
    }

    public static LootItemCondition.Builder builder(ChanceModifier... modifiers) {
        return builder(1.0D, modifiers);
    }

    @Override
    public LootItemConditionType getType() {
        return LootConditionTypes.CHANCE_MODIFIERS.get();
    }

    /** 把整条链求值成一个确定性的概率，不做随机判定。 */
    public double resolveChance(LootContext context) {
        double chance = this.baseChance;

        for (ChanceModifier modifier : this.modifiers) {
            chance = modifier.apply(chance, context);
        }

        return chance;
    }

    @Override
    public boolean test(LootContext context) {
        return context.getRandom().nextFloat() < this.resolveChance(context);
    }
}