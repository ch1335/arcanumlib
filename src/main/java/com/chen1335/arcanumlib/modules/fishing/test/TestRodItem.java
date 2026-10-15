package com.chen1335.arcanumlib.modules.fishing.test;

import com.chen1335.arcanumlib.modules.fishing.SimpleFishingRodItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * 测试鱼竿：逻辑照抄原版 {@link FishingRodItem#use}，只把"找钩子"换成找我们自己的 {@link TestFishingHook}。
 *
 * <p>为什么不能沿用原版：原版把钩子存在 {@code Player.fishing} 上，而那个字段的类型写死成原版
 * {@code FishingHook}，自定义钩子放不进去，所以这里按"同世界的、以本玩家为持有者的钩子"来查。
 */
public class TestRodItem extends SimpleFishingRodItem {

    public TestRodItem(Item.Properties properties) {
        super(properties);
    }

    protected Entity createHook(Player player, @Nullable ItemStack rod, Level level, float fishLuck, float fishSpeed) {
        return new TestFishingHook(player, rod, level, fishLuck, fishSpeed);
    }
}