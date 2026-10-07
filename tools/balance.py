#!/usr/bin/env python3
"""The balancing sheet for the player's equipment (Acts 1-2).

The numbers come from the parts' data files (design/player/**/data.yaml, the economy, the
difficulty levers, the enemies and the levels); the expected purchases per hangar visit are in
design/player/balance-plan.yaml. The weapon tables are rendered by tools/sync_tables.py.

The checks themselves are content tests (design/tech/architecture, balance checks as JUnit tests):
BalanceTest (this sheet's plan, DPS and enemy checks) and ActPlaythroughTest (Act 1 flown with the
plan on every difficulty). This script prints the same sheet for reading:

Usage:
  python3 tools/balance.py              the sheet per visit, levels 01-14, and the enemy checks
  python3 tools/balance.py --weapons    print every weapon's per-level numbers

Per visit: the income of the level before it (the level's typical haul from its credit-budget
table where the level has data, else budget(n)), the plan's purchases (with the free charges a
special gives at its unlock), the repair estimate, the power load and the plan's forward
single-target DPS against the reference DPS the enemy stat blocks assume (design/enemies/data.yaml),
inside 0.75-1.33x; Levels 01-03's gentle onboarding (up to 1.75x) is the accepted exception
(user decision 2026-10-01). Per level with data: the slowest time to kill among its enemies and its
boss's at the plan's DPS. Per enemy: the time to kill at its first level and the bounty against
the balancing basis (design/enemies/README.md); a deviation is listed, marked when it is an
accepted exception (the Coilwyrm's bounty, user decision 2026-10-05); BalanceTest holds the same
ACCEPTED list and the ones pending a decision.

Exit status: 1 when the plan overspends, exceeds the power, buys an item before its unlock or
over a special's most charges, or the DPS leaves its band (other than the accepted exception).
"""
import re
import sys

from design_data import DESIGN, draw, enemy_dir, load, stats, upgrade_costs, weapons
from sync_tables import credit_budget, level_rows

ONBOARDING = {1, 2, 3}           # accepted: about 1.5x the reference DPS (design/enemies, Balancing basis)
DPS_BAND = (0.75, 1.33)
ONBOARDING_HIGH = 1.75
EFFECTIVE = 0.6                  # bosses and set pieces: effective DPS 0.6 x reference
SHARE_TOLERANCE = 1 / 3          # a boss's or set piece's bounty share: "about" its target
# Deviations from the balancing basis the user accepted (BalanceTest.ACCEPTED holds the same list).
ACCEPTED = {
    ("coilwyrm", "bounty"): "a multi-part enemy, cutting it up is extra work; a head-first kill pays 40",
    ("ravager", "ttk"): "a fast, fragile pack hunter (user decision 2026-10-07, M5 part C)",
}
HOLD_SHARE = 0.6                 # a hold's cluster dies in at most this share of its window (BalanceTest, medium)


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
        "wingmen": load("player/wingmen/data.yaml"),
    }


def budget(data, n):
    b = data["economy"]["budget"]
    return b["base"] * b["growth"] ** (n - 1)


def level_dir(n):
    """The level's directory relative to design/, or None while it has no data file."""
    found = list((DESIGN / "campaign").glob(f"act-*/level-{n:02d}-*/data.yaml"))
    return found[0].parent.relative_to(DESIGN).as_posix() if found else None


def typical_haul(d):
    """The typical haul of the credit-budget table rendered from the level's data."""
    total = next(line for line in credit_budget(d).splitlines() if line.startswith("| **Total**"))
    return int(re.findall(r"\*\*([\d,]+)\*\*", total)[-1].replace(",", ""))


def income(data, n):
    """(credits the level earns, whether it is the level's typical haul)."""
    d = level_dir(n)
    return (typical_haul(d), True) if d else (round(budget(data, n)), False)


