package vanguard.sim;

import java.util.Optional;

/**
 * An enemy in flight, as its {@link Spawn} planned it: it flies its path; at the path's end it is
 * gone, or it holds there (hovering, or orbiting in a circle) while its gun fires, and then
 * leaves the play field. Pooled: {@link #spawn} reuses the instance.
 */
public final class Enemy implements Hashed {
    /** The most its facing turns in one step: one step of a 16-angle set (art direction, Rotation). */
    private static final double MAX_TURN = StrictMath.PI / 8;

    private enum Phase {
        ENTER,
        HOLD,
        LEAVE
    }

    private EnemySpec spec;
    private int kind;
    private FlightPath path;
    private int segment;
    private double distance;
    private double speed;
    private Phase phase;
    private int holdTicks;
    private Optional<Spawn.Orbit> orbit;
    private double angle;
    private Spawn.Exit exit;
    private double vx;
    private double vy;
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    /**
     * Where it faces, for the presentation only: radians clockwise from straight down the screen.
     * Nothing in the simulation reads it, so it is not part of the state hash.
     */
    private double facing;

    private double hp;
    private int volleyTicks;
    private int burstLeft;
    private int burstTicks;
    private boolean leadsTarget;
    private Optional<PickupType> carried;

    void spawn(Spawn plan) {
        spec = plan.enemy();
        kind = plan.kind();
        path = plan.path();
        segment = 0;
        distance = 0;
        speed = plan.speed();
        phase = Phase.ENTER;
        holdTicks = SimStep.ticks(plan.holdSeconds());
        orbit = plan.orbit();
        exit = plan.exit();
        hp = spec.hp();
        burstLeft = 0;
        leadsTarget = plan.leadsTarget();
        carried = plan.carried();
        place();
        prevX = x;
        prevY = y;
        facing = heading(path.dx(segment), path.dy(segment));
    }

    /**
     * Flies one step; {@code shipX}, {@code shipY} is where a unit that breaks off heads for.
     * Returns false once it is gone: at the end of its path without a hold, or off the play field
     * after leaving.
     */
    boolean move(double shipX, double shipY) {
        prevX = x;
        prevY = y;
        switch (phase) {
            case ENTER -> {
                distance += speed * SimStep.SECONDS;
                if (distance >= path.length()) {
                    distance = path.length();
                    place();
                    if (holdTicks == 0) {
                        return false;
                    }
                    hold();
                } else {
                    place();
                }
            }
            case HOLD -> {
                if (orbit.isPresent()) {
                    circle(orbit.get());
                }
                if (--holdTicks <= 0) {
                    leave(shipX, shipY);
                }
            }
            case LEAVE -> {
                x += vx * SimStep.SECONDS;
                y += vy * SimStep.SECONDS;
                turn();
                return PlayField.overlaps(x, y, spec.hitbox());
            }
        }
        turn();
        return true;
    }

    /**
     * Turns its facing toward the direction it flew this step, by at most {@link #MAX_TURN}, so a
     * sharp turn (breaking out of an orbit) plays through the in-between headings; standing still
     * keeps the facing.
     */
    private void turn() {
        double dx = x - prevX;
        double dy = y - prevY;
        if (dx == 0 && dy == 0) {
            return;
        }
        double delta = Math.IEEEremainder(heading(dx, dy) - facing, 2 * StrictMath.PI);
        facing = Math.IEEEremainder(facing + Math.clamp(delta, -MAX_TURN, MAX_TURN), 2 * StrictMath.PI);
    }

    /** The direction of a movement (y up) in radians clockwise from straight down the screen. */
    private static double heading(double dx, double dy) {
        return StrictMath.atan2(-dx, -dy);
    }

    private void place() {
        segment = path.segmentAt(distance, segment);
        x = path.x(segment, distance);
        y = path.y(segment, distance);
    }

    private void hold() {
        phase = Phase.HOLD;
        // No Optional.map here: it would allocate in the step loop.
        angle = orbit.isPresent() ? orbit.get().angle() : 0;
        // + 1: the gun already counts down in the step the unit stops, which is step 0 of the delay.
        volleyTicks = spec.gun().isPresent() ? SimStep.ticks(spec.gun().get().firstShotDelay()) + 1 : 0;
    }

    private void circle(Spawn.Orbit circle) {
        angle += circle.radiansPerSecond() * SimStep.SECONDS;
        x = circle.centreX() + circle.radius() * Trig.cos(angle);
        y = circle.centreY() + circle.radius() * Trig.sin(angle);
    }

    private void leave(double shipX, double shipY) {
        phase = Phase.LEAVE;
        burstLeft = 0;
        double dx = exit.dx();
        double dy = exit.dy();
        if (exit.towardShip()) {
            double length = Math.sqrt((shipX - x) * (shipX - x) + (shipY - y) * (shipY - y));
            dx = length > 0 ? (shipX - x) / length : 0;
            dy = length > 0 ? (shipY - y) / length : -1;
        }
        vx = dx * speed;
        vy = dy * speed;
    }

    /** Counts down its gun while it holds; returns whether it fires a shot this step. */
    boolean trigger() {
        if (phase != Phase.HOLD || spec.gun().isEmpty()) {
            return false;
        }
        EnemyGun gun = spec.gun().get();
        if (burstLeft > 0) {
            if (--burstTicks > 0) {
                return false;
            }
            burstLeft--;
            burstTicks = SimStep.ticks(EnemyGun.BURST_GAP_SECONDS);
            return true;
        }
        if (--volleyTicks > 0) {
            return false;
        }
        volleyTicks = SimStep.ticks(gun.intervalSeconds());
        burstLeft = gun.burst() - 1;
        burstTicks = SimStep.ticks(EnemyGun.BURST_GAP_SECONDS);
        return true;
    }

    /** Takes damage; returns whether it was destroyed. */
    boolean damage(double amount) {
        hp -= amount;
        return hp <= 0;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(kind)
                .add(distance)
                .add(segment)
                .add(phase.ordinal())
                .add(holdTicks)
                .add(angle)
                .add(vx)
                .add(vy)
                .add(x)
                .add(y)
                .add(hp)
                .add(volleyTicks)
                .add(burstLeft)
                .add(burstTicks);
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    boolean leadsTarget() {
        return leadsTarget;
    }

    Optional<PickupType> carried() {
        return carried;
    }

    public EnemySpec spec() {
        return spec;
    }

    /** Its enemy's index in {@link Sortie#enemyKinds()}. */
    public int kind() {
        return kind;
    }

    /**
     * Where it faces, in radians clockwise from straight down the screen: toward its direction of
     * flight, turning at most one 16-angle step per simulation step (art direction, Rotation).
     */
    public double facing() {
        return facing;
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
