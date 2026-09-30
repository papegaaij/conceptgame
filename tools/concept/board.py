#!/usr/bin/env python3
"""Build the review board for a concept round.

Scans design/**/concept/ (including concept/rejected/) for files named
<subject>-r<RR>-<variant>.<ext> and writes design/concept-rounds/round-<RR>/index.html: one
section per subject, variants side by side, images shown pixel-sharp, audio with players. Each
card shows the variant's status as recorded in the owning README's Concept art table. Extra text-only choices (e.g.
story variants) come from the round README: every markdown link to a .md file in its
"## Text choices" section is shown as a link card.

Usage: python3 tools/concept/board.py 01
"""
import html
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DESIGN = ROOT / "design"
IMAGE = {".png", ".gif", ".jpg", ".jpeg", ".webp"}
AUDIO = {".ogg", ".wav", ".mp3", ".flac"}

CSS = """
:root { --bg:#0b0e14; --panel:#151b26; --edge:#2a3547; --text:#d6deeb; --dim:#8391a7;
        --accent:#4fc3f7; --warm:#ffb74d; }
@media (prefers-color-scheme: light) { :root:not([data-theme="dark"]) {
  --bg:#eef1f5; --panel:#ffffff; --edge:#c9d2de; --text:#1b2330; --dim:#5b6878;
  --accent:#0277bd; --warm:#e65100; } }
* { box-sizing: border-box; }
body { margin:0; background:var(--bg); color:var(--text);
       font:15px/1.5 "Segoe UI", Tahoma, Verdana, sans-serif; }
header { padding:24px 16px 8px; max-width:1400px; margin:auto; }
h1 { margin:0; font-size:26px; letter-spacing:.08em; text-transform:uppercase; color:var(--accent); }
header p { color:var(--dim); margin:6px 0 0; }
section { max-width:1400px; margin:18px auto; padding:0 16px; }
h2 { font-size:18px; margin:0 0 4px; color:var(--warm); text-transform:uppercase; letter-spacing:.06em; }
h2 a { color:inherit; text-decoration:none; } h2 a:hover { text-decoration:underline; }
.doc { color:var(--dim); font-size:13px; margin-bottom:10px; }
.doc a { color:var(--accent); }
.grid { display:grid; gap:14px; grid-template-columns:repeat(auto-fit, minmax(280px, 1fr)); }
.card { background:var(--panel); border:1px solid var(--edge); border-radius:6px; padding:10px; }
.card h3 { margin:0 0 8px; font-size:15px; }
.card img { width:100%; height:auto; image-rendering:pixelated; display:block;
            background:#000; border-radius:3px; }
.card audio { width:100%; margin-top:6px; }
.card .file { color:var(--dim); font-size:12px; margin-top:6px; word-break:break-all; }
.status { font-size:12px; font-weight:bold; text-transform:uppercase; margin-left:6px; }
.status.chosen { color:#66bb6a; } .status.rejected { color:#ef5350; }
.status.proposed { color:var(--accent); }
.card.rejected { opacity:.55; }
.card a { color:var(--accent); }
footer { max-width:1400px; margin:30px auto; padding:0 16px 30px; color:var(--dim); font-size:13px; }
"""

NAME = re.compile(r"^(?P<subject>.+)-r(?P<round>\d\d)-(?P<variant>[a-z])\.(?P<ext>[a-z0-9]+)$")
LINK = re.compile(r"\[([^\]]+)\]\(([^)\s]+)\)")


def collect(round_no):
    items = {}
    for f in sorted(DESIGN.rglob("*")):
        if not f.is_file():
            continue
        if f.parent.name == "concept":
            owner = f.parent.parent
        elif f.parent.name == "rejected" and f.parent.parent.name == "concept":
            owner = f.parent.parent.parent
        else:
            continue
        m = NAME.match(f.name)
        if not m or m["round"] != round_no:
            continue
        key = (owner, m["subject"])
        items.setdefault(key, []).append((m["variant"], f))
    return items


def status_of(readme, f):
    """Last cell of the Concept art table row that mentions the file, e.g. 'chosen — ...'."""
    if readme.exists():
        for line in readme.read_text(encoding="utf-8").splitlines():
            if line.startswith("|") and f"/{f.name}" in line:
                return line.strip().strip("|").split("|")[-1].strip()
    return "proposed"


def text_choices(readme):
    if not readme.exists():
        return []
    out, inside = [], False
    for line in readme.read_text(encoding="utf-8").splitlines():
        if line.startswith("## "):
            inside = line.strip().lower() == "## text choices"
        elif inside:
            out += [(label, target) for label, target in LINK.findall(line)
                    if target.split("#")[0].endswith(".md")]
    return out


def main():
    if len(sys.argv) != 2 or not re.fullmatch(r"\d\d", sys.argv[1]):
        print(__doc__)
        return 2
    round_no = sys.argv[1]
    out_dir = DESIGN / "concept-rounds" / f"round-{round_no}"
    out_dir.mkdir(parents=True, exist_ok=True)

    def link(p):
        return html.escape(os.path.relpath(p, out_dir).replace(os.sep, "/"))

    parts = []
    for (owner, subject), variants in sorted(collect(round_no).items(),
                                              key=lambda kv: (str(kv[0][0]), kv[0][1])):
        doc = owner / "README.md"
        cards = []
        for variant, f in sorted(variants):
            ext = f.suffix.lower()
            if ext in IMAGE:
                media = f'<a href="{link(f)}"><img src="{link(f)}" alt="{subject} {variant}" loading="lazy"></a>'
            elif ext in AUDIO:
                media = f'<audio controls preload="none" src="{link(f)}"></audio>'
            else:
                media = f'<a href="{link(f)}">open</a>'
            status = status_of(doc, f)
            word = status.split()[0].lower() if status else "proposed"
            cards.append(f'<div class="card {word}"><h3>Variant {variant.upper()}'
                         f'<span class="status {word}">{html.escape(status)}</span></h3>{media}'
                         f'<div class="file">{html.escape(f.name)}</div></div>')
        parts.append(
            f'<section><h2>{html.escape(subject.replace("-", " "))}</h2>'
            f'<div class="doc">Part: <a href="{link(doc)}">{html.escape(owner.relative_to(DESIGN).as_posix())}</a>'
            f' · prompts: <a href="{link(owner / "concept" / "prompts.md")}">prompts.md</a></div>'
            f'<div class="grid">{"".join(cards)}</div></section>')

    extra = text_choices(out_dir / "README.md")
    if extra:
        cards = "".join(f'<div class="card"><h3>{html.escape(label)}</h3>'
                        f'<a href="{html.escape(target)}">{html.escape(target)}</a></div>'
                        for label, target in extra)
        parts.append(f'<section><h2>Text choices</h2><div class="grid">{cards}</div></section>')

    page = f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Concept Round {round_no}</title>
<style>{CSS}</style></head>
<body>
<header><h1>Concept round {round_no}</h1>
<p>Generated by tools/concept/board.py — the choices to make are listed in
<a href="README.md" style="color:var(--accent)">README.md</a>.</p></header>
{''.join(parts)}
<footer>Regenerate: <code>python3 tools/concept/board.py {round_no}</code></footer>
</body></html>
"""
    (out_dir / "index.html").write_text(page, encoding="utf-8")
    print(f"wrote {out_dir.relative_to(ROOT)}/index.html ({len(parts)} sections)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
