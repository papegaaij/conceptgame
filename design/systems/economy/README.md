---
title: Economy
design: approved
implementation: in-progress
art: n/a
depends-on: [../../player, ../difficulty]
updated: 2026-10-05
---

# Economy

## Summary

**Credits** are the one currency. They are earned in levels (bounties, salvage, level-end
bonuses) and spent in the hangar. The campaign pays out less than it would cost to buy
everything, so the player has to pick a build and adapt it per level.

## Design

### Sources

All amounts are Act 1 values at medium and scale with the **act factor 1.6^(act−1)**, the same
growth as prices (1.07⁷ ≈ 1.6 per act), so a level's income stays in line with its budget.

| Source | Amount (first draft, medium, Act 1) |
|---|---|
| Enemy bounty | popcorn 5 · light 12 · medium 20 · heavy 60 (the exact value per enemy is in its stat block) |
| Mid-boss / act boss | about 15 % / 30 % of the level's budget |
| Salvage pickups | 10 / 50 / 200 |
| Hidden crates | 5–15 % of the level's budget each, usually 1–3 per level |
| Level-end bonus | Grade bonus: S +30 %, A +20 %, B +10 % of credits earned in the level |
| Selling | See sell-back |

**Starting credits: 300** — enough for one small choice at the first hangar visit (before L01),
e.g. the Pulse Cannon's L2 upgrade.

Difficulty multiplies all credit income: easy ×1.25, medium ×1.0, hard ×0.9. See
[difficulty](../difficulty/README.md). The factor is applied and **rounded per payout**, half to
even like the HP lever (easy: a Skitter pays 6, a small salvage 12, the secondary objective 62),
so the debrief's per-source lines add up exactly to the level's credits.

Two budgeting conventions, used by level documents and `tools/balance.py`:

- **Act-factor bounties round per kill**: an enemy's Act 1 bounty times the act factor is
  rounded to whole credits for each kill, not summed first and rounded once.
- **The level's bounty scale comes last**: a level's data may set a `bounty_scale` (1 if not
  given) that multiplies every bounty paid in it: kills, boss and set-piece parts, ground units,
  a destructible's own bounty, a spawner's burst and a shot spore mine. Not salvage, crates or
  objectives. The order per payout is: Act 1 bounty × act factor × difficulty income × bounty
  scale, then **one** rounding, half to even. The scale only moves credits; the score uses the
  bounty without it. A level sets its scale so its typical haul lands on its budget (below).
- **Spawned adds are budgeted at their expected count**: units that appear during play
  (Skitters from hive nodes, units launched by a boss) count in the level budget at the number
  a typical run expects to see, at their normal bounty.

### Data cores

