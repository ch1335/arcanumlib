package com.chen1335.arcanumlib.test;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOp;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomEntryConditionEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomInjectorOpEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomPoolConditionEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomTableConditionEvent;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.LootInjectorApplyEvent;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;

/**
 * 配对 {@link TestLootInjectorProvider} 里 Custom 系列注入器的运行时实现。
 *
 * <p>只认自己那几个 custom_id，其他 id 一律不处理；没有测试 json 时这几个方法都是空转。
 * 每次命中都会打日志，方便在 latest.log 里确认自定义分支确实被走到了。
 */
@EventBusSubscriber(modid = ArcanumLib.MODID)
public class TestCustomLootHandlers {

    /** test_custom_op：直接往表里塞一个必掉的钻石块。 */
    @SubscribeEvent
    public static void onCustomOp(CustomInjectorOpEvent event) {
        if (!"test_custom_op".equals(event.getId())) {
            return;
        }

        event.getTable().pools.add(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .name("arcanumlib_test_custom")
                .add(LootItem.lootTableItem(Items.DIAMOND_BLOCK))
                .build());
        ArcanumLib.LOGGER.info("[test] custom op applied");
    }

    /** test_table：只命中僵尸系的表。 */
    @SubscribeEvent
    public static void onCustomTable(CustomTableConditionEvent event) {
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
    public static void onCustomPool(CustomPoolConditionEvent event) {
        if (!"test_pool".equals(event.getId())) {
            return;
        }

        event.setResult(event.getIndex() == 0);
    }

    /** test_entry：命中所有单例条目。调用量最大，用静态开关把日志压到一条。 */
    @SubscribeEvent
    public static void onCustomEntry(CustomEntryConditionEvent event) {
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

        event.setInjector(new LootInjector(injector.id(), injector.tableCondition(), injector.priority(),
                List.of(new LootInjectorOp.AddPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.DIAMOND_BLOCK))
                        .build()))));
        ArcanumLib.LOGGER.info("[test] injector replaced by LootInjectorApplyEvent on {}", event.getTableId());
    }
}
