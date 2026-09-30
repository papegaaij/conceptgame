---
title: Ship systems
design: draft
implementation: not-started
art: none
depends-on: [../generator, ../../ui/hangar]
updated: 2026-09-30
---

# Ship systems

## Summary

The engine (a core component, always fitted) and the modules that go into the **utility bays**:
two bays at the start, and a third that can be bought from act 3 (price 5 000). Utility modules
are the "nice extra options": they make the ship better at a job without adding another gun.

## Design

### Engine (core)

| Model | Speed | Draw | Price (first draft) | Available |
|---|---|---|---|---|
| Mk I | 180 px/s | 0 MW | starter | start |
| Mk II | 195 px/s | 1 MW | 1 200 | act 1 |
| Mk III | 210 px/s | 1 MW | 3 500 | act 3 |
| Mk IV | 230 px/s | 2 MW | 8 000 | act 5 |

### Utility modules

| Module | Effect | Levels | Draw | Price | Unlock | Design |
|---|---|---|---|---|---|---|
| Sensor suite | Improves hangar intel detail (see below) and shows off-screen threat arrows at L2+ | L1–L3 | 1 | 800 / 2 000 / 4 500 | start | idea |
| Pickup magnet | Pickup radius 24 → 48 / 72 / 96 px | L1–L3 | 1 | 600 / 1 500 / 3 000 | act 1 | idea |
| Salvage scanner | +10 / +20 % credits from drops; reveals hidden crates | L1–L2 | 1 | 2 500 / 6 000 | act 2 | idea |
| Targeting computer | Homing turn rate +20 %, enemy HP bars, boss weak-point markers | L1 | 1 | 3 000 | act 2 | idea |
| Evasive thrusters | Double-tap direction: 48 px dash, 0.25 s invulnerable, 3 s cooldown | L1 | 2 | 4 000 | act 3 | idea |
| Auto-repair nanites | Repairs 1 armour per 4 s, up to 50 % of max armour | L1–L2 (2 s at L2) | 3 | 6 000 / 12 000 | act 4 | idea |
| Pressure hull | Removes the underwater top-speed and shield-regen penalties (see [europa](../../world/europa/README.md#under-water-rules)) | L1 | 1 | 2 000 | L22 | idea |
| Ascendancy IFF spoofer | Ascendancy turrets hesitate 0.5 s before firing | L1 | 2 | 5 000 | act 6 (story) | idea |

### Hydro-kit (automatic)

Not a module and not for sale: before L23 every Stormhawk gets a free field refit for
underwater play. It takes no slot and draws no power. What changes under water is defined in
[europa](../../world/europa/README.md#under-water-rules).

### Sensor levels and hangar intel

The [hangar intel panel](../../ui/hangar/README.md) shows the next level's threat profile. The
sensor suite decides how much of it is visible:

| Sensor | Intel shown |
|---|---|
| none | Setting, dominant layers, main attack direction |
| L1 | + all attack directions with the share of waves per direction, density (1–5), hazards |
| L2 | + enemy types with portraits, boss name and silhouette, special availability |
| L3 | + recommended weapon traits highlighted in the shop, wave timeline strip, secret count |

Dr. Varga speaks one line per intel item. With low sensors, her lines are more uncertain
("Our scans are patchy, Lancer…").

## Implementation

- [ ] Engine speed per model
- [ ] Utility bays (2, third purchasable) and module effects
- [ ] Sensor level controls the intel panel detail
- [ ] Underwater penalties and the pressure hull

## Open questions

- Is the underwater speed/shield penalty desirable? It makes Europa feel different and gives
  the pressure hull a reason to exist, but it also punishes players who skip it.

## Decisions

- 2026-09-30: Extra options live in utility bays so they compete with each other, not with guns.
- 2026-09-30: The hydro-kit is an automatic free refit; the pressure hull is an optional L22 module
  (underwater rules owned by [europa](../../world/europa/README.md#under-water-rules)).
