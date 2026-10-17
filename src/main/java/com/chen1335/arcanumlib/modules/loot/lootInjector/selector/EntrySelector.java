package com.chen1335.arcanumlib.modules.loot.lootInjector.selector;

import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOps;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomEntrySelectorEvent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.*;

import java.util.List;
import java.util.Optional;

/**
 * 条目选择器：把条目和它在池内的下标传进来就地判断。读条目内容全靠 AT（物品/标签/权重在单例基类，条件在容器基类）。
 * 复合条目（alternatives/sequence/group）只看得到 children，不递归。
 */
public interface EntrySelector {

    Codec<EntrySelector> CODEC = LootInjectorOps.ENTRY_SELECTORS.byNameCodec()
            .dispatch("type", EntrySelector::getType, EntrySelectorType::codec);

    EntrySelectorType getType();

    boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries);

    /** 匹配所有条目。 */
    record Any() implements EntrySelector {
        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.ENTRY_ANY.get();
        }
        public static final MapCodec<Any> CODEC = MapCodec.unit(Any::new);
        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            return true;
        }
    }

    /** 按池内下标匹配。 */
    record ByIndex(int index) implements EntrySelector {
        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.ENTRY_BY_INDEX.get();
        }
        public static final MapCodec<ByIndex> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("index").forGetter(ByIndex::index)).apply(instance, ByIndex::new));
        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            return this.index == index;
        }
    }

    /** 匹配某个物品条目。 */
    record IsItem(ResourceLocation item) implements EntrySelector {
        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.ENTRY_IS_ITEM.get();
        }
        public static final MapCodec<IsItem> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(IsItem::item)).apply(instance, IsItem::new));
        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            if (!(entry instanceof LootItem lootItem)) {
                return false;
            }

            return lootItem.item.unwrapKey().map(ResourceKey::location).map(this.item::equals).orElse(false);
        }
    }

    /** 匹配某个物品标签条目。 */
    record IsTag(TagKey<Item> tag) implements EntrySelector {
        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.ENTRY_IS_TAG.get();
        }
        public static final MapCodec<IsTag> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(IsTag::tag)).apply(instance, IsTag::new));
        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            return entry instanceof TagEntry tagEntry && tagEntry.tag.equals(this.tag);
        }
    }

    /**
    * 匹配"物品本身属于某个标签"的物品条目。和 {@link IsTag} 的区别：那个匹配 {@code minecraft:tag} 条目、比标签本身；
    * 这个匹配写死具体物品的 {@code minecraft:item} 条目、按标签判断其物品是否被收录。
    */
    record IsItemWithTag(TagKey<Item> tag) implements EntrySelector {
        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.ENTRY_IS_ITEM_WITH_TAG.get();
        }
        public static final MapCodec<IsItemWithTag> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(IsItemWithTag::tag))
                .apply(instance, IsItemWithTag::new));
        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            return entry instanceof LootItem lootItem && lootItem.item.is(this.tag);
        }
    }

    /** 按条目类型匹配，例如 {@code minecraft:item}、{@code minecraft:alternatives}。 */
    record IsType(ResourceLocation entryType) implements EntrySelector {
        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.ENTRY_IS_TYPE.get();
        }
        public static final MapCodec<IsType> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("entryType").forGetter(IsType::entryType)).apply(instance, IsType::new));
        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            return this.entryType.equals(BuiltInRegistries.LOOT_POOL_ENTRY_TYPE.getKey(entry.getType()));
        }
    }

    /**
 * 条目权重是否落在闭区间 [min, max] 内。两个字段都可省略（min 默认 0、max 默认 Integer.MAX_VALUE）。
 * <p>权重口径与 {@link PoolWeights} 一致：单例条目取它的 weight（按 0 夹下限），复合条目按子树累加。
     */
    record WeightInRange(int min, int max) implements EntrySelector {
        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.ENTRY_WEIGHT_IN_RANGE.get();
        }
        public static final MapCodec<WeightInRange> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.optionalFieldOf("min", 0).forGetter(WeightInRange::min),
                Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(WeightInRange::max))
                .apply(instance, WeightInRange::new));
        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            int weight = PoolWeights.of(entry);

            return weight >= this.min && weight <= this.max;
        }
    }

    /**
    * 该条目在【所属池总权重】里占的比例是否落在闭区间 [min, max] 内（0~1）。口径与
    * {@link PoolSelector.TotalWeightInRange} 一致：都不含 quality × 幸运、也不按条件过滤分支。
    */
    record WeightShareInRange(float min, float max) implements EntrySelector {
        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.ENTRY_WEIGHT_SHARE.get();
        }
        public static final MapCodec<WeightShareInRange> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("min", 0.0F).forGetter(WeightShareInRange::min),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("max", 1.0F).forGetter(WeightShareInRange::max))
                .apply(instance, WeightShareInRange::new));
        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            int total = PoolWeights.total(pool);
            if (total <= 0) {
                return false;
            }

            float share = (float) PoolWeights.of(entry) / total;
            return share >= this.min && share <= this.max;
        }
    }
    /** 条目身上是否已经挂着某种条件，按条件类型 id 比较。 */
    record HasCondition(ResourceLocation conditionType) implements EntrySelector {
        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.ENTRY_HAS_CONDITION.get();
        }
        public static final MapCodec<HasCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("conditionType").forGetter(HasCondition::conditionType))
                .apply(instance, HasCondition::new));
        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            return entry.conditions.stream()
                    .anyMatch(condition -> this.conditionType.equals(
                            BuiltInRegistries.LOOT_CONDITION_TYPE.getKey(condition.getType())));
        }
    }

    /** 条目身上是否已经挂着某种函数，按函数类型 id 比较。 */
    record HasFunction(ResourceLocation functionType) implements EntrySelector {
        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.ENTRY_HAS_FUNCTION.get();
        }
        public static final MapCodec<HasFunction> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("functionType").forGetter(HasFunction::functionType))
                .apply(instance, HasFunction::new));
        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            if (!(entry instanceof LootPoolSingletonContainer singleton)) {
                return false;
            }

            return singleton.functions.stream()
                    .anyMatch(function -> this.functionType.equals(
                            BuiltInRegistries.LOOT_FUNCTION_TYPE.getKey(function.getType())));
        }
    }

    /** 复合条目里任意子条目满足条件。 */
    record HasChild(EntrySelector child) implements EntrySelector {
        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.ENTRY_HAS_CHILD.get();
        }
        public static final MapCodec<HasChild> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                EntrySelector.CODEC.fieldOf("entrySelector").forGetter(HasChild::child)).apply(instance, HasChild::new));
        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            if (!(entry instanceof CompositeEntryBase composite)) {
                return false;
            }

            for (int i = 0; i < composite.children.size(); i++) {
                if (this.child.test(i, composite.children.get(i), pool, registries)) {
                    return true;
                }
            }

            return false;
        }
    }

    /** 取反。 */
    record Not(EntrySelector child) implements EntrySelector {
        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.ENTRY_NOT.get();
        }
        public static final MapCodec<Not> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                EntrySelector.CODEC.fieldOf("condition").forGetter(Not::child)).apply(instance, Not::new));
        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            return !this.child.test(index, entry, pool, registries);
        }
    }

    /** 任一满足。 */
    record AnyOf(List<EntrySelector> children) implements EntrySelector {
        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.ENTRY_ANY_OF.get();
        }
        public static final MapCodec<AnyOf> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                EntrySelector.CODEC.listOf().fieldOf("conditions").forGetter(AnyOf::children))
                .apply(instance, AnyOf::new));
        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            return this.children.stream().anyMatch(child -> child.test(index, entry, pool, registries));
        }
    }

    /** 全部满足。 */
    record AllOf(List<EntrySelector> children) implements EntrySelector {
        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.ENTRY_ALL_OF.get();
        }
        public static final MapCodec<AllOf> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                EntrySelector.CODEC.listOf().fieldOf("conditions").forGetter(AllOf::children))
                .apply(instance, AllOf::new));
        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            return this.children.stream().allMatch(child -> child.test(index, entry, pool, registries));
        }
    }
    /**
     * 自定义选择器：json 里只给一个 id，判断交给监听 CustomEntrySelectorEvent 的代码。
     * 每次判定都会发事件（注入器 × 表规模），监听者要自己控制开销。
     */
    record Custom(String id, Optional<CompoundTag> data) implements EntrySelector {
        /** 只给 id 的简写：不带附加数据。 */
        public Custom(String id) {
            this(id, Optional.empty());
        }
        public static final MapCodec<Custom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("custom_id").forGetter(Custom::id),
                LootInjectorOps.CUSTOM_DATA.forGetter(Custom::data)).apply(instance, Custom::new));

        @Override
        public EntrySelectorType getType() {
            return LootInjectorOps.CUSTOM_ENTRY.get();
        }

        @Override
        public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
            return CustomEntrySelectorEvent.post(this.id, this.data.orElseGet(CompoundTag::new), index, entry, pool, registries);
        }
    }
}
