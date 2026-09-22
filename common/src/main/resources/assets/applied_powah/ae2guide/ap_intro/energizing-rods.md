---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: Energizing rods
  position: 20
  icon: powah:energizing_rod_starter
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

Feed a nearby **Powah Energizing Orb** using energy from your **ME network**.

Seven tiers, matching Powah: starter → basic → hardened → blazing → niotic → spirited → nitro.

| Rod | Energy used |
|-----|-------------|
| **AE** energizing rod | AE from the ME grid |
| **ME** energizing rod | FE from the ME network (needs Applied Flux) |

## How to use

1. Place the rod **next to an AE2 cable** — it will face that cable.
2. Put a **Powah Energizing Orb** (or AP ME/Advanced Orb) nearby, with a valid recipe inside.
3. When the network has power (and a free channel), the rod pulls energy and pushes it into the orb. A beam shows it is working.

Item tooltips show that tier's **max buffer** and **max output** (same idea as Powah rods).

### Wrench link (Powah wrench)

You can use a **Powah Wrench** in link mode to bind a rod to a specific orb:

1. Switch the wrench to **Link** mode (shift-right-click in air).
2. Right-click the **rod** — chat says "Start link".
3. Right-click the **orb** — chat says "Link done".  
   The rod will now target that orb even if other orbs are nearby.

Right-clicking an AP orb with a linked wrench does **not** open the GUI; it completes the link instead.

Mined rods keep stored buffer energy when broken (configurable via `rodsKeepEnergyOnBreak`).

Without **Applied Flux**, ME rods cannot pull FE (their recipes are disabled too).
