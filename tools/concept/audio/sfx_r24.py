#!/usr/bin/env python3
"""Concept round 24: the Coilwyrm's death bursts, four options a-d (closed: segment burst b, head
burst c), and the chosen pair's production builders.

The head's death sets off a ripple down the chain: every segment and then the tail bursts in turn,
one every `POP_INTERVAL` s (design/enemies/air/coilwyrm, data.yaml `segment_chain.pop_interval`).
Per option this writes, into design/audio/sfx/concept/ (the unchosen ones into concept/rejected/):

  enemy-coilwyrm-burst-r24-<v>.ogg       one segment's burst (also a segment or the tail shot on
                                         its own)
  enemy-coilwyrm-head-burst-r24-<v>.ogg  the head's bigger burst that starts the ripple: a longer
                                         cut of the same source played 18 % slower, over a sub
                                         thump
  enemy-coilwyrm-death-r24-<v>.ogg       the round's review preview, not a game file: the whole
                                         chained death at the round's rhythm (0.05 s, the bursts
                                         4 dB under the explosions, six instances)

and the chosen pair's preview at the game's rhythm:

  enemy-coilwyrm-death-final-r24-a.ogg   the head burst c, then 13 bursts b 0.25 s apart, each at
                                         the explosion level and pitched by the member's hit box
                                         width down the taper, as FlightSounds plays them; built
                                         from the Freesound originals when they are cached
                                         (freesound_fetch.py --download), else from the previews

  a "Gib sound"       RoozyDB, Freesound 504629, CC0 1.0: a sharp wet crack with gooey bits
  b "Burst Flesh"     magnuswaker, Freesound 581092, CC0 1.0: a dense fleshy burst with a low body
  c "Messy Splat 3"   FoolBoyMedia, Freesound 237927, CC-BY 4.0: a bright, messy splatter
  d synthesized       a pressurised pop: a membrane rupture (band noise), a chitin crackle, a
                      saturated gas "bloop" falling 230 -> 70 Hz with a low thud, and wet bubble
                      chirps and a splash hiss scattered over 200 ms

Levels (measured on the existing sounds over the loudest 100 ms of the 200 Hz-5 kHz band, the
band of import_sfx.py): a segment burst at -16 dB (between explosion-tiny-r03-a -18 and -b -14),
ceiling -1.5 dBFS; the head's burst at -14 dB (explosion-r02-a -15), ceiling -1.5 dBFS.

The concept files are cut from the public HQ previews (import_sfx.py's fetch and process).
PRODUCTION holds the chosen pair's treatment for tools/art/sfx_originals.py, which applies it to
the Freesound originals and writes assets/sfx/enemy-coilwyrm-burst-r24-b.ogg and
assets/sfx/enemy-coilwyrm-head-burst-r24-c.ogg.

Usage: python3 tools/concept/audio/sfx_r24.py [a b c d final]
Deterministic: fixed seeds.
"""
import os
import sys
import tempfile
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
from import_sfx import band_rms_db, fetch, process  # noqa: E402
from sfx_r11 import n_of, place  # noqa: E402
from synth import SR, decode, exp_decay, limiter, noise, ramp, saturate, sine, svf, write_ogg, write_wav  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "sfx" / "concept"
CACHE = Path.home() / ".cache" / "terran-vanguard" / "freesound"

SEGMENT_DB, SEGMENT_CEILING = -16.0, -1.5
HEAD_DB, HEAD_CEILING = -14.0, -1.5
HEAD_SLOWDOWN = 0.82          # the head's burst plays at this speed: lower and longer

# The round's review rhythm and mix (the options' previews, as the user heard them).
ROUND_INTERVAL = 0.05         # s
ROUND_POP_DB = -4.0           # the pops under the explosion level
ROUND_PITCH_START, ROUND_PITCH_STEP, ROUND_PITCH_MAX = 0.94, 0.018, 1.2
ROUND_INSTANCES = 6
POP_JITTER = 0.02
EXPLOSIONS_DB = -2.0

