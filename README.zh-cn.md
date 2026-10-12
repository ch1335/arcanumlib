# ArcanumLib

**面向模组作者的战利品与属性框架。**

`Minecraft 1.21.1` · `NeoForge 21.1.219` · `LGPL-3.0` · `0.1.0`

[English](README.md) | **简体中文**

---

## 这是什么

ArcanumLib 把"改掉落"这件事拆成**三层可组合的能力**，可直接通过数据包或者代码配置：

1. **数据包层** —— 写一条 json「**注入器**」，按 表 / 池 / 条目 / 函数 / 条件 五级**筛选**，再**增删改**它们的结构；
2. **代码层** —— 注册自己的**选择器 / 操作 / 加工器**，或者用**事件**在运行时接管某一层；
3. **属性层** —— 四个新属性，让掉落随玩家属性变化。

---

## 模块一览

| 模块 | 位置 | 做什么 |
|---|---|---|
| **战利品注入器** | `src/main/java/.../modules/loot/lootInjector/` | 核心。读 `data/<ns>/loot_injecter/*.json`，在服务端启动 / `/reload` 后应用到战利品表上。提供 5 族选择器（表 13 / 池 9 / 条目 15 / 函数 9 / 条件 8）、41 个操作（表 7 / 池 12 / 条目 17 / 函数 3 / 条件 2）、9 个事件，以及 datagen 支持 |
| **概率加工条件** | `modules/loot/lootCondition/` | 注册 `arcanumlib:chance_modifiers` 条件：用 9 个加工器（`apply` / `set_chance` / `clamp` / `invert` / `difficulty_factor` / `tool_enchantment_level` / `entity_attribute` / `predicate` / `stage`）把基础概率按难度、工具附魔、实体属性等改写 |
| **全部产出条目** | `modules/loot/lootEntry/` | 注册 `arcanumlib:all_of` 条目：与原生 `alternatives` 相反，**每个子项都会产出** |
| **额外战利品** | `modules/loot/bonusLoot/` | 按 `arcanumlib:bonus_loot` 属性对符合条件的表**额外摇取**若干份（默认只对实体掉落与箱子）。配两个事件：一个决定某张表要不要吃增幅，一个可以改倍率或取消 |
| **属性** | `modules/attribute/` | 注册 4 个属性 `seeking` / `fortune` / `looting` / `bonus_loot`，并接上生成与掉落相关事件，让掉落跟着属性走 |
| **指令** | `modules/command/` | `/arcanumlib export lootTable`（导出 modified / original 两套战利品表 json）与 `/arcanumlib loot_simulation <表> [次数] [参数集]`（按参数集填充上下文反复模拟并统计产出） |
| **Advanced Loot Info 兼容** | `compat/ali/` | 在 ALI 的游戏内浏览界面里显示本模组的加工器含义与最终概率（`AliLootPlugin` / `AliChanceCalculator`）。ALI 缺失时自动不加载 |
| **AStages 兼容** | `compat/astages/` | 阶段限制掉落：`has_stage` 条件、`stage` 加工器、`StagePattern` 匹配模式。AStages 缺失时自动不加载 |
| **事件** | `events/` | `EnchantedLootEvent`：让外部代码改写附魔等级 |
| **Mixin** | `mixins/` | 补原版做不到的地方，例如附魔 / 时运计数、以及 `CompositeEntryBaseMixin`（改复合条目的 children 时同步重算预先组合的容器） |
| **KubeJS 联动** | `kubejs/` | 注册脚本事件组 `ArcanumLibEvents`（12 个事件：注入器 10 个 + 额外战利品 2 个），以及绑定 `ArcanumLib` 与 `kubejs/modules/` 下的 14 个工厂 Helper |
| **数据生成** | `test/` | datagen 示例：33 条测试注入器，覆盖每种选择器与操作，同时充当文档里的 json 范例 |

---

## 一个修改战利品的例子

**作用**：往 `minecraft:blocks/oak_leaves` 的 `alternatives` 条目里插一个**最高优先级**的新分支 —— **25% 概率掉 1~3 个钻石**（分支上挂了 `minecraft:random_chance` 条件与 `minecraft:set_count` 函数）。同一个效果有三种等价写法：数据包 json、KubeJS 脚本、Java 代码。

### 数据包

`src/main/resources/data/<你的命名空间>/loot_injecter/*.json`：

> 下面那些 `//` 只是为了便于理解：**JSON 本身不支持注释，复制到数据包时请删掉它们**。


```json
{
  // 优先级：同一张表上按它依次应用（数字越小越早）
  "priority": 1200,

  // 表选择器：arcanumlib:id 表示按表 id 精确命中
  "tableSelector": {
    "type": "arcanumlib:id",
    "id": "minecraft:blocks/oak_leaves"
  },

  "ops": [
    {
      // 表级操作：对命中的每一个池，执行它自己的 ops
      "type": "arcanumlib:modify_pool",
      "ops": [
        {
          // 池级操作：只对「类型是 minecraft:alternatives 的条目」继续操作
          "type": "arcanumlib:modify_entry",
          "entrySelector": {
            "type": "arcanumlib:entry_type",
            "entryType": "minecraft:alternatives"
          },
          "ops": [
            {
              // 条目级操作：把下面的 entry 插到最高优先级（省略 index 就是追加到末尾）
              "type": "arcanumlib:add_child",
              "index": 0,
              "entry": {
                // 要插入的条目本体：一颗钻石
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
  ]
}
```

