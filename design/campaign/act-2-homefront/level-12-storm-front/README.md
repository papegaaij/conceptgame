---
title: Level 12 – Storm Front
design: draft
implementation: not-started
art: chosen
depends-on: [../../../enemies/air/lamprey, ../../../enemies/air/whirl-seed]
updated: 2026-10-01
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

## Briefing

> **Commander Okafor:** "Lancer, Aegis Actual. A storm system has formed off the Azores, and it's
> moving onto the convoy lanes faster than any storm should. Vrell flyers are using it as cover.
> Clear the storm front so the next convoy can follow. Visibility will be poor and contacts will
> come from every side. Homing weapons will help when you can't see what you're shooting."
>
> **Dr. Varga:** "The pressure readings make no sense. Three CDF weather sondes are still
> transmitting inside the front. Fly through them for me and I can tell you why."

*Hangar teaser* (shop screen before L12): "A storm that formed in six hours. Contacts from every
side: homing weapons recommended."

*Varga's intel line*: "They'll come from all sides, Lancer, and the cloud hides them. Homing
missiles see through rain. Spread fire for the seeds."

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `air`, `low-air` (Spore Bombers, cloud banks) |
| Attack directions | all: front 61% · sides 22% · rear 17% |
| Density | 4 |
| Recommended traits | `homing`, `spread` |
| Hazards | Wind gusts (lateral push), low visibility (rain, cloud banks), lightning flashes |
| Boss / mid-boss | none (finale: breakout from the storm, attacks from all sides) |
| Sensor-suite detail | **none**: ocean storm, air + low-air, front. **L1**: front 61 / sides 22 / rear 17, density 4, hazards "wind, low visibility". **L2**: Lamprey (new), Whirl Seed, Needler, Wraith, Mantis, Spore Bomber, Skitter portraits. **L3**: `homing` and `spread` highlighted; the timeline strip marks the storm wall (125–150 s) and the eye (150–175 s); 1 secret. |

## Objective

- **Primary** `reach-end`: survive to the end of the scroll.
- **Secondary**: collect the data from all three CDF weather sondes (fly through each). Pays +120
  credits and unlocks Varga's follow-up line.

## Layout

Ground scroll speed **140 px/s** (the chosen storm scene). Total ≈ 205 s ≈ 28,700 px.

**Motion budget** (see [art direction](../../../art-direction/README.md)): the sea swell and
the rain are the only two strongly animated background elements. Wind streaks and whitecaps
stay faint, the scud only sways, and no element moves more than 2 px per frame. Lightning is a
single short flash (≤ 0.1 s, softened by the flash-reduction option). In the storm-wall peak
the rain gets denser while the swell drops in contrast, so there are still only two strong
elements. In the eye the rain stops and the cloud wall turns slowly.

| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Squall Line | 0–25 | 0–3,500 | 140 | `light` | `deep`: dark grey swell. `low-air`: the first scud banks at the edges. `high-air`: rain starts at t=10 (faint, motion-blurred streaks). One demonstration gust at t=16. | Weather introduced with no enemies; the gust telegraph learned. |
| 2. Rain Wall | 25–75 | 3,500–10,500 | 140 | `medium` | `deep`: heaving swell, sparse torn crests. `low-air`: scud banks (~20–25%), opaque, below the play plane. `high-air`: steady rain (≤ 40%). The first sonde blinks in the rain at t≈60. | Lamprey introduction; seeds on the wind. |
| 3. Lightning Field | 75–125 | 10,500–17,500 | 140 | `medium` | As section 2, plus lightning every 6–10 s. Each flash lights the cloud banks from inside for 0.5 s and reveals what hides under them. Second sonde at t≈110. | Low visibility; attacks from the sides and rear. |
| 4. Storm Wall | 125–150 | 17,500–21,000 | 140 | `heavy` 130–145 (5 s ramps each side) | `low-air`: banks at 30–40% leaving the centre lane readable. `high-air`: dense rain (≤ 40%). A steady 20 px/s drift to the right plus gusts. | The single heavy peak: dense whirl clusters and a Lamprey swarm. |
| 5. The Eye | 150–175 | 21,000–24,500 | 140 | `clear` | `deep`: calm dark sea with a shaft of pale light. `far`: the storm's cloud wall all round, turning slowly clockwise. No rain. Third sonde at t≈165. | Calm; Varga sees the Needlers circling with the storm. |
| 6. Breakout | 175–205 | 24,500–28,700 | 140 | `medium` → `light` by t=200 | Back through the wall into rain, which thins toward the end; the sky lightens at the top edge. | Last waves from all sides, then out of the storm. |

