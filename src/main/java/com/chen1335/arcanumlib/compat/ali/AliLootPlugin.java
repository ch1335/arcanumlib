package com.chen1335.arcanumlib.compat.ali;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifier;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifierCondition;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifierTypes;
import com.chen1335.arcanumlib.compat.astages.HasStageCondition;
import com.chen1335.arcanumlib.compat.astages.StageChanceModifier;
import com.chen1335.arcanumlib.compat.astages.StagePattern;
import com.chen1335.arcanumlib.modules.loot.lootEntry.AllOfEntry;
import com.yanny.aci.api.NumberExpr;
import com.yanny.aci.tooltip.TooltipBuilder;
import com.yanny.ali.api.IDataNode;
import com.yanny.aci.tooltip.TooltipNode;
import com.yanny.ali.api.AliEntrypoint;
import com.yanny.ali.api.IPlugin;
import com.yanny.ali.api.IServerRegistry;
import com.yanny.ali.api.IServerUtils;
import com.yanny.ali.plugin.common.nodes.GroupNode;
import com.yanny.ali.plugin.server.TooltipUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * ALI 显示兼容：把 {@code chance_modifiers} 渲染成嵌套的加工树。
 * ALI 靠注解扫描发现插件，没装 ALI 时本类不会被加载，所以不能被自己的代码引用；不注册会走反射兜底、把 modifier 显示成类名。
 */
@AliEntrypoint
public class AliLootPlugin implements IPlugin {
    private static final String ROOT = ArcanumLib.MODID + ".ali.condition.chance_modifiers";
    private static final String WRAPPER = ArcanumLib.MODID + ".ali.wrapper";

    @Override
    public String getModId() {
        return ArcanumLib.MODID;
    }

    @Override
    public void registerServer(IServerRegistry registry) {
        registry.registerConditionTooltip(ChanceModifierCondition.class, AliLootPlugin::conditionTooltip);
        registry.registerConditionTooltip(HasStageCondition.class, AliLootPlugin::stageTooltip);

        // 只按基础概率修正（链上的修正如约定不参与）；返回 NumberExpr 而非绝对值，和原版 chance modifier 保持同一约定。
        registry.registerChanceModifier(ChanceModifierCondition.class,
                (utils, condition, chance, extra) -> NumberExpr.mul(chance, NumberExpr.constant(condition.baseChance())));
        // 用【池节点】而不是组节点：池节点把子条目横向铺开，正是要的"一行多个格子"。
        // 概率条件本来就挂在这些孩子上，所以直接让它们各自算自己的概率。
        registry.registerEntry(AllOfEntry.class, (utils, entry, rawChance, sumWeight, chanceConditions,
                                                  functions, conditions) -> {
            List<TooltipNode> shared = List.copyOf(chanceConditions);
            List<IDataNode> children = new ArrayList<>();

            for (LootPoolEntryContainer child : entry.children()) {
                NumberExpr childWeight = utils.getEntryWeight(utils, child, new ArrayList<>());
                children.add(utils.getEntryFactory(utils, child)
                        .create(utils, child, rawChance, childWeight, shared, functions, conditions));
            }

            // 用 GroupNode：图标和文案都跟着它走，正好是选择所有条目那一套
            return new GroupNode(children, TooltipUtils.getGroupTooltip().build());
        });
    }

    /** 返回的是未构建的 builder：注册接口要的就是它，标题用 key 设置。 */
    private static TooltipBuilder conditionTooltip(IServerUtils utils, ChanceModifierCondition condition) {
        return TooltipBuilder.branch(root -> {
            // 基础概率现在写在条件自己的字段上，不再是链首的 SetChance
            root.add(leaf("property.base_chance", number(condition.baseChance())));

            for (ChanceModifier modifier : condition.modifiers()) {
                root.add(describe(utils, modifier));
            }
        }).key(ROOT);
    }

    /** 阶段条件：一行显示成 {@code 玩家阶段 : 阶段名}（世界阶段同理）。 */
    private static TooltipBuilder stageTooltip(IServerUtils utils, HasStageCondition condition) {
        String scope = condition.checkServerStages()
                ? key("condition.has_stage.server")
                : key("condition.has_stage.player");

        // 值 + 不带 \ue000 标记的键 → 走 appendTranslatableKeyWithValuesAndComponent：
        // 值被填进键里的 %s，且不会再追加 ": 值"（带标记的键走 appendRawKey，就是上次那个 bug）。
        // 注册接口要的是未 build 的 builder，所以用 .key(...) 收尾（和 conditionTooltip 一样）
        return TooltipBuilder.value(stageName(condition)).key(scope);
    }

