#!/usr/bin/env python3
"""Production bitmap fonts (design/ui, Shared UI rules; design/art-direction/production): the UI
kit's three fonts as BMFont text files with one PNG page each.

Outputs:
  assets/fonts/label-8x12.fnt + .png      label font, 8x12 cells
  assets/fonts/body-10x20.fnt + .png      body font, 10x20 cells
  assets/fonts/heading-20x30.fnt + .png   heading font, 20x30 cells
  design/ui/concept/fonts-final-r13-a.png review sheet, built from the files above

The look of the chosen UI kit (ui-kit-r08-a): DejaVu Sans Mono Bold at 9, 15 and 26 px, rasterised
1-bit by FreeType with its hinting (monochrome target), so stems are whole pixels; white, tinted at
run time; every glyph advances by the cell width, so the fonts stay monospaced. Production rules:
  - every glyph is drawn at the cell's one baseline (label row 9, body 15, heading 24) and centred
    on its own pixels, so capitals, digits and lower case share their top and bottom rows;
  - nothing is clipped to the cell: accents above capitals and the heading's descenders keep their
    pixels (yoffset may be negative), the line height stays the cell height;
  - the character set is printable ASCII plus every other character the game can draw (CHARSET);
    ``--check`` scans the data files and the game's string literals and fails on a missing one.
Every PNG carries the ``Source`` chunk.

Run: python3 tools/art/fonts.py            (fonts, then the review sheet; ~2 s)
     python3 tools/art/fonts.py --review   (only the review sheet, from assets/)
     python3 tools/art/fonts.py --check    (character coverage and baselines; exit 1 on a problem)
"""
import re
import sys
from pathlib import Path

import numpy as np
import yaml
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).resolve().parent))
import artkit  # noqa: E402
from render import raster, sprite  # noqa: E402
from render.config import ROOT  # noqa: E402

FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"
FONTS = ROOT / "assets" / "fonts"
CONCEPT = ROOT / "design" / "ui" / "concept"
SOURCE = artkit.source_note("fonts.py", "UI batch U3") + ", DejaVu Sans Mono Bold"
# (file name, cell width, cell height, pixel size, baseline row): the cells and sizes of ui-kit-r08-a
SPECS = [("label-8x12", 8, 12, 9, 9), ("body-10x20", 10, 20, 15, 15), ("heading-20x30", 20, 30, 26, 24)]
PAGE_WIDTH = 256
PAD = 8  # rows and columns around a cell where a glyph may overflow while it is rasterised
CHARSET = ("".join(chr(c) for c in range(32, 127))
           + "×·◆▲▼◄►•°–—…‘’“”→≈©"
           + "éèëüöäßÉÈËÜÖÄ")
SAMPLES = [
    "MISSION 01: BREAK AT DAWN",
    "LANCER, ROOK. AEGIS TWO'S GOT THE NORTH ARM, YOU'VE GOT THE SOUTH.",
    "CR 12 450 · ×2.5 · MK II \"ARC\" · 36 → 72 PX · T≈40 S",
    "Their ships don’t show up as metal on our scopes — aim for the glowing parts…",
    "◆◆◆ ▲ ▼ ◄ ► • 100 % 3/5 [ENTER] CONTINUE",
]


# --------------------------------------------------------------------------- rasterising

def glyph(font, ch, cell_w, cell_h, base):
    """One glyph as a boolean mask on the cell padded by PAD on every side: hinted 1-bit, its
    pixels centred on the cell, its baseline on row ``base``."""
    img = Image.new("L", (cell_w + 2 * PAD, cell_h + 2 * PAD), 0)
    draw = ImageDraw.Draw(img)
    draw.fontmode = "1"
    left, _, right, _ = draw.textbbox((0, 0), ch, font=font, anchor="ls")
    x = PAD + (cell_w - (right - left)) // 2 - left
    draw.text((x, PAD + base), ch, font=font, fill=255, anchor="ls")
    return np.array(img) > 0


