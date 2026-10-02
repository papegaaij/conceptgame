package vanguard.desktop;

import java.nio.file.Path;
import java.util.Optional;
import vanguard.content.Difficulty;

/**
 * The command-line options.
 *
 * @param benchSeconds fly Level 01 this long, log the frame count and exit (smoke tests); 0 runs
 *     until quit
 * @param settingsFile use this settings file instead of the one in the platform's config directory
 * @param difficulty for testing: the difficulty the bench flies at and the difficulty select starts on
 * @param debugSpeed a debug option: game time runs this many times faster (1 = normal), to get
 *     through a level quickly when testing
 * @param invulnerable a debug option: nothing hits the ship, to see a level to its end when testing
 * @param startLevel start in Level 01 rather than at the title screen; by default only a bench run
 *     does, {@code --start title} lets a bench run test the menus
 */
record LaunchOptions(
        double benchSeconds,
        Optional<Path> settingsFile,
        Difficulty difficulty,
        float debugSpeed,
        boolean invulnerable,
        boolean startLevel) {
    /**
     * Parses {@code [--bench <seconds>] [--settings <file>] [--difficulty easy|medium|hard] [--debug-speed <factor>]
     * [--invulnerable] [--start title|level]}.
     */
    static LaunchOptions parse(String... args) {
        Boolean start = null;
        double benchSeconds = 0;
        Path settingsFile = null;
        Difficulty difficulty = Difficulty.MEDIUM;
        float debugSpeed = 1;
        boolean invulnerable = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--bench" -> benchSeconds = Double.parseDouble(value(args, ++i));
                case "--settings" -> settingsFile = Path.of(value(args, ++i));
                case "--difficulty" -> difficulty = Difficulty.of(value(args, ++i));
                case "--debug-speed" -> debugSpeed = Float.parseFloat(value(args, ++i));
                case "--invulnerable" -> invulnerable = true;
                case "--start" ->
                    start = switch (value(args, ++i)) {
                        case "title" -> false;
                        case "level" -> true;
                        default -> throw new IllegalArgumentException("--start takes title or level");
                    };
                default -> throw new IllegalArgumentException("unknown option " + args[i]);
            }
        }
        if (!(debugSpeed > 0)) {
            throw new IllegalArgumentException("--debug-speed must be > 0");
        }
        boolean startLevel = start != null ? start : benchSeconds > 0;
        return new LaunchOptions(
                benchSeconds, Optional.ofNullable(settingsFile), difficulty, debugSpeed, invulnerable, startLevel);
    }

    private static String value(String[] args, int index) {
        if (index >= args.length) {
            throw new IllegalArgumentException(args[index - 1] + " needs a value");
        }
        return args[index];
    }
}
