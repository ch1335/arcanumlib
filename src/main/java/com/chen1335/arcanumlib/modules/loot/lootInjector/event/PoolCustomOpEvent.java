package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** json 里写了 {@code arcanumlib:pool_custom} 时触发，作用对象是"当前池"。在 NeoForge.EVENT_BUS 上监听。 */
public class PoolCustomOpEvent extends Event {
    private final String id;
    private final LootTable table;
    private final int poolIndex;
    private final LootPool pool;

    public PoolCustomOpEvent(String id, LootTable table, int poolIndex, LootPool pool) {
        this.id = id;
        this.table = table;
        this.poolIndex = poolIndex;
        this.pool = pool;
    }

    public String getId() {
        return this.id;
    }

    /** 当前池所属的表；要用它增删池的话，poolIndex 也已给好。 */
    public LootTable getTable() {
        return this.table;
    }

    public int getPoolIndex() {
        return this.poolIndex;
    }

    public LootPool getPool() {
        return this.pool;
    }

    public static void post(String id, LootTable table, int poolIndex, LootPool pool) {
        NeoForge.EVENT_BUS.post(new PoolCustomOpEvent(id, table, poolIndex, pool));
    }
}
