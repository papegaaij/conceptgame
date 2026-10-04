---
title: Level 06 – Farside
design: approved
implementation: in-progress
art: chosen
depends-on: [../../../enemies/air/mantis, ../../../enemies/air/coilwyrm, ../../../world/luna]
updated: 2026-10-04
---

# Level 06 – Farside

## Summary

Three farside mining settlements have gone silent. Lancer crosses the terminator into total
darkness, flying by headlight, dome lights and flares over Daedalus Rim, and finds the domes
intact and empty: the Vrell take people. Introduces darkness, the Mantis snipers that hold at
the screen edges, and the Coilwyrm, the first segment chain, whose loop-backs strike from the
rear before the player can own a rear gun. About 3 minutes, no boss.

## Briefing

> **Commander Okafor:** "Lancer, three farside settlements stopped answering in the last thirty
> hours. Daedalus Rim is the largest: two thousand people, mostly miners and their families. No
> distress call, no wreckage, just silence. You'll cross the terminator into full dark. Your
> headlight and whatever the settlement still has lit are all you'll see by. Find out what
> happened. If the Vrell are still there, make them leave."
>
> **Dr. Varga:** "Two new contacts. A sniper that sits at the edges of your screen, so guns that
> fire sideways will help. And something long that moves like a serpent and likes to come round
> behind you. Your rear is open, Lancer. Don't fight it there. Move."

*Hangar teaser* (shop screen after L05):
> **Dr. Varga:** "Farside is dark, and they're holding at the edges. Something that shoots
> sideways would earn its keep."

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `air`, `ground` (turrets and mortars, seen only by their glow) |
| Attack directions | **sides and rear focus**: front 53% · sides 29% · rear 18% (the rear share is Coilwyrm loop-backs plus one bottom-edge entry; 65% of waves enter from the top edge) |
| Density | 3 |
| Recommended traits | `side`, `spread` (`homing` from the Micro-missile Pod helps against the edges) |
| Hazards | Darkness: ground visible only in light pools; no rear-firing weapon available yet |
| Boss / mid-boss | none |
| Sensor-suite detail | none: Luna far side, `air` + `ground`, front · L1: + directions with the rear warning, density 3, hazard "darkness" · L2: + Mantis, Coilwyrm, Needler, Stinger, Skitter, Spine Turret, Polyp Mortar portraits; Smart Bomb available · L3: + `side` highlighted, wave strip, 1 secret + 1 data core |

## Objective

- **Primary** `reach-end`: survive to the end of the scroll.
- **Secondary** *Clear the edges*: destroy all 8 Mantis before they leave. +50.

## Layout

Scene: the chosen [Luna far side](../../../art-direction/concept/scene-luna-farside-r09-a.png)
scene (see [Luna](../../../world/luna/README.md), sub-location *Far side*). Scroll speed 130 px/s
(calm, so the dark stays readable). Total ≈ 190 s ≈ 24,700 px; the launch starts 5 s from the
bottom edge, as in Levels 01–05. Motion budget: the falling flares'
moving light pools and the regolith plumes are the two strong background movers; the domes and
rail lamps are static light.

| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Parallax content per layer | Purpose |
|---|---|---|---|---|---|---|
| 1. Terminator | 0–25 | 0–3,250 | 130 | clear | `deep`: stars and the Milky Way; no Earth (it never rises on the far side). `ground`: the last sunlit crater rims sliding into black; the headlight cone switches on. | Into the dark; the headlight. |
| 2. Silent Domes | 25–70 | 3,250–9,100 | 130 | clear | `ground`: Daedalus Rim's outer domes, lights on, airlocks open; the ore rail with lamp posts. `low-air`: none. | First Mantis alone; edges matter. |
| 3. Observatory Array | 70–110 | 9,100–14,300 | 130 | light | `ground`: radio-silent dish arrays, a dark crater (the secret). `low-air`: faint regolith plumes. `high-air`: tumbling ejected rocks. | First Coilwyrm (no loop), then the first loop-back. Flare at t=70. |
| 4. Vrell Fields | 110–160 | 14,300–20,800 | 130 | medium, heavy peak 140–146 | `ground`: teal nest growth with branching luminous tendrils spreading between domes. `low-air`: regolith plumes; a collapsing dome's dust cloud (the heavy peak). | Mantis pincers with Coilwyrms; turrets known only by their glow. Flares at t=112 and t=150. |
| 5. Daedalus Gate | 160–190 | 20,800–24,700 | 130 | light | `ground`: the settlement's main airlock, wide open and lit; the empty landing pad. | Final Coilwyrm and Mantis pincer; the data core. |

