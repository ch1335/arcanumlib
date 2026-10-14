package com.chen1335.arcanumlib.registry;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.fishing.TestFishingHook;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 全模组共用的实体注册表（任何模块的实体都注册到这里，主类只需 register 一次）。
 *
 * <p>尺寸与同步参数照抄原版 {@code fishing_bobber}：0.25×0.25、clientTrackingRange 4、updateInterval 5。
 * 不要 {@code noSummon()}：测试时要能 {@code /summon arcanumlib:test_fishing_hook} 单独看渲染。
 */
public final class ArcanumEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ArcanumLib.MODID);

    /** 测试鱼钩：驱动 {@code SimpleFishingHook} 用，不存盘（同原版浮标）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<TestFishingHook>> TEST_FISHING_HOOK =
            ENTITY_TYPES.register("test_fishing_hook", () -> EntityType.Builder
                    .<TestFishingHook>of(TestFishingHook::new, MobCategory.MISC)
                    .noSave()
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(5)
                    .build("arcanumlib:test_fishing_hook"));

    private ArcanumEntities() {
    }
}