package com.chen1335.arcanumlib.kubejs.helper;

import com.chen1335.arcanumlib.compat.astages.HasStageCondition;
import com.chen1335.arcanumlib.compat.astages.StagePattern;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifier;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifierCondition;
import org.jetbrains.annotations.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.*;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 战利品条件的脚本工厂：本模组的条件 + **所有原版条件**。
 *
 * <p>分两类用法：
 * <ul>
 *     <li><b>类型安全</b>：原版里凡是 {@code public record} 的条件都有同名工厂，参数与它的组件一一对应；</li>
 *     <li><b>通用兜底</b>：{@link #of(JsonElement, net.minecraft.core.HolderLookup.Provider)} 与
 *         {@link #ofType(String, JsonObject, net.minecraft.core.HolderLookup.Provider)} 走注册表里的 codec，
 *         原版 / 本模组 / 别的模组的条件都能造 —— 这也是包私有条件类的唯一入口。</li>
 * </ul>
 *
 * <p>{@link #ids()} 能列出当前所有已注册的条件 id，方便在脚本里核对可用值。
 */
public final class LootConditionHelper {

    private LootConditionHelper() {
    }

    /**
     * 从 json 造任意条件：走 {@code LootItemCondition.DIRECT_CODEC}，原版 / 各模组的都能用。
     */
    @Info("Parses any loot condition from raw json through the registered codecs. Works for vanilla, this mod and any other mod, including conditions whose classes are not public.")
    public static LootItemCondition of(JsonElement json, net.minecraft.core.HolderLookup.Provider registries) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);

        return LootItemCondition.DIRECT_CODEC.parse(ops, json).getOrThrow();
    }

    /**
     * 按类型 id + 字段造条件，例如 {@code ofType("minecraft:random_chance", fields, registries)}。
     */
    @Info("Passes with the given probability (0.0 to 1.0).")
    public static LootItemCondition ofType(String typeId, JsonObject fields, net.minecraft.core.HolderLookup.Provider registries) {
        JsonObject json = fields.deepCopy();
        json.addProperty("condition", typeId);

        return of(json, registries);
    }

    /**
     * 当前所有已注册的条件 id（原版 + 各模组），已排序。
     */
    @Info("Every registered loot condition id (vanilla, this mod and other mods), sorted. Handy to check what is available in a script.")
    public static List<String> ids() {
        return BuiltInRegistries.LOOT_CONDITION_TYPE.keySet().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
    }

    /**
     * 本模组 {@code arcanumlib:chance_modifiers}。
     */
    @Info("`arcanumlib:chance_modifiers`: starts from baseChance and runs it through the given chance modifiers.")
    public static LootItemCondition chanceModifiers(double baseChance, List<ChanceModifier> modifiers) {
        return new ChanceModifierCondition(baseChance, modifiers);
    }

    /**
     * 本模组 {@code arcanumlib:has_stage}（装了 AStages 时才有意义）。
     */
    @Info("`arcanumlib:has_stage`: passes only when the player or world matches the given stage pattern (requires AStages).")
    public static LootItemCondition hasStage(StagePattern stage, @Nullable String desc, boolean checkServerStages) {
        return new HasStageCondition(stage, Optional.ofNullable(desc), checkServerStages);
    }

    /**
     * 原版 {@code minecraft:inverted}（{@code InvertedLootItemCondition}）。
     */
    @Info("minecraft:inverted — passes when the wrapped condition fails.")
    public static LootItemCondition inverted(LootItemCondition term) {
        return new InvertedLootItemCondition(term);
    }

    /**
     * 原版 {@code minecraft:random_chance}（{@code LootItemRandomChanceCondition}）。
     */
    @Info("minecraft:random_chance — passes with the given probability, from 0.0 to 1.0.")
    public static LootItemCondition randomChance(NumberProvider chance) {
        return new LootItemRandomChanceCondition(chance);
    }

    /**
     * 原版 {@code minecraft:random_chance_with_enchanted_bonus}（{@code LootItemRandomChanceWithEnchantedBonusCondition}）。
     */
    @Info("minecraft:random_chance_with_enchanted_bonus — passes with a chance that grows with the tool's enchantment level.")
    public static LootItemCondition randomChanceWithEnchantedBonus(float unenchantedChance, LevelBasedValue enchantedChance, Holder<Enchantment> enchantment) {
        return new LootItemRandomChanceWithEnchantedBonusCondition(unenchantedChance, enchantedChance, enchantment);
    }

    /**
     * 原版 {@code minecraft:entity_properties}（{@code LootItemEntityPropertyCondition}）。
     */
    @Info("minecraft:entity_properties — tests properties of an entity in the loot context, such as this, killer or player.")
    public static LootItemCondition entityProperties(@Nullable EntityPredicate predicate, LootContext.EntityTarget entityTarget) {
        return new LootItemEntityPropertyCondition(Optional.ofNullable(predicate), entityTarget);
    }

    /**
     * 原版 {@code minecraft:entity_scores}（{@code EntityHasScoreCondition}）。
     */
    @Info("minecraft:entity_scores — tests the scoreboard values of an entity in the loot context.")
    public static LootItemCondition entityScores(Map<String, IntRange> scores, LootContext.EntityTarget entityTarget) {
        return new EntityHasScoreCondition(scores, entityTarget);
    }

    /**
     * 原版 {@code minecraft:block_state_property}（{@code LootItemBlockStatePropertyCondition}）。
     */
    @Info("minecraft:block_state_property — tests the block state of the block that was broken.")
    public static LootItemCondition blockStateProperty(Holder<Block> block, @Nullable StatePropertiesPredicate properties) {
        return new LootItemBlockStatePropertyCondition(block, Optional.ofNullable(properties));
    }

    /**
     * 原版 {@code minecraft:match_tool}（{@code MatchTool}）。
     */
    @Info("minecraft:match_tool — tests the tool that was used to break the block.")
    public static LootItemCondition matchTool(@Nullable ItemPredicate predicate) {
        return new MatchTool(Optional.ofNullable(predicate));
    }

    /**
     * 原版 {@code minecraft:table_bonus}（{@code BonusLevelTableCondition}）。
     */
    @Info("minecraft:table_bonus — passes with a chance picked from a per-enchantment-level table.")
    public static LootItemCondition tableBonus(Holder<Enchantment> enchantment, List<Float> values) {
        return new BonusLevelTableCondition(enchantment, values);
    }

    /**
     * 原版 {@code minecraft:damage_source_properties}（{@code DamageSourceCondition}）。
     */
    @Info("minecraft:damage_source_properties — tests properties of the damage source that killed the entity.")
    public static LootItemCondition damageSourceProperties(@Nullable DamageSourcePredicate predicate) {
        return new DamageSourceCondition(Optional.ofNullable(predicate));
    }

    /**
     * 原版 {@code minecraft:location_check}（{@code LocationCheck}）。
     */
    @Info("minecraft:location_check — tests the location of the loot context, such as biome, light level or block state.")
    public static LootItemCondition locationCheck(@Nullable LocationPredicate predicate, BlockPos offset) {
        return new LocationCheck(Optional.ofNullable(predicate), offset);
    }

    /**
     * 原版 {@code minecraft:weather_check}（{@code WeatherCheck}）。
     */
    @Info("minecraft:weather_check — tests whether it is raining or thundering.")
    public static LootItemCondition weatherCheck(@Nullable Boolean isRaining, @Nullable Boolean isThundering) {
        return new WeatherCheck(Optional.ofNullable(isRaining), Optional.ofNullable(isThundering));
    }

    /**
     * 原版 {@code minecraft:reference}（{@code ConditionReference}）。
     */
    @Info("minecraft:reference — delegates to another loot condition by its id.")
    public static LootItemCondition reference(ResourceKey<LootItemCondition> name) {
        return new ConditionReference(name);
    }

    /**
     * 原版 {@code minecraft:time_check}（{@code TimeCheck}）。
     */
    @Info("minecraft:time_check — tests the world time, optionally with a modulo period.")
    public static LootItemCondition timeCheck(@Nullable Long period, IntRange value) {
        return new TimeCheck(Optional.ofNullable(period), value);
    }

    /**
     * 原版 {@code minecraft:value_check}（{@code ValueCheckCondition}）。
     */
    @Info("minecraft:value_check — tests a number provider against a range.")
    public static LootItemCondition valueCheck(NumberProvider provider, IntRange range) {
        return new ValueCheckCondition(provider, range);
    }

    /**
     * 原版 {@code minecraft:enchantment_active_check}（{@code EnchantmentActiveCheck}）。
     */
    @Info("minecraft:enchantment_active_check — passes when the given enchantment is active in the loot context.")
    public static LootItemCondition enchantmentActiveCheck(boolean active) {
        return new EnchantmentActiveCheck(active);
    }
}
