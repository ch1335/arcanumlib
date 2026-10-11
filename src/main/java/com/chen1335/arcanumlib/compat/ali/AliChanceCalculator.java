package com.chen1335.arcanumlib.compat.ali;

import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifier;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifierCondition;
import com.yanny.aci.api.NumberExpr;

/**
 * 把加工器链折成 ALI 的符号表达式（返回倍率，不是绝对值）。
 * 只折不依赖上下文的：apply / set_chance / clamp / invert；其余跳过 —— ALI 给的 LootContext 是空壳，
 * 拿它算会显示出"看起来精确、其实是默认值"的数。
 */
final class AliChanceCalculator {

    private AliChanceCalculator() {
    }

    static NumberExpr factor(ChanceModifierCondition condition) {
        NumberExpr chance = NumberExpr.constant(condition.baseChance());

        for (ChanceModifier modifier : condition.modifiers()) {
            chance = foldPure(chance, modifier);
        }

        return chance;
    }

    /** 只折不需要上下文的那几种；其余返回原表达式，等于这个加工器不参与显示的概率。 */
    private static NumberExpr foldPure(NumberExpr chance, ChanceModifier modifier) {
        return switch (modifier) {
            case ChanceModifier.SetChance it -> NumberExpr.constant(it.value());
            case ChanceModifier.Clamp it -> NumberExpr.clamp(chance,
                    NumberExpr.constant(it.min()), NumberExpr.constant(it.max()));
            case ChanceModifier.Invert ignored -> NumberExpr.sub(NumberExpr.constant(1.0D), chance);
            case ChanceModifier.Apply it -> it.operation() == ChanceModifier.Operation.ADD
                    ? NumberExpr.add(chance, NumberExpr.constant(it.value()))
                    : NumberExpr.mul(chance, NumberExpr.constant(it.value()));
            default -> chance;
        };
    }
}