## Waves

Enemy definitions are linked per row; formation names come from the
[formation vocabulary](../../../enemies/README.md#formation-vocabulary). Returning units use the
act HP factor from the [balancing basis](../../../enemies/README.md#balancing-basis); HP values
live in the enemy specs only. Whirl clusters are released from a point just outside an edge
(seed pods bursting on the wind, telegraphed by a puff of spores).

| t (s) | Section | Formation | Enemies | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 32 | 2 | stream | [Lamprey](../../../enemies/air/lamprey/README.md) | 3 | front | **New.** One at a time, 1.5 s apart, nothing else on screen; first latch shows the shake-off prompt |
| 40 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 46 | 2 | whirl cluster | [Whirl Seed](../../../enemies/air/whirl-seed/README.md) | 6 | sides (left, on the wind) | The wind bends their spiral to the right |
| 58 | 2 | swarm | Lamprey | 4 | front | |
| 64 | 2 | line abreast | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 2 | front (`low-air`) | Under the scud banks; spores rise to the play plane |
| 70 | 2 | line abreast | Needler | 3 | front | |
| 80 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 10 | front (alternating edges) | |
| 88 | 3 | swarm | Lamprey | 5 | sides (out of the cloud banks) | Revealed by the lightning flash at t=87 |
| 96 | 3 | whirl cluster | Whirl Seed | 6 | front | |
| 104 | 3 | rear ambush | [Wraith](../../../enemies/air/wraith/README.md) | 2 | rear | The first one drops an overdrive |
| 112 | 3 | stream | Lamprey | 4 | rear | |
| 120 | 3 | pincer | Needler | 4 | sides | |
| 124 | 3 | pincer | [Mantis](../../../enemies/air/mantis/README.md) | 2 | sides | Laser sweeps across the rain |
| 132 | 4 | whirl cluster | Whirl Seed | 6 | front | |
| 136 | 4 | swarm | Lamprey | 6 | front | Out of the heavy banks |
| 140 | 4 | whirl cluster | Whirl Seed | 6 | sides (left, on the wind) | |
| 146 | 4 | line abreast | Spore Bomber | 2 | front (`low-air`) | |
| 158 | 5 | circle | Needler | 8 | front | Orbits the eye's centre clockwise with the storm for 8 s before breaking; the leader drops an overdrive |
| 166 | 5 | rear ambush | Wraith | 3 | rear | |
| 176 | 6 | snake | Skitter | 8 | front | |
| 186 | 6 | swarm | Lamprey | 5 | rear | |
| 194 | 6 | whirl cluster | Whirl Seed | 6 | front | |
| 196 | 6 | V-wing | Needler | 5 | front | |

Totals: Lamprey 27 · Whirl Seed 30 · Needler 25 · Wraith 5 · Spore Bomber 4 · Skitter 18 ·
Mantis 2. Of 23 waves, 14 enter from the front, 5 from the sides and 4 from the rear.

## Ground targets

None. L12 is an air level over open sea.

## Hazards

- **Wind gusts**: a gust pushes the player's ship (and Rook's) sideways at 40 px/s for 2.5 s.
  It is telegraphed 1.0 s ahead: the wind streaks brighten and an arrow shows at the upwind
  edge. Whirl Seeds and Lampreys drift with it (+40 px/s); **enemy bullets do not**, for
  readability. Gusts come at t=16 (demo), 52, 90, 118 and 182, and every 6 s inside the
  storm wall on top of its steady 20 px/s drift. There are none in the eye.
- **Low visibility**: opaque cloud banks on `low-air` may hide enemy bodies, never bullets.
  Enemies under a bank show a faint glow, and lightning reveals them fully.
- **Lightning**: a flash only, no damage. It obeys the flash-reduction option.

## Secrets and pickups

- **Drifting canister** (t≈100): a CDF supply canister drifts inside a cloud bank on the right,
  visible only in lightning flashes. Three hits break it open: **210** credits.
- **Weather sondes** ×3 (t≈60, 110, 165): blinking buoys on the play plane; flying through one
  collects its data (secondary objective). They can't be shot.
- **Overdrive** ×2: from the first Wraith of the t=104 ambush and the t=158 Needler circle's
  leader.
- **Armour patch**: floating in the eye at t≈152. Shield cells drop more often than usual in
  this level (×1.5 drop rate), to offset the Lamprey drain.

## Radio chatter

| Trigger | Speaker | Line |
|---|---|---|
| t=2 | Okafor | "Weather service calls it the worst Atlantic storm in forty years. It formed in six hours. Stay together." |
| t=14 | Rook | "Feel that? The wind's trying to take the stick off me." |
| t=30 | Varga | "Eel-shaped contacts. They latch on and drain your shield. Shake hard to throw them off." |
| First latch | Rook | "You've got a passenger! Shake it off!" |
| t=44 | Rook | "Seeds on the wind, left side!" |
| t=58 (first sonde in view) | Varga | "There's a CDF sonde out there. Fly through it and I can read its data." |
| t=86 | Varga | "Lightning shows what's in the clouds. Watch the flashes." |
| t=102 | Rook | "Six o'clock, Lancer!" |
| t=128 | Okafor | "Storm wall ahead. Instruments will be useless in there. Fly by eye." |
| t=152 | Rook | "Blue sky. I'd almost forgotten what it looks like." |
| t=156 | Varga | "Lancer, look at them. They're circling with the wind. They aren't caught in this storm. They're steering it." |
| t=158 | Okafor | "Say again, Doctor?" |
| t=159 | Varga | "They're herding the weather, Commander. They understand our climate better than we do." |
| t=164 | The Choir | "[the Choir sings]" |
| t=200 | Rook | "Next time, I'm bringing an umbrella." |
| Level end | Okafor | "You're through. The storm is breaking up behind you, as if someone let it go." |
| Secondary objective met | Varga | "Three sondes, full data. They fed the storm, Commander. I'll have the pattern by morning." |

## Boss / mid-boss

None. The breakout (section 6) is the finale.

## Music & ambience

L12 uses the Act 2 B theme "Firestorm" (track 7 in the
[track list](../../../audio/music/README.md#track-list); no separate storm variant), with the intensity stem on from the storm wall to
the end of the eye. The base stem drops out in the eye and returns at the breakout. Ambience: the
[ocean storm ambience](../../../audio/sfx/README.md#ambience-per-setting) (rain and thunder), its
thunder synchronised with the lightning flashes.

## Credit budget

The total is within 0.2% of budget(12) = **2,105** from the
[economy](../../../systems/economy/README.md#per-level-budget) curve. Bounties are the spec values ×
the act factor 1.6, rounded per kill: Skitter 8, Needler 19, Lamprey 19, Whirl Seed 5, Wraith 48,
Spore Bomber 40, Mantis 48. Spores (2 each) are not budgeted, because their number depends on how
long the bombers live.

| Source | Credits (medium) |
|---|---|
| Kills: Lamprey 27 × 19 + Whirl Seed 30 × 5 + Needler 25 × 19 + Wraith 5 × 48 + Spore Bomber 4 × 40 + Skitter 18 × 8 + Mantis 2 × 48 | 1,778 |
| Ground targets | 0 |
| Secret: drifting canister | 210 |
| Secondary objective | 120 |
| **Total** | **2,108** |

## Difficulty notes

- **Easy**: gusts push at 25 px/s with a 1.5 s telegraph; the storm wall has no steady drift;
  the t=136 Lamprey swarm has 4.
- **Hard**: gusts push at 55 px/s and also come at t=34 and t=72; a Mantis pincer joins the eye
  at t=170; the canister needs five hits.

## Concept art

No level-specific concept files. The look comes from the chosen storm scene in
[art direction](../../../art-direction/README.md):
[scene-storm-r10-a.png](../../../art-direction/concept/scene-storm-r10-a.png) and its
[scroll loop](../../../art-direction/concept/scene-storm-r10-a.gif), which shows the storm-wall
peak; the other sections run lighter. The eye of the storm has no concept yet.

## Implementation

- [ ] Scroll timeline, sections, atmosphere intensity and parallax content per layer as in *Layout*; heavy only from 130 to 145 s.
- [ ] Motion budget: sea and rain as the only strong elements; lightning flash ≤ 0.1 s, flash-reduction option honoured.
- [ ] Weather system: rain, lightning reveals under cloud banks, wind gusts with a 1.0 s telegraph pushing the ships and light enemies but not bullets.
- [ ] Wave script matches the *Waves* table.
- [ ] Weather sondes (secondary) and the drifting-canister secret.
- [ ] Radio chatter cues fire at their triggers.
- [ ] Credit total at medium with perfect collection is 2,105 (± 5%).
- [ ] Easy/hard variations as in *Difficulty notes*.

## Decisions

- 2026-10-01: Promoted from the Act 2 roster to a draft level document.
- 2026-10-01: Heavy atmosphere only as a 15 s peak at the storm wall; sea and rain are the two
  strong elements everywhere (motion budget, round 09 feedback).
- 2026-10-01: Open question resolved: L12 keeps "Firestorm" (Act 2 B); no separate storm variant (user decision).