def apply_plan(data):
    """Yield (level, state) for levels 01-14 following the expected-loadout plan."""
    plan, W = data["plan"], data["weapons"]
    state = {"credits": data["economy"]["starting_credits"], "spent": 0, "income": 0, "slots": {},
             "invested": {}, "core": {}, "utility": {}, "charges": {}, "log": [], "earned": None}

    def pay(amount, what, slot=None):
        state["credits"] -= amount
        state["spent"] += amount
        state["log"].append(f"{what} ({amount})")
        if slot:
            state["invested"][slot] = state["invested"].get(slot, 0) + amount

    for n in range(1, 15):
        key = f"{n:02d}"
        state["log"] = []
        # the free charges a special gives once at its unlock, as the hangar opens
        for name, sp in data["specials"].items():
            if sp["unlock"] == n and sp.get("free_charges"):
                state["charges"][name] = state["charges"].get(name, 0) + sp["free_charges"]
                state["log"].append(f"{sp['free_charges']}× {name} charge free")
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
            if slot == "escort":  # Rook's gun (M5 part C): price factor × its base weapon's price, no power
                gun = escort_gun(data, slug)
                pay(round(data["wingmen"]["guns"]["price_factor"] * W[gun["base"]]["price"]),
                    f"buy Rook's {gun['name']}", slot)
                state["slots"][slot] = [slug, 1]
                continue
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
            state["charges"][name] = state["charges"].get(name, 0) + count
            if state["charges"][name] > sp["max_charges"]:
                state["log"].append(f"!! {name}: {state['charges'][name]} charges, at most {sp['max_charges']}")
            pay(sp["charge_price"] * count, f"{count}× {name} charge")
        if n > 1:
            pay(plan["repair_points_per_level"] * data["repair_cost"], "repairs (estimate)")
        yield n, state
        earned, haul = income(data, n)
        state["earned"] = (earned, haul)
        state["credits"] += earned
        state["income"] += earned


def escort_gun(data, gun_id):
    return next(g for g in data["wingmen"]["guns"]["list"] if g["id"] == gun_id)


def loadout_numbers(data, state):
    W, core = data["weapons"], data["core"]
    load_mw, fwd, rear, volley = 0.0, 0.0, 0.0, 0.0
    for slot, (slug, lvl) in state["slots"].items():
        if slot == "escort":
            continue
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


# --- enemies ----------------------------------------------------------------------------------

def enemy(slug):
    return load(f"{enemy_dir(slug)}/data.yaml")


def enemy_slugs():
    return sorted(p.parent.name for p in (DESIGN / "enemies").glob("*/*/data.yaml"))


def level_enemies(d):
    """The enemies a level uses: waves (incl. a spawner's brood), ground units, set pieces, its boss."""
    level = load(f"{d}/data.yaml")
    slugs = set()
    for wave in level.get("waves", []):
        for g in wave.get("groups") or [wave]:
            slugs.add(g["enemy"])
    slugs.update(g["enemy"] for g in level.get("ground_targets", []) if "enemy" in g)
    slugs.update(p["enemy"] for p in level.get("set_pieces", []))
    for s in list(slugs):
        slugs.update(a["spawn"]["enemy"] for a in enemy(s).get("attacks", []) if a.get("spawn"))
    boss = level.get("boss", {}).get("enemy")
    return sorted(slugs - {boss}), boss


def boss_or_set_piece(e):
    return "boss" in e or e["tier"] == "huge"


def ttk_problem(e, ref):
    """The time to kill at the first level against the balancing basis, or None (see BalanceTest)."""
    r = ref.get(e["first_level"])
    if r is None:
        return None
    if e["tier"] == "tiny" and "boss" not in e:
        shot = load("player/weapons/pulse-cannon/data.yaml")["levels"][0]["damage"]
        return None if e["hp"] <= shot else f"HP {e['hp']}: not one hit of the starting gun ({shot})"
    if "boss" in e:
        lo, hi = (45, 75) if e["boss"]["kind"] == "mid-boss" else (90, 180)
    else:
        lo, hi = {"small": (0, 0.3), "medium": (0.4, 1.5), "large": (1, 3), "huge": (20, 40)}[e["tier"]]
    dps = r * (EFFECTIVE if boss_or_set_piece(e) else 1)
    if e["hp"] < lo * dps - 1 or e["hp"] > hi * dps + 1:  # whole HP: 1 HP of rounding
        return f"HP {e['hp']:g} / {dps:.1f} DPS = {e['hp'] / dps:.2f} s, target {lo:g}-{hi:g} s"
    return None


