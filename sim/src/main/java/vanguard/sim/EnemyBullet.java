package vanguard.sim;

/** An enemy bullet on the player's plane, flying straight until it hits the ship or leaves the screen. */
public final class EnemyBullet implements Hashed {
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private double vx;
    private double vy;
    private double damage;

    void fire(double startX, double startY, double velocityX, double velocityY, double bulletDamage) {
        x = prevX = startX;
        y = prevY = startY;
        vx = velocityX;
        vy = velocityY;
        damage = bulletDamage;
    }

    void move() {
        prevX = x;
        prevY = y;
        x += vx * SimStep.SECONDS;
        y += vy * SimStep.SECONDS;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(x).add(y).add(vx).add(vy).add(damage);
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

    /** The direction of flight in radians, 0 = straight up the screen, clockwise. */
    public double heading() {
        return StrictMath.atan2(vx, vy);
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
