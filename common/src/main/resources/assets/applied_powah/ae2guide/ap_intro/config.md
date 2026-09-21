---
navigation:
  parent: applied_powah:ap_intro/ap_intro-index.md
  title: Configuration
  title.zh_cn: 配置
  position: 50
categories:
  - Applied Powah
---

# Configuration

`config/applied_powah-common.toml` (name may vary after first run):

| Key | Default | Meaning |
|-----|---------|---------|
| `pullIntervalTicks` | 1 | Interval between network energy pulls (ticks) |
| `networkReserveRatio` | 0.05 | Fraction of ME grid energy that must be kept (5%) |
| `aeBurstAe` | 10000000 | AE pulled per AE-rod burst |
| `meBurstFe` | 20000000 | FE pulled per ME-rod burst |

Values are configurable; balance for your pack.
