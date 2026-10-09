package vanguard.game.render;

import java.util.Arrays;
import vanguard.sim.SimStep;

/**
 * M5 part E: each Driftjelly's pulse at its own, changing rate (it quickens as the ship closes in,
 * {@link NavalLooks#jellyFps}): per unit (by its serial) the frames it has pulsed through, advanced on
 * the simulation's steps at the rate of the moment, so a change of rate never jumps a frame; and
 * whether its last advance reached a contraction (where its ripple train starts). Pure presentation;
 * a fixed table, allocation-free; a unit not seen for a while gives its slot up.
 */
final class PulseClock {
    /** At most this many pulsing units at once (a few fields of 6–12). */
    static final int CAPACITY = 64;

    private final int[] serials = new int[CAPACITY];
    private final double[] phases = new double[CAPACITY];
    private final long[] seen = new long[CAPACITY];
    private boolean contracted;

    PulseClock() {
        reset();
    }

    /** Forgets every unit (the level restarts). */
    void reset() {
        Arrays.fill(serials, -1);
        Arrays.fill(phases, 0);
        Arrays.fill(seen, Long.MIN_VALUE);
        contracted = false;
    }

    /**
     * Unit {@code serial}'s pulse frame (0..frames−1) at step {@code tick}, advanced from where it was
     * at {@code fps}; a unit seen for the first time starts at a phase of its own (by its serial).
     */
    int frame(int serial, long tick, double fps, int frames) {
        int slot = slot(serial);
        contracted = false;
        if (seen[slot] != Long.MIN_VALUE && tick > seen[slot]) {
            long before = (long) Math.floor(phases[slot]);
            phases[slot] += (tick - seen[slot]) * fps * SimStep.SECONDS;
            long after = (long) Math.floor(phases[slot]);
            for (long f = before + 1; f <= after; f++) {
                contracted |= Math.floorMod(f, (long) frames) == NavalLooks.CONTRACTED;
            }
        }
        if (seen[slot] == Long.MIN_VALUE) {
            phases[slot] = Math.floorMod(serial * 7L, (long) frames);
        }
        seen[slot] = tick;
        return (int) Math.floorMod((long) Math.floor(phases[slot]), (long) frames);
    }

    /** Whether the last {@link #frame} call's advance reached a contraction (its ripple starts now). */
    boolean contracted() {
        return contracted;
    }

    /** The unit's slot: its own, a free one, or the one seen longest ago. */
    private int slot(int serial) {
        int free = -1;
        int oldest = 0;
        for (int i = 0; i < CAPACITY; i++) {
            if (serials[i] == serial) {
                return i;
            }
            if (serials[i] < 0 && free < 0) {
                free = i;
            }
            if (seen[i] < seen[oldest]) {
                oldest = i;
            }
        }
        int slot = free >= 0 ? free : oldest;
        serials[slot] = serial;
        seen[slot] = Long.MIN_VALUE;
        phases[slot] = 0;
        return slot;
    }
}
