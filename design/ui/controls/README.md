---
title: Controls
design: approved
implementation: in-progress
art: n/a
depends-on: [../../player/ship, ../../player/specials]
updated: 2026-10-02
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
| Pause | Esc (fixed) | P | Start |
| Toggle full screen | Alt+Enter | F11 | — |

- **Hold to fire** is the default: weapons fire while the fire button is held. An **auto-fire
  toggle** in Options makes the ship fire continuously without holding (holding then does nothing
  extra).
- Three action buttons: **fire**, **special** and **hold for precision** (slower, finer movement).
- Menus: arrows / D-pad to navigate, Enter / A confirm, Esc / B back.
- **Esc and Enter are fixed system keys** (back / pause and confirm), like the full-screen keys:
  they cannot be remapped or bound to another action. Pause keeps Esc as its primary key; every
  action keeps a remappable alternative key (Pause: P).
- Remapping: per action, both a primary and an alternative key (Pause: the alternative only).
  Conflicts are shown and swapped.
- Gamepad stick dead zone configurable (default 20 %).
- **Toggle full screen** works everywhere, is not remappable (both keys are the platform
  conventions) and has no gamepad binding; the display mode rules live in
  [options](../options/README.md).

## Implementation

- [x] Input actions abstraction with keyboard and gamepad bindings
- [x] Auto-fire option
- [x] Remapping screen in Options with conflict detection
- [x] Alt+Enter / F11 toggle full screen on every screen
- [ ] Double-tap dash detection (only when the evasive thrusters module is fitted)

## Open questions

- None open.

## Decisions

- 2026-09-30: One fire button for all weapons, separate special button, precision hold.
- 2026-10-01: Controls: hold-to-fire by default with an auto-fire toggle in Options; buttons for fire (all weapons), special and hold-for-precision.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-01: Full-screen toggle added (user requirement): Alt+Enter and F11, not remappable, back in review for these details.
- 2026-10-01: Full-screen toggle keys accepted by the user.
- 2026-10-01: M1 implementation (`vanguard.game.input`): `Bindings` maps every action to a primary key, an alternative key and a set of gamepad controls (immutable; a remap makes a changed copy), `ActionInput` samples them once per frame into held/pressed states. Menu confirm (Enter / A) and back (Esc / B / Back) are fixed bindings; Enter while Alt is held does not confirm, so Alt+Enter only toggles full screen. Auto-fire is `controls.auto-fire=true` in `settings.properties` until the Options screen (M3). Dash has V and the left bumper; the double-tap primary input comes with the evasive thrusters. Stick dead zone fixed at 20 % for now. In flight, Pause (Esc / P / Start) returns to the title until the pause screen (M3).
- 2026-10-02: M3 part A: remapping in the Controls tab of [options](../options/README.md).
  Confirm on a cell starts a press-a-key capture ("PRESS A KEY..."; for the gamepad column a
  button: A, B, X, Y, the bumpers, the triggers or Start); what was held when it started does not
  count, Esc or the gamepad's Back cancel, F11 is never captured. A key or button that another
  action already has shows the conflict ("CONFLICT: 'Z' IS ALREADY FIRE (ALTERNATIVE) - ENTER SWAPS
  THE TWO"); confirm swaps (the other slot gets the old key or buttons), Back keeps the old
  bindings. The gamepad column of the four move actions stays the left stick and the D-pad. The
  menus keep fixed keys (arrows, D-pad and left stick, Enter / A, Esc / B / Back, Q / E and the
  bumpers for tabs), so a remap never locks them up. Bindings, auto-fire and the stick dead zone
  (now configurable, 5–50 %) persist in the settings file. In flight, Pause now opens the pause
  menu. Because Esc cancels a capture, Esc can only come back to a slot through reset to defaults
  (open question).
- 2026-10-02: Esc and Enter are fixed system keys (user decision): back / pause and confirm, not
  remappable; every action keeps a remappable alternative key. In the Controls tab Pause's primary
  cell shows ESC dimmed and cannot be selected for a capture; a capture never takes Enter (nor F11),
  Esc cancels it; the settings file's Pause primary key and any slot naming Esc, Enter or F11 are
  ignored (default kept). Tests: `BindingsTest`, `KeyCaptureTest`, `SettingsFileTest`.