def bounty_problem(data, e):
    """The bounty against its class (Act 1 terms) or, for bosses and set pieces, its share of the budget."""
    if boss_or_set_piece(e):
        share = (0.15 if e["boss"]["kind"] == "mid-boss" else 0.30) if "boss" in e else 0.15
        d = level_dir(e["first_level"])
        if not d:
            return None
        # Data holds Act 1 terms; the act factor applies to every payout, a boss's included (no exemption).
        act = int(d.split("/")[1].split("-")[1])
        paid = (e["bounty"] * data["economy"]["act_factor"] ** (act - 1)
                * load(f"{d}/data.yaml").get("bounty_scale", 1))
        actual = paid / budget(data, e["first_level"])
        if abs(actual - share) > share * SHARE_TOLERANCE:
            return (f"{paid:.0f} paid = {100 * actual:.1f} % of L{e['first_level']:02d}'s budget, "
                    f"target about {100 * share:.0f} %")
        return None
    hardened = e.get("armour") == "hardened"
    lo, hi = {"tiny": (2, 5), "small": (10, 15), "medium": (40, 60) if hardened else (18, 30),
              "large": (40, 60)}[e["tier"]]
    return None if lo <= e["bounty"] <= hi else f"{e['bounty']} outside the {e['tier']} class {lo}-{hi}"


ACT_2 = range(8, 15)  # Levels 08-14


def act_hp_report(ref):
    """The act HP factor (design/enemies, Balancing basis; D5 = c of M5 part A): every returning
    medium-or-larger unit's HP (parts summed, medium) and time to kill at the Act 2 levels after its
    first; BalanceTest checks the same."""
    print("\nReturning units in Act 2 (act HP factor = reference DPS at the level / at the first level):")
    print("Enemy            | Tier     | First | " + " | ".join(f"L{n:02d} HP / TTK" for n in ACT_2))
    for slug in enemy_slugs():
        e = enemy(slug)
        if "boss" in e or e["tier"] in ("tiny", "small", "huge") or e["first_level"] not in ref:
            continue
        cells = []
        for n in ACT_2:
            if n <= e["first_level"] or n not in ref:
                cells.append(f"{'-':>12}")
                continue
            hp = e["hp"] * ref[n] / ref[e["first_level"]]
            cells.append(f"{hp:5.0f} {hp / ref[n]:4.2f} s")
        print(f"{e['name']:16} | {e['tier']:8} | L{e['first_level']:02d}   | " + " | ".join(cells))


def enemy_report(data, ref):
    print("\nEnemies (balancing basis, at the first level):")
    print("Enemy            | Tier     | First | HP     | TTK at ref (s) | Bounty | Deviation")
    deviations = 0
    for slug in enemy_slugs():
        e = enemy(slug)
        r = ref.get(e["first_level"])
        dps = r * (EFFECTIVE if boss_or_set_piece(e) else 1) if r else None
        found = [(kind, p) for kind, p in (("ttk", ttk_problem(e, ref)), ("bounty", bounty_problem(data, e))) if p]
        problems = [p + (f" (accepted: {ACCEPTED[slug, kind]})" if (slug, kind) in ACCEPTED else "")
                    for kind, p in found]
        deviations += sum((slug, kind) not in ACCEPTED for kind, _ in found)
        kind = e["boss"]["kind"] if "boss" in e else e["tier"]
        print(f"{e['name']:16} | {kind:8} | L{e['first_level']:02d}   | {e['hp']:6g} | "
              f"{(e['hp'] / dps) if dps else 0:14.2f} | {e['bounty']:6} | " + "; ".join(problems))
    if deviations:
        print("A deviation fails BalanceTest unless it is listed there as accepted or pending a decision.")


# --- report -----------------------------------------------------------------------------------

