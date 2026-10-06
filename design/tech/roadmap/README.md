---
title: Implementation roadmap
design: approved
implementation: in-progress
art: n/a
depends-on: [../architecture, ../../campaign]
updated: 2026-10-05
---

# Implementation roadmap

## Summary

The order in which the approved Acts 1–2 design is built: seven milestones, each ending in a
playable build that runs on all three OSes and is reviewed by the user. The tickets are the
*Implementation* checklists of the design documents (about 525 items over 97 documents); a
milestone lists which documents it covers, it does not copy their items.

## Design

### Milestones

| # | Milestone | Playable result | Main tickets |
|---|---|---|---|
| M0 | **Skeleton** | The game starts in full screen, shows an empty title screen, toggles display mode, quits; CI green on three OSes | [architecture](../architecture/README.md) (build, carried-over foundations, CI), [options](../../ui/options/README.md) display mode and settings file |
| M1 | **First flight** | Fly the ship over one scrolling setting with parallax, fire the starting gun, shoot Skitters, take damage, die and retry | [ship](../../player/ship/README.md), the starting front weapon, [Skitter](../../enemies/air/skitter/README.md), shield/armour, [retry](../../systems/retry/README.md), a minimal [HUD](../../ui/hud/README.md), music and SFX players, [controls](../../ui/controls/README.md) |
| M2 | **Level 01** | [Level 01](../../campaign/act-1-first-contact/level-01-break-at-dawn/README.md) from launch to end: scroll timeline, waves from its data file, ground targets, radio chatter, objectives, debrief totals | data-file loader and table sync, level script runner, formations, layers, [scoring](../../systems/scoring/README.md), [debrief](../../ui/debrief/README.md), replay test for L01 |
| M3 | **The campaign loop** | Menu → new game → briefing → hangar (buy, fit, sell) → level → debrief → save; load game | [main menu](../../ui/main-menu/README.md), [briefing](../../ui/briefing/README.md), [hangar](../../ui/hangar/README.md), [economy](../../systems/economy/README.md), [saves](../../systems/saves/README.md), [difficulty](../../systems/difficulty/README.md), [pause](../../ui/pause/README.md), [options](../../ui/options/README.md) |
| M4 | **Act 1** | Levels 02–07 with their enemies, bosses, weapons, specials and allies; balance tests green | [act 1](../../campaign/act-1-first-contact/README.md) levels, the Act 1 [enemies](../../enemies/README.md) and bosses, [weapons](../../player/weapons/README.md), [specials](../../player/specials/README.md), [allies](../../allies/README.md); the M3 documents' `later: M4` items: [economy](../../systems/economy/README.md) bounties and balancing sheet, [difficulty](../../systems/difficulty/README.md) bullet tags, [retry](../../systems/retry/README.md) boss checkpoint, [debrief](../../ui/debrief/README.md) data cores and act summary, [hangar](../../ui/hangar/README.md) test fire and Varga's intel lines |
| M5 | **Act 2** | Levels 08–14 and their new units and equipment | [act 2](../../campaign/act-2-homefront/README.md) levels and their enemies and equipment, the [wingmen](../../player/wingmen/README.md) escort slot (Rook from L08), the [saves](../../systems/saves/README.md) `escort` field, the Targeting computer in the shop with its effects (economy, from part H), the perspective towers and roofs of L08 (art direction) |
| M6 | **Acts 1–2 release** | Credits screen with the CC-BY attributions, polish pass, release bundles for all OSes on a `v0.1` tag | [credits](../../ui/credits/README.md), mouse support in the out-of-game screens ([ui](../../ui/README.md)), the [scoring](../../systems/scoring/README.md) high-score table per difficulty with its name entry at game over and at the campaign's end, remaining checklist items of Acts 1–2 |

### Rules

- Work happens on a branch per milestone (`m0-skeleton`, …), merged into `main` when the user
  has reviewed the playable build; smaller steps inside a milestone are commits.
- A milestone is done when its documents' checklist items are ticked (deferred items aside, see
  below), their `implementation` status is `done`, CI is green and the user has played the build.
