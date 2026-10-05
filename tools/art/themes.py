#!/usr/bin/env python3
"""Production music: the title, hangar and briefing themes (UI batch part U3, round 13) and the Act 1
boss and act-end music (M4 part G, round 25).

Outputs:
  assets/music/title-theme.ogg     "Terran Vanguard", full length (chosen: title-theme-full-r08-a)
  assets/music/hangar-theme.ogg    "Dry Dock", full length (chosen: hangar-theme-full-r08-a)
  assets/music/briefing-theme.ogg  "Situation Room" (chosen: briefing-theme-r08-a)
  assets/music/choir-descends.ogg  #18 "The Choir Descends", full length (chosen: choir-descends-full-r08-a)
  assets/music/boss-warning.ogg    #22 "Red Alert", played once (chosen: boss-warning-r08-a)
  assets/music/act-complete.ogg    #24 "Act Complete", played once (chosen: act-complete-r08-a)
  design/audio/music/concept/themes-final-r13-a.png      review sheet of round 13
  design/audio/music/concept/boss-music-final-r25-a.png  review sheet of round 25
  design/audio/music/concept/{choir-descends,boss-warning,act-complete}-final-r25-a.ogg
                                   the round-25 files as the game loads them (byte copies)
  design/audio/music/concept/boss-handoff-final-r25-a.ogg
                                   the boss cue as the game mixes it: track 22, track 18 from 4.8 s
  design/audio/music/concept/choir-descends-seam-final-r25-a.ogg
                                   track 18 across its loop seam, 8 s each side, as it loops

Each piece is rendered by the frozen concept generator that made the chosen file
(tools/concept/audio/music_r08.py: ``render_loop`` for the themes, ``render_sting`` for the
one-shot cues; same composition, seeds, mix EQ and master), which already used the final settings
of the production plan: OGG Vorbis q6, -14 LUFS integrated; a theme as intro + loop + 2-bar fade
tail with sample-exact ``LOOPSTART`` / ``LOOPLENGTH`` comments, a cue as one take with a short
fade. The encoded stream is then remuxed (packets copied, nothing re-encoded) with a ``SOURCE``
comment, which marks it as final: ``importPlaceholders`` (``vanguard.pipeline.PlaceholderSounds``)
keeps it. The two round-25 listening aids are mixes of the decoded finals (re-encoded at q6, gain
unchanged); the game never loads them.

``--check`` verifies the files in assets/music: SOURCE comment, nominal bitrate of q6 (192 kbit/s
at 44.1 kHz stereo), integrated loudness within 0.5 LU of -14 LUFS, true peak below -1 dBTP; for a
theme the loop comments and a click-free seam (the jump from the loop's last sample to its first no
larger than the typical sample-to-sample change just before it), for a cue no loop comments and a
silent end. For the boss warning it checks the hand-off the game makes (vanguard.game.audio.BossCue,
at Tracks.BOSS_WARNING_BARS_SECONDS read from the Java source): the hand-off is the downbeat after
three bars at 150 BPM to the sample, track 18 starts on its own downbeat at sample 0, the warning's
tail under it stays far below it, the summed 16-bit stream never clips, and the loudness step from
the warning's last bar into the boss track's first stays within HANDOFF_STEP. It also reports
whether the decoded audio is identical to the chosen concept file.

Usage: python3 tools/art/themes.py [title] [hangar] [briefing] [boss] [warning] [actcomplete]
                                                       render (default all), then check and review
       python3 tools/art/themes.py --check [keys]      check only (default all)
       python3 tools/art/themes.py --review [keys]     review sheets (and r25 aids) of the keys' rounds
Run time ~4 min for the three round-13 themes, ~2 min for the round-25 pieces.
"""
import re
import shutil
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
from synth import SR, db, decode, lufs, write_ogg, write_wav  # noqa: E402
from render import raster  # noqa: E402

SCRIPT = "themes.py"
MUSIC = artkit.ROOT / "assets" / "music"
CONCEPT = artkit.DESIGN / "audio" / "music" / "concept"
TRACKS_JAVA = artkit.ROOT / "game" / "src" / "main" / "java" / "vanguard" / "game" / "audio" / "Tracks.java"
TARGET_LUFS = -14.0
LOUDNESS_TOLERANCE = 0.5
MAX_TRUE_PEAK = -1.0
Q6_NOMINAL = 192000
MAX_SEAM = 1.5
SILENT_END = -60.0  # dBFS peak of a cue's last 10 ms
HANDOFF_STEP = 4.0  # LU between the warning's last sounding bar and the boss track's first bar
HANDOFF_TAIL = -30.0  # dB of the warning's tail under the boss track, relative to the track there
WARNING_BPM, WARNING_BARS = 150, 3

