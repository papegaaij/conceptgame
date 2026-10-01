---
title: Level 01 – Break at Dawn
design: approved
implementation: not-started
art: chosen
depends-on: [../../../enemies/air/skitter, ../../../enemies/air/needler]
updated: 2026-10-01
---

# Level 01 – Break at Dawn

## Summary

The first level and the tutorial. At dawn over Earth, a Vrell scout force hits the **Gagarin
shipyards**, the UTC's orbital shipyard. Lancer launches with Aegis Wing and fights off waves of
Skitters and Needlers over the yard's gantries. The level teaches movement, shooting, pickups
and credits, and ends in a large circling Needler formation. There is no boss. About 3 minutes.

## Briefing

> **Commander Okafor:** "Lancer, this is Aegis Actual. Six weeks ago something came through the
> Tether Gate. Forty minutes ago it arrived at L1, and now it's falling on the Gagarin yards. We
> don't know what they are or what they want. What we do know is that half the Coalition's new
> hulls are sitting in those docks. You're the first bird off the south rail; Rook takes Aegis
> Two off the north rail and covers the far side of the yard. Keep them off the yards, keep
> your head, and bring me back something we can study. Aegis Actual out."
>
> **Dr. Varga:** "Their ships don't show up as metal on our scopes. Whatever they're made of, it
> burns. Aim for the glowing parts."

