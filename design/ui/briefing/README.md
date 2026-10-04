---
title: Briefing screen
design: approved
implementation: done
art: chosen
depends-on: [../../story, ../../campaign]
updated: 2026-10-04
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
  optionally a map image (672×240 above the text, 4 lines of text below it; a longer page goes on
  over the next screens with the same image, each counted as a page). Several speakers can alternate (Okafor, Varga, Rook, intercepted
  transmissions from Vorne or the Choir).
- Text types out at twice the radio's speed with a soft blip: 60 characters/s at the default
  text speed of 30; the Gameplay tab's text speed scales both. Confirm shows the full page, then
  continues. Skip jumps to the objectives.
- Portraits: 144×144, pre-rendered, three expressions per main character (neutral, grim, fierce);
  a page names its expression in the data, neutral otherwise.
  Interference/static effect for intercepted transmissions.
- Act start/end briefings may be longer; normal levels are 2–4 pages.
- Briefing text lives with each level in the [campaign](../../campaign/README.md).

## Concept art

Concept [round 08](../../concept-rounds/round-08/README.md) — glass style (out-of-game) per the ui style rule; generator `tools/concept/ui_r08.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/briefing-r08-a.png](concept/briefing-r08-a.png) | Briefing — L10 *Evacuation Corridor*: Okafor portrait, typewriter text, tactical map with the shuttle route, threat summary with direction dial, objectives and hangar teaser | chosen |
| [concept/act-title-r08-a.png](concept/act-title-r08-a.png) | Act title card — "ACT II / HOMEFRONT" in the logo-D chrome over Nova Lagos | chosen |

Production art, UI batch part U2 (for concept round 13, opened by part U3): no art of its own; the screen draws the production glass kit ([tools/art/ui_kit.py](../../../tools/art/README.md)), its sheet made from the capture by `tools/art/ui_review.py`. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/briefing-final-r13-a.png](concept/briefing-final-r13-a.png) | Review sheet: the game capture at 1× with a 2× detail of the header, portrait frame and text panel | chosen |
| [concept/briefing-capture-final-r13-a.png](concept/briefing-capture-final-r13-a.png) | Game capture (retaken in part U3): page 1 of the Act 1 intro with its image, the Tether Gate, above the text; Okafor in the trim frame, the objectives and hangar teaser panels, the key-hint plate | chosen |

Production art, UI batch part U3: the briefing images, rendered by [tools/art/briefing_images.py](../../../tools/art/README.md) into `assets/ui/briefing/`; the portraits' expressions are the [characters](../../story/characters/README.md)' review files. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/briefing-images-final-r13-a.png](concept/briefing-images-final-r13-a.png) | Review sheet: the nine 672×240 images, one per page of the Act 1 intro (Tether Gate, outer stations, the L1 strike group, Earth orbit's squadrons, the Stormhawk schematic), Level 01 (the Gagarin yards' rails, the scan of the Skitter and Needler) and Level 02 (the burning south arm's docks, the "yield" pattern and the turret's blind arc) | chosen |
| [concept/briefing-images-final-r20-a.png](concept/briefing-images-final-r20-a.png) | Review sheet, batch "M4 briefing images" ([round 20](../../concept-rounds/round-20/README.md)): the four 672×240 images of Level 03 (the high lanes over the battle site's debris field with the spore carriers seeding Earth; the Spore Bomber, its rising spores and a wide spread, the long-range echo) and Level 04 (the convoy road from Tranquility Base across the rille to the mass-driver terminal, brood pods, walkers in the craters, Hammer flight; the Scuttler's claws and glowing back) | chosen |
| [concept/briefing-images-final-r21-a.png](concept/briefing-images-final-r21-a.png) | Review sheet, M4 part E ([round 21](../../concept-rounds/round-21/README.md)): the two 672×240 images of Level 05 (the nest crater beside the mass-driver line with batteries A–D, Lancer's run over the rim, the sleds; the Polyp Mortar's lob to its marker a second ahead, the ship moving out, an unknown contact in orbit) | chosen |

## Implementation

- [x] Briefing script format: pages, speaker, text, objectives
- [x] A page's image (tactical map or mission image): `image` in the data, drawn above the text
- [x] Typewriter text with skip and page advance
- [x] Portrait frame with the transmission-static effect
- [x] Portrait expressions (neutral, grim, fierce): `expression` in the data, neutral by default

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
  generated from the level's objectives ("SURVIVE TO THE END OF THE MISSION", "BONUS: DESTROY 65 %
  OF ALL ENEMIES"). The act title card's chrome lettering is rendered by
  `tools/concept/ui_assets.py acts` with the chosen card's chrome treatment; its scene is the title
  scene until a still of the Gagarin yards exists. Not built: map images (no level has one), the
  concept's threat summary (the hangar intel's job, part B2), expressions and the transmission
  static, so the format and portrait items stay open.
- 2026-10-02: User decision: briefings type at **twice the radio speed**, 60 characters/s at the
  default text speed of 30 (`GameplaySettings.briefingTextSpeed()`); the text-speed setting scales
  both. This settles the B1 note's open question (30 vs ~60).
- 2026-10-02: M3 close-out (user decision). The script format is data: a level's `briefing` pages
  (speaker and line) and teaser in its data.yaml, the act's title card and act briefing in the act's
  data.yaml, the objective lines generated from the level's objectives
  (`vanguard.content.campaign.BriefingScript`); so that part of the item is done and the page image
  waits for the first briefing image from the art track. The portrait opens through a burst of
  transmission static (0.35 s, fading out) on the first page and whenever the speaker changes, and
  closes through one (fading in) after the last page before the hangar opens
  (`vanguard.game.render.TransmissionStatic`: one noise texture generated at start-up, drawn from a
  random offset each frame with three brighter rolling bands, no allocation per frame). The
  expressions need new portrait art and move to the art track (see the
  [production plan](../../art-direction/production/README.md#order-of-work)). The steady
  interference for intercepted transmissions has no page in Acts 1–2 (the radio's Choir line is
  not a briefing); it comes with the first intercepted briefing.
- 2026-10-02: Production art, UI batch part U2: the briefing draws the production glass kit
  (panels, the trim nine-patch as the portrait frame, header rules, lit bar cells for the bullets
  and the typing cursor, the key-hint plate) over the title scene; layouts and fonts unchanged.
  Review files proposed for round 13; `art` stays `chosen`.
- 2026-10-02: Production art, UI batch part U3 (the two items deferred to the art track). A briefing
  page takes an optional `expression` (`neutral`, `grim`, `fierce`; neutral if not given) and an
  optional `image`, the name of a 672×240 picture in `assets/ui/briefing/`
  (`tools/art/briefing_images.py`); the screen draws the image in the trim frame at the top of the
  text panel and the text below it, 4 lines to a screen: a page longer than that goes on over the
  next screens with the same speaker and image, each counted in the page number (the Level 01
  briefing's first page takes two). Images for every page of the Act 1 intro and of Levels 01
  and 02 (Level 02's wait for its data in M4; its README names them). Expressions set in the data
  where the text calls for them: the Act 1 intro's page 2 ("Eleven days later our outer stations
  stopped answering…") grim, page 4 ("That makes us the line.") fierce; the rest neutral. The
  hangar teaser keeps neutral. Review files proposed for round 13; `art` stays `chosen`.
- 2026-10-02: Concept round 13 closed (user decision): the screen in the production glass kit and the nine briefing images of the Act 1 intro and Levels 01–02 (`tools/art/briefing_images.py`) approved as **final**; `art` stays `chosen`, since the images of the later levels and a still per act for the act title cards do not exist yet.
- 2026-10-03: Briefing images of Levels 03 and 04 (batch "M4 briefing images", `tools/art/briefing_images.py`), one per page, named in the levels' data; the review sheet `briefing-images-final-r20-a` proposed for [round 20](../../concept-rounds/round-20/README.md). `art` stays `chosen`.
- 2026-10-03: Concept round 20 closed (user decision, "both accepted"): the four briefing images of Levels 03 and 04 (`briefing-images-final-r20-a`) approved as **final**; `art` stays `chosen`, since the images of the later levels and a still per act for the act title cards do not exist yet.
- 2026-10-04: Briefing images of Level 05 (M4 part E, `tools/art/briefing_images.py`), one per page, named in the level's data; the review sheet `briefing-images-final-r21-a` proposed for [round 21](../../concept-rounds/round-21/README.md). `art` stays `chosen`.
- 2026-10-04: Concept round 21 (user): Level 05's two briefing images (`briefing-images-final-r21-a`)
  approved as **final**; `art` stays `chosen` as after round 20.
