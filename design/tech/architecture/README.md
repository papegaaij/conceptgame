---
title: Architecture
design: approved
implementation: in-progress
art: n/a
depends-on: [.., ../../player, ../../enemies, ../../campaign]
updated: 2026-10-01
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

The documents already describe every number in tables; the code needs them as data. To keep one
source, the numbers move into **YAML data files inside the part's directory**, and the tables in
the README are **rendered from the data**, extending what `tools/balance.py --sync` already does
for `design/player/balance-data.json`:

| Part | Data file | Rendered into |
|---|---|---|
| Enemy | `design/enemies/<category>/<slug>/data.yaml` (HP, size, layer, speed, attacks, bounty, angle set) | the stats table of its README |
| Weapon and other equipment | `design/player/.../<slug>/data.yaml` (replaces `balance-data.json`) | the per-level tables of its README |
| Level | `design/campaign/<act>/<level>/data.yaml` (sections, scroll, waves, ground targets, cues, budget) | the *Waves*, *Ground targets* and *Radio chatter* tables |
| Economy, scoring, difficulty | `design/systems/<part>/data.yaml` | their tables |

- `content` loads them with Jackson (YAML) into Java records and validates them: references
  resolve (a wave's enemy exists), values are in range, required fields are present.
- A sync tool renders the tables (`python3 tools/sync_tables.py`, which absorbs
  `balance.py --sync`), and `check_docs.py` fails when a table differs from its data. Prose
  stays hand-written; only marked tables are generated.
- Balance checks (budget per level, DPS against the reference, time to kill, bounty) become
  JUnit tests over the loaded content, replacing `tools/balance.py`'s checks.

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
- The `pipeline` module turns them into build output at build time: angle sets (using the
  symmetry rule), texture atlases, audio in OGG. Generated atlases are never committed.

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
- [ ] Data-file loader with validation; `sync_tables.py`; `check_docs.py` compares rendered tables
- [ ] Balance checks ported to JUnit; `balance-data.json` migrated to per-part `data.yaml`
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
