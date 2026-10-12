package com.chen1335.arcanumlib.kubejs.modules.lootInjector.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.core.HolderLookup;

/**
 * 脚本事件 {@code ArcanumLibEvents.customPoolSelector}：判定池选择器 {@code custom} 时触发。参数与 Java 侧 {@code CustomPoolSelectorEvent} 对齐。
 *
 * <p>参数与 Java 侧事件一一对应，包括 {@code registries}（求值时可用注册表访问）。
 */
public class CustomPoolSelectorKubeEvent implements KubeEvent {

    private final String id;
    /** json 里可选的附加数据；没写就是空 tag（与 Java 侧同名事件一致）。 */
    private final CompoundTag data;
    private final int index;
    private final LootPool pool;
    private final HolderLookup.Provider registries;
    private Boolean result;

    public CustomPoolSelectorKubeEvent(String id, CompoundTag data, int index, LootPool pool, HolderLookup.Provider registries) {
        this.id = id;
        this.data = data;
        this.index = index;
        this.pool = pool;
        this.registries = registries;
    }

    public String getId() {
        return this.id;
    }

    /** json 里可选的附加数据；没写就是空 tag。 */
    public CompoundTag getData() {
        return this.data;
    }

    public int getIndex() {
        return this.index;
    }

    public LootPool getPool() {
        return this.pool;
    }

    /** 求值时用的注册表访问，与 Java 侧事件收到的 registries 是同一个。 */
    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }

    /** 显式设定判定结果；不设就是"没表态"，沿用 Java 侧监听者的结果。 */
    public void setResult(boolean value) {
        this.result = value;
    }

    public Boolean getResult() {
        return this.result;
    }
}
