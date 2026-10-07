package vanguard.game.level;

import java.util.Comparator;
import java.util.List;
import vanguard.content.voice.VoiceLines;
import vanguard.sim.LevelScript;
import vanguard.sim.SimStep;
import vanguard.sim.Sortie;
import vanguard.sim.WeaponSpec;
import vanguard.sim.WingmanSpec;

/**
 * The level script's radio cues as the {@link RadioQueue} needs them: how each queues, and how long
 * until the next timed line is due, so an event line only takes a gap that ends before it. M5 part
 * C: the timed lines are in script time, which slows in a hold zone, while the queue runs on real
 * time, so the gap is measured in real seconds at the level clock's current rate; and a line that
 * requires an escort ({@code requires: escort}) counts only while he flies.
 */
public final class RadioSchedule {
    private final LevelScript script;
    /** The timed cues' ticks, in order. */
    private final int[] timed;
    /** Per timed cue (same order) whether it plays only while the escort flies. */
    private final boolean[] escortOnly;

    public RadioSchedule(LevelScript script) {
        this(script, false);
    }

    /** @param specialFitted whether a special is fitted: the timed cues that require one play only then */
    public RadioSchedule(LevelScript script, boolean specialFitted) {
        this(script, specialFitted ? LevelScript.RadioCue.FITTED_SPECIAL : 0);
    }

    /**
     * @param fitted what is fitted ({@link LevelScript.RadioCue#FITTED_SPECIAL}, {@link
     *     LevelScript.RadioCue#FITTED_HOMING} bits): the timed cues that require something else never
     *     play; those that require the escort are asked about at each {@link #untilTimed(double,
     *     double, boolean)}
     */
    public RadioSchedule(LevelScript script, int fitted) {
        this.script = script;
        int withEscort = fitted | LevelScript.RadioCue.FITTED_ESCORT;
        List<LevelScript.RadioCue> cues = script.radio().stream()
                .filter(cue -> cue.trigger() == LevelScript.CueTrigger.TIME)
                .filter(cue -> cue.allowedWith(withEscort))
                .sorted(Comparator.comparingDouble(LevelScript.RadioCue::t))
                .toList();
        timed = cues.stream().mapToInt(cue -> SimStep.ticks(cue.t())).toArray();
        escortOnly = new boolean[cues.size()];
        for (int i = 0; i < escortOnly.length; i++) {
            escortOnly[i] = needsEscort(cues.get(i));
        }
    }

    /** Whether a cue plays only while an escort flies ({@code requires: escort}). */
    public static boolean needsEscort(LevelScript.RadioCue cue) {
        return (cue.requires() & LevelScript.RadioCue.FITTED_ESCORT) != 0;
    }

    /**
     * What a sortie's ship has fitted, as the radio's {@link LevelScript.RadioCue#FITTED_SPECIAL} and
     * {@link LevelScript.RadioCue#FITTED_HOMING} bits (a weapon with homing or turret delivery), as the
     * simulation's radio asks for them.
     */
    public static int fitted(Sortie sortie) {
        int fitted = sortie.special().fitted() ? LevelScript.RadioCue.FITTED_SPECIAL : 0;
        for (int m = 0; m < sortie.armament().size(); m++) {
            WeaponSpec.Delivery delivery = sortie.armament().mount(m).weapon().delivery();
            if (delivery == WeaponSpec.Delivery.HOMING || delivery == WeaponSpec.Delivery.TURRET) {
                fitted |= LevelScript.RadioCue.FITTED_HOMING;
            }
        }
        return fitted;
    }

    /** Whether the sortie's escort flies now (hired, fitted and not ejected): a {@code requires: escort} cue plays. */
    public static boolean escortFlying(Sortie sortie) {
        return sortie.wingman().map(wingman -> !wingman.ejected()).orElse(false);
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
        return untilTimed(levelSeconds, 1, true);
    }

    /**
     * Real seconds from {@code levelSeconds} (script time) until the next timed cue starts, the level
     * clock running on at {@code scriptRate} script seconds per real second (M5 part C: 0.2 in a
     * hold zone at 30 of 150 px/s, 0 while an arena halts it); infinite after the last.
     *
     * @param escortFlying whether the escort flies: without him the cues that require him are skipped
     */
    public float untilTimed(double levelSeconds, double scriptRate, boolean escortFlying) {
        int levelTick = SimStep.ticks(levelSeconds);
        for (int i = 0; i < timed.length; i++) {
            if (timed[i] > levelTick && (escortFlying || !escortOnly[i])) {
                double script = (timed[i] - levelTick) * SimStep.SECONDS;
                if (scriptRate >= 1) {
                    return (float) script;
                }
                return scriptRate > 0 ? (float) (script / scriptRate) : Float.POSITIVE_INFINITY;
            }
        }
        return Float.POSITIVE_INFINITY;
    }
}
