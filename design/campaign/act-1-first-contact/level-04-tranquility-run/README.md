---
title: Level 04 – Tranquility Run
design: draft
implementation: not-started
art: chosen
depends-on: [../../../enemies/air/brood-pod, ../../../enemies/ground/scuttler, ../../../world/luna, ../../../player/specials]
updated: 2026-10-01
---

# Level 04 – Tranquility Run

## Summary

Brood pods have landed around Tranquility Base. Lancer escorts five civilian crawlers along the
Mare Tranquillitatis road to the mass-driver terminal while Vrell walkers stalk them from the
craters. The first planetary surface and the first `escort` objective (primary: losing the
whole convoy fails the mission). It introduces the Brood Pod, the Scuttler walker that turns to
face where it walks, hardened ground targets, and the first special (the Airstrike). About
3 minutes, no boss.

## Briefing

> **Commander Okafor:** "Lancer, brood pods came down around Tranquility Base overnight. The
> heritage site is fine. The town around it is not. Four hundred civilians are loading into five
> crawlers for the run to the mass-driver terminal, and Vrell walkers are already in the craters
> along the road. You are their escort. Crawlers are slow and they can't dodge: anything on the
> ground that shoots will shoot at them. Lose all five and this mission is over. High Command
> has released Hammer flight to us. Use it."
>
> **Dr. Varga:** "The walkers turn to face where they're going. Their claws stop your rounds, but
> their backs glow. Be patient."

*Hangar teaser* (shop screen after L03):
> **Commander Okafor:** "Tomorrow you fly over people, Lancer. Bring something that can hit the
> ground."

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `ground` (turrets, walkers, the convoy), `air` |
| Attack directions | front 86% · sides 14% (walker pincers over the crater rims) |
| Density | 2 |
| Recommended traits | `forward`, `anti-ground` |
| Hazards | none lethal; ground fire targets the convoy |
| Boss / mid-boss | none |
| Sensor-suite detail | none: Luna, `ground` + `air`, front · L1: + directions, density 2, "escort: 5 crawlers" · L2: + Brood Pod, Scuttler, Spine Turret, Needler, Skitter portraits; Airstrike available · L3: + `anti-ground` highlighted, wave strip, 1 secret |

## Objective

- **Primary** `escort`: bring the convoy of five crawlers to the terminal gate at the end of the
  scroll. Each crawler that arrives pays +30. **Fails** when all five crawlers are destroyed
  (mission failed, [retry](../../../systems/retry/README.md)); each crawler lost before that
  only lowers the reward.
- **Secondary** *Quick hands*: kill every Brood Pod before it bursts on its own (6 pods). +50.

### The convoy

- Five CDF heavy crawlers (civilian evacuation haulers) on the `ground` layer, 72×40 px each,
  in a column 60 px apart along the road. They move with the scroll and drift left and right as
  the road winds, so they stay in the lower half of the play field (y ≈ 330–520).
- HP 60 each at medium. Crawlers take damage from **bullets fired by ground-layer enemies**
  (Spine Turrets and Scuttlers; air-layer enemy bullets pass over them) and from a Scuttler's
  claws when it walks into the column (10 per second). Skitters and Needlers ignore them.
