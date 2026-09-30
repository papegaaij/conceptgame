---
title: Level 01 – Break at Dawn
design: draft
implementation: not-started
art: none
updated: 2026-09-30
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
> hulls are sitting in those docks. You're the first bird out. Rook's on your wing. Keep them
> off the yards, keep your head, and bring me back something we can study.
> Aegis Actual out."
>
> **Dr. Varga:** "Their ships don't show up as metal on our scopes. Whatever they're made of, it
> burns. Aim for the glowing parts."

*Hangar teaser* (not shown: L01 has no preceding hangar visit; the game starts in the briefing).

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `air` (all enemies), `deep` (Earth, the yards' far structures) |
| Attack directions | front 85% · sides 10% · rear 5% (one warned wave) |
| Density | 1 |
| Recommended traits | `forward` |
| Hazards | none |
| Boss / mid-boss | none (final wave: Needler circle) |
| Sensor-suite detail | n/a (the sensor suite isn't available yet) |

## Objective

- **Primary** `reach-end`: survive to the end of the scroll.
- **Secondary**: destroy at least 80% of all enemies. Pays +50 credits and triggers a line from
  Okafor.

## Layout

Baseline scroll speed 60 px/s at the 960×540 baseline (play field 480×540, see
[art direction](../../../art-direction/README.md)). Total ≈ 190 s ≈ 11,400 px.

| Section | t (s) | Scroll (px) | Layers and content | Purpose |
|---|---|---|---|---|
| 1. Launch | 0–20 | 0–1,200 | `deep`: Earth's curve with the sunrise terminator, starfield. `ground`: the Gagarin shipyards' launch rail sliding out of view. `air`: Rook's ship alongside. | Get used to movement; no enemies. Control hints in the side HUD. |
| 2. First Wave | 20–60 | 1,200–3,600 | `deep`: Earth, the Moon small in the distance. `ground`: open dock frames. | Shooting basics; Skitters (harmless rammers) first, then the first Needlers that shoot back. |
| 3. Yard Crossing | 60–110 | 3,600–6,600 | `ground`: gantries, cranes, a half-built cruiser hull, cargo containers (destructible, drop credits). | Ground layer as scenery and loot; the first side entry; the secret beacon. |
| 4. Pursuit | 110–160 | 6,600–9,600 | `ground`: yard perimeter, defence platforms burning. `deep`: debris clouds drifting. | Mixed waves, one warned rear wave, rising density. |
| 5. Scout Leader | 160–190 | 9,600–11,400 | Open space past the yard; the Vrell strike group's glow on the horizon. | Final set piece: Needler circle plus Skitter streams. Then the level ends. |

## Waves

Enemy definitions live in [air enemies](../../../enemies/air/README.md) (Skitter, Needler).
Formation names come from the [formation vocabulary](../../../enemies/README.md).

| t (s) | Section | Formation | Enemies | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 22 | 2 | snake | Skitter | 6 | front (left) | Curls toward the centre, no fire |
| 30 | 2 | snake | Skitter | 6 | front (right) | Mirror of the previous wave |
| 40 | 2 | V-wing | Needler | 5 | front | Aimed shot every 2.5 s each; radio cue "they shoot back" |
| 50 | 2 | line abreast | Skitter | 8 | front | Sweeps down the screen |
| 64 | 3 | line abreast | Needler | 3 | front | Hover, fire, leave |
| 75 | 3 | snake | Skitter | 6 | sides (left) | First side entry, slow; radio warning |
| 86 | 3 | V-wing | Needler | 5 | front | |
| 96 | 3 | stream | Skitter | 10 | front (alternating edges) | Teaches sweeping fire |
| 112 | 4 | snake + V-wing | Skitter + Needler | 6 + 3 | front | First mixed wave |
| 122 | 4 | pincer | Needler | 4 | sides | 2 from each side, hold for 4 s at the screen edge |
| 134 | 4 | line abreast | Skitter | 6 | rear | Warned 3 s ahead ("Contacts on your six!"); slow |
| 146 | 4 | V-wing | Needler | 7 | front | Large V |
| 162 | 5 | circle | Needler | 8 | front | Orbit a point above the centre for 6 s, then break toward the player one by one |
| 166 | 5 | stream | Skitter | 12 | sides (both) | Enters while the circle is orbiting |

Totals: Skitter 60 · Needler 35.

## Ground targets

| Section | Target | Effect |
|---|---|---|
| 3 | Cargo containers ×10 (ground layer, destructible, 3 HP) | Each drops a 15-credit pickup |
| 3 | Beacon on the crane at t≈90 (blinks red) | Secret, see below |

No hostile ground targets in L01. Hostile ground targets arrive in L02.

## Hazards

None. Debris in section 4 is scenery only (the `deep` layer).

## Secrets and pickups

- **Beacon cache** (t≈90): hitting the blinking crane beacon 3 times releases a credit capsule
  worth 80 credits. Rook: "*Nice shooting. Finders keepers.*"
- **Repair pickup** at t≈140: dropped by the last Needler of the t=122 pincer. Restores armour.
- A **weapon power-up** is not in L01; weapon upgrades are bought in the hangar.

## Radio chatter

| Trigger | Speaker | Line |
|---|---|---|
| t=2 | Rook | "Lancer, Rook. Looks like it's you and me first. Try to keep up." |
| t=18 | Okafor | "Contacts inbound. Weapons free." |
| First Skitter destroyed | Rook | "They pop like bugs. Big, angry bugs." |
| t=40 (Needlers enter) | Varga | "Those ones are armed. Keep moving." |
| t=73 (before side wave) | Rook | "Movement on the left! They're flanking the yard." |
| t=131 (before rear wave) | Okafor | "Contacts on your six, Lancer!" |
| t=160 | Choir (distorted) | "*...yield...*" |
| t=161 | Varga | "Did... did they just talk to us?" |
| Level end | Okafor | "Good work, Aegis. That was the scouts. The rest are coming." |
| Secondary objective met | Okafor | "Clean sweep. I'll make sure High Command hears about it." |

## Boss / mid-boss

None. The Needler circle (t=162) acts as the finale.

## Music & ambience

The Act 1 theme "First Contact" (see [audio](../../../audio/README.md)), starting at the section 2
transition. Section 1 has ambience only: the launch rail, engine hum and radio static.

## Credit budget

The total matches budget(1) from the [economy](../../../systems/economy/README.md#per-level-budget)
curve. Enemy credit values are proposals, to be confirmed in the enemy stat blocks:
Skitter 5, Needler 12.

| Source | Credits (medium) |
|---|---|
| Kills: Skitter 60 × 5 + Needler 35 × 12 | 720 |
| Ground targets: cargo containers 10 × 15 | 150 |
| Secret: beacon cache | 80 |
| Secondary objective | 50 |
| **Total** | **1,000** |

## Difficulty notes

- **Easy**: Needlers fire every 3.5 s; no rear wave (it enters from the front instead);
  two extra repair pickups.
- **Hard**: Needlers fire 2-shot bursts; the rear wave has 10 Skitters; the Needler circle breaks
  toward the player in pairs.

## Implementation

- [ ] Scroll timeline, sections and parallax content per layer as in *Layout*.
- [ ] Wave script matches the *Waves* table (time, formation, count, entry edge).
- [ ] Destructible cargo containers and the beacon secret.
- [ ] Radio chatter cues fire at their triggers with portraits in the side HUD.
- [ ] Secondary objective tracked and rewarded.
- [ ] Credit total at medium with perfect collection is 1,000 (± 5%).
- [ ] Easy/hard variations as in *Difficulty notes*.
- [ ] Control hints shown in section 1 (skippable).

## Open questions

- Should the first launch be playable (the ship accelerating off the rail), or start with the
  ship already in open space?

## Decisions

- 2026-09-30: L01 has no boss; the finale is a formation set piece.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
