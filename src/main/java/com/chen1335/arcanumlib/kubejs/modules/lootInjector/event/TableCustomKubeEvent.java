package com.chen1335.arcanumlib.kubejs.modules.lootInjector.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.HolderLookup;

/**
 * 脚本事件 {@code ArcanumLibEvents.tableCustom}：json 里写了 {@code arcanumlib:table_custom} 时触发。参数与 Java 侧 {@code TableCustomOpEvent} 对齐。
 *
 * <p>参数与 Java 侧事件一一对应，包括 {@code registries}（求值时可用注册表访问）。
 */
public class TableCustomKubeEvent implements KubeEvent {

    private final String id;
    private final CompoundTag data;
    private final LootTable table;
    private final HolderLookup.Provider registries;

    public TableCustomKubeEvent(String id, CompoundTag data, LootTable table, HolderLookup.Provider registries) {
        this.id = id;
        this.data = data;
        this.table = table;
        this.registries = registries;
    }

    public String getId() {
        return this.id;
    }

    /** json 里可选的附加数据；没写就是空 tag。 */
    public CompoundTag getData() {
        return this.data;
    }

    public LootTable getTable() {
        return this.table;
    }

    /** 求值时用的注册表访问，与 Java 侧事件收到的 registries 是同一个。 */
    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }
}
