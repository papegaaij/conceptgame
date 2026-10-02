---
title: Concept round 13 — UI production batch
design: approved
implementation: n/a
art: chosen
updated: 2026-10-02
---

# Concept round 13 — UI production batch

## Summary

The second batch of **final** art ([production plan](../../art-direction/production/README.md)):
the UI batch's three parts, U1 (the in-game HUD), U2 (the glass kit and the out-of-game screens,
the Level 01 north-arm fix) and U3 (portraits, briefing images, intel pictures, themes, fonts),
rendered by the generators in [tools/art/](../../../tools/art/README.md) into `assets/`. Open
[index.html](index.html) in a browser (regenerate with `python3 tools/concept/board.py 13`): one
review sheet per part, built from the files the game loads, game captures, and a chosen / final
listening table for the themes. Every choice is *approve as final* or *redo* (with what to change);
only an approved part gets `art: final`.

## Choices

| # | Part | What is proposed | Review files | Weak spots (Claude) | Outcome |
|---|---|---|---|---|---|
| 1 | [HUD](../../ui/hud/README.md) (U1) | the side panels as bevelled, brushed-steel plates with four corner rivets, the label plate, the LCD and portrait wells, the phosphor fill and readout glow as nine-patches (`tools/art/hud.py`) | `hud-final-r13-a`, `hud-capture-final-r13-a` | the plates are whole 240×540 images (the screen is fixed, so they never stretch) | approved — final |
| 2 | [Glass UI kit](../../ui/README.md) (U2) | the out-of-game screens' glass body and metal trim as separate nine-patches, amber and dialog trims, insets, selection bar, cursors, chips, tabs, knob, bar cell, key-hint plate, callouts (`tools/art/ui_kit.py`) | `ui-kit-final-r13-a`, `ui-kit-capture-final-r13-a` | lines, grids and ticks stay drawn fills | approved — final |
| 3 | [Main menu](../../ui/main-menu/README.md), title scene and logo (U2) | main menu A's hero scene (960×540) and logo D at the production bar (`tools/art/ui_scenes.py`), the difficulty screen in the kit | `main-menu-final-r13-a`, `main-menu-capture-final-r13-a`, `difficulty-capture-final-r13-a` | the act title cards still use the title scene until a still per act exists | approved — final |
| 4 | [Hangar](../../ui/hangar/README.md), tactical map and icons (U2) | the Act 1 tactical map (960×540) and 94 equipment icons in two sizes (`tools/art/icons.py`) | `hangar-final-r13-a`, `hangar-capture-final-r13-a` | one map (Act 1) so far; the parts without a model share one emblem per kind | approved — final |
| 5 | [Hangar intel pictures](../../ui/hangar/README.md) (U3) | the sensor-L2 portraits of the Skitter, Needler, Stinger and Spine Turret (30×30) and the silhouettes of the Gorgon Frigate and Brood Carrier (40×40) (`tools/art/intel.py`) | `intel-final-r13-a` | no capture: the panel shows them only from sensor L2 (out of reach before Level 01) and no level with a boss has data yet; the Gorgon's silhouette at 40 px reads more as a shell than a bell with three necks | approved — final |
| 6 | [Briefing](../../ui/briefing/README.md), screen and images (U2, U3) | the screen in the glass kit; nine 672×240 tactical maps and mission images, one per page of the Act 1 intro and Levels 01–02 (`tools/art/briefing_images.py`), a long page going on over the next screens below its image | `briefing-final-r13-a`, `briefing-capture-final-r13-a`, `briefing-images-final-r13-a` | the planets are dark holographic relief, Earth reads mostly by its rim; the labels use the concept pixel font, not the game's bitmap fonts; Level 01's first page now takes two screens | approved — final |
| 7 | [Debrief](../../ui/debrief/README.md) (U2) | the kit over the dimmed title scene | `debrief-final-r13-a`, `debrief-capture-final-r13-a` | no art of its own | approved — final |
| 8 | [Pause](../../ui/pause/README.md) and [options](../../ui/options/README.md) (U2) | the kit over the dimmed play field | `pause-final-r13-a`, `pause-capture-final-r13-a`, `options-capture-final-r13-a` | no art of their own | approved — final |
| 9 | [Portraits](../../story/characters/README.md) (U3) | every speaker of Acts 1–2 in the chosen style B: Okafor, Rook, Varga in neutral, grim and fierce (briefing 144×144 for Okafor and Varga, radio 72×72), the generic officer and civilian neutral, the Choir's 32-frame loop, animated on the radio (`tools/art/portraits.py`); expressions set in the Act 1 and Level 01 data | `portraits-final-r13-a` (png, gif), `portraits-capture-final-r13-a` | at 72 px grim reads mostly by the lowered head and fierce by the open mouth; Varga's glasses hide her brows, so her expressions are the subtlest | approved — final |
| 10 | [Themes](../../audio/music/README.md) (U3) | the title, hangar and briefing themes from their chosen generator at the final settings: OGG q6, −14 LUFS, `LOOPSTART` / `LOOPLENGTH`, `SOURCE` comment (`tools/art/themes.py`) | `themes-final-r13-a`, *Listening* below | the audio is identical to the chosen files (their generator already used the final settings), so this approves the chosen mixes as they are | approved — final |
| 11 | [Bitmap fonts](../../ui/README.md) (U3) | the three BMFont fonts (8×12, 10×20, 20×30) from DejaVu Sans Mono Bold in `tools/art/fonts.py`: the 114 glyphs unchanged, 13 added (en dash, typographic quotes, →, ≈, accented capitals), a coverage check over the data and the code | `fonts-final-r13-a` | the label font's 9 px face in an 8 px cell leaves wide gaps and cramps M, W and & (the chosen kit's metrics, kept) | approved — final |
| 12 | [Level 01 north arm](../../campaign/act-1-first-contact/level-01-break-at-dawn/README.md) (U2) | the north-arm piece re-rendered so its solar wings end inside the piece (user decision; the rest of the approved backdrop unchanged) | `north-arm-final-r13-a` | only that piece changed | approved — final |