### KubeJS

脚本用**事件**注册同一条注入器；选择器与操作用 Helper 工厂造，条目上的条件与函数用 Builder 的 `.when(...)` / `.apply(...)` 挂：


```js
ArcanumLibEvents.registerLootInjector(event => {
  // 先造要插入的条目：钻石
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
  event.register('mypack:oak_leaves_diamond',
    // 选表：按 id 命中 minecraft:blocks/oak_leaves
    TableSelectorHelper.byId('minecraft:blocks/oak_leaves'),
    1200,
    [
      // 表级：对每一个池都做后面的操作
      TableOperatorHelper.modifyPool(
        // 池选择器：任意池
        PoolSelectorHelper.any(),
        [
          // 池级：只处理类型为 alternatives 的条目
          PoolOperatorHelper.modifyEntry(
            EntrySelectorHelper.isType('minecraft:alternatives'),
            [
              // 条目级：插到最高优先级（index 0）
              EntryOperatorHelper.addChild(diamond, 0)
            ]
          )
        ]
      )
    ]
  )
})
```

### Java

Java 侧监听同一个事件，条目用原版工厂造（条件 / 函数接在 Builder 上），操作与 json 一样逐层套：


```java
NeoForge.EVENT_BUS.addListener((LootInjectorRegisterEvent event) -> {
    // 注册注入器：id / 选表 / 优先级 / 操作列表
    event.register(ResourceLocation.fromNamespaceAndPath("mypack", "oak_leaves_diamond"),
            // 选表：按 id 命中 minecraft:blocks/oak_leaves
            new TableSelector.ById(ResourceLocation.withDefaultNamespace("blocks/oak_leaves")),
            1200,
            // 表级：对每一个池，只处理 alternatives 条目，在最高优先级插一条钻石条目
            List.of(new TableOperator.ModifyPool(
                    // 池选择器：任意池
                    new PoolSelector.Any(),
                    List.of(new PoolOperator.ModifyEntry(
                            // 目标条目：类型为 minecraft:alternatives
                            new EntrySelector.IsType(ResourceLocation.withDefaultNamespace("alternatives")),
                            List.of(new EntryOperator.AddChild(
                                    // 要插入的条目：钻石 + 25% 条件 + 数量 1~3
                                    LootItem.lootTableItem(Items.DIAMOND)
                                            .when(LootItemRandomChanceCondition.randomChance(0.25F))
                                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 3.0F)))
                                            .build(),
                                    0)))))));
});
```

- 三份写法**等价**：同一个 id（`mypack:oak_leaves_diamond`）、同一个 `priority`（1200）、同一套操作，条目上同样挂着 `random_chance` 与 `set_count`；
- 选择器与操作一共 5 族 / 41 个，Helper 与记录一一对应（见[注入器文档](docs/zh_cn/战利品表注入器.md) §20.9 的工厂拼法）。
---

## 指令

| 指令 | 作用 |
|---|---|
| `/arcanumlib export lootTable` | 导出到 `run/arcanumlib/exportedLootTable/`：`modified/` 是**内存里**（已被注入器改过）的表，`original/` 是**原始文件逐字节复制** |
| `/arcanumlib loot_simulation <lootTable> [次数] [参数集]` | 反复生成同一张表并统计「物品 / 数量 / 比例 / 概率」；参数集决定上下文怎么填（方块类 / 实体类 / 通用） |

两条都需要**权限等级 2**。详见 [指令文档](docs/zh_cn/指令.md)。

---


## 文档

仓库里的 `docs/` 是完整文档：

| 文档 | 内容 |
|---|---|
| [战利品表注入器](docs/zh_cn/战利品表注入器.md) | 注入器完整参考：5 族选择器、41 个操作、12 个事件、常见任务配方 |
| [战利品条件](docs/zh_cn/战利品条件.md) | `arcanumlib:chance_modifiers` 与 9 个概率加工器 |
| [属性](docs/zh_cn/属性.md) | 4 个属性与它们的取用方式 |
| [指令](docs/zh_cn/指令.md) | 两条指令的参数与输出格式 |
| [English docs](docs/en_us/Home.md) | 上述文档的英文版 |

---

## 许可

GNU **LGPLv3**（GNU Lesser General Public License v3.0）© chen_1335

- 完整正文见仓库根目录的 [LICENSE](LICENSE)（LGPLv3）与 [COPYING](COPYING)（LGPLv3 引用的 GPLv3）；
- 以库的形式被其它模组引用时，按 LGPLv3 的条款执行。
