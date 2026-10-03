---
title: Brood Pod
design: approved
implementation: done
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-03
---

# Brood Pod

## Summary

A slow, pulsing teal-black egg sac. It bursts into six Skitters when killed — or on its own after 8 seconds — so it rewards fast focused fire.

## Design

### Stat block

The numbers live in [data.yaml](data.yaml). The table is still hand-written: `tools/sync_tables.py` cannot render a `spawn` attack yet (it has no bullet class). Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis.

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
| Movement | `drift` down the screen, slight `sine` (amplitude 20 px, 4 s per swing) |
| Attack | `spawn`: bursts into 6 Skitters when killed **or** 8 s after entering (the pulse speeds up over the last 3 s as a telegraph); the Skitters fly out straight at 160 px/s in a 120° arc toward the player. Contact damage `medium` = 15 |
| Formations | carrier + escorts (with 2–4 Needlers orbiting it as escorts), single, line abreast (2) |
| Weak points | teal veins (drawn only) |
| Effective traits | `forward`, `piercing` (focus) |
| Credits | 20 (score 200 × chain); 8 if it bursts on its own (score accordingly, not a kill); the Skitters pay their own 5 |
| Death | `medium` wet burst: membrane, ichor; the Skitters fan out in a 120° arc toward the player |
| First level / used in | L04; recurring in Acts 1–2 |
| Difficulty hooks | hard: 8 Skitters; easy: bursts after 10 s |

### Behaviour

- The lower bounty for a self-burst is shown in the debrief tally, teaching that speed matters.
- **Self-burst.** The 8 s run from the moment its centre crosses the top edge. A self-burst pays 8
  credits (score 80, no chain) and is **not a kill**: it counts toward no kill counter, does not
  extend the chain and fires no kill event. In a level whose secondary objective is an `escapes`
  objective on the Brood Pod (Level 04's *Quick hands*), a self-burst counts as an escape. A pod the
  player kills pays 20 and is a kill. Its Skitters are released either way.
- **Escorts.** In `carrier + escorts` the Needlers `orbit` the moving pod at their own orbit
  numbers (radius 90 px, 60°/s), the orbit's centre following the pod. When the pod dies or
  bursts they break off toward the ship one by one, 0.5 s apart, like a `circle`'s break.
- Killing it with an `area` weapon still releases the Skitters, but they spawn damaged (die to anything).

### Concept art

Chosen concept: [brood-pod-r04-a.png](../concept/brood-pod-r04-a.png) (listed in the [air](../README.md#concept-art) Concept art table).

## Concept art

Production art proposal for concept round 16 (M4 part D, the Level 04 batch; pending part D's doc gaps), review files built from the files in `assets/` by `tools/art/vrell_l04.py` (`--review pod` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/brood-pod-final-r16-a.png](concept/brood-pod-final-r16-a.png) | Final sprites: the 8-frame pulse loop (`brood-pod_0..7`, 64×64, 32 colours, veins brightening with the swell), the wet burst (`brood-pod-burst_0..11`, 96×96, additive) and the membrane tatters with rib shards (`brood-pod-tatters_0..11`, 96×96, solid), shown alone and with the medium burst | chosen |
| [concept/brood-pod-final-r16-a.gif](concept/brood-pod-final-r16-a.gif) | The pulse, the faster telegraph pulse, then the burst with six Skitters fanning out | chosen |

## Implementation

- [x] Burst-on-kill and burst-on-timer with telegraph
- [x] Skitter spawn arc
- [x] Bounty depends on how it ended
- [x] Stat block values loaded from data; global difficulty multipliers applied
- [x] Self-burst: 8 credits, not a kill, an escape for an `escapes` objective
- [x] Needler escorts orbit the moving pod and break off when it dies or bursts
- [x] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-03: Concept round 16 closed (user decision): the production sprites (pulse loop, wet burst, membrane tatters) approved as **final**, `art: final`, ahead of M4 part D. Part D's doc gaps may still require changes to them; those go through a later round.
- 2026-10-03: Part D doc gaps settled (main-agent choices following earlier decisions): the
  numbers moved into [data.yaml](data.yaml) (`spawn` attack, `drift` and `sine` movement, hard
  `spawn_count` and easy `spawn_after` hooks, since `burst` is taken); the weak point is drawn only
  (single-part rule), so the ×1.5 is gone, and it is the teal veins of the chosen concept; the stat
  block's 64×64 wins over the concept's 48 px (the final sprites are 64×64); `line abreast` (2) added
  to the formations (Level 04, t=150). A self-burst pays 8, is not a kill and counts as an escape for
  *Quick hands*; the escorts orbit the moving pod and break off like a circle. Chosen here: the
  sine's 4 s swing, the timer starting when the centre crosses the top edge, the released Skitters
  flying straight out at 160 px/s, and the escorts breaking off when the pod ends.
- 2026-10-03: M4 part D step 3, built: `drift` with the `sine` offset, the self-burst timer from
  the centre crossing the top edge (exposed to the renderer, which plays the pulse up to three times
  faster over the last 3 s), the Skitters released from its centre evenly over the 120° arc centred
  on the ship (end units on the arc's edges), pooled ordinary Skitter units flying straight at
  160 px/s, the self-burst's 8 credits scored at the kill score without the chain and counted as an
  escape, the `spawn_count` / `spawn_after` hooks, and the Needler escorts (`carrier + escorts`:
  the wave's carrier group first; each escort circles the carrier that entered last, 120° apart, and
  breaks off toward the ship 0, 0.5 and 1 s after the pod ends). The death is the wet burst
  (`brood-pod-burst`, read as its additive death glow) over the medium explosion and the solid
  tatters, also for a self-burst, with the fleshy-burst sound (enemy-spawn-r08-b). Not built: the
  area-kill rule ("its Skitters spawn damaged") has no effect while a Skitter has 1 HP.
