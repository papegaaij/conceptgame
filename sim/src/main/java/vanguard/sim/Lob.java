package vanguard.sim;

/**
 * A mortar's lob in flight (design/enemies/ground/polyp-mortar): a blob that cannot be shot, flying
 * from the mortar to where the player was at launch, while a lime marker shows there. It lands
 * after its flight time and bursts. Pooled.
 */
public final class Lob implements Hashed {
    private EnemyGun gun;
    private double fromX;
    private double fromY;
    private double toX;
    private double toY;
    private int age;
    private int flightTicks;

    /** Lobbed by a mortar with {@code mortarGun} at (x, y) toward (targetX, targetY). */
    void launch(EnemyGun mortarGun, double x, double y, double targetX, double targetY) {
        gun = mortarGun;
        fromX = x;
        fromY = y;
        toX = targetX;
        toY = targetY;
        age = 0;
        flightTicks = Math.max(1, SimStep.ticks(mortarGun.mortar().orElseThrow().flightSeconds()));
    }

    /** Flies one step; returns whether it landed in this step. */
    boolean fly() {
        age++;
        return age >= flightTicks;
    }

    /** The mortar scrolled down with the ground: the start of the arc goes with it. */
    void scroll(double distance) {
        fromY -= distance;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(fromX).add(fromY).add(toX).add(toY).add(age);
    }

    EnemyGun gun() {
        return gun;
    }

    /** Where it lands, px from the left. */
    public double targetX() {
        return toX;
    }

    /** Where it lands, px from the bottom edge. */
    public double targetY() {
        return toY;
    }

    /** Its share of the flight, 0..1, at {@code alpha} between the previous and the current step. */
    public double progress(double alpha) {
        return Math.min(1, (age - 1 + alpha) / flightTicks);
    }

    /** The blob's position along its arc (straight in the plane; the height is the presentation's). */
    public double renderX(double alpha) {
        return fromX + (toX - fromX) * progress(alpha);
    }

    public double renderY(double alpha) {
        return fromY + (toY - fromY) * progress(alpha);
    }

    /** The impact circle's radius, px. */
    public double impactRadius() {
        return gun.mortar().orElseThrow().impactRadius();
    }
}