    /** 阶段名优先用 desc 的本地化键；没写 desc 就退回 pattern 原文。 */
    private static String stageName(Optional<String> desc, StagePattern stage) {
        return desc.map(TooltipBuilder::translate).orElseGet(stage::pattern);
    }

    private static String stageName(HasStageCondition condition) {
        return stageName(condition.desc(), condition.stage());
    }

    private static String stageName(StageChanceModifier modifier) {
        return stageName(modifier.desc(), modifier.stage());
    }

    /** 本模组的八个加工器在这里逐个渲染；别的模组注册的走 default，只显示类型 id。 */
    private static TooltipNode describe(IServerUtils utils, ChanceModifier modifier) {
        return switch (modifier) {
            case ChanceModifier.SetChance it -> leaf("property.set_chance", number(it.value()));
            // 加/乘合并后压成一行：「应用 : x N」/「应用 : + N」（负值直接 -N）
            case ChanceModifier.Apply it -> leaf("property.apply", applyValue(it));
            case ChanceModifier.Invert ignored -> leaf("property.invert", "1 - chance");
            case ChanceModifier.Clamp it -> TooltipBuilder.branch(branch -> branch
                    .add(leaf("property.min", number(it.min())))
                    .add(leaf("property.max", number(it.max()))))
                    .build(key("property.range"));
            case ChanceModifier.ToolEnchantmentLevel it -> TooltipBuilder.branch(branch -> branch
                    .add(operation(it.operation(), leaf("property.per_level", number(it.perLevel()))))
                    .add(leafTranslation("property.enchantment", enchantmentName(it.enchantment()))))
                    .build(key("property.enchantment_modifier"));
            case ChanceModifier.EntityAttribute it -> TooltipBuilder.branch(branch -> branch
                    .add(operation(it.operation(), leaf("property.per_point", number(it.perPoint()))))
                    .add(leafTranslation("property.attribute", it.attribute().value().getDescriptionId())))
                    .build(key("property.attribute_modifier"));
            case ChanceModifier.DifficultyFactor it -> TooltipBuilder.branch(branch -> {
                difficultyTier(branch, it, "property.peaceful", it.peaceful());
                difficultyTier(branch, it, "property.easy", it.easy());
                difficultyTier(branch, it, "property.normal", it.normal());
                difficultyTier(branch, it, "property.hard", it.hard());
            }).build(key("property.difficulty_modifier"));
            // 阶段加工器：作用域行带成功链、「否则」行带失败链；某条链为空就整行不显示
            case StageChanceModifier it -> TooltipBuilder.branch(branch -> {
                TooltipBuilder scope = TooltipBuilder.value(stageName(it));
                it.success().forEach(child -> scope.add(describe(utils, child)));
                branch.add(scope.build(key(it.checkServerStages()
                        ? "property.stage_scope.server"
                        : "property.stage_scope.player")));

                if (!it.fail().isEmpty()) {
                    branch.add(TooltipBuilder.branch(otherwise -> it.fail()
                                    .forEach(child -> otherwise.add(describe(utils, child))))
                            .build(key("property.otherwise")));
                }
            }).build(key("property.stage_modifier"));
            // 谓词修正：谓词逐个列出（走 ALI 自己的条件渲染），全通过走成功链、否则走失败链；空块不显示
            case ChanceModifier.Predicate it -> TooltipBuilder.branch(branch -> {

                if (!it.predicates().isEmpty()) {
                    // 条件列表交给 ALI 自己的值渲染（与它渲染 all_of、以及 loot 表里的条件段同一条路径）
                    TooltipNode content = utils.getValueTooltip(utils, it.predicates()).build();

                    if (!content.isBlank(false)) {
                        // 基本模式就能显示 → 用 ALI 的渲染。注意别照搬它的 array(标题 + 内容)：
                        // array 是无键节点，childIndent 不 +1，标题会和谓词平级；
                        // 这里把标题当父节点，谓词缩进一层，与上面"满足条件时 / 否则"同一种风格。
                        branch.add(TooltipBuilder.keyOnly(key("property.predicates")).add(content));
                    } else if (!content.isBlank(true)) {
                        // ALI 把条件细节归入「高级战利品信息」（分隔键就叫 advanced_loot_info.delimiter.conditions），
                        // 基本模式下整段为空。这里退回自己列一遍注册名，至少让"有哪些谓词"看得见。
                        TooltipBuilder section = TooltipBuilder.branch(predicates -> it.predicates()
                                .forEach(predicate -> predicates.add(TooltipBuilder.value(predicateId(predicate)).build())));
                        branch.add(section.build(key("property.predicates")));
                    }
                }

                if (!it.success().isEmpty()) {
                    branch.add(TooltipBuilder.branch(matched -> it.success()
                                    .forEach(child -> matched.add(describe(utils, child))))
                            .build(key("property.when_matched")));
                }

                if (!it.fail().isEmpty()) {
                    branch.add(TooltipBuilder.branch(otherwise -> it.fail()
                                    .forEach(child -> otherwise.add(describe(utils, child))))
                            .build(key("property.otherwise")));
                }
            }).build(key("property.predicate_modifier"));
            // 接口对外开源，别的模组的加工器不认识：(不猜语义) 只把它的注册名显示出来
            default -> leaf("property.unknown_modifier", typeId(modifier));
        };
    }