# The game's rhythm and mix (FlightSounds, the Coilwyrm's data), kept in step by hand.
POP_INTERVAL = 0.25           # s; 15 simulation steps at 60 Hz
SEGMENTS = 12                 # at medium: 54 -> 27 px sprites, hit boxes 70 %; then the tail
TAIL_WIDTH = 28.0             # px, the tail's hit box
RIPPLE_REFERENCE_WIDTH, RIPPLE_PITCH_START, RIPPLE_PITCH_EXPONENT, RIPPLE_PITCH_MAX = 37.8, 0.94, 0.35, 1.2

CHOSEN = {"burst": "b", "head-burst": "c"}

RECORDED = {
    "a": dict(page="https://freesound.org/people/RoozyDB/sounds/504629/",
              preview="https://cdn.freesound.org/previews/504/504629_8171657-hq.ogg",
              licence="CC0 1.0", segment=dict(offset=0.0, length=0.30, fade=0.16, highpass=90),
              head=dict(offset=0.0, length=0.45, fade=0.20, highpass=50)),
    "b": dict(page="https://freesound.org/people/magnuswaker/sounds/581092/",
              preview="https://cdn.freesound.org/previews/581/581092_11537497-hq.ogg",
              licence="CC0 1.0", segment=dict(offset=0.015, length=0.24, fade=0.14, highpass=90),
              head=dict(offset=0.0, length=0.62, fade=0.30, highpass=50)),
    "c": dict(page="https://freesound.org/people/FoolBoyMedia/sounds/237927/",
              preview="https://cdn.freesound.org/previews/237/237927_4019029-hq.ogg",
              licence="CC-BY 4.0", segment=dict(offset=0.0, length=0.22, fade=0.14, highpass=90),
              head=dict(offset=0.0, length=0.62, fade=0.30, highpass=50)),
}


def short_term_band_db(st, window=0.1):
    """The loudest `window` s of the 200 Hz-5 kHz band, as RMS dB."""
    n = n_of(window)
    hop = n // 4
    return max(band_rms_db(st[:, i:i + n]) for i in range(0, max(1, st.shape[1] - n + 1), hop))


def level(st, band_db, ceiling_db):
    st = st - st.mean(axis=1, keepdims=True)
    for _ in range(3):  # limiting lowers the level a little; make it up
        st = limiter(st * 10 ** ((band_db - short_term_band_db(st)) / 20), ceiling_db)
    return st


def resample(st, speed):
    """Played at `speed` (OpenAL's pitch): shorter and higher above 1."""
    n = int(st.shape[1] / speed)
    src = np.arange(n) * speed
    return np.array([np.interp(src, np.arange(st.shape[1]), ch) for ch in st])


def thump(seconds=0.4, start=85.0, end=38.0, gain=0.3):
    """A sub drop under the head's burst."""
    n = n_of(seconds)
    sig = sine(ramp(n, start, end, tau=0.08), n) * exp_decay(n, 0.11) * gain
    sig[:n_of(0.003)] *= np.linspace(0, 1, n_of(0.003))
    return np.vstack([sig, sig])


def recorded(v, part, cache):
    src = RECORDED[v]
    cut = dict(src[part], peak=-1.0)
    return process(cut, fetch(src["preview"], cache))


