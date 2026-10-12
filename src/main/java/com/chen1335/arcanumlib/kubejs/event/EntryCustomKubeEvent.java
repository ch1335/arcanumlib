package com.chen1335.arcanumlib.kubejs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import java.util.List;
import net.minecraft.core.HolderLookup;

/**
 * 脚本事件 {@code ArcanumLibEvents.entryCustom}：json 里写了 {@code arcanumlib:entry_custom} 时触发。参数与 Java 侧 {@code EntryCustomOpEvent} 对齐。
 *
 * <p>参数与 Java 侧事件一一对应，包括 {@code registries}（求值时可用注册表访问）。
 */
public class EntryCustomKubeEvent implements KubeEvent {

    private final String id;
    private final CompoundTag data;
    private final LootPool pool;
    private final List<LootPoolEntryContainer> entries;
    private final int index;
    private final HolderLookup.Provider registries;

    public EntryCustomKubeEvent(String id, CompoundTag data, LootPool pool, List<LootPoolEntryContainer> entries, int index, HolderLookup.Provider registries) {
        this.id = id;
        this.data = data;
        this.pool = pool;
        this.entries = entries;
        this.index = index;
        this.registries = registries;
    }

    public String getId() {
        return this.id;
    }

    public CompoundTag getData() {
        return this.data;
    }

    public LootPool getPool() {
        return this.pool;
    }

    /** 可变列表：脚本里可以直接 set / remove。 */
    public List<LootPoolEntryContainer> getEntries() {
        return this.entries;
    }

    /** 当前条目在 entries 里的下标。 */
    public int getIndex() {
        return this.index;
    }

    /** 求值时用的注册表访问，与 Java 侧事件收到的 registries 是同一个。 */
    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }
}
