---
title: Level 01 – Break at Dawn
design: approved
implementation: done
art: chosen
depends-on: [../../../enemies/air/skitter, ../../../enemies/air/needler]
updated: 2026-10-02
---

# Level 01 – Break at Dawn

## Summary

The first level and the tutorial. At dawn over Earth, a Vrell scout force hits the **Gagarin
shipyards**, the UTC's orbital shipyard. Lancer launches with Aegis Wing and fights off waves of
Skitters and Needlers over the yard's gantries. The level teaches movement, shooting, pickups
and credits, and ends in a large circling Needler formation. There is no boss. About 3 minutes.

## Briefing

<!-- data: briefing -->
> **Commander Okafor:** "Lancer, this is Aegis Actual. Six weeks ago something came through the
> Tether Gate. Forty minutes ago it arrived at L1, and now it's falling on the Gagarin yards. We
> don't know what they are or what they want. What we do know is that half the Coalition's new
> hulls are sitting in those docks. You're the first bird off the south rail; Rook takes Aegis
> Two off the north rail and covers the far side of the yard. Keep them off the yards, keep your
> head, and bring me back something we can study. Aegis Actual out."
>
> **Dr. Varga:** "Their ships don't show up as metal on our scopes. Whatever they're made of, it
> burns. Aim for the glowing parts."
<!-- /data -->

*Hangar teaser* (on the shop screen of the first hangar visit, after the act intro):
<!-- data: teaser -->
> **Dr. Varga:** "Unknown contacts, small and fast. Your cannon will do, Lancer. Spend a little
> on it if you can."
<!-- /data -->

## Threat profile

