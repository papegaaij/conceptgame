---
title: Enemies
design: approved
implementation: done
art: chosen
updated: 2026-10-05
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
| [air](air/README.md) | Flying enemies on the `air`, `low-air` and `high-air` layers, incl. serpents, spinners and drone trains (22) | approved | not-started | chosen |
| [ground](ground/README.md) | Turrets, walkers, tanks, crawlers, bunkers and spawners on the `ground` layer (18) | approved | not-started | chosen |
| [naval](naval/README.md) | Surface vessels and submerged enemies (`ground` on water, `sub`) (9) | approved | not-started | chosen |
| [space](space/README.md) | Vacuum-only enemies for space levels, incl. the Leviathan (8) | approved | not-started | chosen |
| [bosses](bosses/README.md) | 7 act bosses and 5 mid-bosses | approved | in-progress | chosen |

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

- **Weak points** glow in the unit's glow hue at full brightness (slightly whitened). **Bosses and
  mid-bosses always mark their weak points in lime** (`A8FF2A`), whatever their own colours, so
  players learn one rule for where to shoot (user decision, round 06).
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
| Weak points | Drawn glowing; a damage multiplier only on a part of a multi-part unit (see [multi-part enemies](#multi-part-enemies)) — a single-part unit's weak point is drawn only |
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
  the rear half grows a new head and becomes its own, faster chain — **once per chain**: a regrown
  chain that is cut again simply dies from the cut backwards; on machines (Rail Serpent)
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
| `burst` | n aimed shots in quick succession (0.15 s apart). |
| `fan` | An n-way spread, centred on the player, straight down or along the unit's facing. |
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

<!-- data: formations -->
| Name | Description |
|---|---|
| `single` | One unit on its own, as when a new enemy is introduced. |
| `V-wing` | A V of 3–9 units led by the tip. |
| `line abreast` | A horizontal line moving down together. |
| `column` | A vertical line, one behind the other, on the same path. |
| `snake` | A column following a curving `path`, each unit delayed by a fixed interval. Readability: the head is on screen at least 1.5 s before it can reach the player, however long the snake. |
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
| `solo set piece` | One huge unit on its own, announced by radio (at most one per level). |
| `whirl cluster` | A burst of 5–8 tiny spinners released from one point, spiralling outward and ricocheting. |
<!-- /data -->

**Formation layouts** (first values, to tune): a V-wing's ranks are 50 px apart sideways; a
pincer's groups enter 60 px from their edge; a circle's centre is 180 px below the top of the
play field and its units break off in groups 0.5 s apart, a Needler at its 120 px/s entry speed.

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
| 1 First Contact | tiny, small, medium, large, huge | Coilwyrm (L06), Leviathan (L03), Gorgon Frigate (L05), Brood Carrier (L07) | Scuttler (L04) | Whirl Seed (L03) | Mantis sides (L06, L07), Coilwyrm rear loops (L06), Brood Pod Skitter bursts | 3 of 14 (Skitter, Stinger, Mantis); the 14 used in L01–L07: Skitter, Needler, Stinger, Spine Turret, Spore Bomber, Whirl Seed, Leviathan, Brood Pod, Scuttler, Polyp Mortar, Gorgon Frigate, Coilwyrm, Mantis, Brood Carrier | ✓ |
| 2 Homefront | tiny, small, medium, large, huge | Harbour Kraken (L11), Siege Spire (L14) | Creeper (L08), Ravager packs (L09, L14), Scuttler returns (L13) | Whirl Seed returns (L12) | Wraith and Mote Swarm rear (L10), Mantis sides (L12, L13), Skimmers from all edges (L13), maw Wraiths (L14) | 5 of 20 (Skitter, Stinger, Creeper, Wraith, Mantis); the 20 used in L08–L14: Needler, Skitter, Stinger, Spine Turret, Polyp Mortar, Creeper, Hive Node, Ravager, Mote Swarm, Wraith, Driftjelly, Reef Spitter, Harbour Kraken, Lamprey, Mantis, Spore Bomber, Whirl Seed, Scuttler, Skimmer, Siege Spire | ✓ |
| 3 Red Dust | tiny, small, medium, large, huge | Threadcrawler, Revenant Walker, Dust Colossus | Burrower, Threadcrawler, Shellback (L17), Warden Tank | Dust Devil (L18) | L19 rear, L20 reverse scroll, L21 breaches | 3 of 11 (Burrower, Choir Herald, Threadcrawler) | ✓ |
| 4 Deep Water | small, medium, large, huge | Eel Swarm, Abyssal Maw | Scuttler on the sea floor (L24) | Spiral Nautilus (L25) | Siren side caves, L27 rear | 0 of 10 | ✓ |
| 5 The Belt | tiny, small, medium, large, huge | Rail Serpent, Shard Drone links, Iron Sovereign | Strider (L32), Crawler Tank (L34) | Buzzsaw Drone (L31), Sovereign rings | Minelayer and Rail Serpent rear (L30), Void Leech rear | 1 of 15 (Asteroid Mite) | ✓ |
| 6 Jovian Storm | small, medium, large, huge | Halo Platform, Ascendant | Warden Tank and Strider return (L39, L41) | Halo Platform (L41), Buzzsaw Drone returns (L38) | Honour Guard rear (L40), Harrow all edges | 0 of 12 | ✓ |
| 7 Beyond the Gate | tiny, small, medium, large, huge | Leviathan and Coilwyrm return, Choir Heart | Threadcrawler returns (L45) | Whirl Seed returns (L45) | all-direction levels (L44, L46–50) | 3 of 10 (Rift Skater, Choir Seraph, Threadcrawler) | ✓ |

When a level's waves are written, keep the act passing; the check is repeated when levels are
promoted to draft. Acts 1 and 2 were rechecked against their level documents on 2026-10-01.

### Layer rules

| Enemy layer | Hit by player weapons | Collides with player | Notes |
|---|---|---|---|
| `air` / `space` | All weapons | Yes (contact damage) | The player's own plane. `space` = `air` in vacuum-only levels. |
| `low-air` | All weapons | No | Drawn smaller and lower; its bullets rise to the player plane. |
| `ground` | All weapons; `anti-ground` does ×2 | No | **Hardened** ground targets (bunkers, nodes) can only be damaged by `anti-ground` weapons (incl. the Airstrike); other shots glance off with a spark. |
| `high-air` | `homing` and `beam` only | No | Drawn larger and above the player; drops or deploys things. Descends to `air` to become fully hittable. |
| `sub` (player above water) | `anti-sub` only | No | Seen as a shadow under the waves. When it surfaces it becomes a `ground` (naval surface) target. |
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
  The look follows the speed class, not the enemy's name for its bullet: a "thorn" at standard
  speed (the Needler's) is a standard orb; the yellow needle is only for the fast class.
- The hit box of a `small` enemy bullet is 6×6 px (the 9 px orb's core).
- Every laser and area attack is telegraphed: `laser-line` ≥ 0.8 s, `laser-sweep` ≥ 0.6 s,
  `mortar` impact point marked ≥ 1 s ahead.
- No enemy bullet spawns within 72 px of the player's ship.
- Waves entering from the sides or rear get an **edge warning**: an arrow at the edge of the
  play field ≥ 3 s ahead, often with a radio call.
- A **bullet budget** caps the number of enemy bullets on screen (values per difficulty in
  [difficulty](../systems/difficulty/README.md)). Patterns degrade gracefully (fewer bullets per
  burst) when the budget is hit.

### Balancing basis

The Acts 1–2 stat blocks (round of 2026-10-01) use these first-draft assumptions; the economy
balancing sheet should use the same numbers.

**Reference player DPS** (single target, medium, a typical affordable loadout at that level;
damage units per second, Pulse Cannon L1 = 20, interpolated from the
[weapon roster](../player/weapons/README.md) and the [level budget](../systems/economy/README.md#per-level-budget)):

<!-- data: reference-dps -->
| Level | 01 | 02 | 03 | 04 | 05 | 06 | 07 | 08 | 09 | 10 | 11 | 12 | 13 | 14 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| DPS | 20 | 26 | 32 | 38 | 45 | 52 | 60 | 60 | 63 | 66 | 70 | 73 | 76 | 80 |
<!-- /data -->

Levels 08–14 were lowered on 2026-10-01 (user decision) to what a typical loadout reaches according to
`tools/balance.py` (≈ 60–80), instead of growing the economy. The Act 2 stat blocks have been rescaled
(2026-10-01): each Act 2 unit's HP was multiplied by new ÷ old reference DPS (70–130) at its first level
(e.g. L10 ×0.73, L14 ×0.62) and rounded, keeping the time-to-kill targets.

**Early levels are deliberately easy** (user decision 2026-10-01): in L01–L03 a typical
loadout is about 1.5× stronger than the reference DPS above (`tools/balance.py`). This is
accepted as gentle onboarding; the L01–L03 stat blocks and the reference curve are not raised to
compensate.

Bosses assume an **effective DPS of 0.6 × reference** (accuracy, dodging, phase windows).

**Time-to-kill targets** at a unit's first level: `tiny` one hit · `small` ≤ 0.3 s · `medium`
0.4–1.5 s · `large` parts and heads 1–3 s · `huge` set pieces 20–40 s · mid-bosses 45–75 s ·
act bosses 90–180 s (matches [bosses](bosses/README.md)).

**Act HP factor** (user decision 2026-10-01): a unit returning in a later level gets
`HP × (reference DPS at that level ÷ reference DPS at its first level)`, so its time-to-kill stays
the same; no elite variants. The reference DPS curve is extended act by act as levels are written.

**Damage to the player** (shield first, overflow to armour; collisions split half/half — see
[shields](../player/shields/README.md)):

<!-- data: player-damage -->
| Class | Damage | Used for |
|---|---|---|
| `small` bullet | 4 | thorns, standard orbs, fan and ring bullets |
| `medium` bullet | 6 | large orbs, bursts from heavies, acid spit |
| `heavy` hit | 10 | mortar direct hits, slam arms, rail shots |
| `laser` | 8 per touch | `laser-sweep` / `laser-line` (once per sweep or line) |
| Contact | tiny 6 · small 10 · medium 15 · large 20 · huge 25 | rammers and bodies on the player's layer |
<!-- /data -->

**Ramming:** a `tiny` or `small` unit that touches the ship is destroyed by the impact, so its
contact damage lands once.

For scale: the starting ship (shield 20 + armour 60) survives about 20 small bullets.

**Bullet speed classes:** slow 90–120 px/s · standard 140–170 px/s · fast 190–260 px/s (the
play field is 540 px tall).

**Bounties** (Act 1 terms, medium; the act factor 1.6^(act−1) is applied automatically, see
[economy](../systems/economy/README.md)): `tiny` 2–5 · `small` 10–15 · `medium` 18–30 ·
hardened and `large` 40–60 · `huge` set pieces ≈ 15 % of their level's budget (sum of part
bounties). Mid-bosses and act bosses are given as **absolute** medium credits in their level
(15 % / 30 % of that level's budget) and are not act-scaled again. Accepted exception (user
decision 2026-10-05): the [Coilwyrm](air/coilwyrm/README.md)'s parts total 86, above the `large`
class, as cutting a multi-part enemy up is extra work (a head-first kill pays 40). Check: level
01's worked budget (Skitter 5, Needler 12) is unchanged; a typical Act 1 level of 90–120 kills
earns 600–900 credits from kills plus ground targets and caches, inside the 1 000–1 500 budget.

### Difficulty scaling hooks

The global multipliers (enemy HP, fire rate, bullet speed, bullets per pattern, aiming,
formation size, bullet budget, credits) are defined once in
[difficulty](../systems/difficulty/README.md); every enemy gets them (scaled HP is rounded half to
even, at least 1). What an enemy's stat block
adds are **overrides**:

- Authored variants rather than multipliers: per-difficulty changes to the unit's patterns in its
  `difficulty:` block (a fan's bullets, a burst's shots, a boss attack's `count`, `burst`,
  `interval` or spiral `arms`); a level leaves a wave out on a difficulty with `skip` (see
  [difficulty](../systems/difficulty/README.md); per-bullet `medium+` / `hard-only` tags were
  dropped for these, user decision D3 of M4 part H).
- Hard-only elements: extra attack phases, `death-burst` on selected enemies.
- Opting out of a multiplier where it would break the enemy (e.g. a boss phase with a fixed
  bullet count).

### Target-the-objective hook

In `defend` and `escort` levels some units prefer the objective (a structure to defend or a
convoy unit, see [allies](../allies/README.md)) over the player. This is a per-level setting,
not part of a unit's stat block: the level document lists which units or waves get it and in
which mode. Units without it ignore the objective.

| Mode | Behaviour |
|---|---|
| `nearest` | Aimed attacks go at whichever is closer: the player or the nearest objective unit |
| `alternate` | Every second aimed shot goes at the objective while it is closer than the player |
| `in-arc` | Attacks the objective whenever it lies in the unit's facing arc, otherwise the player |
| `always` | The unit's attack goes at the objective: divers dive at it, mortars mark their impact on it, rammers ram it |

- **Choosing the target.** `nearest` is re-evaluated every simulation step (a turret's barrel
  turns toward the current choice), and an aimed shot picks its target as it is fired: the player
  or the nearest living objective unit, centre to centre. Only `aimed` and `burst` attacks follow
  the hook in `nearest` and `alternate`; a fan along a unit's facing is aimed at nobody.
- An objective-aimed attack uses the unit's normal pattern, look and damage class; whether and
  how much it hurts the objective is in the ally's spec. Shots aimed at the player never hurt an
  objective that only takes objective-aimed damage.
- Telegraphs still apply (mortar impact markers on the objective, edge warnings).
- Difficulty: a level may drop the hook on easy (e.g. divers go back to the player) or add
  units to it on hard.

## Concept art

Concept [round 03](../concept-rounds/round-03/README.md) gives the enemies their first visuals: the Act 1 Vrell set in [air](air/README.md) and [ground](ground/README.md), three Ascendancy units for faction contrast and the Act 1 boss in [bosses](bosses/README.md). Two Vrell design languages are proposed: **A "Sleek chitin"** (smooth, elongated, glossy violet chitin with thin glowing teal seams, pink eye as weak point) and **B "Armoured brood"** (bulky segmented carapace plates, claws and spikes, glow only between plates and in eye clusters). The key Act 1 enemies are shown in both; the others in A. Prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/lineup-r03-a.png](concept/lineup-r03-a.png) | All round-03 enemies at native scale next to the player on four backgrounds, then at 2× with names, plus the Brood Carrier at 1/4 scale — size and readability check | superseded by the r04 lineup |

Concept [round 04](../concept-rounds/round-04/README.md) — colour pass on the chosen enemies with the [role colours](#role-colours) (chitin base = role family, glow = kind of threat; Ascendancy black & gold with a per-unit accent and a thin gold/red rim light). Same models and sheet layout; generator `tools/concept/enemies_r04.py`.

| File | What | Status |
|---|---|---|
| [concept/lineup-r04-a.png](concept/lineup-r04-a.png) | Round-04 lineup: all chosen enemies in their role colours at native scale on four backgrounds, 2× with names, the Brood Carrier at 1/4 scale, and the role-colour legend | chosen |

Concept [round 05](../concept-rounds/round-05/README.md) — size lineup from the tiniest Mote to the Leviathan; generator `tools/concept/enemies_r05.py`.

| File | What | Status |
|---|---|---|
| [concept/size-lineup-r05-a.png](concept/size-lineup-r05-a.png) | Every round-04 and round-05 unit plus the player at 1×, sorted by area; Coilwyrm, Leviathan and Brood Carrier below | superseded by r05-b |
| [concept/size-lineup-r05-b.png](concept/size-lineup-r05-b.png) | Size lineup with the Ravager, Shellback and its curled ball added | chosen |

Concept [round 06](../concept-rounds/round-06/README.md) — lineup of the round-06 units; generator `tools/concept/enemies_r06.py`.

| File | What | Status |
|---|---|---|
| [concept/lineup-r06-a.png](concept/lineup-r06-a.png) | The 11 round-06 units at 1× next to the player, Skitter, Scuttler and Ravager; Threadcrawler chain and assembled Halo Platform below | chosen |

Concept [round 09](../concept-rounds/round-09/README.md) — generator `tools/concept/vfx_r09.py`.

| File | What | Status |
|---|---|---|
| [concept/enemy-bullets-r09-a.png](concept/enemy-bullets-r09-a.png) | Enemy bullet set: 9 types in Vrell and Ascendancy colours with a readability test over the chosen scenes | chosen |

Production art for concept round 12 (the Level 01 batch; part P2 opens the round), review files built from the final frames in `assets/` by `tools/art/enemy_bullets.py` (`--review` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/enemy-bullets-final-r12-a.png](concept/enemy-bullets-final-r12-a.png) | Final Vrell `small` bullets: the orb (15×15, 4-frame core pulse) and the needle (23×23, 16 headings clockwise from straight down; a gold deep band and a tight 2/3 halo instead of the faint olive one) | chosen |
| [concept/enemy-bullets-final-r12-a.gif](concept/enemy-bullets-final-r12-a.gif) | Orbs and needles drifting over a dark blue field | chosen |

Production art for concept round 26 (the M4 part H batch), review files built from the final frames in `assets/` by `tools/art/bullet_medium.py` (`--review` rebuilds only them); prompts: [concept/prompts.md](concept/prompts.md).

| File | What | Status |
|---|---|---|
| [concept/enemy-bullet-medium-final-r26-a.png](concept/enemy-bullet-medium-final-r26-a.png) | Final Vrell `medium` bullet: the large pulsing orb (`orb-medium_0..5`, 23×23, 13 px body, 6-frame pulse; round 09's large orb in the small orb's colours) next to the small orb | proposed |
| [concept/enemy-bullet-medium-final-r26-a.gif](concept/enemy-bullet-medium-final-r26-a.gif) | Large and small orbs drifting over a dark blue field, the large ones slower | proposed |

## Implementation

- [x] Rescale the Act 2 unit and boss HP to the lowered reference DPS for L08–L14 (balancing basis)
- [x] Data-driven enemy definitions using the stat block fields.
- [x] Movement patterns from the vocabulary implemented as reusable behaviours: those the Act 1
      units fly (their stat blocks and Levels 01–07; `EnemySpec`, `FlightPath`, `Formations`).
- [ ] The other movement patterns with the first unit that flies each — **later: M5** (the Act 2
      units) and **later: Act 3** to Act 7 (e.g. `burrow`, `latch`, `teleport`, `mirror`).
- [x] Attack patterns from the vocabulary implemented as reusable emitters: those the Act 1 units
      and bosses use (`aimed`, `burst`, `fan`, `ring`, `spiral`, `laser-sweep`, `mine`, `mortar`,
      `spawn`, `death-burst`, `none`; `EnemyGun`, `Lob`, `Mine`).
- [ ] The other attack patterns with the first unit that uses each — **later: M5** (the Act 2
      units) and **later: Act 3** to Act 7 (e.g. `laser-line`, `link`, `aura`, `drain`, `reflect`).
- [x] Formation spawner that places enemies by formation name and entry edge (`Formations`): the
      Act 1 formations (single, V-wing, line abreast, column, snake, stream, pincer, circle,
      whirl cluster, carrier + escorts, convoy, the walkers' paths and the chains' paths).
- [ ] The other formations with the first level that uses each — **later: M5** (e.g. `swarm`,
      `submerged ambush`) and the acts after it.
- [x] Layer rules for hit detection and collision on `ground`, `low-air`, `air` and `high-air`: what each weapon delivery reaches, hardened ground targets, contact on the player's layer.
- [ ] The `space`, `sub` and `deep` layers — **later: M5** (`sub`, the naval levels) and **later: Act 5** (`space`, the belt's open-space levels from L31; Level 07 flies on `air` like L01–L03).
- [x] Bullet rendering order, telegraphs, edge warnings and the bullet budget (bullets above every
      layer, the `laser-sweep` wedge and the `mortar` marker, `EdgeWarnings`, `Rules.bulletBudget`).
- [x] Global difficulty multipliers with per-enemy overrides (the stat blocks' `difficulty:` hooks,
      `EnemyData.Hook`).
- [x] Target-the-objective hook: per-level unit/wave configuration, mode `nearest` (Level 04's
      convoy; `LevelScript`, `EnemyBullet`).
- [ ] The modes `alternate`, `in-arc` and `always` — **later: M5** (Level 13, the first level that
      uses them).
- [x] Multi-part enemies: segment chains following the head's path history, articulated parts,
      per-part HP and destroyable/armoured/vital parts, chain splitting (the Coilwyrm's `Chain`, the
      Gorgon Frigate's necks, the Leviathan's and the Brood Carrier's parts).
- [ ] A machine chain's split (the rear half stops, drifts and explodes) — **later: Act 5** (the
      Rail Serpent).
- [x] Angle-set sprites (16/32 angles, nearest frame) and radial spinners (16 for the Skitter and
      the needle, 32 for turrets, 48 for the Coilwyrm; the Whirl Seed spinning; `EnemyLooks`).
- [x] Act 1 passes the variety checklist (as built: [act 1](../campaign/act-1-first-contact/README.md#enemy-use-per-level)).
- [ ] Acts 2–7 pass it as built — **later: M5** (Act 2) and **later: Act 3** to Act 7.

## Open questions

- None open.

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
- 2026-10-01: Concept round 05 closed: Ravager and Shellback chosen; size lineup r05-b is the reference.
- 2026-10-01: Concept round 06: all new enemies and bosses liked except the Harbour Kraken (doesn't read as a creature from the depths) and the Halo Platform (rotation too jagged); Driftjelly's waterline ring reads as a drawn circle. All three are redone in round 07 under the new water and smooth-rotation rules in art direction.
- 2026-10-01: Boss and mid-boss weak points always glow lime (user decision). The Wraith uses rust chitin with blue-violet veins everywhere (its own round-06 sheet); the bone/violet Wraiths in the Siege Spire mockup are superseded.
- 2026-10-01: Layer hit rules settled (user accepted the recommendation): all weapons hit `air`, `low-air` and `ground`; `anti-ground` deals ×2 to ground and is required for hardened ground targets; only `homing` and `beam` hit `high-air`; only `anti-sub` hits `sub` (seen from above). Low-air and ground enemies don't collide with the player.
- 2026-10-01: Chain splitting: a cut Coilwyrm's tail end grows a new head once per chain; a second cut kills the severed part. Machines (Rail Serpent) never regrow.
- 2026-10-01: The 25 units of levels 01–14 promoted to full specs in their category directories; balancing basis (reference DPS, TTK targets, damage classes, bullet speeds, bounty classes) added.
- 2026-10-01: Balance: reference DPS for L08–L14 lowered to the typical loadout (≈ 60–80) instead of growing the economy; Act 2 HP to be rescaled.
- 2026-10-01: Returning units use an act HP factor (reference DPS ratio), no elite variants.
- 2026-10-01: Enemy damage values in the balancing basis confirmed; this document owns them.
- 2026-10-01: Concept round 09: enemy bullet set chosen.
- 2026-10-01: Act 2 unit and boss HP rescaled to the lowered L08–L14 reference DPS (Creeper, Hive Node, Ravager, Wraith, Driftjelly, Reef Spitter, Lamprey, Skimmer, Harbour Kraken, Siege Spire); the Mote Swarm stays at 1 HP per mote; bounties unchanged. Act 1 units list no Act 2 HP, so no act factor was applied.
- 2026-10-01: Balancing basis: the player being about 1.5× stronger than the reference in L01–L03 is accepted as gentle onboarding (user decision).
- 2026-10-01: Variety coverage rechecked against the level documents: Act 1 uses 14 units (3 insectoid), Act 2 about 20 (5 insectoid: Skitter, Stinger, Creeper, Wraith, Mantis); both still pass.
- 2026-10-01: New target-the-objective behaviour hook (modes nearest, alternate, in-arc, always), configured per level for defend and escort levels; L04 and L13 use it instead of per-unit overrides.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../reviews/acts-1-2/README.md).
- 2026-10-01: M1: the layer rules for weapons without special traits are encoded in `vanguard.sim.Layer` (hit by standard shots: air, low-air, ground; contact: air only); `sub`, hardened ground targets and the trait exceptions follow with the units that need them.
- 2026-10-01: Rules from M1 (user decisions): HP wording — a Pulse Cannon L1 shot does 2 damage units; `tiny` and `small` rammers are destroyed on impact; the snake readability rule is about the head (on screen ≥ 1.5 s before it can reach the player), so snakes of any length stay allowed.
- 2026-10-02: The balancing basis (reference DPS, damage to the player) and the formation vocabulary moved into [data.yaml](data.yaml) (M2 data files); their tables are rendered from it, and `tools/balance.py` reads the reference DPS from there. Each promoted enemy's stat block lives in a `data.yaml` next to its README.
- 2026-10-02: M2 (Level 01): the formation spawner places snake, V-wing, line abreast
  (front and rear), stream, pincer and circle by entry edge (`vanguard.sim.Formations`), with
  edge warnings ahead of side and rear waves (at least 1.5 s, or the wave's `warning`); enemy
  bullets are drawn above every layer, need 72 px to the ship and respect the bullet budget;
  standard shots hit `air` and `ground` targets, `air` units ram. The other formations, attack
  patterns, layers and telegraphs follow with the levels that use them, so those items stay open.
- 2026-10-02: First tuning values accepted as documented first values (user decision): burst gap
  0.15 s, `small` enemy bullet hit box 6×6, V rank spacing 50 px, pincer 60 px from its edge,
  circle centre 180 px below the top, 0.5 s between circle break groups, a Needler breaking off
  at 120 px/s. They are still constants in code (`vanguard.sim.EnemyGun` for the burst gap and
  the bullet hit box, `vanguard.sim.Formations` for the layouts); moving them into data needs the
  gun, bullet and formation planner to carry them, which waits for the next tuning pass.
- 2026-10-02: The Needler's thorn uses the standard orb look; the yellow needle stays reserved for
  the fast class (user decision).
- 2026-10-02: Edge warnings start at least 3 s ahead (was 1.5 s; user decision after playing Level 01, the warnings were easy to miss).
- 2026-10-02: Production art (Level 01 batch, `tools/art/enemy_bullets.py`): the Vrell orb as 4 frames (core pulse; the game now animates it at 15 fps, each bullet at its own phase) and the needle as a 16-heading angle set (`needle_<k>`, k × 22.5° clockwise from down; not used by Level 01). The body has 1-bit alpha, the outer glow four translucency steps. Review files proposed for round 12.
- 2026-10-02: Concept round 12 closed (user decision): the Vrell `small` bullets (orb and needle) approved as **final**; this index's `art` stays `chosen`, since the other enemies' art is still concept art.
- 2026-10-02: M4 part A: the layer rules of the weapons are in the simulation (`WeaponSpec.Delivery.reaches`, hardened ground targets glance off weapons without `anti-ground`); a level's ground target can be `hardened` in its data.
- 2026-10-02: M4 part B (user decision): a single-part unit's weak point is drawn only (the glowing spot, no multiplier); damage multipliers belong to the parts of multi-part units (the bosses, the Leviathan), which have their own hit boxes.
- 2026-10-03: Target-the-objective hook (main-agent choice, M4 part D): `nearest` is re-evaluated
  every step, an aimed shot picks its target as it fires, and only aimed attacks follow the hook,
  so with the crawler rule (only crawler-aimed shots hurt crawlers, user decision) a Scuttler's
  facing fan never hurts the convoy. The `fan` may be centred on the unit's facing.
- 2026-10-03: M4 part D (Level 04) adds to the vocabulary in the simulation: the `drift` and `sine`
  movement, the `walk` movement on authored ground paths with facing as state, the `spawn` attack
  (Brood Pod), a fan along the facing and an aimed attack only while facing away (Scuttler),
  directional frontal armour, the `carrier + escorts` formation and the walker `single`, `pincer` and
  `convoy`, and the target-the-objective hook in mode `nearest`. The broader items above stay open.
- 2026-10-05: M4 part G (main-agent default, stated to the user): the `space` layer item was
  tagged for "the first space level" in M4, but Level 07 at the L1 point flies on `air` like
  Levels 01–03 (Earth orbit); retagged to Act 5, whose open-belt levels (from L31's Mining Rig
  Escort) are the first on `space`.
- 2026-10-05: M4 part H docs reconciliation: the cross-cutting items are built for Act 1, so each
  is ticked for what Act 1 uses and the rest is split off with the milestone or act of its first
  user (the movement, attack and formation vocabulary, the hook's other modes, the machine chain,
  the variety check of the later acts), so the document is `done` for M4. Render order,
  telegraphs, warnings, the budget, the difficulty overrides and the angle sets are ticked whole.
- 2026-10-05: M4 part H (user decision D3 = A): the overrides' first bullet (per-bullet `medium+` /
  `hard-only` tags) is reworded to the per-difficulty pattern changes in the stat blocks and the
  waves' `skip`, which cover every Act 1 case; see [difficulty](../systems/difficulty/README.md).
