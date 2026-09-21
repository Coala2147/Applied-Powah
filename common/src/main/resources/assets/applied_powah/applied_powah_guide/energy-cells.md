---
navigation:
  parent: index.md
  title: Energy cells
  position: 10
item_ids:
  - applied_powah:super_dense_energy_cell
  - applied_powah:extreme_dense_energy_cell
---

# Energy cells

Two ME-network storage cells with higher capacity than AE2's dense cell.

| Block | Capacity | Crafting |
|-------|----------|----------|
| Super dense energy cell | 12.8M AE | 8× AE2 energy cell + 1 calculation processor |
| Extreme dense energy cell | 102.4M AE | 8× super dense energy cell + 1 calculation processor |

## Behaviour

- Cells attach to an AE2 ME network like other AE2 energy cells.
- Charge rate and priority follow AE2 `EnergyCellBlock` parameters scaled for each tier.
- Internal power uses the AE2 NBT key `internalCurrentPower`.
- Breaking the block in survival exports stored power to the dropped item (AE2 dismantle settings).
- Creative pick-block copies the block entity's current power onto the item stack.

## Tooltip

When an item stack carries `internalCurrentPower`, the tooltip shows:

`Stored Energy: {current}/{max} AE ({percent})`

The line is light gray, matching AE2 energy tooltips. Empty creative-tab stacks show zero stored power.
