---
title: Act 1 – First Contact
design: draft
implementation: not-started
art: none
updated: 2026-10-01
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
| [level-01-break-at-dawn](level-01-break-at-dawn/README.md) | Vrell scouts hit the Gagarin shipyards at dawn; the tutorial level | draft | not-started | chosen |
| [level-02-shipyard-burning](level-02-shipyard-burning/README.md) | The yards burn; first ground targets (Spine Turret), the Stinger and Crane Four; save the drydocks for a bonus | draft | not-started | chosen |
| [level-03-spore-drift](level-03-spore-drift/README.md) | Spore Bombers in the high lanes, a debris field, and the Leviathan set piece with Whirl Seed clusters | draft | not-started | chosen |
| [level-04-tranquility-run](level-04-tranquility-run/README.md) | First Luna level: escort five civilian crawlers past Brood Pods and Scuttler walkers; first special | draft | not-started | chosen |
| [level-05-crater-nest](level-05-crater-nest/README.md) | Destroy four nest batteries in a crater by the mass driver; Polyp Mortar; mid-boss Gorgon Frigate | draft | not-started | chosen |
| [level-06-farside](level-06-farside/README.md) | The dark far side: empty settlements, Mantis snipers at the edges, Coilwyrm loop-backs from the rear | draft | not-started | chosen |
| [level-07-brood-carrier](level-07-brood-carrier/README.md) | Through the L1 picket and the escort screen to the act boss, the Brood Carrier | draft | not-started | chosen |

## Design

### Synopsis

On 14 March 2185 the Tether Gate beyond Neptune activates. Six weeks later a Vrell strike
group appears at the Earth–Moon L1 point and falls on the **Gagarin shipyards**, the UTC's main orbital
shipyard. Aegis Wing scrambles at dawn. After saving what they can of the yards, the wing
follows the Vrell to Luna, where brood pods have landed near Tranquility Base and farside mining
settlements have gone silent. Dr. Varga traces the pods back to a single brood carrier sitting
at L1, and Commander Okafor sends Aegis Wing to kill it.

### Story beats

- L01: first contact. The Choir's first transmission is a wall of song (subtitled only
  "[the Choir sings]", as everywhere in Acts 1–2); Varga hears structure in it.
- L02: Varga reads one repeating pattern in the song as "*yield*". Rook, leading Aegis Two
  on the far side of each battle, and Lancer build their bond over the radio. Okafor is calm
  under fire.
- L03: the Leviathan crosses overhead, the first sign of how large the Vrell can grow. High
  Command assigns Hammer flight (the Airstrike) to Aegis.
- L04: the escort of refugee crawlers makes the war personal; Rook's family is on Luna.
- L05: the first Vrell warship drops out of orbit to defend the crater nest.
- L06: the farside settlements are found empty. The Vrell take people (a hook for later acts).
- L07: the Brood Carrier dies. The Choir sings again; Varga reads two words in it: "*many*" and
  "*late*". A second, larger fleet is detected heading for Earth.

Rook is Lancer's squadron mate throughout the act: he leads Aegis Two in a separate flight and
is heard on the radio only. He becomes Lancer's escort-slot wingman at L08 (the outro
announces it).

### Act intro and outro

Format: [briefing screen](../../ui/briefing/README.md) (act briefings may be longer than level
briefings). Order at a new game: title card → act briefing → first hangar visit (300 starting
credits) → L01 mission briefing → L01.

