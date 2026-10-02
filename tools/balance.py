#!/usr/bin/env python3
"""Balancing check for the player's equipment (Acts 1-2).

The numbers come from the parts' data files (design/player/**/data.yaml, the economy and the
difficulty levers); the expected purchases per hangar visit are in design/player/balance-plan.yaml.
The weapon tables are rendered by tools/sync_tables.py.

Usage:
  python3 tools/balance.py              report per level 01-14: budget vs spending, power load, DPS
  python3 tools/balance.py --weapons    print every weapon's per-level numbers

The report compares the planned loadout's forward single-target DPS with the reference player
DPS the enemy stat blocks assume (design/enemies/data.yaml, the "Balancing basis" table in
design/enemies/README.md) and flags levels outside 0.75-1.33x.

Enemy hook: when design/enemies/balance-data.json exists the report adds time-to-kill and
bounty checks. Expected format (written by the enemy specification work):
  {"enemies": {"<slug>": {"hp": 8, "bounty": 5, "tier": "tiny"}, ...},
   "levels":  {"01": {"waves": {"<slug>": <count>, ...}, "boss": "<slug or null>"}, ...}}
"""
import json
import sys

from design_data import DESIGN, draw, load, stats, upgrade_costs, weapons
from sync_tables import level_rows

ENEMY_DATA = DESIGN / "enemies" / "balance-data.json"


def load_all():
    """Everything the report needs, gathered from the data files."""
    plan = load("player/balance-plan.yaml")
    economy = load("systems/economy/data.yaml")
    return {
        "plan": plan,
        "economy": economy,
        "availability": load("player/data.yaml")["availability"],
        "repair_cost": load("systems/difficulty/data.yaml")["repair_cost"][plan["difficulty"]],
        "weapons": weapons(),
        "core": {
            "generator": load("player/generator/data.yaml")["models"],
            "shield": load("player/shields/data.yaml")["models"],
            "armor": load("player/armor/data.yaml")["plating"],
            "engine": load("player/systems/data.yaml")["engines"],
        },
        "utility": {u["name"]: u for u in load("player/systems/data.yaml")["utility"]},
        "specials": {s["name"]: s for s in load("player/specials/data.yaml")["specials"]},
    }


def budget(data, n):
    b = data["economy"]["budget"]
    return b["base"] * b["growth"] ** (n - 1)


def apply_plan(data):
    """Yield (level, state) for levels 01-14 following the expected-loadout plan."""
    plan, W = data["plan"], data["weapons"]
    state = {"credits": data["economy"]["starting_credits"], "spent": 0, "income": 0, "slots": {},
             "invested": {}, "core": {}, "utility": {}, "log": []}

    def pay(amount, what, slot=None):
        state["credits"] -= amount
        state["spent"] += amount
        state["log"].append(f"{what} ({amount})")
        if slot:
            state["invested"][slot] = state["invested"].get(slot, 0) + amount

    for n in range(1, 15):
        key = f"{n:02d}"
        state["log"] = []
        step = plan["plan"].get(key, {})
        if "start" in step:
            s = step["start"]
            state["slots"]["front"] = list(s["front"])
            for k in ("generator", "shield", "armor", "engine"):
                state["core"][k] = s[k]
        for slot in step.get("sell", []):
            slug, _ = state["slots"].pop(slot)
            refund = round(state["invested"].pop(slot, 0) * data["economy"]["sell_back"])
            pay(-refund, f"sell {W[slug]['name']} ({slot})")
        for slot, slug in step.get("buy", []):
            w = W[slug]
            if w["unlock"] > n:
                state["log"].append(f"!! {slug} not unlocked before L{key}")
            pay(w["price"], f"buy {w['name']} → {slot}", slot)
            state["slots"][slot] = [slug, 1]
        for slot, lvl in step.get("upgrade", []):
            slug, cur = state["slots"][slot]
            costs = upgrade_costs(W[slug])
            for l in range(cur + 1, lvl + 1):
                pay(costs[l - 2], f"{W[slug]['name']} ({slot}) → L{l}", slot)
            state["slots"][slot] = [slug, lvl]
        for kind, idx in step.get("core", []):
            item = data["core"][kind][idx]
            if data["availability"][item["available"]] > n:
                state["log"].append(f"!! {kind} {item['name']} not available before L{key}")
            pay(item["price"], f"{kind} {item['name']}")
            state["core"][kind] = idx
        for name, lvl in step.get("utility", []):
            item = data["utility"][name]
            cur = state["utility"].get(name, 0)
            for l in range(cur + 1, lvl + 1):
                pay(item["prices"][l - 1], f"{name} L{l}")
            state["utility"][name] = lvl
        for name, count in step.get("charges", []):
            sp = data["specials"][name]
            if sp["unlock"] > n:
                state["log"].append(f"!! {name} not unlocked before L{key}")
            pay(sp["charge_price"] * count, f"{count}× {name} charge")
        if n > 1:
            pay(plan["repair_points_per_level"] * data["repair_cost"], "repairs (estimate)")
        yield n, state
        earned = round(budget(data, n) * plan["typical_collection"])
        state["credits"] += earned
        state["income"] += earned


