---
title: Campaign
design: approved
implementation: in-progress
art: none
updated: 2026-10-07
---

# Campaign

## Summary

The campaign is 50 levels in 7 linear acts, each act tied to a setting and ending in a boss.
It follows the war from the first Vrell strike on Earth orbit to the heart of the Choir beyond
the Tether Gate. The Jovian Ascendancy betrayal is hinted at in Acts 3–4 and revealed at the
turn of Act 4 into Act 5. Levels are played one after another, with a hangar visit between
consecutive levels.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [act-1-first-contact](act-1-first-contact/README.md) | Levels 01–07 · Earth orbit & Luna · the Vrell strike · boss Brood Carrier | approved | done | final |
| [act-2-homefront](act-2-homefront/README.md) | Levels 08–14 · Earth surface: megacities, oceans, arctic · boss Siege Spire | approved | in-progress | chosen |
| [act-3-red-dust](act-3-red-dust/README.md) | Levels 15–21 · Mars · first hints of human involvement · boss Dust Colossus | draft | not-started | none |
| [act-4-deep-water](act-4-deep-water/README.md) | Levels 22–28 · Europa ice & under-ice ocean · underwater play · boss Abyssal Maw | draft | not-started | none |
| [act-5-the-belt](act-5-the-belt/README.md) | Levels 29–35 · asteroid belt & stations · the betrayal · boss Iron Sovereign | draft | not-started | none |
| [act-6-jovian-storm](act-6-jovian-storm/README.md) | Levels 36–42 · Jupiter cloud cities & Callisto · hunting Vorne · boss Ascendant | draft | not-started | none |
| [act-7-beyond-the-gate](act-7-beyond-the-gate/README.md) | Levels 43–50 · Tether Gate & Vrell space · final boss Choir Heart | draft | not-started | none |

## Design

### Campaign arc

| Act | Levels | Setting | Story beat | Mechanic introduced | Boss |
|---|---|---|---|---|---|
| 1 | 01–07 | [Earth orbit](../world/earth-orbit/README.md), [Luna](../world/luna/README.md) | The Vrell come through the gate and strike Earth's shipyards. Aegis Wing holds the line. | Basics, ground layer, escort objective, enemies entering from the sides | Brood Carrier |
| 2 | 08–14 | [Earth](../world/earth/README.md) | The Vrell land. The fighting moves to the cities, the oceans and the poles. | Rear attacks, naval layer, weather, defend-the-station | Siege Spire |
| 3 | 15–21 | [Mars](../world/mars/README.md) | Mars colonies under siege. Unmarked human-built drones turn up among the Vrell. | Burrowing enemies, canyon walls, reverse scroll | Dust Colossus |
| 4 | 22–28 | [Europa](../world/europa/README.md) | The Vrell are drilling into Europa's ocean. Helix Dynamics subs are found under the ice. | Underwater play (`sub` layer), darkness | Abyssal Maw |
| 5 | 29–35 | [Belt](../world/belt/README.md) | **The reveal:** Ascendancy fleets fight openly beside the Vrell. The Belt stations fall. | Ascendancy war machines, energy drain, minefields | Iron Sovereign |
| 6 | 36–42 | [Jovian](../world/jovian/README.md) | The UTC strikes back at the Ascendancy heartland. Vorne flees into the gate. | Hybrid biotech enemies, regeneration, mirror shields | Ascendant |
| 7 | 43–50 | [Vrell space](../world/vrell-space/README.md) | Through the Tether Gate. Vorne's end, the Choir, and a hint of the Silence. | Distorted space, elite enemies, boss rush | Choir Heart |

### Pacing rules

- **Every level introduces something**: a new enemy, hazard, objective, layer or mechanic. It
  appears first in a safe situation and then in combination with things already known.
- **Every act adds a setting twist** and at least one mechanic that changes which loadout is best
  (see *Loadout pressure* below).
- **Boss at the end of every act.** Acts 1, 2, 3, 6 and 7 also have a **mid-boss** in one of the
  middle levels (see [bosses](../enemies/bosses/README.md)).
