"""Tiny deterministic numpy synthesizer for concept audio.

Building blocks used by sfx.py and music.py:
  oscillators   saw / pulse (PolyBLEP band-limited), triangle, sine, noise
  envelopes     adsr, exp_decay, ramp (for pitch and filter envelopes)
  filters       svf (resonant state-variable LP/BP/HP with per-sample cutoff), one-pole
  effects       delay (filtered feedback echo), reverb (convolution with a synthetic IR),
                bitcrush, saturate, pan
  sequencing    note_freq, parse_track (tracker-style 16th-step strings), Timeline mixer
  mastering     limiter (look-ahead, no overshoot), lufs (via ffmpeg), master, write_ogg

Only numpy and the ffmpeg binary are required. All randomness goes through explicit seeds.
"""
import json
import re
import subprocess
import tempfile
import wave
from pathlib import Path

import numpy as np

SR = 44100


def t_axis(dur):
    return np.arange(int(round(dur * SR))) / SR


def _as_array(value, n):
    if np.isscalar(value):
        return np.full(n, float(value))
    value = np.asarray(value, dtype=float)
    if len(value) < n:
        value = np.concatenate([value, np.full(n - len(value), value[-1])])
    return value[:n]


# ---------------------------------------------------------------- oscillators

def phase(freq, n, start=0.0):
    """Phase in cycles [0,1) for a (possibly time-varying) frequency."""
    f = _as_array(freq, n)
    ph = start + np.cumsum(f) / SR
    return ph - np.floor(ph), f / SR


def _blep(t, dt):
    out = np.zeros_like(t)
    dt = np.maximum(dt, 1e-9)
    lo = t < dt
    x = t[lo] / dt[lo]
    out[lo] = 2 * x - x * x - 1
    hi = t > 1 - dt
    x = (t[hi] - 1) / dt[hi]
    out[hi] = x * x + 2 * x + 1
    return out


def saw(freq, n, start=0.0):
    t, dt = phase(freq, n, start)
    return 2 * t - 1 - _blep(t, dt)


def pulse(freq, n, width=0.5, start=0.0):
    t, dt = phase(freq, n, start)
    w = np.clip(_as_array(width, n), 0.02, 0.98)
    out = np.where(t < w, 1.0, -1.0)
    out += _blep(t, dt)
    out -= _blep((t - w) % 1.0, dt)
    return out - (2 * w - 1)  # remove DC of the duty cycle


def triangle(freq, n, start=0.0):
    t, _ = phase(freq, n, start)
    return 4 * np.abs(t - 0.5) - 1


def sine(freq, n, start=0.0):
    t, _ = phase(freq, n, start)
    return np.sin(2 * np.pi * t)


def noise(n, rng):
    return rng.uniform(-1, 1, n)


def supersaw(freq, n, voices=5, detune=0.012, rng=None):
    """Detuned saw stack (the 90s trance lead)."""
    rng = rng or np.random.default_rng(0)
    out = np.zeros(n)
    f = _as_array(freq, n)
    for i in range(voices):
        d = (i - (voices - 1) / 2) / max(1, (voices - 1) / 2) * detune
        out += saw(f * (1 + d), n, start=rng.uniform())
    return out / np.sqrt(voices)


# ---------------------------------------------------------------- envelopes

def adsr(n, a=0.005, d=0.1, s=0.7, r=0.1, gate=None):
    """ADSR over n samples; gate (seconds) = when release starts (default n - r)."""
    gate_n = int((gate if gate is not None else n / SR - r) * SR)
    gate_n = max(0, min(gate_n, n))
    env = np.zeros(n)
    a_n, d_n = max(1, int(a * SR)), max(1, int(d * SR))
    idx = np.arange(gate_n)
    att = idx / a_n
    dec = 1 - (1 - s) * np.clip((idx - a_n) / d_n, 0, 1)
    env[:gate_n] = np.where(idx < a_n, att, dec)
    level = env[gate_n - 1] if gate_n > 0 else 0.0
    rel_n = n - gate_n
    if rel_n > 0:
        env[gate_n:] = level * np.exp(-np.arange(rel_n) / max(1, r * SR) * 5)
    return env


