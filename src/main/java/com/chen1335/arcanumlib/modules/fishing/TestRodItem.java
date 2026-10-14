package com.chen1335.arcanumlib.modules.fishing;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.EventHooks;

/**
 * 测试鱼竿：逻辑照抄原版 {@link FishingRodItem#use}，只把"找钩子"换成找我们自己的 {@link TestFishingHook}。
 *
 * <p>为什么不能沿用原版：原版把钩子存在 {@code Player.fishing} 上，而那个字段的类型写死成原版
 * {@code FishingHook}，自定义钩子放不进去，所以这里按"同世界的、以本玩家为持有者的钩子"来查。
 */
public class TestRodItem extends FishingRodItem {
    /** 搜索半径：原版靠字段引用，没有距离限制，这里给个足够大的范围。 */
    private static final double HOOK_SEARCH_RADIUS = 256.0D;

    public TestRodItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        TestFishingHook hook = findOwnedHook(level, player);

        if (hook != null) {
            // 收竿：只有服务端能改物品与实体状态
            if (!level.isClientSide) {
                int damage = hook.retrieve(stack);
                ItemStack original = stack.copy();
                stack.hurtAndBreak(damage, player, LivingEntity.getSlotForHand(hand));
                if (stack.isEmpty()) {
                    EventHooks.onPlayerDestroyItem(player, original, hand);
                }
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE,
                    SoundSource.NEUTRAL, 1.0F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        } else {
            // 抛竿
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW,
                    SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            if (level instanceof ServerLevel serverLevel) {
                // 与原版一致：饵钓给的是"减少的等待时间（秒）"，本模组的 fishSpeed 用 tick 计
                float fishSpeed = (float) (EnchantmentHelper.getFishingTimeReduction(serverLevel, stack, player) * 20.0F);
                float fishLuck = EnchantmentHelper.getFishingLuckBonus(serverLevel, stack, player);
                serverLevel.addFreshEntity(new TestFishingHook(player, serverLevel, fishLuck, fishSpeed));
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            player.gameEvent(GameEvent.ITEM_INTERACT_START);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /** 找出本玩家自己的测试鱼钩（同一世界、持有者是本人）。 */
    private static TestFishingHook findOwnedHook(Level level, Player player) {
        return level.getEntitiesOfClass(TestFishingHook.class, player.getBoundingBox().inflate(HOOK_SEARCH_RADIUS))
                .stream()
                .filter(candidate -> candidate.getOwner() == player)
                .findFirst()
                .orElse(null);
    }
}