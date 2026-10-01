---
title: Briefing screen
design: review
implementation: not-started
art: chosen
depends-on: [../../story, ../../campaign]
updated: 2026-10-01
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
- [ ] Typewriter text with skip and page advance
- [ ] Portrait frame with expressions and transmission-static effect

## Decisions

- 2026-09-30: Briefing comes before the hangar (see [systems](../../systems/README.md)).
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-10-01: Concept round 08: accepted.
