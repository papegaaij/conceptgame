---
title: Options
design: draft
implementation: not-started
art: proposed
depends-on: [../controls, ../../art-direction]
updated: 2026-10-01
---

# Options

## Summary

Settings screen reachable from the main menu and the pause menu, in the glass style. Four tabs: video, audio, controls and gameplay.

## Design

- **Video:** scaling — integer with letterboxing (default) or smooth sharp-bilinear (see
  [art direction](../../art-direction/README.md)); fullscreen/window; optional CRT scanlines.
- **Audio:** master, music, effects and radio-blip volumes; sound test. No voice volume —
  there is no voice acting (text and radio blips only).
- **Controls:** keyboard and gamepad bindings with remapping (press-a-key capture, conflicts
  swap), **auto-fire toggle (off = hold to fire, the default)**, gamepad dead zone. Mapping
  rules live in [controls](../controls/README.md).
- **Gameplay:** text speed for briefings and radio, screen shake, flash reduction.
- Changes apply immediately and persist with the settings file, not with save slots.

## Concept art

Concept [round 08](../../concept-rounds/round-08/README.md) — glass style; generator
`tools/concept/ui_r08.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/options-r08-a.png](concept/options-r08-a.png) | Options — the four tabs as a 2×2 sheet: video with scaling preview, audio sliders, controls with remapping and the auto-fire toggle, gameplay | proposed |

## Implementation

- [ ] Four tabs with keyboard/gamepad navigation
- [ ] Remapping with conflict swap; reset to defaults
- [ ] Settings persisted separately from save slots
- [ ] Reachable from main menu and pause

## Decisions

- 2026-10-01: Screen added for the Acts 1–2 vertical slice (concept round 08).