def splat(seed, length, bloop=(230.0, 70.0), thud_hz=55.0, drops=12, spread=0.2):
    """Option d: a pressurised pop in a wet body (mono)."""
    rng = np.random.default_rng(seed)
    n = n_of(length)
    out = np.zeros(n)
    # The membrane rupture: a bright band-noise snap.
    m = n_of(0.03)
    snap = noise(m, rng) * exp_decay(m, 0.006)
    out[:m] += 0.9 * svf(svf(snap, 5000), 800, mode="hp")
    # The chitin crackle: a few hard clicks in the first 40 ms.
    for _ in range(10):
        k = n_of(0.0012)
        click = svf(noise(k, rng), 2500, mode="hp") * np.hanning(k)
        place(out, click, rng.uniform(0.0, 0.05), rng.uniform(0.6, 1.0))
    # The gas bloop: a saturated sine falling fast, with a low thud.
    m = n_of(0.22)
    body = sine(ramp(m, bloop[0], bloop[1], tau=0.04), m) * exp_decay(m, 0.07)
    out[:m] += 0.45 * saturate(body * 2.0, 2.0)
    out[:m] += 0.2 * sine(thud_hz, m) * exp_decay(m, 0.05)
    # Wet bubble chirps, denser at the start.
    for i in range(drops):
        at = spread * (i / drops) ** 1.6 + rng.uniform(0, 0.012)
        k = n_of(rng.uniform(0.012, 0.03))
        f0 = rng.uniform(900, 2600)
        chirp = sine(ramp(k, f0, f0 * rng.uniform(1.2, 1.5), curve="exp"), k) * exp_decay(k, rng.uniform(0.006, 0.014))
        place(out, chirp, at, 0.45 * (1 - 0.6 * i / drops))
    # The splash hiss.
    m = n_of(min(length, 0.25))
    hiss = svf(svf(noise(m, rng), 6000), 1500, mode="hp") * exp_decay(m, 0.06)
    hiss[:n_of(0.004)] *= np.linspace(0, 1, n_of(0.004))
    out[:m] += 0.5 * hiss
    out = svf(svf(out, 90, mode="hp"), 90, mode="hp")  # no rumble under the ripple
    b = n_of(0.06)
    out[-b:] *= (0.5 + 0.5 * np.cos(np.linspace(0, np.pi, b))) ** 2
    return out


def synthesized(part):
    if part == "segment":
        mono = splat(2401, 0.26)
    else:
        mono = splat(2402, 0.60, bloop=(170.0, 45.0), thud_hz=42.0, drops=20, spread=0.35)
    # A slightly different right channel (another crackle seed) so it is not dead mono.
    return np.vstack([mono, 0.92 * mono + 0.08 * np.roll(mono, n_of(0.0007))])


def head_from(segment_source):
    st = resample(segment_source, HEAD_SLOWDOWN)
    th = thump()
    out = np.zeros((2, max(st.shape[1], th.shape[1])))
    out[:, :st.shape[1]] += st / max(1e-9, np.abs(st).max())
    out[:, :th.shape[1]] += th
    return out


def round_preview(head, segment):
    """The chained death at the round's rhythm (0.05 s, 4 dB under, six instances)."""
    rng = np.random.default_rng(24)
    gain_head = 10 ** (EXPLOSIONS_DB / 20)
    gain_pop = 10 ** ((EXPLOSIONS_DB + ROUND_POP_DB) / 20)
    voices = []  # (start sample, signal)
    for k in range(1, SEGMENTS + 2):
        pitch = min(ROUND_PITCH_MAX, ROUND_PITCH_START * (1 + ROUND_PITCH_STEP) ** (k - 1))
        pitch *= 1 + rng.uniform(-POP_JITTER, POP_JITTER)
        voices.append((n_of(k * ROUND_INTERVAL), resample(segment, pitch) * gain_pop))
    end = max(s + v.shape[1] for s, v in voices)
    out = np.zeros((2, max(end, head.shape[1]) + n_of(0.1)))
    out[:, :head.shape[1]] += head * gain_head
    for i, (start, sig) in enumerate(voices):
        stop = voices[i + ROUND_INSTANCES][0] if i + ROUND_INSTANCES < len(voices) else start + sig.shape[1]
        length = min(sig.shape[1], stop - start)
        out[:, start:start + length] += sig[:, :length]
    return out


def member_widths():
    """The hit box widths of the bursting members, head side first: 12 segments, then the tail."""
    return [0.7 * (54 + (27 - 54) * i / (SEGMENTS - 1)) for i in range(SEGMENTS)] + [TAIL_WIDTH]


def ripple_pitch(width):
    """FlightSounds.ripplePitch: higher as the member narrows down the taper."""
    return min(RIPPLE_PITCH_MAX, RIPPLE_PITCH_START * (RIPPLE_REFERENCE_WIDTH / width) ** RIPPLE_PITCH_EXPONENT)


