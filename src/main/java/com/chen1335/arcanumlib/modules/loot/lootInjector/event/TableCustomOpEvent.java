package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** json 里写了 {@code arcanumlib:table_custom} 时触发，拿到 id 自己改表。在 NeoForge.EVENT_BUS 上监听。 */
public class TableCustomOpEvent extends Event {
    private final String id;
    private final LootTable table;
    private final HolderLookup.Provider registries;

    public TableCustomOpEvent(String id, LootTable table, HolderLookup.Provider registries) {
        this.id = id;
        this.table = table;
        this.registries = registries;
    }

    public String getId() {
        return this.id;
    }

    public LootTable getTable() {
        return this.table;
    }

    /** 求值时可用的注册表访问：需要按 codec 反序列化 / 反查注册项时用。 */
    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }

    public static void post(String id, LootTable table, HolderLookup.Provider registries) {
        NeoForge.EVENT_BUS.post(new TableCustomOpEvent(id, table, registries));
    }
}
