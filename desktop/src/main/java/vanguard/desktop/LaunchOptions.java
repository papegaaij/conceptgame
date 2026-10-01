package vanguard.desktop;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The command-line options.
 *
 * @param benchSeconds run this long, log the frame count and exit (smoke tests); 0 runs until quit
 * @param settingsFile use this settings file instead of the one in the platform's config directory
 */
record LaunchOptions(double benchSeconds, Optional<Path> settingsFile) {
    /** Parses {@code [--bench <seconds>] [--settings <file>]}. */
    static LaunchOptions parse(String... args) {
        double benchSeconds = 0;
        Path settingsFile = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--bench" -> benchSeconds = Double.parseDouble(value(args, ++i));
                case "--settings" -> settingsFile = Path.of(value(args, ++i));
                default -> throw new IllegalArgumentException("unknown option " + args[i]);
            }
        }
        return new LaunchOptions(benchSeconds, Optional.ofNullable(settingsFile));
    }

    private static String value(String[] args, int index) {
        if (index >= args.length) {
            throw new IllegalArgumentException(args[index - 1] + " needs a value");
        }
        return args[index];
    }
}
