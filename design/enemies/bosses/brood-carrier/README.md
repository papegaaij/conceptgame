---
title: Brood Carrier
design: approved
implementation: in-progress
art: chosen
depends-on: [../../../systems/difficulty, ../../../systems/economy]
updated: 2026-10-05
---

# Brood Carrier

## Summary

The Act 1 boss: a teal-black living carrier about one screen long. It comes over nose-first on `high-air` and stops above the ship, launching Skitters and Needlers from its bays, then descends and turns broadside with lime bay sacs open between fan volleys, and finally exposes its core.

## Design

### Stat block

Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis. The stat block and the boss script are in this directory's [data.yaml](data.yaml) (schema in the [architecture](../../../tech/architecture/README.md#data-file-schemas)); the table is rendered from it, as the [Gorgon Frigate](../gorgon-frigate/README.md)'s.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `high-air` (phase 1) → `air` (the turn, phases 2–3) |
| Size tier | `huge` |
| Size | 288×626 px, hitbox 220×560 (nose-down; broadside 560×220); drawn 360×782 px on `high-air` (the 1.25 high-air scale) |
| Parts | hull (`armoured`), 8 bay sacs in 4 pairs (`destroyable`, open only in windows), core under a plate iris (`vital`, phase 3), the head mandible turret (`armoured`, fire only: no HP, not in the bar) |
| Orientation | `fixed` (never rotated at run time): two poses, nose-down (phase 1) and broadside (phases 2–3), with 6–8 pre-rendered turn frames between them; the mandible turret a small heading set |
| HP | bay sac 180 each (1 440); core 2 400; total 3840 (easy 2880 / hard 4992, from the global multipliers) |
| Armour / shield | hull and mandibles armoured (sparks, no damage); a sac takes damage only while open; on `high-air` (phase 1) only `homing` reaches it, the missiles seeking the open sacs all round and passing beneath the hull (`beam` too, from L15); invulnerable during the turn |
| Speed | 40 px/s (coming over nose-first); phases 2–3 hold their station |
| Movement | comes over the top edge nose-first on `high-air` at 40 px/s and stops with its centre at y 270 (about 16.5 s); then the turn: descends to `air` while it glides to its broadside station (x 170, y 150) in 2 s, and turns 90° in place in 2 s, head to the right (invulnerable); see *Phases* |
| Attack | see *Phases*: phase 2: the head mandibles fire a 5-way `fan` (spread 50°, 150 px/s, `small` = 4) every 2.4 s; phase 3: the core fires a 3-arm `spiral` (turning 90°/s, 120 px/s, a bullet per arm every 0.375 s) for the whole phase; and with it a 16-bullet `ring` every 4 s (110 px/s) |
| Formations | carrier + escorts (Skitters and Needlers launched from its bays) |
| Weak points | lime bay sacs (×1.5), lime core (×2) |
| Effective traits | `piercing`, `spread`, `homing` |
| Credits | 450 (score 4500 × chain) (bays 25 each, core 250; × L07's `bounty_scale` 0.75 it pays 340, ≈ 32 % of the L07 budget, 1,051) |
| Death | `huge`: chained explosions from tail to head over 3 s (surviving sacs burst and pay in the chain), screen flash, credit shower |
| First level / used in | L07; echo version in the L49 boss rush |
| Difficulty hooks | easy: phase-1 openings spawn 3 Skitters or 1 Needler; hard: phase-3 spiral has 4 arms; every opening that spawns Skitters spawns 1 more |
<!-- /data -->

**Parts** (the data's `part_list`, written tail to head: the chained death bursts them in that
order). Offsets are px from the hull's centre at 1×, nose-down (dx right, dy up, the head at
negative dy), read off the round-04 concept; the broadside pose turns each (dx, dy) into (−dy, dx),
the head to the right. On `high-air` the offsets and boxes grow by 1.25. These are the sizes the
production art (`tools/art/brood_carrier.py`) renders to.

| Part | Kind | Offset nose-down | Offset broadside | Hit box | HP | Credits |
|---|---|---|---|---|---|---|
| bay 4 left / right (the tail pair) | `destroyable` sac | (∓89, 108) | (−108, ∓89) | 40×40 | 180 | 25 |
| bay 3 left / right | `destroyable` sac | (∓95, 33) | (−33, ∓95) | 40×40 | 180 | 25 |
| core (under the plate iris) | `vital` | (0, −4) | (4, 0) | 60×60 | 2,400 | 250 |
| bay 2 left / right | `destroyable` sac | (∓93, −42) | (42, ∓93) | 40×40 | 180 | 25 |
| bay 1 left / right (the head pair) | `destroyable` sac | (∓86, −117) | (117, ∓86) | 40×40 | 180 | 25 |
| mandibles (the head turret) | `armoured`, fire only | (0, −285) | (285, 0) | 32×32 | — | — |
| hull | `armoured` body | — | — | 220×560 nose-down, 560×220 broadside | — | — |

