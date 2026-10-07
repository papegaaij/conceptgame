---
title: Creeper
design: approved
implementation: done
art: final
depends-on: [../../../systems/difficulty, ../../../systems/economy, ../scuttler]
updated: 2026-10-07
---

# Creeper

## Summary

A slate six-legged salamander with a violet fan gland that crawls in convoys along streets, ramps and low roofs, firing five-way fans at the player, the convoy's fans rippling 0.5 s apart. The Act 2 ground walker, built on the [Scuttler](../scuttler/README.md)'s walk.

## Design

### Stat block

The numbers live in [data.yaml](data.yaml). Values are first-draft balancing numbers at **medium** (see the [balancing basis](../../README.md#balancing-basis)). HP is in damage units (a Pulse Cannon L1 shot does 2); bullet damage classes and speeds are defined in the balancing basis. Its HP is set at its first level (L08, reference DPS 60: time-to-kill 0.5 s); where it returns (L09, L14) it gets the act HP factor of a `medium` unit.

<!-- data: stat-block -->
| Field | Value |
|---|---|
| Faction | Vrell |
| Layer | `ground` |
| Size tier | `medium` |
| Size | 60×60 px, hitbox 40×44 |
| Parts | single |
| Orientation | `16 angles` (16 headings × 6 walk frames; it faces where it walks) |
| HP | 30 (easy 22 / hard 39, from the global multipliers) |
| Armour / shield | none |
| Speed | 35 px/s (plus scroll) |
| Movement | `walk` along an authored ground path (streets, ramps, low roofs); turns at 90°/s; the walk phase advances with distance (22 px per cycle) |
| Attack | 5-way `fan` aimed at the player every 3 s (spread 50°, 140 px/s, `small` = 4); a convoy fires its fans 0.5 s apart, front to back |
| Formations | convoy (3–5) (one path, the units 1.5 s apart) |
| Weak points | violet fan gland (drawn only) |
| Effective traits | `anti-ground`, `area` |
| Credits | 22 (score 220 × chain) |
| Death | `medium` organic burst: legs scatter, the gland bursts violet; leaves a legless husk at its last heading |
| First level / used in | L08; returns L09 and L14 (with the act HP factor) |
| Difficulty hooks | hard: fan 7-way, every 2.6 s |
<!-- /data -->

### Behaviour

- **Walk.** As the Scuttler's: a wave gives the convoy one path of `[x, y]` points in screen
  coordinates at the wave's time, which then scrolls with the ground; the Creeper walks it at
  35 px/s on top of the scroll, turning at most 90°/s, the nearest of its 16 headings drawn, the
  walk phase advancing one 6-frame cycle per 22 px of ground. A convoy's units walk the same path
  1.5 s apart (the walker formations' spacing). The facing goes into the state hash.
- **Where it walks** (art direction, [perspective towers](../../../art-direction/README.md#parallax-layer-model),
  user decision D1 of M5 part B): on the ground plane only, along streets and avenues, up and down
  ramps and across low structures drawn without lean (parking decks, landing pads, low roofs). It
  never climbs a wall or stands on a tower's roof: the towers are perspective scenery.
- **Fan.** Five bullets aimed at the player (`aim: target`, unlike the Scuttler's facing fan),
  first at half its interval after it walks onto the screen, counting only on the screen. It has
  no frontal armour and no spit.
- **Convoy stagger.** A convoy's fans ripple front to back, 0.5 s apart, so the volley reads as a
  wave, not a wall: the fan's `stagger: 0.5` in [data.yaml](data.yaml). The units of one wave
  share a volley clock, which starts as the first of them comes onto the screen: the first volley
  comes half an interval later, then one every interval, and unit *i* (from 0, in the order they
  enter) fires *i* × 0.5 s after the volley's start; a unit that is not on the screen yet (or any
  more) skips its turn. Five units take 2.0 s of the 3.0 s interval (2.6 s on hard), so the ripple
  never overlaps the next volley (the simulation rejects a wave whose ripple would).
- **Hard.** A 7-way fan every 2.6 s: an authored interval (the hook's `attacks.fan.interval`), so
  the difficulty's fire-rate lever does not apply on top of it, as for the Mantis's beam.
- **Death.** A 16-frame `medium` organic burst, then a legless husk at its last heading (16 husk
  frames) that scrolls away with the ground, as the Scuttler's. As a large Vrell unit it
  screeches once as it first comes onto the screen ([sfx](../../../audio/sfx/README.md#enemies)).

### Concept art

Chosen concept: [creeper-r06-a.png](../concept/creeper-r06-a.png), [creeper-r06-a.gif](../concept/creeper-r06-a.gif) (listed in the [ground](../README.md#concept-art) Concept art table). The production sprites (60×60, 16 headings × 6 walk frames, the 16 husks, the death burst and a 30×30 intel portrait) go straight to production from it and were approved as final in concept round 30 (user, 2026-10-07).

The production files, generator notes in [concept/prompts.md](concept/prompts.md):

| File | What | Status |
|---|---|---|
| [concept/creeper-final-r30-a.png](concept/creeper-final-r30-a.png) | Final sprites (`tools/art/creeper.py`): 16 headings × 6 walk frames (`creeper_0..95`, 60×60, 22 px per cycle), the walk cycle at three headings, the legless husks (`creeper-husk_0..15`), the additive glow masks of the gland, the back pores and the eyes (`creeper-glow_0..95`), the 16-frame death (`creeper-tatters_0..15`, solid, and `creeper-death_0..15`, additive, 96×96) alone and with the `medium` burst over the husk | chosen |
| [concept/creeper-final-r30-a.gif](concept/creeper-final-r30-a.gif) | A convoy of three walking a street that turns, 1.5 s apart, their glow masks added; the lead dies and leaves its husk | chosen |

## Implementation

- [x] Stat block values loaded from [data.yaml](data.yaml); global difficulty multipliers applied
- [x] Walker path (the Scuttler's walk) with turn rate and distance-driven walk phase; facing in the state hash
- [x] Fan aimed at the player from a walker
- [x] Staggered convoy fans (0.5 s, the `stagger` key in the data and the [schemas](../../../tech/architecture/README.md#data-file-schemas))
- [x] Hard: 7-way fan with the authored 2.6 s interval
- [x] Death effect (16-frame organic burst), the husk at its last heading, bounty and score per this spec
- [x] The Vrell screech as it comes onto the screen
- [x] Production sprites, husks, death frames and intel portrait (art track, concept round 30)

## Decisions

- 2026-10-01: Promoted from the ground roster to a full spec for the Acts 1–2 wrap-up.
- 2026-10-01: HP rescaled to the lowered L08 reference DPS (36 → 30, ×60/70); time-to-kill stays ≈ 0.5 s.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../../reviews/acts-1-2/README.md).
- 2026-10-06: M5 part B, user decision **D1 = a** (perspective towers are scenery): the Creeper
  walks on the ground plane only, along streets, ramps and low roofs drawn without lean; "crawls
  along roads, rooftops and walls" is struck (no wall crawling, no tower roofs).
- 2026-10-06: M5 part B gap defaults (stated with the user's decisions D1–D6): the numbers moved
  into [data.yaml](data.yaml), the table is rendered from it; the weak point is **drawn only**
  (single-part rule, as the Scuttler's), so its ×1.5 is struck; the fan is **aimed at the player**
  (`aim: target`); the convoy stagger is **0.5 s**; a 16-heading **husk** as the Scuttler's; the
  large-unit **screech** on entry. Our readings, for review: the walk's turn rate 90°/s and stride
  22 px (the Scuttler's, scaled to the 60 px sprite); the stagger as a shared volley clock per wave
  (unit *i* fires *i* × 0.5 s after the first); the hard 2.6 s as an authored interval the
  fire-rate lever does not shorten (the lever alone would give 3.0 ÷ 1.3 ≈ 2.3 s); the formations
  row keeps `convoy` (3–5) only. The `stagger` key is a proposal in this README until the walker
  code reads it.
- 2026-10-06: M5 part B's walker code built: the fan's `aim: target` (the default) aims a walker's
  fan at the player (the Scuttler keeps `facing`), `stagger: 0.5` moved from this README's proposal
  into [data.yaml](data.yaml), the hard hook's `attacks.fan.interval` is used as authored for an
  ordinary attack (2.6 s on hard, no fire-rate lever on top), the husk is the walkers' and the
  screech the large Vrell units'. Our reading of the shared clock, for review: it starts as the
  wave's first unit comes onto the screen and then runs on (it does not pause while every unit is
  off the screen); a boss checkpoint's empty field starts it afresh.
- 2026-10-06: M5 part B, B5 (user decision **D6 = a**, straight to production): the production
  sprites rendered by `tools/art/creeper.py` from the chosen round-06 model, its geometry copied so
  the legs' swing follows the data's 22 px stride (±0.21 model units instead of the concept's
  ±0.18, so the planted feet stay put); the husk keeps the hip stubs and a dim gland; the glow masks
  carry the five pores and the eyes, so they show through Level 08's low-air fog as the Scuttler's
  back does through dust; the death is the Scuttler's 16-frame organic burst in violet with the
  Creeper's own pieces (legs with toe pads, four frill shards, chitin chips). The intel portrait
  (`tools/art/intel.py`, 30×30) walks down at heading 0. Review files proposed for round 30; no
  muzzle flash (the game draws none for enemy fans).
- 2026-10-07: Readability on Level 08's night city (the round-30 capture found the Creeper dark
  grey-brown on the dark navy streets, hardest in the smoke district and on the highway; readability
  rules 3 and 4): the production sprites re-rendered with the slate chitin lifted (albedo ×1.4), the
  legs and frill between dark chitin and slate, three violet glow pores along the back (in the glow
  masks too, so they show through the smoke) and a 1 px lavender-white light rim that follows the
  key light (the husk at 80 % tone with a fainter rim; the death pieces in the lifted colours). The
  intel portrait renders the same model and picks up the lift the next time `tools/art/intel.py`
  runs. Measured on Level 08 composites: see the level's
  [concept/prompts.md](../../../campaign/act-2-homefront/level-08-neon-skyline/concept/prompts.md#level-08-readability-r30-b).
- 2026-10-07: [Concept round 30](../../../concept-rounds/round-30/README.md) closed (user,
  2026-10-07): the production sprites (`creeper_0..95`, the husks, the glow masks, the `medium`
  death with its tatters) and the intel portrait approved as **final**, with the readability lift
  (lifted chitin, violet back pores, the lavender-white rim) and the weak spots as they are (the
  death pieces posed at heading 0, the flat first glow frame, the weak point drawn only); the
  numbers of the stat block accepted. `art: final`; every Implementation item is ticked, so the
  implementation is `done`.
