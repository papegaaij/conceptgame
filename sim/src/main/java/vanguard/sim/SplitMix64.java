package vanguard.sim;

/**
 * Seeded SplitMix64 random generator. The algorithm is fully specified, so the same seed yields
 * the same sequence on every JVM and OS; its single {@code long} of state is part of the state hash.
 */
public final class SplitMix64 {
    private long state;

    public SplitMix64(long seed) {
        this.state = seed;
    }

    public long nextLong() {
        long z = (state += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Uniform in [0, 1). */
    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    /** Uniform in [min, max). */
    public double range(double min, double max) {
        return min + nextDouble() * (max - min);
    }

    /** Uniform in [0, bound). */
    public int nextInt(int bound) {
        return (int) ((nextLong() >>> 33) % bound);
    }

    long state() {
        return state;
    }
}
