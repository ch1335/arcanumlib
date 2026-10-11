package com.chen1335.arcanumlib.test;

import com.chen1335.arcanumlib.ArcanumLib;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifier;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifierCondition;
import com.chen1335.arcanumlib.modules.loot.lootEntry.AllOfEntry;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.EntrySelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.PoolSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.TableSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.data.LootInjectorTemplate;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.ConditionOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.EntryOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.FunctionOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.PoolOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.operator.TableOperator;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.ConditionSelector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.selector.FunctionSelector;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
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
        list.addAll(tableSelectorInjectors());
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
                injector("test_zombies_tag", new TableSelector.EntityTagTable(EntityTypeTags.ZOMBIES),
                        new Drop(0.10D, () -> LootItem.lootTableItem(Items.GOLD_NUGGET))),

                merged("test_temple", BuiltInLootTables.VILLAGE_TEMPLE,
                        new Drop(0.20D, () -> LootItem.lootTableItem(Items.EMERALD))));
    }

    /** 剩下 4 种加工器：set_chance / multiply / invert / add。 */
    private List<LootInjector> modifierInjectors() {
        return List.of(raw("test_zombie_modifiers", new TableSelector.ById(ZOMBIE_TABLE), 1000,
                allOfPool(
                        entry(Items.DIAMOND, 0.02D, new ChanceModifier.Multiply(2.0D)),
                        entry(Items.NETHERITE_SCRAP, 0.95D, new ChanceModifier.Invert()),
                        entry(Items.AMETHYST_SHARD, 0.02D, new ChanceModifier.Add(0.03D)),
                        entry(Items.LAPIS_LAZULI, new ChanceModifier.SetChance(0.08D)))));
    }

    // ---- 地牢：三层作用域的主战场 ----

    private List<LootInjector> dungeonInjectors(HolderLookup.Provider registries) {
        List<LootInjector> list = new ArrayList<>();
        TableSelector dungeon = new TableSelector.ById(BuiltInLootTables.SIMPLE_DUNGEON.location());

        // 表级 add_pool 塞具名池；池级 add_entry 按池下标选中地牢原有的第 0 个池
        list.add(raw("test_dungeon_add", dungeon, 900,
                new TableOperator.AddPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .setBonusRolls(ConstantValue.exactly(1.0F))
                        .name(TEST_POOL_NAME)
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT))
                        .build()),
                modifyPool(new PoolSelector.ByIndex(0), new PoolOperator.AddEntry(
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
                namedPool(POOL_SET_FUNC_NAME, LootItem.lootTableItem(Items.QUARTZ)),
                // 函数作用域用：第一个函数把数量设成 2，第二个是 +3 但被恒假条件挡住，所以初始是 2 个红石
                namedPool("arcanumlib_test_fn", LootItem.lootTableItem(Items.REDSTONE)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)))
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(3.0F), true)
                                .when(LootItemRandomChanceCondition.randomChance(0.0F)))),
                // 条目级条件双门：恒假 + 恒真，初始永不掉落
                namedPool("arcanumlib_test_cond", LootItem.lootTableItem(Items.CHARCOAL)
                        .when(LootItemRandomChanceCondition.randomChance(0.0F))
                        .when(LootItemRandomChanceCondition.randomChance(1.0F))),
                // 池级条件：池自身恒假，初始永不掉落
                new TableOperator.AddPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .name("arcanumlib_test_poolcond")
                        .when(LootItemRandomChanceCondition.randomChance(0.0F))
                        .add(LootItem.lootTableItem(Items.PRISMARINE_SHARD))
                        .build()),
                // match 选择器用：两个同类型但参数不同的 set_count（2 与 +3），只有按参数才能区分；初始 5 个
                namedPool("arcanumlib_test_match", LootItem.lootTableItem(Items.GLOWSTONE_DUST)
                        .when(LootItemRandomChanceCondition.randomChance(1.0F))
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)))
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(3.0F), true))),
                // 条件 match 用：恒真条件，初始必掉 1 个
                namedPool("arcanumlib_test_matchcond", LootItem.lootTableItem(Items.PRISMARINE_CRYSTALS)
                        .when(LootItemRandomChanceCondition.randomChance(1.0F)))));

        // 条目级 replace_entry：地牢箱里的线就地换成皮革。新条目特意挂 enchanted_count_increase（引用附魔注册表），
        // 运行时必须走 RegistryOps 才编码得出来——用 JsonOps 会直接报 "Can't access registry"
        list.add(raw("test_dungeon_replace", dungeon, 1050,
                modifyPool(new PoolSelector.Any(), modifyEntry(new EntrySelector.IsItem(mc("string")),
                        new EntryOperator.ReplaceEntry(LootItem.lootTableItem(Items.LEATHER)
                                .apply(lootingCount(registries))
                                .build())))));

        // poolSelector 组合（name + total_weight）：合成池 arcanumlib_test_alt 是 alternatives，
        // 自身权重 0、两个叶子各 1，所以总权重为 2 才命中——不递归算的话这里是 0、不会命中
        list.add(raw("test_dungeon_weight", dungeon, 1060,
                modifyPool(new PoolSelector.AllOf(List.of(
                        new PoolSelector.ByName("arcanumlib_test_alt"),
                        new PoolSelector.TotalWeightInRange(2, 2))),
                        modifyEntry(new EntrySelector.Any(), new EntryOperator.RemoveEntry()))));

        // 条目级 set_function：只命中占比 0.75 那条（金锭）并给它套数量 5；铁锭占 0.25 不在区间内，保持 1 个
        list.add(raw("test_dungeon_share", dungeon, 1070,
                modifyPool(new PoolSelector.ByName("arcanumlib_test_share"),
                        modifyEntry(new EntrySelector.WeightShareInRange(0.7F, 0.8F),
                                new EntryOperator.SetFunction(List.of(
                                        SetItemCountFunction.setCount(ConstantValue.exactly(5.0F)).build()))))));

        // 池级函数三连：加 +2、摘掉、再加 +1，最终 +1（2 个紫水晶）。
        // 用 add=true 的 set_count，所以 remove_function 一旦失效就会变成 +3（4 个），肉眼可辨
        list.add(raw("test_pool_functions", dungeon, 1080,
                modifyPool(new PoolSelector.ByName(POOL_FUNC_NAME),
                        new PoolOperator.AddFunction(List.of(addCount(2))),
                        new PoolOperator.RemoveFunction(List.of(mc("set_count"))),
                        new PoolOperator.AddFunction(List.of(addCount(1))))));

        // 池级 set_function：整个池的函数列表被换成 +3，石英从 1 个变 4 个
        list.add(raw("test_pool_set_function", dungeon, 1090,
                modifyPool(new PoolSelector.ByName(POOL_SET_FUNC_NAME),
                        new PoolOperator.SetFunction(List.of(addCount(3))))));

        // 条目级：remove_entry 用 ByIndex 与 Any 两种池条件各自命中一次；
        // add_condition / add_function / set_weight / set_quality 都落在具名池的那条金锭上
        list.add(raw("test_dungeon_tune", dungeon, 1100,
                modifyPool(new PoolSelector.Any(), modifyEntry(new EntrySelector.IsItem(mc("gunpowder")),
                        new EntryOperator.RemoveEntry())),
                modifyPool(new PoolSelector.ByIndex(0), modifyEntry(new EntrySelector.IsItem(mc("saddle")),
                        new EntryOperator.RemoveEntry())),
                modifyPool(new PoolSelector.ByName(TEST_POOL_NAME), modifyEntry(new EntrySelector.Any(),
                        new EntryOperator.AddCondition(List.of(
                                LootItemRandomChanceCondition.randomChance(0.5F).build())),
                        new EntryOperator.AddFunction(List.of(
                                SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)).build())),
                        new EntryOperator.SetWeight(20),
                        new EntryOperator.SetQuality(1)))));

        // 条目级 set_condition / set_function：把 1100 加上的 50% 概率门换成必然通过、函数换成空列表，
        // 于是金锭变成「必掉 1 个」。若这两个 op 是追加语义，结果会是 allOf(0.5, 1.0) 仍然 50%、数量还是 2
        list.add(raw("test_dungeon_set_parts", dungeon, 1150,
                modifyPool(new PoolSelector.ByName(TEST_POOL_NAME), modifyEntry(new EntrySelector.Any(),
                        new EntryOperator.SetCondition(List.of(
                                LootItemRandomChanceCondition.randomChance(1.0F).build())),
                        new EntryOperator.SetFunction(List.of())))));

        // 条目级 remove_condition / remove_function：arcanumlib_test_remove 原本「random_chance 0.0 → 永不掉落
        // + set_count 3」，摘掉后应变成「必掉 1 个绿宝石」，两个 op 各自都有肉眼可见的翻转
        list.add(raw("test_dungeon_remove_parts", dungeon, 1160,
                modifyPool(new PoolSelector.ByName("arcanumlib_test_remove"), modifyEntry(new EntrySelector.Any(),
                        new EntryOperator.RemoveCondition(List.of(mc("random_chance"))),
                        new EntryOperator.RemoveFunction(List.of(mc("set_count")))))));

        // 函数与条件作用域：四种组合各得到不同结果，任何一步失效都能看出来
        //   arcanumlib_test_fn 初始 2 个红石（set_count(2)；另一个 +3 被恒假条件挡住）
        //   → 删掉下标 0 的 set_count → 只剩被挡住的 +3 → 1 个
        //   → 再钻进那个函数自身的条件、摘掉恒假门 → 1 + 3 = 4 个
        //   两个都生效 4 / 只删函数 1 / 只摘条件 5 / 都没生效 2
        list.add(raw("test_function_scope", dungeon, 1120,
                modifyPool(new PoolSelector.ByName("arcanumlib_test_fn"), modifyEntry(new EntrySelector.Any(),
                        new EntryOperator.ModifyFunction(new FunctionSelector.ByIndex(0),
                                List.of(new FunctionOperator.RemoveFunction())),
                        new EntryOperator.ModifyFunction(new FunctionSelector.HasCondition(mc("random_chance")),
                                List.of(new FunctionOperator.ModifyCondition(
                                        new ConditionSelector.ByType(mc("random_chance")),
                                        List.of(new ConditionOperator.RemoveCondition())))))),
                // 条目级条件：把恒假门换成恒真，木炭从"永不掉落"变成"必掉"
                modifyPool(new PoolSelector.ByName("arcanumlib_test_cond"), modifyEntry(new EntrySelector.Any(),
                        new EntryOperator.ModifyCondition(new ConditionSelector.ByIndex(0),
                                List.of(new ConditionOperator.ReplaceCondition(
                                        LootItemRandomChanceCondition.randomChance(1.0F).build()))))),
                // 池级条件：摘掉池自身的恒假门，海晶碎片从"永不掉落"变成"必掉"
                modifyPool(new PoolSelector.ByName("arcanumlib_test_poolcond"),
                        new PoolOperator.ModifyCondition(new ConditionSelector.Any(),
                                List.of(new ConditionOperator.RemoveCondition())))));
        // match 选择器（对齐 DataComponentIngredient 的模板匹配）：模板只写关心的 key
        //   arcanumlib_test_match 初始 5 个荧石粉（set_count(2) + set_count(+3)）
        //   → 非严格模板 count=3.0 只命中那个 +3，删掉 → 2 个
        //   → 严格模板 {count:2.0, add:false} 命中剩下的 set_count(2)，换成 set_count(7) → 7 个
        //   两个都生效 7 / 只删 +3 是 2 / 只换是 10 / 都没生效 5，四种结果互不重叠
        list.add(raw("test_match_selector", dungeon, 1130,
                modifyPool(new PoolSelector.ByName("arcanumlib_test_match"), modifyEntry(new EntrySelector.Any(),
                        new EntryOperator.ModifyFunction(new FunctionSelector.Match(
                                template("{\"function\":\"minecraft:set_count\",\"count\":3.0}"), false),
                                List.of(new FunctionOperator.RemoveFunction())),
                        new EntryOperator.ModifyFunction(new FunctionSelector.Match(
                                template("{\"function\":\"minecraft:set_count\",\"count\":2.0,\"add\":false}"), true),
                                List.of(new FunctionOperator.ReplaceFunction(
                                        SetItemCountFunction.setCount(ConstantValue.exactly(7.0F)).build()))))),
                // 条件 match：恒真条件命中并换成恒假，海晶砂粒从"必掉 1 个"变成"永不掉落"
                modifyPool(new PoolSelector.ByName("arcanumlib_test_matchcond"), modifyEntry(new EntrySelector.Any(),
                        new EntryOperator.ModifyCondition(new ConditionSelector.Match(
                                template("{\"condition\":\"minecraft:random_chance\",\"chance\":1.0}"), false),
                                List.of(new ConditionOperator.ReplaceCondition(
                                        LootItemRandomChanceCondition.randomChance(0.0F).build())))))));
        // 池级 remove_pool + 方块标签选择器：木头方块不再掉东西
        list.add(raw("test_logs_remove_pool", new TableSelector.BlockTagTable(BlockTags.LOGS), 1200,
                modifyPool(new PoolSelector.Any(), new PoolOperator.RemovePool())));

        // TableSelector.All：效果是空操作（龙蛋不在任何表里），只把选择器与 remove_entry 跑一遍
        list.add(raw("test_all_tables", new TableSelector.All(), 1500,
                modifyPool(new PoolSelector.Any(), modifyEntry(new EntrySelector.IsItem(mc("dragon_egg")),
                        new EntryOperator.RemoveEntry()))));

        return list;
    }

    /** 表级函数三连：+2、摘掉、+1，最终每件 +1（若 remove_function 失效则是 +3）。 */
    private List<LootInjector> tableFunctionInjectors() {
        return List.of(
                raw("test_table_functions",
                        new TableSelector.ById(BuiltInLootTables.END_CITY_TREASURE.location()), 1000,
                        new TableOperator.AddFunction(List.of(addCount(2))),
                        new TableOperator.RemoveFunction(List.of(mc("set_count"))),
                        new TableOperator.AddFunction(List.of(addCount(1)))),

                // 表级 set_function：整个表的函数列表被换成 +2
                raw("test_table_set_function",
                        new TableSelector.ById(BuiltInLootTables.WOODLAND_MANSION.location()), 1000,
                        new TableOperator.SetFunction(List.of(addCount(2)))),
                // 表级函数作用域：把上面装的 +2 换成 +5（顺带覆盖 all_of / not / function_type 三个选择器）
                raw("test_table_modify_function",
                        new TableSelector.ById(BuiltInLootTables.WOODLAND_MANSION.location()), 1100,
                        new TableOperator.ModifyFunction(
                                new FunctionSelector.AllOf(List.of(
                                        new FunctionSelector.ByType(mc("set_count")),
                                        new FunctionSelector.Not(new FunctionSelector.ByIndex(99)))),
                                List.of(new FunctionOperator.ReplaceFunction(
                                        SetItemCountFunction.setCount(ConstantValue.exactly(5.0F), true).build())))));
    }

    /** 表选择器剩余子类型：Namespace / PathPrefix / Not / AnyOf / AllOf / ByEntityType / ByBlockType。 */
    private List<LootInjector> tableSelectorInjectors() {
        return List.of(
                raw("test_chests_except_end_city", new TableSelector.AllOf(List.of(
                        new TableSelector.Namespace("minecraft"),
                        new TableSelector.PathPrefix("chests/"),
                        new TableSelector.Not(new TableSelector.ById(BuiltInLootTables.END_CITY_TREASURE.location())))),
                        1000, addPool(Items.EMERALD, 0.05D)),
                // 按类型直接匹配：僵尸自己的默认表 / 石头自己的表（对照按标签的两种）
                raw("test_entity_type", new TableSelector.ByEntityType(EntityType.ZOMBIE), 1000, addPool(Items.ROTTEN_FLESH, 0.1D)),
                raw("test_block_type", new TableSelector.ByBlockType(Blocks.STONE), 1000, addPool(Items.COBBLESTONE, 0.1D)),

                raw("test_two_village_chests", new TableSelector.AnyOf(List.of(
                        new TableSelector.ById(BuiltInLootTables.VILLAGE_TEMPLE.location()),
                        new TableSelector.ById(BuiltInLootTables.VILLAGE_TOOLSMITH.location()))),
                        1000, addPool(Items.GOLD_NUGGET, 0.25D)));
    }

    /**
     * 池/条目选择器探针。
     *
     * <p>空 ops 的作用域只求值不改动，所以能把各选择器跑一遍；最后一条才是真改动（地牢箱子里火药 +1）。
     */
    private LootInjector selectorProbe() {
        List<TableOperator> ops = new ArrayList<>();

        ops.add(probeEntry(new EntrySelector.Any()));
        ops.add(probeEntry(new EntrySelector.ByIndex(0)));
        ops.add(probeEntry(new EntrySelector.IsItem(mc("golden_apple"))));
        ops.add(probeEntry(new EntrySelector.IsTag(ItemTags.COALS)));
        // 与上一行成对：地牢表里没有 tag 条目，但有 minecraft:coal 这个物品条目，所以只有这条会命中
        ops.add(probeEntry(new EntrySelector.IsItemWithTag(ItemTags.COALS)));
        ops.add(probeEntry(new EntrySelector.IsType(mc("item"))));
        ops.add(probeEntry(new EntrySelector.IsType(mc("alternatives"))));
        ops.add(probeEntry(new EntrySelector.WeightInRange(10, Integer.MAX_VALUE)));
        ops.add(probeEntry(new EntrySelector.HasCondition(mc("random_chance"))));
        ops.add(probeEntry(new EntrySelector.HasFunction(mc("set_count"))));
        ops.add(probeEntry(new EntrySelector.HasChild(new EntrySelector.IsItem(mc("iron_ingot")))));
        ops.add(probeEntry(new EntrySelector.Not(new EntrySelector.IsItem(mc("golden_apple")))));
        ops.add(probeEntry(new EntrySelector.AnyOf(List.of(
                new EntrySelector.IsItem(mc("golden_apple")),
                new EntrySelector.IsItem(mc("gunpowder"))))));
        ops.add(probeEntry(new EntrySelector.AllOf(List.of(
                new EntrySelector.IsType(mc("item")),
                new EntrySelector.WeightInRange(1, 1)))));

        ops.add(probePool(new PoolSelector.Any()));
        ops.add(probePool(new PoolSelector.ByName("main")));
        ops.add(probePool(new PoolSelector.HasEntry(new EntrySelector.IsItem(mc("gunpowder")))));
        ops.add(probePool(new PoolSelector.Not(new PoolSelector.ByIndex(99))));
        ops.add(probePool(new PoolSelector.AnyOf(List.of(
                new PoolSelector.ByIndex(0), new PoolSelector.ByIndex(1)))));
        ops.add(probePool(new PoolSelector.AllOf(List.of(
                new PoolSelector.Any(), new PoolSelector.Not(new PoolSelector.ByIndex(99))))));

        // 函数/条件选择器探针：空 ops 只求值、不改动
        ops.add(modifyPool(new PoolSelector.Any(), modifyEntry(new EntrySelector.Any(),
                new EntryOperator.ModifyFunction(new FunctionSelector.Any(), List.of()),
                new EntryOperator.ModifyFunction(new FunctionSelector.ByIndex(0), List.of()),
                new EntryOperator.ModifyFunction(new FunctionSelector.ByType(mc("set_count")), List.of()),
                new EntryOperator.ModifyFunction(new FunctionSelector.HasCondition(mc("random_chance")), List.of()),
                new EntryOperator.ModifyFunction(new FunctionSelector.Not(new FunctionSelector.ByIndex(99)), List.of()),
                new EntryOperator.ModifyFunction(new FunctionSelector.AnyOf(List.of(
                        new FunctionSelector.ByIndex(0), new FunctionSelector.ByType(mc("set_count")))), List.of()),
                new EntryOperator.ModifyFunction(new FunctionSelector.AllOf(List.of(
                        new FunctionSelector.Any(), new FunctionSelector.Not(new FunctionSelector.ByIndex(99)))),
                        List.of()),
                new EntryOperator.ModifyFunction(new FunctionSelector.Custom("test_function"), List.of()),
                new EntryOperator.ModifyCondition(new ConditionSelector.Any(), List.of()),
                new EntryOperator.ModifyCondition(new ConditionSelector.ByIndex(0), List.of()),
                new EntryOperator.ModifyCondition(new ConditionSelector.ByType(mc("random_chance")), List.of()),
                new EntryOperator.ModifyCondition(new ConditionSelector.Not(new ConditionSelector.ByIndex(99)), List.of()),
                new EntryOperator.ModifyCondition(new ConditionSelector.AnyOf(List.of(
                        new ConditionSelector.ByIndex(0), new ConditionSelector.ByType(mc("random_chance")))),
                        List.of()),
                new EntryOperator.ModifyCondition(new ConditionSelector.AllOf(List.of(
                        new ConditionSelector.Any(), new ConditionSelector.Not(new ConditionSelector.ByIndex(99)))),
                        List.of()),
                new EntryOperator.ModifyCondition(new ConditionSelector.Custom("test_condition"), List.of()))));
        ops.add(modifyPool(new PoolSelector.Any(), modifyEntry(new EntrySelector.IsItem(mc("gunpowder")),
                new EntryOperator.AddFunction(List.of(
                        SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)).build())))));

        return raw("test_selector_probes", new TableSelector.ById(BuiltInLootTables.SIMPLE_DUNGEON.location()),
                1000, ops.toArray(TableOperator[]::new));
    }

    /** 三类自定义 op 与三类自定义选择器；实际行为由 {@link TestCustomLootHandlers} 决定。 */
    private List<LootInjector> customInjectors() {
        return List.of(
                raw("test_custom", new TableSelector.Custom("test_table"), 1000,
                        new TableOperator.TableCustom("test_table_custom"),
                        modifyPool(new PoolSelector.Custom("test_pool"),
                                new PoolOperator.PoolCustom("test_pool_custom"),
                                modifyEntry(new EntrySelector.Custom("test_entry"),
                                        new EntryOperator.EntryCustom("test_entry_custom")))),

                raw("test_apply_event", new TableSelector.ById(BuiltInLootTables.ABANDONED_MINESHAFT.location()), 1300,
                        addPool(Items.COAL, 0.10D)));
    }

    // ---- 组装辅助 --------------------------------------------------------

    private LootInjector raw(String path, TableSelector tableSelector, int priority, TableOperator... ops) {
        return new LootInjector(id(path), tableSelector, priority, List.of(ops));
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

    private TableOperator modifyPool(PoolSelector poolSelector, PoolOperator... ops) {
        return new TableOperator.ModifyPool(poolSelector, List.of(ops));
    }

    private PoolOperator modifyEntry(EntrySelector entrySelector, EntryOperator... ops) {
        return new PoolOperator.ModifyEntry(entrySelector, List.of(ops));
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

    /** 构造 match 选择器的模板：直接写 json 文本，读起来和最终生成物一致。 */
    private static Dynamic<JsonElement> template(String json) {
        return new Dynamic<>(JsonOps.INSTANCE, JsonParser.parseString(json));
    }
    /** 空 ops 的条目作用域：只求值选择器，不改动表。 */
    private static TableOperator probeEntry(EntrySelector entrySelector) {
        return new TableOperator.ModifyPool(new PoolSelector.Any(),
                List.of(new PoolOperator.ModifyEntry(entrySelector, List.of())));
    }

    /** 空 ops 的池作用域：只求值池选择器。 */
    private static TableOperator probePool(PoolSelector poolSelector) {
        return new TableOperator.ModifyPool(poolSelector, List.of());
    }
}
