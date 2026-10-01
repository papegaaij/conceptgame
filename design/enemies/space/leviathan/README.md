---
title: Leviathan
design: approved
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Leviathan

## Summary

A huge bone-and-violet Vrell whale several times the player's length. It drifts over on `high-air` releasing Whirl Seed clusters, then descends to the play plane where its turrets, fins and blowhole can be shot. A set piece, not a boss.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `high-air` (first pass) → `air` (second pass) |
| Size tier | `huge` |
| Size | ≈480 px long, 300 px across the fins |
| Parts | body (`armoured`), 3 tail segments + fluke (articulated; fluke `destroyable`), 2 pectoral fins (`destroyable`), 4 dorsal turret vents (`destroyable`), blowhole (`vital`) |
| Orientation | 32 angles per part; articulated parts ±30° in 10 steps (model-space re-render pending, round 08) |
| HP | blowhole 150; turret vent 40 each; fin 60 each; fluke 80; total ≈ 550 (easy ×0.75 / hard ×1.3) |
| Armour / shield | body armoured (sparks); high-air pass: only `homing` and `beam` hit |
| Speed | drift 30 px/s; turns 10°/s |
| Movement | first pass: crosses diagonally on `high-air` in ≈ 12 s; second pass: descends (scale and shadow shrink to the air layer over 2 s) and drifts across the upper half for up to 30 s, then leaves |
| Attack | first pass: blowhole releases a whirl cluster of 6 Whirl Seeds every 3 s; second pass: each turret vent fires an `aimed` violet orb (130 px/s, `medium` = 6) every 2.2 s, staggered; fins sweep a 5-way `fan` (150 px/s) every 4 s; contact with the body `huge` = 25 |
| Formations | solo set piece (at most one per level, announced by radio) |
| Weak points | blowhole (violet glow, ×2; it is a regular enemy, so not lime) |
| Effective traits | `piercing`, `homing` (first pass), `forward` |
| Credits | turret vent 20 each, fin 15 each, fluke 20, blowhole 60 → 190 total; killing the blowhole first destroys the rest for the full 190 |
| Death | `huge`: chained `medium` bursts along the body, ichor cloud, whale-song cry; drops a large salvage pickup |
| First level / used in | L03 (Earth orbit); returns in Act 7 (L46) as a variant specified with that act |
| Difficulty hooks | hard: clusters of 8 seeds; vents fire 2-orb bursts |

### Behaviour

- On its first pass at L03 the player has no `homing` yet: the pass is a spectacle to survive (seed clusters), not a fight.
- A set piece pays close to a mid-boss (Act 1 L03 budget ≈ 1 145, mid-boss share 15 % ≈ 170).

### Concept art

Chosen concept: [leviathan-r05-a.png](../concept/leviathan-r05-a.png), [leviathan-r05-a.gif](../concept/leviathan-r05-a.gif) (listed in the [space](../README.md#concept-art) Concept art table).

## Implementation

- [ ] High-air pass with seed clusters; descent to the air layer
- [ ] Per-part HP and destroyable parts with articulated animation
- [ ] Chained death and large salvage drop
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the space roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
