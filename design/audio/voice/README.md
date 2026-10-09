---
title: Voice
design: draft
implementation: in-progress
art: chosen
depends-on: [.., ../../story/characters, ../../ui/hud, ../../ui/options, ../../tech/architecture]
updated: 2026-10-09
---

# Voice

## Summary

The radio lines are spoken. Every line in the level data is rendered offline by the Chatterbox
text-to-speech engine, through the shared radio filter, into OGG files under `assets/`; the game
plays the file with the radio subtitle, which stays on screen. The briefings and the Choir are
voiced by the same pipeline; a line that is only a stage direction (`[the Choir sings]`) plays its
speaker's stage sound instead. The engine and the radio filter were chosen in concept
[round 18](../../concept-rounds/round-18/README.md), the generic speakers' voices are cast in
[round 19](../../concept-rounds/round-19/README.md); the build and CI never run the engine.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [refs](refs/README.md) | Reference clips the voices are cloned from: LibriVox readings, public domain or CC0 | draft | in-progress | chosen |

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
   line, the `easy` / `hard` line variants, a special's call such as Hammer Lead's, Okafor's
   low-armour line from the [armour](../../player/armor/README.md) data, Rook's 27 radio barks
   from the [wingmen](../../player/wingmen/README.md#radio-barks) data, source
   `wingmen barks <trigger>`), and every
   page of the levels' and acts' briefings (not the hangar teaser); `./gradlew
   :pipeline:voiceLines` writes it with the keys and settings to
   `pipeline/build/voice/lines.json` for the renderer. A line with `{ally}` expands to one line
   per convoy unit (One–Five in Level 04); a line with `{side}` (M5 part B) expands to two, `left`
   and `right`, and the game shows and plays the one of Rook's side
   ([wingmen](../../player/wingmen/README.md#scripted-lines-about-him-user-decision-d4-of-m5-part-b)). The text as spoken drops the `*` of italics and reads
   a dash as a comma. A
   line that is only a stage direction in square brackets (the Choir's `[the Choir sings]` in
   Act 1) is not spoken and is not in the list: it shows as text and plays its speaker's **stage
   sound** instead (`stage` in the speaker table, a file in `assets/voice/<speaker>/` that no key
   names; the Choir's sung sting, concept [round 29](../../concept-rounds/round-29/README.md)).
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
   mark, more per sentence end: 0.12 s a comma, 0.4 s a sentence end, an ellipsis counting once),
   and a take that hits the cap or runs past it, or is shorter than half the expected
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
   briefing is a face-to-face talk, not a transmission. A speaker marked `filter: pa` (the
   Level 06 perimeter beacon) gets the **public-address filter** instead of the radio filter
   (`pa()` next to `radio()`: a 250 Hz–4 kHz horn band with resonances at 1.1 and 2.6 kHz, a
   driven horn's saturation, a faint mains hum, slap echoes at 0.13, 0.29 and 0.47 s and a 1.4 s
   hall; no static or clicks), as auditioned in round 23. A radio line whose text ends in a
   **dash** is cut off (Level 10's Lifeline Three, "…What is that—", as auditioned in round 32):
   Whisper (faster-whisper base.en) times the raw take's words, the take is cut hard (3 ms) where
   its last word ends (Whisper's end falls inside the word's tail, so the word is clipped), then
   0.3 s of static as loud as the voice's peak (hiss and dense crackle, breaking up, held 0.12 s
   and dying) and 0.25 s of fading hiss, all through filter b; the file ends there, without the
   closing click: the channel goes dead. OGG Vorbis q4, 44.1 kHz mono, with a
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
| Perimeter beacon | Mark F. Smith | `refs/ref-perimeter-beacon.wav` |
| Lifeboat Seven | Tadhg Hynes | `refs/ref-lifeboat-seven.wav` |
| Civilian (shelter nine) | Faith Abiola-Ellison | `refs/ref-civilian.wav` |
| Kilo Lead | Aaron Bennett | `refs/ref-kilo-lead.wav` |
| Lifeline (Lifeline One, Two, Four, Five, the hit line) | KevinS | `refs/ref-lifeline.wav` |
| Lifeline Three | Maria Kasper | `refs/ref-lifeline-three.wav` |

- **Generic speakers** get readers from the same sources, never a main-cast reader, auditioned in
  [round 19](../../concept-rounds/round-19/README.md). One voice per role, so a role keeps its
  voice across levels: Act 1 has Yard Control and the four docks (Level 02), Ring Control
  (Level 03), Tranquility Control, Crawler One and Convoy (Level 04) and Hammer Lead (the
  Airstrike special). Every role keeps its own voice (round 19, item 8); the four docks are one
  role.
  Level 05 adds **Driver Control** (the mass driver's operator, three lines), cast in
  [round 21](../../concept-rounds/round-21/README.md) (Alex Foster, a).
  Level 06 adds the **Daedalus perimeter beacon** (automated, one line that also loops faintly
  under section 2 as ambience), cast in [round 23](../../concept-rounds/round-23/README.md) (Mark
  F. Smith, a): flat, even settings (`fixed`: exaggeration 0.3, cfg_weight 0.5, temperature 0.6)
  and the **public-address filter** (`filter: pa` in the speaker table) instead of the radio
  filter; the same rendered line feeds the ambience loop.
  Level 07 adds **Lifeboat Seven** (a drifting CDF lifeboat's crew, one line at t=22), cast in
  [round 25](../../concept-rounds/round-25/README.md) (Tadhg Hynes, a), at the neutral settings
  through radio filter b, as auditioned.
  Level 08 adds the **Ikoyi shelter civilian** (speaker `Civilian`, "shelter nine": a civilian in
  a Nova Lagos shelter calling for help, two lines, the call and the secondary objective's thanks;
  the radio portrait `radio-generic-civilian`), auditioned in concept round 30 (user decision D6 a
  of M5 part B): her two real lines with two candidate readers (a, b) from the same CC0 and
  public-domain sources, never a main-cast reader, through radio filter b at the neutral settings
  (`tools/concept/audio/tts_r30.py`), and cast in that round: Faith Abiola-Ellison (b, a woman's
  voice, clip pitch 151 Hz), at the neutral settings through radio filter b, as auditioned; her
  two lines are voiced (both takes pinned in the speaker table).
  Level 09 adds the **Kilo Lead** (speaker `Kilo Lead`: the CDF officer of the Kilo truck convoy
  on the Okonjo Bridge, two lines, the call at t=95 and the secondary objective's thanks; the radio
  portrait `radio-generic-cdf`), auditioned in concept round 31 (user decision D11 = a of M5 part
  C) as the civilian was: his two real lines with two new candidate readers (a, b) from the same
  CC0 and public-domain sources, never a main-cast reader nor a voice already cast for another role,
  through radio filter b at the neutral settings (`tools/concept/audio/tts_r31.py`), and cast in
  that round: Aaron Bennett (a, clip pitch 129 Hz), at the neutral settings through radio filter
  b, as auditioned; his two lines are voiced.
  Level 10 adds two shuttle pilots (user decision D12 = a of M5 part D), each auditioned a/b in
  concept round 32 as the Kilo Lead was (two new candidate readers from the same CC0 and
  public-domain sources, never a main-cast reader nor a voice already cast, through radio filter b
  at the neutral settings, `tools/concept/audio/tts_r32.py`; the radio portrait
  `radio-generic-civilian`): **Lifeline**, Lifeline One's pilot (the t=1 call and the t=196 thanks),
  who is also the voice of Lifeline Two, Four and Five and of the hit line ("Lifeline {ally}, we're
  hit!…", one take per shuttle by `{ally}`): one voice under several names, as the docks; and
  **Lifeline Three**, the lost shuttle's pilot, one line cut off by the lance ("…What is that—";
  the dash reads as a comma, and the renderer cuts the take off after its last word, see
  *Offline pipeline*). Both were cast in that round (user, 2026-10-08): Lifeline is KevinS (b, clip
  pitch 138 Hz), Lifeline Three Maria Kasper (b, 215 Hz), at the neutral settings through radio
  filter b, as auditioned; their lines are voiced.
  Level 11 adds **Atlas Control** (user decision E10 = a of M5 part E; speaker `Atlas Control`: the
  CDF officer of convoy Atlas-Seven; the radio portrait `radio-generic-cdf`), auditioned a/b in
  concept [round 33](../../concept-rounds/round-33/README.md) as the Kilo Lead and Lifeline were: real lines with two new candidate readers from
  the same CC0 and public-domain sources, never a main-cast reader nor a voice already cast, through
  radio filter b at the neutral settings (`tools/concept/audio/tts_r33.py`), marked `uncast: true`
  in the speaker table until the round picks. The candidates: a, Alister (a male voice, clip pitch
  106 Hz, below the Kilo Lead's 129 Hz so the two CDF officers differ), and b, MaryAnn (a female
  voice, 174 Hz, between Dock's 167 Hz and Varga's 193 Hz). Its lines: three timed calls (t=1, t=62.5, t=110), the
  ship-hit and ship-lost lines once per ship name (`{ally}` becomes a naval convoy unit's name,
  Halvorsen, Mbeki or Saint-Laurent: three takes each) and the secondary objective's thanks, about
  ten takes. Cast in round 33 (2026-10-09): Atlas Control is MaryAnn (b), at the neutral settings
  through radio filter b, as auditioned; her ten takes are voiced.

### Speakers and expression

A speaker table (a `data.yaml` next to this README) lists each speaker's slug, reference clip,
base `exaggeration`, `cfg_weight`, `temperature`, optional pinned seeds and an optional `stage`
sound (see *The Choir*). The cue's
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
spoken words come in Acts 3–4. Such a cue plays the Choir's **stage sound** (`stage` in the
speaker table) on the voice bus with the subtitle, as a voice file plays: a few seconds of the
Choir singing without words, at the spoken lines' loudness (−16 LUFS) through radio filter b.
Concept [round 29](../../concept-rounds/round-29/README.md) offers two kinds, a wordless sting
sung in the Choir's voice (Varga's reference voice with the layering above; a, b) and a
synthesized choir pad like the music's (c, d); option **a** plays until the user's choice. The
file is copied into `assets/voice/choir/` by `./gradlew :pipeline:copyPlaceholderStageSounds`;
the renderer leaves files not named by a key alone.

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
- A stage-direction line (`[the Choir sings]`) plays its speaker's stage sound as its voice file:
  the subtitle is held until it ends and the music ducks under it.

### Build and CI

The game only reads the rendered OGGs in `assets/voice/`; the Gradle build and CI never run TTS
and need no GPU, Python or model. A `content` test checks that every radio cue has its voice file
(by key), so a changed line without a render fails `./gradlew check` with the list of missing
lines; until every level is rendered the check lists them without failing (switched on per
level).

## Implementation

- [x] Reference clips per speaker in `design/audio/voice/refs/` under LFS, each in CREDITS.md
      (Act 1's: the main cast, the generic speakers of round 19, Driver Control, the perimeter
      beacon and Lifeboat Seven)
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
- [x] Act 1 lines rendered and committed; reviewed in a concept round (all 151 of the line list
      in `assets/voice/`: Level 07's 18 lines and the Act 1 outro's four pages accepted in round 25;
      the act briefing, Levels 01–06's briefing pages and radio lines and Hammer Lead's Airstrike
      call, 128 lines, and the low-armour line accepted as rendered in round 26)
- [x] Rook's 27 radio barks (M5 part A, source `wingmen barks <trigger>`) rendered, four takes
      pinned, and accepted as rendered in round 28
- [x] Playback with the radio message: voice on its own voice bus, music ducking while it plays,
      URGENT cuts and replays, a subtitle page held until its voice ends, pause and stop,
      text-only fallback; the mission failed screen speaks the level's line (the cut has no
      20 ms fade yet: a libGDX sound stops at once)
- [x] Briefing pages voiced: the voice starts with the page, stops on skip, ducks the music
- [x] Separate voice volume slider in Options' Audio tab, stored in the settings file
- [x] `content` test: every radio cue has its voice file; CI runs no TTS
- [x] [Options](../../ui/options/README.md) and [HUD](../../ui/hud/README.md) documents updated
      for the voice
- [x] `{side}` in a line: two lines (`left`, `right`) in the line list, the game playing the one of
      Rook's side (M5 part B; `VoiceLines.SIDE`, `RadioSchedule.line`)
- [x] The Ikoyi shelter civilian: reference clips for the round-30 audition under `refs/` and in
      CREDITS.md, the chosen reader in the speaker table (M5 part B; b, Faith Abiola-Ellison,
      `refs/ref-civilian.wav`)
- [x] Level 08's lines (radio, the `{side}` takes, the secret, the briefing pages) and the Act 2
      act briefing rendered and reviewed in concept round 30; the civilian's after her casting
      (M5 part B; rendered 2026-10-06, 19 lines, three takes pinned; the civilian's two lines
      rendered 2026-10-07, both takes pinned; accepted as rendered when round 30 closed)
- [x] The Kilo Lead (Level 09): reference clips for the round-31 audition under `refs/` and in
      CREDITS.md, the chosen reader in the speaker table (M5 part C, D11 = a; a, Aaron Bennett,
      `refs/ref-kilo-lead.wav`)
- [x] Level 09's lines (radio, the briefing pages, the cocoon's line, the mission failed screen's
      `{group}` line once per node) rendered and reviewed in concept round 31; the Kilo Lead's
      after his casting (M5 part C; rendered 2026-10-07, 20 lines, four takes pinned; the Kilo
      Lead's two lines rendered 2026-10-07, no pins needed; accepted as rendered when round 31
      closed its voice rows)
