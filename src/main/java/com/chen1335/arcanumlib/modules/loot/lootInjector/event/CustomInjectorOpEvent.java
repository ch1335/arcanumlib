package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** json 里写了 {@code arcanumlib:custom} 的注入操作时触发，拿到 id 自己改表。在 NeoForge.EVENT_BUS 上监听。 */
public class CustomInjectorOpEvent extends Event {
    private final String id;
    private final LootTable table;

    public CustomInjectorOpEvent(String id, LootTable table) {
        this.id = id;
        this.table = table;
    }

    public String getId() {
        return this.id;
    }

    public LootTable getTable() {
        return this.table;
    }

    public static void post(String id, LootTable table) {
        NeoForge.EVENT_BUS.post(new CustomInjectorOpEvent(id, table));
    }
}
