package com.chen1335.arcanumlib.modules.loot.bonusLoot.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 即将用 {@code arcanumlib:bonus_loot} 属性增幅战利品时触发：可以改倍率，也可以整个取消。
 *
 * <p>此时战利品表已经摇完第一次，额外的那几份还没摇 —— 所以改倍率就是改最终结果。
 * 倍率 {@code 1.0} 表示「再摇一份」；改成 0 或取消就是不增幅。
 *
 * <p>只在「表符合前缀规则、倍率大于 0」时触发；调用点见 {@code modules.loot.bonusLoot.BonusLoot#append}。
 */
public class ModifyBonusLootEvent extends Event implements ICancellableEvent {

    private final LootContext lootContext;
    private final ResourceLocation lootTableId;
    private final float originalRatio;
    private float ratio;

    public ModifyBonusLootEvent(LootContext lootContext, ResourceLocation lootTableId, float ratio) {
        this.lootContext = lootContext;
        this.lootTableId = lootTableId;
        this.originalRatio = ratio;
        this.ratio = ratio;
    }

    /** 本次摇取的上下文：里面有实体、方块状态、工具等全部参数。 */
    public LootContext getLootContext() {
        return this.lootContext;
    }

    /** 正在产出战利品的表 id，也就是 {@code context.getQueriedLootTableId()}。 */
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

    /**
     * 触发事件并返回它自己，调用方从里面读最终倍率与取消状态。
     */
    public static ModifyBonusLootEvent post(LootContext lootContext, ResourceLocation lootTableId, float ratio) {
        ModifyBonusLootEvent event = new ModifyBonusLootEvent(lootContext, lootTableId, ratio);
        NeoForge.EVENT_BUS.post(event);

        return event;
    }
}
