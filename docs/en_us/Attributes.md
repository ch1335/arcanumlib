# Attributes provided by this mod

ArcanumLib provides 4 **player attributes**, all with a default base value of `0` — in other words,
**they do nothing at all by default**. You have to raise them with attribute modifiers, the `/attribute` command, or item attributes.

## Overview

| Attribute id | Name | Type | Base value | Range | Effect |
|---|---|---|---|---|---|
| `arcanumlib:seeking` | Seeking | percentage | 0 | 0 ~ 100 | Raises the mob spawn weight of **rare mobs** |
| `arcanumlib:fortune` | Fortune | numeric | 0 | 0 ~ 100 | Added to the effective level of the **Fortune** enchantment |
| `arcanumlib:looting` | Looting | numeric | 0 | 0 ~ 100 | Added to the effective level of the **Looting** enchantment |
| `arcanumlib:bonus_loot` | Bonus Loot | percentage | 0 | 0 ~ 20 | **Rolls entity drops and chest loot extra times** by the ratio |

- All four are attached to `minecraft:player`, and all three are **synced attributes** (the client reads the same value);
- The lower end of the range is 0 for all of them, so they can **only be raised, never lowered**.

---

## Seeking (`arcanumlib:seeking`)

During mob spawn, the weight of **rare** mobs is multiplied by `1 + Seeking value`.

- **What counts as rare**: the mob's weight share in the whole spawn table is < the threshold (default **2%**, server config).
  The weight share is scaled by **weight × average count per spawn**, so that a mob which "spawns 4 at a time" is not underestimated.
- **Which player's value is used**: the **highest** one among all players within **128 blocks** (128 blocks is the vanilla spawn distance).
- **The bonus ratio is not capped**: the formula itself has no upper limit; it is only clamped by the attribute range `0~100` — that is, at most ×101.
- **How to turn it off**: set the threshold in the config to `0`.

> **It is an integer weight**: the bonus is `round(base weight × Seeking value)`.
> So for a mob with a very low base weight (for example weight 1), you only see a change once the Seeking value reaches `0.5` or more — anything below `0.5` is rounded away to 0, which means no effect at all.
> The larger the weight, the smoother it behaves.

---

## Fortune / Looting (`arcanumlib:fortune` / `arcanumlib:looting`)

The attribute value is rounded to an integer and **added directly to the enchantment level**:

| Attribute | Vanilla drop mechanics affected |
|---|---|
| `arcanumlib:fortune` | `apply_bonus` (Fortune adds to the drop count), `table_bonus` (looks up a probability table by level) |
| `arcanumlib:looting` | `enchanted_count_increase` (adds to the count), `random_chance_with_enchanted_bonus` (adds to the chance) |

Which entity is used: **entity drops** use the killer (`ATTACKING_ENTITY`), **block drops** use the mining player (`THIS_ENTITY`).

These two attributes also affect this mod's own [chance conditions](Loot-Conditions.md) as a side effect
(the `tool_enchantment_level` modifier inside `arcanumlib:chance_modifiers`) — because they go through the same event.

> **Mind the semantics**: this **adds levels directly, with a threshold of 0**. So **attribute 3 + an unenchanted pickaxe** is equivalent to **Fortune 3**.
> If what you want is "only amplify an existing enchantment" (no effect when there is no enchantment), you need an extra check; that is currently **not** the semantics.

---

## Bonus Loot (`arcanumlib:bonus_loot`)

After a loot table has been rolled, the **ratio** from the attribute makes the **same table** roll again several times, and the result is appended to the original one.

- **The attribute value is the ratio**: `0.25` means 25% more (shown as `+25%` in the attribute screen). The integer part rolls whole extra copies; the fraction rolls one more copy, splits it into single items and samples by the remaining fraction;
- **Entity drops and chest loot by default**: decided by the loot table id **path prefix**, `entities/` and `chests/` unless changed in the config;
- **Which entity is read**: the killer (`ATTACKING_ENTITY`) first, otherwise `THIS_ENTITY` (the one who opened the chest). With neither (a plain block drop, say) the ratio is 0 and nothing is boosted;
- **Capped at 20**: the attribute range is `0 ~ 20`, so at most 20 extra copies;
- **It re-rolls instead of copying**: chance entries differ on every roll, so the extra copies really are rolled again; the roll shares the same `RandomSource` as the first one, so it stays reproducible.

