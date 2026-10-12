package com.chen1335.arcanumlib.modules.loot.bonusLoot.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 判断「这张战利品表要不要吃 {@code arcanumlib:bonus_loot} 增幅」时触发。
 *
 * <p><b>本事件优先于配置</b>：{@link #getResult()} 的初值是配置里的前缀规则结果
 * （{@link #isConfigAllowed()}），但监听者可以覆盖它 —— 配置里没配的表也能强制放行，
 * 配置里配了的表也能强制拒绝；没有任何监听者改动时就按配置走。
 *
 * <p>触发点在 {@link ModifyBonusLootEvent} 之前：先决定要不要增幅，再决定倍率。
 * 每次「整表的顶层摇取」都会触发一次，包括配置里没匹配上的表。
 */
public class BonusLootCheckEvent extends Event {

    private final LootContext lootContext;
    private final ResourceLocation lootTableId;
    private final boolean configAllowed;
    private boolean result;

    public BonusLootCheckEvent(LootContext lootContext, ResourceLocation lootTableId, boolean configAllowed) {
        this.lootContext = lootContext;
        this.lootTableId = lootTableId;
        this.configAllowed = configAllowed;
        this.result = configAllowed;
    }

    /** 本次摇取的上下文：里面有实体、方块状态、工具等全部参数。 */
    public LootContext getLootContext() {
        return this.lootContext;
    }

    /** 正在产出战利品的表 id，也就是 {@code context.getQueriedLootTableId()}。 */
    public ResourceLocation getLootTableId() {
        return this.lootTableId;
    }

    /** 配置里的前缀规则给出的结果，用来对照自己改了什么。 */
    public boolean isConfigAllowed() {
        return this.configAllowed;
    }

    /** 最终要不要增幅；初值等于 {@link #isConfigAllowed()}。 */
    public boolean getResult() {
        return this.result;
    }

    /** 改判定结果，覆盖配置。 */
    public void setResult(boolean result) {
        this.result = result;
    }

    /** 强制放行：配置里没配的表也吃增幅。 */
    public void allow() {
        this.result = true;
    }

    /** 强制拒绝：配置里配了的表也不吃增幅。 */
    public void deny() {
        this.result = false;
    }

    /** 触发事件并返回它自己，调用方用 {@link #getResult()} 拿最终判定。 */
    public static BonusLootCheckEvent post(LootContext lootContext, ResourceLocation lootTableId, boolean configAllowed) {
        BonusLootCheckEvent event = new BonusLootCheckEvent(lootContext, lootTableId, configAllowed);
        NeoForge.EVENT_BUS.post(event);

        return event;
    }
}
