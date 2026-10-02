---
title: Hangar
design: approved
implementation: in-progress
art: chosen
depends-on: [../../player, ../../systems/economy, ../../campaign]
updated: 2026-10-02
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
  fitted. Launch anyway?") or armour is below 50 %. The trait warning only uses what the intel
  shows: it needs the sensor level that reveals the recommended traits (L3, counting the easy
  bonus); below it there is no trait warning, so the launch never tells more than the intel.

### Transactions

Buying fits the item immediately when the power budget allows, otherwise it goes to the
inventory. Undo within the visit refunds 100 %, and so does selling an item bought during the
visit (purchase plus this visit's upgrades); items owned before the visit sell at the normal
sell-back, see [economy](../../systems/economy/README.md#sell-back-and-undo).

Swapping the plating keeps the damage: the missing armour points stay missing (Standard 41/60 →
Composite I 61/80), with at least 1 point left. Repair is a separate choice.

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
- [x] Slot selection filters the shop; owned/buyable/locked sorting
- [x] Power bar with projected load and refusal
- [x] Comparison deltas vs the fitted item
- [ ] Test-fire preview box — **after the first build** (keep the layout space)
- [ ] Intel panel from the level threat profile, gated by sensor level, with Varga's lines
- [x] Trait-match markers in the shop
- [x] Launch warnings (missing recommended trait, low armour)

## Open questions

Raised by the M3 part B2 build and deferred (user decision, 2026-10-02); each stays as built
until it is handled where stated.

- **Varga's lines** (M4 writing pass): "one line per intel item" has no text yet; the data holds
  one line per sensor level (Level 01: the no-sensor line) and the panel shows it with the hangar
  teaser. The per-item lines and the data that holds them are decided in the M4 writing pass.
- **Sensor L2 portraits and boss silhouette** (art track, from Level 02 on): built as the enemy
  and boss names; Level 02 is the first level whose intel needs them, and the art track decides
  the portraits and the silhouette.
- **Third utility bay** (M4 data): 5 000 in the economy text, from Act 3; it has no data entry and
  is not offered yet. Its data entry comes with M4.

## Decisions

- 2026-09-30: The intel panel sits in the hangar itself, so the player sees it while shopping.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-10-01: Concept round 06: layout **B** (central ship schematic with the panels around it) chosen, but in the glass style of the menus, not metal — restyle in round 07. Layout A rejected.
- 2026-10-01: Concept round 07: hangar layout B in the glass style over the tactical map of Mars (r07-b) chosen; the hangar-bay backdrop (r07-a) rejected.
- 2026-10-01: Test fire comes after the first build; the layout keeps its space.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-02: M3 part B1 placeholder (`vanguard.game.screen.HangarScreen`, marked as such on screen;
  the design here is part B2): one glass panel with the credits, the loadout, the armour, the next
  mission with its hangar teaser, difficulty, score, playtime and retries, and Launch (disabled
  while the next level is not built), Save game (the slot list) and Main menu (confirmation). It
  autosaves when it opens and plays the hangar theme. The real hangar replaces the class behind the
  same constructor; every way into it (intro briefing, debrief, Back to hangar, Abort, Load,
  Continue) already passes the campaign.
- 2026-10-02: M3 part B2 (`vanguard.game.screen.HangarScreen` with `vanguard.game.hangar`, the
  rules in `vanguard.content.campaign.Hangar`, `Catalogue`, `Intel`, `Flight`): layout B after
  hangar-r07-b at 960×540. Top bar: HANGAR, the next mission, act and difficulty, the UNDO, REPAIR
  and SAVE commands, the credits and LAUNCH. Left: the shop for the selected slot (fitted, owned,
  buyable by price with the NEW tag and ◆ markers, locked by unlock level with "LOCKED Lnn"; eight
  rows, scrolling), the selected item with its traits, numbers (DPS, output, capacity, regen,
  delay, armour, speed, charges; draw) and the green/red deltas against the fitted item, its
  choices (buy, upgrade, fit, unfit, sell, buy a charge; refused ones dim with the reason), the
  last transaction's message and the empty test-fire box. Centre: the holographic schematic with
  the parked Stormhawk sprite (3×), callouts for the front, wing and rear mounts with level pips,
  the locked escort slot (Act 2), the GEN/SHD/ARM/ENG/SPC/UTL/UTL tiles and the power bar. Right:
  the intel (below). Keys: Q/E (bumpers) cycle the eleven slots, up/down the rows (up from the
  first row reaches the command bar), left/right the choice or the command, Enter (A) confirms,
  Esc (B) asks to quit (and autosaves). Sell, a launch with a warning and quitting ask first. The
  repair panel opens on what the credits repair and changes by 1 (left/right) or 10 (up/down)
  points. Save opens the slot list; Launch is disabled while the next level is not built (after
  Level 01, until M4). The hangar theme plays; the autosave is written as the hangar opens. Intel
  from the level's `threat_profile` data (Level 01's now structured: setting, layers, density,
  traits, hazards, boss, special limits, Varga's line per sensor level) with the directions,
  enemies, wave times and secret count derived from the script; the sensor level gates the fields
  as in [ship systems](../../player/systems/README.md#sensor-levels-and-hangar-intel), and a
  hidden field shows the sensor level that reveals it. The speaker of the hangar teaser (Varga)
  is shown with her briefing portrait and the teaser. Placeholders: the tactical-map backdrop is
  drawn in code (grid, a planet limb, range rings, an approach route), the same for every level;
  test fire keeps its box. Not built: mouse input (so the first item stays open), the per-item
  Varga lines, the L2 portraits and the boss silhouette (so the intel item stays open), test fire,
  Rook's repairs (Act 2). Until M4 only the Pulse Cannon, shield, plating and engine fly; the
  shop says "NOT YET IN FLIGHT: FROM M4" on the other items and the HUD lists them.
- 2026-10-02: User decisions on the B2 open questions: a plating swap keeps the damage, the
  missing armour points carry over (as built; rule under *Transactions*); selling an item bought
  during the visit refunds its full price like the undo, items owned before keep the normal
  sell-back (`Hangar` tracks the visit's purchases wherever they are moved and restores them with
  the undo; the sell confirmation says the full price comes back; tests `HangarTest`:
  `anItemBoughtDuringTheVisitSellsForAllSpentOnIt`,
  `ofTwoEqualItemsOnlyTheOneBoughtDuringTheVisitSellsForAll`); the missing-trait launch warning
  only names traits the sensor level shows (`Intel.markedTraits()`, L3 with the easy bonus;
  `HangarStateTest.theMissingTraitWarningNeedsTheSensorLevelThatShowsTheTraits`). Varga's per-item
  lines (M4 writing pass), the Level 02 portraits and boss silhouette (art track) and the third
  utility bay's data (M4) are deferred; the open questions say where.
