---
title: Creeper
design: draft
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Creeper

## Summary

A slate six-legged salamander with a violet fan gland that crawls in convoys along roads and rooftops, firing five-way fans. The Act 2 ground walker.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` |
| Size tier | `medium` |
| Size | 60×60 px, hitbox 40×44 |
| Parts | single |
| Orientation | 16 angles × 6 walk phases |
| HP | 36 (easy 27 / hard 47, from the global multipliers) |
| Armour / shield | none |
| Speed | 35 px/s (plus scroll) |
| Movement | `crawl` along roads, rooftops and walls; follows road splines |
| Attack | 5-way `fan` toward the player every 3.0 s (spread 50°, 140 px/s, `small` = 4) |
| Formations | convoy (3–5) |
| Weak points | violet fan gland (×1.5) |
| Effective traits | `anti-ground` (×2), `area` |
| Credits | 22 (score 220 × chain) |
| Death | `medium` organic burst |
| First level / used in | L08; Act 2 |
| Difficulty hooks | hard: fan 7-way, every 2.6 s |

### Behaviour

- Convoys space their fans 0.5 s apart so the volley reads as a wave, not a wall.

### Concept art

Chosen concept: [creeper-r06-a.png](../concept/creeper-r06-a.png), [creeper-r06-a.gif](../concept/creeper-r06-a.gif) (listed in the [ground](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Road-spline crawling
- [ ] Staggered convoy fans
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the ground roster to a full spec for the Acts 1–2 wrap-up.