### Darkness rules

- **What is darkened** (user decision D2 of M4 part F): only the **ground layer and the ground
  units**, by a runtime light map over them. The ground is near-black except in light pools:
  **dome lights** and **rail lamps** (static, warm), **flares** (scripted, see below), the ship's
  **headlight**, a cool cone 200 px long and 60° wide ahead of the ship (260 px on easy), switched
  on with Varga's t=22 line, and the player's shots, which light the ground they pass.
- **Air units stay fully lit**: the darkness never touches the `air` layer, so the Mantis,
  Coilwyrm and the rest of the air roster read as in any level.
- **Turrets and mortars** are seen by their glow: the Spine Turret and the Polyp Mortar get
  **glow frames** (their emissive parts: barrel root, lime mouth) drawn at full brightness after
  the light pass; the rest of their body is lit only inside light pools. Rejected: a
  luminance-threshold shader (no new art, but bright highlights survive as "glow") and darkness
  baked into the tiles (cheapest, but the glow-only turrets are lost).
- **Enemy bullets, edge warnings and mortar markers are always fully visible** (readability
  rules); darkness never hides a threat to the ship, only bodies and scenery.
- **Flares**: the Daedalus perimeter beacon, still on automatic, fires a flare shell at t=70,
  t=112 and t=150. Each flare falls slowly for 8 s (12 s on easy), casting a moving 240 px
  orange-white pool; each flare's x is authored in the level data. Hard drops the t=112 flare.

## Waves

Enemy specs: [Mantis](../../../enemies/air/mantis/README.md),
[Coilwyrm](../../../enemies/air/coilwyrm/README.md),
[Needler](../../../enemies/air/needler/README.md),
[Stinger](../../../enemies/air/stinger/README.md),
[Skitter](../../../enemies/air/skitter/README.md).

| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 30 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front | |
| 36 | 2 | single | [Mantis](../../../enemies/air/mantis/README.md) | 1 | sides (left) | **Introduction**: alone, edge warning, radio tip |
| 48 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 56 | 2 | pincer | [Mantis](../../../enemies/air/mantis/README.md) | 2 | sides | One per edge |
| 64 | 2 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | Crimson dive glow reads in the dark |
| 78 | 3 | snake (solo) | [Coilwyrm](../../../enemies/air/coilwyrm/README.md) | 1 | front | **Introduction**: `swirl` down and out of the bottom edge, **no loop** |
| 88 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 8 | front (alternating edges) | |
| 96 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 4 | front | |
| 102 | 3 | snake (solo) | [Coilwyrm](../../../enemies/air/coilwyrm/README.md) | 1 | front → rear | `loop` + `rear-entry` after 6 s; edge warning 3 s; radio "six" |
| 116 | 4 | pincer | [Mantis](../../../enemies/air/mantis/README.md) | 2 | sides | |
| 124 | 4 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | |
| 134 | 4 | snake (solo) | [Coilwyrm](../../../enemies/air/coilwyrm/README.md) | 1 | rear | Enters from the bottom edge (`rear-entry`), swirls up and out the top |
| 146 | 4 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front | Out of the heavy dust |
| 152 | 4 | single | [Mantis](../../../enemies/air/mantis/README.md) | 1 | sides (right) | |
| 162 | 5 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 170 | 5 | snake (solo) | [Coilwyrm](../../../enemies/air/coilwyrm/README.md) | 1 | front → rear | `figure-8` then `loop` to the rear; edge warning |
| 172 | 5 | pincer | [Mantis](../../../enemies/air/mantis/README.md) | 2 | sides | Finale with the last Coilwyrm |

Totals: Mantis 8 · Coilwyrm 4 · Needler 12 · Stinger 6 · Skitter 20.

