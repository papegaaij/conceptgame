package vanguard.sim;

import java.util.Optional;

/**
 * M5 part F, a level's weather in play (design/campaign Level 12, Weather): the wind's push sideways
 * on the level clock (the gusts and the drifts with their ramps, added up) and the lightning flashes'
 * events. The push is a function of the level step alone, so it carries no state but the next flash;
 * without weather it is exactly 0 and nothing of it is hashed. Built once per sortie; stepping it never
 * allocates.
 */
public final class Wind {
    private final int[] gustStarts;
    private final int[] gustSigns;
    private final int gustTicks;
    private final int rampTicks;
    private final int telegraphTicks;
    private final double push;
    private final int[] driftStarts;
    private final int[] driftEnds;
    private final double[] driftPushes;
    private final int[] flashes;
    private final boolean present;
    private int nextFlash;

    Wind(Optional<LevelScript.Weather> weather) {
        present = weather.isPresent();
        LevelScript.Weather w = weather.orElse(null);
        int gusts = w == null ? 0 : w.gusts().size();
        gustStarts = new int[gusts];
        gustSigns = new int[gusts];
        for (int i = 0; i < gusts; i++) {
            gustStarts[i] = SimStep.ticks(w.gusts().get(i).t());
            gustSigns[i] = w.gusts().get(i).fromLeft() ? 1 : -1;
        }
        gustTicks = w == null ? 0 : SimStep.ticks(w.gustSeconds());
        rampTicks = w == null ? 0 : SimStep.ticks(w.rampSeconds());
        telegraphTicks = w == null ? 0 : SimStep.ticks(w.telegraphSeconds());
        push = w == null ? 0 : w.push();
        int drifts = w == null ? 0 : w.drifts().size();
        driftStarts = new int[drifts];
        driftEnds = new int[drifts];
        driftPushes = new double[drifts];
        for (int i = 0; i < drifts; i++) {
            LevelScript.Drift drift = w.drifts().get(i);
            driftStarts[i] = SimStep.ticks(drift.start());
            driftEnds[i] = SimStep.ticks(drift.end());
            driftPushes[i] = drift.fromLeft() ? drift.push() : -drift.push();
        }
        int count = w == null ? 0 : w.lightning().size();
        flashes = new int[count];
        for (int i = 0; i < count; i++) {
            flashes[i] = SimStep.ticks(w.lightning().get(i));
        }
    }

    /** Whether the level has weather at all. */
    public boolean present() {
        return present;
    }

    /**
     * The push at {@code levelTick}, px/s to the right (negative: to the left): every gust's and drift's,
     * each ramping up linearly over the first ramp steps of its span and down over its last.
     */
    public double push(int levelTick) {
        if (!present) {
            return 0;
        }
        double sum = 0;
        for (int i = 0; i < gustStarts.length; i++) {
            if (push > 0) {
                sum += gustSigns[i] * push * envelope(levelTick - gustStarts[i], gustTicks);
            }
        }
        for (int i = 0; i < driftStarts.length; i++) {
            sum += driftPushes[i] * envelope(levelTick - driftStarts[i], driftEnds[i] - driftStarts[i]);
        }
        return sum;
    }

    /** The share of the full push {@code k} steps into a span of {@code span} steps with the ramps. */
    private double envelope(int k, int span) {
        if (k < 0 || k >= span) {
            return 0;
        }
        if (rampTicks == 0) {
            return 1;
        }
        return Math.min(1, Math.min((double) k / rampTicks, (double) (span - k) / rampTicks));
    }

    /**
     * The side a gust whose telegraph runs at {@code levelTick} blows from, for the presentation's
     * streaks and arrow at the upwind edge: 1 from the left, -1 from the right, 0 for none. The telegraph
     * runs from its telegraph time ahead of the gust's start until the gust's push has ramped up.
     */
    public int telegraph(int levelTick) {
        for (int i = 0; i < gustStarts.length; i++) {
            int k = levelTick - gustStarts[i];
            if (k >= -telegraphTicks && k < rampTicks) {
                return gustSigns[i];
            }
        }
        return 0;
    }

    /** Raises a {@link SimEvents.Type#LIGHTNING} event for every flash due at or before {@code levelTick}. */
    void flash(int levelTick, SimEvents events) {
        while (nextFlash < flashes.length && flashes[nextFlash] <= levelTick) {
            events.add(SimEvents.Type.LIGHTNING, PlayField.WIDTH / 2.0, PlayField.HEIGHT, nextFlash);
            nextFlash++;
        }
    }

    /** Back to the level start. */
    void reset() {
        nextFlash = 0;
    }

    /** The next flash's index: the flashes before it have been raised. */
    public int nextFlash() {
        return nextFlash;
    }

    void addTo(StateHash hash) {
        hash.add(nextFlash);
    }
}
