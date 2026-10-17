package com.chen1335.arcanumlib.modules.loot.lootInjector.selector;

import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOps;
import com.chen1335.arcanumlib.modules.loot.lootInjector.event.CustomFunctionSelectorEvent;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;

import java.util.List;
import java.util.Optional;

/**
 * 函数选择器：在某个容器的函数列表里挑出要动的函数。
 * <p>函数自身还可能带条件（{@link LootItemConditionalFunction#predicates}），那是
 * {@code modify_function} 作用域往里再钻一层的入口——现有一堆 flat op 完全够不着那一层。
 */
public interface FunctionSelector {

    Codec<FunctionSelector> CODEC = LootInjectorOps.FUNCTION_SELECTORS.byNameCodec()
            .dispatch("type", FunctionSelector::getType, FunctionSelectorType::codec);

    FunctionSelectorType getType();

    /** registries 是给 match 这类需要序列化候选的选择器用的，其余实现忽略即可。 */
    boolean test(int index, LootItemFunction function, HolderLookup.Provider registries);

    /** 匹配所有函数。 */
    record Any() implements FunctionSelector {
        @Override
        public FunctionSelectorType getType() {
            return LootInjectorOps.FUNCTION_ANY.get();
        }
        public static final MapCodec<Any> CODEC = MapCodec.unit(Any::new);
        @Override
        public boolean test(int index, LootItemFunction function, HolderLookup.Provider registries) {
            return true;
        }
    }

