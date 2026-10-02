"""The design tree's data files (design/**/data.yaml), shared by the doc tools.

Loads the files with PyYAML, fills the `notes` templates (README wording with `{field}`
placeholders, see design/tech/architecture/README.md#data-file-schemas) and computes the weapon
numbers that both the weapon tables (sync_tables.py) and the balance report (balance.py) use.
"""
import math
import string
from functools import cache
from pathlib import Path

import yaml

ROOT = Path(__file__).resolve().parent.parent
DESIGN = ROOT / "design"
SLOT_ORDER = ("front", "rear", "wing")
BASE_ANGLE = {"front": 0.0, "wing": 0.0, "rear": 180.0}


@cache
def load(rel):
    """The parsed file at `rel`, a path relative to design/ (e.g. "player/data.yaml")."""
    return yaml.safe_load((DESIGN / rel).read_text(encoding="utf-8"))


def weapons():
    """Every weapon's data by slug, front guns first, then rear guns and wing mounts, by unlock level."""
    found = {p.parent.name: load(p.relative_to(DESIGN).as_posix())
             for p in (DESIGN / "player" / "weapons").glob("*/data.yaml")}
    return dict(sorted(found.items(), key=lambda kv: (SLOT_ORDER.index(kv[1]["slot"]), kv[1]["unlock"], kv[0])))


def enemy_dir(slug):
    """The directory of an enemy's data file, relative to design/."""
    matches = list((DESIGN / "enemies").glob(f"*/{slug}/data.yaml"))
    if len(matches) != 1:
        raise ValueError(f"no single enemy data file for '{slug}'")
    return matches[0].parent.relative_to(DESIGN).as_posix()


# --- formatting -------------------------------------------------------------------------------

def num(v):
    """A number without trailing zeros: 2.0 -> 2, 0.25 -> 0.25."""
    return f"{v:g}"


def grouped(n, sep=" "):
    """A whole number with thousands separators: 12000 -> "12 000" (or "12,000")."""
    return f"{round(n):,}".replace(",", sep)


def round_half_up(x):
    return math.floor(x + 0.5)


class _Template(string.Formatter):
    """str.format with dotted field names ({movement.swoop.radius}) and [lo, hi] ranges as lo–hi."""

    def get_field(self, field_name, args, kwargs):
        value = kwargs
        for part in field_name.split("."):
            value = value[part]
        return value, field_name

    def format_field(self, value, format_spec):
        if isinstance(value, list) and len(value) == 2:
            return f"{num(value[0])}–{num(value[1])}"
        if isinstance(value, float) and not format_spec:
            return num(value)
        return format(value, format_spec)


def fill(template, values):
    """The template with each {field} replaced by its value in `values`."""
    return _Template().vformat(template, (), values)


# --- weapon numbers ---------------------------------------------------------------------------

def weapon_rules():
    return load("player/weapons/data.yaml")


def draw(w, level):
    """Power draw at upgrade level 1-5: linear from L1 to L5, rounded to the draw step."""
    step = weapon_rules()["draw_round"]
    lo, hi = w["draw"]
    return math.floor((lo + (hi - lo) * (level - 1) / 4) / step + 0.5 + 1e-9) * step


def upgrade_costs(w):
    """The costs of the upgrades to L2, L3, L4 and L5."""
    base = w.get("upgrade_base", w["price"])
    return [round(base * f) for f in weapon_rules()["upgrade_cost_factors"]]


def base_angle(w, lv):
    if w.get("mirrored"):
        return lv["pattern"][0][1] if len(lv["pattern"]) == 1 else sum(a for _, a in lv["pattern"]) / len(lv["pattern"])
    return BASE_ANGLE[w["slot"]]


def focus(w, lv):
    """Share of a volley that hits the single target (seeking, lobbed and dropped weapons: all)."""
    if w.get("seek"):
        return 1.0
    target = weapon_rules()["single_target"]
    half = target["width"] / 2 + w["size"][0] / 2
    base = base_angle(w, lv)
    hits = sum(1 for x, a in lv["pattern"]
               if abs(x + target["distance"] * math.tan(math.radians(a - base))) <= half)
    return hits / len(lv["pattern"])


def stats(w, lv):
    """(volley DPS, single-target DPS) of one upgrade level or the overdrive pattern."""
    volley = len(lv["pattern"]) * lv["damage"] * lv["rate"]
    return volley, volley * focus(w, lv)
