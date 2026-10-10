package com.chen1335.arcanumlib.modules.loot.lootCondition;

import com.chen1335.arcanumlib.ArcanumLib;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 本模组的战利品条件类型。 */
public final class LootConditionTypes {

    public static final DeferredRegister<LootItemConditionType> CONDITION_TYPES =
            DeferredRegister.create(BuiltInRegistries.LOOT_CONDITION_TYPE, ArcanumLib.MODID);

    /** 概率加工链：从 1.0 起算，依次加工后做随机判定。 */
    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> CHANCE_MODIFIERS =
            CONDITION_TYPES.register("chance_modifiers", () -> new LootItemConditionType(ChanceModifierCondition.CODEC));

    private LootConditionTypes() {
    }
}
