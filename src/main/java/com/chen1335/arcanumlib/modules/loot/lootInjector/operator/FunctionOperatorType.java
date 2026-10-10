package com.chen1335.arcanumlib.modules.loot.lootInjector.operator;

import com.mojang.serialization.MapCodec;

/** 函数级 op 的注册表项；作用对象是"当前函数"。 */
public record FunctionOperatorType(MapCodec<? extends FunctionOperator> codec) {
}
