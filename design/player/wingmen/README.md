---
title: Wingmen and drones
design: approved
implementation: in-progress
art: final
depends-on: [../weapons, ../../story, ../../systems/saves, ../../ui/hangar, ../../ui/hud]
updated: 2026-10-07
---

# Wingmen and drones

## Summary

Extra guns that fly themselves. The AI wingman Lt. Kenji "Rook" Tanaka flies his own ship in
the **escort slot**. Drones are cheaper helpers: light drones use a wing mount, and a heavy
drone can take the escort slot instead of Rook.

## Design

### Escort slot (recommended answer to "wing slot or separate slot?")

A separate **escort slot**, unlocked at L08 when Rook is assigned as Lancer's wingman (a story
beat in [act 2](../../campaign/act-2-homefront/README.md); Rook has been in Aegis Wing for years
and flies in the wider formation before that). It holds either Rook or one heavy drone. It draws no power
from the Stormhawk's generator: the escort brings its own. Its costs are hiring/upgrade prices
and repairs.

Why a separate slot: if Rook took a wing mount, players would compare a whole pilot to a
missile pod, and he would rarely be worth it. A separate slot keeps both choices interesting and
makes his availability a story lever: Rook is shot down at the end of L26 and is missing for
L27–L29 (see [Rook](../../story/characters/rook/README.md)). During those levels the escort slot
can only hold a heavy drone; Rook is freed at Ceres Hub in L29 and is back from L30. His
upgrades are kept.

In Act 2 the slot always holds Rook: there is no empty escort slot and no choice of escort until
the Warden (L22).

### Rook (AI wingman)

| Property | Value (first draft) |
|---|---|
| Hire | Free when he is assigned (L08); his guns and their upgrades are paid |
| Ship | An AF-12 Stormhawk built on the variant-C airframe (the blended manta from the [ship concepts](../ship/README.md)) in Rook's own colours; 42×42 sprite, slightly smaller than the player's 48×48 so the player's ship always reads first; armour 80, no shield |
| Position | Formation slot beside and slightly behind the player (left or right, hangar setting) |
| Behaviour | Keeps formation, fires whenever the player fires, dodges bullets with a delay, prefers the player's target, then an enemy near him (a flank threat, within 160 px) |
| Weapon | One gun fitted from his own inventory (see *Rook's guns* and *Escort inventory* below), each gun upgradable L1–L5 at 60 % of the player's prices |
| Downed | At 0 armour he ejects and is out for the rest of the level; his armour is repaired in the hangar, like the player's (see *Armour and repairs*) |
| Radio | Banters on the radio; warns about threats from the rear ("Six o'clock, Lancer!") |

### Where Rook flies

- **Hired** when the hangar opens with Level 08 or later as the next level: the visit after Level
  07's act end, or a loaded save that is past it. From then on the save's `escort` holds him
  ([saves](../../systems/saves/README.md)).
- He flies in **every campaign level from 08 on** while he is hired and not grounded (below). He
  does **not** fly in Act 1 levels replayed from mission select, nor in a `--level` run below 8
  (without `--escort`); his armour and guns are then left as they are.
- The L08 HUD hint "Rook's side can be set in the hangar" points at his side setting.

### Rook's AI (first-draft parameters)

Coordinates are relative to the player's centre, +y pointing down the screen; the side (left or
right) is the hangar setting and mirrors the x values.

**Ship**

