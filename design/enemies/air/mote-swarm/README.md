---
title: Mote Swarm
design: approved
implementation: not-started
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-01
---

# Mote Swarm

## Summary

A flock of 12–30 tiny ember-like motes that swirls in front of the player, sweeps off screen and loops back to dive from behind. Spread and area weapons shred it.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (1 = one Pulse Cannon L1 shot); bullet damage classes and speeds are defined in the balancing basis.

| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `air` |
| Size tier | `tiny` |
| Size | 16×16 px each, hitbox 10×10 |
| Parts | a flock of independent motes |
| Orientation | 16 angles |
| HP | 1 per mote (easy 1 / hard 1) |
| Armour / shield | none |
| Speed | 200 px/s (flock cruise), 260 px/s diving |
| Movement | `flock` (separation 18 px, alignment, cohesion toward a leader) following a leader route: swirl in front, exit at the bottom-left or bottom-right, `rear-entry` after 1.5 s (edge warning), dive up through the player's lane |
| Attack | `none` — contact (damage `tiny` = 6 per mote) |
| Formations | swarm, rear ambush |
| Weak points | none |
| Effective traits | `spread`, `area`, `rear` |
| Credits | 2 per mote (score 20 × chain); a full swarm of 20 ≈ 40 |
| Death | `tiny` ember puff |
| First level / used in | L10; returns in Act 3 (L20) |
| Difficulty hooks | flock size via the global formation lever (base 20: easy 16, hard 24) |

### Behaviour

- Chains are easy to build on a swarm, which makes it a score opportunity.
- Motes never fire; the swarm's danger is its loop-back path.

### Concept art

Chosen concept: [mote-swarm-r05-a.png](../concept/mote-swarm-r05-a.png), [mote-swarm-r05-a.gif](../concept/mote-swarm-r05-a.gif) (listed in the [air](../README.md#concept-art) Concept art table).

## Implementation

- [ ] Boids flock with leader route
- [ ] Off-screen loop and rear entry with warning
- [ ] Per-mote kills count toward chains
- [ ] Stat block values loaded from data; global difficulty multipliers applied
- [ ] Death effect, bounty and score per this spec

## Decisions

- 2026-10-01: Promoted from the air roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
