package vanguard.sim;

import java.util.Arrays;

/**
 * A level's road on the ground layer (design/campaign, Level 04: the convoy road): straight
 * between points given as the ground scroll distance at which each passes the middle of the screen
 * and its x there, and straight up the screen before the first and after the last point (as the
 * backdrop art lays it out, tools/art/backdrop_l04.py). A ground point at screen height y (y up)
 * lies at distance {@code groundScroll + y - PlayField.HEIGHT / 2}. Its arc length from the first
 * point anchors the road texture to the ground.
 */
public final class Road {
    private final double width;
    private final double[] distances;
    private final double[] xs;
    /** The arc length from the first point to each point. */
    private final double[] arcs;

    /**
     * @param width the ribbon's width, px (presentation)
     * @param distances the points' ground distances, strictly ascending
     * @param xs the points' x in the play field
     */
    public Road(double width, double[] distances, double[] xs) {
        if (distances.length < 2 || distances.length != xs.length || !(width > 0)) {
            throw new IllegalArgumentException("a road has a width and at least two points, each with an x");
        }
        for (int i = 1; i < distances.length; i++) {
            if (!(distances[i] > distances[i - 1])) {
                throw new IllegalArgumentException("the road's points are in order along it");
            }
        }
        this.width = width;
        this.distances = distances.clone();
        this.xs = xs.clone();
        arcs = new double[xs.length];
        for (int i = 1; i < xs.length; i++) {
            double along = distances[i] - distances[i - 1];
            double across = xs[i] - xs[i - 1];
            arcs[i] = arcs[i - 1] + Math.sqrt(along * along + across * across);
        }
    }

    public double width() {
        return width;
    }

    /** The ground distance of the first point. */
    public double start() {
        return distances[0];
    }

    /** The ground distance of the last point. */
    public double end() {
        return distances[distances.length - 1];
    }

    /** The road's x at ground distance {@code distance}. */
    public double x(double distance) {
        if (distance <= distances[0]) {
            return xs[0];
        }
        int last = distances.length - 1;
        if (distance >= distances[last]) {
            return xs[last];
        }
        int i = segment(distance);
        return xs[i] + (distance - distances[i]) * slope(i);
    }

    /** The road's sideways change per px along it at {@code distance}: positive bends right going up. */
    public double slope(double distance) {
        if (distance < distances[0] || distance >= distances[distances.length - 1]) {
            return 0;
        }
        return slope(segment(distance));
    }

    private double slope(int segment) {
        return (xs[segment + 1] - xs[segment]) / (distances[segment + 1] - distances[segment]);
    }

    /** The road's direction at {@code distance} going up the screen: degrees clockwise from straight up. */
    public double headingDegrees(double distance) {
        return StrictMath.toDegrees(StrictMath.atan(slope(distance)));
    }

    /** The arc length along the road from its first point to {@code distance}; negative before it. */
    public double arc(double distance) {
        if (distance <= distances[0]) {
            return distance - distances[0];
        }
        int last = distances.length - 1;
        if (distance >= distances[last]) {
            return arcs[last] + distance - distances[last];
        }
        int i = segment(distance);
        double slope = slope(i);
        return arcs[i] + (distance - distances[i]) * Math.sqrt(1 + slope * slope);
    }

    /** The segment that holds {@code distance}, which lies inside the road. */
    private int segment(double distance) {
        int found = Arrays.binarySearch(distances, distance);
        return found >= 0 ? Math.min(found, distances.length - 2) : -found - 2;
    }
}
