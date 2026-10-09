#!/usr/bin/env python3
"""Concept round 33: the Atlas Control audition (Level 11, M5 part E, user decision E10 = a).

Outputs (design/audio/voice/concept/):
  voice-atlas-control-r33-<a|b>.ogg  Atlas Control (the CDF officer of convoy Atlas-Seven): three of
                                     its lines from Level 11's radio chatter, t=1 (the convoy's
                                     call: "Aegis flight, Atlas-Seven. Three hulls of reactor parts
                                     for the Arctic grid. We'd like to arrive with all three."), a
                                     cargo ship's first hit with {ally} = Halvorsen ("The Halvorsen
                                     is hit! Taking on water, but holding!") and t=62.5 (the sonar
                                     contact: "Sonar has a contact. Big. Very big. It's gone deep
                                     again."), 1 s apart, each through radio filter b on its own
OGG Vorbis q4, 44.1 kHz mono; Chatterbox with candidate reference clip a or b at the neutral
settings of round 19, as the game would render them (data.yaml's `expressions.neutral`); the text as
spoken, as the game speaks it (the ship's name in place of {ally}).

Casting: a is a male voice, b a female one, so the round picks between two clearly different
officers; both are new (not in the cast, not auditioned before) and readers of naval or wartime
non-fiction, chosen for a measured delivery (the pitches are in prompts.md):
  a Alister (Sea-Power in the Pacific, Bywater)
  b MaryAnn (Eighteen Months in the War Zone: A Record of a Woman's Work, Finzi)

Reference clips: design/audio/voice/refs/ref-atlas-control-r33-<a|b>.wav (Git LFS, both in
CREDITS.md), 12 s cuts from LibriVox chapter recordings (public domain, sources in REFS; licences
checked on each archive.org item's `licenseurl`), made as in rounds 19-32 with
    ffmpeg -ss 60 -t 12 -i https://archive.org/download/<item>/<file> -ac 1 -ar 24000 <clip>.wav
(`python3 tools/concept/audio/tts_r33.py --refs` cuts the missing ones) and checked with their
Whisper (faster-whisper base.en) transcript, the .txt next to each clip.
At the close of round 33 (2026-10-09, b picked) b's clip was renamed refs/ref-atlas-control.wav
(the cast voice's), a's deleted with its CREDITS.md row and a's audition moved to concept/rejected/.

Setup: the Chatterbox venv of round 18 (~/.cache/tv-tts/venv-chatterbox, see tts_r18.py); Whisper
(faster-whisper) from the Dia venv (~/.cache/tv-tts/venv-dia), for the read-back.
Rerun: python3 tools/concept/audio/tts_r33.py            (generate, post-process, read back)
       python3 tools/concept/audio/tts_r33.py --post     (post-process and read back only)
Raw WAVs go to ~/.cache/tv-tts/raw/r33/. Seeds are fixed, but GPU sampling is not bit-exact.
"""
import os
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
REFDIR = ROOT / "design" / "audio" / "voice" / "refs"
DEST = ROOT / "design" / "audio" / "voice" / "concept"
BASE = Path.home() / ".cache" / "tv-tts"
RAW = BASE / "raw" / "r33"
WHISPER_PY = BASE / "venv-dia" / "bin" / "python"

SPEAKER = "atlas-control"
NEUTRAL = {"exaggeration": 0.5, "cfg_weight": 0.5, "temperature": 0.7}
# Level 11's lines (design/campaign/act-2-homefront/level-11-atlantic-convoy/README.md, Radio
# chatter), as spoken: an order (the convoy's call), a ship-name line (the ally-hit line with
# {ally} = Halvorsen) and the tense one (the sonar contact).
LINES = [
    "Aegis flight, Atlas-Seven. Three hulls of reactor parts for the Arctic grid. We'd like to arrive "
    "with all three.",
    "The Halvorsen is hit! Taking on water, but holding!",
    "Sonar has a contact. Big. Very big. It's gone deep again.",
]
GAP = 1.0  # s of silence between two filtered lines

# variant: (archive.org item, chapter file, cut start s, reader, licence)
REFS = {
    "a": ("seapowerpacific_2406_librivox", "seapowerinthepacific_01_bywater_64kb.mp3", 60, "Alister",
          "Public Domain Mark 1.0"),
    "b": ("eighteenmonthsinthewarzone_1409_librivox", "eighteenmonths_01_finzi_64kb.mp3", 60, "MaryAnn",
          "Public Domain Mark 1.0"),
}
SEED = 33


