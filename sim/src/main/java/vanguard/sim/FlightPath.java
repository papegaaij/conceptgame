package vanguard.sim;

/**
 * A path an enemy flies (design/enemies, movement vocabulary: {@code path}, {@code swoop},
 * {@code straight}): a smooth Catmull-Rom curve through control points, sampled into a polyline
 * once when the level is planned, so following it per step is plain interpolation without
 * allocation.
 */
public final class FlightPath {
    private static final int SAMPLES_PER_SEGMENT = 16;

    private final double[] xs;
    private final double[] ys;
    /** Distance along the path at every sample. */
    private final double[] distances;

    private FlightPath(double[] xs, double[] ys) {
        this.xs = xs;
        this.ys = ys;
        distances = new double[xs.length];
        for (int i = 1; i < xs.length; i++) {
            distances[i] = distances[i - 1] + StrictMath.hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1]);
        }
    }

    /** A path through the control points, given as x, y pairs in play-field pixels. */
    public static FlightPath through(double... points) {
        int count = points.length / 2;
        if (points.length % 2 != 0 || count < 2) {
            throw new IllegalArgumentException("a path needs at least two x, y pairs");
        }
        int samples = (count - 1) * SAMPLES_PER_SEGMENT + 1;
        double[] xs = new double[samples];
        double[] ys = new double[samples];
        for (int segment = 0; segment < count - 1; segment++) {
            for (int s = 0; s < SAMPLES_PER_SEGMENT; s++) {
                double t = (double) s / SAMPLES_PER_SEGMENT;
                int index = segment * SAMPLES_PER_SEGMENT + s;
                xs[index] = catmullRom(points, count, segment, 0, t);
                ys[index] = catmullRom(points, count, segment, 1, t);
            }
        }
        xs[samples - 1] = points[2 * count - 2];
        ys[samples - 1] = points[2 * count - 1];
        return new FlightPath(xs, ys);
    }

    /** One coordinate ({@code axis} 0 = x, 1 = y) of the uniform Catmull-Rom curve, end points repeated. */
    private static double catmullRom(double[] points, int count, int segment, int axis, double t) {
        double p0 = points[2 * Math.max(segment - 1, 0) + axis];
        double p1 = points[2 * segment + axis];
        double p2 = points[2 * (segment + 1) + axis];
        double p3 = points[2 * Math.min(segment + 2, count - 1) + axis];
        double t2 = t * t;
        double t3 = t2 * t;
        return 0.5 * (2 * p1 + (p2 - p0) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (3 * p1 - p0 - 3 * p2 + p3) * t3);
    }

    /** The same path flipped left to right across the play field. */
    public FlightPath mirrored() {
        double[] mirroredXs = new double[xs.length];
        for (int i = 0; i < xs.length; i++) {
            mirroredXs[i] = PlayField.WIDTH - xs[i];
        }
        return new FlightPath(mirroredXs, ys.clone());
    }

    /** M5 part D: the x of its last point. */
    double endX() {
        return xs[xs.length - 1];
    }

    public double length() {
        return distances[distances.length - 1];
    }

    /**
     * The polyline segment that contains {@code distance}, searching forward from {@code from}: a
     * follower passes its last segment back in, so the search is short.
     */
    int segmentAt(double distance, int from) {
        int segment = from;
        while (segment < distances.length - 2 && distances[segment + 1] < distance) {
            segment++;
        }
        return segment;
    }

    /** The x extent of polyline segment {@code segment}: with {@link #dy} its direction of flight. */
    double dx(int segment) {
        return xs[segment + 1] - xs[segment];
    }

    /** The y extent of polyline segment {@code segment}. */
    double dy(int segment) {
        return ys[segment + 1] - ys[segment];
    }

    double x(int segment, double distance) {
        return lerp(xs, segment, distance);
    }

    double y(int segment, double distance) {
        return lerp(ys, segment, distance);
    }

    private double lerp(double[] values, int segment, double distance) {
        double start = distances[segment];
        double span = distances[segment + 1] - start;
        double t = span > 0 ? Math.clamp((distance - start) / span, 0, 1) : 0;
        return values[segment] + (values[segment + 1] - values[segment]) * t;
    }
}