- [x] Lifeline and Lifeline Three (Level 10): reference clips for the round-32 audition under
      `refs/` and in CREDITS.md, the chosen readers in the speaker table (M5 part D, D12 = a; b,
      KevinS, `refs/ref-lifeline.wav`; b, Maria Kasper, `refs/ref-lifeline-three.wav`)
- [x] A line ending in a dash is cut off after its last word into static and a dead channel
      (`tools/art/voice.py`, Lifeline Three's t=116 line)
- [x] Level 10's lines (radio, the briefing pages, the ferry's line, the mission failed screen's
      line, the four level-end lines with the numbers spoken) rendered and reviewed in concept
      round 32; the Lifeline speakers' after their casting (M5 part D; the cast speakers' rendered
      2026-10-08, 22 lines, four takes pinned; the Lifeline speakers' eight lines rendered
      2026-10-08, five takes pinned)
- [x] Atlas Control (Level 11): reference clips for the round-33 audition under `refs/` and in
      CREDITS.md, the chosen reader in the speaker table (M5 part E, E10 = a; steps E1a and E4; b,
      MaryAnn, `refs/ref-atlas-control.wav`)
- [x] `{ally}` as a naval convoy unit's name in the line list (one take per cargo ship name;
      `VoiceLines.radio`, `NavalConvoyLinesTest`; the escort frigate takes no lane, is never hit or
      lost and has none) (M5 part E, steps E2b and E4)
