---
title: Level 07 – Brood Carrier
design: approved
implementation: done
art: final
depends-on: [../../../enemies/bosses/brood-carrier, ../../../world/earth-orbit]
updated: 2026-10-05
---

# Level 07 – Brood Carrier

## Summary

The act finale. Varga has traced every pod back to one ship holding at the Earth–Moon L1 point,
where the Vrell first arrived. Lancer flies through the wreck of the overrun L1 picket and the
carrier's escort screen, then fights the Brood Carrier: first as its hull comes over on
`high-air` and stops above the ship, then broadside, then at its core. A short approach (about
100 s) plus the boss (90–150 s) and a 35 s aftermath. The Choir answers its death, and a second,
larger fleet is detected heading for Earth.

## Briefing

<!-- data: briefing -->
> **Commander Okafor:** "Aegis, Varga has traced every pod that hit Luna back to one ship: a
> brood carrier holding at L1, where this started. It's the size of a station, it launches
> everything we've fought for a week, and it is guarded. You'll punch through its escort screen,
> then take it apart. Varga says its weak points are the launch sacs and a core under armour.
> When the carrier dies, the pods stop coming. Bring it down. Aegis Actual out."
>
> **Dr. Varga:** "On its first pass it flies above you, and only missiles reach that high. If
> you have them, use them. If not, survive until it comes down to your level, then hit the lime
> glow."
<!-- /data -->

*Images* (production art straight away, part F's D8; [briefing images](../../../ui/briefing/README.md);
rendered in step 3 of M4 part G, reviewed in round 25): Okafor's page `level-07-l1-carrier` (the carrier holding at
the Earth–Moon L1 point, the overrun picket, its escort screen), Varga's `level-07-overhead-scan`
(the overhead pass, missiles reaching up to the lime sacs, the plate iris); the level's data names
them as each page's `image`.

*Hangar teaser* (shop screen after L06):
<!-- data: teaser -->
> **Dr. Varga:** "One big target, armoured sacs in a row. Something that punches through them
> all, and missiles, if you can afford them."
<!-- /data -->

## Threat profile

<!-- data: threat-profile -->
| Field | Value |
|---|---|
| Dominant layers | `air`, `high-air` (the carrier's first phase), `low-air` (Spore Bombers in the escort screen) |
| Attack directions | front 98% · sides 2% |
| Density | 3 |
| Recommended traits | `forward`, `piercing` (`homing` for the overhead phase) |
| Hazards | none (spore haze is visual only) |
| Boss / mid-boss | Boss: Brood Carrier |
| Objective (intel, from sensor L1) | DESTROY THE CARRIER |
| Sensor-suite detail | none: L1 point, `air` + `high-air`, front · L1: + directions, density 3, "act boss", OBJECTIVE `DESTROY THE CARRIER` · L2: + Needler, Stinger, Skitter, Brood Pod, Spore Bomber, Mantis portraits; boss name and silhouette · L3: + `piercing` and `homing` highlighted, wave strip, 1 secret |
<!-- /data -->

## Objective

- **Primary** `reach-end` with the boss arena: the arena halts the level clock while the carrier
  lives, so the level can only end after the kill. It ends at the end of the *Aftermath*, about
  35 s after the kill, when the closing radio lines have played.
- **Secondary** *Gut the bays*: destroy all eight bay sacs before phase 2 times out (70 s after
  it starts); sacs destroyed with missiles in phase 1 count. +50. Tracker `BAYS n / 8`, then
  `DONE` in green, or `FAILED` in red at the timeout.

## Layout

Scene: the chosen [Earth orbit](../../../world/earth-orbit/README.md) scene
([sheet](../../../art-direction/concept/parallax-r03-a.png)) with the orbital defence ring
sub-location: the overrun L1 picket. The deep layer shows Earth smaller than in L01–L03, with
the Moon opposite. Scroll speed 170 px/s for the approach (normal, pressing forward), 20 px/s in
the boss arena (per the [Brood Carrier](../../../enemies/bosses/brood-carrier/README.md) spec),
40 px/s in the aftermath. Total ≈ 270 s ≈ 21,100 px at a fight of 135 s (the arena halts or
ends early with the kill). Motion budget: the spore haze and the drifting picket wreckage, then
the carrier alone, then its carcass and the ichor clouds.

<!-- data: level-sections -->
| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Picket Line | 0–30 | 0–5,100 | 170 | light | `deep`: Earth's disc behind, smaller than in L01–L03, the Moon ahead, stars. `far`: a sister sensor station breaking up. `ground`: overrun CDF picket platforms, gun mounts torn open, teal growth. `high-air`: ice streaks. | A fast start through the wreckage; the lifeboat secret. |
| 2. Escort Screen | 30–70 | 5,100–11,900 | 170 | medium | `ground`: the last picket platform, then open space. `low-air`: olive-grey spore haze left by the escort's bombers. `far`: the carrier's glow on the horizon. | Pods with escorts, bombers on `low-air`. |
| 3. Shadow of the Carrier | 70–100 | 11,900–17,000 | 170 | medium, heavy peak 88–94 | `far`: the carrier's silhouette growing. `low-air`: the carrier's spore wake (the heavy peak). `ground`: none. | Mantis pincer and the last escorts; the haze clears for the boss. |
| 4. Brood Carrier | 100–235 | 17,000–19,700 | 20 | clear | `air`: the carrier (one screen long), on `high-air` in phase 1. `deep`: Earth and the Moon. | The act boss, three phases (≈ 135 s at medium); the scroll halts at the section's end while the carrier lives. |
| 5. Aftermath | 235–270 | 19,700–21,100 | 40 | light | `ground`: the carrier's burnt-out carcass drifting away (the boss's own sprite, drawn on the play plane after the chained death; no backdrop piece). `low-air`: drifting ichor clouds from the carcass. `far`: long-range contacts appear as a glittering line beyond the Moon (the second fleet; on far, since deep moves only 21 px in the aftermath). | Credit shower, the closing radio lines in flight (35 s after the kill); the Choir, the second fleet. |
<!-- /data -->

