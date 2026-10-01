---
title: Player
design: draft
implementation: not-started
art: proposed
depends-on: [../systems, ../ui/hangar]
updated: 2026-10-01
---

# Player

## Summary

The player flies the AF-12 Stormhawk interceptor as "Lancer" of Aegis Wing. The ship has a
fixed hull with Tyrian-style loadout slots. Between levels the player fills those slots in the
[hangar](../ui/hangar/README.md) with credits earned in combat, and the power budget of the
generator limits what can be fitted at the same time.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [ship](ship/README.md) | AF-12 Stormhawk: movement, hitbox, sprite and animation requirements | draft | not-started | chosen |
| [weapons](weapons/README.md) | Front, rear and wing-mount weapons, traits, power draw, costs, upgrades | draft | not-started | proposed |
| [generator](generator/README.md) | Power output that limits the loadout; spare power boosts shield regen | draft | not-started | none |
| [shields](shields/README.md) | Regenerating energy shield, capacity, regen and delay | draft | not-started | none |
| [armor](armor/README.md) | Non-regenerating hull points, plating upgrades, repairs | draft | not-started | none |
| [wingmen](wingmen/README.md) | AI wingman Rook (escort slot) and drones (wing mounts) | draft | not-started | proposed |
| [specials](specials/README.md) | Special abilities: airstrike, smart bomb, EMP, orbital lance, … | draft | not-started | proposed |
| [systems](systems/README.md) | Engines and utility-bay modules: magnet, sensors, auto-repair, … | draft | not-started | none |

## Design

### Loadout

```
                 ┌─────────────┐
                 │  FRONT GUN  │   1 front weapon (always fitted)
                 └──────┬──────┘
   ┌──────────┐   ╱▔▔▔▔▔▔▔▔▔╲   ┌──────────┐
   │ L WING   │──│ STORMHAWK │──│  R WING  │   2 wing mounts: pods, bomb racks, light drones
   └──────────┘   ╲▁▁▁▁▁▁▁▁▁╱   └──────────┘
                 ┌──────┴──────┐
                 │  REAR GUN   │   1 rear weapon (optional)
                 └─────────────┘
  ─────────────────────────────────────────────────
  Core        Generator · Shield · Armour · Engine        (always fitted, upgraded)
  Special     1 special ability (charges or cooldown)
  Utility     2 bays (3rd bay bought from act 3): magnet, sensors, auto-repair, …
  Escort      1 escort slot, unlocked in act 2: wingman Rook or a heavy drone
```

### Slot rules

- Every slot holds one item. The front gun and all core components are always fitted: they can
  be swapped or upgraded but never left empty.
- Wing mounts are independent: the same pod type in both mounts is allowed and common.
- One button fires all weapons (front, rear and wing). The special has its own button.
- Selling or unfitting an item keeps it in the **inventory** at no cost. Owned items can be
  refitted for free at any time in the hangar. See [economy](../systems/economy/README.md) for
  sell-back.
- An item can only be fitted when the projected **power load** does not exceed the generator
  output (see below). The hangar prevents over-budget fits and shows the missing MW.
- **Availability** in the component tables (`start`, `act N`) means: in the shop from the first
  hangar visit of that act; `act 1` items appear from the visit before L02, `act 2` items from
  the visit before L08, and so on. Weapons and specials use exact levels (`Lnn`).
