---
title: Level 10 – Evacuation Corridor
design: approved
implementation: done
art: final
depends-on: [../../../enemies/air/wraith, ../../../enemies/air/mote-swarm, ../../../allies, ../../../player/wingmen]
updated: 2026-10-08
---

# Level 10 – Evacuation Corridor

## Summary

The first **rear-heavy** level and the act's `escort` mission. At first light five civilian
shuttles, *Lifeline One–Five*, lift off from Eko spaceport with eleven hundred evacuees, and Lancer
and Rook fly the corridor with them across the Nova Lagos outskirts to the lagoon, where they climb
out. Cloaked [Wraiths](../../../enemies/air/wraith/README.md) pass overhead, decloak behind the
player and fire up the screen through the shuttle band, and
[Mote Swarm](../../../enemies/air/mote-swarm/README.md) flocks sweep past and loop back from below:
about a third of the threat comes from behind. One shuttle, Lifeline Three, is lost in a scripted
event at t=118; every other loss is up to the player. 200 s at 190 px/s, the fastest scroll so far;
no boss.

## Briefing

Two pages, as Levels 08 and 09, each **one screen** (`BriefingLayoutTest`): Okafor's mission, then
Varga's warning about the rear attackers. Level 09 ended with "The outer districts evacuate at first
light, and you're flying cover."; this level flies at that first light.

<!-- data: briefing -->
> **Commander Okafor:** "Nova Lagos evacuates at first light, Lancer. Five shuttles lift from
> Eko spaceport with eleven hundred people aboard. You and Rook are their corridor to the
> lagoon. Every shuttle we lose is two hundred and twenty lives."
>
> **Dr. Varga:** "The Vrell hunt evacuation flights from behind. Cloaked flyers pass overhead,
> then decloak at your six and fire up the screen. Flocks sweep past and turn back. Fit a rear
> gun, Lancer."
<!-- /data -->