<!-- data: threat-profile -->
| Field | Value |
|---|---|
| Dominant layers | `air` (all enemies), `deep` (Earth, the yards' far structures) |
| Attack directions | front 71% · sides 23% · rear 6% (one warned wave) |
| Density | 1 |
| Recommended traits | `forward` |
| Hazards | none |
| Boss / mid-boss | none (final wave: Needler circle) |
| Sensor-suite detail | The sensor suite (800) is in the shop from the start but out of reach of the 300 starting credits, so the panel shows the no-sensor view: setting, dominant layer `air`, main direction front. Varga: "Our scans are patchy, Lancer. Small, fast, lots of them." |
<!-- /data -->

## Objective

- **Primary** `reach-end`: survive to the end of the scroll.
- **Secondary**: destroy at least 80% of all enemies. Pays +50 credits and triggers a line from
  Okafor.

## Layout

Scene: the chosen [Earth orbit](../../../world/earth-orbit/README.md) scene
([sheet](../../../art-direction/concept/parallax-r03-a.png)), used here at the light end of the
atmosphere range. Scroll speed 130 px/s at the 960×540 baseline (a calm level per the
[art direction](../../../art-direction/README.md#parallax-layer-model); play field 480×540).
Total ≈ 180 s ≈ 23,400 px; the last waves (t=162, 166) are gone about 4 s before the end. Motion budget: the only strongly animated background elements are the
drifting low-air cloud decks and, in section 4, the burning platforms.

<!-- data: level-sections -->
| Section | t (s) | Scroll (px) | Speed (px/s) | Atmosphere | Layers and content | Purpose |
|---|---|---|---|---|---|---|
| 1. Launch | 0–20 | 0–2,600 | 130 | clear | `deep`: Earth's curve with the sunrise terminator, starfield. `far`: the yard's north arm, where two distant Stormhawks (Aegis Two, Rook's flight) launch and bank away out of view. `ground`: the Gagarin shipyards' south launch rail sliding out of view. | Get used to movement; no enemies. Control prompts in the side HUD. |
| 2. First Wave | 20–60 | 2,600–7,800 | 130 | light | `deep`: Earth, the Moon small in the distance. `ground`: open dock frames. `low-air`: thin cloud-deck wisps. | Shooting basics; Skitters (harmless rammers) first, then the first Needlers that shoot back. |
| 3. Yard Crossing | 60–110 | 7,800–14,300 | 130 | light | `ground`: gantries, cranes, a half-built cruiser hull, cargo containers (destructible). `low-air`: lattice beams and crane jibs passing under the player. | Ground layer as scenery and loot; the first side entry; the secret beacon. |
| 4. Pursuit | 110–160 | 14,300–20,800 | 130 | medium | `ground`: yard perimeter, defence platforms burning. `low-air`: cloud decks drifting between the yard and the play plane. `high-air`: thin spark streaks. | Mixed waves, one warned rear wave, rising density. |
| 5. Scout Leader | 160–180 | 20,800–23,400 | 130 | clear | `deep`: open space past the yard; the Vrell strike group's glow on the horizon. `ground`: the last perimeter platform. | Final set piece: Needler circle plus Skitter streams. Then the level ends. |
<!-- /data -->

### Backdrop

How the layers above are composed, rendered from the `backdrop` in [data.yaml](data.yaml) (schema
in [architecture](../../../tech/architecture/README.md#data-file-schemas)). A layer's tile set
repeats along the layer and changes at a seam that enters at the top edge when the section starts;
a set piece is listed with the seconds it is on screen; the atmosphere names the low-air cloud
banks, the high-air wisps and the haze over `deep` and `far`, and a change ramps across the
section boundary (`ramp`). The art is a placeholder ([backdrop_l01.py](../../../../tools/concept/backdrop_l01.py)).

<!-- data: backdrop -->
| Section | Atmosphere | `deep` | `far` | `ground` | `low-air` | `high-air` |
|---|---|---|---|---|---|---|
| 1. Launch | clear: wisps, haze 0 % | `earth`; earth-dawn 0–103 s; moon 3–39 s | north-arm 0–12 s; stormhawk-far flies 1.5–6 s; stormhawk-far flies 2.5–7 s | launch-rail 0–4 s; crossbeam 20–24 s | | |
| 2. First Wave | light: banks-light, wisps, haze 8 % | `earth` | | `dock-frames`; dock-frame 27–33 s; dock-frame 40–46 s; dock-frame 51–57 s; crossbeam 60–64 s | | |
| 3. Yard Crossing | light: banks-light, wisps, haze 8 % | `earth` | | `gantry-rails`; bridge-crane 68–72 s; cruiser-hull 78–86 s; bridge-crane 90–94 s; crossbeam 110–114 s | lattice-beam 64–68 s; crane-jib 72–76 s; lattice-beam 83–87 s; crane-jib 95–99 s; lattice-beam 101–105 s | |
| 4. Pursuit | medium: banks-medium, wisps, haze 16 % | `earth`; earth-limb 143–180 s | | `perimeter`; platform-burning 115–121 s; platform-burning 131–137 s; platform-burning 147–153 s; crossbeam 160–164 s | | `spark-streaks` |
| 5. Scout Leader | clear: wisps, haze 0 % | `earth`; vrell-glow 167–180 s | | platform 165–171 s | | |
<!-- /data -->

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

<!-- data: waves -->
| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |
|---|---|---|---|---|---|---|
| 22 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (left) | Curls toward the centre, no fire |
| 30 | 2 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | front (right) | Mirror of the previous wave |
| 40 | 2 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | Aimed shot every 2.5 s each; radio cue "they shoot back" |
| 50 | 2 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 8 | front | Sweeps down the screen |
| 64 | 3 | line abreast | [Needler](../../../enemies/air/needler/README.md) | 3 | front | Hover, fire, leave |
| 75 | 3 | snake | [Skitter](../../../enemies/air/skitter/README.md) | 6 | left side | First side entry, slow (120 px/s); radio warning |
| 86 | 3 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 5 | front | |
| 96 | 3 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 10 | front (alternating edges) | Teaches sweeping fire |
| 112 | 4 | snake + V-wing | [Skitter](../../../enemies/air/skitter/README.md) + [Needler](../../../enemies/air/needler/README.md) | 6 + 3 | front | First mixed wave |
| 122 | 4 | pincer | [Needler](../../../enemies/air/needler/README.md) | 4 | sides | 2 from each side, hold for 4 s at the screen edge |
| 134 | 4 | line abreast | [Skitter](../../../enemies/air/skitter/README.md) | 6 | rear | Warned 3 s ahead ("Contacts on your six, Lancer!"); slow (120 px/s) |
| 146 | 4 | V-wing | [Needler](../../../enemies/air/needler/README.md) | 7 | front | Large V |
| 162 | 5 | circle | [Needler](../../../enemies/air/needler/README.md) | 8 | front | Orbit a point above the centre for 6 s, then break toward the player one by one |
| 166 | 5 | stream | [Skitter](../../../enemies/air/skitter/README.md) | 12 | sides | Enters while the circle is orbiting |

Totals: Skitter 60 · Needler 35.
<!-- /data -->

## Ground targets

<!-- data: ground-targets -->
| Section | Target | Effect |
|---|---|---|
| 3 | Cargo containers ×10 (ground layer, destructible, 3 HP) | Each pays 5 and drops a small salvage pickup (10) |
| 3 | Beacon on the crane at t≈90 (blinks red) | Secret, see below |
<!-- /data -->

No hostile ground targets in L01. Hostile ground targets arrive in L02. The containers and the
beacon follow the [loot-target readability rule](../../../art-direction/README.md#readability-rules)
(warm hazard markings, light rim, glint, hit flash, damage state, break-apart; the beacon also
blinks); placeholders from [ground_targets.py](../../../../tools/concept/ground_targets.py).

## Hazards

None. Debris in section 4 is scenery only (the `deep` layer).

## Secrets and pickups

Pickup types are defined in [player](../../../player/README.md#in-level-pickups).

- **Beacon cache** (t≈90): hitting the blinking crane beacon 3 times releases a hidden crate
  worth 80 credits. Rook, on the radio from the far side of the yard: "*Nice shooting. Finders
  keepers.*"
- **Armour patch** (around t≈140): dropped by the last Needler of the t=122 pincer.
- Shield cells: every 4th Needler kill drops one (the [Needler](../../../enemies/air/needler/README.md)'s
  drop rule). No overdrive in L01; weapon upgrades are bought in the hangar.

## Radio chatter

Text and radio blips only (no voice acting). Rook flies Aegis Two elsewhere in the yard and is
heard on the radio only.

<!-- data: radio -->
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
<!-- /data -->

## Boss / mid-boss

None. The Needler circle (t=162) acts as the finale.

## Music & ambience

Track 5 *Act 1 B: Earth orbit & Luna* ("Coalition Rising", the main motif; see the
[track list](../../../audio/music/README.md#track-list)), starting at the section 2
transition, base stem only until section 4, where it crossfades to the full mix (the round 11
stems). Section 1 has ambience only: the Earth-orbit
ambience ([sfx](../../../audio/sfx/README.md#ambience-per-setting)), the launch rail and radio
blips. Mission complete jingle at the end.

## Credit budget

The total matches budget(1) from the [economy](../../../systems/economy/README.md#per-level-budget)
curve. Bounties from the stat blocks: Skitter 5, Needler 12.

<!-- data: credit-budget -->
| Source | Credits (medium) |
|---|---|
| Kills: Skitter 60 × 5 + Needler 35 × 12 | 720 |
| Ground targets: cargo containers 10 × (5 + small salvage 10) | 150 |
| Secret: beacon cache (hidden crate, 8% of budget) | 80 |
| Secondary objective | 50 |
| **Total** | **1,000** |
<!-- /data -->

## Difficulty notes

- **Easy**: Needlers fire every ≈3.6 s (the global fire-rate lever, ×0.7); no rear wave (it
  enters from the front instead); two extra armour patches, dropped by the last Needler of the
  t=40 and t=86 V-wings.
- **Hard**: Needlers fire 2-shot bursts; the rear wave has 10 Skitters; the Needler circle breaks
  toward the player in pairs, and selected circle Needlers lead the target (stat-block hook).

## Concept art

Concept [round 11](../../../concept-rounds/round-11/README.md) — two weak spots of the backdrop
placeholders fixed in `tools/concept/backdrop_l01.py` (not a choice; the board shows before and
after, made by `tools/concept/backdrop_fixes_r11.py`; prompts:
[concept/prompts.md](concept/prompts.md)). Only `earth-dawn.png` and `platform-burning_0…3.png`
in `assets/backdrop/level-01/` changed; every other piece is byte-identical.

| File | What | Status |
|---|---|---|
| [concept/backdrop-fixes-r11-a.png](concept/backdrop-fixes-r11-a.png) | Before / after: the dawn terminator (ordered dither, 16 alpha steps and 32 colours instead of hard bands) and the burning platform's four frames (ragged venting plumes in a dull fire ramp with smoke and embers instead of round orange blobs) | chosen |
| [concept/backdrop-fixes-r11-a.gif](concept/backdrop-fixes-r11-a.gif) | Before / after: the burning platform's loop at 8 fps | chosen |

## Implementation

- [x] Scroll timeline, sections, atmosphere intensity and parallax content per layer as in *Layout*.
- [x] Wave script matches the *Waves* table (time, formation, count, entry edge).
- [x] Destructible cargo containers and the beacon secret.
- [x] Radio chatter cues fire at their triggers with portraits in the side HUD.
- [x] Secondary objective tracked and rewarded.
- [x] Credit total at medium with perfect collection is 1,000 (± 5%).
- [x] Easy/hard variations as in *Difficulty notes*.
- [x] Control prompts shown in section 1 (skippable).
- [x] Aegis Two (Rook's flight) appears only as distant `far`-layer scenery in the launch; no
      wingman or escort sprite on the play plane.

## Open questions

- The haze strength per atmosphere intensity, the cloud banks' drift speed and the length of an
  atmosphere ramp are first values in [data.yaml](data.yaml) (`backdrop.atmosphere`, `drift`,
  `ramp`): the art direction gives bank coverage per intensity, but no haze strength, drift speed
  or ramp length beyond "several seconds".
- Motion budget: the two launching Stormhawks (section 1) are counted as one animated element,
  like the cloud decks and the burning platforms; *Layout* names only the last two.

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
- 2026-10-02: The level script moved into [data.yaml](data.yaml) (M2 data files): the *Layout*, *Waves* (with the totals), *Ground targets*, *Radio chatter* and *Credit budget* tables are rendered from it by `tools/sync_tables.py`; the credit budget is now computed from the waves, the stat-block bounties and the economy's budget(1). Objectives, music cues, the beacon line, the placed armour patch and the easy/hard changes are in the data too; their prose here stays hand-written.
- 2026-10-02: User decisions (M2): the threat profile's attack directions are derived from the
  waves (each entry's share of the enemies at medium, rendered into the table, so they cannot
  drift: front 71 % · sides 23 % · rear 6 % instead of the hand-written 85/10/5); the rear wave's
  note quotes the radio line exactly; the armour patch is dropped by the last Needler of the t=122
  pincer ("t≈140" is approximate wording, so the data file has no time for it); easy Needlers fire
  at the global fire-rate lever (×0.7, ≈3.6 s) instead of a 3.5 s override; `sides` alone means
  both side edges, a single side reads "left side"; shield cells drop on every 4th Needler kill
  (the Needler's drop rule). Recorded in [data.yaml](data.yaml) for tuning after playing: the 10
  cargo containers in pairs on alternating sides of section 3 (t = 62, 71, 80, 89, 98), the beacon
  at x = 300, the two extra easy armour patches on the last Needler of the t=40 and t=86 V-wings,
  the slow Skitter waves (t=75, t=134) at 120 px/s and a 0.5 s gap between stream units.
- 2026-10-02: M2 implementation (`vanguard.sim.Sortie` with `WaveSchedule` and `Formations`,
  built from the data by `vanguard.content.SimSpecs`; `vanguard.game.screen.LevelScreen` and
  `DebriefScreen`). Level 01 plays from launch to debrief at the difficulty of the `--difficulty`
  launch option (medium by default). A replay test flies the whole level at medium. Still open:
  the per-section atmosphere and layer content (the backdrop is the chosen Earth orbit scene
  throughout) and Aegis Two in the launch. Placeholders: the cargo containers and the beacon are
  drawn in code (no concept art yet); the formation layouts (where a V hovers, the circle's centre
  at 180 px below the top, the 0.5 s between circle break groups, the snake and stream paths) are
  first values in `Formations`, to be tuned after playing.
- 2026-10-02: M2 part C, the backdrop (user feedback: "the same elements keep scrolling through the
  screen the whole time"): the layers of *Layout* are laid out in the `backdrop` of
  [data.yaml](data.yaml) and drawn by `vanguard.game.render.Backdrop`; the *Backdrop* table is
  rendered from it. Per section a tile set per layer, changing at a seam that enters at the top
  edge when the section starts (structures change like a tile map instead of dissolving into each
  other; crossbeams cover the ground seams); set pieces placed by the time their centre passes the
  middle of the screen; the atmosphere picks cloud banks (~12 % light, ~22 % medium cover, both
  drifting), wisps and haze, ramping across a boundary. Aegis Two: two distant Stormhawks launch
  from the north arm on `far` and bank away (an angle set along a path). The deep layer runs from
  space with the Moon and Earth's dawn limb (night side, city lights, the sunrise terminator) over
  the day side to Earth's curve falling behind in section 5, with the Vrell glow above it.
  `content` checks the density (at most 3 mid-size set pieces on screen) and the motion budget (at
  most 2 animated elements, nothing faster than 2 px per frame on its own) frame by frame.
  Placeholders: all images come from `tools/concept/backdrop_l01.py`, built from the chosen Earth
  orbit scene's kit and palette; cloud-bank shadows on the ground are left out.
- 2026-10-02: User decisions after playing (user decision): the level is **180 s** instead of
  190 s (≈ 23,400 px; it took too long to complete after the last enemy): section 5 ends at 180,
  the waves are unchanged, so the level completes about 4 s after the t=162 circle and the t=166
  stream are gone; the credit budget is unchanged (nothing paid in the cut tail). The finale's
  backdrop moved with it: the last platform at t=168 (gone by 171 s, leaving open space for the
  outro), the Vrell glow from 166 s (its top edge stays above the screen) and Earth's limb
  re-rendered ("the planet stops abruptly": it was a flat, thick bright band under an opaque black
  slab): the day side hazes and darkens towards a curved horizon (radius 520 px), a thin bright
  rim and a faint airglow fade into open space; its crest enters at the top at 158 s and is
  200 px above the bottom edge at the end. The cargo containers and the beacon "did not stand out
  as targets" (code-drawn in colours close to the yard's): they follow the new loot-target
  readability rule of the art direction, as pre-rendered placeholder sprites (rust body,
  amber/black hazard bands, 1 px light rim, a glint every 2 s, white hit flash, damaged frame
  after the first hit, an 8-frame break-apart; the beacon's red lens blinks at 1 Hz).
- 2026-10-02: The gap at the top of the screen in the last seconds (user, after playing) came from
  the 5 s outro after the level end, in which the scroll runs on: the Vrell glow's top edge (from
  181 s) and then the opaque top edge of Earth's limb (from 184 s), above which the `earth` tiles
  showed, came onto the screen. Earth's limb is now 720 px tall (the crest still enters at the
  top at 158 s and is 200 px above the bottom at 180 s) and the Vrell glow 300 px, placed so both
  top edges stay above the screen until 186 s. The same rule caught a 19 px notch of space at the
  dawn limb's top-left corner (68–103 s): Earth's curve now covers its whole top edge and the
  overlay fades out over its top 24 rows; the Moon moved 6 px left (x = 44) to stay in the space
  beside the limb. The deep-layer rule of the [art direction](../../../art-direction/README.md#parallax-layer-model)
  is now checked by the tests.
- 2026-10-02: Section 5 (Scout Leader) is `clear` instead of `light` (user decision): past Earth's limb there is no cloud deck, so the low-air banks no longer drift over open space in the outro.
- 2026-10-02: Backdrop placeholder fixes (concept round 11): the dawn terminator's posterization
  bands are dithered away and the burning platforms' fires are ragged venting plumes instead of
  blobs; only `earth-dawn` and `platform-burning` were re-rendered.
- 2026-10-02: Concept round 11: the backdrop fixes (dithered dawn terminator, venting fire plumes) accepted by the user.
- 2026-10-02: M3 part B1: the briefing pages and the hangar teaser moved into
  [data.yaml](data.yaml) (`briefing`), rendered into *Briefing*; the game shows them on the briefing
  screen and the hangar. The launch rail sound plays with the launch, the edge-warning tone and look
  A with every warning, and the music plays the base stem from section 2 and the full mix from
  section 4 (round 11 choices). The level is flown from the campaign: its armour at the start is the
  campaign's, a destroyed ship leads to the mission failed screen, and a won level is banked.
