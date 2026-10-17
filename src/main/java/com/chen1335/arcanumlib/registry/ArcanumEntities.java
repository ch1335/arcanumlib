package com.chen1335.arcanumlib.registry;

import com.chen1335.arcanumlib.ArcanumLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 全模组共用的实体注册表（任何模块的实体都注册到这里，主类只需 register 一次）。
 *
 * <p>尺寸与同步参数照抄原版 {@code fishing_bobber}：0.25×0.25、clientTrackingRange 4、updateInterval 5。
 */
public final class ArcanumEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ArcanumLib.MODID);

    private ArcanumEntities() {
    }
}
