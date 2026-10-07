---
title: Briefing screen
design: approved
implementation: done
art: chosen
depends-on: [../../story, ../../campaign]
updated: 2026-10-07
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
│                                            [ENTER] continue  [ESC] main menu │
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
  continues.
- **Back (Esc / B) quits to the main menu** (user decision 2026-10-04), on the title card too,
  after the kit's confirm dialog ("QUIT TO MAIN MENU?", as the hangar's quit asks). It leaves the
  campaign as it is: after a won level the briefing writes the autosave the hangar would have
  written as it opened, so the level stays won and Continue goes on in the hangar; a new game's
  intro briefing has nothing to keep yet and writes no save (an older campaign's autosave stays).
  Back no longer skips to the objectives; confirm still shows each page at once.
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
| [concept/briefing-images-final-r23-a.png](concept/briefing-images-final-r23-a.png) | Review sheet, M4 part F ([round 23](../../concept-rounds/round-23/README.md)): the two 672×240 images of Level 06 (the far side across the terminator, the silent settlements, Daedalus Rim's lit domes, Lancer's run into the dark by headlight; the Mantis at the screen edge sweeping its beam, side-firing guns reaching it, the Coilwyrm coming round behind the ship) | chosen |
| [concept/briefing-images-final-r25-a.png](concept/briefing-images-final-r25-a.png) | Review sheet, M4 part G (round 25): the two 672×240 images of Level 07 (the Brood Carrier holding at L1 with its escort screen, the overrun picket and the pods on Luna traced back to it; the overhead scan: nose-down on high air over the ship with its shadow and a bay pair open, only missiles reaching up, the turn in place, broadside at the ship's level with a pair open and the plate iris open over the lime core) and the four of the Act 1 outro, one per page (the carcass drifting apart at L1 with Earth beyond; Daedalus Rim still empty, lights on, the file open; the second fleet's track from beyond the Moon into Earth's atmosphere; Aegis Wing reassigned to Earth defence, Rook's craft on Lancer's wing) | chosen |
| [concept/briefing-images-final-r30-a.png](concept/briefing-images-final-r30-a.png) | Review sheet, M5 part B (round 30): the four 672×240 images of the Act 2 intro, one per page (the landers' burning trails coming down through the cloud deck over the Gulf of Guinea with the CDF tracking overlay counting them; the CDF global display with the three landing zones and the act's fronts: the cities, the Atlantic sea lanes, the Arctic relay chain; a Nova Lagos street from rooftop height, people on the low roofs looking up as Lancer passes low; Aegis Wing and Rook's Ember on a coastal airbase at dusk, Nova Lagos burning 40 km off) and the two of Level 08 (the night route over the harbour, the elevated highways, the tower district and the Third Mainland highway to the Ikoyi shelters, the walkers heading for them, Lancer and Rook; the Creeper on a low roof with its aimed five-way fan, a convoy's fans 0.5 s apart, the anti-ground ×2 marker) | chosen |
| [concept/briefing-images-final-r31-a.png](concept/briefing-images-final-r31-a.png) | Review sheet, M5 part C (round 31): the two 672×240 images of Level 09 (`level-09-arcology-district`: the route over the arcology district with the three node clusters, the Okonjo Bridge and the Ndidi Arcology; `level-09-node-scan`: the Hive Node's iris cycle and the Ravager's pounce, from the production sprites) | chosen |

## Implementation

- [x] Briefing script format: pages, speaker, text, objectives
- [x] A page's image (tactical map or mission image): `image` in the data, drawn above the text
- [x] Typewriter text with skip and page advance
- [x] Back (Esc / B) asks, then quits to the main menu, keeping the campaign (the won level's
      autosave; no save for a new game's intro): `vanguard.game.briefing.BriefingExit`, tested in
      `BriefingExitTest`
- [x] Portrait frame with the transmission-static effect
- [x] Portrait expressions (neutral, grim, fierce): `expression` in the data, neutral by default
- [x] Level 06's two images rendered again for round 23's outcomes: the Coilwyrm at spacing 0.5
      (overlapping) and the Mantis's beam and telegraph from its head
- [x] Level 07's two images and the Act 1 outro's four (one per page, part G's D3), named in the
      level's and the act's data; `BriefingLayoutTest` checks every level page's and outro page's
      image at the screen's size
- [x] The Act 2 intro's four images and Level 08's two (one per page, D3 of M5 part B), named in
      the act's and the level's data; `BriefingLayoutTest` passes with them

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
- 2026-10-04 (user decision): Esc in the briefing did nothing useful (it skipped to the last
  page), so the player had to go on to the hangar to reach the main menu. Back now quits to the
  main menu after a confirm dialog and keeps the campaign (see Design); it no longer skips. The
  confirm dialog and the autosave on leaving are main-agent choices, for the user to confirm.
- 2026-10-04: Concept round 21 (user): Level 05's two briefing images (`briefing-images-final-r21-a`)
  approved as **final**; `art` stays `chosen` as after round 20.
- 2026-10-04: Briefing images of Level 06 (M4 part F, straight to production per user decision D8,
  `tools/art/briefing_images.py`), one per page, named in the level's data; they show the
  production Mantis, Coilwyrm, headlight cone and a Daedalus dome (`mantis.py`, `coilwyrm.py`,
  `l06_darkness.py`, `backdrop_l06.py`); the review sheet `briefing-images-final-r23-a` proposed
  for [round 23](../../concept-rounds/round-23/README.md). `art` stays `chosen`.
- 2026-10-05: Concept round 23 (user decision): Level 06's two briefing images
  (`level-06-daedalus-rim`, `level-06-edge-scan`) accepted. **Pending**: both are to be rendered
  again for the round's other outcomes, the Coilwyrm's spacing of 0.5 (an overlapping chain of
  about 330 px instead of beads with gaps) and the Mantis's beam and telegraph starting at its
  head (its eye), not its centre; `art` stays `chosen`.
- 2026-10-05: Level 06's two images rendered again for round 23's outcomes
  (`tools/art/briefing_images.py`, sheet `briefing-images-final-r23-a` rebuilt). `level-06-edge-scan`:
  the Coilwyrm at the data's spacing of 0.5, an overlapping chain of about 330 px on the same loop,
  with the head's path history dashed beyond the tail so the image still shows it coming round; the
  Mantis's telegraph and beam start at its eye (`sweep.origin`) in the chosen charged-lance look
  (`tools/art/mantis_beam.py`: its beam, tip and eye-ring sprites, the 70° wedge from its generator
  code at the image's 240 px reach, centred on the bearing to the ship), the beam 30 % into the
  sweep; "its beam sweeps across you" moved inside the new wedge. The wedge is no longer drawn in the
  lines layer, so that layer's 16 colours are no longer pulled toward its red: the labels and arrows
  show their intended red, violet, green and cyan. `level-06-daedalus-rim` shows neither unit and
  came out pixel-identical. `art` stays `chosen`.
- 2026-10-05: Briefing images of Level 07 and the Act 1 outro (M4 part G step A4, straight to
  production as part F's D8, `tools/art/briefing_images.py`), one per page (D3 for the outro), named
  in the level's and the act's data: `level-07-l1-carrier`, `level-07-overhead-scan`,
  `act-1-outro-carcass`, `act-1-outro-daedalus-rim`, `act-1-outro-second-fleet`,
  `act-1-outro-rook`. They show the production Brood Carrier (`brood_carrier.py`: composed as the
  game draws it, nose-down and broadside, a turn frame; part G's D1 for the three stages), its
  break-up chunks and Level 07's ichor clouds (`brood_carrier_death.py`, `backdrop_l07.py`),
  Level 06's Daedalus gate and domes, Rook's radio portrait (`portraits.py`) and his craft from the
  chosen round-09 render (`vfx_r08.py`; no production wingman sprite until the escort slot is built).
  The earlier seventeen images and sheets re-render byte-identical. Review sheet
  `briefing-images-final-r25-a` proposed for round 25. `art` stays `chosen`.
- 2026-10-05: Round 25 closed (user): the two Level 07 images and the four Act 1 outro images
  approved as **final** as they are; `art` stays `chosen`, as after round 20 (the images of the
  later levels and the act title cards' stills do not exist yet). The carcass image still shows the
  head chunk's eye lights from before the carcass fix; a trial re-render with the current death
  art moved the chunks (one over the header's "AFTER"), so the approved file was kept and a
  re-render is left for a later round.
- 2026-10-05: `act-1-outro-carcass` re-rendered with the current carcass (the round-25 close left
  it for later): the eight break-up chunks now take their last tumble frame (the cut flesh cooled,
  no lime glow, no eye lights) instead of a random one, and only their placement changed. The
  image had drawn the death data's offsets with y down although they are y up, so the carrier was
  mirrored top to bottom; the pieces now assemble the right way up, each moved by its drift less
  the carcass's common drift (1.6× across, 1× up and down, 4 px high), so they come apart round
  the bracket's middle, all inside it, clear of the title and the labels. Scale, labels, colours,
  Earth, Luna, the ichor clouds and the debris dots are unchanged; the other twenty-two images stay
  byte-identical. Review sheet `briefing-images-final-r25-a` rebuilt.
- 2026-10-06: Briefing images of the Act 2 intro and Level 08 (M5 part B, straight to production
  per D6, `tools/art/briefing_images.py`), one per page (D3), named in the act's and the level's
  data: `act-2-landfall`, `act-2-front-lines`, `act-2-over-home`, `act-2-scramble`,
  `level-08-nova-lagos`, `level-08-walker-scan`. They show the production Creeper with its gland
  glow (`creeper.py`, the walk frame at heading 4), the Vrell orb (`enemy_bullets.py`), the bomb
  rack's shot (`weapon_fx.py`), the Stormhawk and Rook's craft, now the production Ember
  sprite (`rook.py`; the Act 1 outro image keeps the round-09 render it was approved with). The
  globe display is a flat projection of hand-drawn coastlines, enough to read the continents; the
  street view a perspective drawn in the display's lines. The earlier twenty-three images and their
  sheets re-render byte-identical. Review sheet `briefing-images-final-r30-a` proposed for round
  30; `art` stays `chosen`.
- 2026-10-07: From the Level 08 capture (round 30). From Act 2 on, a level's briefing page fits
  one screen (4 lines below its image), as every act page already does; Act 1's level pages were
  written and voiced to go on over the next screens and keep doing so, as the act outros do
  (`BriefingLayoutTest.anActPageAndALevelPageFromAct2OnFitOneScreen`). An act outro's NEXT panel and
  its last page's hint follow the route: the next act's title card and the next briefing when the
  next level opens an act with data (`ACT II: HOMEFRONT, THEN THE BRIEFING FOR MISSION 08: NEON
  SKYLINE`, `ENTER TO ACT II`), the next briefing within an act (`--act-end`), the hangar only when
  the next level has no data (`OutroOnwardTest`).
- 2026-10-07: `level-08-walker-scan` re-rendered with the Creeper's readability lift (round 30:
  lighter chitin and legs, back pores, light rim), so the scan shows the Creeper the level flies; the
  other twenty-eight images stay byte-identical. Review sheet `briefing-images-final-r30-a` rebuilt.
- 2026-10-07: [Concept round 30](../../concept-rounds/round-30/README.md) closed (user,
  2026-10-07): the six briefing images of the Act 2 intro and Level 08 approved as **final**
  (`level-08-walker-scan` with the Creeper's lift; the rough coastlines, the less holographic
  "over home", the busy convoy fans and the agent's numbers on the images as they are). The rule
  found in the capture is **kept**: from Act 2 on, an act page and a level page each fit one screen
  (4 lines below the image, `BriefingLayoutTest.anActPageAndALevelPageFromAct2OnFitOneScreen`), so
  later acts say less per page or use more pages; Act 1's level pages keep running on over the next
  screens, as written and voiced. `art` stays `chosen`.
- 2026-10-07: Level 09's briefing images (M5 part C, straight to production per D9 = a,
  `tools/art/briefing_images.py`), named in the level's data: `level-09-arcology-district` and
  `level-09-node-scan`; review sheet `briefing-images-final-r31-a` proposed for round 31.
- 2026-10-07: [Concept round 31](../../concept-rounds/round-31/README.md) closed for the briefing
  images (user, 2026-10-07): Level 09's two images, `level-09-arcology-district` (corner label
  "LAST HOUR OF NIGHT") and `level-09-node-scan`, approved as **final** (the small cluster letters
  and the level's numbers on the images as they are). `art` stays `chosen`.
