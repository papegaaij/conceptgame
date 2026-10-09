package vanguard.game.render;

import java.util.Arrays;

/**
 * M5 part E: a large part's hit flash that does not restart under sustained fire (the Harbour
 * Kraken's head and arms, design/art-direction, Decisions 2026-10-09): a hit flashes the part for
 * {@value #TICKS} steps, and a hit within {@value #COOLDOWN_TICKS} steps of the last flash's start
 * does not start another, so a part under a stream of rounds blinks about four times a second instead
 * of turning into a white silhouette.
 */
final class HitFlash {
    /** A flash lasts as long as every other hit flash. */
    static final int TICKS = 2;
    /** The least steps from one flash's start to the next (0.25 s). */
    static final int COOLDOWN_TICKS = 15;

    private static final long NEVER = Long.MIN_VALUE / 2;

    private long[] started = new long[0];

    /** Forgets every flash (the level restarts or the fight is retried). */
    void reset() {
        Arrays.fill(started, NEVER);
    }

    /**
     * Whether part {@code part} shows its flash at step {@code tick}, its last hit {@code ticksSinceHit}
     * steps ago; called once a frame or more, in step order.
     */
    boolean on(int part, long tick, int ticksSinceHit) {
        if (part >= started.length) {
            int from = started.length;
            started = Arrays.copyOf(started, part + 1);
            Arrays.fill(started, from, started.length, NEVER);
        }
        if (ticksSinceHit >= 0 && ticksSinceHit < TICKS) {
            long hit = tick - ticksSinceHit;
            if (hit - started[part] >= COOLDOWN_TICKS) {
                started[part] = hit;
            }
        }
        long since = tick - started[part];
        return since >= 0 && since < TICKS;
    }
}
