#!/usr/bin/env python3
"""Review sheets of the UI screens that have no art of their own (design/ui: briefing, debrief,
pause and options): they draw the glass UI kit (tools/art/ui_kit.py) over the title scene or the
dimmed play field, so their sheet is the game's own capture at 1x with a 2x detail.

Inputs: the game captures ``<part>/concept/<subject>-capture-final-r13-a.png`` (960x540 screenshots
of a ``--bench`` run under xvfb, ``--start title`` or ``--start level``, with a temporary
``--settings`` file and save directory).
Outputs: design/ui/briefing/concept/briefing-final-r13-a.png, design/ui/debrief/concept/
debrief-final-r13-a.png, design/ui/pause/concept/pause-final-r13-a.png (pause and options).

Run: python3 tools/art/ui_review.py   (~2 s)
"""
from PIL import Image

from artkit import DESIGN, ROOT, sprite

from render import raster  # noqa: E402

ROUND = "r13"
UI = DESIGN / "ui"
W, H = 960, 540

# sheet subject -> (its concept directory, title, [(capture part, capture subject, 2x detail box)])
SHEETS = {
    "briefing": ("briefing", "BRIEFING (FINAL R13): THE GLASS KIT OVER THE TITLE SCENE",
                 [("briefing", "briefing", (0, 0, 480, 300))]),
    "debrief": ("debrief", "DEBRIEF (FINAL R13): THE GLASS KIT OVER THE DIMMED TITLE SCENE",
                [("debrief", "debrief", (120, 20, 600, 320))]),
    "pause": ("pause", "PAUSE AND OPTIONS (FINAL R13): THE GLASS KIT OVER THE DIMMED PLAY FIELD",
              [("pause", "pause", (300, 110, 660, 420)), ("options", "options", (0, 0, 480, 160))]),
}


def capture(part, subject):
    return Image.open(UI / part / "concept" / f"{subject}-capture-final-{ROUND}-a.png").convert("RGBA")


def sheet(subject):
    folder, title, shots = SHEETS[subject]
    rows = [(capture(part, name), box) for part, name, box in shots]
    heights = [max(H, (box[3] - box[1]) * 2) + 30 for _, box in rows]
    width = 16 + W + 16 + max((box[2] - box[0]) * 2 for _, box in rows) + 16
    img = raster.sheet(width, 40 + sum(heights), title, f"PRODUCTION ART, UI BATCH - {ROUND.upper()}")
    y = 38
    for (shot, box), (part, name, _), height in zip(rows, shots, heights):
        raster.draw_text(img, 16, y, f"GAME CAPTURE, 960X540 AT 1X ({name.upper()})", raster.LABEL)
        img.alpha_composite(shot, (16, y + 14))
        raster.draw_text(img, 16 + W + 16, y, f"2X DETAIL {box}", raster.LABEL)
        img.alpha_composite(sprite.enlarge(shot.crop(box), 2), (16 + W + 16, y + 14))
        y += height
    path = UI / folder / "concept" / f"{subject}-final-{ROUND}-a.png"
    img.convert("RGB").save(path, optimize=True)
    print(f"review: {path.relative_to(ROOT)}")


if __name__ == "__main__":
    for name in SHEETS:
        sheet(name)
