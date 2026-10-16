# Attributes provided by this mod

ArcanumLib provides 4 **player attributes**, all with a default base value of `0` — in other words,
**they do nothing at all by default**. You have to raise them with attribute modifiers, the `/attribute` command, or item attributes.

## Table of Contents

1. [Overview](#overview)
2. [Seeking (`arcanumlib:seeking`)](#seeking-arcanumlibseeking)
3. [Fortune / Looting (`arcanumlib:fortune` / `arcanumlib:looting`)](#fortune--looting-arcanumlibfortune--arcanumliblooting)
4. [Bonus Loot (`arcanumlib:bonus_loot`)](#bonus-loot-arcanumlibbonus_loot)
5. [Attribute Adjustment](#attribute-adjustment)
6. [Events](#events)
7. [Config](#config)
8. [Related source](#related-source)

---

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

---

## Attribute Adjustment

An attribute adjustment applies a one-off delta at the moment an attribute value is read. It differs from
an attribute modifier in lifetime: a modifier is written into the attribute instance, is synced and is saved
with the world, while an adjustment writes no persistent state and applies only to that single read.

### How it works

Before reading certain attributes this mod fires `LivingGetAttributeAdjustmentEvent`; a listener writes its
into the carrier provided by the event, keyed by operation, and they take part in that attribute calculation. The
first three operations mean the same as for attribute modifiers; the last two are stages this mod adds. Neutral values:

| Operation | Meaning | Neutral value |
|---|---|---|
| `ADD_VALUE` | Added directly to the attribute base value | `0` |
| `ADD_MULTIPLIED_BASE` | Added on top of the adjusted base value | `0` |
| `ADD_MULTIPLIED_TOTAL` | Multiplies the final value | `1` |
| `ADD_FINAL` | Added right after the total multipliers | `0` |
| `SET_VALUE` | Replaces the value outright; `NaN` means "do not replace" | `NaN` |

The order is `ADD_VALUE` -> `ADD_MULTIPLIED_BASE` -> `ADD_MULTIPLIED_TOTAL` -> `ADD_FINAL` -> `SET_VALUE`: the
`ADD_VALUE` delta is part of the base value and therefore feeds every later stage, the `ADD_MULTIPLIED_BASE` delta
is added next and is affected by the total multipliers, and `SET_VALUE` depends on order (a later adjustment keeps
operating on its result) while the other operations do not. All of them happen before the attribute clamps the value.

### Contexts

The event carries a context object that describes the situation the read happens in, so a listener can decide whether and how much to adjust.

| Context | Situation | Carries |
|---|---|---|
| `DamageContext` | Reading damage multipliers, armor, armor toughness or the ApothicAttributes pierce, shred, crit chance, crit damage and projectile damage attributes during damage resolution | The victim, the damage source and the current damage value |
| `AttackingContext` | An attacker reading attack damage or crit damage | The attacker and the target |
| `LootInfoContext` | Reading attributes during loot resolution | The loot context of this roll |

### Reading values

To read an attribute value that includes temporary adjustments, use `AdjustmentHelper.getAttributeWithAdjusted`:
it registers the adjustment, reads the attribute and removes the adjustment again. When the read has to be done by
the caller (for example to keep another mod wrapper on that call), use the overload taking a `DoubleSupplier`.

```java
double armor = AdjustmentHelper.getAttributeWithAdjusted(entity, Attributes.ARMOR, new DamageContext(entity, source, damage));
```

### Instrumented attributes

| Attribute | Context | When |
|---|---|---|
| `arcanumlib:damage_multiplier` | `DamageContext` | When the damage correction bonus is calculated |
| `arcanumlib:damage_taken_multiplier` | `DamageContext` | When the incoming damage reduction is calculated |
| `minecraft:armor` | `DamageContext` | When armor reduction is calculated |
| `minecraft:armor_toughness` | `DamageContext` | When armor toughness reduction is calculated |
| `minecraft:attack_damage` | `AttackingContext` | Player attacks |
| `arcanumlib:fortune` | `LootInfoContext` | When adding the effective Fortune level during loot resolution |
| `arcanumlib:looting` | `LootInfoContext` | When adding the effective Looting level during loot resolution |
| `arcanumlib:bonus_loot` | `LootInfoContext` | Rolling bonus loot |
| `apothic_attributes:armor_pierce` | `DamageContext` | When armor pierce is calculated (requires ApothicAttributes to be loaded) |
| `apothic_attributes:armor_shred` | `DamageContext` | When armor shred is calculated (requires ApothicAttributes to be loaded) |
| `apothic_attributes:prot_pierce` | `DamageContext` | When protection pierce is calculated (requires ApothicAttributes to be loaded) |
| `apothic_attributes:prot_shred` | `DamageContext` | When protection shred is calculated (requires ApothicAttributes to be loaded) |
| `apothic_attributes:crit_chance` | `DamageContext` | When the critical hit decision is made (requires ApothicAttributes to be loaded) |
| `apothic_attributes:crit_damage` | `DamageContext` / `AttackingContext` | When the crit strike is resolved (DamageContext) and when a vanilla critical hit is decided (AttackingContext) (requires ApothicAttributes to be loaded) |
| `apothic_attributes:projectile_damage` | `DamageContext` | When projectile damage is calculated (requires ApothicAttributes to be loaded) |

Reads of any other attribute do not go through this mechanism.

### Example

A listener tells the situations apart through `IContext`. The example below means: when a wooden sword hits a zombie, the attack damage gets a correction equal to +20.

```java
@SubscribeEvent
public static void onAdjustAttribute(LivingGetAttributeAdjustmentEvent event) {
    // only care about the attack damage attribute
    if (event.getAttribute().value() != Attributes.ATTACK_DAMAGE.value()) {
        return;
    }
    // the context has to be "an attacker reading attack damage"
    if (!(event.getContext() instanceof AttackingContext context)) {
        return;
    }
    LivingEntity attacker = context.attacker();
    if (attacker.getMainHandItem().is(Items.WOODEN_SWORD) && context.target().getType() == EntityType.ZOMBIE) {
        // ADD_VALUE matches the modifier operation of the same name: equal to +20 attack damage
        event.getAdjustment().add(AttributeAdjustment.Operation.ADD_VALUE, 20.0D);
    }
}
```

- An `ADD_VALUE` adjustment is part of the base value and therefore also feeds the two later stages;
- The example only applies in the **player attack** situation (`AttackingContext`), so it leaves the zombie own attributes untouched.

### Notes

- Adjustments are neither saved nor synced, and they stop applying when the read finishes;
- An attribute calculation fires the event again on every recomputation, so the adjustment is asked for anew each time and may be non-deterministic (for example it can rely on random numbers or on state that changes over time);
- A listener may add or remove attribute modifiers from the event callback, but doing so serves no purpose beyond making things more complicated.

---

## Events

Every event this mod fires is collected here; each one says which part of the mod it belongs to.

### `BonusLootCheckEvent` (bonus loot)

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

### `ModifyBonusLootEvent` (bonus loot)

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

### `LivingAttributeChangeEvent` (attribute value change)
Fired whenever the value of an attribute instance **really changes** (Java side, `NeoForge.EVENT_BUS`).
It covers every operation that makes an attribute recompute: changing the base value, adding / removing / updating modifiers, entity sync and loading from NBT.

| Method | Description |
|---|---|
| `getLivingEntity()` | The entity the attribute belongs to |
| `getAttributeInstance()` | The instance that changed (attribute type, base value and modifier list are all in it) |
| `getNewValue()` | The value after the change (modifiers already applied) |

- **Read only**: it cannot be cancelled and the value cannot be changed - use attribute modifiers for that;
- **The same value fires once**: only a real change is posted, so sync and NBT loading do not spam it;

```java
@SubscribeEvent
public static void onAttributeChanged(LivingAttributeChangeEvent event) {
    // only care about Fortune
    if (event.getAttributeInstance().getAttribute().value() != ArcanumAttributes.FORTUNE.value()) {
        return;
    }

    // the new Fortune value - do something with it
    double newValue = event.getNewValue();
}
```

---


### `LivingGetAttributeAdjustmentEvent` (attribute adjustment)

Fired right before the value of a certain attribute is read (Java side, on `NeoForge.EVENT_BUS`). A listener
writes deltas by operation; they only apply to that single read. See [Attribute Adjustment](#attribute-adjustment).

| Method | Description |
|---|---|
| `getAttribute()` | The attribute being read |
| `getOwner()` | The entity the attribute belongs to |
| `getContext()` | The context of this read |
| `getAdjustment()` | The adjustment carrier; read or accumulate by operation (`get` / `set` / `add`) |

- It cannot be cancelled: deltas are accumulated by operation rather than vetoed as a whole;
- It only fires for instrumented attribute reads; other reads never see this event.

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
| Attribute value change event (fires for any value change) | `modules/attribute/events/LivingAttributeChangeEvent.java` |
| Where it fires: hooked on `AttributeInstance#setDirty` | `mixins/AttributeInstanceMixin.java` |
| The adjustment carrier (one field per operation) | `modules/attribute/AttributeAdjustment/AttributeAdjustment.java` |
| The entry point for reading adjusted values | `modules/attribute/AttributeAdjustment/AdjustmentHelper.java` |
| The adjustment event | `modules/attribute/events/LivingGetAttributeAdjustmentEvent.java` |
| Where it is wired in: the three stages of an attribute calculation | `mixins/AttributeInstanceMixin.java` |
| Where it is wired in: the armor / protection formulas of ApothicAttributes | `mixins/apothic_attributes/ALCombatRulesMixin.java` |
| Where it is wired in: the critical strike resolution of ApothicAttributes | `mixins/apothic_attributes/AttributeEventsMixin.java` |