# Voice briefs and prompts — concept round 18

Round 18 compares five offline text-to-speech engines for the spoken radio lines (the text stays
on screen as subtitles). Every engine reads the same five lines; every output goes through the
same radio filter. Reproduce with:

```
python3 tools/concept/audio/tts_r18.py            # all engines, then the post-process
python3 tools/concept/audio/tts_r18.py dia        # one engine, then the post-process
python3 tools/concept/audio/tts_r18.py --post     # post-process only (raw WAVs in ~/.cache/tv-tts/raw/)
```

Generator: `tools/concept/audio/tts_r18.py` (its header lists the venvs, pip packages, versions
and model ids; synth library `tools/concept/audio/synth.py`). Files: `radio-<engine>-<line>-r18-a.ogg`
(through the radio filter) and `voice-<engine>-<line>-r18-a.ogg` (the raw engine output, peak
−3 dBFS, the Choir after its layering). OGG Vorbis q4, 44.1 kHz mono. Seeds are fixed, but GPU
sampling is not bit-exact, so a rerun can differ slightly.

**Lines** (from Act 1's `data.yaml` and [the Choir](../../story/characters/the-choir/README.md)):

| Line | Speaker | Mood | Text |
|---|---|---|---|
| a-okafor | [Okafor](../../story/characters/okafor/README.md) (50s, commander) | grim, steady | "That's the yards. Not all of them. Enough. Come home, Aegis." (L02 end) |
| b-varga | [Varga](../../story/characters/varga/README.md) (40s, intel) | calm, precise | "Walker on the rille rim. The claws are armour. Wait for it to turn, then hit the glowing back." (L04) |
| c-rook | [Rook](../../story/characters/rook/README.md) (wingman) | joking | "You know, they told me you were the quiet type. Good. More airtime for me." (L02) |
| d-control | Tranquility Control (generic CDF) | urgent, shouted | "Walkers coming over the crater rims, both sides of the road!" (L04) |
| e-choir | [the Choir](../../story/characters/the-choir/README.md) | eerie | "...many... we are many..." (its first words, Acts 3–4) |

**Radio filter** (identical for every file, `radio()` in the generator): 0.15 s pad either side,
faint hiss (−44 dBFS) and sparse crackle, 300 Hz high-pass and 3.4 kHz low-pass (two 12 dB/oct
passes each), tanh saturation (drive 2), a 4 ms band-passed click in and out, mastered to
−16 LUFS with a −1.5 dBFS ceiling.

**Radio filter b — more static** (after the user's choice of Chatterbox: "the radio could use a
bit more static"; `radio(x, "b")`, run with `python3 tools/concept/audio/tts_r18.py --post-b`):
the same five Chatterbox takes (the raw WAVs, not regenerated) through the same chain, with a
steady hiss bed about 10 dB louder (noise ×0.019 instead of ×0.006, with a slow 0.7 Hz / 3.1 Hz
flutter of ±25 %) and about 2.5× the crackle (0.1 % of the samples at up to 0.11 instead of
0.04 % at 0.08). Measured: the noise floor in the lead-in rises from about −55 to −43 dBFS RMS,
roughly 27 dB under the voice; the loudness stays −16 LUFS. Files `radio-chatterbox-<line>-r18-b.ogg`.
Outcome (user decision, 2026-10-03): filter b chosen; it is now `radio()`'s default, and `--post`
still writes the `-a` files with filter a (`radio(x, "a")`), which are in `rejected/`.

**The Choir** (identical for every engine, `choir()` in the generator, before the radio
filter): the engine's line layered at −12, −5, 0 and +7 semitones (ffmpeg rubberband, same
length), staggered 0–45 ms, plus a reversed copy, over a 55/82 Hz drone with low noise, through
a 2.2 s synthetic reverb.

**Reference voices** (cloned by Chatterbox and Dia): 12 s cut at 60 s from LibriVox chapter
recordings, which are public domain; no celebrity or known person. Not stored in the repo yet
(`~/.cache/tv-tts/refs/`); `tts_r18.py` documents the cut. They move into the repo under LFS
with the [voice](../voice/README.md#reference-voices) part.

| Voice | Reader | Source | Licence |
|---|---|---|---|
| okafor | Ruth Golding | [The Haunted Man, ch. 1](https://archive.org/details/haunted_man_rg_librivox) | CC0 1.0 |
| varga (also the Choir's base) | Betsie Bush | [Deephaven, ch. 1](https://archive.org/details/deephaven_0812_bb_librivox) | public domain |
| rook | Rick Rodstrom | [You Know Me Al, ch. 1](https://archive.org/details/youknowme_al_0908_librivox) | public domain |
| control | David Leeson | [Hero Tales from American History, ch. 1](https://archive.org/details/hero_tales_from_american_history_dl_0905_librivox) | public domain |

---

## radio-chatterbox

Chatterbox (Resemble AI), model `ResembleAI/chatterbox` (0.5 B, English), MIT code and weights;
every output carries Resemble's inaudible Perth watermark. Voice: zero-shot clone of the
reference clip; emotion: `exaggeration` (0.25–2), `cfg_weight` (lower = slower, more dramatic),
`temperature`. Plain text, no tags.

| Voice | Reference | exaggeration | cfg_weight | temperature |
|---|---|---|---|---|
| okafor | okafor | 0.45 | 0.5 | 0.7 |
| varga | varga | 0.5 | 0.5 | 0.7 |
| rook | rook | 0.8 | 0.4 | 0.8 |
| control | control | 1.2 | 0.3 | 0.8 |
| choir | varga | 0.3 | 0.3 | 0.7 |

Round 18 outcome: Chatterbox chosen; the Parler, Dia, Kokoro and Bark files are in `rejected/`.

## radio-parler

Parler-TTS, model `parler-tts/parler-tts-mini-v1` (0.9 B) in fp16 (large-v1, 2.2 B, ran out of memory: about 5 GB of the 8 GB are free next to the desktop), Apache-2.0 code and weights.
Voice and emotion are described in text; a named speaker keeps the voice consistent. Plain text.

| Voice | Description |
|---|---|
| okafor | "Laura speaks in a low, grave and sad voice at a slow pace, steady and controlled. The recording is very clear with no background noise." |
| varga | "Lea speaks in a calm, precise and clear voice at a moderate pace. …" |
| rook | "Jon speaks in a cheerful, playful and very expressive voice at a fast pace. …" |
| control | "Gary shouts urgently in a loud, tense and very expressive voice at a very fast pace. …" |
| choir | "Jenna speaks in a very slow, breathy, monotone whisper. …" |

## radio-dia

Dia (Nari Labs), model `nari-labs/Dia-1.6B-0626` in fp16, Apache-2.0 code and weights. Voice:
clone of the reference clip, whose transcript (made by faster-whisper `base.en`) is prepended
to the text as Dia requires. Emotion: only through the text and non-verbal tags. Sampling:
cfg_scale 3.0, temperature 1.2, top_p 0.95, cfg_filter_top_k 45.

| Line | Text as read |
|---|---|
| a-okafor | "[S1] (sighs) That's the yards. Not all of them. Enough. Come home, Aegis." |
| b-varga | "[S1] " + the line |
| c-rook | "[S1] You know, they told me you were the quiet type. Good. (laughs) More airtime for me." |
| d-control | "[S1] Walkers coming over the crater rims! Both sides of the road!" |
| e-choir | "[S1] ...many... we are many..." (varga reference) |

## radio-kokoro

Kokoro (baseline), model `hexgrad/Kokoro-82M`, Apache-2.0 code and weights (its phonemiser falls
back on espeak-ng, GPL, as a separate build tool; the generated audio is not affected). Voice:
one of its built-in voices; no emotion control, only speed. Plain text.

| Voice | Kokoro voice | Speed |
|---|---|---|
| okafor | `bf_emma` (British female) | 0.9 |
| varga | `af_heart` (American female) | 1.0 |
| rook | `am_puck` (American male) | 1.1 |
| control | `am_fenrir` (American male) | 1.2 |
| choir | `af_bella` (American female) | 0.8 |

## radio-bark

Bark (Suno), model `suno/bark` (full models, CPU offload), MIT code and weights. Voice: a
speaker preset (history prompt); emotion through text cues: `[sighs]`, `[laughs]`, capitals,
ellipses. Very random from take to take.

| Voice | Preset | Text as read |
|---|---|---|
| okafor | `v2/en_speaker_9` | "[sighs] That's the yards. Not all of them. Enough... Come home, Aegis." |
| varga | `v2/de_speaker_3` (German-accented) | the line |
| rook | `v2/en_speaker_6` | "You know, they told me you were the quiet type. Good. [laughs] More airtime for me." |
| control | `v2/en_speaker_3` | "WALKERS coming over the crater rims! BOTH SIDES OF THE ROAD!" |
| choir | `v2/en_speaker_9` | the line |

## voice-chatterbox, voice-parler, voice-dia, voice-kokoro, voice-bark

The same takes before the radio filter (the Choir after its layering), to judge the engine
itself.
