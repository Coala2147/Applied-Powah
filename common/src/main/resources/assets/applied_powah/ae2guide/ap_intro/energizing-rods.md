---
navigation:
  parent: ap_intro/ap_intro-index.md
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

Seven tiers each for **AE** and **ME** rods:
starter → basic → hardened → blazing → niotic → spirited → nitro.

Rods are full blocks (not cable-bus parts) with a thin collision shape.

## Network energy

| Type | Source | Unit |
|------|--------|------|
| AE energizing rod | AE in the ME grid | AE |
| ME energizing rod | FE in the ME network (Applied Flux) | FE |

Buffer and transfer follow Powah `energizing_rods` for the same tier.
AE item tooltip values are FE/2 (AE2 conversion 1 AE = 2 FE).

## Rules

- Connects to AE2 on the **facing** side only.
- Requires an active grid node (channel). Idle draw is 1 AE/t.
- AE extraction keeps a configurable grid reserve (default 5%).
- Fixed pull interval; no tick acceleration.
- Supplies a nearby Powah Energizing Orb when that orb holds a valid recipe.
- Mined rod items do not retain internal energy.

## Applied Flux missing

ME rod recipes are disabled. Existing ME rod blocks will not extract FE until Applied Flux is installed.

## Item tooltip

Shows **buffer** and **transfer** specs only (not live stored energy).
