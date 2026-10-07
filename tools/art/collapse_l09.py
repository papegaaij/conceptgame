#!/usr/bin/env python3
"""Production art: Level 09's arcology collapse, look c (design/campaign/act-2-homefront/
level-09-arcology-fall, Collapse set piece; concept round 31's collapse-r31-c, approved by the user
on 2026-10-07 with their tweaks). The concept script tools/concept/collapse_r31.py is imported
unchanged (frozen): its puff bank and its rubble heap are written at the size the game draws them.
The lean, the drop, the cast shadow and the particles are drawn by the game (CollapseLooks with the
tower projection), from the timings and counts of the concept's prompts entry.

Outputs:
  assets/sprites/collapse-puff_0..7.png   128x128 RGBA, the dust puffs (0-3 billowy clusters of
                             small balls, 4-7 hazier), lit from the top left (pale grey to navy
                             grey), soft noisy edges; the game draws them scaled 0.08-2.4x, never
                             rotated (Level 09's unit atlas: SpriteUse claims `collapse-puff` for a
                             level with a collapse)
  assets/backdrop/level-09/arcology-heap.png   280x250, the dust-coated rubble heap left where the
                             tower stood (the collapse's `rubble` piece, not placed: it fades in
                             at the impact, centred on the tower's foot)

Run: python3 tools/art/collapse_l09.py   (~10 s)
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image

import artkit
from artkit import ROOT

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "concept"))
import collapse_r31 as c31  # noqa: E402  (concept script, imported unchanged: variant c)

SOURCE = artkit.source_note("collapse_l09.py", "M5 part C batch, concept round 31 c")
HEAP = ROOT / "assets" / "backdrop" / "level-09" / "arcology-heap.png"


def puffs():
    """The concept's eight puff sprites as 8-bit RGBA (its float bank: rgb 0..255, alpha 0..1)."""
    out = []
    for src in c31.puff_bank():
        rgba = np.dstack([np.clip(src[..., :3], 0, 255), np.clip(src[..., 3:], 0, 1) * 255])
        out.append(Image.fromarray(np.round(rgba).astype(np.uint8), "RGBA"))
    return out


def heap():
    """The concept's rubble heap (280 x 250) as 8-bit RGBA."""
    arr = c31.c_heap()
    return Image.fromarray(np.round(np.clip(arr, 0, 255)).astype(np.uint8), "RGBA")


def heap_image(w=280, h=250):
    """The heap for tools/art/backdrop_l09.py's generators (its `arcology-heap` piece)."""
    img = heap()
    assert img.size == (w, h), img.size
    return img


def main():
    frames = puffs()
    artkit.write_frames("collapse-puff", frames, SOURCE)
    artkit.save_png(heap(), HEAP, SOURCE)
    print(f"{len(frames)} puffs, {HEAP.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
