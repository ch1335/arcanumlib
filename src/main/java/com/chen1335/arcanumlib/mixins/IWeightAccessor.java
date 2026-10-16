package com.chen1335.arcanumlib.mixins;

import net.minecraft.util.random.Weight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Weight.class)
public interface IWeightAccessor {
    @Mutable
    @Accessor("value")
    void arcanumlib$setValue(int value);
}