# round: (sheet file, sheet title, sheet subtitle, SOURCE comment)
ROUNDS = {
    13: ("themes-final-r13-a.png", "THEMES (FINAL)", "PRODUCTION ART, UI BATCH - R13",
         artkit.source_note(SCRIPT, "UI batch")),
    25: ("boss-music-final-r25-a.png", "BOSS AND ACT END MUSIC (FINAL)", "PRODUCTION ART, M4 PART G - R25",
         artkit.source_note(SCRIPT, "M4 part G")),
}
# key: (asset name, music_r08 key, title shown on the sheet, round); loops first, then one-shots
THEMES = {
    "title": ("title-theme", "title", "TERRAN VANGUARD - TITLE THEME", 13),
    "hangar": ("hangar-theme", "hangar", "DRY DOCK - HANGAR THEME", 13),
    "briefing": ("briefing-theme", "briefing", "SITUATION ROOM - BRIEFING THEME", 13),
    "boss": ("choir-descends", "choir", "THE CHOIR DESCENDS - TRACK 18, VRELL BOSS THEME", 25),
}
CUES = {
    "warning": ("boss-warning", "warning", "RED ALERT - TRACK 22, BOSS WARNING", 25),
    "actcomplete": ("act-complete", "actcomplete", "ACT COMPLETE - TRACK 24, FANFARE", 25),
}
PIECES = {**THEMES, **CUES}
HANDOFF = CONCEPT / "boss-handoff-final-r25-a.ogg"
SEAM = CONCEPT / "choir-descends-seam-final-r25-a.ogg"
HANDOFF_BARS_AFTER = 8  # bars of the boss track in the hand-off aid
SEAM_SECONDS = 8.0


def concept_file(key):
    table = music_r08.LOOPS if key in THEMES else music_r08.STINGS
    return CONCEPT / f"{table[PIECES[key][1]][0]}.ogg"


def asset_file(key):
    return MUSIC / f"{PIECES[key][0]}.ogg"


def review_copy(key):
    """The round-25 review file: the final asset as the game loads it, byte for byte."""
    return CONCEPT / f"{PIECES[key][0]}-final-r{PIECES[key][3]}-a.ogg"


def source(key):
    return ROUNDS[PIECES[key][3]][3]


def render(key):
    """Render with the chosen generator into a temporary file, then remux with the SOURCE comment."""
    with tempfile.TemporaryDirectory() as tmp:
        if key in THEMES:
            music_r08.render_loop(PIECES[key][1], Path(tmp))
        else:
            music_r08.render_sting(PIECES[key][1], Path(tmp))
        rendered = Path(tmp) / concept_file(key).name
        MUSIC.mkdir(parents=True, exist_ok=True)
        subprocess.run(
            ["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(rendered), "-c:a", "copy",
             "-metadata:s:a:0", f"SOURCE={source(key)}", "-fflags", "+bitexact", str(asset_file(key))],
            check=True)
    if PIECES[key][3] == 25:
        shutil.copyfile(asset_file(key), review_copy(key))


def tags(path):
    out = subprocess.run(
        ["ffprobe", "-v", "error", "-select_streams", "a:0", "-show_entries",
         "stream=bit_rate,sample_rate,channels:stream_tags", "-of", "default=nw=1", str(path)],
        capture_output=True, text=True, check=True).stdout
    return dict(line.removeprefix("TAG:").split("=", 1) for line in out.splitlines() if "=" in line)


