# Commands

All commands of this mod live under `/arcanumlib` and require **permission level 2** (operator).

## Table of contents

1. [Export Loot Tables](#1-export-loot-tables)
2. [Simulate Loot Tables](#2-simulate-loot-tables)

---

## 1. Export Loot Tables

```
/arcanumlib export lootTable
```

Exports the loot tables as json, which is handy for diffing or for writing documentation. Files go under the **game directory** (in a dev environment that is `run/`; the path comes from NeoForge's runtime directory, not from the working directory):

| Directory | Content | Layout |
|---|---|---|
| `arcanumlib/exportedLootTable/modified/` | The tables **in memory** at the moment the command runs, i.e. after the injectors and other mods have acted | `<namespace>/<path>.json` |
| `arcanumlib/exportedLootTable/original/` | A **byte-for-byte copy of the original files**, with no runtime changes | `<namespace>/loot_table/<path>.json` |

### Notes

- `modified/` contains **only the tables an injector changed** (filtered by the applied record), not everything;
- `original/` copies **every** original file from the datapack, so the two directories usually hold **different numbers** of files;
- Run it **after** the server/world has started (that is when the injectors are applied); running it after a `/reload` works just as well;
- A short summary is sent to chat and to the log.

## 2. Simulate Loot Tables

```
/arcanumlib loot_simulation <lootTable> [times] [param set]
```

Repeatedly generates the same loot table (1000 times when omitted, capped at 100000) and aggregates by item:

| Column | Meaning |
|---|---|
| Item | The item name; each row gets its own colour |
| Count | How many were obtained across all runs |
| Ratio | count / total items |
| Chance | count / attempts |

### How the context is filled

The **kind of the parameter set** decides who gets filled in (when the set is omitted, the table's own set is used):

| Kind | ORIGIN | THIS_ENTITY | Others |
|---|---|---|---|
| Block | The block the player **looks at** | The player | `ATTACKING_ENTITY` = the player; `BLOCK_STATE` / `BLOCK_ENTITY` come from that block |
| Entity | The position of the entity looked at | The entity the player looks at | `DAMAGE_SOURCE` = player attack; `ATTACKING_ENTITY` / `DIRECT_ATTACKING_ENTITY` / `LAST_DAMAGE_PLAYER` = the player |
| Generic | The entity looked at first, else the block looked at, else the player's feet | That entity when there is one, otherwise the player | Same as above |
| Others | The player position | The player | Attackers and the like go to the player |

### Notes

- Block kind = `chest` / `block` / `block_use` / `hit_block` / `advancement_location`; entity kind = `entity` / `enchanted_damage`; generic kind = `generic`;
- There are **23** parameter sets to choose from: `empty`, `chest`, `command`, `selector`, `fishing`, `entity`, `equipment`, `archaeology`, `gift`, `barter`, `vault`, `advancement_reward`, `advancement_entity`, `advancement_location`, `block_use`, `generic`, `block`, `shearing`, `enchanted_damage`, `enchanted_item`, `enchanted_location`, `enchanted_entity`, `hit_block` (all in the `minecraft:` namespace);
- Only the parameters the set **allows** are filled in, so it never fails because "this set does not accept that parameter";
- If a required parameter is still missing the command fails with an explanation;
- `<lootTable>` supports tab completion (with or without the namespace);
- Every run uses a fresh random seed, so results are not replicated.
