#!/usr/bin/env python3
"""Balancing check for the player's equipment (Acts 1-2) and renderer of the weapon tables.

The numbers live in design/player/balance-data.json (the single source; see its "_doc").

Usage:
  python3 tools/balance.py              report per level 01-14: budget vs spending, power load, DPS
  python3 tools/balance.py --sync       rewrite the per-level tables in design/player/weapons/*/README.md
  python3 tools/balance.py --weapons    print every weapon's per-level numbers

The report compares the planned loadout's forward single-target DPS with the reference player
DPS the enemy stat blocks assume (table in design/enemies/README.md, "Balancing basis") and flags
levels outside 0.75-1.33x.

Enemy hook: when design/enemies/balance-data.json exists the report adds time-to-kill and
bounty checks. Expected format (written by the enemy specification work):
  {"enemies": {"<slug>": {"hp": 8, "bounty": 5, "tier": "tiny"}, ...},
   "levels":  {"01": {"waves": {"<slug>": <count>, ...}, "boss": "<slug or null>"}, ...}}
"""
import json
import math
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / "design" / "player" / "balance-data.json"
ENEMY_DATA = ROOT / "design" / "enemies" / "balance-data.json"
ENEMY_README = ROOT / "design" / "enemies" / "README.md"
WEAPON_DIR = ROOT / "design" / "player" / "weapons"
BASE_ANGLE = {"front": 0.0, "wing": 0.0, "rear": 180.0}
MARK_START, MARK_END = "<!-- balance:levels -->", "<!-- /balance:levels -->"


def load():
    return json.loads(DATA.read_text(encoding="utf-8"))


def round_half(x, step=0.5):
    return math.floor(x / step + 0.5 + 1e-9) * step


def draw(w, level, step):
    lo, hi = w["draw"]
    return round_half(lo + (hi - lo) * (level - 1) / 4, step)


def upgrade_costs(w, factors):
    base = w.get("upgrade_base", w["price"])
    return [round(base * f) for f in factors]


def base_angle(w, lv):
    if w.get("mirrored"):
        return lv["pattern"][0][1] if len(lv["pattern"]) == 1 else sum(a for _, a in lv["pattern"]) / len(lv["pattern"])
    return BASE_ANGLE[w["slot"]]


def focus(w, lv, dist, width):
    """Share of a volley that hits a target `width` px wide at `dist` px straight ahead of the muzzle."""
    if w.get("seek"):
        return 1.0
    half = width / 2 + w["size"][0] / 2
    base = base_angle(w, lv)
    hits = sum(1 for x, a in lv["pattern"]
               if abs(x + dist * math.tan(math.radians(a - base))) <= half)
    return hits / len(lv["pattern"])


def stats(w, lv, a):
    n = len(lv["pattern"])
    volley = n * lv["damage"] * lv["rate"]
    single = volley * focus(w, lv, a["focus_distance_px"], a["focus_target_width_px"])
    return volley, single


def pattern_text(w, lv):
    pat = lv["pattern"]
    if len(pat) == 1:
        return "single"
    xs = sorted({x for x, _ in pat})
    base = base_angle(w, lv)
    angs = sorted({round(a - base, 1) for _, a in pat})
    if len(angs) == 1:
        return "parallel " + ", ".join(f"{x:+g}" for x in xs) + " px"
    if len(xs) == 1:
        return f"fan ±{max(abs(v) for v in angs):g}°"
    return f"{len(pat)} mixed"


def extra_text(lv):
    parts = []
    for key, label, unit in (("pierce", "pierce", ""), ("blast", "blast", " px"),
                             ("turn", "turn", "°/s"), ("max_live", "max live", "")):
        if key in lv:
            parts.append(f"{label} {lv[key]:g}{unit}")
    return ", ".join(parts) or "—"


def level_rows(w, data):
    a, e = data["assumptions"], data["economy"]
    costs = upgrade_costs(w, e["upgrade_cost_factors"])
    rows = []
    for i, lv in enumerate(w["levels"] + [w["overdrive"]], start=1):
        volley, single = stats(w, lv, a)
        od = i == 6
        rows.append([
            "OD" if od else str(i),
            str(len(lv["pattern"])),
            pattern_text(w, lv),
            f"{lv['rate']:g}",
            f"{lv['damage']:g}",
            f"{volley:.1f}",
            f"{single:.1f}",
            extra_text(lv),
            "— (ignores cap)" if od else f"{draw(w, i, e['draw_round']):g}",
            "—" if od or i == 1 else f"{costs[i - 2]:,}".replace(",", " "),
        ])
    return rows


