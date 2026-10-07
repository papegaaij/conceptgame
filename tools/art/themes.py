#!/usr/bin/env python3
"""Production music: the title, hangar and briefing themes (UI batch part U3, round 13), the Act 1
boss and act-end music (M4 part G, round 25), the Act 1 level themes with their base stems, the
three jingles and the mini-boss sting (M4 part H, round 26) and the first two Act 2 level themes with
their base stems (M5 part B, round 30; M5 part C, round 31).

Outputs:
  assets/music/title-theme.ogg     "Terran Vanguard", full length (chosen: title-theme-full-r08-a)
  assets/music/hangar-theme.ogg    "Dry Dock", full length (chosen: hangar-theme-full-r08-a)
  assets/music/briefing-theme.ogg  "Situation Room" (chosen: briefing-theme-r08-a)
  assets/music/choir-descends.ogg  #18 "The Choir Descends", full length (chosen: choir-descends-full-r08-a)
  assets/music/boss-warning.ogg    #22 "Red Alert", played once (chosen: boss-warning-r08-a)
  assets/music/act-complete.ogg    #24 "Act Complete", played once (chosen: act-complete-r08-a)
  assets/music/afterburner.ogg     #4 "Afterburner", full length (chosen: afterburner-full-r08-a)
  assets/music/afterburner-base.ogg  its base stem (chosen: afterburner-base-r15-a, music_r15.py)
  assets/music/coalition-rising.ogg  #5 "Coalition Rising", full length (chosen: coalition-rising-full-r08-a)
  assets/music/coalition-rising-base.ogg  its base stem (chosen: coalition-rising-base-r11-a, music_r11.py)
  assets/music/miniboss-sting.ogg  #21 "Contact Heavy", played once (chosen: miniboss-sting-r08-a)
  assets/music/mission-complete.ogg  #23, played once (chosen: mission-complete-r08-a)
  assets/music/mission-failed.ogg  #25, played once (chosen: mission-failed-r08-a)
  assets/music/game-over.ogg       #26, played once (chosen: game-over-r08-a)
  assets/music/homefront.ogg       #6 "Homefront", full length (chosen: homefront-full-r08-a)
  assets/music/homefront-base.ogg  its base stem (music_r30.py; straight to production, no concept file)
  assets/music/firestorm.ogg       #7 "Firestorm", full length (chosen: act2-b-theme-r08-a)
  assets/music/firestorm-base.ogg  its base stem (music_r31.py; straight to production, no concept file)
  design/audio/music/concept/themes-final-r13-a.png      review sheet of round 13
  design/audio/music/concept/boss-music-final-r25-a.png  review sheet of round 25
  design/audio/music/concept/{choir-descends,boss-warning,act-complete}-final-r25-a.ogg
                                   the round-25 files as the game loads them (byte copies)
  design/audio/music/concept/boss-handoff-final-r25-a.ogg
                                   the boss cue as the game mixes it: track 22, track 18 from 4.8 s
  design/audio/music/concept/choir-descends-seam-final-r25-a.ogg
                                   track 18 across its loop seam, 8 s each side, as it loops
  design/audio/music/concept/music-final-r26-a.png       review sheet of round 26
  design/audio/music/concept/<asset>-final-r26-a.ogg     the eight round-26 files (byte copies)
  design/audio/music/concept/<theme or stem>-seam-final-r26-a.ogg
                                   tracks 4 and 5 and their base stems across the loop seam
  design/audio/music/concept/music-final-r30-a.png       review sheet of round 30
  design/audio/music/concept/homefront{,-base}-final-r30-a.ogg
                                   the two round-30 files (byte copies)
  design/audio/music/concept/homefront{,-base}-seam-final-r30-a.ogg
                                   track 6 and its base stem across the loop seam
  design/audio/music/concept/music-final-r31-a.png       review sheet of round 31
  design/audio/music/concept/firestorm{,-base}-final-r31-a.ogg
                                   the two round-31 files (byte copies)
  design/audio/music/concept/firestorm{,-base}-seam-final-r31-a.ogg
                                   track 7 and its base stem across the loop seam

Each piece is rendered by the frozen concept generator that made the chosen file
(tools/concept/audio/music_r08.py: ``render_loop`` for the themes, ``render_sting`` for the
one-shot cues; music_r15.py / music_r11.py / music_r30.py / music_r31.py for the base stems, which fail unless
their full mix is the chosen full mix byte for byte; same composition, seeds, mix EQ and master), which already
used the final settings of the production plan: OGG Vorbis q6, -14 LUFS integrated; a theme as
intro + loop + 2-bar fade tail with sample-exact ``LOOPSTART`` / ``LOOPLENGTH`` comments, a cue as
one take with a short fade. The encoded stream is then remuxed (packets copied, nothing re-encoded) with a ``SOURCE``
comment, which marks it as final: ``importPlaceholders`` (``vanguard.pipeline.PlaceholderSounds``)
keeps it. One exception (TRUE_PEAK_FIX, round 26): the chosen "Afterburner" and its base stem
failed the true-peak limit (-0.5 and -0.8 dBTP: inter-sample overs at kick transients made by the
Vorbis encoder; a plain gain cut would have taken the full mix below -14.5 LUFS), so the pair is
rendered as its stem generator renders it, proven identical to both chosen files, and then gets one
gain envelope for both (the crossfade stays exact) that dips only around those transients, aimed at
TP_FIX_AIM and re-encoded until no 4x peak is above TP_FIX_LIMIT. Round 31 does the same for the
chosen "Firestorm" and its new base stem (-0.2 and -0.1 dBTP; a plain cut would again have taken the full
mix under -14.5 LUFS); that stem has no concept file, so only the full mix is compared there (the stem
comes from the same pass). The listening aids are mixes
of the decoded finals (re-encoded at q6, gain unchanged); the game never loads them.

``--check`` verifies the files in assets/music: SOURCE comment, nominal bitrate of q6 (192 kbit/s
at 44.1 kHz stereo), integrated loudness within 0.5 LU of -14 LUFS, true peak below -1 dBTP; for a
theme the loop comments and a click-free seam (the jump from the loop's last sample to its first no
larger than the typical sample-to-sample change just before it), for a cue no loop comments and a
silent end. A base stem is mastered with its full mix's gain (the crossfade needs it), so instead of
-14 LUFS it must be quieter than the full mix, and its length and loop comments must equal the full
mix's (sample-aligned stems). For the boss warning it checks the hand-off the game makes (vanguard.game.audio.BossCue,
at Tracks.BOSS_WARNING_BARS_SECONDS read from the Java source): the hand-off is the downbeat after
three bars at 150 BPM to the sample, track 18 starts on its own downbeat at sample 0, the warning's
tail under it stays far below it, the summed 16-bit stream never clips, and the loudness step from
the warning's last bar into the boss track's first stays within HANDOFF_STEP. It also reports
whether the decoded audio is identical to the chosen concept file (if not, how far it differs); a
piece rendered straight to production (the "Homefront" and "Firestorm" base stems, rounds 30 and 31:
each generator proves its full mix is the chosen one) has no concept file to compare with.

Usage: python3 tools/art/themes.py [title] [hangar] [briefing] [boss] [warning] [actcomplete]
                                   [afterburner] [afterburner-base] [coalition] [coalition-base]
                                   [miniboss] [complete] [failed] [gameover] [homefront]
                                   [homefront-base] [firestorm] [firestorm-base]
                                                       render (default all), then check and review
       python3 tools/art/themes.py --check [keys]      check only (default all)
       python3 tools/art/themes.py --review [keys]     review sheets (and r25/r26 aids) of the keys' rounds
Run time ~4 min for the three round-13 themes, ~2 min for the round-25 pieces, ~5 min for round 26,
~6 min for round 30, ~6 min for round 31.
"""
import importlib
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
import music_r11  # noqa: E402
from music import ns  # noqa: E402
from music_r02 import seam_ratio  # noqa: E402
from synth import SR, db, decode, lufs, undb, write_ogg, write_wav  # noqa: E402
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
    26: ("music-final-r26-a.png", "LEVEL THEMES, STEMS, JINGLES AND STING (FINAL)",
         "PRODUCTION ART, M4 PART H - R26", artkit.source_note(SCRIPT, "M4 part H")),
    30: ("music-final-r30-a.png", "HOMEFRONT AND ITS BASE STEM (FINAL)", "PRODUCTION ART, M5 PART B - R30",
         artkit.source_note(SCRIPT, "M5 part B")),
    31: ("music-final-r31-a.png", "FIRESTORM AND ITS BASE STEM (FINAL)", "PRODUCTION ART, M5 PART C - R31",
         artkit.source_note(SCRIPT, "M5 part C")),
}
# key: (asset name, music_r08 key, title shown on the sheet, round); loops first, then one-shots
THEMES = {
    "title": ("title-theme", "title", "TERRAN VANGUARD - TITLE THEME", 13),
    "hangar": ("hangar-theme", "hangar", "DRY DOCK - HANGAR THEME", 13),
    "briefing": ("briefing-theme", "briefing", "SITUATION ROOM - BRIEFING THEME", 13),
    "boss": ("choir-descends", "choir", "THE CHOIR DESCENDS - TRACK 18, VRELL BOSS THEME", 25),
    "afterburner": ("afterburner", "afterburner", "AFTERBURNER - TRACK 4, ACT 1 A", 26),
    "coalition": ("coalition-rising", "coalition", "COALITION RISING - TRACK 5, ACT 1 B", 26),
    "homefront": ("homefront", "homefront", "HOMEFRONT - TRACK 6, ACT 2 A", 30),
    "firestorm": ("firestorm", "act2b", "FIRESTORM - TRACK 7, ACT 2 B", 31),
}
# key: (asset name, key of its full mix in THEMES, title, round, stem generator, chosen file)
STEMS = {
    "afterburner-base": ("afterburner-base", "afterburner", "AFTERBURNER - TRACK 4, BASE STEM", 26,
                         "music_r15", "afterburner-base-r15-a"),
    "coalition-base": ("coalition-rising-base", "coalition", "COALITION RISING - TRACK 5, BASE STEM", 26,
                       "music_r11", "coalition-rising-base-r11-a"),
    # no concept round: music_r30 writes homefront-base-r30-a.ogg only into the temporary render directory
    "homefront-base": ("homefront-base", "homefront", "HOMEFRONT - TRACK 6, BASE STEM", 30,
                       "music_r30", "homefront-base-r30-a"),
    # no concept round either: music_r31 writes firestorm-base-r31-a.ogg only into the temporary directory
    "firestorm-base": ("firestorm-base", "firestorm", "FIRESTORM - TRACK 7, BASE STEM", 31,
                       "music_r31", "firestorm-base-r31-a"),
}
CUES = {
    "warning": ("boss-warning", "warning", "RED ALERT - TRACK 22, BOSS WARNING", 25),
    "actcomplete": ("act-complete", "actcomplete", "ACT COMPLETE - TRACK 24, FANFARE", 25),
    "miniboss": ("miniboss-sting", "miniboss", "CONTACT HEAVY - TRACK 21, MINI-BOSS STING", 26),
    "complete": ("mission-complete", "complete", "MISSION COMPLETE - TRACK 23, VICTORY JINGLE", 26),
    "failed": ("mission-failed", "failed", "MISSION FAILED - TRACK 25, STING", 26),
    "gameover": ("game-over", "gameover", "GAME OVER - TRACK 26", 26),
}
PIECES = {**THEMES, **STEMS, **CUES}
LOOPED = {**THEMES, **STEMS}
HANDOFF = CONCEPT / "boss-handoff-final-r25-a.ogg"
# Full mixes whose chosen encode overshoots the true-peak limit (round 26 check: "afterburner" -0.5 dBTP,
# its base stem -0.8 dBTP, on isolated kick transients made by the Vorbis encoder; round 31 check:
# "firestorm" -0.2 dBTP at 8 spots, its base stem -0.1 dBTP at 2): rendered as their stem generator
# renders the pair, then one transient gain envelope for both files (TP_FIX_*).
TRUE_PEAK_FIX = {"afterburner", "firestorm"}
TP_FIX_LIMIT = -1.2  # dB, 4x oversampled peak of the decoded files above which a transient is dipped
TP_FIX_AIM = -1.3  # dB, where the dip aims (below the limit: the encoder moves peaks by a few 0.01 dB)
TP_FIX_ATTACK, TP_FIX_HOLD, TP_FIX_RELEASE = 0.002, 0.001, 0.05  # s, raised-cosine ramps around an over
HANDOFF_BARS_AFTER = 8  # bars of the boss track in the hand-off aid
SEAM_SECONDS = 8.0


