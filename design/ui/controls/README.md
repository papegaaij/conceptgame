---
title: Controls
design: draft
implementation: not-started
art: n/a
depends-on: [../../player/ship, ../../player/specials]
updated: 2026-10-01
---

# Controls

## Summary

Four actions in the level: move, fire, special, precision mode. Plus pause. Keyboard and
gamepad are both first-class; everything can be remapped in Options.

## Design

| Action | Keyboard (default) | Alt keys | Gamepad |
|---|---|---|---|
| Move | Arrow keys | W A S D | Left stick / D-pad |
| Fire (all weapons) | Space | Z | A / Cross (or right trigger) |
| Special | X | Left Ctrl | B / Circle |
| Precision mode (hold) | Left Shift | C | Right bumper |
| Dash (evasive thrusters) | Double-tap a direction | V | Left bumper |
| Pause | Esc | P | Start |

- **Hold to fire** is the default: weapons fire while the fire button is held. An **auto-fire
  toggle** in Options makes the ship fire continuously without holding (holding then does nothing
  extra).
- Three action buttons: **fire**, **special** and **hold for precision** (slower, finer movement).
- Menus: arrows / D-pad to navigate, Enter / A confirm, Esc / B back.
- Remapping: per action, both a primary and an alternative key. Conflicts are shown and swapped.
- Gamepad stick dead zone configurable (default 20 %).

## Implementation

- [ ] Input actions abstraction with keyboard and gamepad bindings
- [ ] Auto-fire option
- [ ] Remapping screen in Options with conflict detection
- [ ] Double-tap dash detection (only when the evasive thrusters module is fitted)

## Open questions

- None open.

## Decisions

- 2026-09-30: One fire button for all weapons, separate special button, precision hold.
- 2026-10-01: Controls: hold-to-fire by default with an auto-fire toggle in Options; buttons for fire (all weapons), special and hold-for-precision.
