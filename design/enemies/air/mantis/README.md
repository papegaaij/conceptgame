---
title: Mantis
design: review
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Mantis

## Summary

A bone-white Vrell sniper that enters from a side edge, holds position and sweeps a short laser across the lower screen. The first enemy that demands `side` or `spread` weapons.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `medium` |
| Size | 80×80 px, hitbox 40×56 |
| Parts | single |
| Orientation | `fixed` facing inward (mirrored per side) |
| HP | 26 (easy 20 / hard 34, from the global multipliers) |
| Armour / shield | none |
| Speed | 150 px/s entering |
| Movement | enters from the left or right edge (edge warning 1.5 s), `hover` at x = 40 px from the edge, y = 120–360 px, for 6 s, then exits the way it came |
| Attack | `laser-sweep` through a 70° arc over 1.2 s, telegraphed 0.6 s (crimson arc shown), every 3.0 s while hovering; beam damage `laser` = 8 per touch (once per sweep) |
| Formations | pincer (one from each side), single |
| Weak points | crimson thorax glow (×1.5) |
| Effective traits | `side`, `spread` |
| Credits | 30 (score 300 × chain) |
| Death | `medium` burst: bone shards, crimson flash |
| First level / used in | L06; Acts 1–2 |
| Difficulty hooks | hard: 90° arc, every 2.5 s |

### Behaviour

- The Mantis hovers outside the reach of forward guns unless the player moves close to the edge — the intended dilemma.
- Never fires from off-screen; it must be fully visible before the first telegraph.

### Concept art

Chosen concept: [mantis-r04-a.png](../concept/mantis-r04-a.png) (listed in the [air](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Side entry with edge warning
- [ ] Laser sweep with 0.6 s arc telegraph
- [ ] Exit after 6 s
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
