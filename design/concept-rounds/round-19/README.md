---
title: Concept round 19 — casting the generic radio speakers
design: approved
implementation: n/a
art: chosen
depends-on: [../../audio/voice]
updated: 2026-10-03
---

# Concept round 19 — casting the generic radio speakers

## Summary

Which voice does each generic radio speaker of Act 1 get? Each speaker says one of its real
lines with two candidate reference voices (`-a`, `-b`), LibriVox readers whose recordings are
public domain or CC0 and who are not in the main cast, rendered by Chatterbox through radio
filter b. Open [index.html](index.html) in a browser (regenerate with
`python3 tools/concept/board.py 19`); the files are in [voice](../../audio/voice/README.md#concept-art),
the lines, settings and readers in its [prompts](../../audio/voice/concept/prompts.md), the clips
in [refs](../../audio/voice/refs/README.md) and [CREDITS.md](../../../CREDITS.md).

## Choices

Closed 2026-10-03: the user picked one reader per speaker. Pitch is the reference clip's median F0
(female ≈ 165–255 Hz, male ≈ 85–155 Hz).

| # | Speaker (line) | a | b | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | **Yard Control** (L02, 5 lines: Crane Four, the docks lost) | Mark Nelson, *A Princess of Mars* (public domain), 145 Hz | Elizabeth Klett, *Lady Audley's Secret* (public domain), 177 Hz | Whisper heard a's "Watch the arm, Aegis" as "Watch the armages": possibly slurred | **a** (user, 2026-10-03) |
| 2 | **Dock One–Four** (L02; one voice for the four docks, or see item 8) | Tom Crawford, *The Sea Wolf* (public domain), 93 Hz | Kristin Hughes, *Dr. Jekyll and Mr. Hyde* (public domain), 167 Hz | Whisper heard b's "Dock One" as "Jock want": check the opening | **b** (user, 2026-10-03) |
| 3 | **Ring Control** (L03, 2 lines) | Bob Neufeld, *A Study in Scarlet* (CC0), 143 Hz | Cori Samuel, *Frankenstein* (CC0), 222 Hz | none measured | **b** (user, 2026-10-03) |
| 4 | **Tranquility Control** (L04, 2 lines, one shouted) | John Greenman, *Huckleberry Finn* (public domain), 131 Hz | Karen Savage, *The Scarlet Pimpernel* (public domain), 198 Hz | a ran on past the line on five of six seeds (trailing syllables); the kept seed is clean to Whisper. Option **c**: keep round 18's David Leeson ([radio b](../../audio/concept/radio-chatterbox-d-control-r18-b.ogg)) | **b** (user, 2026-10-03) |
| 5 | **Crawler One** (L04, 3 lines, a civilian driver) | Phil Chenevert, *Pollyanna* (CC0), 164 Hz | Kara Shallenberg, *Heidi* (public domain), 197 Hz | a is a male reader, but the cut has him voicing a woman's dialogue (164 Hz): the clone may come out pitched up | **b** (user, 2026-10-03) |
| 6 | **Convoy** (L04, the first crawler hit, shouted) | Adrian Praetzellis, *Treasure Island* (public domain), 118 Hz | Judy Bieber, *Dorothy and the Wizard in Oz* (public domain), 211 Hz | a's clip is a verse dedication, read slowly | **a** (user, 2026-10-03) |
| 7 | **Hammer Lead** (the Airstrike call, any level) | Mark F. Smith, *The Lost World* (public domain), 102 Hz | Gord Mackenzie, *Scaramouche* (public domain), 100 Hz | a three-word line (1.6–1.7 s): little to judge by | **b** (user, 2026-10-03) |
| 8 | **Merge minor speakers?** | one voice for several minor speakers, e.g. Convoy = Crawler One (both civilians in the same convoy), the docks = one dock voice, Ring Control = Yard Control (both CDF controllers) | keep every speaker its own voice | fewer voices make the cast smaller but the controllers of different stations alike | **b**: every speaker its own voice (the user cast each one) |

## Listening

| Speaker | a | b |
|---|---|---|
| Yard Control | [a](../../audio/voice/concept/voice-yard-control-r19-a.ogg) | [b](../../audio/voice/concept/rejected/voice-yard-control-r19-b.ogg) |
| Dock One–Four | [a](../../audio/voice/concept/rejected/voice-dock-r19-a.ogg) | [b](../../audio/voice/concept/voice-dock-r19-b.ogg) |
| Ring Control | [a](../../audio/voice/concept/rejected/voice-ring-control-r19-a.ogg) | [b](../../audio/voice/concept/voice-ring-control-r19-b.ogg) |
| Tranquility Control | [a](../../audio/voice/concept/rejected/voice-tranquility-control-r19-a.ogg) | [b](../../audio/voice/concept/voice-tranquility-control-r19-b.ogg) |
| Crawler One | [a](../../audio/voice/concept/rejected/voice-crawler-one-r19-a.ogg) | [b](../../audio/voice/concept/voice-crawler-one-r19-b.ogg) |
| Convoy | [a](../../audio/voice/concept/voice-convoy-r19-a.ogg) | [b](../../audio/voice/concept/rejected/voice-convoy-r19-b.ogg) |
| Hammer Lead | [a](../../audio/voice/concept/rejected/voice-hammer-lead-r19-a.ogg) | [b](../../audio/voice/concept/voice-hammer-lead-r19-b.ogg) |

## Notes

- The speakers are every generic speaker in Act 1's level data (Levels 01–04) and the specials'
  calls; Level 01 has only the main cast. The four docks share their lines, so they are one role
  here.
- The readers and the archive.org sources are in the [prompts](../../audio/voice/concept/prompts.md)
  and [CREDITS.md](../../../CREDITS.md); every item's licence was checked on its archive.org
  `licenseurl` (public domain mark/dedication or CC0 1.0) on 2026-10-03.
- One take per line, fixed seeds; Claude cannot hear the files: the notes are Whisper
  transcripts, lengths and pitches only.

## Decisions

- 2026-10-03: Opened with two candidate readers for each of the seven generic speakers of Act 1.
- 2026-10-03: Closed with the user's picks: Yard Control a (Mark Nelson), Dock One–Four b
  (Kristin Hughes), Ring Control b (Cori Samuel), Tranquility Control b (Karen Savage), Crawler One
  b (Kara Shallenberg), Convoy a (Adrian Praetzellis), Hammer Lead b (Gord Mackenzie); every
  speaker keeps its own voice (item 8: b). The rejected takes moved to
  `design/audio/voice/concept/rejected/`; the cast is recorded in the
  [voice](../../audio/voice/README.md#reference-voices) README.
