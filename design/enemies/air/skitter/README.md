---
title: Skitter
design: approved
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Skitter

## Summary

Swarm fodder of grown chitin: tiny, fast, one hit to kill. It teaches the player to shoot and to read a stream of enemies, and it pads most Vrell waves.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `tiny` |
| Size | 24×24 px, hitbox 16×16 |
| Parts | single |
| Orientation | `fixed` (flies nose-down; banks into curves) |
| HP | 1 (easy 1 / hard 1, from the global multipliers) |
| Armour / shield | none |
| Speed | 190 px/s (stream 160, swoop up to 240) |
| Movement | `snake` along an authored `path` (unit spacing 0.25 s), `swoop` (entry curve radius 120–200 px), `straight` in streams |
| Attack | `none` — contact only (contact damage `tiny` = 6) |
| Formations | snake (6–12), stream, line abreast (5–7), swarm |
| Weak points | none (dies in one hit) |
| Effective traits | `spread`, `forward` |
| Credits | 5 (score 50 × chain) |
| Death | `tiny` pop: chitin flakes, teal glow flash; no drop |
| First level / used in | L01; recurring fodder through Acts 1–2 and the Brood Pod / Brood Carrier / Hive Node spawns |
| Difficulty hooks | none beyond the global levers |

### Behaviour

- Skitters never fire. Their threat is the path: snakes that sweep across the player's lane and streams that alternate edges.
- A snake always enters from off-screen along its spline; the whole snake is visible within 1.5 s so the player can read it.
- Spawned Skitters (Brood Pod, Brood Carrier, Hive Node) leave their spawner on a short `swoop` toward the player, then continue `straight` and exit.

### Concept art

Chosen concept: [skitter-r04-a.png](../concept/skitter-r04-a.png) (listed in the [air](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Snake, stream and swarm entry paths authored as data
- [ ] Contact damage 6, split shield/armour per the collision rule
- [ ] Pays 5 credits; counts toward chains
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
