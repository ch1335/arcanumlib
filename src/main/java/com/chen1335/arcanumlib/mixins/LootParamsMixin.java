package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.mixinsAPI.ILootParamsExtension;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LootParams.class)
public class LootParamsMixin implements ILootParamsExtension {
    @Unique
    private LootContextParamSet arcanumlib$paramSet;


    public LootContextParamSet arcanumlib$getParamSet() {
        return arcanumlib$paramSet;
    }

    public void arcanumlib$setParamSet(LootContextParamSet arcanumlib$paramSet) {
        this.arcanumlib$paramSet = arcanumlib$paramSet;
    }
}
