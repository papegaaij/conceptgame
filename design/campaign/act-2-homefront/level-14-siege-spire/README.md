---
title: Level 14 – Siege Spire
design: review
implementation: not-started
art: chosen
depends-on: [../../../enemies/bosses/siege-spire]
updated: 2026-10-01
---

# Level 14 – Siege Spire

## Summary

The Act 2 finale. A three-kilometre Vrell citadel, the **Siege Spire**, has grown through the
heart of **Geneva Concord**, the UTC capital, and its canopy shadows the whole old town. The
Coalition throws everything it has left at it. Lancer and Rook go in first, along the lakefront
and through the old town, where the Spire's roots burst out of the streets and grow guns, to the
Concord rotunda where the trunk stands. There they fight the act boss, the
[Siege Spire](../../../enemies/bosses/siege-spire/README.md). The boss sums up the act: ground
targets first, then attacks from the rear, then dodging in the open. About 1 minute 50 seconds
of approach plus the boss fight (≈ 140 s).

## Briefing

> **Commander Okafor:** "Lancer, Aegis Actual. Two nights ago something grew out of the heart of
> Geneva Concord. It is three kilometres tall and it is still growing. We call it the Siege Spire.
> The Vrell across Earth fight as one, and Dr. Varga believes the Spire is what binds them. Its
> roots run under the whole old town, and they grow guns where they break the surface. Tonight
> the Coalition puts everything it has left into the air. Your flight goes in first: along the
> lakefront, through the old town, to the Concord rotunda where the trunk stands. Destroy the
> Spire. Aegis Actual out."

*Hangar teaser* (shop screen before L14): "The Siege Spire. Roots on the ground, flyers from
behind, then a fight in the open. Anti-ground and piercing recommended."

*Varga's intel line*: "Bring anti-ground for the roots and something that pierces. When the crown
opens, look behind you."

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `ground` (roots, root-burst growths, hive nodes, Creepers, Ravagers), `air`, `high-air` (cloaked Wraiths, the canopy) |
| Attack directions | front 71% · rear 29% (the approach); in the boss fight the maw's Wraiths loop round to the rear |
| Density | 4 |
| Recommended traits | `anti-ground`, `piercing` (`rear` helps against the Wraiths) |
| Hazards | Root bursts (telegraphed, ground layer, no collision); low light under the canopy |
| Boss / mid-boss | Act boss [Siege Spire](../../../enemies/bosses/siege-spire/README.md) |
| Sensor-suite detail | **none**: Geneva Concord, ground + air, front. **L1**: front 71 / rear 29, density 4, hazard "root bursts". **L2**: Creeper, Ravager, Hive Node, Wraith, Needler, Spine Turret, Polyp Mortar portraits; boss name and silhouette. **L3**: `anti-ground` and `piercing` highlighted; the timeline strip marks the root field (60–95 s) and the boss (≈113 s); 1 secret. |

## Objective

- **Primary** `boss`: destroy the Siege Spire.
- **Secondary**: destroy all 8 root-burst growths in the root field (4 Spine Turrets and 4 Polyp
  Mortars) before they leave the screen. Pays +100 credits.

## Layout

