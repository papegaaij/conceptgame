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
        WALK,
        /** A member of a segment chain, placed by its {@link Chain}. */
        CHAIN,
        /** M5 part D: a {@code rear ambush} unit on its way (design/enemies/air/wraith); see {@link AmbushPhase}. */
        AMBUSH,
        /** M5 part D: a member of a swarm, placed by its {@link Flock}. */
        FLOCK
    }

    /**
     * M5 part D: where a {@code rear ambush} unit is on its way (design/enemies/air/wraith), for the
     * presentation and the tests; {@link #NONE} for any other unit.
     */
    public enum AmbushPhase {
        NONE,
        /** Cloaked, straight down its lane from the top edge, past the ship and off the bottom edge. */
        SWOOP,
        /** Cloaked, below the bottom edge; the edge's warning runs ahead of its re-entry. */
        GAP,
        /** Cloaked, back up its lane from below the bottom edge to its hold point. */
        RISE,
        /** At its hold point: the decloak flash, on its decloaked layer, its gun silent. */
        DECLOAK,
        /** Decloaked, holding at its hold point, its bursts up the screen. */
        HOLD,
        /** Decloaked, out up the nearer side lane and off the top edge. */
        EXIT
    }

    /** A walker is at a waypoint this close to it and heads for the next. */
    private static final double ARRIVED = 6;

    private EnemySpec spec;
    /** Its hit box: its stat block's, or a chain member's own (the segments taper). */
    private Hitbox box;
    /** The chain it is a member of; null for any other unit (the link is hashed only with one). */
    private Chain chain;
    /** Its place in its chain: 0 the head. */
    private int link;

    /** A sweep's state (design/enemies/air/mantis): steps to the next telegraph, -1 when none comes. */
    private int sweepWait;
    /** Steps into the current telegraph and sweep; -1 while none runs. */
    private int sweepTicks;
    /** The sweep's centre: radians clockwise from straight down. */
    private double sweepCentre;
    /** Whether the current sweep hit the ship. */
    private boolean sweepHit;

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
    /**
     * Simulation steps since it last took damage, for the Targeting computer's HP bar; {@link
     * Integer#MAX_VALUE} before its first hit. For the presentation only, not part of the state hash.
     */
    private int ticksSinceHit = Integer.MAX_VALUE;

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
    /** A boss's launched unit (part G): steps it glides out before it holds, and how long it then holds; 0 for none. */
    private int glideTicks;

    private int glideHoldTicks;

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
    /**
     * M5 part B: its wave's volley clock (a staggered walker wave, design/enemies/ground/creeper)
     * and its place in the wave (from 0, in entry order); -1 and 0 without one.
     */
    private int volleyGroup = -1;

    private int volleyUnit;
    /** The edge its wave entered from (a wingman's formation follows the sides and rear waves); front for any other unit. */
    private WaveSpec.Entry entry = WaveSpec.Entry.FRONT;

    /** {@link #spawnStep}: nothing happens. */
    static final int SPAWN_NONE = 0;
    /** {@link #spawnStep}: the iris starts to open (the telegraph). */
    static final int SPAWN_TELEGRAPH = 1;
    /** {@link #spawnStep}: the spawner releases its units now. */
    static final int SPAWN_RELEASE = 2;

    /**
     * M5 part C, a periodic spawner (design/enemies/ground/hive-node): steps until its next release,
     * -1 before its centre crossed the top edge.
     */
    private int spawnWait;
    /** Steps since its iris started to open; -1 while it is shut. */
    private int irisTicks;
    /** Whether the iris now opening releases its units at its end. */
    private boolean releasing;

    /** M5 part C, a pounce (design/enemies/ground/ravager): steps since take-off; -1 while it is not leaping. */
    private int leapTicks;
    /** Steps before it may leap again (from landing). */
    private int pounceWait;
    /** The leap's steps, and the steps of its air window [{@link #airFrom}, {@link #airTo}). */
    private int leapTotal;

    private int airFrom;
    private int airTo;
    /**
     * Where the leap started, where it lands and the point its middle (the apex and the air window)
     * passes over: the ship's position at take-off (user decision 2026-10-07, the overshoot).
     */
    private double leapFromX;

    private double leapFromY;
    private double leapToX;
    private double leapToY;
    private double leapOverX;
    private double leapOverY;
    /** What this leap touched already, as {@link #TOUCHED_SHIP} and {@link #TOUCHED_WINGMAN} bits: once each per leap. */
    private int pounceTouched;
    /** Whether it landed in this step (not hashed: set and read within the step). */
    private boolean landed;

    /** M5 part C: its wave's tag; "" for none (not hashed: it follows from the plan). */
    private String tag = "";

    /** M5 part D: an ambush unit's plan; null for any other unit (its fields are hashed only with one). */
    private Spawn.Ambush ambush;

    private AmbushPhase ambushPhase = AmbushPhase.NONE;
    /** Steps since it entered, until its re-entry. */
    private int ambushTicks;
    /** Steps from its entry to its re-entry at the bottom edge (the end of its edge warning). */
    private int reentryTicks;
    /** Whether it decloaked in this step (not hashed: set and read within the step). */
    private boolean decloaked;
    /** The volleys it started in its hold. */
    private int ambushVolleys;
    /**
     * M5 part D: the volleys of an ambush unit's hold (design/enemies/air/wraith: "twice in its
     * hold"); hard's longer hold does not add a third.
     */
    static final int AMBUSH_VOLLEYS = 2;

    /** M5 part D: the flock it is a member of; null for any other unit (hashed only with one). */
    private Flock flock;
    /** Its place in its flock, from 0 in entry order. */
    private int member;

    /**
     * M5 part D: the air escort's units its body overlapped in the last step, one bit per unit (bit k
     * for unit k): a contact hurts a unit once, when it starts (design/allies, evacuation shuttle).
     */
    private int allyContacts;

    /**
     * M5 part E: its drift on top of the scroll, px/s (y up): a field unit's along its wave's current
     * (design/enemies/naval/driftjelly), a raft's along its nest's (design/enemies/naval/reef-spitter);
     * hashed only while it drifts.
     */
    private double driftX;

    private double driftY;
    /** M5 part E, a submerging unit (the Driftjelly; hashed only with a submerge): whether its current layer is {@link Layer#SUB}. */
    private boolean submerged;
    /** Its swap timer's random state, its own seed (a SplitMix64 state stepped by {@link #nextSwap()}). */
    private long swapSeed;
    /** Steps until its next swap starts. */
    private int swapWait;
    /** Steps into the swap running now; -1 while none runs. */
    private int swapTicks = -1;
    /** Whether the swap running now goes down (from the surface). */
    private boolean swapDown;
    /** M5 part E, a proximity ring (hashed only with one): steps until it may fire its ring again; 0 ready. */
    private int ringWait;

    /** A pounce's contact with the ship. */
    static final int TOUCHED_SHIP = 1;
    /** A pounce's contact with the wingman. */
    static final int TOUCHED_WINGMAN = 2;

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
        box = spec.hitbox();
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
        ticksSinceHit = Integer.MAX_VALUE;
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
            if (spec.pounce().isPresent()) {
                EnemySpec.Pounce pounce = spec.pounce().get();
                leapTotal = Math.max(1, SimStep.ticks(pounce.leapSeconds()));
                int air = Math.min(leapTotal, SimStep.ticks(pounce.airSeconds()));
                // The middle of the leap: its steps run 1 .. leapTotal.
                airFrom = 1 + (leapTotal - air) / 2;
                airTo = airFrom + air;
            }
        }
        if (plan.field().isPresent()) {
            // M5 part E: a field unit lies on the sea from its appearance, scrolling and drifting.
            Spawn.Field field = plan.field().get();
            phase = Phase.GROUND;
            x = prevX = field.x();
            y = prevY = field.y();
            driftX = field.vx();
            driftY = field.vy();
            facing = 0;
            submerge(field.submerged(), field.seed(), true);
        }
        if (plan.ambush().isPresent()) {
            ambush = plan.ambush().get();
            phase = Phase.AMBUSH;
            ambushPhase = AmbushPhase.SWOOP;
            ambushTicks = 0;
            reentryTicks = Math.max(1, plan.ambushReentryTick() - plan.tick());
        }
    }

    /** The fields of Level 04's spawners, escorts and walkers back to a plain unit's. */
    private void clearLevel04() {
        entry = WaveSpec.Entry.FRONT;
        tag = "";
        chain = null;
        link = 0;
        sweepWait = -1;
        sweepTicks = -1;
        sweepCentre = 0;
        sweepHit = false;
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
        volleyGroup = -1;
        volleyUnit = 0;
        glideTicks = 0;
        glideHoldTicks = 0;
        spawnWait = -1;
        irisTicks = -1;
        releasing = false;
        leapTicks = -1;
        pounceWait = 0;
        leapTotal = 0;
        airFrom = 0;
        airTo = 0;
        leapFromX = 0;
        leapFromY = 0;
        leapToX = 0;
        leapToY = 0;
        leapOverX = 0;
        leapOverY = 0;
        pounceTouched = 0;
        allyContacts = 0;
        landed = false;
        ambush = null;
        ambushPhase = AmbushPhase.NONE;
        ambushTicks = 0;
        reentryTicks = 0;
        decloaked = false;
        ambushVolleys = 0;
        flock = null;
        member = 0;
        driftX = 0;
        driftY = 0;
        submerged = false;
        swapSeed = 0;
        swapWait = 0;
        swapTicks = -1;
        swapDown = false;
        ringWait = 0;
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
        box = spec.hitbox();
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
        ticksSinceHit = Integer.MAX_VALUE;
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
     * A unit a boss's window launches at (atX, atY) (design/enemies/bosses, part G): it flies
     * straight out at (velX, velY) px/s; with a glide it holds after {@code glideSeconds} for its
     * hover time (the middle of its range), its gun firing, and then leaves down the screen at that
     * speed; without one it flies on until it leaves the play field.
     */
    void launch(
            EnemySpec enemySpec,
            int enemyKind,
            double atX,
            double atY,
            double velX,
            double velY,
            double glideSeconds,
            int unitSerial) {
        hatch(enemySpec, enemyKind, atX, atY, velX, velY, unitSerial);
        if (glideSeconds > 0 && enemySpec.hover().isPresent()) {
            Range hover = enemySpec.hover().get().seconds();
            glideTicks = Math.max(1, SimStep.ticks(glideSeconds));
            glideHoldTicks = Math.max(1, SimStep.ticks((hover.min() + hover.max()) / 2));
        }
    }

    /**
     * A ground unit entering at the top edge at {@code atX}, in {@code inGroup} (-1 for none). Its
     * gun waits its first-shot delay from here.
     */
    void root(EnemySpec enemySpec, int enemyKind, double atX, int unitSerial, int inGroup) {
        root(enemySpec, enemyKind, atX, unitSerial, inGroup, 0, -1);
    }

    /**
     * As {@link #root(EnemySpec, int, double, int, int)}; M5 part E: a unit whose stat block drifts
     * (a raft, design/enemies/naval/reef-spitter) drifts at that speed along its nest's current,
     * given as the current's direction ({@code currentX}, {@code currentY}: its sine and minus its
     * cosine from straight down, worked out once when the level is set up, since {@link StrictMath}'s
     * sine allocates).
     */
    void root(
            EnemySpec enemySpec,
            int enemyKind,
            double atX,
            int unitSerial,
            int inGroup,
            double currentX,
            double currentY) {
        clearLevel04();
        spec = enemySpec;
        box = spec.hitbox();
        kind = enemyKind;
        serial = unitSerial;
        group = inGroup;
        phase = Phase.GROUND;
        x = prevX = atX;
        y = prevY = PlayField.HEIGHT + box.height() / 2;
        hp = spec.hp();
        ticksSinceHit = Integer.MAX_VALUE;
        aim = 0;
        facing = 0;
        inArc = false;
        burstLeft = 0;
        leadsTarget = false;
        carried = Optional.empty();
        volleyTicks = spec.gun().isPresent() ? SimStep.ticks(spec.gun().get().firstShotDelay()) + 1 : 0;
        diveFired = diveShot = false;
        diveTicks = 0;
        if (spec.drift() > 0) {
            driftX = spec.drift() * currentX;
            driftY = spec.drift() * currentY;
        }
    }

    /**
     * M5 part E: a submerging unit starts {@code under} the surface or on it, its swap timer seeded
     * with {@code seed}; a field's units start at a seeded point of their first wait ({@code
     * anyPoint}: a wait drawn from 0 to the longest), so a field swaps on the screen and not in step.
     */
    private void submerge(boolean under, long seed, boolean anyPoint) {
        if (spec.submerge().isEmpty()) {
            return;
        }
        EnemySpec.Submerge submerge = spec.submerge().get();
        submerged = under;
        swapSeed = seed;
        swapTicks = -1;
        swapDown = false;
        double wait = anyPoint
                ? nextSwap() * submerge.everyMax()
                : submerge.everyMin() + nextSwap() * (submerge.everyMax() - submerge.everyMin());
        swapWait = Math.max(1, SimStep.ticks(wait));
    }

    /** The next draw of its swap timer, uniform in [0, 1): SplitMix64 on its own state. */
    private double nextSwap() {
        long z = (swapSeed += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return ((z ^ (z >>> 31)) >>> 11) * 0x1.0p-53;
    }

    /**
     * M5 part E: one real step of a submerging unit's timer: a running swap goes on (the layer flips
     * at its middle) or ends, drawing the next wait; otherwise the wait counts down to the next swap.
     */
    private void swapStep() {
        EnemySpec.Submerge submerge = spec.submerge().get();
        int total = Math.max(2, SimStep.ticks(submerge.swapSeconds()));
        if (swapTicks >= 0) {
            if (++swapTicks == total / 2) {
                submerged = swapDown;
            }
            if (swapTicks >= total) {
                swapTicks = -1;
                double wait = submerge.everyMin() + nextSwap() * (submerge.everyMax() - submerge.everyMin());
                swapWait = Math.max(1, SimStep.ticks(wait));
            }
        } else if (--swapWait <= 0) {
            swapTicks = 0;
            swapDown = !submerged;
        }
    }

    /** M5 part E: whether a submerging unit's current layer is {@link Layer#SUB}, under the surface. */
    public boolean submerged() {
        return submerged;
    }

    /**
     * M5 part E: how far a submerging unit is through the swap running now, between the previous
     * and the current step, 0 to 1 (its layer flips at 0.5); -1 while none runs (for the looks).
     */
    public double swap(double alpha) {
        if (swapTicks < 0 || spec.submerge().isEmpty()) {
            return -1;
        }
        int total = Math.max(2, SimStep.ticks(spec.submerge().get().swapSeconds()));
        return Math.min(1, Math.max(0, (swapTicks - 1 + alpha) / total));
    }

    /** M5 part E: whether the swap running now goes down (it dives); false for one that surfaces or none. */
    public boolean diving() {
        return swapTicks >= 0 && swapDown;
    }

    /** M5 part E: whether its proximity ring may fire now (its cooldown from the last ring is over). */
    boolean ringReady() {
        return ringWait == 0;
    }

    /** M5 part E: it fired its proximity ring: the cooldown starts. */
    void ringFired() {
        ringWait = Math.max(1, SimStep.ticks(spec.ring().orElseThrow().cooldownSeconds()));
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
        if (ticksSinceHit < Integer.MAX_VALUE) {
            ticksSinceHit++;
        }
        // M5 part E: the swap timer and the ring's cooldown run on the real steps (a halted arena too).
        if (spec.submerge().isPresent()) {
            swapStep();
        }
        if (ringWait > 0) {
            ringWait--;
        }
        if (phase == Phase.CHAIN || phase == Phase.FLOCK) {
            // Its chain or its flock placed it this step already.
            return true;
        }
        prevX = x;
        prevY = y;
        switch (phase) {
            case SPIRAL -> {
                spiral();
                return true;
            }
            case AMBUSH -> {
                if (!ambushStep()) {
                    return false;
                }
            }
            case GROUND -> {
                y -= groundScroll;
                if (driftX != 0 || driftY != 0) {
                    // M5 part E: a field unit or a raft drifts on the current, on top of the scroll.
                    x += driftX * SimStep.SECONDS;
                    y += driftY * SimStep.SECONDS;
                    if (x + box.width() / 2 < 0 || x - box.width() / 2 > PlayField.WIDTH) {
                        return false;
                    }
                }
                track(aimX, aimY);
                return y + box.height() / 2 > 0;
            }
            case WALK -> {
                scrolled += groundScroll;
                landed = false;
                if (leapTicks >= 0) {
                    // In a pounce it flies over the ship's position at take-off and lands beyond it.
                    leap();
                } else {
                    y -= groundScroll;
                    walk();
                    if (pounceWait > 0) {
                        pounceWait--;
                    }
                }
                boolean on = PlayField.overlaps(x, y, box);
                entered |= on;
                return on || (!entered && y + box.height() / 2 > 0);
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
                if (spec.sweep().isPresent()) {
                    sweep(shipX, shipY);
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
                if (glideTicks > 0 && --glideTicks == 0) {
                    // A launched unit's glide is over: it holds, its gun firing, then leaves down.
                    holdTicks = glideHoldTicks;
                    hold();
                    return true;
                }
                if (spec.dive().isPresent() && !diveFired) {
                    EnemySpec.Dive dive = spec.dive().get();
                    boolean passed = prevY > shipY && y <= shipY;
                    if (passed || ++diveTicks >= SimStep.ticks(dive.fireAfterSeconds())) {
                        diveFired = true;
                        diveShot = true;
                    }
                }
                return PlayField.overlaps(x, y, box);
            }
        }
        turn();
        return true;
    }

    /**
     * One step of a rear ambush unit's way (design/enemies/air/wraith, user decision D6 = a of M5
     * part D): its swoop down its lane and off the bottom edge, the gap below it, its rise back up to
     * its hold point, the decloak flash and the hold there (its gun counting its first-shot delay
     * from the stop), then its exit up the side lane; returns false once it has left the top edge.
     */
    private boolean ambushStep() {
        decloaked = false;
        switch (ambushPhase) {
            case SWOOP, GAP -> {
                if (ambushPhase == AmbushPhase.SWOOP) {
                    distance += speed * SimStep.SECONDS;
                    if (distance >= path.length()) {
                        distance = path.length();
                        ambushPhase = AmbushPhase.GAP;
                    }
                    place();
                }
                if (++ambushTicks >= reentryTicks) {
                    // It re-enters its lane where its swoop left it, below the bottom edge.
                    ambushPhase = AmbushPhase.RISE;
                    path = ambush.rise();
                    segment = 0;
                    distance = 0;
                    place();
                }
            }
            case RISE -> {
                distance += speed * SimStep.SECONDS;
                if (distance >= path.length()) {
                    distance = path.length();
                    place();
                    decloak();
                } else {
                    place();
                }
            }
            case DECLOAK -> {
                if (--holdTicks <= 0) {
                    ambushPhase = AmbushPhase.HOLD;
                    holdTicks = Math.max(1, SimStep.ticks(ambush.holdSeconds()));
                }
            }
            case HOLD -> {
                if (--holdTicks <= 0) {
                    ambushPhase = AmbushPhase.EXIT;
                    burstLeft = 0;
                    path = ambush.exit();
                    segment = 0;
                    distance = 0;
                    speed = ambush.exitSpeed();
                }
            }
            case EXIT -> {
                distance += speed * SimStep.SECONDS;
                if (distance >= path.length()) {
                    return false;
                }
                place();
            }
            case NONE -> throw new IllegalStateException("an ambush unit without its phase");
        }
        return true;
    }

    /** It stops at its hold point and decloaks: the flash starts, its gun counts from here. */
    private void decloak() {
        decloaked = true;
        burstLeft = 0;
        // + 1: the gun already counts down in the step the unit stops, as a hover's (see hold()).
        volleyTicks = spec.gun().isPresent() ? SimStep.ticks(spec.gun().get().firstShotDelay()) + 1 : 0;
        int flash = SimStep.ticks(ambush.flashSeconds());
        if (flash > 0) {
            ambushPhase = AmbushPhase.DECLOAK;
            holdTicks = flash;
        } else {
            ambushPhase = AmbushPhase.HOLD;
            holdTicks = Math.max(1, SimStep.ticks(ambush.holdSeconds()));
        }
    }

    /** M5 part D: where a rear ambush unit is on its way; {@link AmbushPhase#NONE} for any other unit. */
    public AmbushPhase ambushPhase() {
        return ambushPhase;
    }

    /** M5 part D: whether it decloaked in this step (the {@link SimEvents.Type#DECLOAK} event). */
    boolean decloakedNow() {
        return decloaked;
    }

    /**
     * M5 part D: whether it flies cloaked (design/enemies/air/wraith): a unit with a cloak before its
     * decloak, drawn as a shimmer on its stat block's layer; false for any other unit.
     */
    public boolean cloaked() {
        return spec.cloak().isPresent() && !decloakedPhase();
    }

    /**
     * M5 part D: how far its decloak is, between the previous and the current step ({@code alpha} in
     * [0, 1]): 0 while it is cloaked, rising evenly to 1 over the flash, 1 after it and for a unit
     * without a cloak.
     */
    public double decloak(double alpha) {
        if (spec.cloak().isEmpty() || ambushPhase.compareTo(AmbushPhase.HOLD) >= 0) {
            return 1;
        }
        if (ambushPhase != AmbushPhase.DECLOAK) {
            return 0;
        }
        int flash = Math.max(1, SimStep.ticks(ambush.flashSeconds()));
        return Math.clamp((flash - holdTicks + alpha) / flash, 0.0, 1.0);
    }

    /** Whether it is at or past its decloak (an ambush unit's flash, hold and exit). */
    private boolean decloakedPhase() {
        return ambushPhase.compareTo(AmbushPhase.DECLOAK) >= 0;
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
     * One step of a pounce's leap: its first half to the point it passes over (the ship's position at
     * take-off), its second half on to its landing point; at its end it lands and waits its interval.
     */
    private void leap() {
        leapTicks++;
        double p = (double) leapTicks / leapTotal;
        if (2 * leapTicks <= leapTotal) {
            x = leapFromX + (leapOverX - leapFromX) * 2 * p;
            y = leapFromY + (leapOverY - leapFromY) * 2 * p;
        } else {
            x = leapOverX + (leapToX - leapOverX) * (2 * p - 1);
            y = leapOverY + (leapToY - leapOverY) * (2 * p - 1);
        }
        if (leapTicks >= leapTotal) {
            leapTicks = -1;
            pounceWait = SimStep.ticks(spec.pounce().orElseThrow().intervalSeconds());
            landed = true;
        }
    }

    /**
     * M5 part C (design/enemies/ground/ravager): a walker with a pounce leaps at the ship at
     * (shipX, shipY) when it is on the screen, ready (not leaping, its interval since the last
     * landing over) and the ship's centre is within its range; returns whether it took off now.
     * The leap overshoots (user decision 2026-10-07): its middle, the apex and the air window, passes
     * over the ship's position at take-off and it lands as far beyond, its landing point kept on the
     * play field (its centre at least half its hit box inside every edge; the second half of the leap
     * is then shorter), so a ship that holds still is touched and one that moves away is not.
     */
    boolean pounce(double shipX, double shipY) {
        if (walkPath == null
                || spec.pounce().isEmpty()
                || leapTicks >= 0
                || pounceWait > 0
                || !PlayField.overlaps(x, y, box)) {
            return false;
        }
        double dx = shipX - x;
        double dy = shipY - y;
        double range = spec.pounce().get().range();
        if (dx * dx + dy * dy > range * range) {
            return false;
        }
        leapTicks = 0;
        leapFromX = x;
        leapFromY = y;
        leapOverX = shipX;
        leapOverY = shipY;
        double halfWidth = box.width() / 2;
        double halfHeight = box.height() / 2;
        leapToX = Math.clamp(2 * shipX - x, halfWidth, PlayField.WIDTH - halfWidth);
        leapToY = Math.clamp(2 * shipY - y, halfHeight, PlayField.HEIGHT - halfHeight);
        pounceTouched = 0;
        if (dx != 0 || dy != 0) {
            facing = heading(dx, dy);
        }
        return true;
    }

    /** Whether it is in a pounce's leap. */
    public boolean leaping() {
        return leapTicks >= 0;
    }

    /** Whether its pounce landed in this step. */
    boolean landedNow() {
        return landed;
    }

    /**
     * How far its leap is, from 0 at take-off to 1 at landing, between the previous and the current
     * step ({@code alpha} in [0, 1]); 0 while it is not leaping.
     */
    public double leapProgress(double alpha) {
        if (leapTicks < 0) {
            return 0;
        }
        return Math.clamp((leapTicks - 1 + alpha) / leapTotal, 0.0, 1.0);
    }

    /**
     * Its drawn scale in a leap (design/enemies/ground/ravager): 1 at take-off and landing, its
     * pounce's scale at the apex, along a parabola; 1 while it is not leaping.
     */
    public double leapScale(double alpha) {
        if (leapTicks < 0) {
            return 1;
        }
        double p = leapProgress(alpha);
        return 1 + (spec.pounce().orElseThrow().scale() - 1) * 4 * p * (1 - p);
    }

    /**
     * The layer it is on now (M5 part C): its stat block's, except {@link Layer#AIR} in the air
     * window of a pounce's leap and (M5 part D) its cloak's layer from its decloak flash's start on.
     * Every hit, contact and targeting rule reads this.
     */
    public Layer layer() {
        if (leapTicks >= airFrom && leapTicks < airTo) {
            return Layer.AIR;
        }
        // M5 part D: a cloaked unit is on its decloaked layer from its flash's start on.
        if (spec.cloak().isPresent() && decloakedPhase()) {
            return spec.cloak().get().layer();
        }
        // M5 part E: a submerging unit is under the surface from its dive's middle to its rise's.
        if (submerged) {
            return Layer.SUB;
        }
        return spec.layer();
    }

    /**
     * A pounce touches {@code what} ({@link #TOUCHED_SHIP} or {@link #TOUCHED_WINGMAN}): returns
     * whether it is the leap's first touch of it, which deals its contact; once per leap.
     */
    boolean pounceTouch(int what) {
        if ((pounceTouched & what) != 0) {
            return false;
        }
        pounceTouched |= what;
        return true;
    }

    /**
     * One step of a periodic spawner's cycle (design/enemies/ground/hive-node), after it moved:
     * {@link #SPAWN_RELEASE} when its units go now, {@link #SPAWN_TELEGRAPH} when its iris starts to
     * open, else {@link #SPAWN_NONE}. The cycle starts as its centre crosses the top edge; an
     * opening due while the ship at (shipX, shipY) is within the spawner's shut distance, or while
     * its centre is below the bottom edge, is skipped, and so is a release the ship came that close
     * to during the telegraph (the iris shuts again without it).
     */
    int spawnStep(double shipX, double shipY) {
        EnemySpec.Spawner spawner = spec.spawner().orElseThrow();
        int telegraph = SimStep.ticks(spawner.telegraphSeconds());
        if (irisTicks >= 0 && ++irisTicks >= 2 * Math.max(1, telegraph)) {
            irisTicks = -1;
        }
        if (spawnWait < 0) {
            if (y > PlayField.HEIGHT) {
                return SPAWN_NONE;
            }
            spawnWait = SimStep.ticks(spawner.everySeconds());
            return SPAWN_NONE;
        }
        spawnWait--;
        double dx = shipX - x;
        double dy = shipY - y;
        boolean clear = dx * dx + dy * dy > spawner.shutWithin() * spawner.shutWithin() && y >= 0;
        int result = SPAWN_NONE;
        if (spawnWait == telegraph) {
            releasing = clear;
            if (clear) {
                irisTicks = 0;
                result = SPAWN_TELEGRAPH;
            }
        }
        if (spawnWait == 0) {
            spawnWait = SimStep.ticks(spawner.everySeconds());
            if (releasing && clear) {
                result = SPAWN_RELEASE;
            }
            releasing = false;
        }
        return result;
    }

    /**
     * How far its iris is open (design/enemies/ground/hive-node): from 0 (shut) it opens evenly over
     * the telegraph to 1 at the release and shuts again over as long; 0 for other units.
     */
    public double iris() {
        if (irisTicks < 0) {
            return 0;
        }
        int telegraph = Math.max(1, SimStep.ticks(spec.spawner().orElseThrow().telegraphSeconds()));
        return irisTicks <= telegraph
                ? (double) irisTicks / telegraph
                : Math.max(0, (double) (2 * telegraph - irisTicks) / telegraph);
    }

    /** Whether its iris is opening for a release now: the spawn's telegraph. */
    public boolean irisOpening() {
        return releasing && irisTicks >= 0;
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
        if (walkPath == null || !PlayField.overlaps(x, y, box)) {
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

    /**
     * A walker of a staggered wave joins its wave's volley clock {@code clock} as unit {@code unit}
     * (from 0, in entry order); -1: it keeps its own clock.
     */
    void volley(int clock, int unit) {
        volleyGroup = walkPath != null ? clock : -1;
        volleyUnit = volleyGroup >= 0 ? unit : 0;
    }

    /** Its wave's volley clock; -1 for a unit on its own clock. */
    int volleyGroup() {
        return volleyGroup;
    }

    /**
     * Whether a walker of a staggered wave fires its fan this step (M5 part B, design/enemies/
     * ground/creeper): the wave's volley clock {@code clocks[volleyGroup]} starts as the first of
     * its units is on the screen; the first volley comes half an interval later and then one every
     * interval, unit i firing i × the stagger after the volley's start. A unit off the screen skips
     * its turn.
     */
    boolean staggeredVolley(int[] clocks) {
        if (spec.gun().isEmpty() || !PlayField.overlaps(x, y, box)) {
            return false;
        }
        if (clocks[volleyGroup] == 0) {
            clocks[volleyGroup] = 1;
        }
        EnemyGun gun = spec.gun().get();
        int since = clocks[volleyGroup]
                - SimStep.ticks(gun.intervalSeconds() / 2)
                - SimStep.ticks(volleyUnit * spec.walker().orElseThrow().staggerSeconds());
        return since >= 0 && since % SimStep.ticks(gun.intervalSeconds()) == 0;
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
        double half = box.width() / 2;
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
        if (spec.gun().isEmpty() || !PlayField.overlaps(x, y, box)) {
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
        if (spec.sweep().isPresent()) {
            sweepWait = SimStep.ticks(spec.sweep().get().firstDelaySeconds());
        }
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
        if (phase == Phase.CHAIN) {
            // A chain's head fires its fan while it is on the screen.
            if (spec.gun().isEmpty() || !PlayField.overlaps(x, y, box) || --volleyTicks > 0) {
                return false;
            }
            volleyTicks = SimStep.ticks(spec.gun().get().intervalSeconds());
            return true;
        }
        if (phase == Phase.WALK) {
            // A walker's fan, along its facing, while it is on the screen.
            if (spec.gun().isEmpty() || !PlayField.overlaps(x, y, box) || --volleyTicks > 0) {
                return false;
            }
            volleyTicks = SimStep.ticks(spec.gun().get().intervalSeconds());
            return true;
        }
        if (spec.gun().isPresent() && spec.gun().get().mine().isPresent()) {
            // A mine layer drops a spore at its interval wherever it flies on the screen.
            if (phase == Phase.GROUND || !PlayField.overlaps(x, y, box) || --volleyTicks > 0) {
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
                || (phase == Phase.ESCORT && y < PlayField.HEIGHT)
                || ambushPhase == AmbushPhase.DECLOAK
                || ambushPhase == AmbushPhase.HOLD;
        if (!ready || spec.gun().isEmpty()) {
            return false;
        }
        EnemyGun gun = spec.gun().get();
        if (burstLeft > 0) {
            if (ambushPhase != AmbushPhase.NONE) {
                // M5 part D: an ambush unit's interval runs from its burst's start (its two bursts
                // 1.2 s apart in its hold, design/enemies/air/wraith); other units' from its end.
                volleyTicks--;
            }
            if (--burstTicks > 0) {
                return false;
            }
            burstLeft--;
            burstTicks = SimStep.ticks(gun.burstGapSeconds());
            return true;
        }
        if (--volleyTicks > 0) {
            return false;
        }
        if (ambushPhase == AmbushPhase.DECLOAK) {
            // M5 part D: the gun is silent until the decloak flash ends.
            volleyTicks = 1;
            return false;
        }
        if (ambushPhase != AmbushPhase.NONE && ++ambushVolleys > AMBUSH_VOLLEYS) {
            volleyTicks = Integer.MAX_VALUE;
            return false;
        }
        volleyTicks = SimStep.ticks(gun.intervalSeconds());
        burstLeft = gun.burst() - 1;
        burstTicks = SimStep.ticks(gun.burstGapSeconds());
        return true;
    }

    /**
     * The index of the shot {@link #trigger()} just let go in its burst, from 0 (M5 part D: a burst
     * fired straight up walks across its fan).
     */
    int burstShot() {
        return spec.gun().isPresent() ? spec.gun().get().burst() - 1 - burstLeft : 0;
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
        // A segment chain's original head is a weak point (its spec carries the chain); a set
        // piece's parts multiply theirs in SetPiece.damagePart.
        hp -= spec.chain().isPresent() ? amount * spec.chain().get().headMultiplier() : amount;
        ticksSinceHit = 0;
        return hp <= 0;
    }

    /** The hit points left. */
    public double hp() {
        return hp;
    }

    /** Simulation steps since it last took damage; {@link Integer#MAX_VALUE} before (the Targeting computer's HP bar). */
    public int ticksSinceHit() {
        return ticksSinceHit;
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
        if (glideHoldTicks > 0) {
            hash.add(glideTicks);
        }
        if (escort) {
            hash.add(breakTicks).add(centreX).add(centreY).add(carrier == null ? -1 : carrier.serial);
        }
        if (chain != null) {
            hash.add(chain.serial()).add(link);
        }
        if (spec.sweep().isPresent()) {
            hash.add(sweepWait).add(sweepTicks).add(sweepCentre).add(sweepHit ? 1 : 0);
        }
        if (walkPath != null) {
            hash.add(facing)
                    .add(walked)
                    .add(waypoint)
                    .add(scrolled)
                    .add(spitTicks)
                    .add(entered ? 1 : 0);
        }
        if (volleyGroup >= 0) {
            hash.add(volleyGroup).add(volleyUnit);
        }
        // M5 part C's spawners and pounces add their state; earlier units hash as before.
        if (spec.spawner().isPresent()) {
            hash.add(spawnWait).add(irisTicks).add(releasing ? 1 : 0);
        }
        if (spec.pounce().isPresent()) {
            hash.add(leapTicks)
                    .add(pounceWait)
                    .add(leapFromX)
                    .add(leapFromY)
                    .add(leapToX)
                    .add(leapToY)
                    .add(leapOverX)
                    .add(leapOverY)
                    .add(pounceTouched);
        }
        // M5 part D's ambush units and flock members add theirs; earlier units hash as before.
        if (ambush != null) {
            hash.add(ambushPhase.ordinal())
                    .add(ambushTicks)
                    .add(reentryTicks)
                    .add(speed)
                    .add(ambushVolleys);
        }
        if (flock != null) {
            hash.add(flock.serial()).add(member);
        }
        if (allyContacts != 0) {
            // M5 part D: only while it overlaps an air escort's unit, so the other levels hash as before.
            hash.add(allyContacts);
        }
        // M5 part E's drifting, submerging and pulsing units add theirs; earlier units hash as before.
        if (driftX != 0 || driftY != 0) {
            hash.add(driftX).add(driftY);
        }
        if (spec.submerge().isPresent()) {
            hash.add(submerged ? 1 : 0)
                    .add(swapSeed)
                    .add(swapWait)
                    .add(swapTicks)
                    .add(swapDown ? 1 : 0);
        }
        if (spec.ring().isPresent()) {
            hash.add(ringWait);
        }
    }

    /** M5 part D: the air escort's units it overlapped in the last step, one bit per unit. */
    int allyContacts() {
        return allyContacts;
    }

    /** M5 part D: the air escort's units it overlaps now, one bit per unit. */
    void allyContacts(int units) {
        allyContacts = units;
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    /**
     * Unique among the units of an attempt (a shot's lock; the presentation's once-per-unit cues,
     * such as the Vrell screech as a large unit enters the screen).
     */
    public int serial() {
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

    /**
     * One step of a laser sweep while it hovers (design/enemies/air/mantis): the wait for the next
     * telegraph runs; a telegraph starts only if its sweep ends before the hover does, and fixes the
     * sweep's centre on the ship's bearing from its eye, clamped between straight inward and straight
     * down.
     */
    private void sweep(double shipX, double shipY) {
        EnemySpec.Sweep sweep = spec.sweep().orElseThrow();
        int telegraph = SimStep.ticks(sweep.telegraphSeconds());
        int total = telegraph + SimStep.ticks(sweep.sweepSeconds());
        if (sweepTicks >= 0 && ++sweepTicks >= total) {
            sweepTicks = -1;
        }
        if (sweepWait < 0 || --sweepWait > 0) {
            return;
        }
        if (holdTicks < total) {
            sweepWait = -1;
            return;
        }
        boolean left = x < PlayField.WIDTH / 2.0;
        double inward = left ? -StrictMath.PI / 2 : StrictMath.PI / 2;
        double bearing = heading(shipX - x - beamOffsetX(), shipY - y - beamOffsetY());
        sweepCentre = left ? Math.clamp(bearing, inward, 0) : Math.clamp(bearing, 0, inward);
        sweepTicks = 0;
        sweepHit = false;
        sweepWait = SimStep.ticks(sweep.intervalSeconds());
    }

    /** Whether its sweep is in its telegraph now. */
    public boolean telegraphing() {
        return sweepTicks >= 0
                && sweepTicks < SimStep.ticks(spec.sweep().orElseThrow().telegraphSeconds());
    }

    /** Whether its beam is sweeping now. */
    public boolean sweeping() {
        return sweepTicks >= 0 && !telegraphing();
    }

    /** Whether the sweep just entered its beam this step (for its sound). */
    boolean sweepStarted() {
        return sweepTicks == SimStep.ticks(spec.sweep().orElseThrow().telegraphSeconds());
    }

    /** Whether the sweep's telegraph just started this step (for its sound). */
    boolean telegraphStarted() {
        return sweepTicks == 0;
    }

    /** The sweep's centre, radians clockwise from straight down. */
    public double sweepCentre() {
        return sweepCentre;
    }

    /** The sweep's first edge: from straight inward's side, radians clockwise from straight down. */
    public double sweepFrom() {
        double half = spec.sweep().orElseThrow().arcRadians() / 2;
        return x < PlayField.WIDTH / 2.0 ? sweepCentre - half : sweepCentre + half;
    }

    /** The sweep's last edge, toward straight down. */
    public double sweepTo() {
        double half = spec.sweep().orElseThrow().arcRadians() / 2;
        return x < PlayField.WIDTH / 2.0 ? sweepCentre + half : sweepCentre - half;
    }

    /**
     * The beam's heading now, radians clockwise from straight down, moving evenly from
     * {@link #sweepFrom()} to {@link #sweepTo()} over the sweep; {@code alpha} interpolates between steps.
     */
    public double beam(double alpha) {
        EnemySpec.Sweep sweep = spec.sweep().orElseThrow();
        int telegraph = SimStep.ticks(sweep.telegraphSeconds());
        double into = Math.max(0, sweepTicks - telegraph + alpha) / SimStep.ticks(sweep.sweepSeconds());
        return sweepFrom() + (sweepTo() - sweepFrom()) * Math.min(1, into);
    }

    /**
     * The beam's origin (its eye) from the unit's centre along x: toward the field it hovers over,
     * so mirrored on the right edge; with {@link #beamOffsetY()} where the beam, its hit test and
     * its telegraph start.
     */
    public double beamOffsetX() {
        double in = spec.sweep().orElseThrow().originIn();
        return x < PlayField.WIDTH / 2.0 ? in : -in;
    }

    /** The beam's origin from the unit's centre along y (up the field: its eye hangs below it). */
    public double beamOffsetY() {
        return -spec.sweep().orElseThrow().originDown();
    }

    /** Whether the current sweep hit the ship already. */
    boolean sweepHit() {
        return sweepHit;
    }

    void markSweepHit() {
        sweepHit = true;
    }

    /** Its hit box: its stat block's, or a chain member's own. */
    public Hitbox hitbox() {
        return box;
    }

    /**
     * Links it into {@code into} as member {@code index} at (atX, atY), facing {@code heading}: a
     * chain's head, segment or tail with its own hit box. A head's gun waits its first-shot delay.
     */
    void link(
            Chain into,
            int index,
            EnemySpec memberSpec,
            int memberKind,
            Hitbox memberBox,
            int unitSerial,
            double atX,
            double atY,
            double heading) {
        clearLevel04();
        chain = into;
        link = index;
        spec = memberSpec;
        box = memberBox;
        kind = memberKind;
        serial = unitSerial;
        group = -1;
        aim = 0;
        inArc = false;
        diveTicks = 0;
        diveFired = false;
        diveShot = false;
        segment = 0;
        distance = 0;
        speed = 0;
        phase = Phase.CHAIN;
        holdTicks = 0;
        orbit = Optional.empty();
        exit = Spawn.Exit.DOWN;
        hp = spec.hp();
        ticksSinceHit = Integer.MAX_VALUE;
        volleyTicks = spec.gun().isPresent() ? SimStep.ticks(spec.gun().get().firstShotDelay()) + 1 : 0;
        burstLeft = 0;
        burstTicks = 0;
        leadsTarget = false;
        carried = Optional.empty();
        spiralTicks = 0;
        ricochetsLeft = 0;
        vx = 0;
        vy = 0;
        x = prevX = atX;
        y = prevY = atY;
        facing = heading;
    }

    /** M5 part C: marks it as a unit of a wave tagged {@code waveTag} ("" for none). */
    void tag(String waveTag) {
        tag = waveTag;
    }

    /** M5 part C: the tag of its wave (Level 09's {@code bridge}); "" for none or a unit that is not a wave's. */
    public String tag() {
        return tag;
    }

    /** Marks it as a unit of a wave that entered from {@code edge}. */
    void entered(WaveSpec.Entry edge) {
        entry = edge;
    }

    /**
     * The edge its wave entered from; front for a unit that is not a wave's (a chain's member: see
     * {@link Chain#entry()}; a flock's member: see {@link Flock#entry()}); M5 part D: a rear ambush
     * unit's is the front until its re-entry.
     */
    public WaveSpec.Entry entry() {
        // M5 part D: a rear ambush unit's wave is a rear wave from its re-entry on.
        if (ambushPhase == AmbushPhase.SWOOP || ambushPhase == AmbushPhase.GAP) {
            return WaveSpec.Entry.FRONT;
        }
        return entry;
    }

    /** It carries {@code pickup}, dropped when it is destroyed (a chain's head carries its wave's). */
    void carry(Optional<PickupType> pickup) {
        carried = pickup;
    }

    /** Its chain moved it to (atX, atY), facing {@code heading}. */
    void chainTo(double atX, double atY, double heading) {
        prevX = x;
        prevY = y;
        x = atX;
        y = atY;
        facing = heading;
    }

    /** Moves it to another chain as member {@code index} (the rear part of a cut). */
    void relink(Chain into, int index) {
        chain = into;
        link = index;
    }

    /** The chain it is a member of; null for other units. */
    public Chain chain() {
        return chain;
    }

    /** Its place in its chain, 0 the head. */
    public int link() {
        return link;
    }

    /**
     * M5 part D: joins {@code into} as member {@code index} at (atX, atY), facing {@code heading}: a
     * swarm's member, placed by its flock from now on (design/enemies/air/mote-swarm).
     */
    void join(Flock into, int index, Spawn plan, int unitSerial, double atX, double atY, double heading) {
        clearLevel04();
        flock = into;
        member = index;
        spec = plan.enemy();
        box = spec.hitbox();
        kind = plan.kind();
        serial = unitSerial;
        group = -1;
        aim = 0;
        inArc = false;
        diveTicks = 0;
        diveFired = false;
        diveShot = false;
        path = plan.path();
        segment = 0;
        distance = 0;
        speed = plan.speed();
        phase = Phase.FLOCK;
        holdTicks = 0;
        orbit = Optional.empty();
        exit = Spawn.Exit.DOWN;
        hp = spec.hp();
        ticksSinceHit = Integer.MAX_VALUE;
        volleyTicks = 0;
        burstLeft = 0;
        burstTicks = 0;
        leadsTarget = false;
        carried = plan.carried();
        spiralTicks = 0;
        ricochetsLeft = 0;
        vx = 0;
        vy = 0;
        x = prevX = atX;
        y = prevY = atY;
        facing = heading;
    }

    /** M5 part D: the flock it is a member of; null for other units. */
    public Flock flock() {
        return flock;
    }

    /** M5 part D: its place in its flock, from 0 in entry order. */
    public int member() {
        return member;
    }
}