*Images* (production art straight away, user decision D10 = a of M5 part D;
[briefing images](../../../ui/briefing/README.md)): Okafor's page `level-10-evacuation-route` (the
corridor from above at first light: Eko spaceport and its pads, the jammed suburbs, the maglev
viaduct between the creep districts, the coast road with the capsized ferry, the lagoon; the five
shuttles' route marked), Varga's `level-10-wraith-scan` (a Wraith's scan cloaked and decloaked, its
pass overhead, loop and rear entry drawn as a path, and a Mote Swarm's loop-back); the level's data
names them as each page's `image`.

*Hangar teaser* (shop screen before L10; Rook, `LT. K. TANAKA / AEGIS TWO`, not voiced, as round
30's):

<!-- data: teaser -->
> **Rook:** "Shuttles to babysit and bugs that hit from behind. Bolt a gun on your tail,
> Lancer."
<!-- /data -->

## Threat profile

Generated from the level data's `threat_profile`. The directions are counted by each wave's `from`
(the intel's rule): a looping
Mote Swarm enters at the top (`from: front`, with a `loop_back`), so the profile's text gives the
share **by threat** as well (Level 06's precedent).

<!-- data: threat-profile -->
| Field | Value |
|---|---|
| Dominant layers | `air`, `high-air` (cloaked Wraiths); `low-air` carries smoke, sea mist and CDF helicopters (scenery) |
| Attack directions | front 82% · sides 1% · rear 16% by enemy count; **by threat** about a third from behind (the looping Mote Swarms come back from the bottom edge, and the Wraiths strike from it) |
| Density | 3 |
| Recommended traits | `rear` (**required**, user decision D9 = a), `spread` (`homing` also reaches a cloaked Wraith on its pass overhead) |
| Hazards | none (enemy fire crossing the shuttle band hurts the shuttles) |
| Boss / mid-boss | none (finale: a Wraith ambush as the shuttles climb) |
| Sensor-suite detail | none: Earth megacity at first light, `air` + `high-air`, front and rear · L1: + directions, density 3, the objective `ESCORT 5 SHUTTLES` · L2: + Wraith (new), Mote Swarm (new), Skitter, Needler and Stinger portraits; no boss · L3: + `rear` and `spread` highlighted, the wave strip marking the five Wraith ambushes (50, 79, 106, 157 and 187.5 s), 1 secret |
<!-- /data -->

**Launch warning** (user decision D9 = a): the threat profile marks `rear` as `required` (the schema
of M5 part C's `anti-ground`), so the hangar warns at **every** sensor level, not only from L3,
when no **rear-slot weapon** is fitted. Only rear guns count: homing shots pick targets ahead of the
ship (the Hornet's cone is 120°, the Micro-missile's 140°) and Rook fires only up the screen.

*Varga's intel line* per sensor-suite level (the data's `varga`):

| Sensor suite | Line |
|---|---|
| none | "Nova Lagos's outskirts at first light, Lancer. Flyers only, and five shuttles to keep alive." |
| L1 | "A third of it comes from behind you, Lancer. Five shuttles in one corridor: keep them flying." |
| L2 | "New: the Wraith, cloaked until it strikes from behind, and the Mote Swarm, a flock that turns back." |
| L3 | "A rear gun is a must: only homing finds a cloaked Wraith. One cache: a capsized ferry's hatch. Shoot it open." |

## Objective

- **Primary** `escort` (user decision D5 = a): escort the five
  [evacuation shuttles](../../../allies/README.md#evacuation-shuttle), **Lifeline One to Five**, to
  the climb-out at the end of the scroll.
  - **Lifeline Three** is lost in a scripted event at t=118 (D4 = a, see *Scripted loss*). Before
    that nothing can hurt it, so the scripted loss is always this shuttle and never doubles up with
    a player-caused loss. It is not one of the four **saveable** shuttles.
  - **Fails at once** when the four saveable shuttles are lost, even before t=118, through the
    [failed primary objective](../../../systems/retry/README.md#on-a-failed-primary-objective) flow
    (the mission failed screen 3 s later), with Okafor's line on that screen. Each loss before that
    only lowers the pay.
  - **Pay**: each saveable shuttle home earns the escort's pay per unit home, **25 credits** in
    Act 1 terms (40 in Act 2, the credit factor 1.6), Level 04's mechanism with its debrief row; up
    to 160. The typical haul counts it at the primary's rate 1.0.
- **No secondary objective.** The draft's "+40 per surviving shuttle" was that pay, not a
  secondary (the four level-end lines by count say how it went).

### The shuttles

Five [evacuation shuttles](../../../allies/README.md#evacuation-shuttle) (sprite 64×40, hit box
48×28, armour 120: easy 180, hard 90; smoke below 50 %). Their numbers are first values for the
level data.

- **Stations** (user decision D1 = a): a loose double column in the **band y = 140–300 px** (px
  below the top edge; the sprites stay inside it), Lifeline One leading in the middle. Each drifts
  slowly round its station on a deterministic **lane sway**, a lazy figure-eight
  (x + 16 sin θ, y + 4 sin 2θ, θ = 2π (t ÷ period + phase)), on the level clock. They never react
  to threats or to the player, and their hit boxes never overlap. They stay between x = 120 and 360,
  so the Wraiths' exit lanes along the side edges stay clear.

  | Shuttle | Station x, y (px) | Period | Phase |
  |---|---|---|---|
  | Lifeline One | 240, 165 | 9 s | 0 |
  | Lifeline Two | 168, 215 | 11 s | 0.25 |
  | Lifeline Three | 312, 215 | 10 s | 0.5 |
  | Lifeline Four | 168, 270 | 12 s | 0.75 |
  | Lifeline Five | 312, 270 | 8 s | 0.1 |

- **Liftoff** (t 1–7): at t=1 each shuttle stands on its pad, in the lower half of the screen
  (first values: One at 240, 400; Two and Three at 176 and 304, 440; Four and Five at 176 and 304,
  490), as the spaceport scrolls past. Over **6 s** it climbs to its station: its screen position
  eases (smoothstep) from the pad's to the station's while the pad scrolls away beneath it, and it
  is drawn from the ground layer's scale up to the air scale (ground → `low-air` → `air`), its
  shadow separating. It is **untouchable** throughout: enemy fire and contact pass through it.
- **What hurts them** (D2 = a): every enemy bullet that touches a shuttle's hit box hurts it by the
  bullet's class (`small` 4, `medium` 6) and is spent; an `air` enemy's body hurts it by the
  enemy's tier once per contact (a mote 6), and a `tiny` or `small` enemy is destroyed by the
  impact, as when it rams the ship. Front shooters' shots at the ship below the band cross it too,
  and the Wraiths' bursts go **straight up through it** as a fixed fan, aimed at nobody (user,
  2026-10-08; [wraith](../../../enemies/air/wraith/README.md)): "don't fly under the convoy" is part
  of the level. The Skitter snakes and streams fly round the band (down a side lane, then across
  below it), so they no longer ram it on their way past. Player fire, Rook's
  and the specials never hurt them, and the ship and Rook fly through them. **Tuning**: the armour
  and the swarms' routes are tuned until the autopilot, which does not guard them, keeps at least
  3 of the 4 saveable shuttles on medium; a per-shuttle `damage_factor` is the fallback.
- **Lost**: it loses power and **glides down into `far`** over its whole 3 s (scaled down to far's
  scale, darkened toward far's hazy tone from the moment it is hit, sliding 64 px out of its column
  away from the band's middle so the shuttle below does not hide it, fading out only in its last
  0.36 s, trailing a steady plume of large dark smoke, no debris on the play plane), a presentation
  effect: the simulation removes it at once. Okafor's `ally-lost` line names it.
- **Climb-out** (t 196–198): the engines flare and the shuttles climb off the top edge,
  untouchable; every saveable shuttle alive at t=196 is home.
- **HUD** (D3 = a): the two-line [objective tracker](../../../ui/hud/README.md#left-panel-mission),
  line one `SHUTTLES n / 4` (the saveable shuttles still flying), line two five small armour bars
  in order One to Five (a white flash on a hit, amber below 50 %, a red flash then dark when lost;
  Lifeline Three's bar dark from t=118, without changing the count; a shuttle home after the
  climb-out full in pale mint under a green glow). The wave warning banner sits above the band in
  this level (97–127 px below the top edge), clear of Lifeline One's station.

### Scripted loss (t=118)

User decision D4 = a, as the draft wrote it:

- **Untouchable before it**: Lifeline Three takes no damage before t=118, and enemy fire and
  contact **pass through** it (no bullet is spent on it, no rammer dies on it), so it is no free
  shield for the others.
- **The glow** (t 116–118): an alien glow pulses in the cloud and smoke over Lifeline Three, drawn
  as light under the air layer (on `deep` the section's ground would hide it) and on no layer
  anything can hit; its look is round 32's b, a teal iris vortex opening straight above Lifeline
  Three. Lifeline Three's pilot sees it (the t=116 line).
- **The lance** (t=118): a broad teal-white column of light lands on Lifeline Three from the iris
  and collapses into rippling teal and violet shock rings. It cannot be prevented and hurts nothing
  else. The shuttle loses power and glides into `far` like any lost
  shuttle.
- **It costs nothing**: no pay lost (it is not one of the escort's paid units, nor in the typical
  haul's), it never counts toward the fail, and it raises no `ally-lost` or `first-ally-lost` cue;
  the radio event `scripted-loss` cues Okafor's line instead.
- **The music ducks −6 dB for 3 s** from t=118, no sting; with the radio's duck the lower of the
  two applies, they do not add up.
- **Look**: round 32's **b** "Iris column" (D10 = a: an a/b at production quality; user pick
  2026-10-08), `tools/art/lance_l10.py`; a's thorn spear was rejected.

## Layout

Ground scroll speed **190 px/s** (a chase level per the
[art direction](../../../art-direction/README.md#parallax-layer-model)), the launch the first 5 s.
Total **200 s ≈ 38,000 px**. L10 has no hold zones, so script time is real time. **Time of day**
(user decision D11 = a): **first light**, as Level 09's end line promises: the sprawl at dawn in
section 1, the morning sky over the lagoon in section 5. Motion budget: the strong elements are the
fast-moving ground and the smoke columns of section 3; the refugee traffic on the roads is static
or crawls; the helicopters and the sea mist are the moving scenery.

<!-- data: level-sections -->
| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Liftoff | 0–20 | 0–3,800 | 190 | light | `deep`: the sprawl at dawn, a pale sky in the east. `ground`: Eko spaceport, its pads, blast fences and service vehicles. `low-air`: smoke from the city behind. | The liftoff (the five shuttles rise off their pads, t 1–7); Skitters and Needlers over the pads from t≈8. |
| 2. Refugee Roads | 20–70 | 3,800–13,300 | 190 | clear | `ground`: suburbs, highways jammed with stalled traffic and refugee columns (lights only, no figures at this scale), parks and football pitches. `low-air`: CDF helicopters heading the other way. | Mote Swarm and Wraith introductions, each in a safe situation first; the first rear waves. |
| 3. The Corridor | 70–130 | 13,300–24,700 | 190 | medium | `ground`: a maglev viaduct between two infested districts, creep on both sides. `low-air`: smoke columns. `deep`: the cloud deck. The loss's glow (the lander) pulses over Lifeline Three at t 116–118, drawn under the air layer. `high-air`: smoke wisps (≤ 40 %). | Rear pressure builds; the scripted loss at t=118. |
| 4. Coast Road | 130–175 | 24,700–33,250 | 190 | light, medium peak 160–175 | `ground`: the lagoon shore and the coast road, fishing piers, sandbars, the capsized ferry with its blinking hatch. `low-air`: sea-mist banks rolling in from the right. | Mixed front and rear waves, the crossing swarms; the ferry secret. |
| 5. Orbital Corridor | 175–200 | 33,250–38,000 | 190 | clear | `deep`: the open lagoon and the sea under the morning sky. `ground`: shallow water, buoys. | The shuttles' engines flare and they climb off the top of the screen (t 196–198); the finale (a Wraith ambush) and the release. |
<!-- /data -->

## Waves

Enemy definitions are linked per row; formation names come from the
[formation vocabulary](../../../enemies/README.md#formation-vocabulary) (`swarm` and `rear ambush`
are built in M5 part D). The Wraith and the Mote Swarm are new; the returning units are all `tiny`
or `small` and keep their HP. Every rear entry is warned at the bottom edge **3 s** ahead, and Rook
switches to his Trail formation (see
[Rook's AI](../../../player/wingmen/README.md#rooks-ai-first-draft-parameters)):

- A **`rear ambush`** of Wraiths (`from: rear`) enters at the top at its time, cloaked, and is
  warned 3 s ahead of its **re-entry** at the bottom edge (about 4.6 s after its time), not ahead of
  its time; its lanes are spread evenly across the bottom edge.
- A **looping swarm** (`from: front` with a `loop_back`) is warned 3 s ahead of its re-entry; the
  off-screen gap is 1.5 s, so the warning starts while it still exits (the chains' rule).
- A **swarm from the rear** and a **line abreast from the rear** are warned 3 s ahead of their
  time, as every rear wave.

**Density and pacing** (built in M5 part D's data step): at least **50 enemies a minute** at medium
over the 195 s after the launch
(`DensityTest`), and no empty screen over **3 s of real time** but the quiet end (`PacingTest`, every
difficulty, the plan's loadout with Rook). The draft's 137 enemies (42 a minute) were too few, and
its 6–14 s between waves and its enemy-free liftoff broke the pacing rule. The table keeps the
draft's set pieces (the introductions, the loop-backs, the crossing swarms, the Wraith ambushes, the
circle) and fills the gaps with Act 2 popcorn (Skitter streams and snakes, Needler lines, small Mote
Swarms and rear Skitter lines), a wave every 2.5–3 s, the first at t=7.5 over the pads: **318
enemies, about 98 a minute** (the Skitter snakes of 6 and streams of 4 as in Level 09; Level 09
needed 84 a minute to pass the pacing rule). `PacingTest` with the autopilot: no pause over 3 s on
medium and hard; on easy only the quiet end (from t≈195). The Skitter snakes and streams keep out
of the shuttle band (2026-10-08): a snake comes down the side lane of its edge (x 50 or 430) to
250 px below the top edge, curls across below the band (340–400 px down) and leaves at the bottom
of the other side; a stream's units from the left come down x = 70 to 300 px down and then run
diagonally to the bottom right, those from the right the mirror image (the routes in the data's
`paths`, at least 47 px clear of the band's hit boxes).

<!-- data: waves -->
| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 7.5 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | Over the pads as the shuttles climb; the first targets |
| 10.5 | 1 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | Curls over the blast fences |
| 13.5 | 1 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 16 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 18.5 | 1 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | |
| 21 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | Over the first suburbs |
| 24 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | |
| 27 | 2 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 10 | front | **New.** Safe first sight: sweeps down the left lane and leaves at the bottom-left without looping back; Rook's line at t=24, Varga's at 32 |
| 30 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 33 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | |
| 36 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | |
| 39 | 2 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 5 | rear | The first rear wave: the 3 s bottom-edge warning, Rook's rear bark and his Trail; nothing fires |
| 42 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | |
| 45 | 2 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 6 | front | Down the right lane, no loop-back |
| 48 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | |
| 50 | 2 | rear ambush | [Wraith](../../../enemies/air/wraith/README.md) | 1 | rear | **New.** One Wraith down the centre lane, cloaked; the warning from ≈ 51.6, re-entry ≈ 54.6, decloak ≈ 55 (Varga's `first-decloak` line); its bursts cross the shuttle band |
| 52.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 55.5 | 2 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 58.5 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | |
| 61.5 | 2 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 14 | front | The full pattern: down the left lane, across below the band, out at the bottom-right, back from the bottom 1.5 s later (≈ t 66) up the right lane past the ship (`loop_back`); Rook's `first-loop-back` line; hard loops back twice |
| 64.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 67.5 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | |
| 70.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | Onto the maglev viaduct |
| 73 | 3 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 4 | front | A rear gun catches them after the dive |
| 76 | 3 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 5 | rear | |
| 79 | 3 | rear ambush | [Wraith](../../../enemies/air/wraith/README.md) | 2 | rear | Re-entry ≈ t 83.6 |
| 81.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 84.5 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 87.5 | 3 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 16 | front | Loops back (≈ t 92) up the left lane, past the shuttle column; hard twice |
| 90.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 93.5 | 3 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | |
| 96.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (alternating edges) | |
| 100 | 3 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | |
| 103 | 3 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 5 | rear | |
| 106 | 3 | rear ambush | [Wraith](../../../enemies/air/wraith/README.md) | 2 | rear | Easy 1 |
| 108.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | |
| 111.5 | 3 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | The last one drops an overdrive |
| 114.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | Under the violet glow (from t=116); the lance strikes Lifeline Three at t=118 |
| 119 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 122 | 3 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | |
| 125 | 3 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | front | Loops back from the bottom-left (≈ t 129.5); hard twice |
| 128 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 130.5 | 4 | column | [Stinger](../../../enemies/air/stinger/README.md) | 4 | front | Onto the coast road |
| 133.5 | 4 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | |
| 136.5 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 139.5 | 4 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | rear | Crossing swarms: up from the bottom-left |
| 139.5 | 4 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 8 | rear | Up from the bottom-right at the same time |
| 142.5 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 145.5 | 4 | pincer | [Needler](../../../enemies/air/needler/README.md) | 4 | sides | 2 per side, hold 4 s; Rook's flank bark |
| 148.5 | 4 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | Over the capsized ferry |
| 151.5 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (alternating edges) | |
| 154.5 | 4 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | The last one drops an armour patch |
| 157 | 4 | rear ambush | [Wraith](../../../enemies/air/wraith/README.md) | 3 | rear | Easy 2 |
| 160 | 4 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 5 | rear | |
| 163 | 4 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | Into the sea mist |
| 166 | 4 | swarm | [Mote Swarm](../../../enemies/air/mote-swarm/README.md) | 10 | front | Loops back (≈ t 170.5); hard twice |
| 169 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 170 | 4 | rear ambush | [Wraith](../../../enemies/air/wraith/README.md) | 2 | rear | Hard only |
| 172 | 4 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | |
| 175.5 | 5 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | Over the open lagoon |
| 178.5 | 5 | circle | [Needler](../../../enemies/air/needler/README.md) | 6 | front | Orbits ahead of the shuttles for 6 s, then breaks one by one (hard: in pairs) |
| 181.5 | 5 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 5 | rear | |
| 184.5 | 5 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 187.5 | 5 | rear ambush | [Wraith](../../../enemies/air/wraith/README.md) | 3 | rear | Decloaks (≈ t 192.5) as the shuttles start to climb; the finale |

Totals: Skitter 169 · Needler 47 · Mote Swarm 80 · Wraith 11 · Stinger 11.
<!-- /data -->

Totals at medium: 318 (Mote Swarm in motes; hard +2 Wraiths, the t=170 pair). By `from`: 262
front, 52 rear, 4 sides; the 48 motes of the four looping swarms attack from the rear as well.

## Ground targets

<!-- data: ground-targets -->
| Section | Target | Effect |
|---|---|---|
| 4 | The capsized ferry's hull hatch on a sandbar, blinking: a destructible of 50 HP (about a second of the forward guns lined up on it, so a passing sweep does not open it at the top edge), no bounty (t≈150) | Releases the hidden crate (secret, see below) |
<!-- /data -->

No hostile ground targets: the threat in L10 is all in the air.

## Hazards

None. The danger is to the shuttle column: enemy fire aimed at the player from ahead, and the
Wraiths' bursts straight up from below, cross the shuttle band.

## Secrets and pickups

- **Ferry cache** (t≈150): the hatch, a destructible ground object with a hidden crate (as Level
  09's cocoon, but not hardened), releases a CDF supply crate worth **160** credits (100 in Act 1
  terms × 1.6). Its **50 HP** take about a second of the forward guns lined up on it, so a passing
  sweep does not open it at the top edge. A pilot lined up under it with the fire held has his
  shots in the air as it enters: the plan's guns with Rook open it about **140 px down** (the
  capture: about 150), every gun at its top level about 80 px down. Its crate drifts at 40 px/s and,
  in this level only, stays **12 s** (`crate_seconds`; other pickups and other levels' crates 6 s):
  dropped anywhere on the screen, even at the top edge (10.6 s of drift), it reaches a ship holding
  its start line under the hatch (from 140 px down 6.7 s later; opened lower, sooner). As everywhere, the secret counts as found when it is opened
  (the grade and the Explorer bonus); the crate's credits only when it is caught. Its line, Varga: "*That ferry's hold had a CDF supply crate in it. Take it,
  Lancer.*"
- **Overdrive**: dropped by the last Needler of the t=111.5 V-wing, after the t≈106 ambush.
- **Armour patch**: dropped by the last Needler of the t=154.5 V-wing, before the t≈157 ambush.
- A Wraith carries nothing: it dies low behind the ship, 20–70 px above the bottom edge, where its
  drop would drift off the screen before it could be caught (the draft's carriers moved in M5 part
  D's data step).
- Shield cells follow the normal drop table.

## Radio chatter

Spoken radio ([voice](../../../audio/voice/README.md)), as Levels 08 and 09. The script is retimed
to the HUD's 1 s rule (`RadioTimelineTest`): a timed line holds the radio for the longer of its
voice and its text (5 s for the last page plus 3 s for each page before it, typed at 30 characters a
second), then 0.4 s of silence, and starts after the one before it has closed. The draft clashed in
five places; now: Lifeline One at t=1 (two pages, closing ≈ 11.7) and Okafor at t=12; Rook at t=24
and Varga at t=32; Varga's Wraith line and Rook's flock line follow what happens (events) instead
of t=54 and t=60; Lifeline Three at t=116, Okafor's loss line on the loss's event (≈ 123.5, after
it) and Rook at t=131.5; Varga at t=139 (before the ferry's line can queue); Rook at t=188.5 and
Lifeline One at t=196.

**New speakers** (user decision D12 = a), auditioned a/b in round 32 and cast there (both **b**,
user pick 2026-10-08; see [voice](../../../audio/voice/README.md)): **Lifeline**, Lifeline One's pilot, also the voice of Two, Four
and Five and of the hit line (one voice, several names, as the docks); **Lifeline Three**, one line,
cut off. Both use the generic civilian radio portrait. **Rook's lines** carry `requires: escort`.
New events (M5 part D): `first-decloak` (the attempt's first Wraith decloak), `first-loop-back`
(the first swarm's leader re-entering at the bottom edge; as `escort-first-kill`'s line it counts
as a scripted Rook line for the barks' spacing, so the rear bark of the same loop-back is
withdrawn), `scripted-loss` (the lance) and `ally-lost` (every player-caused loss).

<!-- data: radio -->
| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Generic civilian (Lifeline One) | "Eko Control, Lifeline One. Five birds, eleven hundred souls. We're going." |
| t=12 | Okafor | "Lifeline flight, Aegis has you. Stay in the corridor and stay together." |
| t=24 | Rook | "Something's coming down the left. A lot of somethings." |
| t=32 | Varga | "That's a flock, not a formation. They steer like starlings." |
| First decloak (`first-decloak`) | Varga | "It hid in plain sight! Cloaked, only homing touches it. Rear guns!" |
| First loop-back (`first-loop-back`) | Rook | "The flock's turned around. They're coming up behind us!" |
| First shuttle hit (`first-ally-hit`) | Generic civilian (Lifeline) | "Lifeline {ally}, we're hit! Still flying. Please stay close!" |
| t=116 | Generic civilian (Lifeline Three) | "Aegis, there's a light above the clouds. What is that—" |
| Scripted loss (`scripted-loss`) | Okafor | "Lifeline Three is down. Keep the others moving, Lancer." |
| t=131.5 | Rook | "Where did that come from? Nothing on my scope!" |
| t=139 | Varga | "That shot came from above the cloud deck. Something big is sitting up there, out of reach." |
| A shuttle is lost (`ally-lost`) | Okafor | "We lost Lifeline {ally}. Stay on the others." |
| t=180 | The Choir (distorted) | "[the Choir sings]" |
| t=188.5 | Rook | "Last one out of Lagos, get the lights." |
| t=196 | Generic civilian (Lifeline One) | "Corridor clear. We can see the sky. Thank you, Aegis. Thank you." |
| Level end, 4 shuttles home | Okafor | "Four of five made orbit. Eight hundred and eighty people will see tomorrow. Remember the ones you got home." |
| Level end, 3 shuttles home | Okafor | "Three shuttles made orbit. Six hundred and sixty people will see tomorrow. Good work, Lancer." |
| Level end, 2 shuttles home | Okafor | "Two shuttles made orbit. Four hundred and forty people will see tomorrow. It's not nothing, Lancer." |
| Level end, 1 shuttle home | Okafor | "One shuttle made orbit. Two hundred and twenty people will see tomorrow. Hold on to that." |
| The four saveable shuttles lost (the failed screen's line) | Okafor | "We've lost the corridor, Lancer. Pull back." |
<!-- /data -->

The mission-failed line is not played on the radio: it is the mission failed screen's line. The
level-end lines are four explicit lines by the count home (the cue's `allies`), so no `{n}`
arithmetic is needed; the numbers are spoken (220, 440, 660, 880 people). The cut-off "What is
that—" is checked by ear in the render (a dash reads as a comma). Dropped from the draft: Rook's
scripted "Six o'clock, Lancer!" at t=50 (his rear bark says it), "and beam weapons" (no player beam
exists in Act 2), Okafor's "(1.5 s pause)" (no pipeline support; the event's queueing gives the
beat), and the "Secondary objective met" line (the four-home level-end line holds "Four of five").

## Boss / mid-boss

None. The Wraith ambush under the climbing shuttles (t≈187.5, decloaking ≈ 192.5) is the finale.

## Music & ambience

The Act 2 A theme **"Homefront"** (track 6 in the
[track list](../../../audio/music/README.md#track-list), final with its base stem): the megacity
ambience alone in section 1, the **base stem from section 2** and the **full mix from section 3**
(t=70; the draft's "intensity stem from t=82" had no hook, stems switch by section or by a run-time
hook). At the scripted loss the music **ducks −6 dB for 3 s** with no sting (a new duck on the
`scripted-loss` event; the lower of it and the radio's duck applies). Ambience: the
[megacity ambience](../../../audio/sfx/README.md#ambience-per-setting) (`earth-megacity`),
**crossfading over 4 s to the ocean ambience** at section 4 (round 08's ocean loop, brought forward
from part E; an ambience change by section is new). New sounds, picked in round 32: the Wraith's
decloak (b, a metallic rip over a membrane swish), the Mote Swarm's whoosh (b, an insect chitter, on
its entry and its loop-back) and the lance's strike (a, an alien falling whine into a thunder crack,
started at t 116.8 so its impact lands at 118) ([sfx](../../../audio/sfx/README.md#enemies)); the shuttles' hits and losses reuse the ally sounds.

## Credit budget

Budget(10) = 700 × 1.07⁹ ≈ **1,287**, the typical haul's target
([economy](../../../systems/economy/README.md#per-level-budget)), not a perfect collection: the
typical player takes 60 % of the air kills, half the secrets and the primary's pay as authored. The
level's `bounty_scale` puts the typical haul on it within ±5 % (`TypicalHaulTest`). The stat-block
bounties (Act 1 terms) are paid × the act factor 1.6 × the scale, rounded per kill; the data keeps
Act 1 terms for the rest (the crate 100, the escort's 25 per shuttle). Built: `bounty_scale`
**0.56** (Skitter 4, Mote 2, Needler 11, Stinger 13, Wraith 27 a kill), the typical haul **1,316**
(+2.2 %), a perfect run **2,113** (1.64 × budget). The bounty's rounding leaves no scale between
−3 % and +4 %; the 28 Skitters the snakes (6, the stat block's least) and streams (4) add lift the
haul from −3 % to +2 %, which also keeps the balance plan's repairs affordable at the L13 visit. The gap check's
≈ 0.85–0.9 assumed about 210 enemies; the density the pacing rule needs brings it down, as in Level
09 (0.47 estimated, 0.4 built). The draft's 1,838 was the old perfect-run curve with Act-2-scaled
bounties (Mote 3, Wraith 48) and a crate of 160 and "+40" per shuttle in Act 2 terms.

<!-- data: credit-budget -->
| Source | Perfect run | Typical haul |
|---|---|---|
| Kills: Skitter 169 × 5 + Needler 47 × 12 + Mote Swarm 80 × 2 + Wraith 11 × 30 + Stinger 11 × 15 | 1,793 | 1,076 |
| Primary objective: 4 shuttles home × 25 (of 5: the scripted loss's shuttle is not paid) | 160 | 160 |
| Secret: ferry cache (hidden crate, 8% of budget) | 160 | 80 |
| **Total** (bounty scale 0.56) | **2,113** | **1,316** |
| Budget(n) = the typical haul's target; typical +2 %, perfect 1.64 × budget | | 1,287 |
<!-- /data -->

**Rear firepower in the balance plan** (user decision D7 = a): the plan's L10 visit buys the **Tail
Gun** (rear DPS 10; moved here from L09 by part C's D8) and **refits Rook's Autocannon** (owned
since L08, a new `fit` action: the Mortar has nothing to hit in L10). The Wraith's 16 HP ÷ 10 =
1.6 s of its 2.9 s decloak-and-hold window; `BalanceTest` gets a **rear check**: the Wraith's HP ÷
the plan's rear DPS at most 0.6 × (flash + hold) at medium (16 ÷ 10 = 1.6 ≤ 1.74). The plan's
forward DPS row counts the Bomb Rack, which has no ground targets here.

## Difficulty notes

- **Easy**: shuttle armour **180**; the t≈106 and t≈157 ambushes have one Wraith fewer (the
  finale keeps its three: the counts are authored, so the formation lever does not apply); the Mote
  Swarms loop back once, as on medium (the draft's "one dive and then leave" is what medium does),
  16 motes for 20 by the formation lever.
- **Hard**: shuttle armour **90**; the Wraiths' 7-shot bursts and 3.0 s holds (their stat block); an
  extra rear ambush of 2 Wraiths at t=170 (`skip: [easy, medium]`); the three-Wraith ambushes stay
  three (authored: the formation lever would make them four, more than the autopilot survives); every
  looping swarm loops back **twice**; 24 motes for 20.

## Concept art

No level-specific concept files yet. The look comes from the chosen megacity scene in
[art direction](../../../art-direction/README.md):
[parallax-r03-b.png](../../../art-direction/concept/parallax-r03-b.png), built for this level at first
light on Level 08's production kit and its traffic, with Level 09's creep tones for the infested
districts and the water of the chosen ocean scene
([scene-ocean-r10-a.png](../../../art-direction/concept/scene-ocean-r10-a.png)) for the coast and the
lagoon. Part D (user decision D10 = a) takes the backdrop, the briefing images, the Wraith, the
motes and their intel portraits straight to production, and proposes **a/b at production quality**
only for the [evacuation shuttle](../../../allies/README.md#evacuation-shuttle) (a picked) and for
the scripted loss's look (the glow and the lance; b picked) in concept round 32, closed 2026-10-08
with the production art approved as final. Prompts and generator notes:
[concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/rejected/loss-r32-a.png](concept/rejected/loss-r32-a.png), [.gif](concept/rejected/loss-r32-a.gif) | Scripted loss look a "Thorn spear" (`tools/art/lance_l10.py`): violet light in the cloud deck ahead of Lifeline Three pulsing 116–118, a thin thorned violet spear at 118, a ring flash with thorn spikes, the wreck's glide (production frames, dawn stand-in) | rejected |
| [concept/loss-r32-b.png](concept/loss-r32-b.png), [.gif](concept/loss-r32-b.gif) | Scripted loss look b "Iris column" (`tools/art/lance_l10.py --variant b`): a teal vortex opening straight above Lifeline Three, framing it in a violet ring, then a broad teal-white column at 118 collapsing into rippling teal and violet shock rings; in the game since the pick | chosen |
| [concept/backdrop-final-r32-a.png](concept/backdrop-final-r32-a.png), [.gif](concept/backdrop-final-r32-a.gif) | Production backdrop at first light on Level 08's kit (`tools/art/backdrop_l10.py`; its block for the level data is [concept/backdrop-proposal.yaml](concept/backdrop-proposal.yaml)): Eko spaceport and the five launch pads, the jammed suburbs, the maglev viaduct between the creep districts, the coast road and the capsized ferry with its hatch, the open lagoon; the ferry hatch's sprites; the GIF the liftoff (t 0–8); approved as final | chosen |
| [concept/level-10-capture-final-r32-a.png](concept/level-10-capture-final-r32-a.png), [.mp4](concept/level-10-capture-final-r32-a.mp4) | Round 32 capture of the game after the fix pass (medium, the plan's loadout, Rook's Autocannon, `--invulnerable`, a key-driven sweep): liftoff, a cloaked Wraith, decloak, the fixed fan up through the band, the lance, a loop-back round the band, the ferry hatch and its crate caught, the rear banner, a finale loss, the climb-out, the mint home bars, the debrief (3/4 home, A); 15 panels and the whole run with sound (details in [prompts.md](concept/prompts.md)); taken with the placeholder sounds and look a | chosen |
| [concept/loss-capture-final-r32-a.png](concept/loss-capture-final-r32-a.png), [.mp4](concept/loss-capture-final-r32-a.mp4) | The scripted loss in the game after the fix pass, look a: glow, lance, flash, the wreck's 3 s glide (darker, smoking, sliding out); t 110–122 with sound; taken before the pick (look b and lance a are in the game since) | chosen |
| [concept/level-10-readability-r32-a.png](concept/level-10-readability-r32-a.png) | 1:1 and 2× crops after the fix pass: the Wraith cloaked and decloaking, its fan up through the band and a hit, the lost shuttle's wreck, a mote loop-back clear of the band, the ferry hatch open and its crate reaching the ship, the rear banner above the band, the tracker's mint home bars | chosen |

## Implementation

- [x] Scroll timeline, sections, atmosphere and layer content as in *Layout*, at first light
      (D11 = a): Level 10's own backdrop on Level 08's kit, 190 px/s, ≈ 38,000 px
- [x] The shuttles (D1 = a): five stations in the band with the lane sway, no reaction to threats;
      the liftoff (t 1–7) and the climb-out (t 196–198) untouchable; armour 120 (easy 180, hard
      90); smoke below 50 %; the glide into `far` when lost
- [x] What hurts them (D2 = a): every enemy bullet (spent) and every `air` contact, small rammers
      destroyed; measured: the autopilot keeps at least 3 of the 4 saveable shuttles on medium
- [x] The scripted loss (D4 = a): Lifeline Three untouchable until t=118 with fire and contact
      passing through, the glow from t=116, the lance (round 32's look b, its sound a), no fail, no
      pay lost, no loss cue; the `scripted-loss` event; the music duck
- [x] The fail rule and pay (D5 = a): the mission fails at once when the four saveable shuttles are
      lost (the failed primary flow, Okafor's line); 25 per shuttle home (Act 1 terms); four
      level-end lines by the count home
- [x] The tracker (D3 = a): `SHUTTLES n / 4` over five armour bars
- [x] Wave script matches the *Waves* table (final times and counts by the data step); at least
      50 enemies per minute at medium (`DensityTest`); no empty screen over 3 s of real time but
      the quiet end (`PacingTest`, every difficulty)
- [x] Wraith `rear ambush` waves and Mote Swarm flocks with their loop-backs, each re-entry warned
      3 s ahead; Rook's Trail on them; a `WingmanTest` case for Trail near the Wraiths' hold band
- [x] Ground target, the ferry secret and the pickups
- [x] Radio voiced, every timed line at most 1 s late (`RadioTimelineTest`, with Rook and without
      him); the events `first-decloak`, `first-loop-back`, `scripted-loss` and `ally-lost`; Rook's
      lines `requires: escort`; Lifeline and Lifeline Three cast in round 32 (D12 = a) and voiced
- [x] Threat profile with `required: [rear]` and its launch warning at every sensor level (D9 = a),
      Varga's four intel lines and Rook's teaser fitting the intel panel (`IntelPanelLayoutTest`),
      the Wraith's and the Mote Swarm's intel portraits
- [x] Briefing: Okafor's and Varga's pages, each one screen (`BriefingLayoutTest`), with their
      images
- [x] Typical haul at medium within ±5 % of budget(10) = 1,287 with the level's `bounty_scale`
      (`TypicalHaulTest`)
- [x] Balance plan per D7 = a (the Tail Gun, Rook's Autocannon refitted) and `BalanceTest`'s rear
      check; the autopilot flies the level to its end with Rook on every difficulty
- [x] Music: "Homefront"'s base stem from section 2, the full mix from section 3, the −6 dB duck at
      the loss; the ambience crossfading to the ocean at section 4
- [x] Easy/hard variations as in *Difficulty notes*

## Open questions

- None (concept round 32 closed 2026-10-08).

## Decisions

- 2026-10-01: Promoted from the Act 2 roster to a draft level document.
- 2026-10-01: One scripted shuttle loss for the story; every other loss depends on the player,
  and losing all of them fails the mission (user decision). *Settled on 2026-10-08 by D5 = a: the
  mission fails when the four saveable shuttles are lost.*
- 2026-10-01: Open question resolved: friendly units got their own part, [allies](../../../allies/README.md); the shuttle spec moved there. Sprites come in a later concept round.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-08: M5 part D (user decisions of 2026-10-08): **D1 = a** the shuttles hold authored
  stations in the band y = 140–300 and drift slowly on a deterministic lane sway, never reacting to
  threats; rejected: b (sidestepping fire like Rook: a second AI, risk of flying into fire) and c
  (fixed stations: the band reads as a block). **D2 = a** every enemy bullet and every `air`
  contact hurts a shuttle, small rammers die on it, tuned until the autopilot keeps at least 3 of
  the 4 saveable on medium (a `damage_factor` the fallback); rejected: b (only threats from below:
  the same bullet hits or not) and c (Level 04's aimed-only rule: the rear bursts would no longer
  threaten the convoy). **D3 = a** the two-line tracker, `SHUTTLES n / 4` over five armour bars;
  rejected: b (Level 04's pips, three states for 120 armour) and c (five portrait frames, as the
  draft said: no room in the left panel). **D4 = a** Lifeline Three takes no damage before t=118,
  fire and contact pass through it, the glow pulses from t=116, the lance cannot be prevented, and
  it costs nothing (no pay, no fail, no loss cue); rejected: b (absorbing hits: a free shield) and
  c (the lance on the most damaged saveable shuttle: against the decision of 2026-10-01). **D5 = a**
  the mission fails at once when the four saveable shuttles are lost, even before t=118; the
  draft's "+40 per shuttle" is the escort's pay per unit home (25 in Act 1 terms, rate 1.0); no
  secondary; four level-end lines, the n = 4 one holding "Four of five"; rejected: b (a real "All
  four home" secondary, competing with the bars) and c (the pay as a new secondary kind).
  **D6 = a** the Wraith as its stat block (cloaked from the top, rear entry, decloaked exit along a
  side lane); **D7 = a** Wraith HP 16, a `BalanceTest` rear check, the plan refitting Rook's
  Autocannon at L10 ([wraith](../../../enemies/air/wraith/README.md)). **D8 = a** real flocking
  ([mote swarm](../../../enemies/air/mote-swarm/README.md)). **D9 = a** `required: [rear]`, the
  launch warning at every sensor level unless a rear-slot weapon is fitted; rejected: b
  (recommended only). **D10 = a** art straight to production, a/b only for the shuttle and the
  loss's glow and lance; rejected: b (no choice for the story beat) and c (a concept round first,
  placeholders in the build). **D11 = a** first light (dawn), as Level 09's voiced end line; rejected:
  b (dusk, as the draft: Level 09's line rewritten and re-voiced). **D12 = a** two new voices
  auditioned a/b in round 32, Lifeline (One, Two, Four, Five and the hit line) and Lifeline Three;
  rejected: b (one voice for all: the lost pilot sounding like the lead) and c (reusing Level 08's
  civilian and Level 07's Lifeboat Seven).
- 2026-10-08: M5 part D stated defaults (accepted with the decisions): the data in Act 1 terms
  (crate 100, escort 25 per shuttle) with a `bounty_scale` fitted by `TypicalHaulTest`, the scripted
  shuttle outside the haul's stations; the waves filled with Act 2 popcorn to at least 50 a minute
  and the 3 s pacing rule on every difficulty, the first enemies at t≈8 over the pads (the draft's
  enemy-free 20 s liftoff broke the rule); 190 px/s kept; edge warnings 3 s everywhere (the
  draft's 1.5 s replaced), the swarm's 1.5 s off-screen gap kept; directions by `from`, the looping
  swarms `from: front` with the share by threat in the profile's text (the draft's "42 % of the
  waves from the bottom edge" counted waves, and the L3 wave-strip marks are text, as Level 09's);
  the new formations `swarm` and `rear ambush`; the Wraith's veins drawn only; hard's 7-shot bursts,
  3.0 s holds, the t=170 pair and two loop-backs; easy as written but the motes as medium; the
  radio retimed to the 1 s rule (the draft's clashes at t=1–3, 32–36, 50–60, 116–121 and 196–198),
  Rook's t=50 line dropped, Varga's Wraith line on `first-decloak` without "and beam", Rook's flock
  line on `first-loop-back` (it played before the swarm had entered), Okafor's loss line on
  `scripted-loss` without the pause, `ally-lost` for every player loss (only the first had a cue),
  the hit line's speaker Lifeline with `{ally}`, a mission-failed line, "two hundred and twenty
  lives" (the briefing said two hundred against 1,100 ÷ 5), Rook's lines `requires: escort`; two
  one-screen briefing pages with images, Rook's teaser, Varga's four lines; `traits: [rear, spread]`
  with `homing` in the text; the shuttles untouchable in the liftoff and the climb-out, the lost
  one gliding into `far` as a presentation effect, armour 180 / 120 / 90; the ferry hatch a
  destructible of about 8 HP with a hidden crate; the overdrive and the armour patch as drafted;
  "Homefront"'s base stem from section 2 and full mix from section 3 (the draft's t=82 had no hook),
  the −6 dB duck, the ambience crossfade at section 4; no change to Rook's AI. The draft's
  contradictions are fixed: the budget (1,838 on the old curve → budget(10) ≈ 1,287), the
  Act-2-scaled values in it, the density, the pacing, the edge warnings, the directions, the radio
  clashes and content, the people per shuttle, the time of day (dusk → first light), the music's
  t=82, the fail rule, the secondary that was a pay, the HUD's portrait frames, the Wraith's entry
  edge, the difficulty notes. The liftoff starts on the pads and eases to the stations while the
  pads scroll away (the draft's 6 s on scrolling pads). Our readings, for review in round 32: the
  stations, sways and pads' first values; the shuttles' x range 120–360 (the Wraiths' side lanes);
  a rammer dying on a shuttle paid as on the ship; Okafor's `ally-lost` line at every loss, the
  first included; the radio times. `design` goes to `review` for the new texts (round 32);
  `implementation` is `in-progress`.
- 2026-10-08: The backdrop and the briefing images go straight to production (D10 = a) at first light
  (D11 = a); the CDF "helicopters" are Level 08's CDF gunship (scenery); haze and sea mist drift west
  on the sea wind.
- 2026-10-08: M5 part D data step: 318 enemies at medium (snakes of 6, streams of 4), 98 a minute;
  `bounty_scale` 0.56, typical haul 1,316 (+2.2 %); the Wraith drops moved to Needlers; the three-Wraith
  ambushes authored (hard's and easy's finale stay 3); the autopilot keeps 3–4 of 4 shuttles on medium
  with their armour unchanged; a ship within 72 px of a holding Wraith silences it (to review in round
  32). The shuttles bank fully at 20 px/s sideways (was 60: their lane sway never reached a bank frame).
- 2026-10-08: Fixes from the round 32 capture (user decisions of 2026-10-08). **Wraith bursts
  straight up** (user, option a): the two bursts of a holding Wraith go straight up the screen as a
  fixed 40° fan through the shuttle band, aimed at nobody, and the 72 px no-fire distance does not
  apply to them ([wraith](../../../enemies/air/wraith/README.md)). **Popcorn round the band**: the
  capture's early shuttle damage (t 12–45) came mostly from Skitter snakes ramming the band, so the
  ten snakes and twenty streams fly routes down a side lane and across below the band (*Waves*);
  the counts, 318 enemies and 97.8 a minute, and the typical haul (1,316, +2.2 %, `bounty_scale`
  0.56) are unchanged; the pacing gained (hard has no pause over 3 s now). Measured with the
  autopilot and the plan's fit (17 seeds on medium, 9 on easy and hard): medium keeps **3 of 4** on
  every seed, 2185 included (it was 3–4 with the aimed bursts; grade A, both Airstrike charges, the
  crate caught); easy 4 of 4 on every seed; hard **1 of 4** on every seed (it was 2–3), on its first
  attempt: the fans and the Stingers' dives through the band take most of it, and hard's 90 armour
  raised to 120 brought only 1–2, so hard's numbers are left as they are (kept, see below). The
  shuttle armour and the Wraith counts are unchanged (D2's target holds on medium). **The lost
  shuttle's glide** read about 1.5 s of its 3 s: the wreck frames shrink to 0.35 (past far's 0.45)
  and, under the low-air haze, read as gone from the middle of the glide; the game now plays the
  frames down to far's scale (0.47) over the whole glide, darkens the wreck from the hit toward
  far's tone, fades it only in its last 0.36 s and trails a steady plume of larger, darker smoke; a
  check capture showed Lifeline Three's wreck also sinking straight behind Lifeline Five, so it now
  slides 64 px out of the column (away from the middle) instead of the review's 18 px yaw.
  **The ferry hatch**: 8 HP opened at the top edge under passing fire and its crate ran out its 6 s
  before it reached the ship; at **50 HP** it takes about a second of the forward guns lined up on
  it; the autopilot lines up on it. *Amended the same day*: `Level10Test` had the plan's guns open
  it about 215 px down with the crate caught 4.8 s later, but it started firing only as the hatch
  entered, so its first shots still had the screen to climb; a game capture with the plan's fit and
  Rook (Autocannon 2), lined up from t≈147 with the fire held, opened it at t≈150.8 about 150 px
  down, and the crate, 6 s at 40 px/s (240 px), ran out at y≈370, above the start line (y≈444): the
  secret counted, the 160 credits did not. The fire held, the plan's fit opens it about 142 px down
  (Rook's level makes no difference) and every gun at its top level about 79 px down, and only a
  crate that drifts the whole screen is safe, so the level's data sets **`crate_seconds: 12`**
  (a level key, default the player's 6 s; only Level 10 sets it, earlier levels unchanged): from
  the top edge the crate reaches the ship's collection radius on its start line in 10.6 s; from
  142 px down it is caught 6.65 s later, from 79 px 8.2 s. A faster drift was rejected (it would
  change every pickup's look and catch); the hatch's HP is kept. `Level10Test` now holds the fire
  from 10 s before the hatch enters with those three fits and checks the top-edge bound. The
  autopilot (9 seeds): easy and medium open the hatch and catch the crate on every seed, hard opens
  it on 3 of 9 and catches every crate it opens (unchanged: on hard it is busy behind it). The secret still counts as found
  when the hatch opens, as in every level and the scoring rule ("secrets found"); only the crate's
  credits need the catch. **The rear-warning banner** covered Lifeline One's station; with an air
  escort it sits above the band. **A home shuttle's bar** read like a lost one's; it is now full in
  pale mint under a green glow ([HUD](../../../ui/hud/README.md)). A passive pilot on medium no
  longer loses all four before the lance (`Level10Test` checks the fail rule on hard).
- 2026-10-08: **Hard keeps 1 of 4** (user): one shuttle home clears the level, and hard is meant
  to be hard, so hard's numbers stay as built (armour 90, 7-shot fans, the t=170 pair).
- 2026-10-08: Concept round 32 closed (user): the backdrop with the ferry hatch, the briefing images
  and the Wraith's and Mote Swarm's art approved as final, so `art: final`; the evacuation shuttle
  **a** (lifting body; b, the heavy lifter, rejected); the scripted loss's look **b** "Iris column"
  (user pick; a's thorn spear moved to `concept/rejected/`), written into the game by
  `tools/art/lance_l10.py` (no `lance-flash`, so `LossLooks` draws the column centred on the hit);
  the sounds decloak **b**, swarm **b** and lance **a** (user picks; produced into `assets/sfx/`, the
  lance still started at t 116.8 for its impact 1.2 s in); the Lifeline voices **b** and **b**. The
  captures, the voiced lines, the texts, the part D numbers and decisions and every build choice
  accepted as built, among them sections 1–3's night-navy (21a) and a near-dead shuttle's bar in the
  climb-out (21o); the texts approved, so `design: approved`.
