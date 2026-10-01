# Spike results (2026-10-01)

Measured on the development machine: Linux (Ubuntu 26.04), NVIDIA RTX 2070 (driver 580.178.04,
OpenGL 4.6), OpenJDK 21.0.12, libGDX 1.14.2 (LWJGL 3.3.3), window 1920×1080 = the 960×540
frame buffer at 2×. The raw benchmark reports are in [results/](results/); how to reproduce
them is in [README.md](README.md#benchmarks-used-for-resultsmd).

| # | Gate | Result | Key number |
|---|---|---|---|
| 1 | Rendering load | **pass** | p99 frame time 0.49 ms (budget 16.7 ms) |
| 2 | Garbage collection | **pass with generational ZGC**; G1 marginal fail | ZGC max pause 0.012 ms; G1 max pause 2.26 ms |
| 3 | Angle sprites | **pass** (smoothness: manual) | 768 frames, 10 pages of 2048², 160 MiB video memory |
| 4 | Music | **pass** (automated); listening: manual | sample-exact over two loop boundaries |
| 5 | Sound effects | **pass** (latency: manual) | 384 + 16,175 plays, 0 without a source |
| 6 | Input | **manual** | hot-plug logging implemented |
| 7 | Tests | **pass** | replay hash identical on Java 21 and 25 locally and on Linux, Windows and macOS in CI |
| 8 | Packaging | **pass** (clean machine: not tested) | 4 bundles from Linux, 112–116 MB zipped |
| 9 | Code quality | for the user's review | see [README.md](README.md) |
| 10 | Display modes | **pass** (keys: manual) | 10 toggles window 1920×1080 ↔ full screen 3840×2160, state kept; 2–4 slow frames per switch |

## 1. Rendering load: pass

`--autopilot --bench 60 --no-vsync` ([results/gate1.txt](results/gate1.txt)):

| Frames | fps | p50 | p95 | p99 | p99.9 | max | frames > 16.7 ms |
|---|---|---|---|---|---|---|---|
| 320,074 in 60 s | 5,335 | 0.174 ms | 0.212 ms | 0.490 ms | 0.870 ms | 61.0 ms | 3 |

Load per frame (averages): 7 parallax layers (6 tiling textures plus the play plane, high-air at
40 % opacity), 150 enemies on the ground, sub, low-air and air layers (about 130 of them on
screen; the rest are entering or leaving), 334 bullets and 310 sparks = 645 additive glowing
sprites (each bullet is a glow quad plus a core quad, so about 980 additive quads), the
224 px Halo Platform, the HUD side panels, music streaming and about 53 (throttled) sound effects a second.

The three slow frames (and a 59.7 ms one in the 5-minute G1 run, 25.6 ms in the legacy ZGC run)
are sporadic: they do not coincide with GC pauses, did not reappear in a repeat 60 s run (max
3.4 ms) nor in the generational ZGC run (max 5.3 ms), and the desktop was in normal use during
the runs. With vsync on, all scenes hold 60 fps with no frame over 25 ms.

## 2. Garbage collection: pass with generational ZGC

Three 5-minute runs of the gate-1 load, vsync off (≈5,300 fps, so per-second allocation is
far above what 60 fps produces):

| Collector | Collections in 300 s | Longest pause | Frame max | Report |
|---|---|---|---|---|
| G1 (default heap 1 GB) | 2 young | **2.26 ms** (first young GC, 145 → 28 MB incl. start-up garbage); second 1.01 ms | 59.7 ms | [gate2-g1](results/gate2-g1.txt), [pauses](results/gate2-g1-pauses.txt) |
| ZGC generational (`-XX:+ZGenerational -Xmx256m`) | 30 | **0.012 ms** | 5.3 ms | [gate2-zgc-gen](results/gate2-zgc-gen.txt), [pauses](results/gate2-zgc-gen-pauses.txt) |
| ZGC legacy (`-XX:+UseZGC`, default heap) | 0 | none | 25.6 ms | [gate2-zgc](results/gate2-zgc.txt) |

A repeat 60 s G1 run had one young pause of 1.80 ms. The small heap in the generational ZGC run
is deliberate, to force collections: with the default 4 GB heap nothing is ever collected in
5 minutes.

Allocation rate: the render thread allocates **52 bytes per frame** at 5,300 fps (270 KB/s);
at 60 fps in the play scene it is about 1.4–2 KB per frame (85–115 KB/s), most of it inside
libGDX (`Sound.play` boxes `Long` ids into hash maps) rather than in the spike's code. The whole
process allocates about 0.25 MB/s (G1 log: 48 MB of young generation in 190 s). The simulation
itself allocates nothing per step (unit test `WorldTest.steppingDoesNotAllocate`).

Conclusion: G1 is close to the 2 ms limit; generational ZGC is far below it. On Java 21,
`-XX:+UseZGC` alone selects the **legacy** single-generation ZGC; generational needs
`-XX:+ZGenerational` (default from Java 23). The Construo launcher enables ZGC by default, so the
packaging config adds `-XX:+ZGenerational`.

## 3. Angle sprites: pass (smoothness to be judged by eye)

The round-07 sheet holds only one assembled Halo Platform frame (the concept's 768 frames were
rendered from a 3D model and exist only inside the GIF). `tools/AngleSetGenerator` (Gradle task
`:tools:generateHaloAtlas`) cuts it out, keys the panel background to transparency and rotates
it in 768 steps of 0.47° (bicubic), then packs it with gdx-tools TexturePacker. A stand-in: the
whole ring including the core and its drop shadow turns, which the production set will not do.

| Frames | Frame size | Atlas pages | PNG on disk | Video memory (`GL_NVX_gpu_memory_info`, before/after load) |
|---|---|---|---|---|
| 768 | 224×224 | 10 × 2048×2048 RGBA8888 | 64 MB | **160.0 MiB** (five loads: 159.9–161.4 MiB; one outlier 176.1) |

That is exactly 10 × 16 MiB: the driver stores the pages uncompressed with no mipmaps. Load and
draw cost nothing measurable (the halo scene runs at 60 fps; it is also in the gate-1 load).
**Worth knowing:** the ring is 6-fold symmetric, so 128 frames over one 60° step (as the concept
describes) look identical and need 2 pages ≈ 27 MiB; 160 MiB for one enemy would not scale.

## 4. Music: pass (automated); listening is manual

Approach: `VorbisFile` decodes the Ogg with LWJGL's stb_vorbis (already shipped with the libGDX
desktop backend), whose seek is sample-exact; `LoopPoints` reads `LOOPSTART` / `LOOPLENGTH` from
the Vorbis comments; `LoopingStream` fills each buffer and, when it reaches the loop end inside a
buffer, seeks back to the loop start and continues filling the same buffer; `MusicStreamer`
feeds it on its own thread into a libGDX `AudioDevice` (an OpenAL streaming source, 8 × 4 KB
buffers ≈ 186 ms), whose `writeSamples` blocks, so a long frame cannot starve the music.

