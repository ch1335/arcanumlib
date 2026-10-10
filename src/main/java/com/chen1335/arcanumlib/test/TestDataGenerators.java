package com.chen1335.arcanumlib.test;

import com.chen1335.arcanumlib.ArcanumLib;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * 把测试用的数据生成器接进去。只有 runData 会触发，正常启动游戏不产生任何效果。
 * 不写 {@code bus}：该参数在 1.21.1 已标记待删除，总线按事件类型自动判定。
 */
@EventBusSubscriber(modid = ArcanumLib.MODID)
public class TestDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(),
                new TestLootInjectorProvider(event.getGenerator().getPackOutput(), event.getLookupProvider()));
    }
}
