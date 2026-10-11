# Loot Injector Wiki

With json (or datagen) you can modify **any loot table**: add pools, add entries, change weights, replace entries, add or remove or modify conditions and functions —
without touching vanilla data and without fighting other mods over block/entity drop override rights: you only stack your changes onto the part you own.

- Package path: `com.chen1335.arcanumlib.modules.loot.lootInjector`
- Datapack directory: `data/<namespace>/loot_injecter/<injector id>.json`
- The namespace is **not limited** to `arcanumlib`: open your own `data/<your modid>/loot_injecter/` and you can write your own injectors, using the same set of types.

> **json syntax restrictions (important)**: loading goes through vanilla `SimpleJsonResourceReloadListener`, which internally calls
> `GsonHelper.fromJson(gson, reader, JsonElement.class, false)` — **strict mode**.
> So **`//` comments are not allowed and trailing commas are not allowed**; every example in this wiki is strictly valid json.
> (If you want comments, put them in an ordinary field such as `_comment`; unknown fields are ignored.)

---

## Table of Contents

1. [How It Runs](#1-how-it-runs)
2. [Nested Scope Model](#2-nested-scope-model)
3. [Execution Semantics (Must Read)](#3-execution-semantics-must-read)
4. [Top-Level Fields](#4-top-level-fields)
5. [Table Selectors (12)](#5-table-selectors-12)
6. [Table-Level Operators (7)](#6-table-level-operators-7)
7. [Pool Selectors (9)](#7-pool-selectors-9)
8. [Pool-Level Operators (12)](#8-pool-level-operators-12)
9. [Entry Selectors (15)](#9-entry-selectors-15)
10. [Entry-Level Operators (13)](#10-entry-level-operators-13)
11. [Function Selectors (9)](#11-function-selectors-9)
12. [Function-Level Operators (3)](#12-function-level-operators-3)
13. [Condition Selectors (8)](#13-condition-selectors-8)
14. [Condition-Level Operators (2)](#14-condition-level-operators-2)
15. [Custom Hooks (9 Events)](#15-custom-hooks-9-events)
16. [Extension Points for Mods](#16-extension-points-for-mods)
17. [Cookbook: Common Tasks](#17-cookbook-common-tasks)
18. [Writing Injectors with Datagen](#18-writing-injectors-with-datagen)
19. [Cheat Sheet](#19-cheat-sheet)
20. [Semantic Boundaries and Pitfalls](#20-semantic-boundaries-and-pitfalls)
21. [Loot Conditions Provided by This Mod](#21-loot-conditions-provided-by-this-mod)

---

## 1. How It Runs

**When it is applied**: once when the server has finished starting up, and once after every datapack reload (`/reload`); it sweeps through all injectors and applies them to the loot tables.

- It does **not** run on every roll, so even writing several hundred injectors will not affect the performance of drop resolution;
- Each injector is applied only once to **the same table** (deduplicated internally by injector id × table, and `/reload` simply starts over);
- The application order is decided by `priority` (smaller goes first); the parsing results of injectors that share the same `priority` come from an **unordered** Map, so **two injectors on the same table must not rely on their relative order when their priority is equal** — if you want determinism, give them different priorities.

The startup log contains a one-line summary you can use to confirm that everything ran (the numbers are examples):

```
Loot injection: 26 injectors, 1183 loot tables, 4521 applications in 36.5538 ms
```

The values are, in order: the number of successfully parsed injectors, the number of loot tables visited, and the number of "injector × matched table" applications. If this line appears, every injector was parsed and executed successfully.

> **A parse failure will not crash the game**: a single injector that fails to parse only writes one error log entry and is skipped; the remaining injectors keep working as usual.
> When debugging, first check whether the log contains `Failed to parse loot injector ...`.

---

## 2. Nested Scope Model

`ops` is a **tree**, not a flat list. The nesting level decides "what this item acts on":

```
Top-level ops                     → acts on the whole table (TableOperator)
├── add_pool                      (table-level leaf)
├── set_function / add_function / remove_function
├── table_custom
└── modify_pool  ← scope
    ├── poolSelector   selects which pools
    └── ops                          → acts on "each matched pool" (PoolOperator)
        ├── remove_pool
        ├── add_entry
        ├── set_condition / add_condition / remove_condition
        ├── set_function / add_function / remove_function
        ├── pool_custom
        ├── modify_function ← scope
        │   ├── functionSelector    selects which functions
        │   └── ops                      → acts on "each matched function" (FunctionOperator)
        │       ├── remove_function
        │       ├── replace_function
        │       └── modify_condition ← scope
        │           ├── conditionSelector  selects which of the function's own conditions
        │           └── ops                    → acts on "each matched condition" (ConditionOperator)
        └── modify_condition ← scope
            ├── conditionSelector   selects which of the pool's own conditions
            └── ops
        └── modify_entry ← scope
            ├── entrySelector       selects which entries
            └── ops                      → acts on "each matched entry" (EntryOperator)
                ├── remove_entry / replace_entry
                ├── set_weight / set_quality
                ├── set_condition / add_condition / remove_condition
                ├── set_function / add_function / remove_function
                ├── entry_custom
                ├── modify_function ← scope (same as above, but the target is this entry's function list)
                └── modify_condition ← scope (same as above, but the target is this entry's own conditions)
```

**Why nesting instead of a flat list**: selectors such as `poolCondition` / `entryCondition` are written only on **scope** items, and leaf ops no longer carry a selector — they naturally act only on "the current object". Therefore:

- It is impossible to put `remove_pool` on the wrong level (this type is simply not registered at table level, so parsing fails immediately);
- The same id (for example `set_function`) is registered once at each of the five levels — table / pool / entry / function / condition — and is **disambiguated by nesting level**;
- Order is encoded in the structure: outer levels come before inner levels, and within one level the array order applies.

---

## 3. Execution Semantics (Must Read)

### 3.1 Sequential Application

Ops in the same array execute in order, and a later op can see the changes made by an earlier one.

### 3.2 Snapshot Iteration + Cursor Stop

When a scope decides "which objects matched", it uses a **snapshot** (`List.copyOf`), so a child op deleting the current object will not make the iteration skip items. The rules are:

- **If the current object was deleted by an earlier child op, the remaining child ops stop immediately** (they will not keep modifying an object that has already been deleted);
- Deleting some other object does not affect the current round.

### 3.3 Condition "Ownership"

| What you want to change | Which level to write it on |
|---|---|
| The table's own `functions` | Table level (`LootTable` has **no** `conditions`, so there is no table-level `set_condition`) |
| The pool's own `conditions` / `functions` | Pool level |
| The entry's own `conditions` (every entry has them) | Entry level |
| The entry's own `functions` (only singleton entries have them; `alternatives`/`sequence`/`group` do not) | Entry level |
| The `conditions` **carried by a function itself** | Function-level `modify_condition` |

The last row is the most valuable layer of this structure: a function's own conditions (`LootItemConditionalFunction.predicates`) cannot be reached by any other way of writing things.

### 3.4 Composites Are Always Recomputed

When rolling/resolving, vanilla does not use the `conditions` / `functions` lists themselves, but the `compositeCondition` / `compositeFunction` / `compositePredicates` cached at construction time.
Every op that modifies a list **automatically recomputes the corresponding composite**, so you do not have to care; but if you write your own custom op, **you must recompute it yourself**, otherwise it will "look modified while actually having no effect".

---

## 4. Top-Level Fields

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `ops` | array | ✅ | — | List of table-level ops |
| `priority` | int | | `1000` | Smaller applies first |
| `tableSelector` | table selector | | `{"type":"arcanumlib:all"}` | Which tables this injector acts on |

```json
{
  "priority": 1000,
  "tableSelector": { "type": "arcanumlib:id", "id": "minecraft:chests/simple_dungeon" },
  "ops": []
}
```

---

## 5. Table Selectors (12)

Decides "whether this injector should act on a given table". Registry: `arcanumlib:table_selector`.

| id | Fields | Description |
|---|---|---|
| `all` | — | All tables |
| `id` | `id` | Exact match on the table id |
| `namespace` | `namespace` | Match on the namespace |
| `path_prefix` | `prefix` | Match on the path prefix |
| `entity_tag` | `tag` | Entity tag; the table id equals the default loot table of any entity in that tag |
| `block_tag` | `tag` | Block tag; the table id equals the table of any block in that tag |
| `entity_type` | `entity` | Entity type; the table id equals its default loot table (no tag expansion) |
| `block_type` | `block` | Block; the table id equals its loot table (no tag expansion; blocks without a loot table never match) |
| `not` | `condition` | Negation |
| `any_of` | `conditions` | Any of them matches |
| `all_of` | `conditions` | All of them match |
| `custom` | `custom_id` | The decision is handed to the listeners of `CustomTableSelectorEvent` |

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

## 6. Table-Level Operators (7)

Written inside the top-level `ops`. Registry: `arcanumlib:table_operator`.

| id | Fields | Description |
|---|---|---|
| `add_pool` | `pool` | **Append** a pool to the table (vanilla json pool format) |
| `modify_pool` | `poolSelector`, `ops` | Scope: run the child ops on each matched pool |
| `modify_function` | `functionSelector`, `ops` | Scope: run the child ops on the matched functions in the table's function list |
| `set_function` | `functions` | **Replace** the table's own function list |
| `add_function` | `functions` | **Append** to the table's own function list |
| `remove_function` | `functionTypes` | Remove the table's own functions by function type id |
| `table_custom` | `custom_id` | Handed to the listeners of `TableCustomOpEvent` |

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

## 7. Pool Selectors (9)

Written inside `modify_pool.poolSelector`. Registry: `arcanumlib:pool_selector`.

| id | Fields | Description |
|---|---|---|
| `any` | — | All pools (**default**) |
| `index` | `index` | Pool index (starting from 0) |
| `name` | `name` | Pool name; **unnamed pools never match** |
| `has_entry` | `entrySelector` | **Any** entry in the pool satisfies that entry selector |
| `total_weight` | `min`, `max` | The pool's total weight falls inside the closed interval |
| `not` | `condition` | Negation |
| `any_of` | `conditions` | Any of them matches |
| `all_of` | `conditions` | All of them match |
| `custom` | `custom_id` | Handed to the listeners of `CustomPoolSelectorEvent` |

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

**How the total weight is measured** (`arcanumlib:total_weight`, as well as the entry-level `weight_in_range` / `weight_share`):

- Only the **static base weight** counts: the `weight` field, equivalent to vanilla `getWeight(luck = 0)` (including the `max(..., 0)` clamp);
- **Excludes** `quality × luck` — selectors have no access to a `LootContext`;
- **Does not filter by entry conditions** — vanilla expands via `expand(context)` when rolling;
- Composite entries (`alternatives`/`sequence`/`group`) have no `weight` of their own, so their **subtrees are summed recursively**; for `alternatives`, vanilla only walks the first branch whose condition passes, whereas here every branch is counted, which makes it a **static upper bound**.

---

## 8. Pool-Level Operators (12)

Written inside `modify_pool.ops`. Registry: `arcanumlib:pool_operator`.

| id | Fields | Description |
|---|---|---|
| `remove_pool` | — | Delete **the current pool** |
| `add_entry` | `entry` | Append an entry to the current pool |
| `modify_entry` | `entrySelector`, `ops` | Scope: run the child ops on the matched entries in the current pool |
| `modify_function` | `functionSelector`, `ops` | Scope: on the current pool's function list |
| `modify_condition` | `conditionSelector`, `ops` | Scope: on the current pool's own conditions |
| `set_condition` | `conditions` | Replace the pool's own condition list |
| `add_condition` | `conditions` | Append to the pool's own condition list |
| `remove_condition` | `conditionTypes` | Remove the pool's own conditions by condition type id |
| `set_function` | `functions` | Replace the pool's own function list |
| `add_function` | `functions` | Append to the pool's own function list |
| `remove_function` | `functionTypes` | Remove the pool's own functions by function type id |
| `pool_custom` | `custom_id` | Handed to the listeners of `PoolCustomOpEvent` |

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

## 9. Entry Selectors (15)

Written inside `modify_entry.entrySelector`. Registry: `arcanumlib:entry_selector`.

| id | Fields | Description |
|---|---|---|
| `any` | — | All entries (**default**) |
| `index` | `index` | The entry's index inside the pool |
| `item` | `item` | Exact match on the item id (only meaningful for `minecraft:item` entries) |
| `tag` | `tag` | Matches **`minecraft:tag` entries**: the tag itself equals the given tag |
| `item_with_tag` | `tag` | Matches `minecraft:item` entries: their **item** belongs to the given tag (note the difference from `tag`) |
| `entry_type` | `entryType` | Entry type, such as `minecraft:item`, `minecraft:alternatives` |
| `weight_in_range` | `min`, `max` | The entry's weight (for a composite entry, its subtree's) falls inside the closed interval |
| `weight_share` | `min`, `max` | The entry's share of its pool's total weight falls inside the closed interval `[0, 1]` |
| `has_condition` | `conditionType` | The entry carries a condition of this type |
| `has_function` | `functionType` | The entry carries a function of this type |
| `has_child` | `entrySelector` | Some **direct child** of a composite entry matches (not recursive) |
| `not` | `condition` | Negation |
| `any_of` | `conditions` | Any of them matches |
| `all_of` | `conditions` | All of them match |
| `custom` | `custom_id` | Handed to the listeners of `CustomEntrySelectorEvent` |

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

## 10. Entry-Level Operators (13)

Written inside `modify_entry.ops`. Registry: `arcanumlib:entry_operator`.

| id | Fields | Description |
|---|---|---|
| `remove_entry` | — | Delete the current entry |
| `replace_entry` | `entry` | Replace the current entry with a new one (one independent instance per site) |
| `set_weight` | `weight` | Set the weight; **a negative number means "do not change"**; composite entries have no weight and are skipped |
| `set_quality` | `quality` | Set the quality; **a negative number means "do not change"**; composite entries have no quality and are skipped |
| `modify_function` | `functionSelector`, `ops` | Scope: on the current entry's function list |
| `modify_condition` | `conditionSelector`, `ops` | Scope: on the current entry's own conditions |
| `set_condition` | `conditions` | Replace the current entry's condition list |
| `add_condition` | `conditions` | Append to the current entry's condition list |
| `remove_condition` | `conditionTypes` | Remove the current entry's conditions by type id |
| `set_function` | `functions` | Replace the current entry's function list (composite entries are skipped) |
| `add_function` | `functions` | Append to the current entry's function list (composite entries are skipped) |
| `remove_function` | `functionTypes` | Remove the current entry's functions by type id (composite entries are skipped) |
| `entry_custom` | `custom_id` | Handed to the listeners of `EntryCustomOpEvent` |

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

## 11. Function Selectors (9)

Written inside `modify_function.functionSelector`. Registry: `arcanumlib:function_selector`.

| id | Fields | Description |
|---|---|---|
| `any` | — | All functions (**default**) |
| `index` | `index` | The function's index in the list |
| `function_type` | `functionType` | By function type id, such as `minecraft:set_count` |
| `has_condition` | `conditionType` | The function's **own** conditions contain this type |
| `not` | `selector` | Negation |
| `any_of` | `selectors` | Any of them matches |
| `all_of` | `selectors` | All of them match |
| `match` | `function`, `strict` | Match by the **keys of a template json** (see below) |
| `custom` | `custom_id` | Handed to the listeners of `CustomFunctionSelectorEvent` |

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

### About `match`

The semantics mirror vanilla's `DataComponentIngredient` component matching:

- The template keeps the **raw json**, so **only the keys you actually wrote are compared** — keys you did not write do not participate, and codec defaults never pollute the result (this is the key reason it is more useful than "deserialize into a function first, then compare");
- `strict` defaults to `false`: every key in the template must exist in the candidate with an equal value (subset matching);
- `strict: true`: requires an **exact match** with the candidate (compared by the equality of the serialized json);
- Arrays are treated as **ordered arguments**: the lengths must be equal and every element must match;
- Numbers are compared by value (`3` and `3.0` count as equal);
- The type key in the template (for functions `"function"`, for conditions `"condition"`) **must be present**, otherwise it never matches.

Typical use: **telling functions of the same type apart by their arguments**. For example, if an entry has both `set_count(2)` and `set_count(+3)`, `function_type` selects both of them, while `match` can pick out just one.

> Note: the `match` template is **not validated**. A misspelled key (for example `coun`) raises no error, it simply never matches.

---

## 12. Function-Level Operators (3)

Written inside `modify_function.ops`. Registry: `arcanumlib:function_operator`.

| id | Fields | Description |
|---|---|---|
| `remove_function` | — | Delete the current function |
| `replace_function` | `function` | Replace the current function with a new one (one independent instance per site) |
| `modify_condition` | `conditionSelector`, `ops` | Scope: descend into **the current function's own conditions**; non-conditional functions (such as `set_count`) have no conditions and are skipped |

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

> There is **no** generic implementation of "change an argument" (for example changing `count` from 5 to 10): each function's argument fields are private to that function.
> When you need it, combine `remove_function` + `add_function`, or write a `function_custom`-level custom op (see section 16).

---

## 13. Condition Selectors (8)

Written inside `modify_condition.conditionSelector`. Registry: `arcanumlib:condition_selector`.

| id | Fields | Description |
|---|---|---|
| `any` | — | All conditions (**default**) |
| `index` | `index` | The condition's index |
| `condition_type` | `conditionType` | By condition type id, such as `minecraft:random_chance` |
| `not` | `selector` | Negation |
| `any_of` | `selectors` | Any of them matches |
| `all_of` | `selectors` | All of them match |
| `match` | `condition`, `strict` | Match by the keys of a template json (same semantics as `match` on the function side) |
| `custom` | `custom_id` | Handed to the listeners of `CustomConditionSelectorEvent` |

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

## 14. Condition-Level Operators (2)

Written inside `modify_condition.ops`. Registry: `arcanumlib:condition_operator`.

| id | Fields | Description |
|---|---|---|
| `remove_condition` | — | Delete the current condition |
| `replace_condition` | `condition` | Replace the current condition with a new one (one independent instance per site) |

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

## 15. Custom Hooks (9 Events)

The first five are custom decisions for **selectors**, the next three are custom behaviours for **operators**, and the last one replaces a whole injector.
All of them are listened for on `NeoForge.EVENT_BUS`.

| Event | When it fires | Paired json |
|---|---|---|
| `CustomTableSelectorEvent` | When evaluating a table selector `custom` | `{"type":"arcanumlib:custom","custom_id":"..."}` |
| `CustomPoolSelectorEvent` | When evaluating a pool selector `custom` | Same as above (written inside `poolSelector`) |
| `CustomEntrySelectorEvent` | When evaluating an entry selector `custom` | Same as above (written inside `entrySelector`) |
| `CustomFunctionSelectorEvent` | When evaluating a function selector `custom` | Same as above (written inside `functionSelector`) |
| `CustomConditionSelectorEvent` | When evaluating a condition selector `custom` | Same as above (written inside `conditionSelector`) |
| `TableCustomOpEvent` | When executing `table_custom` | `{"type":"arcanumlib:table_custom","custom_id":"..."}` |
| `PoolCustomOpEvent` | When executing `pool_custom` | `{"type":"arcanumlib:pool_custom","custom_id":"..."}` |
| `EntryCustomOpEvent` | When executing `entry_custom` | `{"type":"arcanumlib:entry_custom","custom_id":"..."}` |
| `LootInjectorApplyEvent` | **Before** each injector is applied | — (the whole injector can be replaced, or the application can be cancelled) |

> **All 8 `custom` events carry `getRegistries()`** (not `LootInjectorApplyEvent`: it replaces a whole injector and has no use for it).
> Use it to read registries or do codec round-trips; it is the same `registries` that a custom selector's `test(...)` or a custom op's `apply(...)` receives:
>
> ```java
> RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, event.getRegistries());
> ```

```java
@EventBusSubscriber(modid = MyMod.MODID)
public final class MyLootHooks {

    /** Selector: a custom decision. Judge from whatever you are handed; you only need setResult. */
    @SubscribeEvent
    public static void onCustomEntry(CustomEntrySelectorEvent event) {
        if (!"my_entry_hook".equals(event.getId())) {
            return;
        }

        // Example: match only "singleton entries that carry no conditions at all"
        // need registries? use event.getRegistries() (all 8 custom events have it, see the note above)
        event.setResult(event.getEntry() instanceof LootPoolSingletonContainer
                && event.getEntry().conditions.isEmpty());
    }

    /** Operator: modify "the current entry" directly. entries is already a mutable list, so set / remove work. */
    @SubscribeEvent
    public static void onEntryCustom(EntryCustomOpEvent event) {
        if (!"my_entry_hook".equals(event.getId())) {
            return;
        }

        event.getEntries().set(event.getIndex(), LootItem.lootTableItem(Items.DIAMOND).build());
    }

    /** Operator: modify "the current pool". */
    @SubscribeEvent
    public static void onPoolCustom(PoolCustomOpEvent event) {
        if (!"my_pool_hook".equals(event.getId())) {
            return;
        }

        PoolOperator.mutableEntries(event.getPool()).add(LootItem.lootTableItem(Items.EMERALD).build());
    }

    /** Operator: modify the whole table. At this point you already know which injector is doing it. */
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

    /** Full replacement: you can reorganize an injector by table id / priority. */
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

## 16. Extension Points for Mods

The whole injector system has **10 custom registries**, each a container of `MapCodec`s, registered with the standard `DeferredRegister`:

| Registry key | What it holds |
|---|---|
| `arcanumlib:table_selector` | Table selectors |
| `arcanumlib:pool_selector` | Pool selectors |
| `arcanumlib:entry_selector` | Entry selectors |
| `arcanumlib:function_selector` | Function selectors |
| `arcanumlib:condition_selector` | Condition selectors |
| `arcanumlib:table_operator` | Table-level ops |
| `arcanumlib:pool_operator` | Pool-level ops |
| `arcanumlib:entry_operator` | Entry-level ops |
| `arcanumlib:function_operator` | Function-level ops |
| `arcanumlib:condition_operator` | Condition-level ops |

```java
public final class MyOps {
    // Take "entry selectors" as an example: register a new id
    public static final DeferredRegister<EntrySelectorType> ENTRY_SELECTORS =
            DeferredRegister.create(LootInjectorOps.ENTRY_SELECTOR_KEY, MyMod.MODID);

    public static final DeferredHolder<EntrySelectorType, EntrySelectorType> IS_SHINY =
            ENTRY_SELECTORS.register("is_shiny", () -> new EntrySelectorType(IsShiny.CODEC));

    public static void register(IEventBus bus) {
        ENTRY_SELECTORS.register(bus);
    }
}

/** All you implement is a test; the 5 selectors' test and the 5 ops' apply all receive registries (for registry lookups / codec round-trips). */
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

**Three hard requirements when implementing a custom op**:

1. **After modifying a list you must recompute the composite** — matching/resolution uses `compositeCondition` / `compositeFunction` / `compositePredicates`:
   - Entry/pool conditions: `compositeCondition = Util.allOf(conditions)`
   - Entry/pool/table functions: `compositeFunction = LootItemFunctions.compose(functions)`
   - **Conditions carried by a function**: `compositePredicates = Util.allOf(predicates)` (the field lives on `LootItemConditionalFunction`)
   - The composite condition itself: `composedPredicate` (the field lives on `CompositeLootItemCondition`)
2. **Turn the list into a mutable one before adding or removing** (`new ArrayList<>(...)`): the list comes from a codec and may be an immutable implementation.
   At pool level there is a ready-made `PoolOperator.mutableEntries(pool)`.
3. **Do not modify lists inside a recursive traversal**: scope ops already iterate over a snapshot, so the implementation side only needs to handle "the current object".

> All the vanilla fields that need to be read or written are opened in `META-INF/accesstransformer.cfg` (`LootTable.pools`/`functions`,
> `LootPool.entries`/`conditions`/`functions`, and so on); if your own op needs other fields, **check whether an AT is already open** for them.

---

## 17. Cookbook: Common Tasks

### 17.1 Add an "Extra Drop Pool" to a Table

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

### 17.2 Replace "String" with "Leather" in a Table

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

### 17.3 Delete All Entries Whose "Weight Share Is Below 1%"

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

### 17.4 Remove All "Looting Bonuses"

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

### 17.5 Replace Only the `set_count` with "One Specific Argument"

`set_count(2)` and `set_count(+3)` have the same type, so `function_type` selects both of them; only `match` can pick out one:

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

### 17.6 Remove "the Always-False Condition Carried by a Function" (No Other Way Can Do It)

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

### 17.7 Make "a Certain Pool" Drop for Sure (Remove the Pool's Own Chance Gate)

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

## 18. Writing Injectors with Datagen

If you would rather not hand-write json, use datagen and enjoy compile-time checking and codec round-trip verification along the way:

```java
public final class MyLootInjectors extends LootInjectorTemplate {

    public MyLootInjectors(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, "My Mod Loot Injectors");
    }

    /** The namespace of the injector id; the file ends up at data/<ns>/loot_injecter/<path>.json. */
    @Override
    protected ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("mymod", path);
    }

    /** The chance condition used by the template skeleton; both the base chance and the per-level looting bonus live in Drop. */
    @Override
    protected LootItemCondition.Builder chanceCondition(Drop drop) {
        return LootItemRandomChanceCondition.randomChance((float) drop.chance());
    }

    @Override
    protected List<LootInjector> injectors(HolderLookup.Provider registries) {
        // Convenience skeletons provided by the template: by table id / by entity / any table selector, fed a list of Drops directly
        return List.of(
                merged("dungeon_extra", BuiltInLootTables.SIMPLE_DUNGEON,
                        new Drop(0.25D, 0.02D, () -> LootItem.lootTableItem(Items.GOLD_INGOT)
                                .apply(lootingCount(registries))),
                        new Drop(0.05D, () -> LootItem.lootTableItem(Items.EMERALD))),

                // When you need nested scopes, build the op tree directly
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

Then hook it into `GatherDataEvent`:

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

> **Enchantment reference pitfall**: functions such as `enchanted_count_increase` carry a `Holder<Enchantment>`, which cannot be looked up in the enchantment registry during the datagen stage,
> so injectors using it can only be **hand-written as json** (or use a reference that is already present in `event.getLookupProvider()`).

---

## 19. Cheat Sheet

### All Ops (37)

| Layer | id |
|---|---|
| Table (7) | `add_pool`, `modify_pool`, `modify_function`, `set_function`, `add_function`, `remove_function`, `table_custom` |
| Pool (12) | `remove_pool`, `add_entry`, `modify_entry`, `modify_function`, `modify_condition`, `set_condition`, `add_condition`, `remove_condition`, `set_function`, `add_function`, `remove_function`, `pool_custom` |
| Entry (13) | `remove_entry`, `replace_entry`, `set_weight`, `set_quality`, `modify_function`, `modify_condition`, `set_condition`, `add_condition`, `remove_condition`, `set_function`, `add_function`, `remove_function`, `entry_custom` |
| Function (3) | `remove_function`, `replace_function`, `modify_condition` |
| Condition (2) | `remove_condition`, `replace_condition` |

### All Selectors (53)

| Family | id | Optional fields |
|---|---|---|
| Table (12) | `all` / `id` / `namespace` / `path_prefix` / `entity_tag` / `block_tag` / `entity_type` / `block_type` / `not` / `any_of` / `all_of` / `custom` | `id`, `namespace`, `prefix`, `tag`, `entity`, `block`, `condition`, `conditions`, `custom_id` |
| Pool (9) | `any` / `index` / `name` / `has_entry` / `total_weight` / `not` / `any_of` / `all_of` / `custom` | `index`, `name`, `entrySelector`, `min`, `max`, `condition`, `conditions`, `custom_id` |
| Entry (15) | `any` / `index` / `item` / `tag` / `item_with_tag` / `entry_type` / `weight_in_range` / `weight_share` / `has_condition` / `has_function` / `has_child` / `not` / `any_of` / `all_of` / `custom` | `index`, `item`, `tag`, `entryType`, `min`, `max`, `conditionType`, `functionType`, `entrySelector`, `condition`, `conditions`, `custom_id` |
| Function (9) | `any` / `index` / `function_type` / `has_condition` / `not` / `any_of` / `all_of` / `match` / `custom` | `index`, `functionType`, `conditionType`, `selector`, `selectors`, `function`, `strict`, `custom_id` |
| Condition (8) | `any` / `index` / `condition_type` / `not` / `any_of` / `all_of` / `match` / `custom` | `index`, `conditionType`, `selector`, `selectors`, `condition`, `strict`, `custom_id` |

> Note that two field names are inconsistent (for historical reasons; **the actual field names are what counts**):
> `not` on table/pool/entry selectors uses the `condition` field, while `not` on function/condition selectors uses `selector`;
> `any_of`/`all_of` use `conditions` on the table/pool/entry side and `selectors` on the function/condition side.

### The Same id Appears in Several Families / Layers (This Is Normal)

Every family has its own registry, and the same id is **registered once in each of those registries**; json disambiguates by **the field it sits in / the level it sits at**:

| Duplicated id | Appears in |
|---|---|
| `any`, `index`, `not`, `any_of`, `all_of`, `custom` | **One copy in each** of the five selector families |
| `set_function`, `add_function`, `remove_function` | Table / Pool / Entry / Function / Condition — they act on **their own container's** function list |
| `set_condition`, `add_condition`, `remove_condition` | Pool / Entry / Condition |
| `remove_function`, `replace_function` | They can also appear at **function level**, but there they act on "the current function" itself rather than on some container |

So `{"type":"arcanumlib:custom","custom_id":"x"}` written inside `tableSelector` is a table selector, and written inside `functionSelector` it is a function selector —
**`type` does not need a family-name prefix**. Dispatch is done by `(registry, id)`, and the registry is decided by the field; writing it in the wrong place fails at parse time with "this level has no such type", and never silently takes a wrong branch.

> The only ones that "carry a prefix" are the three customs on the **operator side**: `table_custom` / `pool_custom` / `entry_custom`.
> Each of them is the only custom entry point at its level, and the prefix makes them easier to read; selector-side `custom`, by contrast, follows the style of "generic ids carry no prefix".

### Type Keys for Functions and Conditions in Vanilla json

- Functions: `"function": "minecraft:set_count"` (the dispatch field of `LootItemFunctions.ROOT_CODEC`)
- Conditions: `"condition": "minecraft:random_chance"` (the dispatch field of `LootItemCondition.DIRECT_CODEC`)

These two ids are exactly what you write in `remove_function.functionTypes` / `remove_condition.conditionTypes`.

---

## 20. Semantic Boundaries and Pitfalls

1. **Strict json parsing**: `//` comments and trailing commas both fail to parse (see the top of this document).
2. **Weights use a static measure**: `total_weight` / `weight_in_range` / `weight_share` exclude `quality × luck`,
   do not filter by entry conditions, and `alternatives` sums every branch. **Do not use them to reproduce the exact probability of a given roll**;
   they are suited to coarse screening such as "pick the small pools / pick the rare entries".
3. **Unnamed pools never match `name`**: most vanilla pools have no name.
4. **`tag` and `item_with_tag` are not the same thing**: the former matches **`minecraft:tag` entries**, the latter matches
   `minecraft:item` entries and checks whether their item belongs to the tag. Vanilla tables are almost entirely the latter.
5. **Composite entries have no `weight` / `quality` / `functions`**: `set_weight`, `set_quality`,
   `set_*function` and `remove_function` skip `alternatives`/`sequence`/`group`;
   `conditions`, on the other hand, exist on every entry.
6. **When rolling, `alternatives` only walks the first branch whose condition passes**: so when you add conditions to
   its children, or judge with `has_child`, you must be aware that only one branch is actually effective at runtime.
7. **Order is undefined for equal priorities**: please give multiple injectors on the same table different priorities.
8. **`match` templates are not validated**: a misspelled key fails silently.
9. **`replace_*` performs a deep copy**: every replacement site re-decodes its own independent instance, so later changes never bleed into each other;
   the cost is one codec round trip under `RegistryOps` (which only happens during the apply stage, not while rolling).
10. **A negative number in `set_weight` / `set_quality` means "do not change"**: this keeps the legacy semantics, so old json can be moved over directly;
    if you really want to set 0, write 0.
11. **`LootTable` has no `conditions`**: so there is no table-level `set_condition` / `add_condition` / `remove_condition`.
12. **Nothing in vanilla ever calls `LootTable.freeze()`**: but we modify AT-opened fields directly, which bypasses its
    `checkFrozen()` guard. If anything ever freezes a shared table in the future, injection will break its immutability assumption.

---

## 21. Loot Conditions Provided by This Mod

The `conditions` / `add_condition` / `set_condition` inside injectors accept **ordinary vanilla condition json**,
so the loot conditions that ArcanumLib provides itself can be written there directly as well. They are all registered under `minecraft:loot_condition_type`,
and **the entries are as follows** (more will be added later; every new one gets a row in this table):

| Condition id | One-line description | Detailed usage |
|---|---|---|
| `arcanumlib:chance_modifiers` | Chance modifier chain: starts from `base_chance`, passes through a list of modifiers in order, then rolls once against the final chance | [Loot Conditions Guide](Loot-Conditions.md) |
| `arcanumlib:has_stage` | Stage condition: with AStages installed, gates on player / world stages | [Loot Conditions Guide](Loot-Conditions.md) |

Example usage (attached to some entry):

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

To test "does this entry carry this condition?", use the entry selector
`{"type":"arcanumlib:has_condition","conditionType":"arcanumlib:chance_modifiers"}`.

---
## Appendix: Related Source Locations

| Content | File |
|---|---|
| The per-level op interfaces and implementations | `modules/loot/lootInjector/operator/{Table,Pool,Entry,Function,Condition}Operator.java` |
| The five selector families | `modules/loot/lootInjector/selector/{Table,Pool,Entry,Function,Condition}Selector.java` |
| Weight measure | `modules/loot/lootInjector/selector/PoolWeights.java` |
| The subset comparison of `match` | `modules/loot/lootInjector/selector/JsonSubset.java` |
| Registries and all registrations | `modules/loot/lootInjector/LootInjectorOps.java` |
| The injector itself and application | `modules/loot/lootInjector/{LootInjector,LootInjectorApplier,LootInjectorReloadListener}.java` |
| Events | `modules/loot/lootInjector/event/` |
| Datagen base class and template | `modules/loot/lootInjector/data/{LootInjectorProvider,LootInjectorTemplate}.java` |
| Access transformer | `src/main/resources/META-INF/accesstransformer.cfg` |
| Official test data (all 37 ops and 52 of the 53 selectors have a json example; the `tableSelector` default `all` never shows up in generated output because it is the default) | `src/generated/resources/data/arcanumlib/loot_injecter/` |
