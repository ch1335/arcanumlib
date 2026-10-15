package com.chen1335.arcanumlib.common.attachmentTypes;

import com.chen1335.arcanumlib.modules.attribute.AttributeAdjustment.AdjustmentData;
import com.chen1335.arcanumlib.modules.fishing.IFishingHook;
import com.chen1335.arcanumlib.registry.ArcanumAttachments;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/**
 * 挂在实体（目前是玩家）上的数据：记着这个玩家当前的那根鱼钩。
 *
 * <p>不存盘：钩子本身就不存盘，指针跟着一起失效才是对的（换维度 / 重登后自然为空）。
 */
public class EntityData {
    /**
     * 玩家当前的鱼钩；没在钓鱼时为 null。
     */
    @Nullable
    public IFishingHook fishing;

    public final AdjustmentData adjustmentData = new AdjustmentData();

    public static EntityData get(Entity entity) {
        return entity.getData(ArcanumAttachments.PLAYER_DATA);
    }
}
