package vanguard.game.level;

import vanguard.content.voice.VoiceLines;
import vanguard.sim.LevelScript;
import vanguard.sim.SimStep;
import vanguard.sim.WingmanSpec;

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
            // A boss's closing lines (Level 07's four after the kill) queue together and must not go stale.
            case LEVEL_END, SECONDARY_OBJECTIVE, BOSS_DESTROYED -> RadioQueue.Priority.CLOSING;
            default -> RadioQueue.Priority.EVENT;
        };
    }

    /** The placeholder in a convoy line that names the unit it is about ("Crawler {ally} is hit!"). */
    public static final String ALLY = VoiceLines.ALLY;

    /** A cue's line with {@link #ALLY} filled in as the convoy unit's number word ({@code unit} from 0). */
    public static String line(String line, int unit) {
        return VoiceLines.allyLine(line, unit);
    }

    /**
     * A cue's line as shown and spoken: {@link #ALLY} as the convoy unit's number word ({@code unit}
     * from 0, -1 for none) and (M5 part B) {@link VoiceLines#SIDE} as the escort's side, {@code
     * left} or {@code right} ({@code side}: the save's, or {@code --escort}'s {@code side=}).
     */
    public static String line(String line, int unit, WingmanSpec.Side side) {
        return VoiceLines.sideLine(line(line, unit), side == WingmanSpec.Side.LEFT ? "left" : "right");
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
