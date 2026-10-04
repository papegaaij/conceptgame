---
title: Voice
design: draft
implementation: in-progress
art: chosen
depends-on: [.., ../../story/characters, ../../ui/hud, ../../ui/options, ../../tech/architecture]
updated: 2026-10-04
---

# Voice

## Summary

The radio lines are spoken. Every line in the level data is rendered offline by the Chatterbox
text-to-speech engine, through the shared radio filter, into OGG files under `assets/`; the game
plays the file with the radio subtitle, which stays on screen. The briefings and the Choir are
voiced by the same pipeline. The engine and the radio filter were chosen in concept
[round 18](../../concept-rounds/round-18/README.md), the generic speakers' voices are cast in
[round 19](../../concept-rounds/round-19/README.md); the build and CI never run the engine.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [refs](refs/README.md) | Reference clips the voices are cloned from: LibriVox readings, public domain or CC0 | draft | done | chosen |

## Design

### Engine

**Chatterbox** (Resemble AI, model `ResembleAI/chatterbox`, 0.5 B, English), MIT code and
weights; every output carries Resemble's inaudible Perth watermark. It clones a voice zero-shot
from a 5–15 s reference clip and has two emotion controls, `exaggeration` (0.25–2) and
`cfg_weight` (lower is slower and more dramatic), plus `temperature`. It runs at about real time
on the RTX 2070 (3.6 GB VRAM). The round's setup (venv, versions, the model download) is in the
header of `tools/concept/audio/tts_r18.py`; the production renderer reuses it.

### Offline pipeline

1. **Line list.** `vanguard.content.voice.VoiceLines` (shared by the game) lists every `radio`
   cue of every level's `data.yaml` (timed and event cues, a secret's line, the `mission-failed`
   line, the `easy` / `hard` line variants, a special's call such as Hammer Lead's), and every
   page of the levels' and acts' briefings (not the hangar teaser); `./gradlew
   :pipeline:voiceLines` writes it with the keys and settings to
   `pipeline/build/voice/lines.json` for the renderer. A line with `{ally}` expands to one line
   per convoy unit (One–Five in Level 04). The text as spoken drops the `*` of italics and reads
   a dash as a comma. A
   line that is only a stage direction in square brackets (the Choir's `[the Choir sings]` in
   Act 1) is not spoken: it stays text, with the music's choir pads.
2. **Key.** Each line gets a key: a hash (SHA-256, first 12 hex digits) of the speaker's voice
   (slug, reference clip, the settings the line is spoken with, the layering), the text as
   spoken, the `expression`, the `shout` flag and the filter (radio, distorted or dry: briefing
   pages are dry). The file is
   `assets/voice/<speaker-slug>/<hash>.ogg` (e.g. `assets/voice/okafor/3f9a0c1b7e2d.ogg`); the
   game finds it by computing the same key from the cue. A line whose key already has a file is
   **not re-rendered**; changing the text, the expression or a speaker's settings changes the key,
   so only that line is redone. The renderer deletes the files no line uses any more (`--keep`
   only lists them).
3. **Render.** Chatterbox speaks the line with the speaker's reference clip and the settings of
   the expression (below). Each take has a fixed seed (from the key); the generation is capped
   at 1.5× the expected length from the word count (2.6 words/s plus a little per punctuation
   mark), and a take that hits the cap or runs past it, or is shorter than half the expected
   length, is retried with the next seed (up to eight); the retries are logged. Long lines (the
   briefing pages) are spoken by sentences, in chunks of up to 28 words, joined with 0.35 s of
   silence; leading and trailing silence is trimmed. A take can be pinned by seed in the speaker
   table (`pins`, by key) when the automatic pick sounds wrong.
4. **Post.** The Choir's layering (below) where it applies, then the radio filter as in
   `radio()` of the round's generator: 0.15 s pad either side, static, 300 Hz high-pass and
   3.4 kHz low-pass (two 12 dB/oct passes each), tanh saturation (drive 2), a 4 ms click in and
   out, mastered to −16 LUFS with a −1.5 dBFS ceiling. The static is **filter b** of round 18
   (user decision): a steady hiss bed (noise ×0.019 with a slow 0.7 Hz / 3.1 Hz flutter of
   ±25 %) and crackle on 0.1 % of the samples at up to 0.11, about 27 dB under the voice; it is
   `radio()`'s default in the round's generator. **Briefing pages** skip the radio filter: the
   same render and mastering (−16 LUFS, −1.5 dBFS), no band-pass, static or clicks, since the
   briefing is a face-to-face talk, not a transmission. OGG Vorbis q4, 44.1 kHz mono, with a
   comment tag naming the tool, the speaker and the key.
