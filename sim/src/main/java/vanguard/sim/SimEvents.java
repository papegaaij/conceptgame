package vanguard.sim;

/**
 * What happened during the last step, for the presentation (sound effects, explosions, HUD
 * flashes); the simulation never calls the presentation itself. Fixed capacity in parallel arrays,
 * so recording an event never allocates; events beyond the capacity are dropped, which is harmless
 * because they are not part of the game state.
 */
public final class SimEvents {
    /** Event kinds; the position is where it happened, in play-field pixels. */
    public enum Type {
        /** The front gun fired a volley (at the muzzle). */
        SHOT_FIRED,
        /** A shot hit an enemy (at the shot). */
        ENEMY_HIT,
        /** An enemy was destroyed (at the enemy). */
        ENEMY_DESTROYED,
        /** The shield absorbed damage (at the ship). */
        SHIELD_HIT,
        /** The shield dropped to zero (at the ship). */
        SHIELD_BROKEN,
        /** The armour took damage (at the ship). */
        ARMOUR_HIT,
        /** Armour reached zero (at the ship). */
        SHIP_DESTROYED,
        /** The sortie started again after the ship was destroyed (at the ship's start position). */
        SORTIE_RESTARTED;

        private static final Type[] VALUES = values();
    }

    private final int[] types;
    private final double[] xs;
    private final double[] ys;
    private int size;

    SimEvents(int capacity) {
        types = new int[capacity];
        xs = new double[capacity];
        ys = new double[capacity];
    }

    void add(Type type, double x, double y) {
        if (size < types.length) {
            types[size] = type.ordinal();
            xs[size] = x;
            ys[size] = y;
            size++;
        }
    }

    void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }

    public Type type(int index) {
        return Type.VALUES[types[index]];
    }

    public double x(int index) {
        return xs[index];
    }

    public double y(int index) {
        return ys[index];
    }
}
