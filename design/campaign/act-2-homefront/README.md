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

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [level-08-neon-skyline](level-08-neon-skyline/README.md) | Night megacity, Rook joins as wingman, Creeper intro · front · density 3 | draft | not-started | chosen |
| [level-09-arcology-fall](level-09-arcology-fall/README.md) | `destroy-targets`: six hardened hive nodes, Ravager packs, the arcology collapse · front · density 3 | draft | not-started | chosen |
| [level-10-evacuation-corridor](level-10-evacuation-corridor/README.md) | `escort` of five shuttles, first rear-heavy level (Wraith, Mote Swarm), one scripted loss · rear 42% · density 3 | draft | not-started | chosen |
| [level-11-atlantic-convoy](level-11-atlantic-convoy/README.md) | Naval layer (Driftjelly, Reef Spitter), `sub` shadows, mid-boss Harbour Kraken · front · density 3 | draft | not-started | chosen |
| [level-12-storm-front](level-12-storm-front/README.md) | Weather (rain, lightning, gusts), Lamprey intro, Varga sees the Vrell herd the storm · all · density 4 | draft | not-started | chosen |
| [level-13-polar-relay](level-13-polar-relay/README.md) | `defend` the Arctic relay for 180 s, Skimmer intro, whiteout · all edges · density 4 | draft | not-started | chosen |
| [level-14-siege-spire](level-14-siege-spire/README.md) | Geneva Concord approach through the root field, act boss Siege Spire · front · density 4 | draft | not-started | chosen |

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

### Act intro and outro

Shown as described in [story](../../story/README.md#how-the-story-is-told): the title card, then
Okafor's full-screen act briefing over a still image, before the hangar visit that precedes L08.
After the Siege Spire falls, the act complete fanfare plays over L14's daylight outro, followed by
the act debrief.

**Title card**

> ACT II — HOMEFRONT
>
> Earth · May 2185

**Act briefing** (still image: Nova Lagos at night seen from low orbit, fires along the coast,
the landing trails of the second fleet cutting down through the cloud deck)

> **Commander Okafor:** "The Brood Carrier is dead. It was not the fleet. It was the vanguard.
>
> While we fought at L1, a second fleet, far larger, came in behind the debris field. Forty
> hours ago it made landfall on three continents. The orbital defences never saw it coming
> down.
>
> Aegis Wing is recalled to Earth. We go where the line is breaking: the cities first, then the
> sea lanes, then the Arctic relays that hold the planetary defence grid together.
>
> From today you fly over home. Over streets you know, and over people who will be watching the
> sky for you. Fly accordingly.
>
> Mission briefings follow. Aegis Actual out."

**Act debrief** (after L14; still image: the withered stump of the Spire in Geneva at sunrise,
the lake bright behind it)

> **Commander Okafor:** "The Siege Spire is dead. Within the hour every Vrell force on Earth lost
> its coordination. Some are digging in, most are scattering, and the army is clearing them city
> by city.
>
> Nova Lagos is counting its losses. So is Geneva. So are we. Remember the names, then get some
> sleep.
>
> An hour ago Ares Landing sent a priority signal. Mars is under siege, and the colonies have been
> holding alone for three weeks.
>
> We take the fight to them now. Aegis Wing ships out for Mars in seventy-two hours. Aegis Actual
> out."

### Settings

[Earth](../../world/earth/README.md): megacity (L08–10, L14), open ocean (L11–12), arctic (L13).

### New mechanics

- AI wingman (L08), hardened ground targets that need `anti-ground` (L09).
- Rear attacks as a level focus (L10), naval surface targets and the `sub` layer as scenery
  (L11), weather hazards (L12), `defend` objective with halted scroll (L13).

### Factions present

Vrell only.

### Boss

[Siege Spire](../../enemies/bosses/siege-spire/README.md) (L14); mid-boss
[Harbour Kraken](../../enemies/bosses/harbour-kraken/README.md) (L11).

### Units in the act

New: Creeper (L08), Hive Node and Ravager (L09), Wraith and Mote Swarm (L10), Driftjelly, Reef
Spitter and the Harbour Kraken (L11), Lamprey (L12), Skimmer (L13), Siege Spire (L14).
Returning from Act 1 (with the act HP factor from the
[balancing basis](../../enemies/README.md#balancing-basis)): Skitter, Needler, Stinger (L08–L14),
Spine Turret, Polyp Mortar (L08, L09, L13, L14), Spore Bomber (L12), Mantis (L12, L13), Whirl
Seed (L12), Scuttler (L13). The level documents list the waves.

### Music

Per level, from the [track list](../../audio/music/README.md#track-list): L08, L10 and L13 use the
Act 2 A theme "Homefront"; L09, L11 and the L14 approach use the Act 2 B theme "Firestorm"; the
Kraken (L11) gets the mini-boss sting; the Siege Spire uses the boss warning and the Vrell boss
theme "The Choir Descends", then the act complete fanfare. The storm variant planned for L12 is
not in the track list yet, so L12 uses "Firestorm" for now (see [L12](level-12-storm-front/README.md)).

## Implementation

- [ ] All 7 levels promoted to draft level documents and implemented.
- [ ] Weather system (rain, lightning, wind drift) for L12.
- [ ] Halted-scroll `defend` mode for L13.
- [ ] Act title card, act briefing and act debrief as in *Act intro and outro*.

## Open questions

- None open.

## Decisions

- 2026-09-30: Earth megacity names Nova Lagos and Geneva Concord, now recorded as sub-locations in [earth](../../world/earth/README.md).
- 2026-09-30: Enemy variety pass: new units added to the level rows so the act passes the [variety checklist](../../enemies/README.md#variety-checklist-per-act).
- 2026-10-01: L09 introduces the Ravager, an animal-like Vrell pack hunter (round 05 follow-up).
- 2026-10-01: Objective failure rules applied to L09 (destroy-targets), L10 (escort) and L13 (defend). L10: one scripted shuttle loss for the story; the rest depends on the player.
- 2026-10-01: Levels 08–14 promoted from the roster to draft level documents; Rook joins as the player's wingman in the escort slot at L08; radio chatter is text plus radio blips only (user decisions). Act intro (title card and briefing) and act debrief added.
