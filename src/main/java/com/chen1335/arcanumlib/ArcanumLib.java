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
        LootInjectorOps.OP_TYPES.register(modEventBus);
        LootInjectorOps.TABLE_CONDITION_TYPES.register(modEventBus);
        LootInjectorOps.POOL_CONDITION_TYPES.register(modEventBus);
        LootInjectorOps.ENTRY_CONDITION_TYPES.register(modEventBus);
        modEventBus.addListener(NewRegistryEvent.class, event -> {
            event.register(LootInjectorOps.REGISTRY);
            event.register(LootInjectorOps.TABLE_CONDITIONS);
            event.register(LootInjectorOps.POOL_CONDITIONS);
            event.register(LootInjectorOps.ENTRY_CONDITIONS);
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
