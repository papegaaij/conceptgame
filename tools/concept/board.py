#!/usr/bin/env python3
"""Build the review board for a concept round.

Scans design/**/concept/ (including concept/rejected/) for files named
<subject>-r<RR>-<variant>.<ext> and writes design/concept-rounds/round-<RR>/index.html: one
section per subject, variants side by side, images shown pixel-sharp, audio with players. Each
card shows the variant's status as recorded in the owning README's Concept art table. Extra text-only choices (e.g.
story variants) come from the round README: every markdown link to a .md file in its
"## Text choices" section is shown as a link card. A "## Listening" section's markdown table is
shown as a table whose links to audio files become players (e.g. before / after comparisons).

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
table.listen { border-collapse:collapse; width:100%; font-size:13px; }
table.listen th, table.listen td { border-bottom:1px solid var(--edge); padding:4px 6px; text-align:left; }
table.listen audio { width:220px; height:28px; }
nav { max-width:1400px; margin:8px auto 0; padding:0 16px; font-size:13px; color:var(--dim); }
nav div { margin:2px 0; } nav b { color:var(--text); } nav a { color:var(--accent); margin-right:10px; }
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
            # only Concept art rows: they start with a link to the file itself
            if line.startswith(("| [concept/", "| [concept/rejected/")) and f"/{f.name}](" in line:
                return line.strip().strip("|").split("|")[-1].strip()
    return "proposed"


def text_choices(readme):
    return [(label, target) for line in section_lines(readme, "text choices")
            for label, target in LINK.findall(line) if target.split("#")[0].endswith(".md")]


def section_lines(readme, title):
    """The lines of a README's ``## <title>`` section."""
    if not readme.exists():
        return []
    out, inside = [], False
    for line in readme.read_text(encoding="utf-8").splitlines():
        if line.startswith("## "):
            inside = line.strip().lower() == f"## {title}"
        elif inside:
            out.append(line)
    return out


def listening_table(readme):
    """The "## Listening" section's markdown table as HTML, audio links as players."""
    rows = [line.strip().strip("|").split("|") for line in section_lines(readme, "listening")
            if line.startswith("|") and not re.fullmatch(r"[|\s:-]+", line)]
    if not rows:
        return ""

    def player(m):
        label, target = m[1], m[2]
        if Path(target).suffix.lower() in AUDIO:
            return f'<audio controls preload="none" src="{target}" title="{label}"></audio>'
        return f'<a href="{target}">{label}</a>'

    def cell(text):
        return LINK.sub(player, html.escape(text.strip(), quote=False))
    head = "".join(f"<th>{html.escape(c.strip())}</th>" for c in rows[0])
    body = "".join("<tr>" + "".join(f"<td>{cell(c)}</td>" for c in r) + "</tr>" for r in rows[1:])
    return f'<table class="listen"><tr>{head}</tr>{body}</table>'


def main():
    if len(sys.argv) != 2 or not re.fullmatch(r"\d\d", sys.argv[1]):
        print(__doc__)
        return 2
    round_no = sys.argv[1]
    out_dir = DESIGN / "concept-rounds" / f"round-{round_no}"
    out_dir.mkdir(parents=True, exist_ok=True)

    def link(p):
        return html.escape(os.path.relpath(p, out_dir).replace(os.sep, "/"))

    parts, toc = [], {}
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
        anchor = f"{owner.relative_to(DESIGN).as_posix().replace('/', '-')}-{subject}"
        toc.setdefault(owner.relative_to(DESIGN).as_posix(), []).append((anchor, subject))
        parts.append(
            f'<section id="{html.escape(anchor)}"><h2>{html.escape(subject.replace("-", " "))}</h2>'
            f'<div class="doc">Part: <a href="{link(doc)}">{html.escape(owner.relative_to(DESIGN).as_posix())}</a>'
            f' · prompts: <a href="{link(owner / "concept" / "prompts.md")}">prompts.md</a></div>'
            f'<div class="grid">{"".join(cards)}</div></section>')

    extra = text_choices(out_dir / "README.md")
    if extra:
        cards = "".join(f'<div class="card"><h3>{html.escape(label)}</h3>'
                        f'<a href="{html.escape(target)}">{html.escape(target)}</a></div>'
                        for label, target in extra)
        parts.append(f'<section><h2>Text choices</h2><div class="grid">{cards}</div></section>')

    listening = listening_table(out_dir / "README.md")
    if listening:
        parts.append(f'<section id="listening"><h2>Listening</h2><div class="card">{listening}</div></section>')

    nav = "".join(f'<div><b>{html.escape(owner)}</b>: '
                  + "".join(f'<a href="#{html.escape(a)}">{html.escape(s.replace("-", " "))}</a>'
                            for a, s in items) + "</div>" for owner, items in toc.items())
    page = f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Concept Round {round_no}</title>
<style>{CSS}</style></head>
<body>
<header><h1>Concept round {round_no}</h1>
<p>Generated by tools/concept/board.py — the choices to make are listed in
<a href="README.md" style="color:var(--accent)">README.md</a>.</p></header>
<nav>{nav}</nav>
{''.join(parts)}
<footer>Regenerate: <code>python3 tools/concept/board.py {round_no}</code></footer>
</body></html>
"""
    (out_dir / "index.html").write_text(page, encoding="utf-8")
    print(f"wrote {out_dir.relative_to(ROOT)}/index.html ({len(parts)} sections)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
