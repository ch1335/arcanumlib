package com.chen1335.arcanumlib.modules.loot.lootInjector.selector;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;

/**
 * 池与条目的【静态】总权重计算，池条件与条目条件共用同一套口径。
 *
 * <p>选择器拿不到 LootContext，所以只算基础 weight：不含 {@code quality × 幸运}（按幸运 0）、
 * 不按条目条件过滤分支，且把 alternatives 的所有分支都累加（原版掷骰只走第一个条件通过的分支）。
 * 也就是"条件全满足、幸运为 0"时的值，适合做粗筛，不等于某次掷骰的实数。
 */
public final class PoolWeights {

    /** 整个池的总权重。 */
    public static int total(LootPool pool) {
        int total = 0;
        for (int i = 0; i < pool.entries.size(); i++) {
            total += of(pool.entries.get(i));
        }

        return total;
    }

    /** 单个条目的权重；复合条目（alternatives/sequence/group）自身没有 weight，权重都在叶子上，所以递归累加。 */
    public static int of(LootPoolEntryContainer entry) {
        if (entry instanceof LootPoolSingletonContainer singleton) {
            // 等价于 getWeight(luck = 0)：原版是在 expand 出来的 LootPoolEntry 上叠加 quality × 幸运再夹到 0
            return Math.max(singleton.weight, 0);
        }

        if (!(entry instanceof CompositeEntryBase composite)) {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < composite.children.size(); i++) {
            total += of(composite.children.get(i));
        }

        return total;
    }

    private PoolWeights() {
    }
}
