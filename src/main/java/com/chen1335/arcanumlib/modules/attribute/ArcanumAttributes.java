package com.chen1335.arcanumlib.modules.attribute;

import com.chen1335.arcanumlib.ArcanumLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.common.PercentageAttribute;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 本模组的自定义属性。 */
public final class ArcanumAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, ArcanumLib.MODID);

    /**
     * 寻觅：提高稀有生物的刷怪权重。占比低于阈值（服务端配置）的生物最终权重 = 基础权重 ×（1 + 该值）。
     * 加成比例本身不设上限，只受属性范围夹取（0~100，即最多 ×101）；基础值 0，默认不生效。
     */
    public static final DeferredHolder<Attribute, Attribute> SEEKING = ATTRIBUTES.register(
            "seeking",
            () -> new PercentageAttribute("attribute.name.arcanumlib.seeking", 0.0D, 0.0D, 100.0D)
                    .setSyncable(true));

    /**
     * 时运：直接加在「时运」附魔的等效等级上，作用于 apply_bonus / table_bonus 那些原版掉落机制。
     * 基础值 0，默认不生效；范围下限 0，所以只能加不能减。
     */
    public static final DeferredHolder<Attribute, Attribute> FORTUNE = ATTRIBUTES.register(
            "fortune",
            () -> new RangedAttribute("attribute.name.arcanumlib.fortune", 0.0D, 0.0D, 100.0D)
                    .setSyncable(true));

    /** 抢夺：直接加在「抢夺」附魔的等效等级上，作用于 enchanted_count_increase / random_chance_with_enchanted_bonus。 */
    public static final DeferredHolder<Attribute, Attribute> LOOTING = ATTRIBUTES.register(
            "looting",
            () -> new RangedAttribute("attribute.name.arcanumlib.looting", 0.0D, 0.0D, 100.0D)
                    .setSyncable(true));

    /**
     * 额外战利品：按这个倍率对同一张表额外摇取若干份，追加到原结果里。
     * 字面值就是倍率（0.25 = 再多 25%），默认只对实体掉落与箱子战利品生效，前缀见配置。
     * 基础值 0，默认不生效；上限 20（即最多再多 20 倍）。
     */
    public static final DeferredHolder<Attribute, Attribute> BONUS_LOOT = ATTRIBUTES.register(
            "bonus_loot",
            () -> new PercentageAttribute("attribute.name.arcanumlib.bonus_loot", 0.0D, 0.0D, 20.0D)
                    .setSyncable(true));

    private ArcanumAttributes() {
    }
}
