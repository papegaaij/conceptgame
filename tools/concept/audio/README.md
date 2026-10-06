# Concept audio generators

A small numpy synthesizer that makes reproducible concept sound effects and music sketches.
Requires Python 3, numpy and the `ffmpeg` binary (with libvorbis). No other audio libraries.

| Script | Purpose | Output |
|---|---|---|
| `synth.py` | Library: PolyBLEP saw/pulse, triangle, sine, noise, supersaw; ADSR and ramps; resonant state-variable filter; delay, convolution reverb, bitcrush, saturation, panning; tracker-style pattern parser and mixer; look-ahead limiter, loudness mastering (via ffmpeg `loudnorm`), OGG writer with a post-encode peak check | — |
| `sfx.py` | Concept round 01 sound effects | `design/audio/sfx/concept/*-r01-*.ogg` (the rejected shots and explosions in `concept/rejected/`) |
| `sfx_r08.py` | Concept round 08: synthesized pickups and UI sounds (pickup-r01 family) | `design/audio/sfx/concept/{pickup,ui}-*-r08-*.ogg` |
| `import_sfx.py` | Recorded CC0/CC-BY sound effects from Freesound (trim, cut, fades, optional loop, high/low-pass, peak or 200 Hz–5 kHz band levelling); sources in `CREDITS.md` | `design/audio/sfx/concept/*.ogg` (+ `rejected/`) |
| `music.py` | Concept round 01 music sketches (`a` trance/techno, `b` synth-orchestral) | `design/audio/music/concept/*.ogg` |
| `music_r02.py` | Concept round 02: five loopable themes (`title hangar boss mars europa`). Intro + loop + fade tail per file, loop points in the `LOOPSTART`/`LOOPLENGTH` Vorbis comments, sample-exact loops | `design/audio/music/concept/*-r02-a.ogg` |
| `music_r03.py` | Concept round 03: five more themes (`earth belt jovian ascendancy final`), same loop format; adds Vorne's motif and new voices (trumpet, storm tremolo strings, organ, anvils, thunder, heartbeat, radio static, siren) | `design/audio/music/concept/*-r03-a.ogg` |
| `sfx_r11.py` | Concept round 11: synthesized launch-rail sounds and edge-warning tones, band-levelled (200 Hz–5 kHz RMS, limiter at the ceiling) | `design/audio/sfx/concept/{launch-rail,ui-edge-warning}-r11-*.ogg` |
| `sfx_r16.py` | Concept round 16: the Leviathan's synthesized whale-song cry (formant voice, gliding fundamental, a beating second voice, long reverb), band-levelled like the recorded enemy sounds (−30 dB, ceiling −6 dBFS) | `design/audio/sfx/concept/enemy-leviathan-cry-r16-a.ogg` |
| `sfx_r24.py` | Concept round 24: the Coilwyrm's death bursts, options a–c recorded (Freesound HQ previews cut with `import_sfx.py`'s functions) and d synthesized (a pressurised pop); per option a segment burst, a head burst (slowed, over a sub thump) and a review preview of the whole chained death mixed as the game plays it; levelled on the loudest 100 ms of the 200 Hz–5 kHz band; the unchosen into `rejected/`, plus the chosen pair (b, c) at the game's 0.25 s rhythm; `PRODUCTION` gives `tools/art/sfx_originals.py` the chosen pair's treatment | `design/audio/sfx/concept/enemy-coilwyrm-{burst,head-burst,death}-r24-*.ogg`, `enemy-coilwyrm-death-final-r24-a.ogg` (+ `rejected/`) |
| `sfx_r25.py` | Concept round 25: the Brood Carrier's roar, bay sac opening, closing and burst, unit launch and plate iris, and the lifeboat tow's cable snap, an a/b pair each, recorded (CC0/CC-BY), cut from the cached Freesound originals (`freesound_fetch.py --download`; the HQ preview when one is missing) with `import_sfx.py`'s `process`, optional slowdown or reversal, levelled on the loudest 100 ms of the 200 Hz–5 kHz band; prints each file's checks | `design/audio/sfx/concept/{enemy-carrier-*,secret-cable-snap}-r25-*.ogg` |
| `sfx_r27.py` | Concept round 27: the save-done sound, an a/b pair synthesized in the round-08 UI family (`sfx_r08.py`'s notes, bells and sparkles, peak −8 dBFS), and the Coilwyrm's chain-cut tear, an a/b pair recorded (CC0/CC-BY), cut from the cached Freesound originals like `sfx_r25.py` (optional slowdown, levelled on the loudest 100 ms of the 200 Hz–5 kHz band); `PRODUCTION` holds the chosen tear (a) for `tools/art/sfx_originals.py`; prints each file's checks | `design/audio/sfx/concept/{ui-save,enemy-coilwyrm-cut}-r27-*.ogg` (+ `rejected/`) |
| `choir_r29.py` | Concept round 29: the sound of the Choir's `[the Choir sings]`: a and b a wordless sting sung in Varga's reference voice (two Chatterbox vowel takes, pitch-flattened and stretched by TD-PSOLA, then `choir()` of `tts_r18.py`), c and d the music's choir voice (`v_choir` of `music_r02.py`) through `choir()`'s drone and reverb; all through radio filter b at −16 LUFS; needs the Chatterbox venv for the takes (`--post` without); the chosen b (`CHOSEN`) into `concept/`, a, c and d into `rejected/` | `design/audio/voice/concept/voice-choir-sings-r29-b.ogg` (+ `rejected/` a, c, d) |
| `music_r11.py` | Concept round 11: base stem of "Coalition Rising", from the same render as the chosen full mix (tagged timpani, the full mix's chains, linked mastering); checks that the full mix is still byte-identical to the r08 file (`--full` writes it otherwise) | `design/audio/music/concept/coalition-rising-base-r11-a.ogg` |
| `analyze.py` | Objective checks: duration, peak, RMS, DC, LUFS, true peak, edge levels; optional spectrogram PNGs | stdout (+ PNGs in a directory you give it) |

```
python3 tools/concept/audio/sfx.py
python3 tools/concept/audio/music.py            # ~20 s; 'a' or 'b' renders one sketch
python3 tools/concept/audio/music_r02.py        # ~3.5 min; or name themes: title hangar boss mars europa
python3 tools/concept/audio/music_r03.py        # ~5 min; or: earth belt jovian ascendancy final
python3 tools/concept/audio/analyze.py design/audio/*/concept/*.ogg --spectrogram /tmp/spec
```

All output is deterministic: fixed seeds, bit-exact ffmpeg flags, identical bytes on re-run.
Keep spectrograms and other inspection files out of the repo.

Music is written as tracker patterns (see `synth.parse_track`): 16 tokens per bar, one per
16th step. `C#5` starts a note, `-` holds the previous note, `.` is silence.

Level conventions (see `design/audio/sfx/concept/prompts.md`): player shots peak at −10 dBFS,
pickups at −4 dBFS, explosions at −1.5 dBFS; music is mastered to −14 LUFS with a −2 dBFS
sample-peak ceiling. `synth.write_ogg(..., tags=...)` writes Vorbis comments such as loop points.
