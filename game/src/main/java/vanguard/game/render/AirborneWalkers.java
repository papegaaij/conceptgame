package vanguard.game.render;

import vanguard.sim.Enemy;
import vanguard.sim.Layer;
import vanguard.sim.Sortie;

/**
 * M5 part C: where the ground units in a pounce's air window were after the last step (the Ravager,
 * on {@code air} for the middle of its leap; design/enemies/ground/ravager), so the screen can tell
 * that a ground kind's death event belongs to one in the air: its burst then stays on the play field
 * instead of scrolling with the ground, and the layer it was hit on is air. A death event names only
 * the kind and the place, so the place is matched against these, within a step's leap. Allocation-free.
 */
public final class AirborneWalkers {
    /** At most this many at once (a pack of five, all leaping). */
    static final int CAPACITY = 16;
    /** A leap covers at most about 9 px a step (twice the 200 px range in 0.75 s): matched within this. */
    static final double MATCH = 16;

    private final double[] xs = new double[CAPACITY];
    private final double[] ys = new double[CAPACITY];
    private final int[] kinds = new int[CAPACITY];
    private int size;

    /** Notes the sortie's ground units that are in the air now; call before each step. */
    public void record(Sortie sortie) {
        size = 0;
        for (int i = 0; i < sortie.enemyCount() && size < CAPACITY; i++) {
            Enemy enemy = sortie.enemy(i);
            if (enemy.layer() != enemy.spec().layer()) {
                add(enemy.kind(), enemy.renderX(1), enemy.renderY(1));
            }
        }
    }

    /** Adds one, for the tests. */
    void add(int kind, double x, double y) {
        if (size < CAPACITY) {
            kinds[size] = kind;
            xs[size] = x;
            ys[size] = y;
            size++;
        }
    }

    /** Whether a unit of {@code kind} that died at (x, y) was one of those in the air. */
    public boolean contains(int kind, double x, double y) {
        for (int i = 0; i < size; i++) {
            double dx = xs[i] - x;
            double dy = ys[i] - y;
            if (kinds[i] == kind && dx * dx + dy * dy <= MATCH * MATCH) {
                return true;
            }
        }
        return false;
    }

    /** The layer a unit of {@code kind} with stat-block layer {@code layer} died on at (x, y). */
    public Layer layerOf(int kind, Layer layer, double x, double y) {
        return layer != Layer.AIR && contains(kind, x, y) ? Layer.AIR : layer;
    }
}