5. **Commit.** The rendered OGGs are committed under LFS like the other assets.

The renderer is the production generator `tools/art/voice.py`: it runs Gradle for the line list,
Chatterbox in the TTS venv in `~/.cache/tv-tts/` (outside the repo and the Gradle build) and the
post-process with the system Python.

### Reference voices

One reference clip per speaker, **CC0 or public domain** only, never a celebrity or a known
person: LibriVox chapter readings (a 12 s cut, 24 kHz mono) whose archive.org item carries a
public-domain or CC0 licence. The clips live **in the repo** under [refs](refs/README.md)
(`ref-<speaker>.wav` plus the Whisper transcript the cut was checked against), stored in **Git
LFS**, each recorded in [CREDITS.md](../../../CREDITS.md) with the reader, the book and
chapter, the archive.org URL, the licence and the cut.

- **Main cast** (user decision): round 18's readers stay: Okafor (Ruth Golding, CC0 1.0), Varga
  and the Choir's base (Betsie Bush), Rook (Rick Rodstrom), all public domain.
The cast (round 18 for the main cast, [round 19](../../concept-rounds/round-19/README.md) for the
generic speakers; the speaker table in [data.yaml](data.yaml) maps the speakers' names to these):

| Speaker | Reader | Reference clip |
|---|---|---|
| Okafor | Ruth Golding | `refs/ref-okafor.wav` |
| Varga, the Choir's base | Betsie Bush | `refs/ref-varga.wav` |
| Rook | Rick Rodstrom | `refs/ref-rook.wav` |
| Yard Control | Mark Nelson | `refs/ref-yard-control.wav` |
| Dock One–Four (one voice) | Kristin Hughes | `refs/ref-dock.wav` |
| Ring Control | Cori Samuel | `refs/ref-ring-control.wav` |
| Tranquility Control | Karen Savage | `refs/ref-tranquility-control.wav` |
| Crawler One | Kara Shallenberg | `refs/ref-crawler-one.wav` |
| Convoy | Adrian Praetzellis | `refs/ref-convoy.wav` |
| Hammer Lead | Gord Mackenzie | `refs/ref-hammer-lead.wav` |
| Driver Control | Alex Foster | `refs/ref-driver-control.wav` |

- **Generic speakers** get readers from the same sources, never a main-cast reader, auditioned in
  [round 19](../../concept-rounds/round-19/README.md). One voice per role, so a role keeps its
  voice across levels: Act 1 has Yard Control and the four docks (Level 02), Ring Control
  (Level 03), Tranquility Control, Crawler One and Convoy (Level 04) and Hammer Lead (the
  Airstrike special). Every role keeps its own voice (round 19, item 8); the four docks are one
  role.
  Level 05 adds **Driver Control** (the mass driver's operator, three lines), cast in
  [round 21](../../concept-rounds/round-21/README.md) (Alex Foster, a).
  Level 06 adds the **Daedalus perimeter beacon** (automated, one line that also loops faintly
  under section 2 as ambience). It gets its own audition in M4 part F's concept round: two CC0 or
  public-domain candidates through a **public-address filter** (more band-limited than the radio
  filter, with a slight echo), the same rendered line feeding the ambience loop. Until then it is
  `uncast` (text with the radio blips); the PA filter is a new filter value next to radio,
  distorted and dry (planned, part F).

### Speakers and expression

A speaker table (a `data.yaml` next to this README) lists each speaker's slug, reference clip,
base `exaggeration`, `cfg_weight`, `temperature` and optional pinned seeds. The cue's
`expression` adjusts them; round 18's settings are the starting point:

| Expression | exaggeration | cfg_weight | temperature | Round 18 example |
|---|---|---|---|---|
| neutral | 0.5 | 0.5 | 0.7 | Varga, calm |
| grim | 0.45 | 0.5 | 0.7 | Okafor, "That's the yards…" |
| fierce | 0.8 | 0.4 | 0.8 | Rook, joking (the same settings) |
| shout (a cue with `shout: true`) | 1.2 | 0.3 | 0.8 | Tranquility Control's shout |

A speaker's own base values can shift the row (Rook livelier, Okafor drier). `distorted` cues
(a damaged channel) get a stronger filter: more static and dropouts.

**Shouting** is its own field on a radio cue, `shout: true` (user decision), separate from the
queue priority: `urgent` stays a queue priority only (it interrupts, see *Playback*), and a line
can be shouted without interrupting, or interrupt without being shouted. A shouted line uses the
shout row whatever its `expression` (the portrait still follows the `expression`).

### The Choir

The Vrell's Choir is the engine's line in the Varga reference voice (exaggeration 0.3,
cfg_weight 0.3), normalised and layered in post as in `choir()` of the round's generator: copies
at −12, −5, 0 and +7 semitones (same length, gains 0.8 / 0.55 / 0.45 / 0.3), staggered 0–45 ms,
plus a reversed copy at 0.22, over a 55/82 Hz drone with low noise, through a 2.2 s reverb
(35 % wet); then the radio filter. The Choir is **voiced** (user decision), layered as in round
18. Its Act 1 cues are stage directions (`[the Choir sings]`), which are not spoken; its first
spoken words come in Acts 3–4.

### Briefings

The briefing pages (a level's and an act's) are **voiced** (user decision) by the same pipeline
and the same speaker table, without the radio filter (see *Post* above). The voice starts with
the page; the page's text shows in full as today, and the player can skip to the next page,
which stops the voice. The briefing music ducks while a page speaks, as the level music does for
the radio.

### Playback

- The voice file starts with the radio message, together with the portrait's static and the
  subtitle's typing; the subtitle stays (and can be read at its own speed). A subtitle page is
  **held until its voice line ends** (user decision): its 3 s / 5 s timing is a minimum, and the
  next page or message waits for the voice.
- The voice plays on its own **voice** bus (the voice slider times the master volume); the music
  ducks by the existing 4 dB while it plays (the duck now follows the voice, not only the shown
  message).
- An **URGENT** line interrupts: the playing voice is cut (a short fade, ~20 ms, with the squelch
  click), the urgent line plays, and the interrupted line replays from its start, as the
  subtitle already does.
- Pausing the game pauses the voice; leaving the level stops it.
- **Voice volume**: a **separate slider** (user decision) in Options' Audio tab next to the
  music, effects and radio volumes, times the master volume; the radio-blip (radio bus) volume
  stays as it is. It applies to the radio voice and the briefings.
- A missing voice file is not an error: the line shows as text with the blips, as today.

### Build and CI

The game only reads the rendered OGGs in `assets/voice/`; the Gradle build and CI never run TTS
and need no GPU, Python or model. A `content` test checks that every radio cue has its voice file
(by key), so a changed line without a render fails `./gradlew check` with the list of missing
lines; until every level is rendered the check lists them without failing (switched on per
level).

## Implementation

- [ ] Reference clips per speaker in `design/audio/voice/refs/` under LFS, each in CREDITS.md
      (main cast done; the generic speakers after round 19)
- [x] Speaker table (`data.yaml`) with clips, base settings and the expression offsets
- [x] Line list and key (hash of speaker settings, text, expression, `shout`, filter; `{ally}`
      and difficulty variants expanded; briefing pages included; bracketed stage directions
      skipped) shared by the renderer and the game
- [x] Radio cue field `shout` in the data model and the
      [data-file schemas](../../tech/architecture/README.md#data-file-schemas), separate from
      the queue priority; the shouted Act 1 lines marked
- [x] Offline renderer: Chatterbox in its venv, several takes, length check, the Choir's layering,
      the radio filter (filter b; none for briefing pages), OGG under
      `assets/voice/<speaker>/<hash>.ogg`; skips lines already rendered; deletes unused files
- [ ] Act 1 lines rendered and committed; reviewed in a concept round (rendered, not yet
      committed or reviewed; Level 05's 20 lines rendered 2026-10-03, Driver Control's after its casting on 2026-10-04)
- [x] Playback with the radio message: voice on its own voice bus, music ducking while it plays,
      URGENT cuts and replays, a subtitle page held until its voice ends, pause and stop,
      text-only fallback; the mission failed screen speaks the level's line (the cut has no
      20 ms fade yet: a libGDX sound stops at once)
- [x] Briefing pages voiced: the voice starts with the page, stops on skip, ducks the music
- [x] Separate voice volume slider in Options' Audio tab, stored in the settings file
- [x] `content` test: every radio cue has its voice file; CI runs no TTS
- [x] [Options](../../ui/options/README.md) and [HUD](../../ui/hud/README.md) documents updated
      for the voice

## Concept art

Concept [round 19](../../concept-rounds/round-19/README.md) — casting the generic speakers: each
speaker says one of its real lines with two candidate reference voices (`-a`, `-b`), rendered by
Chatterbox through radio filter b (`tools/concept/audio/tts_r19.py`; lines, settings and readers
in [concept/prompts.md](concept/prompts.md), the clips in [refs](refs/README.md)).

| File | What | Status |
|---|---|---|
| [concept/voice-yard-control-r19-a.ogg](concept/voice-yard-control-r19-a.ogg) | Yard Control, reader Mark Nelson (`tools/concept/audio/tts_r19.py`) | chosen |
| [concept/rejected/voice-yard-control-r19-b.ogg](concept/rejected/voice-yard-control-r19-b.ogg) | Yard Control, reader Elizabeth Klett (`tools/concept/audio/tts_r19.py`) | rejected |
| [concept/rejected/voice-dock-r19-a.ogg](concept/rejected/voice-dock-r19-a.ogg) | Dock One–Four, reader Tom Crawford (`tools/concept/audio/tts_r19.py`) | rejected |
| [concept/voice-dock-r19-b.ogg](concept/voice-dock-r19-b.ogg) | Dock One–Four, reader Kristin Hughes (`tools/concept/audio/tts_r19.py`) | chosen |
| [concept/rejected/voice-ring-control-r19-a.ogg](concept/rejected/voice-ring-control-r19-a.ogg) | Ring Control, reader Bob Neufeld (`tools/concept/audio/tts_r19.py`) | rejected |
| [concept/voice-ring-control-r19-b.ogg](concept/voice-ring-control-r19-b.ogg) | Ring Control, reader Cori Samuel (`tools/concept/audio/tts_r19.py`) | chosen |
| [concept/rejected/voice-tranquility-control-r19-a.ogg](concept/rejected/voice-tranquility-control-r19-a.ogg) | Tranquility Control, reader John Greenman (`tools/concept/audio/tts_r19.py`) | rejected |
| [concept/voice-tranquility-control-r19-b.ogg](concept/voice-tranquility-control-r19-b.ogg) | Tranquility Control, reader Karen Savage (`tools/concept/audio/tts_r19.py`) | chosen |
| [concept/rejected/voice-crawler-one-r19-a.ogg](concept/rejected/voice-crawler-one-r19-a.ogg) | Crawler One, reader Phil Chenevert (`tools/concept/audio/tts_r19.py`) | rejected |
| [concept/voice-crawler-one-r19-b.ogg](concept/voice-crawler-one-r19-b.ogg) | Crawler One, reader Kara Shallenberg (`tools/concept/audio/tts_r19.py`) | chosen |
| [concept/voice-convoy-r19-a.ogg](concept/voice-convoy-r19-a.ogg) | Convoy, reader Adrian Praetzellis (`tools/concept/audio/tts_r19.py`) | chosen |
| [concept/rejected/voice-convoy-r19-b.ogg](concept/rejected/voice-convoy-r19-b.ogg) | Convoy, reader Judy Bieber (`tools/concept/audio/tts_r19.py`) | rejected |
| [concept/rejected/voice-hammer-lead-r19-a.ogg](concept/rejected/voice-hammer-lead-r19-a.ogg) | Hammer Lead, reader Mark F. Smith (`tools/concept/audio/tts_r19.py`) | rejected |
| [concept/voice-hammer-lead-r19-b.ogg](concept/voice-hammer-lead-r19-b.ogg) | Hammer Lead, reader Gord Mackenzie (`tools/concept/audio/tts_r19.py`) | chosen |
| [concept/voice-driver-control-r21-a.ogg](concept/voice-driver-control-r21-a.ogg) | Driver Control (round 21 audition), reader Alex Foster (`tools/concept/audio/tts_r21.py`) | chosen |
| [concept/rejected/voice-driver-control-r21-b.ogg](concept/rejected/voice-driver-control-r21-b.ogg) | Driver Control (round 21 audition), reader Rebecca (`tools/concept/audio/tts_r21.py`) | rejected |

## Open questions

- Who voices the Daedalus perimeter beacon (Level 06)? Audition of two CC0/PD candidates through
  the public-address filter in M4 part F's concept round; `uncast` until the user picks.

## Decisions

- 2026-10-03: Created after concept round 18: Chatterbox chosen for the spoken radio lines (user:
  "by far the best"), reversing the 2026-10-01 "no voice acting" decision in
  [Audio](../README.md#decisions); the text stays as subtitles. Draft for the user's review.
- 2026-10-03: The open questions decided by the user: radio filter **b** (more static, round 18
  item 2); the main cast keeps round 18's LibriVox readers, the generic speakers get readers from
  the same CC0 and public-domain sources, auditioned in a short round (round 19); the Choir is
  voiced (layered as in round 18) and the briefings too (same pipeline, without the radio
  filter); a separate voice volume slider; a subtitle page is held until its voice line ends; a
  `shout` field on radio cues, separate from the queue priority (user decision).
- 2026-10-03: The reference clips moved into the repo (`refs/`, Git LFS, in CREDITS.md); concept
  round 19 opened to cast the generic speakers.
- 2026-10-03: Round 19 closed (user): Yard Control Mark Nelson (a), the docks Kristin Hughes (b),
  Ring Control Cori Samuel (b), Tranquility Control Karen Savage (b, not round 18's David Leeson),
  Crawler One Kara Shallenberg (b), Convoy Adrian Praetzellis (a), Hammer Lead Gord Mackenzie (b);
  no voices merged. The rejected takes moved to `concept/rejected/`, the unused clips were
  deleted with their CREDITS.md rows.
- 2026-10-03: Implemented: the speaker table, the line list and keys (`VoiceLines`), the `shout`
  field (seven shouted Act 1 lines: Rook's and Okafor's flank warnings in Level 01, Dock One,
  Rook's right-flank call and Dock Four in Level 02, Convoy and Tranquility Control's walkers in
  Level 04), the renderer `tools/art/voice.py` (it deletes unused files rather than only listing
  them), playback in the level, the briefings and the mission failed screen, and the voice
  slider. Act 1's lines rendered; their review and the timing of the voiced radio are open.
- 2026-10-03: M4 part E (user decision): Level 05's new speaker Driver Control gets its own voice
  from a short audition (two CC0 or public-domain candidates, as in round 19), held later in the
  part; no voice is merged with Tranquility Control.
- 2026-10-03: M4 part E: until its audition, Driver Control is marked `uncast: true` in the
  speaker table (no `ref`): its Level 05 lines have no voice file and play as text with the radio
  blips; VoiceFilesTest allows a missing voice only for such a speaker, and Level 05's lines are
  checked (20 rendered: the radio, the four per-battery failure lines from `{group}`, the
  secret and the briefing pages).
- 2026-10-04: Driver Control audition (user decision D4 of M4 part E) opened in round 21: its
  t=12.5 warning with two public-domain LibriVox voices (`tools/concept/audio/tts_r21.py`).
- 2026-10-04: Round 21 decided (user): Driver Control is Alex Foster (a); `uncast` removed from
  the speaker table, Rebecca's take moved to `concept/rejected/` and her clip deleted with its
  CREDITS.md row; Driver Control's lines rendered by `tools/art/voice.py`.
- 2026-10-04: M4 part F (user decision D7): Level 06's Daedalus perimeter beacon gets an audition
  of two CC0/PD candidates through a public-address filter, in the part's concept round; the same
  line loops under section 2. `uncast` until then. Rejected: reusing a cast generic voice with the
  PA filter (no audition).
