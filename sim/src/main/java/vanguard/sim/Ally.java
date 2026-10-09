package vanguard.sim;

/**
 * A unit of the level's convoy (design/allies, civilian crawler): waiting below the screen until
 * it rolls in, rolling in from the bottom edge to its station, holding its station on the road,
 * or a wreck lying on the road and scrolling away with the ground. It counts the steps since its
 * last hit for the hit flash, and the ground it has covered for the wheels.
 *
 * <p>M5 part D, an air escort's unit (design/allies, evacuation shuttle): standing on its pad
 * ({@link State#PAD}), lifting off ({@link State#LIFTING}), flying its station ({@link
 * State#FLYING}), climbing out ({@link State#CLIMBING}) and {@link State#HOME} off the top edge, or
 * lost ({@link State#WRECK}: it stays where it was lost, and the game draws its glide down into
 * {@code far} from {@link #ticksSinceLost()}). It also tells its {@link #lift(double) lift} (the
 * scale from the ground layer's to the air layer's), whether it is {@link #untouchable()} and its
 * sideways speed for the bank.
 */
public final class Ally implements Hashed {
    /** Where it is in its run. */
    public enum State {
        WAITING,
        ROLLING,
        STATION,
        /** A lost unit: a ground unit's wreck on the road; M5 part D, an air unit gliding down (presentation). */
        WRECK,
        /** M5 part D: an air unit standing on its pad before the liftoff, scrolling with the ground. */
        PAD,
        /** M5 part D: an air unit lifting off from its pad to its station (untouchable). */
        LIFTING,
        /** M5 part D: an air unit flying its station with the lane sway. */
        FLYING,
        /** M5 part D: an air unit climbing out off the top edge (untouchable). */
        CLIMBING,
        /** M5 part D: an air unit that climbed out off the top edge: home. */
        HOME,
        /**
         * M5 part E: a naval convoy's unit gliding between its station and its arena lane (or, a unit
         * that leaves, below the bottom edge), either way, on the real steps.
         */
        GLIDING,
        /** M5 part E: a naval convoy's unit in its arena lane while the scroll is halted. */
        LANE,
        /** M5 part E: a naval convoy's unit that left the arena, below the bottom edge until the boss is down. */
        AWAY
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
    /** M5 part D: 0 on the ground layer's scale to 1 on the air layer's; derived from the level clock, so not hashed. */
    private double lift;

    private double prevLift;
    /** M5 part D: whether enemy fire and contact pass through it; derived from the level clock, so not hashed. */
    private boolean untouchable;

    void reset(double startY, double fullHp) {
        state = State.WAITING;
        x = prevX = PlayField.WIDTH / 2.0;
        y = prevY = startY;
        hp = maxHp = fullHp;
        travelled = prevTravelled = 0;
        ticksSinceHit = Integer.MAX_VALUE;
        heading = 0;
        lift = prevLift = 1;
        untouchable = false;
    }

    /** M5 part D: an air unit waiting in {@code first} at ({@code atX}, {@code atY}) with {@code fullHp}. */
    void resetAir(State first, double atX, double atY, double fullHp, double startLift) {
        reset(atY, fullHp);
        state = first;
        x = prevX = atX;
        lift = prevLift = startLift;
    }

    /** M5 part E: a naval unit at its station ({@code atX}, {@code atY}) with {@code fullHp}. */
    void resetNaval(double atX, double atY, double fullHp) {
        reset(atY, fullHp);
        state = State.STATION;
        x = prevX = atX;
    }

    /** M5 part E: a naval unit sails at ({@code atX}, {@code atY}), straight up the screen. */
    void sail(double atX, double atY) {
        x = atX;
        y = atY;
    }

    /** M5 part E: takes over {@code other}'s state (a boss checkpoint); both are units of the same convoy. */
    void copyFrom(Ally other) {
        state = other.state;
        x = other.x;
        y = other.y;
        prevX = other.prevX;
        prevY = other.prevY;
        hp = other.hp;
        maxHp = other.maxHp;
        travelled = other.travelled;
        prevTravelled = other.prevTravelled;
        ticksSinceHit = other.ticksSinceHit;
        heading = other.heading;
        lift = other.lift;
        prevLift = other.prevLift;
        untouchable = other.untouchable;
    }

    /** Remembers where it was for the interpolation, before a step moves it. */
    void remember() {
        prevX = x;
        prevY = y;
        prevTravelled = travelled;
        prevLift = lift;
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

    /** M5 part D: an air unit at ({@code atX}, {@code atY}) with its lift and whether it is untouchable. */
    void fly(double atX, double atY, double share, boolean passThrough) {
        x = atX;
        y = atY;
        lift = share;
        untouchable = passThrough;
    }

    /** M5 part D: the scripted loss takes it, whatever its HP: lost, as from a hit. */
    void lose() {
        ticksSinceHit = 0;
        hp = 0;
        state = State.WRECK;
        untouchable = true;
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
            untouchable = true;
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

    /** M5 part E: the hits it took, from its full HP (a naval unit's: the slams it took). */
    public double hitsTaken() {
        return maxHp - hp;
    }

    /** Whether it is on its way or at its station, not waiting and not a wreck. */
    public boolean rolling() {
        return state == State.ROLLING || state == State.STATION;
    }

    /** Whether it is still alive (waiting, rolling or at its station). */
    public boolean alive() {
        return state != State.WRECK;
    }

    /**
     * M5 part D: whether an air unit can be hit now (flying its station, not lost, not in its
     * untouchable windows); a ground unit's own checks are {@link #rolling()}.
     */
    public boolean touchable() {
        return state == State.FLYING && !untouchable;
    }

    /**
     * M5 part D: whether enemy fire and contact pass through it now: an air unit on its pad, lifting
     * off, climbing out or home, or the scripted loss's unit before its loss (lost units as well).
     */
    public boolean untouchable() {
        return untouchable;
    }

    /** M5 part D: whether it was lost (a ground unit's wreck; an air unit gliding down or gone). */
    public boolean lost() {
        return state == State.WRECK;
    }

    /** M5 part D: steps since it was lost (its glide's clock); meaningful while {@link #lost()}. */
    public int ticksSinceLost() {
        return ticksSinceHit;
    }

    /**
     * M5 part D: its scale between the ground layer's (0, on its pad) and the air layer's (1),
     * between the previous and the current step; 1 for a ground ally (drawn at its own layer).
     */
    public double lift(double alpha) {
        return prevLift + (lift - prevLift) * alpha;
    }

    /** M5 part D: its sideways speed in the last step, px/s (positive to the right): the bank. */
    public double bankVelocity() {
        return (x - prevX) / SimStep.SECONDS;
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
