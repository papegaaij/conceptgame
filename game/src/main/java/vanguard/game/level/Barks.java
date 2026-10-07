package vanguard.game.level;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import vanguard.content.WingmenData;
import vanguard.sim.LevelScript;
import vanguard.sim.SimStep;
import vanguard.sim.WaveSpec;

/**
 * Rook's radio barks (design/player/wingmen, Radio barks): eight triggers in priority order, each
 * with its line variants, queued on the {@link RadioQueue} as event lines (they wait for a gap, go
 * stale after 6 s and never push a timed line), except his eject bark.
 *
 * <ul>
 *   <li><b>Spacing:</b> a bark is dropped when it fires less than {@code spacing} (8 s) after the
 *       start of any Rook line on the radio (a bark or a scripted line), or within that time of a
 *       timed Rook line of the level script: the scripted line says it.
 *   <li><b>The eject bark</b> is never dropped: it ignores the spacing and the scripted lines, cuts
 *       a bark of his that waits or plays, and queues as an urgent line, so it plays at once (an
 *       urgent line already on the radio finishes first) and nothing replaces it.
 *   <li><b>Priority:</b> only one bark waits at a time; one of a higher priority (earlier in the
 *       data) replaces the waiting one, one of the same or a lower priority is dropped.
 *   <li><b>Variants:</b> the <i>n</i>-th bark of a trigger in an attempt (from 0) says variant
 *       (level number + <i>n</i>) mod the trigger's count, so levels open on different lines and a
 *       retry replays the same ones.
 *   <li>After he ejects his low-armour bark can no longer fire; the others still do (he watches the
 *       scope from his pod).
 * </ul>
 *
 * The triggers come from the level's events (a boss's arrival, his low armour and ejection, kills),
 * from the level script (a rear wave 1.5 s before it enters, a sides wave as it enters) and from the
 * state the screen reads each step (the player's armour, overdrive pickups on the field). Everything
 * goes by the level's clock, so a retry plays the same barks; nothing here allocates per step except
 * a queued bark.
 */
public final class Barks {
    /** The triggers, by their id in design/player/wingmen/data.yaml. */
    public enum Trigger {
        BOSS_WARNING("boss-warning"),
        REAR_WAVE("rear-wave"),
        SIDES_WAVE("sides-wave"),
        PLAYER_ARMOUR("player-armour"),
        ROOK_ARMOUR(WingmenData.ROOK_ARMOUR),
        ROOK_EJECTS("rook-ejects"),
        KILL_STREAK("kill-streak"),
        OVERDRIVE("overdrive");

        private final String id;

