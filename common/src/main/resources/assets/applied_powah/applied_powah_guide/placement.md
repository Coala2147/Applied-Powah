---
navigation:
  parent: index.md
  title: Placement
  position: 30
---

# Placement

Energizing rods place only when the target location is valid for an AE2 cable connection.

## Rules

1. The rod must be placed **adjacent to an AE2 cable** (glass, covered, smart, and colour variants). Dense cables are not used as rod hosts.
2. Placing while targeting a cable sets `facing` toward that cable.
3. If exactly one cable is adjacent and none was targeted, `facing` points at that cable.
4. If several cables are adjacent and none was targeted: default south, else east, else any adjacent cable.
5. If no cable is adjacent, the rod cannot be placed.
6. If the connected cable is later removed, the rod may drop or reorient according to remaining neighbours.

Energy cells place like AE2 energy cells; they do not use the rod cable-host rules.
