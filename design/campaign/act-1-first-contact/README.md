---
title: Act 1 – First Contact
design: draft
implementation: not-started
art: none
updated: 2026-09-30
---

# Act 1 – First Contact

## Summary

Levels 01–07. The Tether Gate opens and the first Vrell wave strikes Earth's orbital shipyards
and the Lunar colonies. Aegis Wing is the only squadron ready to fly. The act teaches the
basics: layers, ground targets, escorting, and enemies coming from the sides. It ends with the
Brood Carrier at the Earth–Moon L1 point.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [level-01-break-at-dawn](level-01-break-at-dawn/README.md) | Vrell scouts hit the Gagarin shipyards at dawn; the tutorial level | draft | not-started | none |

## Roster

| # | Name | Setting | Layers | Directions | Density | Recommended traits | Introduces | Notes | Design |
|---|---|---|---|---|---|---|---|---|---|
| 02 | Shipyard Burning | [earth-orbit](../../world/earth-orbit/README.md) | air, ground | front, sides | 2 | forward, spread | [Spine Turret](../../enemies/ground/README.md) on station hulls (first `ground` targets), [Stinger](../../enemies/air/README.md) | Save the drydocks: every intact dock pays bonus credits | idea |
| 03 | Spore Drift | [earth-orbit](../../world/earth-orbit/README.md) | air, low-air, high-air | front | 2 | spread | [Spore Bomber](../../enemies/air/README.md), drifting spore mines, debris field hazard; [Leviathan](../../enemies/space/README.md) set piece drifting over on `high-air`, releasing [Whirl Seed](../../enemies/air/README.md) clusters | First level where a spread weapon clearly pays off. First `huge` enemy and first spinners | idea |
| 04 | Tranquility Run | [luna](../../world/luna/README.md) | ground, air | front | 2 | forward, anti-ground | [Brood Pod](../../enemies/air/README.md), `escort` objective (lunar convoy crawlers on the ground layer); [Scuttler](../../enemies/ground/README.md) walkers stalking the convoy | First planetary surface; first special ability available. First walker: it turns to face where it walks | idea |
| 05 | Crater Nest | [luna](../../world/luna/README.md) | ground, air | front, sides | 3 | anti-ground, spread | [Polyp Mortar](../../enemies/ground/README.md), mid-boss [Gorgon Frigate](../../enemies/bosses/README.md) | `destroy-targets`: a Vrell nest seeded inside a crater | idea |
| 06 | Farside | [luna](../../world/luna/README.md) | air, ground | sides, front, rear | 3 | side, spread | [Mantis](../../enemies/air/README.md) holding at the screen sides; [Coilwyrm](../../enemies/air/README.md) serpents swirling between crater rims and looping round to strike from the rear | The dark far side: long shadows, lights from mining domes. First segment-chain enemy; cut segments regrow a head. Its rear loops are dodged, not shot: rear guns arrive at L08 | idea |
| 07 | Brood Carrier | [earth-orbit](../../world/earth-orbit/README.md) | air, high-air | front | 3 | forward, piercing | Boss [Brood Carrier](../../enemies/bosses/README.md); `high-air` layer (carrier hull passes over the player) | Short approach through the carrier's escorts, then the boss | idea |

## Design

### Synopsis

On 14 March 2185 the Tether Gate beyond Neptune activates. Six weeks later a Vrell strike
group appears at the Earth–Moon L1 point and falls on the **Gagarin shipyards**, the UTC's main orbital
shipyard. Aegis Wing scrambles at dawn. After saving what they can of the yards, the wing
follows the Vrell to Luna, where brood pods have landed near Tranquility Base and farside mining
settlements have gone silent. Dr. Varga traces the pods back to a single brood carrier sitting
at L1, and Commander Okafor sends Aegis Wing to kill it.

### Story beats

- L01: first contact. The Choir's first transmission is a wall of noise that resolves into a
  word: "*Yield.*"
- L02–03: Rook and Lancer build their bond over the radio. Okafor is calm under fire.
- L04: the escort of refugee crawlers makes the war personal.
- L06: the farside settlements are found empty. The Vrell take people (a hook for later acts).
- L07: the Brood Carrier dies. The Choir answers: "*We are many. You are late.*" A second,
  larger fleet is detected heading for Earth.

### Settings

[Earth orbit](../../world/earth-orbit/README.md) (L01–03, L07) and
[Luna](../../world/luna/README.md) (L04–06).

### New mechanics

- Basic movement and shooting, pickups, credits (L01).
- `ground` layer targets (L02), `low-air` enemies (L03).
- A `huge` set-piece enemy and tiny spinners (L03), a walker (L04), a segment-chain serpent
  that attacks from the rear (L06).
- `escort` objective (L04), `destroy-targets` objective (L05).
- Enemies entering from and holding at the sides (L06).
- `high-air` layer: the boss hull passes above the player (L07).

### Factions present

Vrell only. The Ascendancy is not seen or mentioned in this act.

### Boss

[Brood Carrier](../../enemies/bosses/README.md) (L07); mid-boss
[Gorgon Frigate](../../enemies/bosses/README.md) (L05).

### Music

Act theme "First Contact" plus the boss theme; see [audio](../../audio/README.md).

## Implementation

- [ ] All 7 levels promoted to draft level documents and implemented.
- [ ] Act 1 title card and opening briefing.
- [ ] Shop unlocks for L02–L07 as listed in the campaign loadout-pressure table.

## Open questions

- Should L01 have a skippable in-level tutorial (control prompts), or should the briefing and
  radio chatter teach everything?

## Decisions

- 2026-09-30: Act split: Earth orbit (L01–03, L07) and Luna (L04–06).
- 2026-09-30: Enemy variety pass: new units added to the level rows so the act passes the [variety checklist](../../enemies/README.md#variety-checklist-per-act).
