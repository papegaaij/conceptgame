---
title: Allies
design: draft
implementation: not-started
art: none
depends-on: [../enemies, ../art-direction, ../ui/hud]
updated: 2026-10-01
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
- **Art**: no ally sprites exist yet; they get concept art in a later round. The chosen ocean
  scene ([scene-ocean-r10-a](../art-direction/concept/scene-ocean-r10-a.png)) already has the
  container-ship and frigate models the convoy will reuse.

### Civilian crawler

CDF heavy crawler, a civilian evacuation hauler.

| Property | Value |
|---|---|
| Size / layer | 72×40 px, `ground` |
| HP | 60 at medium (difficulty variants in the level) |
| Damaged by | Bullets fired by `ground`-layer enemies (turrets, walkers); a walker's claws on contact (10 per second). `air`-layer bullets pass over it |
| Behaviour | Follows the road spline in a column at the scroll speed, drifting left and right as the road winds; never stops for threats |
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

## Implementation

- [ ] Ally entity type: layer, HP or integrity, damage sources per spec, immune to player fire
- [ ] Damage feedback: hit flash, smoke below 50 %, non-debris destruction per type
- [ ] HUD objective tracker hookup (pips or integrity bar)
- [ ] Specs above loaded from data; level overrides (difficulty HP, positions) from the level

## Open questions

- Sprites for the crawler, shuttle, cargo ship, frigate and relay: a later concept round. The
  cargo ship and frigate can reuse the ocean scene's models.

## Decisions

- 2026-10-01: Created (user decision) to hold the friendly units that were defined only in level
  documents: crawlers (L04), shuttles (L10), convoy cargo ships and frigate (L11), the Nansen
  Relay (L13), plus supply drones, the L07 lifeboat and the L02 drydocks as roster rows. Level
  documents now link here instead of repeating the specs.
