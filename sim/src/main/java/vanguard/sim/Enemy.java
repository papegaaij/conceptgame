package vanguard.sim;

import java.util.Optional;

/**
 * An enemy in flight, as its {@link Spawn} planned it: it flies its path; at the path's end it is
 * gone, or it holds there (hovering, or orbiting in a circle) while its gun fires, and then
 * leaves the play field; a diver's hold is its pause, its leaving the dive, in which it fires
 * once. A ground unit ({@link #root}) is fixed to the ground and scrolls with it; a turret turns
 * its barrel towards the ship and fires along it while the ship is inside its arc. Pooled:
 * {@link #spawn} and {@link #root} reuse the instance.
 */
public final class Enemy implements Hashed {
    /** The most its facing turns in one step: one step of a 16-angle set (art direction, Rotation). */
    private static final double MAX_TURN = StrictMath.PI / 8;

    private enum Phase {
        ENTER,
        HOLD,
        LEAVE,
        /** Fixed to the ground layer. */
        GROUND,
        /** Spiralling out of a whirl cluster's release point. */
        SPIRAL
    }

    private EnemySpec spec;
    private int kind;
    private int serial;
    /** The ground group it belongs to (a dock of Level 02); -1 for none. */
    private int group;

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
    /** A turret's barrel: radians clockwise from straight down the screen. */
    private double aim;
    /** Whether the ship is inside a turret's arc this step. */
    private boolean inArc;

    private int diveTicks;
    private boolean diveFired;
    private boolean diveShot;
    private double releaseX;
    private double releaseY;
    private double spiralAngle;
    private int spiralTicks;
    private int ricochetsLeft;

    /** @param unitSerial unique among the units of an attempt, for the shots that lock onto it */
    void spawn(Spawn plan, int unitSerial) {
        serial = unitSerial;
        group = -1;
        aim = 0;
        inArc = false;
        diveTicks = 0;
        diveFired = false;
        diveShot = false;
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
        spiralTicks = 0;
        ricochetsLeft = spec.spiral().isPresent() ? spec.spiral().get().ricochets() : 0;
        if (plan.release().isPresent() && spec.spiral().isPresent()) {
            Spawn.Release release = plan.release().get();
            phase = Phase.SPIRAL;
            releaseX = x = release.x();
            releaseY = y = release.y();
            spiralAngle = release.angle();
        }
        if (spec.gun().isPresent() && spec.gun().get().mine().isPresent()) {
            volleyTicks = SimStep.ticks(spec.gun().get().intervalSeconds());
        }
        prevX = x;
        prevY = y;
        facing = heading(path.dx(segment), path.dy(segment));
    }

    /**
     * A ground unit entering at the top edge at {@code atX}, in {@code inGroup} (-1 for none). Its
     * gun waits its first-shot delay from here.
     */
    void root(EnemySpec enemySpec, int enemyKind, double atX, int unitSerial, int inGroup) {
        spec = enemySpec;
        kind = enemyKind;
        serial = unitSerial;
        group = inGroup;
        phase = Phase.GROUND;
        x = prevX = atX;
        y = prevY = PlayField.HEIGHT + spec.hitbox().height() / 2;
        hp = spec.hp();
        aim = 0;
        facing = 0;
        inArc = false;
        burstLeft = 0;
        leadsTarget = false;
        carried = Optional.empty();
        volleyTicks = spec.gun().isPresent() ? SimStep.ticks(spec.gun().get().firstShotDelay()) + 1 : 0;
        diveFired = diveShot = false;
        diveTicks = 0;
    }

