package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.modules.fishing.IFishingHook;
import com.chen1335.arcanumlib.registry.ArcanumAttachments;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin extends Projectile implements IFishingHook {

    protected FishingHookMixin(EntityType<? extends Projectile> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/FishingHook;setOwner(Lnet/minecraft/world/entity/Entity;)V"))
    private void setOwner(Player player, Level level, int luck, int lureSpeed, CallbackInfo ci) {
        ItemStack itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (itemInHand.getItem() instanceof FishingRodItem) {
            this.getData(ArcanumAttachments.FISHING_HOOK_DATA).setRod(itemInHand);
        } else {
            itemInHand = player.getItemInHand(InteractionHand.OFF_HAND);
            if (itemInHand.getItem() instanceof FishingRodItem) {
                this.getData(ArcanumAttachments.FISHING_HOOK_DATA).setRod(itemInHand);
            }
        }
    }
}
