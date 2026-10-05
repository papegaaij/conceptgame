---
title: Debrief screen
design: approved
implementation: in-progress
art: final
depends-on: [../../systems/scoring, ../../systems/economy, ../../systems/saves]
updated: 2026-10-05
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
│   ENEMIES DESTROYED        184 / 203     91 %   + 18 200   RATING ─────────  │
│   ARMOUR DAMAGE TAKEN      12                              KILLS     36 / 40 │
│   SECRETS FOUND            2 / 3                           ARMOUR    21 / 30 │
│   MAX CHAIN                58   ×3.5                       SECRETS   10 / 15 │
│   BOSS TIME                1:42  (par 2:00)  RUSH + 6 000  CHAIN     15 / 15 │
│                                                            ───────────────── │
│                                                            RATING         82 │
│                                                                     A+ AT 85 │
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
  skips the animation. Back (Esc, the gamepad's back button) does what confirm does: there is
  nothing to go back to.
- The grade stamp lands with a heavy SFX. A new best grade for the level gets a "NEW BEST" tag.
- With the stamp, the **grade breakdown** appears above it in the right column: the rating's parts
  ([scoring](../../systems/scoring/README.md#grades)) as points of their weight (KILLS of 40,
  ARMOUR of 30, SECRETS of 15, CHAIN of 15; a part at full marks in green), a rule, the RATING
  (rounded down, so it never shows a threshold the grade missed) and the next grade's threshold
  (`A+ AT 85`; the top grade shows its own).
- **BOSS TIME** (levels with a boss or mid-boss: L05, L07): the time from the boss bar appearing
  to the kill, the boss's par in brackets and, under par, the
  [Boss rush](../../systems/scoring/README.md#level-end-bonuses) bonus (`RUSH + 2 000`); without
  a boss the row is left out.
- Data cores found in the level show as a small list with their lore titles (readable later;
  the title is the secret's name in capitals, `SETTLEMENT LOG`).
- **Act summary** (act-final debriefs, M4 part G): after the level's own tally and grade, confirm
  turns to a second page instead of leaving: `ACT I COMPLETE · FIRST CONTACT`, one row per level
  of the act (number, name, best grade, the credits it banked with its grade bonus and its kills),
  the act's totals (kills, credits) and the data cores found in the act with their lore titles;
  confirm then goes on to the act outro. A level a save did not record (saves from before format
  version 2, see [saves](../../systems/saves/README.md)) shows dashes and is left out of the
  totals, which then say so (`6 OF 7 MISSIONS RECORDED`). A replay from mission select has no act
  summary.

## Concept art

Concept [round 08](../../concept-rounds/round-08/README.md) — glass style (out-of-game) per the ui style rule; generator `tools/concept/ui_r08.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/debrief-r08-a.png](concept/debrief-r08-a.png) | Debrief — mission 10 complete: tally, credits breakdown, chain and score, grade stamp | chosen |
| [concept/mission-failed-r08-a.png](concept/mission-failed-r08-a.png) | Mission failed — frozen frame tinted red, retry / retry from boss / hangar / quit, discarded earnings | chosen |
| [concept/game-over-r08-a.png](concept/game-over-r08-a.png) | Game over (hard, 0/3 retries) — campaign stats, Okafor's last transmission, top-10 with letter-grid name entry | chosen |

Production art, UI batch part U2 (for concept round 13, opened by part U3): no art of its own; the screen draws the production glass kit ([tools/art/ui_kit.py](../../../tools/art/README.md)), its sheet made from the capture by `tools/art/ui_review.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/debrief-final-r13-a.png](concept/debrief-final-r13-a.png) | Review sheet: the game capture at 1× with a 2× detail of the title, tally and header rules | chosen |
| [concept/debrief-capture-final-r13-a.png](concept/debrief-capture-final-r13-a.png) | Game capture: Level 01 complete (an invulnerable run at debug speed 8), the tally and credits on glass over the dimmed title scene, the grade on its amber-trimmed glass | chosen |

## Implementation

- [x] Tally sequence with count-up animation and skip
- [x] Grade calculation display and credit bonus
- [x] Back (Esc, the gamepad's back button) skips and goes on like confirm (`DebriefExitTest`)
- [x] Grade breakdown: the rating's parts, the rating and the next grade's threshold beside the stamp (`DebriefRatingTest`)
- [ ] Data core list with the lore titles (M4 part G)
- [ ] Act summary page after the act's last level, from the save's per-level records; not on a
      replay (M4 part G)
- [ ] BOSS TIME row with the par and the Boss rush bonus in levels with a boss (M4 part G; the
      bonus itself is paid since part E)

## Decisions

- 2026-09-30: Grade and bonuses as defined in [scoring](../../systems/scoring/README.md).
- 2026-10-01: Concept round 08: accepted.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-02: M2 placeholder (`vanguard.game.screen.DebriefScreen`), in the glass style of
  the chosen debrief-r08-a: the mission complete jingle, the tally (kills with the Destruction
  bonus, armour damage, secrets with Explorer, max chain, secondary objective), the credits by
  source with the grade bonus and the total, the score and the grade stamp; lines 0.3 s apart with
  a tick while their numbers count up, the total chime and the stamp sound. Confirm skips, then
  returns to the title. No "NEW BEST" tag until there are saves.
- 2026-10-02: M3 part A: drawn with the UI kit's fonts (20×30 title and grade, 10×20 rows, 8×12
  label); confirm at the end returns to the main menu until the briefing and the hangar follow
  (part B).
- 2026-10-02: M3 part B1: the debrief banks the level in the campaign (credits with the grade bonus,
  score, kills, the armour left, the best grade) and shows "NEW BEST" under the stamp when the grade
  beats the level's recorded best; confirm at the end goes to the next level's briefing, or to the
  hangar while the next level is not built (after Level 01 for now).
- 2026-10-02: M3 close-out (user decision): the data core list and the act summary move to M4; the document is done for M3.
- 2026-10-02: Production art, UI batch part U2: the debrief draws the production glass kit (a
  glass panel with its trim, header rules, the grade on an amber-trimmed glass card) over the title
  scene dimmed to 35 % (Earth orbit, Level 01's setting) instead of flat fills over black; layout
  and fonts unchanged. Review files proposed for round 13; `art` stays `chosen`.
- 2026-10-02: Concept round 13 closed (user decision): the debrief in the production glass kit over the dimmed title scene approved as **final**; it has no art of its own, so `art: final`.
- 2026-10-04: The top grade is **A+** (user decision, see [scoring](../../systems/scoring/README.md)):
  two full-size 20×30 glyphs at ×3 overrun the 110 px stamp, so the letter stays at ×3 and the plus
  is drawn at ×2, raised beside it.
- 2026-10-04: Back on the debrief (user decision): Esc and the gamepad's back button go on like
  confirm (skip the count-up, then leave), since the debrief has nothing to go back to. Test:
  `DebriefExitTest`.
- 2026-10-05: Grade breakdown in the debrief (user decision): KILLS, ARMOUR, SECRETS and CHAIN as
  points of their weight, the rating and the next grade's threshold (`RATING 88`, `A+ AT 85`), in
  the right column above the stamp with an amber RATING heading and trim rules like the tally's; it
  appears with the stamp. The tally's and credits' heading rules now end at the number column
  (they ran under the new column). The sim's `LevelResult.Rating` carries the parts and the target
  grade.
- 2026-10-05: M4 part G (user decision D2 and its defaults): the act summary comes in the act's
  last debrief, before the act outro; it sums each won level's banked credits (with the grade
  bonus) and kills, which the save records from format version 2 on (older saves show what is
  recorded). Main-agent layout choices: a second page after the level's tally, rows per level
  with the best grade, the act's data cores with their lore titles, and no summary on a replay.
  The BOSS TIME row of the mock, missing since part E, is built with it. Status back to
  `in-progress` for these items.
