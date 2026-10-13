![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen?style=flat-square)
![Forge](https://img.shields.io/badge/Forge-47%2B-orange?style=flat-square)
![Java](https://img.shields.io/badge/Java-17-blue?style=flat-square)
![AE2](https://img.shields.io/badge/AE2-15%2B%20required-critical?style=flat-square)
![License](https://img.shields.io/badge/License-LGPLv3-blue.svg?style=flat-square)

# Applied Powah

## Table of Contents

* [About](#about)
* [Contacts](#contacts)
* [License](#license)
* [Downloads](#downloads)
* [Installation](#installation)
* [Issues](#issues)
* [Content](#content)
* [Building](#building)
* [Repository Layout](#repository-layout)
* [Contribution](#contribution)
* [Localization](#localization)
* [Credits](#credits)

## About

An **Applied Energistics 2** addon that bridges ME networks and **Powah** energizing:
high-density AE energy cells, and full-block AE/ME Energizing Rods that pull energy
from your ME network and feed Powah Energizing Orbs — including three grid-aware
orbs of our own, one of which accepts crafting jobs straight from the ME network.

## Contacts

* [GitHub](https://github.com/Coala2147/Applied-Powah)
* Issues: [GitHub Issues](https://github.com/Coala2147/Applied-Powah/issues)

## License

* Code — (c) 2026 Coala, [LGPL-3.0-only](https://www.gnu.org/licenses/lgpl-3.0.html)
  (same as AE2 / Powah)
* Bundled textures derived from AE2 / Powah assets remain under their respective
  licenses (AE2 textures: CC BY-NC-SA 3.0). Original retextures are planned
  before 1.0; until then, borrowed art is intentionally kept in AE2/Powah style.
* Guide text — no restriction, translate freely.

## Downloads

Release jars are published as
[GitHub Releases](https://github.com/Coala2147/Applied-Powah/releases)
(`AppliedPowah-<version>.jar`). This is an alpha series: expect rough edges and
save your world before testing.

## Installation

Drop the jar into your `minecraft/mods/` folder.

| Mod | Requirement | Notes |
|-----|-------------|-------|
| Minecraft 1.20.1 + Forge 47+ | **Required** | |
| **Applied Energistics 2 15+** | **Required** | Hard prerequisite |
| GuideME | Guaranteed via AE2 | AE2 hard-depends it; our guide pages ride on AE2's guidebook system |
| Powah 5+ | Strongly recommended | Without it, energizing orbs and their recipes are disabled (soft dependency — no crash) |
| Applied Flux 1+ | Optional | Required only for ME rod FE extraction; without it, ME rod recipes are disabled |
| JEI 15+ | Optional | Adds AP rods/orbs as catalysts on the Powah Energizing category |
| Jade / WTHIT / TOP | Optional | Overlay tooltips. Energy / grid state for our blocks comes through AE2's own overlay integration; orbs additionally show recipe progress |

## Issues

Crashing, have a suggestion, found a bug? [Create an issue](https://github.com/Coala2147/Applied-Powah/issues/new)!

1. Make sure your issue has not already been answered or fixed, and that you are on the latest alpha.
2. Include: what happened, what you expected, how to reproduce, server or single player, mod list with versions, and a crash log (use a paste service).
3. Note that **Powah-less** setups are supported: if you report a crash without Powah installed, it is a P1 bug — please report it.

## Content

| Component | Description |
|-----------|-------------|
| Super / Extreme Dense Energy Cell | 12.8M / 102.4M AE network storage; 8× previous cell + calculation processor; keeps stored AE when mined |
| AE Energizing Rod ×7 tiers | Full block; extracts **AE** from the ME network; feeds nearby Energizing Orbs; Powah-tier stats |
| ME Energizing Rod ×7 tiers | Full block; extracts **FE** via Applied Flux; same orb feed |
| Fluix Energizing Orb | Powah energizing recipes in an ME-connected machine; rod-fed; 5 AE2 upgrade slots |
| Advanced Energizing Orb | 4 independent recipe pages, 4 rod slots + internal energy cache; pulls from the ME network (AE or FE) |
| ME Energizing Orb | Pattern-provider style host: 4 recipe pages, accepts ME crafting jobs, 36 encoded-pattern slots, return-inventory |
| Orb upgrade slots | AE2's own cards: speed card ×5 on every tier, energy card ×4 on Advanced / ME |
| Wrench link | Powah wrench binds rods to specific orbs (Powah workflow) |

In-game documentation is part of the **AE2 GuideME book** — hold **G** on any
Applied Powah item to open its page.

## Building

1. JDK 17 (any vendor), then:
   ```
   ./gradlew test reobfJar
   ```
   The jar lands in `build/libs/` and is copied to `releases/`.
2. No proxy is required: the build uses China mirrors (Aliyun / bmclapi) plus
   Modrinth and Maven Central, all reachable directly. If you are behind a
   firewall, uncomment the `systemProp.*proxy*` lines in `gradle.properties`.
3. Local compile jars live in `libs/` (see `libs/README.md`): AE2, Powah,
   Applied Flux (implementation), JEI + GuideME (compileOnly).
4. `mod_version` in `gradle.properties` drives both the jar name and
   `mods.toml` (Maven-legal, e.g. `0.6.0-alpha`).

IDE runs: `./gradlew genIntellijRuns` / `genEclipseRuns` are not needed —
import the Gradle project and use the generated `runClient` / `runServer`
configurations (ForgeGradle userdev).

## Repository Layout

```
common/src/main/java/com/coala/appliedpowah/   # game logic (platform-shared)
forge/src/main/java/.../AppliedPowah.java      # @Mod entrypoint (Forge)
common/src/main/resources/assets/applied_powah # models / textures / lang / ae2guide pages
common/src/main/resources/data/applied_powah   # recipes / loot tables / tags
common/src/main/resources/META-INF/services    # AE2 TooltipProvider registration (Jade/WTHIT/TOP)
docs/                                          # engineering docs (local, not tracked)
refs/                                          # read-only reference sources (not tracked)
libs/                                          # local compile jars (not tracked)
```

Developer notes worth knowing before you dive in:

* **Soft-dependency discipline**: Powah and Applied Flux are optional. All
  Powah-touching code paths are gated by registration checks
  (`Ae2Bridge.init`), runtime `ModList.get().isLoaded(...)` guards, and
  try/catch. JEI and overlay-mod integration classes are only loaded when the
  respective mod is present.
* **Resource conventions**: see `docs/资源管理.md` (local). The `ae2`
  namespace holds exactly one file — the screen style JSON forced there by
  AE2's `InitScreens`; everything else is `applied_powah`.
* **Overlay tooltips** are registered through AE2's addon API
  (`appeng.api.integrations.igtooltip.TooltipProvider`, ServiceLoader) instead
  of a hand-written Jade plugin — see
  `common/.../integration/APTooltipProvider.java`.

## Contribution

Before you want to add major changes, you might want to discuss them first,
before wasting your time. If you are still willing to contribute, you can do so
via [Pull Request](https://help.github.com/articles/creating-a-pull-request).

Keep in mind:

* A PR should be focused. One logical change per PR.
* Follow the style of the file you are editing; match AE2/Powah conventions
  where our code mirrors them.
* Do not add hard dependencies. Powah / Applied Flux / JEI / GuideME / Jade
  stay optional, and every integration must degrade gracefully.
* Resource additions follow `docs/资源管理.md`; run the reference-integrity
  check described there before submitting.

**Getting Started**

1. Fork this repository
2. Clone your fork
3. Create a branch, make your changes
4. Run `./gradlew test reobfJar` and make sure it passes
5. Commit with a clear message and open a PR

## Localization

* `en_us` and `zh_cn` ship in the repository; both are maintained by hand.
* Files are UTF-8.
* Preserve `%s` / `%1$d` placeholders when editing strings.
* Translation PRs for additional languages are welcome — copy `en_us.json` and
  translate values only.

## Credits

* [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) —
  the mod we extend, its guidebook system, tooltip/overlay integration API, and
  borrowed textures
* [Powah](https://github.com/owmii/Powah) — the energizing machines we feed,
  tier model, and borrowed textures
* [Applied Flux](https://github.com/GlodBlock/Applied-Flux) — FE side of ME rods
* [GuideME](https://github.com/Applied-Energistics/GuideME) — in-game guide book
* [Jade](https://www.curseforge.com/minecraft/mc-mods/jade) — overlay tooltips
* [ExtendedAE](https://github.com/GlodBlock/ExtendedAE) — reference addon for
  AE2 screen styles and integration patterns
