---
title: Level 07 – Brood Carrier
design: approved
implementation: not-started
art: chosen
depends-on: [../../../enemies/bosses/brood-carrier, ../../../world/earth-orbit]
updated: 2026-10-01
---

# Level 07 – Brood Carrier

## Summary

The act finale. Varga has traced every pod back to one ship holding at the Earth–Moon L1 point,
where the Vrell first arrived. Lancer flies through the wreck of the overrun L1 picket and the
carrier's escort screen, then fights the Brood Carrier: first as its hull passes over on
`high-air`, then broadside, then at its core. A short approach (about 100 s) plus the boss
(90–150 s). The Choir answers its death, and a second, larger fleet is detected heading for
Earth.

## Briefing

> **Commander Okafor:** "Aegis, Varga has traced every pod that hit Luna back to one ship: a
> brood carrier holding at L1, where this started. It's the size of a station, it launches
> everything we've fought for a week, and it is guarded. You'll punch through its escort
> screen, then take it apart. Varga says its weak points are the launch sacs and a core under
> armour. When the carrier dies, the pods stop coming. Bring it down. Aegis Actual out."
>
> **Dr. Varga:** "On its first pass it flies above you, and only missiles reach that high. If
> you have them, use them. If not, survive until it comes down to your level, then hit the lime
> glow."

