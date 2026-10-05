package vanguard.sim;

/**
 * A pickup drifting down the screen until the ship collects it or its time runs out (design/player);
 * in a fitted Pickup magnet's reach it flies at the ship instead (design/player/systems).
 */
public final class Pickup implements Hashed {
    private PickupType type;
    private int credits;
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private int ticksLeft;

    void drop(PickupType pickupType, int pickupCredits, double startX, double startY, int lifetimeTicks) {
        type = pickupType;
        credits = pickupCredits;
        x = prevX = startX;
        y = prevY = startY;
        ticksLeft = lifetimeTicks;
    }

    /** Drifts down by {@code distance}; returns false once its time is up or it has left the screen. */
    boolean drift(double distance) {
        prevX = x;
        prevY = y;
        y -= distance;
        return --ticksLeft > 0 && y > 0;
    }

    /** Whether it is within {@code radius} of the point ({@code px}, {@code py}). */
    boolean within(double px, double py, double radius) {
        double dx = px - x;
        double dy = py - y;
        return dx * dx + dy * dy <= radius * radius;
    }

    /**
     * Flies {@code distance} straight at the point ({@code px}, {@code py}), stopping on it, instead
     * of drifting; returns false once its time is up or it has left the screen.
     */
    boolean pull(double px, double py, double distance) {
        prevX = x;
        prevY = y;
        double dx = px - x;
        double dy = py - y;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length <= distance) {
            x = px;
            y = py;
        } else {
            x += dx * distance / length;
            y += dy * distance / length;
        }
        return --ticksLeft > 0 && y > 0;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(type.ordinal()).add(credits).add(x).add(y).add(ticksLeft);
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    /** Credits before the credit factor; 0 for pickups that pay none. */
    int credits() {
        return credits;
    }

    public PickupType type() {
        return type;
    }

    /** Steps until it is gone, for the blinking in its last moments. */
    public int ticksLeft() {
        return ticksLeft;
    }

    /** The position of the current step; a pulled pickup moves sideways too, see {@link #renderX(double)}. */
    public double renderX() {
        return x;
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
