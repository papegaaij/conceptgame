#!/usr/bin/env python3
"""Concept round 35: the Nansen audition (Level 13 "Polar Relay", M5 part G, decision G9 = a,
pending the user's review).

Outputs (design/audio/voice/concept/):
  voice-nansen-r35-<a|b>.ogg  Nansen (the CDF officer of Nansen Relay, a new speaker): three of its
                              lines from Level 13's radio chatter, t=36 (the arrival: "Aegis,
                              Nansen. Very glad to see you. They come at us from every direction out
                              here."), the relay below half (the `ally-below` 0.5 event: "Integrity
                              at half! We can't take much more!") and t=218 (grid online: "Grid sync
                              complete. Every battery on Earth just came back online."), 1 s apart,
                              each through radio filter b on its own
OGG Vorbis q4, 44.1 kHz mono; Chatterbox with candidate reference clip a or b at the neutral
settings of round 19, as the game would render them (data.yaml's `expressions.neutral`).

Casting: a is a male voice, b a female one, so the round picks between two clearly different
officers; both are new (not in the cast, not auditioned before) and readers of polar-expedition
non-fiction, chosen for a measured delivery (the pitches are in prompts.md):
  a Patrick Eaton (In the Arctic Seas, McClintock)
  b Sharon Riskedahl (Farthest North, Volume 1, ch. 1 part 1, Nansen; file 01 is the contents)

Reference clips: design/audio/voice/refs/ref-nansen-r35-<a|b>.wav (Git LFS, both for CREDITS.md),
12 s cuts from LibriVox chapter recordings (public domain, sources in REFS; licences checked on each
archive.org item's `licenseurl`), made as in rounds 19-33 with
    ffmpeg -ss 60 -t 12 -i https://archive.org/download/<item>/<file> -ac 1 -ar 24000 <clip>.wav
(`python3 tools/concept/audio/tts_r35.py --refs` cuts the missing ones) and checked with their
Whisper (faster-whisper base.en) transcript, the .txt next to each clip.

Setup: the Chatterbox venv of round 18 (~/.cache/tv-tts/venv-chatterbox, see tts_r18.py); Whisper
(faster-whisper) from the Dia venv (~/.cache/tv-tts/venv-dia), for the read-back.
Rerun: python3 tools/concept/audio/tts_r35.py            (generate, post-process, read back)
       python3 tools/concept/audio/tts_r35.py --post     (post-process and read back only)
       python3 tools/concept/audio/tts_r35.py --refs     (cut the missing reference clips)
Raw WAVs go to ~/.cache/tv-tts/raw/r35/. Seeds are fixed, but GPU sampling is not bit-exact.
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
RAW = BASE / "raw" / "r35"
WHISPER_PY = BASE / "venv-dia" / "bin" / "python"

SPEAKER = "nansen"
NEUTRAL = {"exaggeration": 0.5, "cfg_weight": 0.5, "temperature": 0.7}
# Level 13's lines (design/campaign/act-2-homefront/level-13-polar-relay/README.md, Radio chatter),
# as spoken: the arrival (t=36), the tense one (the relay below half, an event) and the release
# (t=218, grid online).
LINES = [
    "Aegis, Nansen. Very glad to see you. They come at us from every direction out here.",
    "Integrity at half! We can't take much more!",
    "Grid sync complete. Every battery on Earth just came back online.",
]
GAP = 1.0  # s of silence between two filtered lines

# variant: (archive.org item, chapter file, cut start s, reader, licence)
REFS = {
    "a": ("arctic_seas_pje_1311_librivox", "inarcticseas_01_mcclintock_64kb.mp3", 60, "Patrick Eaton",
          "Public Domain Mark 1.0"),
    "b": ("farthestnorth_vol1_0912_librivox", "farthestnorth_02_nansen_64kb.mp3", 60, "Sharon Riskedahl",
          "public domain (creativecommons.org/licenses/publicdomain)"),
}
SEED = 35


def ref(variant):
    return REFDIR / f"ref-{SPEAKER}-r35-{variant}.wav"


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
            path = DEST / f"voice-{SPEAKER}-r35-{variant}.ogg"
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
        tags = {"COMMENT": f"tools/concept/audio/tts_r35.py {SPEAKER} {variant}"}
        s.write_ogg(DEST / f"voice-{SPEAKER}-r35-{variant}.ogg", y, quality=4, tags=tags)
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
