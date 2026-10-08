---
title: Implementation roadmap
design: approved
implementation: in-progress
art: n/a
depends-on: [../architecture, ../../campaign]
updated: 2026-10-08
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
| M5 | **Act 2** | Levels 08–14 and their new units and equipment | [act 2](../../campaign/act-2-homefront/README.md) levels and their enemies and equipment, the [wingmen](../../player/wingmen/README.md) escort slot (Rook from L08), the [saves](../../systems/saves/README.md) `escort` field, the Targeting computer in the shop with its effects and the Salvage scanner's ([ship systems](../../player/systems/README.md), part A), the perspective towers and roofs of L08 (art direction); built in parts A–I (below) |
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

### M5 parts

M5 is built like M4: each part is a commit series the user can play, opened with its own gap check
and the user's decisions, closed by its production-art concept round, so M5 ships no
placeholders. The order is linear; H comes last among the levels, as it reuses B's Creeper, C's
Hive Node and Ravager and D's Wraith.

| Part | Scope | Depends on | Round |
|---|---|---|---|
| **A** Act 2 systems — **done** | Rook and the escort slot ([wingmen](../../player/wingmen/README.md): AI, guns, escort inventory, side, repairs, eject, retry and checkpoint, barks, `--escort`), save format 3 with `escort`, the hangar's escort UI and the HUD's escort box; the Act 2 arsenal without water (final effects and sounds of the Tail Gun, Fan Blaster, Hornet Launcher and Swivel Gun, the Proximity Mines' delivery and `area`); the Targeting computer and the Salvage scanner; the act HP factor and the Act 1 terms in data ([enemies](../../enemies/README.md#balancing-basis), [economy](../../systems/economy/README.md)). Flown on the Act 1 levels with `--loadout` and `--escort` | — | 28 (closed): Rook's craft and eject pod, the weapon effects, the Act 2 weapon sounds, the barks' voices, the scanner's glint |
| **B** Level 08 and the Act 2 intro — **done** | Level data (dense, typical haul, four Varga lines, voiced); Creeper; the megacity backdrop with perspective towers (scenery only) and traffic lanes; Act 2's data (title card, briefing pages, images); "Homefront" final and stems; the Act 1 → Act 2 transition | A | 30 (closed): the Creeper, the megacity backdrop and towers, the Act 2 still and briefing images, Rook's briefing portraits, "Homefront" and its base stem, the billboard (a), the traffic (a), the civilian's voice (b) |
| **C** Level 09 — **done** | Hive Node (hardened, periodic spawn), Ravager (`pack`, the pounce's air window), hardened enemies; hold zones and the level clock in script time; the named-target tracker; a missed node fails at once; the collapse; the bridge secondary (a wave tag); the `required` launch warning; the balance plan's anti-ground (Bomb Rack and Rook's Mortar); multi-level units in each level's atlas; "Firestorm" final and stems with a run-time stem hook; the Kilo Lead's audition | B | 31 (closed): the collapse's look (a and b rejected, the rework c approved with two tweaks and built), the Hive Node, the Ravager, the Level 09 backdrop with the trucks and the cocoon, the briefing images, "Firestorm" and its base stem approved as final; the collapse sound (a), both pounce sounds (picked at random), the Kilo Lead's voice (a); the captures, the voiced lines, the texts, the numbers, the decisions and the build choices accepted |
| **D** Level 10 — **done** | Wraith (cloak, loop, rear entry, `rear ambush`); Mote Swarm (`flock`, `swarm`, authored paths: the Skitter's item); the air escort (shuttles: stations and lane sway, liftoff and climb-out, every bullet and contact hurting) and the scripted loss with its music duck; the `SHUTTLES` tracker with armour bars; `required: [rear]` and the plan's rear check; the ambience change by section; rear-heavy pacing; the Lifeline voices' auditions | B | 32 (closed): the Wraith, the Mote Swarm, the Level 10 backdrop at first light with the ferry hatch, the briefing images and intel portraits approved as final; the shuttle (a), the loss's look (b, the iris column), the decloak sound (b, CC-BY), the swarm sound (b), the lance sound (a), the Lifeline and Lifeline Three voices (b, b); the captures, the voiced lines, the texts, the numbers, the decisions and the build choices accepted |
| **E** Level 11 | The `sub` layer and the water rules; the Torpedo Pod and `anti-sub`; Driftjelly, Reef Spitter; convoy ships and frigate; the Harbour Kraken mid-boss; the ocean backdrop; the Bomb Rack over water | A | 33 |
| **F** Level 12 | Weather (rain, lightning reveal, gusts, the eye); Lamprey (chase, latch, drain, shake-off) with `swarm`; sondes; the storm backdrop; the mines' sounds in play | D, E | 34 |
| **G** Level 13 | `defend` with a timed halt; the Nansen Relay and its integrity bar; hook modes `alternate`, `in-arc`, `always`; Skimmer; `cross`; whiteout; supply drones; the arctic backdrop | E | 35 |
| **H** Level 14 and the act end | Root bursts; the Siege Spire act boss (root parts, maw launches, ground → air phase); the Geneva backdrop; the daylight section; Act 2's outro pages and act summary; the act-end order | B, C, D | 36 |
| **I** Close-out | `ActPlaythroughTest` and `BalanceTest` for Act 2 with Rook and the Act 2 plan; the variety checklist as built; the later tags (drones and the other utility modules to Acts 3–6); leftover sounds and the voice listen-through; the user's playthrough | all | 37 |

## Implementation

- [x] M0 Skeleton
- [x] M1 First flight
- [x] M2 Level 01
- [x] M3 The campaign loop
- [x] M4 Act 1 (built, parts A–H; concept round 26 closed and every M4 part's art final; the
      balance tests pass in part H's builds; the user played the build — its one finding, the
      Choir's silent stage directions, fixed in round 29 —; CI green on the branch; merged into
      main on 2026-10-06)
- [ ] M5 Act 2 (parts A–I under *M5 parts*; part A done, concept round 28 closed; part B, Level 08
      and the Act 2 intro, done, concept round 30 closed; part C, Level 09, done, concept round 31
      closed; part D, Level 10, done, concept round 32
      closed)
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
- 2026-10-06: M5 plan, from the gap analysis of the `m5-act-2` branch: parts A–I as under *M5
  parts*, concept rounds 28–36, one per part, as in M4. Part A started with the user's decisions
  D1–D7: **D1 = a** (part A builds Rook and the escort slot, the five Act 2 weapons that need no
  water, the Targeting computer and the Salvage scanner; the Torpedo Pod and the `sub` layer move
  to part E), **D2 = a** (Rook's kills pay like the player's), **D3 = b** (a hangar repair line for
  Rook, launch warning below 50 %), **D4 = a** (Rook's guns in an escort inventory at 60 %),
  **D5 = c** (the act HP factor only for returning `medium` and larger units from Act 2 on),
  **D6 = a** (the Targeting computer's HP bars, weak-point brackets and +20 % homing turn),
  **D7 = a** (the Salvage scanner's +10 / +20 % and glint built in part A); the gap analysis's
  stated defaults apply (save format 3, Rook only in levels from 08, `--escort`, the barks' queue
  rules, data in Act 1 terms). The Targeting computer moves from part H to part A. Details are in
  [wingmen](../../player/wingmen/README.md), [ship systems](../../player/systems/README.md),
  [enemies](../../enemies/README.md) and [economy](../../systems/economy/README.md).
- 2026-10-06: M5 part A done: concept round 28 closed with everything accepted (the mine's arming
  beep a, the secret glint a). Rook and the escort slot, the five Act 2 weapons that need no water,
  the Targeting computer and the Salvage scanner (their design rows now `approved`), the act HP
  factor and the boss-bounty rule are built and reviewed; Rook's sprites, the five weapons' effects
  and the glint are final. Two wingmen items stay open: the guns and barks tables rendered from the
  data (not built), and the balance item (`BalanceTest` with his DPS, the plan buying his guns, the
  autopilot and `ActPlaythroughTest` flying with him), tagged **later: M5 part I** with the
  close-out's Act 2 balance tests. The next part is **B**, Level 08 and the Act 2 intro (round 29).
- 2026-10-06: M4 done: the user played the build (one finding, the Choir's stage directions made no
  sound: fixed with a stage sound, concept round 29), CI green on m4-act-1; merged into main.
- 2026-10-06: The M5 concept rounds shift by one: round 29 went to the Choir's stage-sound fix on
  m4-act-1 (merged into main with M4), so part B's round is 30 and parts C–I's are 31–37 (the
  *M5 parts* table).
- 2026-10-06: M5 part B started, from its gap check, with the user's decisions D1–D6:
  **D1 = a** (the megacity's towers in true perspective as scenery only, drawn by the renderer;
  turrets, Creepers, ground targets and crates at street level and on low structures drawn
  without lean; no wall crawling: [art direction](../../art-direction/README.md#parallax-layer-model)),
  **D2 = a** (civilian traffic is backdrop scenery, shots pass through), **D3 = a** (the Act 2
  intro texts rewritten: about four act briefing pages and two Level 08 pages, no repetition of
  the Act 1 outro, the contradictions fixed), **D4 = a** (a radio event `escort-first-kill` for
  Rook's first kill and `{side}` line variants: [wingmen](../../player/wingmen/README.md)),
  **D5 = a** (the opener shortened to about 10 s, the level densified with its own units, the
  Creepers 12 → 15) and **D6 = a** (the chosen concepts and the four new backdrop sections
  straight to production; a/b only for the billboard, the traffic and the Ikoyi shelter
  civilian's voice). The gap check's stated defaults apply: the data in Act 1 terms (crate 100,
  secondary 56), no act HP factor on Level 08's units (all `tiny` or `small`); Rook's scripted
  flank line dropped, no glide-in, his side prompt at about t=6; the radio retimed to the 1 s
  rule; the Creeper's weak point drawn only, its fan aimed, the 0.5 s stagger, a 16-heading husk
  and the screech; ambience in section 1, the "Homefront" base stem from section 2 and the full
  mix in section 5; the Act 2 title card over its still with one image per act page and two for
  Level 08's briefing; an M4 save already at the Level 08 hangar skips the Act 2 intro (a new
  campaign or `--level 7` shows it); Act 2 keeps the Act 1 tactical map. Details in
  [Act 2](../../campaign/act-2-homefront/README.md), its
  [Level 08](../../campaign/act-2-homefront/level-08-neon-skyline/README.md), the
  [Creeper](../../enemies/ground/creeper/README.md), [music](../../audio/music/README.md),
  [sfx](../../audio/sfx/README.md), [voice](../../audio/voice/README.md) and the
  [schemas](../architecture/README.md#data-file-schemas). Part B's concept round is 30.
- 2026-10-07: M5 part B done: concept round 30 closed (user, 2026-10-07). The production art
  approved as final, the billboard **a** (the neon sky-sign), the traffic **a** (the wedge cars
  and the CDF gunship) and the Ikoyi shelter civilian **b** (Faith Abiola-Ellison) chosen and
  produced at the close; the captures, the voiced lines, the texts (Level 08 and Act 2 now
  `approved`), the part B numbers and the D1–D6 checklist accepted; the rule "from Act 2 on, an act
  page and a level page each fit one screen" kept, and the hangar teaser's role labels kept
  (`Speaker.role`). The next part is **C**, Level 09 (round 31).
- 2026-10-07: M5 part C started (Level 09), from its gap check, with the user's decisions D1–D11:
  **D1 = a** (a missed hive node fails the mission at once, as Level 05; no rear stream, no
  flown-on mission), **D2 = a** (in a hold zone the level clock slows with the scroll: the level's
  times are script time), **D3 = a** (no hold timeout; the hold speed by difficulty, 20 / 30 /
  40 px/s), **D4 = a** (the Ravager is `air` for the middle 0.3 s of its pounce), **D5 = a** (the
  collapse is a simulated sweep that kills the ground units under it and pays like Airstrike
  kills), **D6 = a** (the bridge secondary counts the two tagged bridge waves), **D7 = a** (a
  `required` trait warns at launch at every sensor level, counting Rook's Mortar and the specials),
  **D8 = c** (the balance plan's L09 visit: a Bomb Rack for the left Autocannon and Rook's Mortar,
  the Tail Gun to L10; a hardened check in `BalanceTest`), **D9 = a** (art straight to production
  in the last hour of the night; a/b only for the collapse's look, the Kilo Lead's voice and the
  pounce and collapse sounds), **D10 = a** (a unit several levels use goes into each of their unit
  atlases) and **D11 = a** (the Kilo Lead auditioned a/b in round 31). The gap check's stated
  defaults apply: weak points drawn only (the node's ×2 and the Ravager's ×1.5 struck); hardened
  flown for enemies, homing shots and Rook skipping them unless anti-ground; the node's spawn every
  4 s (2 Skitters, hard 3; shut within 96 px; one opening per node in the checks); six named groups
  and the `NODES A1 … C2` tracker; `pack` in the vocabulary; the data in Act 1 terms with a
  `bounty_scale`; the Creeper's act HP factor; density from the level's own roster; the radio
  retimed with the events `hold-start`, `first-pounce`, `collapse` and `requires: escort`; two
  one-screen briefing pages, Rook's teaser, Varga's four lines; the Airstrike charge only with a
  special; the cocoon on a low roof; the trucks as scenery and nothing on `far`; the base stem from
  the launch and the full mix in the holds and from the collapse; moving scenery on the real clock;
  no hold indicator; the Act 2 README's fixes. Details in
  [Level 09](../../campaign/act-2-homefront/level-09-arcology-fall/README.md), the
  [Hive Node](../../enemies/ground/hive-node/README.md), the
  [Ravager](../../enemies/ground/ravager/README.md), the
  [schemas](../architecture/README.md#data-file-schemas), the [hangar](../../ui/hangar/README.md),
  the [HUD](../../ui/hud/README.md), [music](../../audio/music/README.md),
  [sfx](../../audio/sfx/README.md), [voice](../../audio/voice/README.md) and the
  [production plan](../../art-direction/production/README.md). Part C's concept round is 31.
- 2026-10-07: M5 part C: concept round 31 closed but for row 4 (user, 2026-10-07). The Hive Node,
  the Ravager, Level 09's backdrop with the Kilo trucks and the cocoon, the two briefing images and
  "Firestorm" with its base stem approved as final; the collapse sound **a** (CC-BY, on the credits
  roll), **both** Ravager pounces kept (one picked at random per pounce) and the Kilo Lead **a**
  (Aaron Bennett) chosen and produced; the capture, the 20 voiced lines, the texts (Level 09 now
  `approved`), the part C numbers, the D1–D11 checklist and the six build choices (a)–(f) accepted.
  The collapse's look: both **a** (topples across) and **b** (pancakes into a dust wave) rejected;
  the user asked for a rework (it sways a little, then collapses straight down; its shadow moves
  with it; dust in every direction from just before the impact and a large dust blast at it; no
  sharp dust edge), variant c in progress, and the collapse's kills then spread outward from the
  tower's foot at the impact over about 1 s instead of sweeping left to right (planned, built after
  c is approved). Part C is done once row 4 closes.
- 2026-10-07: M5 part C done, concept round 31 closed (user): the collapse's look **c** approved
  with two tweaks (the shadow halved; a single lean to the right instead of a sway) and built in the
  game (the lean and the drop by the tower projection, the cast shadow, the puff sprites, the
  rubble heap, the kills outward from the tower's foot over 1 s, the sound's crash at the impact);
  hold C lasts until the collapse's dust has settled (9.0 s after the warning starts, no longer the
  blast's end at 4.0 s), so the heap stays in sight after a late kill. The next part is **D**,
  Level 10 (round 32).
- 2026-10-08: M5 part D started (Level 10), from its gap check, with the user's decisions D1–D12,
  all (a): **D1** the shuttles hold authored stations in a band with a slow lane sway and never
  react to threats; **D2** every enemy bullet and contact hurts a shuttle, tuned until the
  autopilot keeps at least 3 of the 4 saveable on medium; **D3** the tracker `SHUTTLES n / 4` over
  five armour bars; **D4** Lifeline Three untouchable before t=118 (fire passes through), the
  scripted loss costing nothing; **D5** the mission fails at once when the four saveable shuttles
  are lost, the escort's pay per shuttle home, no secondary, four level-end lines; **D6** the Wraith
  as its stat block (cloaked from the top, rear entry, decloaked exit along a side lane); **D7** the
  Wraith's HP 27 → 16, a rear check in `BalanceTest`, the plan's L10 visit refitting Rook's
  Autocannon (a `fit` action); **D8** real flocking for the Mote Swarm (its route as data, the
  Skitter's authored-paths item for snakes and swarms); **D9** `required: [rear]`; **D10** art
  straight to production, a/b only for the shuttle and the loss's glow and lance; **D11** first
  light; **D12** two voices auditioned, Lifeline and Lifeline Three. The gap check's stated
  defaults apply: data in Act 1 terms with a `bounty_scale`; the waves filled with popcorn to at
  least 50 a minute and the 3 s pacing rule, the first enemies at t≈8; 190 px/s; 3 s edge warnings
  everywhere; directions by `from`, the looping swarms `from: front` with the share by threat in the
  text; the formations `swarm` and `rear ambush`; the Wraith's veins drawn only; hard's 7-shot
  bursts, 3.0 s holds, an extra pair at t=170 and two loop-backs; the radio retimed with the events
  `first-decloak`, `first-loop-back`, `scripted-loss` and `ally-lost`, Rook's t=50 line dropped, a
  mission-failed line, "two hundred and twenty"; two one-screen briefing pages, Rook's teaser,
  Varga's four lines; the shuttles untouchable in the liftoff and the climb-out, the glide into
  `far`, armour 180 / 120 / 90; the ferry hatch with a hidden crate; "Homefront"'s base stem from
  section 2, the full mix from section 3, the −6 dB duck, the ambience to the ocean at section 4; no
  change to Rook's AI. Details in
  [Level 10](../../campaign/act-2-homefront/level-10-evacuation-corridor/README.md), the
  [Wraith](../../enemies/air/wraith/README.md), the [Mote Swarm](../../enemies/air/mote-swarm/README.md),
  the [allies](../../allies/README.md), the [schemas](../architecture/README.md#data-file-schemas),
  the [HUD](../../ui/hud/README.md), [wingmen](../../player/wingmen/README.md),
  [music](../../audio/music/README.md), [sfx](../../audio/sfx/README.md) and
  [voice](../../audio/voice/README.md). Part D's concept round is 32.
- 2026-10-08: M5 part D done, concept round 32 closed (user). After the capture: the Wraith's
  bursts fly as a fixed 40° fan straight up through the shuttle band, not aimed, and the no-fire
  distance does not apply to them (user); the capture's bugs fixed (the popcorn routes round the
  band, the wreck's full 3 s glide, the ferry hatch at 50 HP with a 12 s crate, the rear banner
  above the band, the home bar in pale mint). Hard keeps 1 of 4 shuttles home on the autopilot
  (user: hard is meant to be hard). The next part is **E**, Level 11 (round 33).
