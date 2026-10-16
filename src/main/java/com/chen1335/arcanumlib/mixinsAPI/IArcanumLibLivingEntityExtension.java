package com.chen1335.arcanumlib.mixinsAPI;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;

public interface IArcanumLibLivingEntityExtension {
    void arcanumlib$dropCustomDeathLoot(ServerLevel level, DamageSource damageSource, boolean recentlyHit);

    int arcanumlib$lastHurtByPlayerTime();
}
