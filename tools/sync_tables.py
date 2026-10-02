#!/usr/bin/env python3
"""Render the marked tables in the design READMEs from the data files next to them.

A generated table sits between two markers in a README:

    <!-- data: waves -->
    | t (s) | Section | ... |
    <!-- /data -->

The name after "data:" picks a renderer below; it reads the data.yaml in the README's own
directory (and, where it needs them, other data files: an enemy's bounty for a level's credit
budget, the difficulty levers for an enemy's HP). Everything outside the markers is hand-written.
The schemas are documented in design/tech/architecture/README.md#data-file-schemas.

Usage:
  python3 tools/sync_tables.py          rewrite every marked table from its data
  python3 tools/sync_tables.py --check  list the tables that differ from their data, exit 1 if any
"""
import os
import re
import sys

from design_data import (DESIGN, ROOT, base_angle, draw, enemy_dir, fill, grouped, load, num, round_half_up,
                         stats, upgrade_costs, weapon_rules)

BLOCK = re.compile(r"(<!-- data: ([a-z-]+) -->\n)(.*?)(<!-- /data -->)", re.S)


def row(cells):
    return "|" + "|".join(f" {c} " if c != "" else " " for c in cells) + "|"


def table(head, rows):
    return "\n".join([row(head), "|" + "---|" * len(head)] + [row(r) for r in rows])


def ticks(names):
    return ", ".join(f"`{n}`" for n in names)


def price(p):
    return "starter" if p == 0 else grouped(p)


def available(item):
    note = item.get("notes", {}).get("available")
    return item["available"] + (f" {note}" if note else "")


def data(d):
    return load(f"{d}/data.yaml")


def scoring():
    return load("systems/scoring/data.yaml")


def pickups():
    return load("player/data.yaml")["pickups"]


def pickup_credits(name):
    """Credits of a credit pickup named in a level ("small salvage")."""
    size, kind = name.split(" ", 1)
    return pickups()[kind]["credits"][size]


# --- player equipment -------------------------------------------------------------------------

def level_rows(w):
    """The per-level table rows of a weapon: levels 1-5 and the overdrive pattern."""
    rows = []
    for i, lv in enumerate(w["levels"] + [w["overdrive"]], start=1):
        volley, single = stats(w, lv)
        od = i == 6
        rows.append([
            "OD" if od else str(i), str(len(lv["pattern"])), pattern_text(w, lv), num(lv["rate"]),
            num(lv["damage"]), f"{volley:.1f}", f"{single:.1f}", extra_text(lv),
            "— (ignores cap)" if od else num(draw(w, i)),
            "—" if od or i == 1 else grouped(upgrade_costs(w)[i - 2]),
        ])
    return rows


def weapon_levels(d):
    w = data(d)
    rows = level_rows(w)
    per = " Values are per pod." if w.get("pod") else (" Values are per side." if w.get("mirrored") else "")
    target = weapon_rules()["single_target"]
    head = ["Level", "Shots", "Pattern", "Rate /s", "Damage", "Volley DPS", "Single-target DPS", "Extra",
            "Draw MW", "Upgrade cost"]
    note = (f"Generated from [data.yaml](data.yaml) by `tools/sync_tables.py`; do not edit by hand; the exact "
            f"offset and angle of every shot are in that file.{per} Single-target DPS counts the shots that hit a "
            f"{target['width']} px target {target['distance']} px from the muzzle"
            f"{' (seeking/lobbed: all shots)' if w.get('seek') else ''}. "
            f"OD = the overdrive pattern ({pickups()['overdrive']['seconds']} s pickup).")
    return table(head, rows) + "\n\n" + note


def pattern_text(w, lv):
    pat = lv["pattern"]
    if len(pat) == 1:
        return "single"
    xs = sorted({x for x, _ in pat})
    base = base_angle(w, lv)
    angles = sorted({round(a - base, 1) for _, a in pat})
    if len(angles) == 1:
        return "parallel " + ", ".join(f"{x:+g}" for x in xs) + " px"
    if len(xs) == 1:
        return f"fan ±{max(abs(v) for v in angles):g}°"
    return f"{len(pat)} mixed"


