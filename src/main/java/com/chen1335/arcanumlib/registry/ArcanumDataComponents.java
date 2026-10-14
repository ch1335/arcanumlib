package com.chen1335.arcanumlib.registry;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.holder.EntityHolderPayload;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 全模组共用的数据组件注册表。
 *
 * <p>{@link #register} 同时接上 codec 与 stream codec：前者给数据包 / 存盘，后者给客户端同步
 * —— 自定义渲染、客户端逻辑要读的组件必须两个都给，否则服务端有数据、客户端是空。
 */
public final class ArcanumDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ArcanumLib.MODID);

    /** EntityHolderItem 里包装的那个实体（codec + 网络同步都接好）。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<EntityHolderPayload>> HELD_ENTITY =
            register("held_entity", EntityHolderPayload.CODEC, EntityHolderPayload.STREAM_CODEC);

    private ArcanumDataComponents() {
    }

    /** 通用注册：一次给齐持久化与网络同步。 */
    public static <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> register(
            String name, Codec<T> codec, StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        return COMPONENTS.register(name, () -> DataComponentType.<T>builder()
                .persistent(codec)
                .networkSynchronized(streamCodec)
                .build());
    }
}