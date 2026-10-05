---
title: Level 06 – Farside
design: approved
implementation: done
art: final
depends-on: [../../../enemies/air/mantis, ../../../enemies/air/coilwyrm, ../../../world/luna]
updated: 2026-10-05
---

# Level 06 – Farside

## Summary

Three farside mining settlements have gone silent. Lancer crosses the terminator into total
darkness, flying by headlight, dome lights and flares over Daedalus Rim, and finds the domes
intact and empty: the Vrell take people. Introduces darkness, the Mantis snipers that hold at
the screen edges, and the Coilwyrm, the first segment chain, whose loop-backs strike from the
rear before the player can own a rear gun. About 3 minutes, no boss.

## Briefing

<!-- data: briefing -->
> **Commander Okafor:** "Lancer, three farside settlements stopped answering in the last thirty
> hours. Daedalus Rim is the largest: two thousand people, mostly miners and their families. No
> distress call, no wreckage, just silence. You'll cross the terminator into full dark. Your
> headlight and whatever the settlement still has lit are all you'll see by. Find out what
> happened. If the Vrell are still there, make them leave."
>
> **Dr. Varga:** "Two new contacts. A sniper that sits at the edges of your screen, so guns that
> fire sideways will help. And something long that moves like a serpent and likes to come round
> behind you. Your rear is open, Lancer. Don't fight it there. Move."
<!-- /data -->

*Images* (production art straight away, user decision D8; [briefing images](../../../ui/briefing/README.md),
accepted in [round 23](../../../concept-rounds/round-23/README.md); both still to be re-rendered for the
Coilwyrm's 0.5 spacing and the beam from the Mantis's head): Okafor's page
`level-06-daedalus-rim` (the far side across the terminator, two settlements silent, Daedalus Rim's
lit domes with its 2,000 people, Lancer's run into the dark by headlight along the ore rail), Varga's
`level-06-edge-scan` (the Mantis at the screen edge sweeping its beam across, side-firing guns
reaching it; the Coilwyrm chain coming round behind the ship, its open rear, "move"); the level's
data names them as each page's `image`.

*Hangar teaser* (shop screen after L05):
<!-- data: teaser -->
> **Dr. Varga:** "Farside is dark, and they're holding at the edges. Something that shoots
> sideways would earn its keep."
<!-- /data -->

## Threat profile

<!-- data: threat-profile -->
| Field | Value |
|---|---|
| Dominant layers | `air`, `ground` (turrets and mortars, seen only by their glow) |
| Attack directions | front 95% · sides 5% · rear 1% by enemy count (the Skitter streams come from the front); by threat **the sides and rear**: 8 Mantis at the edges, and every Coilwyrm but the first strikes from the bottom edge (two loop-backs, one rear entry) |
| Density | 3 |
| Recommended traits | `side`, `spread` (`homing` from the Micro-missile Pod helps against the edges) |
| Hazards | Darkness: the ground visible only in light pools; no rear-firing weapon available yet |
| Boss / mid-boss | none |
| Sensor-suite detail | none: Luna far side, `air` + `ground`, front · L1: + directions with the rear warning, density 3, hazard "darkness" · L2: + Mantis, Coilwyrm, Needler, Stinger, Skitter, Spine Turret, Polyp Mortar portraits; Smart Bomb available · L3: + `side` highlighted, wave strip, 1 secret + 1 data core |
<!-- /data -->

## Objective

- **Primary** `reach-end`: survive to the end of the scroll.
- **Secondary** *Clear the edges*: destroy all 8 Mantis before they leave (`escapes: mantis`;
  failed at the first Mantis that leaves alive). +50.

## Layout

Scene: the chosen [Luna far side](../../../art-direction/concept/scene-luna-farside-r09-a.png)
scene (see [Luna](../../../world/luna/README.md), sub-location *Far side*). Scroll speed 130 px/s
(calm, so the dark stays readable). Total ≈ 190 s ≈ 24,700 px; the launch starts 5 s from the
bottom edge, as in Levels 01–05. The backdrop is Level 06's own (`tools/art/backdrop_l06.py`, the
data's `backdrop` block): the terminator, dome-field, array-field and Vrell-field ground tile sets
with the ore rail at x 410 through all of them, the domes, dish arrays, nests, the dark crater, the
landing pad and Daedalus Gate. Motion budget: the falling flares'
moving light pools and the regolith plumes are the two strong background movers; the domes and
rail lamps are static light.

<!-- data: level-sections -->
| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Terminator | 0–25 | 0–3,250 | 130 | clear | `ground`: the last sunlit crater rims sliding into black (no `deep` layer on Luna, so no Milky Way and no Earth: it never rises on the far side); the headlight cone switches on at t=22. | Into the dark; the headlight. |
| 2. Silent Domes | 25–70 | 3,250–9,100 | 130 | clear | `ground`: Daedalus Rim's outer domes, lights on, airlocks open; the ore rail with lamp posts. `low-air`: none. | First Mantis alone; edges matter. |
| 3. Observatory Array | 70–110 | 9,100–14,300 | 130 | light | `ground`: radio-silent dish arrays, a dark crater (the secret). `low-air`: faint regolith plumes. | First Coilwyrm (no loop), then the first loop-back. Flare at t=70. |
| 4. Vrell Fields | 110–160 | 14,300–20,800 | 130 | medium, heavy peak 140–146 | `ground`: teal nest growth with branching luminous tendrils spreading between domes. `low-air`: regolith plumes; a collapsing dome's dust cloud (the heavy peak). | Mantis pincers with Coilwyrms; turrets known only by their glow. Flares at t=112 and t=150. |
| 5. Daedalus Gate | 160–190 | 20,800–24,700 | 130 | light | `ground`: the settlement's main airlock, wide open and lit; the empty landing pad. | Final Coilwyrm and Mantis pincer; the data core. |
<!-- /data -->

### Darkness rules

- **What is darkened** (user decision D2 of M4 part F): only the **ground layer and the ground
  units**, by a runtime light map over them. The ground is near-black except in light pools:
  **dome lights** and **rail lamps** (static, warm), **flares** (scripted, see below), the ship's
  **headlight**, a cool cone 200 px long and 60° wide ahead of the ship (260 px on easy), switched
  on with Varga's t=20 line, and the player's shots, which light the ground they pass.
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
  orange-white pool drifting 30 px/s down the screen; each flare's x and height are authored in
  the level data (the t=112 flare bursts over the survey cache as it passes). Hard drops the t=112
  flare.
- **Static pools**: nine dome and rail lamps in the data's `darkness.lights` (the outer domes, the
  rail lamp over the ore cart, a lit dome in the Vrell field, the landing pad and the airlock's
  dome light over the terminal); ambient 0.12 elsewhere.

