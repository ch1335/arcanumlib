package com.chen1335.arcanumlib.modules.fishing.client;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.registry.ArcanumEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 客户端渲染注册：测试鱼钩直接用"照抄原版浮标"的那套渲染器。
 */
@EventBusSubscriber(modid = ArcanumLib.MODID, value = Dist.CLIENT)
public final class FishingClientEvents {
    private FishingClientEvents() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ArcanumEntities.TEST_FISHING_HOOK.get(), SimpleFishingHookRenderer::new);
    }
}