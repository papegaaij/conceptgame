# Concept audio generators

A small numpy synthesizer that makes reproducible concept sound effects and music sketches.
Requires Python 3, numpy and the `ffmpeg` binary (with libvorbis). No other audio libraries.

| Script | Purpose | Output |
|---|---|---|
| `synth.py` | Library: PolyBLEP saw/pulse, triangle, sine, noise, supersaw; ADSR and ramps; resonant state-variable filter; delay, convolution reverb, bitcrush, saturation, panning; tracker-style pattern parser and mixer; look-ahead limiter, loudness mastering (via ffmpeg `loudnorm`), OGG writer with a post-encode peak check | — |
| `sfx.py` | Concept round 01 sound effects | `design/audio/sfx/concept/*.ogg` |
| `music.py` | Concept round 01 music sketches (`a` trance/techno, `b` synth-orchestral) | `design/audio/music/concept/*.ogg` |
| `music_r02.py` | Concept round 02: five loopable themes (`title hangar boss mars europa`). Intro + loop + fade tail per file, loop points in the `LOOPSTART`/`LOOPLENGTH` Vorbis comments, sample-exact loops | `design/audio/music/concept/*-r02-a.ogg` |
| `music_r03.py` | Concept round 03: five more themes (`earth belt jovian ascendancy final`), same loop format; adds Vorne's motif and new voices (trumpet, storm tremolo strings, organ, anvils, thunder, heartbeat, radio static, siren) | `design/audio/music/concept/*-r03-a.ogg` |
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
