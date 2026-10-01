---
title: Act 2 – Homefront
design: draft
implementation: not-started
art: none
updated: 2026-10-01
---

# Act 2 – Homefront

## Summary

Levels 08–14. The second Vrell fleet breaks through and lands on Earth. Aegis Wing fights over
megacities, across the Atlantic and at the Arctic relay stations. The act introduces attacks
from behind, the naval layer, weather and defend-the-station missions. It ends against the
Siege Spire, a Vrell citadel that has rooted itself in the heart of the UTC capital.

## Roster

| # | Name | Setting | Layers | Directions | Density | Recommended traits | Introduces | Notes | Design |
|---|---|---|---|---|---|---|---|---|---|
| 08 | Neon Skyline | [earth](../../world/earth/README.md) – megacity | ground, low-air, air | front | 3 | forward, anti-ground | [Creeper](../../enemies/ground/README.md) walkers crawling over rooftops; city parallax with traffic lanes | Night-time megacity, neon under smoke; Rook joins as AI wingman | idea |
| 09 | Arcology Fall | [earth](../../world/earth/README.md) – megacity | ground, air | front | 3 | anti-ground, area | [Hive Node](../../enemies/ground/README.md) (hardened spawner); [Ravager](../../enemies/ground/README.md) packs galloping through the ruined streets | `destroy-targets` (primary): kill the nodes before the hive spreads over the arcology; nodes left alive keep spawning, and any node alive at the end of the scroll fails the mission | idea |
| 10 | Evacuation Corridor | [earth](../../world/earth/README.md) – megacity outskirts | air, high-air | rear, front | 3 | rear, forward | [Wraith](../../enemies/air/README.md) rear ambushes; [Mote Swarm](../../enemies/air/README.md) flocks that sweep past, loop round and dive from behind | **First rear-heavy level** (~40% of waves from the bottom edge). Primary `escort`: evacuation shuttles. One shuttle loss is scripted for the story; every other loss depends on the player, and losing all of them fails the mission | idea |
| 11 | Atlantic Convoy | [earth](../../world/earth/README.md) – ocean | ground (naval surface), sub, air | front | 3 | spread, anti-ground | Naval layer: [Driftjelly](../../enemies/naval/README.md), [Reef Spitter](../../enemies/naval/README.md); mid-boss [Harbour Kraken](../../enemies/bosses/README.md) | First glimpse of the `sub` layer (shadows under the waves); anti-sub is optional here | idea |
| 12 | Storm Front | [earth](../../world/earth/README.md) – ocean storm | air, low-air | all | 4 | homing, spread | [Lamprey](../../enemies/air/README.md); weather: lightning flashes, rain, wind drift | Low visibility; `homing` shines. [Whirl Seed](../../enemies/air/README.md) clusters return, carried by the storm winds | idea |
| 13 | Polar Relay | [earth](../../world/earth/README.md) – arctic | ground, ground (naval surface), air | all | 4 | spread, rear, side | [Skimmer](../../enemies/naval/README.md) boats weaving through ice floes; primary `defend` objective (the relay destroyed = mission failed) | Scroll halts over the relay station; waves from every edge for 3 minutes. [Scuttler](../../enemies/ground/README.md) walkers return, striding across the ice floes | idea |
| 14 | Siege Spire | [earth](../../world/earth/README.md) – UTC capital | ground, air, high-air | front | 4 | anti-ground, piercing | Boss [Siege Spire](../../enemies/bosses/README.md) (ground-rooted, with airborne parts) | The capital under a Vrell canopy; approach through the old city first | idea |

## Design

### Synopsis

The Brood Carrier was a vanguard. A far larger fleet screens its approach with debris and makes
landfall across three continents. Aegis Wing is recalled to Earth. They fight through the
megacity of **Nova Lagos**, cover the civilian evacuation, and escort a supply convoy across an
Atlantic full of Vrell reef-growths. Then they hold the Arctic relay that coordinates the
planetary defence grid. Finally the Coalition throws everything at the Siege Spire, which grew
overnight in the middle of **Geneva Concord**, the UTC capital.

### Story beats

- L08: Rook, a five-year Aegis Wing veteran who has flown in the wider formation so far, is
  assigned as Lancer's wingman (the escort slot unlocks; see [wingmen](../../player/wingmen/README.md)).
- L10: civilians on the radio. The first loss the player can't prevent (a shuttle is scripted
  to be lost).
- L12: Dr. Varga notices the Vrell are herding the storm. They understand Earth's weather
  systems too well.
- L13: holding the relay keeps the grid online, which is the setup for the Mars counter-offensive.
- L14: with the Spire destroyed, the Vrell on Earth lose coordination. Okafor: "*We take the
  fight to them now.*" Mars calls for help.

### Settings

[Earth](../../world/earth/README.md): megacity (L08–10, L14), open ocean (L11–12), arctic (L13).

### New mechanics

- AI wingman (L08), hardened ground targets that need `anti-ground` (L09).
- Rear attacks as a level focus (L10), naval surface targets and the `sub` layer as scenery
  (L11), weather hazards (L12), `defend` objective with halted scroll (L13).

### Factions present

Vrell only.

### Boss

[Siege Spire](../../enemies/bosses/README.md) (L14); mid-boss
[Harbour Kraken](../../enemies/bosses/README.md) (L11).

### Music

Act theme "Homefront", storm variant for L12, boss theme; see [audio](../../audio/README.md).

## Implementation

- [ ] All 7 levels promoted to draft level documents and implemented.
- [ ] Weather system (rain, lightning, wind drift) for L12.
- [ ] Halted-scroll `defend` mode for L13.

## Open questions

- None open.

## Decisions

- 2026-09-30: Earth megacity names Nova Lagos and Geneva Concord, now recorded as sub-locations in [earth](../../world/earth/README.md).
- 2026-09-30: Enemy variety pass: new units added to the level rows so the act passes the [variety checklist](../../enemies/README.md#variety-checklist-per-act).
- 2026-10-01: L09 introduces the Ravager, an animal-like Vrell pack hunter (round 05 follow-up).
- 2026-10-01: Objective failure rules applied to L09 (destroy-targets), L10 (escort) and L13 (defend). L10: one scripted shuttle loss for the story; the rest depends on the player.
