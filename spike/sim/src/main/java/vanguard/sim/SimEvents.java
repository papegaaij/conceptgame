package vanguard.sim;

/**
 * What happened during the last step, for the presentation layer (sparks, sound effects).
 * Fixed capacity and stored in parallel arrays, so recording an event never allocates; events
 * beyond the capacity are dropped, which is harmless because they are not part of the game state.
 */
public final class SimEvents {
    /** Event kinds. */
    public enum Type {
        PLAYER_SHOT, ENEMY_SHOT, HIT, KILL, PLAYER_HIT;

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
