package vanguard.game;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Command-line options of the spike.
 *
 * @param scene        what to show
 * @param autopilot    let the {@link vanguard.sim.Autopilot} fly instead of the player
 * @param benchSeconds run this long, print a report and exit; 0 runs until closed
 * @param vsync        synchronise with the display (off to measure frame times)
 * @param recordTo     write the commands of the run to this file as a replay
 * @param fullscreen   start in borderless full screen instead of a window
 * @param toggleEvery  switch between full screen and window every this many seconds; 0 never
 */
public record GameOptions(SceneKind scene, boolean autopilot, double benchSeconds, boolean vsync,
                          Optional<Path> recordTo, boolean fullscreen, double toggleEvery) {

    /** The scenes of the spike, one per gate that needs a window. */
    public enum SceneKind {
        /** Full play field under gate-1 load: parallax, enemies, bullets, glow, HUD, music, SFX. */
        PLAY,
        /** The 768-frame Halo Platform angle set rotating slowly, with video memory figures. */
        HALO,
        /** Bursts of 32 simultaneous sound effects over the music. */
        SFX
    }

    public boolean benchmark() {
        return benchSeconds > 0;
    }

    /**
     * Parses {@code [--scene play|halo|sfx] [--autopilot] [--bench <seconds>] [--no-vsync]
     * [--record <file>] [--fullscreen] [--toggle-every <seconds>]}.
     */
    public static GameOptions parse(String... args) {
        SceneKind scene = SceneKind.PLAY;
        boolean autopilot = false;
        double benchSeconds = 0;
        boolean vsync = true;
        Path recordTo = null;
        boolean fullscreen = false;
        double toggleEvery = 0;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--scene" -> scene = SceneKind.valueOf(value(args, ++i).toUpperCase(Locale.ROOT));
                case "--autopilot" -> autopilot = true;
                case "--bench" -> benchSeconds = Double.parseDouble(value(args, ++i));
                case "--no-vsync" -> vsync = false;
                case "--record" -> recordTo = Path.of(value(args, ++i));
                case "--fullscreen" -> fullscreen = true;
                case "--toggle-every" -> toggleEvery = Double.parseDouble(value(args, ++i));
                default -> throw new IllegalArgumentException("unknown option " + args[i]);
            }
        }
        return new GameOptions(scene, autopilot, benchSeconds, vsync, Optional.ofNullable(recordTo), fullscreen,
                toggleEvery);
    }

    private static String value(String[] args, int index) {
        if (index >= args.length) {
            throw new IllegalArgumentException(args[index - 1] + " needs a value");
        }
        return args[index];
    }
}
