---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: Energy cells
  position: 10
item_ids:
  - applied_powah:super_dense_energy_cell
  - applied_powah:extreme_dense_energy_cell
categories:
- applied powah
---

# Energy cells

High-capacity AE storage for ME networks.

## Super dense energy cell

Capacity **12.8M AE**. Crafted from eight AE2 energy cells and one calculation processor.

<Row gap="12">
  <ItemImage id="ae2:energy_cell" scale="3" />
  <ItemImage id="ae2:energy_cell" scale="3" />
  <ItemImage id="ae2:energy_cell" scale="3" />
</Row>
<Row gap="12">
  <ItemImage id="ae2:energy_cell" scale="3" />
  <ItemImage id="ae2:calculation_processor" scale="3" />
  <ItemImage id="ae2:energy_cell" scale="3" />
</Row>
<Row gap="12">
  <ItemImage id="ae2:energy_cell" scale="3" />
  <ItemImage id="ae2:energy_cell" scale="3" />
  <ItemImage id="ae2:energy_cell" scale="3" />
</Row>

<Row>
  <ItemImage id="applied_powah:super_dense_energy_cell" scale="4" />
</Row>

<RecipeFor id="applied_powah:super_dense_energy_cell" fallbackText="Workbench recipe: 8× ae2:energy_cell + ae2:calculation_processor" />

## Extreme dense energy cell

Capacity **102.4M AE**. Eight super dense cells plus one calculation processor.

<RecipeFor id="applied_powah:extreme_dense_energy_cell" fallbackText="Workbench recipe: 8× applied_powah:super_dense_energy_cell + ae2:calculation_processor" />

## Behaviour

- Acts as an AE2 energy cell on the ME network.
- Item NBT key: `internalCurrentPower`.
- Survival dismantle exports stored AE onto the dropped item (AE2 path).
- Creative pick-block copies stored AE onto the item (this addon syncs power for cells).

## Tooltip

Label in light gray; value in a deeper gray (Powah-style):

`Stored Energy: 0/12.8M AE (0%)`
