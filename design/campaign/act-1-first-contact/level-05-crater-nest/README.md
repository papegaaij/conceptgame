---
title: Level 05 – Crater Nest
design: approved
implementation: in-progress
art: chosen
depends-on: [../../../enemies/ground/polyp-mortar, ../../../enemies/bosses/gorgon-frigate, ../../../world/luna]
updated: 2026-10-04
---

# Level 05 – Crater Nest

## Summary

The Tranquility pods came from a Vrell nest growing in a crater beside the mass-driver line.
Lancer runs the still-firing mass driver, drops over the crater rim and must destroy all four
nest batteries on the crater floor (`destroy-targets`; a battery left alive fails the mission).
After the last battery, the Gorgon Frigate drops out of orbit to defend the nest: the first
mid-boss and the first multi-part enemy. Introduces the Polyp Mortar, the mass-driver sleds and
low-gravity debris. About 3½ minutes including the mid-boss.

## Briefing

<!-- data: briefing -->
> **Commander Okafor:** "Lancer, the pods at Tranquility weren't random. Survey drones found
> where they came from: a crater beside the mass-driver line, and the Vrell are growing a nest
> in it. Four batteries of acid-throwers and turrets guard the floor. Destroy all four. If a
> battery is still alive when you pass it, the nest survives and the mission fails. There's no
> second run at that crater today. And stay clear of the rail. The mass driver is still firing
> on automatic."
>
> **Dr. Varga:** "The acid-throwers mark their target a second before it lands. Keep moving and
> you'll be fine. And something in orbit keeps watching that crater. I don't like it."
<!-- /data -->

*Images* (production art, [briefing images](../../../ui/briefing/README.md), proposed in
[round 21](../../../concept-rounds/round-21/README.md)): Okafor's page `level-05-crater-nest` (the
nest crater beside the mass-driver line, batteries A–D on its floor, Lancer's run down the rail and
over the rim, the sleds on automatic), Varga's `level-05-mortar-scan` (the Polyp Mortar's lob arcing
to its lime marker a second ahead, the ship moving out of it, an unknown contact holding in orbit
over the crater); the level's data names them as each page's `image`.

*Hangar teaser* (shop screen after L04):
<!-- data: teaser -->
> **Dr. Varga:** "Turrets, mortars and a lot of ground to cover. Bring something that hits the
> surface, and something wide for the flanks."
<!-- /data -->

## Threat profile

<!-- data: threat-profile -->
| Field | Value |
|---|---|
| Dominant layers | `ground` (mortars, turrets, the batteries), `air` |
| Attack directions | front 81% · sides 19% |
| Density | 3 |
| Recommended traits | `anti-ground`, `spread` |
| Hazards | Mass-driver sleds (section 1); low-gravity debris from destroyed ground targets |
| Boss / mid-boss | Mid-boss: Gorgon Frigate |
| Sensor-suite detail | none: Luna, `ground` + `air`, front · L1: + directions, density 3, hazards "sleds, debris", OBJECTIVE `DESTROY 4 BATTERIES` · L2: + Needler, Skitter, Stinger, Brood Pod portraits; mid-boss name and silhouette · L3: + `anti-ground` highlighted, wave strip, 1 secret |
<!-- /data -->

## Objective