Track: "Coalition Rising" (`coalition-rising-full-r08-a.ogg`), intro 340,772 frames (7.7 s),
loop 5,773,091 frames (130.9 s): the first loop seam is heard **2 min 19 s** after the start.

Automated tests (`MusicLoopTest`, `LoopingStreamTest`, headless):
- the stream equals a straight decode of the file frame by frame across two loop boundaries
  (11.9 million frames compared, odd buffer size 4093) — no gap, no duplicate;
- the audio after the loop end matches the audio after the loop start best at offset 0 (error
  0.8 % of the signal energy vs ≥ 4.5 % at ±1 sample), so the loop points line up with the
  decoded audio to the sample;
- the same logic with a synthetic source for buffer sizes 1, 7, 64, 1000 and 5000 frames.

## 5. Sound effects: pass

`DesktopLauncher` raises the OpenAL source count from 16 to 64. `--scene sfx --bench 10`
([results/gate5-sfx.txt](results/gate5-sfx.txt)) starts all 16 effects twice (two pitches),
32 at the same moment, once a second over the music: **12 bursts, 384 plays, 0 plays without a
free source**, frame time unaffected (max 16.8 ms with vsync). The 5-minute
play runs made 16,175 plays with 0 failures. Shot-to-sound latency and audible dropouts must be
judged by ear.

Note: libGDX's music and `AudioDevice` take their source from the same pool as the sound
effects, and claiming one is not thread-safe, so the streamer claims its source on the render
thread before its own thread starts.

## 6. Input: manual

Keyboard (arrows/WASD, space/Z) and gamepads (left stick or d-pad, A) via gdx-controllers
2.2.4 are merged into one command set per step. Present, connected and disconnected gamepads
are logged (`[input] gamepad connected: <name>`). No gamepad was attached during these runs; the
controller manager started without errors.

## 7. Tests: pass