Rear attacks are dodged, not shot: the first rear-firing weapon (Tail Gun) is in the shop from
L08 (the Side Splitter on the rear mount fires sideways). Every side and rear entry is edge-warned
**3 s** ahead, the code's minimum (the 1.5 s of the first draft follows it; easy warns
Coilwyrm loop-backs 4 s ahead), and every rear strike is called by Rook (see *Radio chatter*).
A loop-back re-enters from the bottom edge at the head's x 6 s after the head left the screen.
The Mantis hovers at y = 120–360 px with two sweeps per 6 s hover, its sprite mirrored per side.

**Density** (user decision D1 of M4 part F): the table above is the first draft, 66 enemies at
medium (air 50, ground 16) over 185 s of scroll, about 21 a minute. The level reaches the
campaign's [minimum](../../README.md#difficulty-curve) of **at least 40 enemies a minute** (124
or more over the 185 s) with **more and larger waves of this level's own light enemies**
(Skitter streams and snakes, Needler and Stinger groups) overlapping the threats, never generic
filler; the Mantis and Coilwyrm beats stay readable. A **Coilwyrm counts as one enemy** for the
density and the kill ratio (its segments pay bounty only). The added waves are written in the
build step, in the level's `data.yaml`, whose rendered *Waves* table replaces this one.

## Ground targets

| Section | t (s) | Target | Effect |
|---|---|---|---|
| 2 | 45 | [Spine Turret](../../../enemies/ground/spine-turret/README.md) ×2 between domes | Bounty; violet barrel glow only until lit |
| 3 | 70 | [Polyp Mortar](../../../enemies/ground/polyp-mortar/README.md) ×2 at the array | Bounty; revealed by the first flare |
| 3 | 90 | Turret nest: Spine Turret ×3 | Bounty |
| 4 | 118 | Polyp Mortar ×2 + Spine Turret ×2 in the Vrell field | Bounty |
| 4 | 140 | Turret nest: Spine Turret ×3 | Bounty; inside the heavy peak |
| 5 | 165 | Polyp Mortar ×2 on the landing pad | Bounty |
| 2 | 55 | Abandoned ore cart on the rail (ground layer, 3 HP), under a rail lamp | Medium salvage (50) |

Totals: Spine Turret 10 · Polyp Mortar 6.

## Hazards

- **Darkness** (all sections; see *Darkness rules*).
- **No rear defence**: Coilwyrm loop-backs come from the bottom edge; the answer is to move
  out of the head's path. A Coilwyrm cut by side or spread fire regrows a head once (spec rule),
  so a careless cut doubles the threat.

## Secrets and pickups

