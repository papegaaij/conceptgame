#!/usr/bin/env python3
"""Concept round 19: casting the generic radio speakers of Act 1 (Chatterbox, radio filter b).

Outputs (design/audio/voice/concept/):
  voice-<speaker>-r19-<a|b>.ogg   one real line per speaker, spoken by Chatterbox with candidate
                                  reference clip a or b, through radio filter b (radio() of
                                  tts_r18.py, its default), OGG Vorbis q4, 44.1 kHz mono, -16 LUFS.
  speaker: yard-control, dock, ring-control, tranquility-control, crawler-one, convoy,
  hammer-lead (the lines and settings are in LINES below and in design/audio/voice/concept/
  prompts.md).

Reference clips: the chosen candidate of each speaker is design/audio/voice/refs/ref-<speaker>.wav
(in the repo, Git LFS; CHOSEN below); the rejected ones were deleted from the repo when the round
closed and are rebuilt into ~/.cache/tv-tts/refs/r19/ref-<speaker>-r19-<v>.wav. All are 12 s cuts from LibriVox chapter recordings (public domain or CC0, sources in REFS and in
CREDITS.md), made with
    ffmpeg -ss <start> -t 12 -i <chapter>_64kb.mp3 -ac 1 -ar 24000 ref-<speaker>-r19-<v>.wav
from https://archive.org/download/<item>/<file>; the .txt next to each clip is its Whisper
(faster-whisper base.en) transcript, the check that the cut is the reader's narration.

Setup: the Chatterbox venv of round 18 (~/.cache/tv-tts/venv-chatterbox, see tts_r18.py).
Rerun: python3 tools/concept/audio/tts_r19.py            (generate + post-process)
       python3 tools/concept/audio/tts_r19.py --post     (post-process only)
Raw WAVs go to ~/.cache/tv-tts/raw/r19/. Seeds are fixed, but GPU sampling is not bit-exact.
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
RAW = BASE / "raw" / "r19"

NEUTRAL = {"exaggeration": 0.5, "cfg_weight": 0.5, "temperature": 0.7}
FIERCE = {"exaggeration": 0.8, "cfg_weight": 0.4, "temperature": 0.8}
URGENT = {"exaggeration": 1.2, "cfg_weight": 0.3, "temperature": 0.8}

# speaker: (text as spoken, settings). Real lines from the levels' data.yaml (the dash of
# Dock One's line is read as a comma; Convoy's {ally} as "Three").
LINES = {
    "yard-control": ("Crane Four is still on automatic. Watch the arm, Aegis!", FIERCE),
    "dock": ("Dock One to anyone! They're growing on the hull, burn it off us!", URGENT),
    "ring-control": ("Debris field ahead. Big pieces will stop your rounds. And theirs.", NEUTRAL),
    "tranquility-control": ("Walkers coming over the crater rims, both sides of the road!",
                            URGENT),
    "crawler-one": ("Crawler One rolling. We're slow and we're loud, Aegis. Please don't leave "
                    "us.", NEUTRAL),
    "convoy": ("We're taking fire! Crawler Three is hit!", URGENT),
    "hammer-lead": ("Hammer flight, inbound!", FIERCE),
}

# (speaker, variant): (archive.org item, chapter file, cut start s, reader, licence)
REFS = {
    ("yard-control", "a"): ("princess_mars_0810_librivox", "aprincessofmars_00-02_burroughs_64kb.mp3",
                            60, "Mark Nelson", "PD"),
    ("yard-control", "b"): ("lady_audleys_secret_ek_librivox",
                            "lady_audleys_secret_01_braddon_64kb.mp3", 60, "Elizabeth Klett", "PD"),
    ("dock", "a"): ("sea_wolf_0907_librivox", "seawolf_01_london_64kb.mp3", 60, "Tom Crawford",
                    "PD"),
    ("dock", "b"): ("jekyll_and_hyde_klh_0904_librivox", "jekyll_01_stevenson_64kb.mp3", 60,
                    "Kristin Hughes", "PD"),
    ("ring-control", "a"): ("studyinscarlet_bn_librivox", "studyinscarlet_01_doyle_64kb.mp3", 60,
                            "Bob Neufeld", "CC0"),
    ("ring-control", "b"): ("frankenstein_cs_librivox", "frankenstein_01_shelley_64kb.mp3", 60,
                            "Cori Samuel", "CC0"),
    ("tranquility-control", "a"): ("huckleberry_finn_0908_librivox", "finn_01_twain_64kb.mp3", 60,
                                   "John Greenman", "PD"),
    ("tranquility-control", "b"): ("scarlet_pimpernel_ks_librivox",
                                   "scarlet_pimpernel_01_orczy_64kb.mp3", 60, "Karen Savage",
                                   "PD"),
    ("crawler-one", "a"): ("pollyanna-1_pc_librivox", "pollyanna_01_porter_64kb.mp3", 60,
                           "Phil Chenevert", "CC0"),
    ("crawler-one", "b"): ("heidi_solo_librivox", "heidi_01_spyri_64kb.mp3", 60,
                           "Kara Shallenberg", "PD"),
    ("convoy", "a"): ("treasure_island_ap_librivox", "treasure_island_01-02_stevenson_64kb.mp3",
                      60, "Adrian Praetzellis", "PD"),
    ("convoy", "b"): ("dorothy_wizard_oz_librivox", "dorothywiz_01_baum_64kb.mp3", 60,
                      "Judy Bieber", "PD"),
    ("hammer-lead", "a"): ("lost_world_mfs_librivox", "lost_world_01_doyle_64kb.mp3", 120,
                           "Mark F. Smith", "PD"),
    ("hammer-lead", "b"): ("scaramouche_gm_librivox", "scaramouche_b01_c01_sabatini_64kb.mp3", 60,
                           "Gord Mackenzie", "PD"),
}
SEED = 19
# The user's picks when the round closed (2026-10-03).
CHOSEN = {"yard-control": "a", "dock": "b", "ring-control": "b", "tranquility-control": "b",
          "crawler-one": "b", "convoy": "a", "hammer-lead": "b"}


def ref_clip(speaker, variant):
    if CHOSEN[speaker] == variant:
        return REFDIR / f"ref-{speaker}.wav"
    return BASE / "refs" / "r19" / f"ref-{speaker}-r19-{variant}.wav"
# Takes re-rolled with another seed (seeds 19+i, 1019, 2019, 3019 and 5019 ran on past the line
# with this reference at exaggeration 1.2: Whisper heard trailing syllables; 4019 is clean).
SEED_OF = {("tranquility-control", "a"): 4019}


def worker():
    import random

    import numpy as np
    import soundfile as sf
    import torch
    from chatterbox.tts import ChatterboxTTS
    model = ChatterboxTTS.from_pretrained(device="cuda")
    RAW.mkdir(parents=True, exist_ok=True)
    for i, (speaker, variant) in enumerate(REFS):
        text, settings = LINES[speaker]
        for f in (random.seed, np.random.seed, torch.manual_seed):
            f(SEED_OF.get((speaker, variant), SEED + i))
        wav = model.generate(text, audio_prompt_path=str(ref_clip(speaker, variant)),
                             **settings)
        wav = wav.cpu().numpy().reshape(-1)
        sf.write(str(RAW / f"{speaker}-{variant}.wav"), wav.astype("float32"), model.sr)
        print(speaker, variant, round(len(wav) / model.sr, 2), "s", flush=True)


def post():
    sys.path.insert(0, str(HERE))
    import tts_r18
    s = tts_r18._synth()
    DEST.mkdir(parents=True, exist_ok=True)
    for speaker, variant in REFS:
        x = s.decode(RAW / f"{speaker}-{variant}.wav")[0]
        y = tts_r18.radio(x)  # filter b
        tags = {"COMMENT": f"tools/concept/audio/tts_r19.py {speaker} {variant}"}
        dest = DEST if CHOSEN[speaker] == variant else DEST / "rejected"
        s.write_ogg(dest / f"voice-{speaker}-r19-{variant}.ogg", y, quality=4, tags=tags)
        print(speaker, variant, round(s.lufs(y)[0], 1), "LUFS", flush=True)


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
