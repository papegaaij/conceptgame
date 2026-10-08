package vanguard.sim;

/**
 * A swarm in flight (design/enemies/air/mote-swarm, user decision D8 = a of M5 part D): a boids
 * flock round a leader point. The leader point is not a unit: it flies the wave's route at the
 * route's speed and on past its end; {@code gap} s after a path's end it re-enters below the bottom
 * edge on the next loop-back's path at the dive speed (the members re-entering with it as a cloud),
 * until the loop-backs are flown. Every member is an ordinary {@link Enemy} (a kill, a bounty, a
 * chain step) that steers by separation, alignment and cohesion among the members within the
 * flock's radius and toward the leader point, turning at most its turn rate.
 *
 * <p>Deterministic and allocation-free: the members in entry order, every member's new heading
 * worked out from the positions at the start of the step before any of them moves, {@link
 * StrictMath#atan2} for the angles and the {@link Trig} table for their sines (StrictMath's sines
 * allocate), fixed arrays for at most {@link #MAX_MEMBERS} members. A destroyed member drops out
 * and the rest close up. Pooled.
 */
public final class Flock implements Hashed {
    /** The most members a flock has (at most 576 pair checks a step). */
    public static final int MAX_MEMBERS = 24;
    /** The weight of the separation rule (the data's weights are the other three's). */
    static final double SEPARATION = 3.0;
    /**
     * After the members flew, each pair closer than the separation moves apart by this share of the
     * shortfall (half each): the separation as a soft constraint as well as a steering rule, so the
     * cloud reads as motes, not a clump.
     */
    static final double RELAX = 0.3;
    /**
     * A member's pace against the flock's speed: this share flying away from the leader point or
     * within the radius of it, growing with its distance up to {@link #MOST_PACE} (stragglers catch up).
     */
    static final double LEAST_PACE = 0.8;

    static final double MOST_PACE = 1.3;
    /**
     * After the last path's end, members still on the play field this long are gone too (a member
     * left behind in a corner), s.
     */
    static final double LINGER_SECONDS = 6;

    private final Enemy[] members = new Enemy[MAX_MEMBERS];
    private final double[] px = new double[MAX_MEMBERS];
    private final double[] py = new double[MAX_MEMBERS];
    /** Each member's heading (radians, counter-clockwise from the +x axis, y up) and speed. */
    private final double[] heading = new double[MAX_MEMBERS];

    private final double[] pace = new double[MAX_MEMBERS];
    /** The headings and paces worked out in this step, applied once every member has its own. */
    private final double[] nextHeading = new double[MAX_MEMBERS];

    private final double[] nextPace = new double[MAX_MEMBERS];
    private int size;

    private EnemySpec.FlockSpec spec;
    private Spawn.Route route;
    private int serial;
    /** Steps since the wave entered. */
    private int ticks;
    /** The path the leader point flies: the route, then each loop-back's. */
    private FlightPath path;

    private int segment;
    private double distance;
    private double speed;
    /** Whether the leader point is past its path's end, flying straight on. */
    private boolean straight;

    private double leaderX;
    private double leaderY;
    private double leaderVx;
    private double leaderVy;
    /** The loop-backs flown so far. */
    private int loops;
    /** The loop-back that started in this step (from 1); 0 for none (not hashed: set and read within the step). */
    private int loopedBack;
    /** Steps since the last path's end, once no loop-back is left; -1 before. */
    private int doneTicks;
    /** The edge it entered from last: its wave's, the rear once it has looped back. */
    private WaveSpec.Entry entry = WaveSpec.Entry.FRONT;

    /** A wave's flock entering from {@code from}: the leader point at its route's start, no members yet. */
    void start(EnemySpec.FlockSpec flockSpec, int flockSerial, Spawn.Route flockRoute, WaveSpec.Entry from) {
        spec = flockSpec;
        serial = flockSerial;
        route = flockRoute;
        entry = from;
        ticks = 0;
        path = route.path();
        segment = 0;
        distance = 0;
        speed = route.speed();
        straight = false;
        leaderX = path.x(0, 0);
        leaderY = path.y(0, 0);
        double length = Math.sqrt(path.dx(0) * path.dx(0) + path.dy(0) * path.dy(0));
        leaderVx = length > 0 ? path.dx(0) / length * speed : 0;
        leaderVy = length > 0 ? path.dy(0) / length * speed : -speed;
        loops = 0;
        loopedBack = 0;
        doneTicks = -1;
        size = 0;
        for (int i = 0; i < MAX_MEMBERS; i++) {
            members[i] = null;
        }
    }

