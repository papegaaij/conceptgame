package vanguard.sim;

/**
 * A unit of the level's convoy (design/allies, civilian crawler): waiting below the screen until
 * it rolls in, rolling in from the bottom edge to its station, holding its station on the road,
 * or a wreck lying on the road and scrolling away with the ground. It counts the steps since its
 * last hit for the hit flash, and the ground it has covered for the wheels.
 */
public final class Ally implements Hashed {
    /** Where it is in its run. */
    public enum State {
        WAITING,
        ROLLING,
        STATION,
        WRECK
    }

    private State state = State.WAITING;
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private double hp;
    private double maxHp;
    private double travelled;
    private double prevTravelled;
    private int ticksSinceHit = Integer.MAX_VALUE;
    /** Degrees clockwise from straight up, the road's direction; derived from the position, so not hashed. */
    private double heading;

    void reset(double startY, double fullHp) {
        state = State.WAITING;
        x = prevX = PlayField.WIDTH / 2.0;
        y = prevY = startY;
        hp = maxHp = fullHp;
        travelled = prevTravelled = 0;
        ticksSinceHit = Integer.MAX_VALUE;
        heading = 0;
    }

    /** Remembers where it was for the interpolation, before a step moves it. */
    void remember() {
        prevX = x;
        prevY = y;
        prevTravelled = travelled;
        if (ticksSinceHit < Integer.MAX_VALUE) {
            ticksSinceHit++;
        }
    }

    void state(State next) {
        state = next;
    }

    /** Puts it at ({@code atX}, {@code atY}) facing {@code degrees}, having covered {@code distance} more ground. */
    void place(double atX, double atY, double degrees, double distance) {
        x = atX;
        y = atY;
        heading = degrees;
        travelled += distance;
    }

    /** Starts the interpolation from where it is, as it appears. */
    void settle() {
        prevX = x;
        prevY = y;
        prevTravelled = travelled;
    }

    /** A wreck scrolls down with the ground. */
    void scroll(double distance) {
        y -= distance;
    }

    /** Takes damage; returns whether that destroyed it. */
    boolean damage(double amount) {
        ticksSinceHit = 0;
        hp -= amount;
        if (hp <= 0) {
            hp = 0;
            state = State.WRECK;
            return true;
        }
        return false;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(state.ordinal()).add(x).add(y).add(hp).add(travelled).add(ticksSinceHit);
    }

    public State state() {
        return state;
    }

    /** Whether it is on its way or at its station, not waiting and not a wreck. */
    public boolean rolling() {
        return state == State.ROLLING || state == State.STATION;
    }

    /** Whether it is still alive (waiting, rolling or at its station). */
    public boolean alive() {
        return state != State.WRECK;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    /** The share of its HP left, 0..1. */
    public double hpShare() {
        return hp / maxHp;
    }

    /** Steps since its last hit; {@link Integer#MAX_VALUE} before the first. */
    public int ticksSinceHit() {
        return ticksSinceHit;
    }

    /** Degrees clockwise from straight up: the road's direction where it is (a wreck keeps its last). */
    public double headingDegrees() {
        return heading;
    }

    /** The ground it has covered, px, between the previous and the current step: drives the wheel frames. */
    public double travelled(double alpha) {
        return prevTravelled + (travelled - prevTravelled) * alpha;
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
