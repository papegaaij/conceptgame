---
title: Ravager
design: approved
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Ravager

## Summary

An animal-like Vrell pack hunter: a rust hound-raptor of chitin and sinew that gallops in packs across streets and plains and pounces at the player's ground position.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground`; pounce apex briefly on `air` |
| Size tier | `medium` |
| Size | 56×56 px, hitbox 40×28 |
| Parts | single |
| Orientation | 16 angles × 8 gallop frames |
| HP | 16 (easy 12 / hard 21, from the global multipliers) |
| Armour / shield | none |
| Speed | 160 px/s galloping |
| Movement | `walk` (gallop) in packs of 3–5 along authored ground paths; when within 200 px of the player's ground position it **pounces**: a 0.75 s leap, drawn up to 43 % larger with its shadow sliding away |
| Attack | pounce: during the middle 0.3 s of the leap it is on the `air` layer and deals contact damage `medium` = 15; cooldown 3 s; no ranged attack |
| Formations | pack (3–5) |
| Weak points | glowing teal maw (×1.5) |
| Effective traits | `anti-ground` (×2), `spread`; hittable mid-pounce by any weapon |
| Credits | 18 (score 180 × chain) |
| Death | `small` organic burst |
| First level / used in | L09; Act 2 |
| Difficulty hooks | hard: pounce cooldown 2 s, packs +1 |

### Behaviour

- Ground units normally never collide with the player; the pounce apex is the one deliberate exception, shown clearly by the scale-up and shadow separation.
- A pounce that misses lands and the Ravager turns and continues the pack path.

### Concept art

Chosen concept: [ravager-r05-a.png](../concept/ravager-r05-a.png), [ravager-r05-a.gif](../concept/ravager-r05-a.gif) (listed in the [ground](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Pack gallop with distance-driven frames
- [ ] Pounce arc with air-layer window for collision
- [ ] Shadow separation during the leap
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the ground roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L09 reference DPS (20 → 16, ×63/80); time-to-kill stays ≈ 0.25 s.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
