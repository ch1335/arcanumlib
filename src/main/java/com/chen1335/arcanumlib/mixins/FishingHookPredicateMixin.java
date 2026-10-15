package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.modules.fishing.IFishingHook;
import net.minecraft.advancements.critereon.FishingHookPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(FishingHookPredicate.class)
public abstract class FishingHookPredicateMixin {

    @Shadow
    public abstract Optional<Boolean> inOpenWater();

    @Inject(method = "matches", at = @At("RETURN"), cancellable = true)
    private void matches(Entity entity, ServerLevel level, Vec3 position, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            if (entity instanceof IFishingHook fishingHook) {
                cir.setReturnValue(this.inOpenWater().isEmpty() || this.inOpenWater().get() == fishingHook.isOpenWaterFishing());
            }
        }
    }
}