- All balancing numbers for Acts 1–2 live in [balance-data.json](balance-data.json); check them
  with `python3 tools/balance.py` (see [weapons](weapons/README.md#data-and-balancing)).

### Power budget

Every fitted weapon, shield, engine and utility module has a **power draw** in MW. Draw rises
with the upgrade level. The sum of the draws is the **load**, and it may not exceed the
generator's **output**. Armour, specials and the escort slot draw no power.

Spare power is not wasted: each spare MW gives **+10 % shield regeneration** (max +50 %), so
keeping some headroom is a real choice.

Worked example (first-draft numbers), start of act 2:

| Slot | Item | Draw |
|---|---|---|
| Generator | Mk II | output **11 MW** |
| Front | Scatter Vulcan L2 | 3.5 |
| Rear | Tail Gun L1 | 1 |
| L wing | Micro-missile pod L1 | 1 |
| R wing | Micro-missile pod L1 | 1 |
| Shield | Mk I | 2 |
| Engine | Mk I | 0 |
| Utility | Sensor suite L1 | 1 |
| | **Load** | **9.5 / 11** → 1.5 MW spare → +15 % shield regen |

Upgrading the Vulcan to L3 (+0.5) fits. Swapping the Tail Gun for a Rear Laser (3 MW) would
reach 11.5 and is refused until the generator goes to Mk III.

### In-level pickups

Upgrades are bought, not found. In-level pickups give credits and short-term help, so a good
loadout stays the main source of strength:

| Pickup | Source | Effect |
|---|---|---|
| Salvage (small / medium / large) | Enemy drops, crates | +10 / +50 / +200 credits (before difficulty multiplier) |
| Overdrive | Carriers, crates (~2 per level) | All weapons +1 level for 20 s (L5 becomes a stronger "L6" pattern) |
| Shield cell | Frequent drops | Restores 25 % of shield capacity |
| Armour patch | Rare (0–2 per level) | Restores 10 armour points |
| Special charge | Rare | +1 charge (charge-based specials only) |
| Data core | Hidden / secret areas | Lore entry and unlocks an item in the shop catalogue; counts for the grade |

A [pickup magnet](systems/README.md) widens the collection radius. Uncollected pickups drift
down and leave the screen after 6 s.

### Damage scale (first draft)

Used by enemies and weapons so numbers stay comparable:

- Player: starts with 20 shield and 60 armour.
- Enemy bullets: small 5, medium 10, heavy 20. Collision 15–40 by enemy size.
- Enemy HP: popcorn 5–10, medium 30–60, heavy 150–400, mini-boss ~1500, act boss 5 000–20 000.
- Starter Pulse Cannon L1: about 20 DPS. A well-upgraded late-game loadout: about 250 DPS.

## Concept art

Concept [round 09](../concept-rounds/round-09/README.md) — generator `tools/concept/vfx_r09.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/pickups-r09-a.png](concept/pickups-r09-a.png) | The 8 pickups as small 3D models with pulsing outline and halo, rocking so they never read as needles; colour and greyscale bullet-confusion test (sheet) | proposed |
| [concept/pickups-r09-a.gif](concept/pickups-r09-a.gif) | The 8 pickups as small 3D models with pulsing outline and halo, rocking so they never read as needles; colour and greyscale bullet-confusion test (motion) | proposed |

## Implementation

- [ ] Loadout data model: slots, fitted items, inventory, upgrade levels
- [ ] Power load calculation and over-budget refusal
- [ ] Spare-power shield regen bonus
- [ ] Single fire button fires all weapons; special on a separate button
- [ ] Pickup types, drop tables and 6 s despawn
- [ ] Overdrive: temporary +1 weapon level with HUD timer

## Open questions

- None open.

## Decisions

- 2026-09-30: Loadout = front, rear, 2 wing mounts, generator, shield, armour, engine, special,
  utility bays, escort slot.
- 2026-09-30: Static power budget with spare-power bonus rather than a draining energy pool
  (easier to read in the hangar; the choice happens before the level).
- 2026-09-30: Upgrades are bought only; in-level pickups are credits and temporary boosts.
- 2026-10-01: Controls: hold-to-fire with an auto-fire toggle in Options; fire, special and hold-for-precision buttons (see [controls](../ui/controls/README.md)). Rook takes the separate escort slot (confirmed). Utility bays: two, a third buyable. One special equipped at a time.
- 2026-10-01: Component availability `act N` defined as the first hangar visit of that act (`act 1` = before L02); balancing numbers centralised in balance-data.json.