*Hangar teaser* (shop screen after L06):
> **Dr. Varga:** "One big target with armoured sacs in a row. A gun that punches through more than
> one would be ideal, and missiles, if you can afford them."

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `air`, `high-air` (the carrier's first pass), `low-air` (Spore Bombers in the escort screen) |
| Attack directions | front 93% · sides 7% |
| Density | 3 |
| Recommended traits | `forward`, `piercing` (`homing` for the overhead pass) |
| Hazards | none (spore haze is visual only) |
| Boss / mid-boss | Boss: Brood Carrier |
| Sensor-suite detail | none: L1 point, `air` + `high-air`, front · L1: + directions, density 3, "act boss" · L2: + Needler, Stinger, Skitter, Brood Pod, Spore Bomber, Mantis portraits; boss name and silhouette; Decoy Flares in the shop · L3: + `piercing` and `homing` highlighted, wave strip, 1 secret |

## Objective

- **Primary** `boss`: destroy the Brood Carrier. The approach is `reach-end`; the level ends
  when the carrier dies.
- **Secondary** *Gut the bays*: destroy all eight bay sacs before the phase-2 timeout (70 s).
  +50.

## Layout

Scene: the chosen [Earth orbit](../../../world/earth-orbit/README.md) scene
([sheet](../../../art-direction/concept/parallax-r03-a.png)) with the orbital defence ring
sub-location: the overrun L1 picket. The deep layer shows Earth smaller than in L01–L03, with
the Moon opposite. Scroll speed 170 px/s for the approach (normal, pressing forward), 20 px/s in
the boss arena (per the [Brood Carrier](../../../enemies/bosses/brood-carrier/README.md) spec).
Total ≈ 245 s ≈ 21,400 px. Motion budget: the spore haze and the drifting picket wreckage, then
the carrier alone.

| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Parallax content per layer | Purpose |
|---|---|---|---|---|---|---|
| 1. Picket Line | 0–30 | 0–5,100 | 170 | light | `deep`: Earth's disc behind, the Moon ahead, stars. `far`: a sister sensor station breaking up. `ground`: overrun CDF picket platforms, gun mounts torn open, teal growth. `high-air`: ice streaks. | A fast start through the wreckage; the lifeboat secret. |
| 2. Escort Screen | 30–70 | 5,100–11,900 | 170 | medium | `ground`: the last picket platform, then open space. `low-air`: olive-grey spore haze left by the escort's bombers. `far`: the carrier's glow on the horizon. | Pods with escorts, bombers on `low-air`. |
| 3. Shadow of the Carrier | 70–100 | 11,900–17,000 | 170 | medium, heavy peak 88–94, then clear | `far`: the carrier's silhouette growing. `low-air`: the carrier's spore wake (the heavy peak). `ground`: none. | Mantis pincer and the last escorts; the haze clears for the boss. |
| 4. Brood Carrier | 100–235 | 17,000–19,700 | 20 | clear | `air`/`high-air`: the carrier (one screen long). `deep`: Earth and the Moon. | The act boss, three phases (≈ 135 s at medium). |
| 5. Aftermath | 235–245 | 19,700–21,400 | 170 | light | `low-air`: drifting ichor clouds from the carcass. `deep`: long-range contacts appear as a glittering line beyond the Moon. | Credit shower, the Choir, the second fleet. |

## Waves

Enemy specs: [Needler](../../../enemies/air/needler/README.md),
[Stinger](../../../enemies/air/stinger/README.md),
[Skitter](../../../enemies/air/skitter/README.md),
[Brood Pod](../../../enemies/air/brood-pod/README.md),
[Spore Bomber](../../../enemies/air/spore-bomber/README.md),
[Mantis](../../../enemies/air/mantis/README.md),
[Brood Carrier](../../../enemies/bosses/brood-carrier/README.md). No new regular enemy: the act's
roster returns for the final exam.

| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 8 | 1 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | Among the platforms |
| 16 | 1 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | |
| 24 | 1 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front | |
| 34 | 2 | carrier + escorts | [Brood Pod](../../../enemies/air/brood-pod/README.md) + [Needler](../../../enemies/air/needler/README.md) | 1 + 3 | front | |
| 44 | 2 | line abreast | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 2 | front | `low-air` |
| 50 | 2 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 4 | front | Over the spore mines |
| 58 | 2 | carrier + escorts | [Brood Pod](../../../enemies/air/brood-pod/README.md) + [Needler](../../../enemies/air/needler/README.md) | 1 + 3 | front | |
| 64 | 2 | line abreast | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 2 | front | |
| 74 | 3 | pincer | [Mantis](../../../enemies/air/mantis/README.md) | 2 | sides | Edge warnings |
| 80 | 3 | line abreast | [Brood Pod](../../../enemies/air/brood-pod/README.md) | 2 | front | While the Mantis hold |
| 86 | 3 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | Inside the heavy peak |
| 92 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (alternating edges) | |
| 96 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 4 | front | Last escorts; boss warning at t=100 |
| 100 | 4 | boss | [Brood Carrier](../../../enemies/bosses/brood-carrier/README.md) | 1 | front | Phase 1 spawns: 8 bay-pair openings, alternating 4 Skitters and 2 Needlers (16 + 8) |

Totals (approach): Needler 15 · Stinger 10 · Skitter 12 · Brood Pod 4 (24 Skitters released) ·
Spore Bomber 4 · Mantis 2. Boss phase 1: Skitter 16 · Needler 8.

## Ground targets

No hostile ground targets: the picket platforms in section 1 are wreckage (scenery). The
lifeboat secret drifts among them.

## Hazards

None beyond the enemies. Spore mines from the four bombers follow their spec; the heavy spore
wake in section 3 never hides bullets or mines.

## Secrets and pickups

Pickup types are defined in [player](../../../player/README.md#in-level-pickups).

- **Lifeboat tow** (hidden crate, 75): at t≈26 a drifting CDF lifeboat (friendly, shots pass
  through it) tows a cargo pod on an amber-lit cable. 3 hits on the cable release the pod.
- **Armour patch** ×2: dropped by the second Mantis of the t=74 pincer and by the last Needler
  of the t=96 line, just before the boss.
- **Overdrive** ×2: dropped by the Brood Pod of the t=58 group and by the carrier's first
  destroyed bay sac.
- **Special charge**: dropped by the last Spore Bomber (t=64).
- Shield cells at the normal rate.

## Radio chatter

Text and radio blips only. Rook leads Aegis Two and the rest of the wing against the carrier's
far-flank escorts (radio only).

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "This is it, Aegis. Kill the carrier and the pods stop coming." |
| t=4 | Rook | "Aegis Two and the rest of the wing are on its far flank, keeping the other escorts busy. Save us some, Lancer." |
| t=22 | Generic CDF (Lifeboat Seven) | "Lifeboat Seven, crew of four, drifting. Aegis, that pod we're towing is yours if you cut it loose." |
| t=44 | Varga | "Spore bombers screening the carrier. Same rules as the high lanes." |
| t=72 | Rook | "Edges, Lancer! Snipers, both sides!" |
| t=95 | Varga | "Carrier in visual range. It's as big as the main dock at Gagarin. It launches everything through those sacs. Hit the lime glow." |
| t=98 | The Choir (distorted) | "[the Choir sings]" |
| t=100 (boss warning) | Okafor | "Brood Carrier, dead ahead. She's all yours, Lancer." |
| t=103, homing fitted | Varga | "It's passing over you. Your missiles can reach it up there. Everything else, clear the spawns." |
| t=103, no homing | Varga | "It's above you. You can't touch it up there. Clear the spawns and wait for it to come down." |
| Phase 2 starts | Varga | "It's coming down and turning broadside! The sacs open between volleys. That's your window." |
| Phase 2 timeout | Okafor | "Forget the sacs. Go for the core." |
| Phase 3 starts | Varga | "The iris is open. That's the core. Everything you've got!" |
| Carrier destroyed | Rook | "Okay. Okay. That was big. We did big." |
| +3 s | The Choir (distorted) | "[the Choir sings]" |
| +5 s | Varga | "Same pattern as at the shipyards, and a new one. If I'm reading it right: *many*. And *late*." |
| +8 s | Okafor | "Long range just lit up. A second fleet, much bigger, heading for Earth. Aegis, come home. We have work to do." |
| Secondary met | Okafor | "Every bay gutted before it could turn. Textbook, Lancer." |

## Boss / mid-boss

[Brood Carrier](../../../enemies/bosses/brood-carrier/README.md), the Act 1 boss. Level notes:

- **Arena**: open space at the L1 point, beyond the picket, as in the spec. The scroll slows to
  20 px/s.
- **Phase 1** (25 s, `high-air`): read in this level as 8 bay-pair openings (every 3 s), each
  pair spawning either 4 Skitters or 2 Needlers, alternating: 16 Skitters and 8 Needlers in all.
  At L07 the player can own the Micro-missile Pod (`homing`, from L06); damage to the bays
  carries over. The hull on `high-air` follows the high-air drawing rule (bullets always on top).
- **Phase 2** (sacs, ≤ 70 s): 1,440 HP at an effective 0.6 × 60 = 36 DPS ≈ 40 s.
- **Phase 3** (core 2,400 HP): ≈ 67 s. Whole fight ≈ 135 s, inside the 90–150 s target.
- Boss checkpoint at the boss warning on easy and medium ([retry](../../../systems/retry/README.md)).
- Death: 3-s chained explosions tail to head, screen flash, credit shower; then the act-end
  outro (see the [act](../README.md#act-intro-and-outro)).

## Music & ambience

Track 5 *Act 1 B: Earth orbit & Luna* ("Coalition Rising", the main theme in full, see the
[track list](../../../audio/music/README.md#track-list)) for the approach; track 22 boss warning
at t=100, then track 18 *Boss: Vrell* ("The Choir Descends"); track 24 act complete after the
outro radio lines. Ambience: Earth orbit ([sfx](../../../audio/sfx/README.md#ambience-per-setting))
with the warning klaxon under the boss warning.

## Credit budget

Budget(7) = 1,000 × 1.07⁶ ≈ **1,501** ([economy](../../../systems/economy/README.md#per-level-budget)).
Bounties from the stat blocks: Needler 12, Stinger 15, Skitter 5, Brood Pod 20, Spore Bomber 25,
Mantis 30, Brood Carrier 450 (bays 8 × 25 + core 250, ≈ 30% of the budget).

| Source | Credits (medium) |
|---|---|
| Kills (approach): Needler 15 × 12 + Stinger 10 × 15 + Skitter 12 × 5 + Brood Pod 4 × 20 + released Skitters 24 × 5 + Spore Bomber 4 × 25 + Mantis 2 × 30 | 750 |
| Boss: Brood Carrier 450 + phase-1 spawns (Skitter 16 × 5 + Needler 8 × 12) | 626 |
| Pickups: lifeboat tow 75 (5%) | 75 |
| Secondary: gut the bays | 50 |
| **Total** | **1,501** |

Spawns in phase 2 and after a phase-2 timeout only happen while sacs survive, so they are not
in the total. Spore mines (1 each) are excluded as in L03.

## Difficulty notes

- **Easy**: phase-1 openings spawn 3 Skitters or 1 Needler; the approach Mantis hold for 4 s
  instead of 6 s.
- **Hard**: phase-3 spiral has 4 arms and bays spawn one extra Skitter (stat-block hooks); the
  t=80 line has 3 Brood Pods; no armour patch before the boss.

## Implementation

- [ ] Scroll timeline, sections, atmosphere intensity and parallax content as in *Layout*,
      including the 20 px/s boss arena.
- [ ] Approach wave script matches the *Waves* table.
- [ ] Brood Carrier per its spec, with the phase-1 spawn script as read above.
- [ ] Phase-dependent radio lines (homing fitted or not, timeout).
- [ ] Secondary objective: all eight sacs destroyed before the 70-s timeout.
- [ ] Lifeboat tow secret; the lifeboat is not hittable.
- [ ] Boss checkpoint on easy/medium.
- [ ] Act-end outro plays after the carrier's death sequence.
- [ ] Credit total at medium with perfect collection is 1,501 (± 5%).
- [ ] Easy/hard variations as in *Difficulty notes*.

## Decisions

- 2026-10-01: Promoted from the act roster to a draft level document. Approach of about 100 s
  through the overrun L1 picket and the escort screen with no new regular enemy (the act's roster
  returns), then the boss. The Choir stays unintelligible ("[the Choir sings]"); Varga's partial
  reading ("many", "late") replaces the earlier spoken line and foreshadows its first words in
  Act 3.
- 2026-10-01: Open questions resolved in the Brood Carrier spec: arena at the L1 point; phase-1 spawns per bay pair.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
