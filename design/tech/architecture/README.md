---
title: Architecture
design: approved
implementation: done
art: n/a
depends-on: [.., ../../player, ../../enemies, ../../campaign]
updated: 2026-10-07
---

# Architecture

## Summary

How the game code is organised: a Gradle multi-project build at the repository root with a
pure-Java simulation, a content loader, the libGDX presentation layer, the desktop launcher and
an asset pipeline. Game numbers live in data files next to the design documents that describe
them, and the documents' tables are rendered from those files, so the design tree stays the
single source of truth. Everything is tested headless; CI builds and tests on Linux, Windows
and macOS.

## Design

### Repository layout

```
build.gradle.kts, settings.gradle.kts, gradlew   Gradle build at the root (wrapper, version catalog)
buildSrc/        shared Java conventions (Java 21, compiler flags, tests, formatting)
sim/             pure-Java simulation — no libGDX
content/         data model (records) and the loader for the data files — no libGDX
game/            libGDX presentation: screens, rendering, audio, input, UI kit
desktop/         LWJGL3 launcher, settings file, Construo packaging
pipeline/        build-time asset tools: angle sets, atlas packing, audio conversion
assets/          production source art and audio (Git LFS); packed into build output
design/          the design tree, including the data files (see below)
tools/           Python concept-art generators and doc tools (unchanged)
```

Dependencies only point downwards: `desktop → game → content → sim`; `pipeline` is used by the
build only. Gradle enforces it: `sim` and `content` cannot see libGDX. Root package `vanguard`
(`vanguard.sim`, `vanguard.content`, `vanguard.game`, `vanguard.desktop`, `vanguard.pipeline`).

### What carries over from the spike

| Spike part | In the game |
|---|---|
| `buildSrc` conventions, version catalog, wrapper | as is, moved to the root |
| `FixedStepClock`, `SplitMix64`, `Trig`, `Pool`, `StateHash`, `InputRecording` | `sim`, as the foundation |
| `PixelScreen`, `DisplayModes` | `game`, extended with sharp-bilinear and settings |
| `MusicStreamer`, `LoopingStream`, `VorbisFile`, `SfxBank` | `game/audio` |
| `PlayerInput` | rewritten as input actions with remapping ([controls](../../ui/controls/README.md)) |
| `AngleSetGenerator` | `pipeline`, extended with symmetry steps ([art direction](../../art-direction/README.md)) |
| Benchmark scenes, `World`, `Autopilot`, procedural art | not carried over; the benchmark mode returns as a `--bench` option |
| CI workflow, Construo setup | as is, extended with release builds |

The spike is never merged; it is kept for reference as the tag `spike-libgdx`.

### Simulation (`sim`)

- **Fixed 60 Hz step**; the renderer interpolates between the last two states.
- **Deterministic**: seeded `SplitMix64` per level and difficulty, table-based trigonometry
  filled from `StrictMath`, no wall-clock time, no `HashMap` iteration order in logic.
- **No allocation per step**: entities, bullets and effects live in pools; a unit test measures
  allocation per step, as in the spike.
- Owns the rules: movement and formations, layers and layer hit rules, weapons and projectiles,
  collisions, shield/armour, pickups, scoring, credits, objectives, the level script runner.
- Emits **events** (shot fired, hit, explosion, radio cue, objective changed) that the
  presentation turns into sound, effects and HUD changes; `sim` never calls the presentation.
- The campaign state (credits, loadout, unlocks, difficulty) is plain Java and serialisable for
  save slots ([saves](../../systems/saves/README.md)).

### Content: data files next to the documents

The documents describe every number in tables; the code needs them as data. To keep one source,
the numbers live in **YAML data files inside the part's directory** (`data.yaml` next to the
README), and the tables in the README are **rendered from the data**:

