---
title: Concept round 18 — text-to-speech for the radio lines
design: approved
implementation: n/a
art: chosen
depends-on: [../../audio, ../../story]
updated: 2026-10-03
---

# Concept round 18 — text-to-speech for the radio lines

## Summary

Should the radio lines be spoken, and if so by which offline text-to-speech engine? Five engines
whose code and weights allow commercial use read the same five Act 1 lines (Okafor grim, Varga
calm, Rook joking, an urgent shout, the Choir); every take goes through one shared radio filter,
and the subtitles stay. Open [index.html](index.html) in a browser (regenerate with
`python3 tools/concept/board.py 18`); the files, settings and reference voices are in
[audio](../../audio/README.md#concept-art) and its [prompts](../../audio/concept/prompts.md). This
reopens the 2026-10-01 decision "text and radio blips only, no voice acting". Item 1 was decided
on 2026-10-03: **Chatterbox**, which reverses that decision (the subtitles stay); the spoken lines
are designed in [voice](../../audio/voice/README.md). Item 2 was added the same day after the
user's remark that the radio could use a bit more static: the same Chatterbox takes through a
filter with more static, to compare with the `-a` files. Item 2 was decided the same day: **filter
b**; the round is closed.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [Audio](../../audio/README.md) | **pick a TTS engine** for spoken radio lines (or keep text only): Chatterbox, Parler-TTS, Dia, Kokoro or Bark, see [Engines](#engines) | `radio-<engine>-<line>-r18-a`, raw `voice-<engine>-<line>-r18-a` | Claude cannot hear the files: the notes below are licences, measurements and settings only, not a judgement of how they sound | **Chatterbox** chosen ("by far the best"); the other four engines' files moved to `concept/rejected/` |
| 2 | [Audio](../../audio/README.md) | **radio filter: more static (b)**: the five Chatterbox takes (not regenerated) through the radio filter with a steady hiss bed about 10 dB louder, with a slow flutter, and about 2.5× the crackle; loudness unchanged at −16 LUFS (see the [prompts](../../audio/concept/prompts.md)) | `radio-chatterbox-<line>-r18-b`, against `radio-chatterbox-<line>-r18-a` | the hiss bed sits about 27 dB under the voice (noise floor −43 dBFS RMS instead of −55); Claude cannot judge by ear whether that is enough or too much | **filter b** chosen; the filter-a `radio-chatterbox-*-r18-a` files moved to `concept/rejected/`, the raw Chatterbox takes stay chosen (only the filter changed) |

## Engines

Measured on the RTX 2070 (8 GB, of which about 5 GB are free next to the desktop); speed is
generation time ÷ audio length after loading (lower is faster), VRAM the peak PyTorch
allocation. Pitch is the median voiced F0 of the raw take, a rough check that a voice came out
in the expected range (female ≈ 165–255 Hz, male ≈ 85–155 Hz; shouting raises it).

| Engine | Licence (code / weights) | Voices | Emotion control | Speed, VRAM | Measured notes |
|---|---|---|---|---|---|
| **Chatterbox** (Resemble AI, `ResembleAI/chatterbox`, 0.5 B) | MIT / MIT; every output carries an inaudible Perth watermark | any, cloned zero-shot from a 5–15 s reference (here public-domain LibriVox readers) | `exaggeration` slider (0.25–2) plus `cfg_weight` for pace | 0.7–1.0× real time (first line 2.3×, warm-up), 3.6 GB | all five lines at plausible lengths (1.8–6.4 s); Rook (male reference) measured 203 Hz and the shout 184 Hz, so check that they still sound male |
| **Parler-TTS** (`parler-tts/parler-tts-mini-v1`, 0.9 B) | Apache-2.0 / Apache-2.0 | 34 named speakers, voice described in text | text description only ("sad", "shouts"); weak in practice | 1.3× real time, 2.0 GB; **large-v1 (2.2 B) ran out of memory** | the shout (d) has almost no voiced frames (10 in 2.8 s), probably garbled or whispered; Okafor came out at 204 Hz (female, as described) |
| **Dia** (Nari Labs, `nari-labs/Dia-1.6B-0626`, fp16) | Apache-2.0 / Apache-2.0 (the model card forbids identity misuse and deception) | cloned from a reference clip plus its transcript (made with Whisper) | text only: non-verbal tags `(sighs)`, `(laughs)`, punctuation | about 3× slower than real time, 4.4 GB, the limit of this GPU (the first run ran out of memory; it needs a 2048-token cap and `expandable_segments`) | Rook and the Choir both ran to the token cap (11.6 s for a 3–4 s line), so they probably ramble or repeat; the shout (male reference) measured 269 Hz |
| **Kokoro** (`hexgrad/Kokoro-82M`, baseline) | Apache-2.0 / Apache-2.0 (its phonemiser's espeak-ng fallback is GPL, a separate tool; the audio is not affected) | 50-odd fixed voices (US/UK English) | none, only speed | 0.2–0.9× real time, 1.2 GB (also fine on CPU) | plausible lengths (2.7–5.7 s) and pitches for every voice; no emotion by design |
| **Bark** (Suno, `suno/bark`, full models with CPU offload) | MIT / MIT | fixed speaker presets (one English female preset in the v2 set) | text cues `[sighs]`, `[laughs]`, CAPS; very random per take | 3.5–3.8× slower than real time, 1.9 GB with offload (12 GB without) | every line runs long (5–13 s; Okafor 13.2 s for a 4 s line) and Okafor measured 123 Hz, in the male range, from the female preset `v2/en_speaker_9` |

No engine failed the licence check (each verified on its repository and model card on
2026-10-03), so none was dropped.

## Listening

The same line across the engines; `radio` is the line through the shared filter, `raw` the
engine's own output.

| Line | Chatterbox, filter b (item 2) | Chatterbox | Parler | Dia | Kokoro | Bark |
|---|---|---|---|---|---|---|
| Okafor, grim | [radio b](../../audio/concept/radio-chatterbox-a-okafor-r18-b.ogg) | [radio](../../audio/concept/rejected/radio-chatterbox-a-okafor-r18-a.ogg) · [raw](../../audio/concept/voice-chatterbox-a-okafor-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-parler-a-okafor-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-parler-a-okafor-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-dia-a-okafor-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-dia-a-okafor-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-kokoro-a-okafor-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-kokoro-a-okafor-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-bark-a-okafor-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-bark-a-okafor-r18-a.ogg) |
| Varga, calm | [radio b](../../audio/concept/radio-chatterbox-b-varga-r18-b.ogg) | [radio](../../audio/concept/rejected/radio-chatterbox-b-varga-r18-a.ogg) · [raw](../../audio/concept/voice-chatterbox-b-varga-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-parler-b-varga-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-parler-b-varga-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-dia-b-varga-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-dia-b-varga-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-kokoro-b-varga-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-kokoro-b-varga-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-bark-b-varga-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-bark-b-varga-r18-a.ogg) |
| Rook, joking | [radio b](../../audio/concept/radio-chatterbox-c-rook-r18-b.ogg) | [radio](../../audio/concept/rejected/radio-chatterbox-c-rook-r18-a.ogg) · [raw](../../audio/concept/voice-chatterbox-c-rook-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-parler-c-rook-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-parler-c-rook-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-dia-c-rook-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-dia-c-rook-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-kokoro-c-rook-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-kokoro-c-rook-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-bark-c-rook-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-bark-c-rook-r18-a.ogg) |
| Control, urgent shout | [radio b](../../audio/concept/radio-chatterbox-d-control-r18-b.ogg) | [radio](../../audio/concept/rejected/radio-chatterbox-d-control-r18-a.ogg) · [raw](../../audio/concept/voice-chatterbox-d-control-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-parler-d-control-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-parler-d-control-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-dia-d-control-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-dia-d-control-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-kokoro-d-control-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-kokoro-d-control-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-bark-d-control-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-bark-d-control-r18-a.ogg) |
| the Choir | [radio b](../../audio/concept/radio-chatterbox-e-choir-r18-b.ogg) | [radio](../../audio/concept/rejected/radio-chatterbox-e-choir-r18-a.ogg) · [raw](../../audio/concept/voice-chatterbox-e-choir-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-parler-e-choir-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-parler-e-choir-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-dia-e-choir-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-dia-e-choir-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-kokoro-e-choir-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-kokoro-e-choir-r18-a.ogg) | [radio](../../audio/concept/rejected/radio-bark-e-choir-r18-a.ogg) · [raw](../../audio/concept/rejected/voice-bark-e-choir-r18-a.ogg) |

## Notes

- One take per line and engine, fixed seeds, no cherry-picking; random engines (Bark, Dia) can
  differ a lot on another seed. Production would generate several takes and pick.
- Each line comes from a different speaker, so this round does not test whether a voice stays
  the same across lines; the cloning engines (Chatterbox, Dia) keep it by reusing one reference
  clip, Kokoro by its fixed voice id, Parler by a named speaker, Bark by its preset.
- The radio filter and the Choir's layering are done in post, identically for every engine
  (`tools/concept/audio/tts_r18.py`).
- If spoken lines are adopted, they get their own part, `design/audio/voice/`, with the cast's
  voice settings, the reference clips and the line list generated from the levels' `radio:`
  data; the radio bus, the ducking and the subtitles already exist.
- Setup and size: one venv per engine in `~/.cache/tv-tts/` (outside the repo), about 30 GB with
  the models and pip cache.

## Decisions

- 2026-10-03: Opened with five TTS engines; all five pass the licence check (MIT or Apache-2.0
  for both code and weights).
- 2026-10-03: Item 1 decided by the user: **Chatterbox**, "by far the best"; Parler-TTS, Dia,
  Kokoro and Bark rejected (their files in `design/audio/concept/rejected/`). Spoken radio lines
  are adopted, reversing the 2026-10-01 "no voice acting" decision; the text stays as subtitles
  ([voice](../../audio/voice/README.md), draft). The user added "the radio could use a bit more
  static": the round is reopened with item 2, filter b.
- 2026-10-03: Item 2 decided by the user: radio filter **b** (more static), now the default of
  `radio()` in `tools/concept/audio/tts_r18.py`. The filter-a Chatterbox files
  (`radio-chatterbox-*-r18-a`) moved to `design/audio/concept/rejected/`; the raw
  `voice-chatterbox-*-r18-a` takes stay chosen as the voice takes. Round closed.
