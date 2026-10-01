---
title: Stinger
design: draft
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Stinger

## Summary

A rust-red wasp-like diver: it enters, locks on and dives past the player, firing a fan at the bottom of its dive. Teaches dodging sideways.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `small` |
| Size | 40×40 px, hitbox 24×30 |
| Parts | single |
| Orientation | `fixed` (rotates up to ±30° toward the dive vector) |
| HP | 6 (easy 4 / hard 8, from the global multipliers) |
| Armour / shield | none |
| Speed | 100 px/s entering; dive 420 px/s |
| Movement | `dive`: enter to y = 90–160 px, pause 0.5 s (crimson glow flares as telegraph), dive at the player's position, continue off the bottom edge |
| Attack | 3-way `fan` (spread 30°, 170 px/s, damage `small` = 4) when it passes the player's height or after 0.6 s of dive |
| Formations | V-wing (3–5, diving one after another 0.4 s apart), column |
| Weak points | glowing crimson abdomen (×1.5) |
| Effective traits | `forward`, `spread`; `rear` catches it after the dive |
| Credits | 15 (score 150 × chain) |
| Death | `small` burst: rust shards, crimson flash |
| First level / used in | L02; recurring diver through Acts 1–2 |
| Difficulty hooks | hard: fan 5-way; dive pause 0.4 s |

### Behaviour

- The pause and glow flare are the telegraph; the dive vector is fixed at the moment the pause ends, so sidestepping works.
- A Stinger that passes the player without dying exits; it never returns.

### Concept art

Chosen concept: [stinger-r04-a.png](../concept/stinger-r04-a.png) (listed in the [air](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Dive with telegraph pause and locked vector
- [ ] Fan fired at the bottom of the dive
- [ ] Exits after the dive
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
