---
title: Wingmen and drones
design: approved
implementation: in-progress
art: chosen
depends-on: [../weapons, ../../story, ../../systems/saves, ../../ui/hangar, ../../ui/hud]
updated: 2026-10-06
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
| Behaviour | Keeps formation, shoots what the player shoots, dodges bullets with a delay, targets enemies closing from the sides |
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
| Movement | Top speed 250 px/s, full speed in 0.2 s; never closer than 40 px to the player; keeps the 12 px play-field margin |
| Collision with an `air` enemy | Rook takes the enemy's full contact damage and deals 20 to it; a `tiny` or `small` enemy is destroyed by the impact, as when it rams the player |

**Formations** — he switches automatically and glides to the new slot over 0.6 s.

| Formation | Offset | When |
|---|---|---|
| Wing (default) | (64, +28) | No other condition |
| Wide | (120, +10) | A `sides` wave is active, or an enemy is within 160 px of him horizontally |
| Trail | (40, +90) | A `rear` wave is active: pursuers overtake him first and enter his firing cone sooner |

A wave is active from its first unit's entry until its last unit has died or left the screen.
When both Trail and Wide apply, **Trail** wins. When the slot lies outside the play field (the
player flies close to the edge on Rook's side, or near the bottom edge in Trail), he takes the
**mirrored slot** on the other side, and returns to his own side once it has been inside the
field for 1 s. Formation changes and side swaps start after his reaction delay.

**Reactions**

| Property | Value |
|---|---|
| Reaction delay | 0.25 s for target switches, formation changes and dodges |
| Dodging | Every 0.1 s he predicts enemy bullets 0.4 s ahead; if one would pass within 14 px, he sidesteps up to 48 px at right angles to it, then returns to his slot. Because of the delay he avoids about 70 % of single bullets and fewer in dense patterns |
| Firing cone | 30° ahead of his ship (15° either side of straight up), range 360 px: where he picks his target. His craft never turns (banking frames only), so he fires up the screen |
| Fire | Fires whenever the player is firing, at his gun's rate, with a target in his cone or without one; he never fires while the player does not. The target only decides what he aims at: a homing gun's shots start locked onto it (without one they find their own) |
| Target priority | (1) an enemy the player damaged in the last 1.0 s; (2) an enemy within 160 px of him horizontally (flank threat); (3) the nearest enemy — all within his cone. Layers follow his gun's traits (Missiles can hit `high-air`; Mortar only `ground`) |

Enemies never aim at Rook: aimed attacks and the target-the-objective hook pick the player or the
objective as before, and Rook is hit by what crosses his path. He collects no pickups and is not
pushed by the scroll. The AI uses no randomness, so a sortie with him stays deterministic (state
hash, replays); runs without him keep their hashes.

**Rook's guns** — each uses a player weapon's per-level table, scaled; prices are 60 % of the
player weapon's base price, upgrades follow the [weapons](../weapons/README.md#common-rules)
formula on 60 % of the player weapon's upgrade base.

| Gun | Id | Based on | Scale | DPS L1 → L5 | Price | Upgrades L2 / L3 / L4 / L5 | Available |
|---|---|---|---|---|---|---|---|
| Autocannon | `autocannon` | [Autocannon Pod](../weapons/autocannon-pod/README.md) | × 1.5 | 12 → 37 | free (fitted when he joins) | 150 / 300 / 600 / 1 200 | L08 |
| Scatter | `scatter` | [Scatter Vulcan](../weapons/scatter-vulcan/README.md) | × 0.6 (volley) | 11 → 39 | 720 | 360 / 720 / 1 440 / 2 880 | L08 |
| Missiles | `missiles` | [Micro-missile Pod](../weapons/micro-missile-pod/README.md) | × 1.5 | 12 → 42 | 480 | 240 / 480 / 960 / 1 920 | L08 |
| Mortar | `mortar` | [Hammer Mortar](../weapons/hammer-mortar/README.md) | × 0.6 | 15 → 54 | 900 | 450 / 900 / 1 800 / 3 600 | L08 |

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

- At 0 armour he **ejects**: the pod pops out and drifts off-screen towards the nearer side edge
  (120 px/s) while the ship explodes (`medium` explosion). He is out for the rest of the level.
  This never fails the mission and costs no score.
- His scripted radio lines still play after he ejects (he is on the radio from his pod). A line
  that needs him flying (a level's "Rook's first kill") does not fire.
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

### Radio barks

Rook's barks are **event lines** under the [radio queue](../../ui/hud/README.md#left-panel-mission)
rules: they wait for a gap, are dropped as stale after 6 s, never push a timed line, and are voiced
like every Act 1 line ([voice](../../audio/voice/README.md)).

- **Spacing:** at least **8 s** from the start of any Rook line (a bark or a scripted line) to the
  next bark. A bark that fires sooner is dropped, and so is one that fires within 8 s before a
  timed Rook line is due: the scripted line says it.
- **Priority:** only one bark waits at a time; a bark of a higher priority (lower number) replaces
  a waiting one, a lower one is dropped.
- **Variants:** the *n*-th bark of a trigger in an attempt (from 0) plays variant
  (level number + *n*) mod the trigger's count, so levels open on different lines and a retry
  replays the same ones.
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
  the `medium` explosion and the pod drifting off to the nearer side edge.
- **Production sprites** (art track, concept round 28; `tools/art/rook.py`), in `assets/sprites/`:
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
| [concept/rook-ingame-capture-r28-a.png](concept/rook-ingame-capture-r28-a.png) | Game capture, Level 02 with `--escort rook:missiles:3,side=right`: Rook (placeholder Ember frames) in the Wide formation with his warm engine glow and shadow, his "Flank! Watch the edges, Lancer!" bark on the radio, the HUD's escort box | proposed |
| [concept/rook-final-r28-a.png](concept/rook-final-r28-a.png) | Production sprites (`tools/art/rook.py`): the five banking frames, with both flames at their pivots, the flame loop, the eject pod's tumble, 1× beside the Stormhawk with a greyscale check | proposed |
| [concept/rook-final-r28-a.gif](concept/rook-final-r28-a.gif) | Loop (`tools/art/rook.py`): Rook in the Wing formation banking with the Stormhawk, flames flickering, then his eject pod tumbling off to the side | proposed |
| [concept/part-a-capture-final-r28-a.png](concept/part-a-capture-final-r28-a.png) | Game capture sheet (M5 part A): Rook in production sprites in Wing and Wide, his muzzle flash and smoke, the eject (Level 06, scratch low armour) with the pod and `EJECTED`, his rear-wave bark, the Hornet, Swivel, Fan Blaster, Tail Gun and Proximity Mines, the Targeting computer's brackets and HP bar, the Salvage scanner's glint, the hangar's escort tab | proposed |
| [concept/part-a-capture-final-r28-a.mp4](concept/part-a-capture-final-r28-a.mp4) | The same capture as video with the game's sound (101 s): formations, the barks' voices, the eject, the Act 2 weapons, the utility modules, the hangar | proposed |

## Implementation

M5 part A builds Rook and the escort slot; production art and the barks' voices are proposed in
concept round 28.

- [ ] Escort slot unlocked by a story flag: Rook hired when the hangar opens with Level 08 or later next; the save's `escort` (format version 3, see [saves](../../systems/saves/README.md))
- [ ] Rook flies only in levels from 08 on (not in Act 1 replays from mission select, not on `--level` below 8)
- [ ] Rook AI as in *Rook's AI*: formations Wing / Wide / Trail with the 0.6 s glide and the mirrored slot at the edges, reaction delay, dodging, targeting priority, firing cone, fire whenever the player fires; deterministic (state hash, replay test)
- [ ] Collisions and damage: enemy bullets and `air` contacts on his 11×11 hitbox, 20 to the enemy, `tiny`/`small` rammers destroyed
- [ ] Rook's four guns derived from the player weapon tables (scale on damage, one muzzle, no overdrive), with their 60 % prices and upgrade costs
- [x] Escort inventory in the hangar: buy, upgrade each gun L1–L5, fit one, sell at the sell-back share, undo in the visit (see [hangar](../../ui/hangar/README.md))
- [x] His side setting (left/right) in the hangar, saved
- [x] Armour kept between levels; the hangar's Rook repair line at the difficulty's repair cost; launch warning below 50 %; grounded at 0
- [ ] His kills pay like the player's: bounty, score, chain, objectives, statistics
- [x] Eject: the pod drifting off to the nearer edge, the `medium` explosion, out for the level, never a failure
- [ ] Retry with his level-start state (and the armour floor); the boss checkpoint includes him
- [x] Wingman radio barks: the eight triggers with their variants, priority, 8 s spacing and the event-line queue rules; voiced (the game plays each line's voice file once it is rendered, concept round 28)
- [x] HUD escort box (see [HUD](../../ui/hud/README.md))
- [x] His craft in the level: banking frames, engine flames, runtime shadow, muzzle flash, hit flash, smoke when low, the eject (see *In the game*); the placeholder Ember frames, glow and drawn pod remain as fallbacks
- [ ] Debug option `--escort rook:<gun>:<level>[,side=left|right]` and `--escort none`
- [ ] His data in [data.yaml](data.yaml) with its loader and validator (`WingmenData`); the guns and barks tables rendered from it by `tools/sync_tables.py`
- [ ] `BalanceTest` prints his DPS; the balance plan buys his guns and repairs from L08; the autopilot and `ActPlaythroughTest` fly with him
- [x] Production sprites of Rook's craft (5 banking frames, Ember, 42×42), his engine flame, the eject pod and the engine mounts (`tools/art/rook.py`, review files proposed in concept round 28)
- [ ] His shots in their base weapons' families — **later: art track**
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
