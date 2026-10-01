---
title: Hive Node
design: review
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Hive Node

## Summary

A hardened teal-black spawner mound grown into the ruins. Its iris opens every few seconds to release Skitters until it is destroyed — and only `anti-ground` weapons can hurt it.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` |
| Size tier | `medium` |
| Size | 76×76 px, hitbox 56×56 |
| Parts | single |
| Orientation | `radial` |
| HP | 64 (easy 48 / hard 83, from the global multipliers) |
| Armour / shield | **hardened**: only `anti-ground` weapons (and the Airstrike) damage it; other shots spark off |
| Speed | scrolls with the ground |
| Movement | `terrain` |
| Attack | `spawn`: iris opens every 4.0 s (0.5 s telegraph) and releases 2 Skitters; no direct fire |
| Formations | turret nest (2–4 nodes per objective) |
| Weak points | the open iris (×2, only while open) |
| Effective traits | `anti-ground` (required), Airstrike |
| Credits | 45 (score 450 × chain); spawned Skitters pay 5 |
| Death | `large` organic collapse; creep around it withers over 2 s |
| First level / used in | L09 (primary `destroy-targets` objective); Act 2 |
| Difficulty hooks | hard: 3 Skitters per opening |

### Behaviour

- The hardened rule is taught in L09's briefing and by the spark effect on the first non-anti-ground hit.

### Concept art

Chosen concept: [hive-node-r06-a.png](../concept/hive-node-r06-a.png), [hive-node-r06-a.gif](../concept/hive-node-r06-a.gif) (listed in the [ground](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Hardened damage rule with spark feedback
- [ ] Iris spawn cycle
- [ ] Creep wither on death
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the ground roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L09 reference DPS (80 → 64, ×63/80); time-to-kill stays ≈ 1.0 s.