def concept_file(key):
    if key in STEMS:
        return CONCEPT / f"{STEMS[key][5]}.ogg"
    table = music_r08.LOOPS if key in THEMES else music_r08.STINGS
    return CONCEPT / f"{table[PIECES[key][1]][0]}.ogg"


def asset_file(key):
    return MUSIC / f"{PIECES[key][0]}.ogg"


def review_copy(key):
    """The review file (rounds 25 and on): the final asset as the game loads it, byte for byte."""
    return CONCEPT / f"{PIECES[key][0]}-final-r{PIECES[key][3]}-a.ogg"


def seam_file(key):
    """Listening aid: a looped piece across its loop seam."""
    return CONCEPT / f"{PIECES[key][0]}-seam-final-r{PIECES[key][3]}-a.ogg"


def source(key):
    return ROUNDS[PIECES[key][3]][3]


def oversampled_peak(path):
    """Per sample, the largest |value| of either channel at 4x (soxr), as a true-peak meter sees it."""
    raw = subprocess.run(
        ["ffmpeg", "-hide_banner", "-loglevel", "error", "-i", str(path), "-af",
         f"aresample={4 * SR}:resampler=soxr:precision=28", "-f", "f32le", "-ac", "2", "-"],
        capture_output=True, check=True).stdout
    up = np.abs(np.frombuffer(raw, dtype="<f4").reshape(-1, 2)).max(axis=1)
    n = len(up) // 4
    return up[:n * 4].reshape(n, 4).max(axis=1)