def loadout_numbers(data, state):
    W, core = data["weapons"], data["core"]
    load_mw, fwd, rear, volley = 0.0, 0.0, 0.0, 0.0
    for slot, (slug, lvl) in state["slots"].items():
        w = W[slug]
        v, s = stats(w, w["levels"][lvl - 1])
        load_mw += draw(w, lvl)
        volley += v
        if w["slot"] == "rear":
            rear += s
        else:
            fwd += s
    load_mw += core["shield"][state["core"]["shield"]]["draw"]
    load_mw += core["engine"][state["core"]["engine"]]["draw"]
    load_mw += sum(data["utility"][n]["draw"] for n in state["utility"])
    output = core["generator"][state["core"]["generator"]]["output"]
    return load_mw, output, fwd, rear, volley


def report(data):
    enemies = json.loads(ENEMY_DATA.read_text(encoding="utf-8")) if ENEMY_DATA.exists() else None
    ref = load("enemies/data.yaml")["reference_dps"]
    print("Lvl | Budget | Before visit | Spent here | Left | Load/Out | Fwd DPS | Ref DPS | Fwd/Ref | Rear DPS | Volley DPS | Purchases")
    problems = []
    prev_spent = 0
    for n, st in apply_plan(data):
        level_budget = budget(data, n)
        spent_here = st["spent"] - prev_spent
        prev_spent = st["spent"]
        load_mw, out, fwd, rear, volley = loadout_numbers(data, st)
        flags = []
        if st["credits"] < 0:
            flags.append("OVERSPENT")
        if load_mw > out:
            flags.append("OVER POWER")
        r = ref.get(n)
        ratio = f"{fwd / r:7.2f}" if r else "      -"
        if r and not 0.75 <= fwd / r <= 1.33:
            flags.append(f"DPS {fwd:.0f} vs enemy reference {r:.0f}")
        print(f"{n:02d}  | {level_budget:6.0f} | {st['credits'] + spent_here:12.0f} | {spent_here:10.0f} | "
              f"{st['credits']:4.0f} | {load_mw:4.1f}/{out:<3g} | {fwd:7.1f} | {(r or 0):7.0f} | {ratio} | "
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
                line = f"      enemies: bounty total {bounty:.0f} vs budget {level_budget:.0f}; slowest TTK {worst[0]} {worst[1]:.1f} s"
                if lvl.get("boss"):
                    b = lvl["boss"]
                    line += f"; boss {b} TTK {enemies['enemies'][b]['hp'] / max(fwd, 1e-6):.0f} s"
                print(line)
    if not enemies:
        print("\nEnemy hook: design/enemies/balance-data.json not found — TTK and bounty checks skipped "
              "(see the format in this script's docstring).")
    print("\nProblems: " + ("none" if not problems else "\n  " + "\n  ".join(problems)))
    return 1 if problems else 0


def weapons_dump(data):
    for slug, w in data["weapons"].items():
        print(f"\n{w['name']} ({slug})")
        for r in level_rows(w):
            print("  " + " | ".join(r))


def main():
    data = load_all()
    if "--weapons" in sys.argv:
        weapons_dump(data)
        return 0
    return report(data)


if __name__ == "__main__":
    sys.exit(main())
