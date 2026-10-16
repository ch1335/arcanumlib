package com.chen1335.arcanumlib.mixins;

import net.minecraft.util.random.Weight;
import net.minecraft.util.random.WeightedEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 权重是 {@code private final} 且没有 setter，而寻觅需要【原地】改它。
 *
 * <p>不能改成"权重变了就换个新实例"：{@code NaturalSpawner.canSpawnMobAt} 会重建一次刷怪表再
 * {@code contains} 比对，换了实例 identity 就对不上，稀有生物会被拒。
 */
@Mixin(WeightedEntry.IntrusiveBase.class)
public interface IntrusiveBaseWeightAccessor {

    @Mutable
    @Accessor("weight")
    void arcanumlib$setWeight(Weight weight);

    @Accessor("weight")
    Weight arcanumlib$getWeight();
}
