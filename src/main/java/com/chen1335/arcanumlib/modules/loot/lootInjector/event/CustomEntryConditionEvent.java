package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** 自定义条目选择器求值时触发，用 setResult 决定是否命中。在 NeoForge.EVENT_BUS 上监听。 */
public class CustomEntryConditionEvent extends Event {
    private final String id;
    private final int index;
    private final LootPoolEntryContainer entry;
    private boolean result;

    public CustomEntryConditionEvent(String id, int index, LootPoolEntryContainer entry) {
        this.id = id;
        this.index = index;
        this.entry = entry;
    }

    public String getId() {
        return this.id;
    }

    /** 条目在它所属池里的下标。 */
    public int getIndex() {
        return this.index;
    }

    public LootPoolEntryContainer getEntry() {
        return this.entry;
    }

    public boolean getResult() {
        return this.result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public static boolean post(String id, int index, LootPoolEntryContainer entry) {
        CustomEntryConditionEvent event = new CustomEntryConditionEvent(id, index, entry);
        NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }
}
