---
title: Shields
design: approved
implementation: done
art: final
depends-on: [../generator, ../armor, ../../systems/retry]
updated: 2026-10-05
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

## Concept art

Production art for concept round 26 (the M4 part H batch), review files built from the final frames in `assets/` by `tools/art/ship_fx.py` (`--review` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/shield-ring-final-r26-a.png](concept/shield-ring-final-r26-a.png) | Final shield-hit ring (`ship-shield_0..3`, 60×60, additive): round 08's hex shimmer on a round bubble, the cells lighting up around a hit at the front and a ripple running back, fading over 4 frames; alone and over the hull | chosen |
| [concept/shield-ring-final-r26-a.gif](concept/shield-ring-final-r26-a.gif) | The ring over the hull at the game's 2 steps a frame | chosen |

## Implementation

- [x] Shield capacity, regen, delay and break behaviour
- [x] Damage routing shield → armour, collision split
- [x] Shield hit/break feedback: the blue shimmer on the hull (`FlashShader`, `LevelRenderer`),
  the hit and break sounds (`Sfx.SHIELD_HIT`, `SHIELD_BREAK`), the HUD shield bar flickering while
  it is down after a break (`ShipPanel`)
- [x] The 60×60 4-frame hex-shimmer ring sprite on shield hits (the [ship](../ship/README.md)'s
  asset table; `ShipLooks`, frames by `tools/art/ship_fx.py`)
- [x] The shield-restore chime ([sfx](../../audio/sfx/README.md), part H's SFX pass: once the
  shield is full again after a break, `FlightSounds`)

## Decisions

- 2026-09-30: Shield regenerates, armour does not (user decision).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-01: M1 implementation (`vanguard.sim.Defences`, Mk I numbers in `ShieldModel`): every hit that lands (shield or armour) restarts the 2.0 s delay; a break holds the shield at 0 for 1.0 s + the delay; in a collision the shield's half overflows to armour like a bullet's. Feedback so far: shield-hit and break sounds, a blue shimmer on the hull, the HUD shield bar flickering while down after a break; the hex-ring shimmer sprite is still to come.
- 2026-10-02: The shield models and the break time moved into [data.yaml](data.yaml) (M2 data files); the model table is rendered from it and `vanguard.sim.ShieldModel` is built from it.
- 2026-10-05: M4 part H docs reconciliation: the feedback item is split into the built part (hull
  shimmer, hit and break sounds, the HUD bar's flicker) and what is still missing: the hex-ring
  sprite of the ship's asset table and the shield-restore chime. The document stays
  `in-progress`.
- 2026-10-05: M4 part H: the hex-shimmer ring is drawn additively over the ship on every shield
  hit, its 4 frames over the 8-step shimmer (2 steps each), with the hull's blue shimmer under it.
  The hit point is fixed at the front of the bubble (most bullets come from ahead) rather than at
  the bullet's side. Frames by `tools/art/ship_fx.py`, proposed for round 26 (`art: final`).
- 2026-10-05: Concept round 26 closed (user: the round accepted as proposed): the hex-shimmer ring approved as **final** (`art: final`), with the hit point at the front of the bubble and the 4-frame fade as they are; the restore chime is played by part H's SFX pass, so every item is ticked and the document is `done`.
