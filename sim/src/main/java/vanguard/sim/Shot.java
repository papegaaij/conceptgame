package vanguard.sim;

/** A bolt of the player's front gun, flying straight up until it hits or leaves the screen. */
public final class Shot implements Hashed {
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private double damage;

    void fire(double startX, double startY, double shotDamage) {
        x = prevX = startX;
        y = prevY = startY;
        damage = shotDamage;
    }

    void move(double distance) {
        prevX = x;
        prevY = y;
        y += distance;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(x).add(y).add(damage);
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    double damage() {
        return damage;
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
