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
        this.script = script;
        timed = script.radio().stream()
                .filter(cue -> cue.trigger() == LevelScript.CueTrigger.TIME)
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
