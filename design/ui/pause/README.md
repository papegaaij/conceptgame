---
title: Pause menu
design: draft
implementation: not-started
art: none
depends-on: [../../systems/retry]
updated: 2026-09-30
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

## Implementation

- [ ] Pause overlay with the items above and confirmations
- [ ] Resume countdown
- [ ] Auto-pause on focus loss and gamepad disconnect

## Decisions

- 2026-09-30: Restart and abort from the pause menu follow the retry rules.
