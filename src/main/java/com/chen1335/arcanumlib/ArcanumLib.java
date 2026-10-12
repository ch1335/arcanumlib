package com.chen1335.arcanumlib;

import com.chen1335.arcanumlib.modules.attribute.ArcanumAttributes;
import com.chen1335.arcanumlib.modules.attribute.AttributeConfig;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifierTypes;
import com.chen1335.arcanumlib.modules.loot.lootCondition.LootConditionTypes;
import com.chen1335.arcanumlib.modules.loot.lootEntry.LootEntryTypes;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOps;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import org.slf4j.Logger;

@Mod(ArcanumLib.MODID)
public class ArcanumLib {
    public static final String MODID = "arcanumlib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public ArcanumLib(IEventBus modEventBus, ModContainer modContainer) {
        // 嵌套注入操作：表级 / 池级 / 条目级 / 函数级 / 条件级
        LootInjectorOps.TABLE_OPERATOR_TYPES.register(modEventBus);
        LootInjectorOps.POOL_OPERATOR_TYPES.register(modEventBus);
        LootInjectorOps.ENTRY_OPERATOR_TYPES.register(modEventBus);
        LootInjectorOps.FUNCTION_OPERATOR_TYPES.register(modEventBus);
        LootInjectorOps.CONDITION_OPERATOR_TYPES.register(modEventBus);
        LootInjectorOps.TABLE_SELECTOR_TYPES.register(modEventBus);
        LootInjectorOps.POOL_SELECTOR_TYPES.register(modEventBus);
        LootInjectorOps.ENTRY_SELECTOR_TYPES.register(modEventBus);
        LootInjectorOps.FUNCTION_SELECTOR_TYPES.register(modEventBus);
        LootInjectorOps.CONDITION_SELECTOR_TYPES.register(modEventBus);
        modEventBus.addListener(NewRegistryEvent.class, event -> {
            event.register(LootInjectorOps.TABLE_OPERATORS);
            event.register(LootInjectorOps.POOL_OPERATORS);
            event.register(LootInjectorOps.ENTRY_OPERATORS);
            event.register(LootInjectorOps.FUNCTION_OPERATORS);
            event.register(LootInjectorOps.CONDITION_OPERATORS);
            event.register(LootInjectorOps.TABLE_SELECTORS);
            event.register(LootInjectorOps.POOL_SELECTORS);
            event.register(LootInjectorOps.ENTRY_SELECTORS);
            event.register(LootInjectorOps.FUNCTION_SELECTORS);
            event.register(LootInjectorOps.CONDITION_SELECTORS);
            event.register(ChanceModifierTypes.REGISTRY);
        });
        // 战利品条目类型（原版 LootPoolEntries.register 是私有的）
        LootEntryTypes.ENTRY_TYPES.register(modEventBus);
        // 战利品条件类型
        LootConditionTypes.CONDITION_TYPES.register(modEventBus);
        // 概率加工器类型
        ChanceModifierTypes.TYPES.register(modEventBus);
        // 自定义属性
        ArcanumAttributes.ATTRIBUTES.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SERVER, AttributeConfig.CONFIG_SPEC, "arcanumlib/attribute.toml");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
