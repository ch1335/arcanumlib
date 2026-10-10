package com.chen1335.arcanumlib.modules.loot.lootInjector;

import com.chen1335.arcanumlib.ArcanumLib;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

/**
 * 战利品注入器的接线：注册读取监听器，数据全部加载完之后统一应用。
 * 不用 LootTableLoadEvent：那是注册表反序列化过程中逐表触发的，那时自己的定义还不一定读完。
 */
@EventBusSubscriber(modid = ArcanumLib.MODID)
public class LootInjectorEvents {

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new LootInjectorReloadListener());
    }

    /** 必须用 ServerStartingEvent + HIGHEST：ALI 在 ServerStartingEvent 默认优先级里读表建界面数据，晚一步就永远显示不到。 */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerStarting(ServerStartingEvent event) {
        LootInjectorApplier.applyAll(event.getServer());
    }
}
