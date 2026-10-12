package com.chen1335.arcanumlib.kubejs.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.storage.loot.providers.number.BinomialDistributionGenerator;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.EnchantmentLevelProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;
import net.minecraft.world.level.storage.loot.providers.number.ScoreboardValue;
import net.minecraft.world.level.storage.loot.providers.number.StorageValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraft.world.level.storage.loot.providers.score.ScoreboardNameProvider;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

import java.util.List;

/**
 * 数字提供器的脚本工厂：本模组用到的原版数字提供器（{@code minecraft:constant} / {@code uniform} / {@code binomial} / {@code score} / {@code storage} / {@code enchantment_level}）。
 *
 * <p>{@link #of(JsonElement, HolderLookup.Provider)} / {@link #ofType(String, JsonObject, HolderLookup.Provider)} 走注册表里的 codec，任何数字提供器都能造；
 * {@link #ids()} 列出当前可用 id。
 */
public final class NumberProviderHelper {

    private NumberProviderHelper() {
    }

    @Info(value = "Parses any number provider from raw json through the registered codecs.",
            params = {
                    @Param(name = "json", value = "the provider json"),
                    @Param(name = "registries", value = "registry access, pass event.registries from a script")
            })
    public static NumberProvider of(JsonElement json, HolderLookup.Provider registries) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);

        return NumberProviders.CODEC.parse(ops, json).getOrThrow();
    }

    @Info(value = "Builds a number provider from a type id plus its fields, e.g. ofType(`minecraft:constant`, {value: 3}, registries).",
            params = {
                    @Param(name = "typeId", value = "the provider id"),
                    @Param(name = "fields", value = "the rest of the json"),
                    @Param(name = "registries", value = "registry access")
            })
    public static NumberProvider ofType(String typeId, JsonObject fields, HolderLookup.Provider registries) {
        JsonObject json = fields.deepCopy();
        json.addProperty("type", typeId);

        return of(json, registries);
    }

    @Info("Every registered number provider id, sorted.")
    public static List<String> ids() {
        return BuiltInRegistries.LOOT_NUMBER_PROVIDER_TYPE.keySet().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
    }

    @Info("Always the same value.")
    public static NumberProvider constant(float value) {
        return new ConstantValue(value);
    }

    @Info(value = "A random value between min and max, both inclusive.",
            params = {
                    @Param(name = "min", value = "the lower bound"),
                    @Param(name = "max", value = "the upper bound")
            })
    public static NumberProvider uniform(NumberProvider min, NumberProvider max) {
        return new UniformGenerator(min, max);
    }

    @Info(value = "A binomially distributed value.",
            params = {
                    @Param(name = "n", value = "how many trials are made"),
                    @Param(name = "p", value = "the success probability of each trial, from 0.0 to 1.0")
            })
    public static NumberProvider binomial(NumberProvider n, NumberProvider p) {
        return new BinomialDistributionGenerator(n, p);
    }

    @Info(value = "Reads a scoreboard value of an entity in the loot context.",
            params = {
                    @Param(name = "target", value = "which entity to read the score from"),
                    @Param(name = "score", value = "the objective name"),
                    @Param(name = "scale", value = "the value is multiplied by this")
            })
    public static NumberProvider score(ScoreboardNameProvider target, String score, float scale) {
        return new ScoreboardValue(target, score, scale);
    }

    @Info(value = "Reads a value from command storage.",
            params = {
                    @Param(name = "storage", value = "the command storage id"),
                    @Param(name = "path", value = "the NBT path to read inside it")
            })
    public static NumberProvider storage(ResourceLocation storage, NbtPathArgument.NbtPath path) {
        return new StorageValue(storage, path);
    }

    @Info("The enchantment level of the tool in the loot context, as a level based value.")
    public static NumberProvider enchantmentLevel(LevelBasedValue amount) {
        return new EnchantmentLevelProvider(amount);
    }
}
