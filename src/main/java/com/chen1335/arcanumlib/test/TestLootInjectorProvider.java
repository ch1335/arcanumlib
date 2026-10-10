package com.chen1335.arcanumlib.test;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifier;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifierCondition;
import com.chen1335.arcanumlib.modules.loot.lootEntry.AllOfEntry;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.EntryCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.PoolCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.TableCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.data.LootInjectorTemplate;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.EntryOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.PoolOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.TableOperator;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.TagEntry;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * loot 模块的综合数据生成测试，跑 {@code gradlew runData} 输出到
 * {@code src/generated/resources/data/arcanumlib/loot_injecter/}。
 *
 * <p>格式是三层嵌套作用域：顶层 ops 作用于表，{@code modify_pool} 的子 ops 作用于每个命中的池，
 * {@code modify_entry} 的子 ops 作用于每个命中的条目。条件只写在作用域上。
 *
 * <p>25 个 op 每种至少有一条 json；空 ops 的作用域就是纯选择器探针（只求值、不改表）。
 */
public class TestLootInjectorProvider extends LootInjectorTemplate {

    private static final String TEST_POOL_NAME = "arcanumlib_test_pool";
    /** 池级函数 op 用：单条目、必掉 1 个，叠加 add 模式的 set_count 后数量可直接肉眼核对。 */
    private static final String POOL_FUNC_NAME = "arcanumlib_test_poolfunc";
    private static final String POOL_SET_FUNC_NAME = "arcanumlib_test_setfunc";
    private static final ResourceLocation ZOMBIE_TABLE = EntityType.ZOMBIE.getDefaultLootTable().location();

