---
title: Tech stack
design: approved
implementation: not-started
art: n/a
depends-on: [../art-direction, ../audio, ../ui/controls]
updated: 2026-10-02
---

# Tech stack

## Summary

The technology the game is built on. The game is built with **libGDX on Java, with Gradle**,
code-first, desktop only (Linux, Windows, macOS), released as open source under Apache-2.0.
The choice was confirmed by a throwaway spike measured against the gates below. The design tree stays
engine-agnostic; this section is the only place that names engine APIs.

## Contents

| Part | Summary | Design | Impl | Art |
|---|---|---|---|---|
| [architecture](architecture/README.md) | Module layout, simulation, data files next to the documents, testing, conventions, CI | approved | in-progress | n/a |
| [roadmap](roadmap/README.md) | Seven milestones from skeleton to the Acts 1–2 release | approved | in-progress | n/a |

## Design

### Requirements (user, 2026-10-01)

- Desktop/laptop only (no phone, no web); runs on Linux, Windows and macOS without much trouble.
- Performance matters; libraries must be mature and stable, a good basis for quality software.
- Testable.
- Language: Java or Kotlin. C# rejected (hard to get working properly on Linux); C++ and Rust
  rejected (too low-level).
- Workflow: Claude writes most of the code, the user reviews. Code-first framework, no editor.
- Distribution: free / open source release (itch.io, GitHub). No console ports planned.
- Testing hardware: Linux only; Windows and macOS are covered by CI.

### Candidate funnel

| Candidate | Outcome | Reason |
|---|---|---|
| **libGDX (Java)** | **Shortlisted** | Mature (10+ years), Apache-2.0, code-first, LWJGL3 desktop backend, headless backend for tests |
| Godot 4 | Dropped | Editor-centric; the typed language path is C# |
| MonoGame / FNA, Unity | Dropped | C# |
| SDL3 + own engine, Bevy | Dropped | C++ / Rust; Bevy is also pre-1.0 with frequent breaking changes |
| LWJGL3 without libGDX | Dropped (user) | libGDX already is the thin layer on LWJGL3; own layer means more code to write and test |
| KorGE | Dropped | Small team, breaking changes |
| FXGL / JavaFX, LITIENGINE (Java2D) | Dropped | Not built for hundreds of blended sprites per frame |
| jMonkeyEngine | Dropped | 3D-focused |
| Go + Ebitengine | Dropped (user) | Outside candidate; a new language to review |

### Decided choices

| Topic | Choice |
|---|---|
| Language | Java 21 (LTS) with libGDX 1.14.2; move to Java 25 once libGDX 1.14.3 (LWJGL 3.4) is released |
| Framework | libGDX, desktop backend (LWJGL3), pending the spike |
| Build | Gradle (Kotlin DSL build scripts with the wrapper); packaging with Construo |
| CI | GitHub Actions, build + tests on Linux, Windows and macOS |
| Licence | Apache-2.0 for the repository (the existing `LICENSE`); third-party files keep their own licences, recorded in [CREDITS.md](../../CREDITS.md) |
| Garbage collector | Generational ZGC (`-XX:+UseZGC -XX:+ZGenerational` on Java 21) |

### Desk research findings (2026-10-01)

Sources are recorded in the evaluation session; the key URLs are listed per finding.

