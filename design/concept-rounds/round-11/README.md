---
title: Concept round 11 — production track, first batch
design: approved
implementation: n/a
art: chosen
updated: 2026-10-02
---

# Concept round 11 — production track, first batch

## Summary

The first round of the production-art track ([plan](../../art-direction/production/README.md)):
what Level 01 still lacks or got wrong after playing it. Open [index.html](index.html) in a
browser (regenerate with `python3 tools/concept/board.py 11`). Choices 1–4 need an answer;
item 5 is a fix shown before and after, for a yes or a redo.

## Choices

| # | Choice | Variants | Part | Recommendation | Outcome |
|---|---|---|---|---|---|
| 1 | Launch rail sound (Level 01 section 1, release at 3.6 s of the 5 s launch) | a "mag-lev": motor hum and whine rising, coil ticks, release clunk, engine whoosh · b "catapult": pressure hiss, shuttle rumble and rattle, buffer clunk, steam vent | [sfx](../../audio/sfx/README.md) | a: an electromagnetic rail fits a 2185 orbital yard, and its ticks tell the speed | b chosen (catapult); a rejected |
| 2 | Edge-warning look | A "big pulse" (r09 look at ~3×, glow, smooth pulse) · B "sweeping chevrons" (chevrons run in from the edge) · C "edge glow band" (the whole edge lit, hazard ticks) | [HUD](../../ui/hud/README.md) | C: it reaches peripheral vision wherever the eyes are, and stays calm; A as the fallback | A chosen (big pulse); B, C rejected |
| 3 | Edge-warning tone | a "triple chirp" (three square blips on the flash cycle) · b "contact ping" (chirp into a ringing ping, one echo) | [sfx](../../audio/sfx/README.md) | a: distinct from the low-armour beep and the klaxon, and it carries the flash rhythm | b chosen (contact ping); a rejected |
| 4 | "Coalition Rising" base stem (Level 01 until section 4) | a: ostinato, string pads, timpani, harp, flute, choir; no brass, lead or drums. Sample-aligned with the chosen full mix, same gain (6.4 LU quieter) | [music](../../audio/music/README.md) | a, crossfading to the full mix at section 4 | accepted |
| 5 | Level 01 backdrop fixes (not a choice) | before / after: dawn terminator without posterization bands; burning platforms with ragged venting fires instead of blobs | [Level 01](../../campaign/act-1-first-contact/level-01-break-at-dawn/README.md) | accept | accepted |

Also for review: the [production art plan](../../art-direction/production/README.md) (draft)
and its open questions.

## Notes

- The edge warnings use the round-09 HUD warning amber `FFC800`; the M2 game still draws a small
  red arrow (`FF4030`), which the chosen variant replaces.
- The base stem comes from the same render as the full mix, whose re-render is identical to the
  chosen `coalition-rising-full-r08-a.ogg` byte for byte, so the chosen file is the pair's other
  half: same length (142.3 s) and loop points (340772 + 5773091 samples).
- Only `earth-dawn.png` and `platform-burning_0…3.png` changed in `assets/backdrop/level-01/`;
  the other pieces are byte-identical.

## Decisions

- 2026-10-02: Round opened.
- 2026-10-02: Round closed with the user's choices: launch rail b, edge-warning look A, edge-warning tone b; the base stem and the backdrop fixes accepted.
