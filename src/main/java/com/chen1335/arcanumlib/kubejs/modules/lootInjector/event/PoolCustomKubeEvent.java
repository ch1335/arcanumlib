package com.chen1335.arcanumlib.kubejs.modules.lootInjector.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.core.HolderLookup;

/**
 * 脚本事件 {@code ArcanumLibEvents.poolCustom}：json 里写了 {@code arcanumlib:pool_custom} 时触发，作用对象是"当前池"。参数与 Java 侧 {@code PoolCustomOpEvent} 对齐。
 *
 * <p>参数与 Java 侧事件一一对应，包括 {@code registries}（求值时可用注册表访问）。
 */
public class PoolCustomKubeEvent implements KubeEvent {

    private final String id;
    private final CompoundTag data;
    private final LootTable table;
    private final int poolIndex;
    private final LootPool pool;
    private final HolderLookup.Provider registries;

    public PoolCustomKubeEvent(String id, CompoundTag data, LootTable table, int poolIndex, LootPool pool, HolderLookup.Provider registries) {
        this.id = id;
        this.data = data;
        this.table = table;
        this.poolIndex = poolIndex;
        this.pool = pool;
        this.registries = registries;
    }

    public String getId() {
        return this.id;
    }

    public CompoundTag getData() {
        return this.data;
    }

    public LootTable getTable() {
        return this.table;
    }

    public int getPoolIndex() {
        return this.poolIndex;
    }

    public LootPool getPool() {
        return this.pool;
    }

    /** 求值时用的注册表访问，与 Java 侧事件收到的 registries 是同一个。 */
    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }
}
