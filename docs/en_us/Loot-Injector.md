# Loot Injector

With json (or datagen) you can modify **any loot table**: add pools, add entries, change weights, replace entries, add or remove or modify conditions and functions —
without touching vanilla data and without fighting other mods over block/entity drop override rights: you only stack your changes onto the part you own.

- Package path: `com.chen1335.arcanumlib.modules.loot.lootInjector`
- Datapack directory: `data/<namespace>/loot_injecter/<injector id>.json`
- The namespace is **not limited** to `arcanumlib`: open your own `data/<your modid>/loot_injecter/` and you can write your own injectors, using the same set of types.

---

## Table of Contents

1. [How It Runs](#1-how-it-runs)
2. [Nested Scope Model](#2-nested-scope-model)
3. [Execution Semantics (Must Read)](#3-execution-semantics-must-read)
4. [Top-Level Fields](#4-top-level-fields)
5. [Table Selectors (13)](#5-table-selectors-13)
6. [Table-Level Operators (7)](#6-table-level-operators-7)
7. [Pool Selectors (9)](#7-pool-selectors-9)
8. [Pool-Level Operators (12)](#8-pool-level-operators-12)
9. [Entry Selectors (15)](#9-entry-selectors-15)
10. [Entry-Level Operators (17)](#10-entry-level-operators-17)
11. [Function Selectors (9)](#11-function-selectors-9)
12. [Function-Level Operators (3)](#12-function-level-operators-3)
13. [Condition Selectors (8)](#13-condition-selectors-8)
14. [Condition-Level Operators (2)](#14-condition-level-operators-2)
15. [Custom Hooks (9 Events)](#15-custom-hooks-9-events)
16. [Extension Points for Mods](#16-extension-points-for-mods)
17. [Recipes: Common Tasks](#17-recipes-common-tasks)
18. [Writing Injectors with Datagen](#18-writing-injectors-with-datagen)
19. [Semantic Boundaries and Pitfalls](#19-semantic-boundaries-and-pitfalls)
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
  "tableSelector": {
    "type": "arcanumlib:id",
    "id": "minecraft:chests/simple_dungeon"
  },
  "ops": []
}
```

---

## 5. Table Selectors (13)

Decides "whether this injector should act on a given table". Registry: `arcanumlib:table_selector`.

**Entries**:

- [5.1 `all`](#51-all--all-tables) — all tables
- [5.2 `id`](#52-id--exact-table-id) — exact table id
- [5.3 `namespace`](#53-namespace--namespace-match) — namespace match
- [5.4 `path_prefix`](#54-path_prefix--path-prefix-match) — path prefix match
- [5.5 `entity_tag`](#55-entity_tag--entity-tag) — entity tag
- [5.6 `block_tag`](#56-block_tag--block-tag) — block tag
- [5.7 `entity_type`](#57-entity_type--entity-type) — entity type
- [5.8 `block_type`](#58-block_type--block) — block
- [5.9 `not`](#59-not--negation) — negation
- [5.10 `any_of`](#510-any_of--any-of) — any of
- [5.11 `all_of`](#511-all_of--all-of) — all of
- [5.12 `has_pool`](#512-has_pool--any-pool-satisfies-the-given-pool-selector) — any pool satisfies the given pool selector
- [5.13 `custom`](#513-custom--custom-decided-by-customtableselectorevent) — custom (decided by `CustomTableSelectorEvent`)

#### 5.1 `all` — all tables

- No parameters.

```json
{
  "type": "arcanumlib:all"
}
```

#### 5.2 `id` — exact table id

- `*id`: <ResourceLocation> the target loot table, e.g. `minecraft:entities/zombie`.

```json
{
  "type": "arcanumlib:id",
  "id": "minecraft:entities/zombie"
}
```

#### 5.3 `namespace` — namespace match

- `*namespace`: <String> the namespace, e.g. `minecraft`.

```json
{
  "type": "arcanumlib:namespace",
  "namespace": "minecraft"
}
```

#### 5.4 `path_prefix` — path prefix match

- `*prefix`: <String> the path prefix, e.g. `entities/`.

```json
{
  "type": "arcanumlib:path_prefix",
  "prefix": "entities/"
}
```

#### 5.5 `entity_tag` — entity tag

- `*tag`: <TagKey<EntityType<?>>> an entity tag, e.g. `minecraft:zombies`; the table id equals the default loot table of any entity in it.

```json
{
  "type": "arcanumlib:entity_tag",
  "tag": "minecraft:zombies"
}
```

#### 5.6 `block_tag` — block tag

- `*tag`: <TagKey<Block>> a block tag, e.g. `minecraft:logs`; the table id equals the table of any block in it.

```json
{
  "type": "arcanumlib:block_tag",
  "tag": "minecraft:logs"
}
```

#### 5.7 `entity_type` — entity type

- `*entity`: <EntityType<?>> an entity type, e.g. `minecraft:zombie`; tags are not expanded.

```json
{
  "type": "arcanumlib:entity_type",
  "entity": "minecraft:zombie"
}
```

#### 5.8 `block_type` — block

- `*block`: <Block> a block, e.g. `minecraft:diamond_ore`; tags are not expanded, and blocks without a loot table never match.

```json
{
  "type": "arcanumlib:block_type",
  "block": "minecraft:diamond_ore"
}
```

#### 5.9 `not` — negation

- `*condition`: <TableSelector> the child selector to negate, see [5. Table Selectors](#5-table-selectors-13).

```json
{
  "type": "arcanumlib:not",
  "condition": {
    "type": "arcanumlib:id",
    "id": "minecraft:entities/zombie"
  }
}
```

#### 5.10 `any_of` — any of

- `*conditions`: <json array> a list of child selectors (each a <TableSelector>); matches when any of them matches.

```json
{
  "type": "arcanumlib:any_of",
  "conditions": [
    {
      "type": "arcanumlib:namespace",
      "namespace": "minecraft"
    },
    {
      "type": "arcanumlib:path_prefix",
      "prefix": "chests/"
    }
  ]
}
```

#### 5.11 `all_of` — all of

- `*conditions`: <json array> a list of child selectors (each a <TableSelector>); matches only when all of them match.

```json
{
  "type": "arcanumlib:all_of",
  "conditions": [
    {
      "type": "arcanumlib:id",
      "id": "minecraft:entities/zombie"
    },
    {
      "type": "arcanumlib:has_pool",
      "pool": {
        "type": "arcanumlib:index",
        "index": 0
      }
    }
  ]
}
```

#### 5.12 `has_pool` — any pool satisfies the given pool selector

- `*pool`: <PoolSelector> the pool selector applied to each pool of the table, see [7. Pool Selectors](#7-pool-selectors-9).

```json
{
  "type": "arcanumlib:has_pool",
  "pool": {
    "type": "arcanumlib:index",
    "index": 0
  }
}
```

The `pool` field is a **pool selector**.

#### 5.13 `custom` — custom (decided by `CustomTableSelectorEvent`)

- `*custom_id`: <String> the custom id handed to `CustomTableSelectorEvent` listeners.
- `data`: <CompoundTag> **optional** extra data; either an SNBT string (e.g. `"{team: \"red\"}"`) or a plain json object; when omitted it is an **empty tag** (never null) and listeners read it via `event.getData()`.

```json
{
  "type": "arcanumlib:custom",
  "custom_id": "mymod:my_check"
}
```

---

## 6. Table-Level Operators (7)

Written inside the top-level `ops`. Registry: `arcanumlib:table_operator`.

**Entries**:

- [6.1 `add_pool`](#61-add_pool--append-a-pool-to-the-table-vanilla-json-pool-format) — **Append** a pool to the table (vanilla json pool format)
- [6.2 `modify_pool`](#62-modify_pool--scope-apply-the-child-ops-on-each-matched-pool) — Scope: apply the child ops on each matched pool
- [6.3 `modify_function`](#63-modify_function--scope-apply-the-child-ops-on-the-matched-functions-in-the-tables-function-list) — Scope: apply the child ops on the matched functions in the table's function list
- [6.4 `set_function`](#64-set_function--replace-the-tables-own-function-list) — **Replace** the table's own function list
- [6.5 `add_function`](#65-add_function--append-to-the-tables-own-function-list) — **Append** to the table's own function list
- [6.6 `remove_function`](#66-remove_function--remove-the-tables-own-functions-by-function-type-id) — Remove the table's own functions by function type id
- [6.7 `table_custom`](#67-table_custom--handed-to-the-listeners-of-tablecustomopevent) — Handed to the listeners of `TableCustomOpEvent`

#### 6.1 `add_pool` — **Append** a pool to the table (vanilla json pool format)

- `*pool`: <json object> a vanilla loot pool (`rolls` / `bonus_rolls` / `entries` ...), appended to the table as a whole pool.

```json
{
  "type": "arcanumlib:add_pool",
  "pool": {
    "rolls": 1.0,
    "bonus_rolls": 0.0,
    "name": "my_extra_pool",
    "entries": [
      {
        "type": "minecraft:item",
        "name": "minecraft:diamond",
        "weight": 1,
        "functions": []
      }
    ]
  }
}
```

#### 6.2 `modify_pool` — Scope: apply the child ops on each matched pool

- `*poolSelector`: <PoolSelector> selects the pools to modify, see [7. Pool Selectors](#7-pool-selectors-9).
- `*ops`: <json array> a list of pool operators (each a `PoolOperator`), see [8. Pool-Level Operators](#8-pool-level-operators-12).

```json
{
  "type": "arcanumlib:modify_pool",
  "poolSelector": {
    "type": "arcanumlib:name",
    "name": "my_extra_pool"
  },
  "ops": [
    {
      "type": "arcanumlib:remove_pool"
    },
    {
      "type": "arcanumlib:add_entry",
      "entry": {
        "type": "minecraft:item",
        "name": "minecraft:emerald"
      }
    }
  ]
}
```

#### 6.3 `modify_function` — Scope: apply the child ops on the matched functions in the table's function list

- `*functionSelector`: <FunctionSelector> selects the functions to modify, see [11. Function Selectors](#11-function-selectors-9).
- `*ops`: <json array> a list of function operators (each a `FunctionOperator`), see [12. Function-Level Operators](#12-function-level-operators-3).

```json
{
  "type": "arcanumlib:modify_function",
  "functionSelector": {
    "type": "arcanumlib:function_type",
    "functionType": "minecraft:set_count"
  },
  "ops": [
    {
      "type": "arcanumlib:replace_function",
      "function": {
        "function": "minecraft:set_count",
        "count": 3.0
      }
    }
  ]
}
```

#### 6.4 `set_function` — **Replace** the table's own function list

- `*functions`: <json array> a list of vanilla item modifiers (such as `minecraft:set_count`); **replaces** the table list.

```json
{
  "type": "arcanumlib:set_function",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 2.0,
      "add": true
    }
  ]
}
```

#### 6.5 `add_function` — **Append** to the table's own function list

- `*functions`: <json array> a list of vanilla item modifiers (such as `minecraft:set_count`); **appends** to the table list.

```json
{
  "type": "arcanumlib:add_function",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 1.0,
      "add": true
    }
  ]
}
```

#### 6.6 `remove_function` — Remove the table's own functions by function type id

- `*functionTypes`: <json array> vanilla function type ids to strip, e.g. `["minecraft:set_count"]`.

```json
{
  "type": "arcanumlib:remove_function",
  "functionTypes": [
    "minecraft:set_count"
  ]
}
```

#### 6.7 `table_custom` — Handed to the listeners of `TableCustomOpEvent`

- `*custom_id`: <String> the id handed to `TableCustomOpEvent` listeners.
- `data`: <CompoundTag> **optional** extra data; either an SNBT string (e.g. `"{times: 3}"`) or a plain json object; when omitted it is an **empty tag** (never null) and listeners read it via `event.getData()`.

```json
{
  "type": "arcanumlib:table_custom",
  "custom_id": "my_table_hook"
}
```

---

## 7. Pool Selectors (9)

Written inside `modify_pool.poolSelector`. Registry: `arcanumlib:pool_selector`.

**Entries**:

- [7.1 `any`](#71-any--all-pools-default) — All pools (**default**)
- [7.2 `index`](#72-index--pool-index-starting-from-0) — Pool index (starting from 0)
- [7.3 `name`](#73-name--pool-name-unnamed-pools-never-match) — Pool name; **unnamed pools never match**
- [7.4 `has_entry`](#74-has_entry--any-entry-in-the-pool-satisfies-that-entry-selector) — **Any** entry in the pool satisfies that entry selector
- [7.5 `total_weight`](#75-total_weight--the-pools-total-weight-falls-inside-the-closed-interval) — The pool's total weight falls inside the closed interval
- [7.6 `not`](#76-not--negation) — Negation
- [7.7 `any_of`](#77-any_of--any-of-them-matches) — Any of them matches
- [7.8 `all_of`](#78-all_of--all-of-them-match) — All of them match
- [7.9 `custom`](#79-custom--handed-to-the-listeners-of-custompoolselectorevent) — Handed to the listeners of `CustomPoolSelectorEvent`

#### 7.1 `any` — All pools (**default**)

- No parameters.

```json
{
  "type": "arcanumlib:any"
}
```

#### 7.2 `index` — Pool index (starting from 0)

- `*index`: <int> the index of the pool in the table, starting at 0.

```json
{
  "type": "arcanumlib:index",
  "index": 0
}
```

#### 7.3 `name` — Pool name; **unnamed pools never match**

- `*name`: <String> the pool name; unnamed pools never match.

```json
{
  "type": "arcanumlib:name",
  "name": "main"
}
```

#### 7.4 `has_entry` — **Any** entry in the pool satisfies that entry selector

- `*entrySelector`: <EntrySelector> the selector used to test the entries of the pool, see [9. Entry Selectors](#9-entry-selectors-15).

```json
{
  "type": "arcanumlib:has_entry",
  "entrySelector": {
    "type": "arcanumlib:item",
    "item": "minecraft:gunpowder"
  }
}
```

#### 7.5 `total_weight` — The pool's total weight falls inside the closed interval

- `min`: <int> the lower weight bound (default 0).
- `max`: <int> the upper weight bound (default Integer.MAX_VALUE).

```json
{
  "type": "arcanumlib:total_weight",
  "min": 2,
  "max": 20
}
```

#### 7.6 `not` — Negation

- `*condition`: <PoolSelector> the child selector to negate, see [7. Pool Selectors](#7-pool-selectors-9).

```json
{
  "type": "arcanumlib:not",
  "condition": {
    "type": "arcanumlib:index",
    "index": 99
  }
}
```

#### 7.7 `any_of` — Any of them matches

- `*conditions`: <json array> a list of child selectors (each a <PoolSelector>); matches when any of them matches.

```json
{
  "type": "arcanumlib:any_of",
  "conditions": [
    {
      "type": "arcanumlib:index",
      "index": 0
    },
    {
      "type": "arcanumlib:index",
      "index": 1
    }
  ]
}
```

#### 7.8 `all_of` — All of them match

- `*conditions`: <json array> a list of child selectors (each a <PoolSelector>); matches only when all of them match.

```json
{
  "type": "arcanumlib:all_of",
  "conditions": [
    {
      "type": "arcanumlib:any"
    },
    {
      "type": "arcanumlib:not",
      "condition": {
        "type": "arcanumlib:index",
        "index": 99
      }
    }
  ]
}
```

#### 7.9 `custom` — Handed to the listeners of `CustomPoolSelectorEvent`

- `*custom_id`: <String> the id handed to the pool-level custom event listener.
- `data`: <CompoundTag> **optional** extra data; either an SNBT string (e.g. `"{team: \"red\"}"`) or a plain json object; when omitted it is an **empty tag** (never null) and listeners read it via `event.getData()`.

```json
{
  "type": "arcanumlib:custom",
  "custom_id": "my_pool_hook"
}
```

**How the total weight is measured** (`arcanumlib:total_weight`, as well as the entry-level `weight_in_range` / `weight_share`):
- Only the **static base weight** counts: the `weight` field, equivalent to vanilla `getWeight(luck = 0)` (including the `max(..., 0)` clamp);
- **Excludes** `quality × luck` — selectors have no access to a `LootContext`;
- **Does not filter by entry conditions** — vanilla expands via `expand(context)` when rolling;
- Composite entries (`alternatives`/`sequence`/`group`) have no `weight` of their own, so their **subtrees are summed recursively**; for `alternatives`, vanilla only walks the first branch whose condition passes, whereas here every branch is counted, which makes it a **static upper bound**.
---
## 8. Pool-Level Operators (12)

Written inside `modify_pool.ops`. Registry: `arcanumlib:pool_operator`.

**Entries**:

- [8.1 `remove_pool`](#81-remove_pool--delete-the-current-pool) — Delete **the current pool**
- [8.2 `add_entry`](#82-add_entry--append-an-entry-to-the-current-pool) — Append an entry to the current pool
- [8.3 `modify_entry`](#83-modify_entry--scope-apply-the-child-ops-on-the-matched-entries-in-the-current-pool) — Scope: apply the child ops on the matched entries in the current pool
- [8.4 `modify_function`](#84-modify_function--scope-on-the-current-pools-function-list) — Scope: on the current pool's function list
- [8.5 `modify_condition`](#85-modify_condition--scope-on-the-current-pools-own-conditions) — Scope: on the current pool's own conditions
- [8.6 `set_condition`](#86-set_condition--replace-the-pools-own-condition-list) — Replace the pool's own condition list
- [8.7 `add_condition`](#87-add_condition--append-to-the-pools-own-condition-list) — Append to the pool's own condition list
- [8.8 `remove_condition`](#88-remove_condition--remove-the-pools-own-conditions-by-condition-type-id) — Remove the pool's own conditions by condition type id
- [8.9 `set_function`](#89-set_function--replace-the-pools-own-function-list) — Replace the pool's own function list
- [8.10 `add_function`](#810-add_function--append-to-the-pools-own-function-list) — Append to the pool's own function list
- [8.11 `remove_function`](#811-remove_function--remove-the-pools-own-functions-by-function-type-id) — Remove the pool's own functions by function type id
- [8.12 `pool_custom`](#812-pool_custom--handed-to-the-listeners-of-poolcustomopevent) — Handed to the listeners of `PoolCustomOpEvent`

#### 8.1 `remove_pool` — Delete **the current pool**
- No parameters.

```json
{
  "type": "arcanumlib:remove_pool"
}
```

#### 8.2 `add_entry` — Append an entry to the current pool
- `*entry`: <json object> a vanilla entry (`minecraft:item` / `minecraft:tag` ...).

```json
{
  "type": "arcanumlib:add_entry",
  "entry": {
    "type": "minecraft:item",
    "name": "minecraft:iron_ingot"
  }
}
```

#### 8.3 `modify_entry` — Scope: apply the child ops on the matched entries in the current pool
- `*entrySelector`: <EntrySelector> selects the entries to modify, see [9. Entry Selectors](#9-entry-selectors-15).
- `*ops`: <json array> a list of entry-level operators, see [10. Entry-Level Operators](#10-entry-level-operators-17).

```json
{
  "type": "arcanumlib:modify_entry",
  "entrySelector": {
    "type": "arcanumlib:weight_share",
    "min": 0.7,
    "max": 0.8
  },
  "ops": [
    {
      "type": "arcanumlib:set_function",
      "functions": [
        {
          "function": "minecraft:set_count",
          "count": 5.0
        }
      ]
    }
  ]
}
```

#### 8.4 `modify_function` — Scope: on the current pool's function list
- `*functionSelector`: <FunctionSelector> selects the functions to modify, see [11. Function Selectors](#11-function-selectors-9).
- `*ops`: <json array> a list of function-level operators, see [12. Function-Level Operators](#12-function-level-operators-3).

```json
{
  "type": "arcanumlib:modify_function",
  "functionSelector": {
    "type": "arcanumlib:any"
  },
  "ops": [
    {
      "type": "arcanumlib:remove_function"
    }
  ]
}
```

#### 8.5 `modify_condition` — Scope: on the current pool's own conditions
- `*conditionSelector`: <ConditionSelector> selects the conditions to modify, see [13. Condition Selectors](#13-condition-selectors-8).
- `*ops`: <json array> a list of condition-level operators, see [14. Condition-Level Operators](#14-condition-level-operators-2).

```json
{
  "type": "arcanumlib:modify_condition",
  "conditionSelector": {
    "type": "arcanumlib:condition_type",
    "conditionType": "minecraft:random_chance"
  },
  "ops": [
    {
      "type": "arcanumlib:remove_condition"
    }
  ]
}
```

#### 8.6 `set_condition` — Replace the pool's own condition list
- `*conditions`: <json array> a list of vanilla predicates (each a `minecraft:` predicate).

```json
{
  "type": "arcanumlib:set_condition",
  "conditions": [
    {
      "condition": "minecraft:random_chance",
      "chance": 1.0
    }
  ]
}
```

#### 8.7 `add_condition` — Append to the pool's own condition list
- `*conditions`: <json array> a list of vanilla predicates (each a `minecraft:` predicate).

```json
{
  "type": "arcanumlib:add_condition",
  "conditions": [
    {
      "condition": "minecraft:random_chance",
      "chance": 0.5
    }
  ]
}
```

#### 8.8 `remove_condition` — Remove the pool's own conditions by condition type id
- `*conditionTypes`: <json array> vanilla condition type ids, e.g. `["minecraft:random_chance"]`.

```json
{
  "type": "arcanumlib:remove_condition",
  "conditionTypes": [
    "minecraft:random_chance"
  ]
}
```

#### 8.9 `set_function` — Replace the pool's own function list
- `*functions`: <json array> a list of vanilla item modifiers (such as `minecraft:set_count`).

```json
{
  "type": "arcanumlib:set_function",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 2.0,
      "add": true
    }
  ]
}
```

#### 8.10 `add_function` — Append to the pool's own function list
- `*functions`: <json array> a list of vanilla item modifiers (such as `minecraft:set_count`).

```json
{
  "type": "arcanumlib:add_function",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 1.0,
      "add": true
    }
  ]
}
```

#### 8.11 `remove_function` — Remove the pool's own functions by function type id
- `*functionTypes`: <json array> vanilla function type ids, e.g. `["minecraft:set_count"]`.

```json
{
  "type": "arcanumlib:remove_function",
  "functionTypes": [
    "minecraft:set_count"
  ]
}
```

#### 8.12 `pool_custom` — Handed to the listeners of `PoolCustomOpEvent`
- `*custom_id`: <String> the id handed to the pool-level custom event listener.
- `data`: <CompoundTag> **optional** extra data; either an SNBT string (e.g. `"{times: 3}"`) or a plain json object; when omitted it is an **empty tag** (never null) and listeners read it via `event.getData()`.

```json
{
  "type": "arcanumlib:pool_custom",
  "custom_id": "my_pool_hook"
}
```

---
## 9. Entry Selectors (15)

Written inside `modify_entry.entrySelector`. Registry: `arcanumlib:entry_selector`.

**Entries**:

- [9.1 `any`](#91-any--all-entries-default) — All entries (**default**)
- [9.2 `index`](#92-index--the-entrys-index-inside-the-pool) — The entry's index inside the pool
- [9.3 `item`](#93-item--exact-match-on-the-item-id-only-meaningful-for-minecraftitem-entries) — Exact match on the item id (only meaningful for `minecraft:item` entries)
- [9.4 `tag`](#94-tag--matches-minecrafttag-entries-the-tag-itself-equals-the-given-tag) — Matches **`minecraft:tag` entries**: the tag itself equals the given tag
- [9.5 `item_with_tag`](#95-item_with_tag--matches-minecraftitem-entries-their-item-belongs-to-the-given-tag-note-the-difference-from-tag) — Matches `minecraft:item` entries: their **item** belongs to the given tag (note the difference from `tag`)
- [9.6 `entry_type`](#96-entry_type--entry-type-such-as-minecraftitem-minecraftalternatives) — Entry type, such as `minecraft:item`, `minecraft:alternatives`
- [9.7 `weight_in_range`](#97-weight_in_range--the-entrys-weight-for-a-composite-entry-its-subtrees-falls-inside-the-closed-interval) — The entry's weight (for a composite entry, its subtree's) falls inside the closed interval
- [9.8 `weight_share`](#98-weight_share--the-entrys-share-of-its-pools-total-weight-falls-inside-the-closed-interval-0-1) — The entry's share of its pool's total weight falls inside the closed interval `[0, 1]`
- [9.9 `has_condition`](#99-has_condition--the-entry-carries-a-condition-of-this-type) — The entry carries a condition of this type
- [9.10 `has_function`](#910-has_function--the-entry-carries-a-function-of-this-type) — The entry carries a function of this type
- [9.11 `has_child`](#911-has_child--some-direct-child-of-a-composite-entry-matches-not-recursive) — Some **direct child** of a composite entry matches (not recursive)
- [9.12 `not`](#912-not--negation) — Negation
- [9.13 `any_of`](#913-any_of--any-of-them-matches) — Any of them matches
- [9.14 `all_of`](#914-all_of--all-of-them-match) — All of them match
- [9.15 `custom`](#915-custom--handed-to-the-listeners-of-customentryselectorevent) — Handed to the listeners of `CustomEntrySelectorEvent`

#### 9.1 `any` — All entries (**default**)
- No parameters.

```json
{
  "type": "arcanumlib:any"
}
```

#### 9.2 `index` — The entry's index inside the pool
- `*index`: <int> the index of the entry inside the pool, starting at 0.

```json
{
  "type": "arcanumlib:index",
  "index": 0
}
```

#### 9.3 `item` — Exact match on the item id (only meaningful for `minecraft:item` entries)
- `*item`: <ResourceLocation> an item id, e.g. `minecraft:diamond`.

```json
{
  "type": "arcanumlib:item",
  "item": "minecraft:gunpowder"
}
```

#### 9.4 `tag` — Matches **`minecraft:tag` entries**: the tag itself equals the given tag
- `*tag`: <TagKey<Item>> an item tag; matches a `minecraft:tag` **entry**, comparing the tag itself.

```json
{
  "type": "arcanumlib:tag",
  "tag": "minecraft:coals"
}
```

#### 9.5 `item_with_tag` — Matches `minecraft:item` entries: their **item** belongs to the given tag (note the difference from `tag`)
- `*tag`: <TagKey<Item>> an item tag; matches an entry that names a concrete item, testing whether that item is in the tag.

```json
{
  "type": "arcanumlib:item_with_tag",
  "tag": "minecraft:coals"
}
```

#### 9.6 `entry_type` — Entry type, such as `minecraft:item`, `minecraft:alternatives`
- `*entryType`: <ResourceLocation> an entry type id, e.g. `minecraft:item`.

```json
{
  "type": "arcanumlib:entry_type",
  "entryType": "minecraft:alternatives"
}
```

#### 9.7 `weight_in_range` — The entry's weight (for a composite entry, its subtree's) falls inside the closed interval
- `min`: <int> the lower weight bound (default 0).
- `max`: <int> the upper weight bound (default Integer.MAX_VALUE).

```json
{
  "type": "arcanumlib:weight_in_range",
  "min": 1,
  "max": 1
}
```

#### 9.8 `weight_share` — The entry's share of its pool's total weight falls inside the closed interval `[0, 1]`
- `min`: <float> the lower share bound (0~1).
- `max`: <float> the upper share bound (0~1).

```json
{
  "type": "arcanumlib:weight_share",
  "min": 0.05,
  "max": 0.2
}
```

#### 9.9 `has_condition` — The entry carries a condition of this type
- `*conditionType`: <ResourceLocation> a vanilla condition type id, e.g. `minecraft:random_chance`.

```json
{
  "type": "arcanumlib:has_condition",
  "conditionType": "minecraft:random_chance"
}
```

#### 9.10 `has_function` — The entry carries a function of this type
- `*functionType`: <ResourceLocation> a vanilla function type id, e.g. `minecraft:set_count`.

```json
{
  "type": "arcanumlib:has_function",
  "functionType": "minecraft:set_count"
}
```

#### 9.11 `has_child` — Some **direct child** of a composite entry matches (not recursive)
- `*entrySelector`: <EntrySelector> the selector used to test the direct children, see [9. Entry Selectors](#9-entry-selectors-15).

```json
{
  "type": "arcanumlib:has_child",
  "entrySelector": {
    "type": "arcanumlib:item",
    "item": "minecraft:iron_ingot"
  }
}
```

#### 9.12 `not` — Negation
- `*condition`: <EntrySelector> the child selector to negate, see [9. Entry Selectors](#9-entry-selectors-15).

```json
{
  "type": "arcanumlib:not",
  "condition": {
    "type": "arcanumlib:item",
    "item": "minecraft:golden_apple"
  }
}
```

#### 9.13 `any_of` — Any of them matches
- `*conditions`: <json array> a list of child selectors (each an <EntrySelector>); matches when any of them matches.

```json
{
  "type": "arcanumlib:any_of",
  "conditions": [
    {
      "type": "arcanumlib:item",
      "item": "minecraft:golden_apple"
    },
    {
      "type": "arcanumlib:item",
      "item": "minecraft:gunpowder"
    }
  ]
}
```

#### 9.14 `all_of` — All of them match
- `*conditions`: <json array> a list of child selectors (each an <EntrySelector>); matches only when all of them match.

```json
{
  "type": "arcanumlib:all_of",
  "conditions": [
    {
      "type": "arcanumlib:entry_type",
      "entryType": "minecraft:item"
    },
    {
      "type": "arcanumlib:weight_in_range",
      "min": 1,
      "max": 1
    }
  ]
}
```

#### 9.15 `custom` — Handed to the listeners of `CustomEntrySelectorEvent`
- `*custom_id`: <String> the id handed to the entry-level custom event listener.
- `data`: <CompoundTag> **optional** extra data; either an SNBT string (e.g. `"{team: \"red\"}"`) or a plain json object; when omitted it is an **empty tag** (never null) and listeners read it via `event.getData()`.

```json
{
  "type": "arcanumlib:custom",
  "custom_id": "my_entry_hook"
}
```

---
> To **change** what is inside those children, use the entry-level ops `modify_child` / `add_child` / `remove_child` / `move_child` (see 10.14-10.17).
## 10. Entry-Level Operators (17)

Written inside `modify_entry.ops`. Registry: `arcanumlib:entry_operator`.

**Entries**:

> The `modify_child` / `add_child` / `remove_child` / `move_child` ops in this section **only work on composite entries** (`alternatives` / `group` / `sequence`); see each subsection.
> Mind the scope: `add_child` / `remove_child` / `move_child` all act on **the current entry's** children, so to change a composite you must make **that composite the current entry** (typically by putting these ops in a `modify_entry` whose `entry_type` selects `alternatives` / `group` / `sequence`); `modify_child` is for descending **into a child**. Do not mix the two.

- [10.1 `remove_entry`](#101-remove_entry--delete-the-current-entry) — Delete the current entry
- [10.2 `replace_entry`](#102-replace_entry--replace-the-current-entry-with-a-new-one-one-independent-instance-per-site) — Replace the current entry with a new one (one independent instance per site)
- [10.3 `set_weight`](#103-set_weight--set-the-weight-a-negative-number-means-do-not-change-composite-entries-have-no-weight-and-are-skipped) — Set the weight; **a negative number means "do not change"**; composite entries have no weight and are skipped
- [10.4 `set_quality`](#104-set_quality--set-the-quality-a-negative-number-means-do-not-change-composite-entries-have-no-quality-and-are-skipped) — Set the quality; **a negative number means "do not change"**; composite entries have no quality and are skipped
- [10.5 `modify_function`](#105-modify_function--scope-on-the-current-entrys-function-list) — Scope: on the current entry's function list
- [10.6 `modify_condition`](#106-modify_condition--scope-on-the-current-entrys-own-conditions) — Scope: on the current entry's own conditions
- [10.7 `set_condition`](#107-set_condition--replace-the-current-entrys-condition-list) — Replace the current entry's condition list
- [10.8 `add_condition`](#108-add_condition--append-to-the-current-entrys-condition-list) — Append to the current entry's condition list
- [10.9 `remove_condition`](#109-remove_condition--remove-the-current-entrys-conditions-by-type-id) — Remove the current entry's conditions by type id
- [10.10 `set_function`](#1010-set_function--replace-the-current-entrys-function-list-composite-entries-are-skipped) — Replace the current entry's function list (composite entries are skipped)
- [10.11 `add_function`](#1011-add_function--append-to-the-current-entrys-function-list-composite-entries-are-skipped) — Append to the current entry's function list (composite entries are skipped)
- [10.12 `remove_function`](#1012-remove_function--remove-the-current-entrys-functions-by-type-id-composite-entries-are-skipped) — Remove the current entry's functions by type id (composite entries are skipped)
- [10.13 `entry_custom`](#1013-entry_custom--handed-to-the-listeners-of-entrycustomopevent) — Handed to the listeners of `EntryCustomOpEvent`
- [10.14 `modify_child`](#1014-modify_child--modify-children) — Modify children
- [10.15 `add_child`](#1015-add_child--insert-a-branch) — Insert a branch into a composite
- [10.16 `remove_child`](#1016-remove_child--remove-branches) — Remove children by selector
- [10.17 `move_child`](#1017-move_child--reorder-branches) — Reorder children

#### 10.1 `remove_entry` — Delete the current entry
- No parameters.

```json
{
  "type": "arcanumlib:remove_entry"
}
```

#### 10.2 `replace_entry` — Replace the current entry with a new one (one independent instance per site)
- `*entry`: <json object> a vanilla entry used to replace the current one (one independent instance per site).

```json
{
  "type": "arcanumlib:replace_entry",
  "entry": {
    "type": "minecraft:item",
    "name": "minecraft:leather",
    "functions": [
      {
        "function": "minecraft:enchanted_count_increase",
        "enchantment": "minecraft:looting",
        "count": {
          "type": "minecraft:uniform",
          "min": 0.0,
          "max": 1.0
        }
      }
    ]
  }
}
```

#### 10.3 `set_weight` — Set the weight; **a negative number means "do not change"**; composite entries have no weight and are skipped
- `*weight`: <int> the target weight; a **negative number means "leave unchanged"** (legacy semantics, so old json can be reused).

```json
{
  "type": "arcanumlib:set_weight",
  "weight": 20
}
```

#### 10.4 `set_quality` — Set the quality; **a negative number means "do not change"**; composite entries have no quality and are skipped
- `*quality`: <int> the target quality; a **negative number means "leave unchanged"** (legacy semantics).

```json
{
  "type": "arcanumlib:set_quality",
  "quality": 1
}
```

#### 10.5 `modify_function` — Scope: on the current entry's function list
- `*functionSelector`: <FunctionSelector> selects the functions to modify, see [11. Function Selectors](#11-function-selectors-9).
- `*ops`: <json array> a list of function-level operators, see [12. Function-Level Operators](#12-function-level-operators-3).

```json
{
  "type": "arcanumlib:modify_function",
  "functionSelector": {
    "type": "arcanumlib:match",
    "function": {
      "function": "minecraft:set_count",
      "count": 3.0
    }
  },
  "ops": [
    {
      "type": "arcanumlib:remove_function"
    }
  ]
}
```

#### 10.6 `modify_condition` — Scope: on the current entry's own conditions
- `*conditionSelector`: <ConditionSelector> selects the conditions to modify, see [13. Condition Selectors](#13-condition-selectors-8).
- `*ops`: <json array> a list of condition-level operators, see [14. Condition-Level Operators](#14-condition-level-operators-2).

```json
{
  "type": "arcanumlib:modify_condition",
  "conditionSelector": {
    "type": "arcanumlib:index",
    "index": 0
  },
  "ops": [
    {
      "type": "arcanumlib:replace_condition",
      "condition": {
        "condition": "minecraft:random_chance",
        "chance": 1.0
      }
    }
  ]
}
```

#### 10.7 `set_condition` — Replace the current entry's condition list
- `*conditions`: <json array> a list of vanilla predicates (each a `minecraft:` predicate); **replaces** the entry list.

```json
{
  "type": "arcanumlib:set_condition",
  "conditions": [
    {
      "condition": "minecraft:random_chance",
      "chance": 1.0
    }
  ]
}
```

#### 10.8 `add_condition` — Append to the current entry's condition list
- `*conditions`: <json array> a list of vanilla predicates (each a `minecraft:` predicate); **appends** to the entry list.

```json
{
  "type": "arcanumlib:add_condition",
  "conditions": [
    {
      "condition": "minecraft:random_chance",
      "chance": 0.5
    }
  ]
}
```

#### 10.9 `remove_condition` — Remove the current entry's conditions by type id
- `*conditionTypes`: <json array> vanilla condition type ids, e.g. `["minecraft:random_chance"]`.

```json
{
  "type": "arcanumlib:remove_condition",
  "conditionTypes": [
    "minecraft:random_chance"
  ]
}
```

#### 10.10 `set_function` — Replace the current entry's function list (composite entries are skipped)
- `*functions`: <json array> a list of vanilla item modifiers (such as `minecraft:set_count`); **replaces** the entry list.

```json
{
  "type": "arcanumlib:set_function",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 2.0
    }
  ]
}
```

#### 10.11 `add_function` — Append to the current entry's function list (composite entries are skipped)
- `*functions`: <json array> a list of vanilla item modifiers (such as `minecraft:set_count`); **appends** to the entry list.

```json
{
  "type": "arcanumlib:add_function",
  "functions": [
    {
      "function": "minecraft:set_count",
      "count": 1.0,
      "add": true
    }
  ]
}
```

#### 10.12 `remove_function` — Remove the current entry's functions by type id (composite entries are skipped)
- `*functionTypes`: <json array> vanilla function type ids, e.g. `["minecraft:set_count"]`.

```json
{
  "type": "arcanumlib:remove_function",
  "functionTypes": [
    "minecraft:set_count"
  ]
}
```

#### 10.13 `entry_custom` — Handed to the listeners of `EntryCustomOpEvent`
- `*custom_id`: <String> the id handed to the entry-level custom event listener.
- `data`: <CompoundTag> **optional** extra data; either an SNBT string (e.g. `"{times: 3}"`) or a plain json object; when omitted it is an **empty tag** (never null) and listeners read it via `event.getData()`.

```json
{
  "type": "arcanumlib:entry_custom",
  "custom_id": "my_entry_hook"
}
```

---
#### 10.14 `modify_child` — Modify Children

> Only applies to **composite entries** (`alternatives` / `group` / `sequence`): when the current entry is not a composite the op is **skipped** and nothing changes.

- `entrySelector`: <EntrySelector> which children receive the sub-ops (default `any`, i.e. all of them);
- `ops`: <List of EntryOperator> applied to the selected children in order (default empty).

```json
{
  "type": "arcanumlib:modify_child",
  "entrySelector": { "type": "arcanumlib:entry_type", "entryType": "minecraft:group" },
  "ops": [
    { "type": "arcanumlib:remove_child", "entrySelector": { "type": "arcanumlib:index", "index": 1 } }
  ]
}
```

#### 10.15 `add_child` — Insert a Branch

> Only applies to **composite entries** (`alternatives` / `group` / `sequence`): when the current entry is not a composite the op is **skipped** and nothing changes.

- `entry`: <entry> **required**, the branch to insert;
- `index`: <Int> **optional**, where to insert it (defaults to appending at the end, which keeps existing priorities).

```json
{
  "type": "arcanumlib:add_child",
  "index": 0,
  "entry": { "type": "minecraft:item", "name": "minecraft:diamond" }
}
```

#### 10.16 `remove_child` — Remove Branches

> Only applies to **composite entries** (`alternatives` / `group` / `sequence`): when the current entry is not a composite the op is **skipped** and nothing changes.

- `entrySelector`: <EntrySelector> which children to remove (default `any`, i.e. all; every match is removed).

```json
{
  "type": "arcanumlib:remove_child",
  "entrySelector": { "type": "arcanumlib:item", "item": "minecraft:stick" }
}
```

#### 10.17 `move_child` — Reorder Branches

> Only applies to **composite entries** (`alternatives` / `group` / `sequence`): when the current entry is not a composite the op is **skipped** and nothing changes.

- `entrySelector`: <EntrySelector> the child to move (default `any`; only the first match is moved);
- `position`: <String> `first` / `last` / `index` (default `first`);
- `index`: <Int> the target slot when `position` is `index` (default 0).

> The order of `alternatives` children **is** the priority: moving a branch to `first` makes it win first.

```json
{
  "type": "arcanumlib:move_child",
  "entrySelector": { "type": "arcanumlib:entry_type", "entryType": "minecraft:group" },
  "position": "first"
}
```
## 11. Function Selectors (9)

Written inside `modify_function.functionSelector`. Registry: `arcanumlib:function_selector`.

**Entries**:

- [11.1 `any`](#111-any--all-functions-default) — All functions (**default**)
- [11.2 `index`](#112-index--the-functions-index-in-the-list) — The function's index in the list
- [11.3 `function_type`](#113-function_type--by-function-type-id-such-as-minecraftset_count) — By function type id, such as `minecraft:set_count`
- [11.4 `has_condition`](#114-has_condition--the-functions-own-conditions-contain-this-type) — The function's **own** conditions contain this type
- [11.5 `not`](#115-not--negation) — Negation
- [11.6 `any_of`](#116-any_of--any-of-them-matches) — Any of them matches
- [11.7 `all_of`](#117-all_of--all-of-them-match) — All of them match
- [11.8 `match`](#118-match--match-by-the-keys-of-a-template-json-see-below) — Match by the **keys of a template json** (see below)
- [11.9 `custom`](#119-custom--handed-to-the-listeners-of-customfunctionselectorevent) — Handed to the listeners of `CustomFunctionSelectorEvent`

#### 11.1 `any` — All functions (**default**)
- No parameters.

```json
{
  "type": "arcanumlib:any"
}
```

#### 11.2 `index` — The function's index in the list
- `*index`: <int> the index of the function in the list, starting at 0.

```json
{
  "type": "arcanumlib:index",
  "index": 0
}
```

#### 11.3 `function_type` — By function type id, such as `minecraft:set_count`
- `*functionType`: <ResourceLocation> a vanilla function type id, e.g. `minecraft:set_count`.

```json
{
  "type": "arcanumlib:function_type",
  "functionType": "minecraft:set_count"
}
```

#### 11.4 `has_condition` — The function's **own** conditions contain this type
- `*conditionType`: <ResourceLocation> a vanilla condition type id, e.g. `minecraft:random_chance`; matches when the function carries such a condition.

```json
{
  "type": "arcanumlib:has_condition",
  "conditionType": "minecraft:random_chance"
}
```

#### 11.5 `not` — Negation
- `*selector`: <FunctionSelector> the child selector to negate, see [11. Function Selectors](#11-function-selectors-9).

```json
{
  "type": "arcanumlib:not",
  "selector": {
    "type": "arcanumlib:index",
    "index": 99
  }
}
```

#### 11.6 `any_of` — Any of them matches
- `*selectors`: <json array> a list of child selectors (each a <FunctionSelector>); matches when any of them matches.

```json
{
  "type": "arcanumlib:any_of",
  "selectors": [
    {
      "type": "arcanumlib:index",
      "index": 0
    },
    {
      "type": "arcanumlib:function_type",
      "functionType": "minecraft:set_count"
    }
  ]
}
```

#### 11.7 `all_of` — All of them match
- `*selectors`: <json array> a list of child selectors (each a <FunctionSelector>); matches only when all of them match.

```json
{
  "type": "arcanumlib:all_of",
  "selectors": [
    {
      "type": "arcanumlib:function_type",
      "functionType": "minecraft:set_count"
    },
    {
      "type": "arcanumlib:not",
      "selector": {
        "type": "arcanumlib:index",
        "index": 99
      }
    }
  ]
}
```

#### 11.8 `match` — Match by the **keys of a template json** (see below)
- `*function`: <json object> the template json is kept **as written**, and only the keys you actually wrote are compared.
- `strict`: <boolean> defaults to `false`; when enabled the candidate must match the template **exactly**.

```json
{
  "type": "arcanumlib:match",
  "function": {
    "function": "minecraft:set_count",
    "count": 3.0
  }
}
```

#### 11.9 `custom` — Handed to the listeners of `CustomFunctionSelectorEvent`
- `*custom_id`: <String> the id handed to the function-level custom event listener.
- `data`: <CompoundTag> **optional** extra data; either an SNBT string (e.g. `"{team: \"red\"}"`) or a plain json object; when omitted it is an **empty tag** (never null) and listeners read it via `event.getData()`.

```json
{
  "type": "arcanumlib:custom",
  "custom_id": "my_function_hook"
}
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

**Entries**:

- [12.1 `remove_function`](#121-remove_function--delete-the-current-function) — Delete the current function
- [12.2 `replace_function`](#122-replace_function--replace-the-current-function-with-a-new-one-one-independent-instance-per-site) — Replace the current function with a new one (one independent instance per site)
- [12.3 `modify_condition`](#123-modify_condition--scope-reach-into-the-current-functions-own-conditions-non-conditional-functions-such-as-set_count-have-no-conditions-and-are-skipped) — Scope: reach into **the current function's own conditions**; non-conditional functions (such as `set_count`) have no conditions and are skipped

#### 12.1 `remove_function` — Delete the current function
- No parameters.

```json
{
  "type": "arcanumlib:remove_function"
}
```

#### 12.2 `replace_function` — Replace the current function with a new one (one independent instance per site)
- `*function`: <json object> a vanilla item modifier used to replace the current function (one independent instance per site).

```json
{
  "type": "arcanumlib:replace_function",
  "function": {
    "function": "minecraft:set_count",
    "count": 7.0,
    "add": true
  }
}
```

#### 12.3 `modify_condition` — Scope: reach into **the current function's own conditions**; non-conditional functions (such as `set_count`) have no conditions and are skipped
- `*conditionSelector`: <ConditionSelector> selects the conditions to modify, see [13. Condition Selectors](#13-condition-selectors-8).
- `*ops`: <json array> a list of condition-level operators, see [14. Condition-Level Operators](#14-condition-level-operators-2).

```json
{
  "type": "arcanumlib:modify_condition",
  "conditionSelector": {
    "type": "arcanumlib:condition_type",
    "conditionType": "minecraft:random_chance"
  },
  "ops": [
    {
      "type": "arcanumlib:remove_condition"
    }
  ]
}
```

> There is **no** generic implementation of "change an argument" (for example changing `count` from 5 to 10): each function's argument fields are private to that function.
> When you need it, combine `remove_function` + `add_function`, or write a `function_custom`-level custom op (see section 16).
---
## 13. Condition Selectors (8)

Written inside `modify_condition.conditionSelector`. Registry: `arcanumlib:condition_selector`.

**Entries**:

- [13.1 `any`](#131-any--all-conditions-default) — All conditions (**default**)
- [13.2 `index`](#132-index--the-conditions-index) — The condition's index
- [13.3 `condition_type`](#133-condition_type--by-condition-type-id-such-as-minecraftrandom_chance) — By condition type id, such as `minecraft:random_chance`
- [13.4 `not`](#134-not--negation) — Negation
- [13.5 `any_of`](#135-any_of--any-of-them-matches) — Any of them matches
- [13.6 `all_of`](#136-all_of--all-of-them-match) — All of them match
- [13.7 `match`](#137-match--match-by-the-keys-of-a-template-json-same-semantics-as-match-on-the-function-side) — Match by the keys of a template json (same semantics as `match` on the function side)
- [13.8 `custom`](#138-custom--handed-to-the-listeners-of-customconditionselectorevent) — Handed to the listeners of `CustomConditionSelectorEvent`

#### 13.1 `any` — All conditions (**default**)
- No parameters.

```json
{
  "type": "arcanumlib:any"
}
```

#### 13.2 `index` — The condition's index
- `*index`: <int> the index of the condition in the list, starting at 0.

```json
{
  "type": "arcanumlib:index",
  "index": 0
}
```

#### 13.3 `condition_type` — By condition type id, such as `minecraft:random_chance`
- `*conditionType`: <ResourceLocation> a vanilla condition type id, e.g. `minecraft:random_chance`.

```json
{
  "type": "arcanumlib:condition_type",
  "conditionType": "minecraft:random_chance"
}
```

#### 13.4 `not` — Negation
- `*selector`: <ConditionSelector> the child selector to negate, see [13. Condition Selectors](#13-condition-selectors-8).

```json
{
  "type": "arcanumlib:not",
  "selector": {
    "type": "arcanumlib:index",
    "index": 99
  }
}
```

#### 13.5 `any_of` — Any of them matches
- `*selectors`: <json array> a list of child selectors (each a <ConditionSelector>); matches when any of them matches.

```json
{
  "type": "arcanumlib:any_of",
  "selectors": [
    {
      "type": "arcanumlib:index",
      "index": 0
    },
    {
      "type": "arcanumlib:condition_type",
      "conditionType": "minecraft:random_chance"
    }
  ]
}
```

#### 13.6 `all_of` — All of them match
- `*selectors`: <json array> a list of child selectors (each a <ConditionSelector>); matches only when all of them match.

```json
{
  "type": "arcanumlib:all_of",
  "selectors": [
    {
      "type": "arcanumlib:any"
    },
    {
      "type": "arcanumlib:not",
      "selector": {
        "type": "arcanumlib:index",
        "index": 99
      }
    }
  ]
}
```

#### 13.7 `match` — Match by the keys of a template json (same semantics as `match` on the function side)
- `*condition`: <json object> the template json is kept **as written**, and only the keys you actually wrote are compared.
- `strict`: <boolean> defaults to `false`; when enabled the candidate must match the template **exactly**.

```json
{
  "type": "arcanumlib:match",
  "condition": {
    "condition": "minecraft:random_chance",
    "chance": 1.0
  }
}
```

#### 13.8 `custom` — Handed to the listeners of `CustomConditionSelectorEvent`
- `*custom_id`: <String> the id handed to the condition-level custom event listener.
- `data`: <CompoundTag> **optional** extra data; either an SNBT string (e.g. `"{team: \"red\"}"`) or a plain json object; when omitted it is an **empty tag** (never null) and listeners read it via `event.getData()`.

```json
{
  "type": "arcanumlib:custom",
  "custom_id": "my_condition_hook"
}
```

---
## 14. Condition-Level Operators (2)

Written inside `modify_condition.ops`. Registry: `arcanumlib:condition_operator`.

**Entries**:

- [14.1 `remove_condition`](#141-remove_condition--delete-the-current-condition) — Delete the current condition
- [14.2 `replace_condition`](#142-replace_condition--replace-the-current-condition-with-a-new-one-one-independent-instance-per-site) — Replace the current condition with a new one (one independent instance per site)

#### 14.1 `remove_condition` — Delete the current condition
- No parameters.

| `replace_condition` | `condition` | Replace the current condition with a new one (one independent instance per site) |
```json
{
  "type": "arcanumlib:remove_condition"
}
```

#### 14.2 `replace_condition` — Replace the current condition with a new one (one independent instance per site)
- `*condition`: <json object> a vanilla predicate used to replace the current condition (one independent instance per site).

```json
{
  "type": "arcanumlib:replace_condition",
  "condition": {
    "condition": "minecraft:random_chance",
    "chance": 0.25
  }
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
> All eight `custom` events (five selectors plus three operators) also carry `getData()`: the optional `data` from json (an SNBT string or a plain json object); when omitted it is an **empty tag** (never null).

> **All 9 events carry `getRegistries()`** (including `LootInjectorApplyEvent`).
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
        // need registries? use event.getRegistries() (every event has it, see the note above)
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
{
  "type": "mymod:is_shiny",
  "marker": "x"
}
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

## 17. Recipes: Common Tasks

### 17.1 Add an "Extra Drop Pool" to a Table

```json
{
  "priority": 1000,
  "tableSelector": {
    "type": "arcanumlib:id",
    "id": "minecraft:chests/simple_dungeon"
  },
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
            "conditions": [
              {
                "condition": "minecraft:random_chance",
                "chance": 0.05
              }
            ]
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
  "tableSelector": {
    "type": "arcanumlib:id",
    "id": "minecraft:chests/simple_dungeon"
  },
  "ops": [
    {
      "type": "arcanumlib:modify_pool",
      "poolSelector": {
        "type": "arcanumlib:any"
      },
      "ops": [
        {
          "type": "arcanumlib:modify_entry",
          "entrySelector": {
            "type": "arcanumlib:item",
            "item": "minecraft:string"
          },
          "ops": [
            {
              "type": "arcanumlib:replace_entry",
              "entry": {
                "type": "minecraft:item",
                "name": "minecraft:leather"
              }
            }
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
      "poolSelector": {
        "type": "arcanumlib:any"
      },
      "ops": [
        {
          "type": "arcanumlib:modify_entry",
          "entrySelector": {
            "type": "arcanumlib:weight_share",
            "min": 0.0,
            "max": 0.01
          },
          "ops": [
            {
              "type": "arcanumlib:remove_entry"
            }
          ]
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
      "poolSelector": {
        "type": "arcanumlib:any"
      },
      "ops": [
        {
          "type": "arcanumlib:modify_entry",
          "entrySelector": {
            "type": "arcanumlib:any"
          },
          "ops": [
            {
              "type": "arcanumlib:remove_function",
              "functionTypes": [
                "minecraft:enchanted_count_increase"
              ]
            }
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
      "poolSelector": {
        "type": "arcanumlib:any"
      },
      "ops": [
        {
          "type": "arcanumlib:modify_entry",
          "entrySelector": {
            "type": "arcanumlib:any"
          },
          "ops": [
            {
              "type": "arcanumlib:modify_function",
              "functionSelector": {
                "type": "arcanumlib:match",
                "function": {
                  "function": "minecraft:set_count",
                  "count": 3.0
                }
              },
              "ops": [
                {
                  "type": "arcanumlib:replace_function",
                  "function": {
                    "function": "minecraft:set_count",
                    "count": 5.0,
                    "add": true
                  }
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
      "poolSelector": {
        "type": "arcanumlib:any"
      },
      "ops": [
        {
          "type": "arcanumlib:modify_entry",
          "entrySelector": {
            "type": "arcanumlib:any"
          },
          "ops": [
            {
              "type": "arcanumlib:modify_function",
              "functionSelector": {
                "type": "arcanumlib:has_condition",
                "conditionType": "minecraft:random_chance"
              },
              "ops": [
                {
                  "type": "arcanumlib:modify_condition",
                  "conditionSelector": {
                    "type": "arcanumlib:condition_type",
                    "conditionType": "minecraft:random_chance"
                  },
                  "ops": [
                    {
                      "type": "arcanumlib:remove_condition"
                    }
                  ]
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
      "poolSelector": {
        "type": "arcanumlib:name",
        "name": "my_pool"
      },
      "ops": [
        {
          "type": "arcanumlib:modify_condition",
          "conditionSelector": {
            "type": "arcanumlib:any"
          },
          "ops": [
            {
              "type": "arcanumlib:remove_condition"
            }
          ]
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

## 19. Semantic Boundaries and Pitfalls

1. **Weights use a static measure**: `total_weight` / `weight_in_range` / `weight_share` exclude `quality × luck`,
   do not filter by entry conditions, and `alternatives` sums every branch. **Do not use them to reproduce the exact probability of a given roll**;
   they are suited to coarse screening such as "pick the small pools / pick the rare entries".
2. **Unnamed pools never match `name`**: most vanilla pools have no name.
3. **`tag` and `item_with_tag` are not the same thing**: the former matches **`minecraft:tag` entries**, the latter matches
   `minecraft:item` entries and checks whether their item belongs to the tag. Vanilla tables are almost entirely the latter.
4. **Composite entries have no `weight` / `quality` / `functions`**: `set_weight`, `set_quality`,
   `set_*function` and `remove_function` skip `alternatives`/`sequence`/`group`;
   `conditions`, on the other hand, exist on every entry.
5. **When rolling, `alternatives` only walks the first branch whose condition passes**: so when you add conditions to
   its children, or judge with `has_child`, you must be aware that only one branch is actually effective at runtime.
6. **Order is undefined for equal priorities**: please give multiple injectors on the same table different priorities.
7. **`match` templates are not validated**: a misspelled key fails silently.
8. **`replace_*` performs a deep copy**: every replacement site re-decodes its own independent instance, so later changes never bleed into each other;
   the cost is one codec round trip under `RegistryOps` (which only happens during the apply stage, not while rolling).
9. **A negative number in `set_weight` / `set_quality` means "do not change"**: this keeps the legacy semantics, so old json can be moved over directly;
    if you really want to set 0, write 0.
10. **`LootTable` has no `conditions`**: so there is no table-level `set_condition` / `add_condition` / `remove_condition`.
11. **Nothing in vanilla ever calls `LootTable.freeze()`**: but we modify AT-opened fields directly, which bypasses its
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
  "entrySelector": {
    "type": "arcanumlib:item",
    "item": "minecraft:gunpowder"
  },
  "ops": [
    {
      "type": "arcanumlib:add_condition",
      "conditions": [
        {
          "condition": "arcanumlib:chance_modifiers",
          "base_chance": 0.5,
          "chance_modifiers": [
            {
              "type": "arcanumlib:difficulty_factor",
              "hard": 1.5
            }
          ]
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

- A selector over a composite's children **only sees that one level**: go deeper by nesting `modify_child`; the order of `alternatives` children **is** the priority (that is what `move_child` changes).

- When you change a composite's children, the pre-composed container inside it (`composedChildren`) has to be recomputed too; replacing only the `children` field will not affect drops. This mod handles it in `CompositeEntryBaseMixin`; third-party code that touches children itself must do the same.
