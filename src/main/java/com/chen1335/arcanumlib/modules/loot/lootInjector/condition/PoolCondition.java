package com.chen1335.arcanumlib.modules.loot.lootInjector.condition;

import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOps;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomPoolConditionEvent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.storage.loot.LootPool;

import java.util.List;

/**
 * 池选择器：把池和它的下标传进来就地判断。带 index 是因为"按位置选"和"按内容选"都要用，两者可叠加。
 * 读取池内内容依赖 AT 打开的 {@code LootPool.entries}。
 */
public interface PoolCondition {

    Codec<PoolCondition> CODEC = LootInjectorOps.POOL_CONDITIONS.byNameCodec()
            .dispatch("type", PoolCondition::getType, PoolConditionType::codec);

    PoolConditionType getType();

    boolean test(int index, LootPool pool);

    /** 匹配所有池。 */
    record Any() implements PoolCondition {
        @Override
        public PoolConditionType getType() {
            return LootInjectorOps.POOL_ANY.get();
        }
        public static final MapCodec<Any> CODEC = MapCodec.unit(Any::new);
        @Override
        public boolean test(int index, LootPool pool) {
            return true;
        }
    }

    /** 按下标匹配，JSON 里从 0 开始。 */
    record ByIndex(int index) implements PoolCondition {
        @Override
        public PoolConditionType getType() {
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
    record ByName(String name) implements PoolCondition {
        @Override
        public PoolConditionType getType() {
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
    record HasEntry(EntryCondition entryCondition) implements PoolCondition {
        @Override
        public PoolConditionType getType() {
            return LootInjectorOps.POOL_HAS_ENTRY.get();
        }
        public static final MapCodec<HasEntry> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                EntryCondition.CODEC.fieldOf("entryCondition").forGetter(HasEntry::entryCondition))
                .apply(instance, HasEntry::new));
        @Override
        public boolean test(int index, LootPool pool) {
            for (int i = 0; i < pool.entries.size(); i++) {
                if (this.entryCondition.test(i, pool.entries.get(i))) {
                    return true;
                }
            }

            return false;
        }
    }

    /** 取反。 */
    record Not(PoolCondition child) implements PoolCondition {
        @Override
        public PoolConditionType getType() {
            return LootInjectorOps.POOL_NOT.get();
        }
        public static final MapCodec<Not> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PoolCondition.CODEC.fieldOf("condition").forGetter(Not::child)).apply(instance, Not::new));
        @Override
        public boolean test(int index, LootPool pool) {
            return !this.child.test(index, pool);
        }
    }

    /** 任一满足。 */
    record AnyOf(List<PoolCondition> children) implements PoolCondition {
        @Override
        public PoolConditionType getType() {
            return LootInjectorOps.POOL_ANY_OF.get();
        }
        public static final MapCodec<AnyOf> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PoolCondition.CODEC.listOf().fieldOf("conditions").forGetter(AnyOf::children))
                .apply(instance, AnyOf::new));
        @Override
        public boolean test(int index, LootPool pool) {
            return this.children.stream().anyMatch(child -> child.test(index, pool));
        }
    }

    /** 全部满足。 */
    record AllOf(List<PoolCondition> children) implements PoolCondition {
        @Override
        public PoolConditionType getType() {
            return LootInjectorOps.POOL_ALL_OF.get();
        }
        public static final MapCodec<AllOf> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                PoolCondition.CODEC.listOf().fieldOf("conditions").forGetter(AllOf::children))
                .apply(instance, AllOf::new));
        @Override
        public boolean test(int index, LootPool pool) {
            return this.children.stream().allMatch(child -> child.test(index, pool));
        }
    }
    /**
     * 自定义选择器：json 里只给一个 id，判断交给监听 CustomPoolConditionEvent 的代码。
     * 每次判定都会发事件（注入器 × 表规模），监听者要自己控制开销。
     */
    record Custom(String id) implements PoolCondition {
        public static final MapCodec<Custom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("custom_id").forGetter(Custom::id)).apply(instance, Custom::new));

        @Override
        public PoolConditionType getType() {
            return LootInjectorOps.CUSTOM_POOL.get();
        }

        @Override
        public boolean test(int index, LootPool pool) {
            return CustomPoolConditionEvent.post(this.id, index, pool);
        }
    }
}
