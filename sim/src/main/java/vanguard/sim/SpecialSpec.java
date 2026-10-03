package vanguard.sim;

/**
 * The fitted special (design/player/specials): its name, the charges carried into the level, the
 * most it carries and how long a press waits while it is busy. The Airstrike is the only special
 * the simulation flies so far.
 *
 * @param charges the charges at the level start
 * @param bufferSeconds how long a press of the special button waits while a strike still flies
 */
public record SpecialSpec(String name, int charges, int maxCharges, double bufferSeconds, AirstrikeSpec airstrike) {
    public SpecialSpec {
        if (charges < 0 || charges > maxCharges) {
            throw new IllegalArgumentException("charges " + charges + " outside 0.." + maxCharges);
        }
    }

    /** The same special with {@code count} charges at the level start. */
    public SpecialSpec withCharges(int count) {
        return new SpecialSpec(name, count, maxCharges, bufferSeconds, airstrike);
    }
}
