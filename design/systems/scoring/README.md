---
title: Scoring
design: approved
implementation: in-progress
art: n/a
depends-on: [../economy]
updated: 2026-10-05
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
- The window **pauses while no enemy is on screen**, so a gap in the level never breaks a chain;
  only enemies left alive on screen can, by outlasting the window. On screen counts a live enemy
  (air or ground unit) on a layer the standard shots reach (not high air) whose hit box overlaps
  the play field, and a set piece or boss while one of its parts on such a layer is on the field,
  not wrecked and not shielded (a boss's descent and the parts a later phase exposes do not count).
- HUD shows `CHAIN 34  ×2.5` with a draining bar for the window; the bar stands still while the
  window is paused.

### Level-end bonuses

<!-- data: score-bonuses -->
| Bonus | Condition | Score |
|---|---|---|
| Destruction | Kill ratio | kill % × 100 × level number |
| Untouched | No armour damage | 5 000 × act |
| Explorer | All secrets / data cores found | 3 000 × act |
| Boss rush | Boss or mid-boss killed under its par time (the boss data's `par`) | 2 000 × act |
<!-- /data -->

### Grades

Grade per level, from a weighted rating: kill ratio 40 %, armour damage taken 30 %, secrets 15 %,
max chain 15 %. Each part maps to 0–100 as follows (first values, to tune once more levels
exist):

- kill ratio: kills ÷ enemies in the level
- armour damage taken: 1 − armour lost ÷ the plating's maximum
- secrets: secrets found ÷ secrets; full marks in a level without secrets
- max chain: the longest chain ÷ 40 (the data's `full_chain`), at most 1

A flawless Level 01 with a chain of 30 rates about 96 (A+). The debrief shows each part's points
of its weight, the rating and the next grade's threshold
([debrief](../../ui/debrief/README.md)).

The grades from best to worst are **A+ · A · B · C · D**. The debrief stamps the letter with its
plus raised beside it; a save written before the rename keeps its best grade (an old `S` loads as
`A+`).

<!-- data: grades -->
| Grade | Rating | Credit bonus |
|---|---|---|
| A+ | ≥ 85 | +30 % |
| A | ≥ 70 | +20 % |
| B | ≥ 50 | +10 % |
| C | ≥ 30 | — |
| D | < 30 | — |
<!-- /data -->

Best grade per level is stored in the save and shown in the
[mission select](../../ui/mission-select/README.md), whose replays can raise it.

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
- 2026-10-03: M4 part E (user decision): mid-bosses earn the Boss rush bonus too; each boss's par
  is its data's `par` (the Gorgon Frigate: 60 s at every difficulty), timed from its bar
  appearing to the kill. The debrief's BOSS TIME row shows it.
- 2026-10-03: M4 part E: the Boss rush is paid (`LevelResult.BossTime`: the level's boss killed
  within its `par` from its arrival, when its bar appears); the debrief's BOSS TIME row is still to
  come.
- 2026-10-04: Checked the grade thresholds against the economy rework (the budget is the typical
  haul; a typical player kills 60 % of the air enemies in denser levels): S needs about 84 %
  kills even when flawless, so lower thresholds are proposed under Open questions; the data is
  unchanged until the user decides.
- 2026-10-04: Grade thresholds for dense levels (user decision): S 85 · A 70 · B 50 · C 30 (were
  90 · 75 · 55 · 35). The typical player (kill ratio about 0.65, 70 % of the armour kept, half the
  secrets, a chain of about 30) still rates about 60, a B; a flawless run needs about 72 % kills for
  an S instead of 84 %, and a strong run (85 % kills, 90 % armour, every secret, a chain of 48)
  moves from A to S.
- 2026-10-04: The top grade **S** becomes **A+** (user decision); the scale is A+ · A · B · C · D,
  with S's threshold and bonus. The debrief draws the plus raised beside the letter, and a save's
  stored best grade `S` loads as `A+`. The entries above keep the old name.
- 2026-10-05: Full chain marks at a chain of **40** instead of 80 (user decision): the rating's
  chain part is the longest chain ÷ 40 (`rating.full_chain` in the data), no longer tied to the
  ×5 multiplier, which stays at 80. Trigger: a Level 06 run with every kill, both secrets, 24
  armour lost and a chain of 19 rated about 77 and got an A; at ÷ 40 it rates about 80 (still an
  A; the plan fit's 60 armour). The debrief shows the breakdown.
- 2026-10-05: The chain window **pauses while no enemy is on screen** (user decision); the window
  stays 2 s. On screen: live enemies (air and ground units) on a layer the standard shots reach and
  overlapping the play field, and set pieces and bosses while a part that is on the field can be
  damaged (not wrecked, not shielded); high-air units and a boss's descent pause it. Implemented
  in the sim (`Tally.step`, `Sortie.chainTargets`, the pause flag in the state hash; tests
  `ChainPauseTest`, `TallyTest`). The autopilot's longest chain on medium (L01–L06) goes from
  20 · 18 · 12 · 15 · 25 · 44 to 36 · 32 · 17 · 30 · 87 · 124; the Level 01 replay hash changed
  (kills and credits unchanged).
