---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: Upgrade cards
  position: 25
  icon: applied_powah:thread_card_p1
categories:
  - Applied Powah
item_ids:
  - applied_powah:thread_card_p1
  - applied_powah:thread_card_p2
  - applied_powah:thread_card_p4
  - applied_powah:thread_card_p8
  - applied_powah:parallel_card_x2
  - applied_powah:parallel_card_x4
  - applied_powah:parallel_card_x6
  - applied_powah:parallel_card_x8
---

# Upgrade cards

Cards for the orb upgrade slots (the row of small slots in the orb GUI). Three
families, each with its own slot budget:

| Family | Max installed | Effect |
|--------|---------------|--------|
| <ItemLink id="applied_powah:thread_card_p1" /> … <ItemLink id="applied_powah:thread_card_p8" /> | **1** | Extra concurrent lanes: the orb keeps more recipe batches in flight at once (+1 / +2 / +4 / +8) |
| <ItemLink id="applied_powah:parallel_card_x2" /> … <ItemLink id="applied_powah:parallel_card_x8" /> | **1** | Batch multiplier: each craft run produces ×2 / ×4 / ×6 / ×8 of the recipe result |
| <ItemLink id="ae2:energy_card" /> | **4** | Bigger energy cache and faster network IO (AE2's own card — no AP version needed) |

The orb has **6** upgrade slots in total; family caps are enforced on insert.

Higher tiers cost the previous tier in the middle, AE2 spatial components and
processors around it, and the top tiers need a singularity.

## Recipes

<RecipeFor id="applied_powah:thread_card_p1" fallbackText="Nether star + advanced card + vibrant quartz glass + spatial cell component 2." />

<RecipeFor id="applied_powah:parallel_card_x2" fallbackText="Advanced card + nether star + crafting accelerator + vibrant quartz glass." />

The full ladder (thread +2/+4/+8, parallel ×4/×6/×8) upgrades the previous card
in the center — see the [Recipes](recipes.md) page for the overview.