    /** 谓词的注册名（例如 minecraft:random_chance），解析不到就退回简单类名。 */
    private static String predicateId(LootItemCondition predicate) {
        ResourceLocation id = BuiltInRegistries.LOOT_CONDITION_TYPE.getKey(predicate.getType());

        return id != null ? id.toString() : predicate.getClass().getSimpleName();
    }
    /** 未知加工器的显示名：优先注册名，理论兜底用简单类名。 */
    private static String typeId(ChanceModifier modifier) {
        ResourceLocation id = ChanceModifierTypes.REGISTRY.getKey(modifier.getType());

        return id != null ? id.toString() : modifier.getClass().getSimpleName();
    }

    /** 运算方式自己一层，把"每级/每点增加多少"挂在它下面。 */
    private static TooltipNode operation(ChanceModifier.Operation operation, TooltipNode value) {
        return TooltipBuilder.branch(branch -> branch.add(value))
                .build(key("operation." + operation.getSerializedName()));
    }

    /** 「金色标签 : 青色值」，外层 WRAPPER 是只含一个 %s 的键。 */
    private static TooltipNode leaf(String property, String value) {
        return TooltipBuilder.keyValue(TooltipBuilder.translate(key(property)), value).build(WRAPPER);
    }

    /**
     * 值为翻译键的版本：附魔与属性名本身就是本地化键，必须再过一次 {@code translate}。
     * {@code keyValue} 的值只有带 \ue000 前缀才会被翻成 Component，否则原样显示成 "enchantment.minecraft.looting"。
     */
    private static TooltipNode leafTranslation(String property, String translationKey) {
        return TooltipBuilder.keyValue(TooltipBuilder.translate(key(property)),
                TooltipBuilder.translate(translationKey)).build(WRAPPER);
    }

    /**
     * 档位值等于中性值时不显示：加法 0 与乘法 1 都表示"这一档什么都没改"，留着只是噪音。
     * 没写的档位会取中性值，所以同样不显示。
     */
    private static void difficultyTier(TooltipBuilder branch, ChanceModifier.DifficultyFactor modifier,
                                       String property, Optional<Double> value) {
        double tier = value.orElse(modifier.operation().neutral());

        if (tier == modifier.operation().neutral()) {
            return;
        }

        branch.add(leaf(property, applyPrefix(modifier.operation(), tier)));
    }

    /** 一行显示"怎么改"：乘 "×N"、加 "+N"（负值就是 "-N"）。 */
    private static String applyPrefix(ChanceModifier.Operation operation, double raw) {
        String value = number(raw);

        return switch (operation) {
            case MULTIPLY -> "×" + value;
            case ADD -> raw < 0 ? value : "+" + value;
        };
    }

    private static String applyValue(ChanceModifier.Apply modifier) {
        return applyPrefix(modifier.operation(), modifier.value());
    }

    /**
     * 附魔是数据驱动注册表，名字按 {@code enchantment.<命名空间>.<路径>} 取。
     * 属性不能用同样拼法：原版 {@code generic.luck} 这种路径带点，拼出来会多一层命名空间。
     */
    private static String enchantmentName(ResourceKey<Enchantment> enchantment) {
        return "enchantment." + enchantment.location().getNamespace() + "." + enchantment.location().getPath();
    }

    private static String number(double value) {
        return String.valueOf(value);
    }

    private static String key(String path) {
        return ArcanumLib.MODID + ".ali." + path;
    }
}
