package com.chen1335.arcanumlib.modules.loot.lootInjector.operator;

import com.mojang.serialization.MapCodec;

/** 池级 op 的注册表项；作用对象是"当前池"。 */
public record PoolOperatorType(MapCodec<? extends PoolOperator> codec) {
}
