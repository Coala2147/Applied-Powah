---
navigation:
  parent: applied_powah:ap_intro/ap_intro-index.md
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
categories:
- applied powah
---

# Energizing rods

Seven tiers each for **AE** and **ME**:
starter → basic → hardened → blazing → niotic → spirited → nitro.

Full blocks with thin Powah-like collision. Not cable-bus parts.

## Energy

| Type | Source | Tooltip unit |
|------|--------|--------------|
| AE rod | AE in the ME grid | AE (FE/2) |
| ME rod | FE in the ME network (Applied Flux) | FE |

Buffer / transfer follow Powah `energizing_rods` for the same tier.

## Rules

- Connects to AE2 on the **facing** side; place next to a cable.
- Requires an active node (channel). Idle draw 1 AE/t.
- AE pull keeps a grid reserve (default 5%, configurable).
- Fixed pull interval — no tick acceleration.
- Feeds a nearby Powah Energizing Orb when that orb has a valid recipe.

## Drops

With `rodsKeepEnergyOnBreak=true` (default), mined rods **keep** their FE buffer on the item (Powah-style `storeToStack`). Placement restores the buffer from NBT.

## Tooltip

- Label gray, value darker gray: `Buffer: 20M FE` / `Transfer: 200k FE/t`
- If the item carries a mined buffer, an AE2-style stored-energy line appears.
