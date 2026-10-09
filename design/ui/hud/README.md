---
title: HUD
design: approved
implementation: in-progress
art: final
depends-on: [../../player, ../../systems/scoring, ../../art-direction]
updated: 2026-10-08
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
| Radio | 72×72 portrait with static on open/close, name, subtitle below the portrait up to 3 lines × 22 chars per page; a longer line is paged: each page typed out, then held 3 s (the last page 5 s), and a spoken line's page at least until its [voice](../../audio/voice/README.md) has reached the page's end (the last page until the voice ends); a radio line is at most **two pages** of word-wrapped lines (writing rule); queued messages: **timed lines go first** (a line at its time in the level script plays before any waiting event line), an **event line** (a reaction to an escaped enemy, a secret, a kill) waits for a gap, a free radio long enough to play it before the next timed line is due, and is dropped as stale once it has waited more than **6 s**; the lines that close a level (its end, a met secondary objective) wait for a gap too but are never dropped; urgent warnings interrupt (and cut the playing voice; the interrupted line replays with its voice): the Airstrike's call and Rook's **eject bark** ([wingmen](../../player/wingmen/README.md#radio-barks): exempt from the barks' spacing, never stale, it cuts his own waiting or playing bark). After a won level the scroll runs on and the debrief waits until the radio has shown its last message (the level-end line and whatever is still queued), at most 15 s after the level end |
| Progress | Level progress bar with a boss marker at the end |
| Control prompts | The contextual prompts of the first levels (see [Level 01](../../campaign/act-1-first-contact/level-01-break-at-dawn/README.md#launch-and-control-prompts)): one line each, the action on the left and its keys on the right (the four arrow keys read `ARROW KEYS`), at most three at once; the box is shown only while a prompt is pending |
| Objective tracker | Shown for every primary or secondary objective; hidden in levels without one. Compact box above the progress bar: objective icon and short label (e.g. "DOCKS", "CRAWLERS", "BATTERIES", "NODES", "SHUTTLES", "CONVOY", "RELAY"), then progress as pips or counters (docks, crawler pips, batteries A–D, hive nodes, shuttles, ship pips, relay integrity bar). A pip flashes green on success and red on a loss or failure; the whole box flashes when the objective is won or lost. Used by [L02](../../campaign/act-1-first-contact/level-02-shipyard-burning/README.md), [L04](../../campaign/act-1-first-contact/level-04-tranquility-run/README.md), [L05](../../campaign/act-1-first-contact/level-05-crater-nest/README.md), [L09](../../campaign/act-2-homefront/level-09-arcology-fall/README.md), [L10](../../campaign/act-2-homefront/level-10-evacuation-corridor/README.md), [L11](../../campaign/act-2-homefront/level-11-atlantic-convoy/README.md) and [L13](../../campaign/act-2-homefront/level-13-polar-relay/README.md); the secondary objective of [L01](../../campaign/act-1-first-contact/level-01-break-at-dawn/README.md) shows a `KILLS n / 76` counter that turns `DONE` and flashes green when met; [L03](../../campaign/act-1-first-contact/level-03-spore-drift/README.md)'s "nothing gets through" shows `BOMBERS n / 10` (destroyed of all), `DONE` in green when met or `FAILED` in red once one gets through, the box flashing green or red. **Two objectives** (a primary and a secondary at once: [L04](../../campaign/act-1-first-contact/level-04-tranquility-run/README.md), [L05](../../campaign/act-1-first-contact/level-05-crater-nest/README.md), [L09](../../campaign/act-2-homefront/level-09-arcology-fall/README.md), [L13](../../campaign/act-2-homefront/level-13-polar-relay/README.md)): a two-line tracker, the primary on line one and the secondary on line two. Level 09 (M5 part C): line one `NODES A1 A2 B1 B2 C1 C2`, one **two-character mark** per named node (the last word of its group's name; dim while it lives, struck through in green when destroyed, red and the line flashing red when one gets past and the mission fails), line two `RAVAGERS n / 8` (the bridge waves' Ravagers destroyed of all; 6 on easy, 10 on hard), then `DONE` or `FAILED`. Level 04: line one `CRAWLERS` with five pips (green; amber below 50 % HP; a white flash on a hit; a red flash, then dark, when lost), line two `PODS n / 6` (pods killed before they burst, of all), then `DONE` or `FAILED`. Level 10 (planned, M5 part D, user decision D3 = a): the two-line tracker with **one** objective, line one `SHUTTLES n / 4` (the saveable shuttles still flying; the count turns red and the box flashes red when the last is lost and the mission fails), line two **five small armour bars** in order, Lifeline One to Five (each about 32 px wide, filled by its armour share: green, a white flash on a hit, amber below 50 %, a red flash then dark when lost; Lifeline Three's bar dark from its scripted loss at t=118, the count unchanged; a shuttle **home** after the climb-out shows a full bar in pale mint under a green glow, unlike both a flying and a lost one's). Level 11 (planned, M5 part E, a stated default): the one-line tracker for its secondary objective, `CONVOY` with **three ship pips** right, one per cargo ship in order (Halvorsen, Mbeki, Saint-Laurent: green; amber after the ship's first slam; a red flash, then dark, when it sinks, the box flashing red as the objective fails), turning `DONE` (green) or `FAILED` (red) at the Kraken's death; the debrief's secondary row `HULLS AFLOAT n / 3` |

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

In a level with two objectives, or an objective with a second line (Level 10's armour bars), the
tracker takes one line from the control prompts' well:
control prompts 384–430 (a two-line well, 42 px), objective tracker 436–482 (a two-line well,
42 px). The layout is fixed per level, so nothing moves during it.

### Right panel (ship)

| Element | Notes |
|---|---|
| Armour | Bar + number; flashes red at 30 % and 15 % |
| Shield | Bar + number; flickers while down after a break |
| Power | Spare power as pips and the resulting regen bonus; glows during overdrive. Output drained by enemies (Void Leech) shows in a warning colour and the gauge flashes when load exceeds output (see [generator](../../player/generator/README.md#enemy-drain-effects)) |
| Weapons | Front, rear, left, right with level pips (5); overdrive timer bar. A weapon that cannot fire in the level reads so in its row instead of its pips: a Torpedo Pod in a level without water reads `NO WATER` (M5 part E; it still draws its power), as the Bomb Rack's `NO GROUND` will in a level without ground |
| Special | Icon, charges or cooldown ring; greyed out if unavailable in this setting |
| Escort | Rook's (or the heavy drone's) armour; "EJECTED" when down. A box under the special row in the weapons' style: the `ESCORT` plate, Rook's 16 px hangar icon and `ROOK`, his armour as a bar with the number (flashing red at 30 %, as the player's); after the eject the bar is empty and the well reads `EJECTED` in red. Drawn only while an escort flies in the level (from Level 08, or with `--escort`); otherwise its region stays empty, so nothing else moves |

(The mock shows the level 21 *Dust Colossus* boss fight; numbers are illustrative.)

### In the play field

- Boss health bar and name at the top during boss fights: the sum of the boss's remaining
  part HP; a mid-boss's bar is shorter (Level 05's Gorgon Frigate is the first, M4 part E).
- **Warning** banners ("WARNING — HOSTILES FROM THE REAR") and **edge warnings**: every wave
  that enters from the sides or the rear gets a flashing arrow at that edge of the play field
  at least 3 s ahead, always, often with a radio call (readability rule in
  [enemies](../../enemies/README.md#bullet-readability-rules)). The banner's band sits across the
  upper play field (its middle 164 px below the top edge); in a level with an air escort
  (Level 10) it sits higher, 97–127 px below the top edge, so it never covers the shuttle band
  (stations from 140 px down).
- With a sensor suite at L2+, extra arrows also track individual off-screen threats (single
  enemies, homing missiles) between waves.
- Small floating numbers for credits picked up. Can be disabled in options (Gameplay tab,
  `CREDIT NUMBERS`).

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

Concept round 26 (M4 part H): the wave banners, the sensor suite's threat arrows, the low-armour
flash and the credit-numbers option in the game (no new art: drawn in code in the round-09 look).
Prompts: [concept/prompts.md](concept/prompts.md#hud-warnings-capture-r26-a).

| File | What | Status |
|---|---|---|
| [concept/hud-warnings-capture-r26-a.png](concept/hud-warnings-capture-r26-a.png) | Game captures: Level 02's side wave with its banner, Level 06's rear loop-back banner (the left-and-right one waiting for it), the sensor L2 arrows at a Coilwyrm's body below the screen and at a Skitter above it; the armour gauge's low-armour flash off and on; the Gameplay tab's `CREDIT NUMBERS` row | chosen |

Concept [round 16](../../concept-rounds/round-16/README.md): the escapes tracker and the layer-skip prompt in the game (no new art: the round-13 kit). Prompts: [concept/prompts.md](concept/prompts.md#escapes-tracker-capture-r16-a).

| File | What | Status |
|---|---|---|
| [concept/escapes-tracker-capture-r16-a.gif](concept/escapes-tracker-capture-r16-a.gif) | Game capture of Level 03 (left panel and play field at 1×, 12 fps, two cuts, 6.5 s): the `LOW-AIR` prompt leaving as the first Spore Bomber dies and `BOMBERS 0 / 10` turning `1 / 10`; then `2 / 10` turning `FAILED` in red with the box's red flash as a bomber of the 42 s line escapes | chosen |

Production art for concept round 26 (M4 part H batch): the boss bar's plate, rendered by
`tools/art/boss_bar.py` in the HUD's metal look; prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/boss-bar-plate-final-r26-a.png](concept/boss-bar-plate-final-r26-a.png) | `hud/boss-bar-plate.9.png`: a 64×20 nine-patch steel strip with riveted end caps and a recessed trough (splits L14 R14 T7 B7, content box L11 R11 T7 B7 = the 6 px fill), at 8× with its splits, and round the 400 px and 240 px bars as drawn | chosen |

## Implementation

- [x] Side panel frames: bevelled metal plates with corner rivets, label plates, LCD wells, bar troughs and phosphor fills from the production art (`tools/art/hud.py`)
- [x] Left panel: mission, score, credits, chain, radio, progress
- [x] Left panel layout: fixed regions without overlap; texts cut off at their well; a test checks the regions and that every prompt and radio line of the content fits, measured with the font's metrics
- [x] Two-objective levels (L04, L05, L13): the two-line tracker and the two-line control prompts' well (shown with a convoy: Level 04's `CRAWLERS` line; Level 05's and Level 13's first lines come with them)
- [x] Level 10's tracker (D3 = a, M5 part D): `SHUTTLES n / 4` over five armour bars (a bar drawer
      in `MissionPanel`: the hit flash, amber below 50 %, the red flash and dark when lost, Lifeline
      Three dark after its scripted loss), fitting the 208 px well (`MissionLayoutTest`) — built:
      `MissionPanel.drawShuttleTracker` (n the saveable shuttles alive of `Sortie.saveableAllies()`,
      the line red and flashing as Level 04's when the mission fails), five 32×8 bars 41 px apart
      (`ALLY_BAR_*`, filling the 196 px text width) centred in line two, each in its trough and
      filled by its armour share; a level without a secondary objective (`Secondary.none()`) shows
      no secondary line or one-line tracker, and its debrief no secondary row; a shuttle home after
      the climb-out full in pale mint under a green glow (`MissionPanel.ALLY_HOME`, 2026-10-08)
- [x] Level 11's tracker (M5 part E, step E3c): `CONVOY` with three ship pips (green, amber after
      the first slam, the red flash then dark when sunk), `DONE` or `FAILED` at the Kraken's death,
      fitting the one-line well (`MissionLayoutTest`); the debrief's row `HULLS AFLOAT n / 3` —
      built: `MissionPanel.drawConvoyTracker` (the `cargo-ship-pip` tinted, a pip per damageable
      convoy unit, white for a moment on a hit), `DebriefScreen.hullsRow`; `PartEHudTest`,
      `LevelScreenPartETest`
- [x] The weapon row's `NO WATER` for a Torpedo Pod in a level without `water: true` (M5 part E,
      step E3c: `ShipPanel.NO_WATER` right-aligned in place of the pips, `Hud.idle` from
      `Sortie.mountIdle`; `PartEHudTest`)
- [x] Level 09's tracker: `NODES` with six two-character marks (`A1` … `C2`; the group marks widened from one letter, the line fitting the 208 px well, a layout test) over `RAVAGERS n / 8` — M5 part C (`MissionPanel.markStep`/`markLeft`: a mark's glyph advance + 4 px, 24 px for a pair, right-aligned; Level 05's letters keep their 16 px; `MissionLayoutTest`)
- [x] Objective tracker: icon, label, pips/counters or integrity bar per level, success/fail flash; hidden in levels without an objective (Act 1's done: Level 01's kill counter, Level 02's dock pips, Level 03's bomber counter, Level 04's crawler pips, Level 05's `BATTERIES A B C D` over `NEST n / 30`, Level 06's `MANTISES` escapes and Level 07's bay count; the later levels' trackers are part of those levels' own tickets)
- [x] Right panel: armour, shield, power (spare-power bar and the regen bonus, glowing in an overdrive), weapons (front, rear, left, right with level pips), overdrive timer
- [x] Right panel: special (M4 part D): a row under the weapons box in their style, `SPECIAL`, the special's 16 px hangar icon, its name and `×` charges; greyed while its strike flies or with no charge left, flashing red when the button is denied
- [x] Right panel: escort box (plate, icon, `ROOK`, armour bar and number, the 30 % flash, `EJECTED`; hidden without an escort) — M5 part A
- [x] Radio message queue with portraits, priority interrupts
- [x] Boss bar and name at the top of the play field, shorter for a mid-boss (M4 part E); drawn on
      its production plate `hud/boss-bar-plate` (a nine-patch) once that exists, plainly until then
      (M4 part H)
- [x] Edge-warning arrows and floating credit numbers (M3) and the boss warning banner (M4 part G,
      with track 22 and the klaxon)
- [x] Wave warning banners ("WARNING — HOSTILES FROM THE REAR"), the sensor suite's threat
      arrows, the option to hide the credit numbers (M4 part H); with an air escort the banner
      above the shuttle band (`WaveBanners.ESCORT_CENTRE_Y`, `WaveBannersTest`, 2026-10-08)
- [x] Low-armour flash: the armour readout and gauge flash red at 30 % and faster at 15 % (M4 part H)

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
- 2026-10-03: The radio is spoken ([voice](../../audio/voice/README.md)): the voice starts with
  the message, a page is held until its voice has reached it, the music ducks while the voice
  plays and an urgent line cuts it.
- 2026-10-03: M4 part E (main-agent choice): the boss bar and name sit at the top of the play
  field, not in the side HUD; it shows the sum of the boss's remaining part HP, shorter for a
  mid-boss. The tracker for Level 05 reads `BATTERIES A B C D`, a letter struck through once
  that battery is cleared.
- 2026-10-03: M4 part E, Level 05's tracker built: line one `BATTERIES` and the letters A–D (dim
  while open, struck through in green once cleared, red when lost, the line flashing red as the
  primary fails), line two the *Scorched crater* count `NEST n / 30`, then `DONE` or `FAILED`; the
  batteries' units carry a 1 px outline in the objective colour (amber `FFE04A`, a placeholder
  until the production round).
- 2026-10-05: Round 25 closed (user): the boss warning banner accepted as built (Level 07's
  capture); the warnings item split into what is built (edge arrows, credit numbers, the boss
  banner) and what is not (wave banners, the sensor's threat arrows, the numbers' option).
- 2026-10-05: M4 part H (defaults of the part's plan; choices for review in round 26):
  - **Wave banners** (`vanguard.game.render.WaveBanners`): no data of their own; an edge warning that
    starts opens "WARNING — HOSTILES FROM THE REAR / LEFT / RIGHT" (both sides "LEFT AND RIGHT",
    a side with the rear "REAR AND LEFT", all three "ON ALL SIDES") with its tone, in the round-09
    look: a 440×30 dark band with an amber frame (pulsing with the edge warnings) and a light top
    line, the 10×20 font in amber, its middle 376 px above the bottom edge (above the boss banner's
    band). It opens over 0.15 s, shows 2.5 s and fades out over its last 0.3 s. One banner at a
    time: a warning that starts while one shows waits and gets its banner after it if its edge is
    still warned, else it is dropped. Drawn in code until production art.
  - **Threat arrows** (`vanguard.game.render.ThreatArrows`, user decision D2 = A): with the fitted
    sensor suite at L2+ (the easy difficulty's +1 is the intel's only), an amber arrowhead with a
    dark outline (10–18 px, 59–100 % opaque as the enemy closes in from 240 px out) sits 14 px
    inside the edge at the enemy's place along it, pointing out at it; for every air enemy off the
    screen that is coming in (not moving away; ground units scroll in, in sight). Enemies within
    28 px along an edge share one arrow, at most eight show, and arrows also show under an edge
    warning (the warning names the edge, the arrow where along it). The simulation knows a unit only
    once it spawns 40 px outside its edge, so a wave's units show their arrows for about 0.3 s
    before they enter; a Coilwyrm's body coming up from the rear shows them for 1–2 s. Its dive out
    on the loop is not tracked (it moves away). No homing enemy missiles exist in Act 1, so the
    concept's ringed missile arrow is not built. The sensor suite is no longer listed under "NOT
    YET AVAILABLE".
  - **Credit numbers option**: the Gameplay tab's `CREDIT NUMBERS` (OFF / ON, default on,
    `gameplay.credit-numbers`) hides the floating numbers; they are still followed while hidden.
  - **Low-armour flash** (`vanguard.game.level.LowArmour`, `ShipPanel`): at or below 30 % of the
    max armour the armour number turns red and a red wash with a glow lies over the gauge, lit
    0.6 s and dark 0.6 s; at or below 15 % 0.3 s each, one flash per low-armour beep (the beeps'
    1.2 s and 0.6 s periods and thresholds); on simulation steps, so it stops in the pause.
  - **Boss bar plate**: `vanguard.game.render.BossBar` draws the bar on `hud/boss-bar-plate`
    (`assets/sprites/hud/boss-bar-plate.9.png`, a nine-patch stretched to the bar's 400 px or a
    mid-boss's 240 px, the fill in the nine-patch's content box, at the plate's own height) once it
    exists, and the plain bar until then.
- 2026-10-05: M4 part H (round 26 batch): the boss bar's production plate `hud/boss-bar-plate`
  (`assets/sprites/hud/boss-bar-plate.9.png`, `tools/art/boss_bar.py`): 64×20 nine-patch, splits
  left/right 14 and top/bottom 7, padding left/right 11 and top/bottom 7, so the content box is
  the trough's 6 px glass the fill goes in; it stretches to 422 px round the act-boss bar and 262 px
  round the mid-boss bar and is drawn at its own 20 px height. Review sheet proposed for round 26.
- 2026-10-05: M4 part H: Okafor's low-armour line ([armour](../../player/armor/README.md), once per
  attempt at 15 %) queues as an **urgent** line (`RadioQueue.Priority.URGENT`, like the Airstrike's
  call): it is a warning that matters only now, so it neither waits for a gap nor goes stale; the
  interrupted line replays after it, and the timed lines it pushes back may start late (urgent
  lines are outside the one-second rule). `RadioTimelineTest` plays it every 10 s of each Act 1
  level: it opens at once and every timed line still plays; it lists the lines pushed back, by up
  to 18 s when it cuts a long line near its end in a dense run of timed lines (Level 03 from
  t=36.5), mostly 2–13 s, once per attempt and only when the ship is nearly lost.
- 2026-10-05: Concept round 26 closed (user: the round accepted as proposed): the boss bar's plate approved as **final**; the wave banners, the sensor suite's threat arrows, the low-armour flash and the credit-numbers option accepted as built, the banners' **code-drawn look as final** (no rendered banner art), with the arrows' short lead (about 0.3 s before a wave enters) as it is. The objective tracker item is ticked for Act 1 (Levels 06 and 07 use the generic tracker; the later levels' trackers belong to those levels), so every item is ticked or tagged later and the document is `done`.
- 2026-10-06: M5 part A: the escort box is specified after the chosen mock (`hud-r08-a.png`:
  `ESCORT ROOK` over his armour bar): under the special row, his icon and name, the bar with the
  number, the low-armour flash at 30 %, `EJECTED` after he ejects, and nothing drawn in a level
  without an escort (main-agent choices; the mock shows no states). His barks use the radio queue
  as event lines (see [wingmen](../../player/wingmen/README.md#radio-barks)). The document is
  `in-progress` again while M5 builds it.
- 2026-10-06: M5 part A, the escort box as built: the `ESCORT` plate under the special's row,
  a 38 px well with his 16 px icon, `ROOK` and his armour number (or `EJECTED` in red) on its top
  row and his armour bar (8 px, the armour colours) under them; the number and the bar flash red at
  30 % in the player's rhythm. The "not yet available" list moved under the escort's region, so it
  stays put whether an escort flies or not. Capture:
  [rook-ingame-capture-r28-a.png](../../player/wingmen/concept/rook-ingame-capture-r28-a.png).
- 2026-10-06: The radio row names Rook's eject bark as an urgent line (M5 part A, after the round-28 capture): it interrupts like the Airstrike's call, is exempt from the barks' 8 s spacing and never goes stale ([wingmen](../../player/wingmen/README.md#radio-barks)).
- 2026-10-06: Concept round 28 closed (user: accepted): the escort box approved as built after the
  chosen mock (`hud-r08-a`), with the main-agent choices the mock did not show (`EJECTED` in red, the
  number and bar flashing red at 30 % in the player's rhythm, nothing drawn without an escort, the
  "not yet available" list under the escort's region); its code-drawn look is **final** like the
  rest of the panel's, so `art` stays `final`. The radio row's urgent eject bark is accepted. Every
  item is ticked, so the document is `done` again.
- 2026-10-07: M5 part C (stated defaults with the user's decisions D1–D11): Level 09's six nodes
  are six ground-target groups `Node A1` … `Node C2`, so its tracker reads `NODES A1 A2 B1 B2 C1
  C2`, a mark per group as Level 05's letters but two characters wide (about 190 of the well's
  208 px), over its secondary `RAVAGERS n / 8`. No HUD element shows a hold zone (Okafor's line at
  the first one says it). The document is `in-progress` again for the wider marks.
- 2026-10-07: M5 part C, game side: the targets tracker's marks are as wide as the widest one plus
  4 px (Level 09's pairs: 24 px, right-aligned so the last ends at the well's text edge, 6 px after
  `NODES`); a one-letter mark keeps the 16 px step and place (Level 05 unchanged). Line two is the
  escapes line scoped to the bridge waves (`RAVAGERS n / 8`, 6 / 10 by difficulty).
- 2026-10-08: M5 part D (user decision D3 = a): Level 10's escort shows in the two-line tracker,
  `SHUTTLES n / 4` (the four saveable shuttles) over five small armour bars in order, instead of
  the five portrait frames with armour bars its draft asked for (no room in the left panel next to
  the radio and the prompts) or Level 04's pips (three states only for 120 armour). Level 10 has no
  secondary, so the bars take line two. The document stays `in-progress` for the bar drawer.
- 2026-10-08: M5 part D, game side: Level 10's tracker built as decided (D3 = a). Our readings, for
  review in round 32: the bars are 32×8 px, 41 px apart, each filled to its shuttle's armour share
  (a full bar while it lifts off and while Lifeline Three is untouchable); a lost shuttle's bar
  flashes red for 1.5 s and then stays an empty dark trough, Lifeline Three's goes dark at once;
  when the mission fails, line one (the label and the count) flashes red as Level 04's does.
- 2026-10-08: Fixes from the round 32 capture of Level 10: the rear-warning banner (its middle 376 px
  above the bottom edge, 150–180 px below the top) covered Lifeline One's station, so with an air
  escort it sits at 428 px (97–127 px below the top edge), above the shuttle band, with a layout
  test against Level 10's stations, sway and sprite; and a shuttle home after the climb-out no longer
  reads like a lost one: its bar is full in pale mint (`C8FFE0`) under a green glow, whatever its
  armour, where a flying shuttle's is green, white or amber by its armour and a lost one's dark.
- 2026-10-08: [Concept round 32](../../concept-rounds/round-32/README.md) closed (user): Level 10's
  tracker bars, the rear-warning banner above the shuttle band and a home shuttle's pale-mint bar
  accepted as built, and a near-dead shuttle's empty-looking bar in the climb-out kept as it is
  (build choices 21f, g, m and o).
- 2026-10-08: M5 part E (stated defaults with the user's decisions of 2026-10-08): Level 11's
  secondary objective "Convoy afloat" (E8 = a) shows the one-line tracker `CONVOY` with three ship
  pips, decided at the Kraken's death (`DONE` or `FAILED`), the debrief's row `HULLS AFLOAT n / 3`;
  a Torpedo Pod in a level without water (E1 = a: a level flag) reads `NO WATER` in its weapon row
  and still draws its power. Our reading, for review in round 33: the pips turn amber after a ship's
  first slam whatever its hits left (on easy a ship survives two).
- 2026-10-08: Level 11's tracker as built (M5 part E, step E3c; our reading, for review in round
  33): the line flashes green when the afloat secondary is met (`DONE` replaces the pips) and red
  the moment a ship sinks (the label red, the sunk pip flashing then dark); the pips stay until the
  Kraken is down, then `FAILED` replaces them. `NO WATER` is drawn in the label's dim colour.
