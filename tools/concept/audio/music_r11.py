#!/usr/bin/env python3
"""Concept round 11 music: stems for "Coalition Rising" (track 5, Act 1 B).

Outputs (design/audio/music/concept/):
  coalition-rising-base-r11-a.ogg   base stem: string ostinato, string pads, timpani, harp,
                                     flute, choir pads and the risers; no brass, no lead strings,
                                     no taiko, snare, hats, crash or toms
  coalition-rising-full-r11-a.ogg   the full mix, rendered in the same pass (written only with
                                     --full; the default check proves that it is the chosen
                                     coalition-rising-full-r08-a.ogg sample for sample)

Both come from one render of music_r08.coalition_full(): the song is rendered once, the timpani
hits are tagged on their way into the "perc" group and also collected in a "timp" group, and
the base stem is mixed from the base groups with exactly the full mix's effect chains and
section lift. Mastering is linked: every loudness pass of synth.master() measures the full mix
and applies the same gain (and the same ceiling) to both, and the encoder's peak correction is
shared too. So the two files have the same length, the same LOOPSTART / LOOPLENGTH and the same
gain, and base + w * (full - base) is a crossfade from the base stem to the full mix: the game
plays both from the same sample and fades the full mix in (intensity) over the base, or
crossfades between them, without phasing.

Usage: python3 tools/concept/audio/music_r11.py [--full] [--out DIR]
"""
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import music_r08 as r08  # noqa: E402
from music import hp, ns, widen  # noqa: E402
from music_r02 import add_tail, seam_ratio  # noqa: E402
from synth import SR, db, decode, delay, limiter, lufs, reverb, undb, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "music" / "concept"
CHOSEN = OUT / "coalition-rising-full-r08-a.ogg"


class _Tagged(np.ndarray):
    """A timpani hit: an ndarray that remembers it belongs to the base stem."""


def _timpani(*args, **kw):
    return r08_timpani(*args, **kw).view(_Tagged)


class _StemSong(r08.FracLoopSong):
    def hit(self, group, sig, bar, step=0, gain=1.0, position=0.0):
        super().hit(group, sig, bar, step, gain, position)
        if isinstance(sig, _Tagged):
            super().hit("timp", np.asarray(sig), bar, step, gain, position)


r08_timpani = r08.timpani


def render():
    """One render of the chosen arrangement; returns (song, full mix, base mix)."""
    r08.timpani, r08.FracLoopSong = _timpani, _StemSong
    try:
        s, full = r08.coalition_full()
    finally:
        r08.timpani, r08.FracLoopSong = r08_timpani, _StemSong.__mro__[1]
    tl = s.tl
    # the same chains and coefficients as the end of music_r08.coalition_full()
    ost = reverb(hp(tl.group("ost"), 45), seconds=1.8, mix=0.22, seed=31)[:, :tl.n]
    strings = widen(reverb(hp(tl.group("strings"), 110), seconds=3.2, mix=0.4, seed=32)[:, :tl.n], 1.5)
    timp = reverb(hp(tl.group("timp"), 38), seconds=2.8, mix=0.25, damping=0.8, seed=34)[:, :tl.n]
    choir = widen(reverb(hp(tl.group("choir"), 150), seconds=3.2, mix=0.4, seed=35)[:, :tl.n], 1.5)
    harp = delay(tl.group("harp"), s.step * 3, feedback=0.35, mix=0.3, damp=3000, tail=0)[:, :tl.n]
    harp = reverb(harp, seconds=2.4, mix=0.3, seed=36)[:, :tl.n]
    wood = reverb(tl.group("wood"), seconds=2.4, mix=0.3, seed=37)[:, :tl.n]
    lift = s.curve([(0, 1.0), (23.75, 1.0), (24.25, 1.41), (31.5, 1.41), (32, 1.0)])
    base = (ost * 0.7 + strings * 0.6 + timp * 0.8 + choir * 0.45 + harp * 0.5 + wood * 0.6
            + tl.group("fx")) * lift
    return s, full, base


def prepare(s, mix):
    """music_r08.render_loop() up to the mastering: DC, intro + loop assembly, fade tail."""
    mix = mix - mix.mean(axis=1, keepdims=True)
    audio, loop_start, loop_len = s.assemble(mix)
    return add_tail(audio, loop_start, 2, int(round(16 * s.step_n))), loop_start, loop_len


def master_linked(full, base, target_lufs=-14.0, ceiling_db=-2.0, passes=3):
    """synth.master() on the full mix; the base stem gets the same gain in every pass."""
    for _ in range(passes):
        gain = undb(target_lufs - lufs(full)[0])
        full, base = limiter(full * gain, ceiling_db), limiter(base * gain, ceiling_db)
    return full, base


def main(argv):
    out = OUT
    if "--out" in argv:
        i = argv.index("--out")
        out = Path(argv[i + 1])
        argv = argv[:i] + argv[i + 2:]
    s, full, base = render()
    full, loop_start, loop_len = prepare(s, full)
    base, _, _ = prepare(s, base)
    full, base = master_linked(full, base)
    for a in (full, base):
        a[:, :ns(0.005)] *= np.linspace(0, 1, ns(0.005))
    tags = {"LOOPSTART": loop_start, "LOOPLENGTH": loop_len}
    tmp = out / "coalition-rising-full-r11-a.ogg"
    # the encoder's peak correction (synth.write_ogg) for the full mix, applied to both
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
            raise SystemExit("the full mix no longer matches coalition-rising-full-r08-a.ogg; "
                             "rerun with --full to write it next to the base stem")
    path = write_ogg(out / "coalition-rising-base-r11-a.ogg", base, max_peak_db=None, tags=tags)
    dec = decode(path)
    print(f"{path.name}: {base.shape[1] / SR:.1f}s  loop {loop_start / SR:.3f}s + {loop_len / SR:.3f}s"
          f"  (samples {loop_start}+{loop_len})  seam {seam_ratio(dec, loop_start, loop_len):.2f}"
          f"  peak {db(np.abs(dec).max()):.1f} dBFS  base vs full {lufs(base)[0] - lufs(full)[0]:+.1f} LU"
          f"  full mix {'identical to r08' if same else 'written as r11'}", flush=True)


if __name__ == "__main__":
    main(sys.argv[1:])
