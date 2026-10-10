package com.chen1335.arcanumlib.modules.loot.lootInjector.selector;

import com.mojang.serialization.MapCodec;

/** 条件选择器的注册表项；作用对象是"条件列表里的某个条件"。 */
public record ConditionSelectorType(MapCodec<? extends ConditionSelector> codec) {
}
