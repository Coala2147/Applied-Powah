---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: Energizing rods
  position: 20
  icon: applied_powah:ae_energizing_rod_starter
categories:
  - Applied Powah
item_ids:
  - applied_powah:ae_energizing_rod_starter
  - applied_powah:ae_energizing_rod_basic
  - applied_powah:ae_energizing_rod_hardened
  - applied_powah:ae_energizing_rod_blazing
  - applied_powah:ae_energizing_rod_niotic
  - applied_powah:ae_energizing_rod_spirited
  - applied_powah:ae_energizing_rod_nitro
  - applied_powah:me_energizing_rod_starter
  - applied_powah:me_energizing_rod_basic
  - applied_powah:me_energizing_rod_hardened
  - applied_powah:me_energizing_rod_blazing
  - applied_powah:me_energizing_rod_niotic
  - applied_powah:me_energizing_rod_spirited
  - applied_powah:me_energizing_rod_nitro
---

# Energizing rods

Full-block rods that feed a nearby **Powah Energizing Orb** (or one of our
[orbs](energizing-orbs.md)) using energy from your **ME network**. Seven tiers,
matching Powah: starter → basic → hardened → blazing → niotic → spirited → nitro.

<Row>
  <BlockImage id="applied_powah:ae_energizing_rod_starter" scale="4" />
  <BlockImage id="applied_powah:me_energizing_rod_starter" scale="4" />
</Row>

| Family | Energy source | Notes |
|--------|---------------|-------|
| <ItemLink id="applied_powah:ae_energizing_rod_starter" /> … nitro | **AE** from the ME grid | No extra mods needed |
| <ItemLink id="applied_powah:me_energizing_rod_starter" /> … nitro | **FE** from the ME network | Needs **Applied Flux**; without it the recipes are disabled |

## Tier stats

Powah-tier numbers. Item tooltips show the same values; the buffer is kept on
the item when you mine the rod (configurable).

| Tier | Buffer (AE rod) | Transfer (AE rod) | Buffer (ME rod) | Transfer (ME rod) |
|------|-----------------|-------------------|-----------------|-------------------|
| starter | 5k | 50/t | 10k | 100/t |
| basic | 20k | 200/t | 40k | 400/t |
| hardened | 50k | 500/t | 100k | 1k/t |
| blazing | 200k | 2k/t | 400k | 4k/t |
| niotic | 500k | 5k/t | 1M | 10k/t |
| spirited | 2M | 20k/t | 4M | 40k/t |
| nitro | 10M | 100k/t | 20M | 200k/t |

## How to use

1. Place the rod **next to an AE2 cable**, aiming at it — see [Placement](placement.md).
2. Put a **Powah Energizing Orb** (or AP orb) nearby with a valid recipe loaded.
3. The rod pulls energy from the network into its buffer and pushes it into the
   orb. A beam shows while it is feeding.

### Wrench link (Powah wrench)

Bind a rod to one specific orb, Powah-style:

1. Switch the wrench to **Link** mode (shift-right-click in air).
2. Right-click the **rod** — chat says "Link started".
3. Right-click the **orb** — chat says "Link done".

The rod then targets that orb even if others are nearby. Right-clicking an AP
orb with a linked wrench completes the link instead of opening the GUI.

## Recipes

AE rods use a **cross** of energy cells; ME rods use the **corners**. Both take
the same-tier Powah rod in the center. Upgrades follow Powah's pattern
(previous rod in the middle), and AE ↔ ME is a shapeless swap.

<RecipeFor id="applied_powah:ae_rod_starter" fallbackText="4× AE2 energy cell in a cross + Powah starter rod (center)." />

<RecipeFor id="applied_powah:me_rod_starter" fallbackText="4× AE2 energy cell in the corners + Powah starter rod (center). Needs Applied Flux." />
