---
title: Spine Turret
design: review
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Spine Turret

## Summary

A grown Vrell turret: a slate bulb with a petal collar and a thorn barrel that rotates to track the player. The basic ground threat.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` |
| Size tier | `small` |
| Size | 40×40 px, hitbox 30×30 |
| Parts | single (barrel rotates) |
| Orientation | barrel 32 angles; body `fixed` |
| HP | 8 (easy 6 / hard 10, from the global multipliers) |
| Armour / shield | none |
| Speed | scrolls with the ground; barrel turn 90°/s |
| Movement | `terrain` |
| Attack | `aimed` yellow thorn every 2.0 s, 160 px/s, `small` = 4; stops firing when the player is behind it (more than 100° from its facing range) |
| Formations | turret nest (3–6) |
| Weak points | violet barrel root (×1.5) |
| Effective traits | `anti-ground` (×2), `forward` |
| Credits | 12 (score 120 × chain) |
| Death | `small` organic crumble; leaves a scorched stump on the ground layer |
| First level / used in | L02; recurring ground fodder in Acts 1–2 (incl. Luna and Vrell creep) |
| Difficulty hooks | hard: 2-shot bursts |

### Behaviour

- Ground layer: no collision with the player; hit by all weapons, ×2 from `anti-ground`.

### Concept art

Chosen concept: [spine-turret-r04-a.png](../concept/spine-turret-r04-a.png) (listed in the [ground](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Barrel tracking at 90°/s with 32 frames
- [ ] Aimed thorns
- [ ] Stump decal on death
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the ground roster to a full spec for the Acts 1–2 wrap-up.
