---
title: Concept round 27 — save-done sound, Coilwyrm chain-cut tear, Vrell screech
design: approved
implementation: n/a
art: chosen
depends-on: [../../audio/sfx, ../../enemies/air/coilwyrm, ../../ui/hangar]
updated: 2026-10-06
---

# Concept round 27 — save-done sound, Coilwyrm chain-cut tear, Vrell screech

## Summary

The user's answers (2026-10-06) to the three sound questions round 26 left open: a/b concepts for
the **save-done** sound (synthesized, the round-08 UI family) and the **Coilwyrm's chain-cut tear**
(recorded, CC0 / CC-BY, cut from the Freesound originals), both by
`tools/concept/audio/sfx_r27.py`; and the **Vrell screech** wired to a large Vrell unit entering
the screen, to approve as built. Open [index.html](index.html) in a browser (regenerate with
`python3 tools/concept/board.py 27`). The sounds are *pick a or b*; option a of each already plays
in the game, so they can be heard there: the save at the hangar (its autosave as it opens after a
level, and its SAVE command to a slot), the tear and the screeches in Level 06
(`./gradlew :desktop:run --args="--level 6"`; Mantises, Coilwyrms, Spore Bombers), Scuttlers in
Level 04 (`--level 4`).

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Save done](../../audio/sfx/README.md#ui-and-radio) | **synthesized**, pick a or b: **a** "write and seal", four quick rising pulse ticks landing on a bright held fifth with a bell an octave up (0.70 s); **b** "calm chime", a soft latch click and two warm bells a fourth apart over a swelling pad (0.92 s). Played when the hangar's autosave is written as the hangar opens and when a save to a slot is written (that save no longer plays the menu confirm first; a failed save plays the back blip); both at peak −8 dBFS like the other UI blips | `ui-save-r27-a`, `ui-save-r27-b` | not listened to; a's ticks could read as the debrief's tally ticks, b's bells as the data-core chime; the autosave sounds over the hangar theme's first bar | **b** (user): the calm chime; a moved to `concept/rejected/`; `Sfx.SAVE_DONE` plays b, copied into `assets/sfx/` by `copyPlaceholderSounds` like the other synthesized UI blips |
| 2 | [Coilwyrm chain-cut tear](../../audio/sfx/README.md#enemies) | **recorded**, pick a or b: **a** "rip_tear FLESH!!!!.wav" by aust_paul (CC0), a monster tearing at flesh, a run of short wet rips, 15 % slower for a bigger body (1.06 s); **b** "Tearing Flesh" by dereklieu (CC-BY 3.0, so on the credits screen), one juicy limb-tearing rip with a low body (0.72 s). Played on the cut (`CHAIN_CUT`) over the cut segment's own burst, instead of the lower Brood Pod burst; levelled to −20 dB on the loudest 100 ms of the 200 Hz–5 kHz band, between the segment burst (−17.3) and the regrowth (−27.4) | `enemy-coilwyrm-cut-r27-a`, `enemy-coilwyrm-cut-r27-b` | not listened to; a is bright (its original has little below 800 Hz) and 1 s long, so two cuts close together overlap (two at a time); b keeps 28 % of its energy below 200 Hz and may boom on large speakers | **a** (user), "rip_tear FLESH!!!!.wav" by aust_paul (CC0); b (CC-BY) moved to `concept/rejected/` and so off the credits roll; `Sfx.COILWYRM_CUT` plays a, written to `assets/sfx/` from the Freesound original by `tools/art/sfx_originals.py` (sfx_r27.py's `PRODUCTION`) |
| 3 | [Vrell screech](../../audio/sfx/README.md#enemies) | **built**, approve or change: the chosen screeches c and d, in turn, as a Mantis, a Coilwyrm's head, a Spore Bomber or a Scuttler first comes onto the screen (its hit box over the play field; the game watches each step, as no simulation event marks an entry), once per unit, **at most one every 3 s** (a unit entering inside them stays silent, also later), at the Vrell spawns' level, ranked with enemy fire. Flown without input on medium: Level 03 has 10 such units and 5 screeches, Level 04 8 and 5, Level 06 12 and 9, Level 07 6 and 3; Levels 01, 02 and 05 none | [c](../../audio/sfx/concept/enemy-screech-r08-c.ogg), [d](../../audio/sfx/concept/enemy-screech-r08-d.ogg) (round 08) | not heard in the game by Claude; a regrown Coilwyrm head does not screech (it grows on the screen); with 3 s Level 06's screeches come every 15–25 s, Level 03's first Spore Bombers at 12 s | **approved as built** (user) |

## Notes

- Review file names: `<subject>-r27-a/b.ogg` in [design/audio/sfx/concept/](../../audio/sfx/concept/prompts.md#round-27--the-save-done-sound-and-the-coilwyrms-chain-cut-tear);
  their `prompts.md` entries say how they are made. Both tears' sources are in
  [CREDITS.md](../../../CREDITS.md); while the round was open the copies of option a in
  `assets/sfx/` came from `:pipeline:importPlaceholders`.
- After the choices: the rejected variants move to `concept/rejected/` (CREDITS.md rows updated);
  the chosen sounds replace option a in `Sfx` (and in the placeholder list); the chosen tear gets
  its production file from `tools/art/sfx_originals.py` (sfx_r27.py's `CHOSEN`).

## Decisions

- 2026-10-06: Opened with the user's decisions on round 26's open sound questions.
- 2026-10-06: Closed (user): save done **b**, the calm chime (a, the rising data ticks, rejected);
  the Coilwyrm's chain-cut tear **a**, "rip_tear FLESH!!!!.wav" by aust_paul, CC0 (b, "Tearing
  Flesh" by dereklieu, CC-BY, rejected); the Vrell screech approved as built. The rejected files
  are in `design/audio/sfx/concept/rejected/` (CREDITS.md updated, the credits roll regenerated);
  the game plays the chosen ones: the save as a concept copy (`copyPlaceholderSounds`), the tear
  rebuilt from its original by `tools/art/sfx_originals.py` (sfx_r27.py's `CHOSEN`).
