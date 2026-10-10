package com.chen1335.arcanumlib.modules.loot.lootInjector.event;

import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjector;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Optional;

/**
 * 某个注入器即将改某张战利品表时触发：可以取消，也可以换成另一个注入器。
 * 换掉之后"已应用"记录里仍是原来那个 id，所以记的是"这个槽位改过了"，既不重复应用也不会反复触发。
 */
public class LootInjectorApplyEvent extends Event implements ICancellableEvent {
    private final ResourceLocation tableId;
    private final LootTable table;
    private LootInjector injector;

    public LootInjectorApplyEvent(LootInjector injector, ResourceLocation tableId, LootTable table) {
        this.injector = injector;
        this.tableId = tableId;
        this.table = table;
    }

    public ResourceLocation getTableId() {
        return this.tableId;
    }

    /** 可以就地改的实例；注意 LootTable 的原版构造器已经把条件合成成 composite，改动要连复合体一起更新。 */
    public LootTable getTable() {
        return this.table;
    }

    /** 即将应用的注入器，id 来自 json 文件路径。 */
    public LootInjector getInjector() {
        return this.injector;
    }

    /** 替换即将应用的注入器；传 {@code null} 等同于取消。 */
    public void setInjector(LootInjector injector) {
        this.injector = injector;
    }

    /** @return 实际要应用的注入器；空表示被取消或没有可用的注入器。 */
    public static Optional<LootInjector> post(LootInjector injector, ResourceLocation tableId, LootTable table) {
        LootInjectorApplyEvent event = new LootInjectorApplyEvent(injector, tableId, table);

        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            return Optional.empty();
        }

        return Optional.ofNullable(event.getInjector());
    }
}