def extra_text(lv):
    parts = [f"{label} {lv[key]:g}{unit}" for key, label, unit in (
        ("pierce", "pierce", ""), ("blast", "blast", " px"), ("turn", "turn", "°/s"), ("max_live", "max live", ""))
        if key in lv]
    return ", ".join(parts) or "—"


def weapon_properties(d):
    w = data(d)
    slot = {"front": "Front gun", "rear": "Rear gun", "wing": "Wing mount (per pod)"}[w["slot"]]
    speed = w.get("speed")
    if speed is None:
        speed_text = "lobbed / dropped"
    elif isinstance(speed, list):
        speed_text = f"{num(speed[0])} → {num(speed[1])} px/s"
    else:
        speed_text = f"{num(speed)} px/s"
    rng = w.get("range")
    if rng == "screen":
        range_text = "until off-screen"
    elif rng == "drop":
        range_text = "drops straight down"
    elif rng is None:
        range_text = f"{num(w['lifetime'])} s lifetime"
    else:
        range_text = f"{num(rng)} px"
    costs = " / ".join(grouped(c) for c in upgrade_costs(w))
    rows = [
        ["Slot", slot],
        ["Traits", ticks(w["traits"])],
        ["Layers hit", w["notes"]["layers_hit"]],
        ["Price", "starter (always owned)" if w["price"] == 0 else f"{grouped(w['price'])} cr"],
        ["Upgrade cost L2 / L3 / L4 / L5", f"{costs} cr (sell-back per [economy](../../../systems/economy/README.md))"],
        ["Unlock", "start" if w["unlock"] == 1 else f"L{w['unlock']:02d} (in the shop from the hangar visit before it)"],
        ["Projectile", f"{speed_text}, {w['size'][0]}×{w['size'][1]} px, range {range_text}"],
        ["Sound family", f"`{w['sfx']}` ([weapon sound families](../../../audio/sfx/README.md#weapon-sound-families))"],
        ["Projectile family (VFX)", f"`{w['vfx']}` (projectile sheet of concept round 08)"],
    ]
    return table(["Property", "Value"], rows)


def shields(d):
    rows = [[m["name"], num(m["capacity"]), num(m["regen"]), f"{m['delay']:.1f} s", f"{num(m['draw'])} MW",
             price(m["price"]), available(m)] for m in data(d)["models"]]
    return table(["Model", "Capacity", "Regen /s", "Regen delay", "Draw", "Price (first draft)", "Available"], rows)


def plating(d):
    rows = [[p["name"], num(p["max"]), price(p["price"]), available(p)] for p in data(d)["plating"]]
    return table(["Plating level", "Max armour", "Price (first draft)", "Available"], rows)


def generators(d):
    rows = [[m["name"], f"{num(m['output'])} MW", price(m["price"]), available(m)] for m in data(d)["models"]]
    return table(["Model", "Output", "Price (first draft)", "Available"], rows)


def engines(d):
    rows = [[m["name"], f"{num(m['speed'])} px/s", f"{num(m['draw'])} MW", price(m["price"]), available(m)]
            for m in data(d)["engines"]]
    return table(["Model", "Speed", "Draw", "Price (first draft)", "Available"], rows)


def utility(d):
    rows = []
    for m in data(d)["utility"]:
        levels = "L1" if len(m["prices"]) == 1 else f"L1–L{len(m['prices'])}"
        note = m["notes"].get("levels")
        rows.append([m["name"], m["notes"]["effect"], levels + (f" {note}" if note else ""), num(m["draw"]),
                     " / ".join(grouped(p) for p in m["prices"]), available(m), m["design"]])
    return table(["Module", "Effect", "Levels", "Draw", "Price", "Unlock", "Design"], rows)


def player_pickups(d):
    rows = []
    for key, p in data(d)["pickups"].items():
        notes = p["notes"]
        name = notes.get("name", key.replace("_", " ").capitalize())
        rows.append([name, notes["source"], fill(notes["effect"], p)])
    return table(["Pickup", "Source", "Effect"], rows)


def ship_movement(d):
    ship = data(d)
    engine = next(e for e in load("player/systems/data.yaml")["engines"] if e["available"] == "start")
    values = ship | {"engine": engine, "centre_margin": ship["size"] / 2 + ship["edge_gap"]}
    rows = [[label, fill(text, values)] for label, text in ship["notes"]["movement"].items()]
    return table(["Property", "Value (first draft)"], rows)


