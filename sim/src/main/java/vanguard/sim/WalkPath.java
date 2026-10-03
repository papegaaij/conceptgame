package vanguard.sim;

/**
 * A walker's authored path (design/enemies/ground/scuttler): its points in play-field pixels (y up)
 * at the moment the walker enters, which then scroll with the ground. Shared by every attempt.
 */
public final class WalkPath {
    private final double[] xs;
    private final double[] ys;

    private WalkPath(double[] xs, double[] ys) {
        this.xs = xs;
        this.ys = ys;
    }

    /** The path through {@code x0, y0, x1, y1, ...}; at least one point. */
    public static WalkPath through(double... points) {
        if (points.length < 2 || points.length % 2 != 0) {
            throw new IllegalArgumentException("a walk path needs x, y pairs");
        }
        double[] xs = new double[points.length / 2];
        double[] ys = new double[points.length / 2];
        for (int i = 0; i < xs.length; i++) {
            xs[i] = points[2 * i];
            ys[i] = points[2 * i + 1];
        }
        return new WalkPath(xs, ys);
    }

    /** The path mirrored about the play field's vertical centre line. */
    public WalkPath mirrored() {
        double[] mirrored = new double[xs.length];
        for (int i = 0; i < xs.length; i++) {
            mirrored[i] = PlayField.WIDTH - xs[i];
        }
        return new WalkPath(mirrored, ys);
    }

    public int points() {
        return xs.length;
    }

    public double x(int point) {
        return xs[point];
    }

    public double y(int point) {
        return ys[point];
    }
}