- [x] Level 11's lines (radio, the briefing pages, the pod's line, the level-end lines) rendered and
      reviewed in concept [round 33](../../concept-rounds/round-33/README.md); Atlas Control's after the casting (M5 part E, step E4; the cast
      speakers' rendered 2026-10-09, 15 lines, three takes pinned, accepted as rendered when round
      33 closed; Atlas Control's ten takes rendered 2026-10-09 at the close, no pins needed)
- [x] A line that is only a stage direction plays its speaker's `stage` sound (speaker table) on
      the voice bus, found by `VoiceLines.radioVoice` for the game and RadioTimelineTest; the
      Choir's is round 29's option b (the sung "ah" F3 to E3), copied by
      `:pipeline:copyPlaceholderStageSounds`; VoiceFilesTest checks the file and every Choir cue

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
| [concept/voice-perimeter-beacon-r23-a.ogg](concept/voice-perimeter-beacon-r23-a.ogg) | Daedalus perimeter beacon (round 23 audition, public-address filter), reader Mark F. Smith (`tools/concept/audio/tts_r23.py`) | chosen |
| [concept/rejected/voice-perimeter-beacon-r23-b.ogg](concept/rejected/voice-perimeter-beacon-r23-b.ogg) | Daedalus perimeter beacon (round 23 audition, public-address filter), reader Lucy Burgoyne (`tools/concept/audio/tts_r23.py`) | rejected |
| [concept/voice-lifeboat-seven-r25-a.ogg](concept/voice-lifeboat-seven-r25-a.ogg) | Lifeboat Seven (round 25 audition), reader Tadhg Hynes (`tools/concept/audio/tts_r25.py`) | chosen |
| [concept/rejected/voice-lifeboat-seven-r25-b.ogg](concept/rejected/voice-lifeboat-seven-r25-b.ogg) | Lifeboat Seven (round 25 audition), reader Lizzie Driver (`tools/concept/audio/tts_r25.py`) | rejected |
| [concept/rejected/voice-choir-sings-r29-a.ogg](concept/rejected/voice-choir-sings-r29-a.ogg) | The Choir sings (round 29): sung sting, "ooh" held on E3 in the Choir's voice, layered (`tools/concept/audio/choir_r29.py`) | rejected |
| [concept/voice-choir-sings-r29-b.ogg](concept/voice-choir-sings-r29-b.ogg) | The Choir sings (round 29): sung sting, "ah" falling F3 to E3 in the Choir's voice, layered (`tools/concept/audio/choir_r29.py`) | chosen |
| [concept/rejected/voice-choir-sings-r29-c.ogg](concept/rejected/voice-choir-sings-r29-c.ogg) | The Choir sings (round 29): synthesized choir pad, "oo" chord E3 B3 E4 B4 (`tools/concept/audio/choir_r29.py`) | rejected |
| [concept/rejected/voice-choir-sings-r29-d.ogg](concept/rejected/voice-choir-sings-r29-d.ogg) | The Choir sings (round 29): synthesized choir pad, the motif's F4 to E4 over an "oo" pad (`tools/concept/audio/choir_r29.py`) | rejected |

