---
title: Coilwyrm
design: approved
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Coilwyrm

## Summary

A rust-and-teal Vrell serpent of a head, twelve segments and a tail that swirls, loops and figure-eights across the screen, often coming back from the rear. The first segment chain: cut it and the tail end grows a new head — once.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `large` (≈330 px along its path) |
| Size | head 58 px, segments 54→27 px, tail 40 px; hitboxes 70 % of sprite |
| Parts | head (`vital`), 12 body segments (`destroyable`), tail (`destroyable`, pays a bonus if destroyed first) |
| Orientation | 16 angles per part type (head, segment, tail); model-space re-render pending (round 08) |
| HP | head 40 (easy 30 / hard 52, from the global multipliers); each segment 4; tail 10 |
| Armour / shield | none |
| Speed | 180 px/s (regrown chain 220 px/s) |
| Movement | head follows an authored `swirl`, `loop` or `figure-8` path; segments `chain` on the head's path history at 0.9 × segment length spacing; frequent `loop` + `rear-entry` (edge warning 1.5 s) |
| Attack | head: 3-way `fan` every 2.0 s (spread 24°, 160 px/s, `small` = 4); contact damage `large` = 20 (head) / `small` = 10 (segments) |
| Formations | snake (solo), pairs crossing |
| Weak points | head teal crest (×2) |
| Effective traits | `piercing` (hits several segments), `spread`, `rear` for the loop-backs |
| Credits | head 40, segment 3 each, tail 10 (+10 if first); whole ≈ 96 (score × 10 × chain) |
| Death | head: `medium` burst and a chained `small` pop down the body (0.06 s per segment); segments: `tiny` pops |
| First level / used in | L06; returns in Act 7 (L44) |
| Difficulty hooks | hard: 14 segments; the regrown head fires a 5-way fan |

### Behaviour

- **Splitting:** destroying a body segment splits the chain. The rear part grows a new head over 0.6 s (it cannot be damaged during regrowth) and becomes its own, faster chain. A regrown chain that is cut again does not regrow: the severed part dies from the cut backwards.
- Killing the original head first destroys the whole chain with the chained explosion and a time bonus (score only).
- Segments overlap 20–30 % so the chain reads as one body in tight turns.

### Concept art

Chosen concept: [coilwyrm-r05-a.png](../concept/coilwyrm-r05-a.png), [coilwyrm-r05-a.gif](../concept/coilwyrm-r05-a.gif) (listed in the [air](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Path-history chain with per-segment angles
- [ ] Split once with regrowth, second cut kills the severed part
- [ ] Chained death explosion
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
