package com.chen1335.arcanumlib.registry;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.common.attachmentTypes.EntityData;
import com.chen1335.arcanumlib.modules.fishing.FishingHookData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 实体附件的注册表（NeoForge {@code AttachmentType}），目前只放钓鱼钩那一个。
 *
 * <p>模板用法：
 * <ul>
 *   <li>换负载类型：把 {@code FishingHookData} 换成自己的类型，默认值给一个"空"实例
 *       （必须写成显式零参 lambda，否则会撞上 builder 的两个重载）；</li>
 *   <li>用 {@code INBTSerializable} 存盘：{@code AttachmentType.serializable(() -> new 你的类型())}（现在就是这种）；</li>
 *   <li>用 Codec 存盘：{@code AttachmentType.builder(...).serialize(你的 Codec)}；</li>
 *   <li>要同步给客户端：{@code .sync(StreamCodec)}（纯服务端逻辑不用）。</li>
 * </ul>
 *
 * <p>读写：{@code hook.getData(ArcanumAttachments.FISHING_HOOK_DATA)} /
 * {@code hook.setData(...)} / {@code hook.removeData(...)}（实体都实现了 {@code IAttachmentHolder}）。
 */
public final class ArcanumAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, ArcanumLib.MODID);

    /** 钓鱼钩上的附件数据；存盘走 {@link FishingHookData#serializeNBT} / {@link FishingHookData#deserializeNBT}。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<FishingHookData>> FISHING_HOOK_DATA =
            ATTACHMENT_TYPES.register("fishing_hook_data", () -> AttachmentType.serializable(FishingHookData::new)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EntityData>> PLAYER_DATA =
            ATTACHMENT_TYPES.register("player_data", () -> AttachmentType.serializable(EntityData::new)
                    .build());

    private ArcanumAttachments() {
    }
}