`ReplayTest` replays a run recorded from the real game (`--autopilot --record`, 3,720 steps =
62 s) and compares the final state hash with the one the game printed live: `e17610307e1c81f0`.
It passes on Java 21 and Java 25 (Linux). 24 tests in total, all green (`./gradlew test`).
`.github/workflows/spike.yml` runs them on ubuntu-latest, windows-latest and macos-latest with
Temurin 21; validated with actionlint 1.7.12. The first CI run
([36916102595](https://github.com/papegaaij/conceptgame/actions/runs/36916102595)) passed on all
three: the replay hash is the same on Linux, Windows and macOS.

## 8. Packaging: pass (the clean-machine check is still open)

`./gradlew :desktop:package{LinuxX64,WinX64,MacX64,MacM1}` with Construo 2.2.2 on this Linux
host: all four bundles build in about 25 s, including downloading four Temurin 21.0.12 JDKs
(pinned by SHA-256).

| Bundle | Zipped |
|---|---|
| linux-x64 | 116.0 MB |
| windows-x64 | 112.3 MB |
| macos-x64 (.app) | 113.0 MB |
| macos-arm64 (.app) | 111.8 MB |

Unpacked Linux bundle: 155 MB = jlinked runtime 70 MB (jdeps chose java.base, java.desktop,
java.management, jdk.management, jdk.unsupported, …) + fat jar 87 MB (115.7 MB of content:
halo atlas 64.4 MB, natives for every OS 34.1 MB, audio 5.7 MB). The Linux bundle **started and
ran a 5 s benchmark** with its own runtime (Temurin 21.0.12 LTS, generational ZGC, 60 fps) —
on this machine, not a clean one. Size reductions for later: 128-frame angle sets, and
per-OS natives.

## 9. Code quality

For the user to judge. Structure, conventions and how to run everything are in
[README.md](README.md).

## 10. Display modes: pass (the keys are checked by hand)

Added after the first spike run, when the user asked for a full-screen toggle (see
`design/ui/options` on `main`). `DisplayModes` switches with Alt+Enter or F11 between a 1920×1080
window and borderless full screen: `setFullscreenMode` with the monitor's *current* display mode,
so GLFW attaches the window to the monitor without a video mode switch. `--fullscreen` starts in
full screen; `--toggle-every <s>` switches automatically for measuring.

```bash
$BIN --autopilot --bench 10 --toggle-every 2
$BIN --scene halo --bench 6 --toggle-every 2
```

| Run | Toggles | Sizes | Result |
|---|---|---|---|
| play, autopilot, 12 s | 6 | window 1920×1080 ↔ full screen 3840×2160 (4× integer scale) | 720 simulation steps in 12 s (no stall), 613 SFX plays without failures, music continuous, no GL errors |
| halo, 8 s | 4 | same | atlas pages kept, rotation continued |

Each switch costs 2–4 frames of 33–67 ms while the monitor reconfigures (with vsync on); nothing
is reloaded or lost. The new back buffer size only arrives with the next `resize` event, so the
outcome is logged there. The NVIDIA video-memory figure is not comparable after a switch (the
4K swap chain counts against it). Not done in the spike: restoring the window position and
remembering the mode in a settings file.

## Surprises and harder-than-expected points

1. **`StrictMath.sin` allocates on JDK 21**: its pure-Java FdLibm port creates a `double[2]` for
   every argument above π/4, so the deterministic simulation allocated 5 KB per step. Fixed with
   `sim/Trig` (a `StrictMath` table with linear interpolation; deterministic and allocation-free).
2. **ZGC on Java 21 is single-generation unless `-XX:+ZGenerational`** is given; Construo's
   launcher turns ZGC on by default, so a packaged Java 21 game silently gets the legacy mode.
3. **Construo bundles the plain `jar`** by default, which lacks the dependencies: the first
   bundle died with `NoClassDefFoundError`. It needs a fat-jar task (`jarTask`). It also logs a
   `jlink --compress` deprecation warning per target.
4. **libGDX audio details**: `setAudioConfig`'s buffer size is in bytes, not samples as its
   Javadoc says; `AudioDevice` sources come from the shared, non-thread-safe pool; `Sound.play`
   allocates.
5. **gdx-tools depends on the old LWJGL 2 backend**, so TexturePacker needs its own Gradle module.
6. **Gradle**: Ant-style include patterns have no `[...]` character classes (silently matched
   nothing); Gradle 9.6 deprecates the Kotlin DSL `by registering` / `by configurations…`
   delegates (fixed). The configuration cache rejects lambdas that capture script objects.
7. Running with the system Java 25 by mistake reproduced the desk-research warnings from LWJGL
   3.3.3 (unsupported JNI version, `sun.misc.Unsafe`, restricted `System::load`).
8. CI must pull LFS files selectively (the halo sheet and the music track), or every run pulls
   the whole design tree.

## Manual checks for the user

- **Gamepad**: `--scene play`, plug and unplug a pad while it runs, check the log lines and that
  stick, d-pad and A work.
- **Music loop**: `--scene play` (or `--scene sfx`), listen past **2:19** for the seam (and at
  7.7 s for the intro-to-loop transition): no gap, no click.
- **Rotation**: `--scene halo`, watch the ring turn at 0.35 rad/s for smoothness.
- **SFX**: `--scene sfx`, listen for dropouts or crackle in the 32-sound bursts, and judge shot
  latency in `--scene play` while firing.
- **Full screen**: press Alt+Enter and F11 in any scene; the picture stays crisp and
  letterboxed in both modes and nothing restarts.
- **Clean machine**: unzip `terran-vanguard-spike-linuxX64.zip` on a machine without Java and
  run `./terran-vanguard-spike`.
