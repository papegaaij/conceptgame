---
title: Debrief screen
design: approved
implementation: done
art: chosen
depends-on: [../../systems/scoring, ../../systems/economy]
updated: 2026-10-02
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

Production art, UI batch part U2 (for concept round 13, opened by part U3): no art of its own; the screen draws the production glass kit ([tools/art/ui_kit.py](../../../tools/art/README.md)), its sheet made from the capture by `tools/art/ui_review.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/debrief-final-r13-a.png](concept/debrief-final-r13-a.png) | Review sheet: the game capture at 1× with a 2× detail of the title, tally and header rules | proposed |
| [concept/debrief-capture-final-r13-a.png](concept/debrief-capture-final-r13-a.png) | Game capture: Level 01 complete (an invulnerable run at debug speed 8), the tally and credits on glass over the dimmed title scene, the grade on its amber-trimmed glass | proposed |

## Implementation

- [x] Tally sequence with count-up animation and skip
- [x] Grade calculation display and credit bonus
- [ ] Data core list and act summary — **later: M4** (data cores and the first act end come with the Act 1 levels)

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
