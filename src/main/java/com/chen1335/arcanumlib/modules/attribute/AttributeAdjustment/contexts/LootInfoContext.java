package com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts;

import com.chen1335.arcanumlib.modules.attribute.events.LivingGetAttributeAdjustmentEvent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;

public record LootInfoContext(LootContext lootContext, LootContextParamSet lootContextParamSet) implements LivingGetAttributeAdjustmentEvent.IContext {
}
