---
navigation:
  parent: ap_intro/ap_intro-index.md
  title: Placement
  position: 35
categories:
  - Applied Powah
---

# Placement

## Energizing rods

Rods are full blocks with a thin shape (like Powah rods):

1. Place next to an **AE2 cable** — otherwise you cannot place it.
2. Aim at a cable while placing; the rod will **face** that cable.
3. Only one cable nearby? Facing is chosen for you.
4. Several cables and you did not aim: it faces **south**, then **east**.
5. If that cable is broken, the rod may drop or snap to another cable.

The rod connects to the network on the **facing** side only; the other sides
are plain block faces.

## Energizing orbs

Orbs connect to the ME network on the **bottom** face only — put a cable (or
any network device) under the block. With `orbMultiFacing` enabled in the
config they can also be rotated visually, but the network side stays bottom.
