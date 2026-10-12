package com.chen1335.arcanumlib.kubejs.modules.bonusLoot;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * 脚本事件 {@code ArcanumLibEvents.bonusLootCheck}：判断某张战利品表要不要吃额外战利品增幅。
 *
 * <p><b>优先级高于配置</b>：不表态（不返回、也不调 {@code setResult}）就按配置的前缀规则走；
 * 返回 {@code true}（或 {@code event.allow()}）强制放行，返回 {@code false}（或 {@code event.deny()}）强制拒绝。
 */
public class BonusLootCheckKubeEvent implements KubeEvent {

    private final LootContext lootContext;
    private final ResourceLocation lootTableId;
    private final boolean configAllowed;
    private Boolean result;

    public BonusLootCheckKubeEvent(LootContext lootContext, ResourceLocation lootTableId, boolean configAllowed) {
        this.lootContext = lootContext;
        this.lootTableId = lootTableId;
        this.configAllowed = configAllowed;
    }

    /** 本次摇取的上下文：里面有实体、方块状态、工具等全部参数。 */
    public LootContext getLootContext() {
        return this.lootContext;
    }

    /** 正在产出战利品的表 id。 */
    public ResourceLocation getLootTableId() {
        return this.lootTableId;
    }

    /** 配置前缀规则给出的结果，用来对照自己要不要改。 */
    public boolean isConfigAllowed() {
        return this.configAllowed;
    }

    /** 脚本给出的判定；{@code null} 表示不表态，按配置走。 */
    public Boolean getResult() {
        return this.result;
    }

    /** 设定判定；传 {@code null} 等于不表态。 */
    public void setResult(Boolean result) {
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
}