### Phases

The boss bar appears when the carrier arrives (L07, t=100); phase 1's 25 s and the par run from
then.

| Phase | Ends at | Behaviour |
|---|---|---|
| 1 — Overhead | timed, 25 s | The hull comes over the top edge nose-first on `high-air` at 40 px/s (drawn at the high-air scale with its shadow and, like the [Leviathan](../../space/leviathan/README.md), at 75 % opacity, easing to opaque as it descends, so the ship and its shots show through the hull; the units it launches are drawn over it) and stops with its centre over the middle of the field (x 240, y 270), covering the ship, at 16.5 s; it holds there to the end of the phase. The bay pairs open in hull order, head to tail, the first once it is on screen (about 6 s after the bar) and then every 2.5 s: 8 openings by the 25 s mark, each opened **pair** together `spawn`ing 4 Skitters or 2 Needlers, alternating (16 Skitters, 8 Needlers). An open sac takes damage, only from `homing`; a destroyed sac stays destroyed, and its pair's later openings spawn half (rounded up), none once both are gone. The fight is about clearing the spawns. |
| Turn | 4 s | Invulnerable. It descends to `air` (scale 1.25 → 1.0, 2 s) while it eases to its broadside station, then turns 90° in place through 6–8 pre-rendered frames (2 s), head to the right. Station: the hull's centre 70 px left of the field's centre (x 170) in the upper half (y 150), so the mandibles sit about 25 px inside the right edge and the tail tendrils overhang the left edge by about 140 px; every sac and the iris stay in the field. |
| 2 — Broadside | sac HP 1 440 → 0, or 70 s after the turn (a **timeout**) | Head turrets fire 5-way `fan`s (spread 50°, 150 px/s, `small` = 4) every 2.4 s. Between volleys (0.2 s after each) one living pair opens for 2 s (a **window**), cycling through the living pairs in hull order; each window releases 1 Skitter per open living sac. A sac takes damage only while open; destroyed bays stop spawning. |
| 3 — Core | core 2 400 → 0 | The plate iris opens (1 s, no fire); the core then fires a 3-arm `spiral` (rotation 90°/s, 120 px/s, 8 bullets/s, a bullet per arm every 0.375 s) for as long as the phase lasts, and at the same time a 16-bullet `ring` every 4 s. If phase 2 timed out, every surviving pair opens together every 6 s (the first 3 s after the iris) for 2 s and releases 2 Skitters per pair; open sacs take damage. |

The stop, the station and the turn's 4 s are fixed in the data (the level-data step of part G).

### Arena