    /**
     * {@code enemy} joins as member {@code index} at (x, y), heading along the route's start at the
     * flock's speed; the caller has placed it there.
     */
    void member(int index, Enemy enemy, double x, double y) {
        members[index] = enemy;
        px[index] = x;
        py[index] = y;
        heading[index] = StrictMath.atan2(leaderVy, leaderVx);
        pace[index] = 1;
        size = Math.max(size, index + 1);
    }

    /** The heading of the route's start, radians clockwise from straight down (a joining member's facing). */
    double startFacing() {
        return StrictMath.atan2(-leaderVx, -leaderVy);
    }

    /**
     * One step: the leader point flies on (re-entering for a due loop-back), every member steers and
     * flies, and the members are moved to their new places.
     */
    void advance() {
        loopedBack = 0;
        ticks++;
        flyLeader();
        steer();
        double cruise = cruise();
        for (int i = 0; i < size; i++) {
            if (members[i] == null) {
                continue;
            }
            heading[i] = nextHeading[i];
            pace[i] = nextPace[i];
            double step = cruise * pace[i] * SimStep.SECONDS;
            px[i] += Trig.cos(heading[i]) * step;
            py[i] += Trig.sin(heading[i]) * step;
        }
        relax();
        for (int i = 0; i < size; i++) {
            if (members[i] != null) {
                // Facing: radians clockwise from straight down the screen.
                members[i].chainTo(px[i], py[i], StrictMath.atan2(-Trig.cos(heading[i]), -Trig.sin(heading[i])));
            }
        }
        if (doneTicks >= 0) {
            doneTicks++;
        }
    }

    /** The leader point's step along its path, straight on past it, or into the next loop-back. */
    private void flyLeader() {
        if (loops < route.loops().size() && ticks >= route.reentryAfter(loops)) {
            reenter();
            return;
        }
        if (straight) {
            leaderX += leaderVx * SimStep.SECONDS;
            leaderY += leaderVy * SimStep.SECONDS;
            return;
        }
        distance += speed * SimStep.SECONDS;
        if (distance >= path.length()) {
            segment = path.segmentAt(path.length(), segment);
            double dx = path.dx(segment);
            double dy = path.dy(segment);
            double length = Math.sqrt(dx * dx + dy * dy);
            leaderVx = length > 0 ? dx / length * speed : 0;
            leaderVy = length > 0 ? dy / length * speed : -speed;
            double over = distance - path.length();
            leaderX = path.x(segment, path.length()) + leaderVx / speed * over;
            leaderY = path.y(segment, path.length()) + leaderVy / speed * over;
            straight = true;
            if (loops >= route.loops().size()) {
                doneTicks = 0;
            }
        } else {
            segment = path.segmentAt(distance, segment);
            double dx = path.x(segment, distance) - leaderX;
            double dy = path.y(segment, distance) - leaderY;
            leaderX += dx;
            leaderY += dy;
            leaderVx = dx * SimStep.PER_SECOND;
            leaderVy = dy * SimStep.PER_SECOND;
        }
    }

