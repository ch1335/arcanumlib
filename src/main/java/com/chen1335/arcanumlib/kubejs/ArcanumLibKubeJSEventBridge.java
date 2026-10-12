package com.chen1335.arcanumlib.kubejs;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomTableSelectorEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomPoolSelectorEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomFunctionSelectorEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomEntrySelectorEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomConditionSelectorEvent;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.event.CustomTableSelectorKubeEvent;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.event.CustomPoolSelectorKubeEvent;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.event.CustomFunctionSelectorKubeEvent;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.event.CustomEntrySelectorKubeEvent;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.event.CustomConditionSelectorKubeEvent;
import dev.latvian.mods.kubejs.event.EventResult;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.event.EntryCustomKubeEvent;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.event.LootInjectorApplyKubeEvent;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.event.PoolCustomKubeEvent;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.event.RegisterLootInjectorKubeEvent;
import com.chen1335.arcanumlib.kubejs.modules.lootInjector.event.TableCustomKubeEvent;
import com.chen1335.arcanumlib.modules.loot.bonusLoot.event.BonusLootCheckEvent;
import com.chen1335.arcanumlib.modules.loot.bonusLoot.event.ModifyBonusLootEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.EntryCustomOpEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.LootInjectorApplyEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.LootInjectorRegisterEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.PoolCustomOpEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.TableCustomOpEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.chen1335.arcanumlib.kubejs.modules.bonusLoot.BonusLootCheckKubeEvent;
import com.chen1335.arcanumlib.kubejs.modules.bonusLoot.ModifyBonusLootKubeEvent;

/**
 * 把本模组的 NeoForge 事件转发成 KubeJS 脚本事件。
 *
 * <p>用 {@code LOWEST} 优先级：让 Java 侧的监听者先处理完，脚本再决定要不要取消。
 * 没有脚本监听时 {@code hasListeners()} 为假，整个转发是零开销的空操作。
 */
@EventBusSubscriber(modid = ArcanumLib.MODID)
public class ArcanumLibKubeJSEventBridge {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLootInjectorApply(LootInjectorApplyEvent event) {
        if (!ArcanumLibKubeJSEvents.LOOT_INJECTOR_APPLY.hasListeners()) {
            return;
        }

        LootInjectorApplyKubeEvent kubeEvent = new LootInjectorApplyKubeEvent(
                event.getInjector().id(), event.getTableId(), event.getTable(), event.getInjector(), event.getRegistries());

        ArcanumLibKubeJSEvents.LOOT_INJECTOR_APPLY.post(kubeEvent);