# --- enemies ----------------------------------------------------------------------------------

def stat_block(d):
    e = data(d)
    notes = e["notes"]
    basis = load("enemies/data.yaml")
    levers = load("systems/difficulty/data.yaml")["enemy_hp"]
    values = e | {"contact_damage": basis["contact_damage"][e["tier"]]}
    bullet = {b["bullet"]: b["damage"] for b in basis["bullet_damage"]}

    def hp(level):
        return max(1, round_half_up(e["hp"] * levers[level]))

    def suffix(key):
        return f" {fill(notes[key], values)}" if key in notes else ""

    attacks = "; ".join(fill(a["notes"]["text"], a | {"damage": bullet[a["bullet"]]}) for a in e["attacks"])
    formations = ", ".join(f["name"] + (f" ({'–'.join(str(n) for n in f['size'])})" if "size" in f else "")
                           for f in e["formations"])
    weak = ", ".join(f"{w['name']} (×{num(w['multiplier'])})" for w in e["weak_points"]) or "none"
    rows = [
        ["Faction", e["faction"]],
        ["Layer", f"`{e['layer']}`"],
        ["Size tier", f"`{e['tier']}`"],
        ["Size", f"{e['size'][0]}×{e['size'][1]} px, hitbox {e['hitbox'][0]}×{e['hitbox'][1]}"],
        ["Parts", e["parts"]],
        ["Orientation", f"`{e['orientation']}`" + suffix("orientation")],
        ["HP", f"{num(e['hp'])} (easy {hp('easy')} / hard {hp('hard')}, from the global multipliers)"],
        ["Armour / shield", e["armour"]],
        ["Speed", f"{num(e['speed'])} px/s" + suffix("speed")],
        ["Movement", fill(notes["movement"], values)],
        ["Attack", attacks or fill(notes["attack"], values)],
        ["Formations", formations],
        ["Weak points", weak + suffix("weak_points")],
        ["Effective traits", ticks(e["traits"])],
        ["Credits", f"{e['bounty']} (score {e['bounty'] * scoring()['kill_score']} × chain)"],
        ["Death", notes["death"]],
        ["First level / used in", f"L{e['first_level']:02d}; {notes['used_in']}"],
        ["Difficulty hooks", notes["difficulty"]],
    ]
    return table(["Field", "Value"], rows)


def reference_dps(d):
    dps = data(d)["reference_dps"]
    return table(["Level"] + [f"{n:02d}" for n in dps], [["DPS"] + [num(v) for v in dps.values()]])


def player_damage(d):
    basis = data(d)
    rows = [[b["notes"]["label"], num(b["damage"]) + (f" {b['notes']['damage']}" if "damage" in b["notes"] else ""),
             b["notes"]["used_for"]] for b in basis["bullet_damage"]]
    contact = " · ".join(f"{tier} {num(v)}" for tier, v in basis["contact_damage"].items())
    rows.append(["Contact", contact, basis["notes"]["contact"]])
    return table(["Class", "Damage", "Used for"], rows)


def formations(d):
    return table(["Name", "Description"], [[f"`{n}`", text] for n, text in data(d)["formations"].items()])


# --- levels -----------------------------------------------------------------------------------

EDGES = {"left": "left", "right": "right", "both": "both", "alternating": "alternating edges"}
EVENTS = {"level-end": "Level end", "secondary-objective": "Secondary objective met"}


def sections(level):
    """(start s, end s, start px, end px, speed) per section."""
    out, t, px = [], 0, 0
    for s in level["sections"]:
        speed = s.get("speed", level["scroll_speed"])
        end_px = px + (s["end"] - t) * speed
        out.append((t, s["end"], px, end_px, speed))
        t, px = s["end"], end_px
    return out


def section_of(level, t):
    return next(i for i, (start, end, *_) in enumerate(sections(level), start=1) if start <= t < end)


def groups(wave):
    return wave.get("groups") or [{k: wave[k] for k in ("formation", "enemy", "count")}]


def enemy_link(d, slug):
    return f"[{enemy_name(slug)}]({os.path.relpath(DESIGN / enemy_dir(slug) / 'README.md', DESIGN / d)})"


def enemy_name(slug):
    return load(f"{enemy_dir(slug)}/data.yaml")["name"]


