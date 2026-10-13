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

<RecipeFor id="applied_powah:ae_energizing_rod_starter" fallbackText="4× energy cell in a cross + Powah starter rod (center)." />

<RecipeFor id="applied_powah:me_energizing_rod_starter" fallbackText="4× energy cell in the corners + Powah starter rod (center)." />

<RecipeFor id="applied_powah:ae_energizing_rod_basic" fallbackText="4× energy cell in a cross + AP starter rod (center)." />

## Energizing orbs

All three need **Powah** installed.

<RecipeFor id="applied_powah:fluix_energizing_orb" fallbackText="Powah energizing orb + AE2 import bus + AE2 interface." />

<RecipeFor id="applied_powah:advanced_energizing_orb" fallbackText="4× fluix orb on the edge midpoints + 4× nitro rod in the corners + engineering processor in the center." />

<RecipeFor id="applied_powah:me_energizing_orb" fallbackText="Advanced orb + AE2 pattern provider + 2× capacity card + engineering processor." />

## Upgrade cards

Upgrade slots take AE2's own cards — the addon ships no custom cards. See the
[Upgrade cards](upgrade-cards.md) page.
