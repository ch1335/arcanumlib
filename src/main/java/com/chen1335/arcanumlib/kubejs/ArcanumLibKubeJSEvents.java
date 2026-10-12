package com.chen1335.arcanumlib.kubejs;

import com.chen1335.arcanumlib.kubejs.event.EntryCustomKubeEvent;
import com.chen1335.arcanumlib.kubejs.event.LootInjectorApplyKubeEvent;
import com.chen1335.arcanumlib.kubejs.event.PoolCustomKubeEvent;
import com.chen1335.arcanumlib.kubejs.event.RegisterLootInjectorKubeEvent;
import com.chen1335.arcanumlib.kubejs.event.TableCustomKubeEvent;
import com.chen1335.arcanumlib.kubejs.event.CustomConditionSelectorKubeEvent;
import com.chen1335.arcanumlib.kubejs.event.CustomEntrySelectorKubeEvent;
import com.chen1335.arcanumlib.kubejs.event.CustomFunctionSelectorKubeEvent;
import com.chen1335.arcanumlib.kubejs.event.CustomPoolSelectorKubeEvent;
import com.chen1335.arcanumlib.kubejs.event.CustomTableSelectorKubeEvent;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/**
 * ArcanumLib 的脚本事件组：脚本里写 {@code ArcanumLibEvents.xxx(event => ...)} 就能监听。
 *
 * <p>由 {@code ArcanumLibKubeJSEventBridge} 从本模组的 NeoForge 事件转发过来 —— 注入逻辑本身不依赖 KubeJS。
 */
public interface ArcanumLibKubeJSEvents {

    EventGroup GROUP = EventGroup.of("ArcanumLibEvents");

    /** 每条注入器应用到某张表之前。可以 {@code event.cancel()} 取消。 */
    EventHandler LOOT_INJECTOR_APPLY = GROUP.server("lootInjectorApply", () -> LootInjectorApplyKubeEvent.class);

    /** 运行时注册注入器：数据包 json 之外，脚本也能加自己的注入器（同 id 覆盖数据包里的）。 */
    EventHandler REGISTER_LOOT_INJECTOR = GROUP.server("registerLootInjector", () -> RegisterLootInjectorKubeEvent.class);

    /** {@code arcanumlib:table_custom}：拿到表本体自己改。 */
    EventHandler TABLE_CUSTOM = GROUP.server("tableCustom", () -> TableCustomKubeEvent.class);

    /** {@code arcanumlib:pool_custom}：拿到"当前池"自己改。 */
    EventHandler POOL_CUSTOM = GROUP.server("poolCustom", () -> PoolCustomKubeEvent.class);

    /** {@code arcanumlib:entry_custom}：拿到"当前条目列表 + 下标"自己改。 */
    EventHandler ENTRY_CUSTOM = GROUP.server("entryCustom", () -> EntryCustomKubeEvent.class);

    /** 判定表选择器 {@code custom} 时。{@code return false} / {@code event.setResult(false)} = 不匹配。 */
    EventHandler CUSTOM_TABLE_SELECTOR = GROUP.server("customTableSelector", () -> CustomTableSelectorKubeEvent.class).hasResult();

    /** 判定池选择器 {@code custom} 时。 */
    EventHandler CUSTOM_POOL_SELECTOR = GROUP.server("customPoolSelector", () -> CustomPoolSelectorKubeEvent.class).hasResult();

    /** 判定条目选择器 {@code custom} 时。 */
    EventHandler CUSTOM_ENTRY_SELECTOR = GROUP.server("customEntrySelector", () -> CustomEntrySelectorKubeEvent.class).hasResult();

    /** 判定函数选择器 {@code custom} 时。 */
    EventHandler CUSTOM_FUNCTION_SELECTOR = GROUP.server("customFunctionSelector", () -> CustomFunctionSelectorKubeEvent.class).hasResult();

    /** 判定条件选择器 {@code custom} 时。 */
    EventHandler CUSTOM_CONDITION_SELECTOR = GROUP.server("customConditionSelector", () -> CustomConditionSelectorKubeEvent.class).hasResult();
}