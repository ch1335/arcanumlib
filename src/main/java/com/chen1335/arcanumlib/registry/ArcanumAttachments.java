package com.chen1335.arcanumlib.registry;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.common.attachmentTypes.EntityData;
import com.chen1335.arcanumlib.common.attachmentTypes.PlayerData;
import com.chen1335.arcanumlib.modules.fishing.FishingHookData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 实体附件注册表（NeoForge {@code AttachmentType}）。
 *
 * <ul>
 *   <li>{@code fishing_hook_data}：鱼钩上的竿与饵（{@link FishingHookData}，要存盘，走 INBTSerializable）；</li>
 *   <li>{@code player_data}：玩家身上"当前那根鱼钩"的指针（{@link EntityData}），不存盘——钩子本身就不存盘，
 *       指针跟着失效才是对的。</li>
 * </ul>
 *
 * <p>要用 Codec 存盘的附件：{@code AttachmentType.builder(() -> new 你的类型()).serialize(你的 Codec)}；
 * 要同步给客户端再链一个 {@code .sync(StreamCodec)}（纯服务端逻辑不用）。
 *
 * <p>读写：{@code hook.getData(ArcanumAttachments.FISHING_HOOK_DATA)} / {@code setData} / {@code removeData}
 * （实体都实现了 {@code IAttachmentHolder}）。
 */
public final class ArcanumAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ArcanumLib.MODID);

    /** 钓鱼钩上的附件数据；存盘走 {@link FishingHookData#serializeNBT} / {@link FishingHookData#deserializeNBT}。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FishingHookData>> FISHING_HOOK_DATA =
            ATTACHMENT_TYPES.register("fishing_hook_data", () -> AttachmentType.serializable(FishingHookData::new)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EntityData>> ENTITY_DATA =
            ATTACHMENT_TYPES.register("entity_data", () -> AttachmentType.builder(EntityData::new)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerData>> PLAYER_DATA =
            ATTACHMENT_TYPES.register("player_data", () -> AttachmentType.builder(PlayerData::new)
                    .build());


    private ArcanumAttachments() {
    }
}