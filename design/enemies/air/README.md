---
title: Air enemies
design: draft
implementation: not-started
art: proposed
updated: 2026-09-30
---

# Air enemies

## Summary

Flying enemies on the `air` layer (the player's plane), plus `low-air` flyers below it and
`high-air` units passing above. Air enemies also appear in space levels; vacuum-only enemies are
in [space](../space/README.md). This is the largest category and carries most waves in most
levels.

## Roster

| Name | Faction | Layer | Role/ability | Formations | First level | Design |
|---|---|---|---|---|---|---|
| Skitter | Vrell | air | Swarm fodder. `path`/`swoop` movement, `none` attack: rams on contact. 1 HP. Teaches shooting. | snake, stream, line abreast, swarm | 01 | idea |
| Needler | Vrell | air | Basic gunner. `hover` or `swoop`, fires a slow `aimed` thorn every 2.5 s. 4 HP. | V-wing, line abreast, pincer, circle | 01 | idea |
| Stinger | Vrell | air | `dive`: enters, locks on, dives past the player; fires a 3-way `fan` at the bottom of its dive. | V-wing, column | 02 | idea |
| Spore Bomber | Vrell | low-air | Slow, bulbous; drops drifting spore `mine`s that rise to the player plane after 1 s. Spread fire clears the spores. | line abreast, convoy | 03 | idea |
| Brood Pod | Vrell | air | A slow, pulsing sac. `spawn`: bursts into 6 Skitters when killed **or** after 8 s, so kill it fast with focused fire. | carrier + escorts | 04 | idea |
| Mantis | Vrell | air | Enters from a side edge and holds position (`hover`), sweeping a short `laser-sweep` across the lower screen. Needs `side` or `spread`. | pincer | 06 | idea |
| Wraith | Vrell | high-air → air | Cloaked shimmer passing overhead; decloaks **behind** the player and fires a `burst` up the screen. | rear ambush | 10 | idea |
| Lamprey | Vrell | air | `chase`, then `latch` onto the player and `drain` the shield until shaken off by hard left–right movement. | swarm, stream | 12 | idea |
| Choir Herald | Vrell | air | Support: `aura` gives nearby enemies a shield ring. A priority target, with a glowing crest as weak point. | carrier + escorts, circle | 17 | idea |
| Ghost Drone | Unmarked (Ascendancy) | air | Grey, angular, no Vrell biology. `strafe` with a precise `burst`. The first hint of human involvement. | line abreast, pincer | 18 | idea |
| Talon | Ascendancy | air | Fast interceptor. `swoop` with paired `aimed` shots; small hex shield (2 HP). | V-wing, line abreast | 29 | idea |
| Gilded Gunship | Ascendancy | air | Heavy and slow. Armoured front (50% damage reduction) with a `ring` burst every 3 s; exposed engines at the rear take ×2. | column, carrier + escorts | 30 | idea |
| Hornet | Ascendancy | air | Missile boat. `hover` at the top and launch 4 shootable `homing` missiles. | line abreast | 31 | idea |
| Chimera | Hybrid | air | Talon airframe with Vrell tissue: **regenerates** 20% HP/s after 1 s without taking damage. Rewards burst damage. | V-wing, swarm | 36 | idea |
| Mirror Interceptor | Ascendancy | air | Frontal `reflect` shield bounces non-`beam` shots back. Vulnerable from the sides and rear, or to `beam`/`shield-breaker`. | pincer, rear ambush | 37 | idea |
| Harrow | Hybrid | air | `kamikaze` with a Vrell biomass payload; `death-burst` of 8 bullets whether shot or impacting. Kill it at range. | stream, cross | 38 | idea |
| Choir Seraph | Vrell (elite) | air | Elite. `teleport` next to the player's flank and `mirror` its movement while firing a `spiral`. 40 HP. | circle (pairs) | 47 | idea |

## Design

- Air enemies make up most of every wave table. Every level mixes at least one fodder type
  (Skitter / Talon / Chimera) with one "problem" enemy that needs attention.
- `low-air` and `high-air` members follow the [layer rules](../README.md): they don't collide
  with the player, `high-air` is only hit by `homing` and `beam`, and their bullets always
  travel on the player plane.
- Sizes (proposal, at 960×540): fodder 24–30 px, gunners 36 px, heavies 60–84 px.

## Concept art

Concept [round 03](../../concept-rounds/round-03/README.md). Two Vrell design languages are proposed: **A "Sleek chitin"** (smooth, elongated, glossy violet chitin with thin glowing teal seams, pink eye as weak point) and **B "Armoured brood"** (bulky segmented carapace plates, claws and spikes, glow only between plates and in eye clusters). The key Act 1 enemies are shown in both; the others in A. Each sheet: source render, native sprite with palette, animation hint, in-game view with formation, shadows, bullets and the player for scale. Prompts: [concept/prompts.md](concept/prompts.md); generator `tools/concept/enemies_r03.py`.

| File | What | Status |
|---|---|---|
| [concept/skitter-r03-a.png](concept/skitter-r03-a.png) | Skitter, language A: needle dart with scythe wings; snake formation over a station hull | proposed |
| [concept/skitter-r03-b.png](concept/skitter-r03-b.png) | Skitter, language B: armoured beetle with split glowing elytra | proposed |
| [concept/needler-r03-a.png](concept/needler-r03-a.png) | Needler, language A: slim body with thorn proboscis; V formation firing aimed thorns | proposed |
| [concept/needler-r03-b.png](concept/needler-r03-b.png) | Needler, language B: crab with two claws around a thorn launcher | proposed |
| [concept/stinger-r03-a.png](concept/stinger-r03-a.png) | Stinger (A): wasp-like diver, 3-way fan at the bottom of its dive | proposed |
| [concept/spore-bomber-r03-a.png](concept/spore-bomber-r03-a.png) | Spore Bomber (A): gas-bag blimp on `low-air` dropping spore mines, over Earth from orbit | proposed |
| [concept/brood-pod-r03-a.png](concept/brood-pod-r03-a.png) | Brood Pod (A): pulsing egg sac bursting into six Skitters, over lunar regolith | proposed |
| [concept/mantis-r03-a.png](concept/mantis-r03-a.png) | Mantis (A): side-holding laser sweeper with raptorial arms | proposed |
| [concept/talon-r03-a.png](concept/talon-r03-a.png) | Talon (Ascendancy fighter): forward-swept black-and-gold stealth interceptor with paired aimed shots, asteroid belt | proposed |
| [concept/gilded-gunship-r03-a.png](concept/gilded-gunship-r03-a.png) | Gilded Gunship (Ascendancy gunship): gold front armour, ring burst, exposed rear engines | proposed |

## Implementation

- [ ] Each enemy promoted to its own directory with a stat block before it is implemented.

## Open questions

- Should the Lamprey's `latch` also slow the ship, or only drain the shield?

## Decisions

- 2026-09-30: Roster of 17 air enemies drafted.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-09-30: Concept round 03: first enemy sheets — Act 1 air enemies (Skitter and Needler in both Vrell design languages) plus Talon and Gilded Gunship for faction contrast.
