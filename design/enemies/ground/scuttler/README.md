---
title: Scuttler
design: approved
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Scuttler

## Summary

A slate-and-lime six-legged crab walker that strides across the terrain on its own heading, turning to face where it goes and firing fans in its facing direction. Claws armoured from the front, glowing back exposed.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` (sea floor in Act 4) |
| Size tier | `medium` |
| Size | 64×64 px, hitbox 46×40 |
| Parts | single, with armoured claws |
| Orientation | 16 angles × 6 walk phases (model-space re-render pending, round 08) |
| HP | 28 (easy 21 / hard 36, from the global multipliers) |
| Armour / shield | claws `armoured` from the front (shots from within ±45° of its facing spark off) |
| Speed | 40 px/s walking (plus scroll) |
| Movement | `walk` along an authored ground path; turns at 90°/s; walk phase advances with distance |
| Attack | 5-way `fan` in its facing direction every 2.8 s (spread 60°, 140 px/s, `small` = 4), plus acid spit (`aimed`, 120 px/s, `medium` = 6) every 4 s when facing away from the player |
| Formations | convoy, pincer (walking in from both sides) |
| Weak points | glowing lime back (×2) |
| Effective traits | `anti-ground` (×2), `side`, `homing` |
| Credits | 25 (score 250 × chain) |
| Death | `medium` organic burst: shell fragments, legs scatter |
| First level / used in | L04; returns L13 and on Europa's sea floor (L24) |
| Difficulty hooks | hard: fan 7-way |

### Behaviour

- Walks can cross the player's lane, so the back becomes exposed as it turns: rewards patience or side weapons.

### Concept art

Chosen concept: [scuttler-r05-a.png](../concept/scuttler-r05-a.png), [scuttler-r05-a.gif](../concept/scuttler-r05-a.gif) (listed in the [ground](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Walker path with turn rate and distance-driven walk phase
- [ ] Frontal claw armour
- [ ] Facing fan
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the ground roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
