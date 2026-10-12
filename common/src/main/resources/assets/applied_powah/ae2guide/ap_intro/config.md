---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: Configuration
  position: 50
categories:
  - Applied Powah
---

# Configuration

`config/applied_powah-common.toml` (created on first run). Two sections:

### `[charging_rod]`

| Key | Default | Meaning |
|-----|---------|---------|
| `pullIntervalTicks` | 1 | Ticks between network energy bursts into the rod buffer |
| `networkReserveRatio` | 0.05 | Fraction of ME grid energy that must remain after extraction (5%) |
| `aeBurstAe` | 10000000 | AE pulled per burst attempt (AE rod); FE equivalent is roughly AE × 2 |
| `meBurstFe` | 20000000 | FE pulled per burst attempt (ME rod, Applied Flux) |
| `rodsKeepEnergyOnBreak` | true | Mined/unplaced rods keep their FE buffer on the item NBT (Powah-style drops) |

### `[energizing_orb]`

| Key | Default | Meaning |
|-----|---------|---------|
| `orbMultiFacing` | false | Orbs can use a non-up visual facing. ME connection stays BOTTOM-only |
| `orbAutoExport` | false | Auto-push finished products (adjacent inventories / ME), rate-limited per tick |
| `orbExportItemsPerTick` | 8 | Max items auto-exported per orb per tick |
| `orbPullFromNetwork` | true | Advanced orb pulls from the ME network into its rod cache. The ME orb never pulls (rod-fed only) |

Balance these for your pack; all values apply without a restart except where
noted by Forge.
