---
title: Enemies
design: draft
implementation: not-started
art: proposed
updated: 2026-10-01
---

# Enemies

## Summary

All hostile units in the game: 59 regular enemies across the Vrell, the Jovian Ascendancy and
Ascendancy/Vrell hybrids, plus 7 act bosses and 5 mid-bosses. They range from tiny swarmers to
huge multi-part creatures, and include serpents, walkers, spinners and machines as well as
flyers. This document defines the shared vocabulary for enemy specifications: the stat block,
size tiers, multi-part and orientation rules, movement, attack and formation patterns, layer
rules, bullet readability, difficulty scaling and the per-act variety checklist. The category
directories hold the rosters.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [air](air/README.md) | Flying enemies on the `air`, `low-air` and `high-air` layers, incl. serpents, spinners and drone trains (22) | draft | not-started | chosen |
| [ground](ground/README.md) | Turrets, walkers, tanks, crawlers, bunkers and spawners on the `ground` layer (18) | draft | not-started | proposed |
| [naval](naval/README.md) | Surface vessels and submerged enemies (`ground` on water, `sub`) (9) | draft | not-started | none |
| [space](space/README.md) | Vacuum-only enemies for space levels, incl. the Leviathan (8) | draft | not-started | chosen |
| [bosses](bosses/README.md) | 7 act bosses and 5 mid-bosses | draft | not-started | chosen |

## Design

### Factions and visual language

| Faction | Look | Gameplay identity | Weak points |
|---|---|---|---|
| **Vrell** | Grown, not built: chitin plates, organic curves, bioluminescent glow (teal/violet). The hive grows many body plans: insectoid, serpentine, cetacean, crustacean, cephalopod, seed and plant forms. | Swarms, spawners, organic projectiles (thorns, spores, acid), living terrain; serpents and leviathans that move as many parts. | The glowing parts. Briefings teach "aim for the glow". |
| **Jovian Ascendancy** | Human-built: angular, black hulls with gold trim, visible hex-pattern shields. Aircraft, tanks, walking mechs, spinning drones, drone trains and rotating platforms. | Disciplined formations, shields, missiles, rail guns, mines. Fewer units, each tougher. | Exposed engines at the rear, and shield generators. |
| **Hybrids** (Act 6+) | Ascendancy black-and-gold plating with Vrell tissue growing through the seams. | Combine both: regeneration, reflecting shields, death bursts. | Tissue seams, visible as glowing cracks. |

Unmarked Ascendancy machines in Acts 3–4 (Ghost Drone, Revenant Walker, Depth Hunter) are grey
with no trim. They are recognisably human-made, but belong to no known faction.

#### Role colours

Round 03 feedback: the Vrell read as uniformly magenta. From round 04 every unit gets its own
colour identity inside its faction, so players learn threats by colour (draft rule, awaiting the
user; colour values live in `tools/concept/render/enemy_models.py`):

- **Vrell chitin base = role family**

  | Base | Hex (mid / dark / spike) | Role family | Round 04 units |
  |---|---|---|---|
  | Plum | `A020A8 40004A C890D8` | Swarm fodder | Skitter |
  | Rust | `B04A2C 3A1008 E0B890` | Fast attackers, divers | Stinger |
  | Bone / ivory | `DCCFB4 5C4A5C F4ECD8` | Ranged gunners and snipers | Needler, Mantis |
  | Olive | `7C8C3A 263010 C8C890` | Bombers, area denial from the air | Spore Bomber |
  | Slate (mauve-grey) | `7C6878 241A24 C8B8C0` | Rooted ground units | Spine Turret, Polyp Mortar |
  | Teal-black | `1E5C5A 06201E 8AB8A8` | Spawners and carriers | Brood Pod, Brood Carrier |

- **Vrell glow hue = kind of threat** (seams, eyes, veins, weapon tips)

  | Glow | Hex | Threat | Round 04 units |
  |---|---|---|---|
  | Teal | `00FF9A` | Contact, ramming, spawning | Skitter, Brood Pod, Brood Carrier veins |
  | Violet | `9A4DFF` | Aimed shots | Needler, Spine Turret |
  | Crimson | `FF3038` | Lasers, sweeps and dives (lines of danger) | Stinger, Mantis |
  | Lime | `A8FF2A` | Area denial: mines, spores, acid | Spore Bomber, Polyp Mortar |

