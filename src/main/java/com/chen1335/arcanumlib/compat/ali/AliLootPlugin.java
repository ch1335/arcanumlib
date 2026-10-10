package com.chen1335.arcanumlib.compat.ali;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifier;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifierCondition;
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
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

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
                root.add(describe(modifier));
            }
        }).key(ROOT);
    }

    /** sealed 接口在同一文件里，所以这里可以直接穷举，加新类型时编译器会提醒。 */
    private static TooltipNode describe(ChanceModifier modifier) {
        return switch (modifier) {
            case ChanceModifier.SetChance it -> leaf("property.set_chance", number(it.value()));
            case ChanceModifier.Multiply it -> leaf("property.multiply_factor", number(it.factor()));
            case ChanceModifier.Add it -> leaf("property.add_value", number(it.value()));
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
            case ChanceModifier.DifficultyFactor it -> TooltipBuilder.branch(branch -> branch
                    .add(leaf("property.peaceful", difficulty(it, it.peaceful())))
                    .add(leaf("property.easy", difficulty(it, it.easy())))
                    .add(leaf("property.normal", difficulty(it, it.normal())))
                    .add(leaf("property.hard", difficulty(it, it.hard()))))
                    .build(key("property.difficulty_modifier"));
        };
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

    /** 留空的难度档位按语义取中性值，和实际计算保持一致。 */
    private static String difficulty(ChanceModifier.DifficultyFactor modifier, Optional<Double> value) {
        return "x" + value.orElse(modifier.operation().neutral());
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