| Part | Data file | Rendered into |
|---|---|---|
| Enemy | `design/enemies/<category>/<slug>/data.yaml` (stat block) | its *Stat block* table |
| Enemy basis | `design/enemies/data.yaml` (reference DPS, damage to the player, formation vocabulary) | the *Balancing basis* and *Formation vocabulary* tables |
| Weapon | `design/player/weapons/<slug>/data.yaml`; shared rules in `design/player/weapons/data.yaml` | its property and *Per level* tables |
| Ship and core parts | `design/player/{ship,shields,armor,generator,systems,specials}/data.yaml` | their tables (the specials table is still hand-written) |
| Player-wide | `design/player/data.yaml` (shop availability, pickups) | the *In-level pickups* table |
| Level | `design/campaign/<act>/<level>/data.yaml` (sections, scroll, waves, ground targets, secrets, cues, objectives, music, difficulty changes, backdrop) | the *Threat profile*, *Layout*, *Backdrop*, *Waves*, *Ground targets*, *Radio chatter* and *Credit budget* tables |
| Act | `design/campaign/<act>/data.yaml` (the act's levels, title card, act briefing and act-end outro) | the act's *Act intro and outro* quotes (the outro's still hand-kept, planned `act-outro` table) |
| Allies | `design/allies/data.yaml` (one entry per ally) | the allies' spec tables (still hand-written) |
| Wingmen | planned (M5 part A): `design/player/wingmen/data.yaml` (Rook, his guns, his barks) | the guns and barks tables (still hand-written) |
| Economy, scoring, difficulty, retry | `design/systems/<part>/data.yaml` | the scoring bonus and grade tables and the difficulty levers (the economy's tables are still hand-written; retry has no table) |

- **Marked tables.** A generated table sits between `<!-- data: NAME -->` and `<!-- /data -->`;
  NAME picks a renderer in `tools/sync_tables.py`, which reads the `data.yaml` in the README's
  own directory (and values it derives from other files, such as a level's bounties).
  `python3 tools/sync_tables.py` rewrites the marked tables, `check_docs.py` fails when one
  differs from its data and `--fix` re-renders it. Everything outside the markers is hand-written.
- **README wording** that a table needs but the game does not (descriptions, purposes, notes)
  sits under a `notes` key, in the file or in a row. A note may contain `{field}` placeholders
  (Python format fields; `{a.b}` reaches into a nested value, a `[min, max]` pair prints as
  `min–max`), so a number inside a sentence still has one source. The game ignores `notes`.
- **The expected purchases** of a typical player per hangar visit are not a rule: they live in
  `design/player/balance-plan.yaml`, read by the content tests `BalanceTest` and
  `ActPlaythroughTest` and printed by `tools/balance.py`.
- **Loading.** `content` reads the files with Jackson 3 (YAML) into Java records, one per file
  (`ShipData`, `EnemyData`, `LevelData`, …), gathered in `Content` by `ContentLoader`. Every
  record component is required unless its type is `Optional`; unknown keys are rejected; ranges
  are checked in the records' compact constructors; `ContentValidator` checks across files
  (a wave's enemy and formation exist, an attack's bullet class exists, availability keys
  resolve, starters come first, times and sections fit the level). All problems are collected
  and reported together as `design/<file>:<line>: <field path>: <problem>`.
- **Run time.** The build copies `design/**/data.yaml` with their paths into the `content`
  resources under `design/`, plus an index `design/data-files.txt` (task `:content:designData`);
  the game calls `ContentLoader.fromClasspath()` once at start. The data files are therefore
  part of the jar and the bundles; editing one needs a rebuild, no asset step.
- **Simulation specs.** `sim` defines the records it runs on (`ShipSpec`, `PulseCannon`,
  `ShieldModel`, `Plating`, `Loadout`, `EnemySpec`, `LevelScript`, `Rules`) and knows nothing
  about data files; `content`'s `SimSpecs` builds them from the loaded records at a difficulty,
  with the difficulty levers applied. The dependency stays
  `content → sim`, so the simulation keeps no YAML, Jackson or file access.
- Balance checks (budget per level, DPS against the reference, time to kill, bounty) are
  JUnit tests over the loaded content (`BalanceTest`, with the chained Act 1 playthrough
  `ActPlaythroughTest`); `tools/balance.py` prints the same plan and checks as the balancing sheet.

### Data file schemas

Keys are snake_case; times in seconds, distances and sizes in px, speeds in px/s, power in MW,
prices in credits. Every file starts with a comment that names its README and links here. Sizes
are `[width, height]`, ranges `[min, max]`, points `[x, y]`. `available` is a key of
`design/player/data.yaml`'s `availability` (`start`, `act N`) or an exact level (`L22`); the
first entry of a part's model list is the starter (price 0, `start`).

- **Weapon** (`player/weapons/<slug>/data.yaml`): `name`, `slot` (`front`/`rear`/`wing`),
  `traits`, `price` (0 = starter), `upgrade_base` (default: the price), `unlock` (level),
  `draw` (`[L1, L5]`), `sfx`, `vfx`, `hits`, `speed` (px/s or `[start, end]`; none when lobbed
  or dropped), `size`, `range` (px, `screen` or `drop`) or `lifetime` (a homing weapon has both:
  its `range` is the seek radius), the behaviour numbers `converge` (° a pod turns in towards the
  centre line), `fall` (s a dropped bomb falls), `flight` (s a lobbed shell flies), `snap` (px a
  shell's auto-aim reaches), `cone` (° ahead a homing shot picks its target in; a turret's forward
  cone), `accelerate` (s an accelerating shot takes from its `speed` start to its end), `ports` (px
  either side of the muzzle the shots of a volley leave from in turn, left first), `slew` (°/s: a
  turret, which aims its straight shots at the nearest enemy all round), the mines' `drift` (s their
  drop's drift decays over), `arm` (s before they arm) and `trigger` (px an `air` or `low-air` enemy
  sets one off at; mines have `hits: area`, a `lifetime` and per level a `blast` and `max_live`),
  flags `mirrored` (fires the same pattern to the left too; numbers per side), `pod` (numbers per pod) and `seek`
  (homing, lobbed or dropped: every projectile counts as a hit on a single target), then
  `levels` (five) and `overdrive`, each with `pattern`, `rate` (volleys/s), `damage` per
  projectile and optional `pierce`, `blast`, `turn` (°/s), `max_live`. A pattern lists one
  `[x offset, angle]` per projectile; angle 0 = straight up the screen, 90 = right,
  180 = straight back. The damage of ground-only `anti-ground` weapons already includes the ×2
  anti-ground bonus. `notes.layers_hit` is the README's *Layers hit* cell. Shared rules
  (`player/weapons/data.yaml`): `upgrade_cost_factors` (L2–L5), `draw_round`,
  `single_target` (`distance`, `width`).
- **Enemy** (`enemies/<category>/<slug>/data.yaml`): the stat block of the
  [enemies](../../enemies/README.md#stat-block-template) template: `name`, `faction`, `layer`,
  `tier`, `size`, `hitbox`, `parts` (`single` or `multi`: a multi-part unit lists its
  `part_list`, each part a `name`, an `offset` `[dx, dy]` from the centre in its own frame facing
  down (dx right, dy up towards the tail), a `hitbox`, `hp`, `kind` `destroyable`/`vital`/
  `armoured`, `bounty` and the `attack` it fires by name; the parts' HP and bounties add up to the
  unit's), `orientation`, `hp`, `armour`, `speed`, `movement` (one
  entry per pattern: `snake` `spacing`; `swoop` `radius`, `top_speed`; `straight` `speed`;
  `hover` `seconds`, `y`; `orbit` `radius`, `turn_rate`; `strafe` `y`, the height a convoy turns
  across at; `spiral_out` `seconds`, `turns`, `growth`, `drift`, `ricochets`;
  `drift` `speed` (straight down, no intent); `sine` `amplitude`, `period` (a side-to-side offset
  on another pattern); `walk` `speed`, `turn_rate`, `stride` (px of ground per walk cycle), the
  path coming from the wave), `attacks`
  (`pattern` `aimed`/`fan`/`mine`, an optional `name`, `bullet` class, `interval`, `speed`,
  `first_shot_delay` (for parts sharing an attack: the stagger between them), a fan's `count` and
  `spread`, a mine's `mine` with `arm`, `life`, `drift`, `hp`, `ring`, `ring_bullet`, `credits`;
  a fan's `aim` (`target`, the default, `down` or `facing`), an aimed attack's
  `away` (fires only while the player is more than this many ° off its facing), and the pattern
  `spawn` with no `bullet`, `interval` or `speed` but a `spawn` block: `enemy`, `count`, `after`
  (s from entering to the self-burst), `telegraph` (s), `arc` (°), `speed` (the released units')
  and `burst_bounty` (credits for a self-burst, which is not a kill)),
  `formations` (`name`, `size` `[n]` or `[min, max]`),
  `weak_points` (`name`, `multiplier`), `traits`, `bounty`, `first_level`, `difficulty` hooks
  (`easy`/`hard`: `fan_count`, `dive_pause`, `burst` (aimed attacks), `leads_target_in`, a mine's
  `ring` and `mine_bursts`, a `death_burst` with `count`, `speed`, `bullet`;
  a spawner's `spawn_count` and `spawn_after`). `armour` is a text, or a mapping
  with `front_arc` (° each side of the facing from which direct shots glance; dropped bombs,
  lobbed shells and specials ignore it).
  `drops` (`pickup`, `every`: every n-th kill of the enemy in a level drops it).
  Easy/hard HP in the table are derived from the difficulty levers (rounded half to even, at
  least 1), the
  contact and bullet damage from the basis, the score from the bounty.
  For the Polyp Mortar and the Gorgon Frigate (M4 part E; both flown in Level 05): the attack
  patterns `mortar`
  (its `bullet` is the direct hit, `speed` the ring's; a `mortar` block with `marker` (s the
  impact marker shows ahead, the blob's flight), `impact` (px, the direct-hit circle's diameter),
  `ring` and `ring_bullet`; the difficulty hook `ring` applies to it as to a mine), `ring`
  (`count` bullets) and `spiral` (`arms`, `interval` between two bullets of an arm, `turn_rate`,
  `duration` s); on an aimed attack `burst` (shots per volley), `burst_gap` (s between them) and
  `rotate: true` (the parts sharing it take turns, one volley every `interval` among the living
  ones); a part's `multiplier` (its damage multiplier as a weak point); `hover` without `seconds`
  (holds until killed); `chains` (articulated necks: `name`, `from` (the anchor offset on the
  body), `to` (the part at its end, whose `offset` is the chain's rest end), `segments`,
  `hitbox` per `armoured` segment, `lag` (s of follow-through per segment), `bend` (° it may turn
  towards the player)); the difficulty hook `attacks` (changes per attack name, such as
  `burst` or `count`); and a `boss` block: `kind` (`boss`/`mid-boss`; a mid-boss has the short
  bar), `bar_name`, `par` (s from the bar appearing to the kill, the Boss rush bonus), `phases`
  in order, each a `name`, `until` (`parts` and how many of them are `left` alive when it ends),
  the `attacks` it fires by name or an `alternate` list (each attack runs its `interval` or
  `duration`, then hands over), optional `streams` (`enemy`, `count`, `every` s, `interval` s
  between units, `edge`; the first at the settle), `exposes` (parts that take no damage before
  this phase) and `bend` (the chains' bend in this phase). During its entrance movement a boss
  takes no damage. `hover: {y}` may be one height instead of `[min, max]`.
  For the Mantis and the Coilwyrm (M4 part F; flown in Level 06): the attack pattern
  `laser-sweep` (its `bullet` is the beam's class, damage once per sweep; no `speed`; `aim`
  `target` centres the sweep on the ship's bearing when the telegraph starts; a `sweep` block
  with `arc` (°), `duration` (s), `telegraph` (s, the arc shown ahead), `length` and `width` (px), and an optional `origin: [in, down]`
  (px from the unit's centre to where the beam starts, toward the field and down, mirrored on the
  right edge; default the centre));
  `hover` `edge_x` (px from the side edge it entered by) and `exit: back` (it leaves through that
  edge); the movement `path` with `speed` (the head flies the wave's authored path); a
  `segment_chain` block for a chain of segments following the head's path history: `segments`,
  `size` (the first and last segment's sprite, tapering), `hitbox_share`, `spacing` (× the
  segment length), `hp` and `bounty` per segment, `contact` (the segments' and tail's contact
  class), `regrow` (`seconds`, `speed`, `hp`, `bounty`: the rear part's new head after a cut,
  once per chain) and `pop_interval` (s per segment of the chained death); the unit's `hp` and
  `bounty` include the segments; a part's `first_bonus` (credits when it is destroyed before the
  vital part; the tail) and the difficulty hooks `sweep_arc`, `segments` and
  `regrown_fan_count`. A segment chain counts as one enemy (density, kill ratio): the simulation's
  unit is its head (the vital part's HP and bounty), its segments, tail and regrown head are kinds
  of their own (`<slug>-segment`, `-tail`, `-regrown`) that pay their bounty but are not kills; a
  hard `attacks` change may set an attack's `interval` (authored, so the fire-rate lever does not
  apply on top); `hover` `seconds` may be one time instead of `[min, max]`.
  Planned (part G), for the Brood Carrier (flown in Level 07): the stat block's `layer` is the
  layer a boss arrives on (`high-air`: only `homing` shots reach it, drawn at the 1.25 high-air
  scale, no contact); a boss part of `kind: armoured` fires its `attack` but is never damaged, has
  no `hp` or `bounty` and is not in the bar (the head turrets); the pattern `fan` for a boss
  attack; a `spiral` without `duration` runs for its whole phase (beside a `ring` in a
  non-`alternate` phase, each with its own spiral state); the difficulty hook `attacks` gains
  `arms`, and a hook `spawns` sets a window spawn's `count` by its `name`. In the `boss` block:
  `engages_on_arrival: true` (the first phase starts when the boss arrives, its entrance part of
  the fight, rather than when it settles); `death_seconds` (s the chained death runs, the parts
  bursting in `part_list` order, tail to head; parts still alive then burst and pay); `poses`, the
  further part layouts (the arrival pose is the `part_list` offsets with the stat block's
  `hitbox`), each a `name`, the body's `hitbox` and `offsets` (part
  name → `[dx, dy]` in this pose; parts left out keep their `part_list` offset), drawn from their
  own pre-rendered sprites, never rotated. Per phase: `until` may give `seconds` (from when the
  phase engages, after its move) beside or instead of `parts`/`left`: a phase with only `seconds` is
  timed, one with both times out (a **timeout**) when the seconds run out first; `delay` (s after
  the move before it fires and opens its windows; default 1 s in a later phase that
  alternates, 0 otherwise: the frigate's crown opening, generalised); `move`, the invulnerable opening move: `to` `[x, y]`
  (the centre's station, px from the left edge and px below the top edge, like `hover.y`), `layer` (the layer it descends or rises to), `descend` (s), `pose` (the
  pose's name) and `turn` (s through the turn's pre-rendered frames); `windows`: `groups` (lists
  of part names, in the order the windows cycle), `every` (s), `open` (s a group stays open),
  `offset` (s before the first, after the delay), `all: true` (every group with a living part
  opens at once), and `spawns` in turn, one per opening, each a `name`, `enemy`, `count` per
  group (a group with parts destroyed releases its share rounded up, an empty group nothing),
  `speed`, `arc` (°) and `glide` (s); a part in a window group takes damage only while its group
  is open.
  M5 part B, for the [Creeper](../../enemies/ground/creeper/README.md) (flown in Level 08): a
  walker's `fan` with `aim: target` (the default) is aimed at the player, as a flyer's, and with
  `aim: facing` along its facing (the Scuttler; `down` is rejected for a walker); a walker's fan's
  `stagger` (s, positive; only a walker's fan has one): the units of one wave share a volley clock
  that starts as the first of them comes onto the screen, the first volley half an interval later
  and then one every interval, unit *i* (from 0, in entry order) firing *i* × `stagger` after the
  volley's start, a unit off the screen skipping its turn (a wave whose last unit's turn would
  reach the next volley is rejected when the level is built); and the difficulty hook `attacks`
  with an `interval` on any named attack, an ordinary unit's included: authored, so the fire-rate
  lever does not apply on top.
  M5 part C, for the [Hive Node](../../enemies/ground/hive-node/README.md) and the
  [Ravager](../../enemies/ground/ravager/README.md) (flown in Level 09): `armour: hardened` flies for an
  enemy as for a hardened ground object (only `anti-ground` deliveries, the Airstrike and the
  Smart Bomb damage it; other shots and blasts glance, `SHOT_GLANCED`; a homing shot's lock-on and
  Rook's target pick skip it unless the weapon is `anti-ground`); a **periodic `spawn`**: the
  `spawn` block with `every` (s between releases, the first `every` s after the unit's centre
  crosses the top edge) instead of `after` and `burst_bounty` (a periodic spawner never bursts on
  its own), its `telegraph` (s the iris opens ahead of a release), `enemy`, `count`, `arc`,
  `speed` and `shut_within` (px: an opening due while the ship's centre is this close is skipped,
  and so is a release the ship came that close to during the telegraph; the next one waits a whole
  `every`); a unit has at most one `spawn`; the released units count into the level's enemies as they
  are released (as a boss's streams), the density and haul checks count one release per spawner,
  and the hook `spawn_count` sets the count; the attack pattern **`pounce`** (no `bullet` or
  `speed`; its `interval` is the s from landing until it may leap again, by the fire-rate lever
  unless the hook `attacks: {pounce: {interval}}` authors it, as hard's; a `pounce` block: `range` (px, centre to centre,
  that starts a leap while the unit is on the screen), `leap` (s, aimed at the ship's position at
  take-off, no homing), `air` (s in the middle of the leap on the `air` layer: the unit's
  **current layer**, read at every hit, contact and targeting site instead of its stat block's;
  contact of its tier's class with the ship or the escort once per leap; ×1 from air-reaching
  weapons, ground-only blasts miss, mines trigger) and `scale` (the drawn scale at the apex));
  only a walker pounces; and the formation **`pack`** (see *Level*; the planner flies it).
  Basis (`enemies/data.yaml`): `reference_dps` per level, `bullet_damage`, `contact_damage`
  per tier, `formations` (name: description).
- **Level** (`campaign/<act>/<level>/data.yaml`): `scroll_speed`, `launch_seconds`, optional
  `bounty_scale` (default 1: a factor on every bounty paid in the level, after the act factor and
  the difficulty's income, before the one rounding per payout; see the economy),
  `control_prompts`, timed `prompts` (`t`, `action`, `keys`, `seconds`, an optional `skip` layer:
  the prompt leaves once an enemy on it is destroyed; `requires: special`: shown only with a special fitted); `sections` back to back from t = 0 (`name`, `end`, `atmosphere`, optional
  `speed`, the backdrop's `tiles` (tile set ids, at most one per layer), `arena: true` for the
  boss's arena (the level clock halts at its end while the boss lives and jumps to its end at an
  earlier death; at most one, only with a `boss`); scroll distances are derived); `boss` (`enemy`
  with a `boss` script, `t` it arrives, `x` of its centre, `section`); `waves` in time order, one row each (`t`,
  `formation`, `enemy` slug, `count`, `from` `front`/`sides`/`rear`, `edge`
  `left`/`right`/`alternating` (`sides` without an edge enters from both side edges), optional
  `hold`, `warning`, `break_group`, `speed` (px/s instead of the enemy's own), `interval` (s
  between the units of a stream), `at` (a whirl cluster's release point `[x, y]`, y below the top
  edge), and `easy` / `hard` changes (`from`, `edge`, `count`, `break_group`, `warning`), an
optional `skip` list of the difficulties it is left out on (Level 06's hard-only Coilwyrm pair); a
  segment chain's `paths` (one list of `[x, y]` points per unit, y below the top edge, starting
  outside the play field; with one path every second unit flies it mirrored) and optional
  `loop_back` (`after`: s past the path's end, off the screen, until the head re-enters;
  `path`: its points, shifted sideways to start at the head's x; without one straight up from
  the bottom edge; the bottom edge is warned the wave's `warning` ahead, at least 3 s); a walker
  wave's `paths`, one list of
  `[x, y]` points per unit in screen coordinates at the wave's `t` (y below the top edge, points
  may lie outside the play field), which then scroll with the ground; a `pincer` with one path
  mirrors it for every second unit, other walker formations repeat it 1.5 s apart), a mixed wave
  lists `groups` (a `carrier + escorts` wave lists the carrier's group first)
  instead; `set_pieces` (an `enemy` with a `part_list` and its `passes`, each a `name`, `section`,
  `layer`, fixed `heading` (° from straight down, positive to the right) and a `path` of
  `[t, x, y]` waypoints (y below the top edge); a pass on the player's layer adds `descend`
  (`at`, `seconds`), `hold` (s from the descent's start until it rises and leaves through the top
  edge), `leave_speed` and `easy` / `hard` holds; the README's *Waves* row comes from its
  `notes`); `debris` (the chunk kinds by name with their `size` and a large one's contact
  `damage` or a small one's `hp`, the `clearance` from the ship they enter at, `max_large` on
  screen, the `placed` chunks with `t`, `x`, `chunk` and `drift` `[x, y]` px/s, `easy`
  `leave_out_large` (every n-th large chunk) and `hard` `drift_factor`); `ground_targets` (`target`, `section`, `layer`, `size`, `at` (one `[t, x]` per object:
  when it enters at the top edge and its x), a destructible's `count`, `hp`, `bounty`, `drop`, an
  optional `bonus_drop` with it (a special charge drops only with a special fitted), `hardened`, an
  optional `reveals` (its hidden crate drops when it is destroyed), `dark: true` (a trigger that
  takes hits only while the headlight or a flare lights it), an optional `sprite` (its sprite
  set: `<sprite>_0..2` intact, damaged, wrecked and `<sprite>-break_<n>`; Level 01's
  `cargo-container`, without a wreck, if left out), or
  a trigger's `hits`, `reveals` (triggers revealing the same secret reveal it together, when the
  last of them is spent) and optional `sprite` (its own frames instead of the beacon or the level's
  trigger light: `<sprite>_0..2` intact, hit, spent, or `_0..1` intact, spent; an optional
  `<sprite>-glow` drawn after the darkness's light pass until it is spent (one frame: a still
  light; `<sprite>-glow_0..n`: a loop at 8 fps, `GroundGlow.FRAMES_PER_SECOND`, irregular flicker
  steps baked into the frames; with a hit frame an even loop splits in halves, the first until the
  first hit, the second after it: Level 08's billboard), and `<sprite>-glint_<n>`
  drawn after it only while it is lit; Level 06's survey cache and terminal; a look whose spent
  frame is a wreck, `TriggerBreak`'s billboard, breaks like a destructible on the hit that spends
  it, `SimEvents.Type.TRIGGER_SPENT`: the small explosion, blast and crumble)); `secrets` (`name`, hidden `crate` credits, `radio` line; or a
  `data_core` with the shop item it `unlocks` and `crate: 0`: it drops the data core pickup, its
  line plays when it is collected, and a won level records the core and its unlock); a dark
  level's `darkness` (`headlight`: `from` s, `length` px, an `easy` length, `angle` °; `flare`:
  `seconds` (`easy_seconds`) a flare burns, its pool's `radius` px and `drift` px/s down the
  screen; `flares`: `t`, `x`, `y` (px below the top edge) and an optional `skip` list of
  difficulties; `lights`: static ground pools with `t` (entering at the top edge), `x` and
  `radius`; `ambient`: the ground's brightness outside light, 0 to 1); placed
  `pickups` (`pickup`, `dropped_by` wave and unit `first`/`second`/`last`, or a ground-target
  `group` and unit `last`: dropped where the group's last unit dies when it is cleared); `radio` cues (trigger
  `t` or `event` `first-kill` or `enemy-escaped` (with `enemy`; a set piece escapes at the end of
  its last pass) / `group-cleared` / `group-lost` (with `group`) / `first-group-lost` /
  `secondary-objective` / `level-end` / `first-ally-hit` / `first-ally-lost` (a convoy's first hit
  and first loss; `{ally}` in the line becomes the unit's number word, "Three") / (M5 part B)
  `escort-first-kill` (no `enemy`: the first kill whose killing shot or blast is the
  escort's, `wingmanMount`, while he flies; never after he ejects; once per attempt, the flag in
  the state hash and the boss checkpoint; without an escort flying it never plays) / `boss-phase`
  (with `phase`, the phase's name) / `boss-destroyed` (the level's boss; planned (part G): the
  loader gave such a cue an empty subject while the simulation cues the boss's slug, so it never
  played, Level 05's "Frigate down" included; part G fixes it) / `mission-failed`
  (not played: the line and speaker on the mission failed screen after a failed primary objective;
  at most one; only with an `escort` or `destroy-targets` primary; `{group}` in it becomes the
  lost group's name); a `level-end` cue's `allies` `[min, max]` (the convoy units home, so each outcome
  has its line), and `requires: special` (only with a special fitted; on any cue); planned (part
  G): `requires: homing` (a weapon with homing delivery fitted) and `requires_not` (`special` or
  `homing`: plays only without it), and a `boss-phase` cue's `timeout: true` (it plays only when
  the phase before ended on its timeout with parts it waited for alive);
  `speaker`, `line` (M5 part B: `{side}` in it becomes `left` or `right`, the escort's side: the
  save's, or `--escort`'s `side=`, left when he does not fly; such a line is voiced once per side
  and the subtitle shows the side's text),
  `distorted`, the portrait's optional `expression` (`neutral`, `grim`, `fierce`; neutral if not
  given; a secret's `radio` line takes it too), `shout: true` (the voice shouts the line, with the
  speaker table's `shout` row; separate from how the line queues), and `easy` / `hard` changes
  giving another `line`,
  as when a wave enters elsewhere on that difficulty); `objectives` (`primary`, `secondary` `kill_ratio`, `groups`, `escapes` (the enemy none of
  which may leave the screen alive; a spawner's self-burst counts as an escape) or `kill_all`
  (enemies every unit of which must die, met and failed as `escapes`, with the tracker's `label`)
  and `credits`;
  planned (part G): a fifth secondary kind, `parts` (names of the level boss's parts, every one
  of which must be shot off) with `before` (the boss phase whose end, by timeout, fails it) and
  the tracker's `label`;
  `primary` is `reach-end`, `destroy-targets` with its `targets` (the ground-target groups that
  must be cleared; it fails as soon as a unit of one leaves the screen alive, the groups pay no
  credits, and the secondary then cannot use `groups`) or `escort`, which adds an `escort` block: the `ally` slug, the
  column's centre heights `y` (px below the top edge, the leading unit first, at least the ally's
  length apart), `credits` per unit home (through the credit factor, with a debrief row), `enter`
  (`t` of the first unit, `interval` s between units, `speed` px/s up the screen: they roll in from
  the bottom edge to their stations), the target-the-objective `hook` (`mode`, only `nearest` so
  far, and the `enemies` whose aimed attacks go for the convoy) and `easy` / `hard` `hp`; it fails
  when every unit is lost); `road` (`width`, an optional `texture` id: the ribbon's image
  `assets/backdrop/level-NN/<texture>.png`, its rows by arc length from the first point, a flat
  placeholder colour without one; and `points`, one `[t, x]` per road point, `t` when it passes the
  middle of the screen, which may lie before the start; straight between points and straight up
  beyond them; the convoy follows it; the ribbon stays inside the play field and bends no further
  than the ally's headings, ±30° for the crawler); `music`
  (`track`, `start_section`, optional `start_db` (the theme's level through its start section,
  rising to full at the next), `full_section`, optional `stems` (section: `base` or `full`,
  overriding it), `ambience` (the setting's loop by its key in the
  [sfx](../../audio/sfx/README.md#ambience-per-setting) *Ambience* table: `earth-orbit`, `luna`,
  `earth-megacity`), `end_jingle`, optional `boss_sting` (`miniboss-sting`: track 21 over
  a 0.5 s crossfade when the boss arrives, the theme returning after it), optional `ambience_from`
  (the level time from which only the ambience plays: the theme fades out as at a won level) and
  `voice_loop` (`speaker`, `section`, `db`: that speaker's first timed radio line loops at `db`
  on the voice bus while the section plays, silent without a voice file; Level 06's perimeter
  beacon); planned (part G): `boss_warning` (`boss-warning`: track 22 with the klaxon and the
  warning banner when an act boss arrives, the theme crossfading out) and `boss_track`
  (`choir-descends`: track 18, coming in on the downbeat after the warning and fading out at the
  kill, leaving the ambience)); `sleds` (Level 05's
  mass-driver sleds: rail `x`, `width`, the `first` launch, `period`, `until`, `lights` (s of the
  telegraph), `run` (s on screen), contact `damage`, the `clamp` secret whose trigger only takes
  hits while the rail is dark, and `easy` / `hard` `period`); `rocks` (low-gravity debris a
  destroyed ground unit throws: `count` and `speed` `[min, max]`, `life` s, `size`, `hp`, contact
  `damage`, `clearance` px from the ship, none on easy unless `on_easy`); `boss.notes.streams` (the
  boss streams the credit budget counts); `difficulty` (level-wide
  `easy` / `hard` enemy changes such as `burst` and a walker's `speed_factor`, and `extra_pickups` placed like `pickups`);
  planned (part G): a wave `easy` / `hard` change's `hold`; a placed pickup's `skip` list of
  difficulties; `dropped_by` `parts` (names of the level boss's parts) with `unit` `first`/
  `second`/`last`: the n-th of them shot off drops it (parts lost in the death drop nothing);
  `tows` (Level 07's lifeboat: a friendly craft on the air layer towing a secret's crate): `t` it
  enters at the top edge, `x` of its centre, `drift` `[x, y]` px/s (y up, so negative: down the
  screen), the `boat` and `pod` sizes, the pod's `tether` `[dx, dy]` from the boat's centre, the
  `cable` size (its hit box midway between them, the only part shots hit; boat and pod let shots
  and bullets through and never collide), `hits` to cut it and the secret it `reveals` (the pod
  falls free as that secret's crate);
  `threat_profile` (the hangar intel: `setting`, `layers`, `density` 1–5, recommended `traits`,
  `hazards`, `boss`, optional `specials` limits, optional `objective` (the OBJECTIVE field
  shown from sensor L1, "ESCORT 5 CRAWLERS"), `varga`, Dr. Varga's line for each sensor level
  (`none`, `l1`, `l2`, `l3`, all four required), and `notes` with the *Threat profile* rows,
  where `{directions}` is derived and the other `{fields}` come from the profile); `briefing`
  (`pages` of `speaker` and `line` with the optional portrait `expression` and `image`, the name of
  a tactical map or mission image in `assets/ui/briefing/`, and the hangar `teaser`, rendered into
  *Briefing*); `backdrop`
  (presentation only, see below). The credit budget
  table is derived: kills × bounties (× `bounty_scale`), ground targets, crates, the objectives,
  each as a perfect run and as the typical haul (weighted by the economy's `typical_player`),
  against budget(n) of the economy; the attack directions are each entry's share of the enemies.
  M5 part C, for
  [Level 09](../../campaign/act-2-homefront/level-09-arcology-fall/README.md) (user decisions D2,
  D3, D5, D6 and D7 of M5 part C): **hold zones**, `holds` (each: `groups`, the ground-target
  groups of the objectives it waits for, each group in one hold at most; `y`, px below the top edge
  the first unit of those groups reaches to start it; `speed`, px/s it eases to, with `easy` /
  `hard` `speed`, below every section's speed; `ramp`, s of the ease each way, a smoothstep): the
  scroll eases to the hold's speed and back to the section's once every unit of its groups is gone
  (a hold whose groups include the `collapse`'s, once their clearing started it, only when the
  collapse's dust has settled, the impact + its `dust` `seconds` (user, 2026-10-07; Level 09:
  9.0 s after the warning starts), so the scroll stays at the hold's speed through the lean, the
  drop, the blast and the settling dust and what falls and the heap it leaves stay on the screen;
  no event marks the settling, the hold's `HOLD_END` comes in that step);
  there is no timeout (a unit that leaves the screen alive fails a `destroy-targets` primary at
  once, which also ends the hold); a hold whose groups are gone before it starts never starts; one
  hold runs at a time; the hold's state is in the state hash (only in a level with holds or a
  collapse, so the other levels hash as before). Holds are not built for a level with set pieces,
  cranes, sleds, tows or a boss (`content` rejects it). **The level clock** then advances at the
  current scroll speed ÷ the section's speed (D2 = a: 0.2 at 30 of 150), in fixed point (1/65 536
  of a step; outside a hold exactly one step per step, so the existing levels step and replay as
  before), so every time in a level file is **script time**, the scroll distance ÷ the section's
  speed: waves, ground targets, radio `t`, prompts, edge warnings, the sections and their
  atmosphere, the backdrop's placements, the progress bar and the level end wait with the scroll in
  a hold, while units, bullets, the spawners' cycles, pounces and Rook run on the real steps (the
  density and haul checks count script time, the pacing check real time); the simulation gives
  both clocks and the rate (`Sortie.levelSeconds()`/`scriptSeconds()`, `realSeconds()`,
  `scriptRate()`); planned (M5 part C, the game): the radio queue's own timing and the backdrop's
  animation on the real clock. A **`collapse`** block (D5 = a, round 31's look c: `groups`, the groups whose clearing
  starts it, every unit destroyed; `warning`, s the tower leans before it drops; `drop`, s it takes
  to drop straight down, the impact at `warning` + `drop`; `blast` (`seconds`, `from`, `to`): from
  the impact its kill ring grows from `from` to `to` px round the tower's footprint's centre over
  `seconds`, eased 1 − (1 − t)², the dust blast's front; `tower`, the backdrop's tower piece that
  falls, placed exactly once: the band is its footprint's extent along the scroll (the whole width),
  so it lies on the ground under the tower and scrolls with it however early or late the groups are
  cleared; `dust`, the `atmosphere` it raises and the `seconds` it ramps back out over after the
  impact, at least the blast's (a hold over its groups lasts until then); `rubble`, the backdrop piece it leaves, a piece id of the level's backdrop), timed on the
  real steps, with the events `COLLAPSE_WARNING` (the lean starts), `COLLAPSE_FALL` (the drop
  starts), `COLLAPSE_IMPACT` and `COLLAPSE_END` (the blast has rolled out): every ground unit (on the
  `ground` layer at that moment, so a pouncing Ravager in its air window is spared) in the band
  whose centre the ring reaches is destroyed and pays and scores as an Airstrike kill; air units
  and the player are untouched; built (user, 2026-10-07, round 31's look c), replacing the earlier
  sweep from left to right; the game draws the lean, the drop, the cast shadow, the dust and the
  rubble heap (`CollapseLooks`). A wave's optional **`tag`** (a name) and a secondary `escapes` with a
  `tag` (D6 = a): only the units of the waves with that tag count (their number per difficulty
  follows from the waves), met when all of them are destroyed, failed when one leaves the screen
  alive; `content` checks that a wave of the enemy carries the tag. Any secondary may give a
  **`name`** (Level 09's `Hold the bridge`), used where the generated wording would mislead: the
  briefing's bonus line (`BONUS: HOLD THE BRIDGE`) and the README's credit table (`Secondary: hold
  the bridge`) show it; the tracker's `label` stays apart. A **`pack`** walker wave lists
  one path per unit, as many as its largest difficulty's count (`content` checks it), the units
  entering 0.25 s apart. Radio events **`hold-start`** (the level's first hold starts its ease;
  once; only in a level with holds), **`first-pounce`** (the attempt's first pounce takes off; no
  `enemy`; only with an enemy that pounces) and **`collapse`** (the collapse's warning starts; only
  with a collapse), and **`requires: escort`** on any cue (it plays only while an escort flies:
  hired, fitted and not ejected, asked when the cue is due; Rook's scripted lines; also
  `requires_not: escort`). The threat profile's **`required`** (D7 = a: the traits among its
  `traits` the primary cannot be met without), loaded and checked; planned (M5 part C, the game): a
  launch warning at every sensor level, counting every source that damages hardened targets, see
  the [hangar](../../ui/hangar/README.md). The `music` block's **`full_on`** (`hold`: the full mix
  while a hold runs, fading in over 1 s as it starts and out over 4 s after it ends; `collapse`:
  the full mix from the collapse to the level's end; each only in a level with it), loaded and
  checked, the simulation telling `holdActive()`, `collapseStarted()` and the `HOLD_START`,
  `HOLD_END` and `COLLAPSE_WARNING` events; planned (M5 part C, the game): the run-time stem hook.
- **Level backdrop** (`backdrop` in a level's data file; the *Backdrop* table is rendered from it):
  `scroll_factors` per layer (`deep`, `far`, `ground` = 1.0, `low-air`, `high-air`; `deep` may be
  left out on a top-down surface, Level 04's Luna: then the ground is the layer that covers the
  screen, its tile set in every section and its pieces' edges checked as the deep layer's are, and
  where a ground tile set is not opaque the section's far tile set must be); `ramp` (s an
  atmosphere change takes, centred on the section boundary); `haze_colour` (`rrggbb`);
  `atmosphere` per intensity the level uses (`clear`/`light`/`medium`/`heavy`: optional `banks`
  tile set on low-air, optional `wisps` tile set on high-air, `haze` 0..1 over deep and far);
  `tile_sets` by id (`layer`, `height`; 480 px wide, repeating along the layer; optional `drift`
  px/s sideways, wrapping); `pieces` by id (`layer`, `size`, an animation's `frames` and `fps` or
  an angle set's `headings`, `mid_size` if it counts towards the density rule); `placed` set
  pieces (`piece`, `t` when its centre passes the middle of the screen, `x` in the play field,
  optional `mirror`, a `path` of `[t, dx, dy]` waypoints for a piece with headings, `overhead`
  for a ground piece's part above the road (a bridge's arches, a gate's blockhouse: drawn over
  the convoy and the ground objects and under the ground units, so the crawlers pass under it and
  a turret placed on it stands on it); later pieces on a layer are drawn over earlier ones). A layer position is the layer's scroll (the ground's
  scroll × its factor) plus the screen height; a section's tile set begins at the seam that enters
  at the top edge when the section starts. The images are `assets/backdrop/level-NN/<id>.png`
  (frames and headings `<id>_<n>.png`); `images: level-NN` takes them from another level's
  folder (Level 05 reuses Level 04's Luna until its own art). `content` checks that the ids and layers resolve and,
  sampled every simulation step, the art direction's density (at most 3 mid-size set pieces on
  screen) and motion budget (at most 2 strongly animated elements: animated or moving pieces,
  drifting tile sets, the atmosphere's banks counting as one; nothing moving faster than 120 px/s,
  about 2 px per frame, on its own).
  **Tower pieces** for the perspective towers of the
  [art direction](../../art-direction/README.md#parallax-layer-model) (user decision D1 of M5
  part B: scenery only): a `ground` piece with a `tower` block is drawn in true perspective
  instead of flat. Its `size` is the footprint on the ground; its image `<id>.png` is the roof
  seen from above **at its drawn size**, the footprint × *k* rounded to whole pixels (62 × 64 at
  *h* 1.35: 80 × 83), so it is never resampled at run time. `tower` has `height` (*h* in camera
  units, 0 < *h* ≤ 1.5: the roof is drawn at scale *k* = 6 ÷ (6 − *h*) round the projection
  centre, the play field's (240, 297) counted from its top left, so 1.35 gives 1.29 and 1.5 gives
  1.33), `wall` (the id of a wall texture `assets/backdrop/level-NN/<wall>.png` in kebab-case,
  not a piece's or tile set's id; its columns run along a wall as seen from outside, left to
  right, and repeat every texture width along a long wall; its rows run from the foot, the
  image's bottom row, up to the roof's edge, window rows included, stretched over the wall in
  true perspective: a row at height *z* is drawn at 6 ÷ (6 − *z*), so the storeys grow a little
  towards the roof; several towers may share one) and optional `shade` (0..1, the brightness of
  the walls facing right and down, away from the key light, default 0.5; the walls facing up and
  left are drawn as the texture). Each frame the renderer (`TowerProjection`) draws a tower's
  visible walls (the sides that face the projection centre: the roof leans out past them) and
  then its roof at its projected place on whole pixels, towers in order of height (lowest first,
  then in placement order), after the ground layer's tiles and flat pieces and before the ground
  objects and units, so a tower never hides a unit, a ground target or a bullet; it marks the
  shadow stencil with the rest of the ground, so flyers' shadows fall on walls and roofs at the
  ground offset. A wall is drawn as textured quads, cells of at most 16 px along it and bands up
  it that widen by at most a tenth (about 1 000 quads with 30 towers on screen, 0.1–0.4 ms of
  CPU a frame, one or two draw calls). Placed as other pieces (`t`, `x` of the footprint's
  centre); no `mirror` (the roof and walls are lit from the upper left: a mirrored roof is a
  piece of its own, as for every lit piece), `path` or `overhead`. Nothing the simulation knows
  stands on a tower: ground targets, walker paths and crates stay on streets and low structures
  drawn flat. `content` checks a tower's `height`, `shade` and `wall` id, that it is a ground
  piece with one image, neither mirrored nor overhead, and that its roof's own speed, (*k* − 1) ×
  the scroll, stays inside the motion budget while it is on screen (a tower is on screen while
  its footprint or its roof is); a tower is not one of the budget's animated elements and counts
  towards the density rule only with `mid_size`. A placed piece may also carry a **`repeat`**
  (`count` placements in all, at least 2, each `every` s after the one before, its `path` shifted
  in time with it; a stream of traffic): the renderer and the checks see every copy, a copy's
  problem is reported as `placed[i] (repeat n)`, and the *Backdrop* table lists the stream once
  with "×count every s". Planned (M5 part C, the level clock of hold zones): a placed piece's
  `path` and a tile set's `drift` animate on the real clock, so moving scenery (Level 09's Kilo
  trucks) keeps its speed in a hold; a path's waypoints are timed from the moment its first
  waypoint's script time is reached, and the placements themselves stay in script time (the
  simulation's real clock is `Sortie.realSeconds()`, script time `levelSeconds()`, and between
  steps script time moves at `scriptRate()`).
- **Ship and core parts**: ship (`acceleration_seconds`, `stop_seconds`, `precision_factor`,
  `size`, `edge_gap`, `hull` (hit boxes `[x, y, width, height]` from the sprite's top left), `collection_radius`, `mercy_seconds`, `bank_change_steps`,
  `mounts`: `front`, `wings`, `roots` (the side guns' muzzles), `rear`, `engines`; its speed is the fitted engine's); shields (`break_seconds`, `models` with
  `capacity`, `regen`, `delay`, `draw`); armour (`plating` with `max`, and the low-armour `radio`
  line: `speaker`, optional `expression` and `distorted`, `line`); generator
  (`spare_power`, `models` with `output`); systems (`engines` with `speed`, `draw`; `bays` with `start` (the starting utility bays) and
  `extra` (each bought bay's `price` and `available`; their count and `start` add up to the loadout's
  utility slots); `utility`
  with `draw`, one price per level, `design` status, the Pickup magnet's `magnet` (`radius`, `pull`, one
  per level) and an optional `for_sale` (false keeps an unlocked module out of the shop); the
  Targeting computer's `targeting` (`turn_bonus`, 0.2: the share added to the
  Stormhawk's homing turn rates and the Swivel's slew; `bar_seconds`, 1.5: how long an HP bar stays
  after the last hit; `bar_fade`, 0.3 s) and the Salvage scanner's `salvage` (`bonus`, one share
  per level: `[0.1, 0.2]`)); specials (`input_buffer` (s a press waits
  while the special is busy), `specials` with `name`, `charge_price`, `max_charges`, `unlock` and
  optional `free_charges` (given once at the unlock), and the `airstrike` block: `delay`, `offset`,
  `speed`, `bomber_size`, `bomb_spacing`, `fall`, `blast_radius`, `damage` (`ground`, `air` per
  blast), `cap` (`ground`, `air`, `boss_part` per strike) and the call's `radio` (`speaker`,
  `portrait`, `line`), and the `smart_bomb` block: `flash` (`seconds` held at `opacity`, then
  `fade` s), `ring` (s to cover the play field), `damage` (`all`, `boss_part`), `invulnerable` and
  `repeat` (s)). Each model also has `name`, `price` and `available`.
- **Player** (`player/data.yaml`): `availability`, `pickup_seconds`, `pickup_drift_speed`, `pickups` (salvage
  credits, overdrive `levels` and `seconds`, shield cell `shield_percent`, armour patch
  `armour`, special charge `charges`, data core). Levels name pickups as `small salvage`,
  `armour patch`, ….
- **Systems**: economy (`starting_credits`, `budget` `base` and `growth` (the typical haul),
  `act_factor`, `sell_back`, `typical_player` shares `air_kills`, `ground_targets`, `secrets`,
  `pickups`, `primary`, `secondary`); difficulty (one `{easy, medium, hard}` entry per lever: factors such as
  `enemy_hp`, changes such as `formation_size` (−0.2 = −20 %), `aimed_spread_degrees`, `bullet_budget`,
  `repair_cost`, `retries`, `boss_checkpoint`, `sensor_bonus`); scoring (`kill_score`,
  `pickup_score`, `chain`, `rating` weights and `full_chain`, `bonuses`, `grades`); retry (`armour_floor`, the
  share of the maximum armour a retry starts with at least).
- **Voice** (`audio/voice/data.yaml`): `expressions` (`neutral`, `grim`, `fierce` and `shout`,
  each `exaggeration`, `cfg_weight`, `temperature`: Chatterbox's settings) and `speakers` by voice
  slug: `names` (the speakers as the level data writes them), `ref` (the clip in
  `design/audio/voice/refs/`), optional `shift` (added to the expression's row), `fixed` (settings
  for every line instead), `layering` (`choir`), `pins` (a line's seed by its key) and `filter`
  (`pa`: the public-address filter instead of the radio filter, part of the key) and `stage` (the
  sound file in `assets/voice/<voice>/` a radio line plays when it is only a stage direction,
  `[the Choir sings]`). The line list and keys: `vanguard.content.voice.VoiceLines`; the files
  `assets/voice/<voice>/<key>.ogg`.
- **Allies** (`allies/data.yaml`): one entry per ally
  slug with `name`, `layer`, `size`, `hitbox`, `hp` (medium; the level sets difficulty variants),
  `damaged_by` (`objective_aimed`: only shots the target-the-objective hook aims at it;
  `claws`: damage per second while a walker overlaps it), `follows` (`road`), `headings`
  (`count`, `step` in °, centred on straight up; rendered, not rotated) and `smoke_below` (share
  of its HP).
- **Act** (`campaign/<act>/data.yaml`): `levels` `[first, last]` (global numbers), `title_card`
  (`act`, `name`, `line`), `briefing` (pages as a level's); planned (part G): `outro`, the act-end
  outro after the last level's debrief: `music` (a music file's name, `act-complete`: track 24
  under the first page, played once) and `pages` (as the briefing's: `speaker`, `line`, optional
  `expression` and `image`); the loader checks that every level's act exists and includes it.
  Saves (not a data file; see [saves](../../systems/saves/README.md)): format version 2 (part G)
  adds `stats.levels`, per won level number its banked `credits` and `kills`. Planned (M5 part A):
  format version 3 adds `escort` (`hired`, `side`, `fitted`, `guns` with `item` and `level`,
  `armour`), migrated from version 2.
- **Wingmen** (planned (M5 part A): `player/wingmen/data.yaml`, see
  [wingmen](../../player/wingmen/README.md#data)): `rook` (`joins` (the first
  level), `side` (a new campaign's), `armour`, `size`, `hitbox`, `muzzle` (his one gun's muzzle,
  px from the sprite's top left), `speed`, `acceleration_seconds`,
  `min_distance`, `edge_gap`, `ram_damage`, `launch_warning` (share of his armour), `glide_seconds`,
  `swap_seconds`, `formations` (`wing`, `wide`, `trail`: `[x, y]` offsets from the player's centre,
  y down, x mirrored on the left side), `flank_distance`, `reaction_seconds`, `dodge` (`interval`,
  `look_ahead`, `clearance` (also his gap to the air enemies' bodies), `step`, `reacts` (the share
  of the predicted bullets he reacts to, each decided once by his own seeded generator)), `cone` (° full width), `range`, `recent_hit_seconds`, `eject`
  (`explosion`, `pod_speed`)); `guns` (`price_factor` of the base weapon's price and upgrade base,
  and the `list`, the first the free starter: `id`, `name`, `base` (a weapon slug), `scale` (on each
  projectile's damage), `available`); `barks` (`speaker`, `spacing` s, and `triggers` in priority
  order: `trigger` (`boss-warning`, `rear-wave` with `ahead` s, `sides-wave`, `player-armour` and
  `rook-armour` with `below` (share), `rook-ejects`, `kill-streak` with `kills` and `seconds`,
  `overdrive`), the portrait's `expression`, optional `shout`, and the variant `lines`). His repair
  cost is the difficulty's `repair_cost`.

### Presentation (`game`)

- A **screen state machine** for the flow in [ui](../../ui/README.md): title, menu, difficulty,
  briefing, hangar, level, pause, debrief, options, credits.
- One **UI kit** (glass panels, metal HUD, bitmap fonts 8×12 / 10×20 / 20×30) shared by all
  screens; keyboard and gamepad navigation everywhere.
- Rendering into the 960×540 `PixelScreen`, integer-scaled with letterboxing or sharp-bilinear;
  parallax layers per the [art direction](../../art-direction/README.md); shaders stay
  GLES 2 compatible (the ANGLE fallback on macOS).
- Audio: the streaming music player with intro and loop points, and the SFX bank with 64
  sources. OpenAL Soft sums every source (the effects and the music stream's `AudioDevice`) into
  the device's float mix; it has no limiter on float output by default, so the
  [master limiter](../../audio/README.md#master-limiter) is OpenAL Soft's output limiter
  (`ALC_SOFT_output_limiter`): `desktop`'s `LimitedAudio` (libGDX's `OpenALLwjgl3Audio`, returned by
  the launcher's `createAudio`) resets the device with `ALC_OUTPUT_LIMITER_SOFT` on through
  `alcResetDeviceSOFT` before anything plays, and `OutputLimiter.watch` turns it back on from the
  render loop's `update()` once a second when libGDX's device observer turned it off: the observer
  reopens the device without attributes (`alcReopenDeviceSOFT`) once just after the start and on
  every change of the device list, so the limiter is off for up to a second then (it gives up after
  three resets in a row that do not take). The limiter is fixed by OpenAL Soft: 0 dBFS, 1 ms look-ahead, 2 ms
  hold, automatic attack and release, and an adaptive make-up gain that is the slow release after
  an over. `OutputLimiterTest` checks it on a loopback device (`ALC_SOFT_loopback`), which renders
  the mix into memory without a sound card.

### Assets

- Production sprites and audio are source files in `assets/` (LFS). Until a part's art is
  `final`, its **chosen concept art** is copied in as a placeholder by a script, so the game is
  playable early.
- **Exception: level backdrops.** A chosen setting scene is one composed concept sheet, not the
  tile sets and set pieces a level is built from, so cutting it up gave one repeating crop per
  layer. The backdrop placeholders are therefore rendered by `tools/concept/backdrop_l01.py`
  straight into `assets/backdrop/level-01/` from the chosen Earth orbit scene's models, palette
  and rules, at the sizes the level's data file gives; each PNG carries a `Placeholder` text chunk
  naming the script. `importPlaceholders` does not touch them. The same holds for ground targets
  without concept art: `tools/concept/ground_targets.py` renders Level 01's cargo containers,
  beacon, their break-apart and glint frames into `assets/sprites/` (superseded by
  `tools/art/loot_targets.py`).
- **Final art** is rendered by the production generators in `tools/art/` (see its README)
  straight into `assets/`; every PNG carries a `Source` text chunk naming its generator, which is
  how `importPlaceholders` knows to leave a part alone (`FinalArt`). Attachment points per frame
  (mount points, pod offsets) are JSON pivot files in `assets/pivots/`. Once a part is final, its
  placeholder generator (e.g. `ground_targets.py`) is superseded.
- The `pipeline` module turns them into build output at build time: angle sets (using the
  symmetry rule), texture atlases, audio in OGG. Generated atlases are never committed.
  `packAtlases` checks the packed pages against the budgets of the production plan and fails the
  build when one is exceeded (`AtlasBudget`). It packs a shared `sprites` atlas and one unit atlas
  `level-NN` per level for the sprites only that level uses (`SpriteUse` derives the split from the
  levels' data and fails the build on a sprite nothing uses). `Sprites` keeps the shared atlas
  loaded; `LevelScreen` loads its level's unit atlas (`enterLevel`) and disposes it when it closes
  (`leaveLevel`, counted so a retry's new screen keeps it); lookups by name search both.

### Testing

| Level | What | Where |
|---|---|---|
| Unit | rules in `sim` and `content`: damage, shields, pickups, economy, formations, loader validation | JUnit 5, every module |
| Content | every data file loads and validates; balance checks per level | JUnit over `design/**/data.yaml` |
| Replay | per level a recorded run (seed + input) must give the same state hash, score and credits | JUnit, headless; re-recorded deliberately when rules change |
| Allocation | no allocation per simulation step | JUnit |
| Smoke | the desktop build starts, runs a level for a few seconds in `--bench` mode and exits | CI on Linux (xvfb), locally on all |

Screenshot tests are left out until there is a need.

### Conventions

- Java 21; records, sealed interfaces and switch expressions where they fit; no reflection-based
  frameworks beyond Jackson in `content`.
- Formatting enforced by the build (Spotless with palantir-java-format, 120 columns, as the
  spike code); compiler warnings are errors.
- Small classes, clear names, light Javadoc on public types; tests named as behaviour.
- The README of the part being implemented is the ticket: its *Implementation* checklist is
  ticked in the same change as the code.

### CI and releases

- Every push: build, format check and tests on Linux, Windows and macOS (as in the spike).
- Tags `v*`: Construo bundles for linux-x64, windows-x64, macos-x64 and macos-arm64 are attached
  to a GitHub release. macOS builds are unsigned until notarization is set up.

## Implementation

- [x] Gradle root build with `sim`, `content`, `game`, `desktop`, `pipeline` and `buildSrc`
- [x] Spike foundations carried over as listed, with their tests (`SfxBank` follows with the first sound effects in M1)
- [x] Data-file loader with validation; `sync_tables.py`; `check_docs.py` compares rendered tables
- [x] `balance-data.json` migrated to per-part `data.yaml`
- [x] Balance checks ported to JUnit (`BalanceTest`, `ActPlaythroughTest` in `content`; `tools/balance.py` prints the balancing sheet)
- [x] Asset pipeline: placeholder import from chosen concept art, angle sets, atlases (`:pipeline:importPlaceholders` with `PlaceholderSprites`, `AngleSetGenerator`, `FinalArt`; `packAtlases` with `AtlasPacker` and `AtlasBudget`; see [production](../../art-direction/production/README.md#implementation))
- [x] CI: build, format check, tests on three OSes; release bundles on tags
- [x] Smoke test of the desktop build in CI

## Open questions

- None open.

## Decisions

- 2026-10-01: Drafted after the tech stack was approved.
- 2026-10-01: User decisions: game numbers live in `data.yaml` files next to the documents and
  the README tables are rendered from them; the format is YAML (`balance-data.json` is converted).
- 2026-10-01: Approved by the user.
- 2026-10-01: M0 skeleton built. `FixedStepClock` moved to `sim` (it has no libGDX dependency);
  `InputRecording` lost its `replay` method with the spike's `World`, so `Command` stayed behind
  too. `SfxBank` is not carried over yet: M0 plays no sound effects, and its effect list named
  spike files; it returns with the SFX player in M1. palantir-java-format is pinned (2.80.0) in
  the version catalog.
- 2026-10-01: Placeholders: `./gradlew :pipeline:importPlaceholders` copies the chosen concept art
  and music into `assets/` (committed, LFS); the build only reads `assets/`, so CI pulls just
  `assets/**` from LFS.
- 2026-10-01: macOS first thread: the Construo launcher (roast) starts the JVM on the first thread
  by default (`runOnFirstThread`), so the bundles need nothing; `./gradlew :desktop:run` adds
  `-XstartOnFirstThread` on macOS. libGDX 1.14's `useGlfwAsync()` (LWJGL's `glfw_async`) was not
  chosen: it is newer, untested here, and would mean a second mechanism next to roast's. The
  `installDist` start script does not add the flag; it is a Linux smoke-test tool.
- 2026-10-01: Generational ZGC (`-XX:+UseZGC -XX:+ZGenerational`) in the bundles (roast) and in the
  `run` task and `installDist` scripts, so development runs match the release.
- 2026-10-01: M1 asset pipeline: `:pipeline:importPlaceholders` now also runs `PlaceholderSprites`, which cuts the placeholder frames out of the chosen concept sheets with documented crop rectangles (checkerboards keyed out, glow sprites background-subtracted for additive drawing, zoomed frames sampled back to 1×) into `assets/sprites` and `assets/backdrop` (committed, LFS). `:pipeline:packAtlases` packs them at build time into `sprites` (nearest) and `backdrop` (linear) atlases in the build output, which the desktop resources include instead of the single frames. `SfxBank` is carried over.
- 2026-10-01: M1 simulation: `Sortie` owns the ship, shots and Skitters in pools and emits `SimEvents`; the numbers sit in one record per part (`ShipSpec`, `PulseCannon`, `ShieldModel`, `Plating`, `SkitterSpec`) until the M2 data files. Tests: unit tests per rule, an allocation test over 3 600 steps and a replay test of a recorded minute of the test sortie. `--bench` now flies the test sortie.
- 2026-10-02: M2 data files (part A). One `data.yaml` per part next to its README, YAML with
  comments and compact flow-style rows; README wording a table needs goes under `notes` with
  `{field}` placeholders, so no number exists twice; the schemas are documented once, in *Data
  file schemas*. Generated tables are marked `<!-- data: NAME -->` … `<!-- /data -->` (one
  convention; the weapon tables' `balance:levels` markers were converted). After the migration
  every marked table renders byte-identical to its hand-written version, except the weapon
  tables' footnote, which now names `data.yaml` and `tools/sync_tables.py`.
- 2026-10-02: Tools: `tools/sync_tables.py` renders the tables (PyYAML), `tools/design_data.py`
  holds the loading, the placeholder filling and the weapon numbers shared with
  `tools/balance.py`; `balance.py` reads the data files and the plan
  (`design/player/balance-plan.yaml`) and prints the same report as before. Its enemy hook still
  looks for `design/enemies/balance-data.json`, so the report stays unchanged; pointing it at
  the enemy and level data files is left for the JUnit port. `balance-data.json` is removed:
  its `_doc` now lives in *Data file schemas*, its plan in `balance-plan.yaml`, two values took
  the README's meaning (Bomb Rack `range: drop` instead of 0, Proximity Mines `lifetime: 4`
  instead of no range), and the shield, armour, generator, engine and utility lists gained the rows
  that were only in the READMEs.
- 2026-10-02: Loader: Jackson 3 YAML into records; required unless `Optional` (a custom
  annotation introspector, so the records need no annotations), unknown keys rejected, `notes`
  ignored; float-to-int coercion off. Run-time delivery: the data files are copied into the
  `content` resources with an index rather than read from `design/` at run time, so the jar and
  the bundles carry them and the classpath is the only source.
- 2026-10-02: The simulation receives its specs from `content` (`SimSpecs` builds the `sim`
  records), so `sim` has no dependency on the data format; the M1 constants (`STORMHAWK`,
  `LEVEL_1`, `MK_I`, `STANDARD`, `SKITTER`) are gone. The `sim` rule tests build their own specs
  with the M1 numbers (`TestSpecs`); the replay test moved to `content` and flies the specs built
  from the data, with the same state hash (`6d6181b0b974e1bb`), because every migrated number is
  the same double as the M1 constant.
- 2026-10-02: M2 part B (Level 01). `sim`: `Sortie` runs a `LevelScript` with `Rules` (built by
  `content`'s `SimSpecs` at a `Difficulty`, levers already applied): `WaveSchedule` plans every
  unit once per level (`Formations`, `Spawn`), and an attempt walks it with a cursor; `Enemy`
  (enter, hold or orbit while firing, leave), `EnemyBullet`, `GroundObject`, `Pickup` and `Tally`
  (score, chain, credits by source) live in pools or fixed arrays; radio cues, edge warnings and
  pickups reach the presentation as `SimEvents` with an int value or as read-only state;
  `LevelResult` is built on demand for the debrief, outside the step. `M1`'s `TestSortie`,
  `Skitter`, `SkitterSpec` and `SnakeWave` are gone (`SnakePath` became the general `FlightPath`).
  Content: levels are keyed by their path under `design/campaign` (`act-1-…/level-01-…`), so the
  act and number come from the key. Data schema additions (documented above): enemy `drops`,
  wave `speed` and `interval`, ground target `size` and `at`, `pickup_drift_speed`,
  `notes.threat_profile`; removed: wave edge `both`, a placed pickup's `t`, easy
  `attack_interval`. Tests: an allocation test over a whole level (after a warm-up run, since the
  first run initialises `Trig`'s table and the enum switch maps), and the replay of the whole of
  Level 01 at medium (`level-01-medium-2185.rec`, hash `f3ae19deb416eead`, 90 kills, 916
  credits), re-recorded with `-Dvanguard.recordDir`. `game`: `LevelScreen`, `DebriefScreen`, the
  HUD split into `HudKit`, `MissionPanel` and `ShipPanel`, `RadioQueue`, `ControlPrompts`,
  `LevelMusic`. `desktop`: `--difficulty easy|medium|hard` and the debug option `--debug-speed <n>`
  (game time n times faster, to get through a level quickly when testing).
- 2026-10-02: M2 part C (level backdrop). The backdrop is data (*Level backdrop* above) and
  `vanguard.game.render.Backdrop` draws it: per layer the sections' tile sets (whole pixels, the
  layer's factor of the sim's ground scroll; drawn as texture strips so a seam can fall anywhere),
  the set pieces, the haze, the cloud banks and wisps blended across a ramp, without allocating per
  frame. Tile sets change spatially at a seam (structures would ghost in a dissolve), the
  atmosphere in time (the art direction's ramp). The low-air layer now draws above the ground
  objects. The backdrop atlas uses nearest filtering (everything at native size); the old
  single-crop cuts of `parallax-r03-a.png` and their `PlaceholderSprites` entries are gone. The
  game compiles against the data records, so `content` exports Jackson's annotations (`api`).
  The simulation is untouched (same replay hash).
- 2026-10-02: Level 01 after playing (user decisions): the level is 180 s; the replay was
  re-recorded (`level-01-medium-2185.rec` now 10,800 steps, hash `e602b2264976076f`, still 90
  kills and 916 credits; before: 11,400 steps, `f3ae19deb416eead`). The old recording ran 600
  steps past the new end and still matched, since nothing changed after the last enemy; re-record
  whenever a level's length changes. Loot targets (art direction, readability rule 7):
  `GroundObject` counts the steps since its last hit (hashed, like the ship's `ticksSinceShot`),
  which `LevelRenderer` turns into the white hit flash (`FlashShader`) and the damaged frame; the
  glint phase comes from the tick and the target's x, the beacon blinks with the tick. `Effects`
  comes as `glowing()` (additive) and `solid()`; a destroyed container's break-apart is a solid
  effect placed in ground coordinates (drawn at minus the ground scroll, so it rides the ground)
  above the ground objects and below the low-air layer.
- 2026-10-02: Backdrop and HUD fixes after playing Level 01. `LevelData.OUTRO_SECONDS` (5 s: the
  scroll runs on after the level end until the debrief) is shared by `LevelScreen` and the backdrop
  checks; `BackdropLayer.opaque()` marks `deep`, on which `BackdropCheck` wants a tile set in every
  section, and its density and motion checks now run through the outro; `BackdropAssetsTest`
  steps the level and its outro and fails when a set piece on an opaque layer shows a
  non-transparent edge on screen (the deep-layer coverage rule of the art direction). The left
  HUD panel's regions are `vanguard.game.render.MissionLayout` (y ranges as in the HUD's *Left
  panel layout*), `HudKit` cuts text off at a width, and `MissionLayoutTest` measures the
  content's prompts and radio lines with the built-in font's metrics, read without a GL context
  (`BitmapFontData` from the classpath). Debug launch option `--invulnerable` (with
  `--debug-speed`, for testing only): `Rules.withInvulnerableShip()` lets enemy bullets and rammers
  pass through the ship; it is off unless the option is given, so play and the replay are
  unchanged.
- 2026-10-02: M3 part A (screen flow, UI kit, options). `game`: `screen.ScreenFlow` is a stack
  (`Transition`: stay, open, back, replace, quit); `ui` holds the glass kit (`Glass`, `Fonts`,
  `TitleScene`, `Menu`, `Dialog`, `Words`); `settings` the Options records (`Settings` with
  `VideoSettings`, `AudioSettings`, `ControlSettings`, `GameplaySettings`) and the `SettingsStore`
  seam, which `desktop`'s `SettingsFile` implements next to the display keys; `audio.Mixer` scales
  every sound by its `Bus`. Input: fixed menu actions beside the remappable flight actions,
  `MenuInput` (key repeat), `KeyCapture` and `Bindings.withKey` / `withButton` (conflict swap);
  `ActionInput` reports focus loss and gamepad disconnects. `sim`: `Sortie.retry()` for the pause
  menu's restart (a restart now also clears `complete`); the replay hash is unchanged. Launch
  option `--start title|level` (default: title, level in a bench run), so a bench run can test the
  menus. Assets: `tools/concept/ui_assets.py` renders the bitmap fonts (BMFont text files, PNG
  pages) and the title scene and logo into `assets/`, like the backdrop placeholders; the menu
  sounds join `importPlaceholders`.
- 2026-10-02: M3 part B1. The campaign state is plain Java in `content`
  (`vanguard.content.campaign`, since it needs the difficulty and the level keys that `content`
  owns): `Campaign` (the state and its transitions: a won level banks, a failure loses the attempt,
  retries and the armour floor), `CampaignRules` (its numbers from the data), `CampaignRoute`
  (briefing / hangar / launch between levels), `Briefings` (the briefing before a level, with the
  act intro when the level opens its act), and the saves: `SaveGame` (the record),
  `SaveFormat` (versioned JSON through Jackson's JSON mapper) and `SaveSlots` (the files, written
  atomically). The desktop launcher puts the saves next to the settings file (`saves/`), so a
  `--settings` file in a temporary directory keeps test saves out of the real ones. `sim`: a
  destroyed ship no longer restarts by itself; `Sortie.retry(armour)` starts the next attempt with
  the campaign's armour and the constructor takes the first attempt's armour. New data files: the
  act's `data.yaml` and `systems/retry/data.yaml`; level data gained `briefing`; `sync_tables.py`
  renders the briefings (`briefing`, `teaser`, `act-title-card`, `act-briefing`).
- 2026-10-02: M3 part B2. `vanguard.content.campaign` gained the hangar's rules: `ItemKind` (what
  fits a slot), `Gear` (the immutable credits, loadout, inventory by kind, charges and armour the
  hangar changes; the visit's undo keeps one per transaction), `Catalogue` (every shop item from
  the data with price, upgrades, draw per level, unlock, traits and shown numbers; sell-back,
  repair cost and sensor bonus), `Hangar` (a visit: shop rows with their choices and refusals,
  buy / upgrade / fit / unfit / sell / charge, repair, undo, load and output, sensor level),
  `Intel` (the next level's threat profile and what the sensor level shows) and `Flight` (what a
  sortie flies of the loadout: `SimSpecs.loadout(engine, Pulse Cannon level, shield, plating)`).
  `game` gained `vanguard.game.hangar`: `HangarState` (the screen's focus and keys, headless and
  tested), `HangarView` with `ShopPanel`, `LoadoutPanel`, `IntelPanel` and the code-drawn
  `TacticalMap`, and `Names`. `sim`: `PulseCannon.pattern` fires a level's parallel bolts (the
  replay hash is unchanged at L1). Level data: `threat_profile` is structured (above); the save's
  inventory is a map by kind.
- 2026-10-02: Level 01 music and radio fixes. `LevelData.RadioCue` takes `easy` / `hard`
  `RadioChange`s (another line), applied by `SimSpecs` like the waves' changes, so the sim's cue list
  and the replay are unchanged; `LevelData.Music.startDb` sets the theme's level in its start
  section, which `LevelMusic` raises to full over 2 s after it. The outro after a won level is
  `vanguard.game.level.Outro`: `LevelScreen` hands over to the debrief once `RadioQueue.idle()`
  (nothing shown or queued), at the latest `LevelData.OUTRO_SECONDS` (now 15 s, the longest outro,
  which the backdrop checks step through) after the level end; the sim's level result is
  unchanged.
- 2026-10-02: The spike branch was replaced by the tag `spike-libgdx` (user decision).
- 2026-10-02: Production art, UI batch part U2. `Sprites.region(name)` and `Sprites.patch(name)`
  look up the named regions and nine-patches of the sprite pages; `Glass` takes `Sprites` and
  holds the glass kit's pieces (`ui/…`, tools/art/ui_kit.py) instead of a white pixel and two
  generated textures, so it is no longer `Disposable`. `vanguard.game.hangar.ItemIcons` maps every
  catalogue item (by identity) and the escort to its two icons (`icons/…`, tools/art/icons.py) when
  the hangar opens and fails there if one is missing; `TacticalMap` loads `ui/hangar-map.png`
  (tools/art/ui_scenes.py), which also renders the title scene and logo that
  `tools/concept/ui_assets.py` no longer writes. The replay hash is unchanged.
- 2026-10-02: M4 part A (the arsenal). `sim`: `Sortie` hands its work to `PlayerFire` (the ship's weapons:
  every `Armament.Mount` fires on its own clock, overdrive patterns, projectiles that fly, seek,
  fall and burst; what they reach by `WeaponSpec.Delivery` and the layer rules, hardened ground
  targets), `EnemyForce` (waves, enemies, bullets), `Objectives` and `Radio`; the split itself kept
  the replay's hash. `WeaponSpec` replaces `PulseCannon`: rate, damage, speed, hit box, range or
  lifetime, pierce, blast, turn rate, seek cone, fall or flight time, snap radius and one `Muzzle`
  (offset and angle from the ship's centre) per projectile; `Shot` carries its weapon, velocity,
  heading, range flown, pierce count and the serials it struck, a homing shot its locked target
  (`Enemy.serial`, unique per attempt), a bomb or shell its landing point scrolling with the ground.
  New events: `SHOT_GLANCED`, `BLAST`, `OVERDRIVE_ENDED`; shot and hit events carry the mount.
  `Trig.sin` no longer reads past its table for a tiny negative angle (a missile's heading found it).
  `content`: `SimSpecs.weapon` builds the muzzles from the ship's mount points (pods mirrored on the
  left and turned in by `converge`, side guns from the wing `roots`, mirrored to the left),
  `SimSpecs.loadout` takes the fitted weapons and the spare power (the shield's regen bonus),
  `Flight` maps the campaign loadout and computes the spare power like the hangar; a ground target
  may be `hardened`. Data: weapon `converge`, `fall`, `flight`, `snap`, `cone` (and a homing weapon's
  `range` as its seek radius beside `lifetime`), ship mount `roots`. `game`: `WeaponLooks` (shot
  sprites per angle, heading or level, muzzle flashes, impacts, pods from `pivots/pods.json` via
  `PodPivots`), `FlightSounds` per weapon family, the HUD's power, weapons and overdrive rows.
  Debug launch option `--loadout <slot>=<weapon>[:<level>],...` (`DebugFit`, implies `--start level`;
  testing only). The Level 01 replay keeps its kills (90) and credits (916); its hash is now
  `c58ff0e1fb68eae9` (the new state is hashed, and the starter fit's 4 MW spare power raises its
  shield regen by 40 %).
- 2026-10-02: M4 part B (Level 02). `sim`: ground units (`LevelScript.GroundUnit`: an enemy of the level's
  ground targets, entering at the top edge and scrolling with the ground; a turret's barrel turns
  at its turn rate inside its arc and fires along itself), dives (`EnemySpec.Dive`: the hold is the
  pause, the leave the dive at its own speed, one shot when it passes the ship's height or after
  its fire time), fans (`EnemyGun.fan`, `spreadRadians`), the `single` and `column` formations,
  group objectives (`Objectives`: a group's outcome when its last unit is gone; pay per cleared
  group, `GROUP_CLEARED` / `GROUP_LOST` events and cue triggers), `Crane` (the arm's angle a
  function of the level time; contact at most once a second; it stops every shot; a clamp counted
  while it swings), the overdrive and salvage M pickups. `content`: the level data's `prompts`,
  `cranes`, a section's `peak` (`LevelData.atmosphereStretches()` drives the backdrop's ramps and
  checks), ground targets with `enemy`, `group` and `easy` / `hard` placements, a secondary
  objective of `groups`, radio cues with `group` and `portrait`; the enemy data's `dive`,
  `terrain`, a fan's `count` and `spread`, a turret's `turn_rate` and `arc`, the `±30° tilt`
  orientation, optional weak-point multipliers and the `fan_count`, `dive_pause`, `burst` hooks.
  `game`: `EnemyLooks` finds a unit's frames by its slug (a tilt set, a pause flare, remains),
  `CraneLooks` (`pivots/crane-four.json`), the tracker's group pips, the level's theme by its
  track number. Debug option `--level <n>` (`DebugFit.startAt`, testing only). The Level 01 replay
  is unchanged (`c58ff0e1fb68eae9`).
- 2026-10-02: M4 part C (Level 03), the game. `LevelRenderer` draws the low-air flyers below the
  low-air layer's banks, the debris chunks on the play plane above the flyers (hit flash from
  `Debris.ticksSinceHit`), the spore mines additively just below the enemy bullets (growing and
  brightening while they rise), and the set pieces through `SetPieceLooks` (a part's sprite is
  `<slug>-<part>` with the screen side last, `fin-left`; a part without one is drawn as its
  `-glow`; per-frame pivots from `pivots/<slug>.json`): on the play plane below the ship, off it
  (arriving, descending, rising) above the ship and scaled between 1.25 and 1 by
  `SetPiece.altitude`. A level whose backdrop holds a `lifeboat-light` image draws its triggers as
  that light. `Sprites.frames` returns a sprite's frames in index order; `EnemyLooks` plays a
  radial spinner's frames at its spin; `Effects` can start an animation after a delay (the set
  piece's chained death). `LevelMusic` crossfades both ways per section (`Music.full`); a prompt's
  `skip` layer (`Prompt.skipLayer`) replaces Level 02's ground-prompt special case. The level's
  `backdrop` stays required.
- 2026-10-02: M4 part C, the death effects. `EnemyLooks` finds a unit's death animations by its
  slug like its flare and remains: `<slug>-death` additive, `<slug>-tatters` or `<slug>-husk`
  solid, as `DeathEffect`s (frames, steps per frame, delay) timed by tier (`tiny`: 2 steps a
  frame, the glow 4 steps after the pop; larger: 4 steps a frame, with the burst). `LevelScreen`
  keeps a second solid `Effects` at play-field positions (no ground scroll) for the pieces, which
  `LevelRenderer` draws after the set piece on the play plane, below the ship and the glows; a set
  piece's `<slug>-ichor` cloud (6 steps a frame) starts with each chained burst and the large one,
  `Effects` drawing it at the high-air scale and opacity (`LevelRenderer.highAirScale` and
  `highAirOpacity`) when the unit dies off the plane. `FlightSounds` plays a set piece's cry by
  slug (`Sfx.LEVIATHAN_CRY`) with its death.
- 2026-10-03 (user decision): two M4 part C points accepted as they are: the Level 01 replay's state hash changed with part C (`ReplayTest`, now `616ea9b687b6d5a9`), and the death pieces (`-tatters`, `-husk`) are drawn below the ship.
- 2026-10-03: Schema text for M4 part D (Level 04), marked "planned (part D)" until the loader
  reads it: the `drift`, `sine` and `walk` movement, the `spawn` attack and its hooks, a fan's
  `aim`, an aimed attack's `away`, `armour` as a mapping with `front_arc`, a walker wave's `paths`,
  the level's `road`, the `escort` primary, the threat profile's `objective`, `requires: special`
  on prompts and radio cues, the ally radio events, and the allies' data file. The Brood Pod,
  Scuttler and crawler data files exist ahead of the code, so the content loader rejects them
  until part D's loader work lands.
- 2026-10-03: M4 part D step 2, the convoy: `vanguard.sim.Convoy` (the `escort` primary's units,
  `Ally`, allocated up front and in the state hash only in levels with a convoy, so the Level 01–03
  replay hashes are unchanged), `Road` (straight between its points, as the backdrop art lays it
  out), the target-the-objective hook in mode `nearest` in `EnemyForce` (an aimed shot's target is
  chosen each step and as it fires; a turret's barrel turns toward it; `EnemyBullet` remembers a
  convoy target), the generic failed primary objective (`Sortie.primaryFailed()`, `PRIMARY_FAILED`),
  the escort's pay (`LevelResult.Escort`, part of the objectives' credits) and the radio's
  `FIRST_ALLY_HIT`, `FIRST_ALLY_LOST`, `MISSION_FAILED` cues, level-end ranges and special-only cues.
  The schema text above lost its "planned (part D)" marks for the road, the escort primary, the
  ally events, `requires: special`, the threat profile's `objective` and the allies' data file.
- 2026-10-03: M4 part D step 3, Level 04: `EnemySpec` gains `Sine`, `Brood` (a spawner) and
  `Walker` (walk, frontal arc, the spit as a second gun, the away angle); `Spawn` an `Escort` circle
  and a `WalkPath` (`vanguard.sim.WalkPath`, the authored ground path); `WaveSpec` the `paths` and
  the `CARRIER_ESCORTS` formation. `Enemy` has two new phases, `ESCORT` (circling its carrier, which
  `EnemyForce` hands it as the spawner that entered last, and released when it ends) and `WALK`;
  their fields and a spawner's burst timer enter the state hash only for such units, so the Level
  01–03 replay hashes are unchanged. `EnemyForce.hatch` releases a spawner's units (pooled ordinary
  units in the `LEAVE` phase), the released units count among the level's enemies, and a self-burst
  calls `Escapes.burst` (`Tally.unchained`: the burst bounty at the kill score, no chain, no kill).
  `PlayerFire` makes a direct shot glance off a walker's front (`Enemy.glances`, from `Shot.vx/vy`).
  New events `BROOD_HATCHED`, `BROOD_BURST` and `WALKER_DOWN` (the kind and facing, for the husk).
  Content: `LevelData.Wave.paths`, `EnemyChange.speedFactor`, `GroundTarget.bonusDrop`, a
  destructible's `reveals`, and `BackdropData.base()` (`BackdropLayer.opaque()` is gone). Game:
  `EnemyLooks` reads `-burst` as a death glow, a walker's `-glow` and `-husk` (its remains per
  heading), the walk frame by distance and a spawner's faster telegraph pulse; `LevelRenderer` draws
  walkers on the ground depth and their glow above the low-air layer; `Sfx.BROOD_BURST` and
  `AMBIENCE_LUNA`.
- 2026-10-03: M4 part E (main-agent choice): the enemy schema gains the planned boss and mortar
  fields (the `mortar`, `ring` and `spiral` patterns, aimed bursts and rotation, part
  multipliers, `chains`, per-attack difficulty changes, the `boss` block with phases and par),
  first used by the Polyp Mortar's and the Gorgon Frigate's data files; the loader accepts them
  once part E builds them.
- 2026-10-03: M4 part E step 2, the boss layer (main-agent brief): a boss is a `SetPiece` with a
  `BossSpec` (`vanguard.sim`) instead of passes, built by `SimSpecs.boss` from the stat block's
  `boss`, `chains` and attacks and the level's `boss` placement. It arrives on the level clock,
  descends invulnerable (`partShielded`), settles and sways; its necks are per-piece angles eased
  toward the ship with a first-order lag (`chainAngle`, armoured segments glance shots); its phases
  end on `until`, fire their attacks through `SetPiece.BossActions` (aimed bursts with rotation,
  rings, spirals, alternation after a 1 s crown opening) and release stream units planned once
  (`Formations.streamUnit`). The `Sortie` holds the arena clock (`Section.arena`: halt, early-death
  jump with the ground scroll, 1 s ramp), the boss checkpoint (`retryFromBoss()`: preallocated
  copies of the tally, objectives, radio, defences, charges and the schedule's place, recorded
  just before the arrival) and the Boss rush (`LevelResult.BossTime`). New events `BOSS_ARRIVED`,
  `BOSS_SETTLED`, `BOSS_PHASE`, `BOSS_DESTROYED` (the credit shower's credits), `BOSS_RETRY`; cue
  triggers `boss-phase` and `boss-destroyed`. The game draws the boss in plain shapes
  (`BossLooks`, no concept cut exists) with the bar at the top of the play field, a coin shower
  and the existing Sfx; the mission failed screen offers *Retry from boss* on easy and medium. The
  Level 01–04 replay hashes are unchanged (the new hash fields are added only with a boss or an
  arena). Tests: `BossTest` (sim) and `BossLevelTest` (Level 01's data cut at 150 s with the
  frigate's arena, loaded at Level 05's place: the specs, an autopilot kill on every difficulty,
  determinism).
- 2026-10-03: M4 part E, Level 05 playable: `Lob` (the mortar's blob in `EnemyForce`, marker fixed
  where the ship was, the ring starting 12 px outside the impact circle, the direct hit applied in
  `hitShip`), `Sled` (time-driven like `Crane`: lights, run, one strike per sled, blocking shots and
  bullets, shutting the stuck sled's trigger while lit), thrown rocks as `Debris.toss` (a life, a
  random drift, breaking on the hull), `LevelScript.targets` / `sled` / `rocks` / `groupDrops` and
  `Secondary.killAll` / `label`; `GroundUnit.group` indexes `LevelScript.groups()` (the primary's
  targets or the secondary's groups). The voice speaker table gains `uncast: true` (no `ref`; the
  speaker's lines have no voice file, VoiceFilesTest allows that only for such speakers). The
  Level 01–04 replay hashes are unchanged: the new fields join the state hash only when lobs are
  in flight or the level has sleds or targets. The debug option `--invulnerable` (captures) also
  keeps a lost destroy-targets group from failing the level. Tests: `CraterNestTest` (sim), `Level05Test`
  (budget, totals, difficulties, the balance plan's fit completing every difficulty), PacingTest
  and RadioTimelineTest over Level 05.
- 2026-10-04: Per-level unit atlases (user decision; design/art-direction/production, Budgets):
  `packAtlases` stages the sprite frames per atlas from `SpriteUse` (shared, or the one level that
  uses them) and packs `sprites` plus `level-NN`; `AtlasBudget` counts a level's unit atlas with
  its backdrop pages and prints every atlas's fill. `Sprites.enterLevel`/`leaveLevel` load and
  dispose a level's unit atlas around `LevelScreen`; `LunaLooks` looks up the mortar and sled
  sprites only in a level that has them.
- 2026-10-04: Economy rework: the level data's optional `bounty_scale` (`LevelData.bounties()`,
  default 1) reaches the sim as `ScoringRules.bountyScale`; `Tally.bounty` pays kills, parts,
  ground units, destructibles' bounties, bursts and shot mines × credit factor × scale with one
  rounding, half to even. The economy's `typical_player` shares (`EconomyData.TypicalPlayer`)
  drive the typical haul in the credit-budget tables and the content test helper `TypicalHaul`.
- 2026-10-04: M4 part F step 2, Level 06's mechanics (main-agent brief; code choices): a segment
  chain is a pooled `Chain` (fixed arrays: a 512-point path history with arc lengths) whose
  members are ordinary pooled `Enemy` units in the `CHAIN` phase with their own hit boxes
  (`Enemy.hitbox()`, which every hit test now reads); the chain's head point flies the wave's
  `FlightPath` and its loop-back (`Spawn.Loop`, warned on the bottom edge by `WaveSchedule`), the
  members sit on the history at their offsets. A cut of an uncut wave's chain moves the rear
  members to a new chain that holds 0.6 s while its head grows (the head is made only if a rear
  member is still alive then, so parts killed together do not regrow) and then lunges straight at
  where the ship was; any other cut and the head's death pop the members behind it one by one
  (`CHAIN_POP`, paying nothing). The Mantis's sweep is `EnemySpec.Sweep` on the hovering unit
  (`SWEEP_TELEGRAPH`, `SWEEP_FIRED`, `SWEEP_HIT`), the beam a thickened segment against the hull
  (`Hull.touchesSegment`). The darkness (`LevelScript.Darkness`) is deterministic from the clock
  and the ship: the sim uses only the headlight cone and the flare pools (a `dark` trigger is shut
  outside them); the game's `FarsideLooks` renders a light map (an FBO cleared to the ambient
  light, additive pools for the static lights, the headlight cone, the flares and the shots)
  multiplied over the ground layer and the ground units, then the turrets' and mortars' glows,
  the flare shells, the sweeps and the Smart Bomb. The Smart Bomb is a second kind of
  `SpecialSpec` (`SmartBombSpec`); `SpecialSlot.bomb` grows the ring, clears bullets and lobs and
  deals its damage once per target (the ledger), `Defences.guard` makes the ship invulnerable. A
  data core is a secret whose trigger drops `PickupType.DATA_CORE`; `LevelResult.dataCores`
  carries it to `Campaign.complete`, which records the core and its unlock. Every new piece of
  state is hashed only where it exists, so Levels 01–05 replay with their hashes.
- 2026-10-05: Round 23's close: a trigger may name a `sprite` of its own (Level 06's survey cache
  and terminal; `LevelRenderer` draws its hit and spent frames, its `-glow` after the light pass
  until spent and its `-glint` only while `Sortie.lit`, and gives a `dark` object no loot
  sparkle); `SpriteUse` claims such a trigger's sprite for its level; `LevelScreen` gives
  triggers no break-apart or wreck. The voice speaker table's `filter: pa` maps a speaker's radio
  lines to `VoiceLines.Filter.PA` (in the key; `tools/art/voice.py` posts them with `pa()`).
  `FlightSounds` plays the round's Mantis, flare and regrowth sounds (`Sfx`), the flare's burn
  loop as back-to-back plays over the level's `flareSeconds`. No simulation change: the replay
  hashes stay.
- 2026-10-05: Schema text for M4 part G (Level 07 and the act end), marked "planned (part G)" until
  the loader reads it: the boss keys (`engages_on_arrival`, `death_seconds`, `poses`, a phase's
  `until.seconds`, `delay`, `move` and `windows` with `spawns`; fire-only `armoured` parts; a
  boss `fan`; a spiral without `duration`; the hooks `arms` and `spawns`), the level keys (`tows`,
  a wave change's `hold`, a pickup's `skip`, `dropped_by.parts`, radio `requires: homing`,
  `requires_not` and a boss-phase cue's `timeout`, the `parts`/`before` secondary, music
  `boss_warning` and `boss_track`), the act's `outro` and the save's `stats.levels`. The level
  and act names follow the code the part's mechanics agents wrote alongside; the boss block's
  YAML names map onto `BossSpec`'s `Pose`, `Move`, `Windows` and `Spawn`. The `boss-destroyed`
  cue's empty subject (`SimSpecs` against the boss slug the `Sortie` cues) is the bug part G
  fixes.
- 2026-10-05: Master limiter (M4 part G): the game's mix went over full scale (peaks 1.2–1.59 in
  Levels 05 and 07). OpenAL Soft does the summing, so a limiter of our own would need the mix
  rendered on a loopback device and replayed through a second device (about 20 ms more latency on
  every sound, and libGDX's device handling replaced); OpenAL Soft's output limiter, off by default
  for float output, is turned on instead (`LimitedAudio`, `OutputLimiter`). Measured by replaying
  the recorded unlimited mixes through it on a loopback device: peaks 1.00, unchanged until the
  first over (1 ms late), afterwards on average 0.1 dB lower for a few seconds (see
  [audio](../../audio/README.md#master-limiter)).
- 2026-10-05: M4 part H docs reconciliation: the asset pipeline (placeholder import, angle sets,
  atlases with the per-level budget) is built; ticked.
- 2026-10-05: M4 part H balance tests: `BalanceTest` (content) buys the balance plan visit by visit
  through the shop's own rules (`Hangar`, the test helper `BalancePlan`) with each level's typical
  haul as income and checks the purchases are in the shop, affordable and within the generator's
  output, the plan's fit against the reference DPS (0.75–1.33 ×, Levels 01–03 up to 1.75 × as the
  accepted onboarding exception), and every enemy's time to kill at its first level and bounty
  against the balancing basis (bosses and `huge` set pieces at the effective 0.6 × DPS; a list of
  deviations pending a decision). `ActPlaythroughTest` flies Act 1 as one campaign per difficulty
  with the test autopilot (now public): the plan bought at each visit, repairs, retries as the
  mission failed screen offers them, a save round trip around every visit, and the act's outro
  after Level 07; about 2 s for all three. `tools/balance.py` lost its `balance-data.json` hook,
  reads the enemy and level data and exits 0.
- 2026-10-05: M4 part H: the armour data gained the low-armour `radio` line (`ArmourData.radio`, a
  `LevelData.RadioLine`); `Defences` sets off `SimEvents.Type.ARMOUR_CRITICAL` once per attempt
  at `Defences.CRITICAL_SHARE` (15 %, shared by the game's `LowArmour`) and hashes its flag only
  once set, so replays that never get that low keep their hashes; the level screen queues the line
  urgent and `VoiceLines` lists it (source `armour`).
- 2026-10-06: Schema text for M5 part A (Rook and the escort slot, the Act 2 weapons, the
  Targeting computer and the Salvage scanner), marked "planned (M5 part A)" until the loader reads
  it: the wingmen data file (written with its loader `WingmenData`, since the loader rejects a data
  file it does not know), the weapons' `accelerate`, `ports`, `drift`, `arm`, `trigger` and `slew`, the utility
  modules' `targeting` and `salvage` blocks, and save format version 3 with `escort`.
- 2026-10-06: M5 part A, the Act 2 weapons (built; the weapons' schema text above is no longer
  "planned"). `sim`: `WeaponSpec.Delivery` gains `TURRET` (aimed by `PlayerFire` per mount at its
  slew, the weapon's `turnRate`, hashed only for turret mounts) and `MINE` (a `Shot` that drifts,
  arms and holds its screen position; `PlayerFire.hitGround` sets mines off and bursts them;
  `WeaponSpec.Mines`), and accelerating shots (`endSpeed`, `accelSeconds`; `Shot.speed()`); the
  old 18-argument `WeaponSpec` constructor stays for weapons without them. `content`: `WeaponData`
  reads `accelerate`, `ports`, `slew`, `drift`, `arm`, `trigger` (`hits: area` = mines);
  `SimSpecs.weapon` builds the ports' muzzles and the one turn rate. `game`: `WeaponLooks` gives
  the mines their pulse and blast and the Hornet its smoke trail (`LevelScreen`, `TestFireView`).
- 2026-10-06: M5 part A, the Targeting computer and the Salvage scanner (built; their schema text
  above is no longer "planned"): `SystemsData.Utility` reads `targeting` (`turn_bonus`,
  `bar_seconds`, `bar_fade`) and `salvage` (`bonus` per level); `SimSpecs.fliesUtility` flies both.
  `sim`: `Loadout.salvageBonus` (0 without a scanner) multiplies a salvage pickup's and a hidden
  crate's payout before its one rounding (`Tally.earn` with a factor); `Enemy.ticksSinceHit` counts
  the steps since a unit's last damage for the HP bar, outside the state hash like its facing, so
  every replay hash stays. `content`: `SimSpecs.loadout` takes the turn bonus (the Targeting
  computer's share) and raises the Stormhawk's weapons' `turnRate`; `SimSpecs.wingman` builds Rook's
  guns at the base turn. `Flight` reports `targeting` and `salvage` for the renderer. The act HP
  factor: `SimSpecs.level` passes the level's act and number to everything it builds
  (`SimSpecs.actHpFactor`; `SimSpecs.enemy` and the public `setPiece`/`boss` without a level apply
  none). `game`: `TargetingOverlay` (bars and brackets, after the front scenery, under the spore
  mines and bullets) and `SecretGlints` (before the pickups), handed to `LevelRenderer.modules`;
  both draw from the simulation's state and keep no per-unit state. The `--loadout` debug option
  takes `utility` and `utility2` slots (a module's name in lower case, hyphenated).
- 2026-10-06: The voice speaker table gains `stage` (`VoiceData.Speaker.stage`): a radio line that
  is only a stage direction plays that sound in place of a voice; `VoiceLines.stageSound` finds it
  and `VoiceLines.radioVoice` (a line's rendered file, else its stage sound) serves `Voices` and
  RadioTimelineTest alike. The Choir's is concept round 29's option a, provisionally, copied by
  `:pipeline:copyPlaceholderStageSounds`; option b since the round closed (2026-10-06).
- 2026-10-06: M5 part B (user decisions D1 and D4, and the gap defaults): the schema text gains,
  as planned (M5 part B) until built, the **tower pieces** of the backdrop (a `ground` piece's
  `tower` block: `height`, `wall`, `shade`; drawn per frame round the projection centre, never
  under a unit; scenery only, D1 = a), the radio event **`escort-first-kill`** and **`{side}`** in
  a radio line (D4 = a), the ambience key `earth-megacity`, and for the Creeper a walker's aimed
  fan, the fan's **`stagger`** and an authored hook `interval` on an ordinary attack. The
  `stagger` key is not in the Creeper's data yet: the loader rejects unknown keys, so the
  Creeper's README holds it as a proposal until the walker code reads it. The tower block is a
  proposal for the renderer task to settle with this text.
- 2026-10-06: M5 part B built the Creeper's walker schema (a walker fan's `aim`, the fan's
  `stagger`, the authored hook `interval` on an ordinary attack; `stagger` is now in the Creeper's
  data), the radio event `escort-first-kill`, `{side}` in a radio line and the ambience key
  `earth-megacity`; their schema text is no longer marked planned (the tower pieces are the
  renderer task's).
- 2026-10-06: M5 part B built the **tower pieces** (D1 = a), settling the proposal: the roof's
  image is drawn at its scale (footprint × *k*, never resampled), not at the footprint's size; no
  `mirror` on a tower (symmetry rule: the roof and the walls' shading are lit from the upper
  left); wall rows follow the true perspective up the wall; the projection centre is (240, 297)
  from the top left, y 243 in the play field's y-up coordinates; a tower is on screen for the
  checks while its footprint or its roof is. The optional **`repeat`** helper of a placed piece
  (M3, traffic streams) came with it. `BackdropData.Tower`, `BackdropCheck`, `TowerProjection`;
  tests `BackdropDataTest`, `TowerProjectionTest`.
- 2026-10-07: M5 part B: the billboard's topple (Level 08) takes the default, a destructible's
  break. The hit that spends any ground trigger raises `TRIGGER_SPENT` (its script index; events
  are not hashed, so the replay hashes stay); `LevelScreen` and `FlightSounds` break only a
  trigger `TriggerBreak` names (a spent frame that is a wreck: the billboard): the small explosion
  on the ground, the small blast and its size's crumble (large at 72×40). Level 06's survey cache
  (shot open) and terminal (core released), the beacon, the lifeboat lights and the sled's clamp
  stay quiet. Tests `TriggerSpentTest`, `TriggerBreakTest`, `TriggerBreakSoundsTest`.
- 2026-10-07: Schema text for M5 part C (Level 09; user decisions D1–D11 of 2026-10-07 and the
  stated defaults), marked "planned (M5 part C)" until the loader reads it: for enemies,
  `armour: hardened` flown for a unit, the periodic `spawn` (`every`, `shut_within`; no `after` or
  `burst_bounty`), the attack pattern `pounce` (`interval`, `pounce`: `range`, `leap`, `air`,
  `scale`) with a unit's current layer, and the formation `pack` (in the basis's vocabulary now);
  for levels, `holds` and the level clock in script time (D2 = a, D3 = a), the `collapse` (D5 = a),
  a wave's `tag` and a tagged `escapes` secondary (D6 = a), the radio events `hold-start`,
  `first-pounce` and `collapse` and `requires: escort`, the threat profile's `required` (D7 = a),
  the music's `full_on`; for the backdrop, the real clock for paths and drift. The Hive Node's and
  the Ravager's data files hold only the keys the loader knows; the new ones wait in a comment there
  (the loader rejects unknown keys), as the Creeper's `stagger` waited in its README in part B.
- 2026-10-07: M5 part C, round 31's collapse look c (user): the `collapse` block's `sweep` gives way
  to `drop` and `blast` (`seconds`, `from`, `to`): the tower leans for `warning`, drops for `drop`,
  and from the impact the kills spread outward from its foot on a ring that rides the dust blast's
  front (Level 09: 1.5 s lean, 1.5 s drop, impact at 3.0 s, radius 100 → 390 px in 1 s), the same
  band and pay; new event `COLLAPSE_IMPACT`; `COLLAPSE_END` (and a hold through the collapse) at the
  blast's end. The old key is rejected by the strict loader.
- 2026-10-07: M5 part C (user): a hold through the collapse lasts until the collapse's dust has
  settled (the impact + `dust.seconds`; Level 09: 9.0 s after the warning starts), no longer until
  `COLLAPSE_END`, so with late kills the rubble heap is not scrolled away under the dust. The
  simulation's `LevelScript.Collapse` carries the dust's seconds (`settleSeconds`, at least the
  blast's; `content` checks it); the events are unchanged (no settle event: the music's `full_on:
  collapse` stays on to the level's end).
