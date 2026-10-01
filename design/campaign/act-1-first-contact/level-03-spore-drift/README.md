---
title: Level 03 – Spore Drift
design: review
implementation: not-started
art: chosen
depends-on: [../../../enemies/air/spore-bomber, ../../../enemies/air/whirl-seed, ../../../enemies/space/leviathan]
updated: 2026-10-01
---

# Level 03 – Spore Drift

## Summary

Slow Vrell gas-bags drift through the high orbital lanes over the wreckage of the first battle,
seeding spores toward Earth. Lancer clears the lane through a debris field, and something
enormous crosses overhead: the Leviathan, first as an untouchable spectacle on `high-air`, then
low enough to fight. The level introduces the Spore Bomber and its mines, the `low-air` and
`high-air` layers, the debris field hazard, the first `huge` enemy and the first spinners
(Whirl Seeds). The first level where a spread weapon clearly pays off. About 3 minutes.

## Briefing

> **Commander Okafor:** "Lancer, the yards are holding. Now the Vrell have changed tactics. Slow
> carriers are drifting through the high lanes over last week's battle site, dropping something
> as they go. Varga thinks they're seeding the planet. Clear the lane. Every carrier that gets
> past you is headed for an atmosphere full of people."
>
> **Dr. Varga:** "They're spore bombers, gas-bags flying below your level. The spores they drop
> float up to you a second later, so a wide spread of fire is your friend. There's also an echo
> on long range I can't explain. Very big, very slow. If it comes your way, don't try to
> out-shoot it. Out-fly it."

*Hangar teaser* (shop screen after L02):
> **Dr. Varga:** "Slow targets, and lots of little things drifting at you. Bring something wide."

## Threat profile

