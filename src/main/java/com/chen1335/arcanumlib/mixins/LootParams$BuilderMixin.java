package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.modules.fishing.*;
import com.chen1335.arcanumlib.registry.ArcanumAttachments;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LootParams.Builder.class)
public class LootParams$BuilderMixin {
    @Shadow
    @Final
    private Map<LootContextParam<?>, Object> params;

    @Inject(method = "create", at = @At(value = "NEW", target = "(Lnet/minecraft/server/level/ServerLevel;Ljava/util/Map;Ljava/util/Map;F)Lnet/minecraft/world/level/storage/loot/LootParams;"))
    private void createParams(LootContextParamSet paramSet, CallbackInfoReturnable<LootParams> cir) {
        if (paramSet.equals(LootContextParamSets.FISHING)) {
            if (params.get(LootContextParams.THIS_ENTITY) instanceof IFishingHook hook && hook.getOwner() instanceof LivingEntity owner) {
                FishingHookData data = hook.getData(ArcanumAttachments.FISHING_HOOK_DATA);
                ItemStack bait = data.getBait();
                ItemStack rod = data.getRod();
                params.put(FishingExtraLootContextParams.FISH_BAIT, bait);
                if (rod.getItem() instanceof IFishingRod fishingRod) {
                    fishingRod.modifyLootParams(owner, hook, rod, params);
                    if (bait.getItem() instanceof IFishBait fishBait) {
                        fishBait.modifyLootParams(owner, hook,bait, params);
                    }
                }
            }
        }
    }
}
