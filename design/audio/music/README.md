---
title: Music
design: draft
implementation: not-started
art: chosen
depends-on: [../../campaign]
updated: 2026-09-30
---

# Music

## Summary

About 30 tracks: menu and hangar themes, two level themes per act, boss themes, jingles, and
the ending. Tracks loop seamlessly and switch with short crossfades. A simple two-layer
intensity system makes busy sections hit harder without needing adaptive composition
everywhere.

## Design

### Track list

| # | Track | Use | Style / notes | Length | Priority | Current material |
|---|---|---|---|---|---|---|
| 1 | Title theme | Title & main menu | Heroic main motif, synth-orchestral + beat | 2:00 loop | P1 | [title-theme-r02-a](concept/title-theme-r02-a.ogg) "Terran Vanguard" (proposed) |
| 2 | Hangar | Hangar | Laid-back electronic, mechanical ambience, main motif hint | 2:30 loop | P1 | [hangar-theme-r02-a](concept/hangar-theme-r02-a.ogg) "Dry Dock" (proposed) |
| 3 | Briefing | Briefing screens | Tense pads, sparse percussion | 1:30 loop | P2 | — |
| 4 | Act 1 A: Earth orbit & Luna | Levels 01–07 | Uplifting tracker trance/techno | 3:00 loop | P1 | [music-r01-a](concept/music-r01-a.ogg) "Afterburner" (chosen, to be extended to full length) |
| 5 | Act 1 B: Earth orbit & Luna | Levels 01–07 | Synth-orchestral, the main motif | 3:00 loop | P1 | [music-r01-b](concept/music-r01-b.ogg) "Coalition Rising" (chosen, to be extended to full length) |
| 6–7 | Act 2 A/B: Earth surface | Levels 08–14 | Urgent breakbeat, orchestral hits | 3:00 | P2 | — |
| 8 | Act 3 A: Mars | Levels 15–21 | Dusty breakbeat, desert-scale (phrygian dominant) lead | 3:00 | P2 | [mars-theme-r02-a](concept/mars-theme-r02-a.ogg) "Red Dust Run" (proposed) |
| 9 | Act 3 B: Mars | Levels 15–21 | Dusty mid-tempo techno, Vorne's motif as a faint layer | 3:00 | P2 | — |
| 10 | Act 4 A: Europa | Levels 22–28 | Muffled ambient trance, sonar pings, deep echoes | 3:00 | P2 | [europa-theme-r02-a](concept/europa-theme-r02-a.ogg) "Thera Deep" (proposed) |
| 11 | Act 4 B: Europa | Levels 22–28 | Muffled ambient-dnb, Vorne's motif as a faint layer (L27–28) | 3:00 | P2 | — |
| 12–13 | Act 5 A/B: Asteroid belt | Levels 29–35 | Driving industrial, Vorne's motif in the open | 3:00 | P3 | — |
| 14–15 | Act 6 A/B: Jupiter | Levels 36–42 | Heavy, stormy, choir + distorted bass | 3:00 | P3 | — |
| 16–17 | Act 7 A/B: Beyond the gate | Levels 43–50 | Alien, Choir motif fused with the main motif | 3:00 | P3 | — |
| 18 | Boss: Vrell | Vrell act bosses | Choir, pounding drums | 2:00 loop | P1 | [boss-theme-r02-a](concept/boss-theme-r02-a.ogg) "The Choir Descends" (proposed) |
| 19 | Boss: Ascendancy | Ascendancy bosses | Industrial, Vorne's motif | 2:00 loop | P3 | — |
| 20 | Final boss | Level 50 | All motifs, full orchestra + beat | 3:00 loop | P3 | — |
| 21 | Mini-boss sting | Mini-boss entrance | 4 s stinger, then back to the level track | 0:04 | P2 | — |
| 22 | Boss warning | Before the boss | Alarm + riser, 5 s, bridges to the boss track | 0:05 | P1 | — |
| 23 | Mission complete | Debrief start | Victory jingle | 0:06 | P1 | — |
| 24 | Act complete | After an act boss | Longer fanfare | 0:15 | P2 | — |
| 25 | Mission failed | Failure screen | Short downbeat sting | 0:05 | P1 | — |
| 26 | Game over | Hard-mode game over | Somber, main motif in minor | 0:20 | P3 | — |
| 27 | Ending | Campaign ending | Main motif, orchestral | 3:00 | P3 | — |
| 28 | Credits | Credits roll | Upbeat remix of the title theme | 3:30 | P3 | — |

The concept pieces are 40–65 s sketches of each track's style and material. Production
versions are extended to the listed length.

Which level uses A or B is set per level in the [campaign](../../campaign/README.md).

### Motifs

- **Main motif** (humanity / Lancer): a rising 5-note brass phrase on scale degrees
  **1 – 5 – 8 – 9 – ♭10** (♮10 in major keys), rhythm long-short-long-short-long. In D minor:
  D4 · A4 · D5 · E5 · F5. It grew out of the horn call in "Coalition Rising". It appears in the
  title (brass statement, then in F major in the B section), the hangar (a relaxed 5-8-9-♭10
  fragment in the flute), act 1 and the ending.
- **Choir motif** (Vrell): a wordless descending vocal line on degrees
  **8 – ♭7 – ♭6 – 5**, a tritone drop to **♭2**, resolving to **1**. In E: E5 · D5 · C5 · B4 · F4 · E4.
  The tritone drop and the ♭2→1 half step make it alien. It is sung by a formant "choir" and
  doubled by low brass; augmented (half speed) in breakdowns. First used in the Vrell boss
  theme.
