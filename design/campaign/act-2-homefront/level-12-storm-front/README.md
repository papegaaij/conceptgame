---
title: Level 12 – Storm Front
design: review
implementation: in-progress
art: proposed
depends-on: [../../../enemies/air/lamprey, ../../../enemies/air/whirl-seed, ../../../player/wingmen, ../../../player/weapons/proximity-mines]
updated: 2026-10-12
---

# Level 12 – Storm Front

## Summary

The first weather level. A storm that formed in six hours sits across the convoy lanes off the
Azores, and Vrell flyers use it as cover. Rain, lightning and wind gusts cut visibility and push
the Stormhawk sideways, and contacts come from every side. The level introduces the
[Lamprey](../../../enemies/air/lamprey/README.md), which latches on and drains the shield, and
brings back [Whirl Seed](../../../enemies/air/whirl-seed/README.md) clusters carried on the wind.
In the eye of the storm Dr. Varga realises the Vrell are *herding* it. The heavy atmosphere is a
single short peak at the storm wall, inside the motion budget. About 3 minutes 25 seconds; no
boss.

**Status (M5 part F, step 4).** This document states the design decided on 2026-10-09 (user
decisions F1–F9 of the part's gap check and its stated defaults, see *Decisions*) and is the
ticket for the later steps: the auditions (step 2), the simulation (step 3), the content and the
level's [data.yaml](data.yaml) (step 4, done: the marked tables below are rendered from it by
`tools/sync_tables.py`), the backdrop and props (step 5: the data's `backdrop` block is a stand-in
with Level 11's sea until then), the game, sounds and voices (step 6), the capture (step 7) and
concept round 34 (step 8).

## Briefing

Two pages, as Levels 08–11, each **one screen** (`BriefingLayoutTest`): Okafor's mission, then
Varga's sondes. Draft texts, for review in concept round 34:

<!-- data: briefing -->
> **Commander Okafor:** "Lancer, Aegis Actual. A storm system has formed off the Azores, and
> it's moving onto the convoy lanes faster than any storm should. Vrell flyers are using it as
> cover. Clear the storm front so the next convoy can follow. Expect contacts from every side."
>
> **Dr. Varga:** "The pressure readings make no sense. Three CDF weather sondes are still
> transmitting inside the front. Fly through them for me and I can tell you why."
<!-- /data -->

*Images* (production art straight away, user decision F8 = a; [briefing images](../../../ui/briefing/README.md)):
Okafor's page `level-12-storm-front` (the storm system from above over the convoy lanes off the
Azores: the spiral of cloud, the eye, the Stormhawk's route through it), Varga's
`level-12-sonde-scan` (a CDF weather sonde blinking in the rain beside a pressure plot that makes
no sense), rendered by `tools/art/briefing_images.py` (step 5); the level's data names them as each
page's `image`.

*Hangar teaser* (shop screen before L12; Rook, `LT. K. TANAKA / AEGIS TWO`, not voiced, as Levels
10–11):

<!-- data: teaser -->
> **Rook:** "A storm that blew up in six hours, and things in it that bite. Homing missiles,
> Lancer: you won't see much in there."
<!-- /data -->

## Threat profile

The level's data's `threat_profile`. The directions are counted by each wave's `from` (the intel's
rule). The draft's split (front 61 · sides 22 · rear 17 %) was the target; the rear falls short (6 %)
because only the Wraiths' ambushes and the Lampreys can come from the rear in the built simulation
(a Skitter `stream` from the rear is not flown), and the popcorn the pacing rule needs comes from the
front and the sides (step 4, pending the user's review in the combined round).

<!-- data: threat-profile -->
| Field | Value |
|---|---|
| Dominant layers | `air`, `low-air` (Spore Bombers and the canister under the cloud banks) |
| Attack directions | front 70% · sides 24% · rear 6% |
| Density | 4 |
| Recommended traits | `homing`, `spread`; `side` / `rear` / `area` help against a latched Lamprey |
| Hazards | Wind gusts (sideways push), low visibility (rain, cloud banks), lightning flashes |
| Boss / mid-boss | none (finale: breakout from the storm, attacks from all sides) |
| Sensor-suite detail | **none**: ocean storm, `air` + `low-air`, front. **L1**: + the directions, density 4, hazards "wind, low visibility", the objective `SONDES 3`. **L2**: + Lamprey (new), Whirl Seed, Needler, Wraith, Mantis, Spore Bomber, Skitter portraits. **L3**: + `homing` and `spread` highlighted; the timeline strip marks the storm wall (125–150 s) and the eye (150–175 s); 1 secret. |
<!-- /data -->

*Varga's intel line* per sensor-suite level (the data's `varga`; draft texts for round 34, fitting
the intel panel is checked by `IntelPanelLayoutTest`):

| Sensor suite | Line |
|---|---|
| none | "A storm off the Azores, Lancer, and it grew in hours. Air contacts inside it." |
| L1 | "From every side. Gusts will push you sideways: watch for the wind arrows." |
| L2 | "New: an eel that drains your shield. Shake it off." |
| L3 | "Homing sees through the rain. Spread for the seeds." |

## Objective