The aftermath's 35 s at 40 px/s (user decision D2 of part G) are in the level's data: the arena
halts the level clock at its end while the carrier lives and jumps there at an earlier kill, so the
aftermath always runs its 35 s after the kill. The backdrop is the level's own
(`tools/art/backdrop_l07.py`, merged into the data on 2026-10-05): stars, Earth and the Moon on a
slow deep layer, the overrun picket, the carrier's silhouette ahead and the second fleet's line in
the aftermath.

## Waves

Enemy specs: [Needler](../../../enemies/air/needler/README.md),
[Stinger](../../../enemies/air/stinger/README.md),
[Skitter](../../../enemies/air/skitter/README.md),
[Brood Pod](../../../enemies/air/brood-pod/README.md),
[Spore Bomber](../../../enemies/air/spore-bomber/README.md),
[Mantis](../../../enemies/air/mantis/README.md),
[Brood Carrier](../../../enemies/bosses/brood-carrier/README.md). No new regular enemy: the act's
roster returns for the final exam.

The draft's 71 approach units (about 45 a minute, with 6–10 s gaps) are densified as in L04–L06
(more and larger waves of the act's light enemies, never generic filler): Skitter streams and
snakes, Needler and Stinger groups between the draft's beats, which stay at their times (the pods
with escorts at t=34 and 58, the bombers at 44 and 64, the Mantis pincer at 74, the pods at 80, the
last escorts at 96). That is 105 units, 129 with the pods' Skitters, about 81 a minute up to the
arena (Act 1 minimum 40, `DensityTest`), the densest level of the act as its final exam; the screen
is never empty for more than 3 s on the approach on any difficulty (`PacingTest`; the one longer
pause is the aftermath). The lifeboat tow drifts down the left half from t=22, so the waves of
t=21–34 keep to the right.

<!-- data: waves -->
| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 5.5 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | Over the first platforms |
| 8 | 1 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | Among the platforms |
| 12 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | |
| 16 | 1 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | |
| 18.5 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 23 | 1 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | Right of the lifeboat |
| 26 | 1 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 31 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | |
| 34 | 2 | carrier + escorts | [Brood Pod](../../../enemies/air/brood-pod/README.md) + [Needler](../../../enemies/air/needler/README.md) | 1 + 3 | front | |
| 38.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 42 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | |
| 44 | 2 | line abreast | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 2 | front | `low-air` |
| 47 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 50 | 2 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 4 | front | Over the spore mines |
| 53.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 58 | 2 | carrier + escorts | [Brood Pod](../../../enemies/air/brood-pod/README.md) + [Needler](../../../enemies/air/needler/README.md) | 1 + 3 | front | The pod drops the overdrive |
| 61.5 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | |
| 64 | 2 | line abreast | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 2 | front | The last bomber drops the special charge |
| 67 | 2 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 70.5 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (left) | Into the carrier's shadow |
| 74 | 3 | pincer | [Mantis](../../../enemies/air/mantis/README.md) | 2 | sides | Edge warnings; hold 6 s (easy 4 s); the second drops an armour patch |
| 76 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | Between the two Mantis |
| 80 | 3 | line abreast | [Brood Pod](../../../enemies/air/brood-pod/README.md) | 2 | front | While the Mantis hold; 3 on hard |
| 83 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (right) | |
| 86 | 3 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | Inside the heavy peak |
| 89 | 3 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 4 | front | |
| 92 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 4 | front (alternating edges) | |
| 96 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 4 | front | Last escorts; the last drops an armour patch |
| 100 | 4 | boss | [Brood Carrier](../../../enemies/bosses/brood-carrier/README.md) | 1 | front (nose-first over the top edge, on `high-air`) | Nose-first over the top edge; phase 1: 8 bay-pair openings, pairs 1 and 3 launching 4 Skitters, 2 and 4 2 Needlers (16 + 8); phase 2: 1 Skitter per open sac per window; after a phase-2 timeout 2 Skitters per surviving pair every 6 s |

