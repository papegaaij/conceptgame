#!/usr/bin/env python3
"""Production art, UI batch part U3: the title, hangar and briefing themes.

Outputs:
  assets/music/title-theme.ogg     "Terran Vanguard", full length (chosen: title-theme-full-r08-a)
  assets/music/hangar-theme.ogg    "Dry Dock", full length (chosen: hangar-theme-full-r08-a)
  assets/music/briefing-theme.ogg  "Situation Room" (chosen: briefing-theme-r08-a)
  design/audio/music/concept/themes-final-r13-a.png   review sheet (waveforms, loop, loudness)

Each theme is rendered by the frozen concept generator that made the chosen file
(tools/concept/audio/music_r08.py, ``render_loop``: same composition, seeds, mix EQ and master),
at the final settings of the production plan: OGG Vorbis q6, -14 LUFS integrated, intro + loop +
2-bar fade tail with sample-exact ``LOOPSTART`` / ``LOOPLENGTH`` comments. The encoded stream is
then remuxed (packets copied, nothing re-encoded) with a ``SOURCE`` comment, which marks it as
final: ``importPlaceholders`` (``vanguard.pipeline.PlaceholderSounds``) keeps it.

``--check`` verifies the files in assets/music: SOURCE and loop comments, nominal bitrate of q6
(192 kbit/s at 44.1 kHz stereo), integrated loudness within 0.5 LU of -14 LUFS, true peak below
-1 dBTP, and a click-free seam (the jump from the loop's last sample to its first no larger than
the typical sample-to-sample change just before it). It also reports whether the decoded audio
is identical to the chosen concept file.

Usage: python3 tools/art/themes.py [title] [hangar] [briefing]   render (default all), then check
       python3 tools/art/themes.py --check                        check only
       python3 tools/art/themes.py --review                       review sheet only
Run time ~4 min (the three renders).
"""
import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np
from PIL import ImageDraw

import artkit

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "concept" / "audio"))
import music_r08  # noqa: E402
from music_r02 import seam_ratio  # noqa: E402
from synth import SR, db, decode, lufs  # noqa: E402
from render import raster  # noqa: E402

SCRIPT = "themes.py"
SOURCE = artkit.source_note(SCRIPT, "UI batch")
MUSIC = artkit.ROOT / "assets" / "music"
CONCEPT = artkit.DESIGN / "audio" / "music" / "concept"
REVIEW = CONCEPT / "themes-final-r13-a.png"
TARGET_LUFS = -14.0
LOUDNESS_TOLERANCE = 0.5
MAX_TRUE_PEAK = -1.0
Q6_NOMINAL = 192000
MAX_SEAM = 1.5

# key: (asset name, music_r08 loop key, title shown on the sheet)
THEMES = {
    "title": ("title-theme", "title", "TERRAN VANGUARD - TITLE THEME"),
    "hangar": ("hangar-theme", "hangar", "DRY DOCK - HANGAR THEME"),
    "briefing": ("briefing-theme", "briefing", "SITUATION ROOM - BRIEFING THEME"),
}


def concept_file(key):
    return CONCEPT / f"{music_r08.LOOPS[THEMES[key][1]][0]}.ogg"


def asset_file(key):
    return MUSIC / f"{THEMES[key][0]}.ogg"


def render(key):
    """Render with the chosen generator into a temporary file, then remux with the SOURCE comment."""
    with tempfile.TemporaryDirectory() as tmp:
        music_r08.render_loop(THEMES[key][1], Path(tmp))
        rendered = Path(tmp) / concept_file(key).name
        MUSIC.mkdir(parents=True, exist_ok=True)
        subprocess.run(
            ["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(rendered), "-c:a", "copy",
             "-metadata:s:a:0", f"SOURCE={SOURCE}", "-fflags", "+bitexact", str(asset_file(key))],
            check=True)


def tags(path):
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-select_streams", "a:0", "-show_entries",
         "stream=bit_rate,sample_rate,channels:stream_tags", "-of", "default=nw=1", str(path)],
        capture_output=True, text=True, check=True).stdout
    return dict(line.removeprefix("TAG:").split("=", 1) for line in out.splitlines() if "=" in line)


def measure(path):
    """The numbers the check and the sheet show for one file."""
    info = tags(path)
    audio = decode(path)
    loop_start, loop_len = int(info["LOOPSTART"]), int(info["LOOPLENGTH"])
    loudness, true_peak = lufs(audio)
    return dict(audio=audio, start=loop_start, length=loop_len, lufs=loudness, tp=true_peak,
                seam=seam_ratio(audio, loop_start, loop_len), bitrate=int(info["bit_rate"]),
                source=info.get("SOURCE", ""), size=path.stat().st_size, seconds=audio.shape[1] / SR,
                peak=db(np.abs(audio).max()))


