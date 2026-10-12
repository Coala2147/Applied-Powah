---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: Energy cells
  position: 10
  icon: applied_powah:super_dense_energy_cell
categories:
  - Applied Powah
item_ids:
  - applied_powah:super_dense_energy_cell
  - applied_powah:extreme_dense_energy_cell
---

# Energy cells

Two oversized AE energy cells for networks that outgrew <ItemLink id="dense_energy_cell" />s.
They behave exactly like AE2's own cells — same storage semantics, same charge
behaviour, same `internalCurrentPower` NBT — just bigger.

<Row>
  <BlockImage id="applied_powah:super_dense_energy_cell" scale="4" p:fullness="4" />
  <BlockImage id="applied_powah:extreme_dense_energy_cell" scale="4" p:fullness="4" />
</Row>

| Cell | Capacity | Crafted from |
|------|----------|--------------|
| <ItemLink id="applied_powah:super_dense_energy_cell" /> | 12.8M AE | 8× <ItemLink id="energy_cell" /> + calculation processor |
| <ItemLink id="applied_powah:extreme_dense_energy_cell" /> | 102.4M AE | 8× super dense + calculation processor |

## How to use

1. Place the cell next to an ME cable or any network device that connects.
2. It adds its capacity to the network's energy pool, like a normal AE2 cell.
3. Hover the item — or look at the block with Jade / WTHIT / TOP — to see
   stored energy. AE2's own overlay integration reads our cells directly, so
   the display matches stock AE2 cells.

Breaking the cell in survival keeps the stored AE on the dropped item.

<RecipeFor id="applied_powah:super_dense_energy_cell" fallbackText="8× energy cell + calculation processor." />

<RecipeFor id="applied_powah:extreme_dense_energy_cell" fallbackText="8× super dense energy cell + calculation processor." />
