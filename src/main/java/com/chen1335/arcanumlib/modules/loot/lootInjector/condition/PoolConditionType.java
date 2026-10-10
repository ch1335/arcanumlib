package com.chen1335.arcanumlib.modules.loot.lootInjector.condition;

import com.mojang.serialization.MapCodec;

/** 和 LootPoolEntryType 同款：把 MapCodec 包成注册表项，别的模组可以加自己的池选择器。 */
public record PoolConditionType(MapCodec<? extends PoolCondition> codec) {
}
