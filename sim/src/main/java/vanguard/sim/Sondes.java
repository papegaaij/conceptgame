package vanguard.sim;

import java.util.Optional;

/**
 * M5 part F, the weather sondes in play (design/campaign Level 12, user decision F7 = b): each enters
 * at the top edge at its time and x and drifts straight down at its speed on the play plane, its x
 * held; nothing shoots, pushes, pulls or collides with it. The ship collects one when its centre comes
 * within the radius of the sonde's; one that leaves the bottom edge uncollected is lost. Fixed arrays,
 * so stepping never allocates.
 */
public final class Sondes {
    /** A sonde's state: not entered yet. */
    public static final int WAITING = 0;
    /** On the screen, drifting down. */
    public static final int DRIFTING = 1;
    /** Collected by the ship. */
    public static final int COLLECTED = 2;
    /** Left the bottom edge uncollected. */
    public static final int LOST = 3;

    /** Half a sonde's height, px: it enters and leaves with its body just outside the edge. */
    static final double HALF = 12;

    private final int[] starts;
    private final double[] xs;
    private final double[] ys;
    private final int[] states;
    private final double step;
    private final double radius;
    private int collected;

    Sondes(Optional<LevelScript.Sondes> spec) {
        int n = spec.map(s -> s.at().size()).orElse(0);
        starts = new int[n];
        xs = new double[n];
        ys = new double[n];
        states = new int[n];
        for (int i = 0; i < n; i++) {
            starts[i] = SimStep.ticks(spec.get().at().get(i).t());
            xs[i] = spec.get().at().get(i).x();
        }
        step = spec.map(LevelScript.Sondes::speed).orElse(0.0) * SimStep.SECONDS;
        radius = spec.map(LevelScript.Sondes::radius).orElse(0.0);
    }

    /**
     * One step at {@code levelTick}: the due sondes enter, the others drift; with {@code collecting} the
     * ship at ({@code shipX}, {@code shipY}) collects those within the radius ({@link #collectedNow});
     * those that left the bottom edge are lost ({@link #lostNow}).
     */
    void update(int levelTick, boolean collecting, double shipX, double shipY, SimEvents events) {
        collectedNow = 0;
        lostNow = 0;
        for (int i = 0; i < starts.length; i++) {
            if (states[i] == WAITING && starts[i] <= levelTick) {
                states[i] = DRIFTING;
                ys[i] = PlayField.HEIGHT + HALF;
            } else if (states[i] == DRIFTING) {
                ys[i] -= step;
            } else {
                continue;
            }
            double dx = xs[i] - shipX;
            double dy = ys[i] - shipY;
            if (collecting && dx * dx + dy * dy <= radius * radius) {
                states[i] = COLLECTED;
                collected++;
                collectedNow++;
                events.add(SimEvents.Type.SONDE_COLLECTED, xs[i], ys[i], collected);
            } else if (ys[i] + HALF < 0) {
                states[i] = LOST;
                lostNow++;
            }
        }
    }

    private int collectedNow;
    private int lostNow;

    /** The sondes collected in the last step. */
    int collectedNow() {
        return collectedNow;
    }

    /** The sondes lost (left the bottom edge uncollected) in the last step. */
    int lostNow() {
        return lostNow;
    }

    /** Back to the level start. */
    void reset() {
        java.util.Arrays.fill(states, WAITING);
        java.util.Arrays.fill(ys, 0);
        collected = 0;
        collectedNow = 0;
        lostNow = 0;
    }

    /** The level's sondes. */
    public int count() {
        return starts.length;
    }

    /** The sondes collected in this attempt. */
    public int collected() {
        return collected;
    }

    /** Sonde {@code i}'s state: {@link #WAITING}, {@link #DRIFTING}, {@link #COLLECTED} or {@link #LOST}. */
    public int state(int i) {
        return states[i];
    }

    public double x(int i) {
        return xs[i];
    }

    public double y(int i) {
        return ys[i];
    }

    /** Sonde {@code i}'s height between the previous and the current step; {@code alpha} in [0, 1]. */
    public double renderY(int i, double alpha) {
        return ys[i] + step * (1 - alpha);
    }

    void addTo(StateHash hash) {
        hash.add(collected);
        for (int i = 0; i < starts.length; i++) {
            hash.add(states[i]).add(ys[i]);
        }
    }
}