def render_table(w, data):
    per = " Values are per pod." if w.get("pod") else (" Values are per side." if w.get("mirrored") else "")
    head = ("| Level | Shots | Pattern | Rate /s | Damage | Volley DPS | Single-target DPS | Extra | "
            "Draw MW | Upgrade cost |\n|---|---|---|---|---|---|---|---|---|---|\n")
    body = "\n".join("| " + " | ".join(r) + " |" for r in level_rows(w, data))
    a = data["assumptions"]
    note = (f"\n\nGenerated from [balance-data.json](../../balance-data.json) by `tools/balance.py --sync`; "
            f"do not edit by hand; the exact offset and angle of every shot are in that file.{per} "
            "Single-target DPS counts the shots that hit a "
            f"{a['focus_target_width_px']} px target {a['focus_distance_px']} px from the muzzle"
            f"{' (seeking/lobbed: all shots)' if w.get('seek') else ''}. "
            "OD = the overdrive pattern (20 s pickup).")
    return head + body + note


def sync(data):
    changed = 0
    for slug, w in data["weapons"].items():
        p = WEAPON_DIR / slug / "README.md"
        if not p.exists():
            print(f"missing {p.relative_to(ROOT)}")
            continue
        text = p.read_text(encoding="utf-8")
        new = re.sub(re.escape(MARK_START) + r".*?" + re.escape(MARK_END),
                     MARK_START + "\n" + render_table(w, data) + "\n" + MARK_END, text, flags=re.S)
        if new == text and MARK_START not in text:
            print(f"no table markers in {p.relative_to(ROOT)}")
        elif new != text:
            p.write_text(new, encoding="utf-8")
            changed += 1
    print(f"{changed} weapon table(s) updated")


def apply_plan(data):
    """Yield (level, state) for levels 01-14 following the expected-loadout plan."""
    a, e, W = data["assumptions"], data["economy"], data["weapons"]
    state = {"credits": a["starting_credits"], "spent": 0, "income": 0, "slots": {},
             "invested": {}, "core": {}, "utility": {}, "log": []}
    core_lists = {k: data[k + "s"] if k + "s" in data else data[k] for k in ("generator", "shield", "engine")}
    core_lists["armor"] = data["armor"]
    unlock = a["act_unlock_level"]

    def pay(n, amount, what, slot=None):
        state["credits"] -= amount
        state["spent"] += amount
        state["log"].append(f"{what} ({amount})")
        if slot:
            state["invested"][slot] = state["invested"].get(slot, 0) + amount

    for n in range(1, 15):
        key = f"{n:02d}"
        state["log"] = []
        step = data["plan"].get(key, {})
        if "start" in step:
            s = step["start"]
            state["slots"]["front"] = list(s["front"])
            for k in ("generator", "shield", "armor", "engine"):
                state["core"][k] = s[k]
        for slot in step.get("sell", []):
            slug, _ = state["slots"].pop(slot)
            refund = round(state["invested"].pop(slot, 0) * a.get("sell_back", 0.6))
            pay(n, -refund, f"sell {W[slug]['name']} ({slot})")
        for slot, slug in step.get("buy", []):
            w = W[slug]
            if w["unlock"] > n:
                state["log"].append(f"!! {slug} not unlocked before L{key}")
            pay(n, w["price"], f"buy {w['name']} → {slot}", slot)
            state["slots"][slot] = [slug, 1]
        for slot, lvl in step.get("upgrade", []):
            slug, cur = state["slots"][slot]
            costs = upgrade_costs(W[slug], e["upgrade_cost_factors"])
            for l in range(cur + 1, lvl + 1):
                pay(n, costs[l - 2], f"{W[slug]['name']} ({slot}) → L{l}", slot)
            state["slots"][slot] = [slug, lvl]
        for kind, idx in step.get("core", []):
            item = core_lists[kind][idx]
            if unlock[item["available"]] > n:
                state["log"].append(f"!! {kind} {item['name']} not available before L{key}")
            pay(n, item["price"], f"{kind} {item['name']}")
            state["core"][kind] = idx
        for name, lvl in step.get("utility", []):
            item = next(u for u in data["utility"] if u["name"] == name)
            cur = state["utility"].get(name, 0)
            for l in range(cur + 1, lvl + 1):
                pay(n, item["prices"][l - 1], f"{name} L{l}")
            state["utility"][name] = lvl
        for name, count in step.get("charges", []):
            sp = data["specials"][name]
            if sp["unlock"] > n:
                state["log"].append(f"!! {name} not unlocked before L{key}")
            pay(n, sp["charge_price"] * count, f"{count}× {name} charge")
        if n > 1:
            pay(n, a["repair_points_per_level"] * a["repair_cost_per_point"], "repairs (estimate)")
        yield n, state
        budget = e["budget_base"] * e["budget_growth"] ** (n - 1)
        earned = round(budget * a["typical_collection"])
        state["credits"] += earned
        state["income"] += earned