*Hangar teaser* (on the shop screen of the first hangar visit, after the act intro):
> **Dr. Varga:** "Unknown contacts, small and fast. Your cannon will do, Lancer. Spend a little
> on it if you can."

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `air` (all enemies), `deep` (Earth, the yards' far structures) |
| Attack directions | front 85% · sides 10% · rear 5% (one warned wave) |
| Density | 1 |
| Recommended traits | `forward` |
| Hazards | none |
| Boss / mid-boss | none (final wave: Needler circle) |
| Sensor-suite detail | The sensor suite (800) is in the shop from the start but out of reach of the 300 starting credits, so the panel shows the no-sensor view: setting, dominant layer `air`, main direction front. Varga: "Our scans are patchy, Lancer. Small, fast, lots of them." |

## Objective

- **Primary** `reach-end`: survive to the end of the scroll.
- **Secondary**: destroy at least 80% of all enemies. Pays +50 credits and triggers a line from
  Okafor.

## Layout

Scene: the chosen [Earth orbit](../../../world/earth-orbit/README.md) scene
([sheet](../../../art-direction/concept/parallax-r03-a.png)), used here at the light end of the
atmosphere range. Scroll speed 130 px/s at the 960×540 baseline (a calm level per the
[art direction](../../../art-direction/README.md#parallax-layer-model); play field 480×540).
Total ≈ 190 s ≈ 24,700 px. Motion budget: the only strongly animated background elements are the
drifting low-air cloud decks and, in section 4, the burning platforms.

| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Launch | 0–20 | 0–2,600 | 130 | clear | `deep`: Earth's curve with the sunrise terminator, starfield. `far`: the yard's north arm, where two distant Stormhawks (Aegis Two, Rook's flight) launch and bank away out of view. `ground`: the Gagarin shipyards' south launch rail sliding out of view. | Get used to movement; no enemies. Control prompts in the side HUD. |
| 2. First Wave | 20–60 | 2,600–7,800 | 130 | light | `deep`: Earth, the Moon small in the distance. `ground`: open dock frames. `low-air`: thin cloud-deck wisps. | Shooting basics; Skitters (harmless rammers) first, then the first Needlers that shoot back. |
| 3. Yard Crossing | 60–110 | 7,800–14,300 | 130 | light | `ground`: gantries, cranes, a half-built cruiser hull, cargo containers (destructible). `low-air`: lattice beams and crane jibs passing under the player. | Ground layer as scenery and loot; the first side entry; the secret beacon. |
| 4. Pursuit | 110–160 | 14,300–20,800 | 130 | medium | `ground`: yard perimeter, defence platforms burning. `low-air`: cloud decks drifting between the yard and the play plane. `high-air`: thin spark streaks. | Mixed waves, one warned rear wave, rising density. |
| 5. Scout Leader | 160–190 | 20,800–24,700 | 130 | light | `deep`: open space past the yard; the Vrell strike group's glow on the horizon. `ground`: the last perimeter platform. | Final set piece: Needler circle plus Skitter streams. Then the level ends. |

### Launch and control prompts

The level opens with a **5-second non-playable launch**: the Stormhawk accelerates off the
Gagarin south rail while the briefing's last line plays; control starts in open space. There is
no separate tutorial: contextual control prompts appear in the side HUD (move, fire, precision
in L01; ground targets in L02; layers in L03), shown once and skippable. The special prompt
appears the first time a special is fitted (from L04, when the Airstrike unlocks).

## Waves

Enemy definitions: [Skitter](../../../enemies/air/skitter/README.md) and
[Needler](../../../enemies/air/needler/README.md). Formation names come from the
[formation vocabulary](../../../enemies/README.md#formation-vocabulary).

| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 22 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | Curls toward the centre, no fire |
| 30 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | Mirror of the previous wave |
| 40 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | Aimed shot every 2.5 s each; radio cue "they shoot back" |
| 50 | 2 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 8 | front | Sweeps down the screen |
| 64 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | Hover, fire, leave |
| 75 | 3 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | sides (left) | First side entry, slow; radio warning |
| 86 | 3 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 96 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 10 | front (alternating edges) | Teaches sweeping fire |
| 112 | 4 | snake + V-wing | [Skitter](../../../enemies/air/skitter/README.md) + [Needler](../../../enemies/air/needler/README.md) | 6 + 3 | front | First mixed wave |
| 122 | 4 | pincer | [Needler](../../../enemies/air/needler/README.md) | 4 | sides | 2 from each side, hold for 4 s at the screen edge |
| 134 | 4 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 6 | rear | Warned 3 s ahead ("Contacts on your six!"); slow |
| 146 | 4 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 7 | front | Large V |
| 162 | 5 | circle | [Needler](../../../enemies/air/needler/README.md) | 8 | front | Orbit a point above the centre for 6 s, then break toward the player one by one |
| 166 | 5 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 12 | sides (both) | Enters while the circle is orbiting |

Totals: Skitter 60 · Needler 35.

## Ground targets

| Section | Target | Effect |
|---|---|---|
| 3 | Cargo containers ×10 (ground layer, destructible, 3 HP) | Each pays 5 and drops a small salvage pickup (10) |
| 3 | Beacon on the crane at t≈90 (blinks red) | Secret, see below |

No hostile ground targets in L01. Hostile ground targets arrive in L02.

## Hazards

None. Debris in section 4 is scenery only (the `deep` layer).

## Secrets and pickups

Pickup types are defined in [player](../../../player/README.md#in-level-pickups).

- **Beacon cache** (t≈90): hitting the blinking crane beacon 3 times releases a hidden crate
  worth 80 credits. Rook, on the radio from the far side of the yard: "*Nice shooting. Finders
  keepers.*"
- **Armour patch** at t≈140: dropped by the last Needler of the t=122 pincer.
- Shield cells drop from Needlers at the normal rate. No overdrive in L01; weapon upgrades are
  bought in the hangar.

## Radio chatter

Text and radio blips only (no voice acting). Rook flies Aegis Two elsewhere in the yard and is
heard on the radio only.

| Trigger | Speaker | Line |
|---|---|---|
| t=2 | Rook | "Lancer, Rook. Aegis Two's got the north arm, you've got the south. Try not to have all the fun." |
| t=18 | Okafor | "Contacts inbound. Weapons free." |
| First Skitter destroyed | Rook | "They pop like bugs. Big, angry bugs." |
| t=40 (Needlers enter) | Varga | "Those ones are armed. Keep moving." |
| t=73 (before side wave) | Rook | "Movement on your left! They're flanking the yard." |
| t=131 (before rear wave) | Okafor | "Contacts on your six, Lancer!" |
| t=160 | The Choir (distorted) | "[the Choir sings]" |
| t=161 | Varga | "That isn't noise. There's structure in it. Commander, I think that was a signal." |
| Level end | Okafor | "Good work, Aegis. That was the scouts. The rest are coming." |
| Secondary objective met | Okafor | "Clean sweep. I'll make sure High Command hears about it." |

## Boss / mid-boss

None. The Needler circle (t=162) acts as the finale.

## Music & ambience

Track 5 *Act 1 B: Earth orbit & Luna* ("Coalition Rising", the main motif; see the
[track list](../../../audio/music/README.md#track-list)), starting at the section 2
transition, base stem only until section 4. Section 1 has ambience only: the Earth-orbit
ambience ([sfx](../../../audio/sfx/README.md#ambience-per-setting)), the launch rail and radio
blips. Mission complete jingle at the end.

## Credit budget

The total matches budget(1) from the [economy](../../../systems/economy/README.md#per-level-budget)
curve. Bounties from the stat blocks: Skitter 5, Needler 12.

| Source | Credits (medium) |
|---|---|
| Kills: Skitter 60 × 5 + Needler 35 × 12 | 720 |
| Ground targets: cargo containers 10 × (5 + small salvage 10) | 150 |
| Secret: beacon cache (hidden crate, 8% of budget) | 80 |
| Secondary objective | 50 |
| **Total** | **1,000** |

## Difficulty notes

- **Easy**: Needlers fire every 3.5 s; no rear wave (it enters from the front instead);
  two extra armour patches.
- **Hard**: Needlers fire 2-shot bursts; the rear wave has 10 Skitters; the Needler circle breaks
  toward the player in pairs, and selected circle Needlers lead the target (stat-block hook).

## Implementation

- [ ] Scroll timeline, sections, atmosphere intensity and parallax content per layer as in *Layout*.
- [ ] Wave script matches the *Waves* table (time, formation, count, entry edge).
- [ ] Destructible cargo containers and the beacon secret.
- [ ] Radio chatter cues fire at their triggers with portraits in the side HUD.
- [ ] Secondary objective tracked and rewarded.
- [ ] Credit total at medium with perfect collection is 1,000 (± 5%).
- [ ] Easy/hard variations as in *Difficulty notes*.
- [ ] Control prompts shown in section 1 (skippable).
- [ ] Aegis Two (Rook's flight) appears only as distant `far`-layer scenery in the launch; no
      wingman or escort sprite on the play plane.

## Open questions

- None open.

## Decisions

- 2026-09-30: L01 has no boss; the finale is a formation set piece.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-10-01: Launch: 5-second non-playable launch, then control; no separate tutorial, contextual prompts in L01–L03.
- 2026-10-01: Aligned with the specs written since: Rook leads Aegis Two in a separate flight and
  is heard on the radio only (he becomes the escort-slot wingman at L08); scroll speed 60 →
  130 px/s (the calm range in art direction), distances recalculated and an atmosphere intensity
  per section added; the Choir's line is "[the Choir sings]" as in its character spec for
  Acts 1–2 (the "yield" reading moves to Varga's L02 briefing); the economy's first hangar visit
  before L01 gets a teaser and the no-sensor intel view; pickups renamed to the player pickup
  types (armour patch, small salvage, hidden crate) with the same credit total; music is track 5
  "Coalition Rising"; enemy links point at the unit specs; `art: chosen` (scene and enemies are
  chosen).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
