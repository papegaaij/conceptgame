---
title: Concept round 16 — M4 part C, Level 03
design: review
implementation: n/a
art: proposed
updated: 2026-10-02
---

# Concept round 16 — M4 part C, Level 03

## Summary

The production-art round of M4 part C ([roadmap](../../tech/roadmap/README.md#m4-parts)): Level
03's new units, their death effects, debris, pickup, backdrop and hangar intel pictures, rendered
as **final** art by the generators in [tools/art/](../../../tools/art/README.md) into `assets/`, and game captures of
the level, plus the Leviathan's synthesized death cry. Rows 13–15 were added overnight as a head start on part D (Level 04): production proposals for the Brood Pod and the Scuttler, which still depend on part D's doc gaps, and the civilian crawler's first concept variants. Open [index.html](index.html) in a browser (regenerate with
`python3 tools/concept/board.py 16`). Every choice is *approve as final* or *redo* (say what to
change); only an approved part gets `art: final`. To fly the level:
`./gradlew :desktop:run --args="--level 3"`.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Spore Bomber](../../enemies/air/spore-bomber/README.md) | the 4-frame idle loop (72×72, the gas bag breathing, the bulbs pulsing) and the spore mine's additive pulse, also as drawn while it rises (`tools/art/vrell_l03.py bomber`) | `spore-bomber-final-r16-a` (png, gif), game captures | in the game the spores are small (14 px, 10 px while rising) and read faintly over the green land | |
| 2 | [Whirl Seed](../../enemies/air/whirl-seed/README.md) | 8 spin frames over 60°, each its own render; six blades (already decided) so the spin loops (`tools/art/vrell_l03.py seed`) | `whirl-seed-final-r16-a` (png, gif), game captures | the plum seeds read dim over the dark sea; released under the Leviathan's first pass they stay veiled by its body until they spiral out | |
| 3 | [Debris](../../world/earth-orbit/README.md) | the debris field's chunks: three large (indestructible) and two small (breakable) pieces of scorched station kit and frigate wreckage (`tools/art/vrell_l03.py debris`) | `debris-final-r16-a`, game captures | the two small chunks are grey and low in contrast at 1× | |
| 4 | [Leviathan](../../enemies/space/leviathan/README.md) | the second pass facing down (3 tail-sway frames) with its parts intact and wrecked and the blowhole glow, and the first pass crossing diagonally at 1.25× (`tools/art/leviathan.py`) | `leviathan-final-r16-a` (png, gif), game captures | the layout follows the data's part offsets (a long head, a short tail), not the concept's proportions; the first pass's sway is subtle; the first pass has no wrecked parts (it cannot be hit); in the captures a fin under fire flashes as a flat white cut-out | |
| 5 | [Level 03 backdrop](../../campaign/act-1-first-contact/level-03-spore-drift/README.md) | the high lanes over the first battle's wreckage: the broken *Kestrel* with its lifeboat rack and light, platform halves, wrecks, the burnt-out tug, beacons, buoys, the defence ring, spore haze and banks, ice and spore streaks, cloud fronts (`tools/art/backdrop_l03.py`) | `backdrop-final-r16-a`, game captures | Earth's "full disc" is Level 01's day-side tile with cloud fronts over it, not a disc; the Earth tile and the station kit are Level 01's unchanged | |
| 6 | [Large salvage](../../player/README.md) | the 200-credit drop of a set piece: a raised chip in a ring of six, rocking, with the other pickups' light outline and halo (`tools/art/pickups.py salvage-large`) | `pickups-final-r16-a` (png, gif) | in the edge-on frames of the rock the chips turn dark | |
| 7 | [Intel pictures](../../ui/hangar/README.md) | the 30×30 sensor portraits of the Spore Bomber and the six-bladed Whirl Seed, and the Leviathan's 40×40 "unknown huge contact" silhouette (`tools/art/intel.py spore-bomber whirl-seed boss-leviathan`) | `intel-final-r16-a` | the bomber's portrait is narrow in its 30×30 frame | |
| 8 | [Level 03 in the game](../../campaign/act-1-first-contact/level-03-spore-drift/README.md) | game captures with `--level 3`: the first Spore Bomber with its spores and the `LOW-AIR` prompt, the debris field with the *Kestrel* and its lifeboat lights, the Leviathan's first pass with seed clusters, the Spore Bloom's heavy banks, the second pass with wrecked parts, the clear lane with the defence ring | `level-03-capture-final-r16-a` | with the ship left under it, the introduction bomber dies about a second after it enters, before its spores rise (the capture moves the ship aside) | |
| 9 | [Spore Bomber death](../../enemies/air/spore-bomber/README.md) | the death with the `medium` burst: an additive lime spore cloud (16 frames, 96×96, 4 steps each) over solid olive membrane tatters drawn under the glows (16 frames, 96×96) (`tools/art/vrell_fx.py`) | `spore-bomber-death-final-r16-a` (png, gif) | in the game the tatters are dark olive and read faintly over the green land once the cloud has faded | |
| 10 | [Whirl Seed death](../../enemies/air/whirl-seed/README.md) | the death with the `tiny` pop: the solid husk splitting (12 frames, 40×40, 2 steps each) and an additive teal glint 4 steps after the pop (8 frames, 32×32) (`tools/art/vrell_fx.py`) | `whirl-seed-death-final-r16-a` (png, gif) | at 1× the glint is small and short (0.27 s) and hard to tell from the vulcan's teal bolts; seeds that die under the Leviathan's first pass are veiled by its body | |
| 11 | [Leviathan death](../../enemies/space/leviathan/README.md) | the ichor cloud: one round additive set (16 frames, 160×160, 6 steps each) at every part with its chained `medium` burst and at the centre with the large burst, scaled and faded with the unit off the play plane (`tools/art/vrell_fx.py`) | `leviathan-death-final-r16-a` (png, gif) | the body vanishes the moment it dies, so the clouds and bursts carry the whole death; off the plane (only reachable with a homing weapon, none at L03) it is untested in the game | |
| 12 | [Leviathan whale-song cry](../../audio/sfx/README.md) | a synthesized deep alien call (3 s with its reverb), played under the explosion when the Leviathan dies (`tools/concept/audio/sfx_r16.py`) | `enemy-leviathan-cry-r16-a` (ogg) | a **placeholder**: the user decided on 2026-10-01 that enemy sounds are recorded CC0/CC-BY, so accept this one as an exception or have it replaced by a recorded one | |
| 13 | [Brood Pod](../../enemies/air/brood-pod/README.md) | **part D proposal** (pending part D's doc gaps): 64×64 `fixed`, an 8-frame pulse loop with the veins and eye brightening on each swell (played faster before it bursts), a 12-frame additive wet burst and solid membrane tatters (96×96) (`tools/art/vrell_l04.py`) | `brood-pod-final-r16-a` (png, gif) | the tatters are small and dark and read faintly at 1×; the game looks up `<slug>-death`, so the burst set needs that name when it is wired in |  |
| 14 | [Scuttler](../../enemies/ground/scuttler/README.md) | **part D proposal** (pending part D's doc gaps): the chosen r08 walker at 16 headings × a 6-frame tripod walk (64×64, 96 frames, each its own render), 16 legless husks for its remains, additive lime-back glow masks to show through the dust (`tools/art/vrell_l04.py`) | `scuttler-final-r16-a` (png, gif) | a 24 px stride halves the concept's leg swing, so the walk reads subtly; the lime back is a thin seam (a small weak point); the husk set needs the remains naming (`-stump` style) when wired in |  |
| 15 | [Civilian crawler](../../allies/README.md) | **concept** (Level 04's escort, no art yet): (a) tracked crawler-transporter 72×40, (b) three-car rover train 72×40, (c) pressurised bus with a cargo sled 40×72 (the long size reading); each as a column of five on a road, a close-up, damaged and wrecked (`tools/concept/allies_r16.py`) | `civilian-crawler-r16-a`, `-b`, `-c` (png) | pick a variant **and** the size reading; amber beacons on a ground ally next to the rule that keeps warm amber for loot; (c) needs 84 px spacing instead of 60; the chosen Luna road (18–28 px) must widen for any variant |  |

## Notes

- Review file names: `<subject>-final-r16-a.png/.gif` in each part's `concept/`; their `prompts.md`
  entries say how they are made. Nothing in `assets/` is hand-edited.
- The game captures are whole-window frames, since the layer prompts and the bomber tracker are in
  the HUD's side panels; [prompts.md](../../campaign/act-1-first-contact/level-03-spore-drift/concept/prompts.md#level-03-capture-final-r16-a)
  says how they were made.

## Decisions

- 2026-10-02: Opened with M4 part C.
- 2026-10-02: The death effects of the Spore Bomber, the Whirl Seed and the Leviathan and the
  Leviathan's cry, deferred to the art track at the opening, are added as choices 9–12; they are
  wired into the game.
