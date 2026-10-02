package vanguard.sim;

/** A range of values from {@code min} to {@code max}, such as a hover time of 2–4 s. */
public record Range(double min, double max) {
    public Range {
        if (!(min <= max)) {
            throw new IllegalArgumentException("a range needs min <= max, was [" + min + ", " + max + "]");
        }
    }

    /** A uniformly random value in the range. */
    double pick(SplitMix64 rng) {
        return rng.range(min, max);
    }

    /** The value at {@code fraction} (0 = min, 1 = max). */
    double at(double fraction) {
        return min + (max - min) * fraction;
    }
}