    /**
     * A loop-back: the leader point re-enters on the next loop-back's path at the dive speed, and the
     * members off the play field re-enter with it, as the cloud they flew in (each at most the
     * flock's radius from the point), heading along the path's start.
     */
    private void reenter() {
        path = route.loops().get(loops);
        loops++;
        loopedBack = loops;
        entry = WaveSpec.Entry.REAR;
        speed = route.diveSpeed();
        segment = 0;
        distance = 0;
        straight = false;
        leaderX = path.x(0, 0);
        leaderY = path.y(0, 0);
        double length = Math.sqrt(path.dx(0) * path.dx(0) + path.dy(0) * path.dy(0));
        leaderVx = length > 0 ? path.dx(0) / length * speed : 0;
        leaderVy = length > 0 ? path.dy(0) / length * speed : speed;
        double sumX = 0;
        double sumY = 0;
        int away = 0;
        for (int i = 0; i < size; i++) {
            if (members[i] != null && !PlayField.overlaps(px[i], py[i], members[i].hitbox())) {
                sumX += px[i];
                sumY += py[i];
                away++;
            }
        }
        if (away == 0) {
            return;
        }
        double centreX = sumX / away;
        double centreY = sumY / away;
        double start = StrictMath.atan2(leaderVy, leaderVx);
        for (int i = 0; i < size; i++) {
            if (members[i] == null || PlayField.overlaps(px[i], py[i], members[i].hitbox())) {
                continue;
            }
            double dx = px[i] - centreX;
            double dy = py[i] - centreY;
            double d = Math.sqrt(dx * dx + dy * dy);
            double scale = d > spec.radius() ? spec.radius() / d : 1;
            // Behind the point (below it), so the cloud follows it in rather than leading it.
            px[i] = leaderX + dx * scale - leaderVx / speed * spec.radius();
            py[i] = leaderY + dy * scale - leaderVy / speed * spec.radius();
            heading[i] = start;
            pace[i] = 1;
            // Twice: the second leaves no in-between position from where it was.
            members[i].chainTo(px[i], py[i], StrictMath.atan2(-Trig.cos(start), -Trig.sin(start)));
            members[i].chainTo(px[i], py[i], members[i].facing());
        }
    }

    /** Pairs closer than the separation move apart by {@link #RELAX} of the shortfall, in entry order. */
    private void relax() {
        double separation = spec.separation();
        for (int i = 0; i < size; i++) {
            if (members[i] == null) {
                continue;
            }
            for (int j = i + 1; j < size; j++) {
                if (members[j] == null) {
                    continue;
                }
                double dx = px[j] - px[i];
                double dy = py[j] - py[i];
                double d2 = dx * dx + dy * dy;
                if (d2 >= separation * separation) {
                    continue;
                }
                double d = Math.sqrt(d2);
                double shift = RELAX * (separation - d) / 2;
                double ux = d > 0 ? dx / d : 1;
                double uy = d > 0 ? dy / d : 0;
                px[i] -= ux * shift;
                py[i] -= uy * shift;
                px[j] += ux * shift;
                py[j] += uy * shift;
            }
        }
    }

    /** The flock's cruise speed now: its speed, its dive speed after a loop-back. */
    private double cruise() {
        return loops > 0 ? spec.diveSpeed() : spec.speed();
    }