> **How it differs from an injector**: an injector changes the **structure of the table** (add pools, add entries, change weights — see [Loot Injector](Loot-Injector.md));
> bonus loot leaves the table alone and just **rolls it more times**, so it coexists with global loot modifiers and chance entries by nature.

### Event: `BonusLootCheckEvent`

Fired while deciding whether a table should get the boost - **it outranks the config**: a listener can force it on (tables the config does not list) or force it off (tables the config does list); when nobody changes it the config prefix rule decides.

| Method | Description |
|---|---|
| `getLootContext()` | The context of this roll |
| `getLootTableId()` | The table that is producing the loot |
| `isConfigAllowed()` | What the config prefix rule said (for reference) |
| `getResult()` / `setResult(boolean)` | The final answer; starts out equal to `isConfigAllowed()` |
| `allow()` / `deny()` | Force it on / force it off |

```java
@SubscribeEvent
public static void onBonusLootCheck(BonusLootCheckEvent event) {
    // let block drops get the boost too (the config only lists entities and chests)
    if (event.getLootTableId().getPath().startsWith("blocks/")) {
        event.allow();
    }
}
```

### Event: `ModifyBonusLootEvent`

Fired right before the boost happens (Java side, on `NeoForge.EVENT_BUS`) so you can **change the ratio** or cancel it:

| Method | Description |
|---|---|
| `getLootContext()` | The context of this roll (entities, block state, tool, ...) |
| `getLootTableId()` | The table that is producing the loot |
| `getOriginalRatio()` / `getRatio()` / `setRatio(float)` | Original ratio / current ratio / set the ratio (0 or less means no boost) |
| `setCanceled(true)` | Cancels this boost (`ICancellableEvent`) |

```java
@SubscribeEvent
public static void onModifyBonusLoot(ModifyBonusLootEvent event) {
    if (event.getLootTableId().getPath().startsWith("chests/")) {
        event.setRatio(event.getRatio() * 2.0F);
    }
}
```

---

## How to raise the values

### Command (for debugging)

```
/attribute @s arcanumlib:seeking base set 2
/attribute @s arcanumlib:fortune base set 3
/attribute @s arcanumlib:fortune modifier add mymod:fortune_bonus 3 add_value
```

### Item attributes (the component in an item json)

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

`slot` may be omitted (defaults to `any`); `operation` is one of `add_value` / `add_multiplied_base` / `add_multiplied_total`.

### Code

```java
AttributeInstance instance = player.getAttribute(ArcanumAttributes.FORTUNE);
instance.addOrUpdateTransientModifier(new AttributeModifier(
        ResourceLocation.fromNamespaceAndPath("mymod", "fortune_bonus"),
        3.0D,
        AttributeModifier.Operation.ADD_VALUE));
```

---

## Config

Server config `serverconfig/arcanumlib/attribute.toml` (in a singleplayer world it lives under `saves/<world>/serverconfig/`):

| Key | Default | Description |
|---|---|---|
| `spawn.rare_spawn_weight_threshold` | `0.02` | Rarity threshold (weight share, 0~1); setting it to `0` turns Seeking off |
| `bonus_loot.bonus_loot_prefixes` | `["entities/", "chests/"]` | Loot table id path prefixes that get bonus loot (entity drops and chests by default) |

---


## Related source

| Content | File |
|---|---|
| Attribute definitions | `modules/attribute/ArcanumAttributes.java` |
| Attaching to the player | `modules/attribute/AttributeEvents.java` |
| Seeking spawn logic | `modules/attribute/SpawnEvents.java` |
| Fortune / Looting level bonus | `modules/attribute/EnchantedLootEvents.java` |
| Server config | `modules/attribute/AttributeConfig.java` |
| Enchantment level event (shared by Fortune/Looting and the chance conditions) | `events/EnchantedLootEvent.java` |
| Bonus loot logic | `modules/loot/bonusLoot/BonusLoot.java` |
| The Java event that can change the ratio | `modules/loot/bonusLoot/event/ModifyBonusLootEvent.java` |
| Event that decides whether a table gets the boost (outranks the config) | `modules/loot/bonusLoot/event/BonusLootCheckEvent.java` |
| Injected into `LootTable` | `mixins/LootTableBonusLootMixin.java` |
