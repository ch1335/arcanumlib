package com.chen1335.arcanumlib.modules.loot.lootInjector.selector;

import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOps;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomPoolSelectorEvent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootPool;

import java.util.List;

/**
 * 池选择器：把池和它的下标传进来就地判断。带 index 是因为"按位置选"和"按内容选"都要用，两者可叠加。
 * 读取池内内容依赖 AT 打开的 {@code LootPool.entries}。
 */
public interface PoolSelector {

    Codec<PoolSelector> CODEC = LootInjectorOps.POOL_SELECTORS.byNameCodec()
            .dispatch("type", PoolSelector::getType, PoolSelectorType::codec);

    PoolSelectorType getType();

    boolean test(int index, LootPool pool);

    /** 匹配所有池。 */
    record Any() implements PoolSelector {
        @Override
        public PoolSelectorType getType() {
            return LootInjectorOps.POOL_ANY.get();
        }
        public static final MapCodec<Any> CODEC = MapCodec.unit(Any::new);
        @Override
        public boolean test(int index, LootPool pool) {
            return true;
        }
    }

    /** 按下标匹配，JSON 里从 0 开始。 */
    record ByIndex(int index) implements PoolSelector {
        @Override
        public PoolSelectorType getType() {
            return LootInjectorOps.POOL_BY_INDEX.get();
        }
        public static final MapCodec<ByIndex> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("index").forGetter(ByIndex::index)).apply(instance, ByIndex::new));
        @Override
        public boolean test(int index, LootPool pool) {
            return this.index == index;
        }
    }

    /** 按池名匹配；无名池永远不匹配。 */
    record ByName(String name) implements PoolSelector {
        @Override
        public PoolSelectorType getType() {
            return LootInjectorOps.POOL_BY_NAME.get();
        }
        public static final MapCodec<ByName> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("name").forGetter(ByName::name)).apply(instance, ByName::new));
        @Override
        public boolean test(int index, LootPool pool) {
            return this.name.equals(pool.getName());
        }
    }

    /** 池里任意一个条目满足条件即算命中。 */
    record HasEntry(EntrySelector entrySelector) implements PoolSelector {
        @Override
        public PoolSelectorType getType() {
            return LootInjectorOps.POOL_HAS_ENTRY.get();
        }
        public static final MapCodec<HasEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                EntrySelector.CODEC.fieldOf("entrySelector").forGetter(HasEntry::entrySelector))
                .apply(instance, HasEntry::new));
        @Override
        public boolean test(int index, LootPool pool) {
            for (int i = 0; i < pool.entries.size(); i++) {
                if (this.entrySelector.test(i, pool.entries.get(i), pool)) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * 按池的【总权重】是否落在闭区间 [min, max] 内匹配。
     *
     * <p>只算静态基础权重，和原版掷骰时的实数有出入，因为选择器拿不到 LootContext：
     * ① getWeight(luck) 还要加 quality × 幸运，这里按幸运 0 算；
     * ② 原版经 expand(context) 展开、会按条目条件过滤分支，这里不过滤；
     * ③ alternatives 掷骰时只走第一个条件通过的分支，这里把所有分支都累加（静态上界）。
     */
    record TotalWeightInRange(int min, int max) implements PoolSelector {
        @Override
        public PoolSelectorType getType() {
            return LootInjectorOps.POOL_TOTAL_WEIGHT.get();
        }
        public static final MapCodec<TotalWeightInRange> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.optionalFieldOf("min", 0).forGetter(TotalWeightInRange::min),
                Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(TotalWeightInRange::max))
                .apply(instance, TotalWeightInRange::new));
        @Override
        public boolean test(int index, LootPool pool) {
            int total = PoolWeights.total(pool);

            return total >= this.min && total <= this.max;
        }

    }
    /** 取反。 */
    record Not(PoolSelector child) implements PoolSelector {
        @Override
        public PoolSelectorType getType() {
            return LootInjectorOps.POOL_NOT.get();
        }
        public static final MapCodec<Not> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PoolSelector.CODEC.fieldOf("condition").forGetter(Not::child)).apply(instance, Not::new));
        @Override
        public boolean test(int index, LootPool pool) {
            return !this.child.test(index, pool);
        }
    }

    /** 任一满足。 */
    record AnyOf(List<PoolSelector> children) implements PoolSelector {
        @Override
        public PoolSelectorType getType() {
            return LootInjectorOps.POOL_ANY_OF.get();
        }
        public static final MapCodec<AnyOf> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PoolSelector.CODEC.listOf().fieldOf("conditions").forGetter(AnyOf::children))
                .apply(instance, AnyOf::new));
        @Override
        public boolean test(int index, LootPool pool) {
            return this.children.stream().anyMatch(child -> child.test(index, pool));
        }
    }

    /** 全部满足。 */
    record AllOf(List<PoolSelector> children) implements PoolSelector {
        @Override
        public PoolSelectorType getType() {
            return LootInjectorOps.POOL_ALL_OF.get();
        }
        public static final MapCodec<AllOf> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PoolSelector.CODEC.listOf().fieldOf("conditions").forGetter(AllOf::children))
                .apply(instance, AllOf::new));
        @Override
        public boolean test(int index, LootPool pool) {
            return this.children.stream().allMatch(child -> child.test(index, pool));
        }
    }
    /**
     * 自定义选择器：json 里只给一个 id，判断交给监听 CustomPoolSelectorEvent 的代码。
     * 每次判定都会发事件（注入器 × 表规模），监听者要自己控制开销。
     */
    record Custom(String id) implements PoolSelector {
        public static final MapCodec<Custom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("custom_id").forGetter(Custom::id)).apply(instance, Custom::new));

        @Override
        public PoolSelectorType getType() {
            return LootInjectorOps.CUSTOM_POOL.get();
        }

        @Override
        public boolean test(int index, LootPool pool) {
            return CustomPoolSelectorEvent.post(this.id, index, pool);
        }
    }
}
