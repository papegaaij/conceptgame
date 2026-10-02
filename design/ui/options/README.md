---
title: Options
design: approved
implementation: done
art: final
depends-on: [../controls, ../../art-direction]
updated: 2026-10-02
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
    desktop (2× = 1920×1080 on a 1440p, 3× = 2880×1620 on a 4K desktop) and keeps the
    letterboxed scaling at any size.
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

Production art, UI batch part U2 (for concept round 13, opened by part U3): no art of its own; the screen draws the production glass kit ([tools/art/ui_kit.py](../../../tools/art/README.md)); its capture is on the pause sheet ([pause-final-r13-a](../pause/concept/pause-final-r13-a.png)). Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/options-capture-final-r13-a.png](concept/options-capture-final-r13-a.png) | Game capture: the Gameplay tab from the main menu (tabs, sliders with their knobs, chips) over the dimmed title scene | chosen |

## Implementation

- [x] Four tabs with keyboard/gamepad navigation
- [x] Remapping with conflict swap; reset to defaults
- [x] Settings persisted separately from save slots
- [x] Reachable from main menu and pause
- [x] Display mode: borderless full screen and resizable window, toggled at runtime (Alt+Enter, F11) without losing state; mode, window size/position and monitor persisted
- [x] Display mode switch in the Video tab
- [x] Sound test in the Audio tab

## Decisions

- 2026-10-01: Screen added for the Acts 1–2 vertical slice (concept round 08).
- 2026-10-01: Concept round 08: accepted.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-01: Full-screen mode with a runtime toggle added (user requirement, noticed in the tech spike). Proposed details for review: borderless full screen rather than a video mode switch, Alt+Enter and F11 as toggle keys, first start in full screen.
- 2026-10-01: Display-mode details accepted by the user (borderless full screen, Alt+Enter and F11, first start in full screen).
- 2026-10-01: Display modes built in M0. The settings file is a Java properties file
  `settings.properties` in the platform's config directory (Linux `$XDG_CONFIG_HOME` or
  `~/.config/terran-vanguard/`, Windows `%APPDATA%\Terran Vanguard\`, macOS
  `~/Library/Application Support/Terran Vanguard/`); no new dependency, and unreadable values fall
  back to the first-start defaults. The window size is measured against the monitor's work area
  (the desktop minus task bars and docks), so on a 4K desktop with a top bar the window opens at
  3× = 2880×1620 (4× does not fit beside the bar); the example above was corrected accordingly. A remembered
  window whose centre is on no monitor any more opens at the default size instead. The Video-tab
  switch is a separate item, since the options screen comes with M3.
- 2026-10-02: M3 part A (`vanguard.game.screen.OptionsScreen`, `OptionTabs`, `Remapping`). The
  layout of options-r08-a over the dimmed title scene, the same screen from the main menu and the
  pause menu; up/down select, left/right change, Q / E or the bumpers switch tabs, Back closes and
  writes the settings file (every change applies at once). Video: display mode (full screen /
  window, the same switch as Alt+Enter and F11), scaling (integer + letterbox, or sharp-bilinear:
  a shader that samples as an integer pre-scale with bilinear filtering does and fills the window
  at any scale), CRT scanlines (off / on: the lower half of every pixel row darkened by 35 %, from a
  2× scale up). Audio: master, music, effects and radio-blip volumes in 5 % steps, applied live
  through the mixer to the playing music, the looped ambience and every new sound; a change plays a
  menu blip (the radio slider a typing blip) at the new level. Controls: the remapping table
  (primary, alternative, gamepad for move up/down/left/right, fire, special, precision, dash and
  pause; the move actions keep the stick and the D-pad), auto-fire, stick dead zone (5–50 %,
  default 20 %) and reset to defaults; see [controls](../controls/README.md). Gameplay: text speed
  (10–90 characters/s, default 30: the radio's typing; briefings type at twice it), screen shake (0–100 %, stored only: the game
  has no screen shake yet) and flash reduction (the white hit flash of loot targets and the ship's
  invulnerability blink at 35 %). Settings file keys: `video.scaling` (`integer` /
  `sharp-bilinear`), `video.scanlines`, `audio.master|music|effects|radio` (0..1),
  `controls.auto-fire`, `controls.dead-zone`, `controls.<action>.primary|alternative` (libGDX key
  names, `none`), `controls.<action>.gamepad` (button names), `gameplay.text-speed`,
  `gameplay.screen-shake`, `gameplay.flash-reduction`; an unreadable value falls back to its own
  default. Defaults: every volume 100 % (the game's own mix sets the levels between the buses).
  The sound test is not built yet (new open item). The mock's extra rows (resolution, VSync,
  brightness, interface volume, music style, floating credit numbers, radio subtitle box, HUD
  brightness, pause on focus loss, language) are not in this document and were not built.
- 2026-10-02: The text speed is the radio's; briefings type at twice it (user decision, see
  [briefing](../briefing/README.md)).
- 2026-10-02: M3 close-out: the sound test (`vanguard.game.audio.SoundTest`) is two rows at the
  bottom of the Audio tab. SOUND TEST: MUSIC lists the seven tracks the game has (Terran Vanguard,
  Situation Room, Dry Dock, Coalition Rising, Mission Complete, Mission Failed, Game Over) with
  where each plays; SOUND TEST: EFFECTS lists every other sound effect with the volume it follows
  (effects or radio blips). Left/right pick (wrapping round), confirm plays: a theme loops until
  confirm stops it or another track starts, a jingle plays once; an effect plays once (an effect
  already looping, the ambience under a paused level, is heard as it is). Tracks play solo: the
  music of the screen below (the title theme, a paused level's theme) is silent while one plays,
  and the test stops when the screen closes. The master, music, effects and radio volumes apply,
  also while a track plays. Tests `SoundTestTest`, `OnceStreamTest`, `OptionTabsTest`.
- 2026-10-02: Production art, UI batch part U2: the options screen draws the production glass kit:
  tabs (the active one amber with its underline), the header rule, list-row bands under the key
  bindings, chips and sliders with the recessed track, lit fill and steel knob; layout and fonts
  unchanged. Capture proposed for round 13 (on the pause sheet); `art` stays `chosen`.
- 2026-10-02: Concept round 13 closed (user decision): the options screen in the production glass kit approved as **final**; it has no art of its own, so `art: final`.
