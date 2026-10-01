package vanguard.sim;

/**
 * A Skitter (design/enemies/air/skitter) following a snake path: it waits off-screen until its
 * place in the snake comes up, flies the path and is gone at the path's end. A mirrored Skitter
 * flies the path flipped left to right, and every Skitter of a wave shares a horizontal offset.
 */
public final class Skitter {
    private SnakePath path;
    private boolean mirrored;
    private double offsetX;
    private int segment;
    private double distance;
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private double hp;

    void spawn(SnakePath snakePath, double startDistance, boolean mirror, double offset, double hitPoints) {
        path = snakePath;
        mirrored = mirror;
        offsetX = offset;
        segment = 0;
        distance = startDistance;
        hp = hitPoints;
        place();
        prevX = x;
        prevY = y;
    }

    /** Moves {@code step} pixels along the path; returns false once it has flown past the end. */
    boolean advance(double step) {
        prevX = x;
        prevY = y;
        distance += step;
        if (distance > path.length()) {
            return false;
        }
        place();
        return true;
    }

    private void place() {
        double along = Math.max(distance, 0);
        segment = path.segmentAt(along, segment);
        double pathX = path.x(segment, along);
        x = (mirrored ? PlayField.WIDTH - pathX : pathX) + offsetX;
        y = path.y(segment, along);
    }

    /** Takes damage; returns whether it was destroyed. */
    boolean damage(double amount) {
        hp -= amount;
        return hp <= 0;
    }

    void addTo(StateHash hash) {
        hash.add(distance)
                .add(segment)
                .add(mirrored ? 1 : 0)
                .add(offsetX)
                .add(x)
                .add(y)
                .add(hp);
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public double renderX(double alpha) {
        return prevX + (x - prevX) * alpha;
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public double renderY(double alpha) {
        return prevY + (y - prevY) * alpha;
    }
}