## Notes

- Review file names: `<subject>-final-r13-a.png/.gif` in each part's `concept/`, game captures
  `<subject>-capture-final-r13-a.png`; their `prompts.md` entries say how they are made. Nothing in
  `assets/` is hand-edited.
- The build skips final files when it imports placeholders (`Source` PNG chunk, `SOURCE` OGG
  comment; the music now goes through the same rule) and checks the atlas budgets: the shared
  sprite pages, with the HUD, the kit, the icons, the portraits and the intel pictures, pack into
  one 2048×1024 page (8 MiB of 32).
- The data gained two optional fields: `expression` on briefing pages and radio lines (neutral,
  grim, fierce) and `image` on briefing pages ([architecture](../../tech/architecture/README.md)).

## Listening

Each theme as chosen (round 08) and as rendered for the game; integrated loudness and the loop
(start + length, in samples at 44.1 kHz). The final files carry a `SOURCE` comment and are
otherwise the same audio.

| Theme | Chosen | Final | LUFS chosen → final | Loop |
|---|---|---|---|---|
| Title | [chosen](../../audio/music/concept/title-theme-full-r08-a.ogg) | [final](../../../assets/music/title-theme.ogg) | −14.0 → −14.0 | 357000 + 5376000 |
| Hangar | [chosen](../../audio/music/concept/hangar-theme-full-r08-a.ogg) | [final](../../../assets/music/hangar-theme.ogg) | −14.0 → −14.0 | 264600 + 6585600 |
| Briefing | [chosen](../../audio/music/concept/briefing-theme-r08-a.ogg) | [final](../../../assets/music/briefing-theme.ogg) | −14.0 → −14.0 | 238140 + 3810240 |

## Decisions

- 2026-10-02: Round opened (UI batch, parts U1–U3).
- 2026-10-02: Round closed (user decision): all twelve parts approved as **final**; the docs whose art is now entirely final are `art: final` (HUD, main menu, debrief, pause, options; Level 01 again with the north arm), the docs that also cover art not final yet record the final assets in their Decisions and keep `chosen` (UI with the kit and fonts, hangar, briefing, characters, music).
