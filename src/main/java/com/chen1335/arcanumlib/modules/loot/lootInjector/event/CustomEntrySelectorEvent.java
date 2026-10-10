package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** 自定义条目选择器求值时触发，用 setResult 决定是否命中。在 NeoForge.EVENT_BUS 上监听。 */
public class CustomEntrySelectorEvent extends Event {
    private final String id;
    private final int index;
    private final LootPoolEntryContainer entry;
    private final LootPool pool;
    private boolean result;

    public CustomEntrySelectorEvent(String id, int index, LootPoolEntryContainer entry, LootPool pool) {
        this.id = id;
        this.index = index;
        this.entry = entry;
        this.pool = pool;
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

    /** 条目所属的池：占比这类判定需要池的总权重。 */
    public LootPool getPool() {
        return this.pool;
    }

    public boolean getResult() {
        return this.result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public static boolean post(String id, int index, LootPoolEntryContainer entry, LootPool pool) {
        CustomEntrySelectorEvent event = new CustomEntrySelectorEvent(id, index, entry, pool);
        NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }
}