Concept [round 30](../../concept-rounds/round-30/README.md) (M5 part B) — casting the Ikoyi
shelter civilian of Level 08: her two lines (the t=113 call and the secondary objective's thanks,
1 s apart) with two candidate reference voices (`-a`, `-b`), rendered by Chatterbox at the neutral
settings through radio filter b (`tools/concept/audio/tts_r30.py`; the clips
`refs/ref-civilian-r30-a.wav` and `-b.wav`). Closed 2026-10-07: **b** chosen (its clip renamed
`refs/ref-civilian.wav`); a in `concept/rejected/`, its clip deleted.

| File | What | Status |
|---|---|---|
| [concept/rejected/voice-civilian-r30-a.ogg](concept/rejected/voice-civilian-r30-a.ogg) | The Ikoyi shelter civilian (round 30 audition), reader KirksVoice (`tools/concept/audio/tts_r30.py`) | rejected |
| [concept/voice-civilian-r30-b.ogg](concept/voice-civilian-r30-b.ogg) | The Ikoyi shelter civilian (round 30 audition), reader Faith Abiola-Ellison (`tools/concept/audio/tts_r30.py`) | chosen |

Concept [round 31](../../concept-rounds/round-31/README.md) (M5 part C) — casting the Kilo Lead of
Level 09: his two lines (the t=95 call and the secondary objective's thanks, 1 s apart) with two
candidate reference voices (`-a`, `-b`), rendered by Chatterbox at the neutral settings through
radio filter b (`tools/concept/audio/tts_r31.py`). Closed 2026-10-07: **a** chosen (its clip renamed
`refs/ref-kilo-lead.wav`); b in `concept/rejected/`, its clip deleted.
Concept [round 32](../../concept-rounds/round-32/README.md) (M5 part D) — casting Level 10's
Lifeline (three of its lines) and Lifeline Three (its one line, cut off) with two candidate
reference voices each (`tools/concept/audio/tts_r32.py`). Decided 2026-10-08: **b** for both (the
clips renamed `refs/ref-lifeline.wav` and `refs/ref-lifeline-three.wav`); the a auditions in
`concept/rejected/`, their clips deleted.

| File | What | Status |
|---|---|---|
| [concept/voice-kilo-lead-r31-a.ogg](concept/voice-kilo-lead-r31-a.ogg) | The Kilo Lead (round 31 audition), reader Aaron Bennett (`tools/concept/audio/tts_r31.py`) | chosen |
| [concept/rejected/voice-kilo-lead-r31-b.ogg](concept/rejected/voice-kilo-lead-r31-b.ogg) | The Kilo Lead (round 31 audition), reader tombooker (`tools/concept/audio/tts_r31.py`) | rejected |
| [concept/rejected/voice-lifeline-r32-a.ogg](concept/rejected/voice-lifeline-r32-a.ogg) | Lifeline (round 32 audition: Lifeline One's pilot, also shuttles Two, Four, Five and the hit line), reader Atul Sharma (`tools/concept/audio/tts_r32.py`) | rejected |
| [concept/voice-lifeline-r32-b.ogg](concept/voice-lifeline-r32-b.ogg) | Lifeline (round 32 audition), reader KevinS (`tools/concept/audio/tts_r32.py`) | chosen |
| [concept/rejected/voice-lifeline-three-r32-a.ogg](concept/rejected/voice-lifeline-three-r32-a.ogg) | Lifeline Three (round 32 audition; the line cut off by the lance), reader Kehinde (`tools/concept/audio/tts_r32.py`) | rejected |
| [concept/voice-lifeline-three-r32-b.ogg](concept/voice-lifeline-three-r32-b.ogg) | Lifeline Three (round 32 audition; the line cut off by the lance), reader Maria Kasper (`tools/concept/audio/tts_r32.py`) | chosen |

Concept [round 33](../../concept-rounds/round-33/README.md) (M5 part E, user decision E10 = a) — casting Level 11's Atlas Control, the CDF
officer of convoy Atlas-Seven: three of its lines (the t=1 call, the `ally-hit` line with `{ally}` =
Halvorsen and the t=62.5 sonar contact, 1 s apart) with two candidate reference voices, a male (a)
and a female (b), rendered by Chatterbox at the neutral settings through radio filter b
(`tools/concept/audio/tts_r33.py`; the readers and pitches in
[concept/prompts.md](concept/prompts.md#voice-atlas-control)). Closed 2026-10-09: **b** chosen (MaryAnn,
the clip renamed `refs/ref-atlas-control.wav`); the a audition in `concept/rejected/`, its clip
deleted.

| File | What | Status |
|---|---|---|
| [concept/rejected/voice-atlas-control-r33-a.ogg](concept/rejected/voice-atlas-control-r33-a.ogg) | Atlas Control ([round 33](../../concept-rounds/round-33/README.md) audition), reader Alister (`tools/concept/audio/tts_r33.py`) | rejected |
| [concept/voice-atlas-control-r33-b.ogg](concept/voice-atlas-control-r33-b.ogg) | Atlas Control ([round 33](../../concept-rounds/round-33/README.md) audition), reader MaryAnn (`tools/concept/audio/tts_r33.py`) | chosen |

Concept [round 29](../../concept-rounds/round-29/README.md) — the sound of the Choir's stage
direction `[the Choir sings]`: a wordless sung sting in the Choir's voice (a, b) or a synthesized
choir pad (c, d), all through radio filter b (`tools/concept/audio/choir_r29.py`; how each is made
in [concept/prompts.md](concept/prompts.md#voice-choir-sings)). Closed 2026-10-06: **b** chosen;
a, c and d in `concept/rejected/`.

## Open questions

- None open.

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
- 2026-10-04: Perimeter beacon audition (user decision D7 of M4 part F) opened in round 23: its
  t=50 message with two LibriVox voices, Mark F. Smith (a, CC0) and Lucy Burgoyne (b, public
  domain), through the new public-address filter (`pa()` next to `radio()` in
  `tools/concept/audio/tts_r18.py`; horn band and resonances, slap echoes and a hall instead of
  static and squelch clicks); `tools/concept/audio/tts_r23.py`.
- 2026-10-05: Round 23 decided (user): the perimeter beacon is Mark F. Smith (a), through the
  public-address filter only, as auditioned (not the radio filter on top); `uncast` removed,
  `ref-perimeter-beacon.wav`, `fixed` settings as the audition's and `filter: pa` in the speaker
  table (`VoiceLines.Filter.PA`, part of the line's key; `pa()` in `tools/art/voice.py`'s post);
  Lucy Burgoyne's take moved to `concept/rejected/` and her clip deleted with its CREDITS.md row.
  The beacon's line rendered (6.9 s with the echoes' tail; Whisper reads it back), so it is spoken
  at t=52.5 and loops under section 2.
- 2026-10-05: Okafor's Level 06 line "Domes intact. Airlocks open. Nobody's home." hit the length
  cap with all eight seeds: three short sentences, with Okafor's pauses between them, ran past
  1.5× an estimate that gave a sentence end only 0.12 s. The estimate now gives a sentence end
  0.4 s (an ellipsis counts once); re-rendered, the third seed passed (4.6 s; Whisper reads it
  back). Only missing lines are rendered, so no other file changed.
- 2026-10-05: Lifeboat Seven audition (M4 part G, Level 07) opened in round 25: its t=22 line with
  two public-domain LibriVox voices, Tadhg Hynes (a) and Lizzie Driver (b), neutral settings
  (0.5 / 0.5 / 0.7) through radio filter b (`tools/concept/audio/tts_r25.py`). Until the choice the
  speaker is uncast (an `uncast: true` row in the speaker table, written with Level 07's data) and
  its line plays as text.
- 2026-10-05: M4 part G: Level 07's lines (18: the two briefing pages, 15 radio lines and the lifeboat
  tow secret; Okafor 6, Varga 8, Rook 4) and the Act 1 outro's four pages
  (dry, Okafor 3, Varga 1) rendered by `tools/art/voice.py`; Lifeboat Seven's t=22 line stays text
  until round 25 casts it. One take hit the length cap and passed on the next seed (Okafor's
  "Every bay gutted…"); Rook's "Okay. Okay. That was big. We did big." repeated its last sentence
  with the key's seed and was re-rolled on the next seed, 267639418 (to be pinned in the speaker
  table so a rerun keeps it). Whisper reads every take back (names such as Aegis and Daedalus only
  with a name prompt, as in the earlier levels). Outro page 1 is 14.3 s, under track 24's 16.3 s.
  VoiceFilesTest now checks Level 07 and the outro; RadioTimelineTest no longer exempts Level 07.
- 2026-10-05: Round 25 decided (user): Lifeboat Seven is Tadhg Hynes (a), at the audition's
  neutral settings through radio filter b; `uncast` removed, `ref-lifeboat-seven.wav` in the
  speaker table; Lizzie Driver's take moved to `concept/rejected/` and her clip deleted with its
  CREDITS.md row. The t=22 line rendered by `tools/art/voice.py` on the key's seed (6.2 s of speech,
  6.5 s with the filter's tail); Whisper reads it back word for word, "Aegis" included, without a
  name prompt. VoiceFilesTest now requires every speaker of a rendered level to be cast, and
  RadioTimelineTest no longer exempts an uncast speaker's line. Level 07's other lines and the four
  outro pages were accepted as rendered (round 25, choice 10).
- 2026-10-05: M4 part H: Okafor's low-armour line ("Lancer, your hull won't take much more. Fly
  careful.", grim, from the [armour](../../player/armor/README.md) data; source `armour` in the line
  list) rendered by `tools/art/voice.py`'s pipeline, only that line (one GPU job): the key's seed
  passed at once, 4.5 s of speech under the 7.0 s cap (`assets/voice/okafor/14b14dfe36a3.ogg`,
  4.8 s with the filter's pad). Whisper (base.en) reads the dry take back word for word; through
  radio filter b it hears "howl" for "hull", a short word in the band-pass, so the user's ear
  decides in round 26. VoiceFilesTest requires its file; RadioTimelineTest plays it urgent in every
  level.
- 2026-10-05: Concept round 26 closed (user: the round accepted as proposed): the 128 voiced lines
  no round had reviewed (the act briefing's 5 pages, Levels 01–06's 12 briefing pages and 110 radio
  lines, Hammer Lead's Airstrike call) and Okafor's low-armour line accepted as rendered, none to
  re-render; with round 25's Level 07 and outro, all 151 Act 1 lines are reviewed, so the item is
  ticked and the implementation is `done`. `art` stays `chosen`: the later acts' lines are not
  rendered yet. The document's `design` stays `draft` (not part of this round's approval).
- 2026-10-06: M5 part A: Rook's 27 radio barks ([wingmen](../../player/wingmen/README.md#radio-barks),
  source `wingmen barks <trigger>`; grim for the boss warning and the armour barks, the shout row for
  the rear, sides and eject barks, fierce for the kill streak and the overdrive) rendered by
  `tools/art/voice.py` in one GPU job, radio filter b; no other line of the list was missing. Every
  take passed the length check on its first seed (0.96–4.47 s of speech, none near the cap). Whisper
  (base.en) read each take back, dry and through the filter; four were re-rolled on later seeds and
  pinned in the speaker table: "Rook's hurting, Lancer. Hurting bad." (the key's seed said
  "herding", dry too), "Flank! Watch the edges, Lancer!" ("Clank" through the filter), "Power-up!
  Grab it!" (the "P" lost in the filter) and "Overdrive's up for grabs, Lancer!" ("Over drives");
  each pinned seed is the first of four tried that Whisper reads back word for word both ways. The
  rest read back word for word apart from spelling ("6" for "six", "Punchin'", "Hah"). VoiceFilesTest
  now requires the barks' files. For the user's ear in concept round 28.
- 2026-10-06: Concept round 28 closed (user: accepted): Rook's 27 radio barks accepted as rendered,
  the four pinned takes included, none to re-render (the rear-wave variant "Contacts on six! Why is
  it always six?" stays, beside Level 06's scripted take of the same line). `art` stays `chosen`:
  the later acts' lines are not rendered yet.
- 2026-10-06: The Choir's radio messages made no sound (user): its Act 1 cues are the stage
  direction `[the Choir sings]`, which is not spoken, and the rule "stays text, with the music's
  choir pads" played nothing in the line's place. User decision:
  hear both kinds of sound in a concept round and choose by ear; round 29 opened with a wordless
  sung sting in the Choir's voice (a: Chatterbox's own sustained "ooh", pitch-flattened onto E3;
  b: an "ah" built from half a second of vowel, sung F3 to E3) and a synthesized choir pad like the
  music's (c: an "oo" chord; d: the motif's F4 to E4 over a pad), each through the Choir's
  room and radio filter b at −16 LUFS (`tools/concept/audio/choir_r29.py`). Wired data-driven: the
  speaker table's new `stage` field names the sound a speaker's stage-direction line plays
  (`VoiceLines.stageSound` / `radioVoice`, used by `Voices` and RadioTimelineTest), option a
  provisionally, copied into `assets/voice/choir/` by `:pipeline:copyPlaceholderStageSounds`;
  `tools/art/voice.py` now deletes only unused files named by a key.
- 2026-10-06: Concept round 29 closed (user): the Choir's `[the Choir sings]` plays **b**, the
  sung "ah" in the Choir's voice falling F3 to E3, the motif's landing (the speaker table's
  `stage`, copied into `assets/voice/choir/` by `:pipeline:copyPlaceholderStageSounds`). Rejected:
  a (the held "ooh" on E3) and the synthesized pads c ("oo" chord) and d (the motif over a pad),
  moved to `concept/rejected/` (`choir_r29.py`'s `CHOSEN`).
- 2026-10-06: M5 part B (user decisions D4 = a and D6 = a): Level 08's radio is voiced like Act 1's
  (its "text only" note is reversed in the level's document); a line with **`{side}`** expands to a
  `left` and a `right` line, played by Rook's side; the **Ikoyi shelter civilian** ("shelter nine",
  two lines) is auditioned a/b in concept round 30 and plays as text until cast. The document goes
  back to `in-progress` for part B's renders.
- 2026-10-06: Round 30's civilian audition rendered (`tts_r30.py`): a KirksVoice (African Myths,
  clip pitch 111 Hz), b Faith Abiola-Ellison (Yoruba-speaking Peoples, 151 Hz, near Okafor's 150 Hz),
  both LibriVox public domain (Public Domain Mark 1.0), both lines 1 s apart, neutral, radio filter
  b, −16 LUFS; reference clips and Whisper transcripts in `refs/` and CREDITS.md. Measured only.
- 2026-10-06: M5 part B: `{side}` is built: `VoiceLines` lists a `{side}` line once per side
  (`left`, `right`), and the level screen shows and plays the take of Rook's side setting (left
  when he does not fly).
- 2026-10-06: M5 part B (step B9): Level 08's lines and the Act 2 act briefing rendered by
  `tools/art/voice.py`'s pipeline (only the act's missing lines, nothing deleted), 19 lines: the
  act briefing's four pages and Level 08's two briefing pages (dry), its ten radio lines with
  Rook's t=8.5 side line once per side, his `escort-first-kill` line, and the billboard secret
  (Okafor 10, Varga 4, Rook 5). The Civilian's two lines stay text (uncast until round 30 casts
  her); the Choir's `[the Choir sings]` plays its stage sound; the hangar teaser is not voiced.
  Every take passed the length check, one chunk on its second seed (page 1's "Aegis Actual out.").
  Whisper (base.en) read each take back, dry and through the filter; three Okafor takes were
  re-rolled and pinned in the speaker table, each the first of four seeds that reads back word for
  word both ways: act page 3 (the key's seed said "you've fought"), Level 08's t=1 line ("contact
  inbound") and the level-end line ("arcologies" heard as "all colleges" through the filter). Level
  08's briefing page 1 (32.2 s) keeps the key's seeds: "Bring each other home" reads back only
  with a name prompt, as with all four other seeds tried, and "Ikoyi" and "Vrell" need the prompt
  too. VoiceFilesTest now checks Level 08 and the Act 2 briefing, with the Civilian allowed uncast
  while her audition is open (`AUDITIONING`); RadioTimelineTest no longer exempts Level 08 and
  plays an uncast speaker's line as text in any level. With the real lengths no voiced timed line
  of Level 08 starts more than a second late. For the user's ear in concept round 30.
- 2026-10-07: Level 08's two briefing pages, shortened to fit one screen each, re-rendered by
  `tools/art/voice.py` (only those two lines; their old files deleted as unused): Okafor 20.0 s,
  Varga 12.7 s, both on the key's seed with no retry. Whisper (base.en) reads them back word for
  word but for "the streets in low roofs" for "and" and "Everyone" for "Every one" (with a name
  prompt: Lancer, Rook, Vrell and Ikoyi spelt right).
- 2026-10-07: Round 30's civilian audition re-rendered (`tts_r30.py`, same clips, settings and
  seeds) with the t=113 line's current wording "Shelter nine, Ikoyi! Walkers on the roofs, heading
  our way!" (the first renders read the earlier call): a 10.76 s, b 10.84 s, −16 LUFS. Whisper
  (base.en) reads a back word for word and b with "Sheltonine" for "shelter nine". Measured only.
- 2026-10-07: Concept round 30 closed (user): the Ikoyi shelter civilian is **b**, Faith
  Abiola-Ellison (a woman's voice, clip pitch 151 Hz), at the neutral settings through radio filter
  b, as auditioned; her clip is `refs/ref-civilian.wav` and her two lines are voiced (the t=113
  call pinned on seed 86689958, read back word for word as "Shelter 9, Ikoyi, …"; the secondary's
  thanks on seed 179901323), so `AUDITIONING` is empty again. KirksVoice (a) rejected: his audition
  moved to `concept/rejected/`, his clip deleted with its CREDITS.md row. The 19 lines of Level 08
  and the Act 2 act briefing accepted as rendered, the three pinned Okafor takes included; none to
  re-render. Every item is ticked, so the implementation is `done` again.
- 2026-10-07: M5 part C (user decision D11 = a): Level 09's CDF officer of the Kilo convoy, speaker
  `Kilo Lead`, gets an audition of two new public-domain or CC0 readers (a/b) in concept round 31,
  as the Ikoyi shelter civilian did in round 30; he is `uncast` in the speaker table until then
  and his two lines play as text (`VoiceFilesTest`'s `AUDITIONING` takes him while the round is
  open). Rejected: b (reusing a generic reader already heard in another role, against round 19's
  one voice per role). Level 09's mission failed line names the node (`{group}`), so it is voiced
  once per node, as Level 05's battery line. The document is `in-progress` again for part C.
- 2026-10-07: M5 part C (step C9): Level 09's lines rendered by `tools/art/voice.py`'s pipeline
  (only the missing lines, nothing deleted), 20 lines: its two briefing pages (dry), its eleven
  spoken radio lines (the `hold-start`, `first-kill`, `first-pounce`, `collapse` and level-end event
  lines included; no `{side}` line), the cocoon secret's line and the mission failed line once per
  node (A1–C2) (Okafor 11, Varga 5, Rook 4). The Kilo Lead's two lines stay text (uncast until
  round 31 casts him); the Choir's `[the Choir sings]` plays its stage sound; the hangar teaser and
  the intel lines are not voiced, as in Level 08. Every take passed the length check on its first
  seed. Whisper (base.en) read each take back, dry and through the filter; "arcology", "Vrell" and
  "Airstrike" need a name prompt (without it: "archeology", "Vrel", "air strike"). Four takes were
  re-rolled (four seeds each) and pinned in the speaker table: Varga's briefing page 2 (the key's
  seed read "the streets and packs"; the pin reads "in packs" both ways) and the failed lines of
  nodes A1, A2 and C1 (the key's seeds ran "you, Lancer" into "you'll answer"; the pins read back
  word for word but for sound-alikes: "U Lancer" for A1, "See, one" for C1 through the filter
  without a prompt). VoiceFilesTest now checks Level 09, with the Kilo Lead in `AUDITIONING` while
  round 31 is open; RadioTimelineTest no longer exempts Level 09 (`UNVOICED` is empty). With the
  real lengths no voiced timed line of Level 09 starts more than a second late. For the user's ear
  in concept round 31.