def duck(env, at, gain):
    """Lower ``env`` to ``gain`` at sample ``at``: raised-cosine attack, hold, raised-cosine release."""
    a, h, r = ns(TP_FIX_ATTACK), ns(TP_FIX_HOLD), ns(TP_FIX_RELEASE)
    ramp = lambda k: 0.5 + 0.5 * np.cos(np.linspace(0, np.pi, k))  # noqa: E731  1 -> 0
    curve = np.concatenate([1 - (1 - gain) * (1 - ramp(a)), np.full(2 * h + 1, gain), 1 - (1 - gain) * ramp(r)])
    lo = at - h - a
    c0, c1 = max(0, -lo), min(len(curve), len(env) - lo)
    env[lo + c0:lo + c1] = np.minimum(env[lo + c0:lo + c1], curve[c0:c1])


def render_true_peak_fixed(key, tmp):
    """The full mix ``key`` and its base stem as the stem generator renders them (checked byte for byte
    against the chosen files; a stem rendered straight to production has none, the full mix's check
    covers the pass), then one gain envelope, dipping only around the transients whose
    encoded true peak is over the limit, applied to both before the final encode. Returns
    {key: path} of the two encoded files and the envelope's numbers."""
    stem = next(k for k, v in STEMS.items() if v[1] == key)
    gen = importlib.import_module(STEMS[stem][4])
    s, full, base = gen.render()  # music_r15.main() up to the encode, line for line
    full, loop_start, loop_len = music_r11.prepare(s, full)
    base, _, _ = music_r11.prepare(s, base)
    full, base = music_r11.master_linked(full, base)
    for a in (full, base):
        a[:, :ns(0.005)] *= np.linspace(0, 1, ns(0.005))
    tags = {"LOOPSTART": loop_start, "LOOPLENGTH": loop_len}
    out = {key: Path(tmp) / f"{key}.ogg", stem: Path(tmp) / f"{stem}.ogg"}
    for _ in range(4):
        write_ogg(out[key], full, max_peak_db=None, tags=tags)
        over = db(np.abs(decode(out[key])).max()) + 1.0
        if over <= 0:
            break
        full, base = full * undb(-over - 0.1), base * undb(-over - 0.1)
    write_ogg(out[stem], base, max_peak_db=None, tags=tags)
    for k in out:
        if (k == key or concept_file(k).exists()) and out[k].read_bytes() != concept_file(k).read_bytes():
            raise SystemExit(f"{k}: the generator no longer reproduces {concept_file(k).name}")
    env = np.ones(full.shape[1])
    for _ in range(8):
        overs, encoded = 0, env.copy()  # the envelope the measured files were encoded with
        for k in out:
            peak = oversampled_peak(out[k])[:len(env)]
            for at in np.nonzero(peak > undb(TP_FIX_LIMIT))[0]:
                duck(env, int(at), encoded[at] * undb(TP_FIX_AIM) / peak[at])
                overs += 1
        if not overs:
            break
        write_ogg(out[key], full * env, max_peak_db=None, tags=tags)
        write_ogg(out[stem], base * env, max_peak_db=None, tags=tags)
    else:
        raise SystemExit(f"{key}: true-peak envelope did not converge")
    dipped = env < 1
    print(f"{key} + {stem}: transient envelope down to {db(env.min()):.2f} dB, "
          f"{dipped.sum() / SR * 1000:.0f} ms of {env.size / SR:.1f} s below unity, "
          f"{len(np.nonzero(np.diff(dipped.astype(int)) == 1)[0])} dips", flush=True)
    return out


