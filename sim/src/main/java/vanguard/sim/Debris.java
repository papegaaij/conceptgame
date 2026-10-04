package vanguard.sim;

/**
 * A debris chunk drifting across the screen on the air layer (design/world/earth-orbit, hazards):
 * it blocks shots and enemy bullets; a large one is indestructible and deals contact damage, a
 * small one breaks. It counts the steps since its last hit for its hit flash. Pooled.
 */
public final class Debris implements Hashed {
    private LevelScript.DebrisSpec spec;
    private int serial;
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private double hp;
    private int hitCooldown;
    private int ticksSinceHit;
    private double vx;
    private double vy;
    /** A thrown rock's steps left; -1 for a chunk drifting through. */
    private int life = -1;

    /** Enters at the top edge at {@code atX}; {@code index} is its place in the level's list, unique in an attempt. */
    void place(LevelScript.DebrisSpec debrisSpec, int index, double atX) {
        spec = debrisSpec;
        serial = index;
        x = prevX = atX;
        y = prevY = PlayField.HEIGHT + debrisSpec.size().height() / 2;
        hp = debrisSpec.hp();
        hitCooldown = 0;
        ticksSinceHit = Integer.MAX_VALUE;
        vx = debrisSpec.vx();
        vy = debrisSpec.vy();
        life = -1;
    }

    /**
     * Thrown from (x, y) at (vx, vy) px/s (a rock from a destroyed ground unit, Level 05); it vanishes
     * after {@code lifeTicks} steps. {@code index} is unique in an attempt.
     */
    void toss(
            LevelScript.DebrisSpec debrisSpec,
            int index,
            double atX,
            double atY,
            double velocityX,
            double velocityY,
            int lifeTicks) {
        place(debrisSpec, index, atX);
        y = prevY = atY;
        vx = velocityX;
        vy = velocityY;
        life = lifeTicks;
    }

    /** Drifts one step; returns false once it has left the screen through the bottom or a side edge. */
    boolean move() {
        prevX = x;
        prevY = y;
        x += vx * SimStep.SECONDS;
        y += vy * SimStep.SECONDS;
        if (life >= 0 && --life < 0) {
            return false;
        }
        if (hitCooldown > 0) {
            hitCooldown--;
        }
        if (ticksSinceHit < Integer.MAX_VALUE) {
            ticksSinceHit++;
        }
        Hitbox size = spec.size();
        return y + size.height() / 2 > 0 && x + size.width() / 2 > 0 && x - size.width() / 2 < PlayField.WIDTH;
    }

    /** Whether a box of {@code box} around (bx, by) touches it. */
    boolean touches(double bx, double by, Hitbox box) {
        return spec.size().overlaps(x, y, box, bx, by);
    }

    /** A small chunk takes damage; returns whether it broke. A large one only flashes. */
    boolean damage(double amount) {
        ticksSinceHit = 0;
        hp -= amount;
        return hp <= 0;
    }

    /** A large chunk touching the ship: whether it deals its damage now (at most once per its interval). */
    boolean strike() {
        if (hitCooldown > 0) {
            return false;
        }
        hitCooldown = SimStep.ticks(LevelScript.DebrisSpec.HIT_INTERVAL_SECONDS);
        return true;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(serial).add(x).add(y).add(hp).add(hitCooldown).add(ticksSinceHit);
        if (life >= 0) {
            hash.add(life).add(vx).add(vy);
        }
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    public LevelScript.DebrisSpec spec() {
        return spec;
    }

    /** Its place in the level's list, unique in an attempt (a thrown rock's look picks its shape by it). */
    public int serial() {
        return serial;
    }

    /** Whether it is a thrown rock, which rises and vanishes. */
    public boolean thrown() {
        return life >= 0;
    }

    /** A thrown rock's steps left; -1 for a chunk drifting through. */
    public int life() {
        return life;
    }

    /** Whether it is a large, indestructible chunk. */
    public boolean large() {
        return spec.large();
    }

    /** The sprite to draw: {@code debris-large-a}, ... (assets/sprites). */
    public String sprite() {
        return spec.sprite();
    }

    /** Simulation steps since the last shot hit it; {@link Integer#MAX_VALUE} before the first. */
    public int ticksSinceHit() {
        return ticksSinceHit;
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
