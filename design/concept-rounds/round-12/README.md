---
title: Concept round 12 — Level 01 production batch
design: approved
implementation: n/a
art: chosen
updated: 2026-10-02
---

# Concept round 12 — Level 01 production batch

## Summary

The first batch of **final** art ([production plan](../../art-direction/production/README.md)):
everything Level 01 draws and plays, rendered by the generators in
[tools/art/](../../../tools/art/README.md) into `assets/`. Open [index.html](index.html) in a
browser (regenerate with `python3 tools/concept/board.py 12`): one review sheet and GIF per part,
built from the files the game loads, a game capture, and a before / after listening table for the
recorded sounds. Every choice is *approve as final* or *redo* (with what to change); only an
approved part gets `art: final`.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Stormhawk](../../player/ship/README.md) | 5 banking frames (48×48, 32 colours), the five wing-pod types per frame with pivots, the 3 × 3 engine flame. **P3:** roll ±15° / ±30° seen through a mild perspective camera (the raised wing grows, the lowered one shrinks); pods in lighter, shaded metal | `player-ship-final-r12-a` | the hull's hit boxes (`hull` in data.yaml, from the old hard-bank frame) now cover about 1 px of empty space at the lowered wingtip in the hard-bank frames; left unchanged since it is gameplay | approved — final; hit boxes unchanged (user decision: the ~1 px is negligible) |
| 2 | [Skitter](../../enemies/air/skitter/README.md) | **P3:** `16 angles` (user decision): 16 headings × the 6-frame wing beat (96 frames, 24×24, 24 colours); the wings lift and sweep, so the beat changes the shape; in the game each Skitter shows the heading nearest its direction of flight | `skitter-final-r12-a` | at the raised-wing frames the wings are thin slivers in the diagonal headings | approved — final |
| 3 | [Needler](../../enemies/air/needler/README.md) | 6-frame claw snap (36×36, 32 colours), `fixed`. **P3:** wider swing, opening over three frames and snapping shut in one | `needler-final-r12-a` | none seen | approved — final |
| 4 | [Pulse cannon](../../player/weapons/pulse-cannon/README.md) | bolt (14×26), 3-frame muzzle flash, 4-frame impact, additive. **P3:** the impact's fade rebalanced (first 4 of 5 steps) | `pulse-cannon-final-r12-a` | the impact's last sparks are dim blue-grey, but visible | approved — final |
| 5 | [Vrell bullets](../../enemies/README.md) | orb with a 4-frame core pulse (15×15), needle in 16 headings (23×23). **P3:** the needle's deep band saturated gold, its halo cut to a tight 2/3 band | `enemy-bullets-final-r12-a` | none seen | approved — final |
| 6 | [Pickups](../../player/README.md) | small salvage, shield cell, armour patch, crate: 8-frame spin loops (28–34 px). **P3:** the crate's cross on every face it turns to the viewer | `pickups-final-r12-a` | none seen | approved — final |
| 7 | [Explosions](../../art-direction/README.md) | the ladder tiny / small / medium / large (24–96 px, 12–14 frames, additive) | `explosions-final-r12-a` | none seen (the sheet cuts the large row's last frame; the GIF shows it) | approved — final |
| 8 | [Loot targets](../../campaign/act-1-first-contact/level-01-break-at-dawn/README.md) | cargo container intact / damaged, its 8-frame break-apart, the beacon (4 states), the glint. **P3:** the pieces crumble from their edges over the last three frames | `loot-targets-final-r12-a` | the last frame leaves only a few embers | approved — final |
| 9 | [Level 01 backdrop](../../campaign/act-1-first-contact/level-01-break-at-dawn/README.md) | every tile set and set piece at the production bar (12–32 colours, dithered limb and Vrell glow, 4× Moon, Aegis Two through the sprite path), the four mirrored placements as their own renders | `backdrop-final-r12-a`, `game-capture-final-r12-a` (retaken in P3 with the reworked sprites) | the layouts, the Earth tile and the bright solar panels are the placeholder's; Aegis Two is now 18 px (the far scale of the 48 px ship) instead of 22 | approved — final |
| 10 | [Recorded SFX](../../audio/sfx/README.md) | the 75 chosen recorded sounds rebuilt from the Freesound originals with their unchanged settings (`tools/art/sfx_originals.py`) | *Listening* below | `enemy-shot-small-r08-b` peaks 1.7 dB lower at the same band level; 13 originals above full scale are clipped first, as the previews were | approved — final |

## Notes

- Review file names: `<subject>-final-r12-a.png/.gif` in each part's `concept/`; their
  `prompts.md` entries say how they are made. Nothing in `assets/` is hand-edited.
- The build skips final files when it imports placeholders (`Source` PNG chunk, `SOURCE` OGG
  comment) and checks the atlas budgets: Level 01's backdrop packs into a 2048² and a 1024×2048
  page (24 MiB of 96), the shared sprites into one 2048×256 page (2 MiB of 32).
- Level 01's data now names `-mirrored` pieces where it used `mirror: true` (dock frame, bridge
  crane, crane jib, burning platform), so nothing lit is drawn mirrored (symmetry rule).

