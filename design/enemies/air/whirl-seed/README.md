---
title: Whirl Seed
design: draft
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Whirl Seed

## Summary

A tiny six-fold spinning seed pod released in clusters by Leviathans and spawning reefs. Seeds spiral out, ricochet off the play-field edges and kill on contact; one hit pops them.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `tiny` |
| Size | 26×26 px, hitbox 16×16 |
| Parts | single |
| Orientation | `radial` (6-fold spinner, 8 frames per 60°) |
| HP | 1 (easy 1 / hard 1, from the global multipliers) |
| Armour / shield | none |
| Speed | 160 px/s; spin 1.5 turns/s |
| Movement | `spiral-out` from the release point (radius growth 90 px/s), then `ricochet` off the side edges up to 3 times; leaves through the bottom edge |
| Attack | `none` — contact (damage `tiny` = 6) |
| Formations | whirl cluster (5–8), stream |
| Weak points | none |
| Effective traits | `spread`, `area` |
| Credits | 3 (score 30 × chain) |
| Death | `tiny` pop: seed husk splits, teal glint |
| First level / used in | L03 (Leviathan), L12 (storm clusters); returns in Act 7 (L45) |
| Difficulty hooks | hard: each seed pops into a 3-bullet puff (90 px/s, `small` = 4) |

### Behaviour

- A cluster releases its seeds within 0.3 s at 360°/n spacing, so the spiral reads as one shape.
- Ricochets only on the side edges; seeds never bounce back up from the bottom.

### Concept art

Chosen concept: [whirl-seed-r05-a.png](../concept/whirl-seed-r05-a.png), [whirl-seed-r05-a.gif](../concept/whirl-seed-r05-a.gif) (listed in the [air](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Radial spin frames
- [ ] Spiral-out then ricochet with a bounce limit
- [ ] Hard-mode pop puff
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
