package com.chen1335.arcanumlib.modules.loot.lootCondition;

import com.chen1335.arcanumlib.ArcanumLib;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;

/** 概率加工器的类型注册表；其他模组 DeferredRegister.create(REGISTRY_KEY, 自己的 modid) 就能加新种类。 */
public final class ChanceModifierTypes {
    public static final ResourceKey<Registry<ChanceModifierType>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ArcanumLib.id("chance_modifier"));

    // 只在服务端解析数据包时用得到，不需要同步给客户端
    public static final Registry<ChanceModifierType> REGISTRY = new RegistryBuilder<>(REGISTRY_KEY)
            .sync(false)
            .create();

    public static final DeferredRegister<ChanceModifierType> TYPES =
            DeferredRegister.create(REGISTRY_KEY, ArcanumLib.MODID);

    public static final DeferredHolder<ChanceModifierType, ChanceModifierType> ADD =
            TYPES.register("add", () -> new ChanceModifierType(ChanceModifier.Add.CODEC));
    public static final DeferredHolder<ChanceModifierType, ChanceModifierType> MULTIPLY =
            TYPES.register("multiply", () -> new ChanceModifierType(ChanceModifier.Multiply.CODEC));
    public static final DeferredHolder<ChanceModifierType, ChanceModifierType> SET_CHANCE =
            TYPES.register("set_chance", () -> new ChanceModifierType(ChanceModifier.SetChance.CODEC));
    public static final DeferredHolder<ChanceModifierType, ChanceModifierType> CLAMP =
            TYPES.register("clamp", () -> new ChanceModifierType(ChanceModifier.Clamp.CODEC));
    public static final DeferredHolder<ChanceModifierType, ChanceModifierType> INVERT =
            TYPES.register("invert", () -> new ChanceModifierType(ChanceModifier.Invert.CODEC));
    public static final DeferredHolder<ChanceModifierType, ChanceModifierType> DIFFICULTY_FACTOR =
            TYPES.register("difficulty_factor", () -> new ChanceModifierType(ChanceModifier.DifficultyFactor.CODEC));
    public static final DeferredHolder<ChanceModifierType, ChanceModifierType> TOOL_ENCHANTMENT_LEVEL =
            TYPES.register("tool_enchantment_level", () -> new ChanceModifierType(ChanceModifier.ToolEnchantmentLevel.CODEC));
    public static final DeferredHolder<ChanceModifierType, ChanceModifierType> ENTITY_ATTRIBUTE =
            TYPES.register("entity_attribute", () -> new ChanceModifierType(ChanceModifier.EntityAttribute.CODEC));

    private ChanceModifierTypes() {
    }
}
