---
title: Scoring
design: draft
implementation: not-started
art: n/a
depends-on: [../economy]
updated: 2026-09-30
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
- like credits, the score earned in a failed attempt is discarded on retry

### Chain multiplier

- Every kill within **2 s** of the previous one adds 1 to the chain.
- Multiplier = 1 + 0.5 × floor(chain / 10), max ×5 (at a chain of 80).
- The chain ends when the 2 s window runs out, or when armour takes damage (shield hits do not
  break it).
- HUD shows `CHAIN 34  ×2.5` with a draining bar for the window.

### Level-end bonuses

| Bonus | Condition | Score |
|---|---|---|
| Destruction | Kill ratio | kill % × 100 × level number |
| Untouched | No armour damage | 5 000 × act |
| Explorer | All secrets / data cores found | 3 000 × act |
| Boss rush | Boss killed under par time | 2 000 × act |

### Grades

Grade per level, from a weighted rating: kill ratio 40 %, armour damage taken 30 %, secrets 15 %,
max chain 15 %.

| Grade | Rating | Credit bonus |
|---|---|---|
| S | ≥ 90 | +30 % |
| A | ≥ 75 | +20 % |
| B | ≥ 55 | +10 % |
| C | ≥ 35 | — |
| D | < 35 | — |

Best grade per level is stored in the save and shown in the level-select of a replay mode
(if added later).

### High scores

A local top-10 table per difficulty: name (3–10 characters), score, furthest level, date.
It is filled in at game over (quitting a campaign) and at the campaign's end.

## Implementation

- [ ] Score counter with chain multiplier and HUD display
- [ ] Level-end bonus calculation and grade rating
- [ ] Grade credit bonus fed into the economy
- [ ] High-score table per difficulty

## Open questions

- **Score vs credits.** The original brief said "in levels you can collect points, which you
  can use to spend on your ship". The alternative to this draft is exactly that: **one number,
  points = credits**, earned in levels and spent in the hangar, with no separate score (the
  high-score table would then rank total points earned, not the balance). This draft instead
  keeps two numbers: credits to spend (no chain multiplier, so the economy stays predictable)
  and a score that is never spent (chain multiplier, bonuses, high-score table).
  Recommendation: separate, because spending would otherwise make the score go down and chain
  play would inflate the economy. All documents currently follow the separate model.

## Decisions

- 2026-09-30: Draft separates score from credits.
