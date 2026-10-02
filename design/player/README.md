---
title: Player
design: approved
implementation: in-progress
art: chosen
depends-on: [../systems, ../ui/hangar]
updated: 2026-10-02
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
| [ship](ship/README.md) | AF-12 Stormhawk: movement, hitbox, sprite and animation requirements | approved | in-progress | final |
| [weapons](weapons/README.md) | Front, rear and wing-mount weapons, traits, power draw, costs, upgrades | approved | done | chosen |
| [generator](generator/README.md) | Power output that limits the loadout; spare power boosts shield regen | approved | in-progress | none |
| [shields](shields/README.md) | Regenerating energy shield, capacity, regen and delay | approved | in-progress | none |
| [armor](armor/README.md) | Non-regenerating hull points, plating upgrades, repairs | approved | in-progress | none |
| [wingmen](wingmen/README.md) | AI wingman Rook (escort slot) and drones (wing mounts) | approved | not-started | chosen |
| [specials](specials/README.md) | Special abilities: airstrike, smart bomb, EMP, orbital lance, … | approved | not-started | chosen |
| [systems](systems/README.md) | Engines and utility-bay modules: magnet, sensors, auto-repair, … | approved | not-started | none |

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
- All balancing numbers for Acts 1–2 live in the parts' `data.yaml` files (this directory's
  [data.yaml](data.yaml) holds availability and the pickups); check them with
  `python3 tools/balance.py` (see [weapons](weapons/README.md#data-and-balancing)).

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

<!-- data: pickups -->
| Pickup | Source | Effect |
|---|---|---|
| Salvage (small / medium / large) | Enemy drops, crates | +10 / +50 / +200 credits (before difficulty multiplier) |
| Overdrive | Carriers, crates (~2 per level) | All weapons +1 level for 20 s (L5 becomes a stronger "L6" pattern) |
| Shield cell | Frequent drops | Restores 25 % of shield capacity |
| Armour patch | Rare (0–2 per level) | Restores 10 armour points |
| Special charge | Rare | +1 charge (charge-based specials only) |
| Data core | Hidden / secret areas | Lore entry; unlocks one specific shop item one act early (list in [economy](../systems/economy/README.md#data-cores)); counts for the grade |
<!-- /data -->

A [pickup magnet](systems/README.md) widens the collection radius. Uncollected pickups drift
down and leave the screen after 6 s.

### Damage scale

The player starts with 20 shield and 60 armour; the starter Pulse Cannon L1 does about 20 DPS
(a Pulse Cannon L1 shot does 2 damage units, 10 shots per second). Damage to the player (bullet classes, lasers,
contact by size), enemy HP and the reference player DPS per level are owned by the enemies'
[balancing basis](../enemies/README.md#balancing-basis); this document does not repeat them.

## Concept art

Concept [round 09](../concept-rounds/round-09/README.md) — generator `tools/concept/vfx_r09.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/pickups-r09-a.png](concept/pickups-r09-a.png) | The 8 pickups as small 3D models with pulsing outline and halo, rocking so they never read as needles; colour and greyscale bullet-confusion test (sheet) | chosen |
| [concept/pickups-r09-a.gif](concept/pickups-r09-a.gif) | The 8 pickups as small 3D models with pulsing outline and halo, rocking so they never read as needles; colour and greyscale bullet-confusion test (motion) | chosen |

Production art for concept round 12 (the Level 01 batch; part P2 opens the round), review files built from the final frames in `assets/` by `tools/art/pickups.py` (`--review` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/pickups-final-r12-a.png](concept/pickups-final-r12-a.png) | Final 8-frame loops of salvage S, the shield cell, the armour patch and the hidden crate (salvage L, its cyan cross on every face it turns to the viewer), with the pulsing light outline and halo | chosen |
| [concept/pickups-final-r12-a.gif](concept/pickups-final-r12-a.gif) | The four loops at 10 fps | chosen |

Production art for concept round 15 (M4 part B, the Level 02 batch), by `tools/art/pickups.py salvage-medium overdrive`.

| File | What | Status |
|---|---|---|
| [concept/pickups-final-r15-a.png](concept/pickups-final-r15-a.png) | Final 8-frame loops of salvage M (three credit chips) and the overdrive, rocking, with the pulsing light outline and halo of the Level 01 pickups | chosen |
| [concept/pickups-final-r15-a.gif](concept/pickups-final-r15-a.gif) | The two loops at 10 fps | chosen |

## Implementation

- [x] Loadout data model: slots, fitted items, inventory, upgrade levels
- [x] Power load calculation and over-budget refusal
- [x] Spare-power shield regen bonus
- [x] Single fire button fires all weapons
- [ ] Special on a separate button — **later: M4** (the specials, part D)
- [x] Pickup types, drop tables and 6 s despawn: salvage S and M, overdrive, shield cell, armour patch, the hidden crate
- [ ] Salvage L, special charge and data core — **later: M4** (Levels 03, 04 and 06)
- [x] Overdrive: temporary +1 weapon level with HUD timer (the overdrive pickup itself: *Pickup types* above)

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
- 2026-10-01: Concept round 09: pickups chosen.
- 2026-10-01: Data core pickup now links to the data-core unlock table in economy (each core unlocks one specific item one act early).
- 2026-10-01: Damage scale: the old first-draft numbers (bullets 5/10/20, collisions 15–40, HP ranges) contradicted the enemies balancing basis (4/6/10, contact 6–25); replaced by a link to it, which owns them.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../reviews/acts-1-2/README.md).
- 2026-10-01: Damage-unit wording corrected (user decision): a Pulse Cannon L1 shot does 2 damage units, as in `balance-data.json` and the reference DPS of 20; all HP values already used this scale.
- 2026-10-02: Balancing numbers moved from `balance-data.json` into the parts' `data.yaml` files (M2 data files). This directory's [data.yaml](data.yaml) holds the shop availability and the pickups (the *In-level pickups* table is rendered from it); the expected purchases that `tools/balance.py` checks are in [balance-plan.yaml](balance-plan.yaml).
- 2026-10-02: M2 (Level 01): salvage (small), shield cell, armour patch and the hidden crate
  drop, drift down at 40 px/s (the chosen pickups concept's rule, now `pickup_drift_speed` in
  [data.yaml](data.yaml)), blink in their last 1.5 s and are gone after 6 s; they are collected
  within the ship's 36 px collection radius. Drop tables: the enemy stat blocks' `drops` and the
  levels' carried pickups. The other pickups follow with their levels, so the item stays open.
  Placeholders: the spin loops of [pickups-r09-a](concept/pickups-r09-a.png) (salvage L stands in
  for the hidden crate).
- 2026-10-02: M3 part B2 (`vanguard.content.campaign`: `ItemKind`, `Gear`, `Catalogue`,
  `Hangar`): the loadout per slot with upgrade levels, the inventory by kind, the special charges,
  the power load (the fitted items' draws; a weapon's draw per level linear from L1 to L5, rounded
  half up to 0.5 MW) against the generator's output, refused fits with the missing MW, buys that
  go to the inventory when they do not fit, free refits, and the always-fitted front gun and core
  parts. The sim flies the Pulse Cannon at its level, the shield, the plating and the engine;
  the other weapons, the specials and the utility modules are bought, fitted and saved and fly
  from M4 (`Flight`). The spare-power shield regen bonus is not applied in flight yet (M4), so
  the hangar does not show it.
- 2026-10-02: Production art (Level 01 batch, `tools/art/pickups.py`): the four pickups Level 01 uses, every frame its own 8× render of the round-09 model with one palette per loop; the 1 px light outline is opaque and pulses in brightness, the halo is stepped to four translucency levels. Same sizes as the placeholders. Review files proposed for round 12.
- 2026-10-02: Concept round 12 closed (user decision): the four pickups' production loops approved as **final**; this doc's `art` stays `chosen`, since its other art is still concept art.
- 2026-10-02: M4 part A: every fitted weapon flies, each mount on its own clock while fire is held; the spare power (the generator's output less every fitted item's draw) raises the shield regen in flight, +10 % per MW up to +50 % (the starter fit has 4 MW spare: +40 %), and the HUD's power row shows it; an overdrive switches every weapon to its next level's pattern (L5: the overdrive pattern) for its time, timed on the HUD. The overdrive pickup lands with Level 02 (part B).
- 2026-10-02: M4 part B: the overdrive and salvage M pickups (Level 02), their production loops proposed in concept round 15.
- 2026-10-02: Concept round 15 closed (user decision): salvage M and the overdrive approved as **final**; this doc's `art` stays `chosen`.
