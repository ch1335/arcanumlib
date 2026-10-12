package com.chen1335.arcanumlib.kubejs.modules.lootInjector.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.chen1335.arcanumlib.modules.loot.lootEntry.AllOfEntry;
import com.mojang.serialization.JsonOps;
import dev.latvian.mods.kubejs.typings.Info;
import dev.latvian.mods.kubejs.typings.Param;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.DynamicLoot;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.EntryGroup;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.entries.SequentialEntry;
import net.minecraft.world.level.storage.loot.entries.TagEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 战利品条目（战利品池里的一条）的脚本工厂：原版全部 8 种条目类型。
 *
 * <p>每个方法都有两个版本：
 * <ul>
 *     <li><b>直接版</b>：返回建好的 {@link LootPoolEntryContainer}，可以直接喂给
 *         {@code EntryOperatorHelper.addChild(...)} / {@code PoolOperatorHelper.addEntry(...)}；</li>
 *     <li><b>配置版</b>（多一个 {@code Consumer}）：先拿到原版 Builder，让你挂条件 / 权重 / 函数，
 *         收尾时自动 {@code build()}：
 *         <pre>{@code
 *         LootEntryHelper.item('minecraft:diamond', builder => builder
 *                 .setWeight(3)
 *                 .when(someCondition)
 *                 .apply(someFunction))
 *         }</pre>
 *         条件用 {@code when(...)}（所有条目都有），权重 / quality / 函数用
 *         {@code setWeight(...)} / {@code setQuality(...)} / {@code apply(...)}（叶子条目才有）。
 *     </li>
 * </ul>
 *
 * <p>合成条目（alternatives / group / sequence）收的是**建好的子条目**，内部会包成 Builder，
 * 所以脚本里可以直接嵌套，也可以给整组加条件。
 *
 * <p>需要这些工厂覆盖不到的写法时，用 {@link #ofType(String, JsonObject, HolderLookup.Provider)}
 * 直接写条目 json；{@link #ids()} 列出当前可用的条目 id。
 */
public final class LootEntryHelper {

    private LootEntryHelper() {
    }

    // ---------------------------------------------------------------- 通用入口

    @Info(value = "Parses any loot pool entry from raw json through the vanilla entry codec.",
            params = {
                    @Param(name = "json", value = "the entry json, exactly as it appears in a loot table"),
                    @Param(name = "registries", value = "registry access, pass event.registries from a script")
            })
    public static LootPoolEntryContainer of(JsonElement json, HolderLookup.Provider registries) {
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);

        return LootPoolEntries.CODEC.parse(ops, json).getOrThrow();
    }

    @Info(value = "Builds an entry from a type id plus its fields, e.g. ofType(`minecraft:item`, {name: `minecraft:diamond`, weight: 3}, registries).",
            params = {
                    @Param(name = "typeId", value = "the entry type id, such as minecraft:item or minecraft:alternatives"),
                    @Param(name = "fields", value = "the rest of the entry json"),
                    @Param(name = "registries", value = "registry access, pass event.registries from a script")
            })
    public static LootPoolEntryContainer ofType(String typeId, JsonObject fields, HolderLookup.Provider registries) {
        JsonObject json = fields.deepCopy();
        json.addProperty("type", typeId);

        return of(json, registries);
    }

    @Info("Every registered loot pool entry type id, sorted.")
    public static List<String> ids() {
        return BuiltInRegistries.LOOT_POOL_ENTRY_TYPE.keySet().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .toList();
    }

    // ---------------------------------------------------------------- 叶子条目

    @Info(value = "Drops the given item.", params = {
            @Param(name = "item", value = "the item to drop, such as `minecraft:diamond` or an ItemStack")
    })
    public static LootPoolEntryContainer item(ItemLike item) {
        return LootItem.lootTableItem(item).build();
    }

    @Info(value = "Drops the given item, letting you configure the entry builder first: setWeight(...), setQuality(...), when(condition) and apply(function).",
            params = {
                    @Param(name = "item", value = "the item to drop, such as `minecraft:diamond` or an ItemStack"),
                    @Param(name = "config", value = "called with the vanilla entry builder before it is built, e.g. builder => builder.setWeight(3)")
            })
    public static LootPoolEntryContainer item(ItemLike item, Consumer<LootPoolSingletonContainer.Builder<?>> config) {
        LootPoolSingletonContainer.Builder<?> builder = LootItem.lootTableItem(item);
        config.accept(builder);

        return builder.build();
    }

    @Info(value = "Drops the contents of an item tag as a single entry, one item chosen from the tag.", params = {
            @Param(name = "tag", value = "the item tag to draw from, such as #minecraft:planks")
    })
    public static LootPoolEntryContainer tagContents(TagKey<Item> tag) {
        return TagEntry.tagContents(tag).build();
    }

    @Info(value = "Drops the contents of an item tag as a single entry, letting you configure the entry builder first.",
            params = {
                    @Param(name = "tag", value = "the item tag to draw from, such as #minecraft:planks"),
                    @Param(name = "config", value = "called with the vanilla entry builder before it is built")
            })
    public static LootPoolEntryContainer tagContents(TagKey<Item> tag, Consumer<LootPoolSingletonContainer.Builder<?>> config) {
        LootPoolSingletonContainer.Builder<?> builder = TagEntry.tagContents(tag);
        config.accept(builder);

        return builder.build();
    }

    @Info(value = "Expands the item tag into one entry per item it contains.", params = {
            @Param(name = "tag", value = "the item tag to expand, such as #minecraft:planks")
    })
    public static LootPoolEntryContainer expandTag(TagKey<Item> tag) {
        return TagEntry.expandTag(tag).build();
    }

    @Info(value = "Expands the item tag into one entry per item it contains, letting you configure the entry builder first.",
            params = {
                    @Param(name = "tag", value = "the item tag to expand, such as #minecraft:planks"),
                    @Param(name = "config", value = "called with the vanilla entry builder before it is built")
            })
    public static LootPoolEntryContainer expandTag(TagKey<Item> tag, Consumer<LootPoolSingletonContainer.Builder<?>> config) {
        LootPoolSingletonContainer.Builder<?> builder = TagEntry.expandTag(tag);
        config.accept(builder);

        return builder.build();
    }

    @Info(value = "Rolls another loot table by reference.", params = {
            @Param(name = "lootTable", value = "the id of the loot table to roll")
    })
    public static LootPoolEntryContainer lootTableReference(ResourceKey<LootTable> lootTable) {
        return NestedLootTable.lootTableReference(lootTable).build();
    }

    @Info(value = "Rolls another loot table by reference, letting you configure the entry builder first.",
            params = {
                    @Param(name = "lootTable", value = "the id of the loot table to roll"),
                    @Param(name = "config", value = "called with the vanilla entry builder before it is built")
            })
    public static LootPoolEntryContainer lootTableReference(ResourceKey<LootTable> lootTable, Consumer<LootPoolSingletonContainer.Builder<?>> config) {
        LootPoolSingletonContainer.Builder<?> builder = NestedLootTable.lootTableReference(lootTable);
        config.accept(builder);

        return builder.build();
    }

    @Info(value = "Rolls another loot table that is embedded in this one.", params = {
            @Param(name = "lootTable", value = "the loot table to embed")
    })
    public static LootPoolEntryContainer inlineLootTable(LootTable lootTable) {
        return NestedLootTable.inlineLootTable(lootTable).build();
    }

    @Info(value = "Rolls another loot table that is embedded in this one, letting you configure the entry builder first.",
            params = {
                    @Param(name = "lootTable", value = "the loot table to embed"),
                    @Param(name = "config", value = "called with the vanilla entry builder before it is built")
            })
    public static LootPoolEntryContainer inlineLootTable(LootTable lootTable, Consumer<LootPoolSingletonContainer.Builder<?>> config) {
        LootPoolSingletonContainer.Builder<?> builder = NestedLootTable.inlineLootTable(lootTable);
        config.accept(builder);

        return builder.build();
    }

    @Info(value = "Drops whatever the game puts in the given dynamic drop id, such as a shulker box's contents.", params = {
            @Param(name = "dynamicDropsName", value = "the dynamic drop id, such as minecraft:shulker_box")
    })
    public static LootPoolEntryContainer dynamicEntry(ResourceLocation dynamicDropsName) {
        return DynamicLoot.dynamicEntry(dynamicDropsName).build();
    }

    @Info(value = "Drops whatever the game puts in the given dynamic drop id, letting you configure the entry builder first.",
            params = {
                    @Param(name = "dynamicDropsName", value = "the dynamic drop id, such as minecraft:shulker_box"),
                    @Param(name = "config", value = "called with the vanilla entry builder before it is built")
            })
    public static LootPoolEntryContainer dynamicEntry(ResourceLocation dynamicDropsName, Consumer<LootPoolSingletonContainer.Builder<?>> config) {
        LootPoolSingletonContainer.Builder<?> builder = DynamicLoot.dynamicEntry(dynamicDropsName);
        config.accept(builder);

        return builder.build();
    }

    @Info("Contributes nothing; useful as a placeholder or to attach conditions to.")
    public static LootPoolEntryContainer emptyItem() {
        return EmptyLootItem.emptyItem().build();
    }

    @Info(value = "Contributes nothing, letting you configure the entry builder first; useful as a placeholder or to attach conditions to.",
            params = {
                    @Param(name = "config", value = "called with the vanilla entry builder before it is built")
            })
    public static LootPoolEntryContainer emptyItem(Consumer<LootPoolSingletonContainer.Builder<?>> config) {
        LootPoolSingletonContainer.Builder<?> builder = EmptyLootItem.emptyItem();
        config.accept(builder);

        return builder.build();
    }

    // ---------------------------------------------------------------- 合成条目

    @Info(value = "Tries the children in order and stops at the first one that produces loot, so the order is the priority.", params = {
            @Param(name = "children", value = "the child entries, highest priority first")
    })
    public static LootPoolEntryContainer alternatives(List<LootPoolEntryContainer> children) {
        return AlternativesEntry.alternatives(wrap(children)).build();
    }

    @Info(value = "Tries the children in order and stops at the first one that produces loot, letting you configure the group builder first, e.g. to put a condition on the whole group.",
            params = {
                    @Param(name = "children", value = "the child entries, highest priority first"),
                    @Param(name = "config", value = "called with the vanilla alternatives builder before it is built")
            })
    public static LootPoolEntryContainer alternatives(List<LootPoolEntryContainer> children, Consumer<AlternativesEntry.Builder> config) {
        AlternativesEntry.Builder builder = AlternativesEntry.alternatives(wrap(children));
        config.accept(builder);

        return builder.build();
    }

    @Info(value = "Puts the children in one group; the group behaves as a single entry, which is handy to share weight or conditions.", params = {
            @Param(name = "children", value = "the child entries of the group")
    })
    public static LootPoolEntryContainer list(List<LootPoolEntryContainer> children) {
        return EntryGroup.list(wrap(children)).build();
    }

    @Info(value = "Puts the children in one group, letting you configure the group builder first, e.g. to put a condition on the whole group.",
            params = {
                    @Param(name = "children", value = "the child entries of the group"),
                    @Param(name = "config", value = "called with the vanilla group builder before it is built")
            })
    public static LootPoolEntryContainer list(List<LootPoolEntryContainer> children, Consumer<EntryGroup.Builder> config) {
        EntryGroup.Builder builder = EntryGroup.list(wrap(children));
        config.accept(builder);

        return builder.build();
    }

    @Info(value = "Runs the children one after another and collects everything they produce.", params = {
            @Param(name = "children", value = "the child entries, run in this order")
    })
    public static LootPoolEntryContainer sequential(List<LootPoolEntryContainer> children) {
        return SequentialEntry.sequential(wrap(children)).build();
    }

    @Info(value = "Runs the children one after another and collects everything they produce, letting you configure the sequence builder first.",
            params = {
                    @Param(name = "children", value = "the child entries, run in this order"),
                    @Param(name = "config", value = "called with the vanilla sequence builder before it is built")
            })
    public static LootPoolEntryContainer sequential(List<LootPoolEntryContainer> children, Consumer<SequentialEntry.Builder> config) {
        SequentialEntry.Builder builder = SequentialEntry.sequential(wrap(children));
        config.accept(builder);

        return builder.build();
    }

    // ---------------------------------------------------------------- 本模组的条目

    @Info(value = "ArcanumLib's own `arcanumlib:all_of` entry: once this entry is picked, every child is produced. Use it when several drops in one pool should each roll independently; vanilla group and sequence only ever produce one child per roll.",
            params = {
                    @Param(name = "children", value = "the child entries, all of them are produced")
            })
    public static LootPoolEntryContainer allOf(List<LootPoolEntryContainer> children) {
        return AllOfEntry.allOf(wrapList(children)).build();
    }

    @Info(value = "ArcanumLib's own `arcanumlib:all_of` entry, letting you configure the entry builder first: when(condition) and apply(function).",
            params = {
                    @Param(name = "children", value = "the child entries, all of them are produced"),
                    @Param(name = "config", value = "called with the all_of builder before it is built")
            })
    public static LootPoolEntryContainer allOf(List<LootPoolEntryContainer> children, Consumer<AllOfEntry.Builder> config) {
        AllOfEntry.Builder builder = AllOfEntry.allOf(wrapList(children));
        config.accept(builder);

        return builder.build();
    }

    // ---------------------------------------------------------------- 内部：把建好的条目包成 Builder

    private static LootPoolEntryContainer.Builder<?> wrap(LootPoolEntryContainer entry) {
        return new BuiltEntryBuilder(entry);
    }

    private static LootPoolEntryContainer.Builder<?>[] wrap(List<LootPoolEntryContainer> entries) {
        LootPoolEntryContainer.Builder<?>[] builders = new LootPoolEntryContainer.Builder<?>[entries.size()];

        for (int i = 0; i < entries.size(); i++) {
            builders[i] = wrap(entries.get(i));
        }

        return builders;
    }

    /** 已经建好的条目转成 List 形式的 Builder，给收 List 的原版/本模组工厂用（如 all_of）。 */
    private static List<LootPoolEntryContainer.Builder<?>> wrapList(List<LootPoolEntryContainer> entries) {
        List<LootPoolEntryContainer.Builder<?>> builders = new ArrayList<>(entries.size());

        for (LootPoolEntryContainer entry : entries) {
            builders.add(wrap(entry));
        }

        return builders;
    }

    /** 已经建好的条目套一层 Builder，好让合成条目能把它当孩子收下。 */
    private static final class BuiltEntryBuilder extends LootPoolEntryContainer.Builder<BuiltEntryBuilder> {
        private final LootPoolEntryContainer entry;

        private BuiltEntryBuilder(LootPoolEntryContainer entry) {
            this.entry = entry;
        }

        @Override
        protected BuiltEntryBuilder getThis() {
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return this.entry;
        }
    }
}
