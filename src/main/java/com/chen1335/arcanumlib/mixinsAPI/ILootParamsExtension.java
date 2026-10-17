package com.chen1335.arcanumlib.mixinsAPI;

import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;

public interface ILootParamsExtension {
    LootContextParamSet arcanumlib$getParamSet();

    void arcanumlib$setParamSet(LootContextParamSet arcanumlib$paramSet);
}