        Trigger(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    /**
     * A bark to queue.
     *
     * @param expression the portrait expression's slug ({@code grim})
     * @param shout whether it is shouted (its voice file is the shout row's)
     */
    public record Line(Trigger trigger, String speaker, String expression, boolean shout, String text) {
        /** How it queues: the eject bark is urgent, the others are event lines. */
        public RadioQueue.Priority priority() {
            return trigger == Trigger.ROOK_EJECTS ? RadioQueue.Priority.URGENT : RadioQueue.Priority.EVENT;
        }
    }

    private static final double REAR_AHEAD_SECONDS = 1.5;

    private final WingmenData.Barks data;
    private final int level;
    private final RadioQueue radio;
    private final Function<Line, RadioQueue.Message> queue;
    /** Per trigger its bark (null when the data has none) and its priority (0 the highest). */
    private final WingmenData.Bark[] barks = new WingmenData.Bark[Trigger.values().length];

    private final int[] priorities = new int[Trigger.values().length];
    /** The level times (s) of the timed Rook lines of the level script. */
    private final double[] timedRook;
    /** The steps at which a rear wave's and a sides wave's bark fires, sorted. */
    private final int[] rearTicks;

    private final int[] sidesTicks;
    private final int streakKills;
    private final double streakSeconds;
    private final double playerBelow;

    private final int[] counts = new int[Trigger.values().length];
    private double lastRookStart;
    private RadioQueue.Message waiting;
    private int waitingPriority;
    private boolean ejected;
    /** The player's armour latch: below the share since the last bark; null before the first step. */
    private Boolean playerLow;

    private int overdrives;
    private int lastTick;
    private final Deque<Double> kills = new ArrayDeque<>();

    /**
     * @param level the level number, which picks the first variant
     * @param timedRook the level times of the script's timed Rook lines
     * @param rearWaves the level times at which rear waves enter
     * @param sidesWaves the level times at which sides waves enter
     * @param queue queues a bark as an event line on {@code radio} and returns its message
     */
    public Barks(
            WingmenData.Barks data,
            int level,
            double[] timedRook,
            double[] rearWaves,
            double[] sidesWaves,
            RadioQueue radio,
            Function<Line, RadioQueue.Message> queue) {
        this.data = data;
        this.level = level;
        this.radio = radio;
        this.queue = queue;
        for (Trigger trigger : Trigger.values()) {
            Optional<WingmenData.Bark> bark = data.bark(trigger.id());
            barks[trigger.ordinal()] = bark.orElse(null);
            priorities[trigger.ordinal()] = bark.map(data.triggers()::indexOf).orElse(Integer.MAX_VALUE);
        }
        this.timedRook = timedRook.clone();
        double ahead = bark(Trigger.REAR_WAVE).flatMap(WingmenData.Bark::ahead).orElse(REAR_AHEAD_SECONDS);
        rearTicks = Arrays.stream(rearWaves)
                .mapToInt(t -> SimStep.ticks(Math.max(0, t - ahead)))
                .sorted()
                .toArray();
        sidesTicks = Arrays.stream(sidesWaves).mapToInt(SimStep::ticks).sorted().toArray();
        WingmenData.Bark streak = barks[Trigger.KILL_STREAK.ordinal()];
        streakKills = streak == null ? Integer.MAX_VALUE : streak.kills().orElse(Integer.MAX_VALUE);
        streakSeconds = streak == null ? 0 : streak.seconds().orElse(0.0);
        playerBelow =
                bark(Trigger.PLAYER_ARMOUR).flatMap(WingmenData.Bark::below).orElse(0.0);
        reset(0);
    }

    /**
     * Rook's barks in a level: the script's timed Rook lines (those that play with what is fitted) and
     * its rear and sides waves.
     */
    public static Barks of(
            WingmenData.Barks data,
            LevelScript script,
            boolean specialFitted,
            RadioQueue radio,
            Function<Line, RadioQueue.Message> queue) {
        double[] timed = script.radio().stream()
                .filter(cue -> cue.trigger() == LevelScript.CueTrigger.TIME)
                .filter(cue -> cue.speaker().equals(data.speaker()))
                .filter(cue -> specialFitted || !cue.requiresSpecial())
                .mapToDouble(LevelScript.RadioCue::t)
                .toArray();
        return new Barks(
                data,
                script.number(),
                timed,
                waves(script.waves(), WaveSpec.Entry.REAR),
                waves(script.waves(), WaveSpec.Entry.SIDES),
                radio,
                queue);
    }

    private static double[] waves(List<WaveSpec> waves, WaveSpec.Entry entry) {
        return waves.stream()
                .filter(wave -> wave.entry() == entry)
                .mapToDouble(WaveSpec::t)
                .toArray();
    }

    private Optional<WingmenData.Bark> bark(Trigger trigger) {
        return Optional.ofNullable(barks[trigger.ordinal()]);
    }

    /** A new attempt (or the boss checkpoint) starting at {@code levelTick}: the counts and latches start over. */
    public void reset(int levelTick) {
        Arrays.fill(counts, 0);
        lastRookStart = Double.NEGATIVE_INFINITY;
        if (waiting != null) {
            radio.withdraw(waiting);
        }
        waiting = null;
        ejected = false;
        playerLow = null;
        overdrives = -1;
        lastTick = levelTick;
        kills.clear();
    }

    /** A line opened on the radio at level time {@code t}: a Rook line starts the spacing. */
    public void opened(String speaker, double t) {
        if (speaker.equals(data.speaker())) {
            lastRookStart = t;
        }
    }

    /**
     * A scripted line about Rook fired at level time {@code t} (M5 part B: the radio's {@code
     * escort-first-kill} cue): a Rook line counts as one opened now for the spacing, and a bark of his
     * queued at the same time (the kill that made a streak) is withdrawn, so the scripted line says it.
     */
    public void scripted(String speaker, double t) {
        if (!speaker.equals(data.speaker())) {
            return;
        }
        lastRookStart = t;
        if (waiting != null && radio.waiting(waiting) && waitingPriority >= 0) {
            radio.withdraw(waiting);
            waiting = null;
        }
    }

    /** A boss's warning starts (an act boss's warning, a mid-boss's sting). */
    public boolean bossWarning(double t) {
        return fire(Trigger.BOSS_WARNING, t);
    }

    /** Rook's armour fell below his low-armour share (once per attempt, while he flies). */
    public boolean rookCritical(double t) {
        return !ejected && fire(Trigger.ROOK_ARMOUR, t);
    }

    /**
     * Rook ejects: his low-armour bark can no longer fire, and his eject bark cuts the bark of his that
     * waits or plays (it would come too late after it) and is queued whatever the spacing.
     */
    public boolean rookEjected(double t) {
        ejected = true;
        if (waiting != null) {
            radio.cancel(waiting);
            waiting = null;
        }
        return fire(Trigger.ROOK_EJECTS, t);
    }

    /** An enemy was destroyed (by the player or by him): ten within five seconds is a streak, then the count starts over. */
    public boolean kill(double t) {
        while (!kills.isEmpty() && t - kills.peekFirst() > streakSeconds) {
            kills.removeFirst();
        }
        kills.addLast(t);
        if (kills.size() < streakKills) {
            return false;
        }
        kills.clear();
        return fire(Trigger.KILL_STREAK, t);
    }

    /**
     * After each simulation step: the waves whose barks are due by {@code levelTick}, the player's
     * armour share (a bark when it falls below 30 %, again only after it was back at 30 % or more) and
     * the overdrive pickups on the field (a bark when one appears).
     */
    public void step(int levelTick, double playerArmourShare, int overdrivePickups) {
        double t = levelTick * SimStep.SECONDS;
        if (levelTick > lastTick) {
            if (due(rearTicks, levelTick)) {
                fire(Trigger.REAR_WAVE, t);
            }
            if (due(sidesTicks, levelTick)) {
                fire(Trigger.SIDES_WAVE, t);
            }
        }
        lastTick = levelTick;
        boolean low = playerArmourShare < playerBelow;
        if (playerLow == null) {
            playerLow = low;
        } else if (low && !playerLow) {
            playerLow = true;
            fire(Trigger.PLAYER_ARMOUR, t);
        } else if (!low) {
            playerLow = false;
        }
        if (overdrives >= 0 && overdrivePickups > overdrives) {
            fire(Trigger.OVERDRIVE, t);
        }
        overdrives = overdrivePickups;
    }

    /** Whether one of the steps lies after the last step seen, up to {@code levelTick}. */
    private boolean due(int[] ticks, int levelTick) {
        for (int tick : ticks) {
            if (tick > lastTick && tick <= levelTick) {
                return true;
            }
        }
        return false;
    }

    /**
     * A trigger at level time {@code t}: queued when the spacing and the priority allow it (the eject
     * bark always).
     *
     * @return whether the bark was queued
     */
    public boolean fire(Trigger trigger, double t) {
        WingmenData.Bark bark = barks[trigger.ordinal()];
        if (bark == null) {
            return false;
        }
        boolean eject = trigger == Trigger.ROOK_EJECTS;
        if (!eject && t - lastRookStart < data.spacing()) {
            return false;
        }
        for (double due : timedRook) {
            if (!eject && Math.abs(due - t) < data.spacing()) {
                return false;
            }
        }
        // Nothing outranks the eject bark while it waits.
        int priority = eject ? -1 : priorities[trigger.ordinal()];
        if (waiting != null && radio.waiting(waiting)) {
            if (priority >= waitingPriority) {
                return false;
            }
            radio.withdraw(waiting);
        }
        int n = counts[trigger.ordinal()]++;
        String text = bark.lines().get(Math.floorMod(level + n, bark.lines().size()));
        waiting =
                queue.apply(new Line(trigger, data.speaker(), bark.expression().slug(), bark.shouted(), text));
        waitingPriority = priority;
        return true;
    }
}
