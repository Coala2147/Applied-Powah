---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: Energizing orbs
  position: 30
  icon: applied_powah:fluix_energizing_orb
categories:
  - Applied Powah
item_ids:
  - applied_powah:fluix_energizing_orb
  - applied_powah:advanced_energizing_orb
  - applied_powah:me_energizing_orb
---

# Energizing orbs

Applied Powah adds three ME-connected machines that run Powah **Energizing**
recipes (`powah:energizing`). They need **Powah** installed — without it none of
them (or their recipes) exist, and the mod still loads fine.

<Row>
  <BlockImage id="applied_powah:fluix_energizing_orb" scale="4" />
  <BlockImage id="applied_powah:advanced_energizing_orb" scale="4" />
  <BlockImage id="applied_powah:me_energizing_orb" scale="4" />
</Row>

All three connect to the ME network on the **bottom** face only (cable under
the block), show grid state and stored energy through AE2's overlay
integration (Jade / WTHIT / TOP), and additionally show recipe progress there.

## Fluix Energizing Orb

The simple one: a Powah Energizing Orb that lives in your ME network.

| Property | Behavior |
|----------|----------|
| Recipe pages | **1** (6 inputs + 1 output) |
| Energy cache | **None** — does not store network power |
| Network pull | **Never** pulls AE/FE from ME |
| Rod feed | **Yes** — Powah rods and AP AE/ME rods fill the current recipe |
| Output | Ejection tri-state (see below) |
| Rod slots | No |

It must be charged by rods (or any Powah-compatible energy source) while a
recipe is loaded. Without a recipe it accepts no energy.

### Ejection

Toolbar button on the left of the GUI, cycling three states:

- **Off** — products stay in the page output slot (a ME crafting CPU order
  waits by design and resumes when you switch back)
- **Configured sides** — products are pushed into the selected block faces only;
  a second button opens the face configuration
- **ME network** — products are inserted into the ME network (and CPU leftovers
  return through it)

## Advanced Energizing Orb

A Fluix orb with rod slots, a real energy cache and **4 independent recipe
pages**.

| Property | Behavior |
|----------|----------|
| Recipe pages | **4**, each with its own inputs, output and progress |
| Rod slots | 4 slots × up to **16** AP rods each |
| Rod family | All rods must be **AE** or all **ME** (mixing is blocked) |
| Rod tiers | Mixed tiers allowed |
| Energy cache | Sum of inserted rod capacities (+ energy cards), shown in the GUI and overlays |
| Network pull | Fills the rod cache from ME (AE grid, or FE via Applied Flux) |
| Output slot | Stacks up to **64** of the same product |

How the cache works:

1. Insert AP rods of one family. Each rod adds its tier capacity to the cache.
2. The orb pulls energy from the ME network into that cache.
3. Recipes consume energy from the cache; leftovers stay.
4. No rods → no cache → no network pull.

AE rods: the network side draws **AE**; the internal cache is FE (displayed as
AE = FE/2). ME rods: the network side draws **FE** via Applied Flux.

## ME Energizing Orb

The automated one: a pattern-provider-style host that accepts crafting jobs
from the ME network.

| Property | Behavior |
|----------|----------|
| Recipe pages | **4**, cross-page feeding from ME orders |
| Pattern slots | **36** encoded processing patterns (4×9) |
| Network jobs | Accepts `pushPattern` orders from a crafting CPU (Induction Card style bursts) |
| Rod feed | Same as Advanced (rod slots + cache) |
| Return inventory | Products and leftovers go back through the pushing provider |
| GUI | Orb page + pattern page (toolbar button), like a Pattern Provider |

Open the pattern page with the toolbar button to encode patterns; the crafting
CPU feeds ingredients and takes products automatically.

## GUI

The orb GUI is **176×199** and contains:

- **6 input slots** (2×3 grid) on the left
- **1 output slot** on the right (read-only)
- **Vertical progress bar** next to the output slot (hover for numbers)
- **Warning icon** above the progress area when energy is insufficient
- **Left toolbar** (outside the main panel):
  1. **?** — open this GuideME page
  2. **<** / **>** — switch recipe pages (Advanced / ME only)
  3. **Output** — cycle the ejection tri-state; in *configured sides* mode a
     second button opens the face configuration
  4. **Patterns** (ME orb only) — open the pattern page
- **Right panel** — 5 AE2 upgrade slots (speed cards on every tier, energy cards
  on Advanced / ME)

The page label sits under the GUI title; Advanced orbs also show their rod cache.

### Wrench interaction

Holding a **Powah Wrench** in link mode and right-clicking an orb does **not**
open the GUI; it completes a rod→orb link (see [Energizing rods](energizing-rods.md)).

## Recipes

<RecipeFor id="applied_powah:fluix_energizing_orb" fallbackText="Powah energizing orb + AE2 import bus + AE2 interface. Needs Powah." />

<RecipeFor id="applied_powah:advanced_energizing_orb" fallbackText="4× fluix orb on the edge midpoints + 4× nitro rod in the corners + engineering processor in the center. Needs Powah." />

<RecipeFor id="applied_powah:me_energizing_orb" fallbackText="Advanced orb + AE2 pattern provider + 2× capacity card + engineering processor. Needs Powah." />