    /** 按下标匹配，JSON 里从 0 开始。 */
    record ByIndex(int index) implements FunctionSelector {
        @Override
        public FunctionSelectorType getType() {
            return LootInjectorOps.FUNCTION_BY_INDEX.get();
        }
        public static final MapCodec<ByIndex> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.INT.fieldOf("index").forGetter(ByIndex::index)).apply(instance, ByIndex::new));
        @Override
        public boolean test(int index, LootItemFunction function, HolderLookup.Provider registries) {
            return this.index == index;
        }
    }

    /** 按函数类型 id 匹配；要按类型批量删就用它，或者直接用 flat 的 remove_function。 */
    record ByType(ResourceLocation functionType) implements FunctionSelector {
        @Override
        public FunctionSelectorType getType() {
            return LootInjectorOps.FUNCTION_BY_TYPE.get();
        }
        public static final MapCodec<ByType> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("functionType").forGetter(ByType::functionType))
                .apply(instance, ByType::new));
        @Override
        public boolean test(int index, LootItemFunction function, HolderLookup.Provider registries) {
            return this.functionType.equals(BuiltInRegistries.LOOT_FUNCTION_TYPE.getKey(function.getType()));
        }
    }

    /** 函数自带的某个条件命中即算匹配；非条件型函数（例如 set_count）不匹配。 */
    record HasCondition(ResourceLocation conditionType) implements FunctionSelector {
        @Override
        public FunctionSelectorType getType() {
            return LootInjectorOps.FUNCTION_HAS_CONDITION.get();
        }
        public static final MapCodec<HasCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("conditionType").forGetter(HasCondition::conditionType))
                .apply(instance, HasCondition::new));
        @Override
        public boolean test(int index, LootItemFunction function, HolderLookup.Provider registries) {
            return function instanceof LootItemConditionalFunction conditional
                    && conditional.predicates.stream().anyMatch(condition -> this.conditionType.equals(
                            BuiltInRegistries.LOOT_CONDITION_TYPE.getKey(condition.getType())));
        }
    }

    /** 取反。 */
    record Not(FunctionSelector child) implements FunctionSelector {
        @Override
        public FunctionSelectorType getType() {
            return LootInjectorOps.FUNCTION_NOT.get();
        }
        public static final MapCodec<Not> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FunctionSelector.CODEC.fieldOf("selector").forGetter(Not::child)).apply(instance, Not::new));
        @Override
        public boolean test(int index, LootItemFunction function, HolderLookup.Provider registries) {
            return !this.child.test(index, function, registries);
        }
    }

    /** 任一满足。 */
    record AnyOf(List<FunctionSelector> children) implements FunctionSelector {
        @Override
        public FunctionSelectorType getType() {
            return LootInjectorOps.FUNCTION_ANY_OF.get();
        }
        public static final MapCodec<AnyOf> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FunctionSelector.CODEC.listOf().fieldOf("selectors").forGetter(AnyOf::children))
                .apply(instance, AnyOf::new));
        @Override
        public boolean test(int index, LootItemFunction function, HolderLookup.Provider registries) {
            return this.children.stream().anyMatch(child -> child.test(index, function, registries));
        }
    }

    /** 全部满足。 */
    record AllOf(List<FunctionSelector> children) implements FunctionSelector {
        @Override
        public FunctionSelectorType getType() {
            return LootInjectorOps.FUNCTION_ALL_OF.get();
        }
        public static final MapCodec<AllOf> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FunctionSelector.CODEC.listOf().fieldOf("selectors").forGetter(AllOf::children))
                .apply(instance, AllOf::new));
        @Override
        public boolean test(int index, LootItemFunction function, HolderLookup.Provider registries) {
            return this.children.stream().allMatch(child -> child.test(index, function, registries));
        }
    }

    /**
    * 按模板 json 的 key 匹配：模板只写关心的字段，候选在那些 key 上一致即命中（对齐 DataComponentIngredient），strict 则要求完全一致。
    * 模板存【原始 json】，没写的 key 不参与比对（codec 默认值不污染结果）；数组按有序参数看待，长度一致且逐元素匹配。
    */
    record Match(Dynamic<?> function, boolean strict) implements FunctionSelector {
        @Override
        public FunctionSelectorType getType() {
            return LootInjectorOps.FUNCTION_MATCH.get();
        }
        public static final MapCodec<Match> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.PASSTHROUGH.fieldOf("function").forGetter(Match::function),
                Codec.BOOL.optionalFieldOf("strict", false).forGetter(Match::strict))
                .apply(instance, Match::new));
        @Override
        public boolean test(int index, LootItemFunction function, HolderLookup.Provider registries) {
            // 数据包是 json 读进来的，所以这里的值就是 JsonElement；换成别的 ops 时安全地不匹配
            if (!(this.function.getValue() instanceof JsonElement expected)
                    || !expected.isJsonObject() || !expected.getAsJsonObject().has("function")) {
                return false;                                    // 模板必须是带类型字段的 json 对象
            }

            // 候选里可能有附魔这类数据包注册表引用，必须用 RegistryOps 才编码得出来
            JsonElement actual = LootItemFunctions.ROOT_CODEC
                    .encodeStart(RegistryOps.create(JsonOps.INSTANCE, registries), function).getOrThrow();
            return this.strict ? actual.equals(expected) : JsonSubset.matches(expected, actual);
        }
    }
    /** 自定义：json 里只给一个 id，判断交给 CustomFunctionSelectorEvent 的监听者。 */
    record Custom(String id, Optional<CompoundTag> data) implements FunctionSelector {
        /** 只给 id 的简写：不带附加数据。 */
        public Custom(String id) {
            this(id, Optional.empty());
        }
        @Override
        public FunctionSelectorType getType() {
            return LootInjectorOps.FUNCTION_CUSTOM.get();
        }
        public static final MapCodec<Custom> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.fieldOf("custom_id").forGetter(Custom::id),
                LootInjectorOps.CUSTOM_DATA.forGetter(Custom::data)).apply(instance, Custom::new));
        @Override
        public boolean test(int index, LootItemFunction function, HolderLookup.Provider registries) {
            return CustomFunctionSelectorEvent.post(this.id, this.data.orElseGet(CompoundTag::new), index, function, registries);
        }
    }
}
