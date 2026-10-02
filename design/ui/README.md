---
title: User interface
design: approved
implementation: in-progress
art: chosen
depends-on: [../art-direction, ../systems]
updated: 2026-10-02
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
| [main-menu](main-menu/README.md) | Title screen, continue/new/load, options, credits, quit | approved | done | chosen |
| [briefing](briefing/README.md) | Story briefing before each level: portraits, typed text, objectives | approved | done | chosen |
| [hangar](hangar/README.md) | Ship configurator: intel, shop, loadout, repair, save, launch | approved | done | chosen |
| [hud](hud/README.md) | In-level side panels: status, weapons, special, radio chatter, progress | approved | in-progress | chosen |
| [pause](pause/README.md) | Pause menu during a level | approved | done | chosen |
| [debrief](debrief/README.md) | Level complete: kills, credits, bonuses, grade | approved | done | chosen |
| [controls](controls/README.md) | Keyboard and gamepad mapping, remapping, auto-fire | approved | in-progress | n/a |
| [options](options/README.md) | Video, audio, controls (remapping, auto-fire) and gameplay settings | approved | done | chosen |
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
Level ─► Pause ─► Resume / Restart / Options / Abort to hangar / Quit
Level ─► Mission failed ─► Retry / Back to hangar / Quit      (hard, no retry left: Game over ─► Main menu)
```

### Shared UI rules

- Internal resolution 960×540, integer scaled where the display allows; the
  [art direction](../art-direction/README.md) owns the details. Bitmap fonts: 8×12 for labels,
  radio subtitles and control prompts, 10×20 for body text, 20×30 for headings. A HUD well's
  196 px of text hold 24 label characters (radio lines are written for 22) or 19 body characters.
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

- [x] Screen/state machine for the flow above
- [ ] Shared UI kit: panels, buttons, lists, bars, portrait frame, bitmap fonts
- [ ] Keyboard and gamepad navigation on every screen
- [ ] Mouse support on every out-of-game screen (hover selects, click confirms, wheel scrolls lists, right-click or a back button goes back), with the hangar's panels and the options sliders and remapping usable by mouse; the in-level game stays keyboard/gamepad only — **later: M6** (see the [roadmap](../tech/roadmap/README.md))

## Open questions

- None open.

## Decisions

- 2026-09-30: Controls are a UI part (`controls/`), since remapping lives in the options screen.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-09-30: Fonts were not scaled ×1.5 (that would give 12×12 / 12×24 / 24×24): body text uses 10×20 so the wider panels fit more text per line.
- 2026-10-01: Concept round 06: out-of-game screens use the glass-over-scene style of main menu A; bevelled metal is reserved for the in-game HUD.
- 2026-10-01: Concept round 08: accepted.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../reviews/acts-1-2/README.md).
- 2026-10-02: M3 part A. Screen state machine (`vanguard.game.screen.ScreenFlow`): a stack of
  screens of which only the top one is updated and drawn; a screen *opens* another over itself
  (options over the main menu, pause over the level, options over pause) and gets it back
  unchanged with its music when that one goes back, or *replaces* all of them (difficulty → level,
  level → debrief, quit to main menu). Briefing, hangar and the load-game slot list plug in as
  screens in part B; until then New game goes difficulty → Level 01 and the debrief returns to the
  main menu. UI kit (`vanguard.game.ui`): `Glass` draws the chosen kit's glass panel, header,
  menu items (selected / disabled), chips, slider, rank chevron, key hints and the confirm dialog
  with the generator's colours; `Menu` and `Dialog` are the navigation models. The portrait frame
  comes with the briefing (part B), so the kit item stays open. Bitmap fonts: the kit's proposed
  production fonts (DejaVu Sans Mono Bold rasterised 1-bit into 8×12, 10×20 and 20×30 cells by
  `ui_r08.bitmap_glyphs`) are written as BMFont files into `assets/fonts/` by
  `tools/concept/ui_assets.py`; they replace libGDX's built-in font in the HUD, the debrief and
  every menu. Text has the kit's one-pixel shadow on glass. Keyboard and gamepad navigation on
  every screen built so far (menus use fixed keys: arrows / D-pad / left stick, Enter / A,
  Esc / B / Back, Q / E and the bumpers for tabs, with key repeat); the item stays open until the
  part B screens have it too. Mouse: not supported yet (optional per the rules above).
- 2026-10-02: Characters per line (user decision): radio subtitles and control prompts use the
  8×12 font (22+ characters in the 196 px well); body text at 10×20 fits about 19 characters
  there. The rule "a 240 px side panel fits about 22 body characters" is corrected.
- 2026-10-02: Fonts (user decision): permissive font licences (Bitstream Vera, SIL OFL) are allowed
  as a written exception to the CC0/CC-BY rule, for fonts only, recorded in CREDITS.md and named
  on the credits screen (CLAUDE.md). The DejaVu Sans Mono Bold bitmap fonts stay.
- 2026-10-02: M3 part B1: the campaign screens are in the flow: New game → difficulty → intro
  briefing (act title card, act briefing, L01 briefing) → hangar placeholder → Level 01 → debrief →
  next level's briefing, or the hangar while the next level is not built; Continue and Load game
  open the hangar with a save. A destroyed ship opens the mission failed screen over the level
  (Retry / Back to hangar / Quit), or the game over screen on hard with no retry left; both are now
  drawn in the flow above (from [retry](../systems/retry/README.md)), as is Pause's Abort to hangar.
- 2026-10-02: Mouse support for the out-of-game screens is planned for M6 (user decision after playing M3); keyboard and gamepad stay the primary input.