- **Weak points** glow in the unit's glow hue at full brightness (slightly whitened). Multi-part
  bosses may use a contrasting glow for weak points: the Brood Carrier is teal-veined with
  **lime** bay sacs and core.
- **Ascendancy** stay black & gold; each unit adds one secondary accent (Talon red, Gilded
  Gunship white, Rail Bunker gunmetal) and every Ascendancy sprite gets a 1 px **rim light**:
  gold on edges facing the key light (top-left), red on edges facing away.
- **Reserved hues are never used on bodies or glows**: enemy-bullet magenta `FF40FF`, needle
  yellow `FFFF40` and orange (see [art direction](../art-direction/README.md#readability-rules)),
  player blue / white / cyan. Vrell needles are drawn yellow, orbs magenta, as the bullet rules
  require (round 03 drew needles magenta).
- New units pick a base by role and a glow by threat; a unit whose combination is already
  taken differs by shape and size, never by a new hue outside the tables.

### Stat block template

When an enemy is promoted from a roster row to its own directory, its README gets this stat
block, followed by behaviour notes and the standard sections.

| Field | Meaning |
|---|---|
| Faction | Vrell / Ascendancy / Hybrid / Unmarked |
| Layer | From the layer vocabulary (`ground`, `low-air`, `air`, `high-air`, `sub`, `space`) |
| Size tier | `tiny` / `small` / `medium` / `large` / `huge` (see [size tiers](#size-tiers)) |
| Size | Sprite size in px at the 960×540 baseline (e.g. 36×36; see [art direction](../art-direction/README.md)) |
| Parts | `single`, or the part list for multi-part enemies: segments / articulated parts, with per-part HP and which parts are destroyable (see [multi-part enemies](#multi-part-enemies)) |
| Orientation | `fixed` (always faces down), `16 angles`, `32 angles` or `radial` (see [orientation](#orientation-and-rotation)) |
| HP | In **damage units**: 1 = one shot of the starting front gun at upgrade level 1 |
| Armour / shield | Damage reduction or a shield layer with its own HP; the traits that bypass it |
| Speed | px/s at 960×540 |
| Movement | Pattern name from the movement vocabulary + parameters |
| Attack | Pattern name(s) from the attack vocabulary + interval, bullet count and bullet speed |
| Formations | Formation names it appears in |
| Weak points | Hitboxes with a damage multiplier |
| Effective traits | Weapon traits that do extra damage or are needed |
| Credits | Bounty at medium in Act 1 terms (see [economy](../systems/economy/README.md)); score is derived from it (see [scoring](../systems/scoring/README.md)) |
| Death | Explosion size, debris, drops, death behaviour (e.g. death burst) |
| First level | Level number where it is introduced |
| Difficulty hooks | Overrides of the global scaling (see below) |

### Size tiers

Waves mix sizes on purpose: tiny swarmers make a screen feel alive, huge units make it feel
dangerous. Sizes at the 960×540 baseline (the play field is 480×540); sprite sizes per class are
in [art direction](../art-direction/README.md#sprite-sizes-native-pixels).

| Tier | Size (px) | HP guideline | Typical roles | Examples |
|---|---|---|---|---|
| `tiny` | 16–24 | 1 | Flocks, seeds, clusters; die in one hit, threaten by numbers and paths | Skitter, Whirl Seed, Mote Swarm, Asteroid Mite |
| `small` | 28–48 | 2–8 | Fodder gunners, interceptors, drones | Needler, Talon, Buzzsaw Drone, Glow Angler |
| `medium` | 56–110 | 10–40 | Gunships, walkers, tanks, spawners | Gilded Gunship, Scuttler, Warden Tank, Brood Pod |
| `large` | 120–250 (chains: total length) | 40–150, often per part | Mechs, segment chains, rotating platforms, rays | Coilwyrm, Strider, Rail Serpent, Halo Platform, Abyss Ray |
| `huge` | ≥ 1/3 of the play field, up to multi-screen | per part; 150+ in total | Set-piece creatures and structures that are not bosses | Leviathan |

Mid-bosses are `large` to `huge`, act bosses `huge`. A `huge` regular enemy is a set piece: at
most one on screen, announced by radio, with a bounty close to a mid-boss.

### Multi-part enemies

- **Segment chains** (Coilwyrm, Threadcrawler, Rail Serpent, Eel Swarm): the head moves; every
  segment follows the head's **path history** at a fixed distance, so the body traces exactly
  where the head has been (loops and swirls stay intact). Chains are 6–16 segments.
- **Articulated parts** (Leviathan tail and fins, claws, turret rings): attached to a body at a
  pivot and animated relative to it (sweeps, lagged follow-through), not following a path.
- **Per-part HP.** Each part has its own HP and hitbox. Parts are `armoured` (bullets spark off,
  no damage), `destroyable` (break off, pay a part bounty, may change the enemy's behaviour) or
  `vital` (the weak point; destroying it kills the whole enemy).
- **Chains:** the head is vital. Destroying a body segment splits the chain: on the Coilwyrm
  the rear half grows a new head and becomes its own, faster chain; on machines (Rail Serpent)
  the rear half stops, turns into drifting wreckage and explodes after 1 s. The tail pays a
  bonus when destroyed first.
- **Scoring:** part bounties add up to at least the whole-enemy bounty, so dismantling a big
  enemy part by part pays as well as killing its weak point; killing the vital part first
  destroys the rest with a chained explosion and a time bonus.
- **Hits and collision:** only parts on the player's layer collide. A part flashes when hit; an
  armoured part flashes grey so players learn where not to shoot.

### Orientation and rotation

The first enemies all have a clear nose and tail and only work flying down the screen. Enemies
that move in any direction must read from any angle:

- **`16 angles`** (22.5° steps) for enemies that turn to face their movement; **`32 angles`**
  for large and slow enemies and for turrets. Every angle is a separate pre-rendered frame from
  the 3D model with fixed lighting; the game shows the nearest one (see
  [animation rules](../art-direction/README.md#animation-rules)).
- **`radial`** for spinners: radially symmetric designs that spin in place or along their path
  and need no facing at all.
- **`fixed`** remains for enemies that only ever fly down the screen (most of the original
  roster).
- Independent parts (turrets, heads, arms) turn on their own, e.g. a tank hull drives one way
  while its turret tracks the player.

### Movement pattern vocabulary

| Name | Description |
|---|---|
| `straight` | Constant velocity along a vector. |
| `swoop` | Enter on a curve, cross the screen, exit on a curve. |
| `sine` | Straight path with a sinusoidal side-to-side offset. |
| `dive` | Enter, pause briefly, then accelerate toward the player's current position. |
| `hover` | Enter, hold a position for a set time, then exit. |
| `strafe` | Horizontal pass across the screen at a fixed height. |
| `orbit` | Circle a point (fixed, or relative to a leader or the player). |
| `path` | Follow a hand-authored spline (used for snakes and set pieces). |
| `chase` | Steer toward the player with a limited turn rate. |
| `terrain` | Fixed to the ground layer; moves only with the scroll. |
| `crawl` | Moves along the terrain (roads, rooftops, sea floor) at its own speed. |
| `burrow` | Alternates between submerged (invulnerable, shown as a shadow or ripple) and surfaced. |
| `drift` | Slow, inertial movement with no intent (mines, jellies, derelicts). |
| `latch` | Attaches to the player; the player shakes it off by moving hard back and forth. |
| `teleport` | Vanishes and reappears after a visible warp-in telegraph of at least 0.5 s. |
| `mirror` | Mirrors the player's horizontal movement around the screen centre. |
| `swirl` | A curving path that bends back on itself (S-bends, hooks), crossing its own track; the main pattern for chains and flocks. |
| `spiral-in` / `spiral-out` | Circles a point (often the player) while the radius shrinks or grows. |
| `loop` | A full loop-the-loop mid-path, often to turn around and come back from the rear. |
| `figure-8` | Traces a figure-eight around two points, typically across the whole play field. |
| `cross` | Crosses the screen diagonally or side to side, entering and leaving through different edges. |
| `rear-entry` | Enters from the bottom edge (or loops around to it) and attacks up the screen; always edge-warned. |
| `flock` | Boids-like group movement (separation, alignment, cohesion) steered by a leader or target; produces swirling clouds. |
| `walk` | A walker moving along the terrain on its own heading (not just scrolling), turning to face where it goes; the gait follows the walk cycle. |
| `surface` | Emerges from the ground or water at a telegraphed spot (ripple, dust), acts, and dives again; like `burrow` but it travels while submerged. |
| `ricochet` | Bounces off the play field edges (and off terrain walls) at equal angles. |
| `spin` | Rotates about its own axis while moving (a modifier on any other pattern); spin speed may change with the attack phase. |
| `chain` | Follows a leader's path history at a fixed distance (used by every segment of a chain). |

### Attack pattern vocabulary

| Name | Description |
|---|---|
| `none` | Harmless except on contact (rammers). |
| `aimed` | A single shot at the player's current position. |
| `burst` | n aimed shots in quick succession. |
| `fan` | An n-way spread, centred on the player or straight down. |
| `ring` | A circular burst of n bullets. |
| `spiral` | A rotating stream of bullets. |
| `laser-sweep` | A continuous beam rotating through an arc; the arc is shown for 0.6 s first. |
| `laser-line` | A charged straight beam; a thin line telegraphs it for ≥ 0.8 s. |
| `homing` | Slow projectiles that track the player; shootable (1–3 HP). |
| `mine` | Drops stationary or drifting mines; shootable. |
| `mortar` | An arcing lob to a target point marked on the ground layer; bursts into a `ring` on impact. |
| `kamikaze` | Rams the player at speed; may combine with `death-burst`. |
| `spawn` | Releases other enemies (a carrier or node). |
| `link` | A damaging energy beam between two or more units; acts as a barrier. |
| `aura` | Buffs nearby enemies (shield, speed or regeneration); shown as a visible ring. |
| `drain` | Drains the player's shield or generator energy while in range or latched. |
| `reflect` | A frontal shield bounces non-`beam` player shots back as enemy bullets. |
| `death-burst` | Releases a `ring` or `fan` when destroyed. |

### Formation vocabulary

| Name | Description |
|---|---|
| `V-wing` | A V of 3–9 units led by the tip. |
| `line abreast` | A horizontal line moving down together. |
| `column` | A vertical line, one behind the other, on the same path. |
| `snake` | A column following a curving `path`, each unit delayed by a fixed interval. |
| `stream` | A continuous trickle of single units from alternating edges. |
| `pincer` | Two groups entering from the left and right edges at the same time. |
| `rear ambush` | A group entering from the bottom edge (always warned; see readability). |
| `circle` | Units orbiting a point, then breaking off. |
| `grid` | A block of units that holds and shifts sideways (a nod to classic arcades). |
| `wall` | A full-width line with one or two gaps to fly through. |
| `cross` | Groups from all four edges converging. |
| `carrier + escorts` | A large unit (often `spawn`) with escorts in `orbit`. |
| `turret nest` | 3–6 ground turrets in a cluster with overlapping fire. |
| `convoy` | Ground or naval units in a column along a road, river or lane. |
| `submerged ambush` | `sub` units that surface together around the player. |
| `swarm` | A loose, randomised cloud with flocking behaviour. |
| `whirl cluster` | A burst of 5–8 tiny spinners released from one point, spiralling outward and ricocheting. |

### Variety checklist per act

Every act must pass this checklist (counting the units that appear in the act, new or
returning; returning units are listed in the act's level notes):

1. At least **three size tiers**, including `tiny` or `huge`.
2. At least one **multi-part** enemy (segment chain or articulated).
3. At least one **walker or ground mover** (walking, crawling, tracked, burrowing).
4. At least one **spinner or radial** enemy.
5. At least one **non-front entry** pattern (sides, rear, loop-around, surfacing).
6. **No more than about half insectoid** units (insect, arachnid or myriapod bodies; crustaceans,
   serpents, cetaceans, cephalopods, seeds and machines don't count).

Current coverage (✓ = passes):

| Act | Size tiers | Multi-part | Walker / ground mover | Spinner / radial | Non-front entry | Insectoid share | Pass |
|---|---|---|---|---|---|---|---|
| 1 First Contact | tiny, small, medium, large, huge | Coilwyrm, Leviathan, Gorgon Frigate, Brood Carrier | Scuttler (L04) | Whirl Seed (L03) | Mantis sides, Coilwyrm rear loops | 3 of 14 (Skitter, Stinger, Mantis) | ✓ |
| 2 Homefront | tiny, small, medium, large, huge | Harbour Kraken, Siege Spire | Creeper (L08), Ravager packs (L09), Scuttler returns (L13) | Whirl Seed returns (L12) | Wraith and Mote Swarm rear (L10), all edges (L13) | 3 of 14 (Skitter, Creeper, Wraith) | ✓ |
| 3 Red Dust | tiny, small, medium, large, huge | Threadcrawler, Revenant Walker, Dust Colossus | Burrower, Threadcrawler, Shellback (L17), Warden Tank | Dust Devil (L18) | L19 rear, L20 reverse scroll, L21 breaches | 3 of 11 (Burrower, Choir Herald, Threadcrawler) | ✓ |
| 4 Deep Water | small, medium, large, huge | Eel Swarm, Abyssal Maw | Scuttler on the sea floor (L24) | Spiral Nautilus (L25) | Siren side caves, L27 rear | 0 of 10 | ✓ |
| 5 The Belt | tiny, small, medium, large, huge | Rail Serpent, Shard Drone links, Iron Sovereign | Strider (L32), Crawler Tank (L34) | Buzzsaw Drone (L31), Sovereign rings | Minelayer and Rail Serpent rear (L30), Void Leech rear | 1 of 15 (Asteroid Mite) | ✓ |
| 6 Jovian Storm | small, medium, large, huge | Halo Platform, Ascendant | Warden Tank and Strider return (L39, L41) | Halo Platform (L41), Buzzsaw Drone returns (L38) | Honour Guard rear (L40), Harrow all edges | 0 of 12 | ✓ |
| 7 Beyond the Gate | tiny, small, medium, large, huge | Leviathan and Coilwyrm return, Choir Heart | Threadcrawler returns (L45) | Whirl Seed returns (L45) | all-direction levels (L44, L46–50) | 3 of 10 (Rift Skater, Choir Seraph, Threadcrawler) | ✓ |

When a level's waves are written, keep the act passing; the check is repeated when levels are
promoted to draft.

### Layer rules (proposal)

| Enemy layer | Hit by player weapons | Collides with player | Notes |
|---|---|---|---|
| `air` / `space` | All weapons | Yes (contact damage) | The player's own plane. `space` = `air` in vacuum-only levels. |
| `low-air` | All weapons | No | Drawn smaller and lower; its bullets rise to the player plane. |
| `ground` | All weapons; `anti-ground` does ×2 | No | **Hardened** ground targets (bunkers, nodes) take 25% from weapons without `anti-ground` or `area`. |
| `high-air` | `homing` and `beam` only | No | Drawn larger and above the player; drops or deploys things. Descends to `air` to become fully hittable. |
| `sub` (player above water) | `anti-sub` and `area` only | No | Seen as a shadow under the waves. When it surfaces it becomes a `ground` (naval surface) target. |
| `sub` (underwater mode, Act 4) | All weapons; without `anti-sub` 50% damage | Yes | The `sub` layer is the play plane; see the [under water rules](../world/europa/README.md#under-water-rules). |
| `deep` | Nothing | No | Background only. |

Enemy bullets always travel on the player's plane, whatever layer fired them.

### Bullet readability rules

- Enemy bullets are drawn **above every layer except the HUD**, including foreground clouds and
  debris. Foreground may hide enemy bodies but never enemy bullets.
- Enemy bullets are bright and high-contrast with a dark outline. Player shots are paler and
  less saturated, so the two never read alike.
- Bullet colour follows the faction colours in [factions](../story/factions/README.md): player
  pale blue/white, Vrell saturated teal/violet with a bright core, Ascendancy red/gold. Because
  player blue and Vrell teal are neighbours, saturation and the dark outline carry the
  difference, not hue alone. Where a background uses the same hues (Vrell space, Europa's
  bioluminescence), enemy bullets get larger and brighter (see
  [vrell-space](../world/vrell-space/README.md) and [europa](../world/europa/README.md)).
- Shape encodes threat: small round = standard, elongated = fast, large pulsing = slow and
  heavy, diamond = homing (shootable).
- Every laser and area attack is telegraphed: `laser-line` ≥ 0.8 s, `laser-sweep` ≥ 0.6 s,
  `mortar` impact point marked ≥ 1 s ahead.
- No enemy bullet spawns within 72 px of the player's ship.
- Waves entering from the sides or rear get an **edge warning**: an arrow at the edge of the
  play field ≥ 1.5 s ahead, often with a radio call.
- A **bullet budget** caps the number of enemy bullets on screen (values per difficulty in
  [difficulty](../systems/difficulty/README.md)). Patterns degrade gracefully (fewer bullets per
  burst) when the budget is hit.

### Difficulty scaling hooks

The global multipliers (enemy HP, fire rate, bullet speed, bullets per pattern, aiming,
formation size, bullet budget, credits) are defined once in
[difficulty](../systems/difficulty/README.md); every enemy gets them. What an enemy's stat block
adds are **overrides**:

- `medium+` / `hard-only` tags on individual bullets, attack phases or formation members
  (authored variants rather than multipliers).
- Hard-only elements: extra attack phases, `death-burst` on selected enemies.
- Opting out of a multiplier where it would break the enemy (e.g. a boss phase with a fixed
  bullet count).

## Concept art

Concept [round 03](../concept-rounds/round-03/README.md) gives the enemies their first visuals: the Act 1 Vrell set in [air](air/README.md) and [ground](ground/README.md), three Ascendancy units for faction contrast and the Act 1 boss in [bosses](bosses/README.md). Two Vrell design languages are proposed: **A "Sleek chitin"** (smooth, elongated, glossy violet chitin with thin glowing teal seams, pink eye as weak point) and **B "Armoured brood"** (bulky segmented carapace plates, claws and spikes, glow only between plates and in eye clusters). The key Act 1 enemies are shown in both; the others in A. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/lineup-r03-a.png](concept/lineup-r03-a.png) | All round-03 enemies at native scale next to the player on four backgrounds, then at 2× with names, plus the Brood Carrier at 1/4 scale — size and readability check | superseded by the r04 lineup |

Concept [round 04](../concept-rounds/round-04/README.md) — colour pass on the chosen enemies with the [role colours](../README.md#role-colours) (chitin base = role family, glow = kind of threat; Ascendancy black & gold with a per-unit accent and a thin gold/red rim light). Same models and sheet layout; generator `tools/concept/enemies_r04.py`.

| File | What | Status |
|---|---|---|
| [concept/lineup-r04-a.png](concept/lineup-r04-a.png) | Round-04 lineup: all chosen enemies in their role colours at native scale on four backgrounds, 2× with names, the Brood Carrier at 1/4 scale, and the role-colour legend | chosen |

Concept [round 05](../concept-rounds/round-05/README.md) — size lineup from the tiniest Mote to the Leviathan; generator `tools/concept/enemies_r05.py`.

| File | What | Status |
|---|---|---|
| [concept/size-lineup-r05-a.png](concept/size-lineup-r05-a.png) | Every round-04 and round-05 unit plus the player at 1×, sorted by area; Coilwyrm, Leviathan and Brood Carrier below | chosen — superseded by r05-b when the animal walkers are added |
| [concept/size-lineup-r05-b.png](concept/size-lineup-r05-b.png) | Size lineup with the Ravager, Shellback and its curled ball added | proposed |

## Implementation

- [ ] Data-driven enemy definitions using the stat block fields.
- [ ] Movement patterns from the vocabulary implemented as reusable behaviours.
- [ ] Attack patterns from the vocabulary implemented as reusable emitters.
- [ ] Formation spawner that places enemies by formation name and entry edge.
- [ ] Layer rules for hit detection and collision.
- [ ] Bullet rendering order, telegraphs, edge warnings and the bullet budget.
- [ ] Global difficulty multipliers with per-enemy overrides.
- [ ] Multi-part enemies: segment chains following the head's path history, articulated parts,
      per-part HP and destroyable/armoured/vital parts, chain splitting.
- [ ] Angle-set sprites (16/32 angles, nearest frame) and radial spinners.
- [ ] Every act passes the variety checklist.

## Open questions

- **Vrell design language** (concept round 03): A "Sleek chitin" or B "Armoured brood", or a mix (e.g. B for ground and heavy units, A for fliers)? The choice applies to every Vrell enemy.
- Layer rules: should `ground` targets really be hittable by *all* weapons (Tyrian-style, simple),
  or only by `anti-ground` weapons and bombs (Raptor-style, more loadout pressure)? The proposal
  is all weapons, with hardened targets as the pressure point.
- Should `high-air` enemies be hittable at all, or only become targets when they descend?
- **Chain splitting**: should a Vrell chain (Coilwyrm) really regrow a head on its rear half when cut
  (more enemies, more chaos), or should cut-off segments simply die? Proposal: regrow for Vrell,
  die for machines (Rail Serpent).
- Score vs credits is decided in [scoring](../systems/scoring/README.md) (currently separate,
  with the score derived from the enemy's bounty).

## Decisions

- 2026-09-30: Enemies are organised by layer category (air, ground, naval, space) plus bosses.
- 2026-09-30: Global difficulty levers and the bullet budget moved to
  [difficulty](../systems/difficulty/README.md) (single source); stat blocks keep overrides.
- 2026-09-30: Bullet colours tied to the faction colours; score vs credits deferred to scoring.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-09-30: Concept round 03: first enemy visuals and the lineup sheet; Vrell design-language choice put to the user.
- 2026-09-30: Concept round 03: Vrell design language is mainly **A "sleek chitin"**, with B "armoured brood" allowed where it gives a unit character (the Needler uses B). Chosen: Skitter A, Needler B, Stinger, Spore Bomber, Brood Pod, Mantis, Spine Turret A, Polyp Mortar, Talon, Gilded Gunship, Rail Bunker, Brood Carrier.
- 2026-09-30: Concept round 03 feedback: enemies need more distinct colours — the Vrell set reads too uniformly magenta. Colour differentiation pass in round 04.
- 2026-09-30: Concept round 04: role colours drafted (chitin base = role family, glow = kind of threat; Ascendancy accent + rim light); all chosen enemies re-rendered as r04 proposals. Vrell needles now yellow per the bullet rules.
- 2026-09-30: Enemy variety (user feedback after round 04: enemies too similar in size and shape,
  all insect-like flyers with a fixed front and back). Added on top of the roster: size tiers,
  multi-part rules, orientation (16/32 pre-rendered angles, radial spinners), 13 movement
  patterns, a per-act variety checklist, and 13 new units — Coilwyrm, Leviathan, Scuttler,
  Threadcrawler, Whirl Seed, Mote Swarm, Warden Tank, Strider, Buzzsaw Drone, Rail Serpent, Halo
  Platform, plus Dust Devil and Spiral Nautilus (roster fork's own additions).
- 2026-09-30: Concept round 04: role colours adopted ("the new colors are much better") — chitin base = role family, glow = kind of threat; Ascendancy black & gold with a per-unit accent and the 1 px gold/red rim light. All r04 re-colours chosen.
- 2026-10-01: Round 05 review: the new archetypes are liked. Warden Tank and Strider are kept for the Ascendancy (the Warden Tank keeps its unmarked Act 3 hint at L19); two animal-like Vrell ground walkers added — Ravager (Act 2) and Shellback (Act 3).