def enemy_totals(level):
    totals = {}
    for wave in level["waves"]:
        for g in groups(wave):
            totals[g["enemy"]] = totals.get(g["enemy"], 0) + g["count"]
    return totals


def level_sections(d):
    level = data(d)
    rows = []
    for i, (s, (start, end, px0, px1, speed)) in enumerate(zip(level["sections"], sections(level)), start=1):
        layers = " ".join(f"`{layer}`: {text}" for layer, text in s["notes"]["layers"].items())
        rows.append([f"{i}. {s['name']}", f"{start}–{end}", f"{grouped(px0, ',')}–{grouped(px1, ',')}", num(speed),
                     s["atmosphere"], layers, s["notes"]["purpose"]])
    return table(["Section", "t (s)", "Scroll (px)", "Speed (px/s)", "Atmosphere", "Layers and content", "Purpose"],
                 rows)


def waves(d):
    level = data(d)
    rows = []
    for w in level["waves"]:
        gs = groups(w)
        enter = w["from"] + (f" ({EDGES[w['edge']]})" if "edge" in w else "")
        rows.append([num(w["t"]), str(section_of(level, w["t"])), " + ".join(g["formation"] for g in gs),
                     " + ".join(enemy_link(d, g["enemy"]) for g in gs), " + ".join(str(g["count"]) for g in gs),
                     enter, fill(w["notes"], w) if "notes" in w else ""])
    totals = " · ".join(f"{enemy_name(slug)} {n}" for slug, n in enemy_totals(level).items())
    head = ["t (s)", "Section", "Formation", "Enemies (link)", "Count", "Enter from", "Notes"]
    return table(head, rows) + f"\n\nTotals: {totals}."


def ground_values(target):
    return target | ({"drop_credits": pickup_credits(target["drop"])} if "drop" in target else {})


def ground_targets(d):
    rows = []
    for g in data(d)["ground_targets"]:
        values = ground_values(g)
        rows.append([str(g["section"]), fill(g["notes"]["target"], values), fill(g["notes"]["effect"], values)])
    return table(["Section", "Target", "Effect"], rows)


def radio(d):
    rows = []
    for cue in data(d)["radio"]:
        if "t" in cue:
            note = cue.get("notes", {}).get("trigger")
            trigger = f"t={num(cue['t'])}" + (f" ({note})" if note else "")
        elif cue["event"] == "first-kill":
            trigger = f"First {enemy_name(cue['enemy'])} destroyed"
        else:
            trigger = EVENTS[cue["event"]]
        speaker = cue["speaker"] + (" (distorted)" if cue.get("distorted") else "")
        rows.append([trigger, speaker, f'"{cue["line"]}"'])
    return table(["Trigger", "Speaker", "Line"], rows)


def level_number(d):
    return int(re.match(r"level-(\d+)-", os.path.basename(d)).group(1))


def credit_budget(d):
    level = data(d)
    economy = load("systems/economy/data.yaml")["budget"]
    budget = economy["base"] * economy["growth"] ** (level_number(d) - 1)
    rows = []
    kills = [(slug, n, load(f"{enemy_dir(slug)}/data.yaml")["bounty"]) for slug, n in enemy_totals(level).items()]
    rows.append(["Kills: " + " + ".join(f"{enemy_name(s)} {n} × {b}" for s, n, b in kills),
                 sum(n * b for _, n, b in kills)])
    paying = [ground_values(g) for g in level["ground_targets"] if "bounty" in g]
    if paying:
        rows.append(["Ground targets: " + " + ".join(fill(g["notes"]["budget"], g) for g in paying),
                     sum(g["count"] * (g["bounty"] + g.get("drop_credits", 0)) for g in paying)])
    for s in level["secrets"]:
        rows.append([f"Secret: {s['name']} (hidden crate, {round(100 * s['crate'] / budget)}% of budget)", s["crate"]])
    rows.append(["Secondary objective", level["objectives"]["secondary"]["credits"]])
    total = sum(credits for _, credits in rows)
    cells = [[source, grouped(credits, ",")] for source, credits in rows]
    return table(["Source", "Credits (medium)"], cells + [["**Total**", f"**{grouped(total, ',')}**"]])


# --- systems ----------------------------------------------------------------------------------

