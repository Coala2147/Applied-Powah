---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: Recipes
  position: 40
  icon: applied_powah:ae_energizing_rod_nitro
categories:
  - Applied Powah
---

# Recipes

Every crafting pattern in the addon at a glance. Recipes that need **Powah** or
**Applied Flux** only exist when those mods are installed — the fallback text
below describes them in that case.

## Energy cells

8× the previous cell + **1 calculation processor**.

<RecipeFor id="applied_powah:super_dense_energy_cell" fallbackText="8× AE2 energy cell + calculation processor." />

<RecipeFor id="applied_powah:extreme_dense_energy_cell" fallbackText="8× super dense energy cell + calculation processor." />

## Energizing rods

- **AE rod:** 4× <ItemLink id="energy_cell" /> in a **cross** + same-tier Powah rod in the center.
- **ME rod:** 4× <ItemLink id="energy_cell" /> in the **corners** + same-tier Powah rod in the center. Needs Applied Flux.
- **Upgrade:** same shape as the base rod, previous AP rod in the middle.
- **AE ↔ ME:** shapeless swap of the same tier.

<RecipeFor id="applied_powah:ae_rod_starter" fallbackText="4× energy cell in a cross + Powah starter rod (center)." />

<RecipeFor id="applied_powah:me_rod_starter" fallbackText="4× energy cell in the corners + Powah starter rod (center)." />

<RecipeFor id="applied_powah:ae_rod_basic" fallbackText="4× energy cell in a cross + AP starter rod (center)." />

## Energizing orbs

All three need **Powah** installed.

<RecipeFor id="applied_powah:fluix_energizing_orb" fallbackText="Powah energizing orb + AE2 import bus + AE2 interface." />

<RecipeFor id="applied_powah:advanced_energizing_orb" fallbackText="Fluix orb + 7× nitro rods + extreme dense energy cell." />

<RecipeFor id="applied_powah:me_energizing_orb" fallbackText="Advanced orb + AE2 pattern provider + 2× capacity card + engineering processor." />

## Upgrade cards

Thread and parallel cards stack to 1 and install one at a time per family.
Top tiers use a **singularity**; the previous tier sits in the middle.

<RecipeFor id="applied_powah:thread_card_p1" fallbackText="Nether star + advanced card + vibrant quartz glass + spatial cell component 2." />

<RecipeFor id="applied_powah:thread_card_p8" fallbackText="Thread card +4 + calculation processor + spatial components + singularity." />

<RecipeFor id="applied_powah:parallel_card_x2" fallbackText="Advanced card + nether star + crafting accelerator + vibrant quartz glass." />

<RecipeFor id="applied_powah:parallel_card_x8" fallbackText="Parallel card ×6 + calculation processor + vibrant quartz glass + singularity." />
