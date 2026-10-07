package vanguard.game.render;

/**
 * The backdrop's clocks at the render time (M5 part C, hold zones; design/tech/architecture, the
 * backdrop's real clock): the level clock (script time), which places the pieces along the scroll and
 * blends the atmosphere, and the real clock, on which moving scenery animates, so traffic does not
 * crawl while a hold slows the level clock to a fifth. A piece's {@code path} runs on the real clock
 * from the moment the level clock reached its first waypoint: {@link #pathTime}. To know that moment
 * it keeps how far the real clock had run ahead of the level clock whenever that changed (in a hold,
 * in a boss arena that halts the level clock); outside them the two clocks run together and nothing
 * is recorded. Allocation-free once its arrays have grown to a level's holds.
 */
public final class BackdropClock {
    /** Offsets closer than this are the same (the two clocks' seconds differ in their last bits). */
    private static final double SAME = 1e-9;

    private double[] scripts = new double[256];
    private double[] offsets = new double[256];
    private int size;
    private double script;
    private double real;

    /** Forgets every recorded change: a new attempt. */
    public void reset() {
        size = 0;
        script = 0;
        real = 0;
    }

    /**
     * The clocks after the latest simulation step: records the real clock's lead over the level
     * clock when it changed. A clock that went back (a retry, a boss checkpoint) drops what was
     * recorded after it.
     */
    public void record(double scriptSeconds, double realSeconds) {
        double offset = realSeconds - scriptSeconds;
        while (size > 0 && (scripts[size - 1] > scriptSeconds || offsets[size - 1] > offset + SAME)) {
            size--;
        }
        double last = size == 0 ? 0 : offsets[size - 1];
        if (Math.abs(offset - last) <= SAME) {
            return;
        }
        if (size == scripts.length) {
            scripts = java.util.Arrays.copyOf(scripts, 2 * size);
            offsets = java.util.Arrays.copyOf(offsets, 2 * size);
        }
        scripts[size] = scriptSeconds;
        offsets[size] = offset;
        size++;
    }

    /** The render time: the level clock and the real clock between two steps. */
    public void at(double scriptSeconds, double realSeconds) {
        script = scriptSeconds;
        real = realSeconds;
    }

    /** The level clock (script time) at the render time, s. */
    public double script() {
        return script;
    }

    /** The real clock at the render time, s: tile drift and animation frames run on it. */
    public double real() {
        return real;
    }

    /**
     * The time to read a path at whose first waypoint is at script time {@code first}: before the
     * level clock reaches it, the level clock (the piece rests at its first waypoint); after, that
     * time plus the real seconds since the level clock reached it.
     */
    public double pathTime(double first) {
        if (script < first) {
            return script;
        }
        return real - offsetAt(first);
    }

    /** How far the real clock had run ahead of the level clock when the level clock reached {@code t}. */
    double offsetAt(double t) {
        int low = 0;
        int high = size - 1;
        int found = -1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (scripts[middle] < t) {
                found = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        if (found < 0) {
            return 0;
        }
        // The lead of the last change before it: the clock reached t at most a step later.
        return offsets[found];
    }
}
