---
title: Economy
design: approved
implementation: in-progress
art: n/a
depends-on: [../../player, ../difficulty]
updated: 2026-10-02
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
- **Spawned adds are budgeted at their expected count**: units that appear during play
  (Skitters from hive nodes, units launched by a boss) count in the level budget at the number
  a typical run expects to see, at their normal bounty.

### Data cores

A data core is a hidden lore pickup (see [player](../../player/README.md#in-level-pickups)).
Besides the lore entry, each core unlocks **one specific shop item one act early**: the item is
in the shop from the next hangar visit instead of from its normal unlock. Items found this way
cost their normal price. Later acts add rows as their level documents place cores.

| Data core found in | Item unlocked early | Normal unlock | Why this item |
|---|---|---|---|
| [L06 Farside](../../campaign/act-1-first-contact/level-06-farside/README.md) (settlement log, Daedalus Gate) | [Targeting computer](../../player/systems/README.md) utility module | act 2 → from L07 | The settlement's survey sensors; its boss weak-point markers help against the [Brood Carrier](../../enemies/bosses/brood-carrier/README.md) at L07, and its homing bonus suits the Micro-missile Pod that unlocks at L06 |

### Per-level budget

This curve is the single source for level credit budgets; level documents and the
[campaign](../../campaign/README.md#credit-budget) use it. The credits a perfect run of level
*n* can earn (medium, before grade bonus):

**budget(n) ≈ 1 000 × 1.07^(n−1)**

| Act | Levels | Budget per level | Act total |
|---|---|---|---|
| 1 | 01–07 | 1 000 – 1 500 | ≈ 8 700 |
| 2 | 08–14 | 1 600 – 2 400 | ≈ 13 900 |
| 3 | 15–21 | 2 600 – 3 900 | ≈ 22 300 |
| 4 | 22–28 | 4 100 – 6 200 | ≈ 35 800 |
| 5 | 29–35 | 6 600 – 10 000 | ≈ 57 500 |
| 6 | 36–42 | 10 700 – 16 000 | ≈ 92 400 |
| 7 | 43–50 | 17 100 – 27 500 | ≈ 175 900 |

The campaign total is about 406 000. A typical medium run collects about 70 %, roughly
285 000.

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
  (purchase plus upgrades).
- Unfitted items stay in the inventory for free, so selling is only needed for cash.

### Pricing curve

Base prices grow with the act the item unlocks in (about ×1.6 per act). The upgrade cost
formula is in [weapons](../../player/weapons/README.md#common-rules).

## Implementation

- [ ] Credit balance, income multiplier by difficulty
- [ ] Bounty values per enemy class; boss bounty per act
- [ ] Hangar transaction log for undo; 60 % sell-back otherwise
- [ ] Balancing sheet (spreadsheet or script) that simulates per-level budgets vs prices

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
