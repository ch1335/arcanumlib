package com.chen1335.arcanumlib.modules.loot.lootInjector.selector;

import com.mojang.serialization.MapCodec;

/** 函数选择器的注册表项；作用对象是"函数列表里的某个函数"。 */
public record FunctionSelectorType(MapCodec<? extends FunctionSelector> codec) {
}
