---
title: Shields
design: approved
implementation: in-progress
art: none
depends-on: [../generator, ../armor, ../../systems/retry]
updated: 2026-10-02
---

# Shields

## Summary

The shield absorbs damage before armour and regenerates after a short delay without taking hits.
It is the player's renewable buffer; [armour](../armor/README.md) is the finite one.

## Design

<!-- data: shields -->
| Model | Capacity | Regen /s | Regen delay | Draw | Price (first draft) | Available |
|---|---|---|---|---|---|---|
| Mk I | 20 | 2 | 2.0 s | 2 MW | starter | start |
| Mk II | 30 | 3 | 1.8 s | 3 MW | 1 000 | act 1 |
| Mk III | 45 | 4 | 1.5 s | 4 MW | 3 000 | act 2 |
| Mk IV | 60 | 5 | 1.2 s | 5 MW | 7 000 | act 4 |
| Mk V | 80 | 6 | 1.0 s | 6 MW | 14 000 | act 5 |
<!-- /data -->

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

- [x] Shield capacity, regen, delay and break behaviour
- [x] Damage routing shield → armour, collision split
- [ ] Shield hit/break feedback (sprite shimmer, SFX, HUD flash)

## Decisions

- 2026-09-30: Shield regenerates, armour does not (user decision).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-01: M1 implementation (`vanguard.sim.Defences`, Mk I numbers in `ShieldModel`): every hit that lands (shield or armour) restarts the 2.0 s delay; a break holds the shield at 0 for 1.0 s + the delay; in a collision the shield's half overflows to armour like a bullet's. Feedback so far: shield-hit and break sounds, a blue shimmer on the hull, the HUD shield bar flickering while down after a break; the hex-ring shimmer sprite is still to come.
- 2026-10-02: The shield models and the break time moved into [data.yaml](data.yaml) (M2 data files); the model table is rendered from it and `vanguard.sim.ShieldModel` is built from it.
