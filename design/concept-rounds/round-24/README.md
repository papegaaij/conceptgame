---
title: Concept round 24 — the Coilwyrm's death
design: approved
implementation: n/a
art: chosen
depends-on: [../../enemies/air/coilwyrm, ../../audio/sfx]
updated: 2026-10-05
---

# Concept round 24 — the Coilwyrm's death

## Summary

A small sound round after round 23's review of Level 06: the Coilwyrm's chained death only went
"pop" (the generic tiny explosion and its sound, 0.06 s apart). It became a ripple of wet bursts
from the head down to the tail, each segment showing its own death (glint and pieces); this round
picked the burst sounds from four options, each a segment burst, the head's deeper burst and a
preview of the whole death. **Closed 2026-10-05**: head burst **c**, segment burst **b**, the ripple
five times slower (0.25 s). Open [index.html](index.html) in a browser (regenerate with
`python3 tools/concept/board.py 24`); the chosen pair at the game's rhythm is
`enemy-coilwyrm-death-final-r24-a`, and a game capture with its sound is in the
[Coilwyrm's concept art](../../enemies/air/coilwyrm/README.md#concept-art)
(`coilwyrm-death-capture-r24-a`). Try it in Level 06: `./gradlew :desktop:run --args="--level 6"`.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Coilwyrm death sound](../../audio/sfx/README.md#concept-art) | pick a, b, c or d: **a** "Gib sound" (RoozyDB, CC0): a sharp wet crack with gooey bits; **b** "Burst Flesh" (magnuswaker, CC0): a dense fleshy burst with a low body; **c** "Messy Splat 3" (FoolBoyMedia, CC-BY 4.0): a bright, messy splatter; **d** synthesized (`tools/concept/audio/sfx_r24.py`): a pressurised pop — membrane snap, chitin crackle, a falling gas bloop, wet bubble chirps. Each with a head burst (its source cut longer, 18 % slower, over a sub thump) | `enemy-coilwyrm-death-r24-a`…`-d` (the whole death: head burst, then 13 bursts), `enemy-coilwyrm-burst-r24-a`…`-d`, `enemy-coilwyrm-head-burst-r24-a`…`-d` | **not listened to**: picked and levelled by envelope, band level and spectrum only. How distinct the 13 bursts stay (onset rise above 1 kHz at each burst, mean): a 8.5 dB (clearest), d 4.4, c 3.9, b 3.9 (its 13 ms attack smears into a rumble); a's crack meets the ceiling, so its body is about 3 dB quieter than the others; b's and a's head bursts are mostly sub (the thump); c is CC-BY (credits screen); the previews are HQ previews of the recordings (rebuilt from the originals once chosen) | **head burst c, segment burst b** (user); a and d rejected; the production files rebuilt from the Freesound originals |
| 2 | [Ripple timing and mix](../../enemies/air/coilwyrm/README.md#behaviour) | accept or change: 0.05 s between bursts (13 bursts in 0.65 s at medium, 15 on hard), the bursts 4 dB under the explosions, pitch rising 1.8 % a burst (0.94 → at most 1.2, smaller segments down the taper) with ±2 % random, six instances so three or four overlap uncut; a segment or the tail shot on its own plays the same burst; the chained bursts pay nothing (unchanged) | the `death` previews; Level 06 in the game | the ripple plays the member's own death sprites (round 23's glint and pieces) instead of the generic tiny explosion; 0.04 s would be a faster zip, 0.067 s (the old 4 steps) a slower roll | **too fast** (user: "probably 5 times"): 0.25 s between bursts; the bursts no longer overlap and play at the explosion level, pitched by each member's width down the taper; the waiting members are doomed (not hit, no ramming, no pay) and the headless body drifts to rest; each burst's look and sound in the same step |

## Notes

- File names: `enemy-coilwyrm-burst-r24-<v>.ogg` (segment and tail), `enemy-coilwyrm-head-burst-r24-<v>.ogg`
  (the heads) and `enemy-coilwyrm-death-r24-<v>.ogg` (review preview only) in
  [design/audio/sfx/concept/](../../audio/sfx/concept/prompts.md#round-24--the-coilwyrms-death-bursts).
- Levels: the loudest 100 ms of the 200 Hz–5 kHz band, a segment burst at -16 dB (the tiny
  explosions measure -18 and -14), the head's at -14 dB (explosion-r02-a -15). The whole death
  measures -15 to -18 dB on the same scale, about one explosion: loud enough to read, not deafening.
- The previews a–d (0.05 s, the bursts 4 dB under the explosions, six instances) are the
  round's rhythm as the user heard it; they stay in `concept/rejected/` with the unchosen bursts.

## Decisions

- 2026-10-05: Opened after the user's review of the Coilwyrm in round 23 ("the current sound is just
  a small pop … segments one after the other in rapid succession, more of a burst").
- 2026-10-05: Closed (user: "head burst c, death b, but the death has the effects too fast, they
  need more spacing, probably 5 times what you have now. Also make sure the animation matches the
  sounds"). Head burst c and segment burst b chosen and rebuilt from the Freesound originals
  (`tools/art/sfx_originals.py` with `sfx_r24.py`'s treatment); `pop_interval` 0.25 s; the
  agents' choices for the slower ripple (doomed waiting members, the headless body drifting to
  rest, the pitch by width, the level) are in the
  [Coilwyrm's Decisions](../../enemies/air/coilwyrm/README.md#decisions). Sync: the look and the
  sound of each burst start in the same simulation step; the capture shows them within a frame.
