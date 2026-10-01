---
title: Level 05 – Crater Nest
design: draft
implementation: not-started
art: chosen
depends-on: [../../../enemies/ground/polyp-mortar, ../../../enemies/bosses/gorgon-frigate, ../../../world/luna]
updated: 2026-10-01
---

# Level 05 – Crater Nest

## Summary

The Tranquility pods came from a Vrell nest growing in a crater beside the mass-driver line.
Lancer runs the still-firing mass driver, drops over the crater rim and must destroy all four
nest batteries on the crater floor (`destroy-targets`; a battery left alive fails the mission).
When the last battery falls, the Gorgon Frigate drops out of orbit to defend the nest: the first
mid-boss and the first multi-part enemy. Introduces the Polyp Mortar, the mass-driver sleds and
low-gravity debris. About 3½ minutes including the mid-boss.

## Briefing

> **Commander Okafor:** "Lancer, the pods at Tranquility weren't random. Survey drones found
> where they came from: a crater beside the mass-driver line, and the Vrell are growing a nest
> in it. Four batteries of acid-throwers and turrets guard the floor. Destroy all four. If a
> battery is still alive when you pass it, the nest survives and the mission fails. There's no
> second run at that crater today. And stay clear of the rail. The mass driver is still firing
> on automatic."
>
> **Dr. Varga:** "The acid-throwers mark their target a second before it lands. Keep moving and
> you'll be fine. And something in orbit keeps watching that crater. I don't like it."

*Hangar teaser* (shop screen after L04):
> **Dr. Varga:** "Turrets, mortars and a lot of ground to cover. Bring something that hits the
> surface, and something wide for the flanks."

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `ground` (mortars, turrets, the batteries), `air` |
| Attack directions | front 75% · sides 25% |
| Density | 3 |
| Recommended traits | `anti-ground`, `spread` |
| Hazards | Mass-driver sleds (section 1); low-gravity debris from destroyed ground targets |
| Boss / mid-boss | Mid-boss: Gorgon Frigate |
| Sensor-suite detail | none: Luna, `ground` + `air`, front · L1: + directions, density 3, hazards "sleds, debris", "destroy 4 batteries" · L2: + Polyp Mortar, Spine Turret, Stinger, Needler, Brood Pod portraits; mid-boss name and silhouette · L3: + `anti-ground` highlighted, wave strip, 1 secret |

## Objective

- **Primary** `destroy-targets`: destroy nest batteries A, B, C and D. A battery is destroyed
  when all four of its units (2 Polyp Mortars + 2 Spine Turrets) are dead. **Fails** at the
  moment a battery's last unit scrolls past the bottom edge alive (this level reports the failure
  at once rather than at the end of the scroll, so the player never fights the mid-boss for
  nothing). After battery D the mid-boss must be killed to finish the level (scroll halts in its
  arena until it dies).
- **Secondary** *Scorched crater*: destroy every Polyp Mortar and Spine Turret in the level, in
  the batteries or not (14 + 16). +40.

## Layout

Scene: the chosen [Luna](../../../world/luna/README.md) scene
([sheet](../../../art-direction/concept/scene-luna-r06-a.png)) with its mass-driver rail and
Vrell nest crater. Scroll speed 150 px/s (normal), 30 px/s in the mid-boss arena (per the
[Gorgon Frigate](../../../enemies/bosses/gorgon-frigate/README.md) spec). Total ≈ 215 s ≈ 25,650 px.
Motion budget: the sleds and the regolith plumes in section 1; plumes and the nest's vents
afterwards.

| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Parallax content per layer | Purpose |
|---|---|---|---|---|---|---|
| 1. Mass-Driver Line | 0–35 | 0–5,250 | 150 | clear | `deep`: black sky, Earth on the horizon. `ground`: the mass-driver rail up the right third of the field, its signal lights, ore sidings. `high-air`: launch sleds streaking up the rail. | Learn the sled rhythm; the first Polyp Mortar alone. |
| 2. Crater Rim | 35–75 | 5,250–11,250 | 150 | light | `far`: the crater floor seen past the rim (perspective walls from rim at 1.0 to the floor at 0.6). `ground`: the outer rim, boulders, a broken survey drone. `low-air`: regolith plumes from mortar impacts. | Battery A; side waves. |
| 3. Nest Floor | 75–125 | 11,250–18,750 | 150 | medium, heavy peak 115–122 | `ground`: the crater floor coated in teal creep, pod husks, growth tendrils. `low-air`: spore-dust venting from the nest (the heavy peak). `high-air`: ejected rock streaks. | Batteries B and C; Brood Pods; low-gravity debris. |
| 4. Nest Heart | 125–150 | 18,750–22,500 | 150 | light | `ground`: the central growth, a ring of lime-veined mounds around battery D. `low-air`: thin plumes. | Battery D; the frigate's shadow falls over the floor. |
| 5. Gorgon Frigate | 150–205 | 22,500–24,150 | 30 | clear | `ground`: the nest heart below (no terrain collision). `air`: the frigate in the upper third. | Mid-boss fight (≈ 55 s at medium). |
| 6. Lift-off | 205–215 | 24,150–25,650 | 150 | light | `ground`: the far rim, the burning nest behind. | Credit shower, end. |

## Waves

Enemy specs: [Polyp Mortar](../../../enemies/ground/polyp-mortar/README.md),
[Spine Turret](../../../enemies/ground/spine-turret/README.md),
[Stinger](../../../enemies/air/stinger/README.md),
[Needler](../../../enemies/air/needler/README.md),
[Skitter](../../../enemies/air/skitter/README.md),
[Brood Pod](../../../enemies/air/brood-pod/README.md),
[Gorgon Frigate](../../../enemies/bosses/gorgon-frigate/README.md).

| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 10 | 1 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | Left of the rail |
| 20 | 1 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front | Snakes across the rail between sleds |
| 38 | 2 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | |
| 48 | 2 | pincer | [Needler](../../../enemies/air/needler/README.md) | 4 | sides | Hold at the edges above battery A |
| 62 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 8 | sides (alternating) | |
| 70 | 2 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 4 | front | |
| 80 | 3 | single | [Brood Pod](../../../enemies/air/brood-pod/README.md) | 1 | front | |
| 95 | 3 | carrier + escorts | [Brood Pod](../../../enemies/air/brood-pod/README.md) + [Needler](../../../enemies/air/needler/README.md) | 1 + 4 | front | Over battery B |
| 110 | 3 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | sides (right) | Over battery C; edge warning |
| 118 | 3 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front | In the heavy peak |
| 135 | 4 | single | [Brood Pod](../../../enemies/air/brood-pod/README.md) | 1 | front | Over battery D |
| 150 | 5 | mid-boss | [Gorgon Frigate](../../../enemies/bosses/gorgon-frigate/README.md) | 1 | front (descends from above) | Skitter streams of 6 between phase-1 bursts every 10 s (2 streams at medium par) |

