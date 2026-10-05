---
title: Level 03 – Spore Drift
design: approved
implementation: done
art: final
depends-on: [../../../enemies/air/spore-bomber, ../../../enemies/air/whirl-seed, ../../../enemies/space/leviathan]
updated: 2026-10-05
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

<!-- data: briefing -->
> **Commander Okafor:** "Lancer, the yards are holding. Now the Vrell have changed tactics. Slow
> carriers are drifting through the high lanes over last week's battle site, dropping something
> as they go. Varga thinks they're seeding the planet. Clear the lane. Every carrier that gets
> past you is headed for an atmosphere full of people."
>
> **Dr. Varga:** "They're spore bombers, gas-bags flying below your level. The spores they drop
> float up to you a second later, so a wide spread of fire is your friend. There's also an echo
> on long range I can't explain. Very big, very slow. If it comes your way, don't try to
> out-shoot it. Out-fly it."
<!-- /data -->

*Images* (production art, [briefing images](../../../ui/briefing/README.md)): Okafor's page
`level-03-spore-lanes` (the high lanes over last week's battle site and its debris field, the
spore carriers seeding Earth, Lancer's lane), Varga's `level-03-spore-echo` (the Spore Bomber
below, its spores rising into a wide spread, the long-range echo as a noisy silhouette); the
level's data names them as each page's `image`.

*Hangar teaser* (shop screen after L02):
<!-- data: teaser -->
> **Dr. Varga:** "Slow targets, and lots of little things drifting at you. Bring something
> wide."
<!-- /data -->

## Threat profile

<!-- data: threat-profile -->
| Field | Value |
|---|---|
| Dominant layers | `air`, `low-air` (Spore Bombers), `high-air` (the Leviathan's first pass) |
| Attack directions | front 100% |
| Density | 2 |
| Recommended traits | `spread` (also `forward`) |
| Hazards | Debris field (blocks shots, contact damage); drifting spore mines |
| Boss / mid-boss | none; set piece: the Leviathan (`huge`) |
| Sensor-suite detail | none: setting, `air` + `low-air`, front · L1: + front 100%, density 2, hazards "debris, mines" · L2: + Skitter, Needler, Stinger, Spore Bomber portraits; "unknown huge contact" silhouette · L3: + `spread` highlighted, wave strip, 1 secret |
<!-- /data -->

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

<!-- data: level-sections -->
| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. High Lane | 0–30 | 0–4,200 | 140 | light | `deep`: Earth's full disc below, cloud fronts. `far`: navigation beacons along the lane. `ground`: a few lane-marker buoys. `low-air`: thin cloud-deck wisps. | First Spore Bomber alone; HUD prompt for layers. |
| 2. Debris Field | 30–60 | 4,200–8,400 | 140 | medium | `ground`: the broken frigate *Kestrel* and halves of CDF platforms. `air`: drifting debris chunks (hazard). `low-air`: olive-grey spore haze. `high-air`: ice streaks. | Shots blocked by debris; Stingers; the lifeboat secret. |
| 3. Whale Song | 60–80 | 8,400–11,200 | 140 | clear | `deep`: Earth. `high-air`: the Leviathan's first pass, drawn large above the player. `ground`: sparse wreckage. | Survive the seed clusters; learn that `high-air` cannot be hit yet. |
| 4. Spore Bloom | 80–125 | 11,200–17,500 | 140 | medium, heavy peak 100–108 | `ground`: thinning wreckage, a burnt-out tug. `air`: debris returns. `low-air`: dense spore banks (the heavy peak). `high-air`: spore streaks. | Bomber lines; spread fire pays off. |
| 5. Second Pass | 125–158 | 17,500–20,470 | 90 | light | `deep`: Earth. `air`: the Leviathan descends and drifts across the upper half. `ground`: none (open lane). | The set-piece fight. |
| 6. Clear Lane | 158–185 | 20,470–24,250 | 140 | light | `far`: the orbital defence ring ahead. `ground`: the first ring platforms. | Mop-up and Okafor's Hammer flight news. |
<!-- /data -->

### Contextual prompts

- t=12 (first Spore Bomber): `LOW-AIR` · `BELOW YOU: FIRE` (leaves once a Spore Bomber is destroyed)
- t=62 (Leviathan): `HIGH-AIR` · `HOMING ONLY` (leaves after its time)

Varga's radio lines say the rest: the first at the same moment, the second (t=71) once the
Choir and Rook have reacted to the crossing.

## Waves

Enemy specs: [Spore Bomber](../../../enemies/air/spore-bomber/README.md),
[Whirl Seed](../../../enemies/air/whirl-seed/README.md),
[Leviathan](../../../enemies/space/leviathan/README.md),
[Skitter](../../../enemies/air/skitter/README.md),
[Needler](../../../enemies/air/needler/README.md),
[Stinger](../../../enemies/air/stinger/README.md).

<!-- data: waves -->
| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 5.5 | 1 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | Gone before the first Spore Bomber |
| 12 | 1 | single | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 1 | front | **Introduction**: `straight` down, nothing else on screen |
| 22 | 1 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 10 | front (left) | |
| 34 | 2 | V-wing | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | Dives between debris chunks |
| 42 | 2 | line abreast | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 3 | front | |
| 50 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | Partly behind debris |
| 56 | 2 | column | [Stinger](../../../enemies/air/stinger/README.md) | 3 | front | |
| 62–74 | 3 | solo set piece | [Leviathan](../../../enemies/space/leviathan/README.md) (first pass) | 1 | front (top-left, diagonal) | `high-air`; its blowhole releases the four whirl clusters below (24 seeds) |
| 63 | 3 | whirl cluster | [Whirl Seed](../../../enemies/air/whirl-seed/README.md) | 6 | front | From the Leviathan's blowhole on `high-air`; 8 on hard |
| 66 | 3 | whirl cluster | [Whirl Seed](../../../enemies/air/whirl-seed/README.md) | 6 | front | |
| 69 | 3 | whirl cluster | [Whirl Seed](../../../enemies/air/whirl-seed/README.md) | 6 | front | |
| 72 | 3 | whirl cluster | [Whirl Seed](../../../enemies/air/whirl-seed/README.md) | 6 | front | |
| 84 | 4 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 8 | front (right) | |
| 92 | 4 | line abreast | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 3 | front | Inside the spore banks |
| 102 | 4 | convoy | [Spore Bomber](../../../enemies/air/spore-bomber/README.md) | 3 | front | `strafe` at y≈200 after entering; in the heavy peak |
| 112 | 4 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | Out of the heavy peak |
| 128–158 | 5 | solo set piece | [Leviathan](../../../enemies/space/leviathan/README.md) (second pass) | 1 | front (descends at the top) | Descends to `air` over 2 s, drifts for up to 30 s, then leaves |
| 159 | 6 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | Once the Leviathan has gone |
| 165 | 6 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 12 | front (alternating edges) | |
| 175 | 6 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 4 | front | |
| 180 | 6 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (alternating edges) | The last of them as the lane clears |

Totals: Skitter 54 · Spore Bomber 10 · Stinger 6 · Needler 9 · Whirl Seed 24 · Leviathan 1.
<!-- /data -->

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

- **Lifeboat rack** (hidden crate, 75): on the *Kestrel* wreck (ground layer, t≈44) four amber
  lights blink around an escape-pod rack. Shooting all four releases a salvage canister; Rook:
  "The *Kestrel*'s lifeboat stores. Her crew would want you to have them, Lancer."
- **Large salvage** (200): the Leviathan's death drop, per its spec. Only paid if it dies before
  leaving at the end of its second pass.
- **Overdrive**: dropped by the second Spore Bomber of the t=102 convoy (the campaign's second;
  Level 02 drops the first).
- **Armour patch**: dropped by the last Needler of the t=50 V-wing.
- Shield cells at the normal rate. Spores pay 1 credit each (see *Credit budget*).

## Radio chatter

Text and radio blips only. Rook flies Aegis Two in the wreck field to the north (radio only).

<!-- data: radio -->
| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Okafor | "Aegis, the high lanes are filling up with something. Find out what, and stop it." |
| t=12 (first bomber) | Varga | "That gas-bag is full of spores. They float up to your level after a second. Shoot them once they glow." |
| t=24 | Rook | "Aegis Two in the wreck field north of you. Watch the big chunks, Lancer. They don't care whose side you're on." |
| t=36.5 | Generic CDF (Ring Control) | "Debris field ahead. Big pieces will stop your rounds. And theirs." |
| First bomber leaves the screen | Okafor | "One got past. That's spores on somebody's city, Aegis." |
| t=47 | Varga | "Commander, something very large on long range. It's not on any chart. And it's… singing?" |
| t=58.5 | The Choir (distorted) | "[the Choir sings]" |
| t=64.5 | Rook | "Okay. Okay. That's big. We can do big." |
| t=71 | Varga | "It's above you. Your guns can't reach that high. Dodge the seeds and wait." |
| t=126 | Varga | "It's coming down to your level! The vents on its back, the fins, the blowhole. Hit the glow." |
| Leviathan destroyed | Generic CDF (Ring Control) | "Ring Control confirms: the big contact is down." |
| Leviathan leaves alive | Varga | "It's leaving. I have a feeling we'll see that one again." |
| t=168.5 | Okafor | "Good flying. And Lancer, High Command just gave us Hammer flight: two bombers on call. You'll find them in the hangar." |
| Level end | Okafor | "Lane's clear. Come home, Aegis." |
| Secondary met | Okafor | "Not one bomber got through. Earth owes you a drink, Lancer." |
<!-- /data -->

## Boss / mid-boss

No boss. The [Leviathan](../../../enemies/space/leviathan/README.md) is a `huge` set piece
(at most one per level, announced by radio):

- **First pass** (t=62–74): crosses diagonally from top-left to bottom-right on `high-air`,
  releasing four whirl clusters; drawn above the ship, large and at 75 % opacity. At L03 the player cannot own a `homing` or `beam` weapon yet
  (the Micro-missile Pod arrives at L06), so the pass is survived, not fought.
- **Second pass** (t=128–158): descends to `air` near the top centre and drifts across the upper
  half. The scroll slows to 90 px/s; no other waves enter while it is on screen. At the
  reference DPS for L03 (32) its 510 HP take about 16–24 s, inside the 30-s window.
- Killing the blowhole first destroys the rest for the full 113 (spec rule).

## Music & ambience

Track 5 *Act 1 B: Earth orbit & Luna* ("Coalition Rising", see the
[track list](../../../audio/music/README.md#track-list)). During the first pass the music drops
to the base stem and the Leviathan's whale-song call carries over it; the intensity stem is
forced on for the second pass. Ambience: Earth orbit
([sfx](../../../audio/sfx/README.md#ambience-per-setting)) plus debris impacts.

## Credit budget

Budget(3) = 700 × 1.07² ≈ **801**, the typical haul's target ([economy](../../../systems/economy/README.md#per-level-budget)); the level's `bounty_scale` of 1.07 puts the typical haul on it (`TypicalHaulTest`).
Bounties from the stat blocks: Spore Bomber 25, Whirl Seed 3, Skitter 5, Needler 12, Stinger 15,
Leviathan 113 (part bounties; 121 at the scale, ≈ 15 % of the budget, the set-piece share).

<!-- data: credit-budget -->
| Source | Perfect run | Typical haul |
|---|---|---|
| Kills: Skitter 54 × 5 + Spore Bomber 10 × 25 + Stinger 6 × 15 + Needler 9 × 12 + Whirl Seed 24 × 3 | 825 | 495 |
| Set piece: Leviathan parts | 122 | 98 |
| Pickup: Leviathan large salvage | 200 | 128 |
| Secret: lifeboat rack (hidden crate, 9% of budget) | 75 | 38 |
| Secondary: no Spore Bomber gets through | 50 | 25 |
| **Total** (bounty scale 1.07) | **1,272** | **783** |
| Budget(n) = the typical haul's target; typical -2 %, perfect 1.59 × budget | | 801 |
<!-- /data -->

Spore mines pay 1 each but their number depends on how long each bomber lives (up to ~7 per
bomber), so they are not part of the total; a typical run adds 20–30 credits, inside the ± 5%
tolerance.

## Difficulty notes

- **Easy**: spores never burst on their own (stat-block hook); half the large debris chunks; the
  Leviathan's second pass holds for 30 s, as on medium (40 s would run past its section and over
  the t=165 wave).
- **Hard**: whirl clusters of 8 and 2-orb vent bursts (stat-block hooks); spores burst into
  8-bullet rings; debris drifts at up to 60 px/s; the Leviathan leaves after 24 s.

## Concept art

Production art for concept round 16 (M4 part C, the Level 03 batch), review files built from the final files in `assets/` by `tools/art/backdrop_l03.py` (`--review` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/backdrop-final-r16-a.png](concept/backdrop-final-r16-a.png) | Final backdrop pieces: every tile set and set piece of the level's `backdrop` data (the broken *Kestrel* with its lifeboat rack and the additive rack light, platform halves, wrecks, the burnt-out tug, beacons, buoys, the defence ring, spore haze and banks, ice and spore streaks, weather fronts; Level 01's Earth, decks, wisps and kit for the rest) | chosen |
| [concept/level-03-capture-final-r16-a.png](concept/level-03-capture-final-r16-a.png) | Game captures of the level with `--level 3` (whole window, HUD included): the first Spore Bomber and its rising spores with the LOW-AIR prompt, the debris field and the *Kestrel*'s lifeboat lights, the Leviathan's translucent first pass with whirl seeds, the Spore Bloom peak, the second pass with wrecked vents and fins, the clear lane with the defence ring | chosen |
| [concept/level-03-rendering-capture-r16-a.gif](concept/level-03-rendering-capture-r16-a.gif) | Game capture (play field at 1×, its lower 480×360, 15 fps, 5.5 s, t≈100–105.5, the ship not firing): the 92 s Spore Bomber line veiled by the Spore Bloom's heavy low-air banks, their spore mines growing and brightening as they rise above the haze | chosen |
| [concept/level-03-rendering-capture-r16-b.gif](concept/level-03-rendering-capture-r16-b.gif) | Game capture (play field at 1×, 15 fps, 6.3 s, t≈124.7–131): the Leviathan's second pass coming down from `high-air` at 1.25× over the ship, switching below it at 1× on the play plane, with the 0-1-2-1 tail sway and the blowhole glow pulsing | chosen |

## Implementation

- [x] Scroll timeline and sections as in *Layout*, including the 90 px/s slowdown for the
      second pass.
- [x] Atmosphere intensity and parallax content as in *Layout* (the backdrop block and its
      rendering).
- [x] Wave script matches the *Waves* table; no other waves while the Leviathan is on the play
      plane (the loader checks it on every difficulty).
- [x] Debris chunks: indestructible large / breakable small, both block shots; spawn rules
      (simulation; at most 4 large on screen, checked by the loader).
- [x] Debris chunks, spore mines, Whirl Seeds and the Leviathan drawn (its parts wrecked, its
      descent and rise).
- [x] Leviathan first pass on `high-air` (seed clusters), second pass on `air` with part
      bounties, leaves after 30 s if alive.
- [x] Layer prompts at t=12 and t=62 in the data (once, skippable).
- [x] The `LOW-AIR` prompt leaves once a `low-air` enemy is destroyed (its `skip`, in the HUD).
- [x] Secondary objective tracks bombers leaving the screen (simulation).
- [x] HUD tracker for the "nothing gets through" objective.
- [x] Lifeboat rack secret and the scripted overdrive and armour patch drops.
- [x] Radio cues fire at their triggers.
- [x] The timed radio lines start at most 1 s late behind the queue at the default text speed
      (`RadioTimelineTest`).
- [x] Hangar intel before the level: from sensor L2 the Leviathan shows as an "unknown huge
      contact" with its silhouette, not by name (the level's set pieces, `Intel.contacts()`).
- [x] Typical haul at medium within ±5 % of budget(3) = 801 with `bounty_scale` 1.07 (`TypicalHaulTest`); a perfect run earns 1,272.
- [x] At least 32 enemies per minute of scroll at medium (34.3; `DensityTest`).
- [x] Easy/hard variations as in *Difficulty notes*.

## Open questions

- None open.

## Decisions

- 2026-10-01: Promoted from the act roster to a draft level document. The Leviathan's first
  pass is deliberately unfightable (no `homing` before L06); the fight is its second pass, with
  the scroll slowed and no other waves. Secondary objective chosen to frame the spores as a
  threat to Earth (a hook for the Act 2 landings). Hammer flight is announced here, so the
  Airstrike is in the shop before L04.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-02: M4 part B (user decision): Level 02 drops the campaign's first overdrive; this level's is the second.
- 2026-10-02: M4 part C (user decisions): the layer prompts in the one-line prompt format, `LOW-AIR` · `BELOW YOU: FIRE` and `HIGH-AIR` · `HOMING ONLY`; the Leviathan's 510 HP; its easy hold 30 s like medium; the debris chunks are placed in the data (time, x, size, drift), the easy level leaving out every second large chunk and the hard one drifting them up to 60 px/s; "nothing gets through" fails as soon as a Spore Bomber leaves the screen; the lifeboat rack's four lights are one-hit triggers that together reveal the secret.
- 2026-10-02: M4 part C, the simulation (choices for review): the Leviathan's first pass crosses
  on a fixed heading of 58° from the top left (centre from (-265, -150) at t=55 to (846, 544) at
  t=80, y below the top edge), its blowhole over the four cluster release points; the second pass
  comes down the centre on `high-air` (t=124–128), descends over 2 s, drifts 30 px/s between
  x=150 and 330 at about y=195 and rises out through the top at 160 px/s at t=158 (152 on hard);
  shots over the armoured body fly on towards a living part ahead of them and glance off where
  none is; the vents' first orbs come 0.6, 1.2, 1.8 and 2.4 s after it reaches the play plane
  and the fins' first fans 4 s after it; the debris drifts at most 40 px/s (the speed, not each
  axis); the lifeboat line is Rook's.
- 2026-10-02: M4 part C, the game (choices for review): the Spore Bombers on `low-air` are drawn
  at their sprite size above the ground and below the low-air banks, so the heavy peak's spore
  banks veil them ("inside the spore banks"); the spore mines are drawn above the haze just below
  the bullets, growing from 70 % and brightening from half while they rise; the Leviathan off the
  play plane (its first pass, arriving, descending, rising) is drawn above the ship at 1.25× (the
  first pass's sprites are drawn at that scale) and scales to 1× by its altitude, switching below
  the ship when it reaches the play plane; its sway plays 0-1-2-1 at 0.3 s a frame and the
  blowhole's glow pulses every 1.2 s; its death is a medium burst at each part in turn, 0.1 s
  apart, then a large one. The objective tracker reads `BOMBERS n / 10`, `DONE` in green or
  `FAILED` in red, the box flashing green or red; the lifeboat lights blink at the beacon's 1 Hz
  until shot. The lights' placements were moved onto the *Kestrel*'s lenses (they were mirrored
  about the wreck's centre, about 1 s late).
- 2026-10-02 (user decision): the Leviathan is drawn at 75 % opacity while it is off the play plane
  (the first pass on `high-air`, the second arriving, descending and rising), body, parts and glow
  alike, and opaque on the plane, where it collides; descending and rising the opacity eases with
  its altitude and reaches full when it switches below the ship (see its
  [spec](../../../enemies/space/leviathan/README.md#decisions)).
- 2026-10-02 (user decision): the hangar intel before this level shows the Leviathan from sensor
  L2 as an "unknown huge contact" with its 40×40 silhouette, as the threat profile promises; it is
  derived from the level's set pieces and their size tier, not named (see
  [hangar](../../../ui/hangar/README.md#decisions)).
- 2026-10-02: Radio retimed to the queue (one line at a time, each page typed at 30 characters a
  second and held 3 s, the last 5 s, 0.4 s between lines): the lines around the first pass played
  up to 17 s late behind the ones before them, after the Leviathan had gone. Order and text kept,
  times moved: Okafor 2 → 1, Ring Control 30 → 36.5, Varga's long-range echo 58 → 47, the Choir
  61 → 58.5 (as the Leviathan's nose comes on screen), Rook 63 → 64.5, Varga's "It's above you"
  65 → 71 (the earliest it can play after the lines before it). Every timed line now starts at
  most 0.2 s late at the default text speed (`RadioTimelineTest`). Ring Control's "Debris field
  ahead" plays inside the field (its first chunk enters at 30.5 s): the three lines before it fill
  the radio until 36 s.
- 2026-10-02 (user decision): radio priorities (the rule is in the
  [HUD](../../../ui/hud/README.md#decisions) radio): timed lines go first, an event line waits for a
  gap before the next timed line and is dropped after 6 s of waiting, so an escaped bomber or the
  lifeboat secret no longer pushes the first pass's lines back. For the Leviathan's lines to find
  their gap, Okafor's Hammer flight line moves from t=160 to 168.5: leaving alive, the Leviathan
  is gone through the top edge at about 160.7 s (154.7 s on hard), and Varga's "It's leaving" (7 s)
  now plays before Okafor; killed during the fight, Ring Control's line fits before it too.
  `RadioTimelineTest` checks the timed lines with an escaped bomber before the first pass, the
  secret, both, and either Leviathan line.
- 2026-10-03: Concept round 16 closed (user decision): the backdrop approved as **final**, with the game captures; `art` stays `chosen`, since the level has no briefing images yet.
- 2026-10-03 (user decision): the M4 part C points left for review accepted as they are: shots over the Leviathan's armoured body fly on towards a living part ahead of them; the lifeboat line is Rook's, in its written wording; Varga's intel line without a sensor ("Slow carriers in the high lanes and a lot of wreckage, Lancer. And something big on long range.").
- 2026-10-03: Concept round 16 closed again (user decision, choice 17): the game choices for review above accepted as they are: the Spore Bombers drawn between the ground and the low-air banks, the spore mines above the haze growing and brightening as they rise, the Leviathan off the play plane above the ship at 1.25× scaling to 1× and switching below it on the plane, its 0-1-2-1 sway and the blowhole glow, the chained death bursts, the objective tracker and the lifeboat lights' blink. The simulation choices for review left after the points accepted earlier today (the first pass's path, the second pass's timings, the vents' and fins' first shots, the debris speed) accepted as they are too, with the user's acceptance of the captures. `art` stays `chosen`: the level still has no briefing images.
- 2026-10-03: Briefing images (M4 batch, `tools/art/briefing_images.py`): one per page,
  `level-03-spore-lanes` and `level-03-spore-echo`, named in the data; proposed for
  [round 20](../../../concept-rounds/round-20/README.md). `art` stays `chosen` until the round is
  approved.
- 2026-10-03: Concept round 20 closed (user decision, "both accepted"): the briefing images approved as **final**. They were the last missing piece; every other part with a look is final, so all of the level's art is final, `art: final`.
- 2026-10-04: Density rework (user decision: at least 32 enemies per minute in the warm-up
  levels): Skitter snakes of 6 at t = 5.5, 112 and 159, a stream of 6 at t = 180, the t=22 snake
  6 → 10 and the t=165 stream 8 → 12 lift the density from 23.7 to 34.3, all outside the Spore
  Bombers' runs and the Leviathan's pass. The autopilot took more damage on hard, so the hard
  Stingers are thinned instead (t=34 V-wing and t=56 column 3 → 2). Damage per run (mean over the
  Skitter waves' timings jittered): easy 19.8 → 20.8, medium 49.4 → 53.1, hard 124.7 → 112.5.
  `bounty_scale` 0.97 puts the typical haul on the budget (805 of 801; a perfect run 1,288).
- 2026-10-05: M4 part H balance pass (user decision): the Leviathan's bounty falls from 190 to 113
  (≈ 15 % of the budget at the scale; it was set when the budget was 1,145, and was 23 % of 801),
  and `bounty_scale` rises from 0.97 to 1.07 so the typical haul stays on the budget: typical
  805 → 783 (−2 %), perfect 1,288 → 1,272 (1.61 → 1.59 × budget). A Skitter's 5 rounds to 5 up to
  a scale of 1.09 and to 6 from 1.1, so the haul steps from 784 (−2 %) to 822 (+3 %) there; 1.07
  is the closer of the two.
- 2026-10-05: Concept round 26 closed (user: the round accepted as proposed): the Leviathan's bounty 113 and the level's `bounty_scale` 1.07 accepted (typical haul 783 of the budget's 801).
