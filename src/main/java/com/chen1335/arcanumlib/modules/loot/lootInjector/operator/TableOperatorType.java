package com.chen1335.arcanumlib.modules.loot.lootInjector.operator;

import com.mojang.serialization.MapCodec;

/** 和 LootPoolEntryType 同款：把 MapCodec 包成注册表项，方便别的模组加自己的表级 op。 */
public record TableOperatorType(MapCodec<? extends TableOperator> codec) {
}
