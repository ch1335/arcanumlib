package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 自定义表选择器求值时触发，用 setResult 决定是否命中。
 * 注入器会拿每张表逐个问一遍（注入器数 × 表数），别做重活。在 NeoForge.EVENT_BUS 上监听。
 */
public class CustomTableSelectorEvent extends Event {
    private final String id;
    private final ResourceLocation tableId;
    private final LootTable table;
    private final HolderLookup.Provider registries;
    private boolean result;

    public CustomTableSelectorEvent(String id, ResourceLocation tableId, LootTable table, HolderLookup.Provider registries) {
        this.id = id;
        this.tableId = tableId;
        this.table = table;
        this.registries = registries;
    }

    public String getId() {
        return this.id;
    }

    public ResourceLocation getTableId() {
        return this.tableId;
    }

    public LootTable getTable() {
        return this.table;
    }

    public boolean getResult() {
        return this.result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    /** 求值时可用的注册表访问：需要按 codec 反序列化 / 反查注册项时用。 */
    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }

    public static boolean post(String id, ResourceLocation tableId, LootTable table, HolderLookup.Provider registries) {
        CustomTableSelectorEvent event = new CustomTableSelectorEvent(id, tableId, table, registries);
        NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }
}