- **Breather levels** have lower density and plenty of credit pickups and secrets, and come
  right before a spike: L22 (act opener), L34 *Salvage Run*, L46 *Graveyard of the Choir*.
- **Mostly from the front.** In any level at least ~60% of waves enter from the top edge, even
  where the intel lists `all` directions. Levels with a `rear` or `sides` focus say so in their
  threat profile, so the hangar can warn the player.
- **No long pauses** (user decision): from Level 04 on, the screen is never empty of enemies
  (air or ground) for longer than about 3 s, outside the launch and scripted moments, and a level
  has at most 2 longer pauses. Gaps are filled with small, easy waves of enemies the act already
  knows, so the difficulty does not rise. Levels 01–03 are exempt as the warm-up. `PacingTest`
  measures it on the autopilot's runs.
- **Level length**: 3–5 minutes of scroll plus the boss fight. Act 1 levels sit near 3 minutes;
  Act 7 levels near 5.
- **Objective types**: `reach-end` (default), `escort`, `defend` (scroll halts and waves come
  from all sides), `destroy-targets` (a set of named ground targets), `survive` (timer),
  `boss`. A level has one primary objective. Optional secondary objectives give bonus credits.
  A secondary objective is a stretch goal the typical player meets in about every other run; for
  a kill-ratio secondary the rule of thumb is **65 % of the level's enemies** (was 80 %, too high
  for dense levels where a typical player kills 60 % of the air enemies). A kill-ratio secondary
  is **judged at the level's end**: its credits and its line come on completion, with the
  level-end lines (its line before the level-end line); the HUD counts the kills against the
  share during the level but shows it met only then. Group, escape and kill-all secondaries are
  decided when their last unit is gone, which can be mid-level.
- **Objective failure**: failing the **primary** objective is mission failed — the level is
  retried (see [retry](../systems/retry/README.md)). Each level doc sets the failure condition;
  defaults: `defend` fails when the defended station is destroyed, `escort` when every escorted
  unit is lost (each unit lost before that only lowers the reward), `destroy-targets` when a
  named target survives to the end of the scroll. Failing a **secondary** objective only loses
  its bonus.
- **Lost-mission prompt** (user decision): as soon as a primary objective becomes impossible
  (a missed hive node, a lost escort, a destroyed relay), the game shows "Mission lost — retry
  now?" immediately, with an option to fly on to the end; either way the attempt's credits are
  discarded (see [retry](../systems/retry/README.md)).

### Difficulty curve

Density is rated 1–5 in the level rosters, where 1 is tutorial-quiet and 5 means the screen is
full. The curve is a rising sawtooth: an act opener sits at or below the previous act's boss
level, density climbs through the act, and breathers dip in just before a spike.

| Act | Density per level | Range |
|---|---|---|
| 1 | 1 · 2 · 2 · 2 · 3 · 3 · 3 | 1–3 |
| 2 | 3 · 3 · 3 · 3 · 4 · 4 · 4 | 3–4 |
| 3 | 3 · 4 · 4 · 3 · 4 · 4 · 4 | 3–4 |
| 4 | 3 · 3 · 4 · 4 · 3 · 4 · 4 | 3–4 |
| 5 | 4 · 3 · 4 · 4 · 4 · 2 · 5 | 2–5 |
| 6 | 4 · 4 · 5 · 4 · 5 · 5 · 5 | 4–5 |
| 7 | 4 · 4 · 5 · 3 · 5 · 5 · 5 · 5 | 3–5 |

**Minimum density** (user decision): levels are dense, more to shoot in the same duration at the
same momentary threat: many small, easy units rather than more dangerous ones. Every level
reaches its act's minimum in **enemies per minute of scroll** (air and ground units, released
spawns included, at medium; the launch and the boss fight excluded: a level with an arena counts
up to the arena). The warm-up Levels 01–03 need at least 32 per minute (user decision); a breather
may dip 20 % under its act's minimum. The content test `DensityTest` checks every level.

