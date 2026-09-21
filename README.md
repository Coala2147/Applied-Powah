# Applied Powah

Integration addon for **Applied Energistics 2** and **Powah** on Minecraft **1.20.1 Forge**.

Provides high-density AE energy cells and AE/ME Energizing Rods that draw energy from an ME network and supply nearby Powah Energizing Orbs.

In-game documentation is merged into the **AE2 GuideME** book (`assets/applied_powah/ae2guide/`), same pattern as ExtendedAE / Advanced AE — not a separate GUI book. Hold **G** on Applied Powah items to open the matching page.

- **License**: LGPL-3.0-only
- **Mod ID**: `applied_powah`
- **Java package**: `com.coala.appliedpowah`
- **Repository**: `https://github.com/Coala2147/Applied-Powah.git`
- **Docs**: this README, `docs/工程指导.md`, `AGENTS.md`, `docs/guidebook/`
- **Release jars**: `releases/AppliedPowah-<mod_version>.jar` (older builds retained)

## Content (current alpha)

| Component | Description |
|-----------|-------------|
| Super / Extreme Dense Energy Cell | 12.8M / 102.4M AE; crafted from 8× previous cell + calculation processor |
| AE Energizing Rod ×7 tiers | Full block; extracts **AE** from the ME network; feeds a nearby Powah Energizing Orb |
| ME Energizing Rod ×7 tiers | Full block; extracts **FE** (Applied Flux) from the ME network; feeds an Energizing Orb |
| Recipes | AE cross / ME corners + same-tier Powah rod; tier upgrades follow Powah patterns; AE↔ME shapeless; gated by mod presence |
| Placement | Requires an adjacent AE2 cable; `facing` targets the cable (aimed cable → single cable → south/east) |
| Collision | Thin rod shape (Powah-like), not a full cube |
| Beam | Visual beam while transferring energy to an orb (borrowed textures until 1.0) |

Item tooltips follow AE2 wording and light-gray styling (`Stored Energy: … AE (…%)`). Rod items do not fabricate live stored energy. Behaviour details are in the GuideME book (`assets/applied_powah/applied_powah_guide/`).

## Dependencies

| Mod | Required |
|-----|----------|
| Minecraft 1.20.1 + Forge 47+ | Yes |
| **Applied Energistics 2 15+** | **Yes** |
| Powah 5+ | Strongly recommended (Energizing Orb and recipe source) |
| Applied Flux 1+ | Required for ME rod FE extraction |
| JEI 15+ (optional) | Rod catalysts on the Powah Energizing category |
| GuideME (via AE2) | In-game guide book pages |

## Building

```
JAVA_HOME=D:\Program Files\Java\jdk-17
HTTP(S)_PROXY=http://127.0.0.1:7890   # also set in gradle.properties systemProp
./gradlew build
# → releases/AppliedPowah-<mod_version>.jar
```

Set `mod_version` in `gradle.properties` (Maven-legal, e.g. `0.1.0-alpha.9`).

Local compile jars live in `libs/` (AE2, Powah, Applied Flux, JEI). See `libs/README.md`.

## Source layout

```
common/src/main/java/com/coala/appliedpowah/   # game logic
forge/src/main/java/.../AppliedPowah.java      # @Mod entrypoint
common/src/main/resources/assets/applied_powah # models/textures/lang
common/src/main/resources/data/applied_powah   # recipes/loot tables
docs/                                           # engineering docs and guidebook drafts
releases/                                       # built jars
refs/                                           # read-only reference sources (not in git)
```

## Roadmap (excerpt)

- **In progress**: full-block energizing rods, network extraction, orb feed, beam, localization, AE2-style tooltips, JEI catalysts
- **Later**: Jade integration, original textures (before 1.0), additional machines under review
- Details: `docs/工程指导.md` and `docs/PLAN_高级充能台与指导体系.md`
