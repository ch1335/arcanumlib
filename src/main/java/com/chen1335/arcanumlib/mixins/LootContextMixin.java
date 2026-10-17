package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.mixinsAPI.ILootContextExtension;
import com.chen1335.arcanumlib.mixinsAPI.ILootParamsExtension;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LootContext.class)
public class LootContextMixin implements ILootContextExtension {

    @Shadow
    @Final
    private LootParams params;

    @Override
    public LootContextParamSet arcanumlib$getParamSet() {
        return ((ILootParamsExtension) params).arcanumlib$getParamSet();
    }
}