- **Primary** `reach-end`: survive to the end of the scroll.
- **Secondary** "Sondes" (all or nothing): fly through all three CDF weather sondes. It is
  **met** at the third, **failed** at once when a sonde leaves the bottom edge uncollected. Pays
  **75** credits in Act 1 terms (120 in Act 2, the credit factor 1.6); the typical haul counts it at
  the economy's secondary share. The new secondary kind `collect`
  ([schemas](../../../tech/architecture/README.md#data-file-schemas)); the HUD's `SONDES n / 3`
  tracker ([HUD](../../../ui/hud/README.md#left-panel-mission)). Its met line unlocks Varga's
  follow-up (*Radio chatter*).

### The sondes

User decision **F7 = b**: blinking buoys **on the play plane**, not on the sea. Each enters at the
top edge at its `t` and `x` and drifts straight down at **60 px/s** (≈ 9 s on the 540 px screen), its x
held; the wind does not push it, it cannot be shot and nothing collides with it. The ship collects
one when its centre comes within **36 px** of the sonde's; the magnet does not pull sondes, and
Rook cannot collect them (defaults). Each collection plays a chime, counts on the tracker and is a
simulation event (`SONDE_COLLECTED`). Three sondes: t≈60 in the rain wall, t≈110 in the lightning
field, t≈165 in the eye; their x lie off the waves' main lanes so that taking one costs a move,
placed in step 4.

## Layout

Ground scroll speed **140 px/s** (the chosen storm scene). Total ≈ 205 s ≈ 28,700 px. Over water
throughout: the data sets **`water: true`** (the [Level 11](../level-11-atlantic-convoy/README.md#layout)
flag: kills on the play field play the water burst; no `sub` units in this level).

**Motion budget** (see [art direction](../../../art-direction/README.md)): the sea swell and
the rain are the only two strongly animated background elements. Wind streaks and whitecaps
stay faint, the scud only sways, and no element moves more than 2 px per frame. Lightning is a
single short flash (≤ 0.1 s, softened by the flash-reduction option). In the storm-wall peak
the rain gets denser while the swell drops in contrast, so there are still only two strong
elements. In the eye the rain stops and the cloud wall turns slowly.

<!-- data: level-sections -->
| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Squall Line | 0–25 | 0–3,500 | 140 | light | `deep`: dark grey swell. `low-air`: the first scud banks at the edges. `high-air`: rain starts at t=10 (faint, motion-blurred streaks). One demonstration gust at t=16. | Weather introduced over light popcorn; the gust telegraph learned. |
| 2. Rain Wall | 25–75 | 3,500–10,500 | 140 | medium | `deep`: heaving swell, sparse torn crests. `low-air`: scud banks (~20–25%), opaque, below the play plane. `high-air`: steady rain (≤ 40%). The first sonde blinks in the rain at t≈60. | Lamprey introduction; seeds on the wind. |
| 3. Lightning Field | 75–125 | 10,500–17,500 | 140 | medium | `low-air`: as section 2, plus lightning every 6–10 s; each flash lights the cloud banks from inside for 0.5 s and shows what flies under them. The drifting canister at t≈99; the second sonde at t≈110. | Low visibility; attacks from the sides and rear. |
| 4. Storm Wall | 125–150 | 17,500–21,000 | 140 | medium, heavy peak 130–145 | `low-air`: banks at 30–40% leaving the centre lane readable. `high-air`: dense rain (≤ 40%). A steady 20 px/s drift to the right plus gusts. | The single heavy peak: dense whirl clusters and a Lamprey swarm. |
| 5. The Eye | 150–175 | 21,000–24,500 | 140 | clear | `deep`: calm dark sea with a shaft of pale light. `far`: the storm's cloud wall all round, turning slowly clockwise (≈ 3°/s). No rain, no gusts. The armour patch at t≈152; the third sonde at t≈165. | Calm; Varga sees the Needlers circling with the storm. |
| 6. Breakout | 175–205 | 24,500–28,700 | 140 | medium, light peak 198–205 | `high-air`: back through the wall into rain, which thins toward the end; the sky lightens at the top edge. | Last waves from all sides, then out of the storm. |
<!-- /data -->

## Weather

The level's `weather` block ([schemas](../../../tech/architecture/README.md#data-file-schemas));
the first level with one. The eye (section 5) has none of it: no rain, gusts or lightning.

- **Wind gusts** (stated defaults): every gust blows **from the left** (the storm's wind, matching
  the seeds "on the wind, left side" and the wall's drift to the right). A gust pushes at
  **40 px/s** (easy 25, hard 55) for **2.5 s** including a 0.25 s ramp up and down, and is
  **telegraphed 1.0 s** ahead (easy 1.5 s): the wind streaks brighten and an arrow shows at the
  upwind edge, drawn unlike an edge warning (a weather look, step 5). Gusts at t=16 (the
  demonstration), 52, 90, 118 and 182, and every 6 s in the storm wall (126, 132, 138, 144) on top
  of its steady drift; hard adds t=34 and t=72.
- **The wall's drift**: a steady **20 px/s** to the right through section 4 (125–150 s, the same
  0.25 s ramps), none on easy.
- **What the wind moves** (stated defaults): the push **adds to positions, not velocities**, so
  steering is unchanged and the push is exactly the gust's px/s; the ship does not bank from it
  and the play field's edges clamp as now. It moves the ship, Rook (as the player:
  [wingmen](../../../player/wingmen/README.md#act-2-hazards)), Lampreys (chasing, flying on,
  stunned or fleeing; a latched one moves with the ship) and Whirl Seeds (their spiral's centre
  too); **not** enemy or player shots, mines, spores, pickups, the sondes or the canister: bullets
  stay readable.
- **Lightning**: authored flash times in the data (stated default), every 6–10 s in section 3 with
  one at t=87 (the side swarm's entry) and two while the canister is on the screen, a few in the
  wall and the breakout. Draft schedule, tuned in step 4 against the canister's window: 78, 87, 93,
  99.5, 103.5, 111, 117, 123; the wall 129, 135, 141, 147; the breakout 180, 189. Each flash is a
  sky flash of at most 0.1 s and lights the banks from inside for **0.5 s**; the flash-reduction
  option softens it (0.35 strength). Never more than 3 flashes a second. The simulation only
  raises an event per flash (`LIGHTNING`), for the thunder; a flash changes nothing in play.
- **Cloud banks hide `low-air` bodies** (user decision **F1 = a**; a level flag, L12 only): units
  on `low-air` (the Spore Bombers) and the canister are drawn under the backdrop's bank tiles with
  a faint glow on top; a flash makes the banks see-through for its 0.5 s. Bullets and spores are
  never hidden (they rise to the play plane), and `air` units always fly above the banks: the
  t=88 swarm "out of the cloud banks" enters at the side edges with the t=87 flash timed to its
  entry. Draw order only: targeting (homing, Rook) is unchanged.
- **Rain** on `high-air`: faint motion-blurred streaks from t=10, steady in sections 2–3, dense in
  the wall (≤ 40 % coverage), none in the eye, back at the breakout and thinning toward t=200;
  presentation only (the data's density keys by time, step 4).

## Waves

Enemy definitions are linked per row; formation names come from the
[formation vocabulary](../../../enemies/README.md#formation-vocabulary). Of the returning units the
`medium` ones (Spore Bomber ×2.28, Mantis ×1.40, Wraith ×1.11 at reference DPS 73) get the
[act HP factor](../../../enemies/README.md#balancing-basis); the `tiny` and `small` ones (Whirl
Seed, Needler, Skitter) keep their HP; the Lamprey is new here (none). HP values live in the enemy
specs only.

- **Lampreys** fly the [Lamprey](../../../enemies/air/lamprey/README.md#behaviour) chase from their
  entry. A **`swarm`** of Lampreys is a swarm of chasers (user decision **F9 = a**): the swarm's
  cloud at its entry edge (front, sides or rear), each member starting its chase after a seeded
  0–0.6 s delay, flying straight in until then; no flocking. A Lamprey **`stream` from the rear**
  enters at the bottom edge, one at a time.
- **Whirl clusters "on the wind"** (t=46, 140): the release point lies just outside the left edge
  (with its edge warning), and its spiral's centre drifts with the wind as the seeds do.
- **The Needler circle in the eye** (t=158) orbits **clockwise** for 8 s before breaking (a wave
  `hold`; the orbit's sense set by the wave, step 3 checks the built sense).
- **Density and pacing** (stated defaults): at least **50 enemies a minute** at medium over the
  level (`DensityTest`) and no empty screen over **3 s** of real time (`PacingTest`, every
  difficulty). The draft's 111 units over 205 s (≈ 33 a minute) are too few: the table keeps the
  set pieces and step 4 filled the gaps with **popcorn from L12's own roster**: Skitter
  streams of 4 (3 just before a quiet moment) and snakes of 6, Whirl clusters of 6 and Needler lines
  of 3, a wave every 3–4 s from t=7, from the front and the sides. **234 units at medium (≈ 70 a
  minute)**, more than the step-1 estimate of 175–185: the plan's two Micro-missile Pods kill the
  waves fast, and fewer popcorn waves left empty screens over 3 s (`PacingTest`; step 4, pending the
  user's review). No rear Skitters: a `stream` from the rear is flown only by chasers.
  The **t=32 introduction stays alone** on the screen: no other wave enters from t=32 until its
  three Lampreys are gone or 6 s have passed, and the popcorn before it leaves the screen first.
  The eye keeps its calm with the least popcorn the 3 s rule allows. Edge warnings 3 s ahead for
  every side and rear wave, as always.

<!-- data: waves -->
| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 7 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | The first targets in the squall |
| 11 | 1 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | |
| 15 | 1 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | Through the demonstration gust at t=16 |
| 18.5 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | left side | |
| 23 | 1 | whirl cluster | [Whirl Seed](../../../enemies/air/whirl-seed/README.md) | 6 | front | |
| 26.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | Gone before the Lamprey introduction |
| 32 | 2 | stream | [Lamprey](../../../enemies/air/lamprey/README.md) | 3 | front | **New.** One at a time, 1.5 s apart, nothing else on screen; the first latch shows the shake-off prompt |
| 38 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | 6 s after the introduction |
| 40 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 43 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | right side | |
| 46 | 2 | whirl cluster | [Whirl Seed](../../../enemies/air/whirl-seed/README.md) | 6 | left side | Released just outside the left edge; the wind bends their spiral to the right |
| 50 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | |
| 54 | 2 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 58 | 2 | swarm | [Lamprey](../../../enemies/air/lamprey/README.md) | 4 | front | |
| 61 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | right side | |
| 64 | 2 | line abreast | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 2 | front | Under the scud banks (`low-air`); spores rise to the play plane |
| 70 | 2 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 73 | 2 | whirl cluster | [Whirl Seed](../../../enemies/air/whirl-seed/README.md) | 6 | front | |
| 76 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | |
| 80 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 10 | front (alternating edges) | |
| 84 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 88 | 3 | swarm | [Lamprey](../../../enemies/air/lamprey/README.md) | 5 | sides | Entering with the lightning flash at t=87 |
| 92 | 3 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | |
| 96 | 3 | whirl cluster | [Whirl Seed](../../../enemies/air/whirl-seed/README.md) | 6 | front | |
| 98.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | right side | Beside the drifting canister |
| 104 | 3 | rear ambush | [Wraith](../../../enemies/air/wraith/README.md) | 2 | rear | The first one drops an overdrive |
| 108 | 3 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | |
| 112 | 3 | stream | [Lamprey](../../../enemies/air/lamprey/README.md) | 4 | rear | |
| 117 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 120 | 3 | pincer | [Needler](../../../enemies/air/needler/README.md) | 4 | sides | 2 per side, hold 4 s |
| 122 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | |
| 124 | 3 | pincer | [Mantis](../../../enemies/air/mantis/README.md) | 2 | sides | Laser sweeps across the rain |
| 128 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | sides (alternating edges) | |
| 132 | 4 | whirl cluster | [Whirl Seed](../../../enemies/air/whirl-seed/README.md) | 6 | front | |
| 136 | 4 | swarm | [Lamprey](../../../enemies/air/lamprey/README.md) | 6 | front | Out of the heavy rain; easy 4 |
| 140 | 4 | whirl cluster | [Whirl Seed](../../../enemies/air/whirl-seed/README.md) | 6 | left side | On the wind with the wall's drift |
| 143 | 4 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | |
| 146 | 4 | line abreast | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 2 | front | `low-air` |
| 150 | 5 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | Into the eye |
| 155 | 5 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 158 | 5 | circle | [Needler](../../../enemies/air/needler/README.md) | 8 | front | Orbits the eye's centre clockwise with the storm for 8 s before breaking; the leader drops an overdrive |
| 162.5 | 5 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | As the circle breaks |
| 166 | 5 | rear ambush | [Wraith](../../../enemies/air/wraith/README.md) | 3 | rear | |
| 169.5 | 5 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 5 | sides (alternating edges) | |
| 170 | 5 | pincer | [Mantis](../../../enemies/air/mantis/README.md) | 2 | sides | **Hard only** |
| 176 | 6 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 8 | front (left) | |
| 180 | 6 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | sides (alternating edges) | |
| 183 | 6 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 186 | 6 | swarm | [Lamprey](../../../enemies/air/lamprey/README.md) | 5 | rear | |
| 190 | 6 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | |
| 194 | 6 | whirl cluster | [Whirl Seed](../../../enemies/air/whirl-seed/README.md) | 6 | front | |
| 196 | 6 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 199.5 | 6 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | sides (alternating edges) | The last wave |

Totals: Skitter 111 · Needler 43 · Whirl Seed 42 · Lamprey 27 · Spore Bomber 4 · Wraith 5 · Mantis 2.
<!-- /data -->

The set pieces at medium are the draft's table: Lamprey 27 · Whirl Seed 30 · Needler 25 · Wraith 5 ·
Spore Bomber 4 · Skitter 18 · Mantis 2 (hard 4), 111 units in 23 waves; the popcorn comes on top (the totals
above).

## Ground targets

No enemies on the ground: L12 is an air level over open sea. The one target is the drifting canister's
trigger on `low-air` (*Secrets and pickups*).

<!-- data: ground-targets -->
| Section | Target | Effect |
|---|---|---|
| 3 | A CDF supply canister drifting on `low-air` inside a cloud bank on the right (trigger, t≈99): drawn under the bank with a faint glint, fully seen in the flashes at t=99.5 and 103.5 | 3 hits (hard 5) by anything that reaches `low-air` break it open as the hidden crate |
<!-- /data -->

## Hazards

- **Wind gusts and the wall's drift**: see *Weather*.
- **Lampreys latching**: the [Lamprey](../../../enemies/air/lamprey/README.md#behaviour)'s latch,
  drain and shake-off. Every latch of at least 0.3 s shows the **shake-off prompt** (an event
  prompt, `SHAKE OFF` · `LEFT-RIGHT`, in the play field below the ship, at most 6 s each time)
  until the attempt's first shake-off; the attempt's first latch plays Rook's line.
- **Low visibility**: the banks hide `low-air` bodies, never bullets (*Weather*).
- **Lightning**: a flash only, no damage. It obeys the flash-reduction option.

## Secrets and pickups

- **Drifting canister** (enters at t≈100, user decision **F2 = a**): a CDF supply canister drifting
  on `low-air` inside a cloud bank on the right, slowly across and down the screen (not pushed by
  the wind). It is drawn under the bank with a faint glint and fully visible in the two flashes
  authored while it is on the screen, and it is **always hittable** by anything that reaches
  `low-air`. It counts **hits**, not HP: **3** (hard 5, a trigger's `hard: {hits}` as Level 11's
  pod) break it open as a crate worth **130** credits in Act 1 terms (208 in Act 2). The Salvage
  scanner's glint shows through the bank. Its line, Varga: "*A CDF supply canister. Lucky find in
  this soup.*" (draft).
- **Overdrive** ×2: from the first Wraith of the t=104 ambush and the t=158 Needler circle's
  leader.
- **Armour patch**: a **placed pickup** floating in the eye, entering at the top edge at t≈152
  (stated default: a pickup at `t` and `x`, not dropped by a wave).
- **Shield cells** drop more often in this level, to offset the Lamprey drain: a level **drop
  factor of 1.5** divides each enemy's drop counter (the Needler's every 4th kill becomes every 3rd;
  rounded to the nearest, at least 1). Lampreys drop nothing.

## Radio chatter

Spoken radio ([voice](../../../audio/voice/README.md)), as Levels 08–11, with the cast speakers
(Okafor, Varga, Rook, the Choir's stage sound; no new speaker). The draft's script is **retimed to
the HUD's 1 s rule** (`RadioTimelineTest`, stated default): a timed line holds the radio for the
longer of its voice and its text (its typing at the default 30 characters a second, then 5 s for
the last page plus 3 s for each page before it), then 0.4 s of silence, and starts after the one
before it has closed. The draft clashed in the eye (t=152/156/158/159/164); step 1's retiming
(150.5, 156, 164.5, 170, 175.5) left out the typing, so step 4 retimed it with the text timeline
(`RadioTimelineTest`, pending the user's review): Rook at t=149.5 as the wall opens, Varga at 157
as the circle forms, the Choir at 169.5, Okafor's "Say again, Doctor?" at 176 and Varga's
"herding" line at 182.5, into the breakout (the voiced check is step 6's). Rook's t=102 "Six o'clock, Lancer!" is **dropped**: his rear bark says
it 1.5 s before the t=104 ambush, as Level 10's t=50. His t=44 line stays: it suppresses the side
bark of the t=46 cluster by the bark spacing ([wingmen](../../../player/wingmen/README.md#radio-barks)).
**Events**: `first-latch` (new: the attempt's first latch; Rook's line, which counts as a scripted
Rook line for the bark spacing) and `secondary-objective`. **Rook's lines** carry
`requires: escort`.

<!-- data: radio -->
| Trigger | Speaker | Line |
|---|---|---|
| t=2 | Okafor | "Weather service calls it the worst Atlantic storm in forty years. It formed in six hours. Stay together." |
| t=14 | Rook | "Feel that? The wind's trying to take the stick off me." |
| t=30 | Varga | "Eel-shaped contacts. They latch on and drain your shield. Shake hard to throw them off." |
| The attempt's first latch (`first-latch`) | Rook | "You've got a passenger! Shake it off!" |
| t=44 | Rook | "Seeds on the wind, left side!" |
| t=58 (first sonde in view) | Varga | "There's a CDF sonde out there. Fly through it and I can read its data." |
| t=86 | Varga | "Lightning shows what's in the clouds. Watch the flashes." |
| t=128 | Okafor | "Storm wall ahead. Instruments will be useless in there. Fly by eye." |
| t=149.5 | Rook | "Blue sky. I'd almost forgotten what it looks like." |
| t=157 | Varga | "Lancer, look at them. They're circling with the wind. They aren't caught in this storm. They're steering it." |
| t=169.5 | The Choir (distorted) | "[the Choir sings]" |
| t=176 | Okafor | "Say again, Doctor?" |
| t=182.5 | Varga | "They're herding the weather, Commander. They understand our climate better than we do." |
| t=200 | Rook | "Next time, I'm bringing an umbrella." |
| Level end | Okafor | "You're through. The storm is breaking up behind you, as if someone let it go." |
| Secondary objective met | Varga | "Three sondes, full data. They fed the storm, Commander. I'll have the pattern by morning." |
<!-- /data -->

## Boss / mid-boss

None. The breakout (section 6) is the finale.

## Music & ambience

The Act 2 B theme **"Firestorm"** (track 7 in the [track list](../../../audio/music/README.md#track-list);
no separate storm variant), user decision **F5 = a**: the storm ambience alone in the squall
(section 1), the **base stem** from section 2, the **full mix** in the storm wall (section 4), the
**base stem at −6 dB in the eye** (section 5, calm, the Choir's stage sound on top), the full mix
again for the breakout (section 6). The switches by section use the music data's `stems`; the
eye's −6 dB is the new per-section level ([music](../../../audio/music/README.md#intensity-layers)).

Ambience (user decision **F6 = a**): a **rain-only storm bed** cut from the same CC0 source as the
chosen [ocean storm ambience](../../../audio/sfx/README.md#ambience-per-setting) (a new file, the
chosen one kept), in sections 1–4 and 6, crossfading to the [ocean ambience](../../../audio/sfx/README.md#ambience-per-setting)
in the eye and back at the breakout (Level 10's ambience change by section); **thunder one-shots**
0.3–1.5 s after each flash (2–3 variants, the delay and variant fixed by the flash's index, no
random draw). New sounds (thunder, gust, latch, drain, shake-off, sonde chime) are in
[sfx](../../../audio/sfx/README.md#enemies). The Proximity Mines unlock here: their drop and arm
sounds are checked in the capture.

## Credit budget

Budget(12) = 700 × 1.07¹¹ ≈ **1,473**, the typical haul's target
([economy](../../../systems/economy/README.md#per-level-budget)), not a perfect collection. **The
data keeps Act 1 terms** (stated default): the stat-block bounties (Lamprey 12, Whirl Seed 3,
Needler 12, Wraith 30, Spore Bomber 25, Skitter 5, Mantis 30), the canister's crate 130 and the
secondary 75; the code pays each × the act factor 1.6 (bounties also × the level's
**`bounty_scale`** **0.75**, fitted by `TypicalHaulTest`: the typical haul 1,476, +0.2 %). Spores are
not budgeted (their number depends on how long the bombers live). The step-1 estimate (≈ 0.9 at 50
a minute) assumed less popcorn; at 70 a minute the scale is lower. The draft's 2,105 was the old
perfect-run curve with Act-2-scaled amounts (Lamprey 19, canister 210).

<!-- data: credit-budget -->
| Source | Perfect run | Typical haul |
|---|---|---|
| Kills: Skitter 111 × 5 + Needler 43 × 12 + Whirl Seed 42 × 3 + Lamprey 27 × 12 + Spore Bomber 4 × 25 + Wraith 5 × 30 + Mantis 2 × 30 | 2,186 | 1,312 |
| Secret: drifting canister (hidden crate, 9% of budget) | 208 | 104 |
| Secondary: fly through all three sondes | 120 | 60 |
| **Total** (bounty scale 0.75) | **2,514** | **1,476** |
| Budget(n) = the typical haul's target; typical +0 %, perfect 1.71 × budget | | 1,473 |
<!-- /data -->

**Balance plan** (stated default): the L12 visit as written (sells both wing pods, buys two
Micro-missile Pods, an Airstrike charge) plus `fit: [[escort, autocannon]]`: Rook's Mortar from
Level 11 has nothing to hit here ([balance plan](../../../player/balance-plan.yaml)). `BalanceTest`
checks the plan's rear gun against a latched Lamprey (3 HP at ×2: 0.15 s of the 1.2 s shake-off)
and the drain on the autopilot (latches, shake-offs, shield drained per latch, armour lost per
difficulty). Its rear check of the returning Wraith (18 HP with the act HP factor against the Tail
Gun L1's 10 DPS: 1.8 s of the 2.9 s flash and hold, 0.62 where 0.6 is the limit) is reported as
pending the user's review; the plan upgrades the rear only at L14.

## Difficulty notes

- **Easy**: gusts push at 25 px/s with a 1.5 s telegraph; the storm wall has no steady drift;
  the t=136 Lamprey swarm has 4.
- **Hard**: gusts push at 55 px/s and also come at t=34 and t=72; a Mantis pincer joins the eye
  at t=170; the canister needs five hits; two Lampreys can latch at once, draining 12/s (the
  [Lamprey](../../../enemies/air/lamprey/README.md)'s hooks).

## Concept art

The look comes from the chosen storm scene in
[art direction](../../../art-direction/README.md):
[scene-storm-r10-a.png](../../../art-direction/concept/scene-storm-r10-a.png) and its
[scroll loop](../../../art-direction/concept/scene-storm-r10-a.gif), which shows the storm-wall
peak; the other sections run lighter. User decision **F8 = a**: the backdrop, props, briefing
images and intel portrait go straight to production; concept round 34 shows a/b only for **the
eye's look** (the turning cloud wall and the light shaft) and **the latch** (the Lamprey on the
hull with the drain flicker), and the sounds a/b for thunder, gust, latch and drain (the sonde
chime one version).

The eye's two looks come from `tools/art/eye_l12.py`, whose `wall_ring(look)` and `shaft(look)` step 5's
backdrop generator imports for the chosen one; briefs in [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/eye-r34-a.png](concept/eye-r34-a.png) | The eye a, "billowing wall, cool shaft": a ring of round billows lit radially from the eye (bright silver inner faces, the storm dark behind), a gentle spiral; a cool silver-white pool of light up and left with a soft haze column; the wall turned 30° after 10 s; the ring and the shaft alone (sea: a stand-in) | proposed |
| [concept/eye-r34-a.gif](concept/eye-r34-a.gif) | The eye a, 6 s in real time: the wall turning clockwise at 3°/s, the calm sea scrolling under it | proposed |
| [concept/eye-r34-b.png](concept/eye-r34-b.png) | The eye b, "banded spiral wall, warm shaft": a darker wall of stretched spiral bands with two thin feeder arms curling into the eye, pale-gold inner faces; a broad warm pool in the centre with slow sun glints on the swell | proposed |
| [concept/eye-r34-b.gif](concept/eye-r34-b.gif) | The eye b, 6 s in real time: the wall turning, the glints fading in and out, the sea scrolling | proposed |
| [concept/backdrop-final-r34-a.png](concept/backdrop-final-r34-a.png), [.gif](concept/backdrop-final-r34-a.gif) | Production backdrop straight from scene-storm-r10-a on Level 11's ocean kit (`tools/art/backdrop_l12.py`, notes in [concept/prompts.md](concept/prompts.md#backdrop-final-r34-a); its block for the level data is [concept/backdrop-proposal.yaml](concept/backdrop-proposal.yaml)): the storm sea in two layers (swell on `deep`, chop with torn crests and wind streaks on `ground`), the eye's calm chop with the two seam pieces, opaque scud banks on `low-air` (light ~12 % at the edges, medium ~22 %, heavy ~35 % with the centre lane clear), two tall banks hiding the drifting canister, the eye's wall ring (look a, turned clockwise at 3°/s by the game) and shaft, the breakout's light; composites with stand-ins for the rain, the flash and its 0.5 s reveal, the low-air bodies under the banks with their glow, the gust telegraph and a sonde; the GIF the canister's two flashes and the way into the eye | proposed |
| [concept/l12-props-final-r34-a.png](concept/l12-props-final-r34-a.png), [.gif](concept/l12-props-final-r34-a.gif) | Final props and weather looks (`tools/art/l12_props.py`, [notes](concept/prompts.md#l12-props-final-r34-a)): the weather sonde (blink loop, collect pop), the supply canister under its drogue chute (intact, hit, spent) with its beacon glow, rain streaks, the gust arrow and wind streaks, lightning bolts, a cloud lit from inside, a hidden body's glow; the GIF a canister under a bank, a flash, a gust telegraph and a sonde collected | proposed |
| [concept/level-12-capture-final-r34-a.png](concept/level-12-capture-final-r34-a.png), [.mp4](concept/level-12-capture-final-r34-a.mp4) | Capture of the game after the fixes of 2026-10-11 (medium, the balance plan's L12 fit, no `--invulnerable` so the Lampreys can latch): 226 / 234 kills, sondes 3 / 3, the canister seen and broken in the t 99.5 flash, three latches (the `SHAKE OFF` · `LEFT-RIGHT` prompt below the ship on the second, shaken off; none on the third), the 2× gust arrow clear of the edge warnings, A, 3,024 credits; fifteen frames and the whole run with sound ([recipe](concept/prompts.md#level-12-capture-final-r34-a)) | proposed |
| [concept/level-12-readability-r34-a.png](concept/level-12-readability-r34-a.png) | 1:1 and 2× crops: rain, gust telegraph, a Spore Bomber under a bank, the canister's glint and flash, the latch on each flank, a stunned Lamprey, a sonde and its pop, the eye's ring and shaft, the breakout light, the `SONDES` tracker, `SHIELD DRAIN`, the shake-off prompt in the play field, the debrief's `SONDES` row ([notes](concept/prompts.md#level-12-readability-r34-a)) | proposed |
| [concept/level-12-capture-mines-r34-a.png](concept/level-12-capture-mines-r34-a.png), [.mp4](concept/level-12-capture-mines-r34-a.mp4) | Supplementary clip of the Proximity Mines in play (step CF; 24 s with sound, the level time burnt in, and a sheet): the plan's fit with the mines in the rear slot through the left flank wave, then a control run with the music and radio off; the sheet's plot of the mix around a drop: the arm chirp is heard, the drop is masked by the front gun (pending the user's review; [recipe](concept/prompts.md#level-12-capture-mines-r34-a)) | proposed |

## Implementation

- [x] Level data `data.yaml` with every block below and this README's tables turned into marked
      tables rendered from it; `PartFLevelKeysTest`, `Level12Test` (M5 part F, step 4)
- [ ] Scroll timeline, sections, atmosphere and layer content as in *Layout*; heavy only from 130
      to 145 s; `water: true` (M5 part F, steps 4 and 5; step 4's data done, the backdrop a stand-in
      with Level 11's sea until step 5)
- [ ] Production backdrop: six sections (squall, rain wall, lightning field, storm wall, the eye
      with its turning cloud wall and light shaft, breakout with the sky lightening), the bank tile
      sets, swell and whitecaps; the motion budget (M5 part F, step 5; the eye a/b in round 34)
- [x] Weather in the simulation: the gust schedule with telegraph, push and ramps, the wall's
      drift, easy/hard values, applied to the ship, Rook, Lampreys and Whirl Seeds only; exactly zero
      in levels without weather; in the state hash only in a level with weather (`WindTest`) (M5
      part F, step 3)
- [x] Weather looks: rain by time, lightning flashes (≤ 0.1 s, the 0.5 s bank light, flash
      reduction, < 3 a second), banks hiding `low-air` bodies with a glow (F1 = a), the gust
      telegraph's streaks and arrow, the eye's turning wall, all on the simulation's ticks (M5 part
      F, step 6: `WeatherLooks` in `LevelRenderer`; the canister under its bank with its beacon's
      glint, the sondes and their collect pop, the Lamprey's latched, stunned and death looks;
      `Level12LooksTest`)
- [x] Lampreys: chase, latch, drain, shake-off, stun and flee as in the
      [Lamprey](../../../enemies/air/lamprey/README.md) spec; the chaser `swarm` (F9 = a) and the
      rear `stream` (M5 part F, step 3)
- [x] Wave script: the set pieces with the popcorn; at least 50 enemies a minute at medium
      (`DensityTest`); no empty screen over 3 s on every difficulty (`PacingTest`); the t=32
      introduction alone; whirl release points on the wind; the clockwise circle (M5 part F,
      steps 3 and 4; step 3's simulation side built: wind-borne release points, `clockwise`)
- [x] The sondes (F7 = b) and the `collect` secondary: 75 in Act 1 terms, the `SONDES n / 3`
      tracker, the debrief row, `SONDE_COLLECTED` (`SondeTest`) (M5 part F, steps 3, 4 and 6: the
      sondes at t 60, 110 and 165, the autopilot taking them; the tracker with its green flash per
      sonde and the debrief's `SONDES n / 3`, `PartFHudTest`, `LevelScreenPartFTest`)
- [x] The drifting canister (F2 = a): a drifting `low-air` trigger of 3 hits (hard 5) under a bank,
      always hittable, a 130 crate; two flashes while it is on the screen (M5 part F, steps 3 and 4;
      step 3's drifting trigger built)
- [x] Pickups: the drop factor 1.5, the placed armour patch in the eye, the two overdrives (M5 part
      F, steps 3 and 4; step 3's drop factor and placed pickups built)
- [x] Radio voiced, every timed line at most 1 s late (`RadioTimelineTest`, with Rook and without
      him); the events `first-latch` (with the shake-off event prompt) and `secondary-objective`;
      Rook's lines `requires: escort` (M5 part F, steps 3, 4 and 6; step 4's data built, step 6's
      voices rendered 2026-10-11: 18 lines of the cast speakers, six takes pinned, no retiming
      needed; pending the user's review in the combined round)
- [x] Threat profile, Varga's four intel lines and Rook's teaser fitting the intel panel
      (`IntelPanelLayoutTest`); the Lamprey's intel portrait (M5 part F, steps 4 and 5)
- [x] Briefing: Okafor's and Varga's pages, each one screen (`BriefingLayoutTest`), with their
      images (M5 part F, steps 4 and 5)
- [x] Typical haul at medium within ±5 % of budget(12) = 1,473 with the level's `bounty_scale`
      (`TypicalHaulTest`) (M5 part F, step 4)
- [x] Balance plan: the L12 visit refits Rook's Autocannon; the autopilot shakes off latches,
      leans against the gusts after their telegraph and takes the sondes; it flies the level to its
      end with Rook on every difficulty; `BalanceTest`'s Lamprey time to kill and drain check (M5
      part F, step 4)
- [x] Music: "Firestorm"'s base stem from section 2, the full mix in the wall, the base stem at
      −6 dB in the eye, the full mix at the breakout (F5 = a); the rain-only storm bed with the
      ocean in the eye, thunder after the flashes (F6 = a); the new sounds (M5 part F, steps 2 and 6:
      round 34's variant a played until the round closes; `Level12SoundsTest`)
- [ ] L12's units, props, rain and flash in L12's own atlas, at most 6 pages (M5 part F, step 5)
- [x] Easy/hard variations as in *Difficulty notes* (M5 part F, step 4)
- [ ] Captures for concept round 34: the level, the wall, the eye, a latch and shake-off, a flash
      with the canister, the sondes, each difficulty's autopilot outcome; the Proximity Mines' drop
      and arm sounds heard in play (M5 part F, step 7)

## Decisions

- 2026-10-01: Promoted from the Act 2 roster to a draft level document.
- 2026-10-01: Heavy atmosphere only as a 15 s peak at the storm wall; sea and rain are the two
  strong elements everywhere (motion budget, round 09 feedback).
- 2026-10-01: Open question resolved: L12 keeps "Firestorm" (Act 2 B); no separate storm variant (user decision).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-09: M5 part F (user decisions of 2026-10-09, all as recommended): **F1 = a** the cloud
  banks hide only `low-air` bodies (draw order, a level flag), air waves "out of the banks" enter
  at the edge with a flash timed to their entry; rejected: b (air units hidden in a bank's
  footprint: a coverage mask, and it breaks the layer rule) and c (banks hide nothing: loses the
  level's idea). **F2 = a** the canister is always hittable, drawn under its bank with a glint,
  fully seen in two authored flashes; rejected: b (Level 06's hit-only-while-lit rule: one or two
  0.5 s windows for 3 hits, and the flash schedule in the simulation) and c (a plain visible
  canister). **F3 = a** what hits a latched Lamprey goes by weapon
  ([Lamprey](../../../enemies/air/lamprey/README.md#behaviour)). **F4 = a** one latch per Lamprey.
  **F5 = a** the base stem at −6 dB in the eye; rejected: b (a third "eye" stem: a three-stream
  mixer and a stem render) and c (the theme fading out in the eye). **F6 = a** a rain-only bed cut
  from the same CC0 source and thunder one-shots after the flashes; rejected: b (thunder on top of
  the bed's own: muddy) and c (flashes timed to the bed's thunder: the schedule coupled to an audio
  loop). **F7 = b** the sondes on the play plane, drifting down at 60 px/s, not pulled by the
  magnet; rejected: a (on the sea at 140 px/s: ≈ 5 s on screen) and c (the magnet pulling them:
  the secondary nearly free). **F8 = a** art straight to production with a/b only for the eye and
  the latch, sounds a/b for thunder, gust, latch and drain; rejected: b (also the whole storm
  backdrop a/b) and c (no a/b at all: the eye unseen). **F9 = a** a Lamprey swarm is a swarm of
  chasers; rejected: b (real flocking until near the ship) and c (swarms as tight streams).
- 2026-10-09: M5 part F stated defaults (accepted with the decisions): data in Act 1 terms with a
  `bounty_scale` (the canister 130, the secondary 75); the waves densified to at least 50 a minute
  with the 3 s pacing rule, popcorn from L12's own roster, the t=32 introduction alone; the act HP
  factor for the returning `medium` units (Spore Bomber, Mantis, Wraith; the `tiny` and `small`
  ones keep their HP by the factor's rule); the wind in the data (gusts by `t` from the left,
  2.5 s, 40 px/s, easy 25, hard 55, telegraph 1.0 s, easy 1.5 s; the wall's 20 px/s drift, none on
  easy; hard's extra gusts at t=34 and 72; none in the eye), added to positions with 0.25 s ramps,
  moving the ship, Rook, Lampreys and Whirl Seeds only; lightning times in the data with a
  simulation event for the thunder; the Lamprey's rules (its spec's *Decisions*); shield cells
  ×1.5 as a level drop factor; the eye's armour patch as a placed pickup; the sondes' tracker,
  chime and event, Rook not collecting them; the radio retimed with the events `first-latch` and
  `secondary-objective`, Rook's t=102 dropped, his t=44 kept, his lines `requires: escort`; voiced
  with the cast speakers, Varga's four intel lines, two briefing pages, Rook's teaser; the banks
  hiding `low-air` bodies in L12 only; the storm bed with the ocean in the eye; L12's own atlas;
  the plan's L12 visit refitting Rook's Autocannon with `BalanceTest`'s Lamprey checks; hard's
  Mantis pincer in the eye, the canister's 5 hits and 2 latch slots; the eye calm (no rain, gusts,
  `clear`) with the cloud wall turning at ≈ 3°/s and the Needler circle clockwise. The draft's
  contradictions fixed: the budget (2,105 on the old curve → budget(12) ≈ 1,473), its Act-2-scaled
  amounts, the density (≈ 33 a minute), the radio clash in the eye, the "air units hidden under
  low-air banks", and the music's "base stem drops out in the eye".
- 2026-10-09: Settled in step 1 (for review in round 34): the Choir moves from t=164 to t=164.5,
  between Varga's two eye lines, and Okafor's "Say again" answers it at t=170; the `water: true`
  flag is set (open sea throughout); the sondes' collect radius is the ship's centre within 36 px.
- 2026-10-09: The storm turns **clockwise** (the eye's cloud wall, the Needler circle), as the
  approved draft says (user): unnatural for a northern-hemisphere storm on purpose, a tell that the
  storm is being steered.
- 2026-10-11: M5 part F step 3 (simulation built; pending the user's review in the combined round):
  a drift ramps up and down inside its span; the gust telegraph runs from its time ahead until the
  gust has ramped up (`Wind.telegraph`); a sonde enters and leaves with its body (24 px) just outside
  the edge, is collected only while the ship flies and counts as lost once wholly below the bottom
  edge (the secondary's failure raises the usual objective-failed event); a placed pickup enters with
  its centre on the top edge; the drifting canister is not hit by bombs or shells bursting on the
  ground nor by torpedoes, and is gone once it leaves the screen; sondes are not supported in a level
  with a boss checkpoint (none in Level 12).
- 2026-10-11: M5 part F step 4 (data, loader and autopilot; pending the user's review in the
  combined round): **234 units at medium (70 a minute)**, not the estimated 175–185: with the plan's
  two Micro-missile Pods the autopilot clears the waves fast, and leaner popcorn left empty screens
  over 3 s; the eye keeps four small popcorn waves (t=150, 155, 162.5, 169.5). **`bounty_scale`
  0.75** (typical haul 1,476, +0.2 %). **Directions front 70 · sides 24 · rear 6 %**: no Skitter
  stream from the rear (the simulation flies a rear `stream` only for chasers), so the rear is the
  Wraiths' and the Lampreys'; the popcorn added from the sides instead. The t=32 introduction: the
  popcorn before it at t=26.5 and nothing else until t=38 (6 s). Sondes at x 400, 70 and 390 (off
  the lanes of the waves round them); the canister enters at (420, top) at t=99 and drifts
  (−24, −70) px/s, ≈ 8 s on the screen, lit by the flashes at 99.5 and 103.5. The whirl clusters on
  the wind are released at x = −16 (just outside the left edge). The armour patch is placed at
  x 300. Rain keys: none until t=10, 0.4 in sections 2–3, 1 in the wall's peak, 0 in the eye, 0.4 at
  the breakout thinning to 0 by the end. The ambience key `earth-storm` waits for step 6 (the game has no storm bed yet). The
  plan's rear check of the returning Wraith (18 HP, 0.62 of its window against the limit 0.6) is
  reported in `BalanceTest` as pending the user's decision.
- 2026-10-11: M5 part F step 4, the radio (pending the user's review): the eye's lines retimed to
  149.5 (Rook), 157 (Varga), 169.5 (the Choir), 176 (Okafor) and 182.5 (Varga) for the typing time
  the step-1 estimate left out; Varga's L2 and L3 intel lines shortened to "New: an eel that drains your
  shield. Shake it off." and "Homing sees through the rain. Spread for the seeds.", and the shake-off prompt's keys to `LEFT-RIGHT` to fit the intel panel and
  the prompt line (`IntelPanelLayoutTest`, `MissionLayoutTest`).
- 2026-10-11: M5 part F step 6, the game side (pending the user's review in the combined round 34):
  the looks follow tools/art/backdrop_l12.py's review composite. **Lightning**: the bolt (at x 84
  for even flashes, 396 for odd) and a flat sky tint for 0.1 s, then the lit cloud over the bolt's
  side and the banks tinted toward (190, 205, 235) at 0.45 and seen through by 0.6, fading over the
  0.5 s reveal; timed by the data on the script clock (the simulation's `LIGHTNING` event plays the
  thunder); a flash within 1/3 s of the last one shown is dropped (none in the data); the flash
  reduction scales the bolt, the sky tint, the lit cloud and the banks' tint to 0.35 but **not the
  see-through**, so the reveal still shows what is under the banks. **Occlusion** is the existing
  draw order (`low-air` units and the canister under the low-air layer); each `low-air` unit gets
  the `weather-glow` pulse (4 fps) and the canister its own beacon glow over the banks.
  **Rain**: 220 streaks at density 1 (the data's keys), falling 480 px/s with a 1:4 slant on the
  steps' real clock, additive at 40 % over the play plane and under the bullets. **Gust
  telegraph**: the arrow 36 px in from the upwind edge (mirrored for a gust from the right) and four
  wind streaks blowing downwind at 216 px/s, while `Wind.telegraph` runs; the **upwind cue** is the
  gust sound panned 0.3 to the upwind side. **The eye**: the wall ring turned clockwise at 3°/s from
  t=150, linear-filtered for its draw, the shaft additive over the calm sea under the ground objects,
  both faded over the backdrop's 5 s ramp centred on 150 and 175 (the section with the clear
  atmosphere); the breakout's light at the top edge fades in over 190–200 (15 s into the last
  section) at 40 % and is held. **Latched Lampreys** are drawn over the ship at the ship's
  position plus their spot (no shadow), gulping at 10 fps with the shield ring's pulse (0.55 /
  0.3 / 0.12 / 0); stunned they tumble through their 8 frames over the stun. **The event prompt**
  (`SHAKE OFF` · `LEFT-RIGHT`) shows from the attempt's first latch until no Lamprey is latched or
  its 6 s are over, once an attempt. **Sounds** (round 34 a): thunder 1–3 in turn, 0.3–1.5 s after
  each flash (a golden-ratio spread over the flash index, so it replays alike); the gust as its
  telegraph starts; the latch's bite; the drain loop while a Lamprey drains in flight; the
  shake-off; the sonde's chime. The theme's `section_db` ramps over 1 s.
- 2026-10-11: M5 part F step 6, the voices (pending the user's review in the combined round): the 18
  voiced lines (Okafor 5, Varga 8, Rook 5; the Choir's line plays its stage sound) rendered and
  measured; six takes pinned (see [voice](../../../audio/voice/README.md)); `RadioTimelineTest` passes
  with the real lengths, with and without Rook, so no radio time was changed.
- 2026-10-11: M5 part F, fixes after the round 34 capture (the user is on vacation: the capture's
  defaults, **pending the user's review in the combined round**). **Shake-off prompt**: it showed in
  the left panel's three-line well under the radio, far from the ship, for 0.6 s and once an
  attempt, so the real shake-off had none; it now stands in the play field as a warning (the
  [HUD](../../../ui/hud/README.md#in-the-play-field) rule), a one-line box below the ship, on every
  latch of at least 0.3 s, at most 6 s each time, until the attempt's first shake-off. **Rain**: it
  was very faint (about 1 % streak pixels, 1.5 % in the wall): 480 streaks at density 1 instead of
  220, half as long again at density 1 (the wall's sheets; ×1.2 at the steady 0.4), and drawn so
  the brightest streak pixel adds 40 % of white (the high-air cap read on what is drawn, not on the
  dim image: 0.64 of the image instead of 0.4); still additive under the bullets, and still the
  only strong motion beside the sea. **Gust telegraph**: it was weak and under the side edge
  warnings: the arrow at twice its size, drawn twice additive, its centre 72 px in from the upwind
  edge and 60 px below the top edge (clear of the side warnings' bars and glow, 99–441 px down, and
  the wave banner); seven wind streaks instead of four, twice as thick, at the high-air cap.
  **Reveal**: a flash lit only a cloud puff by the bolt, the bodies under the stacked bank layers
  (tiles, banks, tall banks, each at 1 − 0.6·reveal) still hidden: during the reveal the `low-air`
  units and the canister are drawn again over the banks at 0.6·reveal, wherever they are on the
  screen, and the reveal holds at full for its first 0.2 s before fading over 0.3 s. **Sounds**:
  the latch, shake-off and gust at the warnings' level (the gust from −4 dB, the shake-off from −2
  dB; the latch was there already) and a latch or shake-off ducks the theme by 6 dB for 1 s.
  Left as review items: the eye's 2 s dissolve, the plan fit not latched before t≈112, the armour
  use.
- 2026-10-12: M5 part G, step G8 (**pending the user's review in the combined round**): the shake-off
  cue measured about 6 dB under the busy mix and was marginal at one of the capture's two shake-offs,
  though it already plays at the warnings' level (the game's ceiling). Its file is now 3.5 dB louder
  (round 34's placeholder regenerated at −13.5 dB instead of −17; [sfx](../../../audio/sfx/README.md));
  the latch, the gust and the theme's 6 dB duck stay.
