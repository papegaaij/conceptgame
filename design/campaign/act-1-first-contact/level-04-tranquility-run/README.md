---
title: Level 04 – Tranquility Run
design: approved
implementation: done
art: chosen
depends-on: [../../../enemies/air/brood-pod, ../../../enemies/ground/scuttler, ../../../allies, ../../../world/luna, ../../../player/specials, ../../../systems/retry]
updated: 2026-10-03
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

<!-- data: briefing -->
> **Commander Okafor:** "Lancer, brood pods came down around Tranquility Base overnight. The
> heritage site is fine. The town around it is not. Four hundred civilians are loading into five
> crawlers for the run to the mass-driver terminal, and Vrell walkers are already in the craters
> along the road. You are their escort. Crawlers are slow and they can't dodge: anything on the
> ground that shoots will shoot at them. Lose all five and this mission is over. High Command
> has released Hammer flight to us. Use it."
>
> **Dr. Varga:** "The walkers turn to face where they're going. Their claws stop your rounds,
> but their backs glow. Be patient."
<!-- /data -->

*Hangar teaser* (shop screen after L03):
<!-- data: teaser -->
> **Commander Okafor:** "Tomorrow you fly over people, Lancer. Bring something that can hit the
> ground."
<!-- /data -->

## Threat profile

<!-- data: threat-profile -->
| Field | Value |
|---|---|
| Dominant layers | `ground` (turrets, walkers, the convoy), `air` |
| Attack directions | front 95% · sides 5% (walker pincers over the crater rims) |
| Density | 2 |
| Recommended traits | `forward`, `anti-ground` |
| Hazards | none lethal; ground fire targets the convoy |
| Boss / mid-boss | none |
| Sensor-suite detail | none: Luna, `ground` + `air`, front · L1: + directions, density 2, OBJECTIVE `ESCORT 5 CRAWLERS` · L2: + Brood Pod, Scuttler, Spine Turret, Needler, Skitter portraits; Airstrike available · L3: + `anti-ground` highlighted, wave strip, 1 secret |
<!-- /data -->

## Objective

