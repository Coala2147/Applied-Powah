---
navigation:
  parent: index.md
  title: Configuration
  position: 50
---

# Configuration

Common config file (Forge): `config/applied_powah-common.toml`

Section `charging_rod`:

| Key | Default | Meaning |
|-----|---------|---------|
| `pullIntervalTicks` | 1 | Ticks between network extraction bursts into the rod buffer |
| `networkReserveRatio` | 0.05 | Fraction of AE grid capacity that must remain after extraction |
| `aeBurstAe` | 10000000 | AE requested per burst for AE rods |
| `meBurstFe` | 20000000 | FE requested per burst for ME rods |

Extraction never uses `TickRateModulation.FASTER`. Adjust interval and burst values for pack balance.