        if (kubeEvent.isCanceled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLootInjectorRegister(LootInjectorRegisterEvent event) {
        if (ArcanumLibKubeJSEvents.REGISTER_LOOT_INJECTOR.hasListeners()) {
            ArcanumLibKubeJSEvents.REGISTER_LOOT_INJECTOR.post(new RegisterLootInjectorKubeEvent(event));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBonusLootCheck(BonusLootCheckEvent event) {
        if (!ArcanumLibKubeJSEvents.BONUS_LOOT_CHECK.hasListeners()) {
            return;
        }

        BonusLootCheckKubeEvent kubeEvent = new BonusLootCheckKubeEvent(
                event.getLootContext(), event.getLootTableId(), event.isConfigAllowed());
        EventResult result = ArcanumLibKubeJSEvents.BONUS_LOOT_CHECK.post(kubeEvent);

        // 返回值优先，其次看事件自己有没有设定；都不表态就沿用配置的结果
        if (result.interruptTrue()) {
            event.setResult(true);
        } else if (result.interruptFalse()) {
            event.setResult(false);
        } else if (kubeEvent.getResult() != null) {
            event.setResult(kubeEvent.getResult());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onModifyBonusLoot(ModifyBonusLootEvent event) {
        if (!ArcanumLibKubeJSEvents.MODIFY_BONUS_LOOT.hasListeners()) {
            return;
        }

        ModifyBonusLootKubeEvent kubeEvent = new ModifyBonusLootKubeEvent(
                event.getLootContext(), event.getLootTableId(), event.getOriginalRatio());
        ArcanumLibKubeJSEvents.MODIFY_BONUS_LOOT.post(kubeEvent);

        if (kubeEvent.isCanceled()) {
            event.setCanceled(true);
            return;
        }

        event.setRatio(kubeEvent.getRatio());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTableCustom(TableCustomOpEvent event) {        if (ArcanumLibKubeJSEvents.TABLE_CUSTOM.hasListeners()) {
            ArcanumLibKubeJSEvents.TABLE_CUSTOM.post(new TableCustomKubeEvent(
                    event.getId(), event.getData(), event.getTable(), event.getRegistries()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPoolCustom(PoolCustomOpEvent event) {
        if (ArcanumLibKubeJSEvents.POOL_CUSTOM.hasListeners()) {
            ArcanumLibKubeJSEvents.POOL_CUSTOM.post(new PoolCustomKubeEvent(
                    event.getId(), event.getData(), event.getTable(), event.getPoolIndex(), event.getPool(), event.getRegistries()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEntryCustom(EntryCustomOpEvent event) {
        if (ArcanumLibKubeJSEvents.ENTRY_CUSTOM.hasListeners()) {
            ArcanumLibKubeJSEvents.ENTRY_CUSTOM.post(new EntryCustomKubeEvent(
                    event.getId(), event.getData(), event.getPool(), event.getEntries(), event.getIndex(), event.getRegistries()));
        }
    }

    // ---- 五个自定义选择器：把脚本结果回写到 Java 事件 ----

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCustomTableSelector(CustomTableSelectorEvent event) {
        if (!ArcanumLibKubeJSEvents.CUSTOM_TABLE_SELECTOR.hasListeners()) {
            return;
        }

        CustomTableSelectorKubeEvent kubeEvent = new CustomTableSelectorKubeEvent(
                event.getId(), event.getData(), event.getTableId(), event.getTable(), event.getRegistries());
        EventResult result = ArcanumLibKubeJSEvents.CUSTOM_TABLE_SELECTOR.post(kubeEvent);

        if (result.interruptTrue()) {
            event.setResult(true);
        } else if (result.interruptFalse()) {
            event.setResult(false);
        } else if (kubeEvent.getResult() != null) {
            event.setResult(kubeEvent.getResult());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCustomPoolSelector(CustomPoolSelectorEvent event) {
        if (!ArcanumLibKubeJSEvents.CUSTOM_POOL_SELECTOR.hasListeners()) {
            return;
        }

        CustomPoolSelectorKubeEvent kubeEvent = new CustomPoolSelectorKubeEvent(
                event.getId(), event.getData(), event.getIndex(), event.getPool(), event.getRegistries());
        EventResult result = ArcanumLibKubeJSEvents.CUSTOM_POOL_SELECTOR.post(kubeEvent);

        if (result.interruptTrue()) {
            event.setResult(true);
        } else if (result.interruptFalse()) {
            event.setResult(false);
        } else if (kubeEvent.getResult() != null) {
            event.setResult(kubeEvent.getResult());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCustomEntrySelector(CustomEntrySelectorEvent event) {
        if (!ArcanumLibKubeJSEvents.CUSTOM_ENTRY_SELECTOR.hasListeners()) {
            return;
        }

        CustomEntrySelectorKubeEvent kubeEvent = new CustomEntrySelectorKubeEvent(
                event.getId(), event.getData(), event.getIndex(), event.getEntry(), event.getPool(), event.getRegistries());
        EventResult result = ArcanumLibKubeJSEvents.CUSTOM_ENTRY_SELECTOR.post(kubeEvent);

        if (result.interruptTrue()) {
            event.setResult(true);
        } else if (result.interruptFalse()) {
            event.setResult(false);
        } else if (kubeEvent.getResult() != null) {
            event.setResult(kubeEvent.getResult());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCustomFunctionSelector(CustomFunctionSelectorEvent event) {
        if (!ArcanumLibKubeJSEvents.CUSTOM_FUNCTION_SELECTOR.hasListeners()) {
            return;
        }

        CustomFunctionSelectorKubeEvent kubeEvent = new CustomFunctionSelectorKubeEvent(
                event.getId(), event.getData(), event.getIndex(), event.getFunction(), event.getRegistries());
        EventResult result = ArcanumLibKubeJSEvents.CUSTOM_FUNCTION_SELECTOR.post(kubeEvent);

        if (result.interruptTrue()) {
            event.setResult(true);
        } else if (result.interruptFalse()) {
            event.setResult(false);
        } else if (kubeEvent.getResult() != null) {
            event.setResult(kubeEvent.getResult());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCustomConditionSelector(CustomConditionSelectorEvent event) {
        if (!ArcanumLibKubeJSEvents.CUSTOM_CONDITION_SELECTOR.hasListeners()) {
            return;
        }

        CustomConditionSelectorKubeEvent kubeEvent = new CustomConditionSelectorKubeEvent(
                event.getId(), event.getData(), event.getIndex(), event.getCondition(), event.getRegistries());
        EventResult result = ArcanumLibKubeJSEvents.CUSTOM_CONDITION_SELECTOR.post(kubeEvent);

        if (result.interruptTrue()) {
            event.setResult(true);
        } else if (result.interruptFalse()) {
            event.setResult(false);
        } else if (kubeEvent.getResult() != null) {
            event.setResult(kubeEvent.getResult());
        }
    }
}
