---
title: Reef Spitter
design: approved
implementation: done
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-09
---

# Reef Spitter

## Summary

A slate barnacle gun grown on a bobbing kelp raft. It rotates to aim and fires three-way fans across the ocean lanes.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis. The numbers live in [data.yaml](data.yaml) (M5 part E, step E2b), with the raft's drift (`terrain.drift`, along its nest's `current`) of the [schemas](../../../tech/architecture/README.md#data-file-schemas); the table is rendered from it.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` (naval surface) |
| Size tier | `small` |
| Size | 36×36 px, hitbox 26×26 (the gun, on an 84 px raft) |
| Parts | single (the gun); the raft is its drawn base, scenery that sinks when the gun dies |
| Orientation | `32 angles` (the gun; the raft bobs, drawn only) |
| HP | 7 (easy 5 / hard 9, from the global multipliers) |
| Armour / shield | none |
| Speed | 10 px/s (the raft's drift on the current, on top of the sea's scroll) |
| Movement | `terrain` on water (bobbing drawn only) with a drift of 10 px/s along its nest's current |
| Attack | 3-way `fan` every 2.4 s (spread 30°, 150 px/s, `small` = 4) toward the player, 1 s after it enters the screen; recoil frame |
| Formations | turret nest (3–5) |
| Weak points | violet barrel root (drawn only) |
| Effective traits | `anti-ground`, `spread` |
| Credits | 14 (score 140 × chain) |
| Death | `small` burst; the raft sinks with a splash and ripple train |
| First level / used in | L11; Act 2 |
| Difficulty hooks | hard: 5-way fan |
<!-- /data -->

### Behaviour

- Naval surface units are ground-layer targets: no collision, hit by every weapon that reaches
  `ground` (bombs, mortar shells, the Airstrike and torpedoes included; `anti-ground` ×2).
- **Drift** (M5 part E): a raft is a ground unit placed by a ground target's nest that, besides
  scrolling with the sea, drifts at 10 px/s along its nest's current (° from straight down; default
  straight down, with the scroll). The bobbing is presentation only: the hit box stays on the
  drifting point.
- **Sinking**: when the gun dies the raft sinks in a splash and a ripple train over about 1 s, a
  presentation effect (the simulation removes the unit at once); no crater, no wreck, as every kill
  in a level with `water: true`.
- **Weak point drawn only** ([single-part rule](../../README.md#stat-block-template)): the violet
  barrel root glows but takes no extra damage; the draft's ×1.5 is struck.

### Concept art

Chosen concept: [reef-spitter-r06-a.png](../concept/reef-spitter-r06-a.png), [reef-spitter-r06-a.gif](../concept/reef-spitter-r06-a.gif) (listed in the [naval](../README.md#concept-art) Concept art table). M5 part E takes it straight to production (user decision E9 = a, concept round 33): the gun at 32 angles with its recoil frame, the raft's bob frames with foam collars, the sinking steps, the death and an intel portrait (`tools/art/reef_spitter.py`).

The production files (M5 part E, straight to production per E9 = a; approved as final in concept round 33), generator notes in [concept/prompts.md](concept/prompts.md):

| File | What | Status |
|---|---|---|
| [concept/reef-spitter-final-r33-a.png](concept/reef-spitter-final-r33-a.png) | Final sprites (`tools/art/reef_spitter.py`): the gun at 32 headings and its recoil frames (36×36) with the muzzle pivots, the raft for the `sub` pass, the raft above the waterline bobbing with its foam collar (`reef-spitter-raft_0..7`, 96×96), the sinking (`reef-spitter-sink_0..9`), the violet burst and the gun's pieces, each as drawn on a sea stand-in | chosen |
| [concept/reef-spitter-final-r33-a.gif](concept/reef-spitter-final-r33-a.gif) | A nest of three drifting rafts tracking a ship and firing fans, one shot: the gun bursts, the raft sinks | chosen |

## Implementation

- [x] Stat block values in a `data.yaml` loaded from data; global difficulty multipliers applied
      (M5 part E, step E2b)
- [x] The raft's 10 px/s drift on its nest's current (M5 part E, step E2b)
- [x] Gun tracking (32 angles) with the recoil frame; the raft bobbing (M5 part E, steps E1b and
      E3c: the raft's plain body in the `sub` pass, its bob frame on the surface, the gun at the
      frame's gun point, the recoil frame for 6 steps after a volley that left within 24 px of it)
- [x] Sink on death with water effects (M5 part E, steps E1b and E3c: the gun's tatters and glow,
      the small water burst and ripple train, the raft's ten sinking steps over its plain body)
- [x] Death effect, bounty and score per this spec; production sprites and intel portrait (M5
      part E, steps E1b and E3e; approved as final in round 33)

## Decisions

- 2026-10-01: Promoted from the naval roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L11 reference DPS (10 → 7, ×70/100).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-08: M5 part E (user decisions and stated defaults of 2026-10-08): the raft's drift and its
  sinking as specified above (the drift a key of the unit, the sinking a presentation effect of
  every kill on water); torpedoes reach it as every `ground` unit over water (default); its bounty
  14 in Act 1 terms, as written; the barrel root's ×1.5 struck by the single-part rule (a
  single-part unit's weak point is drawn only, [enemies](../../README.md#stat-block-template)). Our
  reading, for review in round 33: the raft is the gun's drawn base, not a part. `design` goes to `review`; `implementation` is `in-progress` for part
  E.
- 2026-10-08: M5 part E, step E2b (our readings, for review in round 33): its first volley 1.0 s
  after it enters the screen, as the Spine Turret's (the draft gave none); the fan aims at the ship
  at once (no barrel turn rate: the gun's 32 headings are drawn toward the ship); a raft that drifts
  off a side edge is gone, as one off the bottom edge.
- 2026-10-09: Concept round 33 closed (user): the production art (the gun at 32 headings with its recoil, the `-raft-sub` and eight `-raft` frames, the sinking, the
  death and tatters) and the intel picture approved as **final**, the weak spots as they are
  (the raft's cut edge, the dark intel picture); the stat block's numbers and the build choices of round 33 (the rafts on the flanks (d), the first volley 1.0 s after
  entering (e), the weak point drawn only (t)) accepted. `design: approved`, `art: final`;
  every Implementation item is ticked, so the implementation is `done`.
