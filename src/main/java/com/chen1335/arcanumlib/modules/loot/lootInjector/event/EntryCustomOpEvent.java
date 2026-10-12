package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
    /** json 里的可选附加数据；没写就是空 tag（不会是 null）。 */
    private final CompoundTag data;
    private final LootPool pool;
    private final List<LootPoolEntryContainer> entries;
    private final int index;
    private final HolderLookup.Provider registries;

    public EntryCustomOpEvent(String id, CompoundTag data, LootPool pool, List<LootPoolEntryContainer> entries, int index, HolderLookup.Provider registries) {
        this.id = id;
        this.data = data;
        this.pool = pool;
        this.entries = entries;
        this.index = index;
        this.registries = registries;
    }

    /** json 里的可选附加数据；没写就是空 tag（不会是 null）。 */
    public CompoundTag getData() {
        return this.data;
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

    /** 求值时可用的注册表访问：需要按 codec 反序列化 / 反查注册项时用。 */
    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }

    public static void post(String id, CompoundTag data, LootPool pool, List<LootPoolEntryContainer> entries, int index, HolderLookup.Provider registries) {
        NeoForge.EVENT_BUS.post(new EntryCustomOpEvent(id, data, pool, entries, index, registries));
    }
}
