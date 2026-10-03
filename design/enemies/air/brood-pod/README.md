---
title: Brood Pod
design: approved
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-03
---

# Brood Pod

## Summary

A slow, pulsing teal-black egg sac. It bursts into six Skitters when killed — or on its own after 8 seconds — so it rewards fast focused fire.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

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

## Concept art

Production art proposal for concept round 16 (M4 part D, the Level 04 batch; pending part D's doc gaps), review files built from the files in `assets/` by `tools/art/vrell_l04.py` (`--review pod` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/brood-pod-final-r16-a.png](concept/brood-pod-final-r16-a.png) | Final sprites: the 8-frame pulse loop (`brood-pod_0..7`, 64×64, 32 colours, veins brightening with the swell), the wet burst (`brood-pod-burst_0..11`, 96×96, additive) and the membrane tatters with rib shards (`brood-pod-tatters_0..11`, 96×96, solid), shown alone and with the medium burst | proposed |
| [concept/brood-pod-final-r16-a.gif](concept/brood-pod-final-r16-a.gif) | The pulse, the faster telegraph pulse, then the burst with six Skitters fanning out | proposed |

## Implementation

- [ ] Burst-on-kill and burst-on-timer with telegraph
- [ ] Skitter spawn arc
- [ ] Bounty depends on how it ended
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