- **Primary** `escort`: bring the convoy of five crawlers to the terminal gate at the end of the
  scroll. Each crawler that arrives pays +30 (through the difficulty's credit factor, with its own
  debrief row; the grade is unchanged). **Fails** when all five crawlers are destroyed: the
  [failed primary objective](../../../systems/retry/README.md#on-a-failed-primary-objective) flow
  (mission failed with Okafor's convoy line, retry); each crawler lost before that only lowers
  the reward.
- **Secondary** *Quick hands*: kill every Brood Pod before it bursts on its own (6 pods). +50.
  An `escapes` objective on the Brood Pod: a pod's
  [self-burst](../../../enemies/air/brood-pod/README.md#behaviour) counts as an escape and fails it.

### The convoy

- Five [civilian crawlers](../../../allies/README.md#civilian-crawler) (size, HP, damage
  sources and behaviour in the allies spec) in a column along the road, **84 px apart** centre to
  centre (12 px between a crawler's tail and the next one's nose). Their centres sit at
  **y = 150, 234, 318, 402 and 486** px below the top of the play field, Crawler One leading at
  the top, so the column spans y = 114–522 (the lower three quarters of the screen, 18 px clear
  of the bottom edge). They hold these heights while the road scrolls under them.
- Here that means: only shots **aimed at a crawler** hurt it. Spine Turrets and the Scuttlers'
  acid spit use the [target-the-objective hook](../../../enemies/README.md#target-the-objective-hook)
  in mode `nearest`, so each of their aimed shots goes at the player or the nearest crawler,
  whichever is closer as it fires; a crawler-aimed shot still hurts the player, who can
  body-block it. The Scuttlers' facing fans aim at nobody and never hurt a crawler. A Scuttler's
  claws do 10 per second while it overlaps a crawler as it passes (no grip). Skitters and Needlers
  ignore the convoy.
- The [two-line objective tracker](../../../ui/hud/README.md#left-panel-mission): `CRAWLERS`
  with five pips (green; amber below 50 %; a white flash on a hit; a red flash, then dark, when
  lost), then `PODS n / 6` for *Quick hands*, `DONE` or `FAILED`.

### The road

The convoy road is level data: a curve of `[t, x]` points (`t` when that road point passes the
middle of the screen, as for placed backdrop pieces), straight between its points, drawn by the
game as a 56 px textured ribbon on the ground layer under the ground set pieces (see
[Luna](../../../world/luna/README.md#parallax-layers)); the crawlers follow it. A crawler's x is the road's x at its centre and its heading the road's
direction there, shown as the nearest of its 7 headings. So the crawlers never skid, the road
bends at most 30° from straight up the screen (at 120 px/s it moves sideways at most 69 px/s)
and its centre stays within x = 72–408 px.

## Layout

Scene: the chosen [Luna](../../../world/luna/README.md) scene
([sheet](../../../art-direction/concept/scene-luna-r06-a.png)) with its Tranquility Base and
mass-driver elements. Scroll speed 120 px/s (the crawlers' pace; calm, see
[art direction](../../../art-direction/README.md#parallax-layer-model)). Total ≈ 190 s ≈ 22,800 px.
The Stormhawk's launch is 5 s, flying in from the bottom edge. Scroll factors from the scene:
`low-air` 1.35, `high-air` 2.2. A top-down surface has no `deep` layer: Earth shows only as the
earthshine tint in the crater shadows. Motion budget: regolith plumes and the mass-driver sleds
are the only strong background movers.

<!-- data: level-sections -->
| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Tranquility Base | 0–25 | 0–3,000 | 120 | clear | `ground`: Tranquility Base: domes, the Apollo 11 heritage dome, the convoy assembling and pulling out, earthshine-tinted crater shadows. `low-air`: none. | First planetary surface; the convoy forms up; the first Brood Pod alone. |
| 2. Mare Road | 25–70 | 3,000–8,400 | 120 | light | `ground`: the convoy road across grey mare, abandoned rovers, turret growth on boulders. `low-air`: thin regolith plumes from impacts. | Protect the convoy from turrets; pods with escorts. |
| 3. Rille Crossing | 70–110 | 8,400–13,200 | 120 | medium | `far`: the floor of a sinuous rille (40% haze). `ground`: the rille rim and a road bridge across it. `low-air`: dust kicked up by the walkers. | The first walker, alone on the rille rim; then pairs. |
| 4. Landing Zone | 110–155 | 13,200–18,600 | 120 | medium, heavy peak 130–137 | `ground`: craters with split pod husks and the first teal Vrell growth. `low-air`: a regolith plume from a pod lander impact (the heavy peak). `high-air`: ejected rock streaks. | Walker pincer and pods together; the hidden cache. |
| 5. Terminal Gate | 155–190 | 18,600–22,800 | 120 | light | `ground`: the mass-driver rail with sleds launching to orbit (scenery), the terminal's vehicle hangar (380×300 px) with its lit door, landing lights on its apron, the road ending inside it. | Final pods and walker; the convoy drives into the hangar. |
<!-- /data -->

## Waves

Enemy specs: [Brood Pod](../../../enemies/air/brood-pod/README.md),
[Scuttler](../../../enemies/ground/scuttler/README.md),
[Needler](../../../enemies/air/needler/README.md),
[Skitter](../../../enemies/air/skitter/README.md). Scuttlers walk authored ground paths that
cross the convoy's road (in screen coordinates, scrolling with the ground); they turn toward
the column and claw a crawler they walk across.

The **pacing filler** waves are small Skitter streams (3–4 units, 2–2.75 s apart, the same count
on every difficulty) that keep the screen from emptying between the set waves, per the
campaign's [pacing rule](../../README.md#pacing-rules). Skitters only ram the ship: they do not
hook the convoy, so its threat is unchanged. The only long pause left is the convoy's run into
the terminal (from about 169 s).

<!-- data: waves -->
| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 7 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (left) | Pacing filler |
| 13.5 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | Pacing filler |
| 22 | 1 | single | [Brood Pod](../../../enemies/air/brood-pod/README.md) | 1 | front | **Introduction**: alone, above the base; bursts into 6 Skitters |
| 27.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | Pacing filler |
| 32 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | |
| 35.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | Pacing filler |
| 45 | 2 | carrier + escorts | [Brood Pod](../../../enemies/air/brood-pod/README.md) + [Needler](../../../enemies/air/needler/README.md) | 1 + 3 | front | Needlers `orbit` the pod |
| 51.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | Pacing filler |
| 60 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 3 | front | Over a turret nest |
| 66.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (right) | Pacing filler |
| 75 | 3 | single | [Scuttler](../../../enemies/ground/scuttler/README.md) | 1 | front | **Introduction**: walks along the rille rim on the ground layer beside the road, then turns toward the column, showing its back as it turns |
| 80 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | Pacing filler |
| 88 | 3 | pincer | [Scuttler](../../../enemies/ground/scuttler/README.md) | 2 | sides | Walk in over both crater rims toward the convoy; edge warnings |
| 95 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 9 | front (alternating edges) | While the pincer walks in |
| 100 | 3 | single | [Brood Pod](../../../enemies/air/brood-pod/README.md) | 1 | front | |
| 107.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | Pacing filler |
| 115 | 4 | carrier + escorts | [Brood Pod](../../../enemies/air/brood-pod/README.md) + [Needler](../../../enemies/air/needler/README.md) | 1 + 3 | front | |
| 120 | 4 | convoy | [Scuttler](../../../enemies/ground/scuttler/README.md) | 2 | front | Walk down the road toward the column, claws first |
| 127.5 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (left) | Pacing filler |
| 133.5 | 4 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (right) | Pacing filler |
| 140 | 4 | pincer | [Scuttler](../../../enemies/ground/scuttler/README.md) | 2 | sides | Out of the heavy dust; edge warnings; two per side on hard |
| 150 | 4 | line abreast | [Brood Pod](../../../enemies/air/brood-pod/README.md) | 2 | front | |
| 160 | 5 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | |
| 163 | 5 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 3 | front (alternating edges) | Pacing filler |
| 165 | 5 | single | [Scuttler](../../../enemies/ground/scuttler/README.md) | 1 | front | Comes out of the gate yard toward the convoy |

Totals: Skitter 50 · Brood Pod 6 · Needler 12 · Scuttler 8.
<!-- /data -->

## Ground targets

<!-- data: ground-targets -->
| Section | Target | Effect |
|---|---|---|
| 2 | Turret nest: Spine Turret ×3 by the road (t≈40) | Fires at the player and the convoy |
| 2 | CDF supply drop (ground layer, 3 HP; t≈55) | Medium salvage (50); also a special charge if a special is fitted (on top of the free first Airstrike charge) |
| 2 | Turret nest: Spine Turret ×4 on boulders (t≈62) | As above |
| 3 | Spine Turret ×2 on the bridge arches over the road (t≈105) | As above |
| 4 | Collapsed prospector's dugout in a crater rim (**hardened**, 20 HP; t≈125) | Only `anti-ground` weapons or the Airstrike open it; releases the hidden crate |
| 4 | Turret nest: Spine Turret ×4 among the pod husks (t≈145) | As above |
<!-- /data -->

Spine Turrets: 13 in total. Scuttlers are ground enemies scripted in *Waves*.

## Hazards

None lethal. The danger is to the convoy: the turrets' shots and the Scuttlers' acid spit go at
whichever is closer, the player or the nearest crawler (hook mode `nearest`, see *The convoy*). The heavy dust peak in section 4 never hides bullets, but it
hides the walkers' bodies (their lime backs glow through it).

## Secrets and pickups

Pickup types are defined in [player](../../../player/README.md#in-level-pickups).

- **Prospector's cache** (hidden crate, 100): the hardened dugout at t≈125; Okafor: "A
  prospector's cache. Whoever dug in there would want it used, Lancer." This is the first
  hardened target, placed where an `anti-ground` weapon (Bomb Rack, from L03) or a Hammer flight
  run can reach it.
- **Supply drop** (t≈55): medium salvage 50; plus one special charge if a special is fitted
  (otherwise nothing extra).
- **Armour patch** ×1: dropped by the last Needler of the t=115 group.
- **Overdrive**: dropped by the Brood Pod of the t=45 group (if it is killed before it bursts).
- Shield cells at the normal rate.

### Contextual prompt

- t=75 (first Scuttler), only with the Airstrike fitted: `SPECIAL` · `CALL HAMMER` (the
  action/keys format of the prompt well). This is the special prompt announced in L01 (prompts
  otherwise end after L03).

## Radio chatter

Text and radio blips only. Rook flies Aegis Two over Shackleton (radio only).

<!-- data: radio -->
| Trigger | Speaker | Line |
|---|---|---|
| t=0 | Okafor | "Five crawlers, four hundred civilians. Get them to the terminal, Lancer." |
| t=11 | Generic civilian (Crawler One) | "Crawler One rolling. We're slow and we're loud, Aegis. Please don't leave us." |
| t=22 (first pod) | Varga | "That's an egg sac. It hatches on its own in eight seconds. Kill it before then, and be ready for what comes out." |
| t=34.5 | Rook | "Aegis Two over Shackleton. My family's down there somewhere, Lancer. Bring those crawlers home." |
| t=60 (Airstrike fitted) | Okafor | "Hammer flight is on station. Your call, Lancer." |
| First crawler hit | Generic civilian (Convoy) | "We're taking fire! Crawler {ally} is hit!" |
| Crawler lost (first) | Generic civilian (Crawler One) | "We lost {ally}. Oh God, we lost {ally}." |
| Crawler lost (first), after the convoy's line | Okafor | "Keep the rest moving, Lancer." |
| t=74 (first Scuttler) | Varga | "Walker on the rille rim. The claws are armour. Wait for it to turn, then hit the glowing back." |
| t=86 | Generic CDF (Tranquility Control) | "Walkers coming over the crater rims, both sides of the road!" |
| t=128 | Generic CDF (Tranquility Control) | "Lander impact, grid four. Dust everywhere. Stay on the convoy." |
| t=176 | Generic civilian (Crawler One) | "Terminal in sight! Open those doors!" |
| Level end, 5 crawlers | Okafor | "All five. Four hundred people. That's what we're for, Aegis." |
| Level end, 1–4 crawlers | Okafor | "We got most of them home. Most." |
| Mission failed (the failed screen's line) | Okafor | "The convoy is gone, Lancer. Pull back." |
| Secondary met | Varga | "Not one sac hatched on its own. You're learning their rhythm." |
<!-- /data -->

## Boss / mid-boss

None.

## Music & ambience

Track 4 *Act 1 A: Earth orbit & Luna* ("Afterburner", see the
[track list](../../../audio/music/README.md#track-list)), base stem only through sections 1–2
(Luna starts tense and sparse), the full mix from section 3 (`full_section: 3`, the Rille
Crossing, where the first walker comes). Ambience: Luna
([sfx](../../../audio/sfx/README.md#ambience-per-setting)) plus crawler engine rumble while the
convoy is on screen. Mission failed sting on convoy loss.

## Credit budget

Budget(4) = 1,000 × 1.07³ ≈ **1,225** ([economy](../../../systems/economy/README.md#per-level-budget)).
The pacing filler's 35 Skitters (175 credits) lift a perfect run to 1,400, 14% over the curve.
Bounties from the stat blocks: Brood Pod 20 (8 if it bursts on its own), Skitter 5, Scuttler 25,
Needler 12, Spine Turret 12.

<!-- data: credit-budget -->
| Source | Credits (medium) |
|---|---|
| Kills: Skitter 50 × 5 + Brood Pod 6 × 20 + Needler 12 × 12 + Scuttler 8 × 25 + released Skitter 36 × 5 | 894 |
| Ground targets: Spine Turret 13 × 12 + supply drop medium salvage 50 | 206 |
| Primary objective: 5 crawlers home × 30 | 150 |
| Secret: prospector's cache (hidden crate, 8% of budget) | 100 |
| Secondary: every Brood Pod killed before it bursts | 50 |
| **Total** | **1,400** |
<!-- /data -->

## Difficulty notes

- **Easy**: crawlers have 90 HP; Scuttlers walk 20% slower; pods burst after 10 s (stat-block
  hook).
- **Hard**: crawlers have 45 HP; Scuttler fans are 7-way and pods release 8 Skitters
  (stat-block hooks); the t=140 pincer has two Scuttlers per side.

## Concept art

Concept round 17 (M4 part D): the backdrop's production art proposal (its review sheet built from the final files in `assets/` by `tools/art/backdrop_l04.py`, `--review` rebuilds only it), two concept variants each for the prospector's dugout and the CDF supply drop (`tools/concept/ground_targets_r17.py`; variant a chosen for both and made into the game's sprites by `tools/art/l04_targets.py`), and captures of the level as built; prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/backdrop-final-r17-a.png](concept/backdrop-final-r17-a.png) | Final backdrop pieces: every tile set and set piece of the `backdrop` data (proposed in a separate file, merged into [data.yaml](data.yaml) in step 3): five ground tile sets on one terrain (grey mare ×2, the rille rims over the far rille floor, the crater field with Vrell roots, the mass-driver field), regolith plumes, ejected-rock streaks, Tranquility Base (apron, domes, the Apollo 11 heritage dome, pads), rovers, boulders and turret sockets, the rille's ends and road bridge (the deck under the convoy, its two arches as an overhead piece over it), pod-lander husks, the rail head, the sled overlay and the terminal's vehicle hangar (the apron under the convoy, the 380×300 hangar as an overhead piece), the road texture; with composites of the level at twelve times, the overhead pieces drawn over the convoy | chosen |
| [concept/dugout-r17-a.png](concept/dugout-r17-a.png) | Prospector's dugout (48×32, hardened), variant A: a corrugated hut half-buried in a regolith berm with sandbag rows, an armoured hatch plate in amber/black hazard stripes and a hazard band round the roof, a regolith slide over the collapsed far end, a mast with a red lamp; intact, damaged (dented roof, the mast bent, scorch), wrecked (roof blown open onto the dark interior, ribs curled, the hatch plate thrown aside, the amber burnt dull) on Luna regolith at 1× and 3× (`tools/concept/ground_targets_r17.py`) | chosen |
| [concept/rejected/dugout-r17-b.png](concept/rejected/dugout-r17-b.png) | Prospector's dugout, variant B: a low octagonal bunker of sintered regolith with an armoured roof hatch in an amber/black hazard ring, a vent stack, a small solar panel and rubble over one corner; damaged (cracks, a dented hatch, the panel knocked askew), wrecked (blown open at the hatch into a pit, slab pieces tilted, the panel gone) | rejected — a chosen |
| [concept/supply-drop-r17-a.png](concept/supply-drop-r17-a.png) | CDF supply drop (32×24, 3 HP), variant A: a steel drop crate with amber/black hazard end bands, lid ribs, a red lamp and four corner retro nozzles; damaged (dented lid, a gouge, one nozzle torn off), wrecked (split open, the side walls splayed, an empty dark hold) | chosen |
| [concept/level-04-capture-final-r17-a.png](concept/level-04-capture-final-r17-a.png) | Game capture of the level as built (M4 part D step 3), whole window, 2 × 4: the launch with the convoy rolling in at Tranquility Base, the first Brood Pod over the road and its burst into six Skitters, the t=40 turret nest on its boulders, the intro Scuttler on the rille's west rim fanning down at the column, the convoy on the road bridge, the heavy lander plume in the Landing Zone, the column at the terminal gate | chosen |
| [concept/l04-targets-final-r17-b.png](concept/l04-targets-final-r17-b.png) | Final sprites of the chosen variants a (`tools/art/l04_targets.py`): the dugout (48×32) and the supply drop (32×24) intact, damaged and wrecked, and each one's eight-frame break-apart (72×72 and 48×48: six pieces fly apart, tumble, char and crumble) | chosen |
| [concept/l04-targets-final-r17-b.gif](concept/l04-targets-final-r17-b.gif) | Both targets animated: intact, two hits, the break-apart over the wreck left behind | chosen |
| [concept/level-04-capture-final-r17-b.png](concept/level-04-capture-final-r17-b.png) | Game capture after round 17's feedback, whole window, 2 × 3: the supply drop beside the road (t≈53), the convoy on the road bridge passing under its first arch (t≈103) and the column on the deck with a Spine Turret standing on each arch (t≈104), the dugout in its crater field (t≈122), the column at the terminal gate (t≈185) and the crawlers driving in under the blockhouse, the road ending there (t≈187) | chosen |
| [concept/level-04-capture-final-r17-c.png](concept/level-04-capture-final-r17-c.png) | Game capture after the hangar redo (round 17 item 9), whole window, 2 × 3: the terminal's vehicle hangar (380×300) entering the screen with the column on its apron (t≈185.7), Crawler One at the lit door (t≈186.7), the column driving in under the roof (t≈187.7 and t≈188.7), the last crawler in the door bay (t≈189.2), all crawlers inside (t≈189.7) | chosen |
| [concept/rejected/supply-drop-r17-b.png](concept/rejected/supply-drop-r17-b.png) | CDF supply drop, variant B: a squat cylindrical drop pod on four splayed legs with two amber/black hazard rings, a nose cone, a hatch strip and a red lamp; damaged (a dent, a leg snapped), wrecked (split along the top, the nose cone knocked off and charred) | rejected — a chosen |

## Implementation

- [x] Scroll timeline, sections, atmosphere intensity and parallax content as in *Layout*.
- [x] Road: the `[t, x]` curve in the level data, drawn as a 56 px textured ribbon on the ground
      layer; bends within ±30°, centre within x = 72–408.
- [x] Convoy: five ground-layer crawlers at y = 150, 234, 318, 402, 486 following the road with 7
      headings, 60 HP, damaged only by crawler-aimed shots (hook `nearest`) and pass-through
      Scuttler claws (10/s); the two-line HUD tracker (`CRAWLERS` pips, `PODS n / 6`).
- [x] Escort objective: +30 per arriving crawler through the credit factor, with a debrief row;
      the failed-primary-objective flow when all five are lost, with Okafor's convoy line.
- [x] *Quick hands* as an `escapes` objective: a pod's self-burst counts as an escape.
- [x] Scuttler walk paths cross the road and turn toward the convoy.
- [x] Wave script matches the *Waves* table.
- [x] Hardened dugout: only `anti-ground` and the Airstrike damage it.
- [x] Supply drop pays a special charge only if a special is fitted.
- [x] Special prompt `SPECIAL` · `CALL HAMMER` at t=75 (once, only with the Airstrike fitted).
- [x] Radio cues fire at their triggers, including the per-outcome end lines; Okafor's Hammer flight line only with the Airstrike fitted.
- [x] Credit total at medium with perfect collection is 1,400 (the curve's 1,225 + 14% for the pacing filler).
- [x] Pacing rule: no empty screen over 3 s after the launch except the run into the terminal, on every difficulty (`PacingTest`).
- [x] Easy/hard variations as in *Difficulty notes*.

## Open questions

- None open.

## Decisions

- 2026-10-01: Promoted from the act roster to a draft level document. Escort rules: only
  bullets from ground-layer enemies and Scuttler claws hurt crawlers, so air enemies threaten the
  player and ground enemies threaten the convoy. The mass driver is scenery here and becomes a
  hazard in L05. First hardened target placed as the secret, so `anti-ground` pays off without
  being mandatory.
- 2026-10-01: The crawler spec moved to [allies](../../../allies/README.md#civilian-crawler); the convoy section keeps only the level setup. Turret and Scuttler targeting expressed with the target-the-objective hook (mode `nearest`).
- 2026-10-01: Supply-drop text aligned with the free first Airstrike charge: the drop's charge comes on top of it.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-03: M4 part D doc gaps (user decisions): crawlers are hit only by enemy shots aimed at a
  crawler (the L13 relay rule); Scuttler claws pass through (10/s while overlapping, no grip); 1
  free Airstrike charge at the hangar visit before this level, fitted automatically into an empty
  special slot with a hangar notice (recorded in [specials](../../../player/specials/README.md));
  a two-line HUD tracker for the primary and the secondary; no `deep` layer on Luna, Earth only as
  an earthshine tint; the mass-driver rail on the ground layer, as in the chosen scene; the road
  is level data, a `[t, x]` curve drawn as a textured ribbon that the crawlers follow; the
  crawler is concept variant c (40 wide × 72 long), the column 84 px apart in the lower ~70 % of
  the screen and the road widened; the intro Scuttler walks the rille rim on the ground layer
  beside the road, and Varga's line says "rim"; Okafor's Hammer flight line moves to t=60 and
  plays only with the Airstrike fitted, the first Brood Pod wave (and Varga's line on it) moves
  from t=18 to t=22, Okafor's opening line from t=1 to t=0, the texts unchanged; bombs, shells
  and the Airstrike ignore the Scuttler's frontal armour.
- 2026-10-03: M4 part D doc gaps (main-agent choices following earlier decisions): the column's
  centres y = 150, 234, 318, 402, 486 (tail 18 px above the bottom edge; 408 px of column cannot
  fit inside 70 % = 378 px); the road 56 px wide, bending at most 30° and staying within
  x = 72–408; the escort pays through the credit factor with a debrief row, grade unchanged;
  *Quick hands* is an `escapes` objective; a 5 s launch flying in from the bottom; the full mix
  from section 3 (`full_section: 3`; stems switch per section, not at the first walker); the
  scene's scroll factors (low-air 1.35, high-air 2.2); the threat profile's directions as derived
  (90/10 instead of 86/14); an OBJECTIVE field at sensor L1; the prompt in the action/keys
  format; a failed convoy uses the generic failed-primary-objective flow of
  [retry](../../../systems/retry/README.md#on-a-failed-primary-objective). `art` stays `chosen`:
  every part with a look has a chosen concept or final art now that the crawler's concept was
  chosen in round 16 (the dugout and the supply drop are small props without a concept yet);
  `implementation: in-progress` as part D starts.
- 2026-10-03: M4 part D step 2 built the generic parts this level uses, tested on a test level (Level
  01's script with a convoy) until this level's data exists: the road and its ribbon, the convoy
  (rolling in from the bottom edge one crawler per second from t=4 at 84 px/s up the screen, so the
  column arrives together), the `nearest` hook, the claws, the escort's pay and debrief row, the
  failed-primary-objective flow with the level's line, the `first-ally-hit` / `first-ally-lost`
  cues naming the crawler (`{ally}`), the per-outcome level-end lines, cues and prompts that require
  a special, the two-line tracker and the OBJECTIVE intel field. The *Implementation* items stay
  open until the level's data uses them.
- 2026-10-03: M4 part D step 3 built the level: its [data.yaml](data.yaml) (the backdrop proposal's
  `backdrop`, `road` and section tiles merged in, the proposal file deleted), the Brood Pod and the
  Scuttler in the simulation, and the tables above rendered from the data. Numbers chosen here: the
  Scuttlers' walk paths (in the data, per wave), the turret nests on the backdrop's sockets
  (entering 2.25 s before their base piece passes the middle; the husk nest on the two husks' rims at
  x 92/150 and 344/400), the dugout (48×32, 20 HP, hardened) at x 110 passing the middle at ≈125.2 s,
  the supply drop (32×24, 3 HP, medium salvage plus a special charge as a `bonus_drop`) at x 330 at
  ≈54.9 s, the hard pincer at t=140 as a wave change to 4 (the second pair 1.5 s later), easy's
  slower Scuttlers as a level `speed_factor: 0.8`, Okafor's cache line (new), and two radio lines
  retimed so none starts more than a second late: Crawler One from t=4 to t=11 (Okafor's opening
  line runs to 10.9 s) and Rook from t=30 to t=34.5 (Varga's pod line runs to 34.2 s). The Hammer
  Lead line comes from the specials data with the call, not from this table. The dugout and the
  supply drop use the cargo container's frames (the dugout at 1.5×) until round 17's choice. The
  autopilot (starter loadout, Pulse Cannon only) brings 5 crawlers home on easy and medium and 2 on
  hard (Level04Test). The crawlers are drawn over the terminal gate (the gate is a ground set piece
  below the convoy), so the column does not vanish into it.
- 2026-10-03: Round 17 feedback. The user chose the dugout's and the supply drop's variant **a**
  (user decision); `tools/art/l04_targets.py` renders them as production sprites (intact, damaged,
  wrecked and a break-apart each; the wreck stays on the ground where a target was destroyed), and
  the level's `ground_targets` name them by `sprite`. The user found the crawlers driving over the
  arches that carry the bridge's turrets strange (drop the arches, or let the crawlers pass under
  them); main-agent choice following that feedback: the crawlers pass **under** the arches. The
  road bridge and the terminal gate are split into a base (deck, apron) under the road and the
  convoy and an `overhead` part (the two arches with the turret sockets, the gate's blockhouse)
  drawn over the convoy and under the ground units, so the Spine Turrets stand on the arches; the
  arches cast a short shadow onto the road and the crawlers. The road now ends under the
  blockhouse (its last point at 187.6 s, the blockhouse's centre) and is not drawn past its end, and
  a crawler past the end has driven into the terminal and is no longer drawn: the column vanishes
  into the gate instead of reappearing above it.
- 2026-10-03: Round 17 item 9 feedback (user): the bridge is right now, but the hangar was far too
  small for the crawlers to fit in; it must be at least five times as large. The gate's 282×80 px
  blockhouse is replaced by a vehicle hangar 380×300 px (5.05× the footprint), rendered at that
  size by `tools/art/backdrop_l04.py`: a ribbed, vaulted vehicle hall between two flat-roofed wings
  (radiators, a dish, a solar array, the control tower), an 80 px wide door bay in its south face
  with an amber-lit frame and hazard-striped jambs, and a larger apron with two rows of landing
  lights, a threshold bar and four floodlight masts. Both pieces are now 432×480 at t=187.65, x=216:
  the hangar spans x 26–406 so it stays clear of the mass-driver rail at x 432. The road's last
  stretch moves to x 216 (`[178, 236]`, `[184, 216]`, `[187.6, 216]`; the end time is unchanged)
  so it meets the door square; its last point now lies 40 px inside the door, so a crawler is fully
  under the roof when it stops being drawn. The column enters from 186.6 s (Crawler One) to 189.4 s
  (the last), before the level's end at 190 s; the level's length did not change.
- 2026-10-03: Round 17 verdict on items 1–8 (user decision): the Luna backdrop approved as final
  except the bridge (redone as round 17 item 9, the crawlers passing under the arches and into the
  gate; `backdrop-final-r17-a.png` has since been regenerated in place with the split bridge and
  gate); dugout a and supply drop a chosen, the b variants moved to `concept/rejected/`, their
  production art (`l04-targets-final-r17-b`) is item 10; the captures accepted; and the user
  accepted the part D numbers as listed in [round 17](../../../concept-rounds/round-17/README.md#part-d-numbers).
- 2026-10-03: Concept round 17 closed (user decision): the bridge and the enlarged terminal hangar
  (item 9) approved as final, so the backdrop is **final**; the production art of dugout a and
  supply drop a (item 10, `l04-targets-final-r17-b`) approved as final; the captures accepted.
  `art` stays `chosen`, since the level has no briefing images yet.
- 2026-10-03: Pacing filler (user decision, campaign pacing rule): eleven small Skitter streams
  (t = 7, 13.5, 27.5, 35.5, 51.5, 66.5, 80, 107.5, 127.5, 133.5, 163; 35 Skitters, fixed counts on
  every difficulty) fill the empty stretches of 4–17 s between the waves; no turrets, Scuttlers or
  convoy hooks are added. A perfect run now earns 1,400 instead of 1,225 (+14%).
- 2026-10-03 (user decision): the filler Skitters' extra credits are accepted: a perfect run
  earns 1,400 (14 % above the economy curve), so the economy runs slightly ahead from Level 04 on;
  the convoy's ~21 s run into the hangar without enemies stays as one of the two allowed pauses.
