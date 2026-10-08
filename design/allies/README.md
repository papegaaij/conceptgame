---
title: Allies
design: approved
implementation: in-progress
art: final
depends-on: [../enemies, ../art-direction, ../ui/hud]
updated: 2026-10-08
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
  lifting body, picked and approved as final); the other
  allies get concept art in a later round. The chosen ocean
  scene ([scene-ocean-r10-a](../art-direction/concept/scene-ocean-r10-a.png)) already has the
  container-ship and frigate models the convoy will reuse.

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

UTC container ship (naval surface).

| Property | Value |
|---|---|
| Size / layer | about 56×120 px bow-up, so it fits one 120 px slam lane (first draft); `ground` (naval surface), water rules apply |
| HP | two hits: survives one boss slam (smoke, listing), sinks on the second |
| Damaged by | Only the [Harbour Kraken](../enemies/bosses/harbour-kraken/README.md#behaviour)'s slams; no other enemy attacks it |
| Behaviour | Steams at the scroll speed, so it holds station in the lower half of the screen while the sea streams past; holds a lane when the scroll halts |
| Destroyed | Sinks with a foam ring; the frigate picks up the crew (radio) |
| Levels | [L11 Atlantic Convoy](../campaign/act-2-homefront/level-11-atlantic-convoy/README.md) (*Halvorsen*, *Mbeki*, *Saint-Laurent*) |

### Escort frigate

CDF escort frigate (naval surface).

| Property | Value |
|---|---|
| Size / layer | about 40×110 px (first draft); `ground` (naval surface) |
| HP | none: it cannot be damaged |
| Behaviour | Steams with the convoy; its flak bursts are a visual cue only (they hit nothing). Stays out of boss arenas |
| Levels | [L11 Atlantic Convoy](../campaign/act-2-homefront/level-11-atlantic-convoy/README.md) (*CDFS Ruyter*) |

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

## Implementation

- [x] Ally entity type: layer, HP or integrity, damage sources per spec, immune to player fire (the ground convoy, `vanguard.sim.Convoy`; the air and structure allies come with their levels)
- [x] Damage feedback: hit flash, smoke below 50 %, non-debris destruction per type (the crawler: a wreck burning on the road)
- [x] HUD objective tracker hookup (pips or integrity bar) (the crawler's pips; the relay's integrity bar with Level 13)
- [x] Specs above loaded from data; level overrides (difficulty HP, positions) from the level
- [x] Civilian crawler: follows the road curve with 7 headings; hit only by crawler-aimed shots and pass-through claws (10/s)
- [x] Evacuation shuttle, simulation and data (M5 part D, Level 10; `vanguard.sim.Convoy` with `LevelScript.Air`, `ShuttleEscortTest`): an `air` escort holding stations in a band with the lane sway, no road (D1 = a); hit by every enemy bullet and `air` contact, once per contact, small rammers destroyed and paid (D2 = a); untouchable windows (the pads and the liftoff, the climb-out, a scripted loss: fire and contact pass through); lost units kept where they were lost; the keys `bullets`, `contact`, `banks` and `glide` read
- [x] Evacuation shuttle, presentation (M5 part D, the game): the liftoff's scale, the banking frames, smoke below 50 %; the glide into `far` when lost; its armour bars in the tracker (D3 = a); production sprite (round 32's a)
- [ ] Convoy cargo ship (two slams) and escort frigate (flak as a cue only); data and production sprites — **later: M5 part E** (Level 11)
- [ ] Nansen Relay: integrity, relay-aimed damage, dark when destroyed; data and production art — **later: M5 part G** (Level 13)
- [ ] CDF supply drone (roster): arc and armour patch drop — **later: M5 part G** (Level 13) and **part H** (Level 14)

## Open questions

- Sprites for the cargo ship, frigate and relay: a later concept round. The cargo ship and frigate
  can reuse the ocean scene's models.

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
