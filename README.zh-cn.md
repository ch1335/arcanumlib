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
| **全部产出条目** | `modules/loot/lootEntry/` | 注册 `arcanumlib:all_of`（与原生 `alternatives` 相反，**每个子项都会产出**）与 `arcanumlib:summon_mob`（**不产物品，改在 ORIGIN 处生成实体**） |
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

## 改掉落的例子

追加条目、改造条目各一个，每个都给**数据包 / KubeJS / Java** 三种等价写法 —— 见[战利品表注入器](docs/zh_cn/战利品表注入器.md)。

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