    /**
     * Every member's new heading and pace from the positions at the start of the step: away from
     * members closer than the separation, along the neighbours' mean heading (alignment), toward
     * their centre (cohesion) and toward the leader point, turning at most the turn rate.
     */
    private void steer() {
        double radius = spec.radius();
        double radius2 = radius * radius;
        double separation = spec.separation();
        double most = spec.turnRate() * SimStep.SECONDS;
        for (int i = 0; i < size; i++) {
            if (members[i] == null) {
                continue;
            }
            double sepX = 0;
            double sepY = 0;
            double alignX = 0;
            double alignY = 0;
            double sumX = 0;
            double sumY = 0;
            int neighbours = 0;
            for (int j = 0; j < size; j++) {
                if (j == i || members[j] == null) {
                    continue;
                }
                double dx = px[j] - px[i];
                double dy = py[j] - py[i];
                double d2 = dx * dx + dy * dy;
                if (d2 >= radius2) {
                    continue;
                }
                neighbours++;
                alignX += Trig.cos(heading[j]);
                alignY += Trig.sin(heading[j]);
                sumX += px[j];
                sumY += py[j];
                if (d2 < separation * separation) {
                    double d = Math.sqrt(d2);
                    double push = (separation - d) / separation;
                    if (d > 0) {
                        sepX -= dx / d * push;
                        sepY -= dy / d * push;
                    } else {
                        // On top of each other: the later member gives way to the side, by entry order.
                        sepX += j < i ? push : -push;
                    }
                }
            }
            double ownX = Trig.cos(heading[i]);
            double ownY = Trig.sin(heading[i]);
            double toX = leaderX - px[i];
            double toY = leaderY - py[i];
            double toLeader = Math.sqrt(toX * toX + toY * toY);
            double dirX = 0;
            double dirY = 0;
            if (toLeader > 0) {
                double pull = spec.leader() * Math.min(1, toLeader / radius) / toLeader;
                dirX += toX * pull;
                dirY += toY * pull;
            }
            if (neighbours > 0) {
                double align = Math.sqrt(alignX * alignX + alignY * alignY);
                if (align > 0) {
                    dirX += spec.alignment() * alignX / align;
                    dirY += spec.alignment() * alignY / align;
                }
                double cohesionX = (sumX / neighbours - px[i]) / radius;
                double cohesionY = (sumY / neighbours - py[i]) / radius;
                dirX += spec.cohesion() * cohesionX;
                dirY += spec.cohesion() * cohesionY;
                dirX += SEPARATION * sepX;
                dirY += SEPARATION * sepY;
            }
            double wanted = dirX == 0 && dirY == 0 ? heading[i] : StrictMath.atan2(dirY, dirX);
            double delta = Math.IEEEremainder(wanted - heading[i], 2 * StrictMath.PI);
            nextHeading[i] = Math.IEEEremainder(heading[i] + Math.clamp(delta, -most, most), 2 * StrictMath.PI);
            // Flying away from the leader point it eases off; toward it, it catches up the further off it is.
            boolean toward = toX * ownX + toY * ownY > 0;
            nextPace[i] = toward ? Math.clamp(toLeader / radius, LEAST_PACE, MOST_PACE) : LEAST_PACE;
        }
    }

    /**
     * Member {@code i} is gone (destroyed, or it left the screen): it drops out of the flock.
     */
    void drop(int i) {
        members[i] = null;
    }

    /** Whether it has flown its course: no loop-back left and the leader point past its last path's end. */
    boolean finished() {
        return doneTicks >= 0;
    }

    /** Whether its members still on the play field are gone too: {@link #LINGER_SECONDS} past its course. */
    boolean lingered() {
        return doneTicks >= SimStep.ticks(LINGER_SECONDS);
    }

    /** Whether any member is alive. */
    public boolean alive() {
        for (int i = 0; i < size; i++) {
            if (members[i] != null) {
                return true;
            }
        }
        return false;
    }

    /** The loop-back that started in this step, from 1; 0 for none. */
    int loopedBackNow() {
        return loopedBack;
    }

    /** The member slots, in entry order; a slot of a gone member is empty. */
    public int size() {
        return size;
    }

    /** Member {@code i}, or null when it is gone. */
    public Enemy member(int i) {
        return members[i];
    }

    /** The leader point (not a unit): where the flock steers to; for the presentation's swarm sound. */
    public double leaderX() {
        return leaderX;
    }

    public double leaderY() {
        return leaderY;
    }

    /** The loop-backs flown so far. */
    public int loopsFlown() {
        return loops;
    }

    /** The loop-backs its route has. */
    public int loopsPlanned() {
        return route.loops().size();
    }

    /** Whether it dives: after a loop-back, at the dive speed. */
    public boolean diving() {
        return loops > 0;
    }

    /** The edge it entered from last: its wave's, the rear once it has looped back. */
    public WaveSpec.Entry entry() {
        return entry;
    }

    public EnemySpec.FlockSpec spec() {
        return spec;
    }

    /** Unique among the flocks of an attempt. */
    int serial() {
        return serial;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(serial)
                .add(ticks)
                .add(segment)
                .add(distance)
                .add(speed)
                .add(straight ? 1 : 0)
                .add(leaderX)
                .add(leaderY)
                .add(leaderVx)
                .add(leaderVy)
                .add(loops)
                .add(doneTicks)
                .add(entry.ordinal())
                .add(size);
        for (int i = 0; i < size; i++) {
            hash.add(members[i] == null ? -1 : members[i].serial())
                    .add(px[i])
                    .add(py[i])
                    .add(heading[i])
                    .add(pace[i]);
        }
    }
}
