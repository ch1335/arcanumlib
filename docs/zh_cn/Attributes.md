# 本模组提供的属性

ArcanumLib 提供 3 条**玩家属性**，默认基础值都是 `0`——也就是**默认完全不生效**，
要用属性修饰符、`/attribute` 命令或物品属性去加值。

## 一览

| 属性 id | 名称 | 类型 | 基础值 | 范围 | 效果 |
|---|---|---|---|---|---|
| `arcanumlib:seeking` | 寻觅 | 百分比 | 0 | 0 ~ 100 | 提高**稀有生物**的刷怪权重 |
| `arcanumlib:fortune` | 时运 | 数值 | 0 | 0 ~ 100 | 加在**时运**附魔的等效等级上 |
| `arcanumlib:looting` | 抢夺 | 数值 | 0 | 0 ~ 100 | 加在**抢夺**附魔的等效等级上 |

- 三条都挂在 `minecraft:player` 上，且都是**同步属性**（客户端能读到同一个值）；
- 范围下限都是 0，所以**只能加、不能减**。

---

## 寻觅（`arcanumlib:seeking`）

刷怪时，把**稀有**生物的权重乘以 `1 + 寻觅值`。

- **什么算稀有**：该生物在整张刷怪表里的权重占比 < 阈值（默认 **2%**，服务端配置）。
  占比按「权重 × 平均每次刷新的数量」折算，免得"一次刷 4 只"的怪被低估。
- **取哪个玩家的值**：**128 格内**所有玩家里**最高**的那个（128 格就是原版刷怪距离）。
- **加成比例不封顶**：公式本身不设上限，只受属性范围 `0~100` 夹取——也就是最多 ×101。
- **怎么关掉**：把配置里的阈值设成 `0`。

> **它是整数权重**：加成量是 `四舍五入(基础权重 × 寻觅值)`。
> 所以基础权重很低的生物（例如权重 1）要寻觅值到 `0.5` 以上才看得出变化——不足 `0.5` 会被抹成 0，等于没效果。
> 权重越大的生物越平滑。

---

## 时运 / 抢夺（`arcanumlib:fortune` / `arcanumlib:looting`）

把属性值取整后**直接加在附魔等级上**：

| 属性 | 影响的原版掉落机制 |
|---|---|
| `arcanumlib:fortune` | `apply_bonus`（时运加掉落数量）、`table_bonus`（按等级查概率表） |
| `arcanumlib:looting` | `enchanted_count_increase`（加数量）、`random_chance_with_enchanted_bonus`（加概率） |

取哪个实体：**实体掉落**取击杀者（`ATTACKING_ENTITY`），**方块掉落**取挖矿的玩家（`THIS_ENTITY`）。

这两条属性也顺带影响本模组自己的[概率条件](Loot-Conditions.md)（`arcanumlib:chance_modifiers` 里的
`tool_enchantment_level` 加工器）——因为走的是同一个事件。

> **注意语义**：这是**直接加等级、门槛为 0**。所以「属性 3 + 一把没附魔的镐子」等价于「时运 3」。
> 如果你的需求是"只放大已有的附魔"（没附魔就不生效），需要加一行判断，目前**不是**这个语义。

---

## 怎么加值

### 命令（调试用）

```
/attribute @s arcanumlib:seeking base set 2
/attribute @s arcanumlib:fortune base set 3
/attribute @s arcanumlib:fortune modifier add mymod:fortune_bonus 3 add_value
```

### 物品属性（物品 json 里的组件）

```json
{
  "minecraft:attribute_modifiers": [
    {
      "type": "arcanumlib:fortune",
      "id": "mymod:fortune_bonus",
      "amount": 2.0,
      "operation": "add_value",
      "slot": "mainhand"
    }
  ]
}
```

`slot` 可省略（默认 `any`）；`operation` 取 `add_value` / `add_multiplied_base` / `add_multiplied_total`。

### 代码

```java
AttributeInstance instance = player.getAttribute(ArcanumAttributes.FORTUNE);
instance.addOrUpdateTransientModifier(new AttributeModifier(
        ResourceLocation.fromNamespaceAndPath("mymod", "fortune_bonus"),
        3.0D,
        AttributeModifier.Operation.ADD_VALUE));
```

---

## 配置

服务端配置 `serverconfig/arcanumlib/attribute.toml`（单人存档在 `saves/<存档>/serverconfig/` 下）：

| 键 | 默认 | 说明 |
|---|---|---|
| `spawn.rare_spawn_weight_threshold` | `0.02` | 稀有判定阈值（权重占比，0~1）；设 `0` 等于关掉寻觅 |

---

## 语言键

| 键 | 中文 | 英文 |
|---|---|---|
| `attribute.name.arcanumlib.seeking` | 寻觅 | Seeking |
| `attribute.name.arcanumlib.fortune` | 时运 | Fortune |
| `attribute.name.arcanumlib.looting` | 抢夺 | Looting |

另有对应的 `….desc` 三条（简短说明，供显示名称的工具使用）。

---

## 相关源码

| 内容 | 文件 |
|---|---|
| 属性定义 | `modules/attribute/ArcanumAttributes.java` |
| 挂到玩家 | `modules/attribute/AttributeEvents.java` |
| 寻觅的刷怪逻辑 | `modules/attribute/SpawnEvents.java` |
| 时运 / 抢夺的等级加成 | `modules/attribute/EnchantedLootEvents.java` |
| 服务端配置 | `modules/attribute/AttributeConfig.java` |
| 附魔等级事件（时运/抢夺与概率条件共用） | `events/EnchantedLootEvent.java` |
