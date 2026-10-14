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
| **All-of entry** | `modules/loot/lootEntry/` | Registers `arcanumlib:all_of` (the opposite of vanilla `alternatives` — **every child is produced**) and `arcanumlib:summon_mob` (**produces no items; spawns an entity at ORIGIN**) |
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

One example appends an entry and one modifies an existing entry, each in three equivalent forms - **datapack / KubeJS / Java** - see [Loot Injector](docs/en_us/Loot-Injector.md).

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
