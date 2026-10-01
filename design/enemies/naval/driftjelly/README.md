---
title: Driftjelly
design: approved
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Driftjelly

## Summary

An olive jellyfish mine with lime veins that drifts on or below the surface and pulses a ring of shots when the player comes close.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` (surface) or `sub` (submerged) |
| Size tier | `small` |
| Size | 40×40 px, hitbox 28×28 |
| Parts | single |
| Orientation | `radial` (4 pulse frames) |
| HP | 4 (easy 3 / hard 5, from the global multipliers) |
| Armour / shield | submerged: only `anti-sub` hits it (layer rule) |
| Speed | 15 px/s drift |
| Movement | `drift`; surfaced and submerged jellies swap every 6–10 s (a jelly surfacing follows the water rules: foam collar, ripples) |
| Attack | 8-bullet `ring` (90 px/s, `small` = 4) when the player is within 96 px; cooldown 2.5 s |
| Formations | swarm (scattered fields of 6–12) |
| Weak points | lime bell veins (×1.5) |
| Effective traits | `spread`, `area` (surfaced), `anti-sub` (submerged) |
| Credits | 10 (score 100 × chain) |
| Death | `small` wet pop; on water a splash and ripple train |
| First level / used in | L11; Act 2 oceans; Europa (Act 4) |
| Difficulty hooks | hard: 12-bullet ring, radius 110 px |

### Behaviour

- The 96 px trigger radius is a diagram on the concept sheet only; in game the bell's pulse quickening is the warning.

### Concept art

Chosen concept: [driftjelly-r07-a.png](../concept/driftjelly-r07-a.png), [driftjelly-r07-a.gif](../concept/driftjelly-r07-a.gif) (listed in the [naval](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Surface/submerged states with the layer rule
- [ ] Proximity ring with cooldown
- [ ] Waterline foam per the water rules
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the naval roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L11 reference DPS (6 → 4, ×70/100).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