def exp_decay(n, tau):
    return np.exp(-np.arange(n) / (tau * SR))


def ramp(n, start, end, curve="exp", tau=None):
    """Glide from start to end: 'exp' (log-frequency), 'lin', or exponential approach (tau)."""
    x = np.linspace(0, 1, n)
    if tau is not None:
        return end + (start - end) * np.exp(-np.arange(n) / (tau * SR))
    if curve == "exp":
        return start * (end / start) ** x
    return start + (end - start) * x


def fade(sig, fin=0.002, fout=0.005):
    """Short fades at both ends so nothing clicks."""
    sig = sig.copy()
    a, b = int(fin * SR), int(fout * SR)
    if a:
        sig[..., :a] *= np.linspace(0, 1, a)
    if b:
        sig[..., -b:] *= np.linspace(1, 0, b)
    return sig


# ---------------------------------------------------------------- filters

def svf(x, cutoff, q=0.707, mode="lp"):
    """Topology-preserving state-variable filter, per-sample cutoff (Hz)."""
    n = len(x)
    fc = np.clip(_as_array(cutoff, n), 10, SR * 0.45)
    g = np.tan(np.pi * fc / SR)
    k = 1.0 / q
    a1 = 1 / (1 + g * (g + k))
    a2 = g * a1
    a3 = g * a2
    xs, A1, A2, A3 = x.tolist(), a1.tolist(), a2.tolist(), a3.tolist()
    out = [0.0] * n
    ic1 = ic2 = 0.0
    for i in range(n):
        v3 = xs[i] - ic2
        v1 = A1[i] * ic1 + A2[i] * v3
        v2 = ic2 + A2[i] * ic1 + A3[i] * v3
        ic1 = 2 * v1 - ic1
        ic2 = 2 * v2 - ic2
        if mode == "lp":
            out[i] = v2
        elif mode == "bp":
            out[i] = v1
        else:  # hp
            out[i] = xs[i] - k * v1 - v2
    return np.array(out)


def onepole_lp(x, cutoff):
    """Cheap static one-pole low-pass (vectorised via FFT of its impulse response)."""
    a = np.exp(-2 * np.pi * cutoff / SR)
    m = min(len(x), int(SR * 0.05) + int(5 / max(1e-6, 1 - a)))
    h = (1 - a) * a ** np.arange(max(8, m))
    return fft_convolve(x, h)[: len(x)]


def fft_convolve(x, h):
    n = len(x) + len(h) - 1
    size = 1 << (n - 1).bit_length()
    return np.fft.irfft(np.fft.rfft(x, size) * np.fft.rfft(h, size), size)[:n]


# ---------------------------------------------------------------- effects

def saturate(x, drive=1.0):
    return np.tanh(x * drive) / np.tanh(drive)


def bitcrush(x, bits=8, downsample=1):
    if downsample > 1:
        x = np.repeat(x[::downsample], downsample)[: len(x)]
    q = 2 ** (bits - 1)
    return np.round(x * q) / q


def pan(mono, position=0.0):
    """Equal-power pan, position -1 (left) .. 1 (right). Returns (2, n)."""
    ang = (position + 1) * np.pi / 4
    return np.vstack([mono * np.cos(ang), mono * np.sin(ang)])


def delay(stereo, time, feedback=0.35, mix=0.3, damp=4000, pingpong=True, tail=None):
    """Feedback echo as a finite sum of damped taps; extends the signal by `tail` s."""
    tail = tail if tail is not None else time * 8
    ext = int(tail * SR)
    x = np.pad(stereo, ((0, 0), (0, ext)))
    out = x.copy()
    d = int(time * SR)
    tap = x.copy()
    gain = 1.0
    k = 0
    while True:
        k += 1
        gain *= feedback
        if gain < 0.01 or k * d >= x.shape[1]:
            break
        tap = np.vstack([onepole_lp(tap[0], damp), onepole_lp(tap[1], damp)])
        shifted = np.zeros_like(x)
        shifted[:, k * d:] = tap[:, : x.shape[1] - k * d]
        if pingpong and k % 2:
            shifted = shifted[::-1]
        out += mix * gain * shifted / feedback
    return out