Pickup types are defined in [player](../../../player/README.md#in-level-pickups).

- **Survey cache** (hidden crate, 135): a CDF survey cache in the dark crater at t≈95, invisible
  until the headlight or the t=112 flare lights it. Three marker lights on its lid; 3 hits open it.
  It **can only be hit while lit** (user decision D3 of M4 part F): the simulation knows the
  headlight cone and the flare pools (deterministic and ship-relative), and shots pass through
  it in the dark, so the secret is about finding it, not spraying fire into the dark.
- **Ore cart** (t≈55): medium salvage 50, under a rail lamp.
- **Data core** (t≈180): the open airlock's terminal at Daedalus Gate, lit by the dome light.
  2 hits release the core: the settlement's last log (lore entry). It unlocks the Targeting
  computer from the L07 hangar visit (see [economy](../../../systems/economy/README.md#data-cores)):
  part F records the core and the unlock in the save; the item takes effect in M5, when utility
  modules exist (user decision D6). The data core counts with the secrets for the grade.
- **Armour patch** ×2: dropped by the second Mantis of the t=56 pincer and by the last Needler
  of the t=162 line.
- **Overdrive**: dropped by the last Needler of the t=96 line.
- Shield cells at the normal rate.

## Radio chatter

Spoken radio ([voice](../../../audio/voice/README.md)). Rook flies Aegis Two on the north lane over the other settlements
(radio only). He is from a Luna mining family; this level is personal for him.

| Trigger | Speaker | Line |
|---|---|---|
| t=2 | Okafor | "Aegis, Daedalus Rim stopped answering thirty hours ago. Find out why." |
| t=10 | Rook | "Aegis Two on the north lane. I've got cousins on Daedalus, Lancer." |
| t=22 | Varga | "You're over the terminator. Headlight on. The Vrell glow; the ground doesn't. Trust the glow." |
| t=34 | Rook | "Something on your left edge. It's just… sitting there." |
| t=37 | Varga | "It's a sniper. It holds at the edge and sweeps a beam across you. Get an angle on it, or get away from that side." |
| t=50 | Generic CDF (Daedalus perimeter beacon, automated) | "…Daedalus perimeter. All residents report to shelter… all residents report…" |
| t=62 | Okafor | "Domes intact. Airlocks open. Nobody's home." |
| t=76 | Varga | "Long contact, moving like a snake. Cut it and the back half grows a new head. Go for the head." |
| t=100 (moves) | Rook | "It's turning, it's coming round behind you! Six, Lancer, six!" |
| t=101 | Varga | "You can't shoot behind you yet. Dodge it." |
| t=132 | Rook | "Contacts on six! Why is it always six?" |
| t=168 (moves) | Rook | "Another one's coming round. Watch your tail." |
| t=178 | Varga | "No bodies. No damage. Commander, the Vrell didn't kill them. They took them." |
| t=181 | Okafor | "…Understood. Log it, Varga. Aegis, finish this and come home." |
| Level end | Okafor | "Daedalus is empty. So are the others. We'll find them." |
| Level end +3 s | Rook | "…Copy. Aegis Two, heading home." |
| Data core collected | Varga | "That's the settlement log. I'll… read it later." |
| Secondary met | Okafor | "Not one sniper left standing. Good eyes in the dark, Lancer." |

Retiming in the build step (main-agent choice): Rook's "six" lines move after the Coilwyrm
loop-backs they call, to the start of each rear strike's edge warning (the t=100 line comes
before the t=102 chain has even entered; its loop-back hits about t=108; the same for t=168 and
the t=170 figure-8); Varga's t=101 follows Rook's line; the t=178/181 pair and t=34/37 are spread
to the HUD's 1 s rule (`RadioTimelineTest`). The Mantis and Coilwyrm first-sighting lines stay
timed. The perimeter beacon is `uncast` until its audition (user decision D7, see
[voice](../../../audio/voice/README.md)): two CC0/PD candidates through a public-address filter.

## Boss / mid-boss

None. The finale is the last Coilwyrm's figure-8 and loop-back with a Mantis pincer.

## Music & ambience

Track 5 *Act 1 B: Earth orbit & Luna* ("Coalition Rising", see the
[track list](../../../audio/music/README.md#track-list)): the base stem from section 1 (the far
side is sparse and tense), the full stem from section 4 (`full_section: 4`). Ambience: Luna
([sfx](../../../audio/sfx/README.md#ambience-per-setting)), with the perimeter beacon's
automated message looping faintly under section 2 (the same rendered line as its radio cue).
Ambience only from t=186, the last 4 s, after the Varga/Okafor exchange.

## Credit budget

Budget(6) = 700 × 1.07⁵ ≈ **982**, the typical haul's target
([economy](../../../systems/economy/README.md#per-level-budget)), not a perfect collection.
Bounties from the stat blocks: Mantis 30, Coilwyrm 86 per chain (head 40, 12 segments × 3,
tail 10; +10 if the tail goes first, so a perfect dismantle pays 96; a head-first kill pays the
head only), Needler 12, Stinger 15, Skitter 5, Spine Turret 12, Polyp Mortar 15.

The first-draft sources give a typical haul of about 875 before scaling; the density waves
(D1) add about 174, so a level `bounty_scale` near **0.93** lands the typical haul on 982. The
exact factor is set in the build step, where the *Credit budget* table is rendered from the
level's `data.yaml` and checked by `TypicalHaulTest` (±5 %).

## Difficulty notes

- **Easy**: Coilwyrm loop-backs are edge-warned 4 s ahead (instead of 3 s); flares last
  12 s; the headlight cone is 260 px long.
- **Hard**: Coilwyrms have 14 segments and the Mantis sweeps 90° (stat-block hooks); no flare
  at t=112; the t=146 Skitter snake is replaced by a new two-chain Coilwyrm wave, the chains
  crossing each other (*pairs crossing*; the t=134 chain has left by then); the Mantis sweeps every
  2.5 s.

## Implementation

- [ ] Scroll timeline, sections, atmosphere intensity and parallax content as in *Layout*.
- [x] Darkness: a light map darkens only the ground layer and the ground units (dome and rail
      lights, flares, the headlight cone and shot light); Spine Turret and Polyp Mortar glow
      frames drawn after it (a violet or lime glow until the glow frames exist); air units fully
      lit; bullets, edge warnings and mortar markers never darkened (the level data's `darkness`
      block, M4 part F step 2).
- [ ] Scripted flares at t=70, 112 and 150 with moving light pools.
- [ ] Wave script matches the *Waves* table; the first Coilwyrm does not loop.
- [ ] Every side and rear entry is edge-warned 3 s ahead (loop-backs 4 s on easy); Rook calls
      each rear strike at the start of its warning.
- [ ] At least 40 enemies per minute of scroll at medium (`DensityTest`), from Level 06's own
      light enemies; a Coilwyrm counts once.
- [ ] Survey cache revealed by light and hittable only while lit; data core at the airlock
      terminal, recorded in the save with its unlock.
- [ ] Secondary objective counts Mantis kills before they exit.
- [ ] Radio cues fire at their triggers.
- [ ] Typical haul at medium within ±5 % of budget(6) = 982 with the level's `bounty_scale`
      (`TypicalHaulTest`).
- [ ] Easy/hard variations as in *Difficulty notes*.

## Decisions

- 2026-10-01: Promoted from the act roster to a draft level document. Darkness never hides a
  threat to the ship (bullets, warnings, markers stay visible); it hides bodies, scenery and the
  secret. The settlement's emptiness is shown, not told: open airlocks, lights on, an automated
  beacon, and one line from Varga.
- 2026-10-01: Open question resolved: the data core unlocks the Targeting computer one act early, from the L07 hangar visit (table in economy).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-04: M4 part F decisions (user): **D1** at least 40 enemies a minute from more and
  larger waves of the level's own light enemies (never generic filler), a `bounty_scale` that
  lands the typical haul on budget(6) = 982 (700 × 1.07⁵; the 1,000 × 1.07⁵ = 1,403 perfect-run
  target was stale), a Coilwyrm counted as one enemy; rejected: counting the segments (games the
  rule) and a breather 20 % under (L06 is not one). **D2** only the ground layer and ground units
  are darkened, by a light map; turrets and mortars get glow frames; air units stay lit. **D3**
  the survey cache can only be hit while lit. **D4** the Mantis beam is 300 px and sweeps centred
  on the ship's bearing ([Mantis](../../../enemies/air/mantis/README.md)). **D5** one free Smart
  Bomb charge at its unlock ([specials](../../../player/specials/README.md)). **D6** the data
  core's unlock is recorded now, the Targeting computer takes effect in M5. **D7** the perimeter
  beacon is auditioned (two CC0/PD voices, public-address filter) in the part's round. **D8**
  a/b concepts only for parts without a chosen concept (the Mantis beam and telegraph, the ore
  cart, the survey cache, the data core and airlock terminal, the new sounds); everything shown
  in a chosen concept (Mantis, Coilwyrm, the Smart Bomb's flash, ring and sound, the backdrop with
  its flare shell and headlight cone, the intel portraits, the briefing images) goes straight to
  production; the turret and mortar glow frames are derived and reviewed only.
- 2026-10-04: Contradictions settled (main-agent choice): the 1.5 s edge warning follows the
  code's 3 s minimum (easy 4 s for loop-backs); Rook's "six" lines move after the loop-backs they
  call (retimed in the build step); the radio is spoken; "no rear weapon" reads "no rear-firing
  weapon" (the Side Splitter fires sideways); the music runs the base stem from section 1 and the
  full stem from section 4; hard's *pairs crossing* is a new two-chain wave at t=146.
- 2026-10-04: M4 part F step 2 built the level's mechanics (main-agent brief): the Coilwyrm's
  chain and loop-back, the Mantis's side hover and sweep, the darkness (the sim checks only the
  headlight cone and the flares; a `dark: true` trigger takes hits only while lit), the Smart
  Bomb and the data core (a secret with `data_core: {unlocks: ...}`), with placeholder art and
  sounds; the schema is in the [architecture](../../../tech/architecture/README.md#data-file-schemas).
  The waves, flares, survey cache and terminal are placed in the next step, in this level's
  `data.yaml`.
