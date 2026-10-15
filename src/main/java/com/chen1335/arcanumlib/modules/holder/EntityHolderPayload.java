package com.chen1335.arcanumlib.modules.holder;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.EntityType;

/**
 * EntityHolderItem 包装的数据：装的是哪个实体、要生成几只、以及可选的自定义 NBT（写法同 /summon 的 nbt 参数）。
 *
 * <p>数量必须放在这里而不是堆叠数上：作为战利品标记时物品是 **0 数量**（原版语义＝空），堆叠数永远是 0。
 *
 * <p>必须同时有 codec（数据包 / 存盘）与 stream codec（同步到客户端），否则客户端的自定义渲染读不到实体。
 */
public record EntityHolderPayload(EntityType<?> entity, CompoundTag nbt, int count) {
    public static final Codec<EntityHolderPayload> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("target").forGetter(EntityHolderPayload::entity),
            CompoundTag.CODEC.optionalFieldOf("nbt", new CompoundTag()).forGetter(EntityHolderPayload::nbt),
            Codec.INT.optionalFieldOf("count", 1).forGetter(EntityHolderPayload::count)
    ).apply(instance, EntityHolderPayload::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EntityHolderPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ENTITY_TYPE), EntityHolderPayload::entity,
            ByteBufCodecs.COMPOUND_TAG, EntityHolderPayload::nbt,
            ByteBufCodecs.VAR_INT, EntityHolderPayload::count,
            EntityHolderPayload::new);
}