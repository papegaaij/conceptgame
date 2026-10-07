---
title: Level 08 – Neon Skyline
design: approved
implementation: done
art: final
depends-on: [../../../player/wingmen, ../../../enemies/ground/creeper]
updated: 2026-10-07
---

# Level 08 – Neon Skyline

## Summary

The Act 2 opener. At night over **Nova Lagos**, Lancer flies low over the harbour, the elevated
highways and the tower district while Vrell walkers crawl through the streets and over the low
roofs toward the Ikoyi shelters. It is Rook's first sortie on Lancer's wing in the escort slot.
The level introduces the [Creeper](../../../enemies/ground/creeper/README.md) and the megacity
parallax with its perspective towers and traffic lanes, and ends with a Creeper convoy on an
elevated highway under a Needler circle. There is no boss. About 3 minutes 20 seconds.

## Briefing

Two pages, as Act 1's levels have (user decision D3 of M5 part B). They follow the Act 2 title
card and the act briefing ([Act 2](../README.md#act-intro-and-outro)), which tell the landfall,
so they carry only tonight's mission:

<!-- data: briefing -->
> **Commander Okafor:** "Nova Lagos has fought since dawn, Lancer: forty million people, and the
> Vrell among them. Fly low: harbour, highways, towers, then the Third Mainland to the Ikoyi
> shelters. First sortie with Rook on your wing. Bring each other home."
>
> **Dr. Varga:** "The walkers are new. We call them Creepers. They crawl the streets and low
> roofs and fan their fire at you every few seconds. Anything that hits the ground hurts them
> twice as much. Every one you stop never reaches the shelters."
<!-- /data -->

*Images* (production art straight away, user decision D6 of M5 part B;
[briefing images](../../../ui/briefing/README.md)): Okafor's page `level-08-nova-lagos` (the
night route over the city: the harbour, the elevated highways, the tower district, the Third
Mainland highway and the Ikoyi shelters at its far end), Varga's `level-08-walker-scan` (a
Creeper on a low roof, its aimed five-way fan, a convoy's fans staggered 0.5 s apart, the
`anti-ground` ×2 marker); the level's data names them as each page's `image`.

*Hangar teaser* (shop screen before L08, where Rook is hired and the escort slot opens):

<!-- data: teaser -->
> **Rook:** "Escort slot's mine now, Lancer. Fit me a gun, and bring something that hits the
> ground."
<!-- /data -->

## Threat profile

