package com.chen1335.arcanumlib.events;

import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 用附魔等级改战利品时触发（数量加成、概率加成那类函数与谓词），可以改掉等级。
 * 想拿实体就从 context 里取 THIS_ENTITY / ATTACKING_ENTITY。在 NeoForge.EVENT_BUS 上监听。
 */
public class EnchantedLootEvent extends Event {
    private final Holder<Enchantment> enchantment;
    private final LootContext context;
    private int level;

    public EnchantedLootEvent(Holder<Enchantment> enchantment, LootContext context, int level) {
        this.enchantment = enchantment;
        this.context = context;
        this.level = level;
    }

    public Holder<Enchantment> getEnchantment() {
        return this.enchantment;
    }

    /** 战利品上下文；各触发点都来自战利品，所以不会是 null。 */
    public LootContext getContext() {
        return this.context;
    }

    /** 原本要返回的等级。 */
    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public static int post(Holder<Enchantment> enchantment, LootContext context, int level) {
        EnchantedLootEvent event = new EnchantedLootEvent(enchantment, context, level);
        NeoForge.EVENT_BUS.post(event);
        return event.getLevel();
    }
}