Open space at the Earth–Moon L1 point, beyond the Vrell picket ([L07](../../../campaign/act-1-first-contact/level-07-brood-carrier/README.md)): scroll slows to 20 px/s during the fight. Target duration 90–150 s at medium. The level test's autopilot with the balance plan's fit takes phase 1 25 s, the turn 4 s, phase 2 about 40 s and phase 3 about 21 s (the core's ×2 weak point counts): about 90 s at medium, 72 s on easy and 102 s on hard (the par question is open in [L07](../../../campaign/act-1-first-contact/level-07-brood-carrier/README.md#open-questions)).

- **Par**: 150 s at every difficulty, from the bar appearing to the kill (the target plus about
  10 %, as the frigate's 60 s for 55 s); under par pays the
  [Boss rush](../../../systems/scoring/README.md#level-end-bonuses) bonus. The autopilot's kill
  times (72–108 s) are well under it; see the open question in L07.
- **Boss bar**: the long bar of an act boss, the sacs' and the core's remaining HP, at the top of
  the play field ([HUD](../../../ui/hud/README.md#in-the-play-field)).
- **Death**: the parts burst one after another from tail to head over 3 s, sacs that survived a
  timeout among them (they pay their bounty, so the carrier always pays 450 before the level's
  scale), with the break-up and the ichor clouds; a screen flash (reduced by the flash-reduction
  option); the credit shower after the chain. The **carcass** is dead: its eyes, tendrils, spine
  seam, sacs and core are dark from the break-up on, and only the torn flesh along the cuts glows
  lime at the swap, cooling to dead tissue over the next 12 s; the blasts that trail on the
  chunks are fire, no ichor. Its eight chunks drift apart
  slowly through the whole aftermath (L07: 35 s after the kill), the carcass as a whole drifting
  down and to the right from the broadside station (the tail comes into view from the left edge),
  sinking (drawn smaller), darkening, and still on screen when the level ends.

### Behaviour

- Phase 2 ends when all sacs are gone or after 70 s, so a weak build is never stuck forever.
- The size question (one screen vs two) is settled: about one screen, as chosen in round 03.
- A sac destroyed in any phase pays its bounty, stops spawning and leaves a burst stump leaking
  ichor; the first destroyed sac drops L07's overdrive.
- A hit sac or core takes a short white tint (45 %, held 4 steps, fading over 12), not the 2-step
  solid white flash of other parts: under steady fire the next hit comes before the tint is gone,
  so it holds instead of strobing.
- A launched unit leaves its sac downward toward the `air` layer, so while the carrier is off the
  play plane its launched units are drawn over the hull (and over the ship), not under it.

## Concept art

Chosen concept: [brood-carrier-r04-a.png](../concept/brood-carrier-r04-a.png) (listed in the [bosses](../README.md#concept-art) Concept art table). Production review files (round 25, step A1 of part G) and their brief: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/brood-carrier-final-r25-a.png](concept/brood-carrier-final-r25-a.png) | Final sprites (`tools/art/brood_carrier.py`): the carrier as the game composes it (nose-down closed and open, broadside closed and in phase 3), the data's hit boxes and part offsets over both poses, the 9 hull frames of the turn (nose-down 288×626, 7 turn frames 11.25° apart with the head swinging right, broadside 626×288), the bay sac nose-down and broadside (closed, 2 swelling, open, burst; 40×40), the plate iris (5 stages, 64×64), the core glow, the mandible turret at 17 headings (±90° of straight down); one 48-colour palette | proposed |
| [concept/brood-carrier-final-r25-a.gif](concept/brood-carrier-final-r25-a.gif) | The pairs opening head to tail nose-down with the turret tracking, the turn, the broadside windows with two sacs bursting, the iris opening over the pulsing core | proposed |
| [concept/brood-carrier-death-final-r25-a.png](concept/brood-carrier-death-final-r25-a.png) | The break-up (`tools/art/brood_carrier_death.py`): the eight chunks at their offsets beside the wreck as the game draws it, the break-up at four steps up to the level's end (the play field outlined), each chunk's 3 tumble frames (dead: no lights; the cuts cooling), the blast layout over the game's tail-to-head chain, the ichor cloud | proposed |
| [concept/brood-carrier-death-final-r25-a.gif](concept/brood-carrier-death-final-r25-a.gif) | The whole death: the chain from tail to head with the blast wave, the flash and the swap at 3 s, the chunks drifting apart under trailing blasts, from 5 s after the swap a 10× time-lapse to the level's end (35 s after the kill), sinking and darkening | proposed |
| [concept/brood-carrier-capture-final-r25-a.png](concept/brood-carrier-capture-final-r25-a.png) | Game capture: `desktop/build/install/terran-vanguard/bin/terran-vanguard --bench 150 --settings <file> --level 7 --invulnerable --debug-speed 2 --loadout front=pulse-cannon:5,left=micro-missile-pod:3,right=micro-missile-pod:3` under `xvfb-run -s "-screen 0 960x540x24"` (settings: a 960×540 window at 0,0, `audio.master=0`, `controls.auto-fire=true`), recorded with `ffmpeg -f x11grab -draw_mouse 0` (4 fps, the turn from a second run at 15 fps), eight whole-window frames in a 2 × 4 grid, 8 px apart on `#0b0e14`. Left to right, top to bottom: on `high-air` over the ship (1.25×), the pairs open nose-down; two frames of the turn; broadside with a pair open; phase 3 (iris open, the spiral and the ring; after the phase-2 timeout); the chain's blasts; the chunks drifting apart | proposed |

## Implementation

- [x] Overhead phase on `high-air` with the openings and spawns; hits on open sacs by `homing` only
- [x] The turn: descent to `air` and the broadside pose, invulnerable; station off-centre
- [x] Bay sac windows between fan volleys, cycling the living pairs; 70 s timeout
- [ ] Core phase with the spiral and rings together; timeout spawns; 3-s chained death tail to
      head with the surviving sacs paying, screen flash
- [x] Fire-only armoured turrets, out of the bar
- [x] Stat block values and boss script loaded from data; global difficulty multipliers and the
      easy/hard hooks applied; par 100 s
- [ ] Death effect, bounty and score per this spec
- [x] Production sprites: the three poses, the sac and iris stages, the turrets, the break-up
      (`tools/art/brood_carrier.py`, `tools/art/brood_carrier_death.py`; review in round 25)

## Decisions

- 2026-10-01: Promoted from the bosses roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: Arena set to the Earth–Moon L1 point (was Earth orbit above Gagarin), matching story, campaign and Luna docs; phase-1 spawns are per opened bay pair, as L07 assumes.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-05: M4 part G, D1 = b (user): instead of a 25 s pass across the screen (too short at
  40 px/s: the hull is 782 px long on `high-air`, a crossing about 33 s) and a 32-angle turn (no
  room in one boss atlas page, and lit sprites are never rotated), the carrier comes over
  nose-first at 40 px/s, stops above the ship and spawns for the rest of phase 1, then descends
  and turns 90° in place through 6–8 pre-rendered frames, invulnerable (a pose in the simulation,
  no rotated hit boxes). Rejected: a pass that leaves and returns broadside, and a run-time
  rotation.
- 2026-10-05: M4 part G defaults (main agent, not objected to): broadside held about 70 px
  off-centre with the head inside the field; in phase 1 only open sacs take damage, only from
  `homing`; phase-2 windows release 1 Skitter per open living sac and cycle through the living
  pairs; a timeout's spawns are 2 Skitters per surviving pair every 6 s; surviving sacs burst and
  pay at the kill; par 150 s (tightened to 100 s on 2026-10-05); the bounty stays 450.
- 2026-10-05: Consequences written in (main-agent choice): with the 16.5 s entrance the openings
  start once the first pair is on screen (about 6 s) and come every 2.5 s instead of every 3 s, so
  the 8 openings still fit in the 25 s; a pair with one sac gone spawns half (rounded up); the
  spiral runs for the whole phase beside the ring (8 bullets/s over 3 arms: a bullet per arm every
  0.375 s); the iris takes 1 s to open before the core fires; the head turrets are fire-only and
  outside the bar and the total HP; the screen flash is the act boss's (mid-bosses have none).
- 2026-10-05: Level-data step of part G: the stat block and boss script moved into
  [data.yaml](data.yaml) and the table rendered from it. Sizes and offsets read off the round-04
  concept for the art step (*Parts*); one head turret (`mandibles`) instead of two, since a fan
  attack fires from every part that carries it (two would double the volley; `rotate` is for aimed
  attacks only). The windows of phase 2 open 0.2 s after each volley, those of phase 3 (after a
  timeout) 3 s after the iris; the timeout's 70 s run from the end of the turn (the engine's phase
  clock); the ring of phase 3 flies at 110 px/s.
- 2026-10-05: Production art, step A1 of part G (straight to production, part F's D8: the concept
  was chosen in round 04): `tools/art/brood_carrier.py` and `tools/art/brood_carrier_death.py`
  render the round-04 model re-laid to the data, every part sprite at its `part_list` and
  broadside offset (the data's offsets kept unchanged: the overlay of the data's hit boxes on the
  renders matches in both poses). Nine hull frames (nose-down, 7 turn frames 11.25° apart, the head
  swinging right, broadside), so the turn is never rotated at run time; one sac set per pose
  (the renderer draws one for all eight sacs), one iris set for both poses (a radial part), the
  turret at 17 headings (±90°; the fans aim at the ship). The death is a break-up from the start
  (rounds 16 and 21): eight chunks swapped in under the flash at the chain's end (3 s) and drifting
  apart for 12 s, under a wave of blasts that runs tail to head with the chain. Level 07's unit
  atlas: one 2048² page, 65 % full (the carrier alone, about 10.6 MiB of 16). Proposed for round 25.
- 2026-10-05: Round 25's Level 07 capture, user decision: the carrier on `high-air` is translucent
  like the Leviathan (75 %, easing to opaque as it descends; the shared high-air opacity of the
  set pieces), since the opaque 782 px hull hid the ship and its shots for about 10 s of the
  overhead phase. Fixes from the same capture (render and art, main-agent brief): the units
  launched from the sacs are drawn over the hull while it is off the plane (they showed only at
  its edges); a hit sac or core takes a held white tint instead of a 2-step solid white flash
  (a sac under steady fire strobed as a white 40 px disc); the carcass has no lights of its own
  (the head's cyan eyes and tendrils glowed on the chunks) and its cuts cool over the tumble
  frames, and the blasts trailing on the chunks are fire only (an ichor burst on a chunk at kill
  + 1.5 s read as a lime glow dot); the chunks drift for the whole 35 s aftermath (they were gone after
  12 s), the carcass drifting down and to the right as a whole.