def score_bonuses(d):
    rows = []
    for b in data(d)["bonuses"]:
        score = ("kill % × " if b.get("per_kill_percent") else "") + grouped(b["points"])
        score += " × " + {"level": "level number", "act": "act"}[b["scale"]]
        rows.append([b["name"], b["notes"]["condition"], score])
    return table(["Bonus", "Condition", "Score"], rows)


def grades(d):
    rows, previous = [], None
    for g in data(d)["grades"]:
        rating = f"≥ {g['rating']}" if "rating" in g else f"< {previous}"
        bonus = f"+{round_half_up(100 * g['credit_bonus'])} %" if g["credit_bonus"] else "—"
        rows.append([g["grade"], rating, bonus])
        previous = g.get("rating")
    return table(["Grade", "Rating", "Credit bonus"], rows)


def difficulty(d):
    lv = data(d)
    order = ("easy", "medium", "hard")

    def factor(v):
        text = f"{v:.2f}".rstrip("0")
        return "× " + (text + "0" if text.endswith(".") else text)

    def change(v, at_least=None):
        if v == 0:
            return "base"
        sign = "+" if v > 0 else "−"
        text = f"{sign}{round_half_up(abs(v) * 100)} %"
        return text + (f" (at least {sign}{at_least})" if at_least else "")

    def per(key, fmt):
        return [fmt(lv[key][level]) for level in order]

    retries = [f"{lv['retries'][level]}, {lv['notes']['retries']}" if level in lv["retries"] else "unlimited"
               for level in order]
    rows = [
        ["Enemy HP"] + per("enemy_hp", factor),
        ["Enemy bullet speed"] + per("enemy_bullet_speed", factor),
        ["Enemy fire rate"] + per("enemy_fire_rate", factor),
        ["Bullets per `fan` / `ring` / `burst`"] + per("pattern_bullets", lambda v: change(v, 1)),
        ["Aimed shots"] + per("aimed_shots", str),
        ["Formation size"] + per("formation_size", change),
        ["Bullet budget (max enemy bullets on screen)"] + per("bullet_budget", num),
        ["Player shield regen"] + per("shield_regen", factor),
        ["Credit income"] + per("credit_income", factor),
        ["Score multiplier"] + per("score", factor),
        ["Armour repair in hangar"] + per("repair_cost", lambda v: f"{v} cr / point" if v else "free"),
        ["Retries per level"] + retries,
        ["Boss checkpoint"] + per("boss_checkpoint", lambda v: "yes" if v else "no"),
        ["Intel"] + per("sensor_bonus", lambda v: f"sensor level +{v}" if v else "normal"),
    ]
    return table(["Lever", "Easy", "Medium", "Hard"], rows)


RENDERERS = {
    "weapon-levels": weapon_levels, "weapon-properties": weapon_properties, "shields": shields,
    "plating": plating, "generators": generators, "engines": engines, "utility": utility,
    "pickups": player_pickups, "ship-movement": ship_movement, "stat-block": stat_block,
    "reference-dps": reference_dps, "player-damage": player_damage, "formations": formations,
    "level-sections": level_sections, "waves": waves, "ground-targets": ground_targets, "radio": radio,
    "credit-budget": credit_budget, "score-bonuses": score_bonuses, "grades": grades, "difficulty": difficulty,
}


def synced(readme):
    """The README's text with every marked table rendered from its data."""
    d = readme.parent.relative_to(DESIGN).as_posix()

    def render(m):
        name = m.group(2)
        if name not in RENDERERS:
            raise ValueError(f"unknown table '{name}'")
        return m.group(1) + RENDERERS[name](d) + "\n" + m.group(4)

    return BLOCK.sub(render, readme.read_text(encoding="utf-8"))


def marked_readmes():
    return [p for p in sorted(DESIGN.rglob("README.md")) if "<!-- data: " in p.read_text(encoding="utf-8")]


def main():
    check = "--check" in sys.argv[1:]
    stale = 0
    for readme in marked_readmes():
        new = synced(readme)
        if new != readme.read_text(encoding="utf-8"):
            stale += 1
            print(f"{'differs' if check else 'updated'}: {readme.relative_to(ROOT).as_posix()}")
            if not check:
                readme.write_text(new, encoding="utf-8")
    print(f"{stale} README(s) {'differ from their data' if check else 'updated'}")
    return 1 if check and stale else 0


if __name__ == "__main__":
    sys.exit(main())
