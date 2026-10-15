package com.chen1335.arcanumlib.modules.fishing;

import com.chen1335.arcanumlib.modules.fishing.fishMediaTypes.IFishMediaType;
import com.chen1335.arcanumlib.registry.ArcanumEntities;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

/**
 * 测试鱼钩：把 {@link SimpleFishingHook} 变成可实例化的实体，用来验证整套钓鱼流程。
 *
 * <p>行为全部沿用基类（水体 = 水、战利品 = 原版钓鱼表、渲染 = 原版浮标那套），
 * 所以它同时也可以当作"照抄原版"的参考实现。
 */
public class TestFishingHook extends SimpleFishingHook {
    /** 实体工厂用的构造器（{@code EntityType.Builder.of(TestFishingHook::new, ...)}）。 */
    public TestFishingHook(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    /** 抛竿：位置、初速度与鱼竿记录都由基类构造器完成（内部会调 castFrom）。 */
    public TestFishingHook(Player player, Level level, float fishLuck, float fishSpeed) {
        super(ArcanumEntities.TEST_FISHING_HOOK.get(), player, level, fishLuck, fishSpeed);
    }

    @Override
    public IFishMediaType getFishMediaType() {
        return FishMediaTypes.LAVA;
    }
}