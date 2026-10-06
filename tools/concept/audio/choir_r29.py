#!/usr/bin/env python3
"""Concept round 29: the sound of the Choir's stage direction "[the Choir sings]".

Act 1's five Choir cues (Level 01 t=160, Level 03 t=58.5, Level 05 t=144.5, Level 07 t=94 and its
boss-destroyed cue) are the stage direction "[the Choir sings]", which the voice pipeline does not
speak (design/audio/voice, The Choir). Two kinds of sound, two options each:

Outputs (design/audio/voice/concept/, the unchosen in its rejected/), OGG Vorbis q4, 44.1 kHz mono, -16 LUFS like the radio voice
lines, 3.7-4.0 s with the reverb's tail and the radio filter's pad:
  voice-choir-sings-r29-a.ogg  kind a, wordless sung sting: an "ooh" in the Varga reference voice
                               (the Choir's base), Chatterbox's own sustained vowel (a take of
                               "Ooooooooooooooh." at the Choir's settings that holds the vowel for
                               16 s), 2.2 s cut from its steadiest part, pitch-flattened onto E3
                               (165 Hz, near her natural pitch) with a slow vibrato by TD-PSOLA;
                               then the Choir's layering, choir() of tts_r18.py (copies at -12, -5,
                               0, +7 semitones with gains 0.8 / 0.55 / 0.45 / 0.3, staggered 0-45
                               ms, a reversed copy at 0.22, the 55/82 Hz drone with low noise, the
                               2.2 s reverb 35 % wet), then radio filter b (radio() there)
  voice-choir-sings-r29-b.ogg  kind a: an "ah" built from a short vowel: 0.5 s of the steady vowel
                               of a Chatterbox take of "Ahhhhhhhhhhhhhhhh..." stretched by TD-PSOLA
                               (its pitch periods replayed back and forth) and sung as the Choir
                               motif's landing, the b2 falling to the root (F3 0.85 s, a 0.15 s
                               glide, E3 1.4 s); then choir() and radio(), as a
  voice-choir-sings-r29-c.ogg  kind b, synthesized choir pad: the music's choir voice (v_choir of
                               music_r02.py: four detuned saws through three vowel formants, the
                               pads of "The Choir Descends") holding an "oo" chord E3 B3 E4 B4 for
                               1.8 s (the layering's voicing, gains 0.8 / 0.55 / 0.45 / 0.3), the
                               music's 150 Hz high-pass, then choir()'s drone and reverb (55/82 Hz,
                               2.2 s, 35 % wet) and radio filter b, so it reads as a transmission
  voice-choir-sings-r29-d.ogg  kind b: the Choir motif's last two notes, F4 (0.75 s) then E4
                               (1.0 s; the tritone drop's landing), sung "ah" by v_choir over its
                               "oo" pad on E4 and B4,
                               as in the intro of "The Choir Descends"; treated as c

Setup: the Chatterbox venv of round 18 (~/.cache/tv-tts/venv-chatterbox, see tts_r18.py); the
post-process runs on the system Python with numpy and ffmpeg (rubberband for choir()'s copies).
Rerun: python3 tools/concept/audio/choir_r29.py            (the two takes + post-process)
       python3 tools/concept/audio/choir_r29.py --post     (post-process only)
Chosen (round 29 closed 2026-10-06): b, written to design/audio/voice/concept/; a, c and d go to
its rejected/ (CHOSEN below).
Raw takes go to ~/.cache/tv-tts/raw/r29/. Seeds are fixed, but GPU sampling is not bit-exact, so a
new take can differ; the post-process prints the segment it picked and its pitch.
"""
import os
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
REF = ROOT / "design" / "audio" / "voice" / "refs" / "ref-varga.wav"
DEST = ROOT / "design" / "audio" / "voice" / "concept"
BASE = Path.home() / ".cache" / "tv-tts"
RAW = BASE / "raw" / "r29"

# The Choir's fixed settings in design/audio/voice/data.yaml.
CHOIR = {"exaggeration": 0.3, "cfg_weight": 0.3, "temperature": 0.7}
# take: (text, seed). Picked from trial seeds: "Ooooh" at seed 30 holds the vowel steadily; the
# "Aaaah" spellings come out short (Chatterbox closes them into "eh" or stops), seed 31 of this one
# has 0.5 s of a steady open vowel (F1 about 440 Hz, F2 1200 Hz).
TAKES = {"ooh": ("Ooooooooooooooh.", 30), "ah": ("Ahhhhhhhhhhhhhhhh...", 31)}
# The user's choice (round 29 closed 2026-10-06): the chosen option is written to DEST, the others
# to DEST / "rejected".
CHOSEN = "b"

E3, F3, E4, B3, B4, F4 = 164.81, 174.61, 329.63, 246.94, 493.88, 349.23