def fix_pair(key):
    """The full mix in TRUE_PEAK_FIX that ``key`` (a full mix or its base stem) belongs to, or None."""
    pair = STEMS[key][1] if key in STEMS else key
    return pair if pair in TRUE_PEAK_FIX else None


def render(key, rendered):
    """Render with the chosen generator into a temporary file, then remux with the SOURCE comment.
    ``rendered`` maps a TRUE_PEAK_FIX pair to the directory of its two files (rendered once)."""
    with tempfile.TemporaryDirectory() as tmp:
        pair = fix_pair(key)
        if pair:
            if pair not in rendered:
                rendered[pair] = tempfile.mkdtemp()
                render_true_peak_fixed(pair, rendered[pair])
            src = Path(rendered[pair]) / f"{key}.ogg"
        else:
            if key in STEMS:  # writes the base stem; exits unless its full mix is the chosen one
                importlib.import_module(STEMS[key][4]).main(["--out", tmp])
            elif key in THEMES:
                music_r08.render_loop(PIECES[key][1], Path(tmp))
            else:
                music_r08.render_sting(PIECES[key][1], Path(tmp))
            src = Path(tmp) / concept_file(key).name
        MUSIC.mkdir(parents=True, exist_ok=True)
        subprocess.run(
            ["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-i", str(src), "-c:a", "copy",
             "-metadata:s:a:0", f"SOURCE={source(key)}", "-fflags", "+bitexact", str(asset_file(key))],
            check=True)
    if PIECES[key][3] >= 25:
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
        chosen = decode(concept_file(key)) if concept_file(key).exists() else None
        same = chosen is not None and chosen.shape == m["audio"].shape and np.array_equal(chosen, m["audio"])
        if chosen is None:
            relation = "no concept file (rendered straight to production)"
        elif same:
            relation = f"audio identical to {concept_file(key).name}"
        elif chosen.shape == m["audio"].shape:
            rms = lambda x: 20 * np.log10(np.sqrt((x ** 2).mean()) + 1e-12)  # noqa: E731
            diff = m["audio"] - chosen
            relation = (f"audio differs from {concept_file(key).name} (difference peak "
                        f"{db(np.abs(diff).max()):.1f} dBFS, {rms(chosen) - rms(diff):.0f} dB under the signal)")
        else:
            relation = f"audio differs from {concept_file(key).name} (other length)"
        common = [
            (m["source"] != source(key), "no SOURCE comment"),
            (m["bitrate"] != Q6_NOMINAL, f"nominal bitrate {m['bitrate']}, not q6"),
            (m["tp"] > MAX_TRUE_PEAK, f"true peak {m['tp']:.1f} dBTP"),
        ]
        loudness = f"{m['lufs']:.1f} LUFS"
        if key in STEMS:  # the full mix's gain: quieter than it, aligned with it
            full = measure(asset_file(STEMS[key][1]))
            common += [
                (m["lufs"] >= full["lufs"], f"{m['lufs']:.1f} LUFS, not under the full mix"),
                (m["audio"].shape != full["audio"].shape, "length differs from the full mix"),
                ((m["start"], m["length"]) != (full["start"], full["length"]),
                 "loop comments differ from the full mix"),
            ]
            loudness += f" ({m['lufs'] - full['lufs']:+.1f} LU vs the full mix, same length and loop)"
        else:
            common.append((abs(m["lufs"] - TARGET_LUFS) > LOUDNESS_TOLERANCE, f"{m['lufs']:.1f} LUFS"))
        if key in LOOPED:
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
        if PIECES[key][3] >= 25:
            problems.append((not review_copy(key).exists()
                             or review_copy(key).read_bytes() != asset_file(key).read_bytes(),
                             f"{review_copy(key).name} missing or not the asset"))
        problems = [text for bad, text in problems if bad]
        failed |= bool(problems)
        print(f"{asset_file(key).name}: {m['seconds']:.2f} s, {shape}, {loudness}, "
              f"{m['tp']:.1f} dBTP, {m['size'] / 1e6:.2f} MB, {relation}"
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


def seam_aid(key):
    """A looped piece across its loop seam as the looping stream plays it: the loop's last 8 s, then
    the first 8 s after LOOPSTART."""
    m = measure(asset_file(key))
    end, n, track = m["start"] + m["length"], int(SEAM_SECONDS * SR), m["audio"]
    seam = np.concatenate([track[:, end - n:end], track[:, m["start"]:m["start"] + n]], axis=1)
    write_ogg(seam_file(key), fade_ends(seam), max_peak_db=None)
    return seam_file(key)


def listening_aids(round_no):
    """Round 25: the boss cue as the game mixes it and track 18's seam; later rounds: every looped
    piece's seam."""
    aids = []
    if round_no == 25:
        warning, boss = decode(asset_file("warning")), decode(asset_file("boss"))
        at = int(round(handoff_seconds() * SR))
        bar = int(round(16 * 15 * SR / WARNING_BPM))
        cue, _ = boss_cue(warning, boss, at, at + HANDOFF_BARS_AFTER * bar)
        write_ogg(HANDOFF, fade_out(cue, 1.0), max_peak_db=None)
        aids.append(HANDOFF)
    aids += [seam_aid(k) for k, v in LOOPED.items() if v[3] == round_no]
    for path in aids:
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
    # each theme followed by its stems, then the cues
    order = [k for t in THEMES for k in (t, *[s for s in STEMS if STEMS[s][1] == t])] + list(CUES)
    for key in order:
        asset, _, title, r = PIECES[key][:4]
        if r != round_no:
            continue
        m = measure(asset_file(key))
        head = f"{title}   ASSETS/MUSIC/{asset.upper()}.OGG"
        tail = (f"{m['lufs']:.1f} LUFS   TRUE PEAK {m['tp']:.1f} DBTP   ")
        if key in STEMS:
            tail = (f"{m['lufs']:.1f} LUFS ({m['lufs'] - measure(asset_file(STEMS[key][1]))['lufs']:+.1f} LU, "
                    f"FULL MIX GAIN)   TRUE PEAK {m['tp']:.1f} DBTP   ")
        if key in LOOPED:
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
    elif any(v[3] == round_no for v in CUES.values()):
        legend += "   GREEN: ONE-SHOT CUE"
    raster.draw_text(img, 20, y - 14, legend, raster.LABEL_DIM)
    path = CONCEPT / file
    img.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(artkit.ROOT)}")
    if round_no >= 25:
        listening_aids(round_no)


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
    rendered = {}
    try:
        for key in keys:
            render(key, rendered)
    finally:
        for tmp in rendered.values():
            shutil.rmtree(tmp, ignore_errors=True)
    check(keys)
    for r in rounds:
        review(r)


if __name__ == "__main__":
    main(sys.argv[1:])
