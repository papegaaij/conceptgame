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
| [main-menu](main-menu/README.md) | Title screen, continue/new/load, options, credits, quit | approved | done | final |
| [briefing](briefing/README.md) | Story briefing before each level: portraits, typed text, objectives | approved | done | chosen |
| [hangar](hangar/README.md) | Ship configurator: intel, shop, loadout, repair, save, launch | approved | done | chosen |
| [hud](hud/README.md) | In-level side panels: status, weapons, special, radio chatter, progress | approved | in-progress | final |
| [pause](pause/README.md) | Pause menu during a level | approved | done | final |
| [debrief](debrief/README.md) | Level complete: kills, credits, bonuses, grade | approved | done | final |
| [controls](controls/README.md) | Keyboard and gamepad mapping, remapping, auto-fire | approved | in-progress | n/a |
| [options](options/README.md) | Video, audio, controls (remapping, auto-fire) and gameplay settings | approved | done | final |
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

Production art, UI batch part U2 (for concept round 13, opened by part U3): the glass kit rendered by [tools/art/ui_kit.py](../../tools/art/README.md) into `assets/sprites/ui/` and drawn by `vanguard.game.ui.Glass`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/ui-kit-final-r13-a.png](concept/ui-kit-final-r13-a.png) | Review sheet: the widgets as the screens draw them over the darkened title scene (panels with trim and corner tabs, header rules, list rows with the selection and cursor, menu items, chips on / off / locked, NEW tag, slider, toggle, tabs, pips, chevron, the confirm dialog with its holo brackets, callouts, amber card frame, inset, key-hint plate), a 2× detail and every piece with its size | chosen |
| [concept/ui-kit-capture-final-r13-a.png](concept/ui-kit-capture-final-r13-a.png) | Game capture: the quit confirmation over the main menu | chosen |

Production art, UI batch part U3 (concept round 13): the bitmap fonts rendered by [tools/art/fonts.py](../../tools/art/README.md) into `assets/fonts/`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/fonts-final-r13-a.png](concept/fonts-final-r13-a.png) | Review sheet: the label 8×12, body 10×20 and heading 20×30 fonts, each glyph of the 127-character set at 2× on its cell grid with the baseline marked, and sample lines from the game's texts at 1× | chosen |

## Implementation

- [x] Screen/state machine for the flow above
- [x] Shared UI kit: panels, buttons, lists, bars, portrait frame, bitmap fonts (glass pieces: `tools/art/ui_kit.py`; fonts: `tools/art/fonts.py`)
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
- 2026-10-02: Production art, UI batch part U2: the glass kit is production art
  (`tools/art/ui_kit.py`, `assets/sprites/ui/`, on the shared sprite pages) and `Glass` draws it
  instead of flat fills, with the same layouts and fonts: the glass body (stepped sheen along the
  top, drawn at the panel's opacity) under a separate trim nine-patch (a 2 px rounded metal rail,
  lit top-left, with the four corner tabs), an amber trim for the selected difficulty card, the
  dialog's trim with cyan holographic corner brackets, the recessed inset, the header rule with its
  end cap, the fading amber selection bar (dithered along its length only, so stretching keeps it
  clean), faceted cursors per font, chips and tabs as pixel bevels, the slider's knob, a lit bar
  cell tinted per use, list rows, the key-hint plate, a tag, scroll markers, the trait diamond, the
  rank chevron and the hangar's holographic callouts. Fills remain only for lines, grids, ticks and
  the screen's dimming. The portrait frames are the trim nine-patch, so the shared-kit item is
  ticked. Review files proposed for round 13; `art` stays `chosen`.
- 2026-10-02: Production art, UI batch part U3: the bitmap fonts move from the placeholder script
  `tools/concept/ui_assets.py` to `tools/art/fonts.py` (`Source` chunk on every page). Same font,
  sizes, cells, baselines (label 9, body 15, heading 24) and line heights, so no layout moves: the
  114 glyphs the fonts had are pixel for pixel the same (FreeType's hinted 1-bit raster was already
  clean, and no glyph was clipped by its cell). Each glyph is now placed on the cell's baseline
  explicitly and may overflow the cell (accents over capitals). The set grows to 127 characters:
  the en dash, the typographic quotes ‘ ’ “ ”, → and ≈ (used by the data and the code but missing)
  and the upper-case É È Ë Ü Ö Ä (the screens upper-case their texts). `fonts.py --check` scans
  every data file and the game's and content's string literals (also upper-cased) and fails on a
  character without a glyph or a capital, digit or lower-case letter off the shared rows. Review
  sheet proposed for round 13; `art` stays `chosen`.
- 2026-10-02: Concept round 13 closed (user decision): the glass UI kit (`tools/art/ui_kit.py`) and the three bitmap fonts (`tools/art/fonts.py`) approved as **final**; this doc's `art` stays `chosen`, since not every screen under it is final (hangar, briefing, credits).
