package com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.contexts;

import com.chen1335.arcanumlib.modules.attribute.events.LivingGetAttributeAdjustmentEvent;
import net.minecraft.world.level.storage.loot.LootContext;

public record LootInfoContext(LootContext lootContext) implements LivingGetAttributeAdjustmentEvent.IContext {
}