A data core is a hidden lore pickup (see [player](../../player/README.md#in-level-pickups)).
Besides the lore entry, each core unlocks **one specific shop item one act early**: the item is
in the shop from the next hangar visit instead of from its normal unlock. Items found this way
cost their normal price. Later acts add rows as their level documents place cores.

An unlock is recorded when the core is collected in a won level: the save keeps the core
(`dataCores`) and the unlock (`unlocks`) at once, even when the item does not exist in the game
yet. The L06 core's Targeting computer is such a case: Level 06 (M4 part F) records it, and the
item enters the shop once utility modules exist (M5), from the first hangar visit after that.

| Data core found in | Item unlocked early | Normal unlock | Why this item |
|---|---|---|---|
| [L06 Farside](../../campaign/act-1-first-contact/level-06-farside/README.md) (settlement log, Daedalus Gate) | [Targeting computer](../../player/systems/README.md) utility module | act 2 → from L07 | The settlement's survey sensors; its boss weak-point markers help against the [Brood Carrier](../../enemies/bosses/brood-carrier/README.md) at L07, and its homing bonus suits the Micro-missile Pod that unlocks at L06 |

### Per-level budget

This curve is the single source for level credit budgets; level documents and the
[campaign](../../campaign/README.md#credit-budget) use it. The budget is **the typical player's
haul**, not a perfect run: what a typical medium player earns in level *n*, before the grade
bonus:

**budget(n) ≈ 700 × 1.07^(n−1)**

This is exactly what the shop plan expected before: the old curve (1 000 × 1.07^(n−1), a perfect
run) times the old typical collection of 70 %. So the prices, the shop plan in
[balance-plan.yaml](../../player/balance-plan.yaml) and `tools/balance.py`'s income are unchanged;
the plan's `typical_collection: 0.7` is gone (it is 1.0 now: the plan earns the budget).

| Act | Levels | Budget per level | Act total |
|---|---|---|---|
| 1 | 01–07 | 700 – 1 050 | ≈ 6 100 |
| 2 | 08–14 | 1 120 – 1 690 | ≈ 9 700 |
| 3 | 15–21 | 1 800 – 2 710 | ≈ 15 600 |
| 4 | 22–28 | 2 900 – 4 350 | ≈ 25 100 |
| 5 | 29–35 | 4 650 – 6 980 | ≈ 40 300 |
| 6 | 36–42 | 7 470 – 11 220 | ≈ 64 700 |
| 7 | 43–50 | 12 000 – 19 270 | ≈ 123 100 |

The campaign total for a typical medium player is about 285 000.

**The typical player** (`typical_player` in [data.yaml](data.yaml)) collects of each source:

| Source | Share | Why |
|---|---|---|
| Air enemies killed (released spawns and boss streams too) | 60 % | Dense levels: a typical player cannot shoot everything that crosses the screen |
| Ground targets (ground units, paying destructibles, a set piece's parts) | 80 % | They hold still or move slowly; most are taken |
| Hidden crates (secrets) | 50 % | Half the secrets are found |
| Credit pickups (salvage), of those whose source was destroyed | 80 % | Some drift off screen in a fight |
| Primary objective credits (an escort's units home) | 100 % | As authored: the level is won |
| Secondary objective credits | 50 % | A stretch goal, met in every other run |
| Boss and mid-boss parts | 100 % | The boss dies in every won run |

A level's **typical haul** is its perfect run with each source weighted by these shares (the
*Credit budget* table of each level shows both). It must land within ±5 % of budget(n); a
perfect run then earns well above the budget, about 1.5× or more. Levels are **dense** (see the
[campaign's minimum density](../../campaign/README.md#difficulty-curve)) and set a `bounty_scale`
so the typical haul lands on the budget.

For comparison, maxing out everything would cost roughly 450 000 (generators ≈ 65 000,
shields ≈ 25 000, plating ≈ 22 000, and several weapons at 7.5–8.5 × their base price each).
A typical player can afford about 60–65 % of it, so choices matter.

### Sinks

- Weapons, upgrades, core components, utility modules, the 3rd utility bay (5 000)
- Special charges (consumed)
- Armour repair (medium 5 cr/point, hard 10 cr/point, easy free)
- Rook's upgrades and repairs

### Sell-back and undo

- **Undo**: anything bought during the current hangar visit can be returned for 100 %
  until the player launches. This encourages experimenting.
- **Sell**: items owned from earlier visits sell for 60 % of the total spent on them
  (purchase plus upgrades). An item bought during the current visit sells for 100 % of what was
  spent on it, like its undo; an item owned before the visit keeps the 60 %, even after an
  upgrade in this visit.
- Unfitted items stay in the inventory for free, so selling is only needed for cash.

### Pricing curve

Base prices grow with the act the item unlocks in (about ×1.6 per act). The upgrade cost
formula is in [weapons](../../player/weapons/README.md#common-rules).

## Implementation

- [x] Credit balance, income multiplier by difficulty
- [x] Bounty values per enemy class; boss bounty per act (Act 1's enemy classes, the Gorgon
  Frigate and the Brood Carrier's 450 with its bays and core, M4 parts A–G)
- [x] Hangar transaction log for undo; 60 % sell-back otherwise (100 % for an item bought in the
  same visit)
- [x] Level `bounty_scale` (default 1) on every bounty, after the credit factor, one rounding
  per payout (`ScoringRules.bountyScale`, `Tally.bounty`, test `TallyTest`)
- [x] Typical haul per level: the credit-budget tables (`tools/sync_tables.py`) and the content
  test `TypicalHaulTest` (within ±5 % of the budget for every level)
- [x] Levels 01–05 reworked to the typical-haul budget (density and `bounty_scale`: 1.21, 1.05,
  0.97, 0.79, 0.79; the minimum density in the content test `DensityTest`)
- [x] Data cores: the core and its unlock recorded in the save (`dataCores`, `unlocks`) when
  collected in a won level (M4 part F: a secret's `data_core` in the level data)
- [ ] The unlocked item in the shop once it exists (the Targeting computer: M5)
- [ ] Balancing sheet (spreadsheet or script) that simulates per-level budgets vs prices — **later: M4** (needs the Act 1 levels; the roadmap's balance tests)

## Open questions

- Do credits earned in a failed attempt really vanish? Yes: the user decided this. The
  consequence is that grinding by dying is impossible, which is intended.

## Decisions

- 2026-09-30: One currency (credits); score is tracked separately (see
  [scoring](../scoring/README.md)).
- 2026-09-30: Full refund within a hangar visit, 60 % sell-back afterwards.
- 2026-09-30: Budget curve set to 1 000 × 1.07^(n−1) (was 800 ×) so level 01 matches its
  worked-example budget of 1 000 and a typical run affords 60–65 % of everything; bounties and
  crates scale with the act factor; boss bounties are a share of the level budget.
- 2026-10-01: Starting credits set to 300 (player spec work); `tools/balance.py` models a typical medium player's purchases for levels 01–14 against this curve.
- 2026-10-01: Data cores: each unlocks one specific shop item one act early (user decision); table added, L06's core unlocks the Targeting computer from L07.
- 2026-10-01: Conventions stated: act-factor bounties round per kill; spawned adds (hive-node Skitters, boss-launched units) are budgeted at their expected count.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-02: The economy numbers (starting credits, budget curve, act factor, sell-back) moved into [data.yaml](data.yaml) (M2 data files); the tables here stay hand-written for now, the Level 01 credit budget is computed from the curve.
- 2026-10-02: M2: credits earned in a level are tallied per source (kills, ground targets,
  salvage, secrets, objectives) at the Act 1 value × the difficulty's income × the act factor,
  and lost with a failed attempt. The level's starting balance is the 300 starting credits until
  the campaign state exists (M3).
- 2026-10-02: The difficulty's income factor is rounded per payout, half to even, so the debrief
  adds up exactly (user decision); summing a level's credits first and rounding once was the
  alternative.
- 2026-10-02: M3 part B1: the credit balance lives in the campaign state and its saves, starting
  at the 300 starting credits; a won level banks what it earned plus the grade bonus, a failed or
  aborted attempt banks nothing.
- 2026-10-02: M3 part B2 (`vanguard.content.campaign.Catalogue`, `Hangar`): the shop's prices,
  upgrade costs (factor × upgrade base, rounded half to even), sell-back (60 % of all spent on the
  item, purchase plus upgrades, rounded half to even) and repair costs (difficulty data, per whole
  armour point) come from the data. Undo returns the visit's transactions one by one, last first,
  for 100 % (a sale too); a sale of an item bought in the same visit pays 60 % like any sale, and
  the sell confirmation points to the undo. Items at price 0 are the starters and are not sold in
  the shop. The hangar, the debrief (launch balance plus earnings) and the saves read the same
  campaign balance. Content test (`EconomyTest`): the 300 starting credits buy the Pulse Cannon's
  L2 upgrade and not the 800 sensor suite (as Level 01's threat profile says), and Level 01 flown
  by the test autopilot with it pays for the Autocannon Pod that is NEW at the L02 visit.
- 2026-10-02: Selling an item bought during the current hangar visit refunds all spent on it
  (100 %, like the undo); items owned before the visit keep the 60 % sell-back (user decision;
  `vanguard.content.campaign.Hangar`, tests in `HangarTest`). The B2 rule (60 % for any sale) is
  replaced.
- 2026-10-02: M3 close-out (user decision): the bounty table per enemy class with the boss bounty per act and the balancing sheet move to M4, where the Act 1 enemies, bosses and levels exist; with them marked, the document is done for M3.
- 2026-10-04: The level credit budget is the **typical player's haul**, not a perfect run (user
  decision). The curve becomes 700 × 1.07^(n−1), the old perfect-run curve × the old 70 % typical
  collection, so the shop plan and prices stay; `typical_collection` is dropped from the balance
  plan. A typical player kills 60 % of the air enemies (user decision); the other shares
  (ground 80 %, secrets 50 %, pickups 80 %, primary 100 %, secondary 50 %) are proposed. A perfect
  run earns about 1.5× the budget or more. Levels become denser and get a `bounty_scale` (default
  1), the last factor on every bounty before the one rounding per payout. Today Levels 01–03 fall
  9–14 % under their budget and Levels 04–05 12–15 % over it (perfect 1.43–1.64 × budget); they are
  reworked next.
- 2026-10-04: The typical player's shares are confirmed as proposed (user decision): air 60 %,
  ground 80 %, secrets 50 %, pickups 80 %, primary 100 %, secondary 50 %, boss and mid-boss parts
  100 %.
- 2026-10-04: Density rework of Levels 01–05: denser waves (see the
  [campaign's minimum density](../../campaign/README.md#difficulty-curve)) and a `bounty_scale`
  per level put every typical haul within 1.1 % of its budget: Level 01 × 1.21 (708 of 700),
  Level 02 × 1.05 (756 of 749), Level 03 × 0.97 (805 of 801), Level 04 × 0.79 (857 of 858),
  Level 05 × 0.79 (916 of 918); a perfect run earns 1.45–1.68 × the budget.
- 2026-10-04: The L06 data core's unlock of the Targeting computer is recorded now, as the
  pickup and the save entries (`dataCores`, `unlocks`); the item takes effect once utility modules
  exist in M5 (user decision D6 of M4 part F). Rejected: building the Targeting computer in M4 (a
  whole new system) and swapping the unlock for an item that exists in Act 1.
- 2026-10-05: Round 25 closed (user): Level 07's numbers accepted (`bounty_scale` 0.75, typical
  haul 1,049 of the budget's 1,051, perfect 1,542; the Brood Carrier's 450 bounty, 340 at the
  scale; the boss spawns counted at their expected numbers), so every Act 1 enemy class and boss
  has its bounty.
