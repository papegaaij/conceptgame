---
title: Concept round 33 — M5 part E, Level 11 Atlantic Convoy
design: approved
implementation: n/a
art: chosen
depends-on: [../../campaign/act-2-homefront/level-11-atlantic-convoy, ../../enemies/naval/driftjelly, ../../enemies/naval/reef-spitter, ../../enemies/bosses/harbour-kraken, ../../allies, ../../player/weapons/torpedo-pod, ../../ui/briefing, ../../ui/hangar, ../../ui/hud, ../../audio/sfx, ../../audio/voice, ../../art-direction/production]
updated: 2026-10-09
---

# Concept round 33 — M5 part E, Level 11 Atlantic Convoy

**Closed 2026-10-09** (user): the production art approved as final (the Driftjelly, the Reef Spitter,
the Harbour Kraken with the changes after the user's feedback, the water and torpedo effects, Level
11's props and ocean backdrop, the two briefing images, the three intel pictures); the convoy ship
pair **a**, the lane telegraph and slam **a**, Atlas Control's voice **b** (MaryAnn); the eight
sounds accepted, four of them after a rework; the captures (the Kraken's retaken), voiced lines,
texts (Level 11 now `approved`), numbers (hard kept as built), decisions and every build choice
accepted, (n) changed to 3 hits (hard 5). The picks are in the game (Atlas Control cast and her ten
takes rendered, the seven recorded sounds produced into `assets/sfx/`); the outcome of each row is in
the *Choices* table.

## Summary

The review round of M5 part E ([roadmap](../../tech/roadmap/README.md#m5-parts)): Level 11
*Atlantic Convoy*, the first sea level, the `sub` layer, the Torpedo Pod's first water and the
act's mid-boss, the Harbour Kraken. The production art went straight to production (E9 = a) and is
reviewed here as **final**:
- the Driftjelly, the Reef Spitter and the Harbour Kraken (with the Tiamat grip overlay);
- the water effects and the torpedo's art;
- Level 11's props and the ocean backdrop;
- the two briefing images and the three intel pictures.

Three a/b picks are new: the convoy ship pair, the Kraken's lane telegraph and slam look, and Atlas
Control's voice. The round also holds:
- eight sounds, one proposal each (accept or redo);
- the game captures of the level, the Kraken's arena, a torpedo run and the readability crops;
- a listen to the 15 new voiced lines;
- the new texts (Level 11's document is `design: review`), quoted in full;
- the numbers the agents chose or measured, hard's included;
- the user's part E decisions, for the record;
- the choices made during the build, to confirm one by one.

Open [index.html](index.html) in a browser (regenerate with `python3 tools/concept/board.py 33`).

Every art row is *approve as final* or *redo* (say what to change). An a/b row is *pick a or b*.
A sound, listening, text or numbers row is *accept* or *change* (say what). The build-choice rows
list lettered items, each *yes* (keep as built) or *no* (say what to change). Only an approved part
gets `art: final`.

In the game (`./gradlew :desktop:run --args="…"`):
- Level 11 with the capture's fit (the balance plan's L11 fit: Rook's Mortar refitted, no torpedo):
  `--level 11 --escort rook:mortar:1`. Ship pair **a**, slam look **a** and the eight sounds (as
  placeholders in `assets/sfx/`) are in the game; Atlas Control's lines play as text until the voice is
  cast.
- The torpedo run: the same with `--loadout left=torpedo-pod:3` (the capture's fit). A Torpedo Pod
  fired a second before the sunken pod enters seeks it.
- Hard: `--difficulty hard` (the numbers in row 7).
- The way in: `--level 10 --invulnerable`. Winning it plays the debrief, Level 11's two briefing
  pages and the hangar with Rook's teaser and the intel panel at each sensor level.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1a | [Driftjelly](../../enemies/naval/driftjelly/README.md#concept-art) | **final art** (`tools/art/driftjelly.py`, straight from the chosen round-07 model, E9 = a): the `-sub` body drawn through the sub pass and the surface frames over it, the ring `-pulse`, `-surface` and `-ripple`, the death and tatters. Look at: the surfaced and the submerged jelly side by side, the pulse ring, the ripples where it breaks the surface | [driftjelly-final-r33-a.png](../../enemies/naval/driftjelly/concept/driftjelly-final-r33-a.png), [.gif](../../enemies/naval/driftjelly/concept/driftjelly-final-r33-a.gif) | faint jelly ripples; the submerged jelly reads only through a quiet sub pass | **approved as final** (user): the `-sub` body, the surfaced dome, the ring `-pulse`, `-surface`, `-ripple`, the death and tatters; the weak spots as they are; Driftjelly `art: final`, `done`, its numbers and readings accepted (rows 7, 9), so `design: approved` |
| 1b | [Reef Spitter](../../enemies/naval/reef-spitter/README.md#concept-art) | **final art** (`tools/art/reef_spitter.py`, from the chosen round-06 model): 32 headings, recoil, `-raft-sub` and eight `-raft` frames, sinking, death and tatters. Look at: the barnacle gun on its raft, the raft's edge, the muzzle flash | [reef-spitter-final-r33-a.png](../../enemies/naval/reef-spitter/concept/reef-spitter-final-r33-a.png), [.gif](../../enemies/naval/reef-spitter/concept/reef-spitter-final-r33-a.gif) | the raft's cut edge | **approved as final** (user): the gun at 32 headings, the recoil, the raft frames, the sinking, the death and tatters; the raft's cut edge as it is; Reef Spitter `art: final`, `done`, `design: approved` |
| 1c | [Harbour Kraken](../../enemies/bosses/harbour-kraken/README.md#concept-art) | **final art** (`tools/art/harbour_kraken.py`, from the chosen round-07 model): the head (5 frames, `-sub`, mantle with arm roots ≈ 250 px), the 12 surfacing frames, the slam arms (7 tapers × 32 headings, tips, foam), the idle arms, the shadow, the sinking, the death, and the **Tiamat grip overlay** (`-grip` 4 × 256×156 and `-grip-sub`, drawn by the Kraken over the 208×152 platform). Look at: the head with the eyes open, the grip arms on the deck's edge, the arm roots under the mantle | [harbour-kraken-final-r33-a.png](../../enemies/bosses/harbour-kraken/concept/harbour-kraken-final-r33-a.png), [.gif](../../enemies/bosses/harbour-kraken/concept/harbour-kraken-final-r33-a.gif) | foam edge noise; the grip arms submerged near the head; a subtle swell; the arm over the mantle in lanes 2 and 3 with the head up; the arms' under-water stretch from root to deck | **approved as final** (user), with the changes after the user's feedback (2026-10-09): the head surfaces and sinks off the deck's south edge with Platform Tiamat raised (option b, centre y 78), shots on the surfaced parts hit flesh instead of water ripples, the slamming arms curve and whip, only the hit part flashes (subtler); the retaken Kraken capture accepted; Harbour Kraken `art: final`, `done`, `design: approved` |
| 1d | [Water effects and torpedo](../../art-direction/production/README.md) | **final art** (`tools/art/water_fx.py`): `water-splash`, `-ripple`, `-collar-24/40/64`, `-foam-strip`, `-wake`, the water and under-water explosions, and the Torpedo Pod's shot, bubbles and splash. Look at: the splash and ripple rungs by size, the collars, the torpedo's body and bubble trail | [water-fx-final-r33-a.png](../../art-direction/concept/water-fx-final-r33-a.png), [.gif](../../art-direction/concept/water-fx-final-r33-a.gif) | faint water wake; a rectangular container collar; the torpedo was not found in the first capture's frames (fixed by the lit back, see row 4) | **approved as final** (user): the weak spots as they are (the faint wake, the rectangular container collar); the torpedo's lit back (row 4) kept |
| 1e | [Level 11 props](../../art-direction/README.md#concept-art) | **final art** (`tools/art/l11_props.py`, drawn into the backdrop by `backdrop_l11.py`): `floating-container` (with collar and break), `sunken-pod` (with its rise), the backdrop's `burning-freighter`, `reef-growth-a/b/c` and `reef-root-a`. Look at: the pod on its reef root, the container collar, the reefs | [l11-props-final-r33-a.png](../../art-direction/concept/l11-props-final-r33-a.png), [.gif](../../art-direction/concept/l11-props-final-r33-a.gif) | small reefs and fires; the pod on its root hard to see; flotsam ghost; the container's collar is rectangular | **approved as final** (user): the weak spots as they are |
| 1f | [Ocean backdrop](../../campaign/act-2-homefront/level-11-atlantic-convoy/README.md#concept-art) | **final art** (`tools/art/backdrop_l11.py`, from the chosen ocean scene): six sections, the sea as a swell layer under a translucent chop, seven reef lines with channels, the oil slick, the burning freighter, Platform Tiamat halting at the top (centre (240, 110) at the halt). Look at: the sea in the halt, Tiamat's deck, the light rising to the end | [backdrop-final-r33-a.png](../../campaign/act-2-homefront/level-11-atlantic-convoy/concept/backdrop-final-r33-a.png), [.gif](../../campaign/act-2-homefront/level-11-atlantic-convoy/concept/backdrop-final-r33-a.gif) | the stand-in sea blotchy and static in the halt; ghost contours; small magenta reefs; faint slicks; flat freighter smoke; static flotsam collars; platform lamps lit; open-water overcast only haze | **approved as final** (user): the weak spots as they are; Level 11 `art: final` |
| 1g | [Level 11 briefing images](../../ui/briefing/README.md#concept-art) | **final art** (`tools/art/briefing_images.py`, 672×240): Okafor's `level-11-convoy-route` (the Atlantic route, four hulls, seeded lanes, reef line, Tiamat marked) and Varga's `level-11-sub-scan` (a Driftjelly above and below the waterline, a torpedo's run) | [briefing-images-final-r33-a.png](../../ui/briefing/concept/briefing-images-final-r33-a.png) | faint relief; small ships and reefs; the route runs under the platform | **approved as final** (user): both images; ship pair a picked (row 2a), so the route image needs no redraw |
| 1h | [Intel pictures](../../ui/hangar/README.md#concept-art) | **final art** (`tools/art/intel.py`): the 30×30 sensor-L2 pictures of the Driftjelly, the Reef Spitter and the Harbour Kraken's silhouette ("unknown, large, submerged") | [intel-final-r33-a.png](../../ui/hangar/concept/intel-final-r33-a.png) | the Reef Spitter's picture dark; the Kraken's silhouette shows no arms | **approved as final** (user): the dark Reef Spitter and the armless Kraken silhouette kept |
| 2a | [Convoy ships](../../allies/README.md#convoy-cargo-ship) | **pick a or b**, both at production quality (`tools/art/convoy_ships.py`): the cargo ship (damaged, fire, collar, wake, sinking, pip) and the escort frigate (collar, wake, muzzle, flak). **a** "Atlantic line": the ocean scene's feeder container ship (56×120) and a grey CDF frigate (40×110); **b** "Reactor run": a rust-red heavy-lift carrier with reactor cargo and a CDF stealth trimaran frigate. Variant a is in the game under the game's names | [convoy-ships-r33-a.png](../../allies/concept/convoy-ships-r33-a.png), [.gif](../../allies/concept/convoy-ships-r33-a.gif); [convoy-ships-r33-b.png](../../allies/concept/rejected/convoy-ships-r33-b.png), [.gif](../../allies/concept/rejected/convoy-ships-r33-b.gif) | a: the cargo ship is muted; b: the frigate is dark; a light list for the damage | **a** (user), "Atlantic line", already in the game: approved as final, the weak spots as they are; b moved to `concept/rejected/` (`convoy_ships.py --variant b` now writes its review there) |
| 2b | [Lane telegraph and slam](../../art-direction/README.md#concept-art) | **pick a or b** (`tools/art/water_fx.py --variant`): the lane's warning (churn, mark) and the slam's splash along the arm. **a** "edge dashes + spray sheets"; **b** "lane boil, chevrons, rollers". Variant a is in the game | [slam-r33-a.png](../../art-direction/concept/slam-r33-a.png), [.gif](../../art-direction/concept/slam-r33-a.gif); [slam-r33-b.png](../../art-direction/concept/rejected/slam-r33-b.png), [.gif](../../art-direction/concept/rejected/slam-r33-b.gif) | the capture saw a's spray as the **brightest thing on screen** (b not yet seen in the game) | **a** (user), "edge dashes + spray sheets", already in the game: approved as final, its bright spray included; b moved to `concept/rejected/` (`water_fx.py --variant b` now writes its review there) |
| 2c | [Atlas Control's voice](../../audio/voice/README.md#concept-art) | **audition, pick a or b** (`tools/concept/audio/tts_r33.py`, Chatterbox, radio filter b, neutral): the CDF officer of Atlas-Seven; three lines 1 s apart (the t=1 call, the `ally-hit` line with `{ally}` = Halvorsen, the t=62.5 sonar contact). **a** Alister (LibriVox, public domain, male, 106 Hz); **b** MaryAnn (LibriVox, public domain, female, 174 Hz). His lines play as text until cast | [voice-atlas-control-r33-a.ogg](../../audio/voice/concept/rejected/voice-atlas-control-r33-a.ogg), [voice-atlas-control-r33-b.ogg](../../audio/voice/concept/voice-atlas-control-r33-b.ogg); the clips in [Listening](#listening) | the ship names (Halvorsen, Mbeki, Saint-Laurent) are rendered as separate takes after the pick | **b** (user, 2026-10-09): MaryAnn cast as Atlas Control (`refs/ref-atlas-control.wav`); a in `concept/rejected/`, its clip deleted with its CREDITS.md row. Her ten takes rendered at the close: t=1, t=62.5, t=110, the `ally-hit` and `ally-lost` lines once per ship name and the `secondary-objective` thanks; Whisper reads every take back whole with a name prompt, no pins needed |
| 3 | [Level 11 sounds](../../audio/sfx/README.md#concept-art) | **accept each, or redo** (`tools/concept/audio/sfx_r33.py`, one proposal each, CC0 recordings plus synthesis; after the rework the churn and the sink use byjoshberry's CC-BY waves, already on the credits roll): the Kraken's **slam** ([a](../../audio/sfx/concept/kraken-slam-r33-a.ogg)), **churn** ([a](../../audio/sfx/concept/kraken-churn-r33-a.ogg)), **surfacing** ([a](../../audio/sfx/concept/kraken-surface-r33-a.ogg)) and **death** ([a](../../audio/sfx/concept/kraken-death-r33-a.ogg)); a ship's **hit** ([a](../../audio/sfx/concept/ship-hit-r33-a.ogg)) and **sinking** ([a](../../audio/sfx/concept/ship-sink-r33-a.ogg)); the frigate's **flak** ([a](../../audio/sfx/concept/frigate-flak-r33-a.ogg)); the jelly's **pulse** ([a](../../audio/sfx/concept/driftjelly-pulse-r33-a.ogg)). Listening points in [Listening](#listening) | the eight `*-r33-a.ogg` on the board | not listened to by Claude (envelopes and levels only); the pulse, death, sinking, water routing and surfacing were not tried in play; hard's 0.8 s churn overruns 0.2 s into the arm's rise | **accepted** (user): slam, surfacing, ship hit and jelly pulse as first made; churn (now a surge of waves over a rumble, the byjoshberry waves CC-BY and already on the credits roll), death and sinking reworked without bubbles, the flak raised twice to −16 dB after listening, then accepted. Seven produced at the close (`assets/sfx/`, `tools/art/sfx_originals.py` with `sfx_r33.py`'s `PRODUCTION`), the synthesized pulse copied; `Sfx` keeps its paths |
| 4 | [Level 11 in the game](../../campaign/act-2-homefront/level-11-atlantic-convoy/README.md#concept-art) | **game captures** (the run's options and numbers in [prompts.md](../../campaign/act-2-homefront/level-11-atlantic-convoy/concept/prompts.md)). **The level** (15 panels, medium, arms first, left arm cut at t≈184, 154 of 276 kills, 3 of 3 afloat, 48 s fight, grade B, 1,787 credits) and **the readability crops** (1:1 and 2×) **predate the fix pass**. **The Kraken's arena** was **retaken after the fix pass** (half-white hit flash with a 0.25 s cooldown, the torpedo's lit back, the foreshadow drawn over the chop, the opaque darker head while down; fight ≈ 50 s, sound ≤ 0.15 s late). **The torpedo run** (`left=torpedo-pod:3`: the pod freed at t≈96, the crate caught, 3 of 3, grade A, 2,269 credits) predates the fix pass | [level-11-capture-final-r33-a.png](../../campaign/act-2-homefront/level-11-atlantic-convoy/concept/level-11-capture-final-r33-a.png), [.mp4](../../campaign/act-2-homefront/level-11-atlantic-convoy/concept/level-11-capture-final-r33-a.mp4); [kraken-capture-final-r33-a.png](../../campaign/act-2-homefront/level-11-atlantic-convoy/concept/kraken-capture-final-r33-a.png), [.mp4](../../campaign/act-2-homefront/level-11-atlantic-convoy/concept/kraken-capture-final-r33-a.mp4); [torpedo-capture-r33-a.png](../../campaign/act-2-homefront/level-11-atlantic-convoy/concept/torpedo-capture-r33-a.png), [.mp4](../../campaign/act-2-homefront/level-11-atlantic-convoy/concept/torpedo-capture-r33-a.mp4); [level-11-readability-r33-a.png](../../campaign/act-2-homefront/level-11-atlantic-convoy/concept/level-11-readability-r33-a.png) | the level, torpedo and readability captures still show the faults the fix pass removed (white head under fire, invisible torpedo, foreshadow at t≈55, arms across the mantle); the sound is aligned at "device reopen" (0.15–0.19 s late at the end); `NO WATER` can never show on L11; the arm-over-mantle and the dark under-water stretch are tiny at 1:1 | **accepted** (user): the captures as taken (the level, torpedo and readability captures from before the fix pass; the Kraken's retaken again after the rework of row 1c) |
| 5 | [Level 11's lines voiced](../../audio/voice/README.md#decisions) | **listening row**: 15 new lines rendered with the cast voices (`tools/art/voice.py`): Okafor 4, Varga 7, Rook 4; Atlas Control's lines are text until row 2c. Listening points: "Tiamat", "Vrell", "Atlas-Seven", and whether Okafor's "Aegis" still reads as "ages" (pinned, still so) | [Listening](#listening) | the "Aegis" take (Okafor's level-end line) still sounds like "ages" after the pin | **accepted as rendered** (user): all 15 lines, the pinned takes included; none to re-render |
| 6 | [Texts for review](#texts-for-review) | **text review** (Level 11 is `design: review`): the two briefing pages, Rook's teaser, Varga's four intel lines (L2 and L3 shortened to fit the panel), the radio script, the two level-end lines (the second for no ship afloat), the `CONVOY` tracker and `HULLS AFLOAT n / 3` | — | the L2 and L3 intel lines are shorter than the draft; the sinking line names the ship, not the frigate | **approved** (user): every text as quoted; Level 11's document leaves `review` for `approved` |
| 7 | [Part E numbers](#part-e-numbers) | accept or change the numbers listed under [Part E numbers](#part-e-numbers): enemies per difficulty, density, pacing, `bounty_scale` 0.6, the typical haul 1,414 (+2.7 %), the perfect run, the Kraken's HP, bounty and par, the fight lengths per play style, the cargo ships' HP, the convoy bonus results and **hard** | — | **hard keeps 0 ships and ≈ 120 s over 2–5 attempts** (the earlier stance "hard is supposed to be hard"); the convoy bonus is met only by an arm-first pilot; the level run exactly 3 of 5 is fragile | **accepted** (user); hard kept as built ("hard is supposed to be hard": after the rework medium arm-first 4 of 5 seeds keep all three ships, hard arm-first 0 of 5) |
| 8 | [Part E decisions](#user-decisions-of-part-e-for-the-record) | **for the record, not to choose again**: E1–E10, the 28 stated defaults and the user's 2026-10-09 answers (convoy measured then tuned; the four small fixes; the capture's bugs fixed); check they were carried out | — | | **checked** (user): every decision carried out |
| 9 | [Build choices](#build-choices) | **yes or no, each**: (a)–(y) under [Build choices](#build-choices) | — | (n) is the one that changes a rule | **confirmed** (user): (a)–(y) as built, but (n): the sunken pod's hits lowered to **3** (hard 5) |

## Part E texts and numbers

The texts and numbers the agents chose or measured to build part E (rows 6–9). The details are in
the parts' READMEs (Decisions of 2026-10-08 and 2026-10-09). The design source is the gap analysis
of part E (E1–E10 of 2026-10-08).

### Texts for review

From [Level 11's data](../../campaign/act-2-homefront/level-11-atlantic-convoy/data.yaml).

**Level 11 briefing**, two pages, one screen each
([Level 11](../../campaign/act-2-homefront/level-11-atlantic-convoy/README.md#briefing)):

> **Commander Okafor** (`level-11-convoy-route`): "The Arctic relays are running on reserve power,
> Lancer. Convoy Atlas-Seven carries their reactor parts across the Atlantic: three cargo hulls and
> a frigate. Something large has been tracked near Platform Tiamat, right on their route. Get them
> to open water."
>
> **Dr. Varga** (`level-11-sub-scan`): "The Vrell have seeded the sea lanes. Jellies drift on the
> surface and below it, and gun rafts grow on the reefs. Our guns can't touch what's under the
> waves. Torpedo pods can, if you buy them."

**Hangar teaser** (Rook, `LT. K. TANAKA / AEGIS TWO`, not voiced): "Cargo ships to mind and jellyfish
that shoot back. Torpedo pods are in the shop, if you fancy fishing."

**Varga's intel lines** (one per sensor-suite level, `IntelPanelLayoutTest`; L2 and L3 shortened
from the draft to fit beside the level's rows):

| Sensor suite | Line |
|---|---|
| none | "Open Atlantic, Lancer. Something rides the current out there, on the water and under it." |
| L1 | "Mostly from ahead. Drifting organisms on the sea lanes: don't loiter over them." |
| L2 | "New: Driftjellies and Reef Spitters. And something huge." |
| L3 | "Anti-ground for rafts. Torpedoes for the sunken cache." |

**Radio script** ([Level 11](../../campaign/act-2-homefront/level-11-atlantic-convoy/README.md#radio-chatter);
retimed to the 1 s rule; the arena's lines are events because the level clock halts there):

| Trigger | Speaker | Line |
|---|---|---|
| t=1 | Atlas Control (text until cast, row 2c) | "Aegis flight, Atlas-Seven. Three hulls of reactor parts for the Arctic grid. We'd like to arrive with all three." |
| t=14 | Rook (`requires: escort`) | "Long way to swim, Atlas. We'll keep you dry." |
| t=21.5 | Varga | "Those jellies drift with the current. Don't hover over them: they pulse when you're close." |
| t=34 | Varga | "Some are under the surface, and they still pulse. Our guns can't reach them there. Torpedoes can." |
| t=55 | Rook (`requires: escort`) | "Lancer... did the sea just move under the convoy?" |
| t=62.5 | Atlas Control (text) | "Sonar has a contact. Big. Very big. It's gone deep again." |
| t=70.5 | Varga | "Gun rafts on the reef. Barnacle guns. Anti-ground rounds crack them twice as fast." |
| t=110 | Atlas Control (text) | "Platform Tiamat has been silent for six hours. Our route runs right past it." |
| t=140 | The Choir (distorted) | "[the Choir sings]" (its round-29 stage sound) |
| t=146.5 | Varga | "The contact is rising under the platform. Lancer, it's holding on to it." |
| t=158 | Rook (`requires: escort`) | "Here comes the big one." |
| First lane telegraph (`first-telegraph`) | Okafor | "Watch the churning water. That's where the arms come down." |
| First slam arm severed (`boss-part-destroyed`) | Rook (`requires: escort`) | "Tell the cook we're having calamari." |
| A cargo ship's first hit (`ally-hit`) | Atlas Control (text, a take per ship name) | "The {ally} is hit! Taking on water, but holding!" |
| A cargo ship sinks (`ally-lost`) | Atlas Control (text, a take per ship name; no grim expression) | "We've lost the {ally}. Crew in the water. Ruyter is picking them up." |
| Kraken destroyed (`boss-destroyed`) | Varga | "It's letting go of the platform. It's sinking. Good." |
| Secondary objective met | Atlas Control (text) | "All three hulls afloat. Drinks are on Atlas, Aegis." |
| Sunken pod freed (secret) | Varga | "A CDF supply pod. Good fishing, Lancer." |
| Level end, 1–3 ships afloat | Okafor | "Atlas-Seven is through. The Arctic grid gets its parts. Good work, Aegis." |
| Level end, **no ship afloat** | Okafor | "We lost Atlas-Seven, but the sea lane is open. Come home, Lancer." |

On easy a ship's second hit plays no line (only the first hit and the sinking do). There is no
mission failed line: the convoy never fails the mission.

**Names on screen**: the briefing's objective from sensor L1 `CONVOY TO OPEN WATER`; the tracker
**`CONVOY`**, one line with three ship pips (green; amber after a ship's first slam; a red flash
and then dark when sunk) and `DONE` or `FAILED` at the Kraken's death; the debrief's row
**`HULLS AFLOAT n / 3`** with the secondary's pay (+ 160 CR, Act 2 terms); the Torpedo Pod's idle
readout `NO WATER` (cannot show on Level 11).

### Part E numbers

From [Level 11](../../campaign/act-2-homefront/level-11-atlantic-convoy/README.md), the
[Harbour Kraken](../../enemies/bosses/harbour-kraken/README.md) and their data:

- **Length:** 140 px/s, 160 s of scroll to the halt at Tiamat, the fight, a 15 s exit.
- **Enemies per difficulty:** medium **254 before the arena** (the level's count including the rafts,
  the containers and the arena's jellies: 276 in the capture's run) and two popcorn waves after the
  Kraken: Skitter 110, Mote Swarm 96 (motes), Driftjelly 28, Needler 19, Stinger 4, plus 12 Reef
  Spitter rafts (hard 15, an extra nest of 3 at t≈140). Easy has 6 jellies in each of the t=50 and
  t=100 fields and the other waves by the formation lever; the totals per difficulty were not
  logged by the agents (see the report).
- **Density and pacing:** **98.3 enemies a minute** at medium (the minimum of Act 2 is 50), front
  98 % by count; `PacingTest`: no pause over 3 s on any difficulty except the quiet end.
- **Credit budget:** budget(11) = 700 × 1.07¹⁰ ≈ **1,377**. **`bounty_scale` 0.6** (the first
  estimate was 0.6–0.8): the typical haul **1,414** (+2.7 %, within ±5 %), a perfect run **2,118**
  (1.54 × budget). The data keeps Act 1 terms (Driftjelly 10, Reef Spitter 14, crate 100, secondary
  100, container salvage 10). The autopilot without a torpedo kills only about **half** of the
  jellies (12–18 of 28), ≈ 80 credits less than the model counts, but its whole haul is above the
  budget anyway, so the scale is not raised.
- **The Harbour Kraken:** HP **2,400** (head 1,700, arms 350 each; easy 1,800, hard 3,120);
  **bounty 216** in Act 1 terms (head 144, arms 36 each), paid **208**, **15 %** of the budget (it
  was 186, 13 %, until 2026-10-09); **par 60 s**.
- **Fight lengths** (autopilot, the plan's fit with Rook's Mortar, no torpedo; level runs, five
  seeds): the **arm-first** pilot **66.1 s** mean at medium (55–88; the fixture 58.6 s), **39.2 s**
  on easy in the earlier measurement, **≈ 120 s** on hard; the **eyes-first** pilot **64.1 s** at
  medium (the fixture 62.1 s). `BalanceTest`'s window at the plan's DPS: 57.4 s.
- **Cargo ships' HP** (slams to sink, by difficulty): easy **5**, medium **4** (was 2), hard **2**.
- **The convoy bonus** (secondary "Convoy afloat", all or nothing, 100 in Act 1 terms): arm-first
  medium **3 of 5** level runs with all three afloat (12 of 15 ships; the fixture 5 of 5), easy
  5 of 5; the **eyes-first** pilot **0 of 5** (10 of 15 ships). The typical haul counts the
  secondary at the economy's 50 %. The tests assert at least 3 of 5, so a level result of exactly
  3 of 5 is fragile.
- **Hard** (the user's earlier stance, "hard is supposed to be hard"): two slams sink a ship and no
  pilot of ours keeps one (**0 ships** afloat on every seed), the fight **≈ 120 s** mean with the
  pilot worn down, **2–5 attempts** (one seed 253 s), the secondary never met, the sunken pod needs
  five torpedo hits (six until your answer to (n)), the churn 0.8 s. The numbers stay as built unless you say otherwise.
- **The sunken pod:** a trigger of **3 torpedo hits** (hard 5; 4 and 6 until your answer to (n)) at t≈93.8, x 296; a Smart Bomb frees
  it at once; a crate of 100 (160 paid).
- **Radio** (`RadioTimelineTest`, seven runs with and without Rook): no voiced timed line starts
  more than 1 s late with the cast voices; Atlas Control's lines are timed as text.

### User decisions of part E (for the record)

Not to choose again; check they were carried out (row 8).

- [x] **E1 = a**: a level flag `water: true` (Levels 11–13); the torpedo reads it.
- [x] **E2 = a**: only `anti-sub` (and the Smart Bomb) reaches the `sub` layer.
- [x] **E3 = b**: submerged jellies fire their ring too, its pulse showing through the water.
- [x] **E4 = c**: the hybrid water drawing (a sub pass plus pre-rendered collars).
- [x] **E5 = a**: each slam arm owns half the lanes (the left arm lanes 1–2, the right 3–4).
- [x] **E6 = a**: the Kraken's HP tuned to a ≈ 60 s fight.
- [x] **E7 = a**: its two slam arms as runtime chains.
- [x] **E8 = a**: the secondary all or nothing at the Kraken's death.
- [x] **E9 = a**: art straight to production (rows 1a–1h), a/b only for the ship pair and the slam
  look (rows 2a–2b).
- [x] **E10 = a**: Atlas Control auditioned a/b (row 2c).
- [x] **The 28 stated defaults** (all applied): the data in Act 1 terms with a `bounty_scale`; the
  Kraken's paid bounty ≈ 15 %; popcorn to at least 50 a minute and the 3 s rule; the `field`
  formation; submerged units scrolling with the sea; each jelly's own seeded swap timer; torpedoes
  hitting `sub` and every `ground` unit over water at full damage; the Bomb Rack, mortars and
  Airstrike over water unchanged with a splash; the Smart Bomb hitting `sub`; Rook skipping `sub`
  and keeping out of telegraphed lanes; the plan refitting Rook's Mortar and buying no torpedo;
  lane choice nearest the player's x; the convoy's stations, the glide and the frigate dropping
  back; flak as presentation; the arena's jelly field released at the first surfacing; the sting at
  the halt; the music stems; the tracker and the debrief row; the sunken pod counting hits; the
  checkpoint keeping the ships; ship names as `{ally}`; flyers' shadows on the sea; two one-screen
  briefing pages, Rook's teaser, four intel lines; the radio retimed with the new events; `submerged
  ambush` retagged to Act 4.
- [x] **The convoy bonus measured, then tuned** (user, 2026-10-09): an arm-first autopilot first
  showed no pilot could keep the ships with two slams each; the lever is the cargo ships' HP (4 at
  medium).
- [x] **The four small fixes** (user, 2026-10-09): the ships hold clear of Tiamat, the Kraken's
  bounty ≈ 216 for 15 %, torpedoes seek sunken triggers, the rafts off the hulls (to the flanks).
- [x] **The capture's bugs fixed** (user, 2026-10-09): the head's flash, the torpedo's visibility,
  the foreshadow and the arms across the mantle; the Kraken's capture retaken.

### Build choices

Each **yes** (keep as built) or **no** (say what to change), row 9
([Level 11](../../campaign/act-2-homefront/level-11-atlantic-convoy/README.md),
[Kraken](../../enemies/bosses/harbour-kraken/README.md#behaviour),
[Driftjelly](../../enemies/naval/driftjelly/README.md),
[Reef Spitter](../../enemies/naval/reef-spitter/README.md),
[allies](../../allies/README.md#decisions),
[torpedo pod](../../player/weapons/torpedo-pod/README.md)):

- **(a) The water is drawn as a sub pass plus pre-rendered collars**: submerged things are drawn
  into a 480×540 buffer, swayed 0.7 px, blurred 3×3 and tinted, then the surface frames and the
  collars over it. Its look is **quiet**: the capture found the submerged jelly easy to miss.
- **(b) The Driftjelly's ring fires 72–96 px from the ship**; inside 72 px it waits, so hovering
  directly over a jelly is not punished at once.
- **(c) A field's first swap 0–10 s**, each jelly on its own seeded 6–10 s timer, about half the
  field starting submerged; the swap takes about 0.6 s with the layer flip at the midpoint.
- **(d) The rafts sit on the flanks** (x 46–58 and 430), off the convoy's hulls: an 84 px raft does
  not fit a 54 px gap between hulls.
- **(e) The Reef Spitter's first volley 1.0 s** after it enters.
- **(f) The arena eases to a halt over 1 s**; the boss bar, the sting and the checkpoint come **at
  arrival**, the engagement 1 s later.
- **(g) The slam arm counts as on the ground from its rise until its sinking**, so ground
  weapons hit it while it lies awash.
- **(h) The slam's splash is 6 bullets.**
- **(i) A shot under an open eye counts as hitting the eye** (the head's weak points while the
  eyes are open).
- **(j) The severed-half target rule** (the Kraken's lane choice once an arm is severed: see its
  [behaviour](../../enemies/bosses/harbour-kraken/README.md#behaviour); each arm owns half the lanes, E5 = a).
- **(k) The phase-3 volley rules**: the first volley 1 s after the phase begins, a busy arm sitting
  a volley out, a dive turned round into a rise; a phase ending on slams also ends with both arms
  severed (phase 2 then only skips its slam).
- **(l) The ships hold clear of Tiamat** after the Kraken: `hold_clear` 520 px, Halvorsen and
  Saint-Laurent in lanes 1 and 4, **Mbeki drops off the bottom edge**, then all glide back.
- **(m) Torpedoes seek sunken triggers** (a torpedo fired while the pod is on the screen seeks it).
- **(n) The sunken pod's hits 4 → 3?** One Torpedo Pod at L1 (0.8 torpedoes a second) frees the pod
  only if the pilot fires about a second before it enters; out of step it lands 3 of the 4 hits.
  Keep 4 (a reward for timing; a pod at L3 makes up the miss), or lower it to 3.
- **(o) The Kraken's hit flash**: at most once each 0.25 s, 2 steps, the head only **half white** so
  its eyes and beak stay readable.
- **(p) The torpedo's lit back drawn over the chop**, with a bubble trail, so it can be seen at 1:1.
- **(q) The Kraken's foreshadow** (t=52 for 7 s) drawn **over the chop** and under the convoy,
  untinted, peaking at 0.9.
- **(r) The head is opaque and 25 % darker while down** (the grip arms loop at the sides), instead
  of fainter.
- **(s) Atlas Control's sinking line without the grim expression** (neutral, as the ship is gone
  but the crew is picked up).
- **(t) Weak points are drawn only** (the jelly's and the raft's): no separate part; the raft is the
  gun's drawn base.
- **(u) Tiamat is 208×152 and the Kraken's head about 250 px** (mantle with arm roots), not the
  stat block's ≈ 190 px.
- **(v) `NO WATER` cannot show on Level 11** (all water): the readout is tested in the hangar's test
  range only.
- **(w) The jelly kill rate without torpedoes is about half**, and the scale is not raised for it.
- **(x) The reef growths stay faint**, so they read as scenery, not targets.
- **(y) The arms' under-water stretch is invisible at 1:1** (the capture's tell is the surface
  segments and the foam), kept as built.

## Listening

The 15 new voiced lines (row 5) as the game plays them, from `assets/voice/`, matched to their lines
by the voice key in `tools/art/voice.py`'s lines list; then the eight sounds (row 3) with their
listening points; then Atlas Control's audition clips (row 2c, in `design/audio/voice/refs/`, not a
`concept/` directory). The audition itself is on the board's cards.

| Line | Speaker | Text | Take |
|---|---|---|---|
| Briefing page 1 | Okafor | "The Arctic relays are running on reserve power, Lancer…" (listen for "Tiamat" and "Atlas-Seven") | [play](../../../assets/voice/okafor/a10c19b089ce.ogg) |
| Briefing page 2 | Varga | "The Vrell have seeded the sea lanes…" (listen for "Vrell") | [play](../../../assets/voice/varga/254a96760571.ogg) |
| t=14 | Rook | "Long way to swim, Atlas. We'll keep you dry." | [play](../../../assets/voice/rook/d1e022589af0.ogg) |
| t=21.5 | Varga | "Those jellies drift with the current. Don't hover over them: they pulse when you're close." | [play](../../../assets/voice/varga/fedfde124841.ogg) |
| t=34 | Varga | "Some are under the surface, and they still pulse. Our guns can't reach them there. Torpedoes can." | [play](../../../assets/voice/varga/1ce3d0b56639.ogg) |
| t=55 | Rook | "Lancer... did the sea just move under the convoy?" (pinned take) | [play](../../../assets/voice/rook/7a173798109d.ogg) |
| t=70.5 | Varga | "Gun rafts on the reef. Barnacle guns. Anti-ground rounds crack them twice as fast." | [play](../../../assets/voice/varga/d14b8c078654.ogg) |
| t=146.5 | Varga | "The contact is rising under the platform. Lancer, it's holding on to it." | [play](../../../assets/voice/varga/5c91944a59fb.ogg) |
| t=158 | Rook | "Here comes the big one." | [play](../../../assets/voice/rook/b88e0c037b44.ogg) |
| First lane telegraph | Okafor | "Watch the churning water. That's where the arms come down." | [play](../../../assets/voice/okafor/ed94e6b306a4.ogg) |
| First slam arm severed | Rook | "Tell the cook we're having calamari." (pinned take) | [play](../../../assets/voice/rook/4195e4cad552.ogg) |
| Kraken destroyed | Varga | "It's letting go of the platform. It's sinking. Good." | [play](../../../assets/voice/varga/0e9b164c8804.ogg) |
| Sunken pod freed | Varga | "A CDF supply pod. Good fishing, Lancer." | [play](../../../assets/voice/varga/3ba1d39ed81f.ogg) |
| Level end, 1–3 afloat | Okafor | "Atlas-Seven is through. The Arctic grid gets its parts. Good work, Aegis." (pinned take; listen for "Aegis": it still reads as "ages") | [play](../../../assets/voice/okafor/c496cbac570f.ogg) |
| Level end, no ship afloat | Okafor | "We lost Atlas-Seven, but the sea lane is open. Come home, Lancer." | [play](../../../assets/voice/okafor/057dd917c18a.ogg) |
| t=140 | The Choir | "[the Choir sings]" (round 29's stage sound, reused) | [play](../../../assets/voice/choir/voice-choir-sings-r29-b.ogg) |
| Sound: slam | Kraken | does the slam read as an arm coming down, not a footstep? (2.2 s) | [play](../../audio/sfx/concept/kraken-slam-r33-a.ogg) |
| Sound: churn | Kraken | does the churn read as a boiling lane before the arm? (1.0 s; hard's 0.8 s overruns 0.2 s into the rise) | [play](../../audio/sfx/concept/kraken-churn-r33-a.ogg) |
| Sound: surfacing | Kraken | a deep swell peaking as the crown breaks (2.75 s) | [play](../../audio/sfx/concept/kraken-surface-r33-a.ogg) |
| Sound: death | Kraken | does the groan read as a creature, not static? (5.5 s) | [play](../../audio/sfx/concept/kraken-death-r33-a.ogg) |
| Sound: ship hit | Cargo ship | metal struck, water rushing in (0.94 s) | [play](../../audio/sfx/concept/ship-hit-r33-a.ogg) |
| Sound: ship sinking | Cargo ship | do the creaks read as a hull going down? (4.2 s) | [play](../../audio/sfx/concept/ship-sink-r33-a.ogg) |
| Sound: flak | Frigate | two dull far pops, quiet: does it read as flak? (1.6 s) | [play](../../audio/sfx/concept/frigate-flak-r33-a.ogg) |
| Sound: jelly pulse | Driftjelly | a synthesized bloop and ripple: keep, or reuse the enemy shot sound? (0.9 s) | [play](../../audio/sfx/concept/driftjelly-pulse-r33-a.ogg) |
| Audition clip, Atlas Control a | Alister | the reader's LibriVox clip, 106 Hz (not picked: deleted at the close with its CREDITS.md row) | — |
| Audition clip, Atlas Control b | MaryAnn | the reader's LibriVox clip, 174 Hz, picked: renamed at the close ([transcript](../../audio/voice/refs/ref-atlas-control.txt)) | [play](../../audio/voice/refs/ref-atlas-control.wav) |

## Notes

- Review file names: `<subject>-final-r33-a.png/.gif` (production art), `level-11-capture-final-r33-a`,
  `kraken-capture-final-r33-a`, `torpedo-capture-r33-a` and `level-11-readability-r33-a` (game
  captures), and `<subject>-r33-a/b` (the a/b picks and the eight sounds), in each part's
  `concept/`. Their `prompts.md` entries say how they are made. Nothing in `assets/` is hand-edited.
- The board shows the three videos (`level-11-capture-final-r33-a.mp4`,
  `kraken-capture-final-r33-a.mp4`, `torpedo-capture-r33-a.mp4`) as links ("open"). The audition
  clips live in `design/audio/voice/refs/`, not in a `concept/` directory, so the board shows them
  in the *Listening* table, not on cards.
- Which captures predate the fix pass of 2026-10-09: the level, the torpedo run and the readability
  crops; only the Kraken's was retaken.
- After the choices (at the close):
  - the approved parts get `art: final`; the rejected files move to `concept/rejected/`;
  - ship pair b: `python3 tools/art/convoy_ships.py --variant b`, then re-run
    `python3 tools/art/briefing_images.py level-11-convoy-route`;
  - slam b: `python3 tools/art/water_fx.py --variant b`;
  - the eight sounds go into `sfx_r33.py`'s `PRODUCTION`, are imported by `sfx_originals.py`
    and get `assets/sfx/` rows in CREDITS.md; drop their `copyPlaceholderSounds` entries in
    `pipeline/build.gradle.kts` (the jelly pulse is synthesized: keep a copy of the file);
  - Atlas Control: rename the chosen clip to `refs/ref-atlas-control.wav`, replace `uncast` in
    the voice data with the ref, delete the loser's clip and its CREDITS.md row, render his lines
    (including the per-ship-name takes of the `{ally}` lines) and empty `AUDITIONING`;
  - Level 11's design leaves `review` once the texts are approved.

## Decisions

- 2026-10-09: Opened with M5 part E.
- 2026-10-09: Closed (user verdict on every row): the production art approved as final (rows
  1a–1h), the Harbour Kraken after a rework on the user's feedback (the head surfacing and sinking
  off the deck's south edge with Platform Tiamat raised, flesh hits, whipping arms, a part-only
  flash; its capture retaken and accepted); the convoy ship pair **a** "Atlantic line" (b "Reactor
  run" rejected); the lane telegraph and slam **a** "edge dashes + spray sheets" (b "lane boil,
  chevrons, rollers" rejected); Atlas Control's voice **b** (MaryAnn; a, Alister, rejected), cast and
  her ten takes rendered; the eight sounds accepted (churn, death and sinking reworked without
  bubbles, the flak raised twice to −16 dB), seven produced into `assets/sfx/`; the captures, the 15
  voiced lines, the texts (Level 11 now `approved`), the part E numbers (hard kept as built), the
  decisions and the build choices (a)–(y) accepted as built, but (n): the sunken pod takes 3 torpedo
  hits (hard 5). The rejected files are in each part's `concept/rejected/`.
