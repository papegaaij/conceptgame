#!/usr/bin/env python3
"""Validate the living design documentation in design/.

Checks (see CLAUDE.md for the rules):
  1. every directory under design/ has a README.md (concept/ directories are exempt)
  2. every README.md has valid frontmatter
  3. every subdirectory is listed in its parent's Contents table, with Design/Impl/Art
     columns matching the child's frontmatter
  4. every file in a concept/ directory is referenced by the owning README
  5. relative links in markdown files, depends-on entries and review-board HTML resolve

Usage: python3 tools/check_docs.py        exit code 1 when problems are found
       python3 tools/check_docs.py --fix  first sync Contents status cells from the children
"""
import re
import sys
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DESIGN = ROOT / "design"

REQUIRED = ["title", "design", "implementation", "art", "updated"]
ALLOWED = {
    "design": {"idea", "draft", "review", "approved"},
    "implementation": {"n/a", "not-started", "in-progress", "done"},
    "art": {"n/a", "none", "proposed", "chosen", "final"},
}
MD_LINK = re.compile(r"\]\(([^)\s]+)(?:\s+\"[^\"]*\")?\)")
HTML_LINK = re.compile(r"""(?:src|href)\s*=\s*["']([^"']+)["']""")

problems = []
FIX = "--fix" in sys.argv[1:]


def rel(p):
    return p.relative_to(ROOT).as_posix()


def report(path, msg):
    problems.append(f"{rel(path)}: {msg}")


def parse_frontmatter(path):
    text = path.read_text(encoding="utf-8")
    if not text.startswith("---\n"):
        report(path, "missing frontmatter")
        return None
    end = text.find("\n---", 4)
    if end < 0:
        report(path, "unterminated frontmatter")
        return None
    meta = {}
    for line in text[4:end].splitlines():
        line = line.split(" #", 1)[0].rstrip()
        if not line.strip():
            continue
        if ":" not in line:
            report(path, f"bad frontmatter line: {line!r}")
            continue
        key, value = line.split(":", 1)
        value = value.strip()
        if value.startswith("[") and value.endswith("]"):
            value = [v.strip() for v in value[1:-1].split(",") if v.strip()]
        meta[key.strip()] = value
    return meta


def check_frontmatter(path, meta):
    for key in REQUIRED:
        if key not in meta or meta[key] in ("", []):
            report(path, f"frontmatter lacks '{key}'")
    for key, allowed in ALLOWED.items():
        if key in meta and meta[key] not in allowed:
            report(path, f"'{key}: {meta[key]}' not one of {sorted(allowed)}")
    if "updated" in meta:
        try:
            date.fromisoformat(meta["updated"])
        except (TypeError, ValueError):
            report(path, f"'updated: {meta['updated']}' is not YYYY-MM-DD")
    for dep in meta.get("depends-on", []) or []:
        if not (path.parent / dep).exists():
            report(path, f"depends-on '{dep}' does not exist")


def is_concept(path):
    return "concept" in path.relative_to(DESIGN).parts


def check_contents(readme, subdirs, metas):
    lines = readme.read_text(encoding="utf-8").splitlines()
    for sub in subdirs:
        target = f"{sub.name}/README.md"
        rows = [i for i, l in enumerate(lines) if l.lstrip().startswith("|") and f"]({target})" in l]
        if not rows:
            report(readme, f"Contents table has no row linking '{target}'")
            continue
        child = metas.get(sub / "README.md")
        if not child:
            continue
        cells = [c.strip().strip("`") for c in lines[rows[0]].strip().strip("|").split("|")]
        if len(cells) < 5:
            report(readme, f"Contents row for '{sub.name}' needs Part|Summary|Design|Impl|Art")
            continue
        wanted = [child.get(k, "") for k in ("design", "implementation", "art")]
        if FIX and cells[-3:] != wanted:
            lines[rows[0]] = "| " + " | ".join(cells[:-3] + wanted) + " |"
            readme.write_text("\n".join(lines) + "\n", encoding="utf-8")
            print(f"fixed: {rel(readme)} row '{sub.name}'")
            continue
        for key, got in zip(("design", "implementation", "art"), cells[-3:]):
            if child.get(key) != got:
                report(readme, f"Contents row '{sub.name}': {key} is '{got}', "
                               f"child says '{child.get(key)}'")


def check_concept_files(owner_dir):
    readme = owner_dir / "README.md"
    if not readme.exists():
        return
    text = readme.read_text(encoding="utf-8")
    for f in sorted((owner_dir / "concept").rglob("*")):
        if f.is_file() and f.name != "prompts.md":
            ref = f.relative_to(owner_dir).as_posix()
            if ref not in text:
                report(readme, f"concept file '{ref}' is not listed in the README")
    if not (owner_dir / "concept" / "prompts.md").exists():
        report(readme, "concept/ has no prompts.md")


def check_links(path, pattern):
    text = path.read_text(encoding="utf-8")
    text = re.sub(r"```.*?```", "", text, flags=re.S)  # ignore code blocks
    text = re.sub(r"`[^`\n]*`", "", text)  # and inline code
    for target in pattern.findall(text):
        if re.match(r"^(?:[a-z]+:|#|//)", target) or "{" in target:
            continue
        target = target.split("#", 1)[0].split("?", 1)[0]
        if target and not (path.parent / target).exists():
            report(path, f"broken link '{target}'")


def main():
    if not DESIGN.is_dir():
        print("design/ not found")
        return 1
    dirs = [DESIGN] + sorted(d for d in DESIGN.rglob("*") if d.is_dir())
    metas = {}
    for d in dirs:
        if is_concept(d):
            continue
        readme = d / "README.md"
        if not readme.exists():
            report(d, "directory has no README.md")
            continue
        meta = parse_frontmatter(readme)
        if meta is not None:
            check_frontmatter(readme, meta)
            metas[readme] = meta
    for d in dirs:
        if is_concept(d) or not (d / "README.md").exists():
            continue
        subdirs = [s for s in sorted(d.iterdir()) if s.is_dir() and s.name != "concept"]
        check_contents(d / "README.md", subdirs, metas)
        if (d / "concept").is_dir():
            check_concept_files(d)
    for md in [ROOT / "README.md", ROOT / "CLAUDE.md"] + sorted(DESIGN.rglob("*.md")):
        if md.exists():
            check_links(md, MD_LINK)
    for html in sorted(DESIGN.rglob("*.html")):
        check_links(html, HTML_LINK)

    for p in problems:
        print(p)
    count = len(metas)
    print(f"{count} documents checked, {len(problems)} problem(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
