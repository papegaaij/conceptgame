package vanguard.sim;

/**
 * Deterministic, allocation-free sine and cosine: a table filled once by {@link StrictMath},
 * linearly interpolated with plain IEEE arithmetic (max error about 2e-8).
 *
 * <p>Why not {@code StrictMath.sin} directly: on JDK 21 its pure-Java FdLibm port allocates a
 * {@code double[2]} for argument reduction whenever |x| &gt; pi/4, which made every enemy step
 * allocate. {@code Math.sin} does not allocate but may use CPU intrinsics, so it is not
 * guaranteed to give the same bits on every platform.
 */
public final class Trig {
    private static final int SIZE = 1 << 14;
    private static final double TURN = 2 * StrictMath.PI;
    private static final double[] SINES = new double[SIZE + 1];

    static {
        for (int i = 0; i <= SIZE; i++) {
            SINES[i] = StrictMath.sin(i * TURN / SIZE);
        }
    }

    private Trig() {}

    public static double sin(double radians) {
        double turns = radians / TURN;
        double position = (turns - Math.floor(turns)) * SIZE;
        int index = (int) position;
        double fraction = position - index;
        return SINES[index] + (SINES[index + 1] - SINES[index]) * fraction;
    }

    public static double cos(double radians) {
        return sin(radians + StrictMath.PI / 2);
    }
}