Totals: Needler 12 · Skitter 20 (+ 12 in the frigate's streams) · Stinger 10 · Brood Pod 3
(18 Skitters released) · Gorgon Frigate 1.

## Ground targets

| Section | t (s) | Target | Effect |
|---|---|---|---|
| 1 | 28 | [Polyp Mortar](../../../enemies/ground/polyp-mortar/README.md) ×1 on the rail embankment | **Introduction**: alone, between two sleds; radio tip |
| 2 | 40 | [Spine Turret](../../../enemies/ground/spine-turret/README.md) ×2 on the rim | Bounty |
| 2 | 55 | **Battery A**: Polyp Mortar ×2 + Spine Turret ×2 | Objective target |
| 2 | 70 | Polyp Mortar ×2 on the inner rim | Bounty |
| 3 | 88 | **Battery B**: Polyp Mortar ×2 + Spine Turret ×2 | Objective target |
| 3 | 100 | Spine Turret ×2 | Bounty |
| 3 | 108 | **Battery C**: Polyp Mortar ×2 + Spine Turret ×2 | Objective target; just before the heavy peak |
| 3 | 120 | Polyp Mortar ×1 + Spine Turret ×2 | Bounty |
| 4 | 135 | **Battery D**: Polyp Mortar ×2 + Spine Turret ×2 around the nest heart | Objective target; its death triggers the frigate |
| 4 | 145 | Polyp Mortar ×2 + Spine Turret ×2 | Bounty |

Totals: Polyp Mortar 14 · Spine Turret 16. Each battery's units are outlined in the HUD's
objective colour and the tracker shows A–D.

## Hazards

- **Mass-driver sleds** (section 1, as defined in [Luna](../../../world/luna/README.md#hazards--set-pieces)):
  every 5 s a sled shoots up the rail, a vertical line 24 px wide across the whole field at
  x ≈ 600. Lights along the rail chase upward for 1.5 s before each sled. Contact damage 15;
  sleds block shots for the 0.4 s they are on screen.
- **Low-gravity debris** (sections 2–4): a destroyed ground target throws 2–3 rocks in slow
  arcs onto the `air` layer (drift 40–60 px/s, fall back after 2.5 s). Contact damage 6;
  shootable (1 HP, no credits). Never thrown within 72 px of the player.
- **Mortar impacts**: per the Polyp Mortar spec (lime marker 1 s ahead).

## Secrets and pickups

Pickup types are defined in [player](../../../player/README.md#in-level-pickups).

- **Stuck sled** (hidden crate, 65): an ore canister sits jammed halfway up the rail at t≈30.
  Its clamp can be hit only while the rail lights are dark (between sleds); 3 hits free it.
- **Armour patch** ×1: dropped by the last unit of battery C.
- **Overdrive**: dropped by the Brood Pod of the t=95 group.
- **Special charge**: a CDF supply canister floats down at t≈147, just before the frigate
  (only pays if a charge-based special is fitted).
- Shield cells at the normal rate.

## Radio chatter

Text and radio blips only. Rook flies Aegis Two on the far side of the crater (radio only).

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "There's the crater, Aegis. Four batteries on the floor. All four die, or this was for nothing." |
| t=5 | Generic CDF (Driver Control) | "Mass driver's still on automatic, Aegis. Sleds every five seconds. Watch the rail lights." |
| t=27 (first mortar) | Varga | "That one lobs acid. It marks where it'll land, a second ahead. Don't be there." |
| t=46 (pincer) | Rook | "Aegis Two here. Two groups, both flanks of you, Lancer!" |
| Battery A / B / C destroyed | Okafor | "Battery down." / "Two." / "Three. One left." |
| Battery D destroyed | Okafor | "That's the last battery. The nest is open." |
| Battery passes alive | Okafor | "Battery … is behind you. The nest survives. Pull back, Lancer." |
| t=114 | Generic CDF (Driver Control) | "The nest is venting. Dust everywhere. Your scopes will clear in a few seconds." |
| t=148 | The Choir (distorted) | "[the Choir sings]" |
| t=149 | Generic CDF (Driver Control) | "Big contact dropping out of orbit, right on top of the crater!" |
| t=151 | Varga | "A warship. Three heads, all guns. Take the heads one at a time; then the crown opens. Hit the core." |
| t=154 | Rook | "Oh, it has *heads*. Of course it has heads." |
| Last head destroyed | Varga | "Crown's opening. That's the core. Lime glow, Lancer!" |
| Frigate destroyed | Okafor | "Frigate down. The nest is dead. Get out of there, Aegis." |
| Level end | Okafor | "Good work. Varga wants samples. I told her no." |
| Secondary met | Varga | "Every growth in that crater burned. We'll have nothing to study. Well done, I suppose." |

## Boss / mid-boss

[Gorgon Frigate](../../../enemies/bosses/gorgon-frigate/README.md), the first mid-boss. Level
notes:

- **Intro**: triggered by battery D's death. The frigate's shadow crosses the nest heart, then
  it descends into the upper third at 60 px/s; mini-boss sting, name and short health bar.
- **Arena**: above the nest heart; the scroll slows to 30 px/s and no terrain collides (as in
  the spec's arena line).
- **Duration**: 1,500 HP at an effective 0.6 × 45 = 27 DPS ≈ 55 s, inside the 45–75 s target.
- Ground targets are all dead by now (battery D is the last), so the fight is air only.
- On easy and medium, the boss checkpoint is recorded at the mini-boss sting
  ([retry](../../../systems/retry/README.md)).

## Music & ambience

Track 4 *Act 1 A: Earth orbit & Luna* ("Afterburner", see the
[track list](../../../audio/music/README.md#track-list)); track 21 mini-boss sting at the
frigate's entrance, then back to the level track with the intensity stem on. Ambience: Luna
([sfx](../../../audio/sfx/README.md#ambience-per-setting)) plus a rising electric whine on the
rail before each sled.

## Credit budget

Budget(5) = 1,000 × 1.07⁴ ≈ **1,311** ([economy](../../../systems/economy/README.md#per-level-budget)).
Bounties from the stat blocks: Polyp Mortar 15, Spine Turret 12, Stinger 15, Needler 12,
Skitter 5, Brood Pod 20, Gorgon Frigate 200 (≈ 15% of the budget).

| Source | Credits (medium) |
|---|---|
| Kills: Stinger 10 × 15 + Needler 12 × 12 + Skitter 20 × 5 + Brood Pod 3 × 20 + released Skitters 18 × 5 | 544 |
| Mid-boss: Gorgon Frigate (heads 3 × 30 + core 110) + 2 Skitter streams 12 × 5 | 260 |
| Ground targets: Polyp Mortar 14 × 15 + Spine Turret 16 × 12 | 402 |
| Pickups: stuck sled 65 (5%) | 65 |
| Secondary: scorched crater | 40 |
| **Total** | **1,311** |

The frigate's Skitter streams depend on how long phase 1 lasts; the budget counts the two
streams of a medium-par fight.

## Difficulty notes

- **Easy**: batteries have 1 Polyp Mortar + 1 Spine Turret; sleds every 8 s; no low-gravity
  debris.
- **Hard**: mortars burst into 12-bullet rings and the frigate's last head fires 5-shot bursts
  (stat-block hooks); sleds every 4 s; battery D has a third mortar.

## Implementation

- [ ] Scroll timeline, sections, atmosphere intensity and parallax content as in *Layout*,
      including the 30 px/s arena.
- [ ] Battery objective: four groups of four units, HUD tracker A–D, immediate mission failure
      when a battery's last unit leaves the screen alive.
- [ ] Wave script matches the *Waves* table; the frigate is triggered by battery D's death.
- [ ] Mass-driver sleds with the 1.5 s light telegraph, contact damage 15.
- [ ] Low-gravity debris thrown by destroyed ground targets.
- [ ] Stuck-sled secret hittable only between sleds.
- [ ] Gorgon Frigate per its spec; boss checkpoint on easy/medium.
- [ ] Radio cues fire at their triggers, including per-battery lines.
- [ ] Credit total at medium with perfect collection is 1,311 (± 5%).
- [ ] Easy/hard variations as in *Difficulty notes*.

## Decisions

- 2026-10-01: Promoted from the act roster to a draft level document. The named targets are four
  batteries built from existing units (no new art). A battery missed fails the mission at once,
  not at the end of the scroll, so the mid-boss is only fought on a run that can still succeed.
  The mass-driver sleds (Luna's signature set piece) are introduced here as a hazard.
- 2026-10-01: Open question resolved: the Gorgon Frigate spec's arena now is the nest crater on Luna.