def measure(path):
    """The numbers the check and the sheet show for one file (loop points None for a one-shot)."""
    info = tags(path)
    audio = decode(path)
    loudness, true_peak = lufs(audio)
    m = dict(audio=audio, start=None, length=None, seam=None, lufs=loudness, tp=true_peak,
             bitrate=int(info["bit_rate"]), source=info.get("SOURCE", ""), size=path.stat().st_size,
             seconds=audio.shape[1] / SR, peak=db(np.abs(audio).max()),
             end=db(np.abs(audio[:, -SR // 100:]).max() + 1e-12))
    if "LOOPSTART" in info:
        m["start"], m["length"] = int(info["LOOPSTART"]), int(info["LOOPLENGTH"])
        m["seam"] = seam_ratio(audio, m["start"], m["length"])
    return m


def segment_lufs(audio):
    """Integrated loudness (LUFS) of a short stretch, by ffmpeg's ebur128 (400 ms blocks)."""
    with tempfile.TemporaryDirectory() as tmp:
        wav = Path(tmp) / "s.wav"
        write_wav(wav, audio)
        err = subprocess.run(["ffmpeg", "-hide_banner", "-nostats", "-i", str(wav), "-af", "ebur128",
                              "-f", "null", "-"], capture_output=True, text=True, check=True).stderr
    return float(re.findall(r"I:\s+(-?[\d.]+) LUFS", err)[-1])


def handoff_seconds():
    """Tracks.BOSS_WARNING_BARS_SECONDS, where the game starts track 18 under track 22."""
    found = re.search(r"BOSS_WARNING_BARS_SECONDS\s*=\s*([\d.]+)", TRACKS_JAVA.read_text())
    return float(found.group(1))


def pcm16(audio):
    """The 16-bit samples the game's decoder hands the mixer."""
    return np.clip(np.round(audio * 32768), -32768, 32767).astype(np.int64)


def boss_cue(warning, track, at, frames):
    """BossCue's output: the warning once, the looping track added from frame ``at``, clamped to 16 bit;
    returns (mixed float audio, number of clamped samples)."""
    out = np.zeros((2, frames), dtype=np.int64)
    w = pcm16(warning[:, :frames])
    out[:, :w.shape[1]] += w
    out[:, at:] += pcm16(track[:, :frames - at])
    clamped = int(((out > 32767) | (out < -32768)).sum())
    return np.clip(out, -32768, 32767) / 32768.0, clamped


def handoff(warning, track):
    """The numbers of the boss cue's hand-off (see the module doc)."""
    seconds = handoff_seconds()
    at = int(round(seconds * SR))
    bar = int(round(16 * 15 * SR / WARNING_BPM))
    bars_exact = at == WARNING_BARS * bar and abs(seconds * SR - at) < 1e-6
    onset = int(np.argmax(np.abs(track).max(axis=0) > 10 ** (SILENT_END / 20)))
    beat = bar // 4
    mixed, clamped = boss_cue(warning, track, at, warning.shape[1])
    under = warning[:, at:]
    rms = lambda x: 20 * np.log10(np.sqrt((x ** 2).mean()) + 1e-12)  # noqa: E731
    return dict(
        at=at, seconds=seconds, bars_exact=bars_exact, onset=onset, clamped=clamped,
        last_bar=segment_lufs(warning[:, at - bar:at - beat]),  # the last bar up to its silent beat
        silent_beat=rms(warning[:, at - beat:at]) - rms(warning[:, at - bar:at - beat]),
        first_bar=segment_lufs(track[:, :bar]),
        tail=rms(under) - rms(track[:, :under.shape[1]]),
        peak=db(np.abs(mixed[:, at:]).max()))


def check(keys):
    failed = False
    for key in keys:
        m = measure(asset_file(key))
        chosen = decode(concept_file(key))
        same = chosen.shape == m["audio"].shape and np.array_equal(chosen, m["audio"])
        common = [
            (m["source"] != source(key), "no SOURCE comment"),
            (m["bitrate"] != Q6_NOMINAL, f"nominal bitrate {m['bitrate']}, not q6"),
            (abs(m["lufs"] - TARGET_LUFS) > LOUDNESS_TOLERANCE, f"{m['lufs']:.1f} LUFS"),
            (m["tp"] > MAX_TRUE_PEAK, f"true peak {m['tp']:.1f} dBTP"),
        ]
        if key in THEMES:
            looped = m["start"] is not None
            problems = common + [
                (not looped, "no loop comments"),
                (looped and m["start"] + m["length"] > m["audio"].shape[1], "loop runs past the end"),
                (looped and m["seam"] > MAX_SEAM, f"seam {m['seam'] or 0:.2f}"),
            ]
            shape = (f"loop {m['start']} + {m['length']} samples ({m['start'] / SR:.3f} s + "
                     f"{m['length'] / SR:.3f} s), seam {m['seam']:.2f}" if looped else "no loop")
        else:
            problems = common + [
                (m["start"] is not None, "loop comments on a one-shot cue"),
                (m["end"] > SILENT_END, f"end at {m['end']:.0f} dBFS, not silent"),
            ]
            shape = f"one-shot, end {m['end']:.0f} dBFS"
        if PIECES[key][3] == 25:
            problems.append((not review_copy(key).exists()
                             or review_copy(key).read_bytes() != asset_file(key).read_bytes(),
                             f"{review_copy(key).name} missing or not the asset"))
        problems = [text for bad, text in problems if bad]
        failed |= bool(problems)
        print(f"{asset_file(key).name}: {m['seconds']:.2f} s, {shape}, {m['lufs']:.1f} LUFS, "
              f"{m['tp']:.1f} dBTP, {m['size'] / 1e6:.2f} MB, "
              f"{'audio identical to' if same else 'audio differs from'} {concept_file(key).name}"
              + (f"  FAILED: {', '.join(problems)}" if problems else ""))
        if key == "warning" and asset_file("boss").exists():
            h = handoff(m["audio"], decode(asset_file("boss")))
            step = h["first_bar"] - h["last_bar"]
            problems = [text for bad, text in (
                (not h["bars_exact"], f"hand-off {h['seconds']} s is not {WARNING_BARS} bars at {WARNING_BPM} BPM"),
                (h["at"] >= m["audio"].shape[1], "the warning ends before the hand-off"),
                (h["onset"] > SR // 100, f"track 18 starts {h['onset']} samples late"),
                (h["clamped"] > 0, f"{h['clamped']} clamped samples"),
                (h["tail"] > HANDOFF_TAIL, f"tail {h['tail']:.0f} dB under the track"),
                (abs(step) > HANDOFF_STEP, f"loudness step {step:+.1f} LU"),
            ) if bad]
            failed |= bool(problems)
            print(f"  hand-off at {h['seconds']} s = sample {h['at']} ({WARNING_BARS} bars at {WARNING_BPM} BPM: "
                  f"{'exact' if h['bars_exact'] else 'NOT exact'}), track 18 onset at sample {h['onset']}, "
                  f"warning's last bar {h['last_bar']:.1f} LUFS -> boss track's first bar {h['first_bar']:.1f} "
                  f"LUFS ({step:+.1f} LU), silent beat {h['silent_beat']:.0f} dB under the bar, tail under "
                  f"the track {h['tail']:.0f} dB, summed peak after it {h['peak']:.1f} dBFS, "
                  f"{h['clamped']} clamped samples" + (f"  FAILED: {', '.join(problems)}" if problems else ""))
    if failed:
        raise SystemExit("themes check failed")


# --------------------------------------------------------------------------- listening aids (round 25)

def fade_ends(audio, seconds=0.5):
    n = int(seconds * SR)
    w = np.linspace(0, 1, n)
    audio = audio.copy()
    audio[:, :n] *= w
    audio[:, -n:] *= w[::-1]
    return audio


def fade_out(audio, seconds):
    n = int(seconds * SR)
    audio = audio.copy()
    audio[:, -n:] *= np.linspace(1, 0, n)
    return audio


def listening_aids():
    """The boss cue as the game mixes it, and track 18 across its loop seam as the looping stream plays it."""
    warning, boss = decode(asset_file("warning")), measure(asset_file("boss"))
    at = int(round(handoff_seconds() * SR))
    bar = int(round(16 * 15 * SR / WARNING_BPM))
    cue, _ = boss_cue(warning, boss["audio"], at, at + HANDOFF_BARS_AFTER * bar)
    write_ogg(HANDOFF, fade_out(cue, 1.0), max_peak_db=None)
    end, n = boss["start"] + boss["length"], int(SEAM_SECONDS * SR)
    track = boss["audio"]
    seam = np.concatenate([track[:, end - n:end], track[:, boss["start"]:boss["start"] + n]], axis=1)
    write_ogg(SEAM, fade_ends(seam), max_peak_db=None)
    for path in (HANDOFF, SEAM):
        print(f"aid: {path.relative_to(artkit.ROOT)}")


# --------------------------------------------------------------------------- review sheets

WAVE_W, WAVE_H = 1240, 90
INTRO, LOOP, TAIL = (120, 130, 150), (80, 200, 255), (90, 100, 120)
ONCE, BOSS = (170, 190, 120), (200, 120, 220)


def waveform(img, x0, y0, audio, colour_at, marks):
    """Min/max envelope of the mono mix per column, coloured by ``colour_at(sample)``; amber marks."""
    d = ImageDraw.Draw(img)
    mono = audio.mean(axis=0)
    n = len(mono)
    edges = np.linspace(0, n, WAVE_W + 1).astype(int)
    mid = y0 + WAVE_H // 2
    d.rectangle([x0, y0, x0 + WAVE_W - 1, y0 + WAVE_H - 1], fill=(10, 12, 20, 255))
    for col in range(WAVE_W):
        part = mono[edges[col]:edges[col + 1]]
        d.line([x0 + col, mid - int(part.max() * WAVE_H / 2), x0 + col, mid - int(part.min() * WAVE_H / 2)],
               fill=colour_at(edges[col]))
    for at in marks:
        x = x0 + int(at / n * WAVE_W)
        d.line([x, y0 - 4, x, y0 + WAVE_H + 3], fill=raster.ACCENT)


def loop_colours(m):
    return lambda s: INTRO if s < m["start"] else LOOP if s < m["start"] + m["length"] else TAIL


def rows(round_no):
    """(heading, numbers line, audio, colour function, marks) per sheet row."""
    out = []
    for key, (asset, _, title, r) in PIECES.items():
        if r != round_no:
            continue
        m = measure(asset_file(key))
        head = f"{title}   ASSETS/MUSIC/{asset.upper()}.OGG"
        tail = (f"{m['lufs']:.1f} LUFS   TRUE PEAK {m['tp']:.1f} DBTP   ")
        if key in THEMES:
            line = (f"{m['seconds']:.1f} S   INTRO {m['start'] / SR:.2f} S + LOOP {m['length'] / SR:.2f} S "
                    f"(SAMPLES {m['start']} + {m['length']})   {tail}SEAM {m['seam']:.2f}   "
                    f"Q6 ({m['bitrate'] // 1000} KBIT/S NOMINAL)   {m['size'] / 1e6:.2f} MB")
            out.append((head, line, m["audio"], loop_colours(m), (m["start"], m["start"] + m["length"])))
        else:
            marks = ()
            extra = ""
            if key == "warning":
                at = int(round(handoff_seconds() * SR))
                marks = (at,)
                extra = f"TRACK 18 FROM {at / SR:.2f} S (SAMPLE {at})   "
            line = (f"{m['seconds']:.2f} S, PLAYED ONCE   {extra}{tail}END {m['end']:.0f} DBFS   "
                    f"Q6 ({m['bitrate'] // 1000} KBIT/S NOMINAL)   {m['size'] / 1e6:.2f} MB")
            out.append((head, line, m["audio"], lambda s: ONCE, marks))
    if round_no == 25:
        warning, boss = decode(asset_file("warning")), decode(asset_file("boss"))
        h = handoff(warning, boss)
        bar = int(round(16 * 15 * SR / WARNING_BPM))
        cue, _ = boss_cue(warning, boss, h["at"], h["at"] + HANDOFF_BARS_AFTER * bar)
        at = h["at"]
        out.append((
            "THE HAND-OFF AS THE GAME MIXES IT (BOSSCUE)   CONCEPT/BOSS-HANDOFF-FINAL-R25-A.OGG",
            f"TRACK 22, THEN TRACK 18 FROM SAMPLE {at} ({WARNING_BARS} BARS AT {WARNING_BPM} BPM)   "
            f"LAST BAR {h['last_bar']:.1f} LUFS -> FIRST BAR {h['first_bar']:.1f} LUFS   "
            f"TAIL UNDER IT {h['tail']:.0f} DB   PEAK AFTER {h['peak']:.1f} DBFS   {h['clamped']} CLAMPED",
            cue, lambda s: ONCE if s < at else BOSS, (at,)))
    return out


def review(round_no):
    file, title, subtitle, _ = ROUNDS[round_no]
    block = 40 + WAVE_H + 26
    sheet_rows = rows(round_no)
    img = raster.sheet(WAVE_W + 40, 40 + block * len(sheet_rows), title, subtitle)
    y = 40
    for head, line, audio, colour_at, marks in sheet_rows:
        raster.draw_text(img, 20, y, head, raster.LABEL)
        raster.draw_text(img, 20, y + 12, line, raster.LABEL_DIM)
        waveform(img, 20, y + 30, audio, colour_at, marks)
        y += block
    legend = ("GREY: INTRO, PLAYED ONCE   BLUE: LOOP   DARK: FADE TAIL (ONLY WITHOUT LOOP POINTS)   "
              "AMBER: LOOPSTART / LOOP END")
    if round_no == 25:
        legend += "   GREEN: ONE-SHOT CUE   PURPLE: TRACK 18 UNDER THE CUE   AMBER ON A CUE: THE HAND-OFF"
    raster.draw_text(img, 20, y - 14, legend, raster.LABEL_DIM)
    path = CONCEPT / file
    img.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(artkit.ROOT)}")
    if round_no == 25:
        listening_aids()


def pick(args):
    keys = [a for a in args if not a.startswith("--")] or list(PIECES)
    unknown = set(keys) - set(PIECES)
    if unknown:
        raise SystemExit(f"unknown piece(s): {', '.join(sorted(unknown))}")
    return keys


def main(args):
    keys = pick(args)
    rounds = sorted({PIECES[k][3] for k in keys})
    if "--check" in args:
        check(keys)
        return
    if "--review" in args:
        for r in rounds:
            review(r)
        return
    music_r08.validate_patterns()
    for key in keys:
        render(key)
    check(keys)
    for r in rounds:
        review(r)


if __name__ == "__main__":
    main(sys.argv[1:])