    public TestLootInjectorProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, "ArcanumLib Test Loot Injectors");
    }

    private static ResourceLocation mc(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    @Override
    protected ResourceLocation id(String path) {
        return ArcanumLib.id(path);
    }

    /** 模板路径的概率条件，顺带覆盖附魔等级/实体属性/夹紧/难度系数四种加工器。 */
    @Override
    protected LootItemCondition.Builder chanceCondition(Drop drop) {
        return ChanceModifierCondition.builder(drop.chance(),
                new ChanceModifier.ToolEnchantmentLevel(Enchantments.LOOTING, ChanceModifier.Operation.ADD, drop.perLooting()),
                new ChanceModifier.EntityAttribute(Attributes.LUCK, ChanceModifier.EntitySource.THIS_ENTITY,
                        ChanceModifier.Operation.ADD, 0.01D),
                new ChanceModifier.Clamp(0.0D, 0.9D),
                ChanceModifier.DifficultyFactor.multiply(1.0D, 1.0D, 1.15D, 1.3D));
    }

    @Override
    protected List<LootInjector> injectors(HolderLookup.Provider registries) {
        List<LootInjector> list = new ArrayList<>();
        list.addAll(templateInjectors(registries));
        list.addAll(modifierInjectors());
        list.addAll(dungeonInjectors(registries));
        list.addAll(tableFunctionInjectors());
        list.addAll(tableConditionInjectors());
        list.add(selectorProbe());
        list.addAll(customInjectors());
        return List.copyOf(list);
    }

    // ---- 模板骨架 + 8 种 ChanceModifier ----

    /** 走模板骨架：merged / boss / injector + pools + allChildren + lootingCount + chanceCondition。 */
    private List<LootInjector> templateInjectors(HolderLookup.Provider registries) {
        return List.of(
                // 僵尸：三个各自独立判定的掉落，由默认 AllOfEntry 包住
                boss("test_zombie_basic", EntityType.ZOMBIE,
                        new Drop(0.25D, 0.02D, () -> LootItem.lootTableItem(Items.GOLD_INGOT)
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                                .apply(lootingCount(registries))),
                        new Drop(0.05D, () -> LootItem.lootTableItem(Items.EMERALD)),
                        new Drop(0.15D, () -> LootItem.lootTableItem(Items.IRON_INGOT))),

                // 实体标签选择器：盖住僵尸系全部实体的默认表
                injector("test_zombies_tag", new TableCondition.EntityTagTable(EntityTypeTags.ZOMBIES),
                        new Drop(0.10D, () -> LootItem.lootTableItem(Items.GOLD_NUGGET))),

                merged("test_temple", BuiltInLootTables.VILLAGE_TEMPLE,
                        new Drop(0.20D, () -> LootItem.lootTableItem(Items.EMERALD))));
    }

    /** 剩下 4 种加工器：set_chance / multiply / invert / add。 */
    private List<LootInjector> modifierInjectors() {
        return List.of(raw("test_zombie_modifiers", new TableCondition.ById(ZOMBIE_TABLE), 1000,
                allOfPool(
                        entry(Items.DIAMOND, 0.02D, new ChanceModifier.Multiply(2.0D)),
                        entry(Items.NETHERITE_SCRAP, 0.95D, new ChanceModifier.Invert()),
                        entry(Items.AMETHYST_SHARD, 0.02D, new ChanceModifier.Add(0.03D)),
                        entry(Items.LAPIS_LAZULI, new ChanceModifier.SetChance(0.08D)))));
    }

    // ---- 地牢：三层作用域的主战场 ----

    private List<LootInjector> dungeonInjectors(HolderLookup.Provider registries) {
        List<LootInjector> list = new ArrayList<>();
        TableCondition dungeon = new TableCondition.ById(BuiltInLootTables.SIMPLE_DUNGEON.location());

        // 表级 add_pool 塞具名池；池级 add_entry 按池下标选中地牢原有的第 0 个池
        list.add(raw("test_dungeon_add", dungeon, 900,
                new TableOperator.AddPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .setBonusRolls(ConstantValue.exactly(1.0F))
                        .name(TEST_POOL_NAME)
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT))
                        .build()),
                modifyPool(new PoolCondition.ByIndex(0), new PoolOperator.AddEntry(
                        LootItem.lootTableItem(Items.IRON_INGOT).build()))));

        // 自建 tag 条目与复合条目：原版表里全是扁平 item、也没有 tag 条目，
        // 不造这两个，IsTag / HasChild / IsType(alternatives) 在运行时永远命中不了
        list.add(raw("test_dungeon_entries", dungeon, 950,
                namedPool("arcanumlib_test_tag", TagEntry.tagContents(ItemTags.COALS)
                        .when(LootItemRandomChanceCondition.randomChance(1.0F))),
                namedPool("arcanumlib_test_alt", AlternativesEntry.alternatives(
                        LootItem.lootTableItem(Items.IRON_INGOT),
                        LootItem.lootTableItem(Items.COPPER_INGOT))),
                // 两条条目权重 1 与 3（总 4），占比 0.25 / 0.75，给 weight_share 用
                new TableOperator.AddPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .name("arcanumlib_test_share")
                        .add(LootItem.lootTableItem(Items.IRON_INGOT).setWeight(1))
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT).setWeight(3))
                        .build()),
                // 条件恒假 + 数量 3，被 remove_condition / remove_function 摘掉后应变成「必掉 1 个」
                namedPool("arcanumlib_test_remove", LootItem.lootTableItem(Items.EMERALD)
                        .when(LootItemRandomChanceCondition.randomChance(0.0F))
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(3.0F)))),
                namedPool(POOL_FUNC_NAME, LootItem.lootTableItem(Items.AMETHYST_SHARD)),
                namedPool(POOL_SET_FUNC_NAME, LootItem.lootTableItem(Items.QUARTZ))));

        // 条目级 replace_entry：地牢箱里的线就地换成皮革。新条目特意挂 enchanted_count_increase（引用附魔注册表），
        // 运行时必须走 RegistryOps 才编码得出来——用 JsonOps 会直接报 "Can't access registry"
        list.add(raw("test_dungeon_replace", dungeon, 1050,
                modifyPool(new PoolCondition.Any(), modifyEntry(new EntryCondition.IsItem(mc("string")),
                        new EntryOperator.ReplaceEntry(LootItem.lootTableItem(Items.LEATHER)
                                .apply(lootingCount(registries))
                                .build())))));

        // poolCondition 组合（name + total_weight）：合成池 arcanumlib_test_alt 是 alternatives，
        // 自身权重 0、两个叶子各 1，所以总权重为 2 才命中——不递归算的话这里是 0、不会命中
        list.add(raw("test_dungeon_weight", dungeon, 1060,
                modifyPool(new PoolCondition.AllOf(List.of(
                        new PoolCondition.ByName("arcanumlib_test_alt"),
                        new PoolCondition.TotalWeightInRange(2, 2))),
                        modifyEntry(new EntryCondition.Any(), new EntryOperator.RemoveEntry()))));

        // 条目级 set_function：只命中占比 0.75 那条（金锭）并给它套数量 5；铁锭占 0.25 不在区间内，保持 1 个
        list.add(raw("test_dungeon_share", dungeon, 1070,
                modifyPool(new PoolCondition.ByName("arcanumlib_test_share"),
                        modifyEntry(new EntryCondition.WeightShareInRange(0.7F, 0.8F),
                                new EntryOperator.SetFunction(List.of(
                                        SetItemCountFunction.setCount(ConstantValue.exactly(5.0F)).build()))))));

        // 池级函数三连：加 +2、摘掉、再加 +1，最终 +1（2 个紫水晶）。
        // 用 add=true 的 set_count，所以 remove_function 一旦失效就会变成 +3（4 个），肉眼可辨
        list.add(raw("test_pool_functions", dungeon, 1080,
                modifyPool(new PoolCondition.ByName(POOL_FUNC_NAME),
                        new PoolOperator.AddFunction(List.of(addCount(2))),
                        new PoolOperator.RemoveFunction(List.of(mc("set_count"))),
                        new PoolOperator.AddFunction(List.of(addCount(1))))));

        // 池级 set_function：整个池的函数列表被换成 +3，石英从 1 个变 4 个
        list.add(raw("test_pool_set_function", dungeon, 1090,
                modifyPool(new PoolCondition.ByName(POOL_SET_FUNC_NAME),
                        new PoolOperator.SetFunction(List.of(addCount(3))))));

        // 条目级：remove_entry 用 ByIndex 与 Any 两种池条件各自命中一次；
        // add_condition / add_function / set_weight / set_quality 都落在具名池的那条金锭上
        list.add(raw("test_dungeon_tune", dungeon, 1100,
                modifyPool(new PoolCondition.Any(), modifyEntry(new EntryCondition.IsItem(mc("gunpowder")),
                        new EntryOperator.RemoveEntry())),
                modifyPool(new PoolCondition.ByIndex(0), modifyEntry(new EntryCondition.IsItem(mc("saddle")),
                        new EntryOperator.RemoveEntry())),
                modifyPool(new PoolCondition.ByName(TEST_POOL_NAME), modifyEntry(new EntryCondition.Any(),
                        new EntryOperator.AddCondition(List.of(
                                LootItemRandomChanceCondition.randomChance(0.5F).build())),
                        new EntryOperator.AddFunction(List.of(
                                SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)).build())),
                        new EntryOperator.SetWeight(20),
                        new EntryOperator.SetQuality(1)))));

        // 条目级 set_condition / set_function：把 1100 加上的 50% 概率门换成必然通过、函数换成空列表，
        // 于是金锭变成「必掉 1 个」。若这两个 op 是追加语义，结果会是 allOf(0.5, 1.0) 仍然 50%、数量还是 2
        list.add(raw("test_dungeon_set_parts", dungeon, 1150,
                modifyPool(new PoolCondition.ByName(TEST_POOL_NAME), modifyEntry(new EntryCondition.Any(),
                        new EntryOperator.SetCondition(List.of(
                                LootItemRandomChanceCondition.randomChance(1.0F).build())),
                        new EntryOperator.SetFunction(List.of())))));

        // 条目级 remove_condition / remove_function：arcanumlib_test_remove 原本「random_chance 0.0 → 永不掉落
        // + set_count 3」，摘掉后应变成「必掉 1 个绿宝石」，两个 op 各自都有肉眼可见的翻转
        list.add(raw("test_dungeon_remove_parts", dungeon, 1160,
                modifyPool(new PoolCondition.ByName("arcanumlib_test_remove"), modifyEntry(new EntryCondition.Any(),
                        new EntryOperator.RemoveCondition(List.of(mc("random_chance"))),
                        new EntryOperator.RemoveFunction(List.of(mc("set_count")))))));

        // 池级 remove_pool + 方块标签选择器：木头方块不再掉东西
        list.add(raw("test_logs_remove_pool", new TableCondition.BlockTagTable(BlockTags.LOGS), 1200,
                modifyPool(new PoolCondition.Any(), new PoolOperator.RemovePool())));

        // TableCondition.All：效果是空操作（龙蛋不在任何表里），只把选择器与 remove_entry 跑一遍
        list.add(raw("test_all_tables", new TableCondition.All(), 1500,
                modifyPool(new PoolCondition.Any(), modifyEntry(new EntryCondition.IsItem(mc("dragon_egg")),
                        new EntryOperator.RemoveEntry()))));

        return list;
    }

    /** 表级函数三连：+2、摘掉、+1，最终每件 +1（若 remove_function 失效则是 +3）。 */
    private List<LootInjector> tableFunctionInjectors() {
        return List.of(
                raw("test_table_functions",
                        new TableCondition.ById(BuiltInLootTables.END_CITY_TREASURE.location()), 1000,
                        new TableOperator.AddFunction(List.of(addCount(2))),
                        new TableOperator.RemoveFunction(List.of(mc("set_count"))),
                        new TableOperator.AddFunction(List.of(addCount(1)))),

                // 表级 set_function：整个表的函数列表被换成 +2
                raw("test_table_set_function",
                        new TableCondition.ById(BuiltInLootTables.WOODLAND_MANSION.location()), 1000,
                        new TableOperator.SetFunction(List.of(addCount(2)))));
    }

    /** 表选择器剩余子类型：Namespace / PathPrefix / Not / AnyOf / AllOf。 */
    private List<LootInjector> tableConditionInjectors() {
        return List.of(
                raw("test_chests_except_end_city", new TableCondition.AllOf(List.of(
                        new TableCondition.Namespace("minecraft"),
                        new TableCondition.PathPrefix("chests/"),
                        new TableCondition.Not(new TableCondition.ById(BuiltInLootTables.END_CITY_TREASURE.location())))),
                        1000, addPool(Items.EMERALD, 0.05D)),

                raw("test_two_village_chests", new TableCondition.AnyOf(List.of(
                        new TableCondition.ById(BuiltInLootTables.VILLAGE_TEMPLE.location()),
                        new TableCondition.ById(BuiltInLootTables.VILLAGE_TOOLSMITH.location()))),
                        1000, addPool(Items.GOLD_NUGGET, 0.25D)));
    }

    /**
     * 池/条目选择器探针。
     *
     * <p>空 ops 的作用域只求值不改动，所以能把各选择器跑一遍；最后一条才是真改动（地牢箱子里火药 +1）。
     */
    private LootInjector selectorProbe() {
        List<TableOperator> ops = new ArrayList<>();

        ops.add(probeEntry(new EntryCondition.Any()));
        ops.add(probeEntry(new EntryCondition.ByIndex(0)));
        ops.add(probeEntry(new EntryCondition.IsItem(mc("golden_apple"))));
        ops.add(probeEntry(new EntryCondition.IsTag(ItemTags.COALS)));
        // 与上一行成对：地牢表里没有 tag 条目，但有 minecraft:coal 这个物品条目，所以只有这条会命中
        ops.add(probeEntry(new EntryCondition.IsItemWithTag(ItemTags.COALS)));
        ops.add(probeEntry(new EntryCondition.IsType(mc("item"))));
        ops.add(probeEntry(new EntryCondition.IsType(mc("alternatives"))));
        ops.add(probeEntry(new EntryCondition.WeightInRange(10, Integer.MAX_VALUE)));
        ops.add(probeEntry(new EntryCondition.HasCondition(mc("random_chance"))));
        ops.add(probeEntry(new EntryCondition.HasFunction(mc("set_count"))));
        ops.add(probeEntry(new EntryCondition.HasChild(new EntryCondition.IsItem(mc("iron_ingot")))));
        ops.add(probeEntry(new EntryCondition.Not(new EntryCondition.IsItem(mc("golden_apple")))));
        ops.add(probeEntry(new EntryCondition.AnyOf(List.of(
                new EntryCondition.IsItem(mc("golden_apple")),
                new EntryCondition.IsItem(mc("gunpowder"))))));
        ops.add(probeEntry(new EntryCondition.AllOf(List.of(
                new EntryCondition.IsType(mc("item")),
                new EntryCondition.WeightInRange(1, 1)))));

        ops.add(probePool(new PoolCondition.Any()));
        ops.add(probePool(new PoolCondition.ByName("main")));
        ops.add(probePool(new PoolCondition.HasEntry(new EntryCondition.IsItem(mc("gunpowder")))));
        ops.add(probePool(new PoolCondition.Not(new PoolCondition.ByIndex(99))));
        ops.add(probePool(new PoolCondition.AnyOf(List.of(
                new PoolCondition.ByIndex(0), new PoolCondition.ByIndex(1)))));
        ops.add(probePool(new PoolCondition.AllOf(List.of(
                new PoolCondition.Any(), new PoolCondition.Not(new PoolCondition.ByIndex(99))))));

        ops.add(modifyPool(new PoolCondition.Any(), modifyEntry(new EntryCondition.IsItem(mc("gunpowder")),
                new EntryOperator.AddFunction(List.of(
                        SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)).build())))));

        return raw("test_selector_probes", new TableCondition.ById(BuiltInLootTables.SIMPLE_DUNGEON.location()),
                1000, ops.toArray(TableOperator[]::new));
    }

    /** 三类自定义 op 与三类自定义选择器；实际行为由 {@link TestCustomLootHandlers} 决定。 */
    private List<LootInjector> customInjectors() {
        return List.of(
                raw("test_custom", new TableCondition.Custom("test_table"), 1000,
                        new TableOperator.TableCustom("test_table_custom"),
                        modifyPool(new PoolCondition.Custom("test_pool"),
                                new PoolOperator.PoolCustom("test_pool_custom"),
                                modifyEntry(new EntryCondition.Custom("test_entry"),
                                        new EntryOperator.EntryCustom("test_entry_custom")))),

                raw("test_apply_event", new TableCondition.ById(BuiltInLootTables.ABANDONED_MINESHAFT.location()), 1300,
                        addPool(Items.COAL, 0.10D)));
    }

    // ---- 组装辅助 --------------------------------------------------------

    private LootInjector raw(String path, TableCondition tableCondition, int priority, TableOperator... ops) {
        return new LootInjector(id(path), tableCondition, priority, List.of(ops));
    }

    private TableOperator.AddPool addPool(ItemLike item, double chance) {
        return allOfPool(entry(item, chance));
    }

    private TableOperator.AddPool allOfPool(LootPoolEntryContainer.Builder<?>... children) {
        return new TableOperator.AddPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(AllOfEntry.allOf(List.of(children)))
                .build());
    }

    /** 一个具名池，池里只有传进来的那个条目。 */
    private TableOperator.AddPool namedPool(String name, LootPoolEntryContainer.Builder<?> entry) {
        return new TableOperator.AddPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .name(name)
                .add(entry)
                .build());
    }

    private TableOperator modifyPool(PoolCondition poolCondition, PoolOperator... ops) {
        return new TableOperator.ModifyPool(poolCondition, List.of(ops));
    }

    private PoolOperator modifyEntry(EntryCondition entryCondition, EntryOperator... ops) {
        return new PoolOperator.ModifyEntry(entryCondition, List.of(ops));
    }

    private LootPoolEntryContainer.Builder<?> entry(ItemLike item, double baseChance, ChanceModifier... modifiers) {
        return LootItem.lootTableItem(item).when(ChanceModifierCondition.builder(baseChance, modifiers));
    }

    private LootPoolEntryContainer.Builder<?> entry(ItemLike item, ChanceModifier... modifiers) {
        return entry(item, 0.5D, modifiers);
    }

    /** 追加数量的 set_count：多个叠加会累加，所以"少了一个"能被肉眼看出来。 */
    private static LootItemFunction addCount(int count) {
        return SetItemCountFunction.setCount(ConstantValue.exactly(count), true).build();
    }

    /** 空 ops 的条目作用域：只求值选择器，不改动表。 */
    private static TableOperator probeEntry(EntryCondition entryCondition) {
        return new TableOperator.ModifyPool(new PoolCondition.Any(),
                List.of(new PoolOperator.ModifyEntry(entryCondition, List.of())));
    }

    /** 空 ops 的池作用域：只求值池选择器。 */
    private static TableOperator probePool(PoolCondition poolCondition) {
        return new TableOperator.ModifyPool(poolCondition, List.of());
    }
}
