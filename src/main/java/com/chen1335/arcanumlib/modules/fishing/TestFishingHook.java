package com.chen1335.arcanumlib.modules.fishing;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class TestFishingHook extends SimpleFishingHook{
    protected TestFishingHook(EntityType<? extends Projectile> entityType, Player player, @Nullable ItemStack rod, Level level, float fishLuck, float fishSpeed) {
        super(entityType, player, rod, level, fishLuck, fishSpeed);
    }
}
