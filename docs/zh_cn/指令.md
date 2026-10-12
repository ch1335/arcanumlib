# 指令

本模组目前提供的指令都挂在 `/arcanumlib` 下，需要**权限等级 2**（管理员）。

## 目录

1. [导出战利品表](#1-导出战利品表)
2. [模拟战利品表](#2-模拟战利品表)

---

## 1. 导出战利品表

```
/arcanumlib export lootTable
```

把战利品表导出成 json，方便对照、做 diff 或写文档。输出到**游戏目录**下（开发环境就是 `run/`，路径取自 NeoForge 的运行时目录，不是相对工作目录拼接）：

| 目录 | 内容 | 结构 |
|---|---|---|
| `arcanumlib/exportedLootTable/modified/` | 执行指令那一刻**内存里**的表，也就是注入器与其它模组已经作用过的结果 | `<命名空间>/<路径>.json` |
| `arcanumlib/exportedLootTable/original/` | **原始文件逐字节复制**，不含任何运行时改动 | `<命名空间>/loot_table/<路径>.json` |

### 说明

- `modified/` **只包含被注入器改过**的表（按已应用记录过滤），不是全量；
- `original/` 是**全量**复制数据包里的原始文件，所以两个目录的文件数通常**不相等**；
- 建议在服务器/世界**启动完成之后**执行（注入器在这个时机应用），`/reload` 之后再执行同样有效；
- 执行完会在聊天栏与日志里各给一条汇总。

## 2. 模拟战利品表

```
/arcanumlib loot_simulation <lootTable> [次数] [参数集]
```

反复生成同一张战利品表若干次（省略次数时默认 1000，上限 100000），再按物品类型汇总：

| 列 | 含义 |
|---|---|
| 物品 | 物品名，每行用不同颜色区分 |
| 数量 | 所有轮次累计得到的个数 |
| 比例 | 数量 ÷ 总物品数 |
| 概率 | 数量 ÷ 尝试次数 |

### 上下文怎么填

按**参数集类型**决定填谁（省略参数集时用表自带的那个）：

| 类型 | ORIGIN | THIS_ENTITY | 其它 |
|---|---|---|---|
| 方块类 | 玩家**看向的方块** | 玩家 | `ATTACKING_ENTITY` = 玩家；`BLOCK_STATE` / `BLOCK_ENTITY` 用该方块 |
| 实体类 | 看向**实体**的位置 | 玩家看向的实体 | `DAMAGE_SOURCE` = 玩家攻击；`ATTACKING_ENTITY` / `DIRECT_ATTACKING_ENTITY` / `LAST_DAMAGE_PLAYER` = 玩家 |
| 通用类 | 看向实体优先，其次看向的方块，最后玩家脚下 | 实体时为该实体，否则玩家 | 同上 |
| 其余 | 玩家位置 | 玩家 | 攻击者等归玩家 |

### 说明

- 方块类 = `chest` / `block` / `block_use` / `hit_block` / `advancement_location`；实体类 = `entity` / `enchanted_damage`；通用类 = `generic`；
- 参数集可选值共 **23** 个：`empty`、`chest`、`command`、`selector`、`fishing`、`entity`、`equipment`、`archaeology`、`gift`、`barter`、`vault`、`advancement_reward`、`advancement_entity`、`advancement_location`、`block_use`、`generic`、`block`、`shearing`、`enchanted_damage`、`enchanted_item`、`enchanted_location`、`enchanted_entity`、`hit_block`（均属 `minecraft:`）；
- 只会填该参数集**允许**的参数，所以不会因为"这个参数集不接受该参数"而失败；
- 表需要的必填参数仍然缺时会报错并说明原因；
- `<lootTable>` 支持 Tab 补全（带不带命名空间都能补出来）；
- 每轮使用新的随机种子，避免复刻同一份结果。
