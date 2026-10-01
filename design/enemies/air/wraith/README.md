---
title: Wraith
design: draft
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Wraith

## Summary

A rust-and-blue-violet Vrell manta ghost that passes overhead cloaked, loops behind the player and decloaks to attack up the screen. The signature rear attacker of Act 2.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `high-air` while cloaked → `air` when decloaked |
| Size tier | `medium` |
| Size | 72×72 px, hitbox 50×40 |
| Parts | single |
| Orientation | 16 angles (4 ripple frames) |
| HP | 27 (easy 20 / hard 35, from the global multipliers) |
| Armour / shield | cloaked: only `homing` and `beam` hit it (high-air rule) |
| Speed | 200 px/s cloaked, 120 px/s decloaked |
| Movement | cloaked `swoop` down past the player, `loop` behind, `rear-entry` at y = 470–520 px (edge warning 1.5 s plus Rook's "contacts on six"), decloak (0.4 s violet flash), hold 2.5 s, exit up the screen |
| Attack | 5-shot `burst` up the screen (0.12 s apart, 220 px/s, `medium` = 6) aimed at the player, twice while decloaked |
| Formations | rear ambush (2–4) |
| Weak points | blue-violet veins (×1.5) |
| Effective traits | `rear`, `homing` (also hits it while cloaked) |
| Credits | 30 (score 300 × chain) |
| Death | `medium` burst: membrane tatters, violet flash |
| First level / used in | L10; launched by the Siege Spire (L14) |
| Difficulty hooks | hard: 7-shot bursts; holds 3.0 s |

### Behaviour

- Its colours are rust chitin with blue-violet veins everywhere (user decision, round 06), including when the Siege Spire launches it.
- The cloaked shimmer is visible (refraction), so attentive players see it coming before the warning.

### Concept art

Chosen concept: [wraith-r06-a.png](../concept/wraith-r06-a.png), [wraith-r06-a.gif](../concept/wraith-r06-a.gif) (listed in the [air](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Cloak (high-air) → decloak (air) state change
- [ ] Rear entry with edge warning and radio cue
- [ ] Upward bursts
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L10 reference DPS (36 → 27, ×66/90, rounded up to keep the 0.4 s medium minimum); time-to-kill ≈ 0.41 s.
