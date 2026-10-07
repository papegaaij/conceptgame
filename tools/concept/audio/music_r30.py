#!/usr/bin/env python3
"""Concept round 30 music: the base stem of "Homefront" (track 6, Level 08), the round-11 method of
music_r11.py ("Coalition Rising") and music_r15.py ("Afterburner") applied to the chosen full mix.

Outputs (design/audio/music/concept/, or the --out directory; tools/art/themes.py runs it with --out
into a temporary directory and keeps only the production file assets/music/homefront-base.ogg, since
the stem went straight to production without a concept round):
  homefront-base-r30-a.ogg   base stem: the string / pluck ostinato, the arpeggio, the bass, the
                             string and choir pads (with the choir's "oo" lines), the kicks, the
                             timpani and the sirens and risers; no lead synth, no brass (stabs,
                             hits or horn lines), no counter-melody strings, and no breakbeat top
                             (snares, ghost snares, snare rolls, hats), taiko or crashes
  homefront-full-r30-a.ogg   the full mix, rendered in the same pass (written only with --full;
                             the default check proves it is the chosen homefront-full-r08-a.ogg
                             byte for byte)

Both come from one render of music_r08.homefront_full(): the kicks are tagged on their way into the
"drums" group and the timpani on their way into the "perc" group, and both are also collected in a
"kick" and a "timp" group. The full mix's mixdown() call is captured (its group spec and the kick
times of its sidechain duck), and the base stem is the same mixdown() over the base groups, each with
exactly the full mix's chain (high-pass, duck, delay, reverb seed, widening, gain); the "kick" group
gets the drums chain and the "timp" group the perc chain. Mastering is linked as in music_r11 (every
loudness pass measures the full mix and applies its gain to both, the encoder's peak correction too),
so both files have the same length, LOOPSTART / LOOPLENGTH and gain, and base + w * (full - base)
crossfades without phasing.

Usage: python3 tools/concept/audio/music_r30.py [--full] [--out DIR]
"""
import sys
from functools import lru_cache
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import music_r08 as r08  # noqa: E402
import music_r11 as r11  # noqa: E402
from music import ns  # noqa: E402
from music_r02 import seam_ratio  # noqa: E402
from synth import SR, db, decode, lufs, undb, write_ogg  # noqa: E402

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "design" / "audio" / "music" / "concept"
CHOSEN = OUT / "homefront-full-r08-a.ogg"
# full-mix groups the base stem keeps whole, and the tagged groups with the chain they borrow
BASE_GROUPS = ("fx", "riff", "arp", "bass", "choir", "strings")
TAGGED = {"kick": "drums", "timp": "perc"}


class _Kick(np.ndarray):
    """A kick: an ndarray that remembers it belongs to the base stem."""


class _Timp(np.ndarray):
    """A timpani hit: an ndarray that remembers it belongs to the base stem."""


@lru_cache(None)
def _kick(*args, **kw):
    # cached like music.kick(), so every call hands back the same object as the original does
    return r08_kick(*args, **kw).view(_Kick)


def _timpani(*args, **kw):
    return r08_timpani(*args, **kw).view(_Timp)


class _StemSong(r08.LoopSong):
    def hit(self, group, sig, bar, step=0, gain=1.0, position=0.0):
        super().hit(group, sig, bar, step, gain, position)
        if isinstance(sig, _Kick):
            super().hit("kick", np.asarray(sig), bar, step, gain, position)
        elif isinstance(sig, _Timp):
            super().hit("timp", np.asarray(sig), bar, step, gain, position)


r08_kick, r08_timpani, r08_mixdown = r08.kick, r08.timpani, r08.mixdown
_calls = []


def _mixdown(s, spec, kicks, *args, **kw):
    """The full mix's mixdown(), kept so the base stem is mixed with the same spec and duck."""
    _calls.append((spec, kicks, args, kw))
    return r08_mixdown(s, spec, kicks, *args, **kw)


def render():
    """One render of the chosen arrangement; returns (song, full mix, base mix)."""
    saved = r08.kick, r08.timpani, r08.LoopSong, r08.mixdown
    r08.kick, r08.timpani, r08.LoopSong, r08.mixdown = _kick, _timpani, _StemSong, _mixdown
    try:
        s, full = r08.homefront_full()
    finally:
        r08.kick, r08.timpani, r08.LoopSong, r08.mixdown = saved
    spec, kicks, args, kw = _calls[-1]
    base_spec = {g: spec[g] for g in BASE_GROUPS}
    base_spec.update({g: spec[chain] for g, chain in TAGGED.items()})
    return s, full, r08_mixdown(s, base_spec, kicks, *args, **kw)


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
    tmp = out / "homefront-full-r30-a.ogg"
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
            raise SystemExit("the full mix no longer matches homefront-full-r08-a.ogg; "
                             "rerun with --full to write it next to the base stem")
    path = write_ogg(out / "homefront-base-r30-a.ogg", base, max_peak_db=None, tags=tags)
    dec = decode(path)
    print(f"{path.name}: {base.shape[1] / SR:.1f}s  loop {loop_start / SR:.3f}s + {loop_len / SR:.3f}s"
          f"  (samples {loop_start}+{loop_len})  seam {seam_ratio(dec, loop_start, loop_len):.2f}"
          f"  peak {db(np.abs(dec).max()):.1f} dBFS  base vs full {lufs(base)[0] - lufs(full)[0]:+.1f} LU"
          f"  full mix {'identical to r08' if same else 'written as r30'}", flush=True)


if __name__ == "__main__":
    main(sys.argv[1:])
