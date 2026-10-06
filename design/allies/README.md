---
title: Allies
design: approved
implementation: done
art: final
depends-on: [../enemies, ../art-direction, ../ui/hud]
updated: 2026-10-06
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
  as pips or an integrity bar.
- **No contact damage** to the player: the player can fly over or through every ally.
- **Art**: the civilian crawler's concept is chosen (round 16, variant c; see *Concept art*); the other
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

Civilian orbital shuttle with a CDF evac stripe.

| Property | Value |
|---|---|
| Size / layer | sprite 64×40, hitbox 48×28 px, `air` |
| HP | armour 120, no shield, no regeneration |
| Damaged by | Enemy bullets and contact damage, as they hurt the player |
| Behaviour | Moves with the scroll in a loose formation band, drifting along authored lanes; never steers into the player. Below 50 % it trails smoke and its HUD pip flashes |
| Destroyed | Loses power and glides down into the `far` layer trailing smoke |
| Levels | [L10 Evacuation Corridor](../campaign/act-2-homefront/level-10-evacuation-corridor/README.md) (five shuttles, *Lifeline One–Five*) |

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

## Implementation

- [x] Ally entity type: layer, HP or integrity, damage sources per spec, immune to player fire (the ground convoy, `vanguard.sim.Convoy`; the air and structure allies come with their levels)
- [x] Damage feedback: hit flash, smoke below 50 %, non-debris destruction per type (the crawler: a wreck burning on the road)
- [x] HUD objective tracker hookup (pips or integrity bar) (the crawler's pips; the relay's integrity bar with Level 13)
- [x] Specs above loaded from data; level overrides (difficulty HP, positions) from the level
- [x] Civilian crawler: follows the road curve with 7 headings; hit only by crawler-aimed shots and pass-through claws (10/s)
- [ ] Evacuation shuttle: authored lanes, damage, smoke below 50 %, the glide down when lost, its HUD pips; data and production sprite — **later: M5 part D** (Level 10)
- [ ] Convoy cargo ship (two slams) and escort frigate (flak as a cue only); data and production sprites — **later: M5 part E** (Level 11)
- [ ] Nansen Relay: integrity, relay-aimed damage, dark when destroyed; data and production art — **later: M5 part G** (Level 13)
- [ ] CDF supply drone (roster): arc and armour patch drop — **later: M5 part G** (Level 13) and **part H** (Level 14)

## Open questions

- Sprites for the shuttle, cargo ship, frigate and relay: a later concept round. The cargo ship
  and frigate can reuse the ocean scene's models.

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
