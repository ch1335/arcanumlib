package com.chen1335.arcanumlib.test;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomConditionSelectorEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomEntrySelectorEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomFunctionSelectorEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomPoolSelectorEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomTableSelectorEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.EntryCustomOpEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.LootInjectorApplyEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.PoolCustomOpEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.TableCustomOpEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.PoolOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.TableOperator;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

/**
 * 配对 {@link TestLootInjectorProvider} 里 custom 系列注入器的运行时实现。
 *
 * <p>只认自己那几个 custom_id，其他 id 一律不处理；没有测试 json 时这几个方法都是空转。
 * 命中时会打日志，方便在 latest.log 里确认自定义分支确实被走到了。
 */
@EventBusSubscriber(modid = ArcanumLib.MODID)
public class TestCustomLootHandlers {

    /** test_table_custom：往表里塞一个必掉的钻石块池。 */
    @SubscribeEvent
    public static void onTableCustom(TableCustomOpEvent event) {
        if (!"test_table_custom".equals(event.getId())) {
            return;
        }

        event.getTable().pools.add(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .name("arcanumlib_test_custom")
                .add(LootItem.lootTableItem(Items.DIAMOND_BLOCK))
                .build());
        ArcanumLib.LOGGER.info("[test] table_custom applied");
    }

    /** test_pool_custom：给"当前池"追加一个必掉的下界合金锭条目。 */
    @SubscribeEvent
    public static void onPoolCustom(PoolCustomOpEvent event) {
        if (!"test_pool_custom".equals(event.getId())) {
            return;
        }

        PoolOperator.mutableEntries(event.getPool()).add(LootItem.lootTableItem(Items.NETHERITE_INGOT).build());
        ArcanumLib.LOGGER.info("[test] pool_custom applied on pool {}", event.getPoolIndex());
    }

    /** test_entry_custom：把"当前条目"换成钻石。 */
    @SubscribeEvent
    public static void onEntryCustom(EntryCustomOpEvent event) {
        if (!"test_entry_custom".equals(event.getId())) {
            return;
        }

        event.getEntries().set(event.getIndex(), LootItem.lootTableItem(Items.DIAMOND).build());
        ArcanumLib.LOGGER.info("[test] entry_custom applied at {}", event.getIndex());
    }

    /** test_function：命中所有函数。 */
    @SubscribeEvent
    public static void onCustomFunction(CustomFunctionSelectorEvent event) {
        if (!"test_function".equals(event.getId())) {
            return;
        }

        event.setResult(true);
    }

    /** test_condition：命中所有条件。 */
    @SubscribeEvent
    public static void onCustomCondition(CustomConditionSelectorEvent event) {
        if (!"test_condition".equals(event.getId())) {
            return;
        }

        event.setResult(true);
    }
    /** test_table：只命中僵尸系的表。 */
    @SubscribeEvent
    public static void onCustomTable(CustomTableSelectorEvent event) {
        if (!"test_table".equals(event.getId())) {
            return;
        }

        boolean hit = event.getTableId().getPath().contains("zombie");
        event.setResult(hit);
        if (hit) {
            ArcanumLib.LOGGER.info("[test] custom table condition matched {}", event.getTableId());
        }
    }

    /** test_pool：只命中第 0 个池。会按 注入器 × 表 × 池 调用，所以只在命中时打日志。 */
    @SubscribeEvent
    public static void onCustomPool(CustomPoolSelectorEvent event) {
        if (!"test_pool".equals(event.getId())) {
            return;
        }

        event.setResult(event.getIndex() == 0);
    }

    /** test_entry：命中所有单例条目。调用量最大，不打日志。 */
    @SubscribeEvent
    public static void onCustomEntry(CustomEntrySelectorEvent event) {
        if (!"test_entry".equals(event.getId())) {
            return;
        }

        event.setResult(event.getEntry() instanceof LootPoolSingletonContainer);
    }

    /** test_apply_event：把整个注入器换成"必掉一个钻石块"，用于验证 LootInjectorApplyEvent 的替换语义。 */
    @SubscribeEvent
    public static void onApply(LootInjectorApplyEvent event) {
        LootInjector injector = event.getInjector();
        if (!"arcanumlib:test_apply_event".equals(injector.id().toString())) {
            return;
        }

        event.setInjector(new LootInjector(injector.id(), injector.tableSelector(), injector.priority(),
                List.of(new TableOperator.AddPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.DIAMOND_BLOCK))
                        .build()))));
        ArcanumLib.LOGGER.info("[test] injector replaced by LootInjectorApplyEvent on {}", event.getTableId());
    }
}
