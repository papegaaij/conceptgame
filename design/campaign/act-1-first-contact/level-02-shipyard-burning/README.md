---
title: Level 02 – Shipyard Burning
design: draft
implementation: not-started
art: chosen
depends-on: [../../../enemies/ground/spine-turret, ../../../enemies/air/stinger, ../../../world/earth-orbit]
updated: 2026-10-01
---

# Level 02 – Shipyard Burning

## Summary

The morning after the scout raid, the main Vrell force sets the Gagarin shipyards on fire and
grows turrets on the hulls of four crewed drydocks. Lancer flies the burning south arm, burning
the growths off each dock before it passes. The level introduces the first `ground` targets
(the Spine Turret), the diving Stinger and the shipyard crane, the setting's signature set
piece. About 3 minutes, no boss.

## Briefing

> **Commander Okafor:** "Lancer, the scouts were the knock on the door. The main Vrell force hit
> the Gagarin yards overnight. Half the outer ring is burning, and four drydocks still have work
> crews inside. The Vrell are growing something on the hulls. Burn it off before each dock
> leaves your sector: every dock you clear is a crew that walks home. Aegis Two has the north
> arm. You have the south. Aegis Actual out."
>
> **Dr. Varga:** "About yesterday's transmission. I ran it through every language model we have.
> One pattern repeats, and the best fit is a single word: *yield*. And Lancer, the growths are
> turrets. Once you're behind them, they can't track you."

*Hangar teaser* (shop screen after L01):
> **Dr. Varga:** "The yards are crawling with growths that shoot back. A wider gun would help."

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `air`, `ground` (turrets rooted on station hulls) |
| Attack directions | front 71% · sides 29% (10 of 14 waves from the top edge) |
| Density | 2 |
| Recommended traits | `forward`, `spread` |
| Hazards | Crane Four's sweeping arm (section 3); a coolant cloud in section 4 (visual only) |
| Boss / mid-boss | none |
| Sensor-suite detail | none: setting, `air` + `ground`, front · L1: + directions, density 2, hazard "crane" · L2: + Skitter, Needler, Stinger, Spine Turret portraits; no boss · L3: + `spread` highlighted, wave strip, 1 secret |

## Objective

- **Primary** `reach-end`: survive to the end of the scroll.
- **Secondary** *Save the drydocks*: a dock is saved when all Spine Turrets rooted on it are
  destroyed before the dock passes the bottom edge of the screen. Each saved dock pays +25
  (4 docks, +100). A lost dock only costs its bonus.

## Layout

