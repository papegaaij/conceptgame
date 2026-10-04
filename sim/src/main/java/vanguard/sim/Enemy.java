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
        SPIRAL,
        /** Circling its carrier as an escort, or about to break off once the carrier ended. */
        ESCORT,
        /** A walker on its ground path. */
        WALK
    }

    /** A walker is at a waypoint this close to it and heads for the next. */
    private static final double ARRIVED = 6;

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
     * Where it faces: radians clockwise from straight down the screen. For the presentation only,
     * and not part of the state hash, except for a walker, whose facing decides its armour and its
     * fan (design/enemies/ground/scuttler).
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

    /** A spawner's steps since its centre crossed the top edge; -1 before. */
    private int broodTicks;

    /** Whether it entered as an escort; the escort's fields are in the state hash only then. */
    private boolean escort;
    /** The carrier it circles; null once that ended (or when it entered without one). */
    private Enemy carrier;

    private double escortRadius;
    private double escortRate;
    /** Steps left before an escort whose carrier ended breaks off toward the ship. */
    private int breakTicks;

    private double centreX;
    private double centreY;

    /** A walker's path; null for any other unit (the walker's fields are hashed only with one). */
    private WalkPath walkPath;
    /** The walk path's point it heads for; past the last it walks straight on. */
    private int waypoint;
    /** How far the ground scrolled since it entered (its path's points scroll with it). */
    private double scrolled;
    /** How far it walked over the ground, which drives its walk cycle. */
    private double walked;
    /** Whether it has been on the play field (a walker enters from outside it). */
    private boolean entered;

    private int spitTicks;

    /** @param unitSerial unique among the units of an attempt, for the shots that lock onto it */
    void spawn(Spawn plan, int unitSerial) {
        clearLevel04();
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
        if (plan.escort().isPresent()) {
            Spawn.Escort circle = plan.escort().get();
            phase = Phase.ESCORT;
            escort = true;
            escortRadius = circle.radius();
            escortRate = circle.radiansPerSecond();
            angle = circle.angle();
            breakTicks = SimStep.ticks(circle.breakSeconds());
            volleyTicks =
                    spec.gun().isPresent() ? SimStep.ticks(spec.gun().get().firstShotDelay()) + 1 : 0;
        }
        if (plan.walk().isPresent()) {
            walkPath = plan.walk().get();
            phase = Phase.WALK;
            waypoint = 1;
            x = prevX = walkPath.x(0);
            y = prevY = walkPath.y(0);
            facing = walkPath.points() > 1 ? heading(walkPath.x(1) - x, walkPath.y(1) - y) : StrictMath.PI;
            // The first volleys come half an interval after it walks onto the screen.
            volleyTicks =
                    spec.gun().isPresent() ? SimStep.ticks(spec.gun().get().intervalSeconds() / 2) : 0;
            EnemySpec.Walker walker = spec.walker().orElseThrow();
            spitTicks = walker.spit().isPresent()
                    ? SimStep.ticks(walker.spit().get().intervalSeconds() / 2)
                    : 0;
        }
    }

    /** The fields of Level 04's spawners, escorts and walkers back to a plain unit's. */
    private void clearLevel04() {
        broodTicks = -1;
        escort = false;
        carrier = null;
        escortRadius = 0;
        escortRate = 0;
        breakTicks = 0;
        centreX = 0;
        centreY = 0;
        walkPath = null;
        waypoint = 0;
        scrolled = 0;
        walked = 0;
        entered = false;
        spitTicks = 0;
    }

    /**
     * An escort starts circling {@code circled} (null: it entered without a carrier, so it breaks
     * off at once from where it is).
     */
    void escort(Enemy circled) {
        carrier = circled;
        centreX = circled != null ? circled.x : x;
        centreY = circled != null ? circled.y : y;
        x = prevX = centreX + escortRadius * Trig.cos(angle);
        y = prevY = centreY + escortRadius * Trig.sin(angle);
    }

    /** Whether it circles {@code unit} as an escort. */
    boolean escorts(Enemy unit) {
        return carrier == unit;
    }

    /** Its carrier ended: it circles the carrier's last position until it breaks off toward the ship. */
    void carrierEnded() {
        carrier = null;
    }

    /**
     * A unit released by a spawner at (atX, atY), flying straight out at (velX, velY) px/s until
     * it leaves the play field (design/enemies/air/brood-pod).
     */
    void hatch(EnemySpec enemySpec, int enemyKind, double atX, double atY, double velX, double velY, int unitSerial) {
        clearLevel04();
        spec = enemySpec;
        kind = enemyKind;
        serial = unitSerial;
        group = -1;
        aim = 0;
        inArc = false;
        diveTicks = 0;
        diveFired = false;
        diveShot = false;
        segment = 0;
        distance = 0;
        speed = Math.sqrt(velX * velX + velY * velY);
        phase = Phase.LEAVE;
        holdTicks = 0;
        orbit = Optional.empty();
        exit = Spawn.Exit.DOWN;
        hp = spec.hp();
        volleyTicks = 0;
        burstLeft = 0;
        burstTicks = 0;
        leadsTarget = false;
        carried = Optional.empty();
        spiralTicks = 0;
        ricochetsLeft = 0;
        x = prevX = atX;
        y = prevY = atY;
        vx = velX;
        vy = velY;
        facing = heading(velX, velY);
    }

    /**
     * A ground unit entering at the top edge at {@code atX}, in {@code inGroup} (-1 for none). Its
     * gun waits its first-shot delay from here.
     */
    void root(EnemySpec enemySpec, int enemyKind, double atX, int unitSerial, int inGroup) {
        clearLevel04();
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
        return move(shipX, shipY, groundScroll, shipX, shipY);
    }

    /**
     * As {@link #move(double, double, double)}, with a turret's barrel turning toward ({@code aimX},
     * {@code aimY}): the ship, or the convoy unit the target-the-objective hook chose this step.
     */
    boolean move(double shipX, double shipY, double groundScroll, double aimX, double aimY) {
        prevX = x;
        prevY = y;
        switch (phase) {
            case SPIRAL -> {
                spiral();
                return true;
            }
            case GROUND -> {
                y -= groundScroll;
                track(aimX, aimY);
                return y + spec.hitbox().height() / 2 > 0;
            }
            case WALK -> {
                y -= groundScroll;
                scrolled += groundScroll;
                walk();
                boolean on = PlayField.overlaps(x, y, spec.hitbox());
                entered |= on;
                return on || (!entered && y + spec.hitbox().height() / 2 > 0);
            }
            case ESCORT -> {
                if (carrier != null) {
                    centreX = carrier.x;
                    centreY = carrier.y;
                } else if (--breakTicks < 0) {
                    leave(shipX, shipY);
                    turn();
                    return true;
                }
                angle += escortRate * SimStep.SECONDS;
                x = centreX + escortRadius * Trig.cos(angle);
                y = centreY + escortRadius * Trig.sin(angle);
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
                if (spec.brood().isPresent() && (broodTicks >= 0 || y <= PlayField.HEIGHT)) {
                    broodTicks++;
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

    /**
     * One step of a walker over the ground (design/enemies/ground/scuttler): its facing turns toward
     * the path's next point by at most its turn rate, and it walks along its facing; a point it
     * reached, or one it can no longer turn into (behind it, inside its turning circle), is passed.
     * Past the last point it walks straight on.
     */
    private void walk() {
        EnemySpec.Walker walker = spec.walker().orElseThrow();
        if (waypoint < walkPath.points()) {
            double dx = walkPath.x(waypoint) - x;
            double dy = walkPath.y(waypoint) - scrolled - y;
            double distanceLeft = Math.sqrt(dx * dx + dy * dy);
            double delta = Math.IEEEremainder(heading(dx, dy) - facing, 2 * StrictMath.PI);
            double turning = 2 * walker.speed() / walker.turnRate();
            if (distanceLeft <= ARRIVED || (distanceLeft < turning && Math.abs(delta) > StrictMath.PI / 2)) {
                waypoint++;
            } else {
                double most = walker.turnRate() * SimStep.SECONDS;
                facing = Math.IEEEremainder(facing + Math.clamp(delta, -most, most), 2 * StrictMath.PI);
            }
        }
        double step = walker.speed() * SimStep.SECONDS;
        x -= Trig.sin(facing) * step;
        y -= Trig.cos(facing) * step;
        walked += step;
    }

    /**
     * Whether a direct shot flying at (shotVx, shotVy) glances off a walker's frontal armour: it
     * arrives from within the armour's arc of the walker's facing.
     */
    boolean glances(double shotVx, double shotVy) {
        if (walkPath == null || !(spec.walker().orElseThrow().frontArc() > 0)) {
            return false;
        }
        double length = Math.sqrt(shotVx * shotVx + shotVy * shotVy);
        if (length == 0) {
            return false;
        }
        // The facing as a direction (y up): a shot from the front flies against it.
        double against = (shotVx * Trig.sin(facing) + shotVy * Trig.cos(facing)) / length;
        return against >= Trig.cos(spec.walker().orElseThrow().frontArc());
    }

    /**
     * Counts down a walker's spit while it is on the screen; returns whether it spits this step:
     * when it is ready and the ship at (shipX, shipY) is more than its away angle off its facing.
     */
    boolean spit(double shipX, double shipY) {
        if (walkPath == null || !PlayField.overlaps(x, y, spec.hitbox())) {
            return false;
        }
        EnemySpec.Walker walker = spec.walker().orElseThrow();
        if (walker.spit().isEmpty()) {
            return false;
        }
        if (spitTicks > 0) {
            spitTicks--;
            return false;
        }
        double off = Math.abs(Math.IEEEremainder(heading(shipX - x, shipY - y) - facing, 2 * StrictMath.PI));
        if (off <= walker.awayRadians()) {
            return false;
        }
        spitTicks = SimStep.ticks(walker.spit().get().intervalSeconds());
        return true;
    }

    /** Whether a spawner's time ran out: it bursts on its own this step. */
    boolean burstDue() {
        return broodTicks >= 0
                && broodTicks >= SimStep.ticks(spec.brood().orElseThrow().afterSeconds());
    }

    /**
     * A spawner's seconds left before it bursts on its own (for the telegraph); its whole time
     * until its centre crosses the top edge, and infinite for other units.
     */
    public double burstSeconds() {
        if (spec.brood().isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        return spec.brood().get().afterSeconds() - Math.max(0, broodTicks) * SimStep.SECONDS;
    }

    /** A walker's distance walked over the ground in px (its walk cycle); 0 for other units. */
    public double walked() {
        return walked;
    }

    /** Whether it is a walker. */
    public boolean walking() {
        return phase == Phase.WALK;
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
     * A turret on the screen turns its barrel towards its target (the ship, or the convoy unit the
     * target-the-objective hook chose) by at most its turn rate while the target is inside its arc;
     * outside it, the barrel holds.
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
        if (spec.sine().isPresent()) {
            EnemySpec.Sine sine = spec.sine().get();
            x += sine.amplitude() * Trig.sin(2 * StrictMath.PI * distance / (speed * sine.periodSeconds()));
        }
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
        if (phase == Phase.WALK) {
            // A walker's fan, along its facing, while it is on the screen.
            if (spec.gun().isEmpty() || !PlayField.overlaps(x, y, spec.hitbox()) || --volleyTicks > 0) {
                return false;
            }
            volleyTicks = SimStep.ticks(spec.gun().get().intervalSeconds());
            return true;
        }
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
        boolean ready = phase == Phase.HOLD
                || (phase == Phase.GROUND && inArc)
                || (phase == Phase.ESCORT && y < PlayField.HEIGHT);
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

    /** Takes damage from a shot; returns whether it was destroyed. */
    boolean damage(double amount) {
        return damage(amount, false);
    }

    /**
     * Takes damage; returns whether it was destroyed. {@code fromAbove} marks damage that comes
     * down from above (bomb and shell blasts, the Airstrike): it ignores the frontal armour of a
     * walker such as the Scuttler (design/player/specials, decisions), which reads it once that
     * armour exists.
     */
    boolean damage(double amount, boolean fromAbove) {
        hp -= amount;
        return hp <= 0;
    }

    /** The hit points left. */
    double hp() {
        return hp;
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
        // Level 04's units add their state; the units of Levels 01-03 hash as before.
        if (spec.brood().isPresent()) {
            hash.add(broodTicks);
        }
        if (escort) {
            hash.add(breakTicks).add(centreX).add(centreY).add(carrier == null ? -1 : carrier.serial);
        }
        if (walkPath != null) {
            hash.add(facing)
                    .add(walked)
                    .add(waypoint)
                    .add(scrolled)
                    .add(spitTicks)
                    .add(entered ? 1 : 0);
        }
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

    /** Its ground group (a dock, a battery), or -1; for the presentation. */
    public int groupIndex() {
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