def game_preview(head, segment):
    """The chained death as the game plays it: the head's burst, then one burst every 0.25 s."""
    rng = np.random.default_rng(2424)
    gain = 10 ** (EXPLOSIONS_DB / 20)
    widths = member_widths()
    out = np.zeros((2, n_of(len(widths) * POP_INTERVAL) + segment.shape[1] * 2 + head.shape[1]))
    out[:, :head.shape[1]] += head * gain
    for k, width in enumerate(widths, start=1):
        sig = resample(segment, ripple_pitch(width) * (1 + rng.uniform(-POP_JITTER, POP_JITTER))) * gain
        start = n_of(k * POP_INTERVAL)
        out[:, start:start + sig.shape[1]] += sig
    last = np.nonzero(np.abs(out).max(axis=0) > 1e-6)[0][-1]
    return out[:, :last + n_of(0.1)]


def clipped(path, tmp):
    """An original decoded beyond full scale, clipped as tools/art/sfx_originals.py does."""
    x = decode(path)
    if np.max(np.abs(x)) <= 1:
        return path
    out = Path(tmp) / "clipped.wav"
    write_wav(out, np.clip(x, -1, 1))
    return out


def build_segment(path):
    """The chosen segment burst (b) from a source file: the original or the preview."""
    return level(process(dict(RECORDED["b"]["segment"], peak=-1.0), path), SEGMENT_DB, SEGMENT_CEILING)


def build_head(path):
    """The chosen head burst (c) from a source file: the original or the preview."""
    return level(head_from(process(dict(RECORDED["c"]["head"], peak=-1.0), path)), HEAD_DB, HEAD_CEILING)


# The chosen pair for tools/art/sfx_originals.py: its page (the original's id), its ceiling and
# the treatment applied to the (clipped) original.
PRODUCTION = {
    "enemy-coilwyrm-burst-r24-b": dict(page=RECORDED["b"]["page"], peak=SEGMENT_CEILING, build=build_segment),
    "enemy-coilwyrm-head-burst-r24-c": dict(page=RECORDED["c"]["page"], peak=HEAD_CEILING, build=build_head),
}


def original(page):
    sound = page.rstrip("/").rsplit("/", 1)[1]
    files = sorted(p for p in CACHE.glob(f"{sound}_*") if not p.name.endswith(".part"))
    return files[0] if files else None


def final(cache):
    with tempfile.TemporaryDirectory() as tmp:
        sources = []
        for v in (CHOSEN["burst"], CHOSEN["head-burst"]):
            path = original(RECORDED[v]["page"])
            sources.append(clipped(path, tmp) if path else fetch(RECORDED[v]["preview"], cache))
            print(f"  {v}: {'original ' + path.name if path else 'HQ preview (no cached original)'}")
        segment, head = build_segment(sources[0]), build_head(sources[1])
        out = write_ogg(OUT / "enemy-coilwyrm-death-final-r24-a.ogg", limiter(game_preview(head, segment), -1.0),
                        max_peak_db=-1.0)
    print(f"wrote {out.relative_to(ROOT)}")


def main(argv):
    cache = Path(os.environ.get("TMPDIR", tempfile.gettempdir())) / "conceptgame-sfx-cache"
    cache.mkdir(parents=True, exist_ok=True)
    for v in argv or ["a", "b", "c", "d", "final"]:
        if v == "final":
            final(cache)
            continue
        if v in RECORDED:
            segment = recorded(v, "segment", cache)
            head = head_from(recorded(v, "head", cache))
        else:
            segment = synthesized("segment")
            head = head_from(synthesized("head"))
        segment = level(segment, SEGMENT_DB, SEGMENT_CEILING)
        head = level(head, HEAD_DB, HEAD_CEILING)
        files = (
            ("burst", segment, SEGMENT_CEILING),
            ("head-burst", head, HEAD_CEILING),
            ("death", limiter(round_preview(head, segment), -1.0), -1.0),
        )
        for kind, st, ceiling in files:
            where = OUT if CHOSEN.get(kind) == v else OUT / "rejected"
            out = write_ogg(where / f"enemy-coilwyrm-{kind}-r24-{v}.ogg", st, max_peak_db=ceiling)
            print(f"wrote {out.relative_to(ROOT)}")


if __name__ == "__main__":
    main(sys.argv[1:])
