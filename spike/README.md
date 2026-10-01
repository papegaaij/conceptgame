# Terran Vanguard: libGDX tech spike

A throwaway prototype that measures libGDX 1.14.2 on Java 21 against the nine spike gates in
`design/tech/README.md` (on `main`). It is never merged; the findings go back into the design
tree. Measured results are in [RESULTS.md](RESULTS.md), raw reports in [results/](results/).

## Requirements

- JDK 21 (`JAVA_HOME` pointing at it). Gradle comes through the wrapper.
- The Git LFS files of the design tree: the build reads the Halo Platform sheet, the
  round-08 music track and the sound effects from `../design`.

## Modules

| Module | What | Depends on |
|---|---|---|
| `sim` | The deterministic simulation: fixed 60 Hz step, seeded `SplitMix64`, `StrictMath` plus a table-based `Trig`, pooled entities on parallax layers, collisions, damage, score, state hash, input recordings. **No libGDX.** | JDK only |
| `game` | libGDX presentation: 960×540 frame buffer integer-scaled to the window, parallax layers, interpolated sprites, additive glow, HUD placeholders, the angle-set Halo Platform, a sample-exact looping music streamer, SFX, keyboard and gamepad input, benchmark meters. | `sim`, gdx, gdx-backend-lwjgl3 (stb_vorbis, OpenAL), gdx-controllers |
| `desktop` | LWJGL3 launcher, command-line options, assets in the jar, Construo packaging. | `game`, natives, gdx-controllers-desktop |
| `tools` | Build-time generator for the 768-frame Halo Platform atlas (gdx-tools TexturePacker). Separate because gdx-tools drags in the old LWJGL 2 backend. | gdx-tools |

`buildSrc` holds the shared Java conventions (Java 21 release, `-Xlint:all -Werror`, JUnit 5);
versions live in `gradle/libs.versions.toml`.

Main classes worth reviewing first: `sim/World`, `sim/Pool`, `sim/Trig`, `sim/InputRecording`,
`game/audio/LoopingStream` + `MusicStreamer` + `VorbisFile`, `game/render/WorldRenderer` +
`PixelScreen`, `game/scene/PlayScene`.

## Build and test

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
./gradlew test             # sim + game unit tests, replay test, music loop test (headless)
./gradlew installDist      # desktop/build/install/terran-vanguard-spike
```

`./gradlew :tools:generateHaloAtlas` regenerates the angle atlas on its own; `installDist` and
`run` do it automatically (the atlas goes into the jar under `atlas/`).

## Run

```bash
./gradlew :desktop:run --args="--scene play"     # or start the installed script:
desktop/build/install/terran-vanguard-spike/bin/terran-vanguard-spike [options]
```

| Option | Meaning |
|---|---|
| `--scene play` | (default) the play field under gate-1 load; fly with arrows/WASD, fire with space/Z, or a gamepad (stick/d-pad, A) |
| `--scene halo` | the 768-frame Halo Platform turning at its base speed (0.35 rad/s) |
| `--scene sfx` | bursts of 32 simultaneous sound effects over the music, once a second |
| `--autopilot` | the simulation's autopilot flies and fires instead of the player |
| `--bench <s>` | after a 2 s warm-up, measure for `<s>` seconds, print a report and exit |
| `--no-vsync` | uncapped frame rate, for frame-time measurements |
| `--record <file>` | write the commands of the run as a replay (prints the final state hash) |

Gamepad connects and disconnects are logged (`[input] gamepad connected: …`).

## Benchmarks used for RESULTS.md

```bash
BIN=desktop/build/install/terran-vanguard-spike/bin/terran-vanguard-spike
# gate 1 (and the replay recording used by the gate-7 test)
$BIN --autopilot --bench 60 --no-vsync --record sim/src/test/resources/replay/autopilot-2185.rec
# gate 2, once per collector
JAVA_OPTS="-Xlog:gc*:file=build/bench/gate2-g1-gc.log:uptime,level,tags" $BIN --autopilot --bench 300 --no-vsync
JAVA_OPTS="-XX:+UseZGC -Xlog:gc*:file=build/bench/gate2-zgc-gc.log:uptime,level,tags" $BIN --autopilot --bench 300 --no-vsync
# gate 3 and 5
$BIN --scene halo --bench 20
$BIN --scene sfx --bench 10
```

Re-recording the replay changes the expected hash: copy the hash the game prints into
`ReplayTest.EXPECTED_HASH` (and the step count into `EXPECTED_STEPS`).

## Package

```bash
./gradlew :desktop:packageLinuxX64 :desktop:packageWinX64 :desktop:packageMacX64 :desktop:packageMacM1
ls desktop/build/construo/dist
```

Construo downloads a Temurin 21 JDK per target (pinned by SHA-256), trims it with jlink and
zips it with the fat jar (`:desktop:fatJar`) and the
[roast](https://github.com/fourlastor-alexandria/roast) native launcher, which starts the JVM
with generational ZGC (`-XX:+ZGenerational` is needed on Java 21). All four targets build on
Linux; macOS bundles are unsigned and not notarised.

## CI

`.github/workflows/spike.yml` (repository root) runs `./gradlew test` on Ubuntu, Windows and
macOS with Temurin 21, pulling only the two LFS files the build reads.
