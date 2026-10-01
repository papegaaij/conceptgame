---
title: Brood Pod
design: approved
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Brood Pod

## Summary

A slow, pulsing teal-black egg sac. It bursts into six Skitters when killed — or on its own after 8 seconds — so it rewards fast focused fire.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `medium` |
| Size | 64×64 px, hitbox 50×50 |
| Parts | single |
| Orientation | `fixed` (pulse animation) |
| HP | 30 (easy 22 / hard 39, from the global multipliers) |
| Armour / shield | none |
| Speed | 40 px/s |
| Movement | `drift` down the screen, slight `sine` (amplitude 20 px) |
| Attack | `spawn`: bursts into 6 Skitters when killed **or** 8 s after entering (the pulse speeds up over the last 3 s as a telegraph). Contact damage `medium` = 15 |
| Formations | carrier + escorts (with 2–4 Needlers), single |
| Weak points | teal veins (×1.5) |
| Effective traits | `forward`, `piercing` (focus) |
| Credits | 20 if killed before it bursts on its own, 8 if it bursts (score accordingly); the Skitters pay their own 5 |
| Death | `medium` wet burst: membrane, ichor; the Skitters fan out in a 120° arc toward the player |
| First level / used in | L04; recurring in Acts 1–2 |
| Difficulty hooks | hard: 8 Skitters; easy: bursts after 10 s |

### Behaviour

- The lower bounty for a self-burst is shown in the debrief tally, teaching that speed matters.
- Killing it with an `area` weapon still releases the Skitters, but they spawn damaged (die to anything).

### Concept art

Chosen concept: [brood-pod-r04-a.png](../concept/brood-pod-r04-a.png) (listed in the [air](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Burst-on-kill and burst-on-timer with telegraph
- [ ] Skitter spawn arc
- [ ] Bounty depends on how it ended
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