| Field | Value |
|---|---|
| Dominant layers | `air`, `low-air` (Spore Bombers), `high-air` (the Leviathan's first pass) |
| Attack directions | front 100% |
| Density | 2 |
| Recommended traits | `spread` (also `forward`) |
| Hazards | Debris field (blocks shots, contact damage); drifting spore mines |
| Boss / mid-boss | none; set piece: the Leviathan (`huge`) |
| Sensor-suite detail | none: setting, `air` + `low-air`, front · L1: + front 100%, density 2, hazards "debris, mines" · L2: + Skitter, Needler, Stinger, Spore Bomber portraits; "unknown huge contact" silhouette · L3: + `spread` highlighted, wave strip, 1 secret |

## Objective

- **Primary** `reach-end`: survive to the end of the scroll.
- **Secondary** *Nothing gets through*: destroy all 10 Spore Bombers before they leave the
  screen. +50. A bomber that leaves the bottom edge only costs the bonus.

## Layout

Scene: the chosen [Earth orbit](../../../world/earth-orbit/README.md) scene
([sheet](../../../art-direction/concept/parallax-r03-a.png)) with the debris-field sub-location.
Scroll speed 140 px/s, slowing to 90 px/s while the Leviathan fights on the play plane.
Total ≈ 185 s ≈ 24,250 px. Motion budget: the drifting debris and the low-air spore haze are
the two strong background movers; the haze stays thin while the Leviathan is on screen.

| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Parallax content per layer | Purpose |
|---|---|---|---|---|---|---|
| 1. High Lane | 0–30 | 0–4,200 | 140 | light | `deep`: Earth's full disc below, cloud fronts. `far`: navigation beacons along the lane. `ground`: a few lane-marker buoys. `low-air`: thin cloud-deck wisps. | First Spore Bomber alone; HUD prompt for layers. |
| 2. Debris Field | 30–60 | 4,200–8,400 | 140 | medium | `ground`: the broken frigate *Kestrel* and halves of CDF platforms. `air`: drifting debris chunks (hazard). `low-air`: olive-grey spore haze. `high-air`: ice streaks. | Shots blocked by debris; Stingers; the lifeboat secret. |
| 3. Whale Song | 60–80 | 8,400–11,200 | 140 | clear | `deep`: Earth. `high-air`: the Leviathan's first pass, drawn large above the player. `ground`: sparse wreckage. | Survive the seed clusters; learn that `high-air` cannot be hit yet. |
| 4. Spore Bloom | 80–125 | 11,200–17,500 | 140 | medium, heavy peak 100–108 | `ground`: thinning wreckage, a burnt-out tug. `air`: debris returns. `low-air`: dense spore banks (the heavy peak). `high-air`: spore streaks. | Bomber lines; spread fire pays off. |
| 5. Second Pass | 125–158 | 17,500–20,470 | 90 | light | `deep`: Earth. `air`: the Leviathan descends and drifts across the upper half. `ground`: none (open lane). | The set-piece fight. |
| 6. Clear Lane | 158–185 | 20,470–24,250 | 140 | light | `far`: the orbital defence ring ahead. `ground`: the first ring platforms. | Mop-up and Okafor's Hammer flight news. |

### Contextual prompts

- t=12 (first Spore Bomber): "LOW-AIR: below your flight level. No collision; every weapon hits it."
- t=62 (Leviathan): "HIGH-AIR: above you. Only homing and beam weapons reach it."

## Waves

Enemy specs: [Spore Bomber](../../../enemies/air/spore-bomber/README.md),
[Whirl Seed](../../../enemies/air/whirl-seed/README.md),
[Leviathan](../../../enemies/space/leviathan/README.md),
[Skitter](../../../enemies/air/skitter/README.md),
[Needler](../../../enemies/air/needler/README.md),
[Stinger](../../../enemies/air/stinger/README.md).

| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 12 | 1 | single | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 1 | front | **Introduction**: `straight` down, nothing else on screen |
| 22 | 1 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | |
| 34 | 2 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | Dives between debris chunks |
| 42 | 2 | line abreast | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 3 | front | |
| 50 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | Partly behind debris |
| 56 | 2 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | |
| 62–74 | 3 | solo set piece | [Leviathan](../../../enemies/space/leviathan/README.md) (first pass) | 1 | front (top-left, diagonal) | `high-air`; whirl clusters of 6 [Whirl Seeds](../../../enemies/air/whirl-seed/README.md) at t=63, 66, 69, 72 (24 seeds) |
| 84 | 4 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 8 | front (right) | |
| 92 | 4 | line abreast | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 3 | front | Inside the spore banks |
| 102 | 4 | convoy | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 3 | front | `strafe` at y≈200 after entering; in the heavy peak |
| 128–158 | 5 | solo set piece | [Leviathan](../../../enemies/space/leviathan/README.md) (second pass) | 1 | front (descends at the top) | Descends to `air` over 2 s, drifts for up to 30 s, then leaves |
| 165 | 6 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 8 | front (alternating edges) | |
| 175 | 6 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 4 | front | |

Totals: Spore Bomber 10 · Whirl Seed 24 · Skitter 22 · Needler 9 · Stinger 6 · Leviathan 1.

## Ground targets

No hostile ground targets in L03 (the lane is open space). The wreck of the *Kestrel* holds the
secret below.

## Hazards

- **Debris field** (sections 2 and 4): drifting wreck chunks on the `air` layer, as defined in
  [Earth orbit](../../../world/earth-orbit/README.md#hazards--set-pieces). Large chunks (48–96 px)
  are indestructible, block shots from both sides and deal contact damage 15; small chunks
  (24–32 px) also block shots but break after 6 damage and pay nothing. At most 4 large chunks on
  screen; they drift at 20–40 px/s and never spawn within 120 px of the player.
- **Spore mines**: dropped by Spore Bombers per their spec (rise to the player plane after 1 s,
  burst on contact or after 8 s). Section 4's heavy spore haze never hides them: they are
  drawn above the haze like bullets.

## Secrets and pickups

Pickup types are defined in [player](../../../player/README.md#in-level-pickups).

- **Lifeboat rack** (hidden crate, 75): on the *Kestrel* wreck (ground layer, t≈45) four amber
  lights blink around an escape-pod rack. Shooting all four releases a salvage canister.
- **Large salvage** (200): the Leviathan's death drop, per its spec. Only paid if it dies before
  leaving at the end of its second pass.
- **Overdrive**: dropped by the second Spore Bomber of the t=102 convoy (the first overdrive of
  the campaign).
- **Armour patch**: dropped by the last Needler of the t=50 V-wing.
- Shield cells at the normal rate. Spores pay 1 credit each (see *Credit budget*).

## Radio chatter

Text and radio blips only. Rook flies Aegis Two in the wreck field to the north (radio only).

| Trigger | Speaker | Line |
|---|---|---|
| t=2 | Okafor | "Aegis, the high lanes are filling up with something. Find out what, and stop it." |
| t=12 (first bomber) | Varga | "That gas-bag is full of spores. They float up to your level after a second. Shoot them once they glow." |
| t=24 | Rook | "Aegis Two in the wreck field north of you. Watch the big chunks, Lancer. They don't care whose side you're on." |
| t=30 | Generic CDF (Ring Control) | "Debris field ahead. Big pieces will stop your rounds. And theirs." |
| First bomber leaves the screen | Okafor | "One got past. That's spores on somebody's city, Aegis." |
| t=58 | Varga | "Commander, something very large on long range. It's not on any chart. And it's… singing?" |
| t=61 | The Choir (distorted) | "[the Choir sings]" |
| t=63 | Rook | "Okay. Okay. That's big. We can do big." |
| t=65 | Varga | "It's above you. Your guns can't reach that high. Dodge the seeds and wait." |
| t=126 | Varga | "It's coming down to your level! The vents on its back, the fins, the blowhole. Hit the glow." |
| Leviathan destroyed | Generic CDF (Ring Control) | "Ring Control confirms: the big contact is down." |
| Leviathan leaves alive | Varga | "It's leaving. I have a feeling we'll see that one again." |
| t=160 | Okafor | "Good flying. And Lancer, High Command just gave us Hammer flight: two bombers on call. You'll find them in the hangar." |
| Level end | Okafor | "Lane's clear. Come home, Aegis." |
| Secondary met | Okafor | "Not one bomber got through. Earth owes you a drink, Lancer." |

## Boss / mid-boss

No boss. The [Leviathan](../../../enemies/space/leviathan/README.md) is a `huge` set piece
(at most one per level, announced by radio):

- **First pass** (t=62–74): crosses diagonally from top-left to bottom-right on `high-air`,
  releasing four whirl clusters. At L03 the player cannot own a `homing` or `beam` weapon yet
  (the Micro-missile Pod arrives at L06), so the pass is survived, not fought.
- **Second pass** (t=128–158): descends to `air` near the top centre and drifts across the upper
  half. The scroll slows to 90 px/s; no other waves enter while it is on screen. At the
  reference DPS for L03 (32) its ≈ 550 HP take about 17–25 s, inside the 30-s window.
- Killing the blowhole first destroys the rest for the full 190 (spec rule).

## Music & ambience

Track 5 *Act 1 B: Earth orbit & Luna* ("Coalition Rising", see the
[track list](../../../audio/music/README.md#track-list)). During the first pass the music drops
to the base stem and the Leviathan's whale-song call carries over it; the intensity stem is
forced on for the second pass. Ambience: Earth orbit
([sfx](../../../audio/sfx/README.md#ambience-per-setting)) plus debris impacts.

## Credit budget

Budget(3) = 1,000 × 1.07² ≈ **1,145** ([economy](../../../systems/economy/README.md#per-level-budget)).
Bounties from the stat blocks: Spore Bomber 25, Whirl Seed 3, Skitter 5, Needler 12, Stinger 15,
Leviathan 190 (part bounties, ≈ 17% of the budget, close to the 15% set-piece share).

| Source | Credits (medium) |
|---|---|
| Kills: Spore Bomber 10 × 25 + Whirl Seed 24 × 3 + Skitter 22 × 5 + Needler 9 × 12 + Stinger 6 × 15 | 630 |
| Set piece: Leviathan parts | 190 |
| Pickups: Leviathan large salvage 200 + lifeboat rack 75 (6.5%) | 275 |
| Secondary: no bomber gets through | 50 |
| **Total** | **1,145** |

Spore mines pay 1 each but their number depends on how long each bomber lives (up to ~7 per
bomber), so they are not part of the total; a typical run adds 20–30 credits, inside the ± 5%
tolerance.

## Difficulty notes

- **Easy**: spores never burst on their own (stat-block hook); half the large debris chunks; the
  Leviathan's second pass holds for 40 s before it leaves.
- **Hard**: whirl clusters of 8 and 2-orb vent bursts (stat-block hooks); spores burst into
  8-bullet rings; debris drifts at up to 60 px/s; the Leviathan leaves after 24 s.

## Implementation

- [ ] Scroll timeline, sections, atmosphere intensity and parallax content as in *Layout*,
      including the 90 px/s slowdown for the second pass.
- [ ] Wave script matches the *Waves* table; no other waves while the Leviathan is on the play
      plane.
- [ ] Debris chunks: indestructible large / breakable small, both block shots; spawn rules.
- [ ] Leviathan first pass on `high-air` (seed clusters), second pass on `air` with part
      bounties, leaves after 30 s if alive.
- [ ] Layer prompts at t=12 and t=62 (once, skippable).
- [ ] Secondary objective tracks bombers leaving the bottom edge.
- [ ] Lifeboat rack secret and the scripted overdrive and armour patch drops.
- [ ] Radio cues fire at their triggers.
- [ ] Credit total at medium with perfect collection is 1,145 (± 5%, spores excluded).
- [ ] Easy/hard variations as in *Difficulty notes*.

## Open questions

- None open.

## Decisions

- 2026-10-01: Promoted from the act roster to a draft level document. The Leviathan's first
  pass is deliberately unfightable (no `homing` before L06); the fight is its second pass, with
  the scroll slowed and no other waves. Secondary objective chosen to frame the spores as a
  threat to Earth (a hook for the Act 2 landings). Hammer flight is announced here, so the
  Airstrike is in the shop before L04.
