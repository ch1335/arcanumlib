package com.chen1335.arcanumlib.modules.loot.lootInjector.operator;

import com.mojang.serialization.MapCodec;

/** 条件级 op 的注册表项；作用对象是"当前条件"。 */
public record ConditionOperatorType(MapCodec<? extends ConditionOperator> codec) {
}