Totals: Skitter 62 · Needler 23 · Stinger 10 · Brood Pod 4 · Spore Bomber 4 · Mantis 2 · Brood Carrier 1.
<!-- /data -->

The carrier's window launches in a typical fight (the autopilot with the plan's fit at medium):
phase 1 16 Skitters and 8 Needlers, phase 2 25 Skitters (one per open sac per window over about
40 s), no timeout; 41 Skitters and 8 Needlers in all, which the credit budget counts. Easy: 12 + 4
and about 17; hard: 20 + 8 and about 50 (3 per window).

## Ground targets

No hostile ground targets: the picket platforms in section 1 are wreckage (scenery). The
lifeboat secret drifts among them.

## Hazards

None beyond the enemies. Spore mines from the four bombers follow their spec; the heavy spore
wake in section 3 never hides bullets or mines.

## Secrets and pickups

Pickup types are defined in [player](../../../player/README.md#in-level-pickups).

- **Lifeboat tow** (hidden crate, 75): at t=22 a drifting CDF lifeboat (friendly, shots pass
  through it, it never collides) enters at the top edge at x 130 and drifts down and right
  (12, 45 px/s), across the screen by about t=36; it tows a cargo pod on an amber-lit cable. 3 hits
  on the cable cut it (the secret is found): the lifeboat drifts on with the cut stub, and the pod
  comes loose and falls away from it on its own, tumbling: it keeps the tow's drift, slips aside
  towards the middle of the field at 30 px/s and falls faster and faster (60 px/s²) until it leaves
  the screen. The crate falls out of the pod once the pod is down at 160 px above the bottom edge
  (the ship's part of the screen; at once if the cable is cut that low), about 2–3 s after a cut at
  the top, and is then an ordinary hidden crate ([pickups](../../../player/README.md#in-level-pickups):
  it drifts down at 40 px/s and is gone after 6 s), so a ship that stays low collects it. Sizes for
  the art step: boat 72×36 px, pod 32×32 px hanging 70 px above (behind) the boat's centre, the
  cable's hit box 16×40 px midway between them (the data's `tows`).
- **Armour patch** ×2: dropped by the second Mantis of the t=74 pincer and by the last Needler
  of the t=96 line, just before the boss. Not on hard.
- **Overdrive** ×2: dropped by the Brood Pod of the t=58 group and by the carrier's first
  destroyed bay sac (in any phase).
- **Special charge**: dropped by the last Spore Bomber (t=64).
- Shield cells at the normal rate.

## Radio chatter

Text and radio blips, voiced like every Act 1 level ([voice](../../../audio/voice/README.md));
the Choir's lines stay text. Rook leads Aegis Two and the rest of the wing against the carrier's
far-flank escorts (radio only). Lifeboat Seven is a generic CDF voice, cast in
[round 25](../../../concept-rounds/round-25/README.md) (Tadhg Hynes).

The times are the retimed plan of part G: every timed line starts within 1 s of its time on every
difficulty, with or without a homing weapon or a special fitted, with the lifeboat secret and the
carrier's phases, a timeout and a fast fight set off between them (`RadioTimelineTest`, text
speed; the voiced run follows once the lines are rendered). The two t=107 lines are one cue each
way (`requires` / `requires_not: homing`), so exactly one plays. The closing lines are plain
boss-destroyed cues in queue order (no delays).

<!-- data: radio -->
| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "This is it, Aegis. Kill the carrier and the pods stop coming." |
| t=8 | Rook | "Aegis Two and the rest of the wing are on its far flank, keeping the other escorts busy. Save us some, Lancer." |
| t=22 | Generic CDF (Lifeboat Seven) | "Lifeboat Seven, crew of four, drifting. Aegis, that pod we're towing is yours if you cut it loose." |
| t=44 | Varga | "Spore bombers screening the carrier. Same rules as the high lanes." |
| t=72 (the pincer's warning) | Rook | "Edges, Lancer! Snipers, both sides!" |
| t=82.5 | Varga | "Carrier in visual range. Everything it launches comes through those sacs. Hit the lime glow." |
| t=94 | The Choir (distorted) | "[the Choir sings]" |
| t=100 (boss warning) | Okafor | "Brood Carrier, dead ahead. She's all yours, Lancer." |
| t=107 (homing fitted) | Varga | "It's right over you. Your missiles can reach the open sacs. Everything else, clear the spawns." |
| t=107 (no homing) | Varga | "It's above you. You can't touch it up there. Clear the spawns and wait for it to come down." |
| Phase 2 starts | Varga | "It's coming down and turning broadside! The sacs open between volleys. That's your window." |
| Phase 2 timeout | Okafor | "Forget the sacs. Go for the core." |
| Phase 3 starts | Varga | "The iris is open. That's the core. Everything you've got!" |
| Carrier destroyed (1) | Rook | "Okay. Okay. That was big. We did big." |
| Carrier destroyed (2) | The Choir (distorted) | "[the Choir sings]" |
| Carrier destroyed (3) | Varga | "Same pattern as at the shipyards, and a new one. If I'm reading it right: *many*. And *late*." |
| Carrier destroyed (4) | Okafor | "Long range just lit up. A second fleet, much bigger, heading for Earth. Aegis, come home. We have work to do." |
| Secondary met | Okafor | "Every bay gutted. That carrier launches nothing more. Textbook, Lancer." |
<!-- /data -->

The secret's line (Rook, when the pod comes loose): "Pod's loose. Lifeboat Seven says it's yours,
Lancer. Finders keepers."

## Boss / mid-boss

[Brood Carrier](../../../enemies/bosses/brood-carrier/README.md), the Act 1 boss. Level notes:

- **Arena**: open space at the L1 point, beyond the picket, as in the spec. The scroll slows to
  20 px/s; the arena section halts the level clock while the carrier lives and ends at its death.
- **Phase 1** (25 s from the boss bar, `high-air`): the hull comes over nose-first at 40 px/s and
  stops above the ship; 8 bay-pair openings in hull order, each pair spawning either 4 Skitters or
  2 Needlers, alternating: 16 Skitters and 8 Needlers in all. At L07 the player can own the
  Micro-missile Pod (`homing`, from L06): only open sacs take damage, and only from `homing`; a
  sac destroyed now stays destroyed (it pays, stops spawning and drops the overdrive if it is the
  first). The hull on `high-air` follows the high-air drawing rule (bullets always on top).
- **Turn** (4 s, invulnerable): it descends to `air` while it glides to its broadside station
  (2 s), then turns broadside in place (2 s).
- **Phase 2** (sacs, ≤ 70 s after the turn): 1,440 HP, the sacs taking damage only while open.
- **Phase 3** (core 2,400 HP).
- **Kill times** of the autopilot with the plan's fit (the level test, from the bar): easy 72 s,
  medium 90 s, hard 102 s (with Micro-missile Pods 70 / 90 / 108 s); phase 2 lasts about 40 s
  after the turn at medium (it never timed out) and phase 3 about 21 s, as the sacs' ×1.5 and the core's ×2 weak-point
  multipliers count (the earlier estimate of 67 s for the core left the ×2 out). The fight is at
  the low end of the 90–150 s target; par is 100 s (the medium kill plus about 10 %), so the
  Boss rush bonus takes a quick kill.
- Boss checkpoint at the boss warning on easy and medium ([retry](../../../systems/retry/README.md)).
- Death: 3 s of chained explosions from tail to head (surviving sacs burst and pay in the chain),
  screen flash, credit shower; then the aftermath with the closing lines, the debrief with the act
  summary and the act-end outro (see the [act](../README.md#act-intro-and-outro)).

## Music & ambience

Track 5 *Act 1 B: Earth orbit & Luna* ("Coalition Rising", the main theme in full, see the
[track list](../../../audio/music/README.md#track-list)) for the approach. At t=100 the boss
warning: track 22 with the warning klaxon and the boss banner, bridging into track 18 *Boss:
Vrell* ("The Choir Descends") for the fight. At the kill the boss track fades out and only the
Earth orbit ambience ([sfx](../../../audio/sfx/README.md#ambience-per-setting)) plays through the
aftermath; the debrief plays its usual jingle. Track 24 *Act complete* belongs to the act-end
outro, under its first page (see the [act](../README.md#act-intro-and-outro)).

## Credit budget

Budget(7) = 700 × 1.07⁶ ≈ **1,051**, the typical haul's target
([economy](../../../systems/economy/README.md#per-level-budget)), not a perfect collection.
Bounties from the stat blocks: Needler 12, Stinger 15, Skitter 5, Brood Pod 20, Spore Bomber 25,
Mantis 30, Brood Carrier 450 (bays 8 × 25 + core 250). The carrier keeps its stat-block bounty,
about 43 % of the budget before the level's `bounty_scale`, which absorbs it, as in L05. Boss spawns count at the number a typical fight sees (economy rule); the boss parts
at 100 %, since the boss dies in every won run.

The level's `bounty_scale` of 0.75 puts the typical haul on budget(7) (`TypicalHaulTest`,
1,049, −0.2 %); at that scale the carrier pays 340, about 32 % of the budget. Its spawns count as the
autopilot's typical fight launches them (the level data's `boss.notes.spawns`: 41 Skitters and
8 Needlers).

<!-- data: credit-budget -->
| Source | Perfect run | Typical haul |
|---|---|---|
| Kills: Skitter 62 × 5 + Needler 23 × 12 + Stinger 10 × 15 + Brood Pod 4 × 20 + Spore Bomber 4 × 25 + Mantis 2 × 30 + released Skitter 24 × 5 | 841 | 505 |
| Boss: Brood Carrier (bays 8 × 25 + core 250) | 340 | 340 |
| Boss spawns: Skitter 41 × 5 + Needler 8 × 12 (a typical fight) | 236 | 142 |
| Secret: lifeboat tow (hidden crate, 7% of budget) | 75 | 38 |
| Secondary: all 8 bays destroyed before the broadside phase ends | 50 | 25 |
| **Total** (bounty scale 0.75) | **1,542** | **1,049** |
| Budget(n) = the typical haul's target; typical -0 %, perfect 1.47 × budget | | 1,051 |
<!-- /data -->

Spawns after a phase-2 timeout are not counted: a run that times out trades them for the
secondary. Spore mines (1 each) are excluded as in L03.

## Difficulty notes

- **Easy**: phase-1 openings spawn 3 Skitters or 1 Needler; the approach Mantis hold for 4 s
  instead of 6 s. The densified waves have no changes of their own (the formation-size lever
  applies).
- **Hard**: the phase-3 spiral has 4 arms and every opening that spawns Skitters spawns one more
  (stat-block hooks); the t=80 line has 3 Brood Pods; neither armour patch drops (both come before
  the boss). The densified waves have no changes of their own; phase 2 then launches 3 Skitters
  per window.

## Concept art

The chosen scene is [Earth orbit](../../../art-direction/concept/parallax-r03-a.png) (see *Layout*);
the level's production art goes straight to production (part F's D8) and is reviewed in round 25.
Briefs and generators: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/backdrop-final-r25-a.png](concept/backdrop-final-r25-a.png) | Final backdrop (`tools/art/backdrop_l07.py`, its block merged into the level's data): stars, Earth's whole disc and the Moon on deep (0.015), the sister sensor station breaking up and the picket's torn ring on far, the carrier's hazed silhouette ahead (sections 2–3, on a path) and the second fleet's glittering line (aftermath), overrun picket platforms, torn gun mounts and truss, wrecks on ground, Level 03's spore haze and banks and the aftermath's ichor clouds on low-air, frost and spore streaks on high-air; with twelve composites of the level | chosen |
| [concept/rejected/lifeboat-r25-a.png](concept/rejected/lifeboat-r25-a.png), [.gif](concept/rejected/lifeboat-r25-a.gif) | Lifeboat tow A, "white rescue boat" (`tools/concept/props_r25.py`): a white lifting-body lifeboat (72×36, nose down) with orange outer wings, a CDF blue band, four lit windows and a green strobe; a plain olive canister pod (32×32, not a target); a braided tether (16×44) whose three blinking amber marker lamps are the only target cue, one going out per hit; in context over the level's backdrop with the player ship | rejected (user picked b) |
| [concept/lifeboat-r25-b.png](concept/lifeboat-r25-b.png), [.gif](concept/lifeboat-r25-b.gif) | Lifeboat tow B, "orange lifeboat capsule": a rescue-orange pressure capsule lying across, white end caps and bands, lit portholes, two blue strobes and a tow bridle; a crate-pod with an amber/black hazard lid (reads as loot); a thin cable with an amber light strip and a hazard-striped breakaway coupler where the hits land, split by the second hit | chosen |
| [concept/lifeboat-final-r25-a.png](concept/lifeboat-final-r25-a.png), [.gif](concept/lifeboat-final-r25-a.gif) | Final lifeboat tow sprites from the chosen b (`tools/art/lifeboat.py`): the boat 72×36 with the strobes lit and dark and the strobes' additive halo; the cable 16×44 intact, after 1 hit (the strip above the coupler dark), 2 hits (the coupler split), cut (the lower jaw on the stub from the boat), with the light strip's additive glow (intact, hit); the pod 44×44 at 24 tumble headings, then the same headings with the lid blown off and the hold empty (after the crate fell out); in the level over the backdrop, the GIF from the tow's entry to the crate falling | chosen |
| [concept/lifeboat-capture-final-r25-a.png](concept/lifeboat-capture-final-r25-a.png) | A capture of the game with the final tow sprites, not generated art (see [prompts.md](concept/prompts.md#lifeboat-capture-final-r25-a)): seven play-field crops labelled with the level time (intact, 1 hit, 2 hits, the cut, the pod tumbling, the crate out of the emptied pod, the crate drifting down) and 2× crops of the tow | chosen |
| [concept/level-07-capture-final-r25-a.png](concept/level-07-capture-final-r25-a.png), [.mp4](concept/level-07-capture-final-r25-a.mp4) | A capture of the game, not generated art, retaken after the round's fixes (see [prompts.md](concept/prompts.md#level-07-capture-final-r25-a)): 30 play fields and 6 screens, each labelled with its level time (bar + s from the boss warning, kill + s after the flash): the lifeboat tow (placeholder sprites) entering at t 23, its cable shot (3 hits in 0.2 s), the pod tumbling away, the crate falling out of it 160 px above the bottom edge at t 28.4 and collected; the boss warning banner; the carrier coming over nose-first on high-air at 75 % opacity, bay pair 1 open, launched Skitters drawn over the hull, the hold over the ship (and a 1:1 crop of the ship under the hull); the descend (BAYS 0 / 8, the bar at 77 % from missiles on the open sacs), the turn and the broadside station; pair 1 open with the steady pale hit tint, a sac bursting, a launched Skitter; the iris opening and the core's spiral and ring; the death chain from the tail, the screen flash, the break-up; the aftermath's carcass, its cracks fading, the dark carcass to the end and the second fleet's line of glittering contacts; the debrief (BOSS TIME 0:59, PAR 1:40, SECRETS 1 / 1 with 75 secret credits), the ACT I COMPLETE summary and the four outro pages. The mp4 (81 s, with the game's sound under the master limiter, peak 1.0) plays the tow from t 21.5 to the pick-up and the whole fight from t 98.5 to kill + 8 s | chosen |

## Implementation

- [x] Scroll timeline, sections, atmosphere intensity and parallax content as in *Layout*,
      including the 20 px/s boss arena and the 35 s aftermath (Level 07's own backdrop).
- [x] Wave script from the level's data, densified: at least 40 enemies per minute of scroll at
      medium up to the arena (`DensityTest`) and the pacing rule (`PacingTest`) on every
      difficulty.
- [x] Brood Carrier per its spec: the overhead phase, the turn, the sac windows, the core phase;
      its spawns as read above.
- [x] Phase-dependent radio lines (homing fitted or not, the phase-2 timeout) and the closing
      lines after the kill in queue order; every timed line within 1 s of its time.
- [x] Secondary objective: all eight sacs destroyed before the 70 s timeout, with its tracker.
- [x] Lifeboat tow secret; the lifeboat is not hittable, the cable takes 3 hits (its sprites come
      with the art step).
- [x] Pickups as in *Secrets and pickups*, the overdrive from the first destroyed sac.
- [x] Boss checkpoint on easy/medium.
- [x] Music: the boss warning (track 22, klaxon, banner) into track 18, out at the kill.
- [x] After the aftermath: the debrief with the act summary, then the act-end outro (not on a
      replay).
- [x] Typical haul at medium within ±5 % of budget(7) = 1,051 with the level's `bounty_scale`
      (`TypicalHaulTest`).
- [x] Easy/hard variations as in *Difficulty notes*.

## Decisions

- 2026-10-01: Promoted from the act roster to a draft level document. Approach of about 100 s
  through the overrun L1 picket and the escort screen with no new regular enemy (the act's roster
  returns), then the boss. The Choir stays unintelligible ("[the Choir sings]"); Varga's partial
  reading ("many", "late") replaces the earlier spoken line and foreshadows its first words in
  Act 3.
- 2026-10-01: Open questions resolved in the Brood Carrier spec: arena at the L1 point; phase-1 spawns per bay pair.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-05: M4 part G decisions (user). **D1 = b**: the carrier comes over nose-first at
  40 px/s, stops centred above the ship and spawns for the rest of phase 1, then descends (scale
  1.25 → 1.0) and turns 90° in place through about 6–8 pre-rendered frames in about 2 s,
  invulnerable, into broadside (a pose in the simulation, no rotated hit boxes); rejected: a pass
  that leaves and returns broadside (loses the turn), and rotating the sprite at run time (breaks
  the fixed key light). **D2 = a**: the aftermath grows to about 35 s with the drifting carcass and
  the closing lines in flight, then the debrief with the act summary, then the act outro (four
  voiced pages, track 24 under page 1), then the autosave into the hangar before L08, shown as
  not built yet; the Act 2 title card comes with Act 2's intro in M5; rejected: a 10 s aftermath
  with the lines moved into the outro (rewrites the act end), and the outro before the debrief.
  **D3 = b**: four outro images, one per page (see the [act](../README.md#act-intro-and-outro)).
  **D4 = b**: the Decoy Flares unlock moves to the act of the first homing enemy
  ([specials](../../../player/specials/README.md)); they leave the L07 intel ("Decoy Flares in the
  shop" removed from the sensor L2 detail) and part G; L07's only new shop item is the Hammer
  Mortar.
- 2026-10-05: M4 part G defaults (stated by the main agent, not objected to): the budget follows
  the current rule, the typical haul on budget(7) ≈ 1,051 with a `bounty_scale`, the carrier
  keeping its 450 bounty (the 1,000 × 1.07⁶ = 1,501 perfect-run target was stale); the approach is
  densified with the act's light enemies, as L06's D1; in phase 1 only open sacs take damage, only
  from `homing` (no `beam` weapon before L15); phase-2 windows release 1 Skitter per open living
  sac and a timeout 2 Skitters per surviving pair every 6 s, budgeted at a typical fight's count;
  the windows cycle through the living pairs; surviving sacs burst and pay at the kill; par 150 s;
  the closing lines are boss-destroyed cues in queue order; the radio is retimed to the 1 s rule
  and Varga's t=95 line shortened to two pages (it moves to t=84); no outro on a replay; the
  lifeboat tow and the new sounds get a/b concepts, the carrier, backdrop and briefing images go
  straight to production; Lifeboat Seven is auditioned in the part's round, uncast until then.
- 2026-10-05: Main-agent wording and readings for part G: the homing line says "the open sacs"
  (only they take damage in phase 1); the secondary line no longer says "before it could turn"
  (the sacs die after the turn); the primary stays `reach-end`, since the arena makes the kill a
  condition of the level's end, with DESTROY THE BROOD CARRIER as the intel objective; "the level
  ends when the carrier dies" became "35 s after"; the overdrive comes from the first sac
  destroyed in any phase. The open question on the t=95 line is closed by its new wording.
- 2026-10-05: The user confirmed the phase-1 timing that follows from D1: bay openings every
  2.5 s from about 6 s (the 3 s cadence no longer fits the 25 s phase), a pair with one dead sac
  spawning half, rounded up; and no act summary on a replay (a replay ends with a normal debrief).
- 2026-10-05: Level data of part G (step 2): the waves densified to 105 units (129 with the pods'
  Skitters), about 81 a minute, the draft's beats at their times, two Skitter streams at t=26 and
  t=70.5 closing the pacing gaps; `bounty_scale` 0.75 (typical haul 1,049, −0.2 %; perfect 1,542);
  the carrier's typical spawns counted at the autopilot's medium fight (41 Skitters, 8 Needlers);
  the lifeboat tow at t=22 from x 130, drifting (12, −45) px/s, boat 72×36, pod 32×32 70 px
  behind, cable 16×40; the secret's line given to Rook; the briefing images named
  `level-07-l1-carrier` and `level-07-overhead-scan`; the secondary's tracker label `BAYS`, failed
  when the `Broadside` phase times out ("BONUS: DESTROY ALL 8 BAYS BEFORE THE BROADSIDE ENDS");
  Level 03's backdrop images until Level 07's own. Varga's carrier line at t=82.5 instead of t=84:
  its two pages ran 1.5 s into the Choir's t=94 (and so pushed Okafor's t=100 and Varga's t=107);
  with it every timed line starts within 1 s. Cuts for the hangar's intel panel (its fields ran 4 px past
  the panel's foot at sensor L2 and into Varga's quote, `IntelPanelLayoutTest`): the teaser reads
  "One big target, armoured sacs in a row. Something that punches through them all, and missiles,
  if you can afford them." (two lines less), Varga's intel line is "One ship at L1.
  Station-sized." (one line), and the OBJECTIVE field `DESTROY THE CARRIER` (the full name runs
  past the panel's right edge).
- 2026-10-05: Par tightened from 150 s to 100 s (user): every autopilot run beat 150 s
  (72 / 90 / 102 s), so the Boss rush bonus would have paid nearly every run; the fight length
  and the core's HP stay. Rejected: raising the core's HP, keeping 150 s.
- 2026-10-05: Level 07's own backdrop (`tools/art/backdrop_l07.py`, art step A2 of part G,
  proposed for round 25): the deep layer at 0.015 (below the art direction's 0.05–0.2), so Earth
  behind and the Moon ahead stay on screen from the start through the arena and an early kill's jump
  to the arena's end moves them by at most ~20 px; the carrier's growing silhouette is a far piece on
  a path (nose up, Lancer closing in), which pulls away over the top edge inside the heavy spore wake
  before the boss comes over it; the second fleet is a far piece (the deep layer moves 21 px in the
  whole aftermath); the carcass stays the boss's own sprite (no backdrop piece); nothing but the
  deep layer is on screen in the arena. One backdrop page of 2048² (76 % full).
- 2026-10-05: The lifeboat tow after the cut (user, pick **b** after the round-25 capture, where the
  crate hung about 200 px above a ship that stayed low and expired, SECRETS FOUND 1/1 with 0 secret
  credits): the pod no longer vanishes at the cut with the crate in its place; it comes loose and
  tumbles away on its own (slipping aside so it falls clear of the boat, ever faster) and the crate
  falls out of it low on the screen, a normal hidden crate from there (see *Secrets and pickups*).
  Picked over drifting on with the boat: a pod that keeps the boat's 45 px/s would bring the crate
  down no sooner than today's.
- 2026-10-05: Homing on the overhead pass (the round-25 capture with
  `front=lance-laser:4,left=micro-missile-pod:4,right=micro-missile-pod:4` showed BAYS 0/8 at the
  turn). Cause, confirmed by a test: not the cone alone; a missile launched from a ship under the
  hovering carrier met the armoured hull at once (the hull's 220×560 box covers the field from x 130
  to 350 over its whole height) and glanced unless a part lay straight ahead, and pair 1,
  about 108 px beside the ship at its height, lies inside the missile's turning circle (500 px/s at
  270°/s, a radius of about 106 px), so no missile could reach it. Fix (sim, a high-air boss only):
  a missile seeks its open parts all round within its 350 px (not only in the 140° cone), climbs to
  the one it locks onto at twice its turn rate, and passes beneath the hull and every other part;
  homing elsewhere is unchanged (Levels 01–06, the Leviathan's high-air pass). The autopilot at
  medium (pulse-cannon:3) now does 354 HP to the carrier in phase 1 with pods L1 (no sac burst;
  easy 448, hard 198) and 884 HP with pods L4 (one sac burst, the overdrive drop); the bays
  secondary is met in every run, the kill at 82.8 s with L1 pods (90.2 s with the plan's fit). No
  number changes: phase 1 stays about clearing the spawns, with missiles a head start on the sacs.
- 2026-10-05: Round 25 closed (user): the backdrop approved as final as it is (the deep factor
  0.015 kept), the capture and the part G numbers accepted (density, `bounty_scale` 0.75, par
  100 s, the boss spawns and timing, the homing fix, the tow, the radio retimes, the shortened intel
  texts, Rook's secret line, the act summary layout and the D-decision defaults); the lifeboat tow
  **b** (the orange capsule, the hazard-lidded crate-pod and the amber cable with its breakaway
  coupler) for its production sprites; Lifeboat Seven cast (Tadhg Hynes, a), its t=22 line rendered;
  the chosen sounds play (roar b, sac opening and closing b, launch b, sac burst a, iris b, cable
  snap a). Every Implementation item is done; `art: final` with the round's approval (the tow's
  production sprites follow from b).
- 2026-10-05: Lifeboat tow variant **b** chosen (user), variant a moved to `concept/rejected/`
  (user picked b). Its production sprites (`tools/art/lifeboat.py`, in the level's unit atlas, now
  68 % of its 2048² page; the level at 2 of 6 pages) are drawn by the game: the boat's strobes flash
  once a second (lit frame plus an additive halo), the cable shows the hits taken (`hits` minus the
  hits left; the cut stub last) with its light strip pulsing at 1.2 Hz and the shared loot glint on
  the coupler every 2 s while it holds, and the loose pod tumbles through 24 pre-rendered headings
  (the art rule: nothing lit is rotated at runtime) and shows its emptied hold once the crate has
  fallen out, so the pod no longer reads as loot. The pod's frames are 44×44 over its 32×32 box so
  the corner posts fit at 45°. Review files and a game capture proposed in round 25.
- 2026-10-05: The lifeboat's production sprites (variant b, `tools/art/lifeboat.py`) approved as final by the
  user, including the empty pod (lid blown off) shown once the crate has fallen out.
