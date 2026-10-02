---
title: Briefing screen
design: approved
implementation: in-progress
art: chosen
depends-on: [../../story, ../../campaign]
updated: 2026-10-02
---

# Briefing screen

## Summary

Before every level, the story is told in a briefing: character portraits and typed text on a
tactical display, ending with the mission objectives. This is the main storytelling vehicle,
together with in-level radio chatter (see [HUD](../hud/README.md)).

## Design

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ ACT II · HOMEFRONT                           MISSION 10: EVACUATION CORRIDOR │
├───────────────┬──────────────────────────────────────────────────────────────┤
│ ┌───────────┐ │  ╔════════════════════════════════════════════════════════╗  │
│ │           │ │  ║                                                        ║  │
│ │ PORTRAIT  │ │  ║        TACTICAL MAP / MISSION IMAGE (672×240)          ║  │
│ │ 144×144   │ │  ║                                                        ║  │
│ │           │ │  ╚════════════════════════════════════════════════════════╝  │
│ └───────────┘ │                                                              │
│ CMDR OKAFOR   │  "Lancer, the Vrell are in the Nova Lagos outskirts.         │
│ CDF COMMAND   │   The evacuation corridor is still open. Keep them off the   │
│               │   shuttles, and watch your six. Okafor out."█                │
├───────────────┴──────────────────────────────────────────────────────────────┤
│ OBJECTIVES: ▪ Escort the evacuation shuttles  ▪ Bonus: no shuttle lost       │
│                                                 [ENTER] continue  [ESC] skip │
└──────────────────────────────────────────────────────────────────────────────┘
```

The mock shows level 10 *Evacuation Corridor* from [act 2](../../campaign/act-2-homefront/README.md);
the real briefing text is written in each level document.

- A briefing is a script of **pages**. Each page has a speaker (portrait + name), text, and
  optionally a map image. Several speakers can alternate (Okafor, Varga, Rook, intercepted
  transmissions from Vorne or the Choir).
- Text types out at ~60 characters/s with a soft blip. Confirm shows the full page, then
  continues. Skip jumps to the objectives.
- Portraits: 144×144, pre-rendered, three expressions per main character (neutral, grim, fierce).
  Interference/static effect for intercepted transmissions.
- Act start/end briefings may be longer; normal levels are 2–4 pages.
- Briefing text lives with each level in the [campaign](../../campaign/README.md).

## Concept art

Concept [round 08](../../concept-rounds/round-08/README.md) — glass style (out-of-game) per the ui style rule; generator `tools/concept/ui_r08.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/briefing-r08-a.png](concept/briefing-r08-a.png) | Briefing — L10 *Evacuation Corridor*: Okafor portrait, typewriter text, tactical map with the shuttle route, threat summary with direction dial, objectives and hangar teaser | chosen |
| [concept/act-title-r08-a.png](concept/act-title-r08-a.png) | Act title card — "ACT II / HOMEFRONT" in the logo-D chrome over Nova Lagos | chosen |

## Implementation

- [ ] Briefing script format: pages, speaker, text, image, objectives
- [x] Typewriter text with skip and page advance
- [ ] Portrait frame with expressions and transmission-static effect

## Decisions

- 2026-09-30: Briefing comes before the hangar (see [systems](../../systems/README.md)).
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-10-01: Concept round 08: accepted.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-02: M3 part B1 (`vanguard.game.screen.BriefingScreen`, `vanguard.game.briefing.BriefingPager`).
  Briefing text lives in the data: a level's `briefing` (pages of speaker and line, and the hangar
  teaser) in its data.yaml, the act's title card and act briefing in the act's new data.yaml
  (`design/campaign/act-N-slug/data.yaml`, with the act's level range); the READMEs render them.
  The briefing before a level that opens its act starts with the act title card (held 3.5 s,
  confirm skips) and the act briefing, so a new game's intro briefing is the Act 1 title card, the
  act briefing and the Level 01 briefing (7 pages). Layout after briefing-r08-a: the header with
  the act and the mission, the 144×144 portrait (cut from the chosen r04 sheets) with name plate,
  role and page count, the typed text (upper case, 10×20) with a block cursor, the objectives and
  the hangar teaser below; Back skips to the last page, confirm on the last page goes on to the
  hangar. The text types at the Gameplay tab's text speed (the options document: "text speed for
  briefings and radio"; default 30 characters/s, the Design section says ~60: open question), with
  the typewriter blip on every other character, over the briefing theme. The objective lines are
  generated from the level's objectives ("SURVIVE TO THE END OF THE MISSION", "BONUS: DESTROY 80 %
  OF ALL ENEMIES"). The act title card's chrome lettering is rendered by
  `tools/concept/ui_assets.py acts` with the chosen card's chrome treatment; its scene is the title
  scene until a still of the Gagarin yards exists. Not built: map images (no level has one), the
  concept's threat summary (the hangar intel's job, part B2), expressions and the transmission
  static, so the format and portrait items stay open.