## Waves

Enemy specs: [Mantis](../../../enemies/air/mantis/README.md),
[Coilwyrm](../../../enemies/air/coilwyrm/README.md),
[Needler](../../../enemies/air/needler/README.md),
[Stinger](../../../enemies/air/stinger/README.md),
[Skitter](../../../enemies/air/skitter/README.md).

<!-- data: waves -->
| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 6 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | Over the last sunlit rims |
| 11 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | |
| 16 | 1 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | Into the terminator |
| 18.5 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | As the headlight switches on |
| 23.5 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 28.5 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | |
| 33 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | Away from the Mantis's edge |
| 36 | 2 | single | [Mantis](../../../enemies/air/mantis/README.md) | 1 | left side | **Introduction**: alone, edge warning, radio tip |
| 37.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 42 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | Over the dome turrets |
| 48 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 52 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | |
| 56 | 2 | pincer | [Mantis](../../../enemies/air/mantis/README.md) | 2 | sides | One per edge |
| 60 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | Between the two Mantis |
| 64 | 2 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | Crimson dive glow reads in the dark |
| 67 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | |
| 72 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | Under the first flare |
| 78 | 3 | snake | [Coilwyrm](../../../enemies/air/coilwyrm/README.md) | 1 | front | **Introduction**: `swirl` down and out of the bottom edge, **no loop** |
| 84 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 88 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 8 | front (alternating edges) | |
| 92.5 | 3 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | |
| 96 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 4 | front | |
| 99 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | |
| 102 | 3 | snake | [Coilwyrm](../../../enemies/air/coilwyrm/README.md) | 1 | front | `loop` out of the bottom edge, back from the rear 6 s later (`rear-entry`; edge warning 3 s, 4 s on easy); radio "six" |
| 104.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 110 | 4 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | Into the Vrell fields |
| 113 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | Under the second flare |
| 116 | 4 | pincer | [Mantis](../../../enemies/air/mantis/README.md) | 2 | sides | |
| 120 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 124 | 4 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | |
| 127.5 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | Up to the rear entry |
| 134 | 4 | snake | [Coilwyrm](../../../enemies/air/coilwyrm/README.md) | 1 | rear | Enters from the bottom edge (`rear-entry`), swirls up and out the top |
| 137 | 4 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | |
| 142 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | In the heavy dust |
| 146 | 4 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | Out of the heavy dust; hard: the Coilwyrm pair instead |
| 146 | 4 | snake | [Coilwyrm](../../../enemies/air/coilwyrm/README.md) | 2 | front | Hard only: *pairs crossing*, the second chain on the mirrored path |
| 150 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | Under the third flare |
| 152 | 4 | single | [Mantis](../../../enemies/air/mantis/README.md) | 1 | right side | |
| 155 | 4 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | |
| 158 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | |
| 162 | 5 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 166 | 5 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 168 | 5 | snake | [Coilwyrm](../../../enemies/air/coilwyrm/README.md) | 1 | front | `figure-8`, then out of the bottom edge and back from the rear (edge warning) |
| 172 | 5 | pincer | [Mantis](../../../enemies/air/mantis/README.md) | 2 | sides | Finale with the last Coilwyrm |
| 176 | 5 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 179 | 5 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | Past the open airlock |

