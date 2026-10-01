---
title: Reef Spitter
design: review
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Reef Spitter

## Summary

A slate barnacle gun grown on a bobbing kelp raft. It rotates to aim and fires three-way fans across the ocean lanes.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` (naval surface) |
| Size tier | `small` (gun) on a `medium` raft |
| Size | gun 36×36 px on an 84 px raft; hitbox the gun 26×26 |
| Parts | gun (`vital`), raft (scenery, sinks when the gun dies) |
| Orientation | gun 32 angles |
| HP | 7 (easy 5 / hard 9, from the global multipliers) |
| Armour / shield | none |
| Speed | raft drifts 10 px/s with the current |
| Movement | `terrain` on water (bobbing) |
| Attack | 3-way `fan` every 2.4 s (spread 30°, 150 px/s, `small` = 4) toward the player; recoil frame |
| Formations | turret nest (3–5 rafts) |
| Weak points | violet barrel root (×1.5) |
| Effective traits | `anti-ground` (×2), `spread` |
| Credits | 14 (score 140 × chain) |
| Death | `small` burst; the raft sinks with a splash and ripple train |
| First level / used in | L11; Act 2 |
| Difficulty hooks | hard: 5-way fan |

### Behaviour

- Naval surface units are ground-layer targets: no collision, hit by all weapons.

### Concept art

Chosen concept: [reef-spitter-r06-a.png](../concept/reef-spitter-r06-a.png), [reef-spitter-r06-a.gif](../concept/reef-spitter-r06-a.gif) (listed in the [naval](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Bobbing raft with gun tracking
- [ ] Sink on death with water effects
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the naval roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L11 reference DPS (10 → 7, ×70/100).
