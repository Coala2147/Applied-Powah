---
navigation:
  parent: applied_powah:ap_intro/ap_intro-index.md
  title: Energizing orbs
  position: 30
item_ids:
  - applied_powah:me_energizing_orb
  - applied_powah:advanced_energizing_orb
categories:
- applied powah
---

# Energizing orbs

Applied Powah adds two grid-aware machines that run Powah **Energizing** recipes
(`powah:energizing`): the **ME Energizing Orb** and the **Advanced Energizing Orb**.

Both connect to an ME network on the **bottom** face only (cable under the block).

## ME Energizing Orb

| Property | Behavior |
|----------|----------|
| Recipes | Same as Powah Energizing Orb (6 inputs + 1 output) |
| Energy cache | **None** — does not store network power |
| Network pull | **Never** pulls AE/FE from ME |
| Rod feed | **Yes** — Powah rods and AP AE/ME rods fill the current recipe |
| Auto-export | Optional toggle: push output into ME storage / adjacent inventories |
| Rod slots | No |
| Parallel pages | No (single 6+1 task) |

ME orb is a **GUI + auto-export** machine. It must be charged by rods (or any
Powah-compatible energy source) while a recipe is loaded. Without a recipe it
accepts no energy.

### Auto-export

Toolbar button on the left of the GUI (AE2 auto-export icon):

- **Off** — products stay in the output slot
- **On** — products are inserted into the ME network (Interface-style
  `IStorageService.insert`) and/or adjacent inventories, rate-limited per tick

## Advanced Energizing Orb

| Property | Behavior |
|----------|----------|
| Recipes | Same Powah energizing recipes |
| Rod slots | 4 slots × up to **16** AP rods each |
| Rod family | All rods must be **AE** or all **ME** (mixing families is blocked) |
| Rod tiers | Mixed tiers allowed |
| Energy cache | \(\sum n_i \times C(t_i)\) — created only when rods are inserted |
| Network pull | Fills the rod cache from ME (AE grid or Applied Flux FE) |
| Output slot | Stacks up to **64** of the same product; result count = recipe result |
| Parallel | Reserved for later (UI may show 4 rod slots now) |

### How the cache works

1. Insert AP rods (same family). Each rod adds its tier capacity to the cache.
2. The orb pulls energy from the ME network into that cache
   (like an energy cell / wireless terminal charging from the grid).
3. Recipes consume recipe energy from the cache; leftover energy stays.
4. No rods → no cache → no network pull.

AE rods: network side draws **AE**; internal cache is FE (display AE = FE/2).  
ME rods: network side draws **FE** via Applied Flux (mod required).

### Rod feed

Like Powah, rods in the world near the orb can still `fillEnergy` when a recipe
is present. On the Advanced orb this fills the **rod cache**, not only one recipe.

## GUI

Left toolbar (outside the main panel):

1. **?** — open this GuideME page
2. **Auto-export** — toggle product push to ME

Progress bar shows the current recipe buffer only. Advanced orb also shows
`缓存 current/max` for the rod cache.

## Recipes (temporary)

| Block | Recipe |
|-------|--------|
| ME Energizing Orb | Glass cable + Powah energizing orb + AE2 import bus |
| Advanced Energizing Orb | ME orb + 7× nitro rods + AE2 energy acceptor |

Exact JSONs may change before 0.2.x stable.