def ref(variant):
    return REFDIR / f"ref-{SPEAKER}-r33-{variant}.wav"


def refs():
    """Cut the missing reference clips and write their Whisper transcripts."""
    for variant, (item, name, start, _, _) in REFS.items():
        out = ref(variant)
        if not out.exists():
            subprocess.run(["ffmpeg", "-nostdin", "-loglevel", "error", "-y", "-ss", str(start), "-t", "12",
                            "-i", f"https://archive.org/download/{item}/{name}", "-ac", "1", "-ar", "24000",
                            str(out)], check=True)
    run_whisper("--transcribe-refs")


def worker():
    import random

    import numpy as np
    import soundfile as sf
    import torch
    from chatterbox.tts import ChatterboxTTS
    model = ChatterboxTTS.from_pretrained(device="cuda")
    RAW.mkdir(parents=True, exist_ok=True)
    for i, variant in enumerate(REFS):
        for j, text in enumerate(LINES):
            for f in (random.seed, np.random.seed, torch.manual_seed):
                f(SEED + 10 * i + j)
            wav = model.generate(text, audio_prompt_path=str(ref(variant)), **NEUTRAL)
            wav = wav.cpu().numpy().reshape(-1)
            sf.write(str(RAW / f"{SPEAKER}-{variant}-{j + 1}.wav"), wav.astype("float32"), model.sr)
            print(variant, j + 1, round(len(wav) / model.sr, 2), "s", flush=True)


def whisper(mode):
    """In the Dia venv: the reference transcripts (.txt), the raw takes' or the finished files'
    read-back."""
    from faster_whisper import WhisperModel
    asr = WhisperModel("base.en", device="cpu", compute_type="int8")

    def text(path):
        segs, _ = asr.transcribe(str(path), language="en")
        return " ".join(seg.text.strip() for seg in segs)

    if mode == "--transcribe-refs":
        for variant in REFS:
            t = text(ref(variant))
            ref(variant).with_suffix(".txt").write_text(t + "\n")
            print(ref(variant).name, ":", t)
    else:
        for variant in REFS:
            for j in range(len(LINES)):
                name = f"{SPEAKER}-{variant}-{j + 1}"
                print("raw", name, ":", text(RAW / f"{name}.wav"))
            path = DEST / f"voice-{SPEAKER}-r33-{variant}.ogg"
            print("read back", path.name, ":", text(path))


def run_whisper(mode):
    subprocess.run([str(WHISPER_PY), __file__, "--whisper", mode], check=True,
                   env=dict(os.environ, HF_HOME=str(BASE / "hf")))


def post():
    import numpy as np
    sys.path.insert(0, str(HERE))
    import tts_r18
    s = tts_r18._synth()
    DEST.mkdir(parents=True, exist_ok=True)
    for variant in REFS:
        parts = []
        for j in range(len(LINES)):
            x = s.decode(RAW / f"{SPEAKER}-{variant}-{j + 1}.wav")[0]
            parts += [tts_r18.radio(x), np.zeros((1, int(GAP * s.SR)))]  # filter b
        y = np.concatenate(parts[:-1], axis=1)
        tags = {"COMMENT": f"tools/concept/audio/tts_r33.py {SPEAKER} {variant}"}
        s.write_ogg(DEST / f"voice-{SPEAKER}-r33-{variant}.ogg", y, quality=4, tags=tags)
        print(variant, round(y.shape[1] / s.SR, 2), "s", round(s.lufs(y)[0], 1), "LUFS", flush=True)


def main(args):
    if args[:1] == ["--worker"]:
        worker()
        return
    if args[:1] == ["--whisper"]:
        whisper(args[1])
        return
    if args[:1] == ["--refs"]:
        refs()
        return
    if args[:1] != ["--post"]:
        env = dict(os.environ, HF_HOME=str(BASE / "hf"), TOKENIZERS_PARALLELISM="false",
                   TORCH_FORCE_NO_WEIGHTS_ONLY_LOAD="1")
        py = BASE / "venv-chatterbox" / "bin" / "python"
        subprocess.run([str(py), __file__, "--worker"], env=env, check=True)
    post()
    run_whisper("--read-back")


if __name__ == "__main__":
    main(sys.argv[1:])
