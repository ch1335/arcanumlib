package com.chen1335.arcanumlib.kubejs.modules.bonusLoot;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * 脚本事件 {@code ArcanumLibEvents.modifyBonusLoot}：即将用额外战利品增幅时触发，可以改倍率或取消。
 *
 * <p>此时战利品表已经摇完第一次，额外那几份还没摇 —— 所以改倍率就是改最终结果；
 * 倍率 {@code 1.0} 表示「再摇一份」，改成 0 或 {@code event.cancel()} 就是不增幅。
 */
public class ModifyBonusLootKubeEvent implements KubeEvent {

    private final LootContext lootContext;
    private final ResourceLocation lootTableId;
    private final float originalRatio;
    private float ratio;
    private boolean canceled;

    public ModifyBonusLootKubeEvent(LootContext lootContext, ResourceLocation lootTableId, float ratio) {
        this.lootContext = lootContext;
        this.lootTableId = lootTableId;
        this.originalRatio = ratio;
        this.ratio = ratio;
    }

    /** 本次摇取的上下文：里面有实体、方块状态、工具等全部参数。 */
    public LootContext getLootContext() {
        return this.lootContext;
    }

    /** 正在产出战利品的表 id。 */
    public ResourceLocation getLootTableId() {
        return this.lootTableId;
    }

    /** 属性给出的原始倍率，用来对照自己改了多少。 */
    public float getOriginalRatio() {
        return this.originalRatio;
    }

    /** 当前倍率（可改）；1.0 表示再摇一份。 */
    public float getRatio() {
        return this.ratio;
    }

    /** 改倍率；设为 0 或负数等同于不增幅，超过 20 会被夹到 20。 */
    public void setRatio(float ratio) {
        this.ratio = ratio;
    }

    /** 取消这次增幅。 */
    public void cancel() {
        this.canceled = true;
    }

    public boolean isCanceled() {
        return this.canceled;
    }
}