Scene: the chosen [Earth orbit](../../../world/earth-orbit/README.md) scene
([sheet](../../../art-direction/concept/parallax-r03-a.png)), now burning. Scroll speed
140 px/s (calm, see [art direction](../../../art-direction/README.md#parallax-layer-model)).
Total ≈ 185 s ≈ 25,900 px. Motion budget: venting fires on the ground layer and the drifting
cloud decks are the two strong background movers; the crane arm counts as gameplay, so the
cloud decks thin out while it sweeps.

| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Parallax content per layer | Purpose |
|---|---|---|---|---|---|---|
| 1. Burning Ring | 0–25 | 0–3,500 | 140 | light | `deep`: Earth in morning light, smoke-grey plumes from the outer ring. `far`: the north arm with Aegis Two's tracer fire. `ground`: defence ring platforms, two burning. `low-air`: thin cloud decks. | Re-entry into the yard; one quiet single turret teaches ground targets. |
| 2. Outer Docks | 25–70 | 3,500–9,800 | 140 | light | `ground`: Docks One and Two, open lattice frames with half-built frigates; teal Vrell growth on the hulls. `low-air`: crane jibs and lattice beams. `high-air`: spark streaks. | First dock saves; the Stinger dive is learned. |
| 3. Crane Row | 70–115 | 9,800–16,100 | 140 | light → clear | `ground`: crane rails, Dock Three, cargo pods. `air`: Crane Four's arm (hazard). `low-air`: decks pulled aside for the sweep. | Signature set piece; side entries. |
| 4. Main Drydock | 115–160 | 16,100–22,400 | 140 | medium, heavy peak 140–148 | `ground`: Dock Four, the cruiser *Resolute* in frame, its coolant lines ruptured. `low-air`: white coolant cloud banks (the heavy peak). `high-air`: frost streaks. | The densest fighting; the last dock. |
| 5. Breakout | 160–185 | 22,400–25,900 | 140 | light | `ground`: the yard perimeter falling away. `deep`: open space, the Moon. | Final mixed waves, then the end. |

## Waves

Enemy specs: [Skitter](../../../enemies/air/skitter/README.md),
[Needler](../../../enemies/air/needler/README.md),
[Stinger](../../../enemies/air/stinger/README.md); formations from the
[vocabulary](../../../enemies/README.md#formation-vocabulary).

| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 28 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | Over Dock One, while its turrets fire |
| 40 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 50 | 2 | single | [Stinger](../../../enemies/air/stinger/README.md) | 1 | front | **Introduction**: alone, no other air enemies; radio tip |
| 56 | 2 | column | [Stinger](../../../enemies/air/stinger/README.md) | 2 | front | Dives 0.4 s apart |
| 60 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 8 | sides (both) | Over Dock Two |
| 78 | 3 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | Before the crane sweep |
| 88 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | After the second sweep |
| 100 | 3 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | Over Dock Three |
| 108 | 3 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | sides (right) | Edge warning; radio |
| 118 | 4 | pincer | [Needler](../../../enemies/air/needler/README.md) | 4 | sides | Hold 4 s at the edges over Dock Four |
| 130 | 4 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front | |
| 145 | 4 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 5 | front | Inside the coolant peak; the crimson dive glow reads through the cloud |
| 165 | 5 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 4 | front | |
| 170 | 5 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 8 | sides (both) | Finale |

Totals: Skitter 34 · Needler 16 · Stinger 14.

## Ground targets

All hostile ground targets are [Spine Turrets](../../../enemies/ground/spine-turret/README.md)
(24 in total).

| Section | t (s) | Target | Effect |
|---|---|---|---|
| 1 | 18 | Spine Turret ×1 on an open platform | **Introduction**: no air enemies on screen; HUD prompt "Ground targets: you fly over them. Shoot them." |
| 2 | 30–45 | Dock One: turret nest ×3 | Secondary: Dock One saved |
| 2 | 45 | Spine Turret ×2 on a hull | Bounty only |
| 2 | 55–68 | Dock Two: turret nest ×3 | Secondary: Dock Two saved |
| 3 | 95–110 | Dock Three: turret nest ×3 | Secondary: Dock Three saved |
| 3 | 104 | Spine Turret ×3 on the crane rail | Bounty only |
| 4 | 125–145 | Dock Four: turret nest ×3 | Secondary: Dock Four saved |
| 4 | 150 | Turret nest ×4 on the *Resolute*'s hull | Bounty only |
| 5 | 165 | Spine Turret ×2 on the perimeter | Bounty only |
| 3 | 84 | Cargo pods ×3 (ground layer, 3 HP) | Each drops a small salvage (10) |

## Hazards

- **Crane Four** (section 3, t≈80–95): a gantry crane arm about 260 px long on the `air` layer
  sweeps 90° across the upper half twice (t≈82 and t≈91, each sweep 2.5 s). Warning lights along
  the jib blink for 1.5 s before each sweep. Contact damage 15 (shield first, then armour); the
  arm is indestructible and blocks shots from both sides.
- **Coolant cloud** (section 4, t≈140–148): a heavy `low-air` bank. It never hides bullets
  (readability rules); it does hide ground targets under it, so Dock Four's last turret must be
  hit before the peak or through it.

## Secrets and pickups

Pickup types are defined in [player](../../../player/README.md#in-level-pickups).

- **Crane cache** (hidden crate, 80): a CDF supply canister hangs from Crane Four's hook. Its
  release clamp lights amber only while the arm sweeps; 3 hits on the clamp during a sweep drop
  the canister.
- **Cargo pods** ×3 (t≈84): small salvage, 10 each.
- **Armour patch**: dropped by the last Stinger of the t=145 V-wing.
- **Overdrive**: dropped by the last Needler of the t=118 pincer.
- Shield cells at the normal drop rate.

## Radio chatter

Text and radio blips only. Rook leads Aegis Two on the north arm (radio only).

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "Aegis, the yards are burning. Four docks still have crews aboard. Clear those hulls." |
| t=12 | Rook | "Aegis Two on the north arm. It's a mess over here too, Lancer. Don't wait for me." |
| t=18 (first turret) | Varga | "That growth on the platform is alive. It's a turret. It *grew* a turret, Commander." |
| t=30 (Dock One) | Generic CDF (Dock One) | "Dock One to anyone! They're growing on the hull — burn it off us!" |
| Dock saved (each) | Generic CDF (that dock) | "We're clear! Crew's moving to the shuttles. Thank you, Aegis!" |
| Dock lost (each) | Generic CDF (Yard Control) | "Dock … is gone. We lost contact with the crew." |
| First dock lost | Okafor | "Keep moving, Lancer. The others still need you." |
| t=49 (Stinger) | Varga | "Fast one inbound. It stops before it dives. When it glows red, move sideways." |
| t=78 | Generic CDF (Yard Control) | "Crane Four is still on automatic. Watch the arm, Aegis!" |
| t=106 (side wave) | Rook | "Bandits coming in on your right flank, Lancer!" |
| t=116 (pincer) | Rook | "Both edges! Pick a side and make it count." |
| t=138 | Generic CDF (Dock Four) | "Coolant breach on the *Resolute*! You'll be flying through it!" |
| t=160 | Rook | "You know, they told me you were the quiet type. Good. More airtime for me." |
| Level end | Okafor | "That's the yards. Not all of them. Enough. Come home, Aegis." |
| Secondary: all four docks saved | Okafor | "Four docks, four crews. Well flown, Lancer." |

## Boss / mid-boss

None. Dock Four under the coolant cloud is the level's peak.

## Music & ambience

Track 4 *Act 1 A: Earth orbit & Luna* ("Afterburner", see the
[track list](../../../audio/music/README.md#track-list)); the intensity stem is scripted on for
section 4. Ambience: Earth orbit ([sfx](../../../audio/sfx/README.md#ambience-per-setting)) with
the warning klaxon looping softly while Crane Four's lights blink.

## Credit budget

Budget(2) = 1,000 × 1.07 = **1,070** ([economy](../../../systems/economy/README.md#per-level-budget)).
Bounties from the stat blocks: Skitter 5, Needler 12, Stinger 15, Spine Turret 12.

| Source | Credits (medium) |
|---|---|
| Kills: Skitter 34 × 5 + Needler 16 × 12 + Stinger 14 × 15 | 572 |
| Ground targets: Spine Turret 24 × 12 | 288 |
| Pickups: crane cache 80 (7.5%) + cargo pods 3 × 10 | 110 |
| Secondary: 4 docks × 25 | 100 |
| **Total** | **1,070** |

## Difficulty notes

- **Easy**: docks carry 2 turrets each (a dock is saved when both are gone); the crane sweeps
  once; side waves lose 2 units each.
- **Hard**: docks carry 4 turrets each; the crane sweeps three times; the Stingers' fan is
  5-way (stat-block hook); Spine Turrets fire 2-shot bursts (stat-block hook).

## Implementation

- [ ] Scroll timeline, sections, atmosphere intensity and parallax content as in *Layout*.
- [ ] Wave script matches the *Waves* table.
- [ ] Spine Turrets placed per *Ground targets*; the t=18 turret appears with no air enemies.
- [ ] Dock objective: per-dock turret groups, saved/lost when the dock crosses the bottom edge,
      +25 per saved dock, shown in the HUD objective tracker.
- [ ] Crane Four: 1.5 s light telegraph, two sweeps, contact damage 15, blocks shots.
- [ ] Crane cache clamp is hittable only during a sweep.
- [ ] Coolant peak on `low-air` never hides bullets.
- [ ] HUD prompt for ground targets at t=18 (once, skippable).
- [ ] Radio cues fire at their triggers; dock lines fire per dock.
- [ ] Credit total at medium with perfect collection is 1,070 (± 5%).
- [ ] Easy/hard variations as in *Difficulty notes*.

## Open questions

- None open.

## Decisions

- 2026-10-01: Promoted from the act roster to a draft level document. The drydock secondary is
  tied to the new ground layer: a dock is saved by killing the turrets rooted on it. Crane Four
  (the Earth-orbit signature set piece) appears here. Varga's reading of the L01 transmission
  ("yield") is given in this briefing, keeping the Choir itself unintelligible in Act 1.