def reverb_ir(seconds=2.0, damping=0.5, seed=1):
    """Synthetic stereo room IR: decorrelated noise, exponential decay, darker tail."""
    rng = np.random.default_rng(seed)
    n = int(seconds * SR)
    t = np.arange(n) / SR
    env = np.exp(-t * 6.9 / seconds)  # -60 dB at `seconds`
    ir = []
    for _ in range(2):
        bright = rng.standard_normal(n)
        dark = onepole_lp(rng.standard_normal(n), 1500)
        dark *= np.std(bright) / (np.std(dark) + 1e-9)
        mixw = np.clip(t / seconds * (1 + damping * 2), 0, 1)
        ir.append((bright * (1 - mixw) + dark * mixw) * env)
    ir = np.array(ir)
    ir[:, : int(0.008 * SR)] *= np.linspace(0, 1, int(0.008 * SR))  # predelay-ish onset
    return ir / np.sqrt(np.sum(ir ** 2) / 2)


def reverb(stereo, seconds=2.0, mix=0.25, damping=0.5, seed=1):
    ir = reverb_ir(seconds, damping, seed)
    wet = np.vstack([fft_convolve(stereo[0], ir[0]), fft_convolve(stereo[1], ir[1])])
    dry = np.pad(stereo, ((0, 0), (0, wet.shape[1] - stereo.shape[1])))
    return dry + mix * wet


# ---------------------------------------------------------------- sequencing

NOTE_RE = re.compile(r"^([A-G])([#b]?)(-?\d)$")
SEMIS = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}


def note_number(name):
    m = NOTE_RE.match(name)
    if not m:
        raise ValueError(f"bad note {name!r}")
    letter, acc, octave = m.groups()
    return 12 * (int(octave) + 1) + SEMIS[letter] + {"#": 1, "b": -1, "": 0}[acc]


def note_freq(name_or_midi):
    midi = note_number(name_or_midi) if isinstance(name_or_midi, str) else name_or_midi
    return 440.0 * 2 ** ((midi - 69) / 12)


def parse_track(bars, steps_per_bar=16):
    """Tracker-style pattern: one token per 16th step.

    'A4' starts a note, '-' sustains the previous note, '.' is silence.
    Returns a list of (start_step, length_steps, note_name).
    """
    tokens = []
    for i, bar in enumerate(bars):
        toks = bar.split()
        if len(toks) != steps_per_bar:
            raise ValueError(f"bar {i} has {len(toks)} steps, expected {steps_per_bar}: {bar!r}")
        tokens.extend(toks)
    events, cur = [], None
    for step, tok in enumerate(tokens):
        if tok == "-":
            if cur:
                cur[1] += 1
            continue
        if cur:
            events.append(tuple(cur))
            cur = None
        if tok != ".":
            cur = [step, 1, tok]
    if cur:
        events.append(tuple(cur))
    return events


class Timeline:
    """Stereo mix bus with named groups (for per-group effects / sidechain)."""

    def __init__(self, seconds):
        self.n = int(seconds * SR)
        self.groups = {}

    def add(self, group, sig, at, position=0.0, gain=1.0):
        buf = self.groups.setdefault(group, np.zeros((2, self.n)))
        st = sig if sig.ndim == 2 else pan(sig, position)
        i = int(round(at * SR))
        if i >= self.n:
            return
        m = min(st.shape[1], self.n - i)
        buf[:, i:i + m] += gain * st[:, :m]

    def group(self, name):
        return self.groups.get(name, np.zeros((2, self.n)))


# ---------------------------------------------------------------- mastering

def db(x):
    return 20 * np.log10(np.maximum(x, 1e-12))


def undb(d):
    return 10 ** (d / 20)