| Topic | Finding | Consequence |
|---|---|---|
| Activity | Latest release 1.14.2 (2026-06-05); 2–3 releases a year; ~150 commits/year from ~50 authors, small core team (Nathan Sweet, obigu, Berstanio, Tommy Ettinger, …) | Mature and alive, but slow-moving |
| Shipped games | Slay the Spire, Space Haven, Delver, Shattered Pixel Dungeon (active, on 1.14.0) | Proven for commercial 2D desktop games |
| Java 25 | 1.14.2 ships LWJGL 3.3.3, which warns on Java 25 (JNI version, `sun.misc.Unsafe`; libGDX issue #7713). Master (unreleased 1.14.3) moves to LWJGL 3.4.3, which uses the FFM API on JDK 25+. Jars are Java 8 bytecode without module names (classpath only) | Java 25 cleanly needs libGDX 1.14.3; until then Java 21 LTS |
| Maven | All artefacts are on Maven Central; Maven is "possible but not officially supported"; the archetype is dead (2019); LWJGL natives come in transitively for every platform (trim with exclusions), libGDX natives must be declared | Reason to switch to Gradle (user decision) |
| Packaging | Construo (recommended, cross-builds all OSes) is Gradle-only; packr is stale; jpackage needs one runner per OS; JReleaser's Maven jlink assembler cross-builds runtimes for non-modular apps. A trimmed JRE is ~36 MB (~10–14 MB zipped) | Construo with Gradle; jpackage per runner as fallback |
| macOS | Apple Silicon supported; `-XstartOnFirstThread` handled by a relaunch helper or 1.14's `useGlfwAsync()`; OpenGL frozen at 4.1; ANGLE backend (GLES 2.0 on Metal) exists as a fallback | Works today; ANGLE is the escape hatch if Apple removes OpenGL. Notarization needs a macOS runner and an Apple account |
| Testing | The headless backend mocks audio and returns no GL; no official screenshot-test approach | Confirms principle 1: the simulation must not depend on libGDX |
| Audio | Desktop music loops gaplessly (OpenAL streaming, 3 × 40 KB buffers fed from the main loop); no intro/loop-point support; 16 simultaneous sources by default (configurable) | Raise the source count; build intro + loop sections ourselves; long frame stalls could underrun music |
| Gamepads | gdx-controllers 2.2.4 (2025-06) on Jamepad (SDL2, GameControllerDB mappings, natives from 2023), hot-plug improvements | Works but slowly maintained; a moderate risk |
| GC | libGDX advice is pooling; no JVM flag guidance. Generational ZGC is the only ZGC mode since JDK 24 | Pools plus ZGC; gate 2 measures it |
| Future | No official Vulkan/WebGPU backend; community gdx-webgpu (~0.8) replaces classes rather than the backend | OpenGL remains the rendering path |

### Risks

1. Apple removes OpenGL: mitigated by the ANGLE backend (GLES 2.0 only, so shaders must stay
   GLES 2 compatible).
2. Java 25 support waits on the 1.14.3 release (date unknown).
3. Small core team and slow releases; gamepad library maintained slowly.

### Architecture principles (proposal)

These make the game testable and are what the spike has to demonstrate:

1. **Simulation separate from presentation.** A pure-Java simulation module (no libGDX
   dependency) owns the game state: entities, movement, bullets, collisions, damage, scoring,
   economy. A libGDX module renders it, plays audio and feeds input into it.
2. **Fixed 60 Hz timestep**, rendering interpolated between simulation steps.
3. **Determinism.** Seeded random generator per level; the simulation uses `StrictMath` (Java
   floating point has been strict on every platform since Java 17, but `Math` intrinsics may
   differ per CPU). The same input recording gives the same state hash on every OS.
4. **Replay tests.** A recorded input stream plus a level gives an expected outcome (state hash,
   score, credits, armour); these run headless in JUnit on all three CI runners.
5. **Data-driven content.** Levels, enemies, weapons and economy numbers live in data files
   derived from the design tree, so balancing is a data change with a test, not a code change.
6. **No allocation in the frame loop.** Object pools for bullets, particles and effects; the
   spike measures garbage-collection pauses.

### Spike gates (proposal)

A throwaway prototype, about one level slice, measured on the development machine (Linux,
RTX 2070) and built and tested by CI on all three OSes:

| # | Gate | Pass when |
|---|---|---|
| 1 | Rendering load | 7 parallax layers, 150 enemies, 500 bullets and particles with additive glow, 960×540 integer-scaled to 1920×1080: 99th-percentile frame time under 16.7 ms |
| 2 | Garbage collection | No GC pause over 2 ms during a 5-minute run |
| 3 | Angle sprites | A 768-frame angle set (Halo Platform size) loads from texture atlases; video memory use is measured and rotation is smooth |
| 4 | Music | A round-08 track loops sample-exactly (no gap, no click), including an intro section that plays once before the loop |
| 5 | Sound effects | 32 simultaneous effects (source count raised from the default 16) without dropouts; shot-to-sound latency acceptable |
| 6 | Input | Keyboard and a gamepad, gamepad hot-plugging |
| 7 | Tests | A headless JUnit replay test gives the same state hash on Linux, Windows and macOS in CI |
| 8 | Packaging | Gradle with Construo produces a runnable bundle with a trimmed JRE for each OS; the Linux bundle starts on a clean machine |
| 9 | Code quality | The user finds the spike code and tests pleasant to review |
| 10 | Display modes | Borderless full screen ↔ resizable window toggled at runtime (Alt+Enter, F11) without losing textures, audio or game state; letterboxed integer scaling correct in both (see [options](../ui/options/README.md)) |

### Spike results (2026-10-01)

The spike's code and `spike/RESULTS.md` with the full numbers are kept under the tag
`spike-libgdx` (`git checkout spike-libgdx`); its branch was deleted after M3. CI run:
[36916102595](https://github.com/papegaaij/conceptgame/actions/runs/36916102595).

| # | Result |
|---|---|
| 1 | Pass: 5,335 fps uncapped, p99 frame time 0.49 ms at full load, scaled to 1920×1080 |
| 2 | Pass with generational ZGC (longest pause 0.012 ms in 5 min); G1 just misses (2.26 ms first young GC). Use ZGC (`-XX:+ZGenerational` on Java 21) |
| 3 | Pass on memory: 768 frames of 224 px = 10 atlas pages of 2048², 160 MiB video memory. Exploiting the 6-fold symmetry (128 frames) would cost ~27 MiB. Smoothness: user check pending |
| 4 | Pass (automated): intro once then loop, sample-identical across two seams (headless test). Listening: user check pending |
| 5 | Pass: 64 OpenAL sources, bursts of 32 and 16,175 plays without a failure. Latency: user check pending |
| 6 | Manual: keyboard and gdx-controllers wired with hot-plug logging; gamepad test pending |
| 7 | Pass: replay hash `e17610307e1c81f0` identical on Linux, Windows and macOS in CI (also on Java 25 locally) |
| 8 | Pass: Construo cross-builds all four bundles on Linux in ~25 s, 112–116 MB zipped (the atlas is 64 MB of it); Linux bundle runs with its own runtime. Clean-machine test pending |
| 9 | Pass: approved by the user |
| 10 | Pass: Alt+Enter / F11 switch a 1920×1080 window ↔ borderless full screen 3840×2160 at runtime; simulation, audio and textures continue; 2–4 slow frames (33–67 ms) per switch. Approved by the user |

Lessons for the real project: `StrictMath.sin` allocates on Java 21 (use a lookup table filled
from it); Construo needs a fat jar; gdx-tools pulls in the LWJGL 2 backend (keep it in a tools
module); the LWJGL3 audio buffer size is in bytes; `AudioDevice` and sound effects share one
non-thread-safe source pool; `Sound.play` allocates.

## Implementation

- [x] Desk research on libGDX (versions, Java 25, Maven, macOS, packaging, audio, input)
- [x] Spike built, gates 1–10 measured and reported here
- [x] Decision approved by the user
- [ ] Project skeleton planned in [architecture](architecture/README.md) and [roadmap](roadmap/README.md)

## Open questions

- None open.

## Decisions

- 2026-10-01: Evaluation started. Desktop only, Java (current LTS), code-first, Maven, CI on
  all three OSes, free / open source release. libGDX is the only candidate evaluated
  (user decision); see the candidate funnel for the dropped options.
- 2026-10-01: After the desk research: the spike uses Java 21 with libGDX 1.14.2 (Java 25 needs
  the unreleased 1.14.3); the build is **Gradle** instead of Maven, because Maven has no
  official libGDX support and Construo, the cross-building packager, is Gradle-only. The spike
  lives on the branch `spike/libgdx` (never merged; findings are recorded here on `main`).
  Spike gates 1–9 accepted as written.
- 2026-10-01: Spike approved by the user: **libGDX 1.14.2 on Java 21 with Gradle** is the tech
  stack (Java 25 once libGDX 1.14.3 is released). Second CI run green on all three OSes.
- 2026-10-01: Licence **Apache-2.0** (user decision), matching the repository's `LICENSE`.
- 2026-10-02: The `spike/libgdx` branch was deleted (user decision); the spike is kept as the signed tag `spike-libgdx` on its last commit.