def loadout_numbers(data, state):
    W, a, e = data["weapons"], data["assumptions"], data["economy"]
    load, fwd, rear, volley = 0.0, 0.0, 0.0, 0.0
    for slot, (slug, lvl) in state["slots"].items():
        w = W[slug]
        lv = w["levels"][lvl - 1]
        v, s = stats(w, lv, a)
        load += draw(w, lvl, e["draw_round"])
        volley += v
        if w["slot"] == "rear":
            rear += s
        else:
            fwd += s
    load += data["shields"][state["core"]["shield"]]["draw"]
    load += data["engines"][state["core"]["engine"]]["draw"]
    load += sum(next(u for u in data["utility"] if u["name"] == n)["draw"] for n in state["utility"])
    output = data["generators"][state["core"]["generator"]]["output"]
    return load, output, fwd, rear, volley


def reference_dps():
    """The enemy stat blocks' assumed player DPS per level (enemies README, "Balancing basis")."""
    if not ENEMY_README.exists():
        return {}
    text = ENEMY_README.read_text(encoding="utf-8")
    if "### Balancing basis" not in text:
        return {}
    section = text.split("### Balancing basis", 1)[1]
    levels = dps = None
    for line in section.splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if cells and cells[0] == "Level":
            levels = cells[1:]
        elif cells and cells[0] == "DPS" and levels:
            dps = cells[1:]
            break
    if not (levels and dps):
        return {}
    return {int(l): float(v) for l, v in zip(levels, dps) if l.isdigit()}


def report(data):
    enemies = json.loads(ENEMY_DATA.read_text(encoding="utf-8")) if ENEMY_DATA.exists() else None
    ref = reference_dps()
    e = data["economy"]
    print("Lvl | Budget | Before visit | Spent here | Left | Load/Out | Fwd DPS | Ref DPS | Fwd/Ref | Rear DPS | Volley DPS | Purchases")
    problems = []
    prev_spent = 0
    for n, st in apply_plan(data):
        budget = e["budget_base"] * e["budget_growth"] ** (n - 1)
        spent_here = st["spent"] - prev_spent
        prev_spent = st["spent"]
        load, out, fwd, rear, volley = loadout_numbers(data, st)
        flags = []
        if st["credits"] < 0:
            flags.append("OVERSPENT")
        if load > out:
            flags.append("OVER POWER")
        r = ref.get(n)
        ratio = f"{fwd / r:7.2f}" if r else "      -"
        if r and not 0.75 <= fwd / r <= 1.33:
            flags.append(f"DPS {fwd:.0f} vs enemy reference {r:.0f}")
        print(f"{n:02d}  | {budget:6.0f} | {st['credits'] + spent_here:12.0f} | {spent_here:10.0f} | "
              f"{st['credits']:4.0f} | {load:4.1f}/{out:<3g} | {fwd:7.1f} | {(r or 0):7.0f} | {ratio} | "
              f"{rear:8.1f} | {volley:10.1f} | "
              + "; ".join(st["log"]) + (("  <-- " + ", ".join(flags)) if flags else ""))
        for f in flags:
            problems.append(f"L{n:02d}: {f}")
        problems += [f"L{n:02d}: {m[3:]}" for m in st["log"] if m.startswith("!!")]
        if enemies:
            lvl = enemies.get("levels", {}).get(f"{n:02d}")
            if lvl:
                bounty = sum(enemies["enemies"][s]["bounty"] * c for s, c in lvl.get("waves", {}).items())
                ttk = {s: enemies["enemies"][s]["hp"] / max(fwd, 1e-6) for s in lvl.get("waves", {})}
                worst = max(ttk.items(), key=lambda kv: kv[1]) if ttk else ("-", 0)
                line = f"      enemies: bounty total {bounty:.0f} vs budget {budget:.0f}; slowest TTK {worst[0]} {worst[1]:.1f} s"
                if lvl.get("boss"):
                    b = lvl["boss"]
                    line += f"; boss {b} TTK {enemies['enemies'][b]['hp'] / max(fwd, 1e-6):.0f} s"
                print(line)
    if not ref:
        print("\nNo reference DPS table found in design/enemies/README.md (Balancing basis).")
    if not enemies:
        print("\nEnemy hook: design/enemies/balance-data.json not found — TTK and bounty checks skipped "
              "(see the format in this script's docstring).")
    print("\nProblems: " + ("none" if not problems else "\n  " + "\n  ".join(problems)))
    return 1 if problems else 0


def weapons_dump(data):
    for slug, w in data["weapons"].items():
        print(f"\n{w['name']} ({slug})")
        for r in level_rows(w, data):
            print("  " + " | ".join(r))


def main():
    data = load()
    if "--sync" in sys.argv:
        sync(data)
        return 0
    if "--weapons" in sys.argv:
        weapons_dump(data)
        return 0
    return report(data)


if __name__ == "__main__":
    sys.exit(main())
