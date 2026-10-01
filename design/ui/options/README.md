---
title: Options
design: approved
implementation: not-started
art: chosen
depends-on: [../controls, ../../art-direction]
updated: 2026-10-01
---

# Options

## Summary

Settings screen reachable from the main menu and the pause menu, in the glass style. Four tabs: video, audio, controls and gameplay.

## Design

- **Video:** scaling — integer with letterboxing (default) or smooth sharp-bilinear (see
  [art direction](../../art-direction/README.md)); **display mode** full screen or window
  (see below); optional CRT scanlines.
- **Display mode** (user requirement):
  - **Full screen** is borderless at the desktop resolution of the monitor the game is on (no
    video mode switch, so no flicker and no lost desktop layout); the 960×540 image is scaled
    and letterboxed as in windowed mode.
  - **Window** is resizable; it opens at the largest integer multiple of 960×540 that fits the
    desktop (2× = 1920×1080 on a 1440p or 4K desktop) and keeps the letterboxed scaling at any
    size.
  - **Toggle at any moment** — menus, briefings, hangar and during a level — with **Alt+Enter**
    or **F11** (see [controls](../controls/README.md)), or in this tab. Toggling never pauses or
    restarts anything.
  - The first start opens in full screen. The mode, window size and position, and the monitor are
    remembered in the settings file.
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
| [concept/options-r08-a.png](concept/options-r08-a.png) | Options — the four tabs as a 2×2 sheet: video with scaling preview, audio sliders, controls with remapping and the auto-fire toggle, gameplay | chosen |

## Implementation

- [ ] Four tabs with keyboard/gamepad navigation
- [ ] Remapping with conflict swap; reset to defaults
- [ ] Settings persisted separately from save slots
- [ ] Reachable from main menu and pause
- [ ] Display mode: borderless full screen and resizable window, toggled at runtime (Alt+Enter, F11, Video tab) without losing state; mode, window size/position and monitor persisted

## Decisions

- 2026-10-01: Screen added for the Acts 1–2 vertical slice (concept round 08).
- 2026-10-01: Concept round 08: accepted.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-01: Full-screen mode with a runtime toggle added (user requirement, noticed in the tech spike). Proposed details for review: borderless full screen rather than a video mode switch, Alt+Enter and F11 as toggle keys, first start in full screen.
- 2026-10-01: Display-mode details accepted by the user (borderless full screen, Alt+Enter and F11, first start in full screen).
