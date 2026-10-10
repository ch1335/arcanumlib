package com.chen1335.arcanumlib.test;

import com.chen1335.arcanumlib.ArcanumLib;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifier;
import com.chen1335.arcanumlib.modules.loot.lootCondition.ChanceModifierCondition;
import com.chen1335.arcanumlib.modules.loot.lootEntry.AllOfEntry;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjector;
import com.chen1335.arcanumlib.modules.loot.lootInjector.LootInjectorOp;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.EntryCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.PoolCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.condition.TableCondition;
import com.chen1335.arcanumlib.modules.loot.lootInjector.data.LootInjectorTemplate;
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
 * <p>覆盖：6 种 op、8 种 ChanceModifier、4 类选择器的全部子类型、AllOfEntry、模板骨架，
 * 以及 Custom 系列（配合 {@link TestCustomLootHandlers}）。
 *
 * <p>有些选择器在 vanilla 表里不会命中（原版条目大多是扁平的、池也没有名字），
 * 这类用空操作的 ModifyEntry 只做"编码 + 求值路径"覆盖，不改变掉落。
 */
public class TestLootInjectorProvider extends LootInjectorTemplate {

    private static final String TEST_POOL_NAME = "arcanumlib_test_pool";
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
        list.addAll(opInjectors(registries));
        list.addAll(tableConditionInjectors());
        list.add(selectorProbe());
        list.addAll(customInjectors());
        return List.copyOf(list);
    }

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

    /** 6 种注入操作 + 剩下 4 种加工器。 */
    private List<LootInjector> opInjectors(HolderLookup.Provider registries) {
        List<LootInjector> list = new ArrayList<>();

        // set_chance / multiply / invert / add
        list.add(raw("test_zombie_modifiers", new TableCondition.ById(ZOMBIE_TABLE), 1000,
                allOfPool(
                        entry(Items.DIAMOND, 0.02D, new ChanceModifier.Multiply(2.0D)),
                        entry(Items.NETHERITE_SCRAP, 0.95D, new ChanceModifier.Invert()),
                        entry(Items.AMETHYST_SHARD, 0.02D, new ChanceModifier.Add(0.03D)),
                        entry(Items.LAPIS_LAZULI, new ChanceModifier.SetChance(0.08D)))));

        // AddPool：塞一个具名池，供下一个注入器用 ByName 选中；AddEntry：按下标选中第 0 个池
        list.add(raw("test_dungeon_add", new TableCondition.ById(BuiltInLootTables.SIMPLE_DUNGEON.location()), 900,
                new LootInjectorOp.AddPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .setBonusRolls(ConstantValue.exactly(1.0F))
                        .name(TEST_POOL_NAME)
                        .add(LootItem.lootTableItem(Items.GOLD_INGOT))
                        .build()),
                new LootInjectorOp.AddEntry(new PoolCondition.ByIndex(0),
                        LootItem.lootTableItem(Items.IRON_INGOT).build())));

        // 自建 tag 条目与复合条目：原版表里全是扁平 item、也没有 tag 条目，
        // 不造这两个，IsTag / HasChild / IsType(alternatives) 在运行时永远命中不了
        list.add(raw("test_dungeon_entries", new TableCondition.ById(BuiltInLootTables.SIMPLE_DUNGEON.location()), 950,
                new LootInjectorOp.AddPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .name("arcanumlib_test_tag")
                        .add(TagEntry.tagContents(ItemTags.COALS)
                                .when(LootItemRandomChanceCondition.randomChance(1.0F)))
                        .build()),
                new LootInjectorOp.AddPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .name("arcanumlib_test_alt")
                        .add(AlternativesEntry.alternatives(
                                LootItem.lootTableItem(Items.IRON_INGOT),
                                LootItem.lootTableItem(Items.COPPER_INGOT)))
                        .build())));

        // ModifyEntry：按池名选中上面加的具名池；RemoveEntry：分别用 ByIndex 与 Any 命中
        list.add(raw("test_dungeon_tune", new TableCondition.ById(BuiltInLootTables.SIMPLE_DUNGEON.location()), 1100,
                new LootInjectorOp.ModifyEntry(new PoolCondition.ByName(TEST_POOL_NAME), new EntryCondition.Any(),
                        List.of(LootItemRandomChanceCondition.randomChance(0.5F).build()),
                        List.of(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)).build()),
                        20, 1),
                new LootInjectorOp.RemoveEntry(new PoolCondition.ByIndex(0),
                        new EntryCondition.IsItem(mc("saddle"))),
                new LootInjectorOp.RemoveEntry(new PoolCondition.Any(),
                        new EntryCondition.IsItem(mc("gunpowder")))));

        // ReplaceEntry：地牢箱里的线就地换成皮革。新条目特意挂 enchanted_count_increase（引用附魔注册表），
        // 这样运行时 copy() 必须走 RegistryOps 才能编码得出来——用 JsonOps 会直接报 "Can't access registry"
        list.add(raw("test_dungeon_replace", new TableCondition.ById(BuiltInLootTables.SIMPLE_DUNGEON.location()), 1050,
                new LootInjectorOp.ReplaceEntry(new PoolCondition.Any(), new EntryCondition.IsItem(mc("string")),
                        LootItem.lootTableItem(Items.LEATHER)
                                .apply(lootingCount(registries))
                                .build())));

        // RemovePool + 方块标签选择器：木头方块不再掉东西，便于肉眼确认
        list.add(raw("test_logs_remove_pool", new TableCondition.BlockTagTable(BlockTags.LOGS), 1200,
                new LootInjectorOp.RemovePool(new PoolCondition.Any())));

        // TableCondition.All + 两个 Any；效果是空操作（龙蛋不在任何表里）
        list.add(raw("test_all_tables", new TableCondition.All(), 1500,
                new LootInjectorOp.RemoveEntry(new PoolCondition.Any(),
                        new EntryCondition.IsItem(mc("dragon_egg")))));

        return list;
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
     * <p>空操作的 ModifyEntry（空条件与函数、权重品质 -1）只求值不改动，所以能把各选择器跑一遍；
     * 最后一条才是真改动（矿道箱子里火药 +1）。
     */
    private LootInjector selectorProbe() {
        List<LootInjectorOp> ops = new ArrayList<>();

        ops.add(probe(new EntryCondition.Any()));
        ops.add(probe(new EntryCondition.ByIndex(0)));
        ops.add(probe(new EntryCondition.IsItem(mc("golden_apple"))));
        ops.add(probe(new EntryCondition.IsTag(ItemTags.COALS)));
        // 与上一行成对：地牢表里没有 tag 条目，但有 minecraft:coal 这个物品条目，所以只有这条会命中
        ops.add(probe(new EntryCondition.IsItemWithTag(ItemTags.COALS)));
        ops.add(probe(new EntryCondition.IsType(mc("item"))));
        ops.add(probe(new EntryCondition.IsType(mc("alternatives"))));
        ops.add(probe(new EntryCondition.WeightAtLeast(10)));
        ops.add(probe(new EntryCondition.HasCondition(mc("random_chance"))));
        ops.add(probe(new EntryCondition.HasFunction(mc("set_count"))));
        ops.add(probe(new EntryCondition.HasChild(new EntryCondition.IsItem(mc("iron_ingot")))));
        ops.add(probe(new EntryCondition.Not(new EntryCondition.IsItem(mc("golden_apple")))));
        ops.add(probe(new EntryCondition.AnyOf(List.of(
                new EntryCondition.IsItem(mc("golden_apple")),
                new EntryCondition.IsItem(mc("gunpowder"))))));
        ops.add(probe(new EntryCondition.AllOf(List.of(
                new EntryCondition.IsType(mc("item")),
                new EntryCondition.WeightAtLeast(1)))));

        ops.add(probePool(new PoolCondition.Any()));
        ops.add(probePool(new PoolCondition.ByName("main")));
        ops.add(probePool(new PoolCondition.HasEntry(new EntryCondition.IsItem(mc("gunpowder")))));
        ops.add(probePool(new PoolCondition.Not(new PoolCondition.ByIndex(99))));
        ops.add(probePool(new PoolCondition.AnyOf(List.of(
                new PoolCondition.ByIndex(0), new PoolCondition.ByIndex(1)))));
        ops.add(probePool(new PoolCondition.AllOf(List.of(
                new PoolCondition.Any(), new PoolCondition.Not(new PoolCondition.ByIndex(99))))));

        ops.add(new LootInjectorOp.ModifyEntry(new PoolCondition.Any(), new EntryCondition.IsItem(mc("gunpowder")),
                List.of(), List.of(SetItemCountFunction.setCount(ConstantValue.exactly(2.0F)).build()), -1, -1));

        return raw("test_selector_probes", new TableCondition.ById(BuiltInLootTables.SIMPLE_DUNGEON.location()),
                1000, ops.toArray(LootInjectorOp[]::new));
    }

    /** 自定义 op 与三类自定义选择器；实际行为由 {@link TestCustomLootHandlers} 决定。 */
    private List<LootInjector> customInjectors() {
        return List.of(
                raw("test_custom", new TableCondition.Custom("test_table"), 1000,
                        new LootInjectorOp.Custom("test_custom_op"),
                        new LootInjectorOp.AddEntry(new PoolCondition.Custom("test_pool"),
                                LootItem.lootTableItem(Items.DIAMOND).build()),
                        new LootInjectorOp.ModifyEntry(new PoolCondition.Any(),
                                new EntryCondition.Custom("test_entry"),
                                List.of(), List.of(), 10, 1)),

                raw("test_apply_event", new TableCondition.ById(BuiltInLootTables.ABANDONED_MINESHAFT.location()), 1300,
                        addPool(Items.COAL, 0.10D)));
    }

    // ---- 组装辅助 --------------------------------------------------------

    private LootInjector raw(String path, TableCondition tableCondition, int priority, LootInjectorOp... ops) {
        return new LootInjector(id(path), tableCondition, priority, List.of(ops));
    }

    private LootInjectorOp.AddPool addPool(ItemLike item, double chance) {
        return allOfPool(entry(item, chance));
    }

    private LootInjectorOp.AddPool allOfPool(LootPoolEntryContainer.Builder<?>... children) {
        return new LootInjectorOp.AddPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(AllOfEntry.allOf(List.of(children)))
                .build());
    }

    private LootPoolEntryContainer.Builder<?> entry(ItemLike item, double baseChance, ChanceModifier... modifiers) {
        return LootItem.lootTableItem(item).when(ChanceModifierCondition.builder(baseChance, modifiers));
    }

    private LootPoolEntryContainer.Builder<?> entry(ItemLike item, ChanceModifier... modifiers) {
        return entry(item, 0.5D, modifiers);
    }

    /** 空操作探针：只求值选择器，不改动表。 */
    private static LootInjectorOp probe(EntryCondition entryCondition) {
        return new LootInjectorOp.ModifyEntry(new PoolCondition.Any(), entryCondition, List.of(), List.of(), -1, -1);
    }

    private static LootInjectorOp probePool(PoolCondition poolCondition) {
        return new LootInjectorOp.ModifyEntry(poolCondition, new EntryCondition.Any(), List.of(), List.of(), -1, -1);
    }
}
