#!/usr/bin/env python3
"""Concept round 31: the Kilo Lead's audition (Level 09, M5 part C, user decision D11 = a).

Outputs (design/audio/voice/concept/):
  voice-kilo-lead-r31-<a|b>.ogg  both of the Kilo Lead's lines from Level 09's radio chatter, t=95
                                 ("Aegis, Kilo convoy on the Okonjo Bridge. Sixty civilians in the
                                 trucks, and things are coming over the far bank.") and the
                                 secondary objective's line ("Kilo convoy is across. Thank you,
                                 Aegis."), 1 s apart, each spoken by Chatterbox with candidate
                                 reference clip a or b and put through radio filter b (radio() of
                                 tts_r18.py, its default: its own clicks and -16 LUFS); OGG Vorbis
                                 q4, 44.1 kHz mono; the neutral settings of round 19, as the game
                                 would render them (data.yaml's `expressions.neutral`).

The Kilo Lead is the CDF officer of the Kilo truck convoy on the Okonjo Bridge (Nova Lagos). Both
candidates are male readers of military non-fiction, neither in the cast nor auditioned before:
a Aaron Bennett (On Mamba Station: U.S. Marines in West Africa, 1990-2003, a monograph on the
Marines' evacuation operations in West Africa), b tombooker (The Colored Regulars in the United
States Army).

Reference clips: design/audio/voice/refs/ref-kilo-lead-r31-<a|b>.wav (Git LFS, both in CREDITS.md),
12 s cuts from LibriVox chapter recordings (public domain, sources in REFS; licences checked on each
archive.org item's `licenseurl`), made as in rounds 19, 21, 23, 25 and 30 with
    ffmpeg -ss 60 -t 12 -i https://archive.org/download/<item>/<file> -ac 1 -ar 24000 <clip>.wav
and checked with their Whisper (faster-whisper base.en) transcript, the .txt next to each clip.
Round 31 closed for the Kilo Lead (2026-10-07): a was cast and its clip renamed ref-kilo-lead.wav;
b's was deleted (the command above with REFS's entry cuts it again, to rerun the audition), and b's
audition was moved to design/audio/voice/concept/rejected/ (a rerun writes it to concept/ again).

Setup: the Chatterbox venv of round 18 (~/.cache/tv-tts/venv-chatterbox, see tts_r18.py).
Rerun: python3 tools/concept/audio/tts_r31.py            (generate + post-process)
       python3 tools/concept/audio/tts_r31.py --post     (post-process only)
Raw WAVs go to ~/.cache/tv-tts/raw/r31/. Seeds are fixed, but GPU sampling is not bit-exact.
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
RAW = BASE / "raw" / "r31"

NEUTRAL = {"exaggeration": 0.5, "cfg_weight": 0.5, "temperature": 0.7}
SPEAKER = "kilo-lead"
# Level 09's Kilo Lead lines (design/campaign/act-2-homefront/level-09-arcology-fall/README.md, Radio
# chatter): t=95 and the secondary objective's line.
LINES = [
    "Aegis, Kilo convoy on the Okonjo Bridge. Sixty civilians in the trucks, and things are coming "
    "over the far bank.",
    "Kilo convoy is across. Thank you, Aegis.",
]
GAP = 1.0  # s of silence between the two filtered lines

# variant: (archive.org item, chapter file, cut start s, reader, licence)
REFS = {
    "a": ("onmambastation_2507_librivox", "mambastation_01_antal_64kb.mp3", 60, "Aaron Bennett",
          "Public Domain Mark 1.0"),
    "b": ("coloredregulars_2605_librivox", "coloredregulars_01_steward_64kb.mp3", 60, "tombooker",
          "Public Domain Mark 1.0"),
}
SEED = 31


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
            wav = model.generate(text, audio_prompt_path=str(REFDIR / f"ref-{SPEAKER}-r31-{variant}.wav"),
                                 **NEUTRAL)
            wav = wav.cpu().numpy().reshape(-1)
            sf.write(str(RAW / f"{SPEAKER}-{variant}-{j + 1}.wav"), wav.astype("float32"), model.sr)
            print(SPEAKER, variant, j + 1, round(len(wav) / model.sr, 2), "s", flush=True)


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
        tags = {"COMMENT": f"tools/concept/audio/tts_r31.py {SPEAKER} {variant}"}
        s.write_ogg(DEST / f"voice-{SPEAKER}-r31-{variant}.ogg", y, quality=4, tags=tags)
        print(SPEAKER, variant, round(y.shape[1] / s.SR, 2), "s", round(s.lufs(y)[0], 1), "LUFS", flush=True)


def main(args):
    if args[:1] == ["--worker"]:
        worker()
        return
    if args[:1] != ["--post"]:
        env = dict(os.environ, HF_HOME=str(BASE / "hf"), TOKENIZERS_PARALLELISM="false",
                   TORCH_FORCE_NO_WEIGHTS_ONLY_LOAD="1")
        py = BASE / "venv-chatterbox" / "bin" / "python"
        subprocess.run([str(py), __file__, "--worker"], env=env, check=True)
    post()


if __name__ == "__main__":
    main(sys.argv[1:])
