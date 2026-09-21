---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: Configuration
  position: 50
---

# Configuration

`config/applied_powah-common.toml` → section `charging_rod`:

| Key | Default | Meaning |
|-----|---------|---------|
| `pullIntervalTicks` | 1 | Interval between network extraction bursts |
| `networkReserveRatio` | 0.05 | AE grid capacity retained after extraction |
| `aeBurstAe` | 10000000 | AE per burst (AE rods) |
| `meBurstFe` | 20000000 | FE per burst (ME rods) |
