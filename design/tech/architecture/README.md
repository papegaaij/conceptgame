---
title: Architecture
design: approved
implementation: in-progress
art: n/a
depends-on: [.., ../../player, ../../enemies, ../../campaign]
updated: 2026-10-02
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

The spike branch stays as a reference and is never merged.

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
| Act | `design/campaign/<act>/data.yaml` (the act's levels, title card and act briefing) | the act's *Act intro and outro* quotes |
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
  `design/player/balance-plan.yaml`, read only by `tools/balance.py`.
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
- Balance checks (budget per level, DPS against the reference, time to kill, bounty) become
  JUnit tests over the loaded content, replacing `tools/balance.py`'s checks.

### Data file schemas

Keys are snake_case; times in seconds, distances and sizes in px, speeds in px/s, power in MW,
prices in credits. Every file starts with a comment that names its README and links here. Sizes
are `[width, height]`, ranges `[min, max]`, points `[x, y]`. `available` is a key of
`design/player/data.yaml`'s `availability` (`start`, `act N`) or an exact level (`L22`); the
first entry of a part's model list is the starter (price 0, `start`).

- **Weapon** (`player/weapons/<slug>/data.yaml`): `name`, `slot` (`front`/`rear`/`wing`),
  `traits`, `price` (0 = starter), `upgrade_base` (default: the price), `unlock` (level),
  `draw` (`[L1, L5]`), `sfx`, `vfx`, `hits`, `speed` (px/s or `[start, end]`; none when lobbed
  or dropped), `size`, `range` (px, `screen` or `drop`) or `lifetime`, flags `mirrored` (fires
  the same pattern to the left too; numbers per side), `pod` (numbers per pod) and `seek`
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
  `tier`, `size`, `hitbox`, `parts`, `orientation`, `hp`, `armour`, `speed`, `movement` (one
  entry per pattern: `snake` `spacing`; `swoop` `radius`, `top_speed`; `straight` `speed`;
  `hover` `seconds`, `y`; `orbit` `radius`, `turn_rate`), `attacks` (`pattern`, `bullet` class,
  `interval`, `speed`, `first_shot_delay`), `formations` (`name`, `size` `[n]` or `[min, max]`),
  `weak_points` (`name`, `multiplier`), `traits`, `bounty`, `first_level`, `difficulty` hooks.
  `drops` (`pickup`, `every`: every n-th kill of the enemy in a level drops it).
  Easy/hard HP in the table are derived from the difficulty levers (rounded half to even, at
  least 1), the
  contact and bullet damage from the basis, the score from the bounty.
  Basis (`enemies/data.yaml`): `reference_dps` per level, `bullet_damage`, `contact_damage`
  per tier, `formations` (name: description).
- **Level** (`campaign/<act>/<level>/data.yaml`): `scroll_speed`, `launch_seconds`,
  `control_prompts`; `sections` back to back from t = 0 (`name`, `end`, `atmosphere`, optional
  `speed`, the backdrop's `tiles` (tile set ids, at most one per layer); scroll distances are
  derived); `waves` in time order, one row each (`t`,
  `formation`, `enemy` slug, `count`, `from` `front`/`sides`/`rear`, `edge`
  `left`/`right`/`alternating` (`sides` without an edge enters from both side edges), optional
  `hold`, `warning`, `break_group`, `speed` (px/s instead of the enemy's own), `interval` (s
  between the units of a stream), and `easy` / `hard` changes), a mixed wave lists `groups`
  instead; `ground_targets` (`target`, `section`, `layer`, `size`, `at` (one `[t, x]` per object:
  when it enters at the top edge and its x), a destructible's `count`, `hp`, `bounty`, `drop`, or
  a trigger's `hits`, `reveals`); `secrets` (`name`, hidden `crate` credits, `radio` line); placed
  `pickups` (`pickup`, `dropped_by` wave and unit); `radio` cues (trigger `t` or `event`
  `first-kill` (with `enemy`) / `secondary-objective` / `level-end`; `speaker`, `line`,
  `distorted`, and `easy` / `hard` changes giving another `line`, as when a wave enters elsewhere on
  that difficulty); `objectives` (`primary`, `secondary` `kill_ratio` and `credits`); `music`
  (`track`, `start_section`, optional `start_db` (the theme's level through its start section,
  rising to full at the next), `full_section`, `ambience`, `end_jingle`); `difficulty` (level-wide
  `easy` / `hard` enemy changes such as `burst`, and `extra_pickups` placed like `pickups`);
  `threat_profile` (the hangar intel: `setting`, `layers`, `density` 1–5, recommended `traits`,
  `hazards`, `boss`, optional `specials` limits, `varga` lines per sensor level `none`/`l1`/`l2`/
  `l3`, and `notes` with the *Threat profile* rows, where `{directions}` is derived and the other
  `{fields}` come from the profile); `briefing`
  (`pages` of `speaker` and `line`, and the hangar `teaser`, rendered into *Briefing*); `backdrop`
  (presentation only, see below). The credit budget
  table is derived: kills × bounties, ground targets, crates, the secondary objective, against
  budget(n) of the economy; the attack directions are each entry's share of the enemies.
- **Level backdrop** (`backdrop` in a level's data file; the *Backdrop* table is rendered from it):
  `scroll_factors` per layer (`deep`, `far`, `ground` = 1.0, `low-air`, `high-air`); `ramp` (s an
  atmosphere change takes, centred on the section boundary); `haze_colour` (`rrggbb`);
  `atmosphere` per intensity the level uses (`clear`/`light`/`medium`/`heavy`: optional `banks`
  tile set on low-air, optional `wisps` tile set on high-air, `haze` 0..1 over deep and far);
  `tile_sets` by id (`layer`, `height`; 480 px wide, repeating along the layer; optional `drift`
  px/s sideways, wrapping); `pieces` by id (`layer`, `size`, an animation's `frames` and `fps` or
  an angle set's `headings`, `mid_size` if it counts towards the density rule); `placed` set
  pieces (`piece`, `t` when its centre passes the middle of the screen, `x` in the play field,
  optional `mirror`, a `path` of `[t, dx, dy]` waypoints for a piece with headings; later pieces
  on a layer are drawn over earlier ones). A layer position is the layer's scroll (the ground's
  scroll × its factor) plus the screen height; a section's tile set begins at the seam that enters
  at the top edge when the section starts. The images are `assets/backdrop/level-NN/<id>.png`
  (frames and headings `<id>_<n>.png`). `content` checks that the ids and layers resolve and,
  sampled every simulation step, the art direction's density (at most 3 mid-size set pieces on
  screen) and motion budget (at most 2 strongly animated elements: animated or moving pieces,
  drifting tile sets, the atmosphere's banks counting as one; nothing moving faster than 120 px/s,
  about 2 px per frame, on its own).
- **Ship and core parts**: ship (`acceleration_seconds`, `stop_seconds`, `precision_factor`,
  `size`, `edge_gap`, `hull` (hit boxes `[x, y, width, height]` from the sprite's top left), `collection_radius`, `mercy_seconds`, `bank_change_steps`,
  `mounts`; its speed is the fitted engine's); shields (`break_seconds`, `models` with
  `capacity`, `regen`, `delay`, `draw`); armour (`plating` with `max`); generator
  (`spare_power`, `models` with `output`); systems (`engines` with `speed`, `draw`; `utility`
  with `draw`, one price per level, `design` status); specials (`charge_price`, `max_charges`,
  `unlock`). Each model also has `name`, `price` and `available`.
- **Player** (`player/data.yaml`): `availability`, `pickup_seconds`, `pickup_drift_speed`, `pickups` (salvage
  credits, overdrive `levels` and `seconds`, shield cell `shield_percent`, armour patch
  `armour`, special charge `charges`, data core). Levels name pickups as `small salvage`,
  `armour patch`, ….
- **Systems**: economy (`starting_credits`, `budget` `base` and `growth`, `act_factor`,
  `sell_back`); difficulty (one `{easy, medium, hard}` entry per lever: factors such as
  `enemy_hp`, changes such as `formation_size` (−0.2 = −20 %), `aimed_spread_degrees`, `bullet_budget`,
  `repair_cost`, `retries`, `boss_checkpoint`, `sensor_bonus`); scoring (`kill_score`,
  `pickup_score`, `chain`, `rating` weights, `bonuses`, `grades`); retry (`armour_floor`, the
  share of the maximum armour a retry starts with at least).
- **Act** (`campaign/<act>/data.yaml`): `levels` `[first, last]` (global numbers), `title_card`
  (`act`, `name`, `line`), `briefing` (pages of `speaker` and `line`); the loader checks that every
  level's act exists and includes it.

### Presentation (`game`)

- A **screen state machine** for the flow in [ui](../../ui/README.md): title, menu, difficulty,
  briefing, hangar, level, pause, debrief, options, credits.
- One **UI kit** (glass panels, metal HUD, bitmap fonts 8×12 / 10×20 / 20×30) shared by all
  screens; keyboard and gamepad navigation everywhere.
- Rendering into the 960×540 `PixelScreen`, integer-scaled with letterboxing or sharp-bilinear;
  parallax layers per the [art direction](../../art-direction/README.md); shaders stay
  GLES 2 compatible (the ANGLE fallback on macOS).
- Audio: the streaming music player with intro and loop points, and the SFX bank with 64
  sources.

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
  build when one is exceeded (`AtlasBudget`).

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
- [ ] Balance checks ported to JUnit
- [ ] Asset pipeline: placeholder import from chosen concept art, angle sets, atlases
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