| Property | Value |
|---|---|
| Armour | 80, no shield, no hit invulnerability; takes enemy bullets and contact damage like the player |
| Hitbox | 11×11 px, on the `air` layer |
| Movement | Top speed 250 px/s, full speed in 0.2 s; never closer than 40 px to the player, also where that meets the play-field margin (the player near an edge or in a corner, a side swap gliding past him: he is pushed out along the margin); keeps the 12 px play-field margin. When his straight way to his slot passes within the 40 px (a slot on the player's other side) on a side where the margins leave no room at that distance, he steers round the player the other way, 8 px outside them: with the player low on the screen the way beneath is closed and he passes over him |
| Bodies | His slot keeps his 14 px dodge clearance from every `air` enemy's hit box, over where the enemy flies in the next 0.4 s: a slot inside that box moves to its nearest edge he can reach without crossing the box (he waits on his side while a body passes), and once inside one he leaves it the shortest way that does not cross the body; such a move jinks like a sidestep and keeps the 40 px to the player |
| Collision with an `air` enemy | Rook takes the enemy's full contact damage and deals 20 to it; a `tiny` or `small` enemy is destroyed by the impact, as when it rams the player |

**Formations** — he switches automatically and glides to the new slot over 0.6 s.

| Formation | Offset | When |
|---|---|---|
| Wing (default) | (64, +28) | No other condition |
| Wide | (120, +10) | A `sides` wave is active, or an enemy is within 160 px of him (plain distance from his centre, so an enemy just ahead counts too) |
| Trail | (40, +90) | A `rear` wave is active: pursuers overtake him first and enter his firing cone sooner |

A wave is active from its first unit's entry until its last unit has died or left the screen. A
segment chain counts by the edge it entered from last: a Coilwyrm wave from the rear is a rear
wave, and a chain that loops back is one from its re-entry at the bottom edge.
When both Trail and Wide apply, **Trail** wins. When the slot lies outside the play field (the
player flies close to the edge on Rook's side, or near the bottom edge in Trail), he takes the
**mirrored slot** on the other side, and returns to his own side once it has been inside the
field for 1 s. Formation changes and side swaps start after his reaction delay.

**Reactions**

| Property | Value |
|---|---|
| Reaction delay | 0.25 s for target switches, formation changes and dodges |
| Dodging | Every 0.1 s he predicts enemy bullets 0.6 s ahead (user decision 2026-10-06; 0.4 s left him too little time against aimed fire); the soonest one that would pass within 14 px he decides on **once**: he **reacts to about 70 %** of them (user decision 2026-10-06), sidestepping up to 48 px at right angles to it after his reaction delay and keeping at it while it is the soonest, then back to his slot; the rest he misses and ignores. The 0.25 s delay inside the 0.6 s look-ahead leaves him 0.25–0.35 s to clear a bullet |
| Firing cone | 30° ahead of his ship (15° either side of straight up), range 360 px: where he picks his target. His craft never turns (banking frames only), so he fires up the screen. With the Mortar (a lobbed gun) he picks among the ground targets anywhere ahead of him within the lob's 200 px instead (user decision 2026-10-07), so a target beside the player is his while he flies in formation |
| Fire | Fires whenever the player is firing, at his gun's rate, with a target in his cone or without one; he never fires while the player does not. The target only decides what he aims at: a homing gun's shots start locked onto it (without one they find their own); the Mortar's shells land **on it** at any distance up to the lob's 200 px, their flight time and arc shortened to the distance (user decision 2026-10-07; without a target they land 200 px ahead, snapping to a ground target within 48 px, as the player's) |
| Target priority | (1) an enemy the player damaged in the last 1.0 s; (2) an enemy within 160 px of him (flank threat); (3) the nearest enemy — all within his cone (the Mortar's: ahead within 200 px). Layers follow his gun's traits (Missiles can hit `high-air`; Mortar only `ground`) |

Enemies never aim at Rook: aimed attacks and the target-the-objective hook pick the player or the
objective as before, and Rook is hit by what crosses his path. He collects no pickups and is not
pushed by the scroll. The AI's only randomness is his own generator for the dodge share, seeded
from the sortie's seed and kept in the boss checkpoint, so a sortie with him stays deterministic
(state hash, replays); runs without him keep their hashes.

**Rook's guns** — each uses a player weapon's per-level table, scaled; prices are 60 % of the
player weapon's base price, upgrades follow the [weapons](../weapons/README.md#common-rules)
formula on 60 % of the player weapon's upgrade base.

| Gun | Id | Based on | Scale | DPS L1 → L5 | Price | Upgrades L2 / L3 / L4 / L5 | Available |
|---|---|---|---|---|---|---|---|
| Autocannon | `autocannon` | [Autocannon Pod](../weapons/autocannon-pod/README.md) | × 1.5 | 12 → 37 | free (fitted when he joins) | 150 / 300 / 600 / 1 200 | L08 |
| Scatter | `scatter` | [Scatter Vulcan](../weapons/scatter-vulcan/README.md) | × 0.6 (volley) | 11 → 39 | 720 | 360 / 720 / 1 440 / 2 880 | L08 |
| Missiles | `missiles` | [Micro-missile Pod](../weapons/micro-missile-pod/README.md) | × 1.5 | 12 → 42 | 480 | 240 / 480 / 960 / 1 920 | L08 |
| Mortar | `mortar` | [Hammer Mortar](../weapons/hammer-mortar/README.md) | × 0.6 (aimed: lands on his ground target) | 15 → 54 | 900 | 450 / 900 / 1 800 / 3 600 | L08 |

- The **scale multiplies each projectile's damage**; the pattern, rate, speed, blast, turn rate
  and traits are the base weapon's at the same level. He fires the pattern from one muzzle at his
  nose; a pod weapon's pattern is that of the pod on his side (the Missiles launch outward, away
  from the player). The DPS column is the base weapon's volley DPS × the scale.
- He has **no overdrive**: an overdrive pickup is the player's alone. The Targeting computer's
  homing bonus does not reach his Missiles.
- His shots hit what the player's shots hit and pass through allies, as the player's do. His kills
  pay like the player's (see *Kills*).

### Escort inventory (user decision D4 of M5 part A)

Rook's guns are owned like the player's weapons, in their own inventory:

- Buy any of the four at its price (60 %); each owned gun keeps its own level and is upgraded
  L1–L5 separately. Exactly one is **fitted**; fitting another is free, the others stay in his
  inventory at their levels.
- Sell an unfitted gun at the [sell-back](../../systems/economy/README.md#sell-back-and-undo)
  share (60 % of all spent on it, 100 % for one bought during this visit). The fitted gun cannot
  be sold or unfitted (like the front gun), and the free Autocannon sells for its upgrades only.
- Every escort purchase, upgrade, fit and sale is part of the visit's undo log.
- His guns and the player's weapons are separate: none of his can be fitted on the Stormhawk, and
  no Stormhawk weapon on him. The escort draws no power.

### Armour and repairs (user decision D3 of M5 part A)

- His armour is kept between levels: after a won level it is saved as it ended; a failed or
  aborted attempt leaves it as he launched.
- The hangar's **Repair** has a **Rook line** beside the ship's: per point or all, at the
  difficulty's repair cost per point ([difficulty](../../systems/difficulty/README.md): free on
  easy, 5 on medium, 10 on hard), the same as the player's. **Unrepaired armour stays missing.**
- **Launch warning** when his armour is below 50 % (under 40 of 80), in the same confirmation as
  the player's warnings.
- **Grounded:** after an ejection his armour is 0. At 0 he stays home: the escort tile reads
  `GROUNDED`, the launch confirmation says he will not fly, and any repair (one point is enough)
  puts him back in the air for the next launch.

### Kills (user decision D2 of M5 part A)

Rook's kills pay **like the player's**: the enemy's bounty (with the act factor, the difficulty's
income and the level's bounty scale), its score, the chain, the objectives (a kill ratio, a
group, a named target) and the statistics. The debrief does not list them apart. This follows the
Airstrike's precedent ([specials](../specials/README.md)); the typical haul is unchanged, since
its shares are about what dies.

### Shot down, retry and checkpoint

- At 0 armour he **ejects**: the pod pops out and drifts off-screen sideways (120 px/s, level
  with where he ejected) while the ship explodes (`medium` explosion). It heads for the **nearer
  side edge unless the player's ship is in the way** (on that side of the pod and within **36 px**
  above or below its line); then it takes the other edge, so the pod never passes under the ship.
  It pops out under the explosion and is drawn **over it after 0.1 s**, with a red **glow** around
  it while its beacon blinks and a thin **trail** of light smoke behind it, so it reads on any
  ground. He is out for the rest of the level. This never fails the mission and costs no score.
- His scripted radio lines still play after he ejects (he is on the radio from his pod). A line
  that needs him flying (a level's "Rook's first kill", the `escort-first-kill` event below) does
  not fire.
- On a **retry** he starts again with his level-start state, like the player: his level-start
  armour, at least the retry's armour floor share (as the player's, see
  [retry](../../systems/retry/README.md)).
- The **boss checkpoint** includes him: his position, armour and whether he has ejected. *Retry
  from boss* puts him back in formation in that state (an ejected Rook stays out).

### Act 2 hazards

- [Lampreys](../../enemies/air/lamprey/README.md) latch only on the player.
- The gusts of [Level 12](../../campaign/act-2-homefront/level-12-storm-front/README.md) push him
  like the player.
- Ravager pounces and `air` contacts hit him as designed (his collision rule above).

### Scripted lines about him (user decision D4 of M5 part B)

Two kinds of a level's radio cues are about Rook himself (schema in
[architecture](../../tech/architecture/README.md#data-file-schemas), built in M5 part B):

- **His first kill**: the radio event **`escort-first-kill`** (no `enemy`) fires on the first
  kill in an attempt whose killing shot or blast is his (his gun's mount), while he flies. It
  never fires after he has ejected, nor in a level he does not fly in (`--escort none`, an Act 1
  replay); it fires at most once per attempt (the flag is in the state hash and the boss
  checkpoint, so a retry from the boss keeps it). A cue on it is an event line like the barks'
  triggers; its line counts as a scripted Rook line for the barks' 8 s spacing (from the moment it
  fires: a bark queued with the same kill, a streak, is withdrawn). Level 08's "Splash one" is the
  first.
- **His side**: `{side}` in a radio line becomes `left` or `right`, his side setting (the save's,
  or `--escort`'s `side=`): the line is voiced once per side and plays the take for his side, the
  subtitle showing that text. Level 08's opening "Lancer, I'm on your {side}" is the first.

### Radio barks

Rook's barks are **event lines** under the [radio queue](../../ui/hud/README.md#left-panel-mission)
rules: they wait for a gap, are dropped as stale after 6 s and never push a timed line (all but the
eject bark, an urgent line: see below), and are voiced like every Act 1 line
([voice](../../audio/voice/README.md)).

- **Spacing:** at least **8 s** from the start of any Rook line (a bark or a scripted line) to the
  next bark. A bark that fires sooner is dropped, and so is one that fires within 8 s before a
  timed Rook line is due: the scripted line says it. The **eject bark** (trigger 6) is exempt: it
  is never dropped by the spacing, a scripted line or as stale.
- **Priority:** only one bark waits at a time; a bark of a higher priority (lower number) replaces
  a waiting one, a lower one is dropped.
- **Variants:** the *n*-th bark of a trigger in an attempt (from 0) plays variant
  (level number + *n*) mod the trigger's count, so levels open on different lines and a retry
  replays the same ones.
- The **eject bark is urgent**: it cuts a bark of his that waits or is on the radio and plays at
  once like the Airstrike's call (an urgent line already on the radio finishes first), and no other
  bark replaces it.
- After he ejects, trigger 5 can no longer fire; the others still do (he watches the scope from
  his pod).
- The level documents' scripted "Six o'clock" and "flank" lines (L08 t=92, L10 t=50, L12 t=102,
  L13 t=82 and t=122, L14 t=80) are reviewed by each level's part: they suppress the bark by the
  spacing rule.

| Priority | Trigger | Variants (speaker Rook) |
|---|---|---|
| 1 | Boss warning starts (an act boss's warning, a mid-boss's sting) | "Here comes the big one." · "Heads up. Something nasty's coming." · "Oh, I don't like the look of that." · "Big contact inbound. Stay loose, Lancer." |
| 2 | A `rear` wave enters within 1.5 s (shouted) | "Six o'clock, Lancer!" · "Contacts on six! Why is it always six?" · "Check six, check six!" · "Behind us! They're on our tails!" |
| 3 | A `sides` wave enters (shouted) | "Contacts on your flank!" · "Bandits coming in from the side!" · "Flank! Watch the edges, Lancer!" |
| 4 | The player's armour falls below 30 % (again only after it was back at 30 % or more) | "You're smoking, Lancer. Ease off!" · "That hull won't take much more, Lancer!" · "Lancer, you're coming apart. Get clear!" |
| 5 | Rook's armour falls below 30 % (once per attempt) | "Taking a beating over here!" · "I'm hit. I'm okay. I'm mostly okay!" · "Rook's hurting, Lancer. Hurting bad." |
| 6 | Rook ejects (shouted) | "Punching out! Give 'em hell!" · "I'm out, I'm out! She's all yours, Lancer!" · "Ejecting! Somebody owes me a ship." |
| 7 | 10 kills (the player's and his) within 5 s; the count starts over | "Nice shooting!" · "Ha! Save some for me!" · "Now that's how it's done." · "Look at you go, Lancer." |
| 8 | An overdrive pickup appears | "Power-up! Grab it!" · "Ooh, shiny. Grab that." · "Overdrive's up for grabs, Lancer!" |

Expressions: 1, 4 and 5 `grim`; 2, 3 and 6 `fierce` and shouted; 7 and 8 `fierce` (the voice's
joking row).

### Balance

- Rook adds 12 → 42 DPS. `BalanceTest` prints his DPS beside the player's but does not count it
  against the [reference DPS](../../enemies/README.md#balancing-basis), which was set without him.
- The [balance plan](../balance-plan.yaml) buys his guns and pays his repairs from the L08 visit;
  `ActPlaythroughTest` and the test autopilot fly with him.

### Debug option

`--escort rook:<gun>:<level>[,side=left|right]` flies Rook with that gun (an id from the table
above) at that level and full armour on any level, Act 1's included, for testing; the side is the
save's (left for a new campaign) if left out. `--escort none` flies a level from 08 on without
him. For testing only, like `--loadout`. The option applies to the flights only, never to the
campaign's gear: it does not hire him, his fitted gun, side and armour stay as they were, and no
save (the autosave of a failure, a retry or a won level included) holds it.

### Data

Rook's numbers, his guns and his barks live in [data.yaml](data.yaml) (`rook`, `guns`, `barks`;
schema in [architecture](../../tech/architecture/README.md#data-file-schemas)), loaded by the
content's `WingmenData` (M5 part A). The guns and barks tables above are still hand-written: keep
them in step with the file until `tools/sync_tables.py` renders them. Rook's repair cost per point
is the difficulty's `repair_cost`, so it is not in the file.

### In the game (M5 part A)

- **His craft** is drawn like the player's: the banking frame of his bank, his two warm engine
  flames, a runtime drop shadow on the ground layer, the muzzle flash of his gun at his nose, a
  white flash on a hit, a smoke trail below 30 % armour (denser below 15 %) and, after the eject,
  the `medium` explosion and the pod drifting off to a side edge (the path rule above), over the
  explosion after 0.1 s with its beacon glow and smoke trail.
- **Production sprites** (art track, final since concept round 28; `tools/art/rook.py`), in `assets/sprites/`:
  `rook_0` … `rook_4` (42×42, hard left … hard right at −30, −15, 0, +15 and +30° of roll through
  the Stormhawk's perspective camera, as the player's `ship_0` … `ship_4`, so the pair banks as
  one; the round-09 Ember model); `rook-flame_0` … `rook-flame_2` (8×14, his warm engine flame:
  a pale yellow core, orange body and red-brown tail flickering in length, drawn additively with
  its top centre on each engine mount); `rook-pod_0` … `rook-pod_3` (16×16, the eject pod: a
  slate capsule with a yellow nose band and a canopy, tumbling slowly clockwise in 45° steps with
  its red beacon lit in the first frame only, so the 4-frame loop blinks once; normal blending).
  His engine mounts per banking frame are in `assets/pivots/rook.json` (`points.engine-left` and
  `points.engine-right`, five `[x, y]` each, px from the sprite's top left, y down, as
  `pivots/ship.json`): the nozzles' rear ends, rolled with the hull.
- **Placeholders** (the game's fallbacks while a sprite is missing): the chosen round-09 Ember
  frames (40×40) cut from [rook-craft-r09-a.png](concept/rook-craft-r09-a.png) by the pipeline's
  placeholder import as `rook_0` … `rook_4` (skipped now that the production frames carry their
  `Source` chunk); a small warm glow per engine at the concept's mounts; a drawn capsule with a
  blinking beacon for the pod.
- **Barks** queue on the radio as described above, with their voice files (27 lines, source
  `wingmen barks <trigger>` in the voice list, rendered by `tools/art/voice.py`).
- The HUD's escort box and the hangar's escort UI are in [HUD](../../ui/hud/README.md) and
  [hangar](../../ui/hangar/README.md#escort-from-level-08).

### Heavy drone "Warden" (escort alternative, L22)

| Property | Value |
|---|---|
| Ship | Armour 120, no shield, top speed 200 px/s, hitbox 14×14 px |
| Formation | Wing slot (56, +20), no automatic formation changes |
| Weapon | Slow cannon straight ahead: 1 shell/s, 14 damage (14 DPS), not upgradable |
| Draws fire | While within 150 px of the player, 30 % of enemy aimed shots that would target the player target the Warden instead |
| Destroyed | Out for the rest of the level; rebuilt for the next level, repairs 8 cr per point |
| Radio | None (a drone) |

### Drones

| Name | Slot | Summary | Draw | Price | Unlock | Design |
|---|---|---|---|---|---|---|
| Light drone | wing mount | Orbits the ship at 36 px, fires a small forward gun, blocks bullets (breaks after 8 hits, rebuilds in 6 s). Sold as the Light Drone Bay in [weapons](../weapons/README.md) | 2 → 3 | 2 000 | L15 | idea |
| Rear-guard drone | wing mount | Trails behind, fires backwards (`rear`) | 2 → 3 | 2 400 | L20 | idea |
| Heavy drone "Warden" | escort | Tough (armour 120), slow-firing cannon, draws enemy fire | 0 | 6 000 | L22 | idea |
| Hunter drone | escort | Leaves formation to chase homing targets, fragile | 0 | 7 500 | L29 | idea |

Unlock `Lnn` = in the shop from the hangar visit before level *nn*. The Warden arrives before
Rook's absence (L27–L29) so the escort slot never has to stay empty.

## Concept art

Round 02 — see [round 02](../../concept-rounds/round-02/README.md). AI-generator prompts: [concept/prompts.md](concept/prompts.md). Generated by
`tools/concept/ships_r02.py`. Rook flies the variant-C airframe from
[ship](../ship/README.md) concept round 01. The mockups were drawn at 40×40; the production
sprite is 42×42, the size in *Rook* above.

| File | What | Status |
|---|---|---|
| [concept/rook-craft-r02-a.png](concept/rook-craft-r02-a.png) | Rook scheme A "Ember": dark slate hull, orange/yellow accents, warm engines — darker and warmer than the player | chosen |
| [concept/rejected/rook-craft-r02-b.png](concept/rejected/rook-craft-r02-b.png) | Rook scheme B "Jade": pale lime hull, deep green panels, green engines — same brightness as the player, different hue | rejected — A preferred |

Concept [round 09](../../concept-rounds/round-09/README.md) — generator `tools/concept/vfx_r09.py`.

| File | What | Status |
|---|---|---|
| [concept/rook-craft-r09-a.png](concept/rook-craft-r09-a.png) | Rook (Ember) 5 banking frames at 4×, 1× strip beside the Stormhawk with greyscale check, in-game view | chosen |

Concept round 28 (M5 part A) — game capture, see [prompts](concept/prompts.md#rook-ingame-capture-r28-a).

| File | What | Status |
|---|---|---|
| [concept/rook-ingame-capture-r28-a.png](concept/rook-ingame-capture-r28-a.png) | Game capture, Level 02 with `--escort rook:missiles:3,side=right`: Rook (placeholder Ember frames) in the Wide formation with his warm engine glow and shadow, his "Flank! Watch the edges, Lancer!" bark on the radio, the HUD's escort box | chosen |
| [concept/rook-final-r28-a.png](concept/rook-final-r28-a.png) | Production sprites (`tools/art/rook.py`): the five banking frames, with both flames at their pivots, the flame loop, the eject pod's tumble, 1× beside the Stormhawk with a greyscale check | chosen |
| [concept/rook-final-r28-a.gif](concept/rook-final-r28-a.gif) | Loop (`tools/art/rook.py`): Rook in the Wing formation banking with the Stormhawk, flames flickering, then his eject pod tumbling off to the side | chosen |
| [concept/part-a-capture-final-r28-a.png](concept/part-a-capture-final-r28-a.png) | Game capture sheet (M5 part A): Rook in production sprites in Wing and Wide, his muzzle flash and smoke, the eject (Level 06, scratch low armour) with the pod and `EJECTED`, his rear-wave bark, the Hornet, Swivel, Fan Blaster, Tail Gun and Proximity Mines, the Targeting computer's brackets and HP bar, the Salvage scanner's glint, the hangar's escort tab | chosen |
| [concept/part-a-capture-final-r28-a.mp4](concept/part-a-capture-final-r28-a.mp4) | The same capture as video with the game's sound (101 s): formations, the barks' voices, the eject, the Act 2 weapons, the utility modules, the hangar | chosen |
| [concept/rook-eject-capture-r28-b.png](concept/rook-eject-capture-r28-b.png) | Game capture sheet of the eject after the pod fixes (Level 07 medium, scratch data: armour 2, hitbox 40, no dodging): the pod over the explosion, its red beacon glow and smoke trail at 3× zoom, 0.1 s apart, and the full frame with the pod heading for the edge away from the ship, `EJECTED` in the HUD and the eject bark on the radio | chosen |

## Implementation

M5 part A builds Rook and the escort slot; its production art, the barks' voices and the AI as
built were accepted in concept round 28.

- [x] Escort slot unlocked by a story flag: Rook hired when the hangar opens with Level 08 or later next; the save's `escort` (format version 3, see [saves](../../systems/saves/README.md)) (`Campaign.hireWhenDue`, `SaveFormat.migrateFrom2`; tests in `EscortTest`)
- [x] Rook flies only in levels from 08 on (not in Act 1 replays from mission select, not on `--level` below 8) (`Campaign.escortFlies`; `EscortTest.heFliesFromLevel08ButNotInAnAct1Replay`)
- [x] Rook AI as in *Rook's AI*: formations Wing / Wide / Trail with the 0.6 s glide and the mirrored slot at the edges, reaction delay, dodging, targeting priority, firing cone, fire whenever the player fires; deterministic (state hash, replay test) (`vanguard.sim.Wingman`; tests in `WingmanTest`: the formations, the mirrored slot, Trail on rear chains, the 70 % dodge, the body clearance and the 40 px, his fire; the same commands give the same state hash and a run without him keeps its own, the recorded `ReplayTest` run flies without him)
- [x] Collisions and damage: enemy bullets and `air` contacts on his 11×11 hitbox, 20 to the enemy, `tiny`/`small` rammers destroyed (`Sortie`'s wingman hits: bullets, mines and air contacts, a lasting contact hurting once)
- [x] Rook's four guns derived from the player weapon tables (scale on damage, one muzzle, no overdrive), with their 60 % prices and upgrade costs (`SimSpecs.wingman`; `WingmenDataTest`, `EscortTest.hisGunsCostSixtyPercentOfTheirBaseWeaponsAndDrawNoPower`)
- [x] Escort inventory in the hangar: buy, upgrade each gun L1–L5, fit one, sell at the sell-back share, undo in the visit (see [hangar](../../ui/hangar/README.md))
- [x] His side setting (left/right) in the hangar, saved
- [x] Armour kept between levels; the hangar's Rook repair line at the difficulty's repair cost; launch warning below 50 %; grounded at 0
- [x] His kills pay like the player's: bounty, score, chain, objectives, statistics (his shots are the player's shot pool with his mount, so they share the kill path; `WingmanTest.hisKillsPayLikeThePlayersAndChain`)
- [x] Eject: the pod drifting off to the nearer edge unless the ship is in the way (36 px), over the explosion after 0.1 s with its beacon glow and smoke trail, the `medium` explosion, out for the level, never a failure; the eject bark urgent and exempt from the spacing
- [x] Retry with his level-start state (and the armour floor); the boss checkpoint includes him (`WingmanTest.atZeroArmourHeEjectsWithoutFailingTheLevelAndARetryBringsHimBack`, `theBossCheckpointKeepsHisArmourAndARetryFromBossBringsHimBackInIt`, `EscortTest.aRetryRaisesHisArmourToTheFloorButLeavesAGroundedRookHome`)
- [x] Wingman radio barks: the eight triggers with their variants, priority, 8 s spacing and the event-line queue rules; voiced (27 lines, accepted as rendered in concept round 28)
- [x] HUD escort box (see [HUD](../../ui/hud/README.md))
- [x] His craft in the level: banking frames, engine flames, runtime shadow, muzzle flash, hit flash, smoke when low, the eject (see *In the game*); the placeholder Ember frames, glow and drawn pod remain as fallbacks
- [x] Debug option `--escort rook:<gun>:<level>[,side=left|right]` and `--escort none` (`DebugFit.withEscort`, never saved; `LaunchOptionsTest.aDebugEscortFliesRookWithHisGun`, `EscortTest.theEscortDebugOptionFliesHimOnAnyLevelOrKeepsHimHome`, `theEscortDebugOptionIsNeverSavedNorKept`)
- [x] His data in [data.yaml](data.yaml) with its loader and validator (`WingmenData`, `ContentValidator.checkWingmen`; `WingmenDataTest`)
- [ ] The guns and barks tables above rendered from [data.yaml](data.yaml) by `tools/sync_tables.py` (still hand-written, see *Data*)
- [ ] `BalanceTest` prints his DPS; the balance plan buys his guns and repairs from L08; the autopilot and `ActPlaythroughTest` fly with him — **later: M5 part I** (the close-out's `BalanceTest` and `ActPlaythroughTest` for Act 2 with Rook and the Act 2 plan, [roadmap](../../tech/roadmap/README.md#m5-parts); not built in part A)
- [x] Production sprites of Rook's craft (5 banking frames, Ember, 42×42), his engine flame, the eject pod and the engine mounts (`tools/art/rook.py`, approved as final in concept round 28)
- [x] His shots in their base weapons' families: drawn with the base weapons' final shot sprites and muzzle flash at his nose (`WeaponLooks` builds his gun's look by its base weapon's slug; `WingmanLooks.drawMuzzle`)
- [x] The radio event `escort-first-kill`: his first kill per attempt, only while he flies, never after an eject, the flag in the state hash and the boss checkpoint; loader, `SimSpecs` and schema text (M5 part B, D4)
- [x] `{side}` in a radio line: one take per side in the voice line list, the take and subtitle of his side played (M5 part B, D4)
- [ ] Warden heavy drone: formation, cannon, draw-fire rule — **later: Act 4** (the Warden unlocks at L22)
- [ ] Drone behaviours: orbit, trail, block, rebuild — **later: Act 3** (light drone L15, rear-guard drone L20) and **later: Act 5** (hunter drone L29)
- [ ] Rook missing for L27–L29 (only a heavy drone in the slot), back from L30 — **later: Act 4**

## Open questions

- Should Rook ever be killed for good in the story? The current story keeps him alive (missing
  L27–L29 only); see [Rook](../../story/characters/rook/README.md).
- While Rook is missing, the draft allows a heavy drone in the escort slot. The alternative is
  to lock the slot completely for those levels, which makes his absence hit harder.

## Decisions

- 2026-09-30: Draft uses a separate escort slot (pending user confirmation).
- 2026-09-30: Rook's timeline aligned with story and campaign: escort slot at L08, missing
  L27–L29 (heavy drone only), back from L30. Drone unlock levels added.
- 2026-09-30: Converted to the 960×540 baseline (was 640×360).
- 2026-09-30: Rook flies the variant-C (blended manta) airframe from ship concept round 01, in his own colours; it replaces the placeholder "F-9 Kestrel".
- 2026-09-30: Concept round 02: Rook's craft scheme **A "Ember"** (dark slate, orange/yellow accents) chosen; B "Jade" rejected.
- 2026-10-01: The separate escort slot for Rook is confirmed by the user. Rook's craft uses 5 banking frames like the player.
- 2026-10-01: Rook's AI specified (formations, reactions, targeting, guns derived from player weapons, eject/return, radio bark triggers); Warden heavy drone specified.
- 2026-10-01: Concept round 09: Rook's 5 banking frames chosen.
- 2026-10-01: Approved by the user in the [Acts 1–2 design review](../../reviews/acts-1-2/README.md).
- 2026-10-06: M5 part A, user decisions: **D2 = a**, Rook's kills pay like the player's (bounty,
  score, chain, objectives; the Airstrike's precedent), rejected b (no score or chain: two kill
  paths, broken chains) and c (only the player's kills pay: hiring him would cost credits);
  **D3 = b**, a hangar repair line for Rook like the player's, unrepaired armour stays missing and
  the launch warns below 50 %, rejected a (always repaired, billed at the next visit: a hidden
  charge) and c (free repairs: one sink less). It replaces "he returns next level at full armour;
  repairs charged in the hangar after the level", which contradicted the saves, retry and hangar
  documents. **D4 = a**, his guns are owned like the player's weapons (an escort inventory: buy at
  60 %, upgrade each L1–L5, fit one, sell at 60 %, undo in the visit), rejected b (one gun at a
  time: switching punished) and c (one Rook level for every gun: departs from the per-gun
  upgrades).
- 2026-10-06: M5 part A defaults (stated with the decisions): save format 3 with `escort`; Rook
  flies only in levels from 08 on (not in Act 1 replays, not on `--level` below 8); the debug
  option `--escort`; the slot always holds Rook in Act 2; the barks are event lines, 8 s apart, and
  a scripted Rook line within the spacing suppresses the bark; his scripted lines still play after
  he ejects, lines that need him flying do not; Lampreys latch only on the player, gusts push him;
  his shots pass through allies; his DPS is printed but not counted against the reference DPS, and
  the balance plan buys his guns and repairs from L08; Rook's production sprite is 42×42 (the
  concept note's 40×40 is corrected); the drones and the Warden are tagged with their acts.
- 2026-10-06: M5 part A details (main-agent choices, for review with concept round 28): his
  repairs cost the difficulty's repair cost per point like the player's (free / 5 / 10 on easy /
  medium / hard) instead of a flat 10 cr per point, as D3 makes it the player's kind of repair line;
  at 0 armour after an ejection he is grounded until any repair; a retry gives him the player's
  armour floor; Trail wins over Wide, and the mirrored slot at the field's edges; enemies never aim
  at him; no overdrive and no Targeting computer bonus for his guns; a pod gun fires as the pod on
  his side; the eject pod drifts to the nearer side edge at 120 px/s; a new campaign puts him on
  the left (Level 08's "I'm on your left"); the bark variants (27 lines), their expressions, the
  replace-by-priority wait and the variant rotation; the numbers, guns and barks in
  [data.yaml](data.yaml) with a nose `muzzle` for his one gun (written as a proposal first and
  created together with its loader, since the loader rejects a data file it does not know).
- 2026-10-06: The user accepted the main agent's four readings of D3 and the M5 part A gaps: Rook's repairs
  cost the difficulty's repair cost per point; at 0 armour after an ejection he is grounded until any
  repair; a retry gives him the player's 50 % armour floor; a new campaign puts him on the left.
- 2026-10-06: M5 part A, Rook in the game (main-agent choices, for review with concept round 28):
  his craft is drawn like the player's (bank frames, two engine flames, runtime shadow, muzzle
  flash at his nose, white hit flash, smoke below 30 %), with the round-09 Ember frames as
  placeholders and a drawn warm glow and pod until the production sprites exist (names under *In
  the game*). The barks: a bark is dropped within 8 s of a timed Rook line *either side* of it (a
  scripted line due soon, or one still on the radio), its variant counts every bark queued in the
  attempt, the kill streak counts the player's and his kills in a 5 s window and starts over after
  ten, an attempt that starts below 30 % armour does not bark about it, and a boss-checkpoint retry
  starts the counts over like a retry. A rear wave's bark fires 1.5 s before its entry, a sides
  wave's at its entry, from the level script's waves.
- 2026-10-06: User decision: Rook **fires whenever the player fires**, at his gun's rate, target or
  not; his cone and target priority only pick his target (a homing gun's initial lock). Before, he
  fired only with a target in his ±15° cone: a Level 02 run gave him 27 volleys against the
  player's 850 in 90 s. A headless Level 02 run now (medium, starter loadout, the test autopilot
  firing throughout, 90 s) gives his L1 Autocannon 425 volleys against the player's 850 (5 a second
  from the end of the launch; his L3 Missiles 255).
  Test `WingmanTest.heFiresWheneverThePlayerFiresEvenWithoutATargetAtHisGunsRate`.
- 2026-10-06: User decisions: his side is a hangar setting outside the visit's undo (see
  [hangar](../../ui/hangar/README.md#decisions)); and the `--escort` debug option is never saved
  (a failed `--escort` run had autosaved him as hired): it now lives beside the campaign's gear and
  only the flights take it (`Campaign.escortFlight`; test
  `EscortTest.theEscortDebugOptionIsNeverSavedNorKept`).
- 2026-10-06: M5 part A, Rook's production sprites (straight to production from the chosen
  round-09 Ember concept, no a/b round; for review in concept round 28): `tools/art/rook.py`
  renders the round-09 model unchanged at 42×42 through the Stormhawk's camera and banking angles
  (±15° and ±30°, the concept's ±14° and ±28° rounded to the player's so the pair banks alike),
  32 colours; a warm 8×14 additive flame loop (3 frames) under the hull at the nozzles' rear ends
  (`pivots/rook.json`); the eject pod as a 16×16 slate capsule with Rook's yellow nose band and a
  red beacon, tumbling clockwise in four 45° steps with the beacon lit in one frame (a blink per
  loop; the beacon is red so it reads apart from the yellow and orange trim). The pod was drawn
  once, not as the a/b concepts the art-track item named. They replace the placeholder frames cut
  from the concept (`importPlaceholders` skips them by their `Source` chunk); the shared atlas
  stays one 2048² page (73 %). In a `--bench` run (Levels 02 and 07, Xvfb) his frames bank, the
  flames sit on the mounts and the HUD's escort icon shows the new frame; the pod could not be
  seen in the game, since no bench run made him eject.
- 2026-10-06: User decision: Rook **reacts to about 70 %** of the bullets he predicts (`dodge.reacts`
  0.7; before, he reacted to every one), each bullet decided once by his own generator (seeded from
  the sortie's seed, its state in the boss checkpoint), so a stray bullet hits him now and then. The
  capture of round 28 showed three more AI faults, fixed with it: a segment chain's members carried
  no rear entry, so a Coilwyrm from the rear or a loop-back never put him in Trail (a chain now counts
  by the edge it entered from last); his slot ignored the air enemies' bodies, so he flew into them
  (closest −18 px; now the 14 px body clearance, his way included: he no longer crosses a body to
  reach a slot beyond it); and at the edges the 40 px minimum could give way (now pushed out along
  the margin). A bullet he reacts to stays decided while he sidesteps it (deciding it afresh at
  every prediction drew again and would have missed one only 0.3⁴ of the time). Measured headless
  (the test autopilot, the plan's fit, his L1 Autocannon, seeds 1–4), hits and armour left before →
  after: Level 02 medium 0–2 → 2–3 (68–72), hard 3–5 (60–68); Level 06 medium 0–5 → 0–4 (64–80),
  hard 1–7 → 4–9 (44–64); Level 07 medium 3–8 (48–68) → 5–11 (36–60), hard 11–19 (2–36) → 10–20
  (0–40) with an eject in 2 of 4 runs (3 of seeds 1–8); never closer than 40.0 px to the player; no
  body contact (all hits are bullets, closest gap +2.9 px); Trail now on Level 06's rear wave
  (134 s) in every run (the two loop-back Coilwyrms died before looping back; the loop-back is
  tested). The share hardly sets his wear: reacting to every bullet, Level 07 hard still ejects in 3
  of 8 runs, as the 0.25 s delay inside the 0.4 s look-ahead leaves him 0.05–0.15 s to clear one; a
  0.6 s look-ahead (with the 70 %) measured no eject in those 8 runs (armour 4–50) and about the same
  medium wear. `reacts` and the clearance stay as they are; the look-ahead is for the user. Tests in
  `WingmanTest`.
- 2026-10-06: The eject, after the round-28 capture (the pod drifted to the nearer edge and passed
  under the player's ship; the eject bark fell under the 8 s spacing like any bark): the pod
  takes the **nearer side edge unless the ship is in the way** (on that side and within **36 px**
  of the pod's line; then the other edge; `Wingman.podSide`, tests in `WingmanPodTest`), is drawn
  **over the explosion after 0.1 s** with a red beacon **glow** and a light smoke **trail**
  (`WingmanLooks.drawOver`); and the **eject bark is urgent**: exempt from the spacing, the scripted
  Rook lines and going stale, it cuts his waiting or playing bark and plays at once (`Barks`,
  `RadioQueue.cancel`; tests in `BarksTest`). Checked in the game on a private Xvfb
  ([rook-eject-capture-r28-b.png](concept/rook-eject-capture-r28-b.png), Level 07 medium with
  scratch data so he ejects: armour 2, hitbox 40, no dodging): the pod clears the explosion's edge
  within 0.1–0.2 s, its beacon glow blinks red and the grey puffs trace its path to the right edge
  away from the ship; "I'm out, I'm out! She's all yours, Lancer!" opens on the radio at once. The
  16×16 pod is small at 1× but reads by its trail and blink, so it was not re-rendered larger.
- 2026-10-06: Look-ahead raised from 0.4 to 0.6 s (user): with 0.4 s he ejected in 3 of 8 hard Level 07
  runs even when reacting to every bullet (the 0.25 s delay left 0.05–0.15 s to move); at 0.6 s the
  agent's 8 hard runs had no ejects (armour 4–50 left) and medium wear stayed about the same.
- 2026-10-06: Concept round 28 closed (user: everything accepted). Rook's production sprites
  (`tools/art/rook.py`: the five banking frames at ±15 / ±30°, the 3-frame flame loop, the 16×16
  eject pod drawn once, not as a/b) approved as **final**, so with his shots in the base weapons'
  final sprites every asset he uses is final: `art: final` (the drones and the Warden are `idea`
  rows with no art yet, for their acts). The captures accepted, the pod after its fixes included.
  The AI approved as built, and the round's question answered: the **flank threat** (the Wide
  formation and target priority 2) is **plain distance**, an enemy within 160 px of his centre
  (a circle, so an enemy just ahead counts too), not "closing from the sides" (the Behaviour row
  reworded; the data's `flank_distance` comment already said so). His 27 barks accepted as
  rendered, the four pinned takes included. The part A decisions checked as carried out. Ticked
  after checking the code and tests: the hire and save field, where he flies, the AI, collisions,
  the guns, his kills, retry and checkpoint, `--escort`, the `WingmenData` loader and validator,
  and his shots in the base weapons' families (the item's "later: art track" no longer applies:
  `WeaponLooks` draws them with the base weapons' final sprites). Still open, so the document stays
  `in-progress`: rendering the guns and barks tables from the data (split off the loader item, no
  part named yet), and the balance item (`BalanceTest` printing his DPS, the balance plan buying his
  guns and repairs from L08, the autopilot and `ActPlaythroughTest` flying with him), not built in
  part A and tagged **later: M5 part I**, whose close-out runs those tests for Act 2 with Rook.
- 2026-10-06: M5 part B, user decision **D4 = a**: Level 08's two Rook lines get their mechanics,
  a radio event **`escort-first-kill`** (his first kill per attempt while he flies, never after an
  eject; once per attempt, the flag in the state hash and the boss checkpoint) and **`{side}`** in
  a radio line (one voiced take per side, played by his side setting). Rejected: (b) no new
  mechanics, the first-kill line on the player's first kill and a neutral "I'm on your wing" (a
  weaker beat), and (c) only `{side}`. Our readings: the event fires on a kill by his own shot or blast
  only; a cue on it counts as a scripted Rook line
  for the barks' spacing; in a level he does not fly in it never plays.
- 2026-10-06: M5 part B built the two mechanics of D4: `escort-first-kill` (the simulation's kill
  by his mount's shot or blast, a set piece's vital part included, while he flies; the flag in the
  state hash and the boss checkpoint; the level screen counts it as a scripted Rook line for the
  barks, withdrawing a bark queued with the same kill) and `{side}` (the voice line list has both
  takes; the level screen shows and plays the one of his side setting, left when he does not fly).
- 2026-10-07: Level 08's capture (round 30) showed Rook beneath the player from t≈141 to the level's
  end, not in a formation slot. Not a stale rear edge (he never was in Trail; no unit kept a rear or
  sides entry): back from the left wall with the player low on the screen (y≈69), his way from the
  mirrored slot to his own passed beneath the player, where the 40 px minimum distance reaches below
  the bottom margin, so the push-out held him in that corner for good. When the side his straight way
  passes the player on has no room inside the margins at 40 px and the other side has, he now steers
  round the player that other way (8 px outside the 40 px, a point 45° ahead on the circle): over
  the player when the way beneath is closed. Otherwise his flight is unchanged (PacingTest's Level
  08 runs with Rook show the same pauses as before). Replayed headless with the
  capture's flight plan, he is back in his slot within a second. Test:
  `WingmanTest.heGoesRoundOverThePlayerToHisOwnSlotWhenTheWayBeneathIsClosed` (fails without the
  detour).
- 2026-10-07: M5 part C (user decisions D7 = a and defaults): Rook skips hardened targets
  (Level 09's Hive Nodes) unless his gun is anti-ground (the Mortar); the launch warning for a
  `required` anti-ground trait counts his Mortar while he is not grounded. His scripted (timed)
  radio lines carry `requires: escort`, so they stay silent while he is grounded.
- 2026-10-07: Rook aims his lobbed gun (user, option a of the Level 09 balance issue): the Mortar
  always landed 200 px ahead of him, so flying in formation beside the player his shells fell about
  170 px past the Hive Node the player was bombing and added next to nothing. Now, with a ground
  target (his pick; hardened ones only with this anti-ground gun) within the lob's 200 px, his
  shells land on it at any distance, their flight time (0.6 s at 200 px) and drawn arc scaled to the
  distance; his pick for the Mortar looks anywhere ahead of him within those 200 px instead of the
  30° cone, which a target beside the player never enters within range. Without a target the
  shells fly as before. The player's own Hammer Mortar keeps its fixed range; his formation, dodge
  and look-ahead are unchanged. Test: `WingmanTest.heLobsHisMortarOntoTheGroundTargetThePlayerIsOver`.
- 2026-10-07: [Concept round 31](../../concept-rounds/round-31/README.md) (user, 2026-10-07): the
  build choice (b) **confirmed**: with a lobbed gun Rook picks ground targets anywhere ahead of him
  within 200 px, not only in his 30° cone (beyond the brief "aim the lobbed gun", kept as built).