- **Vorne's motif** (Ascendancy): a cold 4-note arpeggio. It follows the twist timing in
  [story](../../story/README.md): a faint background layer from the first hints in Act 3 (the
  Act 3 B theme and the L19 Revenant Walker fight) and in Act 4 (L27–28), then in the open from
  Act 5, when Vorne's broadcast reveals the Ascendancy.

### Loops and transitions

- Every loop has an intro section (played once) and a loop section with sample-accurate loop
  points. Delivery format (as in the round-02 concept files): intro + loop in one file, loop
  points as Vorbis comments `LOOPSTART` / `LOOPLENGTH` in samples, followed by a short
  fade-out tail that only players ignoring the loop points reach.
- Level → boss: boss warning stinger replaces the level track (crossfade 0.5 s), then the boss
  track starts on the downbeat.
- Level → debrief: 1 s fade, then the mission complete jingle.
- Death: level music cuts instantly (with a short low-pass sweep), then the mission failed sting.

### Intensity layers

Level themes are delivered as two synced stems: **base** and **intensity** (extra drums and
lead). The intensity stem fades in (1 s) when on-screen enemy density is high or a scripted
level section asks for it, and fades out 4 s after calm returns.

## Concept art

Concept rounds 01 and 02 — see [round 01](../../concept-rounds/round-01/README.md). Briefs,
keys, tempos, chord progressions, loop points and AI-generator prompts:
[concept/prompts.md](concept/prompts.md). Generated by `tools/concept/audio/music.py`
(round 01) and `tools/concept/audio/music_r02.py` (round 02).

| File | What | Status |
|---|---|---|
| [concept/music-r01-a.ogg](concept/music-r01-a.ogg) | "Afterburner" — tracker-style trance/techno, 140 BPM, A minor, 37.5 s: arpeggio intro → lead A section → B section with riser and snare roll | chosen — direction for level themes |
| [concept/music-r01-b.ogg](concept/music-r01-b.ogg) | "Coalition Rising" — synth-orchestral, 132 BPM, D minor, 40.7 s: string ostinato and horn call → heroic horn theme → theme in strings with brass stabs | chosen — direction for level themes |
| [concept/title-theme-r02-a.ogg](concept/title-theme-r02-a.ogg) | "Terran Vanguard" — title theme, 126 BPM, D minor, 57.6 s (loop 8.1 s + 45.7 s): timpani and horn call → main motif in brass over gallop strings, taiko and a four-on-the-floor beat → F-major lift with supersaw lead → breakdown with the motif at half speed → build | chosen |
| [concept/hangar-theme-r02-a.ogg](concept/hangar-theme-r02-a.ogg) | "Dry Dock" — hangar, swung downtempo, 90 BPM, D dorian, 64.7 s (loop 6.0 s + 53.3 s): FM electric piano and round sub bass groove → flute melody with a relaxed main-motif fragment → sparse breakdown with vibes; faint hangar clanks | chosen |
| [concept/boss-theme-r02-a.ogg](concept/boss-theme-r02-a.ogg) | "The Choir Descends" — Vrell boss, 150 BPM, E minor/phrygian, 61.2 s (loop 6.8 s + 51.2 s): the Choir motif alone over a drone → distorted 16th bass, pounding drums, Choir motif with brass and orchestral stabs → tritone synth riff → half-time breakdown with the motif augmented in low brass → build | chosen |
| [concept/mars-theme-r02-a.ogg](concept/mars-theme-r02-a.ogg) | "Red Dust Run" — Act 3 A, 135 BPM, E phrygian dominant, 53.8 s (loop 7.6 s + 42.7 s): desert wind, hand drums and a lonely reed → breakbeat, reese bass and reed melody → Andalusian-cadence section with 16th arpeggio → dust-storm breakdown (muffled drums, wind swells) → build | chosen |
| [concept/europa-theme-r02-a.ogg](concept/europa-theme-r02-a.ogg) | "Thera Deep" — Act 4 A, 125 BPM, F minor, 58.1 s (loop 8.2 s + 46.1 s): sonar pings, whale song and bubbles → muffled four-on-the-floor, pumping rolling bass, echoing pluck arp → the filter opens ("surfacing") for the lead melody → deep breakdown without drums → build | chosen |

## Implementation

- [ ] Music player with intro + loop points
- [ ] Crossfades and stinger transitions (boss warning, jingles)
- [ ] Two-stem intensity layer driven by density or level script
- [ ] Per-level track assignment from level data

## Open questions

- 28 tracks is a lot of production. If needed, drop the B variants of acts (−7 tracks) or reuse
  the Vrell boss theme for all bosses.
- Should music be composed as real tracker modules (.xm/.it), which is authentic, small and
  loops for free, or rendered audio? This depends on the engine choice.

## Decisions

- 2026-09-30: Two themes per act, faction boss themes, two-stem intensity system.
- 2026-09-30: Vorne's motif is planted in Acts 3–4 alongside the story hints and heard openly
  from Act 5.
- 2026-09-30: Concept round 01: both sketches chosen — "Afterburner" (tracker trance/techno) and "Coalition Rising" (synth-orchestral) are both the right direction. More tracks in this style are wanted for the other themes.
- 2026-09-30: "Afterburner" and "Coalition Rising" assigned to the Act 1 A and B slots.
- 2026-09-30: Concept round 02 proposed: title, hangar, Vrell boss, Act 3 A (Mars) and Act 4 A
  (Europa) themes. The main motif and the Choir motif are pinned down as notes (see Motifs).
- 2026-09-30: Concept round 02: all five themes chosen ("awesome and spot on"). Keep composing the remaining tracks in this style.