def write_font(name, cell_w, cell_h, size, base):
    font = ImageFont.truetype(FONT_PATH, size)
    placed = []
    x = y = 1
    row_h = 0
    for ch in CHARSET:
        mask = glyph(font, ch, cell_w, cell_h, base)
        if not mask.any():
            placed.append((ch, None, 0, 0))
            continue
        rows, cols = np.where(mask)
        box = (cols.min(), rows.min(), cols.max() + 1, rows.max() + 1)
        w, h = box[2] - box[0], box[3] - box[1]
        if x + w + 1 > PAGE_WIDTH:
            x, y, row_h = 1, y + row_h + 1, 0
        placed.append((ch, mask[box[1]:box[3], box[0]:box[2]], (x, y), box))
        x += w + 1
        row_h = max(row_h, h)
    height = 1
    while height < y + row_h + 1:
        height *= 2
    page = np.zeros((height, PAGE_WIDTH, 4), np.uint8)
    page[..., :3] = 255
    lines = [
        f'info face="{name}" size={cell_h} bold=1 italic=0 charset="" unicode=1 stretchH=100 smooth=0 aa=1 '
        f"padding=0,0,0,0 spacing=1,1",
        f"common lineHeight={cell_h} base={base} scaleW={PAGE_WIDTH} scaleH={height} pages=1 packed=0",
        f'page id=0 file="{name}.png"',
        f"chars count={len(placed)}",
    ]
    for ch, pixels, at, box in placed:
        if pixels is None:
            lines.append(f"char id={ord(ch)} x=0 y=0 width=0 height=0 xoffset=0 yoffset=0 "
                         f"xadvance={cell_w} page=0 chnl=15")
            continue
        (px, py), (h, w) = at, pixels.shape
        page[py:py + h, px:px + w, 3] = pixels * 255
        lines.append(f"char id={ord(ch)} x={px} y={py} width={w} height={h} xoffset={box[0] - PAD} "
                     f"yoffset={box[1] - PAD} xadvance={cell_w} page=0 chnl=15")
    FONTS.mkdir(parents=True, exist_ok=True)
    (FONTS / f"{name}.fnt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    artkit.save_png(Image.fromarray(page, "RGBA"), FONTS / f"{name}.png", SOURCE)
    print(f"wrote assets/fonts/{name}.fnt + .png ({len(placed)} chars, page {PAGE_WIDTH}x{height})")


# --------------------------------------------------------------------------- reading back

def load_font(name):
    """A font back from assets/: (line height, base, {char: (glyph image, xoffset, yoffset, advance)})."""
    text = (FONTS / f"{name}.fnt").read_text(encoding="utf-8")
    page = Image.open(FONTS / f"{name}.png").convert("RGBA")
    common = re.search(r"lineHeight=(\d+) base=(\d+)", text)
    glyphs = {}
    for m in re.finditer(r"char id=(\d+) x=(\d+) y=(\d+) width=(\d+) height=(\d+) xoffset=(-?\d+) "
                         r"yoffset=(-?\d+) xadvance=(\d+)", text):
        cid, x, y, w, h, xo, yo, adv = map(int, m.groups())
        glyphs[chr(cid)] = (page.crop((x, y, x + w, y + h)) if w else None, xo, yo, adv)
    return int(common[1]), int(common[2]), glyphs


def draw_line(img, x, y, text, font, colour):
    """Draws ``text`` with its line top at ``y``, as libGDX lays out a BMFont line."""
    _, _, glyphs = font
    for ch in text:
        g, xo, yo, adv = glyphs[ch]
        if g is not None:
            tinted = Image.new("RGBA", g.size, colour + (0,))
            tinted.putalpha(g.getchannel("A"))
            img.alpha_composite(tinted, (x + xo, y + yo))
        x += adv


# --------------------------------------------------------------------------- the check

JAVA_LITERAL = re.compile(r'"((?:[^"\\\n]|\\.)*)"|\'((?:[^\'\\\n]|\\.))\'')


def strings(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for v in value.values():
            yield from strings(v)
    elif isinstance(value, list):
        for v in value:
            yield from strings(v)


def used_characters():
    """Every character the game can draw, by where it occurs: all text of the data files and the
    string and char literals of the game and content modules (comments left out), each also in
    upper case, since the screens upper-case their texts."""
    found = {}

    def add(text, where):
        for ch in text + text.upper():
            if ch not in "\n\t":
                found.setdefault(ch, set()).add(where)

    for path in sorted((ROOT / "design").rglob("data.yaml")):
        for text in strings(yaml.safe_load(path.read_text(encoding="utf-8"))):
            add(text, str(path.relative_to(ROOT)))
    for module in ("game", "content"):
        for path in sorted((ROOT / module / "src" / "main" / "java").rglob("*.java")):
            source = re.sub(r"/\*.*?\*/|//[^\n]*", "", path.read_text(encoding="utf-8"), flags=re.S)
            for m in JAVA_LITERAL.finditer(source):
                text = m[1] if m[1] is not None else m[2]
                text = re.sub(r"\\u([0-9a-fA-F]{4})", lambda u: chr(int(u[1], 16)), text)
                add(re.sub(r"\\(.)", r"\1", text), path.name)
    return found


def check():
    problems = []
    used = used_characters()
    for name, *_ in SPECS:
        line_height, base, glyphs = load_font(name)
        for ch in sorted(set(used) - set(glyphs)):
            problems.append(f"{name}: no glyph for {ch!r} (U+{ord(ch):04X}), used in {', '.join(sorted(used[ch])[:3])}")
        for group in ("ABCDEFGHIJKLMNOPRSTUVWXYZ", "0123456789", "acemnorsuvwxz"):
            tops = {glyphs[c][2] for c in group}
            bottoms = {glyphs[c][2] + glyphs[c][0].height for c in group}
            if len(tops) > 1 or bottoms != {base}:
                problems.append(f"{name}: {group[:3]}… do not share their rows (tops {tops}, bottoms {bottoms})")
        print(f"{name}: {len(glyphs)} glyphs, line height {line_height}, base {base}")
    print(f"{len(used)} characters in use")
    for p in problems:
        print("PROBLEM:", p)
    return not problems


# --------------------------------------------------------------------------- review

def review():
    fonts = [(name, cw, ch, load_font(name)) for name, cw, ch, *_ in SPECS]
    width = 1300
    blocks = []
    for name, cw, ch, font in fonts:
        per_row = (width - 32) // (cw * 2)
        rows = [CHARSET[i:i + per_row] for i in range(0, len(CHARSET), per_row)]
        blocks.append((name, cw, ch, font, rows))
    height = 46 + sum(14 + len(r) * (ch * 2 + 4) + 8 + len(SAMPLES) * (ch + 2) + 16
                      for _, _, ch, _, r in blocks)
    img = raster.sheet(width, height, "BITMAP FONTS: LABEL 8X12, BODY 10X20, HEADING 20X30 (DEJAVU SANS MONO BOLD)",
                       "PRODUCTION ART, UI BATCH - R13")
    y = 38
    white, amber, cyan = (235, 240, 255), (255, 190, 80), (110, 230, 255)
    for name, cw, ch, font, rows in blocks:
        raster.draw_text(img, 16, y, f"{name.upper()}  ({len(font[2])} GLYPHS, LINE {font[0]}, BASE {font[1]}): "
                                     "FULL SET AT 2X, CELL GRID; SAMPLES AT 1X", raster.LABEL)
        y += 14
        for row in rows:
            strip = Image.new("RGBA", (len(row) * cw, ch), (14, 18, 34, 255))
            d = ImageDraw.Draw(strip)
            for i in range(len(row)):
                d.rectangle([i * cw, 0, i * cw + cw - 1, ch - 1], fill=(22, 28, 50, 255) if i % 2 else (14, 18, 34, 255))
                d.point((i * cw, font[1]), fill=(80, 60, 30, 255))
            draw_line(strip, 0, 0, row, font, white)
            img.alpha_composite(sprite.enlarge(strip, 2), (16, y))
            y += ch * 2 + 4
        y += 8
        for i, text in enumerate(SAMPLES):
            text = text if name != "heading-20x30" else text[:(width - 32) // cw]
            draw_line(img, 16, y, text, font, (white, amber, cyan)[i % 3])
            y += ch + 2
        y += 16
    out = CONCEPT / "fonts-final-r13-a.png"
    img.convert("RGB").save(out, optimize=True)
    print("review:", out.relative_to(ROOT))


def main(args):
    if "--check" in args:
        sys.exit(0 if check() else 1)
    if "--review" not in args:
        for spec in SPECS:
            write_font(*spec)
    review()


if __name__ == "__main__":
    main(sys.argv[1:])
