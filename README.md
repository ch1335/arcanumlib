# ArcanumLib

**A loot and attribute framework for mods.**

`Minecraft 1.21.1` · `NeoForge 21.1.219` · `LGPL-3.0` · `0.1.0`

**English** | [简体中文](README.zh-cn.md)

---

## What is this

ArcanumLib splits "changing drops" into **three composable layers**, configurable straight from a datapack or from code:

1. **Datapack layer** — write a json **injector** that **selects** at five levels (table / pool / entry / function / condition) and then **adds, removes or edits** their structure;
2. **Code layer** — register your own **selectors / operators / modifiers**, or take over a layer at runtime with **events**;
3. **Attribute layer** — four new attributes that make drops follow the player's stats.

---

## Modules

| Module | Location | What it does |
|---|---|---|
| **Loot injector** | `src/main/java/.../modules/loot/lootInjector/` | The core. Reads `data/<ns>/loot_injecter/*.json` and applies it to loot tables on server start / `/reload`. Ships 5 selector families (table 13 / pool 9 / entry 15 / function 9 / condition 8), 41 operators (table 7 / pool 12 / entry 17 / function 3 / condition 2), 9 events and datagen support |
| **Chance modifier condition** | `modules/loot/lootCondition/` | Registers `arcanumlib:chance_modifiers`: nine modifiers (`apply` / `set_chance` / `clamp` / `invert` / `difficulty_factor` / `tool_enchantment_level` / `entity_attribute` / `predicate` / `stage`) that rewrite a base chance from difficulty, tool enchantment, entity attributes and more |
| **All-of entry** | `modules/loot/lootEntry/` | Registers `arcanumlib:all_of`: the opposite of vanilla `alternatives` — **every child is produced** |
| **Bonus loot** | `modules/loot/bonusLoot/` | Rolls a qualifying table extra times by the `arcanumlib:bonus_loot` attribute (entity drops and chests by default). Two events: one decides whether a table qualifies, one can change or cancel the ratio |
| **Attributes** | `modules/attribute/` | Registers four attributes `seeking` / `fortune` / `looting` / `bonus_loot` and wires them into spawn and loot events |
| **Commands** | `modules/command/` | `/arcanumlib export lootTable` (exports the modified and the original tables as json) and `/arcanumlib loot_simulation <table> [times] [param set]` (fills the context from a param set, simulates repeatedly and sums up the output) |
| **Advanced Loot Info compat** | `compat/ali/` | Shows this mod's modifiers and final chances inside ALI's in-game browser (`AliLootPlugin` / `AliChanceCalculator`). Not loaded when ALI is absent |
| **AStages compat** | `compat/astages/` | Stage-gated drops: the `has_stage` condition, the `stage` modifier and `StagePattern`. Not loaded when AStages is absent |
| **Events** | `events/` | `EnchantedLootEvent`: lets outside code rewrite enchantment levels |
| **Mixins** | `mixins/` | Patches what vanilla cannot do, e.g. enchantment / fortune counting, and `CompositeEntryBaseMixin` (recomputes the pre-composed container when a composite entry's children change) |
| **KubeJS integration** | `kubejs/` | Registers the script event group `ArcanumLibEvents` (12 events: 10 for injectors, 2 for bonus loot) and the bindings `ArcanumLib` plus 14 factory Helpers under `kubejs/modules/` |
| **Data generation** | `test/` | A datagen example: 33 test injectors covering every selector and operator, which double as the json examples in the docs |

---

## Examples of changing loot

The same job - changing drops - is shown below through a few examples: some **append** new entries, others **modify** existing ones. Each example is shown in three equivalent forms: datapack json, a KubeJS script, and Java code.

### Example 1: appending an entry to a pool

**Effect**: append one new entry to the **first pool** of `minecraft:chests/simple_dungeon` - a **25% chance to drop 1~3 diamonds** (the entry carries a `minecraft:random_chance` condition and a `minecraft:set_count` function).

#### Datapack

> The `//` lines below are only there to explain things: **json has no comments, so delete them before pasting into a datapack**.

```json
{
  // 优先级：同一张表上按它依次应用（数字越小越早）
  "priority": 1200,

  // 表选择器：arcanumlib:id 表示按表 id 精确命中
  "tableSelector": {
    "type": "arcanumlib:id",
    "id": "minecraft:chests/simple_dungeon"
  },

  "ops": [
    {
      // 表级操作：对命中的池执行它自己的 ops（这里只挑第 1 个池）
      "type": "arcanumlib:modify_pool",
      "poolSelector": {
        "type": "arcanumlib:index",
        "index": 0
      },
      "ops": [
        {
          // 池级操作：往这个池里追加一条条目
          "type": "arcanumlib:add_entry",
          "entry": {
            // 要追加的条目本体：一颗钻石
            "type": "minecraft:item",
            "name": "minecraft:diamond",

            // 原版条件：25% 概率才产出
            "conditions": [
              { "condition": "minecraft:random_chance", "chance": 0.25 }
            ],

            // 原版函数：数量随机 1~3
            "functions": [
              { "function": "minecraft:set_count",
                "count": { "type": "minecraft:uniform", "min": 1, "max": 3 } }
            ]
          }
        }
      ]
    }
  ]
}
```

#### KubeJS

A script registers the same injector through an **event**; selectors and operators come from the Helper factories, and conditions / functions go on the entry with the builder's `.when(...)` / `.apply(...)`:

```js
ArcanumLibEvents.registerLootInjector(event => {
  // 先造要追加的条目：钻石
  let diamond = LootEntryHelper.item('minecraft:diamond',
    builder => builder
      // 条件：25% 概率（when = 挂在条目上的条件）
      .when(
        LootConditionHelper.randomChance(
          NumberProviderHelper.constant(0.25)
        )
      )
      // 函数：数量随机 1~3（apply = 挂在条目上的函数）
      .apply(
        LootFunctionHelper.setCount(
          NumberProviderHelper.uniform(
            NumberProviderHelper.constant(1),
            NumberProviderHelper.constant(3)
          )
        )
      )
  )

  // 注册注入器：id / 选表 / 优先级 / 操作列表
  event.register('mypack:dungeon_diamond',
    // 选表：按 id 命中简单地牢的箱子表
    TableSelectorHelper.byId('minecraft:chests/simple_dungeon'),
    1200,
    [
      // 表级：只挑第 1 个池
      TableOperatorHelper.modifyPool(
        // 池选择器：按索引
        PoolSelectorHelper.byIndex(0),
        [
          // 池级：往这个池里追加条目
          PoolOperatorHelper.addEntry(diamond)
        ]
      )
    ]
  )
})
```

#### Java

The Java side listens to the same event, builds the entry with vanilla factories (conditions and functions go on the builder) and nests the operators exactly like the json:

```java
NeoForge.EVENT_BUS.addListener((LootInjectorRegisterEvent event) -> {
    // 注册注入器：id / 选表 / 优先级 / 操作列表
    event.register(ResourceLocation.fromNamespaceAndPath("mypack", "dungeon_diamond"),
            // 选表：按 id 命中简单地牢的箱子表
            new TableSelector.ById(ResourceLocation.withDefaultNamespace("chests/simple_dungeon")),
            1200,
            // 表级：只挑第 1 个池，往里面追加一条条目
            List.of(new TableOperator.ModifyPool(
                    // 池选择器：按索引
                    new PoolSelector.ByIndex(0),
                    List.of(new PoolOperator.AddEntry(
                            // 要追加的条目：钻石 + 25% 条件 + 数量 1~3
                            LootItem.lootTableItem(Items.DIAMOND)
                                    .when(LootItemRandomChanceCondition.randomChance(0.25F))
                                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                    .build())))));
});
```

### Example 2: modifying an existing entry (nothing new added)

**Effect**: take the **existing gold ingot entry** in the **first pool** of the same table and change it - **raise its weight to 20**, add a **50%** condition and set the amount to **1~2** (vanilla has weight 1, no extra condition and amount 1).

#### Datapack

```json
{
  // 改造已有条目不需要抢很早的优先级
  "priority": 900,

  "tableSelector": {
    "type": "arcanumlib:id",
    "id": "minecraft:chests/simple_dungeon"
  },

  "ops": [
    {
      // 表级：只挑第 1 个池
      "type": "arcanumlib:modify_pool",
      "poolSelector": {
        "type": "arcanumlib:index",
        "index": 0
      },
      "ops": [
        {
          // 池级：只挑出「金锭」那一条（这一层是筛选目标，不新增东西）
          "type": "arcanumlib:modify_entry",
          "entrySelector": {
            "type": "arcanumlib:item",
            "item": "minecraft:gold_ingot"
          },
          "ops": [
            // 改动 1：把它的权重提到 20（原版是 1）
            { "type": "arcanumlib:set_weight", "weight": 20 },

            // 改动 2：再挂一条 50% 的条件
            { "type": "arcanumlib:add_condition",
              "conditions": [ { "condition": "minecraft:random_chance", "chance": 0.5 } ] },

            // 改动 3：挂一个把数量改成 1~2 的函数
            { "type": "arcanumlib:add_function",
              "functions": [ { "function": "minecraft:set_count",
                               "count": { "type": "minecraft:uniform", "min": 1, "max": 2 } } ] }
          ]
        }
      ]
    }
  ]
}
```

#### KubeJS

The same Helpers describe "changes" just as well: `setWeight` / `addCondition` / `addFunction` all return operator objects, so they simply go into the `ops` of `modifyEntry`:

```js
ArcanumLibEvents.registerLootInjector(event => {
  // 要施加到「金锭」这一条目上的三个改动
  let goldChanges = [
    // 权重提到 20
    EntryOperatorHelper.setWeight(20),
    // 再挂一条 50% 的条件
    EntryOperatorHelper.addCondition([
      LootConditionHelper.randomChance(
        NumberProviderHelper.constant(0.5)
      )
    ]),
    // 挂一个把数量改成 1~2 的函数
    EntryOperatorHelper.addFunction([
      LootFunctionHelper.setCount(
        NumberProviderHelper.uniform(
          NumberProviderHelper.constant(1),
          NumberProviderHelper.constant(2)
        )
      )
    ])
  ]

  // 注册注入器：只挑第 1 个池里的金锭条目，把这几个改动套上去
  event.register('mypack:dungeon_gold',
    TableSelectorHelper.byId('minecraft:chests/simple_dungeon'),
    900,
    [
      TableOperatorHelper.modifyPool(
        PoolSelectorHelper.byIndex(0),
        [
          PoolOperatorHelper.modifyEntry(
            EntrySelectorHelper.isItem('minecraft:gold_ingot'),
            goldChanges
          )
        ]
      )
    ]
  )
})
```

#### Java

The Java form matches, except that `AddCondition` / `AddFunction` want **already built** conditions and functions:

```java
NeoForge.EVENT_BUS.addListener((LootInjectorRegisterEvent event) -> {
    event.register(ResourceLocation.fromNamespaceAndPath("mypack", "dungeon_gold"),
            new TableSelector.ById(ResourceLocation.withDefaultNamespace("chests/simple_dungeon")),
            900,
            List.of(new TableOperator.ModifyPool(
                    new PoolSelector.ByIndex(0),
                    List.of(new PoolOperator.ModifyEntry(
                            // 目标：金锭那一条（这里只是筛选，不新增）
                            new EntrySelector.IsItem(ResourceLocation.withDefaultNamespace("gold_ingot")),
                            List.of(
                                    // 权重提到 20
                                    new EntryOperator.SetWeight(20),
                                    // 再挂一条 50% 的条件（条件要先 build 出来）
                                    new EntryOperator.AddCondition(List.of(
                                            LootItemRandomChanceCondition.randomChance(0.5F).build())),
                                    // 挂一个把数量改成 1~2 的函数
                                    new EntryOperator.AddFunction(List.of(
                                            SetItemCountFunction.setCount(
                                                    UniformGenerator.between(1.0F, 2.0F)).build()))))))));
});
```

- Neither example touches anything outside the table structure, and both can be written in all three forms;
- There are 5 selector families and 41 operators; Helpers map one to one onto the records (see the factory assembly in 20.9 of the [injector docs](docs/en_us/Loot-Injector.md)).
---

## Commands

| Command | Effect |
|---|---|
| `/arcanumlib export lootTable` | Exports to `run/arcanumlib/exportedLootTable/`: `modified/` is the **in-memory** table (already touched by injectors), `original/` is a **byte-for-byte copy** of the file |
| `/arcanumlib loot_simulation <lootTable> [times] [param set]` | Rolls the same table over and over and reports item / count / share / chance; the param set decides how the context is filled (block / entity / generic) |

Both need **permission level 2**. See the [command docs](docs/en_us/Commands.md).

---


## Documentation

`docs/` holds the full documentation:

| Document | Content |
|---|---|
| [Loot Injector](docs/en_us/Loot-Injector.md) | Full injector reference: 5 selector families, 41 operators, 12 events, recipes for common tasks |
| [Loot Conditions](docs/en_us/Loot-Conditions.md) | `arcanumlib:chance_modifiers` and its nine modifiers |
| [Attributes](docs/en_us/Attributes.md) | The four attributes and how to use them |
| [Commands](docs/en_us/Commands.md) | Both commands, their arguments and output |
| [中文文档](docs/zh_cn/战利品表注入器.md) | The same documentation in Chinese |

---

## License

GNU **LGPLv3** (GNU Lesser General Public License v3.0) © chen_1335

- Full text in [LICENSE](LICENSE) (LGPLv3) and [COPYING](COPYING) (the GPLv3 it references);
- When another mod depends on this as a library, LGPLv3 terms apply.
