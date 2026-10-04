---
title: Ship systems
design: approved
implementation: not-started
art: none
depends-on: [../generator, ../../ui/hangar]
updated: 2026-10-04
---

# Ship systems

## Summary

The engine (a core component, always fitted) and the modules that go into the **utility bays**:
two bays at the start, and a third that can be bought from act 3 (price 5 000). Utility modules
are the "nice extra options": they make the ship better at a job without adding another gun.

## Design

### Engine (core)

<!-- data: engines -->
| Model | Speed | Draw | Price (first draft) | Available |
|---|---|---|---|---|
| Mk I | 270 px/s | 0 MW | starter | start |
| Mk II | 290 px/s | 1 MW | 1 200 | act 1 |
| Mk III | 315 px/s | 1 MW | 3 500 | act 3 |
| Mk IV | 345 px/s | 2 MW | 8 000 | act 5 |
<!-- /data -->

### Utility modules

<!-- data: utility -->
| Module | Effect | Levels | Draw | Price | Unlock | Design |
|---|---|---|---|---|---|---|
| Sensor suite | Improves hangar intel detail (see below) and shows off-screen threat arrows at L2+ | L1–L3 | 1 | 800 / 2 000 / 4 500 | start | idea |
| Pickup magnet | Pickup radius 36 → 72 / 108 / 144 px | L1–L3 | 1 | 600 / 1 500 / 3 000 | act 1 | idea |
| Salvage scanner | +10 / +20 % credits from drops; reveals hidden crates | L1–L2 | 1 | 2 500 / 6 000 | act 2 | idea |
| Targeting computer | Homing turn rate +20 %, enemy HP bars, boss weak-point markers | L1 | 1 | 3 000 | act 2 (from L07 with the L06 [data core](../../systems/economy/README.md#data-cores)) | idea |
| Evasive thrusters | Double-tap direction: 72 px dash, 0.25 s invulnerable, 3 s cooldown | L1 | 2 | 4 000 | act 3 | idea |
| Auto-repair nanites | Repairs 1 armour per 4 s, up to 50 % of max armour | L1–L2 (2 s at L2) | 3 | 6 000 / 12 000 | act 4 | idea |
| Pressure hull | Removes the underwater top-speed and shield-regen penalties (see [europa](../../world/europa/README.md#under-water-rules)) | L1 | 1 | 2 000 | L22 | idea |
| Ascendancy IFF spoofer | Ascendancy turrets hesitate 0.5 s before firing | L1 | 2 | 5 000 | act 6 (story) | idea |
<!-- /data -->

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
| L1 | + all attack directions with the share of waves per direction, density (1–5), hazards, and an OBJECTIVE field in a level with one (e.g. `ESCORT 5 CRAWLERS`) |
| L2 | + enemy types with portraits, boss name and silhouette, a set piece as an "unknown huge contact" with its silhouette, special availability |
| L3 | + recommended weapon traits highlighted in the shop, wave timeline strip, secret count |

Dr. Varga speaks one line per intel item. With low sensors, her lines are more uncertain
("Our scans are patchy, Lancer…").

### Utility bays (confirmed)

The ship has **two utility bays**; a **third** can be bought (from Act 3, see
[player](../README.md)). Every system on this page occupies one bay.

## Implementation

- [x] Engine speed per model
- [ ] Utility bays (2, third purchasable) and module effects
- [x] Sensor level controls the intel panel detail
- [ ] Underwater penalties and the pressure hull

## Open questions

- Is the underwater speed/shield penalty desirable? It makes Europa feel different and gives
  the pressure hull a reason to exist, but it also punishes players who skip it.

## Decisions

- 2026-09-30: Extra options live in utility bays so they compete with each other, not with guns.
- 2026-09-30: The hydro-kit is an automatic free refit; the pressure hull is an optional L22 module
  (underwater rules owned by [europa](../../world/europa/README.md#under-water-rules)).
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-10-01: Utility bays confirmed: two, a third buyable.
- 2026-10-01: Targeting computer: unlocked from L07 by the L06 data core (one act early), per the data-core rule in economy.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-02: The engines and utility modules moved into [data.yaml](data.yaml) (M2 data files); both tables are rendered from it.
- 2026-10-02: M3 part B2: engines and utility modules are sold in the hangar; the fitted engine's
  speed flies. The intel shows what the best fitted sensor suite's level plus the difficulty's
  sensor bonus (easy +1) allows, at most L3 (`vanguard.content.campaign.Intel`). Two utility bays;
  the same module may be fitted in both (the document does not forbid it). The modules' flight
  effects (magnet, scanner, threat arrows, …) and the third bay (Act 3) follow.
- 2026-10-02 (user decision): sensor L2 also shows a level's set pieces, unnamed, as an unknown
  contact of their size tier with their silhouette (Level 03's Leviathan: "unknown huge contact";
  see [hangar](../../ui/hangar/README.md#decisions)).
- 2026-10-03: Sensor L1 also shows the level's objective as an OBJECTIVE field (main-agent choice, M4 part D: Level 04's intel already promised "escort: 5 crawlers" at L1).
- 2026-10-04: The L06 data core's unlock of the Targeting computer is recorded in the save from
  M4 part F; the module itself comes with the utility modules in M5 and is in the shop from the
  first hangar visit after that for a save that holds the unlock (user decision D6 of M4 part F;
  see [economy](../../systems/economy/README.md#data-cores)).
