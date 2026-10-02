---
title: Scoring
design: approved
implementation: in-progress
art: n/a
depends-on: [../economy]
updated: 2026-10-02
---

# Scoring

## Summary

Kills give both credits (to spend) and **score** (for bragging rights and the high-score
table). Score is never spent. It grows with a chain multiplier for fast consecutive kills,
while credits don't, so skilful play is rewarded without breaking the economy.

## Design

### Score vs credits

The user described "points you can spend". In this draft those are **credits**. The score is
a separate number, shown next to credits on the HUD:

- score per kill = bounty × 10 × chain multiplier
- pickups and bonuses add score equal to 10 × their credit value (no multiplier)
- ground targets (e.g. cargo containers) are **not kills**: they score like pickups (10 × their
  credit value, no chain) and do not count for the kill ratio
- an enemy rammed to death **is** a kill: it pays its bounty and counts for the chain and the kill
  ratio
- like credits, the score earned in a failed attempt is discarded on retry

### Chain multiplier

- Every kill within **2 s** of the previous one adds 1 to the chain.
- Multiplier = 1 + 0.5 × floor(chain / 10), max ×5 (at a chain of 80).
- The chain ends when the 2 s window runs out, or when armour takes damage (shield hits do not
  break it).
- HUD shows `CHAIN 34  ×2.5` with a draining bar for the window.

### Level-end bonuses

<!-- data: score-bonuses -->
| Bonus | Condition | Score |
|---|---|---|
| Destruction | Kill ratio | kill % × 100 × level number |
| Untouched | No armour damage | 5 000 × act |
| Explorer | All secrets / data cores found | 3 000 × act |
| Boss rush | Boss killed under par time | 2 000 × act |
<!-- /data -->

### Grades

Grade per level, from a weighted rating: kill ratio 40 %, armour damage taken 30 %, secrets 15 %,
max chain 15 %. Each part maps to 0–100 as follows (first values, to tune once more levels
exist):

- kill ratio: kills ÷ enemies in the level
- armour damage taken: 1 − armour lost ÷ the plating's maximum
- secrets: secrets found ÷ secrets; full marks in a level without secrets
- max chain: the longest chain ÷ 80 (the chain that reaches ×5), at most 1

A flawless Level 01 with a chain of 30 rates about 91 (S).

<!-- data: grades -->
| Grade | Rating | Credit bonus |
|---|---|---|
| S | ≥ 90 | +30 % |
| A | ≥ 75 | +20 % |
| B | ≥ 55 | +10 % |
| C | ≥ 35 | — |
| D | < 35 | — |
<!-- /data -->

Best grade per level is stored in the save and shown in the level-select of a replay mode
(if added later).

### High scores

A local top-10 table per difficulty: name (3–10 characters), score, furthest level, date.
It is filled in at game over (quitting a campaign) and at the campaign's end.

## Implementation

- [x] Score counter with chain multiplier and HUD display
- [x] Level-end bonus calculation and grade rating
- [ ] Grade credit bonus fed into the economy
- [ ] High-score table per difficulty

## Open questions

- None open.

## Decisions

- 2026-09-30: Draft separates score from credits.
- 2026-10-01: Score and credits stay **separate** (user decision). The alternative — one number, points = credits, as in the original brief — was considered and rejected: spending would lower the score and chain play would inflate the economy.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-02: The scoring numbers moved into [data.yaml](data.yaml) (M2 data files); the bonus and grade tables are rendered from it.
- 2026-10-02: M2 implementation (`vanguard.sim.Tally`, `LevelResult`): score and credits kept
  apart, the chain (2 s window, +0.5 per 10 kills, ×5 at most, broken by armour damage), pickups
  and bonuses at 10 × their credit value, the difficulty's score factor on every score; the
  level-end bonuses (Destruction, Untouched, Explorer; Boss rush waits for bosses) and the grade
  with its credit bonus, shown in the debrief. The bonus goes into the campaign balance with the
  campaign state (M3), so that item stays open.
- 2026-10-02: Grade rating mapping kept as implemented (kill ratio; 1 − armour lost ÷ plating
  maximum; secrets found ÷ secrets, full marks without secrets; longest chain ÷ 80); tune after
  more levels exist (user decision).
- 2026-10-02: Ground targets are not kills (score like pickups, no chain, not in the kill ratio);
  a rammed enemy counts as a kill and pays its bounty (user decision).
