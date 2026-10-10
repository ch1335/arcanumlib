package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 自定义表选择器求值时触发，用 setResult 决定是否命中。
 * 注入器会拿每张表逐个问一遍（注入器数 × 表数），别做重活。在 NeoForge.EVENT_BUS 上监听。
 */
public class CustomTableConditionEvent extends Event {
    private final String id;
    private final ResourceLocation tableId;
    private final LootTable table;
    private boolean result;

    public CustomTableConditionEvent(String id, ResourceLocation tableId, LootTable table) {
        this.id = id;
        this.tableId = tableId;
        this.table = table;
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

    public static boolean post(String id, ResourceLocation tableId, LootTable table) {
        CustomTableConditionEvent event = new CustomTableConditionEvent(id, tableId, table);
        NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }
}
