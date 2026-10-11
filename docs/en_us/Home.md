# ArcanumLib

A NeoForge **dependency library** for Minecraft 1.21.1. Other mods rely on it to get:

| Feature | Docs |
|---|---|
| **Loot table injectors** — change any loot table from JSON or datagen without overwriting vanilla data | [Loot Injector](Loot-Injector.md) |
| **Loot conditions** — `arcanumlib:chance_modifiers`, a chance chain that reacts to difficulty, enchantment levels and entity attributes | [Loot Conditions](Loot-Conditions.md) |
| **Attributes** — `seeking` / `fortune` / `looting` player attributes | [Attributes](Attributes.md) |

- Minecraft `1.21.1`, NeoForge `21.1.219+`
- modid: `arcanumlib`
- target: usable as a dependency by other mods (custom registries are public and documented)

## Where to start

- **Mod authors**: [Loot Injector](Loot-Injector.md) explains the injector JSON format, all 37 operators and 51 selectors, the extension points, and how to generate injectors with datagen.
- **Pack authors**: the same page covers the cookbook section — "add drops to one table", "swap an item", "drop rare entries", and so on.
- **Wiring loot into your own systems**: [Loot Conditions](Loot-Conditions.md) and [Attributes](Attributes.md).

> This page and the three pages above are the English set used for the GitHub Wiki.
> The repository also ships a Chinese translation under `docs/zh_cn/`.