    /**
     * Flies one step; {@code shipX}, {@code shipY} is where a unit that breaks off heads for, and
     * the ground scrolls by {@code groundScroll}. Returns false once it is gone: at the end of its
     * path without a hold, off the play field after leaving, or off the bottom edge for a ground unit.
     */
    boolean move(double shipX, double shipY, double groundScroll) {
        prevX = x;
        prevY = y;
        switch (phase) {
            case SPIRAL -> {
                spiral();
                return true;
            }
            case GROUND -> {
                y -= groundScroll;
                track(shipX, shipY);
                return y + spec.hitbox().height() / 2 > 0;
            }
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
                ricochet();
                turn();
                if (spec.dive().isPresent() && !diveFired) {
                    EnemySpec.Dive dive = spec.dive().get();
                    boolean passed = prevY > shipY && y <= shipY;
                    if (passed || ++diveTicks >= SimStep.ticks(dive.fireAfterSeconds())) {
                        diveFired = true;
                        diveShot = true;
                    }
                }
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

    /**
     * One step of a whirl cluster's spiral (design/enemies/air/whirl-seed): around the release
     * point, which drifts down, at its turn rate and with a growing radius; at its end it flies on
     * along its outward angle, turned downward, and at least as steeply down as the release point
     * drifted (so a seed that ends the spiral pointing sideways still leaves through the bottom).
     */
    private void spiral() {
        EnemySpec.Spiral spiral = spec.spiral().orElseThrow();
        double t = ++spiralTicks * SimStep.SECONDS;
        double angle = spiralAngle + 2 * StrictMath.PI * spiral.turnsPerSecond() * t;
        double radius = spiral.growth() * t;
        double cos = Trig.cos(angle);
        double sin = Trig.sin(angle);
        x = releaseX + radius * cos;
        y = releaseY - spiral.drift() * t + radius * sin;
        if (t >= spiral.seconds()) {
            phase = Phase.LEAVE;
            double down = Math.max(Math.abs(sin), spiral.drift() / speed);
            double length = Math.sqrt(cos * cos + down * down);
            vx = cos / length * speed;
            vy = -down / length * speed;
        }
    }

    /** A spiralled-out unit bounces off the side edges, up to its ricochets. */
    private void ricochet() {
        if (ricochetsLeft == 0) {
            return;
        }
        double half = spec.hitbox().width() / 2;
        if ((x < half && vx < 0) || (x > PlayField.WIDTH - half && vx > 0)) {
            vx = -vx;
            ricochetsLeft--;
        }
    }

    /**
     * A turret on the screen turns its barrel towards the ship by at most its turn rate while the
     * ship is inside its arc; outside it, the barrel holds.
     */
    private void track(double shipX, double shipY) {
        inArc = false;
        if (spec.gun().isEmpty() || !PlayField.overlaps(x, y, spec.hitbox())) {
            return;
        }
        EnemyGun gun = spec.gun().get();
        double wanted = heading(shipX - x, shipY - y);
        if (Math.abs(wanted) > gun.arcRadians()) {
            return;
        }
        inArc = true;
        double delta = Math.IEEEremainder(wanted - aim, 2 * StrictMath.PI);
        double most = gun.turnRate() * SimStep.SECONDS;
        aim = Math.IEEEremainder(aim + Math.clamp(delta, -most, most), 2 * StrictMath.PI);
        facing = aim;
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
        if (spec.dive().isPresent()) {
            speed = spec.dive().get().speed();
        }
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

    /**
     * Counts down its gun while it holds (a turret: while the ship is in its arc); returns whether
     * it fires a shot this step. A diver fires once, in its dive.
     */
    boolean trigger() {
        if (spec.gun().isPresent() && spec.gun().get().mine().isPresent()) {
            // A mine layer drops a spore at its interval wherever it flies on the screen.
            if (phase == Phase.GROUND || !PlayField.overlaps(x, y, spec.hitbox()) || --volleyTicks > 0) {
                return false;
            }
            volleyTicks = SimStep.ticks(spec.gun().get().intervalSeconds());
            return true;
        }
        if (spec.dive().isPresent()) {
            boolean shot = diveShot;
            diveShot = false;
            return shot;
        }
        boolean ready = phase == Phase.HOLD || (phase == Phase.GROUND && inArc);
        if (!ready || spec.gun().isEmpty()) {
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
                .add(serial)
                .add(group)
                .add(aim)
                .add(diveTicks)
                .add(diveFired ? 1 : 0)
                .add(spiralTicks)
                .add(ricochetsLeft)
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

    /** Unique among the units of an attempt. */
    int serial() {
        return serial;
    }

    /** Its ground group (a dock), or -1. */
    int group() {
        return group;
    }

    /** A turret's barrel, radians clockwise from straight down. */
    double aim() {
        return aim;
    }

    /** Whether it is a ground unit. */
    public boolean grounded() {
        return phase == Phase.GROUND;
    }

    /** Whether it is a diver in its pause before the dive (its telegraph). */
    public boolean paused() {
        return phase == Phase.HOLD && spec.dive().isPresent();
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
