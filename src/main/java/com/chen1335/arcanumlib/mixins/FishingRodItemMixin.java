package com.chen1335.arcanumlib.mixins;

import com.chen1335.arcanumlib.modules.fishing.IFishingRod;
import net.minecraft.world.item.FishingRodItem;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FishingRodItem.class)
public class FishingRodItemMixin implements IFishingRod {
}
