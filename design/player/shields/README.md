---
title: Shields
design: draft
implementation: not-started
art: none
depends-on: [../generator, ../armor, ../../systems/retry]
updated: 2026-09-30
---

# Shields

## Summary

The shield absorbs damage before armour and regenerates after a short delay without taking hits.
It is the player's renewable buffer; [armour](../armor/README.md) is the finite one.

## Design

| Model | Capacity | Regen /s | Regen delay | Draw | Price (first draft) | Available |
|---|---|---|---|---|---|---|
| Mk I | 20 | 2 | 2.0 s | 2 MW | starter | start |
| Mk II | 30 | 3 | 1.8 s | 3 MW | 1 000 | act 1 |
| Mk III | 45 | 4 | 1.5 s | 4 MW | 3 000 | act 2 |
| Mk IV | 60 | 5 | 1.2 s | 5 MW | 7 000 | act 4 |
| Mk V | 80 | 6 | 1.0 s | 6 MW | 14 000 | act 5 |

Variant shields (roster, alternative to the Mk line):

| Name | Summary | Design |
|---|---|---|
| Reflex Shield | Capacity 40; bullets absorbed while above 75 % are reflected back upward. 6 MW, 12 000, act 6 | idea |
| Bastion Shield | Capacity 120, regen only 2/s, 3.0 s delay. For tanky builds. 5 MW, 9 000, act 5 | idea |

Rules:
- Damage goes to the shield first. Overflow damage in the same hit goes to armour.
- **Shield break**: when the shield hits 0 it stays down for an extra 1.0 s before the regen
  delay starts. An alarm plays and the HUD bar flashes.
- Spare generator power boosts the regen rate (see [generator](../generator/README.md)).
- Collisions deal half damage to shields and half directly to armour, so ramming stays risky.

## Implementation

- [ ] Shield capacity, regen, delay and break behaviour
- [ ] Damage routing shield → armour, collision split
- [ ] Shield hit/break feedback (sprite shimmer, SFX, HUD flash)

## Decisions

- 2026-09-30: Shield regenerates, armour does not (user decision).