- 2026-10-07: Concept round 31 closed (user): the Kilo Lead is a, Aaron Bennett, neutral as
  auditioned; clip renamed `refs/ref-kilo-lead.wav`, b deleted with its CREDITS.md row; his two
  Level 09 lines voiced (no pins needed), so `AUDITIONING` is empty again. tombooker (b) rejected:
  his audition moved to `concept/rejected/`. The 20 lines of Level 09 accepted as rendered, the four
  pinned takes included; none to re-render. Every item is ticked, so the implementation is `done`
  again.
- 2026-10-08: M5 part D (user decision D12 = a): two new speakers for Level 10, auditioned a/b in
  round 32: `lifeline` (names Lifeline, Lifeline One, Two, Four and Five: the lead pilot and the
  hit line, one voice) and `lifeline-three` (one line, cut off), both `uncast: true` in the speaker
  table until the pick; rejected: b (one voice for every shuttle, the lost pilot sounding like the
  lead) and c (reusing Level 08's civilian and Level 07's Lifeboat Seven). The cast speakers' Level
  10 lines render as soon as the texts are final. The implementation is `in-progress` again.
- 2026-10-08: M5 part D (step D9): Level 10's lines of the cast speakers rendered by
  `tools/art/voice.py`'s pipeline (only the missing lines, nothing deleted), 22 lines: its two
  briefing pages (dry), its spoken radio lines (the `first-decloak`, `first-loop-back`,
  `scripted-loss` and mission failed lines included; `ally-lost` once per shuttle, Lifeline One to
  Five, as Level 04's convoy lines; the four level-end lines by shuttles home; no `{side}` line) and
  the ferry secret's line (Okafor 13, Varga 5, Rook 4). The Lifeline speakers' lines (t=1, t=116,
  t=196 and the `first-ally-hit` line) stay text until round 32 casts them; the Choir's
  `[the Choir sings]` plays its stage sound; the hangar teaser and the intel lines are not voiced,
  as in Levels 08 and 09. Three `ally-lost` takes hit the length cap and kept their second or third
  seed. Whisper (base.en) read each take back, dry and through the filter; "Eko" reads "Eco",
  "Vrell" and "Lagos" need a name prompt, and the numbers come back as digits. Four takes were
  re-rolled (four seeds each) and pinned in the speaker table: Okafor's briefing page 1 (the key's
  seed read "Fides shuttles"), the four-home level-end line ("eight to hundred and eighty" dry),
  Varga's briefing page 2 ("The rail hunt" but with a prompt through the filter; the pin reads
  "Vrell" with a prompt both ways, "rail" without, and "Flock sweep" once through the filter) and
  the ferry line ("berry's" through the filter; the pin reads the sound-alike "ferries" every way).
  Okafor's grim lines leave a pause of about 0.6–1 s between "Lifeline" and its number (the
  `ally-lost` takes and `scripted-loss`): every take does it, so it is left for the ear.
  VoiceFilesTest now checks Level 10, with the Lifeline speakers in `AUDITIONING` while round 32
  is open; RadioTimelineTest no longer exempts Level 10 (`UNVOICED` is empty; the Lifeline lines
  play as text by the uncast rule). With the real lengths no voiced timed line of Level 10 starts
  more than a second late. For the user's ear in concept round 32.