Totals: Skitter 122 · Needler 20 · Mantis 8 · Stinger 12 · Coilwyrm 4.
<!-- /data -->

Rear attacks are dodged, not shot: the first rear-firing weapon (Tail Gun) is in the shop from
L08 (the Side Splitter on the rear mount fires sideways). Every side and rear entry is edge-warned
**3 s** ahead, the code's minimum (the 1.5 s of the first draft follows it; easy warns
Coilwyrm loop-backs 4 s ahead), and every rear strike is called by Rook (see *Radio chatter*).
A loop-back re-enters from the bottom edge at the head's x 6 s after the head left the screen.
The Mantis hovers at y = 120–360 px with two sweeps per 6 s hover, its sprite mirrored per side.
The finale's figure-8 comes at t=168 (2 s before the first draft's 170) so its loop-back strikes
at t=183.6, before the quiet last seconds.

**Density** (user decision D1 of M4 part F): the first draft had 66 enemies at medium (air 50,
ground 16) over 185 s of scroll, about 21 a minute. The level reaches the campaign's
[minimum](../../README.md#difficulty-curve) of at least 40 enemies a minute with **more and
larger waves of this level's own light enemies** (Skitter streams and snakes, Needler and Stinger
groups) between and around the Mantis and Coilwyrm beats, never generic filler: 182 enemies at
medium (air 166, ground 16), **59.0 a minute** (`DensityTest`). A **Coilwyrm counts as one enemy**
for the density and the kill ratio (its segments pay bounty only). The streams are placed so the
screen is never empty for more than 3 s before the quiet last seconds at the airlock
(`PacingTest`: one longer pause, 181.5–190 s, on every difficulty, with the balance plan's fit).

The Coilwyrm paths are authored per wave in the data (`[x, y]` points, y below the top edge): the
t=78 `swirl` from the upper right, looping once near the top and leaving through the bottom edge;
the t=102 `loop` round the right half and out of the bottom edge, its loop-back up through the
middle; the t=134 rear entry swirling up from the bottom left and out of the top; the t=168
`figure-8` through the screen's middle (its lobes crossing at y 290) and out of the bottom edge,
its loop-back up and out to the left; hard's t=146 pair on one diagonal, the second chain
mirrored, so they cross in the middle. The loop-backs' bottom-edge warnings start at t=112.2 and
t=180.6, the rear entry's at t=131.

## Ground targets

<!-- data: ground-targets -->
| Section | Target | Effect |
|---|---|---|
| 2 | [Spine Turret](../../../enemies/ground/spine-turret/README.md) ×2 between domes (t≈45) | Bounty; violet barrel glow only until lit |
| 2 | Abandoned ore cart on the rail (ground layer, 3 HP; t≈55), under a rail lamp | Medium salvage (50) |
| 3 | [Polyp Mortar](../../../enemies/ground/polyp-mortar/README.md) ×2 at the array (t≈70) | Bounty; revealed by the first flare |
| 3 | Turret nest: Spine Turret ×3 (t≈90) | Bounty |
| 3 | CDF survey cache in the dark crater (t≈109.5), three marker lights on its lid | Takes 3 hits, only while the headlight or the t=112 flare lights it; releases the hidden crate |
| 4 | Polyp Mortar ×2 in the Vrell field (t≈118) | Bounty |
| 4 | Spine Turret ×2 in the Vrell field (t≈118) | Bounty |
| 4 | Turret nest: Spine Turret ×3 (t≈140) | Bounty; inside the heavy peak |
| 5 | Polyp Mortar ×2 on the landing pad (t≈165) | Bounty |
| 5 | The open airlock's terminal at Daedalus Gate (t≈180), lit by the dome light | Takes 2 hits; releases the data core (the settlement log) |
<!-- /data -->

Totals: Spine Turret 10 · Polyp Mortar 6. The ore cart, survey cache and terminal keep the
sizes proposed in [round 23](../../../concept-rounds/round-23/README.md) (28×40, 32×24, 40×32) and
fly with the production sprites of its chosen concepts (the data's `sprite`): the ore cart a closed
hopper (b; intact, damaged, wrecked, a break-apart), the survey cache a ribbed CDF case (a; closed,
hit, opened) whose three markers glint only while the headlight or a flare lights it, the terminal
a wall cabinet (b; intact, released) whose LED rows and amber orb glow after the light pass, so
they stay lit in the dark; the released core is an amber orb pickup.

## Hazards

- **Darkness** (all sections; see *Darkness rules*).
- **No rear defence**: Coilwyrm loop-backs come from the bottom edge; the answer is to move
  out of the head's path. A Coilwyrm cut by side or spread fire regrows a head once (spec rule),
  so a careless cut doubles the threat.

## Secrets and pickups

Pickup types are defined in [player](../../../player/README.md#in-level-pickups).

- **Survey cache** (hidden crate, 135): a CDF survey cache in the dark crater at the end of
  section 3 (it enters at t≈109.5; the first draft's t≈95 had left the screen long before the t=112
  flare), invisible until the headlight or the t=112 flare lights it. Three marker lights on its lid; 3 hits open it.
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

<!-- data: radio -->
| Trigger | Speaker | Line |
|---|---|---|
| t=2 | Okafor | "Aegis, Daedalus Rim stopped answering thirty hours ago. Find out why." |
| t=12.5 | Rook | "Aegis Two on the north lane. I've got cousins on Daedalus, Lancer." |
| t=20 | Varga | "You're over the terminator. Headlight on. The Vrell glow; the ground doesn't. Trust the glow." |
| t=32.5 (the first Mantis's warning) | Rook | "Something on your left edge. It's just... sitting there." |
| t=40 | Varga | "It's a sniper. It holds at the edge and sweeps a beam across you. Get an angle on it, or get away from that side." |
| t=52.5 | Generic CDF (Perimeter beacon) | "...Daedalus perimeter. All residents report to shelter... all residents report..." |
| t=64 | Okafor | "Domes intact. Airlocks open. Nobody's home." |
| t=76 | Varga | "Long contact, moving like a snake. Cut it and the back half grows a new head. Go for the head." |
| t=112 (the t=102 loop-back's warning) | Rook | "It's turning, it's coming round behind you! Six, Lancer, six!" |
| t=119.5 | Varga | "You can't shoot behind you yet. Dodge it." |
| t=131 (the t=134 rear entry's warning) | Rook | "Contacts on six! Why is it always six?" |
| t=160 | Varga | "No bodies. No damage. Commander, the Vrell didn't kill them. They took them." |
| t=171 | Okafor | "...Understood. Log it, Varga. Aegis, finish this and come home." |
| t=180.5 (the t=168 loop-back's warning) | Rook | "Another one's coming round. Watch your tail." |
| Level end | Okafor | "Daedalus is empty. So are the others. We'll find them." |
| Level end, after Okafor's line | Rook | "...Copy. Aegis Two, heading home." |
| Secondary met | Okafor | "Not one sniper left standing. Good eyes in the dark, Lancer." |
<!-- /data -->

Retimed in the build step to the HUD's 1 s rule (`RadioTimelineTest`; a text line holds the
radio 6–12 s): Rook's "six" lines start with the bottom-edge warning of the rear strike they
call (t=112 for the t=102 loop-back, which re-enters at 115.2; t=131 for the t=134 rear entry;
t=180.5 for the t=168 figure-8's loop-back; `Level06Test` checks them within 0.5 s), Varga's
"Dodge it" follows Rook's first call at 119.5, and the opening lines are spread (Rook 12.5,
Varga's headlight line and the headlight 20, Rook's Mantis sighting 32.5 with its warning, Varga
40, the beacon 52.5, Okafor 64). The Varga/Okafor exchange moves before the finale (160 and 171),
so the last loop-back's call and the quiet airlock follow it. The level end plays Okafor's line,
then Rook's. The perimeter beacon is voiced by Mark F. Smith through the public-address filter
(user decision D7, cast in [round 23](../../../concept-rounds/round-23/README.md); see
[voice](../../../audio/voice/README.md)).

## Boss / mid-boss

None. The finale is the last Coilwyrm's figure-8 and loop-back with a Mantis pincer.

## Music & ambience

Track 5 *Act 1 B: Earth orbit & Luna* ("Coalition Rising", see the
[track list](../../../audio/music/README.md#track-list)): the base stem from section 1 (the far
side is sparse and tense), the full stem from section 4 (`full_section: 4`). Ambience: Luna
([sfx](../../../audio/sfx/README.md#ambience-per-setting)), with the perimeter beacon's
automated message looping faintly under section 2 (the same rendered line as its radio cue: the
music block's `voice_loop`, −18 dB on the voice bus). Ambience
only from t=186, the last 4 s, after the Varga/Okafor exchange (`ambience_from`: the theme fades
out over a second as at a won level).

## Credit budget

Budget(6) = 700 × 1.07⁵ ≈ **982**, the typical haul's target
([economy](../../../systems/economy/README.md#per-level-budget)), not a perfect collection; the
level's `bounty_scale` of 0.75 puts the typical haul on it (`TypicalHaulTest`, +0.2 %).
Bounties from the stat blocks: Mantis 30, Coilwyrm 86 per chain (head 40, 12 segments × 3,
tail 10, each paid and rounded on its own; +10 if the tail goes first, so a perfect dismantle pays
96, not counted here; a head-first kill pays the head only), Needler 12, Stinger 15, Skitter 5,
Spine Turret 12, Polyp Mortar 15. The data core pays nothing; it counts with the secrets for the
grade.

<!-- data: credit-budget -->
| Source | Perfect run | Typical haul |
|---|---|---|
| Kills: Skitter 122 × 5 + Needler 20 × 12 + Mantis 8 × 30 + Stinger 12 × 15 + Coilwyrm 4 × 86 | 1,224 | 734 |
| Ground targets: Spine Turret 10 × 12 + Polyp Mortar 6 × 15 + ore cart medium salvage 50 | 206 | 157 |
| Secret: survey cache (hidden crate, 14% of budget) | 135 | 68 |
| Data core: settlement log (unlocks the Targeting computer; no credits) | 0 | 0 |
| Secondary: no Mantis gets through | 50 | 25 |
| **Total** (bounty scale 0.75) | **1,615** | **984** |
| Budget(n) = the typical haul's target; typical +0 %, perfect 1.64 × budget | | 982 |
<!-- /data -->

## Difficulty notes

- **Easy**: Coilwyrm loop-backs are edge-warned 4 s ahead (instead of 3 s); flares last
  12 s; the headlight cone is 260 px long.
- **Hard**: Coilwyrms have 14 segments and the Mantis sweeps 90° (stat-block hooks); no flare
  at t=112; the t=146 Skitter snake is replaced by a new two-chain Coilwyrm wave, the chains
  crossing each other (*pairs crossing*; the t=134 chain has left by then; the data's `skip`
  leaves each of the two t=146 waves out on the other difficulties); the Mantis sweeps every 2.5 s.

## Concept art

The chosen scene is the [Luna far side](../../../art-direction/concept/scene-luna-farside-r09-a.png)
(see *Layout*). Production review files and the a/b concepts of the level's props (user decision
D8), with their briefs: [concept/prompts.md](concept/prompts.md); reviewed in
[round 23](../../../concept-rounds/round-23/README.md).

| File | What | Status |
|---|---|---|
| [concept/backdrop-final-r23-a.png](concept/backdrop-final-r23-a.png) | Final backdrop (`tools/art/backdrop_l06.py`): the level's tile sets and set pieces with composites | chosen |
| [concept/darkness-final-r23-a.png](concept/darkness-final-r23-a.png) | Final darkness art (`tools/art/l06_darkness.py`): the Spine Turret's (32 headings) and Polyp Mortar's (8 pulse frames) glow frames, the flare shell (4 frames), the flare pool (240×240) and headlight cone (232×200) as light-map shapes, and two views of the ground under the light map | chosen |
| [concept/darkness-final-r23-a.gif](concept/darkness-final-r23-a.gif) | The ship sweeping past turrets and mortars in the dark, the headlight and a falling flare lighting the ground, the glows over it | chosen |
| [concept/rejected/ore-cart-r23-a.png](concept/rejected/ore-cart-r23-a.png) | Ore cart A: open rust ore tub heaped with ore, hazard rim (`tools/concept/props_r23.py`) | rejected |
| [concept/ore-cart-r23-b.png](concept/ore-cart-r23-b.png) | Ore cart B: closed hopper, hazard-striped hatches, red beacon | chosen |
| [concept/survey-cache-r23-a.png](concept/survey-cache-r23-a.png) | Survey cache A: ribbed olive CDF case, three retro markers that glint only while lit | chosen |
| [concept/rejected/survey-cache-r23-b.png](concept/rejected/survey-cache-r23-b.png) | Survey cache B: half-buried drum, three dim amber blinkers (a hint in the dark) | rejected |
| [concept/rejected/data-core-r23-a.png](concept/rejected/data-core-r23-a.png) | Data core A: pedestal console with a teal screen, cyan cartridge core | rejected |
| [concept/data-core-r23-b.png](concept/data-core-r23-b.png) | Data core B: wall cabinet with LED rows, amber orb core | chosen |
| [concept/level-06-capture-final-r23-a.png](concept/level-06-capture-final-r23-a.png) | Game capture of the playable level (M4 part F step 3, whole window, 2 × 4, Level 06's own backdrop and the production Mantis, Coilwyrm and glows): a Mantis sweep past a lit dome; the first flare over the array mortars; the first Coilwyrm's swirl; the dark crater in the headlight; the rear and side warnings of the first loop-back and the pincer; the Smart Bomb's flash and ring; the finale pincer's crossed beams; the airlock at Daedalus Gate with the data core released | chosen |
| [concept/l06-props-final-r23-a.png](concept/l06-props-final-r23-a.png) | Final prop sprites from the chosen concepts (`tools/art/l06_props.py`, made at the round's close): the ore cart b (28×40: intact, damaged, wrecked; the 60×60 break-apart), the survey cache a (32×24: closed, hit, opened; the markers' additive glint, 4 frames), the data core terminal b (40×32: intact, released; its additive glow frame) and the data core pickup (26×26, 8-frame loop), then each on Level 06's tiles under the light map | chosen |
| [concept/l06-props-final-r23-a.gif](concept/l06-props-final-r23-a.gif) | The survey cache found by the headlight (its markers glinting) and shot open, the ore cart blown apart under its rail lamp, the terminal's core released | chosen |
| [concept/level-06-props-capture-final-r23-a.png](concept/level-06-props-capture-final-r23-a.png) | Game capture of the props (whole play field above, 3× crops below): the ore cart under its rail lamp beside a Mantis (t≈56); the survey cache in the dark crater, its markers glinting in the headlight (t≈111); the terminal at the open airlock just shot, its LEDs dead, the core spawning; the amber core rising over it (t≈181) | chosen |

## Implementation

- [x] Scroll timeline, sections, atmosphere intensity and parallax content as in *Layout*
      (Level 06's own backdrop, `BackdropSeamsTest`).
- [x] Darkness: a light map darkens only the ground layer and the ground units (dome and rail
      lights, flares, the headlight cone and shot light); Spine Turret and Polyp Mortar glow
      frames drawn after it (the production glow frames of `tools/art/l06_darkness.py`, indexed like the units' frames); air units fully
      lit; bullets, edge warnings and mortar markers never darkened (the level data's `darkness`
      block, M4 part F step 2).
- [x] Scripted flares at t=70, 112 and 150 with moving light pools.
- [x] Wave script matches the *Waves* table; the first Coilwyrm does not loop.
- [x] Every side and rear entry is edge-warned 3 s ahead (loop-backs 4 s on easy); Rook calls
      each rear strike at the start of its warning.
- [x] At least 40 enemies per minute of scroll at medium (`DensityTest`), from Level 06's own
      light enemies; a Coilwyrm counts once.
- [x] Survey cache revealed by light and hittable only while lit; data core at the airlock
      terminal, recorded in the save with its unlock.
- [x] Secondary objective counts Mantis kills before they exit.
- [x] Radio cues fire at their triggers, the perimeter beacon's spoken through the PA filter.
- [x] Music: the base stem from section 1, the full stem from section 4, ambience only from
      t=186; the beacon's line loops under section 2 (its voice file, PA filter).
- [x] Props: the ore cart, survey cache and terminal with the production sprites of round 23's
      chosen concepts (`sprite` in the data); the cache's marker glint only while lit, the
      terminal's glow frame after the light pass, the data core pickup sprite.
- [x] Typical haul at medium within ±5 % of budget(6) = 982 with the level's `bounty_scale`
      (`TypicalHaulTest`).
- [x] Easy/hard variations as in *Difficulty notes*.

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
- 2026-10-04: M4 part F step 3 made the level playable (main-agent brief): the level's
  `data.yaml` with 182 enemies at medium (59.0 a minute), `bounty_scale` 0.75 (typical haul 984 of
  982, perfect 1,615), the authored Coilwyrm paths, the flares, lights, survey cache and terminal.
  Main-agent choices: the survey cache moves to t≈109.5 so the t=112 flare can light it; the
  finale's figure-8 comes at t=168 so its loop-back strikes before the quiet end; the radio is
  retimed to the 1 s rule (the Varga/Okafor exchange before the finale); the headlight comes on at
  t=20 with Varga's line; a wave's `skip` leaves it out on a difficulty (hard's Coilwyrm pair
  replaces the t=146 Skitter snake); the music gains `ambience_from` and the beacon's
  `voice_loop`. The head-first kill's time bonus (score only, Coilwyrm spec) is left open: the docs
  give no number.
- 2026-10-04: M4 part F batch, art for round 23: the backdrop (`tools/art/backdrop_l06.py`; its
  block was proposed in a `backdrop-proposal.yaml`, since merged into the data with its lamp
  lights on the domes and rail lamps and the ore cart moved onto the rail at x 410), the darkness's glow frames, flare shell and light shapes
  (`tools/art/l06_darkness.py`), the Mantis and Coilwyrm production sprites, and a/b concepts for
  the ore cart, survey cache and data core terminal (D8). The light map darkens every ground
  pixel, the Vrell tendrils' teal and the terminator's sunlit rims included: they show only inside
  a light (open for the round).
- 2026-10-04: Level 06's own backdrop merged into the data (main-agent brief): its tile sets per
  section, its pieces and placements, its dome, rail-lamp, dish, pad and airlock lights as the
  darkness's static pools (the terminator's sunlit-rim pools left out: the light map tints every
  static pool warm, not sunlight white); the ore cart on the rail at [54.83, 410], the survey cache
  on the dark crater's floor at [108.84, 286] (the t=112 flare moves over it, x 286, y 380), the
  terminal in the main airlock's bay at [180.01, 240]. Capture `level-06-capture-final-r23-a`
  proposed for the round.
- 2026-10-05: [Round 23](../../../concept-rounds/round-23/README.md) closed (user): the backdrop
  and the darkness's glow frames, flare shell and light shapes approved as **final**; the capture,
  the part F numbers and the briefing images accepted (the images to be re-rendered for the
  Coilwyrm's 0.5 spacing and the beam from the Mantis's head); the props' concepts ore cart **b**
  (closed hopper), survey cache **a** (CDF case, the markers glint only while lit) and data core
  terminal **b** (wall cabinet, amber orb) chosen, the others moved to `concept/rejected/`. Their
  production sprites (`tools/art/l06_props.py`, review `l06-props-final-r23-a`) were made and wired
  in at the close, the concepts being accepted (as Level 05's props in round 21): the data's
  `sprite` names each prop's look, the triggers show their hit and spent frames instead of the
  beacon, the cache gets no loot sparkle (its markers' glint shows only in the headlight or a
  flare), the terminal's `-glow` frame is drawn after the light pass until it is spent; capture
  `level-06-props-capture-final-r23-a`. The perimeter beacon is cast (Mark F. Smith, PA filter), so
  its line is spoken and loops under section 2. The level's art is `final`: its backdrop,
  darkness and props are production art (the Mantis and Coilwyrm are their own parts).
