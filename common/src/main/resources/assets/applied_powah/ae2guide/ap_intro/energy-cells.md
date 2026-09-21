---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: Energy cells
  position: 10
item_ids:
  - applied_powah:super_dense_energy_cell
  - applied_powah:extreme_dense_energy_cell
---

# Energy cells

| Block | Capacity | Crafting |
|-------|----------|----------|
| Super dense energy cell | 12.8M AE | 8× AE2 energy cell + calculation processor |
| Extreme dense energy cell | 102.4M AE | 8× super dense + calculation processor |

## Behaviour

- Attaches to an ME network like AE2 energy cells.
- Item NBT key is `internalCurrentPower` (same as AE2).
- Survival dismantle exports stored AE onto the dropped item.
- Creative pick-block copies stored AE onto the item (power is synced for this addon's cells).

## Tooltip

`Stored Energy: {current}/{max} AE ({percent})` — light gray, AE2 wording.
