---
title: Torpedo Pod
design: approved
implementation: done
art: final
depends-on: [.., ../../generator, ../../../systems/economy]
updated: 2026-10-09
---

# Torpedo Pod

## Summary

Wing pod that launches torpedoes into the water. Hits submerged and naval targets; optional in Act 2, essential in Act 4.

## Design

<!-- data: weapon-properties -->
| Property | Value |
|---|---|
| Slot | Wing mount (per pod) |
| Traits | `anti-sub` |
| Layers hit | the `sub` layer and naval `ground` (surface) targets only; full damage under water ([layer rules](../../../enemies/README.md#layer-rules)) |
| Price | 1 000 cr |
| Upgrade cost L2 / L3 / L4 / L5 | 500 / 1 000 / 2 000 / 4 000 cr (sell-back per [economy](../../../systems/economy/README.md)) |
| Unlock | L11 (in the shop from the hangar visit before it) |
| Projectile | 300 → 420 px/s, 5×16 px, range 500 px |
| Sound family | `torpedo` ([weapon sound families](../../../audio/sfx/README.md#weapon-sound-families)) |
| Projectile family (VFX) | `torpedo` (projectile sheet of concept round 08) |
<!-- /data -->

### Per level

<!-- data: weapon-levels -->
| Level | Shots | Pattern | Rate /s | Damage | Volley DPS | Single-target DPS | Extra | Draw MW | Upgrade cost |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 1 | single | 0.8 | 12.5 | 10.0 | 10.0 | turn 60°/s | 1 | — |
| 2 | 1 | single | 0.9 | 14 | 12.6 | 12.6 | turn 60°/s | 1.5 | 500 |
| 3 | 1 | single | 1 | 17 | 17.0 | 17.0 | turn 60°/s | 2 | 1 000 |
| 4 | 1 | single | 1 | 24 | 24.0 | 24.0 | turn 60°/s | 2.5 | 2 000 |
| 5 | 1 | single | 1.2 | 26.7 | 32.0 | 32.0 | turn 60°/s | 3 | 4 000 |
| OD | 1 | single | 1.4 | 28 | 39.2 | 39.2 | turn 60°/s | — (ignores cap) | — |

Generated from [data.yaml](data.yaml) by `tools/sync_tables.py`; do not edit by hand; the exact offset and angle of every shot are in that file. Values are per pod. Single-target DPS counts the shots that hit a 36 px target 100 px from the muzzle (seeking/lobbed: all shots). OD = the overdrive pattern (20 s pickup).
<!-- /data -->

### Behaviour

- Per pod, **above water** (a level with `water: true`, user decision E1 = a of M5 part E: Levels
  11–13, the whole play field): each torpedo **drops** from the pod into the water (a small splash
  at the release point) and runs forward on the `sub` layer under the surface (a bubble wake),
  accelerating from 300 to 420 px/s over 0.5 s (`accelerate`), range 500 px. It steers up to 60°/s
  (`turn`; the Targeting computer does not scale it) toward the nearest `sub` unit, naval surface
  (`ground`) unit, set-piece part on those layers or **sunken trigger** (a `sub` trigger still to be
  freed, such as Level 11's pod; 2026-10-09) in a **60° cone** ahead (`cone`), picked as it is fired
  and again when its target dies; without one it runs straight. Its data gains `accelerate: 0.5` and `cone: 60` in step E2a.
- **What it hits** (default of M5 part E): every `sub` unit and every `ground` unit over water
  (rafts, surfaced jellies, the Kraken's surfaced parts, floating containers, a `sub` trigger such as
  Level 11's sunken pod) at **full damage**, no ×2 (it is not `anti-ground`); never `air`, `low-air`
  or `high-air`. It strikes the first such hit box it touches and is spent. The `sub` layer rules:
  [enemies](../../../enemies/README.md#layer-rules) (E2 = a: only `anti-sub` and the Smart Bomb reach
  it from above).
- Over land or in space there is no water: the pod is **idle** (it fires nothing) but still draws its
  power, and its HUD weapon row reads **`NO WATER`** ([HUD](../../../ui/hud/README.md#right-panel-ship)).
- **Under water** (Act 4) the torpedoes travel on the play plane, range +50 % (750 px), and hit every under-water target at full damage ([Europa rules](../../../world/europa/README.md#under-water-rules)).
- **Hangar**: its test fire runs over a water strip with a submerged dummy
  ([hangar](../../../ui/hangar/README.md)).
- **Looks and sound**: the drop splash, the bubble wake on `sub` (drawn through the generic `sub`
  pass), the impact (the water explosion's small rung); the launch is the `torpedo` family's
  [shot-torpedo-r03-a](../../../audio/sfx/concept/shot-torpedo-r03-a.ogg), played drier above water.

## Implementation

- [x] Weapon data loaded from the shared item data (numbers as in the table above, with
      `accelerate` and `cone`) — M5 part E, step E2a
- [x] Projectile pattern per level 1–5 and the overdrive pattern — M5 part E, step E2a
- [x] Layer hit rules for its traits (see *What it hits*): the torpedo delivery on `sub`, hitting
      `sub` and `ground` over water, pooled and allocation-free (`TorpedoTest`) — M5 part E, step E2a
- [x] Behaviour as described above: the drop, the acceleration, the 60° cone and 60°/s steering,
      the 500 px range; idle without water, still drawing power — M5 part E, step E2a
- [x] Power draw per level counted in the loadout; upgrades priced as listed — M5 part E, step E2a
- [x] Muzzle splash, bubble wake, projectile and impact sprites of its VFX family; the HUD's
      `NO WATER` row; the hangar's test fire over water — M5 part E, steps E1c and E3c
      (`WeaponLooks`: `torpedo-pod-shot` at 32 headings drawn in the `sub` pass with its
      `torpedo-pod-bubbles` trail, `torpedo-pod-splash` where it drops in, `explosion-under-small` /
      `explosion-water-small` at the impact; `ShipPanel.NO_WATER`; `TestFireView`'s water range;
      `PartEHudTest`)
- [x] Sound of its family (the launch and the water burst wired) — M5 part E, step E3d

## Decisions

- 2026-10-01: Promoted from the weapons roster with full per-level numbers for Acts 1–2. Numbers live in `balance-data.json`; the table is generated by `tools/balance.py --sync`.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-02: Numbers moved into [data.yaml](data.yaml) (M2 data files); the property and per-level tables are rendered from it by `tools/sync_tables.py`.
- 2026-10-06: M5 part A (user decision D1 = a): the Torpedo Pod and the `sub` layer move to M5 part E (Level 11, the first level with water); rejected building it in part A with a minimal `sub` layer and test dummies (the water rules would be redone in E). Its items are tagged `later: M5 part E`.
- 2026-10-08: M5 part E (user decisions of 2026-10-08): **E1 = a** water is a level flag
  (`water: true`, Levels 11–13), so the pod runs in the whole play field there and nowhere else;
  **E2 = a** only `anti-sub` (and the Smart Bomb) reaches the `sub` layer, so the torpedo keeps its
  point. Stated defaults: it hits `sub` units and every `ground` unit over water at full damage, no
  ×2, never air; an idle pod still draws its power and reads `NO WATER`; the balance plan buys none
  (it stays optional). Our readings, for review in round 33: the target picked in its 60° cone as it
  is fired and again when its target dies; the `turn` not scaled by the Targeting computer (it is
  not homing); `accelerate` and `cone` added to its data in step E2a. `design` goes to `review`;
  `implementation` is `in-progress`.
- 2026-10-08: M5 part E step E2a (simulation): our readings where the design was silent, for review
  in round 33: it seeks within its 500 px range; it is not `anti-ground`, so a hardened unit is no
  target and glances it; it runs at its speed across the screen like every other shot (the sea's
  scroll does not carry it); fired without a target in its cone it runs straight and never looks
  again (it looks again only when the target it had is gone).
- 2026-10-08: Game presentation as built (M5 part E, step E3c; our reading, for review in round
  33): the torpedo runs in the `sub` pass with its bubbles left on the sea (a puff every 4 steps,
  scrolling with it), drops in with a splash under its pod, and bursts **under the water** when it
  strikes a submerged unit or a submerged part of the Kraken (within 24 px), **on the surface**
  otherwise (a raft, a surfaced jelly, a container); over land (`NO WATER`) it is not drawn at all
  (it fires nothing). The hangar's test fire draws the range as water, the dummies as faint cyan
  boxes under it.
- 2026-10-09: A sunken trigger (a `sub` trigger such as Level 11's pod) is in the torpedo's target
  pick, so a torpedo fired while the pod is on the screen seeks it and a pilot a little off its line
  still frees it. It does not lengthen the pass: a torpedo fired before the pod enters has nothing
  to seek, so a lined-up L1 pod lands 4 hits when the pilot fires from about a second before it
  enters and 3 when out of step; since round 33 (user) the pod takes 3 (hard 5), so one L1 pod frees
  it either way (`Level11Test`).
- 2026-10-09: Fix pass after round 33's capture: the torpedo was not to be seen (its slate body,
  drawn only through the `sub` pass, came out the sea's own colour, and its bubbles with it). The
  body stays in the `sub` pass, and over the chop, under the convoy, its sprite is added again at
  0.9 strength as a pale streak (its lit back just under the surface), fading with its range; its
  bubble trail is drawn on the surface over the chop (bubbles rise), no longer through the `sub`
  pass. Its under-water burst and ring are unchanged.
- 2026-10-09: Concept round 33 closed (user): the torpedo's art (`torpedo-pod-shot` at 32 headings,
  its bubbles and drop splash, `tools/art/water_fx.py`) approved as **final**, its lit back over the
  chop included (build choice p); torpedoes seeking sunken triggers (m) and the sunken pod's 3 hits
  (hard 5; build choice n changed from 4) accepted, the numbers and the torpedo run's capture too.
  `design: approved`, `art: final`; every Implementation item is ticked, so the implementation is
  `done`.
