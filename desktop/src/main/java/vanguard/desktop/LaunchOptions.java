package vanguard.desktop;

import java.nio.file.Path;
import java.util.Optional;
import vanguard.content.Difficulty;
import vanguard.content.campaign.DebugFit;

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
 *     and a debug fit do, {@code --start title} lets a bench run test the menus
 * @param debugFit debug options ({@code --loadout}, {@code --special}, {@code --escort}): weapons, a
 *     special with its charges and Rook in the escort slot (or none) for the level start, see
 *     {@link DebugFit}
 * @param level a debug option ({@code --level}): the level the level start flies, 1 by default
 * @param actEnd a debug option ({@code --act-end}): winning the level start's level ends its act
 *     early, with the act summary in its debrief and the act outro
 */
record LaunchOptions(
        double benchSeconds,
        Optional<Path> settingsFile,
        Difficulty difficulty,
        float debugSpeed,
        boolean invulnerable,
        boolean startLevel,
        Optional<DebugFit> debugFit,
        int level,
        boolean actEnd) {
    /**
     * Parses {@code [--bench <seconds>] [--settings <file>] [--difficulty easy|medium|hard] [--debug-speed <factor>]
     * [--invulnerable] [--start title|level] [--loadout <slot>=<weapon>[:<level>],...] [--special <special>[:<charges>]]
     * [--escort rook:<gun>[:<level>][,side=left|right]|none] [--level <n>] [--act-end]}.
     */
    static LaunchOptions parse(String... args) {
        Boolean start = null;
        double benchSeconds = 0;
        Path settingsFile = null;
        Difficulty difficulty = Difficulty.MEDIUM;
        float debugSpeed = 1;
        boolean invulnerable = false;
        DebugFit debugFit = null;
        String special = null;
        String escort = null;
        int level = 1;
        boolean actEnd = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--bench" -> benchSeconds = Double.parseDouble(value(args, ++i));
                case "--settings" -> settingsFile = Path.of(value(args, ++i));
                case "--difficulty" -> difficulty = Difficulty.of(value(args, ++i));
                case "--debug-speed" -> debugSpeed = Float.parseFloat(value(args, ++i));
                case "--invulnerable" -> invulnerable = true;
                case "--loadout" -> debugFit = DebugFit.parse(value(args, ++i));
                case "--special" -> special = value(args, ++i);
                case "--escort" -> escort = value(args, ++i);
                case "--level" -> level = Integer.parseInt(value(args, ++i));
                case "--act-end" -> actEnd = true;
                case "--start" ->
                    start = switch (value(args, ++i)) {
                        case "title" -> false;
                        case "level" -> true;
                        default -> throw new IllegalArgumentException("--start takes title or level");
                    };
                default -> throw new IllegalArgumentException("unknown option " + args[i]);
            }
        }
        if (special != null) {
            debugFit = (debugFit == null ? DebugFit.NONE : debugFit).withSpecial(special);
        }
        if (escort != null) {
            debugFit = (debugFit == null ? DebugFit.NONE : debugFit).withEscort(escort);
        }
        if (!(debugSpeed > 0)) {
            throw new IllegalArgumentException("--debug-speed must be > 0");
        }
        boolean startLevel = start != null ? start : benchSeconds > 0 || debugFit != null || level != 1 || actEnd;
        if ((debugFit != null || level != 1 || actEnd) && !startLevel) {
            throw new IllegalArgumentException(
                    "--loadout, --special, --escort, --level and --act-end set the level start, they need --start level");
        }
        if (level < 1) {
            throw new IllegalArgumentException("--level must be at least 1");
        }
        return new LaunchOptions(
                benchSeconds,
                Optional.ofNullable(settingsFile),
                difficulty,
                debugSpeed,
                invulnerable,
                startLevel,
                Optional.ofNullable(debugFit),
                level,
                actEnd);
    }

    /**
     * Whether this is a debug run, which writes no save at all (design/systems/saves): a level start
     * (any of {@code --start level}, {@code --level}, {@code --loadout}, {@code --special}, {@code
     * --escort}, {@code --act-end}), a bench run, {@code --invulnerable} or {@code --debug-speed}.
     * {@code --settings} and {@code --difficulty} alone are not.
     */
    boolean debugRun() {
        return startLevel || benchSeconds > 0 || invulnerable || debugSpeed != 1;
    }

    private static String value(String[] args, int index) {
        if (index >= args.length) {
            throw new IllegalArgumentException(args[index - 1] + " needs a value");
        }
        return args[index];
    }
}
