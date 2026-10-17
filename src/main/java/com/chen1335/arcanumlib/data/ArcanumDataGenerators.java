package com.chen1335.arcanumlib.data;

import com.chen1335.arcanumlib.ArcanumLib;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * 本模组的数据生成入口。只有 runData 会触发，正常启动游戏不产生任何效果。
 */
@EventBusSubscriber(modid = ArcanumLib.MODID)
public class ArcanumDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(),
                new ArcanumAttributeTagsProvider(event.getGenerator().getPackOutput(), event.getLookupProvider(),
                        event.getExistingFileHelper()));
    }
}