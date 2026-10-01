---
title: User interface
design: approved
implementation: not-started
art: chosen
depends-on: [../art-direction, ../systems]
updated: 2026-10-01
---

# User interface

## Summary

Every screen outside the action and the HUD around it: main menu, briefing, hangar, in-level
HUD, pause, debrief, and the controls. Two styles (user decision, round 06): **out-of-game
screens** (menus, difficulty, load game, hangar/shop, briefing, debrief) use translucent glass
panels with thin metal trim over a pre-rendered scene, as in main menu A; the **in-game HUD**
alone uses bevelled metal panels with green/amber phosphor readouts. Both share portrait
frames and chunky bitmap fonts.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [main-menu](main-menu/README.md) | Title screen, continue/new/load, options, credits, quit | approved | not-started | chosen |
| [briefing](briefing/README.md) | Story briefing before each level: portraits, typed text, objectives | approved | not-started | chosen |
| [hangar](hangar/README.md) | Ship configurator: intel, shop, loadout, repair, save, launch | approved | not-started | chosen |
| [hud](hud/README.md) | In-level side panels: status, weapons, special, radio chatter, progress | approved | not-started | chosen |
| [pause](pause/README.md) | Pause menu during a level | approved | not-started | chosen |
| [debrief](debrief/README.md) | Level complete: kills, credits, bonuses, grade | approved | not-started | chosen |
| [controls](controls/README.md) | Keyboard and gamepad mapping, remapping, auto-fire | approved | in-progress | n/a |
| [options](options/README.md) | Video, audio, controls (remapping, auto-fire) and gameplay settings | approved | in-progress | chosen |
| [credits](credits/README.md) | Scrolling credits including the CC-BY attributions | approved | not-started | chosen |

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

## Concept art

Concept [round 08](../concept-rounds/round-08/README.md) — the shared UI kit; generator `tools/concept/ui_r08.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/ui-kit-r08-a.png](concept/ui-kit-r08-a.png) | Shared UI kit: glass widgets (panels, menu states, chips, sliders, toggles, tabs, dialogs), metal HUD widgets, and bitmap font specimens 8×12 / 10×20 / 20×30 with the full character set | chosen |

## Implementation

- [ ] Screen/state machine for the flow above
- [ ] Shared UI kit: panels, buttons, lists, bars, portrait frame, bitmap fonts
- [ ] Keyboard and gamepad navigation on every screen

## Decisions

- 2026-09-30: Controls are a UI part (`controls/`), since remapping lives in the options screen.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-09-30: Fonts were not scaled ×1.5 (that would give 12×12 / 12×24 / 24×24): body text uses 10×20 so the wider panels fit more text per line.
- 2026-10-01: Concept round 06: out-of-game screens use the glass-over-scene style of main menu A; bevelled metal is reserved for the in-game HUD.
- 2026-10-01: Concept round 08: accepted.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../reviews/acts-1-2/README.md).