- 2026-10-08: Concept round 32 decided (user) for the two voices, **b** and **b**: Lifeline is
  KevinS (clip pitch 138 Hz), Lifeline Three is Maria Kasper (215 Hz), both at the neutral settings
  through radio filter b, as auditioned; their clips renamed `refs/ref-lifeline.wav` and
  `refs/ref-lifeline-three.wav`, `uncast` replaced by the `ref` in the speaker table. Atul Sharma
  and Kehinde (a) rejected: their auditions moved to `concept/rejected/`, their clips deleted with
  their CREDITS.md rows. `tools/art/voice.py` now cuts off a radio line whose text ends in a dash
  (Lifeline Three's, as auditioned: Whisper times the take, the take is cut where its last word
  ends into 0.3 s of static and 0.25 s of dying hiss, with no closing click; tts_r32's
  `cut_off()`). The Lifeline speakers' eight lines rendered (only the missing lines, nothing
  deleted): t=1 (5.4 s), t=196 (4.1 s), the hit line once per shuttle by `{ally}` (4.6–4.9 s; "Lifeline
  Three, we're hit!" is rendered but never plays, Lifeline Three being untouchable) and Lifeline
  Three's t=116 line (4.6 s, cut after "that" at 3.88 s of the take: the voice at −14 dB (50 ms
  RMS) runs into the static at −16 to −20 dB, dying to −40 dB). Every take passed the length check
  on its first seed and is −16 LUFS. Whisper (base.en) and librosa pyin measured each take raw and
  filtered: four of the five hit lines jumped to about 450–500 Hz on "hit" or "Still" (Two
  throughout; the audition's octave jump) and Four read "Lifeline, or"; t=196 read "Core to clear".
  Five takes re-rolled (six seeds each, Five fourteen) and pinned in the speaker table on takes whose
  words stay at 70–190 Hz: the hit lines One, Two, Four and Five (read back "Lifeline 1/2/4/5,
  we're hit. Still flying. Please stay close.") and t=196 (read "Corridor clear" without a prompt).
  "Eko" still reads "Echo" (the same sound) and "Aegis" "ages" without a prompt; Lifeline Three's
  hit line reads "Lifeline. Free." through the filter (raw: "Three"), left as it never plays.
  `AUDITIONING` is empty again; RadioTimelineTest passes with the real lengths (no voiced timed
  line of Level 10 starts more than a second late). Measured only, not listened to.
- 2026-10-08: M5 part E (user decision E10 = a of 2026-10-08): Level 11's CDF convoy officer, speaker
  `Atlas Control`, gets an audition of two new public-domain or CC0 readers (a/b) in concept [round 33](../../concept-rounds/round-33/README.md),
  as the Kilo Lead and Lifeline did; rejected: b (reusing an Act 1 secondary voice, Convoy or Yard
  Control: one voice in two roles, against round 19's "every role its own voice") and c (cast
  directly: a re-render if disliked). Stated default: `{ally}` in a naval convoy's lines becomes the
  ship's name, three takes per line.
- 2026-10-08: [Round 33](../../concept-rounds/round-33/README.md) candidates for Atlas Control (M5 part E, step E1a): a Alister (Sea-Power in the
  Pacific, Bywater; clip pitch 106 Hz) and b MaryAnn (Eighteen Months in the War Zone, Finzi;
  174 Hz), a male and a female voice, both new and Public Domain Mark 1.0; `atlas-control` added to
  the speaker table as `uncast: true` (no Level 11 data yet, so VoiceFilesTest is unchanged). Three
  of its lines rendered per candidate (`tools/concept/audio/tts_r33.py`, details in
  [concept/prompts.md](concept/prompts.md#voice-atlas-control)). Measured only, not listened to.
- 2026-10-09: M5 part E (step E4): Level 11's lines of the cast speakers rendered by
  `tools/art/voice.py`'s pipeline (only the missing lines, nothing deleted), 15 lines: Okafor 4
  (briefing page 1, `first-telegraph`, the two level-end lines), Varga 7 (briefing page 2, the
  t=21.5, 34, 70.5 and 146.5 lines, `boss-destroyed`, the sunken pod's secret) and Rook 4 (t=14, 55,
  158 and `boss-part-destroyed`); the Choir's `[the Choir sings]` plays its stage sound; the hangar
  teaser and the intel lines are not voiced, as in Levels 08 to 10. **Atlas Control's** five lines
  (t=1, t=62.5, t=110, `secondary-objective` and the `ally-hit` and `ally-lost` lines once per
  ship name) stay text until [round 33](../../concept-rounds/round-33/README.md) casts him: `VoiceLines` now expands `{ally}` in a naval
  convoy's lines to the three cargo ships' names (Halvorsen, Mbeki, Saint-Laurent), six takes in
  all, not number words (`NavalConvoyLinesTest` runs it with Atlas Control cast); `Atlas Control`
  is in VoiceFilesTest's `AUDITIONING` and Level 11 in its `RENDERED`; RadioTimelineTest covers
  Level 11 (seven runs, with and without Rook, all three difficulties): no voiced timed line
  starts more than a second late. Whisper (base.en) read each take back through the filter and
  librosa pyin measured its pitch: three takes re-rolled (four seeds each) and pinned in the speaker
  table: Rook's `boss-part-destroyed` line and t=55 line (the key's seeds jumped an octave, to about
  500 Hz, on their first words) and Okafor's end line "Atlas-Seven is through…" (the key's seed read
  "Good work, ages" without a name prompt; the pin still does, "Aegis" reads right with a prompt:
  a listening point). Other readings: "Vrell" reads "Vrel" and "ceded" for "seeded" in Varga's
  briefing page, "Tiamat" reads "Tiamat" in Okafor's; "Atlas-Seven" reads "Atlas, seven" or
  "Atlas 7". For the user's ear: the names Saint-Laurent, Halvorsen and Mbeki (Atlas Control's
  takes, after the casting), Tiamat (Okafor's briefing page), Vrell (Varga's), "Atlas-Seven" and
  "Aegis" (Okafor's level-end lines).
- 2026-10-09: Concept round 33 decided (user) for Atlas Control, **b**: MaryAnn (clip pitch 174
  Hz), at the neutral settings through radio filter b, as auditioned; her clip renamed
  `refs/ref-atlas-control.wav`, `uncast` replaced by the `ref` in the speaker table and `Atlas
  Control` taken out of VoiceFilesTest's `AUDITIONING`. Alister (a) rejected: his audition moved to
  `concept/rejected/`, his clip deleted with its CREDITS.md row. Her ten takes rendered by
  `tools/art/voice.py` (only the missing lines, nothing deleted): t=1 (6.7 s), t=62.5 (4.2 s),
  t=110 (4.6 s), the `ally-hit` and `ally-lost` lines once per ship name (3.1–4.2 s) and the
  `secondary-objective` thanks (3.7 s). Whisper (base.en) read every take back whole with a name
  prompt (without one: "Halbertson", "St. Laurent", "router" for Ruyter, "halls" for "hulls"); librosa
  pyin on the filtered files read five takes an octave high on single words, but the raw takes of
  the same seeds measured 140–255 Hz throughout (the radio filter's band limit and crackle fool the
  pitch tracker), so no pin was needed. For the user's ear: "hulls" (it may sound like "halls") and
  the three ship names. The cast speakers' 15 lines were accepted as rendered (round 33, user).
