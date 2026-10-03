package vanguard.game.level;

import vanguard.sim.LevelScript;
import vanguard.sim.SimStep;

/**
 * The level script's radio cues as the {@link RadioQueue} needs them: how each queues, and how long
 * until the next timed line is due, so an event line only takes a gap that ends before it.
 */
public final class RadioSchedule {
    private final LevelScript script;
    /** The timed cues' ticks, in order. */
    private final int[] timed;

    public RadioSchedule(LevelScript script) {
        this(script, false);
    }

    /** @param specialFitted whether a special is fitted: the timed cues that require one play only then */
    public RadioSchedule(LevelScript script, boolean specialFitted) {
        this.script = script;
        timed = script.radio().stream()
                .filter(cue -> cue.trigger() == LevelScript.CueTrigger.TIME)
                .filter(cue -> specialFitted || !cue.requiresSpecial())
                .mapToInt(cue -> SimStep.ticks(cue.t()))
                .sorted()
                .toArray();
    }

    /** How cue {@code index} of the script queues. */
    public RadioQueue.Priority priority(int index) {
        return switch (script.radio().get(index).trigger()) {
            case TIME -> RadioQueue.Priority.TIMED;
            case LEVEL_END, SECONDARY_OBJECTIVE -> RadioQueue.Priority.CLOSING;
            default -> RadioQueue.Priority.EVENT;
        };
    }

    /** The placeholder in a convoy line that names the unit it is about ("Crawler {ally} is hit!"). */
    public static final String ALLY = "{ally}";

    private static final String[] NUMBERS = {
        "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten"
    };

    /** A cue's line with {@link #ALLY} filled in as the convoy unit's number word ({@code unit} from 0). */
    public static String line(String line, int unit) {
        if (unit < 0 || !line.contains(ALLY)) {
            return line;
        }
        return line.replace(ALLY, unit < NUMBERS.length ? NUMBERS[unit] : Integer.toString(unit + 1));
    }

    /** Seconds from {@code levelSeconds} until the next timed cue starts; infinite after the last. */
    public float untilTimed(double levelSeconds) {
        int levelTick = SimStep.ticks(levelSeconds);
        for (int tick : timed) {
            if (tick > levelTick) {
                return (float) ((tick - levelTick) * SimStep.SECONDS);
            }
        }
        return Float.POSITIVE_INFINITY;
    }
}
