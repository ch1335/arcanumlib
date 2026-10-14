package com.chen1335.arcanumlib.modules.loot.lootCondition;

import com.chen1335.arcanumlib.events.EnchantedLootEvent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 概率加工器：把当前概率变成新概率，按列表顺序依次应用。概率从 1.0 起算，「乘 0.1」即 10%。
 * 类型走 {@link ChanceModifierTypes} 注册表（必须用 MapCodec，Codec.dispatch 传方法引用会歧义）。
 *
 * <p>对其它模组开放：实现它 + {@code DeferredRegister.create(ChanceModifierTypes.REGISTRY_KEY, 你的 modid)} 注册。
 * 两条约定：{@link #apply} 必须无状态（同一实例会被多次掷骰复用）；{@link #getType} 返回注册时的 holder。
 */
public interface ChanceModifier {

    /** 按注册表名分发，所以 json 里的 {@code type} 是带命名空间的 id（例如 {@code arcanumlib:clamp}）。 */
    Codec<ChanceModifier> CODEC = ChanceModifierTypes.REGISTRY.byNameCodec()
            .dispatch("type", ChanceModifier::getType, ChanceModifierType::codec);

    ChanceModifierType getType();

    double apply(double chance, LootContext context);

    /** 概率加 / 乘一个值（加可为负，用来折减）；operation 必填。 */
    record Apply(Operation operation, double value) implements ChanceModifier {
        public static Apply add(double value) {
            return new Apply(Operation.ADD, value);
        }

        public static Apply multiply(double value) {
            return new Apply(Operation.MULTIPLY, value);
        }

        public static final MapCodec<Apply> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Operation.CODEC.fieldOf("operation").forGetter(Apply::operation),
                Codec.DOUBLE.fieldOf("value").forGetter(Apply::value)).apply(instance, Apply::new));

        @Override
        public ChanceModifierType getType() {
            return ChanceModifierTypes.APPLY.get();
        }

        @Override
        public double apply(double chance, LootContext context) {
            return this.operation.apply(chance, this.value);
        }
    }

    /** 直接设置概率。放链首定初始值；放后面会把前面的结果整个丢掉。 */
    record SetChance(double value) implements ChanceModifier {
        public static SetChance of(double value) {
            return new SetChance(value);
        }

        public static final MapCodec<SetChance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.DOUBLE.fieldOf("value").forGetter(SetChance::value)).apply(instance, SetChance::new));

        @Override
        public ChanceModifierType getType() {
            return ChanceModifierTypes.SET_CHANCE.get();
        }

        @Override
        public double apply(double chance, LootContext context) {
            return this.value;
        }
    }

    /** 把概率夹到 [min, max]，兜底避免负数或超过 1。 */
    record Clamp(double min, double max) implements ChanceModifier {
        public static Clamp of(double min, double max) {
            return new Clamp(min, max);
        }

        public static final MapCodec<Clamp> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.DOUBLE.optionalFieldOf("min", 0.0D).forGetter(Clamp::min),
                Codec.DOUBLE.optionalFieldOf("max", 1.0D).forGetter(Clamp::max)).apply(instance, Clamp::new));

        @Override
        public ChanceModifierType getType() {
            return ChanceModifierTypes.CLAMP.get();
        }

        @Override
        public double apply(double chance, LootContext context) {
            return Math.min(this.max, Math.max(this.min, chance));
        }
    }

    /** 取补：1 - 概率。 */
    record Invert() implements ChanceModifier {
        public static Invert of() {
            return new Invert();
        }

        public static final MapCodec<Invert> CODEC = MapCodec.unit(Invert::new);

        @Override
        public ChanceModifierType getType() {
            return ChanceModifierTypes.INVERT.get();
        }

        @Override
        public double apply(double chance, LootContext context) {
            return 1.0D - chance;
        }
    }

    /** 按游戏难度加工概率。档位可留空，留空按 {@link Operation} 取中性值（乘 1.0 / 加 0.0），即该难度下不改变概率。 */
    record DifficultyFactor(Operation operation,
                            Optional<Double> peaceful,
                            Optional<Double> easy,
                            Optional<Double> normal,
                            Optional<Double> hard) implements ChanceModifier {
        public static final MapCodec<DifficultyFactor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Operation.CODEC.fieldOf("operation").forGetter(DifficultyFactor::operation),
                Codec.DOUBLE.optionalFieldOf("peaceful").forGetter(DifficultyFactor::peaceful),
                Codec.DOUBLE.optionalFieldOf("easy").forGetter(DifficultyFactor::easy),
                Codec.DOUBLE.optionalFieldOf("normal").forGetter(DifficultyFactor::normal),
                Codec.DOUBLE.optionalFieldOf("hard").forGetter(DifficultyFactor::hard)
        ).apply(instance, DifficultyFactor::new));

        /** 乘法版本，datagen 用起来比手搓 Optional 干净。 */
        public static DifficultyFactor multiply(double peaceful, double easy, double normal, double hard) {
            return of(Operation.MULTIPLY, peaceful, easy, normal, hard);
        }

        /** 加法版本。 */
        public static DifficultyFactor add(double peaceful, double easy, double normal, double hard) {
            return of(Operation.ADD, peaceful, easy, normal, hard);
        }

        private static DifficultyFactor of(Operation operation, double peaceful, double easy, double normal, double hard) {
            return new DifficultyFactor(operation, Optional.of(peaceful), Optional.of(easy),
                    Optional.of(normal), Optional.of(hard));
        }

        @Override
        public ChanceModifierType getType() {
            return ChanceModifierTypes.DIFFICULTY_FACTOR.get();
        }

        @Override
        public double apply(double chance, LootContext context) {
            return this.operation.apply(chance, this.valueFor(context.getLevel().getDifficulty()).orElse(this.operation.neutral()));
        }

        private Optional<Double> valueFor(Difficulty difficulty) {
            return switch (difficulty) {
                case PEACEFUL -> this.peaceful;
                case EASY -> this.easy;
                case NORMAL -> this.normal;
                case HARD -> this.hard;
            };
        }
    }

    /**
     * 按工具上某个附魔的等级加工概率：加法 {@code chance + 等级 × per_level}，乘法 {@code chance ×（1 + 等级 × per_level）}。
     * 与 {@link DifficultyFactor} 不同：那边乘档位值本身，这边乘「1 + 增量」。
     */
    record ToolEnchantmentLevel(ResourceKey<Enchantment> enchantment, Operation operation,
                                double perLevel) implements ChanceModifier {
        public static ToolEnchantmentLevel of(ResourceKey<Enchantment> enchantment, Operation operation, double perLevel) {
            return new ToolEnchantmentLevel(enchantment, operation, perLevel);
        }

        public static final MapCodec<ToolEnchantmentLevel> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceKey.codec(Registries.ENCHANTMENT).fieldOf("enchantment").forGetter(ToolEnchantmentLevel::enchantment),
                Operation.CODEC.fieldOf("operation").forGetter(ToolEnchantmentLevel::operation),
                Codec.DOUBLE.optionalFieldOf("per_level", 0.05D).forGetter(ToolEnchantmentLevel::perLevel)
        ).apply(instance, ToolEnchantmentLevel::new));

        @Override
        public ChanceModifierType getType() {
            return ChanceModifierTypes.TOOL_ENCHANTMENT_LEVEL.get();
        }

        @Override
        public double apply(double chance, LootContext context) {
            int level = this.levelOf(context);

            return level <= 0 ? chance : this.operation.applyScaled(chance, level * this.perLevel);
        }

        /**
         * 取不到工具或附魔时按 0 级处理（不改变概率），再发 {@link EnchantedLootEvent} 让外部调整。
         * 附魔是数据驱动注册表，只能存 ResourceKey 再取，不能像属性那样直接存 Holder。
         */
        private int levelOf(LootContext context) {
            Holder<Enchantment> holder = context.getLevel().registryAccess()
                    .registryOrThrow(Registries.ENCHANTMENT)
                    .getHolderOrThrow(this.enchantment);

            // NeoForge 推荐直接用 ItemStack#getEnchantmentLevel，比 EnchantmentHelper 那层更直接
            ItemStack tool = context.getParamOrNull(LootContextParams.TOOL);
            int level = tool == null || tool.isEmpty()
                    ? 0
                    : tool.getEnchantmentLevel(holder);

            return EnchantedLootEvent.post(holder, context, level);
        }
    }

    /**
     * 按某个实体的一条属性加工概率，语义同 {@link ToolEnchantmentLevel}。
     * 拿不到实体或该实体没有这条属性时原样返回（不当 0），所以缺模组也不会意外清零。
     */
    record EntityAttribute(Holder<Attribute> attribute, EntitySource source, Operation operation,
                           double perPoint) implements ChanceModifier {
        public static EntityAttribute of(Holder<Attribute> attribute, EntitySource source, Operation operation, double perPoint) {
            return new EntityAttribute(attribute, source, operation, perPoint);
        }

        public static final MapCodec<EntityAttribute> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(EntityAttribute::attribute),
                EntitySource.CODEC.optionalFieldOf("entity", EntitySource.ATTACKING_ENTITY).forGetter(EntityAttribute::source),
                Operation.CODEC.fieldOf("operation").forGetter(EntityAttribute::operation),
                Codec.DOUBLE.optionalFieldOf("per_point", 0.05D).forGetter(EntityAttribute::perPoint)
        ).apply(instance, EntityAttribute::new));

        @Override
        public ChanceModifierType getType() {
            return ChanceModifierTypes.ENTITY_ATTRIBUTE.get();
        }

        @Override
        public double apply(double chance, LootContext context) {
            AttributeInstance instance = this.instanceOf(context);
            return instance == null ? chance : this.operation.applyScaled(chance, instance.getValue() * this.perPoint);
        }

        private AttributeInstance instanceOf(LootContext context) {
            if (!(this.source.resolve(context) instanceof LivingEntity living)) {
                return null;
            }

            // getInstance 在该实体没有这条属性时返回 null —— 正好就是"有就改、没有就不改"
            return living.getAttributes().getInstance(this.attribute);
        }
    }

    /**
     * 按 {@link LootContext} 的幸运值加工概率：语义与 {@link EntityAttribute} 完全一致，只是"点数"换成上下文的 luck。
     *
     * <p>取值来源：{@code context.getLuck()} —— 开箱是开箱者的幸运（附魔 / 效果已算进去），
     * 钓鱼是钓手的幸运加上钓竿加成。luck 为 0 时 {@code applyScaled(chance, 0)} 原样返回，所以没幸运也不改变概率。
     */
    record Luck(Operation operation, double perPoint) implements ChanceModifier {
        public static Luck of(Operation operation, double perPoint) {
            return new Luck(operation, perPoint);
        }

        public static final MapCodec<Luck> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Operation.CODEC.fieldOf("operation").forGetter(Luck::operation),
                Codec.DOUBLE.optionalFieldOf("per_point", 0.05D).forGetter(Luck::perPoint)
        ).apply(instance, Luck::new));

        @Override
        public ChanceModifierType getType() {
            return ChanceModifierTypes.LUCK.get();
        }

        @Override
        public double apply(double chance, LootContext context) {
            return this.operation.applyScaled(chance, context.getLuck() * this.perPoint);
        }
    }
    /** 用哪个上下文实体取属性。 */
    enum EntitySource implements StringRepresentable {
        /** 上下文主角：实体掉落是被击杀者，宝箱/刷怪笼/考古是开箱玩家。 */
        THIS_ENTITY("this_entity", context -> context.getParamOrNull(LootContextParams.THIS_ENTITY)),
        /** 攻击者，最常用。 */
        ATTACKING_ENTITY("attacking_entity", context -> context.getParamOrNull(LootContextParams.ATTACKING_ENTITY)),
        /** 直接攻击者（如射出的箭）。 */
        DIRECT_ATTACKING_ENTITY("direct_attacking_entity", context -> context.getParamOrNull(LootContextParams.DIRECT_ATTACKING_ENTITY)),
        /** 最后造成伤害的玩家。 */
        LAST_DAMAGE_PLAYER("last_damage_player", context -> context.getParamOrNull(LootContextParams.LAST_DAMAGE_PLAYER));

        public static final Codec<EntitySource> CODEC = StringRepresentable.fromEnum(EntitySource::values);

        private final String name;
        private final Function<LootContext, Entity> resolver;

        EntitySource(String name, Function<LootContext, Entity> resolver) {
            this.name = name;
            this.resolver = resolver;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public Entity resolve(LootContext context) {
            return this.resolver.apply(context);
        }
    }

    /**
     * 谓词修正：谓词列表**全部**通过就走 {@code success} 链，否则走 {@code fail} 链；两条链各自是一串普通加工器。
     * 空谓词列表视为通过（与原版 {@code all_of} 的空列表语义一致）。
     */
    record Predicate(List<LootItemCondition> predicates, List<ChanceModifier> success, List<ChanceModifier> fail)
            implements ChanceModifier {

        public static final MapCodec<Predicate> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                LootItemCondition.DIRECT_CODEC.listOf().fieldOf("predicates").forGetter(Predicate::predicates),
                ChanceModifier.CODEC.listOf().optionalFieldOf("success", List.<ChanceModifier>of())
                        .forGetter(Predicate::success),
                ChanceModifier.CODEC.listOf().optionalFieldOf("fail", List.<ChanceModifier>of())
                        .forGetter(Predicate::fail)
        ).apply(instance, Predicate::new));

        @Override
        public ChanceModifierType getType() {
            return ChanceModifierTypes.PREDICATE.get();
        }

        @Override
        public double apply(double chance, LootContext context) {
            List<ChanceModifier> chain = this.predicates.stream().allMatch(predicate -> predicate.test(context))
                    ? this.success
                    : this.fail;
            double result = chance;

            for (ChanceModifier modifier : chain) {
                result = modifier.apply(result, context);
            }

            return result;
        }
    }
    /** 难度档位的加工语义。 */
    enum Operation implements StringRepresentable {
        /** 概率 + 档位值。 */
        ADD("add"),
        /** 概率 × 档位值。 */
        MULTIPLY("mul");

        public static final Codec<Operation> CODEC = StringRepresentable.fromEnum(Operation::values);

        private final String name;

        Operation(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        /** 档位留空时用的值：加法 0、乘法 1，都不改变概率。 */
        public double neutral() {
            return this == ADD ? 0.0D : 1.0D;
        }

        /** 纯语义：加法 +value，乘法 ×value。 */
        public double apply(double chance, double value) {
            return this == ADD ? chance + value : chance * value;
        }

        /** 增量语义：加法 +delta，乘法 ×(1+delta)；delta 为 0 时都不改变。 */
        public double applyScaled(double chance, double delta) {
            return this == ADD ? chance + delta : chance * (1.0D + delta);
        }
    }
}
