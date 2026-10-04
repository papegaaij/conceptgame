package vanguard.sim;

/**
 * The fitted special (design/player/specials): its name, the charges carried into the level, the
 * most it carries and how long a press waits while it is busy. The simulation flies the Airstrike
 * and the Smart Bomb: one of {@code airstrike} and {@code smartBomb} is given, the other null.
 *
 * @param charges the charges at the level start
 * @param bufferSeconds how long a press of the special button waits while a strike still flies
 */
public record SpecialSpec(
        String name,
        int charges,
        int maxCharges,
        double bufferSeconds,
        AirstrikeSpec airstrike,
        SmartBombSpec smartBomb) {
    public SpecialSpec {
        if (charges < 0 || charges > maxCharges) {
            throw new IllegalArgumentException("charges " + charges + " outside 0.." + maxCharges);
        }
        if ((airstrike == null) == (smartBomb == null)) {
            throw new IllegalArgumentException("a special is an Airstrike or a Smart Bomb");
        }
    }

    /** An Airstrike. */
    public SpecialSpec(String name, int charges, int maxCharges, double bufferSeconds, AirstrikeSpec airstrike) {
        this(name, charges, maxCharges, bufferSeconds, airstrike, null);
    }

    /** A Smart Bomb. */
    public SpecialSpec(String name, int charges, int maxCharges, double bufferSeconds, SmartBombSpec smartBomb) {
        this(name, charges, maxCharges, bufferSeconds, null, smartBomb);
    }

    /** The same special with {@code count} charges at the level start. */
    public SpecialSpec withCharges(int count) {
        return new SpecialSpec(name, count, maxCharges, bufferSeconds, airstrike, smartBomb);
    }
}
