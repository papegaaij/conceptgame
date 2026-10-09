---
title: Allies
design: approved
implementation: in-progress
art: final
depends-on: [../enemies, ../art-direction, ../ui/hud]
updated: 2026-10-09
---

# Allies

## Summary

Friendly units and structures that share the play field with the player: convoys to escort,
structures to defend, and harmless set dressing such as supply drones. This part holds their
specs; level documents place them, set level-specific numbers (difficulty variants, positions,
rewards) and link here. The player's wingman Rook is not an ally in this sense: he is
equipment, see [wingmen](../player/wingmen/README.md).

## Design

### Common rules

- **Player fire never hurts allies.** The player's and Rook's shots, the Airstrike and other
  specials pass through or over them.
- **Layers** follow the [layer model](../art-direction/README.md#parallax-layer-model): `air`
  allies are on the player's plane and are hit by enemy bullets like the player; `ground`
  allies (vehicles, ships, structures) lie below it and are only hurt by what their spec lists,
  because enemy bullets travel on the player's plane.
- **Being targeted**: which enemies go for an ally is set per level with the
  [target-the-objective hook](../enemies/README.md#target-the-objective-hook).
- **Damage feedback**: a hit flash on every hit; smoke below 50 %; a destroyed ally never throws
  explosion debris onto the player's plane (it burns, sinks or glides away).
- **HUD**: objective allies show in the [objective tracker](../ui/hud/README.md#left-panel-mission)
  as pips, armour bars (the shuttles) or an integrity bar.
- **No contact damage** to the player: the player can fly over or through every ally.
- **Art**: the civilian crawler's art is final (rounds 16 and 17; see *Concept art*); the evacuation
  shuttle went straight to production as an a/b in concept round 32 (M5 part D, D10 = a; a, the
  lifting body, picked and approved as final); the convoy's cargo ship and frigate go straight to
  production as one ship pair, an a/b in concept round 33 (M5 part E, E9 = a), starting from the
  container-ship and frigate models of the chosen ocean scene
  ([scene-ocean-r10-a](../art-direction/concept/scene-ocean-r10-a.png)); the other allies get
  concept art in a later round.

### Civilian crawler

CDF heavy crawler, a civilian evacuation hauler (a pressurised bus with a cargo sled). Its numbers
live in [data.yaml](data.yaml); the table below is still hand-written (`tools/sync_tables.py` has
no renderer for allies yet).

| Property | Value |
|---|---|
| Size / layer | 40×72 px (40 wide, 72 long), hitbox 32×64, `ground` |
| HP | 60 at medium (difficulty variants in the level) |
| Damaged by | Only enemy shots **aimed at a crawler** through the [target-the-objective hook](../enemies/README.md#target-the-objective-hook) (normal damage class; the rule of the Nansen Relay). Shots aimed at the player, fans and air enemies' bullets pass over it. A walker's claws: 10 per second while its hitbox overlaps the crawler's as the walker passes (no grip) |
| Behaviour | Follows the level's road curve in a column at the scroll speed, so it holds its height on screen and drifts left and right as the road winds; never stops for threats. Its x is the road's x at its centre, its heading the road's direction rounded to the nearest of **7 headings** (±30° in 10° steps), each rendered, not rotated |
| Destroyed | Stops and burns on the road |
| Levels | [L04 Tranquility Run](../campaign/act-1-first-contact/level-04-tranquility-run/README.md) (five crawlers, escort objective) |

### Evacuation shuttle

Civilian orbital shuttle with a CDF evac stripe. Its numbers live in [data.yaml](data.yaml) (the
table is hand-written), with the air escort's hits, banking and glide (M5 part D, the
[schemas](../tech/architecture/README.md#data-file-schemas)); the simulation flies it.

| Property | Value |
|---|---|
| Size / layer | sprite 64×40, hitbox 48×28 px, `air`, always nose up (rendered banking frames, no headings) |
| HP | armour 120 at medium, no shield, no regeneration (difficulty variants in the level: Level 10's 180 / 120 / 90) |
| Damaged by | Every enemy bullet that touches its hit box, by the bullet's class (`small` 4, `medium` 6), the bullet spent; an `air` enemy's body by the enemy's tier once per contact, a `tiny` or `small` enemy destroyed by the impact as when it rams the ship (user decision D2 = a of M5 part D). Shots aimed at the player cross the band too. A level may make a unit untouchable for a while (the liftoff, the climb-out, a scripted loss): fire and contact then pass through it |
| Behaviour | Holds its **station** in the level's formation band and drifts slowly round it on an authored **lane sway** (deterministic, on the level clock; D1 = a); never reacts to threats or steers into the player; the units' hit boxes never overlap. A level may lift the shuttles off pads (rising from the ground layer's scale to the air scale) and climb them out off the top edge. Below 50 % it trails smoke and its armour bar in the tracker turns amber |
| Destroyed | Loses power and glides down into the `far` layer over about 3 s, scaled down, darkened and trailing smoke; no debris on the play plane (a presentation effect: the simulation removes it at once) |
| HUD | The two-line [objective tracker](../ui/hud/README.md#left-panel-mission): the count of saveable shuttles over one small armour bar per shuttle (D3 = a) |
| Levels | [L10 Evacuation Corridor](../campaign/act-2-homefront/level-10-evacuation-corridor/README.md) (five shuttles, *Lifeline One–Five*; stations, liftoff, scripted loss and fail rule there) |

### Convoy cargo ship

UTC container ship (naval surface). Its numbers live in [data.yaml](data.yaml) (M5 part E, step
E2b: `follows: stations`, `damaged_by.slams`, the
[schemas](../tech/architecture/README.md#data-file-schemas)); the table is hand-written. The
simulation sails it as a unit of a level's `convoy` block.

| Property | Value |
|---|---|
| Size / layer | about 56×120 px bow-up, so it fits one 120 px slam lane (first draft); `ground` (naval surface), water rules apply (bow wave, V-wake and prop-wash, a foam collar at the hull) |
| HP | four hits: survives three boss slams (smoke and a list from the first), sinks on the fourth; the level sets the difficulty variants (Level 11's easy: five, hard: two). Tuned 2026-10-09 so a pilot who cuts the slam arms first keeps the convoy ([Level 11](../campaign/act-2-homefront/level-11-atlantic-convoy/README.md#boss--mid-boss)) |
| Damaged by | Only the [Harbour Kraken](../enemies/bosses/harbour-kraken/README.md#behaviour)'s slams, one hit per slam in its lane; no other enemy attack, bullet or contact touches it (it lies below the player's plane) |
| Behaviour | Holds a **screen-space station** set by the level at the scroll speed, so it stays put in the lower half of the screen while the sea streams past (no sway, wakes only); never reacts to threats. When the scroll halts at the arena it glides over ≈ 3 s to the lane the level gives it; after the boss it holds clear (in its lane, or at a hold point the level gives it) until the sea has scrolled the level's `hold_clear` (Level 11: Platform Tiamat's deck has passed), then glides back to its station (on the simulation's real steps) |
| Destroyed | Lists and sinks in a foam ring (a presentation effect; the simulation marks it sunk at once); the radio names it; the frigate picks up the crew |
| HUD | A pip in the level's one-line `CONVOY` tracker: green, amber after its first hit, a red flash then dark when sunk ([HUD](../ui/hud/README.md#left-panel-mission)) |
| Levels | [L11 Atlantic Convoy](../campaign/act-2-homefront/level-11-atlantic-convoy/README.md) (*Halvorsen*, *Mbeki*, *Saint-Laurent*; stations, lanes and the secondary objective there) |

### Escort frigate

CDF escort frigate (naval surface). Its numbers live in [data.yaml](data.yaml) (M5 part E, step
E2b: `follows: stations`, no `damaged_by`, `flak`); the table is hand-written.

| Property | Value |
|---|---|
| Size / layer | about 40×110 px (first draft); `ground` (naval surface), water rules apply |
| HP | none: nothing damages it, and it is never lost |
| Behaviour | Holds its station with the convoy at the scroll speed; its **flak bursts** are presentation only (puffs on `low-air` over the convoy and a quiet distant flak sound, hitting nothing). Stays out of boss arenas: at the halt it drops back off the bottom edge over ≈ 3 s and returns to its station after the boss, once the convoy's hold is over |
| Levels | [L11 Atlantic Convoy](../campaign/act-2-homefront/level-11-atlantic-convoy/README.md) (*CDFS Ruyter*) |

**A naval convoy is not an escort** (M5 part E): the level gives it in a `convoy` block of its own,
outside the objectives, so it never fails the mission; Level 11's secondary objective counts its
cargo ships afloat. `{ally}` in a radio line becomes the unit's **name** (Halvorsen), where a
crawler's or shuttle's becomes its number word. The boss checkpoint keeps each ship's state.

### Nansen Relay

The Arctic grid relay, a friendly `defend` structure.

| Property | Value |
|---|---|
| Size / layer | footprint 96×96 px on `ground`: dish array, mast, prefab modules, a helipad |
| Integrity | 600 at medium, no regeneration (difficulty variants in the level); a bar in the objective tracker |
| Damaged by | Only attacks **aimed at the relay** through the [target-the-objective hook](../enemies/README.md#target-the-objective-hook) (normal damage class of the attack; a diver's hit is 10). Shots aimed at the player that cross it do nothing. Relay-aimed bullets have the normal enemy look |
| Behaviour | Stationary; launches supply drones (see Roster) |
| Destroyed | Goes dark with a smoke column; the level's mission fails |
| Levels | [L13 Polar Relay](../campaign/act-2-homefront/level-13-polar-relay/README.md) |

## Roster

| Name | Summary | Design |
|---|---|---|
| CDF supply drone | Small `air` drone flying a slow arc and dropping an armour patch; cannot be hit by anyone. L13 (from the relay), L14 | idea |
| CDF lifeboat | Drifting friendly lifeboat towing a cargo pod; shots pass through it, only its tow cable is hittable (hidden crate). L07 | idea |
| Crewed drydock | `ground` structure carrying enemy turrets; saved when its turrets die before it leaves the screen. It is scenery and cannot be damaged itself. L02 | idea |

## Concept art

Prompts and briefs: [concept/prompts.md](concept/prompts.md). Generator:
[tools/concept/allies_r16.py](../../tools/concept/allies_r16.py).

| File | What | Status |
|---|---|---|
| [concept/rejected/civilian-crawler-r16-a.png](concept/rejected/civilian-crawler-r16-a.png) | Civilian crawler A: tracked crawler-transporter, 72×40 (deck on four twin-track trucks, passenger drum, cargo pods, amber corner beacons); on regolith, column of five, damaged and wrecked | rejected — c chosen |
| [concept/rejected/civilian-crawler-r16-b.png](concept/rejected/civilian-crawler-r16-b.png) | Civilian crawler B: six-wheeled rover train, 72×40 (three pressurised cylinders abreast, cab car in the middle, amber beacons); on regolith, column of five, damaged and wrecked | rejected — c chosen |
| [concept/civilian-crawler-r16-c.png](concept/civilian-crawler-r16-c.png) | Civilian crawler C: pressurised bus with a cargo sled, 40×72 (the long size reading); on regolith, column of five (84 px apart, 60 would overlap), damaged and wrecked | chosen |
| [concept/civilian-crawler-final-r17-a.png](concept/civilian-crawler-final-r17-a.png) | Production art, round 17 (`tools/art/civilian_crawler.py`): the 7 headings (±30° in 10° steps) with 3 wheel frames each, the 7 wrecks and the HUD pip (sheet) | chosen |
| [concept/civilian-crawler-final-r17-a.gif](concept/civilian-crawler-final-r17-a.gif) | Production art, round 17: a column of five following a winding Luna road, each at the heading nearest the road's direction (motion) | chosen |
| [concept/evacuation-shuttle-r32-a.png](concept/evacuation-shuttle-r32-a.png), [.gif](concept/evacuation-shuttle-r32-a.gif) | Evacuation shuttle a, "lifting body" (`tools/art/shuttle.py`, production quality, M5 part D, round 32): 64×40, five bank frames (−30…+30°), damaged, liftoff steps, the glide wreck, engine flames and flare, the HUD pip; in the game, approved as final | chosen |
| [concept/rejected/evacuation-shuttle-r32-b.png](concept/rejected/evacuation-shuttle-r32-b.png), [.gif](concept/rejected/evacuation-shuttle-r32-b.gif) | Evacuation shuttle b, "heavy lifter" (`tools/art/shuttle.py --variant b`, production quality): the same set; review files only | rejected |
| [concept/convoy-ships-r33-a.png](concept/convoy-ships-r33-a.png), [.gif](concept/convoy-ships-r33-a.gif) | Convoy ship pair a, "Atlantic line" (`tools/art/convoy_ships.py`, production quality, M5 part E, round 33): the ocean scene's feeder container ship (56×120: afloat, damaged and listing with its fire, 10 sinking steps through the surface, collar, wake, HUD pip) and grey CDF frigate (40×110: collar, wake, gun flash, flak burst); in the game until the pick | chosen |
| [concept/rejected/convoy-ships-r33-b.png](concept/rejected/convoy-ships-r33-b.png), [.gif](concept/rejected/convoy-ships-r33-b.gif) | Convoy ship pair b, "Reactor run" (`tools/art/convoy_ships.py --variant b`, production quality): a rust-red heavy-lift carrier with its bridge forward and reactor cargo, and a CDF stealth trimaran frigate; the same sets; review files only | rejected |

## Implementation

- [x] Ally entity type: layer, HP or integrity, damage sources per spec, immune to player fire (the ground convoy, `vanguard.sim.Convoy`; the air and structure allies come with their levels)
- [x] Damage feedback: hit flash, smoke below 50 %, non-debris destruction per type (the crawler: a wreck burning on the road)
- [x] HUD objective tracker hookup (pips or integrity bar) (the crawler's pips; the relay's integrity bar with Level 13)
- [x] Specs above loaded from data; level overrides (difficulty HP, positions) from the level
- [x] Civilian crawler: follows the road curve with 7 headings; hit only by crawler-aimed shots and pass-through claws (10/s)
- [x] Evacuation shuttle, simulation and data (M5 part D, Level 10; `vanguard.sim.Convoy` with `LevelScript.Air`, `ShuttleEscortTest`): an `air` escort holding stations in a band with the lane sway, no road (D1 = a); hit by every enemy bullet and `air` contact, once per contact, small rammers destroyed and paid (D2 = a); untouchable windows (the pads and the liftoff, the climb-out, a scripted loss: fire and contact pass through); lost units kept where they were lost; the keys `bullets`, `contact`, `banks` and `glide` read
- [x] Evacuation shuttle, presentation (M5 part D, the game): the liftoff's scale, the banking frames, smoke below 50 %; the glide into `far` when lost; its armour bars in the tracker (D3 = a); production sprite (round 32's a)
- [x] Convoy cargo ship and escort frigate, simulation and data (M5 part E, step E2b, Level 11;
      `vanguard.sim.Convoy` with `LevelScript.Naval`, `NavalConvoyTest`):
      `follows: stations` (screen-space stations at the scroll speed, the glide to the arena lanes
      and back, the frigate's exit and return; the hold clear of the boss's platform before the way
      back, `hold_clear` and `hold`, 2026-10-09); `damaged_by: slams` (one hit per slam in its lane,
      four hits, the level's easy five and hard two, 2026-10-09); sunk ships kept for the secondary
      and the boss checkpoint
- [x] Convoy cargo ship and escort frigate, presentation (M5 part E, steps E1c and E3c): wakes,
      smoke and listing after a hit, sinking in a foam ring; the frigate's flak as a cue only; the
      HUD pips; production sprites (round 33's ship pair a/b, E9 = a; variant a under the game's
      names until the pick) — `ConvoyLooks` (naval), `MissionPanel.drawConvoyTracker`,
      `Level11LooksTest`, `PartEHudTest`
- [ ] Nansen Relay: integrity, relay-aimed damage, dark when destroyed; data and production art — **later: M5 part G** (Level 13)
- [ ] CDF supply drone (roster): arc and armour patch drop — **later: M5 part G** (Level 13) and **part H** (Level 14)

## Open questions

- Sprites for the relay: a later concept round (the cargo ship and frigate are round 33's a/b,
  M5 part E).

## Decisions

- 2026-10-01: Created (user decision) to hold the friendly units that were defined only in level
  documents: crawlers (L04), shuttles (L10), convoy cargo ships and frigate (L11), the Nansen
  Relay (L13), plus supply drones, the L07 lifeboat and the L02 drydocks as roster rows. Level
  documents now link here instead of repeating the specs.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../reviews/acts-1-2/README.md).
- 2026-10-03: Concept round 16 closed (user decision): civilian crawler variant **c** chosen, the
  pressurised bus with a cargo sled, with the long size reading: 40 wide × 72 long (the size row
  above). Variants a (tracked crawler-transporter) and b (rover train), both 72×40, moved to
  `concept/rejected/`. Knock-ons for M4 part D: the column needs 84 px spacing instead of
  Level 04's "60 px apart" (60 would overlap), and the chosen Luna road (18–28 px) must widen.
  It is a concept, not production art, so `art: chosen`.
- 2026-10-03: Crawler damage (user decisions, part D): crawlers are hit only by enemy shots aimed at
  a crawler (the Nansen Relay rule) instead of by every ground-enemy bullet; a crawler-aimed bullet
  still hurts the player, so the player can body-block it. Scuttler claws pass through: 10 per
  second while a walker overlaps a crawler as it scrolls past, no grip. The column's spacing is
  84 px and the Luna road widens to fit (see [Level 04](../campaign/act-1-first-contact/level-04-tranquility-run/README.md#the-convoy)).
- 2026-10-03: Crawler data (main-agent choices): the numbers moved into [data.yaml](data.yaml), the
  first ally data (`planned (part D)`: no loader reads it yet). The road's bends are shown with 7
  rendered headings, ±30° in 10° steps. Chosen here: the 32×64 hitbox, axis-aligned at every
  heading.
- 2026-10-03: M4 part D step 2 (main-agent choices): the loader reads [data.yaml](data.yaml) and the
  simulation flies the crawler as a convoy unit of an `escort` primary objective (the convoy rolls in
  from the bottom edge one unit per second to its stations, then follows the road). A
  crawler-aimed shot hits the first crawler it touches, not only the one it was aimed at (the column
  is in its path), and a walker is any `ground` enemy that is not fixed to the ground. Until a smoke
  and a fire effect exist, the wreck's fire is the engine flame drawn additively and the smoke a
  darkened small explosion.
- 2026-10-03: Concept round 17 (user decision): the civilian crawler's production art approved as **final** ([round 17](../concept-rounds/round-17/README.md)).
- 2026-10-05: M4 part H docs reconciliation: implementation `done`; every item is ticked (the Act 1 ally, the civilian crawler; the Act 2 allies come with their levels in M5).
- 2026-10-06: M5 plan (a stated default of part A): the Act 2 allies get their checklist items,
  tagged with the M5 part of their first level (shuttle D, convoy E, relay and supply drone G and
  H); the document stays `done` for M4 under the deferral rule.
- 2026-10-08: M5 part D (user decisions of 2026-10-08 for Level 10): **D1 = a** the evacuation
  shuttle holds an authored station in the level's band and drifts on a deterministic lane sway,
  never reacting to threats; **D2 = a** every enemy bullet and every `air` contact hurts it (a
  `tiny` or `small` enemy destroyed by the impact), as the spec said, made precise: the bullet is
  spent, a contact hurts once; **D3 = a** its HUD is an armour bar per shuttle under the count, not
  the pips the spec said; **D10 = a** its sprite at production quality as an a/b in round 32 (a in
  the game until the pick). Stated defaults: untouchable windows (liftoff, climb-out, a scripted
  loss: fire and contact pass through); the glide into `far` as a presentation effect; the numbers
  in [data.yaml](data.yaml) with the planned keys (`bullets`, `contact`, `banks`, `glide`) in its
  comment until the simulation reads them (`follows: lanes` is read already). Our readings, for
  review: always nose up with 5 banking frames (full bank at 20 px/s sideways; 60 in the first draft never reached a bank frame at the lanes' sway), a 3 s glide. The
  document goes to `review` for the shuttle's spec and is `in-progress` again for its build.
- 2026-10-08: M5 part D step D4 (main-agent choices, for review in round 32): the simulation flies
  the shuttle and reads its keys (`bullets`, `contact`, `banks`, `glide`; `follows: lanes` only
  on the `air` layer). Our readings: a contact hurts a shuttle once while the enemy's body overlaps
  it (a new contact after it left again); the shuttles stand untouchable on their pads before the
  liftoff, scrolling with the ground; a bullet hits the first shuttle it touches that can be hit, so
  one passing through an untouchable shuttle hurts the next; a lost shuttle stays where it was lost in
  the simulation and the game draws its glide from `ticksSinceLost()`; with the debug option
  `--invulnerable` losing every saveable shuttle does not fail the level.
- 2026-10-08: Concept round 32 closed (user): the evacuation shuttle **a** "lifting body" (already in
  the game) approved as **final**, the weak spots as they are (the subtle bank, the small scorches,
  the hazy wreck); b "heavy lifter" rejected and moved to `concept/rejected/`. The shuttle's spec and
  our readings (the stations, sways and pads, the untouchable pads and joint liftoff, a contact once
  per overlap, a bullet passing an untouchable shuttle, a rammer paid, the full bank at 20 px/s, the
  wreck's 64 px slide and glide, `--invulnerable` keeping an air escort from failing) accepted as
  built, so `design: approved`; the shuttle's items are ticked, the rest wait for their M5 parts.
- 2026-10-08: M5 part E (user decisions of 2026-10-08 for Level 11): **E9 = a** the cargo ship and
  the frigate go straight to production as one ship pair, an a/b in concept round 33 (the ocean
  scene's models as the start). Stated defaults: the naval convoy holds screen stations in the lower
  half at the scroll speed (no sway, wakes only), nothing hurts it before the arena, the cargo ships
  glide to their lanes over ≈ 3 s at the halt while the frigate drops back off the bottom edge and
  returns after the boss; the frigate's flak is presentation only; only the Kraken's slams hurt a
  cargo ship (one hit per slam, two hits, easy three in the level); a naval convoy is a level block
  of its own, never the primary; ship names as `{ally}`; the boss checkpoint keeps the ships.
  Our readings, for review in round 33: the ships' glide back to their stations after the boss;
  the sinking a presentation effect (the simulation marks the ship sunk at once). `design` goes to
  `review` for the convoy's spec; `implementation` stays `in-progress`.
- 2026-10-08: M5 part E, step E2b (main-agent choices, first values for review in round 33): the
  cargo ship's hit box 48×112 and the frigate's 32×102 (they only place the stations and lanes:
  nothing on the player's plane touches a ship); the cargo ship smokes and lists from its first hit
  (`smoke_below: 0.75`, so also after easy's first of three); the frigate's `hp: 1` is a placeholder
  the key needs (nothing damages it); its flak bursts every 2 s while it holds its station (not
  while it glides or is away), at a place above it over the convoy that follows from the bursts so
  far (no random draw). A ship is in the lane its centre's x lies in, also while it glides; a sunk
  ship stays a wreck where it sank and glides nowhere.
- 2026-10-08: Presentation as built (M5 part E, step E3c; our reading, for review in round 33):
  each hull bow up with its wake under it (16 frames at 20 fps) and its collar over it (9 steps a
  frame), registered by `assets/pivots/<ally>.json`; the wakes fade with the scroll's speed, out
  as the scroll halts at the arena and back as it resumes; a cargo ship that took a slam shows its
  damaged (listing) frame with its deck fire and three smoke puffs from the pivots, a hit flashes it
  white, a large water burst marks a slam on it and a sunk ship plays its ten sinking steps over 3 s
  where it went down (scrolling with the sea), then is gone. The frigate's bow gun flashes with each
  flak burst; the burst is a puff on low-air over the convoy.
- 2026-10-09: The cargo ship takes **four** slams at medium (was two; Level 11's easy five, hard
  two): with two, every slam the left arm lies awash in strikes Halvorsen's or Mbeki's lane, and no
  pilot could cut it before they sank (measured with an arm-first autopilot; see
  [Level 11](../campaign/act-2-homefront/level-11-atlantic-convoy/README.md#decisions)). Its
  `smoke_below` is 0.85 so it still smokes from its first hit. A naval convoy **holds clear** after
  the boss: the level's `hold_clear` (px of scroll) and a unit's optional `hold` point (the
  [schemas](../tech/architecture/README.md#data-file-schemas)); Level 11's ships wait while Platform
  Tiamat's deck scrolls through instead of gliding back under it.
- 2026-10-09: Concept round 33 closed (user): the convoy ship pair **a** "Atlantic line" (the feeder
  container ship and the grey CDF frigate, already in the game) approved as **final**, the weak spots
  as they are (the muted cargo ship, the light list); b "Reactor run" rejected and moved to
  `concept/rejected/`. The convoy's spec and our readings (the stations and glide, four slams at
  medium, the hold clear of Tiamat with Mbeki off the bottom edge, the rafts on the flanks, the
  frigate's flak as presentation) accepted as built, so `design: approved`; the convoy's items are
  ticked, the rest wait for their M5 parts.