- A hit crawler flashes and its damage shows as smoke; a destroyed crawler stops and burns (no
  explosion debris on the player's layer). The HUD objective tracker shows five crawler pips.
- The player's shots and the Airstrike never hurt crawlers.

## Layout

Scene: the chosen [Luna](../../../world/luna/README.md) scene
([sheet](../../../art-direction/concept/scene-luna-r06-a.png)) with its Tranquility Base and
mass-driver elements. Scroll speed 120 px/s (the crawlers' pace; calm, see
[art direction](../../../art-direction/README.md#parallax-layer-model)). Total ≈ 190 s ≈ 22,800 px.
Motion budget: regolith plumes and the distant mass-driver sleds are the only strong background
movers.

| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Parallax content per layer | Purpose |
|---|---|---|---|---|---|---|
| 1. Tranquility Base | 0–25 | 0–3,000 | 120 | clear | `deep`: black sky, stars, Earth half-lit on the horizon. `ground`: Tranquility Base: domes, the Apollo 11 heritage dome, the convoy assembling and pulling out. `low-air`: none. | First planetary surface; the convoy forms up; the first Brood Pod alone. |
| 2. Mare Road | 25–70 | 3,000–8,400 | 120 | light | `ground`: the convoy road across grey mare, abandoned rovers, turret growth on boulders. `low-air`: thin regolith plumes from impacts. | Protect the convoy from turrets; pods with escorts. |
| 3. Rille Crossing | 70–110 | 8,400–13,200 | 120 | medium | `far`: the floor of a sinuous rille (40% haze). `ground`: the rille rim and a road bridge across it. `low-air`: dust kicked up by the walkers. | The first walker, alone on the rille floor; then pairs. |
| 4. Landing Zone | 110–155 | 13,200–18,600 | 120 | medium, heavy peak 130–137 | `ground`: craters with split pod husks and the first teal Vrell growth. `low-air`: a regolith plume from a pod lander impact (the heavy peak). `high-air`: ejected rock streaks. | Walker pincer and pods together; the hidden cache. |
| 5. Terminal Gate | 155–190 | 18,600–22,800 | 120 | light | `far`: the mass-driver rail with sleds launching to orbit (scenery). `ground`: the terminal's armoured gate, landing lights, the road ending in the gate. | Final pods and walker; the convoy enters the gate. |

## Waves

Enemy specs: [Brood Pod](../../../enemies/air/brood-pod/README.md),
[Scuttler](../../../enemies/ground/scuttler/README.md),
[Needler](../../../enemies/air/needler/README.md),
[Skitter](../../../enemies/air/skitter/README.md). Scuttlers walk authored ground paths that
cross the convoy's road; they turn toward the column and claw it if they reach it.

| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 18 | 1 | single | [Brood Pod](../../../enemies/air/brood-pod/README.md) | 1 | front | **Introduction**: alone, above the base; bursts into 6 Skitters |
| 32 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | |
| 45 | 2 | carrier + escorts | [Brood Pod](../../../enemies/air/brood-pod/README.md) + [Needler](../../../enemies/air/needler/README.md) | 1 + 3 | front | Needlers `orbit` the pod |
| 60 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 3 | front | Over a turret nest |
| 75 | 3 | single | [Scuttler](../../../enemies/ground/scuttler/README.md) | 1 | front | **Introduction**: walks along the rille floor, turns up onto the road behind the convoy, showing its back |
| 88 | 3 | pincer | [Scuttler](../../../enemies/ground/scuttler/README.md) | 2 | sides | Walk in over both crater rims toward the convoy; edge warnings |
| 95 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 9 | front (alternating edges) | While the pincer walks in |
| 100 | 3 | single | [Brood Pod](../../../enemies/air/brood-pod/README.md) | 1 | front | |
| 115 | 4 | carrier + escorts | [Brood Pod](../../../enemies/air/brood-pod/README.md) + [Needler](../../../enemies/air/needler/README.md) | 1 + 3 | front | |
| 120 | 4 | convoy | [Scuttler](../../../enemies/ground/scuttler/README.md) | 2 | front | Walk down the road toward the column, claws first |
| 140 | 4 | pincer | [Scuttler](../../../enemies/ground/scuttler/README.md) | 2 | sides | Out of the heavy dust; edge warnings |
| 150 | 4 | line abreast | [Brood Pod](../../../enemies/air/brood-pod/README.md) | 2 | front | |
| 160 | 5 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 165 | 5 | single | [Scuttler](../../../enemies/ground/scuttler/README.md) | 1 | front | Comes out of the gate yard toward the convoy |

Totals: Brood Pod 6 (36 Skitters released) · Scuttler 8 · Needler 12 · Skitter 15 (waves).

## Ground targets

| Section | t (s) | Target | Effect |
|---|---|---|---|
| 2 | 40 | Turret nest: [Spine Turret](../../../enemies/ground/spine-turret/README.md) ×3 by the road | Fires at the player and the convoy |
| 2 | 62 | Turret nest: Spine Turret ×4 on boulders | As above |
| 3 | 105 | Spine Turret ×2 on the bridge abutments | As above |
| 4 | 145 | Turret nest: Spine Turret ×4 among the pod husks | As above |
| 4 | 125 | Collapsed prospector's dugout in a crater rim (**hardened**, 20 HP) | Only `anti-ground` weapons or the Airstrike open it; releases the hidden crate |
| 2 | 55 | CDF supply drop (ground layer, 3 HP) | Medium salvage (50); also a special charge if a special is fitted |

Spine Turrets: 13 in total. Scuttlers are ground enemies scripted in *Waves*.

## Hazards

None lethal. The danger is to the convoy: turrets and Scuttlers aim at whichever is closer, the
player or the nearest crawler. The heavy dust peak in section 4 never hides bullets, but it
hides the walkers' bodies (their lime backs glow through it).

## Secrets and pickups

Pickup types are defined in [player](../../../player/README.md#in-level-pickups).

- **Prospector's cache** (hidden crate, 100): the hardened dugout at t≈125. This is the first
  hardened target, placed where an `anti-ground` weapon (Bomb Rack, from L03) or a Hammer flight
  run can reach it.
- **Supply drop** (t≈55): medium salvage 50; plus one special charge if a special is fitted
  (otherwise nothing extra).
- **Armour patch** ×1: dropped by the last Needler of the t=115 group.
- **Overdrive**: dropped by the Brood Pod of the t=45 group (if it is killed before it bursts).
- Shield cells at the normal rate.

### Contextual prompt

- t=75 (first Scuttler), only if a special is fitted: "SPECIAL: call Hammer flight." This is
  the special prompt announced in L01 (prompts otherwise end after L03).

## Radio chatter

Text and radio blips only. Rook flies Aegis Two over Shackleton (radio only).

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "Five crawlers, four hundred civilians. Get them to the terminal, Lancer." |
| t=4 | Generic civilian (Crawler One) | "Crawler One rolling. We're slow and we're loud, Aegis. Please don't leave us." |
| t=18 (first pod) | Varga | "That's an egg sac. It hatches on its own in eight seconds. Kill it before then, and be ready for what comes out." |
| t=30 | Rook | "Aegis Two over Shackleton. My family's down there somewhere, Lancer. Bring those crawlers home." |
| First crawler hit | Generic civilian (convoy) | "We're taking fire! Crawler Three is hit!" |
| Crawler lost (first) | Generic civilian (Crawler One) | "We lost Four. Oh God, we lost Four." |
| Crawler lost (first) +3 s | Okafor | "Keep the rest moving, Lancer." |
| t=74 (first Scuttler) | Varga | "Walker on the rille floor. The claws are armour. Wait for it to turn, then hit the glowing back." |
| t=76, special fitted | Okafor | "Hammer flight is on station. Your call, Lancer." |
| Airstrike called | Generic CDF (Hammer Lead) | "Hammer flight, inbound!" |
| t=86 | Generic CDF (Tranquility Control) | "Walkers coming over the crater rims, both sides of the road!" |
| t=128 | Generic CDF (Tranquility Control) | "Lander impact, grid four. Dust everywhere. Stay on the convoy." |
| t=176 | Generic civilian (Crawler One) | "Terminal in sight! Open those doors!" |
| Level end, 5 crawlers | Okafor | "All five. Four hundred people. That's what we're for, Aegis." |
| Level end, 1–4 crawlers | Okafor | "We got most of them home. Most." |
| Mission failed | Okafor | "The convoy is gone, Lancer. Pull back." |
| Secondary met | Varga | "Not one sac hatched on its own. You're learning their rhythm." |

## Boss / mid-boss

None.

## Music & ambience

Track 4 *Act 1 A: Earth orbit & Luna* ("Afterburner", see the
[track list](../../../audio/music/README.md#track-list)), base stem only through sections 1–2
(Luna starts tense and sparse), intensity stem from the first walker. Ambience: Luna
([sfx](../../../audio/sfx/README.md#ambience-per-setting)) plus crawler engine rumble while the
convoy is on screen. Mission failed sting on convoy loss.

## Credit budget

Budget(4) = 1,000 × 1.07³ ≈ **1,225** ([economy](../../../systems/economy/README.md#per-level-budget)).
Bounties from the stat blocks: Brood Pod 20 (8 if it bursts on its own), Skitter 5, Scuttler 25,
Needler 12, Spine Turret 12.

| Source | Credits (medium) |
|---|---|
| Kills: Brood Pod 6 × 20 + released Skitters 36 × 5 + Skitter 15 × 5 + Scuttler 8 × 25 + Needler 12 × 12 | 719 |
| Ground targets: Spine Turret 13 × 12 | 156 |
| Primary objective: 5 crawlers × 30 | 150 |
| Pickups: prospector's cache 100 (8%) + supply drop 50 | 150 |
| Secondary: quick hands | 50 |
| **Total** | **1,225** |

## Difficulty notes

- **Easy**: crawlers have 90 HP; Scuttlers walk 20% slower; pods burst after 10 s (stat-block
  hook).
- **Hard**: crawlers have 45 HP; Scuttler fans are 7-way and pods release 8 Skitters
  (stat-block hooks); the t=140 pincer has two Scuttlers per side.

## Implementation

- [ ] Scroll timeline, sections, atmosphere intensity and parallax content as in *Layout*.
- [ ] Convoy: five ground-layer crawlers following the road spline, 60 HP, damaged only by
      ground-enemy bullets and Scuttler claws; HUD objective tracker with five pips.
- [ ] Escort objective: +30 per arriving crawler; mission failed when all five are lost.
- [ ] Scuttler walk paths cross the road and turn toward the convoy.
- [ ] Wave script matches the *Waves* table.
- [ ] Hardened dugout: only `anti-ground` and the Airstrike damage it.
- [ ] Supply drop pays a special charge only if a special is fitted.
- [ ] Special prompt at t=75 (once, only with a special fitted).
- [ ] Radio cues fire at their triggers, including the per-outcome end lines.
- [ ] Credit total at medium with perfect collection is 1,225 (± 5%).
- [ ] Easy/hard variations as in *Difficulty notes*.

## Open questions

- None open.

## Decisions

- 2026-10-01: Promoted from the act roster to a draft level document. Escort rules: only
  bullets from ground-layer enemies and Scuttler claws hurt crawlers, so air enemies threaten the
  player and ground enemies threaten the convoy. The mass driver is scenery here and becomes a
  hazard in L05. First hardened target placed as the secret, so `anti-ground` pays off without
  being mandatory.
