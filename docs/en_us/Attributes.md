# Attributes provided by this mod

ArcanumLib provides 3 **player attributes**, all with a default base value of `0` — in other words,
**they do nothing at all by default**. You have to raise them with attribute modifiers, the `/attribute` command, or item attributes.

## Overview

| Attribute id | Name | Type | Base value | Range | Effect |
|---|---|---|---|---|---|
| `arcanumlib:seeking` | Seeking | percentage | 0 | 0 ~ 100 | Raises the mob spawn weight of **rare mobs** |
| `arcanumlib:fortune` | Fortune | numeric | 0 | 0 ~ 100 | Added to the effective level of the **Fortune** enchantment |
| `arcanumlib:looting` | Looting | numeric | 0 | 0 ~ 100 | Added to the effective level of the **Looting** enchantment |

- All three are attached to `minecraft:player`, and all three are **synced attributes** (the client reads the same value);
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

---

## Lang keys

| Key | Chinese | English |
|---|---|---|
| `attribute.name.arcanumlib.seeking` | 寻觅 | Seeking |
| `attribute.name.arcanumlib.fortune` | 时运 | Fortune |
| `attribute.name.arcanumlib.looting` | 抢夺 | Looting |

There are also three corresponding `….desc` keys (short descriptions, for tools that display the names).

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
