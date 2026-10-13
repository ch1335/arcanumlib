package com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.ContainerComponentManipulator;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.functions.*;
import net.minecraft.world.level.storage.loot.providers.nbt.NbtProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;

import java.util.List;

/**
 * 战利品函数的脚本工厂：原版全部 40 个函数。
 *
 * <p>有公开 Builder 的那些直接调 Builder；其余用 {@link #ofType(String, JsonObject, HolderLookup.Provider)}。
 * 需要注册表的 Builder 会多一个 {@code registries} 参数，脚本里传 {@code event.registries}。
 */
public final class LootFunctionHelper {

    private LootFunctionHelper() {
    }

    @Info(value = "Parses any loot function from raw json through the vanilla codec.",
            params = {
                    @Param(name = "json", value = "the function json"),
                    @Param(name = "registries", value = "registry access, pass event.registries from a script")
            })
    public static LootItemFunction.Builder of(JsonElement json, HolderLookup.Provider registries) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);

        return () -> LootItemFunctions.ROOT_CODEC.parse(ops, json).getOrThrow();
    }

    @Info(value = "Builds a loot function from a type id plus its fields, e.g. ofType(`minecraft:set_count`, {count: 3}, registries).",
            params = {
                    @Param(name = "typeId", value = "the function id"),
                    @Param(name = "fields", value = "the rest of the json"),
                    @Param(name = "registries", value = "registry access")
            })
    public static LootItemFunction.Builder ofType(String typeId, JsonObject fields, HolderLookup.Provider registries) {
        JsonObject json = fields.deepCopy();
        json.addProperty("function", typeId);

        return of(json, registries);
    }

    @Info("Every registered loot function id, sorted.")
    public static List<String> ids() {
        return BuiltInRegistries.LOOT_FUNCTION_TYPE.keySet().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
    }

    @Info(value = "Sets the stack size to the value of the number provider.",
            params = {
                    @Param(name = "countValue", value = "the new size")
            })
    public static LootItemFunction.Builder setCount(NumberProvider countValue) {
        return SetItemCountFunction.setCount(countValue);
    }

    @Info(value = "Sets the stack size from the number provider.",
            params = {
                    @Param(name = "countValue", value = "the amount to use"),
                    @Param(name = "add", value = "when true it is added to the current count instead of replacing it")
            })
    public static LootItemFunction.Builder setCount(NumberProvider countValue, boolean add) {
        return SetItemCountFunction.setCount(countValue, add);
    }

    @Info(value = "Enchants the item as if it had been enchanted with the given number of levels.",
            params = {
                    @Param(name = "registries", value = "registry access for the enchantment pool"),
                    @Param(name = "levels", value = "how many levels to enchant as")
            })
    public static LootItemFunction.Builder enchantWithLevels(HolderLookup.Provider registries, NumberProvider levels) {
        return EnchantWithLevelsFunction.enchantWithLevels(registries, levels);
    }

    @Info("Enchants the item with one random enchantment.")
    public static LootItemFunction.Builder randomEnchantment() {
        return EnchantRandomlyFunction.randomEnchantment();
    }

    @Info("Enchants the item with one random enchantment.")
    public static LootItemFunction.Builder randomApplicableEnchantment(HolderLookup.Provider registries) {
        return EnchantRandomlyFunction.randomApplicableEnchantment(registries);
    }

    @Info("Merges NBT into the item's custom data component.")
    public static LootItemFunction.Builder setCustomData(CompoundTag tag) {
        return SetCustomDataFunction.setCustomData(tag);
    }

    @Info(value = "Adds or replaces a data component on the item.",
            params = {
                    @Param(name = "component", value = "which component to set"),
                    @Param(name = "value", value = "its new value")
            })
    public static <T> LootItemFunction.Builder setComponent(DataComponentType<T> component, T value) {
        return SetComponentsFunction.setComponent(component, value);
    }

    @Info("Smelts the dropped item as a furnace would.")
    public static LootItemFunction.Builder smelted() {
        return SmeltItemFunction.smelted();
    }

    @Info(value = "Increases the count based on the level of a given enchantment.",
            params = {
                    @Param(name = "registries", value = "registry access"),
                    @Param(name = "count", value = "how much is added per level of that enchantment")
            })
    public static LootItemFunction.Builder lootingMultiplier(HolderLookup.Provider registries, NumberProvider count) {
        return EnchantedCountIncreaseFunction.lootingMultiplier(registries, count);
    }

    @Info(value = "Sets the item's damage (durability).",
            params = {
                    @Param(name = "damageValue", value = "how much durability is left, as a fraction: 1.0 is undamaged, 0.0 is fully damaged")
            })
    public static LootItemFunction.Builder setDamage(NumberProvider damageValue) {
        return SetItemDamageFunction.setDamage(damageValue);
    }

    @Info(value = "Sets the item's damage (durability).",
            params = {
                    @Param(name = "damageValue", value = "how much durability is left, as a fraction: 1.0 is undamaged, 0.0 is fully damaged"),
                    @Param(name = "add", value = "when true it is added to the current remaining durability instead of replacing it")
            })
    public static LootItemFunction.Builder setDamage(NumberProvider damageValue, boolean add) {
        return SetItemDamageFunction.setDamage(damageValue, add);
    }

    @Info(value = "Adds or replaces attribute modifiers on the item.",
            params = {
                    @Param(name = "modifiers", value = "the modifiers to apply, built with attributeModifier(...)"),
                    @Param(name = "replace", value = "when true the modifiers already on the item are replaced, when false they are extended")
            })
    public static LootItemFunction.Builder setAttributes(List<SetAttributesFunction.ModifierBuilder> modifiers, boolean replace) {
        SetAttributesFunction.Builder builder = new SetAttributesFunction.Builder(replace);

        for (SetAttributesFunction.ModifierBuilder modifier : modifiers) {
            builder.withModifier(modifier);
        }

        return builder;
    }

    @Info(value = "Builds one attribute modifier for setAttributes(...). Chain forSlot(...) on the result to limit it to equipment slots.",
            params = {
                    @Param(name = "id", value = "the modifier id, used to tell modifiers apart"),
                    @Param(name = "attribute", value = "which attribute to modify"),
                    @Param(name = "operation", value = "how the amount is applied, such as add_value or multiply_total"),
                    @Param(name = "amount", value = "the amount, as a number provider")
            })
    public static SetAttributesFunction.ModifierBuilder attributeModifier(ResourceLocation id, Holder<Attribute> attribute,
                                                                         AttributeModifier.Operation operation, NumberProvider amount) {
        return SetAttributesFunction.modifier(id, attribute, operation, amount);
    }

    @Info(value = "Sets the name on the item stack. Depending on the target this writes the custom name or the item's base name.",
            params = {
                    @Param(name = "name", value = "the new name component"),
                    @Param(name = "target", value = "which name to write: CUSTOM_NAME for the custom (anvil) name, ITEM_NAME for the item's base name")
            })
    public static LootItemFunction.Builder setName(Component name, SetNameFunction.Target target) {
        return SetNameFunction.setName(name, target);
    }

    @Info(value = "Sets the name on the item stack, optionally resolving entity-sensitive parts of the name against the loot context.",
            params = {
                    @Param(name = "name", value = "the new name component"),
                    @Param(name = "target", value = "which name to write: CUSTOM_NAME for the custom (anvil) name, ITEM_NAME for the item's base name"),
                    @Param(name = "resolutionContext", value = "the entity of the loot context used to resolve entity-sensitive parts of the name, such as scoreboard scores")
            })
    public static LootItemFunction.Builder setName(Component name, SetNameFunction.Target target, LootContext.EntityTarget resolutionContext) {
        return SetNameFunction.setName(name, target, resolutionContext);
    }



    @Info("Copies the name from a block entity or an entity in the loot context.")
    public static LootItemFunction.Builder copyName(CopyNameFunction.NameSource source) {
        return CopyNameFunction.copyName(source);
    }
    @Info(value = "Fills a container item with the given entries.",
            params = {
                    @Param(name = "component", value = "which container component to fill, such as CONTAINER"),
                    @Param(name = "entries", value = "the entry builders describing what goes inside, such as EntryHelper.item(...)")
            })
    public static LootItemFunction.Builder setContents(ContainerComponentManipulator<?> component,
                                               List<LootPoolEntryContainer.Builder<?>> entries) {
        SetContainerContents.Builder builder = SetContainerContents.setContents(component);

        for (LootPoolEntryContainer.Builder<?> entry : entries) {
            builder.withEntry(entry);
        }

        return builder;
    }

    @Info("Limits the stack size to a minimum and/or a maximum.")
    public static LootItemFunction.Builder limitCount(IntRange countLimit) {
        return LimitCount.limitCount(countLimit);
    }

    @Info(value = "Adds extra rolls using a binomial distribution.",
            params = {
                    @Param(name = "enchantment", value = "the enchantment whose level is read"),
                    @Param(name = "probability", value = "the chance of each extra roll"),
                    @Param(name = "extraRounds", value = "how many extra rolls are attempted per enchantment level")
            })
    public static LootItemFunction.Builder addBonusBinomialDistributionCount(Holder<Enchantment> enchantment, float probability, int extraRounds) {
        return ApplyBonusCount.addBonusBinomialDistributionCount(enchantment, probability, extraRounds);
    }

    @Info("Applies a vanilla bonus formula based on an enchantment level.")
    public static LootItemFunction.Builder addOreBonusCount(Holder<Enchantment> enchantment) {
        return ApplyBonusCount.addOreBonusCount(enchantment);
    }

    @Info("Applies a vanilla bonus formula based on an enchantment level.")
    public static LootItemFunction.Builder addUniformBonusCount(Holder<Enchantment> enchantment) {
        return ApplyBonusCount.addUniformBonusCount(enchantment);
    }

    @Info(value = "Adds a uniformly random bonus count.",
            params = {
                    @Param(name = "enchantment", value = "the enchantment whose level is read"),
                    @Param(name = "bonusMultiplier", value = "how many extra rolls are attempted per enchantment level")
            })
    public static LootItemFunction.Builder addUniformBonusCount(Holder<Enchantment> enchantment, int bonusMultiplier) {
        return ApplyBonusCount.addUniformBonusCount(enchantment, bonusMultiplier);
    }

    @Info(value = "Stores a loot table id on a container block entity item.",
            params = {
                    @Param(name = "type", value = "the block entity type the table applies to"),
                    @Param(name = "toolTable", value = "the loot table to store")
            })
    public static LootItemFunction.Builder withLootTable(BlockEntityType<?> type, ResourceKey<LootTable> toolTable) {
        return SetContainerLootTable.withLootTable(type, toolTable);
    }

    @Info(value = "Stores a loot table id and seed on a container block entity item.",
            params = {
                    @Param(name = "type", value = "the block entity type"),
                    @Param(name = "lootTable", value = "the loot table to store"),
                    @Param(name = "seed", value = "the loot table seed")
            })
    public static LootItemFunction.Builder withLootTable(BlockEntityType<?> type, ResourceKey<LootTable> lootTable, long seed) {
        return SetContainerLootTable.withLootTable(type, lootTable, seed);
    }

    @Info("Reduces the count based on the explosion radius, as vanilla explosion decay does.")
    public static LootItemFunction.Builder explosionDecay() {
        return ApplyExplosionDecay.explosionDecay();
    }


    @Info("Fills a player head with the profile of a player in the loot context.")
    public static LootItemFunction.Builder fillPlayerHead(LootContext.EntityTarget entityTarget) {
        return FillPlayerHead.fillPlayerHead(entityTarget);
    }

    @Info(value = "Copies custom data onto the item from an NBT source, such as a block entity or command storage.",
            params = {
                    @Param(name = "source", value = "where the data is read from")
            })
    public static LootItemFunction.Builder copyDataFromNbt(NbtProvider source) {
        return CopyCustomDataFunction.copyData(source);
    }

    @Info(value = "Copies custom data onto the item from an entity in the loot context.",
            params = {
                    @Param(name = "target", value = "which entity of the loot context the data is read from")
            })
    public static LootItemFunction.Builder copyDataFromEntity(LootContext.EntityTarget target) {
        return CopyCustomDataFunction.copyData(target);
    }
    @Info(value = "Copies the given block state properties of the broken block onto the item.",
            params = {
                    @Param(name = "block", value = "the block whose state is read"),
                    @Param(name = "properties", value = "which properties to copy, such as BlockStateProperties.FACING")
            })
    public static LootItemFunction.Builder copyState(Block block, List<Property<?>> properties) {
        CopyBlockState.Builder builder = CopyBlockState.copyState(block);

        for (Property<?> property : properties) {
            builder.copy(property);
        }

        return builder;
    }
    @Info("One banner pattern layer: a pattern plus the colour it is drawn in.")
    public record BannerLayer(
            @Info("the banner pattern to apply") Holder<BannerPattern> pattern,
            @Info("the colour of that layer") DyeColor color) {
    }

    @Info(value = "Applies banner patterns to a banner item.",
            params = {
                    @Param(name = "append", value = "when true the patterns are appended to the existing ones instead of replacing them"),
                    @Param(name = "patterns", value = "the pattern layers to apply, in order")
            })
    public static LootItemFunction.Builder setBannerPattern(boolean append, List<BannerLayer> patterns) {
        SetBannerPatternFunction.Builder builder = SetBannerPatternFunction.setBannerPattern(append);

        for (BannerLayer layer : patterns) {
            builder.addPattern(layer.pattern(), layer.color());
        }

        return builder;
    }

    @Info("Sets the potion contents of the item.")
    public static LootItemFunction.Builder setPotion(Holder<Potion> potion) {
        return SetPotionFunction.setPotion(potion);
    }

    @Info("Sets the goat horn instrument.")
    public static LootItemFunction.Builder setInstrumentOptions(TagKey<Instrument> instrumentOptions) {
        return SetInstrumentFunction.setInstrumentOptions(instrumentOptions);
    }

    @Info("Delegates to another loot function by its id.")
    public static LootItemFunction.Builder functionReference(ResourceKey<LootItemFunction> key) {
        return FunctionReference.functionReference(key);
    }
    @Info(value = "Copies data components from a block entity or an entity in the loot context onto the item.",
            params = {
                    @Param(name = "source", value = "where the components are copied from"),
                    @Param(name = "include", value = "the component types to copy; leave empty and use exclude instead"),
                    @Param(name = "exclude", value = "the component types to skip; leave empty and use include instead")
            })
    public static LootItemFunction.Builder copyComponents(CopyComponentsFunction.Source source,
                                                 List<DataComponentType<?>> include, List<DataComponentType<?>> exclude) {
        CopyComponentsFunction.Builder builder = CopyComponentsFunction.copyComponents(source);

        for (DataComponentType<?> include_ : include) {
            builder.include(include_);
        }

        for (DataComponentType<?> exclude_ : exclude) {
            builder.exclude(exclude_);
        }

        return builder;
    }

    @Info("Sets the amplifier of an ominous bottle.")
    public static LootItemFunction.Builder setAmplifier(NumberProvider amplifier) {
        return SetOminousBottleAmplifierFunction.setAmplifier(amplifier);
    }

    @Info("Turns the item into a treasure map pointing at the nearest matching structure, using the vanilla defaults: destination on_treasure_maps, zoom 2, search radius 50 and skipping already generated chunks.")
    public static LootItemFunction.Builder makeExplorationMap() {
        return ExplorationMapFunction.makeExplorationMap();
    }

    @Info(value = "Turns the item into an exploration map.",
            params = {
                    @Param(name = "destination", value = "the structure tag the map points at, such as #minecraft:on_treasure_maps"),
                    @Param(name = "mapDecoration", value = "the icon drawn on the map"),
                    @Param(name = "zoom", value = "the map scale; a higher zoom shows a larger area"),
                    @Param(name = "searchRadius", value = "how far, in chunks, the structure is searched for"),
                    @Param(name = "skipKnownStructures", value = "when true, structures that already have a map are skipped")
            })
    public static LootItemFunction.Builder makeExplorationMap(TagKey<Structure> destination, Holder<MapDecorationType> mapDecoration,
                                                      byte zoom, int searchRadius, boolean skipKnownStructures) {
        return ExplorationMapFunction.makeExplorationMap()
                .setDestination(destination)
                .setMapDecoration(mapDecoration)
                .setZoom(zoom)
                .setSearchRadius(searchRadius)
                .setSkipKnownStructures(skipKnownStructures);
    }

    @Info("One suspicious stew effect: the effect plus how long it lasts.")
    public record StewEffect(
            @Info("the effect that is applied") Holder<MobEffect> effect,
            @Info("how long the effect lasts, as a number provider") NumberProvider duration) {
    }

    @Info(value = "Gives a suspicious stew its effects.",
            params = {
                    @Param(name = "effects", value = "the effects to apply; vanilla rejects an empty list")
            })
    public static LootItemFunction.Builder stewEffect(List<StewEffect> effects) {
        SetStewEffectFunction.Builder builder = SetStewEffectFunction.stewEffect();

        for (StewEffect effect : effects) {
            builder.withEffect(effect.effect(), effect.duration());
        }

        return builder;
    }

    @Info(value = "Sets the item's lore lines.",
            params = {
                    @Param(name = "lines", value = "the lore lines, in order"),
                    @Param(name = "mode", value = "how the lines combine with the existing lore, such as ListOperation.REPLACE_ALL"),
                    @Param(name = "resolutionContext", value = "the entity of the loot context used to resolve entity-sensitive parts of the lines, such as scoreboard scores")
            })
    public static LootItemFunction.Builder setLore(List<Component> lines, ListOperation mode, LootContext.EntityTarget resolutionContext) {
        SetLoreFunction.Builder builder = SetLoreFunction.setLore().setMode(mode).setResolutionContext(resolutionContext);

        for (Component line : lines) {
            builder.addLine(line);
        }

        return builder;
    }
}
