---
navigation:
  parent: index.md
  title: Energizing rods
  position: 20
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

Applied Powah provides **AE** and **ME** energizing rods in seven tiers:

starter → basic → hardened → blazing → niotic → spirited → nitro

Rods are **full blocks**, not cable-bus parts. Collision is a thin rod shape, similar to Powah rods.

## Energy source

| Type | Source | Unit |
|------|--------|------|
| AE energizing rod | AE stored in the ME **grid** | AE |
| ME energizing rod | FE in the ME **network** (Applied Flux) | FE |

Buffer capacity and transfer rates follow Powah `energizing_rods` defaults for the matching tier.
AE display values are the FE cache divided by two (AE2 `ForgeEnergy = 0.5`).

## Operating rules

- The rod must join an AE2 network on its **facing** side (the side toward the cable).
- The grid node requires a **channel**. Without an active node, the rod does not extract energy.
- Idle power usage is 1.0 AE/t when the node is active.
- After extraction from an AE grid, the grid retains a configurable reserve (default **5%** of grid capacity).
- Extraction uses a fixed interval (`pullIntervalTicks`) and configured burst amounts. Ticks are not accelerated.
- When a Powah Energizing Orb is in range and holds a valid energizing recipe, the rod pushes energy into that orb.
- Breaking a rod does **not** preserve internal FE/AE on the item.

## Without Applied Flux

ME rod **recipes** are disabled when Applied Flux is absent.
If an ME rod block already exists in the world, it will not extract FE until Applied Flux is installed.

## Tooltip

Rod **items** do not show a live energy number. Documentation for behaviour is this guide page (Hold **G**).