Approach at **140 px/s** (the chosen Geneva scene), then the boss arena. During boss phase 1 the
scroll creeps at 15 px/s (spec), so the roots come into range in turn; after that it holds. Times
assume a ≈ 140 s fight (the spec's estimate at the effective boss DPS). Total ≈ 265 s ≈ 17,800 px.
Motion budget: the strong elements are the street traffic and lamps' flicker in sections 1–2
(abandoned cars standing still, only a few moving) and the root bursts and dust in section 3.
The canopy shadow drifts slowly.

| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Lakefront | 0–20 | 0–2,800 | 140 | `light` (canopy haze) | `deep`: Lake Geneva at dusk, dark water. `ground`: the lakefront quays, the Jet d'Eau plume (leaning away from the screen centre, perspective rule), the Rhône bridges, a CDF armoured car half under creep on the Pont du Mont-Blanc (the secret). `low-air`: Hammer flight bombers passing below (scenery). `high-air`: the canopy's edge as a soft shadow (≤ 40%). | Set-up; no enemies before t=24. |
| 2. Old Town | 20–60 | 2,800–8,400 | 140 | `light` | `ground`: perimeter blocks of row houses with ridged roofs and courtyards, the cathedral towers (leaning, perspective rule), sodium-lit streets, parks and tree rows, creep veins along the gutters, the Place du Molard with two hive nodes. `high-air`: canopy shadow. | Creepers in the streets, a turret nest on the cathedral square, the hive-node pair. |
| 3. Root Field | 60–95 | 8,400–13,300 | 140 | `medium` (spore haze) | `ground`: streets broken by roots as thick as trams; four root bursts split the paving (1 s dust telegraph each) and grow a Spine Turret and a Polyp Mortar; creep over the roofs. `low-air`: spore-haze banks lit violet from below. | The signature set piece: roots growing guns. Ravagers out of the craters; the first Wraiths from behind. |
| 4. Concord Approach | 95–113 | 13,300–15,820 | 140 | `medium` → `clear` by t=108 | `ground`: avenues converging on the Concord rotunda's roundabout; the Spire's trunk rising out of it, leaning away from the centre; roots radiating along the avenues. `high-air`: the canopy directly overhead. | Last Wraiths; the Choir's song; the boss warning at t=108. |
| 5. Siege Spire | 113–253 | 15,820–≈16,550 | 15 in phase 1, then 0 | `clear` | `ground`: the arena around the rotunda, the trunk at the top, four turret-pod roots and two mortar roots spread along the avenues. Phase 3: the stump and the ground debris after the tear-free shockwave. `high-air`: canopy shadow only. | The boss fight. |
| 6. Daylight | 253–265 | ≈16,550–≈18,230 | 140 | `clear` | The canopy withers and tears; low sunlight falls across the old town, the lake and the stump. | Release; act complete. |

## Waves

Enemy definitions are linked per row; formation names come from the
[formation vocabulary](../../../enemies/README.md#formation-vocabulary). Returning units use the
act HP factor from the [balancing basis](../../../enemies/README.md#balancing-basis); HP values
live in the enemy specs only.

| t (s) | Section | Formation | Enemies | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 24 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | Over the rooftops |
| 32 | 2 | convoy | [Creeper](../../../enemies/ground/creeper/README.md) | 3 | front | Along the Rue du Rhône |
| 40 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front | Weaves between the cathedral towers |
| 50 | 2 | convoy | Creeper | 3 | front | Down from the cathedral hill |
| 72 | 3 | pack | [Ravager](../../../enemies/ground/ravager/README.md) | 4 | front | Out of a root-burst crater; the leader drops an overdrive |
| 82 | 3 | rear ambush | [Wraith](../../../enemies/air/wraith/README.md) | 2 | rear | Cloaked on `high-air` under the canopy |
| 104 | 4 | rear ambush | Wraith | 2 | rear | |
| boss phase 2 | 5 | spawn (maw) | Wraith | ≈ 8 (2 every 8 s) | rear (loop) | Launched by the Spire's maw (spec); not edge waves, but Rook still calls them |

Totals: Needler 5 · Creeper 6 · Skitter 6 · Ravager 4 · Wraith 4, plus about 8 maw-launched
Wraiths and about 4 node-spawned Skitters. Of the 7 approach waves, 5 enter from the front and
2 from the rear.

## Ground targets

| Section | Target | Effect |
|---|---|---|
| 2 (t≈40) | [Spine Turret](../../../enemies/ground/spine-turret/README.md) turret nest ×3 on the cathedral square | Credits |
| 2 (t≈56) | [Hive Node](../../../enemies/ground/hive-node/README.md) ×2 in the Place du Molard, hardened | Optional here: each spawns 2 Skitters every 4 s while alive; the budget assumes one spawn each |
| 3 (t≈64, 74, 84, 92) | Root bursts ×4, each growing a Spine Turret + a [Polyp Mortar](../../../enemies/ground/polyp-mortar/README.md) after a 1 s dust telegraph | Secondary objective: all 8 growths destroyed |
| 1 (t≈18) | CDF armoured car half under creep on the Pont du Mont-Blanc | Secret, see below |

## Hazards

- **Root bursts**: the paving cracks and dust rises for 1 s, then a root breaks out on the
  ground layer. No collision; the growths start firing 1.5 s after they appear.
- **Low light**: the canopy darkens the old town (backgrounds at the dark end of the recede
  range). Enemy accents and bullets keep their full brightness.

## Secrets and pickups

- **Armoured car** (t≈18): six hits clear the creep off the car on the bridge, and its cargo
  hatch pops open with a CDF supply crate worth **140** credits.
- **Overdrive** ×2: from the t=72 Ravager leader, and from the maw when it dies (phase 2 → 3).
- **Armour patches** ×2: a CDF supply drone crosses at t≈100, and the tear-free shockwave throws
  one up from the debris at the start of phase 3.

## Radio chatter

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "All units, Aegis Actual. Everything the Coalition has left is in the air over Geneva tonight. Make it count." |
| t=3 | Rook | "Lancer, whatever happens in there, I'm on your wing. As usual." |
| t=16 | CDF officer (Hammer Lead) | "Hammer flight on station. Call your targets, Aegis." |
| t=22 | Varga | "The canopy is one organism. The roots feed the trunk. Kill the roots and the trunk starves." |
| t=54 | Varga | "Hive nodes in the Molard. Hardened, like Lagos." |
| t=62 | Okafor | "Roots surfacing in the streets! They're growing guns." |
| t=80 | Rook | "Six o'clock, Lancer!" |
| t=106 | The Choir | "[the Choir sings]" (louder than ever, the portrait frame full of static) |
| t=107 | Varga | "It's singing to the whole city. Lancer, that's the Spire." |
| t=108 | Rook | "Here comes the big one." |
| Boss phase 2 | Varga | "The crown is opening. It's launching flyers, and they'll loop behind you." |
| Boss phase 3 | Okafor | "It's tearing loose! Stay on it, Lancer!" |
| Boss phase 3 (+5 s) | Rook | "Roots and all. Somebody call a gardener." |
| Boss destroyed | Varga | "The canopy's dying. All of it, all at once." |
| Level end | Okafor | "Every Vrell on Earth just lost its voice. They're scattering. We take the fight to them now." |
| Secondary objective met | Okafor | "Root grid in the old town destroyed. The engineers will thank you." |

## Boss / mid-boss

Act boss: [Siege Spire](../../../enemies/bosses/siege-spire/README.md). Its phases, HP and
attacks live in the spec. Arena notes for this level:

- The trunk stands in the Concord rotunda at the top of the arena. The four turret-pod roots and
  two mortar roots break out along the avenues radiating from it, so the phase-1 creep at 15 px/s
  brings them into range in pairs.
- Phase 2: the maw's Wraiths loop behind the player; Rook switches to his Trail formation and
  calls each launch (bark priority 2).
- Phase 3: the freed spire rises to `air` and drifts across the upper half. The withered stump
  and the shockwave debris stay on the ground layer as scenery, with no collision.
- The Airstrike can hit the roots and the maw (ground targets), capped at 150 per part per
  strike by the [specials](../../../player/specials/README.md) rules.
- The boss checkpoint is recorded at the boss warning (t=108) on easy and medium, per the
  [retry](../../../systems/retry/README.md) rules.
- Bounty: **720** absolute (turret pods 60 each, mortar roots 60 each, maw 120, spire 240), not
  act-scaled again. The maw's Wraiths pay their own bounty (see *Credit budget*).

## Music & ambience

The Act 2 B theme "Firestorm" (track 7 in the [track list](../../../audio/music/README.md#track-list))
for the approach. At t=108 the boss warning (track 22) replaces it and bridges into the Vrell
boss theme "The Choir Descends" (track 18). After the kill the act complete fanfare (track 24)
plays over the daylight section. Ambience: the
[megacity ambience](../../../audio/sfx/README.md#ambience-per-setting) with the sirens replaced by
a low organic hum from the canopy, which cuts out when the Spire dies.

## Credit budget

The total matches budget(14) = **2,410** from the
[economy](../../../systems/economy/README.md#per-level-budget) curve. Bounties are the spec values ×
the act factor 1.6, rounded per kill: Skitter 8, Needler 19, Creeper 35, Ravager 29, Wraith 48,
Hive Node 72, Spine Turret 19, Polyp Mortar 24. The boss bounty is absolute.

The maw's Wraiths are budgeted at the expected count: the maw lasts ≈ 33 s at the effective boss
DPS, which gives 4 launches of 2. A slower kill earns a little more.

| Source | Credits (medium) |
|---|---|
| Kills: Needler 5 × 19 + Creeper 6 × 35 + Skitter (6 + 4 node-spawned) × 8 + Ravager 4 × 29 + Wraith (4 + 8 maw) × 48 | 1,077 |
| Ground targets: Hive Node 2 × 72 + Spine Turret 7 × 19 + Polyp Mortar 4 × 24 | 373 |
| Boss: Siege Spire | 720 |
| Secret: armoured car | 140 |
| Secondary objective | 100 |
| **Total** | **2,410** |

## Difficulty notes

- **Easy**: the root-burst growths are Spine Turrets only (the secondary counts 4); the t=82
  Wraiths enter from the front.
- **Hard**: a second Spine Turret nest beside the hive nodes; root bursts grow 1.0 s after the
  telegraph instead of 1.5 s; the boss's hard-mode hooks come from its spec (3 Wraiths per
  launch, a 180° sweep). There is no boss checkpoint on hard.

## Concept art

No level-specific concept files. The look comes from the chosen Geneva scene in
[art direction](../../../art-direction/README.md):
[scene-geneva-r10-a.png](../../../art-direction/concept/scene-geneva-r10-a.png) and its
[scroll loop](../../../art-direction/concept/scene-geneva-r10-a.gif); the boss follows its chosen
[Siege Spire](../../../enemies/bosses/siege-spire/README.md) concept. The daylight outro has no
concept yet.

## Implementation

- [ ] Scroll timeline, sections, atmosphere intensity and parallax content per layer as in *Layout*.
- [ ] Root-burst set piece: 1 s telegraph, root, growths that start firing after 1.5 s.
- [ ] Wave script matches the *Waves* table.
- [ ] Boss arena per the arena notes; phase-1 creep; boss checkpoint at the warning.
- [ ] Armoured-car secret, overdrives, armour patches.
- [ ] Radio chatter cues incl. the boss phase triggers.
- [ ] Secondary objective (8 root-burst growths) tracked and rewarded.
- [ ] Daylight outro with the canopy withering, then the act complete fanfare and the act outro.
- [ ] Credit total at medium with perfect collection is 2,410 (± 5%).
- [ ] Easy/hard variations as in *Difficulty notes*.

## Decisions

- 2026-10-01: Promoted from the Act 2 roster to a draft level document.
- 2026-10-01: Open question resolved: the maw's Wraiths pay their full Wraith bounty at the Act 2 factor, as budgeted here (user decision; recorded in the Siege Spire spec).
