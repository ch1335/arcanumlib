# 战利品表注入器 Wiki

用 json（或数据生成）就能改**任意战利品表**：加池、加条目、改权重、替换条目、增删改条件与函数——
不改原版数据、不和别的模组抢方块/实体的掉落覆盖权，只在自己负责的那部分叠加上去。

- 包路径：`com.chen1335.arcanumlib.modules.loot.lootInjector`
- 数据包目录：`data/<命名空间>/loot_injecter/<注入器 id>.json`
- 命名空间**不限于** `arcanumlib`：自己开一个 `data/<你的modid>/loot_injecter/` 就能写自己的注入器，用的是同一套类型。

> **json 语法限制（重要）**：加载走的是原版 `SimpleJsonResourceReloadListener`，它内部调
> `GsonHelper.fromJson(gson, reader, JsonElement.class, false)`——**严格模式**。
> 所以 **不允许 `//` 注释、不允许尾随逗号**，本 wiki 的示例全部是严格合法的 json。
> （写注释的需求请放到 `_comment` 之类的普通字段里，未知字段会被忽略。）

---

## 目录

1. [它是怎么跑起来的](#1-它是怎么跑起来的)
2. [嵌套作用域模型](#2-嵌套作用域模型)
3. [执行语义（必读）](#3-执行语义必读)
4. [顶层字段](#4-顶层字段)
5. [表选择器（12）](#5-表选择器12)
6. [表级操作（7）](#6-表级操作7)
7. [池选择器（9）](#7-池选择器9)
8. [池级操作（12）](#8-池级操作12)
9. [条目选择器（15）](#9-条目选择器15)
10. [条目级操作（13）](#10-条目级操作13)
11. [函数选择器（9）](#11-函数选择器9)
12. [函数级操作（3）](#12-函数级操作3)
13. [条件选择器（8）](#13-条件选择器8)
14. [条件级操作（2）](#14-条件级操作2)
15. [自定义钩子（9 个事件）](#15-自定义钩子9-个事件)
16. [给模组的扩展点](#16-给模组的扩展点)
17. [常见任务菜谱](#17-常见任务菜谱)
18. [用数据生成写注入器](#18-用数据生成写注入器)
19. [速查表](#19-速查表)
20. [语义边界与坑](#20-语义边界与坑)
21. [本模组提供的战利品条件](#21-本模组提供的战利品条件)

---

## 1. 它是怎么跑起来的

**应用时机**：服务端启动完成、以及每次数据包 reload（`/reload`）之后**各跑一次**，把全部注入器扫一遍并作用到战利品表上。

- **不是**每次掷骰子时跑，所以就算写几百个注入器也不会影响掉落结算的性能；
- 每个注入器对**同一张表**只会应用一次（内部按 注入器 id × 表 去重，`/reload` 会自动重来一遍）；
- 应用顺序由 `priority` 决定（越小越先），同 `priority` 的注入器解析结果来自一个**无序** Map，所以**同一张表上的两个注入器不要依赖 priority 相同的先后关系**——要确定性就给不同的 priority。

启动日志里会出现一行汇总，用来确认跑通（数字是示例）：

```
Loot injection: 26 injectors, 1183 loot tables, 4521 applications in 36.5538 ms
```

含义依次是：解析成功的注入器数、遍历到的战利品表数、"注入器 × 命中的表"的应用次数。这行出现就说明全部注入器都解析并执行成功了。

> **解析失败不会让游戏崩**：单个注入器解析失败只会记一条错误日志并跳过它，其余注入器照常工作。
> 排查时先看日志里有没有 `Failed to parse loot injector ...`。

---

## 2. 嵌套作用域模型

`ops` 是**树**，不是扁平列表。层级决定"这一项作用在什么上"：

```
顶层 ops                          → 作用于整张表（TableOperator）
├── add_pool                      （整表级叶子）
├── set_function / add_function / remove_function
├── table_custom
└── modify_pool  ← 作用域
    ├── poolSelector   选中哪些池
    └── ops                          → 作用于"每个命中的池"（PoolOperator）
        ├── remove_pool
        ├── add_entry
        ├── set_condition / add_condition / remove_condition
        ├── set_function / add_function / remove_function
        ├── pool_custom
        ├── modify_function ← 作用域
        │   ├── functionSelector    选中哪些函数
        │   └── ops                      → 作用于"每个命中的函数"（FunctionOperator）
        │       ├── remove_function
        │       ├── replace_function
        │       └── modify_condition ← 作用域
        │           ├── conditionSelector  选中函数自带的哪些条件
        │           └── ops                    → 作用于"每个命中的条件"（ConditionOperator）
        └── modify_condition ← 作用域
            ├── conditionSelector   选中池自身的哪些条件
            └── ops
        └── modify_entry ← 作用域
            ├── entrySelector       选中哪些条目
            └── ops                      → 作用于"每个命中的条目"（EntryOperator）
                ├── remove_entry / replace_entry
                ├── set_weight / set_quality
                ├── set_condition / add_condition / remove_condition
                ├── set_function / add_function / remove_function
                ├── entry_custom
                ├── modify_function ← 作用域（同上，目标是这个条目的函数列表）
                └── modify_condition ← 作用域（同上，目标是这个条目自身的条件）
```

**为什么嵌套而不是扁平**：`poolCondition` / `entryCondition` 这些选择器只写在**作用域**上，叶子 op 不再自带选择器——它天然只作用于"当前对象"。所以：

- 不可能把 `remove_pool` 写错层级（表级根本没注册这个类型，解析期直接报错）；
- 同一个 id（例如 `set_function`）在表/池/条目/函数/条件五级各注册一份，**靠嵌套层级消歧**；
- 顺序被结构编码：外层先于内层，同一层按数组顺序。

---

## 3. 执行语义（必读）

### 3.1 依次应用

同一个数组里的 op 按顺序执行，前一个的改动后一个能看见。

### 3.2 快照遍历 + 游标停止

作用域判定"哪些对象命中"时用的是**快照**（`List.copyOf`），所以子 op 删除当前对象不会让遍历跳项。规则是：

- **当前对象被前面的子 op 删掉了，剩下的子 op 立即停止**（不会对着已删除的对象继续改）；
- 删掉别的对象不影响当前这一轮。

### 3.3 条件的"归属"

| 想改的东西 | 写在哪一级 |
|---|---|
| 表自身的 `functions` | 表级（`LootTable` **没有** `conditions`，所以表级没有 `set_condition`） |
| 池自身的 `conditions` / `functions` | 池级 |
| 条目自身的 `conditions`（所有条目都有） | 条目级 |
| 条目自身的 `functions`（只有单例条目有；`alternatives`/`sequence`/`group` 没有） | 条目级 |
| **某个函数自带的** `conditions` | 函数级 `modify_condition` |

最后一行是这套结构里最有价值的一层：函数自己的条件（`LootItemConditionalFunction.predicates`）用其它任何写法都够不着。

### 3.4 复合体一定会被重算

原版掷骰/结算时用的不是 `conditions` / `functions` 列表本身，而是构造时缓存好的 `compositeCondition` / `compositeFunction` / `compositePredicates`。
所有会改列表的 op **都会自动重算对应的复合体**，你不用管；但如果你自己写自定义 op，**必须自己重算**，否则"看着改了、实际没生效"。

---

## 4. 顶层字段

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `ops` | 数组 | ✅ | — | 表级操作列表 |
| `priority` | int | | `1000` | 越小越先应用 |
| `tableSelector` | 表选择器 | | `{"type":"arcanumlib:all"}` | 这个注入器作用于哪些表 |

```json
{
  "priority": 1000,
  "tableSelector": { "type": "arcanumlib:id", "id": "minecraft:chests/simple_dungeon" },
  "ops": []
}
```

---

## 5. 表选择器（12）

决定"这个注入器要不要作用到某张表"。注册表：`arcanumlib:table_selector`。

| id | 字段 | 说明 |
|---|---|---|
| `all` | — | 所有表 |
| `id` | `id` | 表 id 精确匹配 |
| `namespace` | `namespace` | 命名空间匹配 |
| `path_prefix` | `prefix` | 路径前缀匹配 |
| `entity_tag` | `tag` | 实体标签；表 id 等于该标签下任一实体的默认战利品表 |
| `block_tag` | `tag` | 方块标签；表 id 等于该标签下任一方块的表 |
| `entity_type` | `entity` | 实体类型；表 id 等于它的默认战利品表（不展开标签） |
| `block_type` | `block` | 方块；表 id 等于它的战利品表（不展开标签；没有战利品表的方块永不命中） |
| `not` | `condition` | 取反 |
| `any_of` | `conditions` | 任一满足 |
| `all_of` | `conditions` | 全部满足 |
| `custom` | `custom_id` | 判断交给 `CustomTableSelectorEvent` 的监听者 |

```json
{ "type": "arcanumlib:all" }
```
```json
{ "type": "arcanumlib:id", "id": "minecraft:chests/simple_dungeon" }
```
```json
{ "type": "arcanumlib:namespace", "namespace": "minecraft" }
```
```json
{ "type": "arcanumlib:path_prefix", "prefix": "chests/" }
```
```json
{ "type": "arcanumlib:entity_tag", "tag": "minecraft:zombies" }
```
```json
{ "type": "arcanumlib:block_tag", "tag": "minecraft:logs" }
```

```json
{ "type": "arcanumlib:entity_type", "entity": "minecraft:zombie" }
```
```json
{ "type": "arcanumlib:block_type", "block": "minecraft:stone" }
```
```json
{ "type": "arcanumlib:not", "condition": { "type": "arcanumlib:id", "id": "minecraft:chests/end_city_treasure" } }
```
```json
{
  "type": "arcanumlib:any_of",
  "conditions": [
    { "type": "arcanumlib:id", "id": "minecraft:chests/village/village_temple" },
    { "type": "arcanumlib:id", "id": "minecraft:chests/village/village_toolsmith" }
  ]
}
```
```json
{
  "type": "arcanumlib:all_of",
  "conditions": [
    { "type": "arcanumlib:namespace", "namespace": "minecraft" },
    { "type": "arcanumlib:path_prefix", "prefix": "chests/" }
  ]
}
```
```json
{ "type": "arcanumlib:custom", "custom_id": "my_table_hook" }
```

---

## 6. 表级操作（7）

写在顶层 `ops` 里。注册表：`arcanumlib:table_operator`。

| id | 字段 | 说明 |
|---|---|---|
| `add_pool` | `pool` | 往表里**追加**一个池（原版 json 池格式） |
| `modify_pool` | `poolSelector`、`ops` | 作用域：对每个命中的池跑子 ops |
| `modify_function` | `functionSelector`、`ops` | 作用域：对表函数列表里命中的函数跑子 ops |
| `set_function` | `functions` | **替换**表自身的函数列表 |
| `add_function` | `functions` | **追加**到表自身的函数列表 |
| `remove_function` | `functionTypes` | 按函数类型 id 摘掉表自身的函数 |
| `table_custom` | `custom_id` | 交给 `TableCustomOpEvent` 的监听者 |

```json
{
  "type": "arcanumlib:add_pool",
  "pool": {
    "rolls": 1.0,
    "bonus_rolls": 0.0,
    "name": "my_extra_pool",
    "entries": [
      { "type": "minecraft:item", "name": "minecraft:diamond", "weight": 1, "functions": [] }
    ]
  }
}
```
```json
{
  "type": "arcanumlib:modify_pool",
  "poolSelector": { "type": "arcanumlib:name", "name": "my_extra_pool" },
  "ops": [
    { "type": "arcanumlib:remove_pool" },
    { "type": "arcanumlib:add_entry", "entry": { "type": "minecraft:item", "name": "minecraft:emerald" } }
  ]
}
```
```json
{
  "type": "arcanumlib:modify_function",
  "functionSelector": { "type": "arcanumlib:function_type", "functionType": "minecraft:set_count" },
  "ops": [
    { "type": "arcanumlib:replace_function", "function": { "function": "minecraft:set_count", "count": 3.0 } }
  ]
}
```
```json
{
  "type": "arcanumlib:set_function",
  "functions": [
    { "function": "minecraft:set_count", "count": 2.0, "add": true }
  ]
}
```
```json
{
  "type": "arcanumlib:add_function",
  "functions": [
    { "function": "minecraft:set_count", "count": 1.0, "add": true }
  ]
}
```
```json
{ "type": "arcanumlib:remove_function", "functionTypes": ["minecraft:set_count"] }
```
```json
{ "type": "arcanumlib:table_custom", "custom_id": "my_table_hook" }
```

---

## 7. 池选择器（9）

写在 `modify_pool.poolSelector` 里。注册表：`arcanumlib:pool_selector`。

| id | 字段 | 说明 |
|---|---|---|
| `any` | — | 所有池（**默认值**） |
| `index` | `index` | 池下标（从 0 开始） |
| `name` | `name` | 池名；**无名池永远不匹配** |
| `has_entry` | `entrySelector` | 池里**任意**条目满足该条目选择器 |
| `total_weight` | `min`、`max` | 池的总权重落在闭区间内 |
| `not` | `condition` | 取反 |
| `any_of` | `conditions` | 任一满足 |
| `all_of` | `conditions` | 全部满足 |
| `custom` | `custom_id` | 交给 `CustomPoolSelectorEvent` 的监听者 |

```json
{ "type": "arcanumlib:any" }
```
```json
{ "type": "arcanumlib:index", "index": 0 }
```
```json
{ "type": "arcanumlib:name", "name": "main" }
```
```json
{
  "type": "arcanumlib:has_entry",
  "entrySelector": { "type": "arcanumlib:item", "item": "minecraft:gunpowder" }
}
```
```json
{ "type": "arcanumlib:total_weight", "min": 2, "max": 20 }
```
```json
{ "type": "arcanumlib:not", "condition": { "type": "arcanumlib:index", "index": 99 } }
```
```json
{
  "type": "arcanumlib:any_of",
  "conditions": [
    { "type": "arcanumlib:index", "index": 0 },
    { "type": "arcanumlib:index", "index": 1 }
  ]
}
```
```json
{
  "type": "arcanumlib:all_of",
  "conditions": [
    { "type": "arcanumlib:any" },
    { "type": "arcanumlib:not", "condition": { "type": "arcanumlib:index", "index": 99 } }
  ]
}
```
```json
{ "type": "arcanumlib:custom", "custom_id": "my_pool_hook" }
```

**总权重的口径**（`arcanumlib:total_weight`，以及条目级的 `weight_in_range` / `weight_share`）：

- 只算**静态基础权重**：`weight` 字段，等价于原版 `getWeight(luck = 0)`（含 `max(..., 0)` 夹取）；
- **不含** `quality × 幸运`——选择器拿不到 `LootContext`；
- **不按条目条件过滤**——原版掷骰时是经 `expand(context)` 展开的；
- 复合条目（`alternatives`/`sequence`/`group`）自身没有 `weight`，**递归累加其子树**；其中 `alternatives` 原版只走第一个条件通过的分支，这里把所有分支都算上，属**静态上界**。

---

## 8. 池级操作（12）

写在 `modify_pool.ops` 里。注册表：`arcanumlib:pool_operator`。

| id | 字段 | 说明 |
|---|---|---|
| `remove_pool` | — | 删掉**当前池** |
| `add_entry` | `entry` | 往当前池追加一个条目 |
| `modify_entry` | `entrySelector`、`ops` | 作用域：对当前池里命中的条目跑子 ops |
| `modify_function` | `functionSelector`、`ops` | 作用域：对当前池的函数列表 |
| `modify_condition` | `conditionSelector`、`ops` | 作用域：对当前池自身的条件 |
| `set_condition` | `conditions` | 替换池自身的条件列表 |
| `add_condition` | `conditions` | 追加到池自身的条件列表 |
| `remove_condition` | `conditionTypes` | 按条件类型 id 摘掉池自身的条件 |
| `set_function` | `functions` | 替换池自身的函数列表 |
| `add_function` | `functions` | 追加到池自身的函数列表 |
| `remove_function` | `functionTypes` | 按函数类型 id 摘掉池自身的函数 |
| `pool_custom` | `custom_id` | 交给 `PoolCustomOpEvent` 的监听者 |

```json
{ "type": "arcanumlib:remove_pool" }
```
```json
{ "type": "arcanumlib:add_entry", "entry": { "type": "minecraft:item", "name": "minecraft:iron_ingot" } }
```
```json
{
  "type": "arcanumlib:modify_entry",
  "entrySelector": { "type": "arcanumlib:weight_share", "min": 0.7, "max": 0.8 },
  "ops": [
    { "type": "arcanumlib:set_function", "functions": [{ "function": "minecraft:set_count", "count": 5.0 }] }
  ]
}
```
```json
{
  "type": "arcanumlib:modify_function",
  "functionSelector": { "type": "arcanumlib:any" },
  "ops": [
    { "type": "arcanumlib:remove_function" }
  ]
}
```
```json
{
  "type": "arcanumlib:modify_condition",
  "conditionSelector": { "type": "arcanumlib:condition_type", "conditionType": "minecraft:random_chance" },
  "ops": [{ "type": "arcanumlib:remove_condition" }]
}
```
```json
{
  "type": "arcanumlib:set_condition",
  "conditions": [{ "condition": "minecraft:random_chance", "chance": 1.0 }]
}
```
```json
{
  "type": "arcanumlib:add_condition",
  "conditions": [{ "condition": "minecraft:random_chance", "chance": 0.5 }]
}
```
```json
{ "type": "arcanumlib:remove_condition", "conditionTypes": ["minecraft:random_chance"] }
```
```json
{
  "type": "arcanumlib:set_function",
  "functions": [{ "function": "minecraft:set_count", "count": 2.0, "add": true }]
}
```
```json
{
  "type": "arcanumlib:add_function",
  "functions": [{ "function": "minecraft:set_count", "count": 1.0, "add": true }]
}
```
```json
{ "type": "arcanumlib:remove_function", "functionTypes": ["minecraft:set_count"] }
```
```json
{ "type": "arcanumlib:pool_custom", "custom_id": "my_pool_hook" }
```

---

## 9. 条目选择器（15）

写在 `modify_entry.entrySelector` 里。注册表：`arcanumlib:entry_selector`。

| id | 字段 | 说明 |
|---|---|---|
| `any` | — | 所有条目（**默认值**） |
| `index` | `index` | 条目在池里的下标 |
| `item` | `item` | 物品 id 精确匹配（只对 `minecraft:item` 条目有意义） |
| `tag` | `tag` | 匹配 `minecraft:tag` **条目**：标签本身等于给定标签 |
| `item_with_tag` | `tag` | 匹配 `minecraft:item` 条目：它的**物品**属于给定标签（注意与 `tag` 的区别） |
| `entry_type` | `entryType` | 条目类型，如 `minecraft:item`、`minecraft:alternatives` |
| `weight_in_range` | `min`、`max` | 条目（复合条目按子树）权重落在闭区间内 |
| `weight_share` | `min`、`max` | 条目权重占所属池总权重的比例落在 `[0, 1]` 闭区间内 |
| `has_condition` | `conditionType` | 条目身上挂着某种条件 |
| `has_function` | `functionType` | 条目身上挂着某种函数 |
| `has_child` | `entrySelector` | 复合条目的**直接子项**里有满足的（不递归） |
| `not` | `condition` | 取反 |
| `any_of` | `conditions` | 任一满足 |
| `all_of` | `conditions` | 全部满足 |
| `custom` | `custom_id` | 交给 `CustomEntrySelectorEvent` 的监听者 |

```json
{ "type": "arcanumlib:any" }
```
```json
{ "type": "arcanumlib:index", "index": 0 }
```
```json
{ "type": "arcanumlib:item", "item": "minecraft:gunpowder" }
```
```json
{ "type": "arcanumlib:tag", "tag": "minecraft:coals" }
```
```json
{ "type": "arcanumlib:item_with_tag", "tag": "minecraft:coals" }
```
```json
{ "type": "arcanumlib:entry_type", "entryType": "minecraft:alternatives" }
```
```json
{ "type": "arcanumlib:weight_in_range", "min": 1, "max": 1 }
```
```json
{ "type": "arcanumlib:weight_share", "min": 0.05, "max": 0.2 }
```
```json
{ "type": "arcanumlib:has_condition", "conditionType": "minecraft:random_chance" }
```
```json
{ "type": "arcanumlib:has_function", "functionType": "minecraft:set_count" }
```
```json
{ "type": "arcanumlib:has_child", "entrySelector": { "type": "arcanumlib:item", "item": "minecraft:iron_ingot" } }
```
```json
{ "type": "arcanumlib:not", "condition": { "type": "arcanumlib:item", "item": "minecraft:golden_apple" } }
```
```json
{
  "type": "arcanumlib:any_of",
  "conditions": [
    { "type": "arcanumlib:item", "item": "minecraft:golden_apple" },
    { "type": "arcanumlib:item", "item": "minecraft:gunpowder" }
  ]
}
```
```json
{
  "type": "arcanumlib:all_of",
  "conditions": [
    { "type": "arcanumlib:entry_type", "entryType": "minecraft:item" },
    { "type": "arcanumlib:weight_in_range", "min": 1, "max": 1 }
  ]
}
```
```json
{ "type": "arcanumlib:custom", "custom_id": "my_entry_hook" }
```

---

## 10. 条目级操作（13）

写在 `modify_entry.ops` 里。注册表：`arcanumlib:entry_operator`。

| id | 字段 | 说明 |
|---|---|---|
| `remove_entry` | — | 删掉当前条目 |
| `replace_entry` | `entry` | 把当前条目换成新条目（每处一份独立实例） |
| `set_weight` | `weight` | 设权重；**负数视为不改**；复合条目没有权重，跳过 |
| `set_quality` | `quality` | 设品质；**负数视为不改**；复合条目没有品质，跳过 |
| `modify_function` | `functionSelector`、`ops` | 作用域：对当前条目的函数列表 |
| `modify_condition` | `conditionSelector`、`ops` | 作用域：对当前条目自身的条件 |
| `set_condition` | `conditions` | 替换当前条目的条件列表 |
| `add_condition` | `conditions` | 追加到当前条目的条件列表 |
| `remove_condition` | `conditionTypes` | 按类型 id 摘掉当前条目的条件 |
| `set_function` | `functions` | 替换当前条目的函数列表（复合条目跳过） |
| `add_function` | `functions` | 追加到当前条目的函数列表（复合条目跳过） |
| `remove_function` | `functionTypes` | 按类型 id 摘掉当前条目的函数（复合条目跳过） |
| `entry_custom` | `custom_id` | 交给 `EntryCustomOpEvent` 的监听者 |

```json
{ "type": "arcanumlib:remove_entry" }
```
```json
{
  "type": "arcanumlib:replace_entry",
  "entry": {
    "type": "minecraft:item",
    "name": "minecraft:leather",
    "functions": [
      { "function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting", "count": { "type": "minecraft:uniform", "min": 0.0, "max": 1.0 } }
    ]
  }
}
```
```json
{ "type": "arcanumlib:set_weight", "weight": 20 }
```
```json
{ "type": "arcanumlib:set_quality", "quality": 1 }
```
```json
{
  "type": "arcanumlib:modify_function",
  "functionSelector": { "type": "arcanumlib:match", "function": { "function": "minecraft:set_count", "count": 3.0 } },
  "ops": [{ "type": "arcanumlib:remove_function" }]
}
```
```json
{
  "type": "arcanumlib:modify_condition",
  "conditionSelector": { "type": "arcanumlib:index", "index": 0 },
  "ops": [
    { "type": "arcanumlib:replace_condition", "condition": { "condition": "minecraft:random_chance", "chance": 1.0 } }
  ]
}
```
```json
{
  "type": "arcanumlib:set_condition",
  "conditions": [{ "condition": "minecraft:random_chance", "chance": 1.0 }]
}
```
```json
{
  "type": "arcanumlib:add_condition",
  "conditions": [{ "condition": "minecraft:random_chance", "chance": 0.5 }]
}
```
```json
{ "type": "arcanumlib:remove_condition", "conditionTypes": ["minecraft:random_chance"] }
```
```json
{
  "type": "arcanumlib:set_function",
  "functions": [{ "function": "minecraft:set_count", "count": 2.0 }]
}
```
```json
{
  "type": "arcanumlib:add_function",
  "functions": [{ "function": "minecraft:set_count", "count": 1.0, "add": true }]
}
```
```json
{ "type": "arcanumlib:remove_function", "functionTypes": ["minecraft:set_count"] }
```
```json
{ "type": "arcanumlib:entry_custom", "custom_id": "my_entry_hook" }
```

---

## 11. 函数选择器（9）

写在 `modify_function.functionSelector` 里。注册表：`arcanumlib:function_selector`。

| id | 字段 | 说明 |
|---|---|---|
| `any` | — | 所有函数（**默认值**） |
| `index` | `index` | 函数在列表里的下标 |
| `function_type` | `functionType` | 按函数类型 id，如 `minecraft:set_count` |
| `has_condition` | `conditionType` | 该函数**自带**的条件里有这种类型 |
| `not` | `selector` | 取反 |
| `any_of` | `selectors` | 任一满足 |
| `all_of` | `selectors` | 全部满足 |
| `match` | `function`、`strict` | 按**模板 json 的 key** 匹配（见下） |
| `custom` | `custom_id` | 交给 `CustomFunctionSelectorEvent` 的监听者 |

```json
{ "type": "arcanumlib:any" }
```
```json
{ "type": "arcanumlib:index", "index": 0 }
```
```json
{ "type": "arcanumlib:function_type", "functionType": "minecraft:set_count" }
```
```json
{ "type": "arcanumlib:has_condition", "conditionType": "minecraft:random_chance" }
```
```json
{ "type": "arcanumlib:not", "selector": { "type": "arcanumlib:index", "index": 99 } }
```
```json
{
  "type": "arcanumlib:any_of",
  "selectors": [
    { "type": "arcanumlib:index", "index": 0 },
    { "type": "arcanumlib:function_type", "functionType": "minecraft:set_count" }
  ]
}
```
```json
{
  "type": "arcanumlib:all_of",
  "selectors": [
    { "type": "arcanumlib:function_type", "functionType": "minecraft:set_count" },
    { "type": "arcanumlib:not", "selector": { "type": "arcanumlib:index", "index": 99 } }
  ]
}
```
```json
{
  "type": "arcanumlib:match",
  "function": { "function": "minecraft:set_count", "count": 3.0 }
}
```
```json
{
  "type": "arcanumlib:match",
  "strict": true,
  "function": { "function": "minecraft:set_count", "count": 2.0, "add": false }
}
```
```json
{ "type": "arcanumlib:custom", "custom_id": "my_function_hook" }
```

### 关于 `match`

语义对齐原版的 `DataComponentIngredient` 组件匹配：

- 模板保留**原始 json**，所以**只比对写出来的 key**——没写的 key 不参与，codec 的默认值不会污染结果（这是它比"先反序列化成函数再比"更有用的关键）；
- `strict` 默认 `false`：模板里每个 key 都要在候选里存在且值一致（子集匹配）；
- `strict: true`：要求与候选**完全一致**（按序列化后的 json 相等比较）；
- 数组按**有序参数**看待：要求长度一致、逐元素匹配；
- 数字按值比较（`3` 与 `3.0` 视为相等）；
- 模板里的类型 key（函数是 `"function"`，条件是 `"condition"`）**必须存在**，否则永远不匹配。

典型用途：**按参数区分同类型的函数**。例如一个条目同时有 `set_count(2)` 和 `set_count(+3)`，`function_type` 会把两个一起选中，而 `match` 能只挑其中一个。

> 注意：`match` 的模板**不会被校验**。key 写错（例如 `coun`）不会报错，只是永远匹配不上。

---

## 12. 函数级操作（3）

写在 `modify_function.ops` 里。注册表：`arcanumlib:function_operator`。

| id | 字段 | 说明 |
|---|---|---|
| `remove_function` | — | 删掉当前函数 |
| `replace_function` | `function` | 把当前函数换成新函数（每处一份独立实例） |
| `modify_condition` | `conditionSelector`、`ops` | 作用域：钻进**当前函数自带的条件**；非条件型函数（如 `set_count`）没有条件，跳过 |

```json
{ "type": "arcanumlib:remove_function" }
```
```json
{
  "type": "arcanumlib:replace_function",
  "function": { "function": "minecraft:set_count", "count": 7.0, "add": true }
}
```
```json
{
  "type": "arcanumlib:modify_condition",
  "conditionSelector": { "type": "arcanumlib:condition_type", "conditionType": "minecraft:random_chance" },
  "ops": [{ "type": "arcanumlib:remove_condition" }]
}
```

> 通用的"改参数"（例如把 `count` 从 5 改成 10）**没有**通用实现：各函数的参数字段都是各自私有的。
> 需要时用 `remove_function` + `add_function` 组合，或者写一个 `function_custom` 级别的自定义 op（见第 16 节）。

---

## 13. 条件选择器（8）

写在 `modify_condition.conditionSelector` 里。注册表：`arcanumlib:condition_selector`。

| id | 字段 | 说明 |
|---|---|---|
| `any` | — | 所有条件（**默认值**） |
| `index` | `index` | 条件下的下标 |
| `condition_type` | `conditionType` | 按条件类型 id，如 `minecraft:random_chance` |
| `not` | `selector` | 取反 |
| `any_of` | `selectors` | 任一满足 |
| `all_of` | `selectors` | 全部满足 |
| `match` | `condition`、`strict` | 按模板 json 的 key 匹配（语义同函数侧的 `match`） |
| `custom` | `custom_id` | 交给 `CustomConditionSelectorEvent` 的监听者 |

```json
{ "type": "arcanumlib:any" }
```
```json
{ "type": "arcanumlib:index", "index": 0 }
```
```json
{ "type": "arcanumlib:condition_type", "conditionType": "minecraft:random_chance" }
```
```json
{ "type": "arcanumlib:not", "selector": { "type": "arcanumlib:index", "index": 99 } }
```
```json
{
  "type": "arcanumlib:any_of",
  "selectors": [
    { "type": "arcanumlib:index", "index": 0 },
    { "type": "arcanumlib:condition_type", "conditionType": "minecraft:random_chance" }
  ]
}
```
```json
{
  "type": "arcanumlib:all_of",
  "selectors": [
    { "type": "arcanumlib:any" },
    { "type": "arcanumlib:not", "selector": { "type": "arcanumlib:index", "index": 99 } }
  ]
}
```
```json
{
  "type": "arcanumlib:match",
  "condition": { "condition": "minecraft:random_chance", "chance": 1.0 }
}
```
```json
{ "type": "arcanumlib:custom", "custom_id": "my_condition_hook" }
```

---

## 14. 条件级操作（2）

写在 `modify_condition.ops` 里。注册表：`arcanumlib:condition_operator`。

| id | 字段 | 说明 |
|---|---|---|
| `remove_condition` | — | 删掉当前条件 |
| `replace_condition` | `condition` | 把当前条件换成新条件（每处一份独立实例） |

```json
{ "type": "arcanumlib:remove_condition" }
```
```json
{
  "type": "arcanumlib:replace_condition",
  "condition": { "condition": "minecraft:random_chance", "chance": 0.25 }
}
```

---

## 15. 自定义钩子（9 个事件）

前五个是**选择器**的自定义判断，后三个是**操作**的自定义行为，最后一个是整条注入器的替换。
全部在 `NeoForge.EVENT_BUS` 上监听。

| 事件 | 触发时机 | 配套的 json |
|---|---|---|
| `CustomTableSelectorEvent` | 判定表选择器 `custom` 时 | `{"type":"arcanumlib:custom","custom_id":"..."}` |
| `CustomPoolSelectorEvent` | 判定池选择器 `custom` 时 | 同上（写在 `poolSelector` 里） |
| `CustomEntrySelectorEvent` | 判定条目选择器 `custom` 时 | 同上（写在 `entrySelector` 里） |
| `CustomFunctionSelectorEvent` | 判定函数选择器 `custom` 时 | 同上（写在 `functionSelector` 里） |
| `CustomConditionSelectorEvent` | 判定条件选择器 `custom` 时 | 同上（写在 `conditionSelector` 里） |
| `TableCustomOpEvent` | 执行 `table_custom` 时 | `{"type":"arcanumlib:table_custom","custom_id":"..."}` |
| `PoolCustomOpEvent` | 执行 `pool_custom` 时 | `{"type":"arcanumlib:pool_custom","custom_id":"..."}` |
| `EntryCustomOpEvent` | 执行 `entry_custom` 时 | `{"type":"arcanumlib:entry_custom","custom_id":"..."}` |
| `LootInjectorApplyEvent` | 每个注入器被**应用之前** | —（可整条替换，也可取消） |

> **8 个 `custom` 事件都带 `getRegistries()`**（`LootInjectorApplyEvent` 不带：它替换整条注入器，用不到）。
> 需要读注册表 / 做 codec 往返时用它，与自定义选择器 `test(...)`、自定义 op `apply(...)` 收到的 `registries` 是同一个：
>
> ```java
> RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, event.getRegistries());
> ```

```java
@EventBusSubscriber(modid = MyMod.MODID)
public final class MyLootHooks {

    /** 选择器：自定义判断。拿到什么就按什么判，只需要 setResult。 */
    @SubscribeEvent
    public static void onCustomEntry(CustomEntrySelectorEvent event) {
        if (!"my_entry_hook".equals(event.getId())) {
            return;
        }

        // 例：只命中"没挂任何条件的单例条目"
        // 需要注册表时用 event.getRegistries()（8 个 custom 事件都有，见上表下方说明）
        event.setResult(event.getEntry() instanceof LootPoolSingletonContainer
                && event.getEntry().conditions.isEmpty());
    }

    /** 操作：直接改"当前条目"。entries 已经是可变列表，可以 set / remove。 */
    @SubscribeEvent
    public static void onEntryCustom(EntryCustomOpEvent event) {
        if (!"my_entry_hook".equals(event.getId())) {
            return;
        }

        event.getEntries().set(event.getIndex(), LootItem.lootTableItem(Items.DIAMOND).build());
    }

    /** 操作：改"当前池"。 */
    @SubscribeEvent
    public static void onPoolCustom(PoolCustomOpEvent event) {
        if (!"my_pool_hook".equals(event.getId())) {
            return;
        }

        PoolOperator.mutableEntries(event.getPool()).add(LootItem.lootTableItem(Items.EMERALD).build());
    }

    /** 操作：改整张表。这时已经知道是哪个注入器在改了。 */
    @SubscribeEvent
    public static void onTableCustom(TableCustomOpEvent event) {
        if (!"my_table_hook".equals(event.getId())) {
            return;
        }

        event.getTable().pools.add(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .add(LootItem.lootTableItem(Items.DIAMOND_BLOCK))
                .build());
    }

    /** 整条替换：可以按表 id / 优先级重新组织一个注入器。 */
    @SubscribeEvent
    public static void onApply(LootInjectorApplyEvent event) {
        if (!"mymod:my_injector".equals(event.getInjector().id().toString())) {
            return;
        }

        LootInjector original = event.getInjector();
        event.setInjector(new LootInjector(original.id(), original.tableSelector(), original.priority(),
                List.of(new TableOperator.AddPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.DIAMOND_BLOCK))
                        .build()))));
    }
}
```

---

## 16. 给模组的扩展点

整个注入器体系有 **10 个自定义注册表**，每个都是 `MapCodec` 的容器，用标准的 `DeferredRegister` 注册：

| 注册表 key | 装什么 |
|---|---|
| `arcanumlib:table_selector` | 表选择器 |
| `arcanumlib:pool_selector` | 池选择器 |
| `arcanumlib:entry_selector` | 条目选择器 |
| `arcanumlib:function_selector` | 函数选择器 |
| `arcanumlib:condition_selector` | 条件选择器 |
| `arcanumlib:table_operator` | 表级 op |
| `arcanumlib:pool_operator` | 池级 op |
| `arcanumlib:entry_operator` | 条目级 op |
| `arcanumlib:function_operator` | 函数级 op |
| `arcanumlib:condition_operator` | 条件级 op |

```java
public final class MyOps {
    // 以"条目选择器"为例：注册一个新的 id
    public static final DeferredRegister<EntrySelectorType> ENTRY_SELECTORS =
            DeferredRegister.create(LootInjectorOps.ENTRY_SELECTOR_KEY, MyMod.MODID);

    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> IS_SHINY =
            ENTRY_SELECTORS.register("is_shiny", () -> new EntrySelectorType(IsShiny.CODEC));

    public static void register(IEventBus bus) {
        ENTRY_SELECTORS.register(bus);
    }
}

/** 实现只要一个 test；5 个选择器的 test 与 5 个 op 的 apply 都会收到 registries（读注册表 / codec 往返时用）。 */
public record IsShiny(String marker) implements EntrySelector {

    public static final MapCodec<IsShiny> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("marker").forGetter(IsShiny::marker)).apply(instance, IsShiny::new));

    @Override
    public EntrySelectorType getType() {
        return MyOps.IS_SHINY.get();
    }

    @Override
    public boolean test(int index, LootPoolEntryContainer entry, LootPool pool, HolderLookup.Provider registries) {
        return entry instanceof LootItem lootItem && lootItem.item.is(MyTags.SHINY);
    }
}
```

```json
{ "type": "mymod:is_shiny", "marker": "x" }
```

**实现自定义 op 时的三条硬性要求**：

1. **改完列表必须重算复合体**——判定/结算用的是 `compositeCondition` / `compositeFunction` / `compositePredicates`：
   - 条目/池的条件：`compositeCondition = Util.allOf(conditions)`
   - 条目/池/表的函数：`compositeFunction = LootItemFunctions.compose(functions)`
   - **函数自带的条件**：`compositePredicates = Util.allOf(predicates)`（字段在 `LootItemConditionalFunction` 上）
   - 复合条件自身：`composedPredicate`（字段在 `CompositeLootItemCondition` 上）
2. **增删前把列表换成可变的**（`new ArrayList<>(...)`）：列表来自 codec，可能是不可变实现。
   池级有个现成的 `PoolOperator.mutableEntries(pool)`。
3. **不要递归遍历里改列表**：作用域已经做了快照遍历，实现侧只需要处理"当前对象"。

> 需要读写的原版字段都在 `META-INF/accesstransformer.cfg` 里开了（`LootTable.pools`/`functions`、
> `LootPool.entries`/`conditions`/`functions` 等）；自己写 op 时如果要用别的字段，**检查是否已开 AT**。

---

## 17. 常见任务菜谱

### 17.1 给某张表追加一个"额外掉落池"

```json
{
  "priority": 1000,
  "tableSelector": { "type": "arcanumlib:id", "id": "minecraft:chests/simple_dungeon" },
  "ops": [
    {
      "type": "arcanumlib:add_pool",
      "pool": {
        "rolls": 1.0,
        "bonus_rolls": 0.0,
        "entries": [
          {
            "type": "minecraft:item",
            "name": "minecraft:diamond",
            "weight": 1,
            "conditions": [{ "condition": "minecraft:random_chance", "chance": 0.05 }]
          }
        ]
      }
    }
  ]
}
```

### 17.2 把某张表里的"线"换成"皮革"

```json
{
  "tableSelector": { "type": "arcanumlib:id", "id": "minecraft:chests/simple_dungeon" },
  "ops": [
    {
      "type": "arcanumlib:modify_pool",
      "poolSelector": { "type": "arcanumlib:any" },
      "ops": [
        {
          "type": "arcanumlib:modify_entry",
          "entrySelector": { "type": "arcanumlib:item", "item": "minecraft:string" },
          "ops": [
            { "type": "arcanumlib:replace_entry", "entry": { "type": "minecraft:item", "name": "minecraft:leather" } }
          ]
        }
      ]
    }
  ]
}
```

### 17.3 删掉所有"权重占比低于 1%"的条目

```json
{
  "ops": [
    {
      "type": "arcanumlib:modify_pool",
      "poolSelector": { "type": "arcanumlib:any" },
      "ops": [
        {
          "type": "arcanumlib:modify_entry",
          "entrySelector": { "type": "arcanumlib:weight_share", "min": 0.0, "max": 0.01 },
          "ops": [{ "type": "arcanumlib:remove_entry" }]
        }
      ]
    }
  ]
}
```

### 17.4 去掉所有"抢夺加成"

```json
{
  "ops": [
    {
      "type": "arcanumlib:modify_pool",
      "poolSelector": { "type": "arcanumlib:any" },
      "ops": [
        {
          "type": "arcanumlib:modify_entry",
          "entrySelector": { "type": "arcanumlib:any" },
          "ops": [
            { "type": "arcanumlib:remove_function", "functionTypes": ["minecraft:enchanted_count_increase"] }
          ]
        }
      ]
    }
  ]
}
```

### 17.5 只把"某个特定参数的 set_count"换成别的数量

`set_count(2)` 和 `set_count(+3)` 类型相同，用 `function_type` 会一起选中，用 `match` 才能只挑一个：

```json
{
  "ops": [
    {
      "type": "arcanumlib:modify_pool",
      "poolSelector": { "type": "arcanumlib:any" },
      "ops": [
        {
          "type": "arcanumlib:modify_entry",
          "entrySelector": { "type": "arcanumlib:any" },
          "ops": [
            {
              "type": "arcanumlib:modify_function",
              "functionSelector": {
                "type": "arcanumlib:match",
                "function": { "function": "minecraft:set_count", "count": 3.0 }
              },
              "ops": [
                {
                  "type": "arcanumlib:replace_function",
                  "function": { "function": "minecraft:set_count", "count": 5.0, "add": true }
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

### 17.6 摘掉"某个函数自带的恒假条件"（其它写法都做不到）

```json
{
  "ops": [
    {
      "type": "arcanumlib:modify_pool",
      "poolSelector": { "type": "arcanumlib:any" },
      "ops": [
        {
          "type": "arcanumlib:modify_entry",
          "entrySelector": { "type": "arcanumlib:any" },
          "ops": [
            {
              "type": "arcanumlib:modify_function",
              "functionSelector": { "type": "arcanumlib:has_condition", "conditionType": "minecraft:random_chance" },
              "ops": [
                {
                  "type": "arcanumlib:modify_condition",
                  "conditionSelector": { "type": "arcanumlib:condition_type", "conditionType": "minecraft:random_chance" },
                  "ops": [{ "type": "arcanumlib:remove_condition" }]
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

### 17.7 让"某个池"必然掉落（摘掉池自身的概率门）

```json
{
  "ops": [
    {
      "type": "arcanumlib:modify_pool",
      "poolSelector": { "type": "arcanumlib:name", "name": "my_pool" },
      "ops": [
        {
          "type": "arcanumlib:modify_condition",
          "conditionSelector": { "type": "arcanumlib:any" },
          "ops": [{ "type": "arcanumlib:remove_condition" }]
        }
      ]
    }
  ]
}
```

---

## 18. 用数据生成写注入器

不想手写 json 就用 datagen，顺带享受编译期检查与 codec 往返验证：

```java
public final class MyLootInjectors extends LootInjectorTemplate {

    public MyLootInjectors(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, "My Mod Loot Injectors");
    }

    /** 注入器 id 的命名空间，最终落在 data/<ns>/loot_injecter/<path>.json。 */
    @Override
    protected ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("mymod", path);
    }

    /** 模板骨架用的概率条件；基础概率与每级抢夺加成都在 Drop 里。 */
    @Override
    protected LootItemCondition.Builder chanceCondition(Drop drop) {
        return LootItemRandomChanceCondition.randomChance((float) drop.chance());
    }

    @Override
    protected List<LootInjector> injectors(HolderLookup.Provider registries) {
        // 模板提供的便捷骨架：按表 id / 生物 / 任意表选择器，直接给一串 Drop
        return List.of(
                merged("dungeon_extra", BuiltInLootTables.SIMPLE_DUNGEON,
                        new Drop(0.25D, 0.02D, () -> LootItem.lootTableItem(Items.GOLD_INGOT)
                                .apply(lootingCount(registries))),
                        new Drop(0.05D, () -> LootItem.lootTableItem(Items.EMERALD))),

                // 需要嵌套作用域时直接构造 op 树
                raw("dungeon_tune",
                        new TableSelector.ById(BuiltInLootTables.SIMPLE_DUNGEON.location()), 1100,
                        new TableOperator.ModifyPool(new PoolSelector.ByName("my_pool"),
                                new PoolOperator.ModifyEntry(new EntrySelector.Any(),
                                        new EntryOperator.SetWeight(20),
                                        new EntryOperator.SetQuality(1)))));
    }

    private LootInjector raw(String path, TableSelector tableSelector, int priority, TableOperator... ops) {
        return new LootInjector(id(path), tableSelector, priority, List.of(ops));
    }
}
```

再挂到 `GatherDataEvent`：

```java
@EventBusSubscriber(modid = MyMod.MODID)
public final class MyDatagen {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(),
                new MyLootInjectors(event.getGenerator().getPackOutput(), event.getLookupProvider()));
    }
}
```

> **附魔引用的坑**：`enchanted_count_increase` 这类函数带 `Holder<Enchantment>`，datagen 阶段附魔注册表里查不到，
> 带它的注入器只能**手写 json**（或者用 `event.getLookupProvider()` 里已有的引用）。

---

## 19. 速查表

### 全部 op（37）

| 层 | id |
|---|---|
| 表（7） | `add_pool`、`modify_pool`、`modify_function`、`set_function`、`add_function`、`remove_function`、`table_custom` |
| 池（12） | `remove_pool`、`add_entry`、`modify_entry`、`modify_function`、`modify_condition`、`set_condition`、`add_condition`、`remove_condition`、`set_function`、`add_function`、`remove_function`、`pool_custom` |
| 条目（13） | `remove_entry`、`replace_entry`、`set_weight`、`set_quality`、`modify_function`、`modify_condition`、`set_condition`、`add_condition`、`remove_condition`、`set_function`、`add_function`、`remove_function`、`entry_custom` |
| 函数（3） | `remove_function`、`replace_function`、`modify_condition` |
| 条件（2） | `remove_condition`、`replace_condition` |

### 全部选择器（53）

| 族 | id | 可选字段 |
|---|---|---|
| 表（12） | `all` / `id` / `namespace` / `path_prefix` / `entity_tag` / `block_tag` / `entity_type` / `block_type` / `not` / `any_of` / `all_of` / `custom` | `id`、`namespace`、`prefix`、`tag`、`entity`、`block`、`condition`、`conditions`、`custom_id` |
| 池（9） | `any` / `index` / `name` / `has_entry` / `total_weight` / `not` / `any_of` / `all_of` / `custom` | `index`、`name`、`entrySelector`、`min`、`max`、`condition`、`conditions`、`custom_id` |
| 条目（15） | `any` / `index` / `item` / `tag` / `item_with_tag` / `entry_type` / `weight_in_range` / `weight_share` / `has_condition` / `has_function` / `has_child` / `not` / `any_of` / `all_of` / `custom` | `index`、`item`、`tag`、`entryType`、`min`、`max`、`conditionType`、`functionType`、`entrySelector`、`condition`、`conditions`、`custom_id` |
| 函数（9） | `any` / `index` / `function_type` / `has_condition` / `not` / `any_of` / `all_of` / `match` / `custom` | `index`、`functionType`、`conditionType`、`selector`、`selectors`、`function`、`strict`、`custom_id` |
| 条件（8） | `any` / `index` / `condition_type` / `not` / `any_of` / `all_of` / `match` / `custom` | `index`、`conditionType`、`selector`、`selectors`、`condition`、`strict`、`custom_id` |

> 注意字段名有两处不统一（历史原因，**以实际字段名为准**）：
> 表/池/条目选择器的 `not` 用 `condition` 字段，而函数/条件选择器的 `not` 用 `selector`；
> `any_of`/`all_of` 在表/池/条目侧是 `conditions`，在函数/条件侧是 `selectors`。

### 同一个 id 会在多族 / 多层重复出现（正常现象）

每一族都有自己的注册表，同一个 id 会在**各自的注册表里各注册一份**，json 里靠**所在字段 / 所在层级**消歧：

| 重复的 id | 出现在 |
|---|---|
| `any`、`index`、`not`、`any_of`、`all_of`、`custom` | 五个选择器族**各一份** |
| `set_function`、`add_function`、`remove_function` | 表 / 池 / 条目 / 函数 / 条件 —— 作用的是**各自那个容器的**函数列表 |
| `set_condition`、`add_condition`、`remove_condition` | 池 / 条目 / 条件 |
| `remove_function`、`replace_function` | 也可以在**函数级**出现，但那时作用的是"当前函数"本身，而不是某个容器 |

所以 `{"type":"arcanumlib:custom","custom_id":"x"}` 写在 `tableSelector` 里就是表选择器、写在 `functionSelector` 里就是函数选择器——
**`type` 不需要带族名前缀**。分发是按 `(注册表, id)` 做的，而注册表由字段决定；写错位置会在解析期直接报"这一层没有这个类型"，不会静默走错分支。

> 唯一"带前缀"的是**操作侧**的三个自定义：`table_custom` / `pool_custom` / `entry_custom`。
> 它们各自是所在层级唯一的自定义入口，带前缀便于阅读；选择器侧的 `custom` 则遵循"通用 id 不带前缀"的风格。

### 原版 json 里函数与条件的类型 key

- 函数：`"function": "minecraft:set_count"`（`LootItemFunctions.ROOT_CODEC` 的分发字段）
- 条件：`"condition": "minecraft:random_chance"`（`LootItemCondition.DIRECT_CODEC` 的分发字段）

`remove_function.functionTypes` / `remove_condition.conditionTypes` 里写的就是这两个 id。

---

## 20. 语义边界与坑

1. **json 严格解析**：`//` 注释和尾随逗号都会解析失败（见文首）。
2. **权重是静态口径**：`total_weight` / `weight_in_range` / `weight_share` 不含 `quality × 幸运`、
   不按条目条件过滤、`alternatives` 把所有分支都累加。**不要用它们精确复现某次掷骰的概率**，
   它们适合做"挑小池 / 挑稀有条目"这类粗筛。
3. **无名池匹配不到 `name`**：原版大多数池没有名字。
4. **`tag` 与 `item_with_tag` 不是一回事**：前者匹配 `minecraft:tag` **条目**，后者匹配
   `minecraft:item` 条目并检查它的物品是否属于标签。原版表里几乎全是后者。
5. **复合条目没有 `weight` / `quality` / `functions`**：`set_weight`、`set_quality`、
   `set_*function`、`remove_function` 遇到 `alternatives`/`sequence`/`group` 会跳过；
   `conditions` 则所有条目都有。
6. **`alternatives` 掷骰只走第一个条件通过的分支**：所以你往它的子项上加条件、或者用
   `has_child` 判断时，要意识到运行时只有一个分支真正生效。
7. **同一 priority 的顺序不确定**：同一张表上的多个注入器请给不同 priority。
8. **`match` 模板不校验**：key 写错会静默失效。
9. **`replace_*` 是深拷贝**：每处替换都会重新解码一份独立实例，所以后续改动不会串味；
   代价是需要在 `RegistryOps` 下做一次 codec 往返（只发生在应用阶段，不是掷骰时）。
10. **`set_weight` / `set_quality` 的负数表示"不改"**：这是沿用旧版语义，方便直接搬旧 json；
    想真的设成 0 就写 0。
11. **`LootTable` 没有 `conditions`**：所以表级没有 `set_condition` / `add_condition` / `remove_condition`。
12. **原版没有任何地方调用 `LootTable.freeze()`**：但我们是直接改 AT 开的字段、会绕过它的
    `checkFrozen()` 守卫。将来若有谁冻结了共享表，注入会破坏它的不可变假设。

---

## 21. 本模组提供的战利品条件

注入器里的 `conditions` / `add_condition` / `set_condition` 接受的是**普通原版条件 json**，
所以 ArcanumLib 自己提供的战利品条件也能直接写进去。它们都注册在 `minecraft:loot_condition_type` 下，
**条目如下**（以后会继续增加，每加一个就在这张表补一行）：

| 条件 id | 一句话说明 | 详细用法 |
|---|---|---|
| `arcanumlib:chance_modifiers` | 概率加工链：从 `base_chance` 起算，按顺序过一串加工器，再用最终概率做一次随机判定 | [战利品条件指南](Loot-Conditions.md) |

用法示例（挂在某个条目上）：

```json
{
  "type": "arcanumlib:modify_entry",
  "entrySelector": { "type": "arcanumlib:item", "item": "minecraft:gunpowder" },
  "ops": [
    {
      "type": "arcanumlib:add_condition",
      "conditions": [
        {
          "condition": "arcanumlib:chance_modifiers",
          "base_chance": 0.5,
          "chance_modifiers": [{ "type": "arcanumlib:difficulty_factor", "hard": 1.5 }]
        }
      ]
    }
  ]
}
```

想判断"条目身上有没有挂这个条件"，用条目选择器
`{"type":"arcanumlib:has_condition","conditionType":"arcanumlib:chance_modifiers"}`。

---
## 附：相关源码位置

| 内容 | 文件 |
|---|---|
| 各层 op 接口与实现 | `modules/loot/lootInjector/operator/{Table,Pool,Entry,Function,Condition}Operator.java` |
| 五族选择器 | `modules/loot/lootInjector/selector/{Table,Pool,Entry,Function,Condition}Selector.java` |
| 权重口径 | `modules/loot/lootInjector/selector/PoolWeights.java` |
| `match` 的子集比对 | `modules/loot/lootInjector/selector/JsonSubset.java` |
| 注册表与全部注册 | `modules/loot/lootInjector/LootInjectorOps.java` |
| 注入器本体与应用 | `modules/loot/lootInjector/{LootInjector,LootInjectorApplier,LootInjectorReloadListener}.java` |
| 事件 | `modules/loot/lootInjector/event/` |
| datagen 基类与模板 | `modules/loot/lootInjector/data/{LootInjectorProvider,LootInjectorTemplate}.java` |
| 访问转换器 | `src/main/resources/META-INF/accesstransformer.cfg` |
| 官方测试数据（37 个 op 与 52 个选择器都有 json 示例；`tableSelector` 的默认值 `all` 因为是默认值不会出现在生成物里） | `src/generated/resources/data/arcanumlib/loot_injecter/` |
