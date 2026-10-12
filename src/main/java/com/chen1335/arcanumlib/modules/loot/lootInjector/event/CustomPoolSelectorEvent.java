package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.loot.LootPool;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/** 自定义池选择器求值时触发，用 setResult 决定是否命中。在 NeoForge.EVENT_BUS 上监听。 */
public class CustomPoolSelectorEvent extends Event {
    private final String id;
    /** json 里的可选附加数据；没写就是空 tag（不会是 null）。 */
    private final CompoundTag data;
    private final int index;
    private final LootPool pool;
    private final HolderLookup.Provider registries;
    private boolean result;

    public CustomPoolSelectorEvent(String id, CompoundTag data, int index, LootPool pool, HolderLookup.Provider registries) {
        this.id = id;
        this.data = data;
        this.index = index;
        this.pool = pool;
        this.registries = registries;
    }

    /** json 里的可选附加数据；没写就是空 tag（不会是 null）。 */
    public CompoundTag getData() {
        return this.data;
    }

    public String getId() {
        return this.id;
    }

    /** 池在它所属表里的下标。 */
    public int getIndex() {
        return this.index;
    }

    public LootPool getPool() {
        return this.pool;
    }

    public boolean getResult() {
        return this.result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    /** 求值时可用的注册表访问：需要按 codec 反序列化 / 反查注册项时用。 */
    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }

    public static boolean post(String id, CompoundTag data, int index, LootPool pool, HolderLookup.Provider registries) {
        CustomPoolSelectorEvent event = new CustomPoolSelectorEvent(id, data, index, pool, registries);
        NeoForge.EVENT_BUS.post(event);
        return event.getResult();
    }
}
