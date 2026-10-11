# 战利品条件（Loot Conditions）

ArcanumLib 提供的战利品条件都收录在这里。它们都是**原版 `LootItemCondition`**，
所以能写进任何战利品表的 `conditions`，也能被注入器体系当普通条件用。

> 本文是**容器**：以后每加一个新条件，在下面的「条件列表」加一行、并补一节详细说明即可。
> 目前有 2 个条目。

## 条件列表

| 条件 id | 一句话说明 | 详情 |
|---|---|---|
| [`arcanumlib:chance_modifiers`](#arcanumlibchance_modifiers) | 概率加工链：从 `base_chance` 起算，按顺序过一串加工器，再用最终概率随机判定 | [下一节](#arcanumlibchance_modifiers) |
| [`arcanumlib:has_stage`](#arcanumlibhas_stage) | 阶段条件：装了 AStages 时，持有匹配阶段的玩家 / 世界才通过 | [文末一节](#arcanumlibhas_stage) |

---

## `arcanumlib:chance_modifiers`

它从 `base_chance` 起算，**按顺序**过一遍你给的加工器列表，用最终概率做一次随机判定。

它解决的问题是原版 `minecraft:random_chance` 做不到的事：**概率随上下文浮动**——
按游戏难度、按工具上的抢夺等级、按攻击者的幸运属性来加成，而且这些能**任意串联**。

- 注册：`minecraft:loot_condition_type` 下的 `arcanumlib:chance_modifiers`
- 源码：`modules/loot/lootCondition/`
- 包路径：`com.chen1335.arcanumlib.modules.loot.lootCondition`

> **json 严格解析**：与注入器一样走 `SimpleJsonResourceReloadListener`（`lenient = false`），
> **不能写 `//` 注释、不能有尾随逗号**。本文示例全部是严格合法 json。

### 目录

1. [最小示例](#1-最小示例)
2. [求值模型](#2-求值模型)
3. [字段](#3-字段)
4. [九个加工器](#4-九个加工器)
5. [两种 operation 语义（最容易错的地方）](#5-两种-operation-语义最容易错的地方)
6. [实体来源 EntitySource](#6-实体来源-entitysource)
7. [EnchantedLootEvent：让外部改附魔等级](#7-enchantedlootevent让外部改附魔等级)
8. [取不到工具 / 实体时的行为](#8-取不到工具--实体时的行为)
9. [常用组合菜谱](#9-常用组合菜谱)
10. [在注入器里使用](#10-在注入器里使用)
11. [用 datagen 构造](#11-用-datagen-构造)
12. [扩展：写自己的加工器](#12-扩展写自己的加工器)
13. [坑与注意事项](#13-坑与注意事项)

---

### 1. 最小示例

因为它就是一个普通战利品条件，所以能写进**任何**战利品表的 `conditions`：

```json
{
  "type": "minecraft:item",
  "name": "minecraft:diamond",
  "conditions": [
    {
      "condition": "arcanumlib:chance_modifiers",
      "base_chance": 0.05,
      "chance_modifiers": [
        { "type": "arcanumlib:tool_enchantment_level", "enchantment": "minecraft:fortune", "per_level": 0.02 },
        { "type": "arcanumlib:clamp", "min": 0.0, "max": 0.5 }
      ]
    }
  ]
}
```

读法：基础 5%，工具上每级**时运**让概率 **×（1 + 0.02）**，最后夹到 `[0, 0.5]`。

- `base_chance` 省略时为 `1.0`；
- `chance_modifiers` 省略时为空链，等价于 `minecraft:random_chance`。

最简形式（等价于"必过"）：

```json
{ "condition": "arcanumlib:chance_modifiers" }
```

---

### 2. 求值模型

```
chance = base_chance
for (modifier : chance_modifiers) {
    chance = modifier.apply(chance, context)
}
通过 = random.nextFloat() < chance
```

三个关键点：

1. **基础概率是条件自己的字段**，不是链首的一个加工器——链只负责"加工"，起点由 `base_chance` 给。
2. **按列表顺序依次应用**，前一个的输出是后一个的输入。所以顺序会改变结果（例如"先乘后加"与"先加后乘"不同）。
3. **概率不做夹紧**：链算完直接拿去判定，所以
   - `> 1`：`nextFloat()` 永远 `< 1`，**必过**；
   - `<= 0`：**必不过**；
   - 中间过程出现负数也没人拦你。
   想限制范围就自己加一个 `clamp`（通常放链尾）。

`ChanceModifierCondition` 还提供了一个不掷骰的入口，方便外部或调试读取"最终概率"：

```java
double chance = new ChanceModifierCondition(0.25D, List.of(...)).resolveChance(context);
```

---

### 3. 字段

| 字段 | 类型 | 必填 | 默认 | 说明 |
|---|---|---|---|---|
| `condition` | 字符串 | ✅ | — | 固定为 `arcanumlib:chance_modifiers` |
| `base_chance` | double | | `1.0` | 链的起点 |
| `chance_modifiers` | 数组 | | `[]` | 加工器列表，按顺序应用 |

---

### 4. 九个加工器

注册表：`arcanumlib:chance_modifier`。每一项都是 `{ "type": "arcanumlib:<id>", ... }`。

| id | 字段 | 语义 |
|---|---|---|
| `apply` | `operation`（默认 `multiply`）、`value`（必填） | `operation` 决定加还是乘：`概率 + value` / `概率 × value` |
| `set_chance` | `value`（必填） | **直接设为** value，丢掉前面的结果 |
| `clamp` | `min`（默认 `0.0`）、`max`（默认 `1.0`） | 夹到 `[min, max]` |
| `invert` | — | `1 - 概率`（取补） |
| `difficulty_factor` | `operation`（默认 `multiply`）、`peaceful`、`easy`、`normal`、`hard`（都可选） | 按当前难度取档位值，用 `operation` 加工 |
| `tool_enchantment_level` | `enchantment`（必填）、`operation`（默认 `multiply`）、`per_level`（默认 `0.05`） | 按工具上某附魔的等级加工 |
| `entity_attribute` | `attribute`（必填）、`entity`（默认 `attacking_entity`）、`operation`（默认 `multiply`）、`per_point`（默认 `0.05`） | 按某实体的一条属性加工 |
| `stage` | `stage`（必填，可简写为字符串）、`desc`、`check_server_stages`、`success`、`fail` | 按 AStages 阶段二选一：命中依次过 `success` 链，否则过 `fail` 链 |
| `predicate` | `predicates`（必填）、`success`、`fail` | 谓词**全部**通过就走 `success` 链，否则走 `fail` 链 |

#### 4.1 `apply` — 加或乘

```json
{ "type": "arcanumlib:apply", "operation": "add", "value": 0.1 }
```
```json
{ "type": "arcanumlib:apply", "operation": "multiply", "value": 2.0 }
```
`operation` 缺省是 `multiply`：乘就是 `chance × value`（乘 0 即"必不过"、乘 2 即"翻倍"）；
写 `add` 就是 `chance + value`，value 可为负，用来折减。

ALI 里显示成一行 `概率修正 : ×N`（加法是 `+N`，负值就是 `-N`）：

```
概率修正 : ×2.0
```
#### 4.2 `set_chance` — 直接设值

```json
{ "type": "arcanumlib:set_chance", "value": 0.08 }
```
放在链首用于定初始值（此时和 `base_chance` 重复，通常没必要）；
**放在链中间会把前面所有加工结果丢掉**，所以一般只在"某条件成立就强制改成固定概率"时用。

#### 4.3 `clamp` — 夹取范围

```json
{ "type": "arcanumlib:clamp", "min": 0.0, "max": 1.0 }
```
省略字段就是上面的默认值。因为它"兜底"的性质，通常放**链尾**，避免前面的加工算出负数或超过 1。

#### 4.4 `invert` — 取补

```json
{ "type": "arcanumlib:invert" }
```
`0.95 → 0.05`。典型用法："95% 概率不掉"改成"5% 概率掉落"。

#### 4.5 `difficulty_factor` — 按游戏难度加工

```json
{
  "type": "arcanumlib:difficulty_factor",
  "operation": "multiply",
  "peaceful": 1.0,
  "easy": 1.0,
  "normal": 1.15,
  "hard": 1.3
}
```
困难难度下概率 ×1.3，普通 ×1.15，和平/简单不变。

- `operation` 默认 `multiply`；用 `add` 时档位值就是"加多少"；
- **档位可以留空**：留空按 `operation` 的**中性值**处理（乘法 1.0 / 加法 0.0），即"该难度下不改变概率"。所以只关心困难难度时可以只写 `hard`：

```json
{ "type": "arcanumlib:difficulty_factor", "hard": 1.5 }
```

ALI 里每个档位显示成一行（`和平 : ×1.0`），**等于中性值的档位不显示**（加法 0 / 乘法 1 就是"没改"），所以上面只写 `hard` 的例子实际显示为：

```
难度修正 :
    ->困难 : ×1.5
```


#### 4.6 `tool_enchantment_level` — 按工具附魔等级加工

```json
{
  "type": "arcanumlib:tool_enchantment_level",
  "enchantment": "minecraft:looting",
  "operation": "multiply",
  "per_level": 0.05
}
```
抢夺每级让概率 ×（1 + 0.05），即抢夺 III → ×1.15。用 `add` 时是 `概率 + 等级 × per_level`。

**它取的是 `LootContextParams.TOOL`**，所以：
- 方块掉落（`THIS_ENTITY` 是挖矿玩家、`TOOL` 是手里的工具）和实体掉落（`TOOL` 是击杀者的武器）都适用；
- 取不到工具（例如宝箱战利品表）→ 按 **0 级**处理 → **不改变概率**（见第 8 节）。

> 附魔是**数据驱动注册表**，所以这里存的是 `ResourceKey`，运行时再取 `Holder`；
> 因此 json 里写的是附魔 id（`"minecraft:looting"`），不像属性那样能直接是 `Holder`。

#### 4.7 `entity_attribute` — 按实体属性加工

```json
{
  "type": "arcanumlib:entity_attribute",
  "attribute": "minecraft:generic.luck",
  "entity": "attacking_entity",
  "operation": "multiply",
  "per_point": 0.01
}
```
攻击者每 1 点幸运让概率 ×（1 + 0.01）。用 `add` 时是 `概率 + 属性值 × per_point`。

- `entity` 见第 6 节，默认 `attacking_entity`；
- 取的是 `LivingEntity.getAttributes().getInstance(attribute)`，**该实体没有这条属性时原样返回**（不当 0），所以缺某个模组的属性不会把概率意外清零。

#### 4.8 `stage` — 按阶段二选一（需要 AStages）

先判断阶段：命中就依次过 `success` 链，否则依次过 `fail` 链；两条链本身都是普通加工器列表。

```json
{
  "type": "arcanumlib:stage",
  "stage": { "pattern": "chapter_*", "type": "wildcard" },
  "desc": "mymod.stage.chapter",
  "check_server_stages": false,
  "success": [
    { "type": "arcanumlib:apply", "value": 2.0 }
  ],
  "fail": [
    { "type": "arcanumlib:set_chance", "value": 0.05 }
  ]
}
```

`stage` / `desc` / `check_server_stages` 三个字段与 `arcanumlib:has_stage` 条件完全一致（`stage` 支持字符串简写或 `pattern`/`type` 对象）。`success` / `fail` 可省略，省略即空链（什么都不改）。

> **没装 AStages 时等同"未命中"**，所以永远走 `fail` 链。

ALI 里渲染成"作用域行带成功链"加一行「否则」带失败链，**某条链为空就整行不显示**：

```
阶段修正 :
    ->玩家阶段 : 第一章
        ->概率修正 : ×2.0
    ->否则 :
        ->设置概率 : 0.05
```

（`阶段修正 :`、`玩家阶段 : %s` / `世界阶段 : %s`、`否则 :` 是本模组提供的语言键；`->` 是 ALI 自己画的树箭头。）
#### 4.9 `predicate` — 按谓词二选一

`predicates` 里的条件**全部**通过就依次过 `success` 链，否则依次过 `fail` 链（空列表视为通过，与原版 `all_of` 一致）；两条链都是一串普通加工器。

```json
{
  "type": "arcanumlib:predicate",
  "predicates": [
    { "condition": "minecraft:killed_by_player" },
    { "condition": "minecraft:inverted", "term": { "condition": "minecraft:killed_by_player" } }
  ],
  "success": [
    { "type": "arcanumlib:apply", "value": 3.0 }
  ],
  "fail": [
    { "type": "arcanumlib:apply", "operation": "add", "value": 0.05 }
  ]
}
```

`success` / `fail` 可省略（省略即空链）。ALI 里分成三段，**空的那一段不显示**；谓词逐行交给 ALI 自己的条件渲染，所以原版或其它模组的条件在这里的显示与别处一致：

```
谓词修正 :
    ->谓词 :
        -><谓词 1 在 ALI 里的渲染>
        -><谓词 2 在 ALI 里的渲染>
    ->满足条件时 :
        ->概率修正 : ×3.0
    ->否则 :
        ->概率修正 : +0.05
```

> `谓词` 走的是 ALI 自己的条件渲染，所以与 ALI 里别处显示的条件一致。注意 ALI 对**少数条件**（如 `minecraft:random_chance`、`table_bonus`，正好也是它配置里 `ignoredPredicateConditions` 默认忽略的那几个）会标成「高级提示」，只有开启**原版高级物品提示（F3 + H）**才显示 —— 那种情况下本项目退回列出谓词的注册名，其余条件照常显示。
---

### 5. 两种 operation 语义（最容易错的地方）

`add` / `multiply` 这个枚举在不同加工器里含义**不一样**，代码里分成两个方法：

| 方法 | ADD | MULTIPLY | 谁在用 |
|---|---|---|---|
| `apply(chance, value)` —— **纯语义** | `chance + value` | `chance × value` | `difficulty_factor`（档位值本身就是乘数/加数） |
| `applyScaled(chance, delta)` —— **增量语义** | `chance + delta` | `chance × (1 + delta)` | `tool_enchantment_level`、`entity_attribute`（delta = 等级×per_level / 属性值×per_point） |

代入看差别：

- `difficulty_factor` + `multiply` + `hard: 1.3` → 概率 **×1.3**
- `tool_enchantment_level` + `multiply` + `per_level: 0.05` + 等级 3 → delta = 0.15 → 概率 **×(1 + 0.15) = ×1.15**

设计原因是 `per_level` / `per_point` 是**增量**：`per_level: 0.05` 的自然读法是"每级 +5%"，而不是"每级乘 0.05"（后者会让等级越高概率越接近 0）。而难度档位天然是"乘数"，所以用纯语义。

---

### 6. 实体来源 EntitySource

`entity_attribute` 的 `entity` 字段，四种取值：

| 值 | 取哪个实体 |
|---|---|
| `this_entity` | 上下文主角：**实体掉落**时是被击杀者，**宝箱 / 刷怪笼 / 考古**时是开箱的玩家 |
| `attacking_entity` | 攻击者（**默认**，最常用） |
| `direct_attacking_entity` | 直接攻击者（例如射出的那支箭） |
| `last_damage_player` | 最后造成伤害的玩家 |

所以"按击杀者属性"写 `attacking_entity`，"按被击杀者属性"（例如按怪物的某属性调整它自己的掉落）写 `this_entity`。

---

### 7. EnchantedLootEvent：让外部改附魔等级

`tool_enchantment_level` 在数出附魔等级之后，**不会直接使用**，而是先发一个事件：

```java
int level = tool.getEnchantmentLevel(holder);
return EnchantedLootEvent.post(holder, context, level);
```

监听者可以改这个等级，从而让"装备栏/词条/词缀"之类的系统参与到掉落概率里：

```java
@EventBusSubscriber(modid = MyMod.MODID)
public final class MyLooting {

    @SubscribeEvent
    public static void onEnchantedLoot(EnchantedLootEvent event) {
        if (!event.getEnchantment().is(Enchantments.LOOTING)) {
            return;
        }

        // 例：我的模组给击杀者额外 +1 级抢夺
        int bonus = myBonusLootingLevel(event.getContext());
        event.setLevel(event.getLevel() + bonus);
    }
}
```

事件 API：

| 方法 | 说明 |
|---|---|
| `getEnchantment()` | `Holder<Enchantment>`，正在被读的附魔 |
| `getContext()` | `LootContext`；实体用 `LootContextParams.THIS_ENTITY` / `ATTACKING_ENTITY` 取 |
| `getLevel()` / `setLevel(int)` | 原本的等级 / 改成你想要的值 |

#### 它不止作用于这个条件

同一个事件还被 **4 个原版机制**触发（由 ArcanumLib 的 mixin 接管），所以监听一次就能同时影响：

| 原版机制 | 类 |
|---|---|
| `apply_bonus`（时运/抢夺加掉落数量） | `ApplyBonusCount` |
| `table_bonus`（按等级查概率表） | `BonusLevelTableCondition` |
| `enchanted_count_increase`（加数量） | `EnchantedCountIncreaseFunction` |
| `random_chance_with_enchanted_bonus`（加概率） | `LootItemRandomChanceWithEnchantedBonusCondition` |

再加上本指南的 `tool_enchantment_level`，一共**五个触发源**。想"让某个装备效果加抢夺"，监听这一个事件就够了。

---

### 8. 取不到工具 / 实体时的行为

这是两处**刻意的设计**，也是和"直觉清零"最容易冲突的地方：

| 加工器 | 缺什么 | 行为 |
|---|---|---|
| `tool_enchantment_level` | 拿不到 `TOOL`、或工具上没有该附魔 | 按 **0 级** → **不改变概率** |
| `entity_attribute` | 拿不到实体（该 `entity` 字段在上下文里没有）、或该实体没有这条属性 | **原样返回** → **不改变概率** |

也就是说这两者都是"**有就改、没有就不改**"，不会把概率变成 0 或 1。

> 注意 `tool_enchantment_level` 里"0 级"和"等级为 0"是同一支路（`level <= 0 ? chance : ...`），
> 所以工具上有附魔但等级为 0（正常不会发生）也是不改变。

---

### 9. 常用组合菜谱

#### 9.1 基础概率 + 抢夺加成 + 上限

```json
{
  "condition": "arcanumlib:chance_modifiers",
  "base_chance": 0.2,
  "chance_modifiers": [
    { "type": "arcanumlib:tool_enchantment_level", "enchantment": "minecraft:looting", "per_level": 0.1 },
    { "type": "arcanumlib:clamp", "max": 0.6 }
  ]
}
```
抢夺每级 +10%（乘 `1 + 0.1×等级`），最高 60%。

#### 9.2 难度越高越容易掉

```json
{
  "condition": "arcanumlib:chance_modifiers",
  "base_chance": 0.1,
  "chance_modifiers": [
    { "type": "arcanumlib:difficulty_factor", "easy": 0.5, "normal": 1.0, "hard": 2.0 }
  ]
}
```
简单难度概率 ×0.5，普通不变，困难 ×2（和平档留空 → 不改变）。

#### 9.3 按攻击者幸运值浮动（幸运 0 时不改变）

```json
{
  "condition": "arcanumlib:chance_modifiers",
  "base_chance": 0.15,
  "chance_modifiers": [
    { "type": "arcanumlib:entity_attribute", "attribute": "minecraft:generic.luck", "per_point": 0.02 },
    { "type": "arcanumlib:clamp", "max": 0.5 }
  ]
}
```

#### 9.4 "95% 概率不掉"改写成"5% 掉"

```json
{
  "condition": "arcanumlib:chance_modifiers",
  "base_chance": 0.95,
  "chance_modifiers": [{ "type": "arcanumlib:invert" }]
}
```

#### 9.5 时运 + 幸运，且两者都只在大于 0 时才生效

```json
{
  "condition": "arcanumlib:chance_modifiers",
  "base_chance": 0.08,
  "chance_modifiers": [
    { "type": "arcanumlib:tool_enchantment_level", "enchantment": "minecraft:fortune", "per_level": 0.05 },
    { "type": "arcanumlib:entity_attribute", "attribute": "minecraft:generic.luck", "entity": "this_entity", "operation": "add", "per_point": 0.01 },
    { "type": "arcanumlib:clamp", "min": 0.0, "max": 1.0 }
  ]
}
```

---

### 10. 在注入器里使用

> 注入器本身的用法见 [Loot Injector.md](Loot-Injector.md)；这一节只讲怎么把它们凑在一起。

注入器体系里的 `conditions` / `add_condition` / `set_condition` 接受的是**普通原版条件 json**，
所以这个条件可以原样写在里面：

```json
{
  "type": "arcanumlib:add_pool",
  "pool": {
    "rolls": 1.0,
    "entries": [
      {
        "type": "minecraft:item",
        "name": "minecraft:diamond",
        "conditions": [
          {
            "condition": "arcanumlib:chance_modifiers",
            "base_chance": 0.25,
            "chance_modifiers": [
              { "type": "arcanumlib:tool_enchantment_level", "enchantment": "minecraft:looting", "per_level": 0.02 }
            ]
          }
        ]
      }
    ]
  }
}
```

用注入器的 `modify_entry` 给已有条目挂上它也一样：

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

> 注入器侧只想判断"条目身上有没有挂这个条件"时，用条目选择器
> `{"type":"arcanumlib:has_condition","conditionType":"arcanumlib:chance_modifiers"}`。

---

### 11. 用 datagen 构造

```java
// 最简单的写法：builder(基础概率, 加工器...)
LootItemCondition.Builder condition = ChanceModifierCondition.builder(0.25D,
        new ChanceModifier.ToolEnchantmentLevel(Enchantments.LOOTING, ChanceModifier.Operation.ADD, 0.02D),
        new ChanceModifier.EntityAttribute(Attributes.LUCK, ChanceModifier.EntitySource.THIS_ENTITY,
                ChanceModifier.Operation.ADD, 0.01D),
        new ChanceModifier.Clamp(0.0D, 0.9D),
        ChanceModifier.DifficultyFactor.multiply(1.0D, 1.0D, 1.15D, 1.3D));
```

- 每个加工器都有 `of(...)` 工厂方法（`Apply.add(0.1D)`、`Clamp.of(0.0D, 1.0D)`、`Invert.of()`…）；
- `DifficultyFactor` 另有 `multiply(peaceful, easy, normal, hard)` 与 `add(...)` 两个便捷工厂，
   **比手搓 `Optional` 干净**（四个档位按 `PEACEFUL / EASY / NORMAL / HARD` 顺序）；
- 省略链就是 `ChanceModifierCondition.builder()`（等价于必过）。

注入器的模板基类 `LootInjectorTemplate` 就是用它来给整条掉落挂概率的：

```java
@Override
protected LootItemCondition.Builder chanceCondition(Drop drop) {
    return ChanceModifierCondition.builder(drop.chance(),
            new ChanceModifier.ToolEnchantmentLevel(Enchantments.LOOTING, ChanceModifier.Operation.ADD, drop.perLooting()),
            new ChanceModifier.Clamp(0.0D, 0.9D));
}
```

---

### 12. 扩展：写自己的加工器

接口对其它模组**开放**：`ChanceModifier` 是普通接口（不是 sealed），实现它、注册进 ArcanumLib 的注册表即可。

#### 12.1 三个公开 API

| 用途 | API |
|---|---|
| 实现接口 | `ChanceModifier`：`apply(double chance, LootContext context)` + `getType()` |
| 包装 codec | `ChanceModifierType`（`MapCodec` 的容器） |
| 注册表 key | `ChanceModifierTypes.REGISTRY_KEY` = `arcanumlib:chance_modifier` |

#### 12.2 完整示例

```java
public final class MyModifiers {

    /** 关键点：DeferredRegister 挂在【自己的 modid】下，注册表 key 用 ArcanumLib 的。 */
    public static final DeferredRegister<ChanceModifierType> MODIFIERS =
            DeferredRegister.create(ChanceModifierTypes.REGISTRY_KEY, MyMod.MODID);

    public static final DeferredHolder<ChanceModifierType, ChanceModifierType> SQUARE =
            MODIFIERS.register("square", () -> new ChanceModifierType(Square.CODEC));

    /** 无状态 record 最省事：同一个条件实例会被多次掷骰复用。 */
    public record Square(double factor) implements ChanceModifier {

        public static final MapCodec<Square> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.DOUBLE.fieldOf("factor").forGetter(Square::factor)).apply(instance, Square::new));

        @Override
        public ChanceModifierType getType() {
            return SQUARE.get();
        }

        @Override
        public double apply(double chance, LootContext context) {
            return chance * this.factor * this.factor;
        }
    }

    public static void register(IEventBus bus) {
        MODIFIERS.register(bus);
    }
}
```

注册之后，它就能像内置加工器一样用在任何地方：

```json
{
  "condition": "arcanumlib:chance_modifiers",
  "base_chance": 0.2,
  "chance_modifiers": [
    { "type": "mymod:square", "factor": 1.5 }
  ]
}
```

#### 12.3 两条硬性约定

1. **`apply` 必须是无状态纯函数**：条件实例会被同一个战利品表**反复使用**（每次掷骰都跑一遍链），
   在里面存可变状态会让结果不可预测。用 `record` 实现最省事。
2. **`getType()` 必须返回注册时那个 holder 的值**（即 `MODIFIERS.register(...)` 返回的 `DeferredHolder`）；
   返回别的实例会让 `type` 反查失败。另外类型**必须用 `MapCodec`**
   （`Codec.dispatch` 传方法引用会有歧义）。

#### 12.4 ALI tooltip 的兜底

装了 ALI 时，本模组的九个加工器有专门渲染；**外部加工器走兜底分支**，只显示它的注册名（例如 `mymod:square`）。
想要自己的渲染，参考 `compat/ali/AliLootPlugin.java` 自己写一个 ALI 插件。

#### 12.5 其它限制

- `operation` 只有 `add` / `multiply` 两种；
- 条件求值会**消耗一次随机数**（`nextFloat()`），与原版 `random_chance` 行为一致；
- 概率链本身**不做夹紧**，链尾不放 `clamp` 的话可能得到负数或 >1 的值（行为是"必不过 / 必过"）。

---

### 13. 坑与注意事项

1. **`operation` 的两种语义**（第 5 节）——`difficulty_factor` 的档位值是**乘数**，而
   `tool_enchantment_level.per_level` / `entity_attribute.per_point` 是**增量**。写错会得到相差很大的概率。
2. **顺序有意义**：`[{add 0.1}, {multiply 2}]` 与 `[{multiply 2}, {add 0.1}]` 结果不同。
3. **自定义加工器必须无状态**：接口对外开放，但实现会被反复调用；不要在 `apply` 里存可变状态。
   另外 ALI 的 tooltip 对外部加工器只显示注册名（第 12.4 节）。
4. **缺工具 / 缺属性都是"不改变"**，不是"清零"；反过来，如果你确实想要"没有就清零"，
   得自己加 `set_chance` 或用别的条件组合。
5. **`tool_enchantment_level` 依赖 `LootContextParams.TOOL`**：宝箱、刷怪笼、钓鱼这类上下文里根本没有工具，
   这个加工器就完全不生效（静默）。想在这种场合生效，用 `entity_attribute` 指向玩家属性。
6. **`entity_attribute` 的 `attribute` 是静态注册表**（`BuiltInRegistries.ATTRIBUTE`），
   而 `tool_enchantment_level` 的 `enchantment` 是**数据包注册表**——所以前者在 datagen 里可以直接写
   `Holder`，后者只能写 `ResourceKey`。
7. **json 严格解析**：不能有注释和尾随逗号。

---

### 附：相关源码

| 内容 | 文件 |
|---|---|
| 条件本体（求值 + builder） | `modules/loot/lootCondition/ChanceModifierCondition.java` |
| 九个加工器 + `Operation` / `EntitySource` | `modules/loot/lootCondition/ChanceModifier.java` |
| 阶段加工器（需 AStages） | `compat/astages/StageChanceModifier.java` |
| 加工器注册表与注册 | `modules/loot/lootCondition/ChanceModifierTypes.java` |
| 条件类型注册 | `modules/loot/lootCondition/LootConditionTypes.java` |
| 附魔等级事件 | `events/EnchantedLootEvent.java` |
| 让原版四种机制也发该事件的 mixin | `mixins/{ApplyBonusCount,BonusLevelTableCondition,EnchantedCountIncreaseFunction,LootItemRandomChanceWithEnchantedBonusCondition}Mixin.java` |
| 官方示例（9 个加工器各有用例） | `src/generated/resources/data/arcanumlib/loot_injecter/` 下的 `test_zombie_modifiers.json`（`apply` / `set_chance` / `invert`）与 `test_temple.json`、`test_zombies_tag.json`、`test_zombie_basic.json`（`clamp` / `difficulty_factor` / `tool_enchantment_level` / `entity_attribute`）、`test_stage_modifier.json`（`stage`）、`test_predicate_modifier.json`（`predicate`） |

---

## `arcanumlib:has_stage`

按 **AStages 的阶段**放行掉落：玩家（或全服）持有匹配的阶段才通过。没装 AStages 时恒不通过。

- 注册：`minecraft:loot_condition_type` 下的 `arcanumlib:has_stage`
- 源码：`compat/astages/HasStageCondition.java`；阶段名匹配器 `compat/astages/StagePattern.java`；AStages 调用全部收在 `compat/astages/`

### 格式

```json
{
  "condition": "arcanumlib:has_stage",
  "stage": { "pattern": "chapter_*", "type": "wildcard" },
  "desc": "mymod.stage.chapter",
  "check_server_stages": false
}
```

| 字段 | 必填 | 默认 | 说明 |
|---|---|---|---|
| `stage` | ✅ | — | 阶段匹配。写字符串就等于 `literal`；也可以写 `{ "pattern": ..., "type": ... }` |
| `desc` | | — | **本地化键**：ALI 里显示的阶段名。不写就显示 `pattern` 原文 |
| `check_server_stages` | | `false` | `true` 查服务器级（世界）阶段，`false` 查玩家级阶段 |

### 匹配方式

`type` 三档，语义对齐 AStages 自己的 `StageMatcher`：

| type | 说明 |
|---|---|
| `literal` | 完全相等（字符串简写即此档） |
| `wildcard` | `*` 通配任意长度，例如 `chapter_*` |
| `regex` | Java 正则，匹配整个阶段名 |

### 取哪个玩家

按顺序取第一个是玩家的：`LAST_DAMAGE_PLAYER` → `ATTACKING_ENTITY` → `THIS_ENTITY`；**都取不到就不通过**（不猜"最近玩家"，避免隐式行为）。

### 在 ALI 里显示

```
玩家阶段 : 第一章      ← check_server_stages: false
世界阶段 : 第一章      ← check_server_stages: true
```

名字来自 `desc` 那个本地化键，**一行显示完**；没写 `desc`、或 `desc` 的键不存在时，直接显示 `pattern` 原文。两个前缀由本模组提供（`%s` 会被替换成阶段名）：

| 键 | 中文 | English |
|---|---|---|
| `arcanumlib.ali.condition.has_stage.player` | 玩家阶段 : %s | Player stage : %s |
| `arcanumlib.ali.condition.has_stage.server` | 世界阶段 : %s | World stage : %s |

### 没装 AStages 时

条件**照常注册但恒不通过**（用到它的 json 仍能正常解析加载），首次使用时打一条 warn 日志。
