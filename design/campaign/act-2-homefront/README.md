---
title: Act 2 – Homefront
design: approved
implementation: in-progress
art: chosen
updated: 2026-10-07
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
| [level-08-neon-skyline](level-08-neon-skyline/README.md) | Night megacity, Rook's first sortie on Lancer's wing, Creeper intro · front · density 3 | approved | done | final |
| [level-09-arcology-fall](level-09-arcology-fall/README.md) | The act's first `destroy-targets`: six hardened hive nodes, hold zones, Ravager packs, the arcology collapse · front · density 3 | approved | done | final |
| [level-10-evacuation-corridor](level-10-evacuation-corridor/README.md) | `escort` of five shuttles, first rear-heavy level (Wraith, Mote Swarm), one scripted loss · rear 42% · density 3 | approved | not-started | chosen |
| [level-11-atlantic-convoy](level-11-atlantic-convoy/README.md) | Naval layer (Driftjelly, Reef Spitter), `sub` shadows, mid-boss Harbour Kraken · front · density 3 | approved | not-started | chosen |
| [level-12-storm-front](level-12-storm-front/README.md) | Weather (rain, lightning, gusts), Lamprey intro, Varga sees the Vrell herd the storm · all · density 4 | approved | not-started | chosen |
| [level-13-polar-relay](level-13-polar-relay/README.md) | `defend` the Arctic relay for 180 s, Skimmer intro, whiteout · all edges · density 4 | approved | not-started | chosen |
| [level-14-siege-spire](level-14-siege-spire/README.md) | Geneva Concord approach through the root field, act boss Siege Spire · front · density 4 | approved | not-started | chosen |

## Design

### Synopsis

The Brood Carrier was a vanguard. A far larger fleet screens its approach with debris and makes
landfall across three continents. Aegis Wing is recalled to Earth. They fight through the
megacity of **Nova Lagos**, cover the civilian evacuation, and escort a supply convoy across an
Atlantic full of Vrell reef-growths. Then they hold the Arctic relay that coordinates the
planetary defence grid. Finally the Coalition throws everything at the Siege Spire, which grew
overnight in the middle of **Geneva Concord**, the UTC capital.

### Story beats

- Throughout the act the Choir breaks in at the big moments but only sings ("[the Choir
  sings]"), as in Act 1; Varga interprets what she can. Its first words wait for Act 3 (see
  [the Choir](../../story/characters/the-choir/README.md)).
- L08: Rook, a five-year Aegis Wing veteran who has flown in the wider formation so far, flies
  his first sortie as Lancer's wingman (Act 1's outro assigns him; the escort slot unlocks at the
  hangar visit before L08, see [wingmen](../../player/wingmen/README.md)).
- L10: civilians on the radio. The first loss the player can't prevent (a shuttle is scripted
  to be lost).
- L12: Dr. Varga notices the Vrell are herding the storm. They understand Earth's weather
  systems too well.
- L13: holding the relay keeps the grid online, which is the setup for the Mars counter-offensive.
- L14: with the Spire destroyed, the Vrell on Earth lose coordination. Okafor: "*We take the
  fight to them now.*" Mars calls for help.

### Act intro and outro

