package com.chen1335.arcanumlib.kubejs.modules.lootInjector.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * 脚本事件 {@code ArcanumLibEvents.customTableSelector}：判定表选择器 {@code custom} 时触发。参数与 Java 侧 {@code CustomTableSelectorEvent} 对齐。
 *
 * <p>参数与 Java 侧事件一一对应，包括 {@code registries}（求值时可用注册表访问）。
 */
public class CustomTableSelectorKubeEvent implements KubeEvent {

    private final String id;
    /** json 里可选的附加数据；没写就是空 tag（与 Java 侧同名事件一致）。 */
    private final CompoundTag data;
    private final ResourceLocation tableId;
    private final LootTable table;
    private final HolderLookup.Provider registries;
    private Boolean result;

    public CustomTableSelectorKubeEvent(String id, CompoundTag data, ResourceLocation tableId, LootTable table, HolderLookup.Provider registries) {
        this.id = id;
        this.data = data;
        this.tableId = tableId;
        this.table = table;
        this.registries = registries;
    }

    /** json 里可选的附加数据；没写就是空 tag。 */
    public CompoundTag getData() {
        return this.data;
    }

    public String getId() {
        return this.id;
    }

    public ResourceLocation getTableId() {
        return this.tableId;
    }

    public LootTable getTable() {
        return this.table;
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