- **Primary** `destroy-targets`: destroy nest batteries A, B, C and D. A battery is destroyed
  when all four of its units (2 Polyp Mortars + 2 Spine Turrets) are dead. **Fails** as soon as
  the first unit of a battery scrolls past the bottom edge alive: the battery can no longer be
  destroyed (the campaign rule, "as soon as a primary objective becomes impossible"). The level
  reports the failure at once through the [failed primary objective](../../../systems/retry/README.md#on-a-failed-primary-objective)
  flow rather than at the end of the scroll, so the player never fights the mid-boss for
  nothing. After battery D the mid-boss must be killed to finish the level (see *Arena clock*).
- **Secondary** *Scorched crater*: destroy every Polyp Mortar and Spine Turret in the level, in
  the batteries or not (14 + 16). +40.

## Layout

Scene: the chosen [Luna](../../../world/luna/README.md) scene
([sheet](../../../art-direction/concept/scene-luna-r06-a.png)) with its mass-driver rail and
Vrell nest crater. Scroll speed 150 px/s (normal), 30 px/s in the mid-boss arena (per the
[Gorgon Frigate](../../../enemies/bosses/gorgon-frigate/README.md) spec). Total ≈ 215 s ≈ 25,650 px
when the frigate dies within the arena's 55 s. Luna is seen top-down: there is no `deep` layer
(Earth only as the earthshine tint) and no perspective `far` walls; the rim and its walls are
ground pieces ([Luna](../../../world/luna/README.md#parallax-layers)).
Motion budget: the sleds and the regolith plumes in section 1; plumes and the nest's vents
afterwards.

<!-- data: level-sections -->
| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Mass-Driver Line | 0–35 | 0–5,250 | 150 | clear | `ground`: regolith, the mass-driver rail at x 432 (Level 04's rail), its signal lights, the launch sleds streaking up the rail. | Learn the sled rhythm; the first Polyp Mortar alone. |
| 2. Crater Rim | 35–75 | 5,250–11,250 | 150 | light | `ground`: the outer rim and its inner walls (ground pieces), boulders, an abandoned rover. `low-air`: regolith plumes from mortar impacts. | Battery A; side waves. |
| 3. Nest Floor | 75–125 | 11,250–18,750 | 150 | medium, heavy peak 115–122 | `ground`: the crater floor with teal Vrell growth and pod husks. `low-air`: spore dust venting from the nest (the heavy peak). `high-air`: ejected rock streaks. | Batteries B and C; Brood Pods; low-gravity debris. |
| 4. Nest Heart | 125–150 | 18,750–22,500 | 150 | light | `ground`: the central growth, pod husks around battery D. `low-air`: thin plumes. | Battery D; the frigate's shadow falls over the floor. |
| 5. Gorgon Frigate | 150–205 | 22,500–24,150 | 30 | clear | `ground`: the nest heart below (no terrain collision). `air`: the frigate, its bell in the upper third. | Mid-boss fight (≈ 55 s at medium); the scroll halts after 55 s while the frigate lives, and section 6 starts at its death. |
| 6. Lift-off | 205–215 | 24,150–25,650 | 150 | light | `ground`: the far rim, the burning nest behind. | Credit shower, end. |
<!-- /data -->

### Arena clock

The arena section scrolls at 30 px/s for 55 s (1,650 px). If the frigate still lives when that
time is up, the scroll halts (the backdrop stands still) and the **level clock pauses** until it
dies; when it dies before, section 6 starts at once (the rest of the arena is skipped, the scroll
ramping back to 150 px/s over 1 s). Section 6 always starts at the frigate's death, and every
timed event after t = 150 (cues, pieces, the lift-off) runs on that pausable clock (user decision
D1 of M4 part E).

## Waves

Enemy specs: [Polyp Mortar](../../../enemies/ground/polyp-mortar/README.md),
[Spine Turret](../../../enemies/ground/spine-turret/README.md),
[Stinger](../../../enemies/air/stinger/README.md),
[Needler](../../../enemies/air/needler/README.md),
[Skitter](../../../enemies/air/skitter/README.md),
[Brood Pod](../../../enemies/air/brood-pod/README.md),
[Gorgon Frigate](../../../enemies/bosses/gorgon-frigate/README.md).

<!-- data: waves -->
| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 5.5 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 2 | front (right) | Pacing filler |
| 10 | 1 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | Left of the rail |
| 13 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (left) | Pacing filler |
| 20 | 1 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | Snakes across the rail between sleds |
| 22 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 2 | front (right) | Pacing filler |
| 30.5 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (left) | Pacing filler |
| 38 | 2 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | |
| 42.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | Pacing filler |
| 48 | 2 | pincer | [Needler](../../../enemies/air/needler/README.md) | 4 | sides | Hold 4 s at the edges above battery A |
| 51.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 2 | front (right) | Pacing filler |
| 58.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 2 | front (left) | Pacing filler |
| 62 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 8 | sides (alternating edges) | All eight on easy too (pacing) |
| 70 | 2 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 4 | front | |
| 73 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | Pacing filler |
| 80 | 3 | single | [Brood Pod](../../../enemies/air/brood-pod/README.md) | 1 | front | |
| 84 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 2 | front (left) | Pacing filler |
| 91 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 2 | front (right) | Pacing filler |
| 95 | 3 | carrier + escorts | [Brood Pod](../../../enemies/air/brood-pod/README.md) + [Needler](../../../enemies/air/needler/README.md) | 1 + 4 | front | Over battery B |
| 102 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 2 | front (left) | Pacing filler |
| 110 | 3 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | right side | Over battery C; edge warning |
| 112 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 2 | front (right) | Pacing filler |
| 118 | 3 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | In the heavy peak |
| 122 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | Pacing filler |
| 128.5 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (left) | Pacing filler |
| 135 | 4 | single | [Brood Pod](../../../enemies/air/brood-pod/README.md) | 1 | front | Over battery D |
| 140.5 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 2 | front (right) | Pacing filler |
| 150 | 5 | mid-boss | [Gorgon Frigate](../../../enemies/bosses/gorgon-frigate/README.md) | 1 | front (descends from above) | Fixed time, after battery D. Skitter streams of 6 at the settle and every 10 s in phase 1 (2 streams at medium par) |

Totals: Skitter 56 · Needler 12 · Stinger 10 · Brood Pod 3 · Gorgon Frigate 1.
<!-- /data -->

The frigate's streams (12 Skitters in a fight at par) and the Brood Pods' released Skitters (18) come
on top of the totals.

**Pacing filler** (user decision D5): the gaps the waves leave are filled as in Level 04, with
small fixed-count Skitter streams (the rows marked *Pacing filler*: 14 streams of 2–3, 35
Skitters, 175 credits) and no new hazards; the t=62 stream keeps all eight on easy. PacingTest
leaves one gap over 3 s on medium (112–116 s) besides the lift-off. Their credits are accepted
above the budget: +14.5 %.

## Ground targets

<!-- data: ground-targets -->
| Section | Target | Effect |
|---|---|---|
| 1 | [Polyp Mortar](../../../enemies/ground/polyp-mortar/README.md) ×1 on the rail embankment (t≈28) | **Introduction**: alone, between two sleds; radio tip |
| 1 | Stuck ore canister on the rail (t≈30) | Its clamp takes 3 hits, only while the rail lights are dark; releases the hidden crate |
| 2 | [Spine Turret](../../../enemies/ground/spine-turret/README.md) ×2 on the rim (t≈40) | Bounty |
| 2 | **Battery A**: Polyp Mortar ×2 (t≈55); easy ×1 | Objective target |
| 2 | **Battery A**: Spine Turret ×2; easy ×1 | Objective target |
| 2 | Polyp Mortar ×2 on the inner rim (t≈70) | Bounty |
| 3 | **Battery B**: Polyp Mortar ×2 (t≈88); easy ×1 | Objective target |
| 3 | **Battery B**: Spine Turret ×2; easy ×1 | Objective target |
| 3 | Spine Turret ×2 (t≈100) | Bounty |
| 3 | **Battery C**: Polyp Mortar ×2 (t≈108, just before the heavy peak); easy ×1 | Objective target; its last unit to die drops the armour patch |
| 3 | **Battery C**: Spine Turret ×2; easy ×1 | Objective target |
| 3 | Polyp Mortar ×1 (t≈120) | Bounty |
| 3 | Spine Turret ×2 (t≈120) | Bounty |
| 4 | **Battery D**: Polyp Mortar ×2 around the nest heart (t≈135; the last battery, gone by ≈ 139 s); easy ×1, hard ×3 | Objective target |
| 4 | **Battery D**: Spine Turret ×2; easy ×1 | Objective target |
| 4 | Polyp Mortar ×2 (t≈145; gone by ≈ 149 s, before the frigate) | Bounty |
| 4 | Spine Turret ×2 (t≈145) | Bounty |
| 4 | CDF supply canister (Level 04's supply drop; ground layer, 3 HP; t≈147) | Small salvage (10); also a special charge if a special is fitted |
<!-- /data -->

Totals: Polyp Mortar 14 · Spine Turret 16. A battery is two ground-target entries (mortars,
turrets) sharing one group; the primary is `destroy-targets` over groups A–D. Each battery's
units are outlined in the HUD's objective colour; the tracker line reads `BATTERIES A B C D`
(a letter struck through once cleared) and the hangar's OBJECTIVE field "DESTROY 4 BATTERIES".

## Hazards

- **Mass-driver sleds** (section 1, as defined in [Luna](../../../world/luna/README.md#hazards--set-pieces)):
  every 5 s (easy 8 s, hard 4 s) a sled shoots up the rail, a vertical line 24 px wide across
  the whole field at x 432 (Level 04's rail, on the ground layer). Lights along the rail chase
  upward for 1.5 s before each sled. It hits the ship whatever its layer (contact damage 15, once
  per sled) and blocks player shots and enemy bullets for the 0.4 s it is on screen; enemies are
  unharmed. The first sled runs at t = 16.5 s, after Driver Control's warning, which puts the
  t=28 mortar between the sleds of 26.5 and 31.5 s (`sleds` in the data). The hazard draws Level
  04's `sled-run` lamp frames over the rail (idle, the fast blink while the lights chase), lit lamps
  added over them (a band sweeping up the rail three times in the telegraph, every lamp while a sled
  runs) and the lit sled on its additive motion streak (`tools/art/l05_hazards.py`).
- **Low-gravity debris** (sections 2–4): a destroyed ground target throws 2–3 rocks (from the
  simulation's random numbers) in slow arcs onto the `air` layer (drift 40–60 px/s, fall back and
  vanish after 2.5 s): a thrown kind of the existing debris. Contact damage 6; shootable (1 HP, no
  credits); a rock that hits the ship breaks. None is thrown when the destroyed unit lies within
  72 px of the player. Off on easy. Each rock is one of three regolith shapes (by its serial),
  tumbling at 4 frames per 0.4 s.
- **Mortar impacts**: per the Polyp Mortar spec (lime marker 1 s ahead; the lime marker
  ring and the acid blob arcing over to it; a destroyed mortar leaves its acid splash decal).

## Secrets and pickups

Pickup types are defined in [player](../../../player/README.md#in-level-pickups).

- **Stuck sled** (hidden crate, 65): an ore canister sits jammed halfway up the rail at t≈30.
  Its clamp is a trigger target (3 hits, reveals the crate) hittable only while the rail lights
  are dark: of each 5 s cycle, 1.5 s lights and 0.4 s sled leave a ≈ 3.1 s window (its beacon is
  lit while it can be hit), its clamp shot open once spent. Found, Rook: "That canister sat jammed on the rail all week. Finders keepers,
  Lancer."

- **Armour patch** ×1: dropped by whichever unit of battery C dies last.
- **Overdrive**: dropped by the Brood Pod of the t=95 group.
- **Special charge**: a CDF supply canister (Level 04's supply drop) floats down at t≈147, a
  ground target with a `bonus_drop` placed to leave the screen before 150 (only pays if a
  charge-based special is fitted).
- Shield cells at the normal rate.

## Radio chatter

Spoken radio ([voice](../../../audio/voice/README.md)). Rook flies Aegis Two on the far side of the
crater (radio only). Driver Control is a new generic speaker; its voice comes from a short
audition (user decision D4) and until then it is marked `uncast` in the speaker table: its lines
show as text with the radio blips, without a voice file. The timed lines are retimed to the 1 s
rule ([HUD](../../../ui/hud/README.md), RadioTimelineTest): Driver Control's warning follows
Okafor's opening line, the venting line waits for battery C's call, and the frigate's lines are
spread from the Choir's song at 144.5 s to Rook's at 170 s. The battery lines are `group-cleared`
cues; the line for a battery passing alive is the `mission-failed` cue, its `{group}` filled with
the battery's name (one voice file per battery).

<!-- data: radio -->
| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "There's the crater, Aegis. Four batteries on the floor. All four die, or this was for nothing." |
| t=12.5 | Generic CDF (Driver Control) | "Mass driver's still on automatic, Aegis. Sleds every five seconds. Watch the rail lights." |
| t=27 (first mortar) | Varga | "That one lobs acid. It marks where it'll land, a second ahead. Don't be there." |
| t=46 (pincer) | Rook | "Aegis Two here. Two groups, both flanks of you, Lancer!" |
| Battery A cleared | Okafor | "Battery down." |
| Battery B cleared | Okafor | "Two." |
| Battery C cleared | Okafor | "Three. One left." |
| Battery D cleared | Okafor | "That's the last battery. The nest is open." |
| Battery passes alive (the failed screen's line) | Okafor | "{group} is behind you. The nest survives. Pull back, Lancer." |
| t=116.5 | Generic CDF (Driver Control) | "The nest is venting. Dust everywhere. Your scopes will clear in a few seconds." |
| t=144.5 | The Choir (distorted) | "[the Choir sings]" |
| t=150.5 | Generic CDF (Driver Control) | "Big contact dropping out of orbit, right on top of the crater!" |
| t=158 (the frigate settles) | Varga | "A warship. Three heads, all guns. Take the heads one at a time; then the crown opens. Hit the core." |
| t=170 (after the first stream) | Rook | "Oh, it has *heads*. Of course it has heads." |
| Last head destroyed | Varga | "Crown's opening. That's the core. Lime glow, Lancer!" |
| Frigate destroyed | Okafor | "Frigate down. The nest is dead. Get out of there, Aegis." |
| Level end | Okafor | "Good work. Varga wants samples. I told her no." |
| Secondary met | Varga | "Every growth in that crater burned. We'll have nothing to study. Well done, I suppose." |
<!-- /data -->

## Boss / mid-boss

[Gorgon Frigate](../../../enemies/bosses/gorgon-frigate/README.md), the first mid-boss. Level
notes:

- **Intro**: at a fixed t = 150 s, after battery D (which is gone by then, destroyed or the
  mission failed). The frigate's shadow crosses the nest heart, then it descends at 60 px/s
  until its bell holds the upper third; mini-boss sting, name and short health bar.
- **Arena**: above the nest heart; the scroll slows to 30 px/s and no terrain collides (as in
  the spec's arena line); see *Arena clock* for a fight longer than 55 s.
- **Duration**: 1,500 HP at an effective 0.6 × 45 = 27 DPS ≈ 55 s, inside the 45–75 s target.
- Ground targets have all left the screen by now (battery D is the last battery, the t=145 group
  is gone by ≈ 149 s), so the fight is air only.
- On easy and medium, the boss checkpoint is recorded at the mini-boss sting
  ([retry](../../../systems/retry/README.md#boss-checkpoint-easy-and-medium)). *Retry from boss*
  keeps the objective tallies (batteries, *Scorched crater* kills) and restarts at the sting on an
  empty field.
- **Par** 60 s at every difficulty: a kill under par pays the Boss rush bonus (2,000 × act;
  [scoring](../../../systems/scoring/README.md#level-end-bonuses)).

## Music & ambience

Track 4 *Act 1 A: Earth orbit & Luna* ("Afterburner", see the
[track list](../../../audio/music/README.md#track-list)); track 21 mini-boss sting at the
frigate's entrance (over a 0.5 s crossfade), then back to the level track with the intensity
stem on. Ambience: Luna
([sfx](../../../audio/sfx/README.md#ambience-per-setting)) plus a rising electric whine on the
rail before each sled.

## Credit budget

Budget(5) = 1,000 × 1.07⁴ ≈ **1,311** ([economy](../../../systems/economy/README.md#per-level-budget)).
Bounties from the stat blocks: Polyp Mortar 15, Spine Turret 12, Stinger 15, Needler 12,
Skitter 5, Brood Pod 20, Gorgon Frigate 200 (≈ 15% of the budget).

<!-- data: credit-budget -->
| Source | Credits (medium) |
|---|---|
| Kills: Skitter 56 × 5 + Needler 12 × 12 + Stinger 10 × 15 + Brood Pod 3 × 20 + released Skitter 18 × 5 | 724 |
| Ground targets: Polyp Mortar 14 × 15 + Spine Turret 16 × 12 + supply canister small salvage 10 | 412 |
| Mid-boss: Gorgon Frigate (left head 30 + centre head 30 + right head 30 + core 110) | 200 |
| Mid-boss streams: 2 × 6 Skitter × 5 (a fight at par) | 60 |
| Secret: stuck sled (hidden crate, 5% of budget) | 65 |
| Secondary: every Polyp Mortar and Spine Turret destroyed | 40 |
| **Total** | **1,501** |
<!-- /data -->

The frigate's Skitter streams depend on how long phase 1 lasts; the budget counts the two
streams of a medium-par fight (`boss.notes.streams`). Without the pacing filler the total is
1,326 (+1.1 %); the filler's 175 credits (see *Waves*) lift it to 1,501, +14.5 %, accepted as in
Level 04 (user decision D5).

## Difficulty notes

- **Easy**: batteries have 1 Polyp Mortar + 1 Spine Turret; sleds every 8 s; no low-gravity
  debris.
- **Hard**: mortars burst into 12-bullet rings and the frigate's last head fires 5-shot bursts
  (stat-block hooks); sleds every 4 s; battery D has a third mortar.

## Concept art

Prompts and the capture's method: [concept/prompts.md](concept/prompts.md). The props' concepts
(user decision D8) were decided in [round 21](../../../concept-rounds/round-21/README.md) (rocks a, ore canister a, acid splash b); the battery
outline gets no concept (the game's 1 px outline in the objective colour stays, main-agent call),
and the props' sounds are in [sfx](../../../audio/sfx/README.md).

| File | What | Status |
|---|---|---|
| [concept/sled-final-r21-a.png](concept/sled-final-r21-a.png) | Final sled sprites (`tools/art/l05_hazards.py`): the lit sled (24×48), its motion streak (32×200, additive), a lit rail lamp (16×16, additive) | chosen |
| [concept/sled-final-r21-a.gif](concept/sled-final-r21-a.gif) | One cycle on Level 04's rail: the lamps chasing up, then the sled racing up on its streak with every lamp lit | chosen |
| [concept/level-05-capture-final-r21-b.png](concept/level-05-capture-final-r21-b.png) | Game capture with the production frigate (round 21 item 12, whole window, 2 × 4): a sled run, the frigate's arrival with its bar, the necks in phase 1, the open core, the core phase, its death in three frames (blast cluster, swap, chunks drifting apart) | chosen |
| [concept/l05-props-final-r21-c.png](concept/l05-props-final-r21-c.png) | Final prop sprites (`tools/art/l05_props.py`, from the chosen concepts): the rocks (3 shapes × 4 tumble frames, 16×16), the ore canister (40×28: beacon dark, lit, clamp shot), the acid splash decal `polyp-mortar-splash` (40×40: fresh, after 1 s, fading) | chosen |
| [concept/rejected/level-05-capture-final-r21-a.png](concept/rejected/level-05-capture-final-r21-a.png) | Headless capture of the playable level (M4 part E): a battery outlined, a mortar lob, the sled, the frigate arriving with its bar, a phase, its death; placeholder props, Level 04's backdrop | rejected |
| [concept/rocks-r21-a.png](concept/rocks-r21-a.png) | Low-gravity rocks (16×16, air layer), concept a: plain regolith chunks with a bright rim; one tumbling in 4 frames, two more shapes (`tools/concept/props_r21.py`) | chosen |
| [concept/rejected/rocks-r21-b.png](concept/rejected/rocks-r21-b.png) | Low-gravity rocks, concept b: dark scorched basalt with a glowing lime creep crust (`tools/concept/props_r21.py`) | rejected |
| [concept/ore-canister-r21-a.png](concept/ore-canister-r21-a.png) | Stuck sled ore canister (40×28 on the rail), concept a: steel ore cylinder, hazard bands, clamp yoke with the beacon; beacon dark, lit, clamp shot (`tools/concept/props_r21.py`) | chosen |
| [concept/rejected/ore-canister-r21-b.png](concept/rejected/ore-canister-r21-b.png) | Ore canister, concept b: open ore skip heaped with ore, hazard rim, clamp jaws at both ends (`tools/concept/props_r21.py`) | rejected |
| [concept/rejected/acid-splash-r21-a.png](concept/rejected/acid-splash-r21-a.png) | Acid splash decal (40×40, ground; the Polyp Mortar's death), concept a: wet lime splatter with droplets and glints; fresh, after 1 s, fading (`tools/concept/props_r21.py`) | rejected |
| [concept/acid-splash-r21-b.png](concept/acid-splash-r21-b.png) | Acid splash decal, concept b: etched burn, a scorched pit with a teal-green bubbling pool and a pale ring (`tools/concept/props_r21.py`) | chosen |

## Implementation

- [x] Scroll timeline, sections, atmosphere intensity and parallax content as in *Layout*,
      including the 30 px/s arena and its pausable clock (*Arena clock*); the backdrop reuses
      Level 04's Luna images (`backdrop.images: level-04`) until Level 05's own art.
- [x] Battery objective: four groups of four units, HUD tracker A–D, immediate mission failure
      when a battery's first unit leaves the screen alive.
- [x] Wave script matches the *Waves* table, the frigate at t = 150; pacing filler per PacingTest.
- [x] Mass-driver sleds with the 1.5 s light telegraph, contact damage 15.
- [x] Low-gravity debris thrown by destroyed ground targets.
- [x] Stuck-sled secret hittable only between sleds.
- [x] Gorgon Frigate per its spec; boss checkpoint on easy/medium with *Retry from boss*
      keeping the objective tallies; Boss rush par 60 s.
- [x] Radio cues fire at their triggers, including per-battery lines (Level05Test,
      RadioTimelineTest; voices rendered, Driver Control uncast).
- [x] Credit total at medium with perfect collection is 1,311 (± 5%) before the pacing filler
      (1,326); with it 1,501 (+14.5 %, user decision D5).
- [x] Easy/hard variations as in *Difficulty notes*.
- [x] Production art for the props without a concept (rocks, ore canister, acid splash, sled
      streak; the battery outline stays the game's 1 px outline) and their sounds (sled whine,
      mortar lob and impact), chosen in round 21 (user decision D8).

## Decisions

- 2026-10-01: Promoted from the act roster to a draft level document. The named targets are four
  batteries built from existing units (no new art). A battery missed fails the mission at once,
  not at the end of the scroll, so the mid-boss is only fought on a run that can still succeed.
  The mass-driver sleds (Luna's signature set piece) are introduced here as a hazard.
- 2026-10-01: Open question resolved: the Gorgon Frigate spec's arena now is the nest crater on Luna.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-03: M4 part E (user decisions): the arena scrolls at 30 px/s for 55 s, then halts while
  the frigate lives, and section 6 starts at its death (a pausable level clock); the frigate
  arrives at a fixed t = 150 s; *Retry from boss* keeps the objective tallies and restarts at the
  sting on an empty field; Driver Control gets a short voice audition later; pacing filler as in
  Level 04, accepting about +10–15 % credits; a missed battery fails the mission when its first
  unit leaves alive; the mid-boss pays the Boss rush bonus with a par of 60 s; the small props
  without a concept (thrown rocks, stuck ore canister, acid splash, battery outline, the sled and
  mortar sounds) get placeholders now and a/b variants in the part's production round.
- 2026-10-03: M4 part E (main-agent choices): the sled runs on Level 04's rail at x 432 on the
  ground layer; no `deep` layer and no perspective `far` walls on Luna; section 3 split at 115 and
  122 s in the data for the heavy peak; batteries as ground-target groups with the tracker
  `BATTERIES A B C D`; the "battery passes alive" line names the battery; the radio is retimed to
  the 1 s rule when the data is written; the music sting crossfades in over 0.5 s.
- 2026-10-03: M4 part E, the level made playable (main-agent and implementation choices): the
  heavy peak is a `peak` of section 3 (115–122 s) rather than a split, so the arena stays section
  5; a battery's four units enter one after another across the floor, 0.5 s apart; the first sled
  runs at 16.5 s, after Driver Control's warning; the Polyp Mortar's 32 px impact circle is its
  diameter and its ring starts 12 px outside it; the batteries are the groups `Battery A`–`D` and
  the failure line names one through `{group}`; the stuck sled's line went to Rook (a secret's
  line has no generic portrait); the backdrop reuses Level 04's images through
  `backdrop.images` rather than copies; the pacing filler is 14 small Skitter streams (+14.5 %),
  the t=62 stream keeps its eight on easy; Level05Test and PacingTest fly the balance plan's
  Level 05 fit (Pulse Cannon L2, two Autocannon Pods, the second shield; Pulse Cannon L3 on
  hard), as the starter fit cannot clear the batteries in time.
- 2026-10-04: M4 part E batch (review files for round 21): the sled hazard gets its production
  sprites (`tools/art/l05_hazards.py`): a lit sled on an additive motion streak, and lit rail lamps
  added over Level 04's lamp frames so the chase stands out from the rail.
- 2026-10-04: M4 part E, round 21 opened: a/b concepts for the rocks, the ore canister and the acid
  splash decal (`tools/concept/props_r21.py`); none for the battery outline, which stays the game's
  1 px objective-colour outline; a/b sounds for the sled's whine and pass and the mortar's lob and
  impact; the Driver Control audition; the two briefing images (`level-05-crater-nest`,
  `level-05-mortar-scan`), named in the data.
- 2026-10-04: Round 21 verdict (user): the sled's production art (`sled-final-r21-a`) approved as
  **final**; the props' concepts rocks a, ore canister a and acid splash b chosen (the others moved
  to `concept/rejected/`; their production art follows); the sled and mortar sounds chosen (see
  [sfx](../../../audio/sfx/README.md)). The capture `level-05-capture-final-r21-a` showed the
  frigate's placeholder shapes (taken before its sprites were wired in): moved to
  `concept/rejected/` and redone as `level-05-capture-final-r21-b` (round 21 item 12).
- 2026-10-04: The props' production sprites from the chosen concepts (`tools/art/l05_props.py`,
  M4 part E batch, review `l05-props-final-r21-c`): `rock` (3 shapes × 4 tumble frames),
  `ore-canister` (beacon dark, lit, clamp shot) and `polyp-mortar-splash`, drawn by the game
  instead of the scaled boulder, the cargo container with a beacon, and no decal.
- 2026-10-04: Round 21 closed (user): the frigate's death redo and the capture
  `level-05-capture-final-r21-b` accepted. The level's art stays `chosen`: its backdrop still
  reuses Level 04's images (no crater rim walls or burning nest of its own yet).
