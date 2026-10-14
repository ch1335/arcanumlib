package com.chen1335.arcanumlib.modules.fishing;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;

public abstract class SimpleFishingHook extends Projectile implements IFishingHook {
    protected SimpleFishingHook(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }


}
