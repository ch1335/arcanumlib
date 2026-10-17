package com.chen1335.arcanumlib.registry;

import com.chen1335.arcanumlib.ArcanumLib;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * 本模组用到的标签。数据包文件放在 {@code data/arcanumlib/tags/<注册表名>/<路径>.json}。
 */
public final class ArcanumTags {

    public static class Attribute {
        /**
         * 带这个标签的属性不吃 {@code ALL_ATTRIBUTE_MULTIPLIER}（全属性倍率）。
         *
         * <p>默认是空标签，也就是不改变原行为；需要排除的属性往
         * {@code data/arcanumlib/tags/attribute/ignore_attribute_multiplier.json} 里加即可（数据包也能改）。
         */
        public static final TagKey<net.minecraft.world.entity.ai.attributes.Attribute> IGNORE_ATTRIBUTE_MULTIPLIER =
                TagKey.create(Registries.ATTRIBUTE, ArcanumLib.id("ignore_attribute_multiplier"));

    }

    public static class Damage {
        /**
         * 带这个标签的伤害类型不吃 {@code DAMAGE_MULTIPLIER}（造成伤害那一侧）。
         *
         * <p>默认是空标签，也就是不改变原行为；需要排除的伤害类型往
         * {@code data/arcanumlib/tags/damage_type/ignore_damage_multiplier.json} 里加即可（数据包也能改）。
         */
        public static final TagKey<DamageType> IGNORE_DAMAGE_MULTIPLIER =
                TagKey.create(Registries.DAMAGE_TYPE, ArcanumLib.id("ignore_damage_multiplier"));

    }
}