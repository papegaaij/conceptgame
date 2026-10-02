---
title: Pause menu
design: approved
implementation: in-progress
art: chosen
depends-on: [../../systems/retry]
updated: 2026-10-02
---

# Pause menu

## Summary

Pressing pause freezes the level, dims the play field and opens a small menu over it. The side
panels stay visible.

## Design

| Item | Behaviour |
|---|---|
| Resume | Back to the level (after a 1 s "3-2-1" to avoid unfair hits) |
| Restart mission | Same as a retry: level-start state, counts as a retry on hard. Confirmation |
| Options | Audio, controls, display (same screens as in the main menu) |
| Abort to hangar | Same as "Back to hangar" after a failure: level-start state. Confirmation |
| Quit to main menu | Progress since the last save is lost. Confirmation |

The game also pauses automatically when the window loses focus or a gamepad disconnects.

## Concept art

Concept [round 08](../../concept-rounds/round-08/README.md) — glass style (out-of-game) per the ui style rule; generator `tools/concept/ui_r08.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/pause-r08-a.png](concept/pause-r08-a.png) | Pause — glass panel over the dimmed HUD frame: resume, restart, options, abort to hangar, quit | chosen |

## Implementation

- [ ] Pause overlay with the items above and confirmations
- [x] Resume countdown
- [x] Auto-pause on focus loss and gamepad disconnect

## Decisions

- 2026-09-30: Restart and abort from the pause menu follow the retry rules.
- 2026-10-01: Concept round 08: accepted.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-02: M3 part A (`vanguard.game.screen.PauseScreen`). Pause (Esc / P / Start) opens the
  glass panel of pause-r08-a over the frozen level, dimmed with its side panels: the mission, time,
  score and difficulty, then Resume, Restart mission, Options, Abort to hangar and Quit to main
  menu. Resume (or Back, or Pause again) counts 3-2-1 over 1 s in the play field before the level
  runs on; Pause during the count returns to the menu. Restart mission asks first, then restarts
  the level as a retry (`Sortie.retry()`: a new attempt, the attempt's earnings lost); Quit to
  main menu asks first ("Progress since the last save is lost"). Abort to hangar is shown
  disabled until the hangar exists (part B), so the overlay item stays open. The level pauses on
  its own when the window loses the focus or a gamepad disconnects (`ActionInput.interrupted()`).
  Music and ambience play on while paused.
