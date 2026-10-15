package com.chen1335.arcanumlib.modules.attribute;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/** 属性相关的服务端配置。 */
public final class AttributeConfig {
    public static final ModConfigSpec CONFIG_SPEC;

    /** 稀有判定阈值（权重占比）：低于它才吃寻觅加成；设为 0 等于关掉寻觅。 */
    public static final ModConfigSpec.DoubleValue RARE_SPAWN_WEIGHT_THRESHOLD;

    /** 吃额外战利品的表 id 路径前缀。 */
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BONUS_LOOT_PREFIXES;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Mob spawn settings").push("spawn");

        RARE_SPAWN_WEIGHT_THRESHOLD = builder
                .comment("Weight-share threshold (0~1) for rare mobs. A mob counts as rare when its share of the",
                        "whole spawn list is below this value. Rare mobs get weight x (1 + the highest Seeking",
                        "among players within 128 blocks). The boost itself is uncapped, but the attribute",
                        "value is clamped to 0~100, so at most x101. Other mobs are unaffected.",
                        "Set to 0 to disable the Seeking attribute entirely.",
                        "Default 0.02 (2%).")
                .defineInRange("rare_spawn_weight_threshold", 0.02D, 0.0D, 1.0D);

        builder.pop();

        builder.comment("Bonus loot settings").push("bonus_loot");

        BONUS_LOOT_PREFIXES = builder
                .comment("Loot table id path prefixes that get bonus loot. The default only covers",
                        "target drops (entities/) and chest loot (chests/).",
                        "Add your own prefixes here if other tables should be boosted too.")
                .defineListAllowEmpty("bonus_loot_prefixes", List.of("entities/", "chests/"),
                        () -> "entities/", o -> o instanceof String);

        builder.pop();

        CONFIG_SPEC = builder.build();
    }

    /** 生效的对额外战利品生效的表 id 路径前缀；配置未加载时退回默认的实体 + 箱子。 */
    public static List<String> bonusLootPrefixes() {
        return CONFIG_SPEC.isLoaded()
                ? List.copyOf(BONUS_LOOT_PREFIXES.get())
                : List.of("entities/", "chests/");
    }

    private AttributeConfig() {
    }

    /** 服务端配置在进入世界前没有加载，此时 get() 会抛异常，退回默认值。 */
    public static double threshold() {
        return CONFIG_SPEC.isLoaded()
                ? RARE_SPAWN_WEIGHT_THRESHOLD.get()
                : RARE_SPAWN_WEIGHT_THRESHOLD.getDefault();
    }
}
