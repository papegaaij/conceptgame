---
title: HUD
design: approved
implementation: in-progress
art: final
depends-on: [../../player, ../../systems/scoring, ../../art-direction]
updated: 2026-10-03
---

# HUD

## Summary

The play field (480×540) is kept clear. All status information lives in the two 240×540 side
panels. Only boss health, warnings and pickup pop-ups appear in the play field itself. The
left panel is about the mission (score, radio), the right panel about the ship.

## Design

### Layout (960×540; each character cell ≈ 10×20 px)

```
┌────────────────────────┬────────────────────────────────────────────────┬────────────────────────┐
│ MISSION 21             │ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░  DUST COLOSSUS   │ ARMOUR              64 │
│ DUST COLOSSUS          │   (boss bar, only during boss fights)          │ █████████████████░░░░░ │
│                        │                                                │ SHIELD              18 │
│ SCORE                  │                                                │ ███████░░░░░░░░░░░░░░░ │
│             1 204 350  │                                                │ POWER ▮▮▮▮▮▮▮▮▯▯  +15% │
│ CREDITS                │                                                │                        │
│                12 450  │                                                │ WEAPONS                │
│ CHAIN 34          ×2.5 │                                                │ F Scatter Vulcan ■■■□□ │
│ ▰▰▰▰▰▰▰▰▰▰▰▱▱▱▱▱▱▱▱▱▱▱ │                                                │ R Tail Gun       ■□□□□ │
│                        │                                                │ L Micro-missile  ■■□□□ │
│ ┌────────┐ ROOK        │                   PLAY FIELD                   │ R Micro-missile  ■■□□□ │
│ │PORTRAIT│ Aegis Wing  │                    480×540                     │ OVERDRIVE ▰▰▰▰▰▱▱  12s │
│ │ 72×72  │ ▂▃▅▇▅▃▂     │                                                │                        │
│ └────────┘             │                                                │ SPECIAL     AIRSTRIKE  │
│ "Six o'clock, Lancer!  │                                                │ ◉◉○○                   │
│  Bandits closing on    │                                                │                        │
│  your tail."           │                                                │ ESCORT  ROOK           │
│                        │                                                │ ███████████████░░░░░░░ │
│                        │                                                │                        │
│                        │                                                │                        │
│                        │                       ▲                        │                        │
│                        │                      ███                       │                        │
│                        │                                                │                        │
│ PROGRESS               │                                                │                        │
│ ▕██████████░░░░░░░░░◆▏ │                                                │                        │
└────────────────────────┴────────────────────────────────────────────────┴────────────────────────┘
```

### Left panel (mission)