def report(data):
    ref = load("enemies/data.yaml")["reference_dps"]
    print("Lvl | Budget | Before visit | Spent here | Left | Load/Out | Fwd DPS | Ref DPS | Fwd/Ref | Rear DPS "
          "| Volley DPS | Purchases")
    problems, accepted, ttk_lines = [], [], []
    prev_spent = 0
    for n, st in apply_plan(data):
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
        if r and not DPS_BAND[0] <= fwd / r <= DPS_BAND[1]:
            if n in ONBOARDING and DPS_BAND[1] < fwd / r <= ONBOARDING_HIGH:
                accepted.append(f"L{n:02d}: DPS {fwd:.0f} vs enemy reference {r:.0f} ({fwd / r:.2f}×), "
                                "gentle onboarding (user decision 2026-10-01)")
            else:
                flags.append(f"DPS {fwd:.0f} vs enemy reference {r:.0f}")
        print(f"{n:02d}  | {budget(data, n):6.0f} | {st['credits'] + spent_here:12.0f} | {spent_here:10.0f} | "
              f"{st['credits']:4.0f} | {load_mw:4.1f}/{out:<3g} | {fwd:7.1f} | {(r or 0):7.0f} | {ratio} | "
              f"{rear:8.1f} | {volley:10.1f} | "
              + "; ".join(st["log"]) + (("  <-- " + ", ".join(flags)) if flags else ""))
        for f in flags:
            problems.append(f"L{n:02d}: {f}")
        problems += [f"L{n:02d}: {m[3:]}" for m in st["log"] if m.startswith("!!")]
        d = level_dir(n)
        if d:
            slugs, boss = level_enemies(d)
            slowest = max(slugs, key=lambda s: enemy(s)["hp"])
            line = (f"L{n:02d}: typical haul {income(data, n)[0]} of budget {budget(data, n):.0f}; "
                    f"slowest TTK at the plan's DPS {enemy(slowest)['name']} {enemy(slowest)['hp'] / fwd:.1f} s")
            if boss:
                line += f"; boss {enemy(boss)['name']} {enemy(boss)['hp'] / (EFFECTIVE * fwd):.0f} s (at 0.6×)"
            ttk_lines.append(line)
            ttk_lines += hold_lines(data, d, st)
    print("\nBefore visit: the credits after the level before it, which earns its typical haul where the "
          "level has data (its credit-budget table), else budget(n).")
    print("\nLevels with data:")
    for line in ttk_lines:
        print("  " + line)
    hard = data["plan"].get("difficulties", {})
    if hard:
        print("\nPlan additions on other difficulties (ActPlaythroughTest): "
              + "; ".join(f"{diff} L{lvl}: " + ", ".join(f"{action} {items}" for action, items in step.items())
                           for diff, steps in hard.items() for lvl, step in steps.items()))
    enemy_report(data, ref)
    act_hp_report(ref)
    print("\nAccepted: " + ("none" if not accepted else "\n  " + "\n  ".join(accepted)))
    print("\nProblems: " + ("none" if not problems else "\n  " + "\n  ".join(problems)))
    return 1 if problems else 0


def anti_ground_dps(data, state):
    """The single-target DPS of the fit's anti-ground sources: its weapons and Rook's gun (his gun's
    scale × its base weapon); the only damage a hardened unit takes (the specials aside)."""
    W, dps = data["weapons"], 0.0
    for slot, (slug, lvl) in state["slots"].items():
        if slot == "escort":
            gun = escort_gun(data, slug)
            w = W[gun["base"]]
            if "anti-ground" in w.get("traits", []):
                dps += gun["scale"] * stats(w, w["levels"][lvl - 1])[1]
        elif "anti-ground" in W[slug].get("traits", []):
            dps += stats(W[slug], W[slug]["levels"][lvl - 1])[1]
    return dps


def hold_lines(data, d, state):
    """A level's hold zones (M5 part C): each cluster's HP against the fit's anti-ground DPS and the
    hold's window at medium (from the trigger until its first unit's far edge leaves the bottom
    edge: the ease, then the hold speed); BalanceTest checks HP / DPS <= 0.6 x window."""
    level = load(f"{d}/data.yaml")
    lines = []
    dps = anti_ground_dps(data, state)
    for i, hold in enumerate(level.get("holds", [])):
        units = [g for g in level["ground_targets"] if g.get("group") in hold["groups"]]
        hp = sum(enemy(g["enemy"])["hp"] * len(g["at"]) for g in units)
        half = max(enemy(g["enemy"])["hitbox"][1] / 2 for g in units)
        ease = hold["ramp"] * (level["scroll_speed"] + hold["speed"]) / 2
        window = hold["ramp"] + (540 + half - hold["y"] - ease) / hold["speed"]
        ttk = hp / dps if dps else float("inf")
        lines.append(f"  hold {i + 1} ({', '.join(hold['groups'])}): {hp:g} HP / anti-ground {dps:.1f} DPS = "
                     f"{ttk:.1f} s of a {window:.1f} s window ({ttk / window:.2f}, at most {HOLD_SHARE})")
    return lines


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