def _sliding_min_forward(g, w):
    """min(g[i:i+w]) for each i, via log-doubling shifts."""
    out = g.copy()
    span = 1
    while span < w:
        s = min(span, w - span)
        shifted = np.concatenate([out[s:], np.full(s, out[-1])])
        out = np.minimum(out, shifted)
        span += s
    return out


def limiter(stereo, ceiling_db=-1.5, lookahead=0.003):
    """Look-ahead brickwall limiter that can never overshoot the ceiling."""
    ceiling = undb(ceiling_db)
    peak = np.max(np.abs(stereo), axis=0)
    need = np.minimum(1.0, ceiling / np.maximum(peak, 1e-12))
    w = max(2, int(lookahead * SR))
    m = _sliding_min_forward(need, w)
    c = np.concatenate([[0], np.cumsum(m)])
    idx = np.arange(len(m))
    lo = np.maximum(0, idx - w + 1)
    smooth = (c[idx + 1] - c[lo]) / (idx + 1 - lo)
    # slow release so the limiter does not pump on every transient
    rel = 0.06
    a = np.exp(-1 / (rel * SR))
    sm = smooth.tolist()
    env = [0.0] * len(sm)
    e = 1.0
    for i, v in enumerate(sm):
        e = v if v < e else v + (e - v) * a
        env[i] = e
    gain = np.minimum(np.array(env), smooth)
    return stereo * gain


def write_wav(path, stereo):
    stereo = np.atleast_2d(stereo)
    pcm = (np.clip(stereo.T, -1, 1) * 32767).astype("<i2")
    with wave.open(str(path), "wb") as w:
        w.setnchannels(pcm.shape[1])
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def lufs(stereo):
    """Integrated loudness (LUFS) and true peak (dBTP) measured by ffmpeg's loudnorm."""
    with tempfile.TemporaryDirectory() as tmp:
        p = Path(tmp) / "m.wav"
        write_wav(p, stereo)
        r = subprocess.run(
            ["ffmpeg", "-hide_banner", "-nostats", "-i", str(p), "-af",
             "loudnorm=print_format=json", "-f", "null", "-"],
            capture_output=True, text=True, check=True)
    js = json.loads(r.stderr[r.stderr.rindex("{"):r.stderr.rindex("}") + 1])
    return float(js["input_i"]), float(js["input_tp"])


def master(stereo, target_lufs=-14.0, ceiling_db=-1.5, passes=3):
    """Gain to target loudness, then limit; repeat so limiting losses are made up."""
    out = stereo
    for _ in range(passes):
        loud, _ = lufs(out)
        out = limiter(out * undb(target_lufs - loud), ceiling_db)
    return out


def normalize_peak(stereo, peak_db):
    return stereo * undb(peak_db) / max(1e-12, np.max(np.abs(stereo)))


def decode(path):
    """Decode any audio file to float stereo (2, n) at SR via ffmpeg."""
    raw = subprocess.run(
        ["ffmpeg", "-hide_banner", "-loglevel", "error", "-i", str(path), "-f", "f32le",
         "-ac", "2", "-ar", str(SR), "-"], capture_output=True, check=True).stdout
    return np.frombuffer(raw, dtype="<f4").reshape(-1, 2).T.astype(float)


def write_ogg(path, stereo, quality=6, max_peak_db=-1.0):
    """Encode to OGG Vorbis via ffmpeg (deterministic for identical input).

    Lossy encoding can overshoot the source peak; the encoded file is decoded and, if its
    peak exceeds max_peak_db, re-encoded with correspondingly less gain.
    """
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    for _ in range(4):
        with tempfile.TemporaryDirectory() as tmp:
            wav = Path(tmp) / "x.wav"
            write_wav(wav, stereo)
            subprocess.run(
                ["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(wav),
                 "-c:a", "libvorbis", "-q:a", str(quality), "-map_metadata", "-1",
                 "-fflags", "+bitexact", "-flags:a", "+bitexact", str(path)],
                check=True)
        if max_peak_db is None:
            break
        over = db(np.max(np.abs(decode(path)))) - max_peak_db
        if over <= 0:
            break
        stereo = stereo * undb(-over - 0.1)
    return path
