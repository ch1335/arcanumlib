package com.chen1335.arcanumlib.kubejs.modules.lootInjector.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjector;
import net.minecraft.core.HolderLookup;

/**
 * 脚本事件 {@code ArcanumLibEvents.lootInjectorApply}：每条注入器被应用到某张表**之前**触发。参数与 Java 侧 {@code LootInjectorApplyEvent} 对齐。
 *
 * <p>参数与 Java 侧事件一一对应，包括 {@code registries}（求值时可用注册表访问）。
 */
public class LootInjectorApplyKubeEvent implements KubeEvent {

    private final ResourceLocation injectorId;
    private final ResourceLocation tableId;
    private final LootTable table;
    private final LootInjector injector;
    private final HolderLookup.Provider registries;
    private boolean canceled;

    public LootInjectorApplyKubeEvent(ResourceLocation injectorId, ResourceLocation tableId, LootTable table, LootInjector injector, HolderLookup.Provider registries) {
        this.injectorId = injectorId;
        this.tableId = tableId;
        this.table = table;
        this.injector = injector;
        this.registries = registries;
    }

    /** 注入器 id（就是它的 json 文件路径）。 */
    public ResourceLocation getInjectorId() {
        return this.injectorId;
    }

    /** 目标战利品表 id。 */
    public ResourceLocation getTableId() {
        return this.tableId;
    }

    /** 目标表本体。 */
    public LootTable getTable() {
        return this.table;
    }

    /** 这次要应用的注入器本体。 */
    public LootInjector getInjector() {
        return this.injector;
    }

    /** 求值时用的注册表访问，与 Java 侧事件收到的 registries 是同一个。 */
    public HolderLookup.Provider getRegistries() {
        return this.registries;
    }

    /** 取消这次应用，注入器对这张表就不生效了。 */
    public void cancel() {
        this.canceled = true;
    }

    public boolean isCanceled() {
        return this.canceled;
    }
}
