---
title: Debrief screen
design: approved
implementation: not-started
art: chosen
depends-on: [../../systems/scoring, ../../systems/economy]
updated: 2026-10-01
---

# Debrief screen

## Summary

After a level is completed, the debrief tallies the results with counting-up numbers and
satisfying sounds, awards the grade, and adds the grade's credit bonus. Then the game moves on
to the next briefing.

## Design

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                     MISSION 21 COMPLETE · DUST COLOSSUS                      │
├──────────────────────────────────────────────────────────────────────────────┤
│   ENEMIES DESTROYED        184 / 203     91 %          + 18 200              │
│   ARMOUR DAMAGE TAKEN      12                                                │
│   SECRETS FOUND            2 / 3                                             │
│   MAX CHAIN                58   ×3.5                                         │
│   BOSS TIME                1:42  (par 2:00)            BOSS RUSH + 6 000     │
│                                                                              │
│   CREDITS EARNED           2 710                                             │
│   GRADE BONUS  A  +20 %      542                                             │
│   ────────────────────────────────                                           │
│   TOTAL CREDITS           15 702                            ┌─────┐          │
│   SCORE                1 382 900                            │  A  │          │
│                                                             └─────┘          │
│                                                  [ENTER] continue            │
└──────────────────────────────────────────────────────────────────────────────┘
```

(Example: level 21 *Dust Colossus*, budget ≈ 3 870 per the
[economy](../../systems/economy/README.md#per-level-budget) curve; numbers are illustrative.
Total credits = balance at level start + credits earned + grade bonus.)

- Lines appear one after another, 0.3 s apart, with a tick SFX while numbers count; confirm
  skips the animation.
- The grade stamp lands with a heavy SFX. A new best grade for the level gets a "NEW BEST" tag.
- Data cores found show as a small list with their lore titles (readable later).
- Act-final debriefs add an act summary (total kills, total credits for the act).

## Concept art

Concept [round 08](../../concept-rounds/round-08/README.md) — glass style (out-of-game) per the ui style rule; generator `tools/concept/ui_r08.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/debrief-r08-a.png](concept/debrief-r08-a.png) | Debrief — mission 10 complete: tally, credits breakdown, chain and score, grade stamp | chosen |
| [concept/mission-failed-r08-a.png](concept/mission-failed-r08-a.png) | Mission failed — frozen frame tinted red, retry / retry from boss / hangar / quit, discarded earnings | chosen |
| [concept/game-over-r08-a.png](concept/game-over-r08-a.png) | Game over (hard, 0/3 retries) — campaign stats, Okafor's last transmission, top-10 with letter-grid name entry | chosen |

## Implementation

- [ ] Tally sequence with count-up animation and skip
- [ ] Grade calculation display and credit bonus
- [ ] Data core list and act summary

## Decisions

- 2026-09-30: Grade and bonuses as defined in [scoring](../../systems/scoring/README.md).
- 2026-10-01: Concept round 08: accepted.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