| Element | Notes |
|---|---|
| Mission number and name | Top |
| Score | 8 digits, counts up quickly |
| Credits | Earned so far, including level-start balance |
| Chain | Chain count, multiplier and draining window bar (see [scoring](../../systems/scoring/README.md)) |
| Radio | 72×72 portrait with static on open/close, name, subtitle below the portrait up to 3 lines × 22 chars per page; a longer line is paged: each page typed out, then held 3 s (the last page 5 s); a radio line is at most **two pages** of word-wrapped lines (writing rule); queued messages: **timed lines go first** (a line at its time in the level script plays before any waiting event line), an **event line** (a reaction to an escaped enemy, a secret, a kill) waits for a gap, a free radio long enough to play it before the next timed line is due, and is dropped as stale once it has waited more than **6 s**; the lines that close a level (its end, a met secondary objective) wait for a gap too but are never dropped; urgent warnings interrupt. After a won level the scroll runs on and the debrief waits until the radio has shown its last message (the level-end line and whatever is still queued), at most 15 s after the level end |
| Progress | Level progress bar with a boss marker at the end |
| Control prompts | The contextual prompts of the first levels (see [Level 01](../../campaign/act-1-first-contact/level-01-break-at-dawn/README.md#launch-and-control-prompts)): one line each, the action on the left and its keys on the right (the four arrow keys read `ARROW KEYS`), at most three at once; the box is shown only while a prompt is pending |
| Objective tracker | Shown for every primary or secondary objective; hidden in levels without one. Compact box above the progress bar: objective icon and short label (e.g. "DOCKS", "CRAWLERS", "BATTERIES", "HIVE NODES", "SHUTTLES", "RELAY"), then progress as pips or counters (docks, crawler pips, batteries A–D, hive nodes, shuttles, relay integrity bar). A pip flashes green on success and red on a loss or failure; the whole box flashes when the objective is won or lost. Used by [L02](../../campaign/act-1-first-contact/level-02-shipyard-burning/README.md), [L04](../../campaign/act-1-first-contact/level-04-tranquility-run/README.md), [L05](../../campaign/act-1-first-contact/level-05-crater-nest/README.md), [L09](../../campaign/act-2-homefront/level-09-arcology-fall/README.md), [L10](../../campaign/act-2-homefront/level-10-evacuation-corridor/README.md) and [L13](../../campaign/act-2-homefront/level-13-polar-relay/README.md); the secondary objective of [L01](../../campaign/act-1-first-contact/level-01-break-at-dawn/README.md) shows a `KILLS n / 76` counter that turns `DONE` and flashes green when met; [L03](../../campaign/act-1-first-contact/level-03-spore-drift/README.md)'s "nothing gets through" shows `BOMBERS n / 10` (destroyed of all), `DONE` in green when met or `FAILED` in red once one gets through, the box flashing green or red. **Two objectives** (a primary and a secondary at once: [L04](../../campaign/act-1-first-contact/level-04-tranquility-run/README.md), [L05](../../campaign/act-1-first-contact/level-05-crater-nest/README.md), [L13](../../campaign/act-2-homefront/level-13-polar-relay/README.md)): a two-line tracker, the primary on line one and the secondary on line two. Level 04: line one `CRAWLERS` with five pips (green; amber below 50 % HP; a white flash on a hit; a red flash, then dark, when lost), line two `PODS n / 6` (pods killed before they burst, of all), then `DONE` or `FAILED` |

### Left panel layout

The left panel (240×540) is a fixed vertical stack: each region is as tall as the most it can
show and keeps its place whether it is filled or empty, so nothing moves or overlaps. Regions
are 6 px apart, 16 px from the panel's top and bottom edges, and 208 px wide (16 px side
margins). Label plates are 22 px tall; an LCD well has a 2 px dark frame and is 24 px tall for
one line or 60 px for three (lines 18 px apart, text 6 px from the well's sides, so 196 px wide).
Text never runs over its well: a line that is too long is cut off at the well's edge, and the
content rules (radio pages of 3 × 22 characters, one-line prompts) keep that from happening.

| Region | y (px from the top) | Contents |
|---|---|---|
| Mission | 16–60 | plate `MISSION nn`, the level's name below it |
| Score | 66–116 | plate, one-line well, digits right-aligned |
| Credits | 122–172 | plate, one-line well, digits right-aligned |
| Chain | 178–206 | `CHAIN n` and the multiplier on one line, the window bar (6 px) below |
| Radio | 212–378 | plate; the 72×72 portrait with the speaker's name beside it (one word per line, 126 px wide); 4 px below, the three-line subtitle well |
| Control prompts | 384–448 | three-line well: action left (88 px column), keys right |
| Objective tracker | 454–482 | one-line well: label left, count right |
| Progress | 488–524 | plate, progress bar (10 px) |

In a level with two objectives the tracker takes one line from the control prompts' well:
control prompts 384–430 (a two-line well, 42 px), objective tracker 436–482 (a two-line well,
42 px). The layout is fixed per level, so nothing moves during it.

### Right panel (ship)

| Element | Notes |
|---|---|
| Armour | Bar + number; flashes red at 30 % and 15 % |
| Shield | Bar + number; flickers while down after a break |
| Power | Spare power as pips and the resulting regen bonus; glows during overdrive. Output drained by enemies (Void Leech) shows in a warning colour and the gauge flashes when load exceeds output (see [generator](../../player/generator/README.md#enemy-drain-effects)) |
| Weapons | Front, rear, left, right with level pips (5); overdrive timer bar |
| Special | Icon, charges or cooldown ring; greyed out if unavailable in this setting |
| Escort | Rook's (or the heavy drone's) armour; "EJECTED" when down |

(The mock shows the level 21 *Dust Colossus* boss fight; numbers are illustrative.)

### In the play field

- Boss health bar and name at the top during boss fights.
- **Warning** banners ("WARNING — HOSTILES FROM THE REAR") and **edge warnings**: every wave
  that enters from the sides or the rear gets a flashing arrow at that edge of the play field
  at least 3 s ahead, always, often with a radio call (readability rule in
  [enemies](../../enemies/README.md#bullet-readability-rules)).
- With a sensor suite at L2+, extra arrows also track individual off-screen threats (single
  enemies, homing missiles) between waves.
- Small floating numbers for credits picked up. Can be disabled in options.

## Concept art

Concept round 01 — see [round 01](../../concept-rounds/round-01/README.md). AI-generator prompts: [concept/prompts.md](concept/prompts.md). Generated by `tools/concept/hud.py`. Both show the full 640×360 screen at 2×, level 07 *Brood Carrier* state (rear gun and escort slot still empty).

| File | What | Status |
|---|---|---|
| [concept/hud-r01-a.png](concept/hud-r01-a.png) | HUD A — classic metallic bevelled panels with LCD readouts | chosen |
| [concept/rejected/hud-r01-b.png](concept/rejected/hud-r01-b.png) | HUD B — dark glass cockpit panels with neon outlines | rejected — does not fit the style |
| [concept/hud-r02-a.png](concept/hud-r02-a.png) | Round 02: HUD A at 960×540 with 240 px panels in palette B, full element list from this document | chosen |

Concept [round 08](../../concept-rounds/round-08/README.md) — HUD A refresh (metal, unchanged style) with the current element set; generator `tools/concept/ui_r08.py`.

| File | What | Status |
|---|---|---|
| [concept/hud-r08-a.png](concept/hud-r08-a.png) | HUD A refresh — L11 Kraken fight: Rook's radio portrait and subtitle queue, overdrive timer, escort box, boss bar with weak point, edge warning | chosen |

Concept [round 09](../../concept-rounds/round-09/README.md) — generator `tools/concept/vfx_r09.py`.

| File | What | Status |
|---|---|---|
| [concept/edge-warnings-r09-a.png](concept/edge-warnings-r09-a.png) | Edge warnings (side and rear) with flash cycle, sensor threat arrows, wave and boss banners, close-ups and 1× play-field panels | chosen (the edge-warning look itself is superseded by edge-warnings-r11-a) |

Concept [round 11](../../concept-rounds/round-11/README.md) — edge warnings that are harder to
miss (after playing Level 01), over a still frame of Level 01's backdrop placeholders; generator
`tools/concept/ui_r11.py`. Each PNG shows the side and rear warning at 1× and one flash cycle at
2×; each GIF 1.6 s of a left warning at 30 fps. The tones are in [sfx](../../audio/sfx/README.md).

| File | What | Status |
|---|---|---|
| [concept/edge-warnings-r11-a.png](concept/edge-warnings-r11-a.png) | A "big pulse": the round-09 bar and chevrons at about 3× (300 px bar, 20 px chevrons, 2× text) with a soft glow; grows in over 0.2 s, then pulses smoothly between 45 % and 100 % (sheet) | chosen |
| [concept/edge-warnings-r11-a.gif](concept/edge-warnings-r11-a.gif) | A "big pulse" (motion) | chosen |
| [concept/rejected/edge-warnings-r11-b.png](concept/rejected/edge-warnings-r11-b.png) | B "sweeping chevrons": a steady edge bar and three rows of chevrons running in from the edge at 2 px per game frame, fading as they go (sheet) | rejected — A chosen (round 11) |
| [concept/rejected/edge-warnings-r11-b.gif](concept/rejected/edge-warnings-r11-b.gif) | B "sweeping chevrons" (motion) | rejected — A chosen (round 11) |
| [concept/rejected/edge-warnings-r11-c.png](concept/rejected/edge-warnings-r11-c.png) | C "edge glow band": the whole edge lit as a 40 px stepped amber band (25–55 %) with hazard ticks on the edge line, breathing on the flash cycle, chevrons and label in the middle (sheet) | rejected — A chosen (round 11) |
| [concept/rejected/edge-warnings-r11-c.gif](concept/rejected/edge-warnings-r11-c.gif) | C "edge glow band" (motion) | rejected — A chosen (round 11) |

Production art, UI batch part U1 (for concept round 13, not yet opened): the HUD's metal parts
rendered by [tools/art/hud.py](../../../tools/art/README.md) into `assets/sprites/hud/`, reviewed
from the files the game loads. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/hud-final-r13-a.png](concept/hud-final-r13-a.png) | Review sheet: the two 240×540 side-panel plates (bevel, four domed corner rivets, brushed steel) with 4× corners, the label plate, the LCD well, phosphor fill and glow nine-patches with their splits, the portrait well, and the pieces as the game draws them | chosen |
| [concept/hud-capture-final-r13-a.png](concept/hud-capture-final-r13-a.png) | Game capture of Level 01 at 960×540 (radio, control prompts, kill tracker, gauges, weapon box) | chosen |

Concept [round 14](../../concept-rounds/round-14/README.md) — the right panel with the arsenal (no new art: the round-13 kit).

| File | What | Status |
|---|---|---|
| [concept/hud-capture-final-r14-a.png](concept/hud-capture-final-r14-a.png) | Game capture of Level 01 with `--loadout` (Hammer Mortar, Bomb Rack, Autocannon Pod, Side Splitter): the power row and the four weapon rows with pips and the overdrive timer | chosen |

Concept [round 16](../../concept-rounds/round-16/README.md): the escapes tracker and the layer-skip prompt in the game (no new art: the round-13 kit). Prompts: [concept/prompts.md](concept/prompts.md#escapes-tracker-capture-r16-a).

| File | What | Status |
|---|---|---|
| [concept/escapes-tracker-capture-r16-a.gif](concept/escapes-tracker-capture-r16-a.gif) | Game capture of Level 03 (left panel and play field at 1×, 12 fps, two cuts, 6.5 s): the `LOW-AIR` prompt leaving as the first Spore Bomber dies and `BOMBERS 0 / 10` turning `1 / 10`; then `2 / 10` turning `FAILED` in red with the box's red flash as a bomber of the 42 s line escapes | chosen |

## Implementation

- [x] Side panel frames: bevelled metal plates with corner rivets, label plates, LCD wells, bar troughs and phosphor fills from the production art (`tools/art/hud.py`)
- [x] Left panel: mission, score, credits, chain, radio, progress
- [x] Left panel layout: fixed regions without overlap; texts cut off at their well; a test checks the regions and that every prompt and radio line of the content fits, measured with the font's metrics
- [x] Two-objective levels (L04, L05, L13): the two-line tracker and the two-line control prompts' well (shown with a convoy: Level 04's `CRAWLERS` line; Level 05's and Level 13's first lines come with them)
- [ ] Objective tracker: icon, label, pips/counters or integrity bar per level, success/fail flash; hidden in levels without an objective (Level 01's kill counter, Level 02's dock pips, Level 03's bomber counter and Level 04's crawler pips done; the other levels' trackers come with them in M4 and M5)
- [x] Right panel: armour, shield, power (spare-power bar and the regen bonus, glowing in an overdrive), weapons (front, rear, left, right with level pips), overdrive timer
- [x] Right panel: special (M4 part D): a row under the weapons box in their style, `SPECIAL`, the special's 16 px hangar icon, its name and `×` charges; greyed while its strike flies or with no charge left, flashing red when the button is denied
- [ ] Right panel: escort — **later: M5** (Rook's escort slot)
- [x] Radio message queue with portraits, priority interrupts
- [ ] Boss bar, warning banners, edge arrows, pickup numbers

## Open questions

- Should the side panels show a subtle live element, such as a mini-radar or the pilot's
  heartbeat? Nice 90s flavour, but it competes for attention.

## Decisions

- 2026-09-30: Status info in the side panels; the play field stays clean.
- 2026-09-30: Edge warnings for side and rear waves are always shown; the sensor suite only adds
  arrows for individual off-screen threats. Power gauge shows enemy drain.
- 2026-09-30: Concept round 01: HUD **A** (metallic bevelled panels, LCD readouts) chosen; B (glass/neon) rejected as not fitting the style.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-09-30: Concept round 02: HUD A at 960×540 with 240 px panels in palette B confirmed.
- 2026-10-01: Concept round 08: accepted.
- 2026-10-01: Concept round 09: edge warnings chosen.
- 2026-10-01: Objective tracker added to the left panel (user decision) for the objective levels L02, L04, L05, L09, L10 and L13. No concept art for it yet; it follows the HUD A style.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-01: M1 placeholder HUD (`vanguard.game.render.HudPanels`): metal panels drawn in code with colours sampled from `hud-r08-a.png` and libGDX's built-in font until the UI kit and its bitmap fonts exist; right panel armour and shield bars with numbers (the shield bar flickers while down after a break) and the front weapon with level pips; left panel the test sortie's name and attempt number.
- 2026-10-02: M2 placeholder left panel (`vanguard.game.render.MissionPanel`): mission
  number and name, score, credits (launch balance plus earnings), chain with its window bar,
  radio with the speaker's 72×72 portrait (cut from the chosen portrait sheets: Rook, Okafor,
  Varga r04, the Choir r08, static) and the typed subtitle with squelch and typing blips, the
  first section's control prompts, a secondary-objective tracker and the progress bar; in the play
  field the edge-warning arrows and floating credit numbers. Not yet: queue interrupts for urgent
  warnings, warning banners, power, special and escort.
- 2026-10-02: The objective tracker is shown for every secondary objective too, so Level 01's
  `KILLS n / 76` counter stays (user decision); showing it only in the listed objective levels
  was the alternative.
- 2026-10-02: Radio lines keep paging (3 × 22 characters per page, typed, each page held 1.5 s),
  with the writing rule that a line is at most two pages (user decision); shortening every line to
  one page was the alternative.
- 2026-10-02: The two-page rule counts word-wrapped lines, not characters (user decision); Level 07's t=95 line (3 pages) is shortened when Level 07 is built in M4.
- 2026-10-02: Radio pages hold twice as long (3 s, the last page 5 s; was 1.5 s and 2.5 s) and edge warnings start at least 3 s ahead (was 1.5 s), user decision after playing Level 01: with the eyes on the action there was too little time to read the radio, and the side and rear warnings were easy to miss.
- 2026-10-02: Left panel layout (after playing Level 01: "crowded, the instructions overflow
  their box, the boxes are crammed together"): a fixed stack of regions with one gap (see *Left
  panel layout*), the level's name as text under the mission plate as on `hud-r08-a.png`, and
  every text cut off at its well instead of running over it. The control prompts get their own
  three-line region between the radio and the objective tracker: the doc only says "side HUD";
  prompts also appear mid-level next to an objective tracker (L02, L04's escort, L12), so they
  cannot take the tracker's place, and the right panel fills up with power, special and escort.
  Prompts are one line each (action and keys), with the arrow keys as `ARROW KEYS` (the four
  key names did not fit). Not chosen: prompts in the play field (the play field stays clean).
- 2026-10-02: Control prompts keep their own region in the left panel under the radio (user decision); they appear mid-level next to objective trackers too, so they cannot share the tracker's place.
- 2026-10-02: M3 part A: the HUD draws with the UI kit's bitmap fonts instead of libGDX's
  built-in font, in the same regions: 8×12 for the plates, the radio subtitles and the control
  prompts (a 22-character radio line is 176 px, inside the 196 px well; in the 10×20 font it would
  be 220 px), 10×20 for the mission name, the readouts, the chain, the speaker's name, the tracker
  and the right panel's numbers and weapon. `MissionLayoutTest` now measures with these fonts. The
  [ui](../README.md) rule "a 240 px side panel fits about 22 body characters" does not hold for the
  10×20 body font in a 196 px well (19 characters): open question there. Text speed and flash
  reduction from the Gameplay tab apply to the radio and the hit flashes.
- 2026-10-02: Concept round 11 proposed: three edge-warning looks (A big pulse, B sweeping chevrons,
  C edge glow band) in the round-09 warning amber `FFC800`, plus two warning tones in
  [sfx](../../audio/sfx/README.md). The M2 placeholder draws a small red (`FF4030`) arrow, not
  the chosen round-09 look; whichever variant is chosen replaces it.
- 2026-10-02: Concept round 11 (user choice): edge-warning look **A** "big pulse" chosen (the round-09 bar and chevrons at about 3× with a glow, growing in over 0.2 s and pulsing between 45 % and 100 %); B and C moved to `concept/rejected/`. The round-09 sheet stays chosen for the sensor threat arrows and the banners. The game still draws a small red placeholder arrow; it follows A when the HUD art is implemented.
- 2026-10-02: Characters per line settled in [ui](../README.md) (user decision): radio subtitles and
  control prompts use the 8×12 font, as built; body text fits about 19 characters in a well.
- 2026-10-02: M3 part B1: the edge warnings follow the chosen look A "big pulse"
  (`vanguard.game.render.EdgeWarnings`, drawn in code with the generator's geometry: a 6 px
  `FFC800` bar 301 px long at the sides and 341 px at the rear with a blurred glow, three 20 px
  chevrons at 100 / 70 / 42 %, "! LEFT" / "! RIGHT" / "! REAR" in the 10×20 font, growing in from
  the edge's middle over 0.2 s, then pulsing 45–100 % on the 0.267 s cycle); the small red
  placeholder arrow is gone. Each warning that starts plays the edge-warning tone
  ([sfx](../../audio/sfx/README.md)). The label is the 10×20 font, close to the concept's 2× 5×7
  raster font.
- 2026-10-02: M3 part B2, TEMPORARY until M4: the right panel's front weapon shows the Pulse
  Cannon's flown level, and under the weapons a "NOT YET AVAILABLE" list names the fitted items
  the sortie leaves out (other weapons, the special, utility modules), so a player who fitted them
  in the hangar sees why they do not fire. It goes when M4 flies them.
- 2026-10-02: M3 close-out: the radio portrait opens and closes through the transmission static of
  the [briefing](../briefing/README.md) (`vanguard.game.render.TransmissionStatic`): it fades out in
  the first 0.35 s of a message and in over the last 0.35 s of its last page
  (`RadioQueue.sinceOpened()` / `untilClosed()`). Priority interrupts stay open, so the radio item
  does too.
- 2026-10-02: The outro waits for the radio (user decision after playing the final-art build): the debrief follows once the radio has shown its last message, at most 10 s after the level end (was a fixed 5 s, which cut Okafor's level-end line short), so the player can read it.
- 2026-10-02: The outro cap is 15 s instead of 10 s (user decision), so the level-end and secondary-objective lines both fit when they queue together (about 14 s); Earth's limb and the Vrell glow moved later in the data (t=189.6 and t=202.8) so their top edges stay above the screen; the art is unchanged.
- 2026-10-02: Production art, UI batch part U1 (user: "It should be metallic bevelled panels, but
  at the moment it's flat and no texture at all. Also the rivets in the corners are missing."):
  the HUD's metal parts are rendered by `tools/art/hud.py` in the chosen HUD A look and drawn by
  `vanguard.game.render.HudKit` instead of flat fills, in the same regions and with the same
  fonts. The side panels are whole 240×540 plates (the screen is a fixed 960×540, and a
  stretched centre would smear the brushed texture): a 3 px chamfer on a dark chassis, four domed
  rivets in recessed washers, brushed streaks and mottling on palette B's UTC HULL ramp,
  ordered-dithered on the face. The label plate is a fixed 120×22 piece; the LCD well (also every
  bar trough and the weapon box), the portrait well with an idle CRT screen, the phosphor fill
  cell (tinted per bar, segment, pip and the tracker flash) and the readout glow (tinted green or
  amber, 28/255) are nine-patches. Armour and shield are segmented gauges (13 px pitch) as on the
  concepts; chain and progress stay continuous. The key light sits lower than the sprites' (about
  27° instead of 43°) so a 45° bevel reads. Review files proposed for round 13; `art` stays
  `chosen` until the user approves them.
- 2026-10-02: Production art, UI batch part U3: the radio shows the production portraits
  (`tools/art/portraits.py`, [characters](../../story/characters/README.md)) in the radio line's
  `expression` (neutral when not given), the Choir's glyph as its 32-frame loop at 12 fps from the
  moment its message opens. Level 01's lines set grim or fierce where the text calls for it:
  Okafor's "Contacts inbound. Weapons free." and "Contacts on your six, Lancer!" and Rook's
  "Movement on your left!" fierce; Varga's "Those ones are armed." and "That isn't noise…" and
  Okafor's level-end "…The rest are coming." grim; the rest neutral.
- 2026-10-02: Concept round 13 closed (user decision): the HUD's production metal parts (side-panel plates, label plate, LCD and portrait wells, phosphor fill, readout glow; `tools/art/hud.py`) approved as **final**, `art: final`.
- 2026-10-02: M4 part A: the right panel shows the power row (spare power as a bar, the regen bonus as a number, glowing amber during an overdrive), a weapons box with one row per slot (F, R, L, R as in the mock: the weapon's name, a pod named by what it fires, and five level pips) and the overdrive timer row (ten pips and the seconds left). "Not yet available" now lists only the special and the utility modules.
- 2026-10-02: Concept round 14 closed (user decision): the right panel with the power row, the weapon rows and the overdrive timer approved as final.
- 2026-10-02: M4 part B: the tracker shows a group objective (Level 02's docks) as its label ("DOCKS") and a pip per dock, dim while open, green when saved, red when lost, flashing as it changes; a level's timed prompts (Level 02's `GROUND` · `FLY OVER, FIRE`) share the control prompts' well.
- 2026-10-02: M4 part C (choice for review): an escapes objective (Level 03's "nothing gets
  through") shows its enemy's last word in plural as the label (`BOMBERS`) and the units destroyed
  of all the level sends (`n / 10`), then `DONE` in green or `FAILED` in red, the whole box
  flashing green or red as it is won or lost; a timed prompt with a `skip` layer leaves once an
  enemy on that layer is destroyed (Level 03's `LOW-AIR` prompt; Level 02's `GROUND` prompt now
  says `skip: ground` in its data instead of a special case in the game).
- 2026-10-02 (user decision): radio queue priorities, after Level 03's captures showed the lines
  of the Leviathan's first pass playing seconds late behind an escaped bomber's line: timed lines
  go first; an event line waits for a gap that ends before the next timed line is due and is
  dropped as stale after 6 s of waiting (`RadioQueue.STALE_SECONDS`, `RadioQueue.Priority`,
  `vanguard.game.level.RadioSchedule` for the time to the next timed line); the level-end and
  secondary-objective lines are never dropped, since the outro waits for them (our reading of the
  rule). The queue has no clock or randomness of its own. Tests: `RadioQueueTest`, and
  `RadioTimelineTest` for Levels 01–03 with the event lines a player can set off (every timed line
  within 1 s of its time; Level 01's Varga line after the Choir is exempt, it follows that line).
- 2026-10-03 (user decision): the radio reading above accepted as it is: the level-end and secondary-objective lines (`RadioQueue.Priority.CLOSING`) are never dropped.
- 2026-10-03: Concept round 16 closed again (user decision, choice 16): the M4 part C choice for review above accepted as it is: the escapes objective's tracker (`BOMBERS n / 10`, then `DONE` in green or `FAILED` in red with the box flashing) and the timed prompt with a `skip` layer (Level 03's `LOW-AIR`, Level 02's `GROUND` via `skip: ground`).
- 2026-10-03 (user decision, M4 part D): a two-line objective tracker for levels with a primary and
  a secondary objective (L04, L05, L13); it takes one line from the control prompts' well. Level
  04: `CRAWLERS` with five pips (green; amber below 50 %; a white flash on a hit; a red flash, then
  dark, when lost) and `PODS n / 6`, then `DONE`/`FAILED`. The region bounds (prompts 384–430,
  tracker 436–482) follow from the well sizes (main-agent choice).
- 2026-10-03: M4 part D step 2 (main-agent choices): an **urgent** radio line (the Airstrike's
  "Hammer flight, inbound!", which had queued as an event line and could be dropped behind long
  lines) interrupts the line on the radio at once, without the gap, and the interrupted line plays
  again from its start right after it (`RadioQueue.Priority.URGENT`). The two-line tracker is
  drawn when the level has a convoy: line one is the ally's label (`CRAWLERS`) and a pip per unit
  (the `civilian-crawler-pip` sprite tinted green, amber below 50 % HP, white for 6 steps after a
  hit, a red flash for 1.5 s then dark when lost; the line flashes red when the last one is lost),
  line two the secondary objective as in a one-line tracker; the control prompts show two lines
  then.
