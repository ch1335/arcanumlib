package com.chen1335.arcanumlib.modules.loot.lootCondition;

import com.mojang.serialization.MapCodec;

/** 和 LootItemConditionType 同款：把 MapCodec 包成注册表项，别的模组可以加自己的加工器。 */
public record ChanceModifierType(MapCodec<? extends ChanceModifier> codec) {
}