def worker():
    import random

    import numpy as np
    import soundfile as sf
    import torch
    from chatterbox.tts import ChatterboxTTS
    model = ChatterboxTTS.from_pretrained(device="cuda")
    RAW.mkdir(parents=True, exist_ok=True)
    for name, (text, seed) in TAKES.items():
        for f in (random.seed, np.random.seed, torch.manual_seed):
            f(seed)
        wav = model.generate(text, audio_prompt_path=str(REF), **CHOIR)
        wav = wav.cpu().numpy().reshape(-1)
        sf.write(str(RAW / f"{name}.wav"), wav.astype("float32"), model.sr)
        print(name, round(len(wav) / model.sr, 2), "s", flush=True)


# ---------------------------------------------------------------- post-process (system python)

def _r18():
    sys.path.insert(0, str(HERE))
    import tts_r18
    return tts_r18


def f0_track(x, sr, hop=0.01, win=0.04, fmin=110, fmax=320):
    """Autocorrelation pitch per hop: (f0 Hz, rms, clarity 0-1) rows."""
    import numpy as np
    n, h = int(win * sr), int(hop * sr)
    lo, hi = int(sr / fmax), int(sr / fmin)
    w = np.hanning(n)
    rows = []
    for i in range(0, len(x) - n, h):
        f = x[i:i + n] * w
        spec = np.fft.rfft(f, 2 * n)
        ac = np.fft.irfft(np.abs(spec) ** 2)[:n]
        if ac[0] <= 0:
            rows.append((0.0, 0.0, 0.0))
            continue
        k = lo + int(np.argmax(ac[lo:hi]))
        rows.append((sr / k, float(np.sqrt(np.mean(f ** 2))), ac[k] / ac[0]))
    return np.array(rows)


def steady(x, sr, length, hop=0.01):
    """The start (s) of the `length` s stretch whose pitch varies least among the voiced, loud
    frames (clarity > 0.65, within 16 dB of the loudest), and its median f0."""
    import numpy as np
    t = f0_track(x, sr, hop)
    loud = t[:, 1] > t[:, 1].max() * 0.16
    good = (t[:, 2] > 0.65) & loud
    n = int(length / hop)
    best, at = None, 0
    for i in range(0, len(t) - n):
        if not good[i:i + n].all():
            continue
        spread = np.std(np.log(t[i:i + n, 0]))
        if best is None or spread < best:
            best, at = spread, i
    if best is None:
        raise SystemExit(f"no steady {length} s stretch")
    return at * hop, float(np.median(t[at:at + n, 0]))


def pitch_marks(x, sr, f0):
    """Pitch marks (sample indices) on the glottal peaks of a steady vowel near `f0`: one per
    period, each refined to the low-passed signal's peak within a fifth of a period."""
    import numpy as np
    s = _r18()._synth()
    y = s.svf(s.svf(x, 2.5 * f0, mode="lp"), 2.5 * f0, mode="lp")
    period = sr / f0
    sign = 1 if np.max(y) >= -np.min(y) else -1
    y = y * sign
    marks = [int(period) + int(np.argmax(y[int(period):int(2 * period)]))]
    while marks[-1] + 2 * period < len(y):
        c = marks[-1] + period
        w = int(0.2 * period)
        lo = int(c) - w
        m = lo + int(np.argmax(y[lo:int(c) + w]))
        period = 0.8 * period + 0.2 * (m - marks[-1])
        marks.append(m)
    return np.array(marks)


def psola(x, sr, marks, f_out, rng):
    """TD-PSOLA: two-period Hann grains around the analysis marks, overlap-added one per output
    period of the target pitch `f_out` (Hz per output sample); the marks are walked back and forth
    so a short vowel can be held. The grains keep the voice's formants."""
    import numpy as np
    n = len(f_out)
    seq = list(range(1, len(marks) - 1))
    seq = seq + seq[-2:0:-1]
    pad = int(sr / 80)
    out = np.zeros(n + 2 * pad)
    t, k = 0.0, int(rng.integers(len(seq)))
    while t < n:
        i = seq[k % len(seq)]
        p = int(min(marks[i] - marks[i - 1], marks[i + 1] - marks[i]))
        g = x[marks[i] - p:marks[i] + p] * np.hanning(2 * p)
        at = int(t) + pad
        out[at - p:at + p] += g
        t += sr / f_out[min(int(t), n - 1)]
        k += 1
    return out[pad:pad + n]


def sung(x, sr, segment_s, notes, seed):
    """The vowel of `x` sung on `notes`: [(Hz, seconds, glide seconds into it)]: its steadiest
    `segment_s` s, its level evened out, held at a flat pitch with a slow vibrato (5.2 Hz, +-0.5 %, fading in over 0.5 s), a 0.25 s attack and a 0.45 s
    release."""
    import numpy as np
    start, f0 = steady(x, sr, segment_s)
    seg = x[int(start * sr):int((start + segment_s) * sr)]
    seg = seg / np.sqrt(_r18()._synth().onepole_lp(seg ** 2, 8) + 1e-9)  # level evened out
    marks = pitch_marks(seg, sr, f0)
    print(f"  steady {segment_s} s at {start:.2f} s, f0 {f0:.0f} Hz, {len(marks)} marks", flush=True)
    f = []
    prev = notes[0][0]
    for hz, dur, glide in notes:
        m = int(dur * sr)
        g = min(m, int(glide * sr))
        ramp = np.concatenate([prev * (hz / prev) ** (0.5 - 0.5 * np.cos(np.linspace(0, np.pi, g))),
                               np.full(m - g, hz)]) if g else np.full(m, hz)
        f.append(ramp)
        prev = hz
    f = np.concatenate(f)
    t = np.arange(len(f)) / sr
    f = f * (1 + 0.005 * np.sin(2 * np.pi * 5.2 * t) * np.clip(t / 0.5, 0, 1))
    y = psola(seg, sr, marks, f, np.random.default_rng(seed))
    env = np.minimum(1, t / 0.25) * np.clip((t[-1] - t) / 0.45, 0, 1) ** 0.7
    return y * env


