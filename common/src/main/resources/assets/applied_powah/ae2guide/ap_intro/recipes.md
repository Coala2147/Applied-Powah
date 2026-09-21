---
navigation:
  parent: applied_powah:ap_intro/ap_intro-index.md
  title: Recipes
  position: 40
item_ids:
  - applied_powah:ae_energizing_rod_nitro
  - applied_powah:me_energizing_rod_nitro
categories:
- applied powah
---

# Recipes

`a` = <ItemLink id="ae2:energy_cell" optional />; `b` = same-tier Powah energizing rod.

## Energy cells

See [Energy cells](energy-cells.md) for the eight-plus-processor patterns.

## AE rod from Powah rod (example: Nitro)

Center: <ItemLink id="powah:energizing_rod_nitro" optional fallback="powah energizing rod (nitro)" />
Cross: four <ItemLink id="ae2:energy_cell" optional />.

```
_ a _
a b a
_ a _
```

<Row gap="8">
  <Column alignItems="center"><ItemLink id="ae2:energy_cell" optional /></Column>
  <Column alignItems="center"><ItemLink id="powah:energizing_rod_nitro" optional fallback="Powah rod (nitro)" /></Column>
  <Column alignItems="center"><ItemLink id="ae2:energy_cell" optional /></Column>
</Row>

<Row>
  <ItemImage id="applied_powah:ae_energizing_rod_nitro" scale="4" />
</Row>

<RecipeFor id="applied_powah:ae_energizing_rod_nitro" fallbackText="Shaped: energy cells in a cross around a nitro Powah rod" />

Other AE tiers use the same cross with the matching Powah rod.

## ME rod from Powah rod (example: Nitro)

Center: same-tier Powah rod. Corners: four energy cells.

```
a _ a
_ b _
a _ a
```

<Row>
  <ItemImage id="applied_powah:me_energizing_rod_nitro" scale="4" />
</Row>

<RecipeFor id="applied_powah:me_energizing_rod_nitro" fallbackText="Shaped: energy cells in the four corners around a nitro Powah rod" />

## AE ↔ ME conversion (example: Nitro)

Shapeless, both directions, same tier:

<Row gap="12">
  <ItemImage id="applied_powah:ae_energizing_rod_nitro" scale="3" />
  <ItemImage id="applied_powah:me_energizing_rod_nitro" scale="3" />
</Row>

<RecipeFor id="applied_powah:me_energizing_rod_nitro" fallbackText="Also: shapeless AE↔ME at the same tier" />

Other tiers: `ae_energizing_rod_<tier>` ↔ `me_energizing_rod_<tier>`.

## Tier upgrades

Materials match the Powah upgrade recipe for the next tier; the rod slot is the previous-tier Applied Powah rod (same AE/ME family).

## Gating

| Mods | Available |
|------|-----------|
| AE2 + Powah + Applied Flux | Cells and all rods |
| AE2 + Powah | Cells and AE rods |
| AE2 only | Cells only |