Format: [briefing screen](../../ui/briefing/README.md), as Act 1's. Order: the L07 debrief with
the act summary → Act 1's outro → this act's title card → the act briefing below → the L08 mission
briefing → the hangar visit before L08 (autosave) → L08, as the [ui](../../ui/README.md) screen
flow puts every mission briefing before its hangar visit. The game shows the title card and the
act briefing in front of the briefing of the act's first level; they live in the act's
[data.yaml](data.yaml) (`levels`, `title_card`, `briefing`, see the
[architecture](../../tech/architecture/README.md#data-file-schemas) schema text) and are rendered
here. A save written at the L08 hangar by an M4 build skips this intro (a default of M5 part B, accepted by the user);
`--level 7` shows it, since winning Level 07 plays the outro and then this intro. After the Siege
Spire falls, the act complete fanfare plays over L14's daylight outro, followed by the act debrief
(M5 part H).

**Title card** (held 3.5 s over the act's still, `ui/act-2-homefront-still.png`: Nova Lagos at
night seen from low orbit, fires along the coast, the landing trails of the second fleet cutting
down through the cloud deck; its chrome lettering from `tools/concept/ui_assets.py`):

<!-- data: act-title-card -->
> ACT II
> HOMEFRONT
> Earth · May 2185
<!-- /data -->

**Act briefing** (four pages by Commander Okafor, each voiced and with its own image in
`assets/ui/briefing/`, as Act 1's pages; user decision D3 of M5 part B). It continues from Act 1's
outro, which already told the carrier's death, the second fleet's course and Rook's assignment, so
it opens on the landfall twelve days later and does not repeat them.

| Page | Speaker | Image |
|---|---|---|
| 1 | Commander Okafor (grim) | `act-2-landfall`: the landers coming down at dawn, seen from below the cloud deck: dozens of burning trails over the sea, a CDF tracking overlay counting them |
| 2 | Commander Okafor | `act-2-front-lines`: a CDF globe display with the three landing zones glowing (Gulf of Guinea, Java Sea, River Plate) and the act's fronts marked: the cities, the Atlantic sea lanes, the Arctic relay chain |
| 3 | Commander Okafor | `act-2-over-home`: a city street at night from rooftop height, people on roofs and balconies looking up as a Stormhawk passes low |
| 4 | Commander Okafor (fierce) | `act-2-scramble`: Aegis Wing's Stormhawks on a coastal CDF airbase at dusk, canopies closing, Nova Lagos burning on the horizon |

<!-- data: act-briefing -->
> **Commander Okafor:** "Twelve days. That is how long we watched the second fleet come in. We
> tracked every ship of it, and we could not stop one. At four o'clock this morning it came down
> through the cloud deck in three places: the Gulf of Guinea, the Java Sea and the River Plate."
>
> **Commander Okafor:** "The landers came down on the cities. The army holds the streets where
> it can; the sky is ours to hold. Aegis Wing goes where the line is breaking: the cities first,
> then the sea lanes, then the Arctic relays that tie the planetary defence grid together."
>
> **Commander Okafor:** "Until now you fought over shipyards and craters, in vacuum. From today
> you fly over home: over streets you know, and over people who will be watching the sky for
> you. Fly accordingly."
>
> **Commander Okafor:** "We could not stop them landing. We can stop them staying. Mission
> briefing follows. Aegis Actual out."
<!-- /data -->

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

- AI wingman (L08); hardened ground targets that need `anti-ground`, hold zones where the scroll
  slows over the targets, and the arcology's collapse (L09).
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
Returning from Act 1: Skitter, Needler, Stinger (L08–L14), Spine Turret, Polyp Mortar (L08, L09,
L13, L14), Spore Bomber (L12), Mantis (L12, L13), Whirl Seed (L12), Scuttler (L13). Only the
returning `medium` and larger units get the act HP factor from the
[balancing basis](../../enemies/README.md#balancing-basis) (D5 = c of M5 part A): here the Spore
Bomber, the Mantis and the Scuttler, and the act's own units in the levels after their first: the
Creeper (L08) in L09 (30 → 32 HP) and L14 (40), the Hive Node and the Ravager (L09) in L14. The
`tiny` and `small` ones (Skitter, Whirl Seed, Needler, Stinger, Spine Turret, Polyp Mortar) keep
their HP, so none of Level 08's returning units changes.
The level documents list the waves.

### Music

Per level, from the [track list](../../audio/music/README.md#track-list): L08, L10 and L13 use the
Act 2 A theme "Homefront"; L09, L11, L12 and the L14 approach use the Act 2 B theme "Firestorm"
(L12 has no separate storm variant); the Kraken (L11) gets the mini-boss sting; the Siege Spire
uses the boss warning and the Vrell boss theme "The Choir Descends", then the act complete
fanfare.

### Voice

Every briefing page and radio line is voiced, as in Act 1 ([voice](../../audio/voice/README.md)):
the act briefing's four pages and each level's briefing pages and radio; the hangar teaser is text
only, by design (round 30). A new speaker is auditioned in the round of the level that introduces
it (Level 08: the Ikoyi shelter civilian, round 30; Level 09: the Kilo Lead, the CDF officer of the
truck convoy, round 31) and plays as text until it is cast.

## Concept art

Production art for concept round 30 (M5 part B batch): the still behind the act's title card,
rendered by `tools/art/act_stills.py act-2` from Level 08's production backdrop
(`tools/art/backdrop_l08.py`, its pieces in `assets/backdrop/level-08/`), and its chrome lettering
(`ui/act-2-homefront-title.png`, `tools/concept/ui_assets.py`); the act briefing's four images are
the [briefing screen](../../ui/briefing/README.md)'s. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/act-2-still-final-r30-a.png](concept/act-2-still-final-r30-a.png) | `ui/act-2-homefront-still.png` (960×540): Nova Lagos at night from above, built from Level 08's production backdrop (its avenues and rooftops, towers with their walls projected, the harbour stepping down to the Gulf of Guinea bottom left), fires on the yards and their smoke, a cloud deck over the top and the right lit from below, eight landers (small Vrell seed pods with glowing heat shields) coming down through it from the top right on their trails; and the title card over it as the game draws it (darkened to 55 %, letterboxed, chrome lettering) | chosen |

## Implementation

- [ ] All 7 levels promoted to draft level documents and implemented.
- [ ] Weather system (rain, lightning, wind drift) for L12.
- [ ] Halted-scroll `defend` mode for L13.
- [x] Act title card over its still (`ui/act-2-homefront-still.png`, `tools/art/act_stills.py`) with
      its lettering (`ui/act-2-homefront-title.png`, `tools/concept/ui_assets.py`), then the four
      voiced act briefing pages with their images, before the L08 briefing at the act's start
      ([data.yaml](data.yaml); M5 part B).
- [ ] Act debrief as in *Act intro and outro* (M5 part H).

## Open questions

- None open.

## Decisions

- 2026-09-30: Earth megacity names Nova Lagos and Geneva Concord, now recorded as sub-locations in [earth](../../world/earth/README.md).
- 2026-09-30: Enemy variety pass: new units added to the level rows so the act passes the [variety checklist](../../enemies/README.md#variety-checklist-per-act).
- 2026-10-01: L09 introduces the Ravager, an animal-like Vrell pack hunter (round 05 follow-up).
- 2026-10-01: Objective failure rules applied to L09 (destroy-targets), L10 (escort) and L13 (defend). L10: one scripted shuttle loss for the story; the rest depends on the player.
- 2026-10-01: Levels 08–14 promoted from the roster to draft level documents; Rook joins as the player's wingman in the escort slot at L08; radio chatter is text plus radio blips only (user decisions). Act intro (title card and briefing) and act debrief added.
- 2026-10-01: Music: L12 uses "Firestorm"; the promised storm variant is dropped (user decision).
- 2026-10-01: Choir in Acts 1–2 confirmed by the user: it only sings ("[the Choir sings]") and Varga interprets it; the Choir, story and act documents agree.
- 2026-10-01: Art status set to `chosen`: every level of the act uses chosen concept art.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-06: M5 part B (user decisions of 2026-10-06): **D3 = a** the act briefing is rewritten
  to four pages (the landfall, where the line is breaking, flying over home, the hand-off to the
  mission briefing), each with its own image and Okafor's expression, like Act 1's; it continues
  from Act 1's outro instead of repeating it (the carrier's death, the second fleet's course and
  Rook's assignment are the outro's). The contradictions are fixed: "forty hours ago" against the
  L08 briefing's "overnight" (now: the landfall at four this morning, twelve days after the outro,
  and Level 08 flies that night), and "the orbital defences never saw it coming down" against
  Varga's tracking in the outro (now: "we tracked every ship of it, and we could not stop one").
  The title card and the pages move into the act's [data.yaml](data.yaml) (`levels: [8, 14]`),
  rendered into *Act intro and outro*; the still described for the act briefing becomes the title
  card's still. The new text goes back to `review` for the user (round 30).
- 2026-10-06: Stale points corrected: the act HP factor applies only to returning `medium` and
  larger units (D5 = c of M5 part A), so Level 08's returning units keep their HP; "radio chatter
  is text plus radio blips only" (2026-10-01) is reversed: every line is voiced since Act 1 (see
  *Voice*). Implementation `in-progress`: the act's data file exists.
- 2026-10-07: The still re-rendered from the current Level 08 backdrop after its readability pass
  (round 30): byte-identical, as the still draws none of the pieces the pass changed (the turret
  nests' landing pad and roof, the parking deck, the smoke banks); its two ringed pads are helipads
  on tall tower roofs. The Concept art prose no longer says it is built from round 03's
  concept (that was its first version).
- 2026-10-07: [Concept round 30](../../concept-rounds/round-30/README.md) closed (user,
  2026-10-07): the title card's still of Nova Lagos with the landers approved as **final** (its
  lower-than-orbit view as it is), the act briefing's four images too
  ([briefing](../../ui/briefing/README.md#decisions)); the texts approved (the title card ACT II ·
  HOMEFRONT · Earth · May 2185, the four act briefing pages, voiced as rendered), so the document
  leaves `review` for `approved`, as Level 08's does. `art` stays `chosen`: Levels 09–14 use
  concept art until their rounds.
- 2026-10-07: M5 part C started (Level 09, user decisions D1–D11 of 2026-10-07): the Contents rows
  lost their stray backticks (Levels 09, 10, 13); the act HP factor's list names the Creeper,
  which returns in L09 (32 HP) and L14, and the Hive Node and Ravager in L14; the *Voice* section no
  longer says the hangar teaser is voiced (it is text only, by design since round 30) and names
  Level 09's new speaker; *New mechanics* names L09's hold zones and collapse.
