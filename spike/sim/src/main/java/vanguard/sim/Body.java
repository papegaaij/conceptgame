package vanguard.sim;

/**
 * Something with a position and a round hit box. The previous position is kept so the renderer
 * can interpolate between two simulation steps. Coordinates are play-field pixels: x 0..480 from
 * the left, y 0..540 from the bottom.
 */
public abstract sealed class Body permits Player, Enemy, Bullet {
    double x;
    double y;
    double prevX;
    double prevY;
    double radius;

    final void place(double newX, double newY) {
        x = prevX = newX;
        y = prevY = newY;
    }

    final void rememberPosition() {
        prevX = x;
        prevY = y;
    }

    final boolean overlaps(Body other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double reach = radius + other.radius;
        return dx * dx + dy * dy < reach * reach;
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public final double renderX(double alpha) {
        return prevX + (x - prevX) * alpha;
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public final double renderY(double alpha) {
        return prevY + (y - prevY) * alpha;
    }

    public final double x() {
        return x;
    }

    public final double y() {
        return y;
    }
}
