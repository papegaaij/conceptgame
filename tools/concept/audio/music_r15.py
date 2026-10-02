#!/usr/bin/env python3
"""Concept round 15 music: stems for "Afterburner" (track 4, Act 1 A), the round-11 method of
music_r11.py ("Coalition Rising") applied to the chosen full mix.

Outputs (design/audio/music/concept/):
  afterburner-base-r15-a.ogg   base stem: bass, arpeggio, pads, the breakdown pluck, the risers
                               and the four-on-the-floor kick; no lead, no claps or hats
  afterburner-full-r15-a.ogg   the full mix, rendered in the same pass (written only with --full;
                               the default check proves it is the chosen afterburner-full-r08-a.ogg
                               sample for sample)

Both come from one render of music_r08.afterburner_full(): the kicks are tagged on their way into
the "drums" group and also collected in a "kick" group, and the base stem is mixed from the base
groups with exactly the full mix's effect chains, sidechain duck and final glue reverb. Mastering
is linked as in music_r11 (every loudness pass measures the full mix and applies its gain to both,
the encoder's peak correction too), so base + w * (full - base) crossfades without phasing.

Usage: python3 tools/concept/audio/music_r15.py [--full] [--out DIR]
"""
import sys
from functools import lru_cache
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import music_r08 as r08  # noqa: E402
import music_r11 as r11  # noqa: E402
from music import hp, ns, widen  # noqa: E402
from music_r02 import seam_ratio  # noqa: E402
from synth import SR, db, decode, delay, lufs, reverb, undb, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "music" / "concept"
CHOSEN = OUT / "afterburner-full-r08-a.ogg"


class _Tagged(np.ndarray):
    """A kick: an ndarray that remembers it belongs to the base stem."""


@lru_cache(None)
def _kick(*args, **kw):
    # cached like music.kick(): music_r08.drum_fill() tests `smp is kick()` to collect the fill
    # kicks for the sidechain, so a fresh view per call would drop them from the duck
    return r08_kick(*args, **kw).view(_Tagged)


class _StemSong(r08.LoopSong):
    def hit(self, group, sig, bar, step=0, gain=1.0, position=0.0):
        super().hit(group, sig, bar, step, gain, position)
        if isinstance(sig, _Tagged):
            super().hit("kick", np.asarray(sig), bar, step, gain, position)


r08_kick = r08.kick
r08_sidechain = r08.sidechain
_ducks = []


def _sidechain(*args, **kw):
    """The full mix's sidechain duck, kept so the base stem's bass and pads duck exactly alike."""
    duck = r08_sidechain(*args, **kw)
    _ducks.append(duck)
    return duck


def render():
    """One render of the chosen arrangement; returns (song, full mix, base mix)."""
    r08.kick, r08.LoopSong, r08.sidechain = _kick, _StemSong, _sidechain
    try:
        s, full = r08.afterburner_full()
    finally:
        r08.kick, r08.LoopSong, r08.sidechain = r08_kick, _StemSong.__mro__[1], r08_sidechain
    tl = s.tl
    duck = _ducks[-1]
    # the same chains and coefficients as the end of music_r08.afterburner_full()
    kick = hp(tl.group("kick"), 35)
    bass = hp(tl.group("bass"), 40) * duck
    arp = delay(tl.group("arp"), s.step * 3, feedback=0.4, mix=0.35, damp=3000, tail=0)[:, :tl.n]
    pad = reverb(hp(tl.group("pad"), 160) * duck, seconds=2.5, mix=0.35, seed=21)[:, :tl.n]
    pad = widen(pad, 1.8)
    pluck = delay(tl.group("pluck"), s.step * 3, feedback=0.45, mix=0.4, damp=2500, tail=0)[:, :tl.n]
    pluck = reverb(pluck, seconds=2.4, mix=0.3, seed=24)[:, :tl.n]
    base = kick * 0.9 + bass * 0.8 + arp * 0.6 + pad * 0.55 + pluck * 0.6 + tl.group("fx")
    return s, full, reverb(base, seconds=1.0, mix=0.06, seed=23)[:, :tl.n]


def main(argv):
    out = OUT
    if "--out" in argv:
        i = argv.index("--out")
        out = Path(argv[i + 1])
        argv = argv[:i] + argv[i + 2:]
    s, full, base = render()
    full, loop_start, loop_len = r11.prepare(s, full)
    base, _, _ = r11.prepare(s, base)
    full, base = r11.master_linked(full, base)
    for a in (full, base):
        a[:, :ns(0.005)] *= np.linspace(0, 1, ns(0.005))
    tags = {"LOOPSTART": loop_start, "LOOPLENGTH": loop_len}
    tmp = out / "afterburner-full-r15-a.ogg"
    for _ in range(4):
        write_ogg(tmp, full, max_peak_db=None, tags=tags)
        over = db(np.abs(decode(tmp)).max()) + 1.0
        if over <= 0:
            break
        full, base = full * undb(-over - 0.1), base * undb(-over - 0.1)
    same = CHOSEN.exists() and CHOSEN.read_bytes() == tmp.read_bytes()
    if "--full" not in argv:
        tmp.unlink()
        if not same:
            raise SystemExit("the full mix no longer matches afterburner-full-r08-a.ogg; "
                             "rerun with --full to write it next to the base stem")
    path = write_ogg(out / "afterburner-base-r15-a.ogg", base, max_peak_db=None, tags=tags)
    dec = decode(path)
    print(f"{path.name}: {base.shape[1] / SR:.1f}s  loop {loop_start / SR:.3f}s + {loop_len / SR:.3f}s"
          f"  (samples {loop_start}+{loop_len})  seam {seam_ratio(dec, loop_start, loop_len):.2f}"
          f"  peak {db(np.abs(dec).max()):.1f} dBFS  base vs full {lufs(base)[0] - lufs(full)[0]:+.1f} LU"
          f"  full mix {'identical to r08' if same else 'written as r15'}", flush=True)


if __name__ == "__main__":
    main(sys.argv[1:])
