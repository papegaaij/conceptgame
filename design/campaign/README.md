---
title: Campaign
design: draft
implementation: not-started
art: none
updated: 2026-09-30
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
| [act-1-first-contact](act-1-first-contact/README.md) | Levels 01–07 · Earth orbit & Luna · the Vrell strike · boss Brood Carrier | draft | not-started | none |
| [act-2-homefront](act-2-homefront/README.md) | Levels 08–14 · Earth surface: megacities, oceans, arctic · boss Siege Spire | draft | not-started | none |
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
- **Level length**: 3–5 minutes of scroll plus the boss fight. Act 1 levels sit near 3 minutes;
  Act 7 levels near 5.
- **Objective types**: `reach-end` (default), `escort`, `defend` (scroll halts and waves come
  from all sides), `destroy-targets` (a set of named ground targets), `survive` (timer),
  `boss`. A level has one primary objective. Optional secondary objectives give bonus credits.

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

Each level has a **credit budget**: the credits available at medium difficulty if every enemy
and pickup is collected. The budget per level comes from the curve in
[economy](../systems/economy/README.md#per-level-budget) (level 01 = 1,000). A level document
splits its budget into kills, ground targets, pickups and secondary objectives.

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
12. **Credit budget** – total available at medium, split into kills / ground targets / pickups
    / secondary objectives.
13. **Difficulty notes** – what changes on easy and hard beyond the global scaling.

It then continues with the standard sections: Concept art, Implementation (acceptance
criteria), Open questions, Decisions. The worked example is
[level 01](act-1-first-contact/level-01-break-at-dawn/README.md).

## Implementation

- [ ] Campaign sequence 01→50 plays in order; the hangar opens between levels.
- [ ] Act transitions show an act title card and the act's opening briefing.
- [ ] Each level's threat profile is available as data for the hangar intel panel.
- [ ] Shop unlocks follow the *Loadout pressure* table (data-driven, per level).
- [ ] Campaign progress (current level, unlocks) is stored in the save game
      ([systems](../systems/README.md)).

## Open questions

- Should failing an `escort`/`defend` objective fail the level, or only cost the bonus?
  Proposal: `defend` fails the level if the defended station is destroyed; `escort` only
  reduces the reward unless every escorted ship is lost.
- Is a level-select screen for replaying cleared levels (for credits and high scores) wanted,
  or is it strictly one level after another?

## Decisions

- 2026-09-30: 7 linear acts by setting, 50 levels globally numbered, a boss per act; the twist
  is revealed at levels 28–29.
- 2026-09-30: Credit budgets per level follow the economy curve (single owner:
  [economy](../systems/economy/README.md)); `homing` becomes available at L06 (the
  Micro-missile Pod) instead of L10.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-09-30: Level template: each section declares an atmosphere intensity so fog, cloud and dust vary through a level (user feedback, concept round 03).
