---
title: HUD
design: approved
implementation: in-progress
art: chosen
depends-on: [../../player, ../../systems/scoring, ../../art-direction]
updated: 2026-10-02
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
| Radio | 72×72 portrait with static on open/close, name, subtitle below the portrait up to 3 lines × 22 chars per page; a longer line is paged: each page typed out, then held 3 s (the last page 5 s); a radio line is at most **two pages** of word-wrapped lines (writing rule); queued messages; urgent warnings interrupt |
| Progress | Level progress bar with a boss marker at the end |
| Control prompts | The contextual prompts of the first levels (see [Level 01](../../campaign/act-1-first-contact/level-01-break-at-dawn/README.md#launch-and-control-prompts)): one line each, the action on the left and its keys on the right (the four arrow keys read `ARROW KEYS`), at most three at once; the box is shown only while a prompt is pending |
| Objective tracker | Shown for every primary or secondary objective; hidden in levels without one. Compact box above the progress bar: objective icon and short label (e.g. "DOCKS", "CRAWLERS", "BATTERIES", "HIVE NODES", "SHUTTLES", "RELAY"), then progress as pips or counters (docks, crawler pips, batteries A–D, hive nodes, shuttles, relay integrity bar). A pip flashes green on success and red on a loss or failure; the whole box flashes when the objective is won or lost. Used by [L02](../../campaign/act-1-first-contact/level-02-shipyard-burning/README.md), [L04](../../campaign/act-1-first-contact/level-04-tranquility-run/README.md), [L05](../../campaign/act-1-first-contact/level-05-crater-nest/README.md), [L09](../../campaign/act-2-homefront/level-09-arcology-fall/README.md), [L10](../../campaign/act-2-homefront/level-10-evacuation-corridor/README.md) and [L13](../../campaign/act-2-homefront/level-13-polar-relay/README.md); the secondary objective of [L01](../../campaign/act-1-first-contact/level-01-break-at-dawn/README.md) shows a `KILLS n / 76` counter that turns `DONE` and flashes green when met |

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
| [concept/edge-warnings-r09-a.png](concept/edge-warnings-r09-a.png) | Edge warnings (side and rear) with flash cycle, sensor threat arrows, wave and boss banners, close-ups and 1× play-field panels | chosen |

## Implementation

- [ ] Side panel frames from the UI kit
- [x] Left panel: mission, score, credits, chain, radio, progress
- [x] Left panel layout: fixed regions without overlap; texts cut off at their well; a test checks the regions and that every prompt and radio line of the content fits, measured with the font's metrics
- [ ] Objective tracker: icon, label, pips/counters or integrity bar per level, success/fail flash; hidden in levels without an objective (Level 01's kill counter done)
- [ ] Right panel: armour, shield, power, weapons, overdrive, special, escort
- [ ] Radio message queue with portraits, priority interrupts
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