**Title card** (over a still of the Gagarin shipyards at dawn, Earth's terminator behind):

> ACT I
> FIRST CONTACT
> Earth orbit · Luna · April 2185

**Act briefing** (Commander Okafor, full screen, still image: the Tether Gate lit up beyond
Neptune, then Earth orbit):

> "On the fourteenth of March, the Tether Gate lit up. For forty-four years it was a dead ring
> beyond Neptune, something for scientists to argue about. Then it opened."
>
> "Eleven days later our outer stations stopped answering. We still don't know what happened to
> the people on them. We call what came through the Vrell. They don't answer our hails. They
> don't negotiate. Every ship they have met, they have destroyed."
>
> "This morning a Vrell strike group came out of nowhere at the Earth–Moon L1 point. It is
> heading for the Gagarin shipyards, where half the Coalition's new fleet is still in dock."
>
> "The Defence Force was built to chase pirates. Aegis Wing is the only squadron in Earth orbit
> with ships fast enough and pilots ready enough to fly today. That makes us the line."
>
> "Lancer, you've had the Stormhawk for nine days. Nobody has flown it longer. Suit up."

**Act-end outro** (after the L07 death sequence and its radio lines; track 24 *act complete*
under the first page; still image: the carrier's carcass drifting at L1 with Earth beyond):

> **Commander Okafor:** "The carrier is dead. The yards are scarred but standing, the
> Tranquility convoy made it home, and nothing is falling on Luna any more. A week ago, none of
> that was certain. Well flown, Aegis."
>
> **Commander Okafor:** "The farside settlements are still empty. Daedalus Rim and two others:
> four thousand people we could not find. I am not closing that file."
>
> **Dr. Varga:** "The fleet on long range is ten times the size of the carrier's group, and its
> course ends in Earth's atmosphere. They aren't coming to fight us in orbit, Commander. They're
> coming to land."
>
> **Commander Okafor:** "Then we meet them on the ground. Aegis is reassigned to Earth defence,
> effective now. And Lancer, from tomorrow Rook flies on your wing. Try to keep him out of
> trouble."

The outro is followed by the Act 2 title card (see [Act 2](../act-2-homefront/README.md)).

### Settings

[Earth orbit](../../world/earth-orbit/README.md) (L01–03, L07) and
[Luna](../../world/luna/README.md) (L04–06).

### New mechanics

- Basic movement and shooting, pickups, credits (L01).
- `ground` layer targets and the shipyard crane hazard (L02); `low-air` and `high-air`
  enemies, the debris field and the first overdrive pickup (L03).
- A `huge` set-piece enemy and tiny spinners (L03), a walker (L04), a segment-chain serpent
  that attacks from the rear (L06).
- `escort` objective, hardened targets and the first special, the Airstrike (L04);
  `destroy-targets` objective, the mass-driver sleds and the first mid-boss (L05).
- Darkness and enemies holding at the sides (L06).
- `high-air` boss: the carrier's hull passes above the player (L07).

### Enemy use per level

| Unit | Levels |
|---|---|
| [Skitter](../../enemies/air/skitter/README.md) | 01–07 (waves and spawns) |
| [Needler](../../enemies/air/needler/README.md) | 01–07 |
| [Stinger](../../enemies/air/stinger/README.md) | 02, 03, 05, 06, 07 |
| [Spine Turret](../../enemies/ground/spine-turret/README.md) | 02, 04, 05, 06 |
| [Spore Bomber](../../enemies/air/spore-bomber/README.md) | 03, 07 |
| [Whirl Seed](../../enemies/air/whirl-seed/README.md) | 03 (Leviathan clusters) |
| [Leviathan](../../enemies/space/leviathan/README.md) | 03 |
| [Brood Pod](../../enemies/air/brood-pod/README.md) | 04, 05, 07 |
| [Scuttler](../../enemies/ground/scuttler/README.md) | 04 |
| [Polyp Mortar](../../enemies/ground/polyp-mortar/README.md) | 05, 06 |
| [Gorgon Frigate](../../enemies/bosses/gorgon-frigate/README.md) | 05 (mid-boss) |
| [Mantis](../../enemies/air/mantis/README.md) | 06, 07 |
| [Coilwyrm](../../enemies/air/coilwyrm/README.md) | 06 |
| [Brood Carrier](../../enemies/bosses/brood-carrier/README.md) | 07 (boss) |

This passes the [variety checklist](../../enemies/README.md#variety-checklist-per-act) as
recorded there: five size tiers, four multi-part units, a walker (L04), a spinner (L03),
side and rear entries (L01, L06), 3 of 14 units insectoid.

### Factions present

Vrell only. The Ascendancy is not seen or mentioned in this act.

### Boss

[Brood Carrier](../../enemies/bosses/brood-carrier/README.md) (L07); mid-boss
[Gorgon Frigate](../../enemies/bosses/gorgon-frigate/README.md) (L05).

### Music

Act 1 uses tracks 4 *Act 1 A* ("Afterburner") and 5 *Act 1 B* ("Coalition Rising") from the
[track list](../../audio/music/README.md#track-list): B in L01, L03, L06 and L07 (the main
motif), A in L02, L04 and L05. L05 adds the mini-boss sting; L07 the boss warning, *Boss: Vrell*
and *Act complete*.

## Implementation

- [ ] All 7 levels promoted to draft level documents and implemented.
- [ ] Act 1 title card, opening briefing and act-end outro as in *Act intro and outro*.
- [ ] Shop unlocks for L02–L07 as listed in the campaign loadout-pressure table.

## Open questions

- None open.

## Decisions

- 2026-09-30: Act split: Earth orbit (L01–03, L07) and Luna (L04–06).
- 2026-09-30: Enemy variety pass: new units added to the level rows so the act passes the [variety checklist](../../enemies/README.md#variety-checklist-per-act).
- 2026-10-01: Objective failure rules applied to L02 (secondary: drydocks) and L04 (primary escort). No separate tutorial: contextual control prompts in the side HUD during L01–L03.
- 2026-10-01: Levels 02–07 promoted from the roster to draft level documents; act intro, title
  card and outro written. Text and radio blips only (no voice acting). Rook is a squadron mate on
  the radio (Aegis Two) until L08; L01 adjusted. The Choir stays unintelligible in Act 1 per its
  character spec: the spoken "Yield" (L01) and "We are many. You are late." (L07) become Varga's
  readings of its song, foreshadowing its first words in Act 3. The Brood Carrier fight stays
  at L1 as in the synopsis.