- Chosen concept art stands in for production art (see [architecture](../architecture/README.md#assets)).
  **Production art** (final-quality renders, every angle set and animation) is a separate track
  that **starts after M2**, once Level 01 has proven the sprite sizes, layers and atlas budget in
  the real game; it then replaces placeholders part by part.
- Design changes found during implementation go into the design tree first, in the same change.
- **Deferred items.** A checklist item that belongs to a later milestone stays in its document,
  unticked, and ends in `— **later: M4** (reason)` (or `later: art track` for work waiting on
  production art); an item that is partly done is split into a ticked part and a deferred part.
  The named milestone's row lists the item among its tickets unless its documents imply it. A
  document whose unticked items all carry a `later:` tag counts as done for its milestone and has
  `implementation: done`; it goes back to `in-progress` when the later milestone starts on it.
  Work for the acts after the Acts 1–2 release names the act instead (`— **later: Act 4**
  (under water)`), since no milestone covers it yet.

### M4 parts

M4 is built in parts, each a commit series the user can play, in this order: **A** the Act 1
arsenal on Level 01 (the simulation split into weapon, enemy and objective parts, the seven Act 1
weapons, the trait layer rules, overdrive, spare power); **B** Level 02; **C** Level 03; **D**
Level 04; **E** Level 05 (the first boss); **F** Level 06; **G** Level 07 and the act end; **H**
the close-out (balance tests, balancing sheet, bullet tags, test fire, Varga's intel lines). The
open points in a part's documents are settled with the user when the part starts. Each level's
production art is a concept round right after its part, so M4 ships no placeholders.

## Implementation

- [x] M0 Skeleton
- [x] M1 First flight
- [x] M2 Level 01
- [x] M3 The campaign loop
- [x] M4 Act 1 (built, parts A–H; concept round 26 closed and every M4 part's art final; the
      balance tests pass in part H's builds; the user played the build — its one finding, the
      Choir's silent stage directions, fixed in round 29 —; CI green on the branch; merged into
      main on 2026-10-06)
- [ ] M5 Act 2
- [ ] M6 Acts 1–2 release

## Open questions

- None open.

## Decisions

- 2026-10-01: Drafted after the tech stack was approved.
- 2026-10-01: User decisions: a branch per milestone, merged after the user has played the
  build; production art starts after M2.
- 2026-10-01: Approved by the user.
- 2026-10-01: M0 Skeleton done: played and accepted by the user, CI green on three OSes.
- 2026-10-01: M1 First flight done: played and accepted by the user.
- 2026-10-02: M2 Level 01 done: played and accepted by the user (data files, level runner, Level 01 with backdrop, HUD and debrief). Production art can start now (see Rules).
- 2026-10-02: M3 part B1 built (round 11 wiring, campaign state, saves, briefing, mission failed and
  game over, a placeholder hangar); part B2 builds the hangar.
- 2026-10-02: M3 part B2 built: the hangar (shop, loadout with the power budget, repair, undo,
  intel by sensor level, save, launch with warnings), the fitted Pulse Cannon level, shield,
  plating and engine flying, and the six B1 follow-up decisions. M3 stays open for the user's
  review; the other weapons, specials, utility effects and the spare-power bonus fly with M4.
- 2026-10-02: Mouse support for the menus, hangar and options added to M6 (user decision).
- 2026-10-02: M3 close-out (user decision): later-milestone items are marked `later: Mn` in their documents (rule under *Rules*) and added to the M4 and M5 rows; the sound test and the transmission static are built; the portrait expressions, briefing images and the intel's L2 portraits go to the art track. Every M3 document is now `done` under that rule; M3 stays open until the user has played the build.
- 2026-10-02: M3 The campaign loop done: played and accepted by the user (menus, options, briefings, hangar, saves, retry and game over); deferred items carry their `later:` milestone.
- 2026-10-02: M4 plan (user decisions): parts A–H as under *M4 parts*; a production-art round per part; the wingman's escort slot moves to M5, since Rook joins at L08.
- 2026-10-02: M4 part A built: the Act 1 arsenal on Level 01 (`--loadout` to fly it before its shop levels), overdrive and the spare-power bonus in flight, the HUD's weapon rows, the effects proposed as final in concept round 14. Deferred items of acts beyond M6 are tagged with their act (rule under *Rules*).
- 2026-10-02: M4 part B built: Level 02 playable from Level 01 in the campaign (`--level 2` to start there), with its production art proposed in concept round 15.
- 2026-10-05: M4 part G started (user decisions D1–D4, see [Level 07](../../campaign/act-1-first-contact/level-07-brood-carrier/README.md)): the Brood Carrier comes over, stops, descends and turns broadside through pre-rendered frames; a 35 s aftermath, then the debrief with the act summary, the act outro (four voiced pages with an image each) and the hangar before L08 (not built yet until M5; the Act 2 title card comes with Act 2's intro); the Decoy Flares leave part G (their unlock moves to L27, `later: Act 4`). The `space` layer and the Bomb Rack's NO GROUND items move from M4 to Act 5.
- 2026-10-05: M4 part H started (user decisions on the close-out): **D2** the Pickup magnet's
  radius and the sensor suite's L2 threat arrows are built in H; the Targeting computer stays out of
  the shop until M5 (its L06 data-core unlock stays in the save). **D3** the per-bullet difficulty
  tags are closed as met by the per-enemy `difficulty:` overrides and the waves' `skip`
  ([difficulty](../../systems/difficulty/README.md)). **D4** Dr. Varga gets one text line per sensor
  level per level. **D5** the credits screen, generated from CREDITS.md, is built in H; the
  high-score table moves to M6 (row above). **D6** the music files are checked against the final
  spec and offered as they are in concept round 26. The docs are reconciled with the build: built
  items ticked, half-built ones split, later ones tagged with their milestone or act.
- 2026-10-05: Concept round 26 closed (user: accepted as proposed): M4 part H's art is final, so
  every M4 part's production art is final and Act 1's `art` is `final` (no placeholders). M4's
  done-criteria now: the Act 1 documents are `done` (every item ticked or tagged later), except the
  [Skitter](../../enemies/air/skitter/README.md)'s "entry paths authored as data" item (the snakes
  and streams are still laid out in code by `Formations`, open since M2) and the hangar's open
  point on the third utility bay (part H's default data entry with `available: act 3` is not made
  yet); the balance tests (`BalanceTest`, `ActPlaythroughTest`) pass in part H's builds; CI
  on the branch was red on macOS until part H's fix, and the run for the part H commit had not
  finished at the close; **the user has not played the M4 build yet**. M4 stays open until then.
- 2026-10-06: M4 done: the user played the build (one finding, the Choir's stage directions made no
  sound: fixed with a stage sound, concept round 29), CI green on m4-act-1; merged into main.
