package vanguard.sim;

/** A pickup drifting down the screen until the ship collects it or its time runs out (design/player). */
public final class Pickup implements Hashed {
    private PickupType type;
    private int credits;
    private double x;
    private double y;
    private double prevY;
    private int ticksLeft;

    void drop(PickupType pickupType, int pickupCredits, double startX, double startY, int lifetimeTicks) {
        type = pickupType;
        credits = pickupCredits;
        x = startX;
        y = prevY = startY;
        ticksLeft = lifetimeTicks;
    }

    /** Drifts down by {@code distance}; returns false once its time is up or it has left the screen. */
    boolean drift(double distance) {
        prevY = y;
        y -= distance;
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

    public double renderX() {
        return x;
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public double renderY(double alpha) {
        return prevY + (y - prevY) * alpha;
    }
}