## Listening

Each recorded sound as chosen (from the Freesound HQ preview) and as rebuilt from the original
file; length, sample peak and the 200 Hz–5 kHz band RMS the sounds are levelled on. All match
within 1 dB except the noted peak.

| Sound | Chosen (preview) | Final (original) | Length s | Peak dBFS chosen → final | Band RMS dB chosen → final | Note |
|---|---|---|---|---|---|---|
| player-shot-r02-a | [chosen](../../audio/sfx/concept/player-shot-r02-a.ogg) | [final](../../../assets/sfx/player-shot-r02-a.ogg) | 0.25 | -10.0 → -10.1 | -30.3 → -30.9 | in the game |
| player-shot-r02-b | [chosen](../../audio/sfx/concept/player-shot-r02-b.ogg) | [final](../../../assets/sfx/player-shot-r02-b.ogg) | 0.28 | -10.2 → -10.1 | -29.2 → -28.8 |  |
| player-shot-r02-c | [chosen](../../audio/sfx/concept/player-shot-r02-c.ogg) | [final](../../../assets/sfx/player-shot-r02-c.ogg) | 0.27 | -10.8 → -10.2 | -27.4 → -26.9 |  |
| player-shot-r02-d | [chosen](../../audio/sfx/concept/player-shot-r02-d.ogg) | [final](../../../assets/sfx/player-shot-r02-d.ogg) | 0.28 | -10.2 → -10.3 | -24.5 → -24.1 | original clipped at full scale |
| player-shot-r02-e | [chosen](../../audio/sfx/concept/player-shot-r02-e.ogg) | [final](../../../assets/sfx/player-shot-r02-e.ogg) | 0.26 | -10.0 → -10.6 | -25.6 → -25.6 |  |
| explosion-r02-a | [chosen](../../audio/sfx/concept/explosion-r02-a.ogg) | [final](../../../assets/sfx/explosion-r02-a.ogg) | 0.70 | -1.8 → -1.7 | -22.6 → -22.5 | in the game |
| explosion-r02-b | [chosen](../../audio/sfx/concept/explosion-r02-b.ogg) | [final](../../../assets/sfx/explosion-r02-b.ogg) | 0.90 | -1.5 → -2.1 | -22.3 → -23.1 |  |
| explosion-r02-c | [chosen](../../audio/sfx/concept/explosion-r02-c.ogg) | [final](../../../assets/sfx/explosion-r02-c.ogg) | 1.70 | -1.6 → -1.6 | -23.4 → -23.3 |  |
| explosion-r02-d | [chosen](../../audio/sfx/concept/explosion-r02-d.ogg) | [final](../../../assets/sfx/explosion-r02-d.ogg) | 2.80 | -1.6 → -1.6 | -24.0 → -23.7 |  |
| explosion-r02-e | [chosen](../../audio/sfx/concept/explosion-r02-e.ogg) | [final](../../../assets/sfx/explosion-r02-e.ogg) | 2.86 | -1.7 → -1.7 | -21.6 → -21.1 | original clipped at full scale |
| shot-vulcan-r03-b | [chosen](../../audio/sfx/concept/shot-vulcan-r03-b.ogg) | [final](../../../assets/sfx/shot-vulcan-r03-b.ogg) | 0.30 | -10.1 → -10.0 | -22.9 → -22.9 |  |
| shot-laser-r03-b | [chosen](../../audio/sfx/concept/shot-laser-r03-b.ogg) | [final](../../../assets/sfx/shot-laser-r03-b.ogg) | 0.30 | -10.2 → -10.2 | -27.0 → -27.0 |  |
| shot-missile-r03-a | [chosen](../../audio/sfx/concept/shot-missile-r03-a.ogg) | [final](../../../assets/sfx/shot-missile-r03-a.ogg) | 0.90 | -8.2 → -8.1 | -27.5 → -26.8 |  |
| shot-micromissile-r03-a | [chosen](../../audio/sfx/concept/shot-micromissile-r03-a.ogg) | [final](../../../assets/sfx/shot-micromissile-r03-a.ogg) | 0.45 | -10.1 → -10.3 | -31.8 → -31.9 | original clipped at full scale |
| shot-mortar-r03-a | [chosen](../../audio/sfx/concept/shot-mortar-r03-a.ogg) | [final](../../../assets/sfx/shot-mortar-r03-a.ogg) | 1.00 | -8.6 → -8.6 | -21.9 → -21.4 | original clipped at full scale |
| shot-bomb-r03-a | [chosen](../../audio/sfx/concept/shot-bomb-r03-a.ogg) | [final](../../../assets/sfx/shot-bomb-r03-a.ogg) | 1.20 | -10.2 → -10.2 | -24.1 → -24.2 |  |
| shot-torpedo-r03-a | [chosen](../../audio/sfx/concept/shot-torpedo-r03-a.ogg) | [final](../../../assets/sfx/shot-torpedo-r03-a.ogg) | 1.00 | -8.0 → -8.0 | -26.7 → -26.7 |  |
| shot-mine-r03-a | [chosen](../../audio/sfx/concept/shot-mine-r03-a.ogg) | [final](../../../assets/sfx/shot-mine-r03-a.ogg) | 0.90 | -8.0 → -8.0 | -34.8 → -35.0 |  |
| shot-tesla-r03-a | [chosen](../../audio/sfx/concept/shot-tesla-r03-a.ogg) | [final](../../../assets/sfx/shot-tesla-r03-a.ogg) | 0.22 | -10.0 → -10.4 | -38.1 → -37.5 |  |
| shot-resonator-r03-a | [chosen](../../audio/sfx/concept/shot-resonator-r03-a.ogg) | [final](../../../assets/sfx/shot-resonator-r03-a.ogg) | 0.60 | -8.1 → -8.0 | -31.5 → -31.3 |  |
| explosion-tiny-r03-a | [chosen](../../audio/sfx/concept/explosion-tiny-r03-a.ogg) | [final](../../../assets/sfx/explosion-tiny-r03-a.ogg) | 0.45 | -4.1 → -4.2 | -22.1 → -22.1 | in the game |
| explosion-tiny-r03-b | [chosen](../../audio/sfx/concept/explosion-tiny-r03-b.ogg) | [final](../../../assets/sfx/explosion-tiny-r03-b.ogg) | 0.60 | -4.1 → -4.2 | -18.6 → -17.9 | in the game |
| explosion-small-r03-a | [chosen](../../audio/sfx/concept/explosion-small-r03-a.ogg) | [final](../../../assets/sfx/explosion-small-r03-a.ogg) | 1.10 | -1.6 → -1.5 | -29.6 → -29.5 | in the game |
| explosion-medium-r03-b | [chosen](../../audio/sfx/concept/explosion-medium-r03-b.ogg) | [final](../../../assets/sfx/explosion-medium-r03-b.ogg) | 2.00 | -1.6 → -1.7 | -29.2 → -29.4 |  |
| explosion-large-r03-a | [chosen](../../audio/sfx/concept/explosion-large-r03-a.ogg) | [final](../../../assets/sfx/explosion-large-r03-a.ogg) | 3.10 | -1.6 → -1.6 | -21.4 → -21.4 | original clipped at full scale |
| explosion-huge-r03-b | [chosen](../../audio/sfx/concept/explosion-huge-r03-b.ogg) | [final](../../../assets/sfx/explosion-huge-r03-b.ogg) | 6.00 | -1.1 → -1.2 | -35.6 → -35.6 |  |
| explosion-underwater-r03-a | [chosen](../../audio/sfx/concept/explosion-underwater-r03-a.ogg) | [final](../../../assets/sfx/explosion-underwater-r03-a.ogg) | 3.00 | -1.5 → -1.5 | -22.7 → -22.7 |  |
| explosion-water-r03-a | [chosen](../../audio/sfx/concept/explosion-water-r03-a.ogg) | [final](../../../assets/sfx/explosion-water-r03-a.ogg) | 1.90 | -1.7 → -1.8 | -31.0 → -30.9 |  |
| shot-beam-r04-a | [chosen](../../audio/sfx/concept/shot-beam-r04-a.ogg) | [final](../../../assets/sfx/shot-beam-r04-a.ogg) | 2.60 | -23.1 → -23.1 | -30.0 → -30.0 |  |
| shot-beam-r04-b | [chosen](../../audio/sfx/concept/shot-beam-r04-b.ogg) | [final](../../../assets/sfx/shot-beam-r04-b.ogg) | 1.90 | -9.4 → -9.5 | -30.0 → -30.0 |  |
| shot-beam-r04-c | [chosen](../../audio/sfx/concept/shot-beam-r04-c.ogg) | [final](../../../assets/sfx/shot-beam-r04-c.ogg) | 2.00 | -17.2 → -17.1 | -30.2 → -30.2 |  |
| shot-beam-start-r04-a | [chosen](../../audio/sfx/concept/shot-beam-start-r04-a.ogg) | [final](../../../assets/sfx/shot-beam-start-r04-a.ogg) | 0.90 | -17.9 → -17.9 | -30.0 → -30.1 |  |
| shot-beam-stop-r04-a | [chosen](../../audio/sfx/concept/shot-beam-stop-r04-a.ogg) | [final](../../../assets/sfx/shot-beam-stop-r04-a.ogg) | 1.30 | -10.0 → -10.0 | -29.9 → -29.9 |  |
| special-sonar-r04-a | [chosen](../../audio/sfx/concept/special-sonar-r04-a.ogg) | [final](../../../assets/sfx/special-sonar-r04-a.ogg) | 3.00 | -4.2 → -4.1 | -21.6 → -21.5 |  |
| explosion-huge-r04-a | [chosen](../../audio/sfx/concept/explosion-huge-r04-a.ogg) | [final](../../../assets/sfx/explosion-huge-r04-a.ogg) | 6.00 | -1.1 → -1.2 | -23.9 → -23.6 | original clipped at full scale |
| explosion-underwater-r04-a | [chosen](../../audio/sfx/concept/explosion-underwater-r04-a.ogg) | [final](../../../assets/sfx/explosion-underwater-r04-a.ogg) | 3.50 | -1.7 → -1.5 | -19.0 → -18.8 |  |
| hit-metal-r08-a | [chosen](../../audio/sfx/concept/hit-metal-r08-a.ogg) | [final](../../../assets/sfx/hit-metal-r08-a.ogg) | 0.30 | -13.9 → -14.8 | -30.0 → -30.0 | in the game |
| hit-metal-r08-b | [chosen](../../audio/sfx/concept/hit-metal-r08-b.ogg) | [final](../../../assets/sfx/hit-metal-r08-b.ogg) | 0.45 | -16.0 → -15.6 | -30.1 → -30.1 | in the game; original clipped at full scale |
| hit-organic-r08-a | [chosen](../../audio/sfx/concept/hit-organic-r08-a.ogg) | [final](../../../assets/sfx/hit-organic-r08-a.ogg) | 0.30 | -8.1 → -8.8 | -31.3 → -32.0 | in the game; original clipped at full scale |
| hit-organic-r08-b | [chosen](../../audio/sfx/concept/hit-organic-r08-b.ogg) | [final](../../../assets/sfx/hit-organic-r08-b.ogg) | 0.40 | -8.2 → -8.0 | -32.4 → -31.9 | in the game |
| hit-crumble-r08-a | [chosen](../../audio/sfx/concept/hit-crumble-r08-a.ogg) | [final](../../../assets/sfx/hit-crumble-r08-a.ogg) | 2.40 | -1.5 → -1.6 | -25.3 → -25.2 |  |
| hit-crumble-r08-b | [chosen](../../audio/sfx/concept/hit-crumble-r08-b.ogg) | [final](../../../assets/sfx/hit-crumble-r08-b.ogg) | 1.60 | -5.8 → -6.0 | -25.0 → -25.0 |  |
| player-shield-hit-r08-a | [chosen](../../audio/sfx/concept/player-shield-hit-r08-a.ogg) | [final](../../../assets/sfx/player-shield-hit-r08-a.ogg) | 0.50 | -2.2 → -2.5 | -26.3 → -26.8 | in the game |
| player-shield-break-r08-a | [chosen](../../audio/sfx/concept/player-shield-break-r08-a.ogg) | [final](../../../assets/sfx/player-shield-break-r08-a.ogg) | 1.40 | -14.4 → -14.3 | -23.9 → -23.9 | in the game |
| player-shield-restore-r08-a | [chosen](../../audio/sfx/concept/player-shield-restore-r08-a.ogg) | [final](../../../assets/sfx/player-shield-restore-r08-a.ogg) | 0.80 | -2.3 → -2.2 | -24.7 → -24.4 | original clipped at full scale |
| player-armour-hit-r08-a | [chosen](../../audio/sfx/concept/player-armour-hit-r08-a.ogg) | [final](../../../assets/sfx/player-armour-hit-r08-a.ogg) | 0.60 | -10.9 → -11.5 | -24.0 → -24.1 | in the game; original clipped at full scale |
| player-low-armour-r08-a | [chosen](../../audio/sfx/concept/player-low-armour-r08-a.ogg) | [final](../../../assets/sfx/player-low-armour-r08-a.ogg) | 0.40 | -11.9 → -11.9 | -23.9 → -23.9 |  |
| player-destroyed-r08-a | [chosen](../../audio/sfx/concept/player-destroyed-r08-a.ogg) | [final](../../../assets/sfx/player-destroyed-r08-a.ogg) | 3.50 | -1.2 → -1.2 | -21.4 → -21.3 | in the game |
| overdrive-start-r08-a | [chosen](../../audio/sfx/concept/overdrive-start-r08-a.ogg) | [final](../../../assets/sfx/overdrive-start-r08-a.ogg) | 0.60 | -9.0 → -8.9 | -23.9 → -24.0 |  |
| overdrive-end-r08-a | [chosen](../../audio/sfx/concept/overdrive-end-r08-a.ogg) | [final](../../../assets/sfx/overdrive-end-r08-a.ogg) | 1.10 | -2.1 → -2.2 | -27.5 → -27.6 |  |
| enemy-shot-small-r08-a | [chosen](../../audio/sfx/concept/enemy-shot-small-r08-a.ogg) | [final](../../../assets/sfx/enemy-shot-small-r08-a.ogg) | 0.45 | -8.2 → -8.3 | -32.6 → -32.1 | in the game |
| enemy-shot-small-r08-b | [chosen](../../audio/sfx/concept/enemy-shot-small-r08-b.ogg) | [final](../../../assets/sfx/enemy-shot-small-r08-b.ogg) | 0.32 | -19.2 → -20.9 | -30.2 → -30.1 | in the game; peak 1.7 dB lower at the same band RMS (band-levelled) |
| enemy-shot-heavy-r08-a | [chosen](../../audio/sfx/concept/enemy-shot-heavy-r08-a.ogg) | [final](../../../assets/sfx/enemy-shot-heavy-r08-a.ogg) | 0.75 | -9.6 → -10.3 | -30.1 → -30.2 | original clipped at full scale |
| enemy-shot-heavy-r08-b | [chosen](../../audio/sfx/concept/enemy-shot-heavy-r08-b.ogg) | [final](../../../assets/sfx/enemy-shot-heavy-r08-b.ogg) | 0.90 | -16.9 → -16.8 | -30.2 → -30.2 |  |
| enemy-laser-warning-r08-a | [chosen](../../audio/sfx/concept/enemy-laser-warning-r08-a.ogg) | [final](../../../assets/sfx/enemy-laser-warning-r08-a.ogg) | 1.20 | -11.4 → -11.5 | -30.0 → -30.0 |  |
| enemy-missile-r08-a | [chosen](../../audio/sfx/concept/enemy-missile-r08-a.ogg) | [final](../../../assets/sfx/enemy-missile-r08-a.ogg) | 1.60 | -16.8 → -16.8 | -30.1 → -30.1 |  |
| enemy-screech-r08-c | [chosen](../../audio/sfx/concept/enemy-screech-r08-c.ogg) | [final](../../../assets/sfx/enemy-screech-r08-c.ogg) | 1.60 | -18.6 → -18.4 | -30.0 → -30.0 |  |
| enemy-screech-r08-d | [chosen](../../audio/sfx/concept/enemy-screech-r08-d.ogg) | [final](../../../assets/sfx/enemy-screech-r08-d.ogg) | 2.00 | -12.1 → -12.2 | -30.1 → -30.1 |  |
| enemy-spawn-r08-a | [chosen](../../audio/sfx/concept/enemy-spawn-r08-a.ogg) | [final](../../../assets/sfx/enemy-spawn-r08-a.ogg) | 1.60 | -14.4 → -13.9 | -30.0 → -30.0 |  |
| enemy-spawn-r08-b | [chosen](../../audio/sfx/concept/enemy-spawn-r08-b.ogg) | [final](../../../assets/sfx/enemy-spawn-r08-b.ogg) | 1.70 | -11.2 → -11.1 | -30.2 → -30.1 | original clipped at full scale |
| enemy-lock-r08-a | [chosen](../../audio/sfx/concept/enemy-lock-r08-a.ogg) | [final](../../../assets/sfx/enemy-lock-r08-a.ogg) | 1.06 | -20.8 → -20.6 | -30.0 → -30.0 |  |
| special-airstrike-jets-r08-a | [chosen](../../audio/sfx/concept/special-airstrike-jets-r08-a.ogg) | [final](../../../assets/sfx/special-airstrike-jets-r08-a.ogg) | 4.50 | -13.3 → -13.0 | -24.1 → -24.1 |  |
| special-airstrike-bombs-r08-a | [chosen](../../audio/sfx/concept/special-airstrike-bombs-r08-a.ogg) | [final](../../../assets/sfx/special-airstrike-bombs-r08-a.ogg) | 5.00 | -1.7 → -1.5 | -16.7 → -16.7 |  |
| special-smartbomb-r08-a | [chosen](../../audio/sfx/concept/special-smartbomb-r08-a.ogg) | [final](../../../assets/sfx/special-smartbomb-r08-a.ogg) | 4.20 | -1.6 → -1.6 | -21.1 → -20.8 |  |
| special-flares-r08-a | [chosen](../../audio/sfx/concept/special-flares-r08-a.ogg) | [final](../../../assets/sfx/special-flares-r08-a.ogg) | 1.40 | -5.3 → -6.2 | -24.1 → -24.1 | original clipped at full scale |
| special-denied-r08-a | [chosen](../../audio/sfx/concept/special-denied-r08-a.ogg) | [final](../../../assets/sfx/special-denied-r08-a.ogg) | 0.30 | -10.1 → -9.9 | -27.1 → -27.1 |  |
| ui-radio-open-r08-a | [chosen](../../audio/sfx/concept/ui-radio-open-r08-a.ogg) | [final](../../../assets/sfx/ui-radio-open-r08-a.ogg) | 0.30 | -19.9 → -19.9 | -26.9 → -27.0 | in the game |
| ui-radio-close-r08-a | [chosen](../../audio/sfx/concept/ui-radio-close-r08-a.ogg) | [final](../../../assets/sfx/ui-radio-close-r08-a.ogg) | 0.50 | -6.3 → -6.8 | -27.1 → -27.3 | in the game |
| ui-klaxon-r08-a | [chosen](../../audio/sfx/concept/ui-klaxon-r08-a.ogg) | [final](../../../assets/sfx/ui-klaxon-r08-a.ogg) | 4.76 | -11.8 → -11.2 | -27.0 → -27.0 |  |
| ambience-orbit-r08-a | [chosen](../../audio/sfx/concept/ambience-orbit-r08-a.ogg) | [final](../../../assets/sfx/ambience-orbit-r08-a.ogg) | 16.00 | -18.5 → -18.7 | -36.0 → -36.0 | in the game |
| ambience-luna-r08-a | [chosen](../../audio/sfx/concept/ambience-luna-r08-a.ogg) | [final](../../../assets/sfx/ambience-luna-r08-a.ogg) | 16.00 | -12.3 → -12.8 | -36.2 → -36.2 |  |
| ambience-city-r08-a | [chosen](../../audio/sfx/concept/ambience-city-r08-a.ogg) | [final](../../../assets/sfx/ambience-city-r08-a.ogg) | 20.00 | -15.0 → -15.1 | -36.1 → -36.1 |  |
| ambience-ocean-r08-a | [chosen](../../audio/sfx/concept/ambience-ocean-r08-a.ogg) | [final](../../../assets/sfx/ambience-ocean-r08-a.ogg) | 16.00 | -15.7 → -16.4 | -36.2 → -36.2 |  |
| ambience-storm-r08-a | [chosen](../../audio/sfx/concept/ambience-storm-r08-a.ogg) | [final](../../../assets/sfx/ambience-storm-r08-a.ogg) | 24.00 | -10.2 → -10.1 | -37.2 → -37.3 |  |
| ambience-arctic-r08-a | [chosen](../../audio/sfx/concept/ambience-arctic-r08-a.ogg) | [final](../../../assets/sfx/ambience-arctic-r08-a.ogg) | 16.00 | -13.1 → -13.1 | -36.1 → -36.1 |  |

## Decisions

- 2026-10-02: Round opened (Level 01 batch, parts P1 and P2).
- 2026-10-02: Part P3 before the review: the Skitter's 16 headings (user decision) and the clear
  weak spots of rows 1–6 and 8 reworked (see **P3** in each row); review files and the game capture
  rebuilt. Explosions, backdrop and SFX unchanged.
- 2026-10-02: Round closed (user decision): all ten parts approved as **final**; the parts with their own doc are `art: final` (Stormhawk, Skitter, Needler, pulse cannon, Level 01), the aggregate docs record the final assets in their Decisions. The Stormhawk's hull hit boxes stay unchanged (the ~1 px over empty space in the hard-bank frames is negligible).