def check(keys):
    failed = False
    for key in keys:
        m = measure(asset_file(key))
        chosen = decode(concept_file(key))
        same = chosen.shape == m["audio"].shape and np.array_equal(chosen, m["audio"])
        problems = [text for bad, text in (
            (m["source"] != SOURCE, "no SOURCE comment"),
            (m["start"] + m["length"] > m["audio"].shape[1], "loop runs past the end"),
            (m["bitrate"] != Q6_NOMINAL, f"nominal bitrate {m['bitrate']}, not q6"),
            (abs(m["lufs"] - TARGET_LUFS) > LOUDNESS_TOLERANCE, f"{m['lufs']:.1f} LUFS"),
            (m["tp"] > MAX_TRUE_PEAK, f"true peak {m['tp']:.1f} dBTP"),
            (m["seam"] > MAX_SEAM, f"seam {m['seam']:.2f}"),
        ) if bad]
        failed |= bool(problems)
        print(f"{asset_file(key).name}: {m['seconds']:.1f} s, loop {m['start']} + {m['length']} samples "
              f"({m['start'] / SR:.3f} s + {m['length'] / SR:.3f} s), {m['lufs']:.1f} LUFS, "
              f"{m['tp']:.1f} dBTP, seam {m['seam']:.2f}, {m['size'] / 1e6:.2f} MB, "
              f"{'audio identical to' if same else 'audio differs from'} {concept_file(key).name}"
              + (f"  FAILED: {', '.join(problems)}" if problems else ""))
    if failed:
        raise SystemExit("themes check failed")


# --------------------------------------------------------------------------- review sheet

WAVE_W, WAVE_H = 1240, 90
INTRO, LOOP, TAIL = (120, 130, 150), (80, 200, 255), (90, 100, 120)


def waveform(img, x0, y0, m):
    """Min/max envelope of the mono mix per column; the intro, loop and tail in their colours."""
    d = ImageDraw.Draw(img)
    mono = m["audio"].mean(axis=0)
    n = len(mono)
    edges = np.linspace(0, n, WAVE_W + 1).astype(int)
    mid = y0 + WAVE_H // 2
    d.rectangle([x0, y0, x0 + WAVE_W - 1, y0 + WAVE_H - 1], fill=(10, 12, 20, 255))
    for col in range(WAVE_W):
        part = mono[edges[col]:edges[col + 1]]
        start = edges[col]
        colour = INTRO if start < m["start"] else LOOP if start < m["start"] + m["length"] else TAIL
        d.line([x0 + col, mid - int(part.max() * WAVE_H / 2), x0 + col, mid - int(part.min() * WAVE_H / 2)],
               fill=colour)
    for at in (m["start"], m["start"] + m["length"]):
        x = x0 + int(at / n * WAVE_W)
        d.line([x, y0 - 4, x, y0 + WAVE_H + 3], fill=raster.ACCENT)


def review():
    block = 40 + WAVE_H + 26
    img = raster.sheet(WAVE_W + 40, 40 + block * len(THEMES), "THEMES (FINAL)",
                       "PRODUCTION ART, UI BATCH - R13")
    y = 40
    for key, (asset, _, title) in THEMES.items():
        m = measure(asset_file(key))
        raster.draw_text(img, 20, y, f"{title}   ASSETS/MUSIC/{asset.upper()}.OGG", raster.LABEL)
        raster.draw_text(
            img, 20, y + 12,
            f"{m['seconds']:.1f} S   INTRO {m['start'] / SR:.2f} S + LOOP {m['length'] / SR:.2f} S "
            f"(SAMPLES {m['start']} + {m['length']})   {m['lufs']:.1f} LUFS   TRUE PEAK {m['tp']:.1f} DBTP   "
            f"SEAM {m['seam']:.2f}   Q6 ({m['bitrate'] // 1000} KBIT/S NOMINAL)   {m['size'] / 1e6:.2f} MB",
            raster.LABEL_DIM)
        waveform(img, 20, y + 30, m)
        y += block
    raster.draw_text(img, 20, y - 14, "GREY: INTRO, PLAYED ONCE   BLUE: LOOP   DARK: FADE TAIL (ONLY WITHOUT LOOP "
                     "POINTS)   AMBER: LOOPSTART / LOOP END", raster.LABEL_DIM)
    img.convert("RGB").save(REVIEW, optimize=True)
    print(f"review: {REVIEW.relative_to(artkit.ROOT)}")


def main(args):
    if "--check" in args:
        check(list(THEMES))
        return
    if "--review" in args:
        review()
        return
    keys = args or list(THEMES)
    unknown = set(keys) - set(THEMES)
    if unknown:
        raise SystemExit(f"unknown theme(s): {', '.join(sorted(unknown))}")
    music_r08.validate_patterns()
    for key in keys:
        render(key)
    check(keys)
    review()


if __name__ == "__main__":
    main(sys.argv[1:])
