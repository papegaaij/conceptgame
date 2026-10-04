---
title: Concept round 22 — Level 05 backdrop
design: approved
implementation: n/a
art: chosen
depends-on: [../../campaign/act-1-first-contact/level-05-crater-nest]
updated: 2026-10-04
---

# Concept round 22 — Level 05 backdrop

## Summary

Level 05's own backdrop, rendered as **final** art by
[tools/art/backdrop_l05.py](../../../tools/art/README.md) into `assets/backdrop/level-05/`. Until
now the level reused Level 04's Luna images. Open [index.html](index.html) in a browser (regenerate with
`python3 tools/concept/board.py 22`). The choice is *approve as final* or *redo* (say what to
change); the level's `art` stays `chosen` until it is approved. The backdrop shows in the game
with `./gradlew :desktop:run --args="--level 5"`.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Level 05 backdrop](../../campaign/act-1-first-contact/level-05-crater-nest/README.md#concept-art) | Level 04's kit and terrain with new pieces from the Layout: the outer rim wall (the rail climbs it in a cutting to a launch lip at the crest) and the far rim, cut from the terrain over the section seams; a crater slope, nest floor and calm arena floor; a growth patch per battery with a socket under every unit; the burning nest (6 frames) at the arena's end, behind the ship at the lift-off; Level 04's mass-driver field, plumes and small pieces rendered again for Level 05 | `backdrop-final-r22-a` (png) | the outer rim reads as light and dark bands more than as a raised wall; in a fight longer than about 46 s the burning nest and the far rim's foot scroll in under the bell; a 1 px line at the shared terrain's wrap row, inherited from Level 04's tiles; the sled lamps still come from Level 04's frames | approved — final (user 2026-10-04) |

## Decisions

- 2026-10-04: Opened with the Level 05 backdrop.
- 2026-10-04: Closed (user): the Level 05 backdrop approved as final. It was the last part of
  Level 05 without final art, so the level becomes `art: final`. (The sled lamps named as a weak
  spot had already moved to Level 05's own `sled-run` frames before the close.)
