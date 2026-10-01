---
title: Hangar
design: draft
implementation: not-started
art: chosen
depends-on: [../../player, ../../systems/economy, ../../campaign]
updated: 2026-10-01
---

# Hangar

## Summary

The ship configurator between levels. The player reads the intel on the next level, buys,
sells, swaps and upgrades equipment within the generator's power budget, repairs armour,
saves, and launches. The intel panel and the shop are linked: weapons whose traits match
the next level's threat profile are marked.

## Design

### Layout (960×540; columns ≈ 300 / 370 / 290 px)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ [INTEL] [SHOP] [LOADOUT] [REPAIR] [SAVE]              CR 12 450   ► LAUNCH   │
├────────────────────────┬──────────────────────────────┬──────────────────────┤
│  LOADOUT               │ SHOP · FRONT GUNS            │ INTEL · MISSION 15   │
│                        │                              │ ┌──────┐ DR. VARGA   │
│      [Scatter L3]      │ ► Scatter Vulcan  L3 owned   │ │ PORT │ "Burrowers  │
│  [Missile] ▲ [Missile] │   Lance Laser     2 500 ◆    │ │ RAIT │  under the  │
│   L2      ███      L2  │   Hornet Launcher 2 000      │ └──────┘  slopes..." │
│      [Tail Gun L1]     │   Hammer Mortar   1 500 ◆    │ Setting  Mars descent│
│                        │   Ion Beam   NEW  5 000      │ Layers   ground, air │
│  GEN  Mk III  14 MW    │ ───────────────────────────  │ From     front 100 % │
│  SHD  Mk II            │ Hammer Mortar L1             │ Density  ███░░ 3     │
│  ARM  Comp I  64/80    │ anti-ground, area            │ Hazards  burrowers   │
│  ENG  Mk I             │ DPS  25 (ground)   Draw 3    │ Boss     none        │
│  SPC  Airstrike ×2     │ vs fitted: DPS −10 air,      │ Recommend            │
│  UTL  Sensor L3, Magnet│      +25 ground              │ ◆ forward            │
│                        │ [BUY 1 500]  [TEST FIRE]     │ ◆ anti-ground        │
│ POWER ███████████░░ 12.5/14 MW   (+15 % shield regen)                        │
└────────────────────────┴──────────────────────────────┴──────────────────────┘
```

(Example: the hangar before level 15 *Olympus Descent* with a level-3 sensor suite; numbers are
illustrative and come from [player](../../player/README.md).)

### Panels

- **Loadout** (left): the ship diagram with every slot. Select a slot to filter the shop to
  items that fit it. Core stats underneath. The **power bar** at the bottom shows the current
  load and, while browsing, the projected load in a lighter colour; it turns red when an item
  would not fit.
- **Shop** (centre): list for the selected slot (owned items first, then buyable, then locked
  with their unlock hint). ◆ markers show how many of the item's traits match the intel
  recommendation. A **NEW** tag marks items that entered the shop since the last visit; the
  unlocks follow the campaign's
  [trait pacing table](../../campaign/README.md#loadout-pressure-and-shop-unlock-pacing). Item details show traits, DPS, draw, price, and a **comparison with the
  fitted item** (green/red deltas).
- **Test fire**: a small looping preview box showing the weapon's pattern at the current level
  against dummy targets. Quick way to understand spread, rear and side weapons.
- **Intel** (right): the next level's threat profile, voiced by Dr. Varga. How much is shown
  depends on the sensor suite level, see [ship systems](../../player/systems/README.md#sensor-levels-and-hangar-intel).
  Fields come from the level's threat profile (see the
  [level template](../../campaign/README.md#level-document-template)): setting, dominant layers,
  attack directions with the share of waves per direction (e.g. "front 60 %, rear 40 %"),
  density (1–5), hazards, special availability (e.g. "No air support under the ice"), boss,
  recommended weapon traits.
- **Repair**: armour repair per point or "repair all", with cost. Also Rook's repairs.
- **Save**: slot list, see [saves](../../systems/saves/README.md).
- **Launch**: confirmation if the loadout lacks a recommended trait ("No anti-sub weapon
  fitted. Launch anyway?") or armour is below 50 %.

### Transactions

Buying fits the item immediately when the power budget allows, otherwise it goes to the
inventory. Undo within the visit refunds 100 %, see
[economy](../../systems/economy/README.md#sell-back-and-undo).

## Concept art

Concept [round 06](../../concept-rounds/round-06/README.md) — full 960×540 screens at 1× plus a 2× detail crop; generator `tools/concept/ui_r06.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/rejected/hangar-r06-a.png](concept/rejected/hangar-r06-a.png) | Hangar A — three columns: loadout diagram, shop list with ◆ / NEW / locked and item detail with test fire, intel panel with Varga (before L15) | rejected — B layout preferred |
| [concept/hangar-r06-b.png](concept/hangar-r06-b.png) | Hangar B — central ship schematic with callouts to the slots, shop drawer left, intel with the large Varga portrait right | superseded by r07-b (glass style) |

Concept [round 07](../../concept-rounds/round-07/README.md) — layout B in the menu's glass style; generator `tools/concept/ui_r07.py`.

| File | What | Status |
|---|---|---|
| [concept/rejected/hangar-r07-a.png](concept/rejected/hangar-r07-a.png) | Hangar r07 A — glass panels over a pre-rendered hangar bay; the parked Stormhawk is the schematic, with slot callouts, module tiles, power bar, shop drawer, intel with Varga (before L15) | rejected — B backdrop preferred |
| [concept/hangar-r07-b.png](concept/hangar-r07-b.png) | Hangar r07 B — same layout over a darkened tactical map of Mars with the descent route; holographic blueprint in the centre | chosen |

## Implementation

- [ ] Tabs/panels as above with keyboard, gamepad and mouse navigation
- [ ] Slot selection filters the shop; owned/buyable/locked sorting
- [ ] Power bar with projected load and refusal
- [ ] Comparison deltas vs the fitted item
- [ ] Test-fire preview box — **after the first build** (keep the layout space)
- [ ] Intel panel from the level threat profile, gated by sensor level, with Varga's lines
- [ ] Trait-match markers in the shop
- [ ] Launch warnings (missing recommended trait, low armour)

## Open questions

- None open.

## Decisions

- 2026-09-30: The intel panel sits in the hangar itself, so the player sees it while shopping.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-10-01: Concept round 06: layout **B** (central ship schematic with the panels around it) chosen, but in the glass style of the menus, not metal — restyle in round 07. Layout A rejected.
- 2026-10-01: Concept round 07: hangar layout B in the glass style over the tactical map of Mars (r07-b) chosen; the hangar-bay backdrop (r07-a) rejected.
- 2026-10-01: Test fire comes after the first build; the layout keeps its space.
