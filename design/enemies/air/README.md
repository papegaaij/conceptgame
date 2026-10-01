---
title: Air enemies
design: draft
implementation: not-started
art: chosen
updated: 2026-09-30
---

# Air enemies

## Summary

Flying enemies on the `air` layer (the player's plane), plus `low-air` flyers below it and
`high-air` units passing above. Air enemies also appear in space levels; vacuum-only enemies are
in [space](../space/README.md). This is the largest category and carries most waves in most
levels.

## Roster

| Name | Faction | Layer | Tier | Role/ability | Formations | First level | Design |
|---|---|---|---|---|---|---|---|
| Skitter | Vrell | air | tiny | Swarm fodder. `path`/`swoop` movement, `none` attack: rams on contact. 1 HP. Teaches shooting. | snake, stream, line abreast, swarm | 01 | idea |
| Needler | Vrell | air | small | Basic gunner. `hover` or `swoop`, fires a slow `aimed` thorn every 2.5 s. 4 HP. | V-wing, line abreast, pincer, circle | 01 | idea |
| Stinger | Vrell | air | small | `dive`: enters, locks on, dives past the player; fires a 3-way `fan` at the bottom of its dive. | V-wing, column | 02 | idea |
| Spore Bomber | Vrell | low-air | medium | Slow, bulbous; drops drifting spore `mine`s that rise to the player plane after 1 s. Spread fire clears the spores. | line abreast, convoy | 03 | idea |
| Brood Pod | Vrell | air | medium | A slow, pulsing sac. `spawn`: bursts into 6 Skitters when killed **or** after 8 s, so kill it fast with focused fire. | carrier + escorts | 04 | idea |
| Mantis | Vrell | air | medium | Enters from a side edge and holds position (`hover`), sweeping a short `laser-sweep` across the lower screen. Needs `side` or `spread`. | pincer | 06 | idea |
| Wraith | Vrell | high-air → air | medium | Cloaked shimmer passing overhead; decloaks **behind** the player and fires a `burst` up the screen. | rear ambush | 10 | idea |
| Lamprey | Vrell | air | small | `chase`, then `latch` onto the player and `drain` the shield until shaken off by hard left–right movement. | swarm, stream | 12 | idea |
| Choir Herald | Vrell | air | medium | Support: `aura` gives nearby enemies a shield ring. A priority target, with a glowing crest as weak point. | carrier + escorts, circle | 17 | idea |
| Ghost Drone | Unmarked (Ascendancy) | air | small | Grey, angular, no Vrell biology. `strafe` with a precise `burst`. The first hint of human involvement. | line abreast, pincer | 18 | idea |
| Talon | Ascendancy | air | small | Fast interceptor. `swoop` with paired `aimed` shots; small hex shield (2 HP). | V-wing, line abreast | 29 | idea |
| Gilded Gunship | Ascendancy | air | medium | Heavy and slow. Armoured front (50% damage reduction) with a `ring` burst every 3 s; exposed engines at the rear take ×2. | column, carrier + escorts | 30 | idea |
| Hornet | Ascendancy | air | medium | Missile boat. `hover` at the top and launch 4 shootable `homing` missiles. | line abreast | 31 | idea |
| Chimera | Hybrid | air | small | Talon airframe with Vrell tissue: **regenerates** 20% HP/s after 1 s without taking damage. Rewards burst damage. | V-wing, swarm | 36 | idea |
| Mirror Interceptor | Ascendancy | air | medium | Frontal `reflect` shield bounces non-`beam` shots back. Vulnerable from the sides and rear, or to `beam`/`shield-breaker`. | pincer, rear ambush | 37 | idea |
| Harrow | Hybrid | air | small | `kamikaze` with a Vrell biomass payload; `death-burst` of 8 bullets whether shot or impacting. Kill it at range. | stream, cross | 38 | idea |
| Choir Seraph | Vrell (elite) | air | medium | Elite. `teleport` next to the player's flank and `mirror` its movement while firing a `spiral`. 40 HP. | circle (pairs) | 47 | idea |
| Whirl Seed | Vrell | air | tiny | Radial seed pod (6-fold) that `spin`s, `ricochet`s off the play field edges and `spiral-out`s from its release point; released in clusters of 5–8 by Leviathans and spawning reefs. Contact only; pops in one hit into a 3-bullet puff on hard. Orientation `radial`. | whirl cluster (burst of seeds spiralling outward), stream | 03 | idea |
| Coilwyrm | Vrell | air | large | Serpent of 8–12 segments (`chain`) whose head follows `swirl`, `loop` and `figure-8` paths across the screen, often looping round to strike from the **rear**. Head is `vital` and fires a 3-way `fan`; each body segment is `destroyable` (1–2 HP) and **splits** the chain, the rear half growing a new head. Orientation 16 angles per segment. | snake (solo), pairs crossing | 06 | idea |
| Mote Swarm | Vrell | air | tiny | 12–30 tiny motes (~16 px) moving as one `flock` with boids-like swirls; the cloud sweeps past, **loops round** (`loop`, `rear-entry`) and dives at the player from behind. 1 HP each; spread and area weapons shred it. Orientation 16 angles. | swarm, rear ambush | 10 | idea |
| Buzzsaw Drone | Ascendancy | air / space | small | Radially symmetric spinning blade drone (4 blades, `radial`). `chase`s the player, then `ricochet`s off edges and asteroids at speed; contact damage only, but lethal in groups. Blades spin faster just before a dash (telegraph). | stream, circle | 31 | idea |
| Rail Serpent | Ascendancy | air / space | large | Armoured drone train of 8–14 cars (`chain`) that snakes across the screen (`swirl`, `cross`) and **enters from the sides or the rear**. Every third car carries a turret firing `aimed` shots; the engine car is `vital`. Destroying a car splits the train: the rear half stops and explodes. Orientation 32 angles per car. | snake, pincer (two trains) | 30 | idea |

## Design

- Air enemies make up most of every wave table. Every level mixes at least one fodder type
  (Skitter / Talon / Chimera) with one "problem" enemy that needs attention.
- `low-air` and `high-air` members follow the [layer rules](../README.md): they don't collide
  with the player, `high-air` is only hit by `homing` and `beam`, and their bullets always
  travel on the player plane.
- Sizes follow the [size tiers](../README.md#size-tiers): fodder `tiny`/`small` (24–30 px),
  gunners `small` (36 px), heavies `medium` (60–84 px), chains `large`.
- Not all air enemies fly nose-down: chains (Coilwyrm, Rail Serpent) and the Mote Swarm turn
  freely at 16/32 angles, and spinners (Whirl Seed, Buzzsaw Drone) are `radial`. See
  [orientation](../README.md#orientation-and-rotation).

## Concept art

Concept [round 03](../../concept-rounds/round-03/README.md). Two Vrell design languages are proposed: **A "Sleek chitin"** (smooth, elongated, glossy violet chitin with thin glowing teal seams, pink eye as weak point) and **B "Armoured brood"** (bulky segmented carapace plates, claws and spikes, glow only between plates and in eye clusters). The key Act 1 enemies are shown in both; the others in A. Each sheet: source render, native sprite with palette, animation hint, in-game view with formation, shadows, bullets and the player for scale. Prompts: [concept/prompts.md](concept/prompts.md); generator `tools/concept/enemies_r03.py`.

| File | What | Status |
|---|---|---|
| [concept/skitter-r03-a.png](concept/skitter-r03-a.png) | Skitter, language A: needle dart with scythe wings; snake formation over a station hull | superseded by the r04 colour pass |
| [concept/rejected/skitter-r03-b.png](concept/rejected/skitter-r03-b.png) | Skitter, language B: armoured beetle with split glowing elytra | rejected — other variant preferred |
| [concept/rejected/needler-r03-a.png](concept/rejected/needler-r03-a.png) | Needler, language A: slim body with thorn proboscis; V formation firing aimed thorns | rejected — other variant preferred |
| [concept/needler-r03-b.png](concept/needler-r03-b.png) | Needler, language B: crab with two claws around a thorn launcher | superseded by the r04 colour pass |
| [concept/stinger-r03-a.png](concept/stinger-r03-a.png) | Stinger (A): wasp-like diver, 3-way fan at the bottom of its dive | superseded by the r04 colour pass |
| [concept/spore-bomber-r03-a.png](concept/spore-bomber-r03-a.png) | Spore Bomber (A): gas-bag blimp on `low-air` dropping spore mines, over Earth from orbit | superseded by the r04 colour pass |
| [concept/brood-pod-r03-a.png](concept/brood-pod-r03-a.png) | Brood Pod (A): pulsing egg sac bursting into six Skitters, over lunar regolith | superseded by the r04 colour pass |
| [concept/mantis-r03-a.png](concept/mantis-r03-a.png) | Mantis (A): side-holding laser sweeper with raptorial arms | superseded by the r04 colour pass |
| [concept/talon-r03-a.png](concept/talon-r03-a.png) | Talon (Ascendancy fighter): forward-swept black-and-gold stealth interceptor with paired aimed shots, asteroid belt | superseded by the r04 colour pass |
| [concept/gilded-gunship-r03-a.png](concept/gilded-gunship-r03-a.png) | Gilded Gunship (Ascendancy gunship): gold front armour, ring burst, exposed rear engines | superseded by the r04 colour pass |

Concept [round 04](../../concept-rounds/round-04/README.md) — colour pass on the chosen enemies with the [role colours](../../README.md#role-colours) (chitin base = role family, glow = kind of threat; Ascendancy black & gold with a per-unit accent and a thin gold/red rim light). Same models and sheet layout; generator `tools/concept/enemies_r04.py`.

| File | What | Status |
|---|---|---|
| [concept/skitter-r04-a.png](concept/skitter-r04-a.png) | Skitter — plum chitin, teal glow (fodder, contact) | chosen |
| [concept/needler-r04-a.png](concept/needler-r04-a.png) | Needler (language B crab) — bone/ivory carapace, violet glow (gunner, aimed shots); yellow needles | chosen |
| [concept/stinger-r04-a.png](concept/stinger-r04-a.png) | Stinger — rust chitin, crimson glow (diver) | chosen |
| [concept/spore-bomber-r04-a.png](concept/spore-bomber-r04-a.png) | Spore Bomber — olive chitin, lime glow and spore bulbs (area denial) | chosen |
| [concept/brood-pod-r04-a.png](concept/brood-pod-r04-a.png) | Brood Pod — teal-black sac, teal veins (spawner); bursts into plum Skitters | chosen |
| [concept/mantis-r04-a.png](concept/mantis-r04-a.png) | Mantis — bone/ivory chitin, crimson glow (laser sweeper) | chosen |
| [concept/talon-r04-a.png](concept/talon-r04-a.png) | Talon — black & gold with red accent and gold/red rim light | chosen |
| [concept/gilded-gunship-r04-a.png](concept/gilded-gunship-r04-a.png) | Gilded Gunship — black & gold with white accent and rim light | chosen |

Concept [round 05](../../concept-rounds/round-05/README.md) — new archetypes, each a PNG sheet plus a GIF in the 480×540 play field; generator `tools/concept/enemies_r05.py`.

| File | What | Status |
|---|---|---|
| [concept/coilwyrm-r05-a.png](concept/coilwyrm-r05-a.png) | Coilwyrm — Vrell serpent (rust/teal): 58 px head + 12 overlapping segments + tail, ~330 px, each part at 16 headings following the head's looping swirl (sheet) | chosen |
| [concept/coilwyrm-r05-a.gif](concept/coilwyrm-r05-a.gif) | Coilwyrm — Vrell serpent (rust/teal): 58 px head + 12 overlapping segments + tail, ~330 px, each part at 16 headings following the head's looping swirl (motion) | chosen |
| [concept/whirl-seed-r05-a.png](concept/whirl-seed-r05-a.png) | Whirl Seed — tiny 26 px radial seed pod (plum/teal), spinning clusters of five that spiral and bounce (sheet) | chosen |
| [concept/whirl-seed-r05-a.gif](concept/whirl-seed-r05-a.gif) | Whirl Seed — tiny 26 px radial seed pod (plum/teal), spinning clusters of five that spiral and bounce (motion) | chosen |
| [concept/mote-swarm-r05-a.png](concept/mote-swarm-r05-a.png) | Mote Swarm — 30 tiny ember motes (rust/crimson) flocking, exiting and returning from behind the player after a REAR! edge warning (sheet) | chosen |
| [concept/mote-swarm-r05-a.gif](concept/mote-swarm-r05-a.gif) | Mote Swarm — 30 tiny ember motes (rust/crimson) flocking, exiting and returning from behind the player after a REAR! edge warning (motion) | chosen |
| [concept/buzzsaw-drone-r05-a.png](concept/buzzsaw-drone-r05-a.png) | Buzzsaw Drone — Ascendancy six-blade spinning drone (gunmetal/gold, rim light), ricochets off the edges (sheet) | chosen |
| [concept/buzzsaw-drone-r05-a.gif](concept/buzzsaw-drone-r05-a.gif) | Buzzsaw Drone — Ascendancy six-blade spinning drone (gunmetal/gold, rim light), ricochets off the edges (motion) | chosen |
| [concept/rail-serpent-r05-a.png](concept/rail-serpent-r05-a.png) | Rail Serpent — Ascendancy drone train (red accent): head car + 8 cars at 16 headings on a winding route (sheet) | chosen |
| [concept/rail-serpent-r05-a.gif](concept/rail-serpent-r05-a.gif) | Rail Serpent — Ascendancy drone train (red accent): head car + 8 cars at 16 headings on a winding route (motion) | chosen |

## Implementation

- [ ] Each enemy promoted to its own directory with a stat block before it is implemented.

## Open questions

- Should the Lamprey's `latch` also slow the ship, or only drain the shield?

## Decisions

- 2026-09-30: Roster of 17 air enemies drafted.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-09-30: Concept round 03: first enemy sheets — Act 1 air enemies (Skitter and Needler in both Vrell design languages) plus Talon and Gilded Gunship for faction contrast.
- 2026-09-30: Concept round 04: colour pass on air enemies with the role colours (r04 proposals).
- 2026-09-30: Enemy variety pass: size tier column added to every row; new units Whirl Seed, Coilwyrm, Mote Swarm, Buzzsaw Drone, Rail Serpent.
