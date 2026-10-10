package com.chen1335.arcanumlib.modules.loot.lootInjector.selector;

import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOps;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomConditionSelectorEvent;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * 条件选择器：在某个列表的条件里挑出要动的条件。
 * 目前有两个入口——容器自身的 conditions，以及某个函数自带的 conditions。
 */
public interface ConditionSelector {

    Codec<ConditionSelector> CODEC = LootInjectorOps.CONDITION_SELECTORS.byNameCodec()
            .dispatch("type", ConditionSelector::getType, ConditionSelectorType::codec);

    ConditionSelectorType getType();

    /** registries 是给 match 这类需要序列化候选的选择器用的，其余实现忽略即可。 */
    boolean test(int index, LootItemCondition condition, HolderLookup.Provider registries);

    /** 匹配所有条件。 */
    record Any() implements ConditionSelector {
        @Override
        public ConditionSelectorType getType() {
            return LootInjectorOps.CONDITION_ANY.get();
        }
        public static final MapCodec<Any> CODEC = MapCodec.unit(Any::new);
        @Override
        public boolean test(int index, LootItemCondition condition, HolderLookup.Provider registries) {
            return true;
        }
    }

    /** 按下标匹配，JSON 里从 0 开始。 */
    record ByIndex(int index) implements ConditionSelector {
        @Override
        public ConditionSelectorType getType() {
            return LootInjectorOps.CONDITION_BY_INDEX.get();
        }
        public static final MapCodec<ByIndex> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("index").forGetter(ByIndex::index)).apply(instance, ByIndex::new));
        @Override
        public boolean test(int index, LootItemCondition condition, HolderLookup.Provider registries) {
            return this.index == index;
        }
    }

    /** 按条件类型 id 匹配；要按类型批量删就用它，或者直接用 flat 的 remove_condition。 */
    record ByType(ResourceLocation conditionType) implements ConditionSelector {
        @Override
        public ConditionSelectorType getType() {
            return LootInjectorOps.CONDITION_BY_TYPE.get();
        }
        public static final MapCodec<ByType> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("conditionType").forGetter(ByType::conditionType))
                .apply(instance, ByType::new));
        @Override
        public boolean test(int index, LootItemCondition condition, HolderLookup.Provider registries) {
            return this.conditionType.equals(BuiltInRegistries.LOOT_CONDITION_TYPE.getKey(condition.getType()));
        }
    }

    /** 取反。 */
    record Not(ConditionSelector child) implements ConditionSelector {
        @Override
        public ConditionSelectorType getType() {
            return LootInjectorOps.CONDITION_NOT.get();
        }
        public static final MapCodec<Not> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ConditionSelector.CODEC.fieldOf("selector").forGetter(Not::child)).apply(instance, Not::new));
        @Override
        public boolean test(int index, LootItemCondition condition, HolderLookup.Provider registries) {
            return !this.child.test(index, condition, registries);
        }
    }

    /** 任一满足。 */
    record AnyOf(List<ConditionSelector> children) implements ConditionSelector {
        @Override
        public ConditionSelectorType getType() {
            return LootInjectorOps.CONDITION_ANY_OF.get();
        }
        public static final MapCodec<AnyOf> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ConditionSelector.CODEC.listOf().fieldOf("selectors").forGetter(AnyOf::children))
                .apply(instance, AnyOf::new));
        @Override
        public boolean test(int index, LootItemCondition condition, HolderLookup.Provider registries) {
            return this.children.stream().anyMatch(child -> child.test(index, condition, registries));
        }
    }

    /** 全部满足。 */
    record AllOf(List<ConditionSelector> children) implements ConditionSelector {
        @Override
        public ConditionSelectorType getType() {
            return LootInjectorOps.CONDITION_ALL_OF.get();
        }
        public static final MapCodec<AllOf> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ConditionSelector.CODEC.listOf().fieldOf("selectors").forGetter(AllOf::children))
                .apply(instance, AllOf::new));
        @Override
        public boolean test(int index, LootItemCondition condition, HolderLookup.Provider registries) {
            return this.children.stream().allMatch(child -> child.test(index, condition, registries));
        }
    }

    /**
     * 按模板 json 的 key 匹配：模板只写你关心的字段，候选在那些 key 上一致就算命中
     * （语义对齐 DataComponentIngredient 的组件匹配）。strict 打开则要求与模板完全一致。
     *
     * <p>模板保存的是【原始 json】，没写出来的 key 不参与比对，codec 的默认值不会污染结果。
     * 数组按有序参数看待，要求长度一致、逐元素匹配。
     */
    record Match(Dynamic<?> condition, boolean strict) implements ConditionSelector {
        @Override
        public ConditionSelectorType getType() {
            return LootInjectorOps.CONDITION_MATCH.get();
        }
        public static final MapCodec<Match> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.PASSTHROUGH.fieldOf("condition").forGetter(Match::condition),
                Codec.BOOL.optionalFieldOf("strict", false).forGetter(Match::strict))
                .apply(instance, Match::new));
        @Override
        public boolean test(int index, LootItemCondition condition, HolderLookup.Provider registries) {
            // 数据包是 json 读进来的，所以这里的值就是 JsonElement；换成别的 ops 时安全地不匹配
            if (!(this.condition.getValue() instanceof JsonElement expected)
                    || !expected.isJsonObject() || !expected.getAsJsonObject().has("condition")) {
                return false;                                    // 模板必须是带类型字段的 json 对象
            }

            JsonElement actual = LootItemCondition.DIRECT_CODEC
                    .encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), condition).getOrThrow();
            return this.strict ? actual.equals(expected) : JsonSubset.matches(expected, actual);
        }
    }
    /** 自定义：json 里只给一个 id，判断交给 CustomConditionSelectorEvent 的监听者。 */
    record Custom(String id) implements ConditionSelector {
        @Override
        public ConditionSelectorType getType() {
            return LootInjectorOps.CONDITION_CUSTOM.get();
        }
        public static final MapCodec<Custom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("custom_id").forGetter(Custom::id)).apply(instance, Custom::new));
        @Override
        public boolean test(int index, LootItemCondition condition, HolderLookup.Provider registries) {
            return CustomConditionSelectorEvent.post(this.id, index, condition);
        }
    }
}