| Act | 1 | 2 | 3 | 4 | 5 | 6 | 7 |
|---|---|---|---|---|---|---|---|
| Minimum enemies per minute | 40 | 50 | 55 | 60 | 70 | 75 | 80 |

After the density rework: L01 32.6, L02 33.3, L03 34.3, L04 48.0, L05 66.6 per minute (before it: 32.6, 29.3, 23.7, 40.5 and 53.4 by this count; the earlier note's L05 36 spread its enemies over the boss fight too).

Density is only one axis. Later levels also get harder through enemy mix, hazards and
mechanics, so a density-4 level in Act 6 is much harder than one in Act 2.

Global difficulty (easy/medium/hard) scales enemy HP, fire rate, bullet speed and wave
composition. The scaling hooks per enemy are defined in [enemies](../enemies/README.md) and the
global levers in [systems](../systems/README.md).

### Loadout pressure and shop unlock pacing

The hangar gets new equipment as the campaign goes on. Every weapon trait from the shared
vocabulary (see [design/README.md](../README.md)) is **available** before the first level that
**needs** it. That gives the player at least one hangar visit of warning, with the intel panel
highlighting it.

| Trait | Available from | First level that strongly rewards it | Why |
|---|---|---|---|
| `forward` | start | L01 | Default |
| `spread` | L02 | L03 *Spore Drift* | Drifting spore mines and wide swarms |
| `anti-ground` | L03 | L04 *Tranquility Run* | First surface level with turret nests |
| `piercing` | L05 | L07 *Brood Carrier* | Armoured boss segments, lined-up swarms |
| `side` | L05 | L06 *Farside* | Mantis enemies hold at the sides |
| `area` | L07 | L09 *Arcology Fall* | Hardened hive nodes in clusters |
| `rear` | L08 | L10 *Evacuation Corridor* | First rear-heavy level (Wraith ambushes) |
| `homing` | L06 | L12 *Storm Front* | Low visibility, attacks from all sides |
| `anti-sub` | L11 (optional) | L23 *Through the Ice* | Required in Act 4; available early for Atlantic sub targets |
| `beam` | L15 | L16 *Valles Canyon Run* | Burns through Tendril barriers |
| `shield-breaker` | L29 | L37 *Gilded Cage* | Mirror interceptors, Gate Wardens (L43) |

This table owns the pacing: [weapons](../player/weapons/README.md) and
[specials](../player/specials/README.md) must offer at least one item with each trait from the
level in the *Available from* column (their unlock levels are set to match). The same table
drives the hangar's "new in shop" markers and the intel panel's "recommended traits".

Other unlock anchors (items and prices in [player](../player/README.md)): first special,
the Airstrike, from L04; escort slot and Rook as wingman from L08; the Warden heavy drone from
L22 (it covers Rook's absence in L27–L29); the Hunter drone from L29.

### Credit budget

Each level has a **credit budget**: the credits a **typical** medium player earns in it (user
decision 2026-10-04): 60 % of the air enemies, most ground targets, half the secrets, the
objectives as authored; the shares are in
[economy](../systems/economy/README.md#per-level-budget), with the curve (level 01 = 700). A
perfect run earns about 1.5× the budget or more. A level document's *Credit budget* table splits
the perfect run and the typical haul into kills, ground targets, pickups, secrets and objectives;
the typical haul must land within ±5 % of the budget, which a level reaches with its density and
its `bounty_scale` (a factor on every bounty in the level, see the economy).

### Level document template

A level starts as a row in its act's Roster table (`idea`). When it moves to `draft` it gets a
directory `level-NN-slug/` with a README holding these sections (after Summary):

1. **Briefing** – what Commander Okafor / Dr. Varga tell the player: situation, objective,
   intel. 80–150 words in character. Also the one-line *hangar teaser* for the previous
   level's shop screen.
2. **Threat profile** – the table the hangar intel panel is built from: dominant layers, attack
   directions (with % of waves per direction), density (1–5), recommended weapon traits,
   hazards, boss/mid-boss, and the intel detail unlocked by a better sensor suite.
3. **Objective** – primary objective type and win/fail conditions; optional secondary
   objectives with bonus credits.
4. **Layout** – the level split into named sections along a **scroll timeline** (time in
   seconds and scroll distance in px at the 960×540 baseline, see [art direction](../art-direction/README.md)). Per section: terrain and
   parallax content per layer, scroll speed, **atmosphere intensity** (`clear` / `light` /
   `medium` / `heavy`, see [art direction](../art-direction/README.md)), and what the player
   should learn or feel there.
5. **Waves** – table `| t (s) | Section | Formation | Enemies (link) | Count | Enter from | Notes |`.
   Enemies and formations are referenced by link and vocabulary name, never re-specified.
6. **Ground targets** – static and ground-layer targets (turrets, buildings, hive nodes), with
   position per section and what destroying them does (credits, opens a path, disables a
   hazard).
7. **Hazards** – terrain collision, weather, mines, darkness, etc.
8. **Secrets and pickups** – credit caches, repair pickups, hidden routes, bonus targets, and
   what reveals them.
9. **Radio chatter** – cue list `| Trigger | Speaker | Line |` (portrait in the side HUD).
10. **Boss / mid-boss** – link to the boss document, arena notes specific to this level.
11. **Music & ambience** – link to the track in [audio](../audio/README.md), plus stings.
12. **Credit budget** – the perfect run and the typical haul at medium (generated), split into
    kills / ground targets / pickups / secrets / objectives, against the budget.
13. **Difficulty notes** – what changes on easy and hard beyond the global scaling.

It then continues with the standard sections: Concept art, Implementation (acceptance
criteria), Open questions, Decisions. The worked example is
[level 01](act-1-first-contact/level-01-break-at-dawn/README.md).

## Implementation

- [x] Campaign sequence 01→07 plays in order; the hangar opens between levels (Act 1, M4)
- [ ] Level 08 after Act 1: the Level 07 debrief with the act summary, the Act 1 outro, Act 2's
      title card and act briefing, Level 08's briefing, the hangar before it, Level 08 (M5 part B)
- [ ] Campaign sequence 09→50 in order — **later: M5** (Levels 09–14, parts C–H) and **later:
      Act 3** to Act 7 (Levels 15–50), each level with its milestone or act
- [x] Act transitions show an act title card and the act's opening briefing.
- [x] Each level's threat profile is available as data for the hangar intel panel: Levels 01–07
      (`threat_profile` in each level's data.yaml, read by `LevelData` and `Intel`)
- [ ] Level 08's threat profile in its data file, with Dr. Varga's four lines (M5 part B)
- [ ] The threat profiles of Levels 09–50 — **later: M5** (Levels 09–14) and **later: Act 3** to
      Act 7, in each level's data file as it is written
- [x] Shop unlocks follow the *Loadout pressure* table (data-driven, per level): each item's
      `unlock` or `available` in its data (`Hangar.available`, `Catalogue`); the Act 1 rows
      (`forward` to `area`) checked against the weapons' data
- [x] The `rear` row checked with its weapon in play: the Tail Gun (`rear` slot and trait,
      `unlock: 8`), built in M5 part A
- [ ] The later rows checked with their weapons in play: `anti-sub` (L11) — **later: M5 part
      E**; `beam` (L15) and `shield-breaker` (L29) with their weapons — **later: Act 3** and
      **later: Act 5**
- [x] Campaign progress (current level, unlocks) is stored in the save game
      ([systems](../systems/README.md)).

## Open questions

- None open.

## Decisions

- 2026-09-30: 7 linear acts by setting, 50 levels globally numbered, a boss per act; the twist
  is revealed at levels 28–29.
- 2026-09-30: Credit budgets per level follow the economy curve (single owner:
  [economy](../systems/economy/README.md)); `homing` becomes available at L06 (the
  Micro-missile Pod) instead of L10.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-09-30: Level template: each section declares an atmosphere intensity so fog, cloud and dust vary through a level (user feedback, concept round 03).
- 2026-10-01: Objective failure: a failed primary objective (escort, defend, destroy-targets) means mission failed and a retry; a failed secondary objective only loses its bonus.
- 2026-10-01: No level select in the first build; replaying completed levels may come later.
- 2026-10-01: Contents art cells for Acts 1 and 2 synced to `chosen` (check_docs --fix).
- 2026-10-01: Lost-mission prompt: when a primary objective becomes impossible, offer an immediate retry instead of waiting for the end of the scroll (user decision, raised in L09).
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../reviews/acts-1-2/README.md).
- 2026-10-02: M3 part B1: the campaign state and its saves hold the next level and the unlocks
  ([saves](../systems/saves/README.md)); the briefing before a level that opens its act starts with
  the act's title card and act briefing, from the act's data file. Only Level 01 is built, so after
  it the campaign waits in the hangar.
- 2026-10-03: Pacing rule (user decision): the pauses between enemies were too long. From Level 04 on,
  the screen is never empty of enemies for more than about 3 s outside the launch and scripted
  moments, with at most 2 longer pauses per level, without raising the difficulty. Levels 01–03
  stay as they are, as the warm-up.
- 2026-10-04: Economy rework (user decision): the credit budget is the typical player's haul
  (level 01 = 700), levels become denser with a minimum density per act (enemies per minute:
  40 · 50 · 55 · 60 · 70 · 75 · 80, proposed values) and a per-level `bounty_scale`; the
  kill-ratio secondary's rule of thumb drops from 80 % to 65 % (proposed). Level 01's 80 %
  secondary changes with its density rework, the next step.
- 2026-10-04: User decisions on the economy rework: the minimum density per act stays 40 · 50 · 55
  · 60 · 70 · 75 · 80 enemies per minute (a breather may dip 20 %), with at least 32 for the
  warm-up Levels 01–03; the kill-ratio secondary's rule of thumb is 65 % (Level 01's 80 % becomes
  65 %). The density rework of Levels 01–05 follows it: Levels 04–05 replace their small Skitter
  filler streams by longer, overlapping Skitter streams and snakes, Levels 02–03 get a few more
  Skitter waves, and every level a `bounty_scale` that puts its typical haul on budget. Where the
  autopilot took more damage, threats were thinned (on hard, or Needlers) rather than the volume.
- 2026-10-04: A kill-ratio secondary is judged at the level's end (fix after the user's report):
  it was met, paid and announced the moment the kills reached the share, which with the 65 % rule
  and the denser waves put Level 01's "Clean sweep" line about 45 s before the end (at 136 s of
  180 on medium). It now comes on completion, before the level-end line. The other event lines of
  Levels 01–05 were checked on the autopilot's runs and fire at the right moments.
- 2026-10-05: M4 part H docs reconciliation: the sequence, threat-profile and shop-unlock items
  are split into the built Act 1 part (ticked) and the later levels (tagged with their milestone
  or act), so the document is `done` for M4 under the roadmap's rule for deferred items.
- 2026-10-06: M5 plan: the Act 2 trait rows are split by part, `rear` with part A (the Act 2
  arsenal) and `anti-sub` with part E (the Torpedo Pod and the `sub` layer, user decision D1 of
  part A; see the [roadmap](../tech/roadmap/README.md#m5-parts)).
- 2026-10-06: M5 part B: the `rear` row is ticked (the Tail Gun, `unlock: 8`, flies since part A);
  the sequence and threat-profile items are split, Level 08's parts (the Act 1 → Act 2 transition
  and Level 08's threat profile with four Varga lines) for part B and Levels 09–14 later in M5.
  The document goes back to `in-progress` for part B. Accepted with the user's decisions (a gap
  default): a save written at the hangar before Level 08 by the M4 build opens straight in that
  hangar and never shows the Act 2 intro; a new campaign or `--level 7` shows it.
