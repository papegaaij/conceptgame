# Terran Vanguard

*Working title. Repository: conceptgame.*

A late-90s style vertical shoot'em up. The year is 2185: an alien fleet pours through a gate at
the edge of the solar system, and it is up to one pilot to defend Earth, its colonies and its
stations across 50 levels.

Everything about the game — story, levels, enemies, ship, systems, UI, art and audio — is
described in the living documentation under [design/](design/README.md), which also serves as
the implementation backlog. The game itself is being built with libGDX on Java 21
([roadmap](design/tech/roadmap/README.md)).

- Working rules for the documentation: [CLAUDE.md](CLAUDE.md)
- Open choices awaiting a decision: [design/concept-rounds/](design/concept-rounds/README.md)
- Validate the documentation: `python3 tools/check_docs.py`

Binary assets are stored with Git LFS; install `git-lfs` before cloning.

## Building and running

Requires JDK 21 (`JAVA_HOME` pointing at it) and the Git LFS files; Gradle comes with the wrapper.

```bash
./gradlew check                 # compile, format check, all tests
./gradlew :desktop:run          # start the game
./gradlew :desktop:packageLinuxX64   # bundle with its own runtime: desktop/build/construo/dist/
```

The first start opens in borderless full screen; **Alt+Enter** or **F11** switch to a window
and back. On the title screen **Enter** (or A on a gamepad) starts Level 01 and **Esc**
(or B / Back) quits; in flight, arrows / WASD or the left stick move, **Space** / Z / A fire,
**Left Shift** / C / right bumper hold precision mode, and **Esc** / P / Start return to the
title. The display mode, monitor and window position are kept in `settings.properties` in the
platform's config directory (`~/.config/terran-vanguard/` on Linux, `%APPDATA%\Terran Vanguard\`
on Windows, `~/Library/Application Support/Terran Vanguard/` on macOS); add
`controls.auto-fire=true` there to fire without holding the button.

Options: `--bench <seconds>` flies Level 01, exits after that time and logs the frame count;
`--settings <file>` uses another settings file; `--difficulty easy|medium|hard` picks the
difficulty (medium by default, until the new-game menu exists); `--debug-speed <n>` (a testing
aid) runs game time n times faster, e.g. 4 to get through Level 01 in under a minute.

Placeholder art and audio come from the chosen concept files: `./gradlew :pipeline:importPlaceholders`
copies them and cuts the sprite frames into `assets/` (committed); the build packs the frames
into texture atlases (`:pipeline:packAtlases`, build output only).
