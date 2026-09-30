---
title: User interface
design: draft
implementation: not-started
art: chosen
depends-on: [../art-direction, ../systems]
updated: 2026-09-30
---

# User interface

## Summary

Every screen outside the action and the HUD around it: main menu, briefing, hangar, in-level
HUD, pause, debrief, and the controls. The style is late-90s military tech: bevelled metal
panels, green/amber phosphor readouts, portrait frames, chunky bitmap fonts.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [main-menu](main-menu/README.md) | Title screen, continue/new/load, options, credits, quit | draft | not-started | chosen |
| [briefing](briefing/README.md) | Story briefing before each level: portraits, typed text, objectives | draft | not-started | none |
| [hangar](hangar/README.md) | Ship configurator: intel, shop, loadout, repair, save, launch | draft | not-started | none |
| [hud](hud/README.md) | In-level side panels: status, weapons, special, radio chatter, progress | draft | not-started | chosen |
| [pause](pause/README.md) | Pause menu during a level | draft | not-started | none |
| [debrief](debrief/README.md) | Level complete: kills, credits, bonuses, grade | draft | not-started | none |
| [controls](controls/README.md) | Keyboard and gamepad mapping, remapping, auto-fire | draft | not-started | n/a |

## Design

### Screen flow

```
Title ─► Main menu ─┬─ Continue ─────────────► Hangar
                    ├─ New game ─► Difficulty ─► Intro briefing ─► Hangar
                    ├─ Load game ─► Slot list ─► Hangar
                    ├─ Options
                    ├─ Credits
                    └─ Quit
Hangar ─► Launch ─► Level ─► Debrief ─► Briefing ─► Hangar ─► …
Level ─► Pause ─► Resume / Restart / Options / Quit
```

### Shared UI rules

- Internal resolution 960×540, integer scaled where the display allows; the
  [art direction](../art-direction/README.md) owns the details. Bitmap fonts: 8×12 for labels,
  10×20 for body text, 20×30 for headings (a 240 px side panel fits about 22 body characters).
- Every screen is fully usable with keyboard or gamepad; the mouse is optional (hangar
  benefits from it).
- Confirm = Enter / A, Back = Esc / B everywhere.
- Panel frames, buttons and bars form one shared UI kit, designed once and reused.

## Concept art

The HUD and logo concepts for round 01 live in [hud](hud/README.md) and
[main-menu](main-menu/README.md).

## Implementation

- [ ] Screen/state machine for the flow above
- [ ] Shared UI kit: panels, buttons, lists, bars, portrait frame, bitmap fonts
- [ ] Keyboard and gamepad navigation on every screen

## Decisions

- 2026-09-30: Controls are a UI part (`controls/`), since remapping lives in the options screen.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-09-30: Fonts were not scaled ×1.5 (that would give 12×12 / 12×24 / 24×24): body text uses 10×20 so the wider panels fit more text per line.