def transmission(x):
    """choir()'s room without its copies, for the synthesized pads: the 150 Hz high-pass of the
    music's choir bus, the 55/82 Hz drone with low noise and the 2.2 s reverb, 35 % wet."""
    import numpy as np
    s = _r18()._synth()
    sr = s.SR
    x = s.svf(x, 150, mode="hp")
    x = x / max(1e-9, np.max(np.abs(x)))
    n = len(x) + int(1.2 * sr)
    out = np.zeros(n)
    out[:len(x)] = x
    t = np.arange(n) / sr
    rng = np.random.default_rng(18)
    drone = (s.sine(55.0, n) + 0.6 * s.sine(82.4, n) * (0.6 + 0.4 * s.sine(0.23, n))
             + 0.5 * s.onepole_lp(s.noise(n, rng), 300)) * 0.12
    env = np.minimum(1, t / 0.4) * np.minimum(1, (t[-1] - t) / 0.6)
    out = out + drone * env
    wet = s.reverb(np.vstack([out, out]), seconds=2.2, mix=0.35, seed=18)[0][:n]
    return wet / max(1e-9, np.max(np.abs(wet)))


def pad(chord, dur, vowel, gains, start=0.0):
    """The music's choir voice (v_choir of music_r02.py) on `chord`, mono."""
    import numpy as np
    from music_r02 import v_choir
    s = _r18()._synth()
    voices = [v_choir(hz, dur, vowel) * g for hz, g in zip(chord, gains)]
    n = int((start + dur + 0.6) * s.SR) + 1
    out = np.zeros(n)
    i = int(start * s.SR)
    for v in voices:
        out[i:i + len(v)] += v
    return out


def options():
    """The four sounds before the radio filter, by variant."""
    import numpy as np
    r18 = _r18()
    s = r18._synth()
    sr = s.SR
    ooh = s.decode(RAW / "ooh.wav")[0]
    ah = s.decode(RAW / "ah.wav")[0]
    print("a: ooh, held on E3", flush=True)
    a = r18.choir(sung(ooh, sr, 2.2, [(E3, 2.2, 0)], 29))
    print("b: ah, F3 falling to E3", flush=True)
    b = r18.choir(sung(ah, sr, 0.5, [(F3, 0.85, 0), (E3, 1.55, 0.15)], 30))
    c = transmission(pad([E3, B3, E4, B4], 1.8, "oo", [0.8, 0.55, 0.45, 0.3]))
    from music_r02 import v_choir
    motif = np.zeros(int(2.45 * sr) + 1)
    for hz, at, dur in ((F4, 0.1, 0.75), (E4, 0.85, 1.0)):
        v = v_choir(hz, dur, "ah") * 0.55
        motif[int(at * sr):int(at * sr) + len(v)] += v[: len(motif) - int(at * sr)]
    under = pad([E4, B4], 1.85, "oo", [0.35, 0.35])
    m = max(len(motif), len(under))
    d = transmission(np.pad(motif, (0, m - len(motif))) + np.pad(under, (0, m - len(under))))
    return {"a": a, "b": b, "c": c, "d": d}


def post():
    r18 = _r18()
    s = r18._synth()
    DEST.mkdir(parents=True, exist_ok=True)
    for variant, x in options().items():
        y = r18.radio(x)  # filter b
        tags = {"COMMENT": f"tools/concept/audio/choir_r29.py {variant}"}
        out = DEST if variant == CHOSEN else DEST / "rejected"
        out.mkdir(parents=True, exist_ok=True)
        path = out / f"voice-choir-sings-r29-{variant}.ogg"
        s.write_ogg(path, y, quality=4, tags=tags)
        z = s.decode(path)
        print(f"{path.name}: {z.shape[1] / s.SR:.2f} s, {s.lufs(z)[0]:.1f} LUFS, "
              f"peak {s.db(abs(z).max()):.1f} dBFS", flush=True)


def main(args):
    if args[:1] == ["--worker"]:
        worker()
        return
    if args[:1] != ["--post"]:
        env = dict(os.environ, HF_HOME=str(BASE / "hf"), TOKENIZERS_PARALLELISM="false",
                   TORCH_FORCE_NO_WEIGHTS_ONLY_LOAD="1")
        py = BASE / "venv-chatterbox" / "bin" / "python"
        subprocess.run([str(py), __file__, "--worker"], env=env, check=True)
    post()


if __name__ == "__main__":
    main(sys.argv[1:])
