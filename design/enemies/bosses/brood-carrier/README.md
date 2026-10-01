---
title: Brood Carrier
design: review
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Brood Carrier

## Summary

The Act 1 boss: a teal-black living carrier about one screen long. It first passes overhead launching Skitters and Needlers, then turns broadside with lime bay sacs open between fan volleys, and finally exposes its core.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `high-air` (phase 1) → `air` (phases 2–3) |
| Size tier | `huge` (act boss) |
| Size | 288×626 px (about one screen long) |
| Parts | hull (`armoured`), 8 launch bays with bay sacs (`destroyable`), core under a plate iris (`vital`, phase 3), head mandible turrets (`armoured`, fire only) |
| Orientation | hull fixed nose-down in phase 1, 32 angles turning broadside; turrets 32 angles |
| HP | bay sac 180 each (1 440); core 2 400; total 3 840 (easy ×0.75 / hard ×1.3) |
| Armour / shield | hull armoured; phase 1 on `high-air`: only `homing` and `beam` hit (damage to bays carries over) |
| Speed | phase 1 crosses at 40 px/s; phases 2–3 hold position |
| Movement | see phases |
| Attack | see phases |
| Formations | carrier + escorts |
| Weak points | lime bay sacs (×1.5) and lime core (×2) |
| Effective traits | `piercing` (bays in a row), `spread`, `homing` |
| Credits | bays 25 each, core 250 → 450 (≈ 30 % of the L07 budget, 1 501) |
| Death | `huge`: chained explosions from tail to head over 3 s, screen flash, credit shower |
| First level / used in | L07; echo version in the L49 boss rush |
| Difficulty hooks | hard: phase 3 spiral has 4 arms; each bay pair spawns 1 extra Skitter |

### Phases

| Phase | Ends at | Behaviour |
|---|---|---|
| 1 — Overhead pass | timed, 25 s | The hull passes over on `high-air` (drawn opaque with its shadow, per the art-direction high-air rule); bays open in pairs every 3 s, and each opened **pair** together `spawn`s 4 Skitters or 2 Needlers, alternating (8 pair openings: 16 Skitters, 8 Needlers). Only `homing`/`beam` reach the hull; the fight is about clearing the spawns. |
| 2 — Broadside | bay sacs 1 440 → 0 (or 70 s) | It descends to `air` and turns broadside across the upper half. Head turrets fire 5-way `fan`s (spread 50°, 150 px/s, `small` = 4) every 2.4 s; between volleys 2 bay sacs open for 2 s at a time. Destroyed bays stop spawning. |
| 3 — Core | core 2 400 → 0 | The plate iris opens; the core fires a 3-arm `spiral` (rotation 90°/s, 120 px/s, 8 bullets/s) and every 4 s a 16-bullet `ring`. If phase 2 timed out, remaining bays keep spawning Skitters every 6 s. |

### Arena

Open space at the Earth–Moon L1 point, beyond the Vrell picket ([L07](../../../campaign/act-1-first-contact/level-07-brood-carrier/README.md)): scroll slows to 20 px/s during the fight. Target duration 90–150 s at medium.

### Behaviour

- Phase 2 ends when all sacs are gone or after 70 s, so a weak build is never stuck forever.
- The size question (one screen vs two) is settled: about one screen, as chosen in round 03.

### Concept art

Chosen concept: [brood-carrier-r04-a.png](../concept/brood-carrier-r04-a.png) (listed in the [bosses](../README.md#concept-art) Concept art table).

## Implementation

- [ ] High-air pass with spawns; descent and broadside turn
- [ ] Bay sac windows between fan volleys
- [ ] Core phase with spiral and rings; 3-s chained death
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the bosses roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Arena set to the Earth–Moon L1 point (was Earth orbit above Gagarin), matching story, campaign and Luna docs; phase-1 spawns are per opened bay pair, as L07 assumes.
