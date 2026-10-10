package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/**
 * json 里写了 {@code arcanumlib:entry_custom} 时触发，作用对象是"当前条目"。
 * entries 已经是可变列表，可以直接 set / remove。在 NeoForge.EVENT_BUS 上监听。
 */
public class EntryCustomOpEvent extends Event {
    private final String id;
    private final LootPool pool;
    private final List<LootPoolEntryContainer> entries;
    private final int index;

    public EntryCustomOpEvent(String id, LootPool pool, List<LootPoolEntryContainer> entries, int index) {
        this.id = id;
        this.pool = pool;
        this.entries = entries;
        this.index = index;
    }

    public String getId() {
        return this.id;
    }

    public LootPool getPool() {
        return this.pool;
    }

    /** 可变列表；改它就是在改当前池。 */
    public List<LootPoolEntryContainer> getEntries() {
        return this.entries;
    }

    public int getIndex() {
        return this.index;
    }

    public LootPoolEntryContainer getEntry() {
        return this.entries.get(this.index);
    }

    public static void post(String id, LootPool pool, List<LootPoolEntryContainer> entries, int index) {
        NeoForge.EVENT_BUS.post(new EntryCustomOpEvent(id, pool, entries, index));
    }
}