<!-- data: threat-profile -->
| Field | Value |
|---|---|
| Dominant layers | `ground` (Creepers on the streets and low roofs, turret nests, mortars), `air`; `low-air` carries the traffic lanes (scenery, no enemies) |
| Attack directions | front 92% · sides 8% by enemy count (two Needler pincers and the finale's side stream) |
| Density | 3 |
| Recommended traits | `anti-ground`, `forward` |
| Hazards | none |
| Boss / mid-boss | none (finale: Creeper convoy plus Needler circle) |
| Sensor-suite detail | none: Earth megacity at night, `ground` + `air`, front · L1: + directions, density 3, no hazards · L2: + Skitter, Needler, Stinger, Creeper (new), Spine Turret and Polyp Mortar portraits; no boss · L3: + `anti-ground` highlighted, the wave strip marking the Creeper convoys at 84, 114, 128 and 162 s, 1 secret |
<!-- /data -->

*Varga's intel line* per sensor-suite level (the data's `varga`, as in Act 1's levels):

| Sensor suite | Line |
|---|---|
| none | "Nova Lagos at night, Lancer. Walkers in the streets, flyers over the towers. Lots of both." |
| L1 | "Nearly all from ahead, two pincers from the flanks, nothing behind you. Smoke late on, but it stays below you." |
| L2 | "New one: the Creeper, a walker. It fans five shots at you every three seconds. Turrets and mortars sit on the low roofs." |
| L3 | "Anti-ground hits Creepers twice as hard. One cache: under the flickering billboard. Three hits bring it down." |

## Objective

- **Primary** `reach-end`: survive to the end of the scroll.
- **Secondary** *No Creeper gets through*: destroy every Creeper (15 at medium, 14 on easy, 17 on
  hard) before it leaves the screen (`escapes: creeper`; failed at the first Creeper that leaves
  alive; they are heading for the Ikoyi shelters). Pays +90 credits (56 in Act 1 terms × the act
  factor 1.6) and triggers the shelter's line.

## Layout

Ground scroll speed **140 px/s** (a calm-to-normal city pace, as in the chosen megacity scene)
at the 960×540 baseline (play field 480×540, see [art direction](../../../art-direction/README.md)).
Total ≈ 200 s ≈ 28,000 px; the launch takes the first 5 s, as in Act 1. Motion budget: the
traffic lanes (sections 1–3) and the smoke columns (sections 4–5) are the strongly animated
elements, and the traffic thins out as the smoke rises, so the two never run at full strength
together.

**Towers and roofs** (user decision D1 = a of M5 part B, see
[art direction](../../../art-direction/README.md)): the tower district's high towers are drawn in
true perspective as **scenery only**: their roofs move faster than the ground and their walls turn
as they scroll, and nothing the game plays with stands on them. Everything gameplay touches sits at
**street level or on low structures drawn without lean**: the avenues, parking decks, landing pads
and low roofs. The turret nests stand on a landing pad, a low roof and the parking deck; the
Creepers crawl along the avenues and the low roofs and down ramps between them, never up or down a
tower wall; the billboard and its crate sit on a low roof.

**Traffic** (user decision D2 = a): the civilian aircars and the CDF gunships on `low-air` are
scenery: no collision, and shots pass through them, so the player never has to hold fire.

<!-- data: level-sections -->
| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. On the Wing | 0–10 | 0–1,400 | 140 | light | `deep`: the night sky's glow on the Gulf of Guinea, seen in the harbour's water. `ground`: Lagos harbour, the water and its piers, a container ship at the pier, gantry cranes on the quay, container yards; the first elevated cross highway over the seam. `low-air`: amber-lit fog banks, CDF gunships heading home. `high-air`: thin mist. | The launch with Rook in his slot from the start (he launches in formation and fires when the player fires); the HUD prompt `ROOK SIDE` · `IN THE HANGAR` at t=6 for 6 s. No enemies until the first Skitters at t=12. |
| 2. Traffic Lanes | 10–65 | 1,400–9,100 | 140 | clear | `ground`: the harbour's last container yards, then two tree-lined avenues as lanes, low roofs, parks, the landing pad on a low roof; the first perspective towers rising along the avenues. `low-air`: streams of civilian aircars fleeing south (scenery; no searchlights, as the streams on the two avenues fill the motion budget), a CDF gunship pair. `high-air`: thin mist. | Rook shoots what the player shoots; his first kill; the first turret nest. |
| 3. Rooftop Crawl | 65–115 | 9,100–16,100 | 140 | light | `ground`: the boulevard and side streets with their lamp lines, blocks of low roofs, rooftop gardens, water tanks, the ramps and ledges the Creepers follow; perspective towers rising between them. `low-air`: thin fog banks lit amber by the avenue lamps; traffic thinning. | Creeper introduction: one alone, then convoys. The billboard secret. |
| 4. Smoke District | 115–160 | 16,100–22,400 | 140 | medium | `ground`: burnt and burning blocks (the fire glow is on them and on the smoke's underside), a collapsed overpass, a parking deck. `low-air`: dense smoke columns (below the play plane). `high-air`: ash wisps (≤ 40% opacity). | Mixed waves, Stinger dives, mortars on the overpass. |
| 5. Neon Heights | 160–200 | 22,400–28,000 | 140 | medium, heavy peak 166–174 | `ground`: neon-lit low roofs and towers, neon signage under smoke (no saturated teal), the elevated Third Mainland highway carrying the finale convoy; at the level's end the Ndidi Arcology, a tower with creep on its roof and upper floors (the lead into L09). `low-air`: smoke banks. | Finale set piece, then the level ends over the arcology district. |
<!-- /data -->

The backdrop is the level's own (`tools/art/backdrop_l08.py`, merged into the data on
2026-10-06): the night sky's glow on the Gulf as the only deep layer, seen in the harbour's water;
four ground tile sets (the harbour, the avenues, the rooftops for sections 3–4, the Third Mainland
highway) with an elevated cross highway over each seam where they change; the towers as ground
pieces drawn in perspective, the Ndidi Arcology the tallest of them at the level's end; the
turret nests' landing pad, roof and parking deck, the billboard's roof, the collapsed overpass and
the Creepers' walk roofs placed from the targets and paths; fog, smoke banks and smoke columns,
the CDF gunships and the streams of civilian sedans and vans (round 30's variant A, 16 headings;
one paint per piece, so the van lane carries sedans while the CDF pair crosses section 2) on
`low-air`; mist and ash on `high-air`. Nothing is on `far`. Three atlas pages with the level's
units (backdrop 2048² and 2048×1024).

## Waves

Enemy definitions are linked per row; formation names come from the
[formation vocabulary](../../../enemies/README.md#formation-vocabulary). All of the level's
returning Act 1 units are `tiny` or `small`, so none gets the act HP factor from the
[balancing basis](../../../enemies/README.md#balancing-basis) (it applies only to returning
`medium` and larger units, D5 = c of M5 part A); HP values live in the enemy specs only.

This table is the design the level data is authored from (M5 part B, step B4); once the data
exists, it is generated from it like Act 1's.

<!-- data: waves -->
| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 12 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | Over the harbour cranes; the first targets for Rook |
| 16 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | |
| 20 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | Over the container yards |
| 23 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | Curls between the avenues |
| 27 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | Over the aircar lanes |
| 32 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 35.5 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | Mirror of the t=23 snake |
| 39.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | Over the first turret nest |
| 46 | 2 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | |
| 49.5 | 2 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | Hover over the highway, fire, leave |
| 52.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 58.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (alternating edges) | |
| 62.5 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | Into the tower district |
| 66 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | Away from the Creeper's side |
| 68 | 3 | single | [Creeper](../../../enemies/ground/creeper/README.md) | 1 | front | **New.** Alone on a low roof, down a ramp to the avenue; Varga's line at t=66 |
| 72 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 76 | 3 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | |
| 79.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 84 | 3 | convoy | [Creeper](../../../enemies/ground/creeper/README.md) | 3 | front | Along the low roofs and down to the avenue, fans 0.5 s apart; Varga's anti-ground line; easy 2, hard 4 |
| 88 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | Over the second turret nest |
| 94 | 3 | pincer | [Needler](../../../enemies/air/needler/README.md) | 4 | sides | 2 per side, hold 4 s; Rook's flank bark |
| 94 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 4 | front | Easy only: the pincer from the front |
| 98 | 3 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | Past the billboard |
| 101.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 107 | 3 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | Dive one after another |
| 110.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | Ahead of the walkers shelter nine reports |
| 114 | 3 | convoy | [Creeper](../../../enemies/ground/creeper/README.md) | 3 | front | **Added** (D5): the walkers shelter nine reports, over the low roofs |
| 117.5 | 4 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | Into the smoke |
| 121 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | Over the parking-deck nest |
| 128 | 4 | convoy | [Creeper](../../../enemies/ground/creeper/README.md) | 3 | front | Through the burning blocks |
| 131.5 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 134.5 | 4 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | |
| 138 | 4 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | Weaves through the smoke columns |
| 142 | 4 | pincer | [Needler](../../../enemies/air/needler/README.md) | 4 | sides | 2 per side; Rook's flank bark |
| 142 | 4 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 4 | front | Easy only: the pincer from the front |
| 145.5 | 4 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | The last one drops the armour patch |
| 149 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | Over the overpass mortars |
| 156 | 4 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | |
| 159.5 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | Over the Third Mainland ahead of the convoy |
| 162 | 5 | convoy | [Creeper](../../../enemies/ground/creeper/README.md) | 5 | front | On the elevated highway; the first one drops an overdrive; hard 6 |
| 165 | 5 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | Into the smoke wall |
| 170 | 5 | circle | [Needler](../../../enemies/air/needler/README.md) | 6 | front | Orbits above the highway for 6 s, then breaks toward the player one by one (hard: in pairs) |
| 174 | 5 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 6 | sides | Enters while the circle orbits |
| 181 | 5 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | Past the arcology district; the last wave |

Totals: Skitter 108 · Needler 42 · Stinger 12 · Creeper 15.
<!-- /data -->

**Density** (user decision D5 = a of M5 part B): the first draft had 80 enemies at medium (72 in
waves, 8 ground units) over 195 s of scroll, 24.6 a minute, against Act 2's
[minimum](../../README.md#difficulty-curve) of **50**, which needs at least 163. The level reaches
it with **more and larger waves of its own units** (Skitter streams and snakes, Needler and
Stinger groups, a third turret nest and a third mortar) between and around the Creeper beats,
plus a fourth Creeper convoy (12 → 15 Creepers): **189 enemies at medium** (air 162, ground 27:
the 15 Creepers, 9 Spine Turrets and 3 Polyp Mortars), **58.2 a minute** over the 195 s after the
launch (`DensityTest`). Per section: 1 none (the opener) · 2 60 · 3 52 · 4 50 · 5 27. The design
had 181; the data step added two Skitter streams (t=110.5 and t=159.5) where the pacing check
found gaps.

The opener is about 10 s (was 20): the launch with Rook in his slot, then the first Skitters at
t=12. **Pacing** (`PacingTest`): the screen stays empty for longer than 3 s only twice, the opener
(the launch's end to the first Skitters, ≈ 7 s) and the quiet end over the arcology district
after the last snake (≈ 183–200 s). Between t=12 and t=181 no two waves start more than 7 s apart
and the turret nests, mortars and Creepers hold the screen in the longer steps. The autopilot
(the balance plan's fit with Rook on its wing) clears the waves quickly, so the data step moved
several waves up by 1–2.5 s (t=27, 35.5, 39.5, 52.5, 76, 79.5, 101.5, 107, 145.5, 149, 174) and
added two Skitter streams (t=110.5 and t=159.5) until no other gap passed 3 s on any difficulty.
On easy the two pincers are a line abreast from the front (their waves at t=94 and t=142 are
listed twice, one row for easy). A convoy's Creepers enter 1.5 s apart on one path; walking north
at 35 px/s against the 140 px/s scroll they move down the screen at 105 px/s, about 160 px apart,
with clear gaps between the 60 px sprites.

## Ground targets

<!-- data: ground-targets -->
| Section | Target | Effect |
|---|---|---|
| 2 | [Spine Turret](../../../enemies/ground/spine-turret/README.md) turret nest ×3 on a landing pad on a low roof (t≈44) | Bounty; first ground threat of the act |
| 3 | Spine Turret turret nest ×3 on a low roof by the avenue (t≈88) | Bounty |
| 3 | Flickering billboard ("LAGOS NEVER SLEEPS") on a low roof (t≈100) | Takes 3 hits and topples; releases the hidden crate (secret, see below) |
| 4 | Spine Turret turret nest ×3 on the parking deck (t≈124) | Bounty |
| 4 | [Polyp Mortar](../../../enemies/ground/polyp-mortar/README.md) ×3 on the collapsed overpass (t≈150) | Bounty; their impact markers teach "watch the ground" again |
| 4 | Spine Turrets between the overpass mortars (t≈150), none on easy and medium; hard ×2 | Bounty |
<!-- /data -->

Totals: Spine Turret 9 · Polyp Mortar 3 (hard: +2 Spine Turrets at the overpass, a
ground-target group with no units on easy and medium). The civilian
traffic on `low-air` is scenery and cannot be hit.

## Hazards

None. Smoke banks in sections 4–5 sit on `low-air`, below the play plane, and never hide
bullets.

## Secrets and pickups

- **Billboard cache** (t≈100): three hits topple the flickering billboard and uncover a CDF
  supply crate worth **160** credits (100 in Act 1 terms × the act factor 1.6). Rook:
  "*Billboard's down, and there's a CDF crate under it. Grab it.*" The sign is round 30's neon
  sky-sign (a): its red LAGOS and white NEVER SLEEPS flicker as an additive glow loop, the first
  hit scorches it and kills a tube and a floodlight, and the third topples it like a destructible
  breaking: the small explosion on the roof, the small blast and the large crumble.
- **Overdrive**: dropped by the first Creeper of the t=162 convoy.
- **Armour patch**: dropped by the last Stinger of the t=145.5 column.
- Shield cells follow the normal drop table.

## Radio chatter

Spoken radio ([voice](../../../audio/voice/README.md)), like every line since Act 1; the
2026-10-01 "text and radio blips only" is reversed. New speaker: the **Civilian (shelter nine)**,
cast in concept round 30 (b, Faith Abiola-Ellison, a woman's voice; user decision D6); her two
lines are voiced.
Rook's generic barks (boss, rear, flank, low armour, kills, overdrive) follow
[his bark table](../../../player/wingmen/README.md#radio-barks): the two Needler pincers (t=94,
t=142) set off his flank bark, so the draft's scripted "Contacts on your flank!" at t=92 is
dropped. His two own lines need the mechanics of user decision D4 = a: his `{side}` line (two
voiced takes, *left* and *right*, played by his side in the save or `--escort side=`; the subtitle
shows the chosen text) and his first-kill line on the `escort-first-kill` event (the first kill by
his shots while he flies, once per attempt, never after he ejects). The billboard's line is the
secret's (see *Secrets and pickups*). The Civilian speaks with the generic civilian radio portrait.

<!-- data: radio -->
| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "Aegis, contacts inbound over the harbour. Weapons free." |
| t=8.5 | Rook | "Copy. Lancer, I'm on your {side}. Don't make me look bad." |
| Rook's first kill (`escort-first-kill`) | Rook | "Splash one. I'm keeping count, by the way." |
| t=26 | Okafor | "The traffic below you is civilians getting out. Keep firing." |
| t=43 | Rook | "Turrets on that landing pad. Mind the thorns." |
| t=66 | Varga | "Walker on the low roofs. It sprays wide. Hit it hard." |
| t=84 | Varga | "Bombs and mortars hit those walkers twice as hard." |
| t=113 | Generic civilian (Civilian) | "Shelter nine, Ikoyi! Walkers on the roofs, heading our way!" |
| t=120.5 | Okafor | "We hear you, shelter nine. Lancer, clear those roofs." |
| t=157 | Okafor | "Convoy on the Third Mainland. Keep it off the shelters." |
| t=165 | The Choir (distorted) | "[the Choir sings]" |
| t=171 | Varga | "That's the song from orbit. It's louder down here." |
| Level end | Okafor | "Good flying, both of you. Get some rest. The arcologies are next." |
| Secondary met | Generic civilian (Civilian) | "Shelter nine here. The roofs are quiet. Thank you, Aegis." |
<!-- /data -->

**Timing** (the HUD's 1 s rule, `RadioTimelineTest`): a line holds the radio for the longer of
its voice and its text (5 s for the last page plus 3 s for each page before it, typed at 30
characters a second), then 0.4 s of silence. Every timed line above is one subtitle page, about
7 s on the radio, and starts after the one before it has closed. The draft's three clashes are
retimed: Okafor's opener and Rook's side line (t=1 and t=3) are now t=1 and t=8.5; the Civilian
and Okafor (t=112 and t=113) now t=113 and t=120.5; the Choir and Varga (t=166 and t=167, with the
Choir's 3.9 s stage sound and its 5.6 s subtitle) now t=165 and t=171, after Okafor's convoy line
at t=157. The event lines get their gaps: 15.8–26 s and 33.4–43 s for Rook's first kill (the first
Skitters enter at t=12), 91–113 s for the billboard's line (it fires when the third hit topples the billboard, at
≈ t=100–105). The
radio test flies the level with Rook on both sides, with his barks and the first-kill event; the
Civilian's lines are voiced since round 30 cast her (4.8 s each; the t=113 call closes well before
Okafor's answer at t=120.5).

## Boss / mid-boss

None. The Creeper convoy with the Needler circle (t=162–181) is the finale.

## Music & ambience

The Act 2 A theme "Homefront" (track 6 in the [track list](../../../audio/music/README.md#track-list)):
section 1 has the [megacity ambience](../../../audio/sfx/README.md#ambience-per-setting) only
(distant sirens), the base stem from section 2 (`start_section: 2`) and the full mix from
section 5 (`full_section: 5`); the
mission-complete jingle at the end.

## Credit budget

Budget(8) = 700 × 1.07⁷ ≈ **1,124**, the typical haul's target
([economy](../../../systems/economy/README.md#per-level-budget)), not a perfect collection: the
typical player takes 60 % of the air kills, 80 % of the ground targets (the Creepers among them)
and half the secrets and secondary objectives. The level's `bounty_scale` puts the typical haul on
it within ±5 % (`TypicalHaulTest`): **0.56**, the typical haul 1,105 (−1.7 %), a perfect run
1,738 (1.55 × budget). The table lists the stat-block bounties (Act 1 terms); each is paid × the
act factor 1.6 × 0.56, rounded per kill: Skitter 4, Needler 11, Stinger 13, Creeper 20, Spine
Turret 11, Polyp Mortar 13. (0.57 rounds the Skitter up to 5 and puts the typical haul at +5.0 %.)
Rook's kills pay like the player's and do not move the typical haul (user decision D2 of M5 part
A).

<!-- data: credit-budget -->
| Source | Perfect run | Typical haul |
|---|---|---|
| Kills: Skitter 108 × 5 + Needler 42 × 12 + Stinger 12 × 15 + Creeper 15 × 22 | 1,350 | 870 |
| Ground targets: Spine Turret 9 × 12 + Polyp Mortar 3 × 15 | 138 | 110 |
| Secret: billboard cache (hidden crate, 9% of budget) | 160 | 80 |
| Secondary: no Creeper gets through | 90 | 45 |
| **Total** (bounty scale 0.56) | **1,738** | **1,105** |
| Budget(n) = the typical haul's target; typical -2 %, perfect 1.55 × budget | | 1,124 |
<!-- /data -->

## Difficulty notes

- **Easy**: both Needler pincers (t=94, t=142) enter from the front instead (as a line abreast); the t=84 convoy has
  2 Creepers; one extra armour patch at t≈130.
- **Hard**: the t=84 and t=162 convoys have one more Creeper each; the Creeper fans seven ways
  every 2.6 s (its stat-block hooks); the Needler circle breaks toward the player in pairs; the
  overpass mortars get a pair of Spine Turrets.

## Concept art

The look comes from the chosen megacity scene in
[art direction](../../../art-direction/README.md):
[parallax-r03-b.png](../../../art-direction/concept/parallax-r03-b.png) (shown at the `heavy`
end of the atmosphere range; this level runs mostly `clear` to `medium`). Part B (user decision
D6 = a) takes the megacity, extended to the harbour, highways, smoke district and arcology
sections in the same kit, and the briefing images straight to production, and proposes a/b
concepts only for the billboard and the traffic (aircars, CDF gunships), all reviewed in round 30.
[Round 30](../../../concept-rounds/round-30/README.md) closed on 2026-10-07: the backdrop and the
captures approved as final, the billboard **a** (the neon sky-sign) and the traffic **a** (the
wedge cars and the CDF gunship) chosen and produced (`tools/art/billboard.py`,
`tools/art/l08_traffic.py`), their b variants in `concept/rejected/`.

Concept round 30 (prompts: [concept/prompts.md](concept/prompts.md); concept generator
`tools/concept/props_r30.py`):

| File | What | Status |
|---|---|---|
| [concept/billboard-r30-a.png](concept/billboard-r30-a.png) | Billboard A "neon sky-sign": a dark panel tilted up on a steel A-frame, red neon LAGOS over cool-white NEVER SLEEPS, floodlights, an amber/black hazard catwalk; intact with its flicker, hit, toppled with the floor stash open; 76×52 sprite, 64×36 hit box; 1× over the megacity with the ship (sheet) | chosen |
| [concept/billboard-r30-a.gif](concept/billboard-r30-a.gif) | Billboard A: flicker, three hits, topple and the crate drifting free (motion) | chosen |
| [concept/rejected/billboard-r30-b.png](concept/rejected/billboard-r30-b.png) | Billboard B "LED screen": a bezelled video screen with a sun hood on a hazard-striped plinth with a CDF cabinet, beige pixel text on a rust screen; glitch-band flicker, cracked and pixel-dead when hit, toppled face down with the cabinet open; 76×52 sprite, 64×36 hit box (sheet) | rejected |
| [concept/rejected/billboard-r30-b.gif](concept/rejected/billboard-r30-b.gif) | Billboard B: flicker, three hits, topple and the crate drifting free (motion) | rejected |
| [concept/billboard-final-r30-a.png](concept/billboard-final-r30-a.png), [.gif](concept/billboard-final-r30-a.gif) | Final billboard sprites from the chosen a (`tools/art/billboard.py`): the neon sky-sign 76×52 intact, hit (scorched, the last S, "PS" and a floodlight dead), toppled (the panel down on its snapped frame, the CDF floor stash open), unlit glass and no baked shadow; its additive neon glow as a 32-frame loop at 8 fps (the concept's flicker steps, intact then after the first hit); in the level over the megacity backdrop, the GIF from its approach through three hits to the crate drifting free | chosen |
| [concept/traffic-r30-a.png](concept/traffic-r30-a.png) | Traffic A "wedge cars": wedge sedan 12×20 and box van 14×24 on corner lift ducts, CDF gunship 32×40 with twin ducted fans; muted scenery on low-air, 16 headings, in streams over the avenues with the ship's shots passing over (sheet) | chosen |
| [concept/traffic-r30-a.gif](concept/traffic-r30-a.gif) | Traffic A: the streams, the CDF pair and the vulcan stream over them (motion) | chosen |
| [concept/rejected/traffic-r30-b.png](concept/rejected/traffic-r30-b.png) | Traffic B "pods": teardrop sedan 12×20, evacuee minibus 14×26 with a lit window band, CDF armoured hauler 30×48 on four tilt ducts; muted scenery on low-air, 16 headings (sheet) | rejected |
| [concept/rejected/traffic-r30-b.gif](concept/rejected/traffic-r30-b.gif) | Traffic B: the streams, the CDF pair and the vulcan stream over them (motion) | rejected |
| [concept/traffic-final-r30-a.png](concept/traffic-final-r30-a.png) | The traffic as production art (`tools/art/backdrop_l08.py` with `l08_traffic.py`, from variant A's models): wedge sedan 24×24 (slate), box van 28×28 (beige), CDF gunship 44×44, 16 headings each, lamp halos baked in; composites of the streams as the game draws them, a 2× crop (sheet) | chosen |
| [concept/traffic-final-r30-a.gif](concept/traffic-final-r30-a.gif) | The traffic, final: the CDF pair (heading 9) crossing the avenue streams, t 36.4–40.4 (motion) | chosen |
| [concept/backdrop-final-r30-a.png](concept/backdrop-final-r30-a.png) | The megacity backdrop as production art (`tools/art/backdrop_l08.py`, straight to production, D6 = a; its block merged into the level's data): the five sections' tile sets (harbour, avenues, rooftops for sections 3–4, the Third Mainland highway), the banks and wisps, the pieces (cross highways over the seams, cranes, ship, yard, the targets' landing pad, roof nest, billboard roof, parking deck and collapsed overpass, the Creepers' walk roofs, burnt blocks, fire, neon signs, smoke columns), the tower roofs and wall textures, the Ndidi Arcology, the traffic (sedan, van, CDF gunship); 18 composites as the game draws them, towers projected (sheet) | chosen |
| [concept/level-08-capture-final-r30-a.png](concept/level-08-capture-final-r30-a.png) | Capture of the game, not generated art: Level 08 flown with Rook (autocannon L2) at medium, labelled with level time (the harbour, Rook's side and first-kill lines, the towers per district, a Creeper convoy's fans and deaths, the three turret nests, the overpass mortars, the billboard secret and its crate, the smoke district, the Choir at t 165 and Varga's reply, the arcology, the debrief), then the Act 1 → Act 2 transition (Level 07's win, the act summary, the four outro pages, the Act 2 title card on its still, the eight briefing pages, the hangar with Rook's teaser, the launch) (sheet) | chosen |
| [concept/level-08-capture-final-r30-a.mp4](concept/level-08-capture-final-r30-a.mp4) | The same capture as video with the game's sound: the transition (outro page 4, title card, briefing pages, hangar, launch), then nine Level 08 stretches from the opener to the arcology (142 s) | chosen |
| [concept/level-08-readability-r30-b.png](concept/level-08-readability-r30-b.png) | Readability pass after the capture (game frames, before above after, same run options and level times): the landing-pad and parking-deck turrets on pale painted spots, the lifted Creeper with its light rim and back pores in the smoke district, the smoke wall parted over the Third Mainland convoy and its husks; each with its silhouette contrast | chosen |

## Implementation

- [x] Scroll timeline, sections, atmosphere intensity and parallax content per layer as in
      *Layout*: the towers in true perspective as scenery, everything gameplay touches at street
      level or on low structures drawn without lean (D1) (Level 08's own backdrop;
      `BackdropSeamsTest` checks its piece edges and the ground across the tile set changes).
- [x] Civilian traffic lanes on `low-air` as scenery: no collision, shots pass through (D2);
      round 30's wedge cars and CDF gunships (`tools/art/l08_traffic.py`).
- [x] Rook in formation from the launch (hired at the hangar visit before L08); the HUD prompt
      `ROOK SIDE` · `IN THE HANGAR` at t=6.
- [x] Wave script matches the *Waves* table (time, formation, count, entry edge); the Creeper
      convoys fan 0.5 s apart.
- [x] At least 50 enemies per minute of scroll at medium (`DensityTest`), from the level's own
      units; no empty screen over 3 s but the opener and the quiet end (`PacingTest`, every
      difficulty).
- [x] Ground targets and the billboard secret.
- [x] Radio voiced, every timed line at most 1 s late (`RadioTimelineTest`, flown with Rook on
      both sides, his barks and the first-kill event); Rook's `{side}` line, his first-kill line
      on the `escort-first-kill` event (never after he ejects); the Civilian voiced (cast in round 30).
- [x] Threat profile with Varga's four intel lines and Rook's teaser fitting the intel panel
      (`IntelPanelLayoutTest`), the Creeper's intel portrait (`PortraitsTest`).
- [x] Briefing: Okafor's and Varga's pages with their images, after Act 2's title card and act
      briefing (the act's first level).
- [x] Secondary objective (every Creeper) tracked and rewarded.
- [x] Typical haul at medium within ±5 % of budget(8) = 1,124 with the level's `bounty_scale`
      (`TypicalHaulTest`).
- [x] Music: ambience only in section 1, the "Homefront" base stem from section 2, the full mix
      from section 5.
- [x] Easy/hard variations as in *Difficulty notes*.

## Open questions

- None open.

## Decisions

- 2026-10-01: Promoted from the Act 2 roster to a draft level document.
- 2026-10-01: Rook joins as the player's wingman in the escort slot at L08 (user decision).
- 2026-10-01: Radio chatter is text plus radio blips only, no voice acting (user decision).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-06: M5 part B (user decisions of 2026-10-06): **D1 = a** the towers in true perspective
  are scenery; turret nests, Creepers, the billboard and its crate sit at street level or on low
  structures drawn without lean (landing pads, low roofs, the parking deck), and the Creepers no
  longer crawl down walls; rejected: gameplay on the tower roofs (a large sim change that carries
  into L09 and L14) and orthographic towers with a baked lean (drops the chosen scene's turning
  walls). **D2 = a** the traffic is scenery (the open question closed); a hittable traffic with a
  penalty would give a reason to hold fire, which the level does not want. **D3 = a** the
  briefing is rewritten to two pages (Okafor's mission, Varga's walker scan, each with its image)
  and no longer repeats the act briefing and Act 1's outro (the second fleet, the landfall,
  Rook's assignment); "overnight" against the act briefing's "forty hours ago" is resolved with
  the act briefing (landfall at dawn, this level that night); the t=1 line no longer assigns Rook
  again. **D4 = a** Rook's first-kill line fires on a new `escort-first-kill` event, his side line
  is a `{side}` line with two voiced takes. **D5 = a** the opener is shortened to about 10 s and
  the level densified with its own units to 181 enemies at medium (55.7 a minute), with a fourth
  Creeper convoy (12 → 15; the secondary now asks for every Creeper). **D6 = a** art straight to
  production but the billboard, the traffic and the Civilian's voice (a/b).
- 2026-10-06: Defaults of the part B plan, accepted by the user: Rook launches in formation (no
  scripted glide-in); his side prompt at t=6; the scripted t=92 flank line is dropped (the pincer
  sets off his flank bark); the radio is voiced (the 2026-10-01 "text only" is reversed, as for
  Act 1) and retimed to the 1 s rule (t=1/3, 112/113, 166/167; see *Radio chatter*); the teaser
  gets a speaker (Rook, who is hired at that hangar visit) and Varga four intel lines, one per
  sensor-suite level; no act HP factor on the level's returning units (all `tiny` or `small`).
  The billboard's line is reworded (Rook's "Finders keepers" was Level 07's), the t=158
  convoy line shortened to one subtitle page (now t=157), and a t=26 Okafor line tells the player
  the traffic is safe to fire over.
- 2026-10-06: Credit budget: the 1,606 perfect-run target was stale; the target is the typical
  haul on budget(8) = 1,124 (±5 %) through the level's `bounty_scale` (≈ 0.57 by the estimate,
  set by the data step). The data keeps Act 1 terms: the crate 100 (pays 160), the secondary 56
  (pays 90). The rewritten texts go to `review` for the user (round 30).
- 2026-10-06: Level data (M5 part B, step B4): the waves as designed, with the pacing check's
  retimes (eleven waves 1–2.5 s earlier) and two added Skitter streams (t=110.5, t=159.5): 189
  enemies at medium, 58.2 a minute; easy's pincers are a line abreast from the front (a pincer
  always enters from the sides). `bounty_scale` 0.56 (typical 1,105, −1.7 %; 0.57 rounds the
  Skitter's bounty up and lands at +5.0 %). The prompt reads `ROOK SIDE` (`ROOK'S SIDE` is 6 px too
  wide for the prompt line). The backdrop borrows Level 01's images until the megacity backdrop.
- 2026-10-06: Level 08's own backdrop merged into the data (`tools/art/backdrop_l08.py`, step B6,
  proposed for round 30; `backdrop-proposal.yaml` deleted): the sections' tiles `gulf` plus
  `harbour`, `avenues`, `rooftops` (sections 3–4) and `third-mainland`; the layer notes reworded for
  a top-down city: `far` is not used (the cranes stand on the quay, the avenue lamps are on the
  ground tiles, the Ndidi Arcology is a ground tower of height 1.5 at the level's end), section 4's
  fire glow is on the burnt blocks and the smoke's underside instead of `deep`, and there are no
  searchlights (the aircar streams fill the motion budget). Level 08 packs into 3 of its 6 atlas
  pages.
- 2026-10-07: Fixes from the round 30 capture. Both briefing pages shortened to fit one screen each
  (Okafor's spilled three lines onto a second screen, Varga's its last word): the same content
  (Nova Lagos and its forty million, the route harbour – highways – towers – Third Mainland, the
  Ikoyi shelters, the first sortie with Rook, "Bring each other home"; the Creepers, their fan,
  anti-ground ×2, every one stopped never reaches the shelters) without the call signs and the
  route's connecting phrases; both voices re-rendered. Rook no longer stays beneath the ship after
  t≈141 ([wingmen](../../../player/wingmen/README.md#decisions), 2026-10-07); the Act 1 outro's
  last page names Act 2's title card and this briefing as next instead of the hangar, and the
  hangar teaser shows Rook as `LT. K. TANAKA / AEGIS TWO`
  ([briefing](../../../ui/briefing/README.md#decisions), [hangar](../../../ui/hangar/README.md#decisions)).
- 2026-10-07: Readability pass after the round-30 capture (art direction: low-air banks leave the
  lanes readable, backgrounds recede, enemies carry a bright accent): the smoke banks part over the
  Creepers, their husks and the ground targets at every moment they are drawn (the heavy wall over
  the Third Mainland convoy fully, from 59 % to about 35 % coverage, the art direction's heavy end;
  the medium smoke thinned there, about 19 % coverage), the t 130.5 smoke plume moved off the t 128
  Creepers' path (x 333 → 93, the backdrop block's only placement change), the landing pad pale
  and every turret of the three nests on a pale painted spot with a ring and lamps; the Creeper
  lifted, with a light rim and violet back pores
  ([creeper](../../../enemies/ground/creeper/README.md#decisions)). Silhouette contrast in the
  game's frames: turrets about 1.3–1.7 → 2.3–3.1, Creepers about 1.2–2.2 → 1.8–5.2
  ([concept/level-08-readability-r30-b.png](concept/level-08-readability-r30-b.png), numbers in
  [concept/prompts.md](concept/prompts.md#level-08-readability-r30-b)).
- 2026-10-07: Traffic variant A in production: one paint per piece (the motion budget counts each
  moving piece id), so the van lane carries sedans while the CDF pair crosses section 2 and section
  3 alternates sedans and vans; gunships at headings 8–10; no baked shadow (a low-air piece casts
  none in the game and would darken the ground units).
- 2026-10-07: Billboard variant A in production (`tools/art/billboard.py`): `billboard_0..2`
  (76×52; intact, hit, toppled; the data's `sprite: billboard`, its size staying 72×40) and the
  additive `billboard-glow_0..31` (8 fps, the first half before the first hit, the second after it,
  looped by the game's `GroundGlow`); no baked shadow (art direction rule 6). Its topple takes the
  default, a destructible's break: the small explosion on the roof, the small blast and the large
  crumble ([architecture](../../../tech/architecture/README.md#decisions), 2026-10-07).
- 2026-10-07: Concept round 30 closed (user, 2026-10-07): the backdrop with its perspective towers
  and the readability pass approved as **final**; the billboard **a** (the neon sky-sign; b, the LED
  screen, rejected) and the traffic **a** (the wedge cars and the CDF gunship; b, the pods,
  rejected), both produced at the close, their b files in `concept/rejected/`; the Civilian cast
  (**b**, Faith Abiola-Ellison, a woman's voice; a, KirksVoice, rejected), her two lines voiced; the
  captures, the voiced lines, the part B numbers and the D1–D6 checklist accepted; the texts
  (the two shortened briefing pages, Rook's teaser, Varga's four intel lines and the radio script)
  approved, so the document leaves `review` for `approved`, as the round said it would. Every art
  part of the level is production art, `art: final`; every Implementation item is ticked, so the
  implementation is `done`.
