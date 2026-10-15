package com.chen1335.arcanumlib.modules.fishing;

import com.chen1335.arcanumlib.common.attachmentTypes.EntityData;
import com.chen1335.arcanumlib.modules.fishing.test.TestFishingHook;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.EventHooks;

import javax.annotation.Nullable;

public abstract class SimpleFishingRodItem extends FishingRodItem {

    public SimpleFishingRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        IFishingHook hook = EntityData.get(player).fishing;
        if (hook != null) {
            if (!level.isClientSide) {
                int damage = retrieve(player,stack);
                ItemStack original = stack.copy();
                stack.hurtAndBreak(damage, player, LivingEntity.getSlotForHand(hand));
                if (stack.isEmpty()) {
                    EventHooks.onPlayerDestroyItem(player, original, hand);
                }
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), getRetrieveSound(player, stack),
                    SoundSource.NEUTRAL, 1.0F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        } else {
            // 抛竿
            level.playSound(null, player.getX(), player.getY(), player.getZ(), getThrowSound(player, stack),
                    SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
            if (level instanceof ServerLevel serverLevel) {
                // 与原版一致：饵钓给的是"减少的等待时间（秒）"，本模组的 fishSpeed 用 tick 计
                float fishSpeed = EnchantmentHelper.getFishingTimeReduction(serverLevel, stack, player) * 20.0F;
                float fishLuck = EnchantmentHelper.getFishingLuckBonus(serverLevel, stack, player);
                serverLevel.addFreshEntity(createHook(player, stack, level, fishLuck, fishSpeed));
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            player.gameEvent(GameEvent.ITEM_INTERACT_START);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    protected int retrieve(Player player, ItemStack stack){
        IFishingHook hook = EntityData.get(player).fishing;
        if (hook != null) {
            return hook.retrieve(stack);
        }
        return 0;
    }

    protected SoundEvent getRetrieveSound(Player player, ItemStack stack) {
        return SoundEvents.FISHING_BOBBER_RETRIEVE;
    }

    protected SoundEvent getThrowSound(Player player, ItemStack stack) {
        return SoundEvents.FISHING_BOBBER_THROW;
    }

    protected abstract Entity createHook(Player player, @Nullable ItemStack rod, Level level, float fishLuck, float fishSpeed);
}
