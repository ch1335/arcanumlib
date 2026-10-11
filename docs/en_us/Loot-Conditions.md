# Loot Conditions

All the loot conditions provided by ArcanumLib are collected here. They are all **vanilla `LootItemCondition`s**,
so they can be written into the `conditions` of any loot table, and the injector system can also use them as ordinary conditions.

> This document is a **container**: whenever a new condition is added later, just add a row to the "Condition list" below
> and append a detailed section. Currently there are 2 entries.

## Condition list

| Condition id | One-line description | Details |
|---|---|---|
| [`arcanumlib:chance_modifiers`](#arcanumlibchance_modifiers) | Chance modifier chain: starts from `base_chance`, passes through a list of modifiers in order, then rolls against the final chance | [Next section](#arcanumlibchance_modifiers) |
| [`arcanumlib:has_stage`](#arcanumlibhas_stage) | Stage condition: with AStages installed, only a player / server holding a matching stage passes | [Last section](#arcanumlibhas_stage) |

---

## `arcanumlib:chance_modifiers`

It starts from `base_chance`, runs through the list of modifiers you give it **in order**, and makes a single random check with the final chance.

The problem it solves is what vanilla `minecraft:random_chance` cannot do: **the chance floats with context** —
bonusing by game difficulty, by the Looting level on the tool, by the attacker's Luck attribute — and these can be **chained arbitrarily**.

- Registry: `arcanumlib:chance_modifiers` under `minecraft:loot_condition_type`
- Source: `modules/loot/lootCondition/`
- Package path: `com.chen1335.arcanumlib.modules.loot.lootCondition`

> **Strict json parsing**: like the injector, it goes through `SimpleJsonResourceReloadListener` (`lenient = false`),
> so **you cannot write `//` comments and you cannot have trailing commas**. Every example in this document is strictly valid json.

### Table of contents

1. [Minimal example](#1-minimal-example)
2. [Evaluation model](#2-evaluation-model)
3. [Fields](#3-fields)
4. [The nine modifiers](#4-the-nine-modifiers)
5. [The two operation semantics (the easiest place to go wrong)](#5-the-two-operation-semantics-the-easiest-place-to-go-wrong)
6. [Entity source: EntitySource](#6-entity-source-entitysource)
7. [EnchantedLootEvent: letting outside code change the enchantment level](#7-enchantedlootevent-letting-outside-code-change-the-enchantment-level)
8. [Behavior when the tool / entity is unavailable](#8-behavior-when-the-tool--entity-is-unavailable)
9. [Common combination recipes](#9-common-combination-recipes)
10. [Using it in the injector](#10-using-it-in-the-injector)
11. [Building it with datagen](#11-building-it-with-datagen)
12. [Extending: writing your own modifier](#12-extending-writing-your-own-modifier)
13. [Pitfalls and notes](#13-pitfalls-and-notes)

---

### 1. Minimal example

Because it is just an ordinary loot condition, it can be written into the `conditions` of **any** loot table:

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

How to read it: base 5%, each level of **Fortune** on the tool makes the chance **× (1 + 0.02)**, and finally it is clamped to `[0, 0.5]`.

- When `base_chance` is omitted it is `1.0`;
- When `chance_modifiers` is omitted it is an empty chain, equivalent to `minecraft:random_chance`.

The simplest form (equivalent to "always passes"):

```json
{ "condition": "arcanumlib:chance_modifiers" }
```

---

### 2. Evaluation model

```
chance = base_chance
for (modifier : chance_modifiers) {
    chance = modifier.apply(chance, context)
}
passes = random.nextFloat() < chance
```

Three key points:

1. **The base chance is a field of the condition itself**, not a modifier at the head of the chain — the chain is only responsible for "processing"; the starting point is given by `base_chance`.
2. **They are applied in list order**, and the output of the previous one is the input of the next. So order changes the result (for example "multiply then add" differs from "add then multiply").
3. **The chance is not clamped**: once the chain is computed it goes straight into the check, so
   - `> 1`: `nextFloat()` is always `< 1`, so it **always passes**;
   - `<= 0`: **never passes**;
   - nobody stops a negative number appearing in the middle of the chain.
   If you want to restrict the range, add a `clamp` yourself (usually at the end of the chain).

`ChanceModifierCondition` also provides an entry point that does not roll the dice, which is handy for reading the "final chance" externally or while debugging:

```java
double chance = new ChanceModifierCondition(0.25D, List.of(...)).resolveChance(context);
```

---

### 3. Fields

| Field | Type | Required | Default | Description |
|---|---|---|---|---|
| `condition` | string | ✅ | — | Fixed to `arcanumlib:chance_modifiers` |
| `base_chance` | double | | `1.0` | Starting point of the chain |
| `chance_modifiers` | array | | `[]` | List of modifiers, applied in order |

---

### 4. The nine modifiers

Registry: `arcanumlib:chance_modifier`. Each entry is `{ "type": "arcanumlib:<id>", ... }`.

| id | Field | Semantics |
|---|---|---|
| `add` | `value` (required) | `chance + value` (may be negative, used to reduce) |
| `multiply` | `factor` (required) | `chance × factor` |
| `set_chance` | `value` (required) | **Directly sets** the value, discarding the previous result |
| `clamp` | `min` (default `0.0`), `max` (default `1.0`) | Clamps to `[min, max]` |
| `invert` | — | `1 - chance` (complement) |
| `difficulty_factor` | `operation` (default `multiply`), `peaceful`, `easy`, `normal`, `hard` (all optional) | Takes the tier value for the current difficulty and processes with `operation` |
| `tool_enchantment_level` | `enchantment` (required), `operation` (default `multiply`), `per_level` (default `0.05`) | Processes by the level of some enchantment on the tool |
| `entity_attribute` | `attribute` (required), `entity` (default `attacking_entity`), `operation` (default `multiply`), `per_point` (default `0.05`) | Processes by one attribute of some entity |
| `stage` | `stage` (required, may be a plain string), `desc`, `check_server_stages`, `success`, `fail` | Picks one of two chains by AStages stage: on a match the `success` chain runs, otherwise the `fail` chain |

#### 4.1 `add` — add a value

```json
{ "type": "arcanumlib:add", "value": 0.1 }
```
`0.2 → 0.3`. A negative value can be used to reduce: `{ "type": "arcanumlib:add", "value": -0.05 }`.

#### 4.2 `multiply` — multiply by a factor

```json
{ "type": "arcanumlib:multiply", "factor": 2.0 }
```
`0.1 → 0.2`. Multiplying by 0 means "never passes", multiplying by 2 means "double".

#### 4.3 `set_chance` — set the value directly

```json
{ "type": "arcanumlib:set_chance", "value": 0.08 }
```
Placed at the head of the chain it sets the initial value (in which case it duplicates `base_chance` and is usually unnecessary);
**placed in the middle of the chain it discards every previous processing result**, so it is generally only used for "if some condition holds, force it to a fixed chance".

#### 4.4 `clamp` — clamp the range

```json
{ "type": "arcanumlib:clamp", "min": 0.0, "max": 1.0 }
```
Omitting the fields gives the defaults above. Because of its "safety net" nature it is usually placed at the **end of the chain**, so that the earlier processing does not produce a negative number or a value above 1.

#### 4.5 `invert` — take the complement

```json
{ "type": "arcanumlib:invert" }
```
`0.95 → 0.05`. Typical use: turn "95% chance of not dropping" into "5% chance of dropping".

#### 4.6 `difficulty_factor` — process by game difficulty

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
On Hard the chance is ×1.3, on Normal ×1.15, and on Peaceful/Easy it is unchanged.

- `operation` defaults to `multiply`; with `add` the tier value is "how much to add";
- **Tiers may be left empty**: an empty tier is treated as the **neutral value** of the `operation` (1.0 for multiply / 0.0 for add), i.e. "does not change the chance on that difficulty". So if you only care about Hard you can write just `hard`:

```json
{ "type": "arcanumlib:difficulty_factor", "hard": 1.5 }
```

#### 4.7 `tool_enchantment_level` — process by the tool's enchantment level

```json
{
  "type": "arcanumlib:tool_enchantment_level",
  "enchantment": "minecraft:looting",
  "operation": "multiply",
  "per_level": 0.05
}
```
Each level of Looting makes the chance ×(1 + 0.05), i.e. Looting III → ×1.15. With `add` it is `chance + level × per_level`.

**It reads `LootContextParams.TOOL`**, so:
- It applies both to block drops (`THIS_ENTITY` is the mining player and `TOOL` is the tool in hand) and to entity drops (`TOOL` is the killer's weapon);
- If the tool cannot be obtained (for example a chest loot table) → it is treated as **level 0** → **the chance is not changed** (see section 8).

> Enchantments are a **data-driven registry**, so what is stored here is a `ResourceKey` and the `Holder` is fetched at runtime;
> therefore the json contains an enchantment id (`"minecraft:looting"`), and unlike attributes it cannot be a `Holder` directly.

#### 4.8 `entity_attribute` — process by an entity attribute

```json
{
  "type": "arcanumlib:entity_attribute",
  "attribute": "minecraft:generic.luck",
  "entity": "attacking_entity",
  "operation": "multiply",
  "per_point": 0.01
}
```
Every 1 point of Luck on the attacker makes the chance ×(1 + 0.01). With `add` it is `chance + attribute value × per_point`.

- For `entity` see section 6; it defaults to `attacking_entity`;
- It reads `LivingEntity.getAttributes().getInstance(attribute)`, and **returns unchanged when the entity does not have that attribute** (it does not treat it as 0), so a missing attribute from some mod will not accidentally zero out the chance.

#### 4.9 `stage` — pick one of two chains by stage (requires AStages)

The stage is checked first: on a match the `success` chain is applied in order, otherwise the `fail` chain is. Both chains are ordinary modifier lists.

```json
{
  "type": "arcanumlib:stage",
  "stage": { "pattern": "chapter_*", "type": "wildcard" },
  "desc": "mymod.stage.chapter",
  "check_server_stages": false,
  "success": [
    { "type": "arcanumlib:multiply", "factor": 2.0 }
  ],
  "fail": [
    { "type": "arcanumlib:set_chance", "value": 0.05 }
  ]
}
```

`stage` / `desc` / `check_server_stages` behave exactly as in the `arcanumlib:has_stage` condition (`stage` accepts a plain string or a `pattern`/`type` object). `success` / `fail` may be omitted; an omitted chain is empty and changes nothing.

> **Without AStages this counts as "no match"**, so the `fail` chain is always the one applied.

In ALI it renders as a scope line carrying the success chain plus an "otherwise" line carrying the fail chain; **an empty chain hides its whole line**:

```
Stage modifier :
    ->Player stage : Chapter 1
        ->Multiplier : 2.0
    ->Otherwise :
        ->Set chance : 0.05
```

(`Stage modifier :`, `Player stage : %s` / `World stage : %s` and `Otherwise :` are localization keys provided by this mod; the `->` is the tree arrow ALI draws itself.)
---

### 5. The two operation semantics (the easiest place to go wrong)

The `add` / `multiply` enum means **different things** in different modifiers; in the code it is split into two methods:

| Method | ADD | MULTIPLY | Who uses it |
|---|---|---|---|
| `apply(chance, value)` — **absolute semantics** | `chance + value` | `chance × value` | `difficulty_factor` (the tier value itself is the multiplier/addend) |
| `applyScaled(chance, delta)` — **delta semantics** | `chance + delta` | `chance × (1 + delta)` | `tool_enchantment_level`, `entity_attribute` (delta = level×per_level / attribute value×per_point) |

Plug in numbers to see the difference:

- `difficulty_factor` + `multiply` + `hard: 1.3` → chance **×1.3**
- `tool_enchantment_level` + `multiply` + `per_level: 0.05` + level 3 → delta = 0.15 → chance **×(1 + 0.15) = ×1.15**

The design reason is that `per_level` / `per_point` are **deltas**: the natural reading of `per_level: 0.05` is "+5% per level", not "multiply by 0.05 per level" (the latter would make the chance approach 0 as the level rises). Difficulty tiers, on the other hand, are naturally "multipliers", so they use absolute semantics.

---

### 6. Entity source: EntitySource

The `entity` field of `entity_attribute` has four values:

| Value | Which entity is used |
|---|---|
| `this_entity` | The protagonist of the context: on an **entity drop** it is the killed entity, on a **chest / spawner / archaeology** it is the player opening it |
| `attacking_entity` | The attacker (**default**, most commonly used) |
| `direct_attacking_entity` | The direct attacker (for example the arrow that was shot) |
| `last_damage_player` | The player who dealt the last damage |

So for "by the killer's attribute" write `attacking_entity`, and for "by the killed entity's attribute" (for example adjusting a mob's own drop by one of its attributes) write `this_entity`.

---

### 7. EnchantedLootEvent: letting outside code change the enchantment level

After counting the enchantment level, `tool_enchantment_level` **does not use it directly**; instead it first fires an event:

```java
int level = tool.getEnchantmentLevel(holder);
return EnchantedLootEvent.post(holder, context, level);
```

A listener can change this level, letting systems such as "equipment slots / entries / affixes" take part in the drop chance:

```java
@EventBusSubscriber(modid = MyMod.MODID)
public final class MyLooting {

    @SubscribeEvent
    public static void onEnchantedLoot(EnchantedLootEvent event) {
        if (!event.getEnchantment().is(Enchantments.LOOTING)) {
            return;
        }

        // Example: my mod gives the killer an extra +1 level of Looting
        int bonus = myBonusLootingLevel(event.getContext());
        event.setLevel(event.getLevel() + bonus);
    }
}
```

Event API:

| Method | Description |
|---|---|
| `getEnchantment()` | `Holder<Enchantment>`, the enchantment currently being read |
| `getContext()` | `LootContext`; fetch entities with `LootContextParams.THIS_ENTITY` / `ATTACKING_ENTITY` |
| `getLevel()` / `setLevel(int)` | The original level / change it to the value you want |

#### It affects more than this condition

The same event is also fired by **4 vanilla mechanics** (taken over by ArcanumLib's mixins), so listening once affects all of them at the same time:

| Vanilla mechanic | Class |
|---|---|
| `apply_bonus` (Fortune/Looting increases the drop count) | `ApplyBonusCount` |
| `table_bonus` (look up a chance table by level) | `BonusLevelTableCondition` |
| `enchanted_count_increase` (increase the count) | `EnchantedCountIncreaseFunction` |
| `random_chance_with_enchanted_bonus` (increase the chance) | `LootItemRandomChanceWithEnchantedBonusCondition` |

Together with `tool_enchantment_level` from this guide, that is **five trigger sources** in total. If you want "some equipment effect adds Looting", listening to this one event is enough.

---

### 8. Behavior when the tool / entity is unavailable

These are two **deliberate design decisions**, and also the place most likely to conflict with the "intuitive zeroing":

| Modifier | What is missing | Behavior |
|---|---|---|
| `tool_enchantment_level` | `TOOL` cannot be obtained, or the tool does not have that enchantment | Treated as **level 0** → **the chance is not changed** |
| `entity_attribute` | The entity cannot be obtained (the `entity` field is absent from the context), or the entity does not have that attribute | **Returned unchanged** → **the chance is not changed** |

In other words, both of them are "**change it if present, leave it alone if absent**", and neither will turn the chance into 0 or 1.

> Note that in `tool_enchantment_level`, "level 0" and "level is 0" are the same branch (`level <= 0 ? chance : ...`),
> so a tool that has the enchantment but at level 0 (which normally cannot happen) also leaves it unchanged.

---

### 9. Common combination recipes

#### 9.1 Base chance + Looting bonus + cap

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
Each level of Looting is +10% (multiply by `1 + 0.1×level`), up to a maximum of 60%.

#### 9.2 The higher the difficulty, the more likely it drops

```json
{
  "condition": "arcanumlib:chance_modifiers",
  "base_chance": 0.1,
  "chance_modifiers": [
    { "type": "arcanumlib:difficulty_factor", "easy": 0.5, "normal": 1.0, "hard": 2.0 }
  ]
}
```
On Easy the chance is ×0.5, on Normal it is unchanged, and on Hard ×2 (the Peaceful tier is left empty → unchanged).

#### 9.3 Floating with the attacker's Luck (unchanged when Luck is 0)

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

#### 9.4 Rewriting "95% chance of not dropping" as "5% drop"

```json
{
  "condition": "arcanumlib:chance_modifiers",
  "base_chance": 0.95,
  "chance_modifiers": [{ "type": "arcanumlib:invert" }]
}
```

#### 9.5 Fortune + Luck, both taking effect only when greater than 0

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

### 10. Using it in the injector

> For how to use the injector itself see [Loot Injector.md](Loot-Injector.md); this section only covers how to put them together.

The `conditions` / `add_condition` / `set_condition` of the injector system accept **ordinary vanilla condition json**,
so this condition can be written inside them as-is:

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

Attaching it to an existing entry with the injector's `modify_entry` works the same way:

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

> When the injector side only wants to test "does the entry have this condition attached", use the entry selector
> `{"type":"arcanumlib:has_condition","conditionType":"arcanumlib:chance_modifiers"}`.

---

### 11. Building it with datagen

```java
// The simplest form: builder(base chance, modifiers...)
LootItemCondition.Builder condition = ChanceModifierCondition.builder(0.25D,
        new ChanceModifier.ToolEnchantmentLevel(Enchantments.LOOTING, ChanceModifier.Operation.ADD, 0.02D),
        new ChanceModifier.EntityAttribute(Attributes.LUCK, ChanceModifier.EntitySource.THIS_ENTITY,
                ChanceModifier.Operation.ADD, 0.01D),
        new ChanceModifier.Clamp(0.0D, 0.9D),
        ChanceModifier.DifficultyFactor.multiply(1.0D, 1.0D, 1.15D, 1.3D));
```

- Every modifier has an `of(...)` factory method (`Add.of(0.1D)`, `Clamp.of(0.0D, 1.0D)`, `Invert.of()`…);
- `DifficultyFactor` additionally has two convenience factories, `multiply(peaceful, easy, normal, hard)` and `add(...)`,
  which are **cleaner than hand-rolling `Optional`s** (the four tiers in `PEACEFUL / EASY / NORMAL / HARD` order);
- Omitting the chain is `ChanceModifierCondition.builder()` (equivalent to always passing).

The injector's template base class `LootInjectorTemplate` uses it to put a chance on a whole drop:

```java
@Override
protected LootItemCondition.Builder chanceCondition(Drop drop) {
    return ChanceModifierCondition.builder(drop.chance(),
            new ChanceModifier.ToolEnchantmentLevel(Enchantments.LOOTING, ChanceModifier.Operation.ADD, drop.perLooting()),
            new ChanceModifier.Clamp(0.0D, 0.9D));
}
```

---

### 12. Extending: writing your own modifier

The interface is **open** to other mods: `ChanceModifier` is an ordinary interface (not sealed); just implement it and register it into ArcanumLib's registry.

#### 12.1 Three public APIs

| Purpose | API |
|---|---|
| Implement the interface | `ChanceModifier`: `apply(double chance, LootContext context)` + `getType()` |
| Wrap a codec | `ChanceModifierType` (a container for `MapCodec`) |
| Registry key | `ChanceModifierTypes.REGISTRY_KEY` = `arcanumlib:chance_modifier` |

#### 12.2 Complete example

```java
public final class MyModifiers {

    /** Key point: the DeferredRegister hangs under **your own modid**, while the registry key is ArcanumLib's. */
    public static final DeferredRegister<ChanceModifierType> MODIFIERS =
            DeferredRegister.create(ChanceModifierTypes.REGISTRY_KEY, MyMod.MODID);

    public static final DeferredHolder<ChanceModifierType, ChanceModifierType> SQUARE =
            MODIFIERS.register("square", () -> new ChanceModifierType(Square.CODEC));

    /** A stateless record is the least trouble: the same condition instance is reused across many rolls. */
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

Once registered, it can be used anywhere just like a built-in modifier:

```json
{
  "condition": "arcanumlib:chance_modifiers",
  "base_chance": 0.2,
  "chance_modifiers": [
    { "type": "mymod:square", "factor": 1.5 }
  ]
}
```

#### 12.3 Two hard rules

1. **`apply` must be a stateless pure function**: a condition instance is **reused repeatedly** by the same loot table (the chain runs on every roll),
   and storing mutable state in it makes the result unpredictable. Implementing it as a `record` is the least trouble.
2. **`getType()` must return the value of the holder that was registered** (that is, the `DeferredHolder` returned by `MODIFIERS.register(...)`);
   returning some other instance makes the `type` lookup fail. Also, the type **must use a `MapCodec`**
   (`Codec.dispatch` with a method reference is ambiguous).

#### 12.4 ALI tooltip fallback

When ALI is installed, this mod's nine modifiers have dedicated rendering; **external modifiers go through the fallback branch** and only show their registry name (for example `mymod:square`).
If you want your own rendering, write an ALI plugin of your own following `compat/ali/AliLootPlugin.java`.

#### 12.5 Other limitations

- `operation` only has the two values `add` / `multiply`;
- Evaluating the condition **consumes one random number** (`nextFloat()`), consistent with vanilla `random_chance` behavior;
- The chance chain itself **does not clamp**; if you do not put a `clamp` at the end of the chain you may get a negative number or a value > 1 (the behavior being "never passes / always passes").

---

### 13. Pitfalls and notes

1. **The two semantics of `operation`** (section 5) — the tier value of `difficulty_factor` is a **multiplier**, while
   `tool_enchantment_level.per_level` / `entity_attribute.per_point` are **deltas**. Getting it wrong produces chances that differ greatly.
2. **Order matters**: `[{add 0.1}, {multiply 2}]` and `[{multiply 2}, {add 0.1}]` give different results.
3. **Custom modifiers must be stateless**: the interface is open to outside code, but the implementation is called repeatedly; do not store mutable state in `apply`.
   Also, the ALI tooltip only shows the registry name for external modifiers (section 12.4).
4. **A missing tool / missing attribute both mean "no change"**, not "zeroed"; conversely, if you really want "zero it when absent",
   you have to add `set_chance` yourself or combine other conditions.
5. **`tool_enchantment_level` depends on `LootContextParams.TOOL`**: contexts such as chests, spawners and fishing have no tool at all,
   so this modifier simply does not take effect there (silently). If you want it to work in those situations, use `entity_attribute` pointing at a player attribute.
6. **`entity_attribute`'s `attribute` is a static registry** (`BuiltInRegistries.ATTRIBUTE`),
   whereas `tool_enchantment_level`'s `enchantment` is a **datapack registry** — so the former can be written directly as a
   `Holder` in datagen, while the latter can only be written as a `ResourceKey`.
7. **Strict json parsing**: no comments and no trailing commas.

---

### Appendix: related source files

| Content | File |
|---|---|
| The condition itself (evaluation + builder) | `modules/loot/lootCondition/ChanceModifierCondition.java` |
| The nine modifiers + `Operation` / `EntitySource` | `modules/loot/lootCondition/ChanceModifier.java` |
| The stage modifier (requires AStages) | `compat/astages/StageChanceModifier.java` |
| Modifier registry and registration | `modules/loot/lootCondition/ChanceModifierTypes.java` |
| Condition type registration | `modules/loot/lootCondition/LootConditionTypes.java` |
| Enchantment level event | `events/EnchantedLootEvent.java` |
| The mixins that make the four vanilla mechanics fire this event too | `mixins/{ApplyBonusCount,BonusLevelTableCondition,EnchantedCountIncreaseFunction,LootItemRandomChanceWithEnchantedBonusCondition}Mixin.java` |
| Official examples (each of the 9 modifiers has a use case) | `test_zombie_modifiers.json` (`add` / `multiply` / `set_chance` / `invert`) and `test_temple.json`, `test_zombies_tag.json`, `test_zombie_basic.json` (`clamp` / `difficulty_factor` / `tool_enchantment_level` / `entity_attribute`) and `test_stage_modifier.json` (`stage`) under `src/generated/resources/data/arcanumlib/loot_injecter/` |

---

## `arcanumlib:has_stage`

Gates loot on an **AStages stage**: it passes only when the player (or the whole server) holds a matching stage. Without AStages installed it never passes.

- Registered as `arcanumlib:has_stage` under `minecraft:loot_condition_type`
- Source: `compat/astages/HasStageCondition.java`; stage name matcher `compat/astages/StagePattern.java`; every AStages call lives in `compat/astages/`

### Format

```json
{
  "condition": "arcanumlib:has_stage",
  "stage": { "pattern": "chapter_*", "type": "wildcard" },
  "desc": "mymod.stage.chapter",
  "check_server_stages": false
}
```

| Field | Required | Default | Meaning |
|---|---|---|---|
| `stage` | ✅ | — | Stage matcher. A plain string means `literal`; or write `{ "pattern": ..., "type": ... }` |
| `desc` | | — | **Localization key** for the name shown in ALI. Without it the raw `pattern` is shown |
| `check_server_stages` | | `false` | `true` checks server-wide (world) stages, `false` checks player stages |

### Matching modes

`type` has three values, matching the semantics of AStages' own `StageMatcher`:

| type | Meaning |
|---|---|
| `literal` | Exact equality (what a plain string means) |
| `wildcard` | `*` matches any length, e.g. `chapter_*` |
| `regex` | Java regex, matched against the whole stage name |

### Which player is used

The first of these that is a player: `LAST_DAMAGE_PLAYER` -> `ATTACKING_ENTITY` -> `THIS_ENTITY`. If none is a player the condition **does not pass** (no implicit "nearest player"). For block loot and chests `THIS_ENTITY` is the player in the normal case.

### How it shows up in ALI

```
Player stage : Chapter 1      <- check_server_stages: false
World stage : Chapter 1       <- check_server_stages: true
```

The name comes from the `desc` localization key and is shown on the **same line**; the raw `pattern` is shown when `desc` is absent or its key does not exist. The two prefixes come from this mod (`%s` is replaced with the stage name):

| Key | 中文 | English |
|---|---|---|
| `arcanumlib.ali.condition.has_stage.player` | 玩家阶段 : %s | Player stage : %s |
| `arcanumlib.ali.condition.has_stage.server` | 世界阶段 : %s | World stage : %s |

### Without AStages

The condition is still registered but **never passes** (JSON using it still parses and loads), and a warning is logged the first time it is evaluated.
