---
navigation:
  parent: index.md
  title: Recipes
  position: 40
---

# Recipes

## Energy cells

| Result | Pattern |
|--------|---------|
| Super dense energy cell | 8× `ae2:energy_cell` + 1× `ae2:calculation_processor` |
| Extreme dense energy cell | 8× `applied_powah:super_dense_energy_cell` + 1× `ae2:calculation_processor` |

## Energizing rods from Powah rods

`a` = AE2 energy cell; `b` = same-tier `powah:energizing_rod_<tier>`.

**AE rod** (cross):

```
_ a _
a b a
_ a _
```

**ME rod** (corners):

```
a _ a
_ b _
a _ a
```

## Tier upgrades

Upgrade recipes match the Powah recipe for the next tier, with the rod slot replaced by the previous-tier **Applied Powah** rod of the same family (AE or ME).

## AE ↔ ME conversion

Same-tier AE and ME rods convert with a shapeless recipe in both directions.

## Mod gating

| Environment | Available |
|-------------|-----------|
| AE2 + Powah + Applied Flux | Cells, AE/ME rods, upgrades, conversions |
| AE2 + Powah (no Applied Flux) | Cells and AE rods only |
| AE2 only (no Powah) | Energy cells only |
| No AE2 | This mod does not load content |

## JEI

With JEI installed, Applied Powah rods appear as **catalysts** on Powah's Energizing category (they supply orbs). Crafted rod recipes remain workbench recipes (press **R** on the item).
