package com.chen1335.arcanumlib.modules.loot.lootEntry;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.function.Consumer;

/**
 * 被抽中一次就把所有孩子都产出，用于"同一个池里多个各自独立的概率条目"。
 * 原版 group/sequence 只是把权重摊平进候选列表，addRandomItem 仍按权重抽一个，会退化成"每次只出 1 个"；createItemStack 收到的是 Consumer，可以调多次。
 */
public class AllOfEntry extends LootPoolSingletonContainer {
    public static final MapCodec<AllOfEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            LootPoolEntries.CODEC.listOf().optionalFieldOf("children", List.of()).forGetter(AllOfEntry::children)
    ).and(LootPoolSingletonContainer.singletonFields(instance)).apply(instance, AllOfEntry::new));

    private final List<LootPoolEntryContainer> children;

    private AllOfEntry(List<LootPoolEntryContainer> children, int weight, int quality,
                       List<LootItemCondition> conditions, List<LootItemFunction> functions) {
        super(weight, quality, conditions, functions);
        this.children = children;
    }

    public List<LootPoolEntryContainer> children() {
        return this.children;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootEntryTypes.ALL_OF.get();
    }

    @Override
    public void createItemStack(Consumer<ItemStack> consumer, LootContext context) {
        for (LootPoolEntryContainer child : this.children) {
            child.expand(context, entry -> entry.createItemStack(consumer, context));
        }
    }

    public static Builder allOf(List<LootPoolEntryContainer.Builder<?>> children) {
        return new Builder(children.stream().map(LootPoolEntryContainer.Builder::build).toList());
    }

    /** 池里只有一个条目时权重没有意义，所以不给外部设置权重/品质的入口。 */
    public static class Builder extends LootPoolSingletonContainer.Builder<Builder> {
        private final List<LootPoolEntryContainer> children;

        private Builder(List<LootPoolEntryContainer> children) {
            this.children = children;
        }

        @Override
        protected Builder getThis() {
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return new AllOfEntry(this.children, this.weight, this.quality,
                    this.getConditions(), this.getFunctions());
        }
    }
}
