package vanguard.sim;

import java.util.List;

/**
 * M5 part E: an arena boss's lanes, slams and surfacing (design/enemies/bosses/harbour-kraken; user
 * decisions E5 = a and E7 = a, the stated defaults of 2026-10-08), stepped by its {@link SetPiece}
 * on the simulation's real steps once it engages (the level clock is halted then).
 *
 * <p><b>Slams.</b> Each slam arm ({@link BossSpec.Arm}) owns a half of the lanes. A slam cycle:
 * the lane is telegraphed for the telegraph time, the arm rises base-to-tip (from then on its layer is
 * {@code ground}, laid along the lane), strikes (the impact, through {@link SetPiece.BossActions#slam}),
 * lies awash, then sinks back under the water ({@code sub}, at rest by its root). A phase slams one
 * lane at a time ({@link BossSpec.Slamming#CHAIN}: the next telegraph when the arm has sunk), once
 * after each dive of its surfacing part ({@link BossSpec.Slamming#AFTER_DIVE}) or in volleys
 * ({@link BossSpec.Slamming#VOLLEY}: one slam per living arm at once, hard's third lane as a second
 * slam of an arm 0.5 s after its first). Single slams alternate between the lane the ship is in and
 * the lane of the convoy ship nearest the ship's x (ties to the lower lane); a choice whose arm is
 * severed goes to the other choice if its arm lives, else the living arm slams its half's lane
 * nearest the ship. A severed arm's cycle stops at once and its half is safe from then on.
 *
 * <p><b>Surfacing.</b> A phase's {@link BossSpec.Surface} part (the head) rises crown first (its
 * layer flipping to {@code ground} at the middle), its window opens (its weak spots open and the
 * phase's attacks fire, each after a tell), then it dives (back to {@code sub} at the middle); a
 * phase that {@code stay}s keeps it up. Its first rise releases the phase's {@link BossSpec.Release}.
 *
 * <p>Everything is allocated here; stepping does not allocate. Its state is hashed with its boss.
 */
public final class SlamArena implements Hashed {
    /** Where a slam arm is in its cycle. */
    public enum ArmState {
        /** Under the water at rest by its root (also a severed arm). */
        IDLE,
        /** Its lane is telegraphed; the arm still rests under the water. */
        TELEGRAPH,
        /** Rising base-to-tip out of the water along its lane ({@code ground}). */
        RISE,
        /** Struck: lying awash along its lane ({@code ground}); a second lane's whip comes in it. */
        AWASH,
        /** Sinking back under the water ({@code sub}). */
        SINK
    }

    /** Where a surfacing part is. */
    public enum SurfaceState {
        /** Under the water. */
        DOWN,
        /** Rising crown first; {@code ground} from the middle. */
        RISING,
        /** Up, its window open. */
        UP,
        /** Diving; {@code sub} from the middle. */
        DIVING
    }

    /** The arm's tip lies this far above the bottom edge when it lies along a lane, px. */
    public static final double TIP_MARGIN = 24;

    private final SetPiece piece;
    private final BossSpec boss;
    private final BossSpec.Lanes lanes;
    private final BossSpec.Slam slam;
    private final int arms;
    private final int[] armPart;
    private final int[] armLanes;
    /** Per part, its arm; -1 for a part that is no slam arm. */
    private final int[] partArm;
    /** Per chain, its arm; -1 for a neck. */
    private final int[] chainArm;
    /** Per arm, its chain; -1 without one. */
    private final int[] armChain;

    private final Layer[] startLayers;
    private final Layer[] layers;

    private final int telegraphTicks;
    private final int riseTicks;
    private final int awashTicks;
    private final int sinkTicks;
    private final int secondTicks;

    /** Per arm: steps into its cycle; -1 idle. */
    private final int[] cycle;
    /** Per arm: the lane it slams first and second (1-based; 0 for none). */
    private final int[] lane1;

    private final int[] lane2;

    /** Per spot, its part; the spots in order. */
    private final List<BossSpec.Spot> spots;

    private int slams;
    private int choices;
    /** Steps until the phase's next slam may start (chain) or its next volley (volley). */
    private int slamWait;
    /** The arm slamming after the last dive, until its cycle ends; -1 for none. */
    private int afterDiveArm;

    private int surfacePart;
    private SurfaceState surface;
    private int surfaceTicks;
    private boolean surfacedOnce;
    private int fanTicks;
    /** The surfacing part's last {@link BossSpec.Surface}, for a dive in a phase without one. */
    private BossSpec.Surface lastSurface;

    SlamArena(SetPiece piece, BossSpec boss, BossSpec.Arena arena, int parts) {
        this.piece = piece;
        this.boss = boss;
        lanes = arena.lanes();
        slam = arena.slam();
        arms = lanes.arms().size();
        armPart = new int[arms];
        armLanes = new int[arms];
        armChain = new int[arms];
        partArm = new int[parts];
        java.util.Arrays.fill(partArm, -1);
        chainArm = new int[boss.chains().size()];
        java.util.Arrays.fill(chainArm, -1);
        for (int a = 0; a < arms; a++) {
            armPart[a] = lanes.arms().get(a).part();
            armLanes[a] = lanes.arms().get(a).lanes();
            partArm[armPart[a]] = a;
            armChain[a] = -1;
            for (int c = 0; c < boss.chains().size(); c++) {
                if (boss.chains().get(c).part() == armPart[a]
                        && boss.chains().get(c).slam()) {
                    armChain[a] = c;
                    chainArm[c] = a;
                }
            }
        }
        if (arena.partLayers().size() != parts) {
            throw new IllegalArgumentException(boss.barName() + ": a layer for every part");
        }
        startLayers = arena.partLayers().toArray(Layer[]::new);
        layers = startLayers.clone();
        spots = arena.spots();
        telegraphTicks = slam == null ? 0 : Math.max(1, SimStep.ticks(slam.telegraphSeconds()));
        riseTicks = slam == null ? 0 : Math.max(1, SimStep.ticks(slam.riseSeconds()));
        awashTicks = slam == null ? 0 : Math.max(1, SimStep.ticks(slam.awashSeconds()));
        sinkTicks = slam == null ? 0 : Math.max(1, SimStep.ticks(slam.sinkSeconds()));
        secondTicks = slam == null ? 0 : SimStep.ticks(slam.secondSeconds());
        cycle = new int[arms];
        lane1 = new int[arms];
        lane2 = new int[arms];
        reset();
    }

    /** Back to the level start: every arm at rest, every part on its starting layer, nothing surfaced. */
    void reset() {
        System.arraycopy(startLayers, 0, layers, 0, layers.length);
        java.util.Arrays.fill(cycle, -1);
        java.util.Arrays.fill(lane1, 0);
        java.util.Arrays.fill(lane2, 0);
        slams = 0;
        choices = 0;
        slamWait = 0;
        afterDiveArm = -1;
        surfacePart = -1;
        surface = SurfaceState.DOWN;
        surfaceTicks = 0;
        surfacedOnce = false;
        fanTicks = 0;
        lastSurface = null;
    }

    /** A phase begins: its slam count starts over; its first slam (or volley) waits its delay. */
    void phaseStarted(BossSpec.Phase phase) {
        slams = 0;
        slamWait = SimStep.ticks(phase.delaySeconds());
        BossSpec.PhaseArena keys = phase.arena();
        if (keys.slamming() != BossSpec.Slamming.AFTER_DIVE) {
            // A slam after the last dive no longer holds the surfacing back.
            afterDiveArm = -1;
        }
        if (keys.surface().isPresent()) {
            BossSpec.Surface next = keys.surface().get();
            if (surfacePart >= 0 && surfacePart != next.part()) {
                layers[surfacePart] = startLayers[surfacePart];
                surface = SurfaceState.DOWN;
            }
            surfacePart = next.part();
            lastSurface = next;
            if (next.stay() && surface == SurfaceState.DIVING) {
                // It turns round in the dive: the rise goes on from where the dive got to.
                int dive = Math.max(1, SimStep.ticks(next.diveSeconds() > 0 ? next.diveSeconds() : next.riseSeconds()));
                int rise = Math.max(1, SimStep.ticks(next.riseSeconds()));
                surfaceTicks = (int) Math.round(rise * (1 - Math.min(1, (double) surfaceTicks / dive)));
                surface = SurfaceState.RISING;
            }
        }
    }

    /**
     * One step of the engaged boss in {@code phase}: the arms' cycles (their impacts), the surfacing
     * and the slams the phase starts; returns whether the phase's attacks fire now (its surfacing
     * part's window is open and the tell has run).
     */
    boolean step(BossSpec.Phase phase, double shipX, SetPiece.BossActions actions, boolean firing) {
        stepArms(actions, firing);
        BossSpec.PhaseArena keys = phase.arena();
        boolean fire = stepSurface(phase, keys, actions, firing, shipX);
        switch (keys.slamming()) {
            case CHAIN -> {
                if (slamWait > 0) {
                    slamWait--;
                } else if (!anyBusy()) {
                    single(shipX, actions);
                }
            }
            case VOLLEY -> {
                if (--slamWait <= 0) {
                    slamWait = Math.max(1, SimStep.ticks(keys.volleySeconds()));
                    volley(shipX, actions);
                }
            }
            case AFTER_DIVE, NONE -> {}
        }
        return fire;
    }

    /** The arms' cycles advance: telegraph, rise, the impacts, awash, sink; a severed arm stops at once. */
    private void stepArms(SetPiece.BossActions actions, boolean firing) {
        for (int a = 0; a < arms; a++) {
            if (cycle[a] < 0) {
                continue;
            }
            if (piece.partWrecked(armPart[a])) {
                stop(a);
                continue;
            }
            cycle[a]++;
            int impact = telegraphTicks + riseTicks;
            if (cycle[a] == telegraphTicks) {
                layers[armPart[a]] = Layer.GROUND;
            }
            if (cycle[a] == impact) {
                impact(a, lane1[a], actions, firing);
            }
            if (lane2[a] > 0 && cycle[a] == impact + secondTicks) {
                impact(a, lane2[a], actions, firing);
            }
            if (cycle[a] == sinkStart(a)) {
                layers[armPart[a]] = Layer.SUB;
            }
            if (cycle[a] >= sinkStart(a) + sinkTicks) {
                stop(a);
            }
        }
        if (afterDiveArm >= 0 && cycle[afterDiveArm] < 0) {
            afterDiveArm = -1;
        }
    }

    private void stop(int a) {
        cycle[a] = -1;
        lane1[a] = 0;
        lane2[a] = 0;
        layers[armPart[a]] = startLayers[armPart[a]];
    }

    /** The step the arm starts to sink at: its last impact plus the awash time. */
    private int sinkStart(int a) {
        return telegraphTicks + riseTicks + (lane2[a] > 0 ? secondTicks : 0) + awashTicks;
    }

    private void impact(int a, int lane, SetPiece.BossActions actions, boolean firing) {
        slams++;
        actions.slam(lane);
        if (!firing || slam.splashCount() == 0) {
            return;
        }
        // The splash: one bullet from each of evenly spaced points along the arm, out to either side in turn, a little
        // down.
        double x = lanes.centre(lane);
        double top = lanes.top();
        double length = top - TIP_MARGIN;
        for (int k = 0; k < slam.splashCount(); k++) {
            double y = top - length * (k + 0.5) / slam.splashCount();
            double angle = k % 2 == 0 ? StrictMath.PI + SPLASH_DOWN : -SPLASH_DOWN;
            actions.bullet(x, y, angle, slam.splashSpeed(), slam.splashDamage());
        }
    }

    /** The splash bullets fly this far below the horizontal, radians (15°). */
    private static final double SPLASH_DOWN = StrictMath.PI / 12;

    /** Whether an arm is in its cycle. */
    private boolean anyBusy() {
        for (int a = 0; a < arms; a++) {
            if (cycle[a] >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * The surfacing part's step in a phase with a surfacing (or with its window still open from the
     * phase before); returns whether the phase's attacks fire now.
     */
    private boolean stepSurface(
            BossSpec.Phase phase,
            BossSpec.PhaseArena keys,
            SetPiece.BossActions actions,
            boolean firing,
            double shipX) {
        if (surfacePart < 0) {
            return false;
        }
        BossSpec.Surface spec = keys.surface().orElse(null);
        if (piece.partWrecked(surfacePart)) {
            return false;
        }
        int rise = Math.max(1, SimStep.ticks(lastSurface.riseSeconds()));
        int dive = Math.max(
                1,
                SimStep.ticks(lastSurface.diveSeconds() > 0 ? lastSurface.diveSeconds() : lastSurface.riseSeconds()));
        switch (surface) {
            case DOWN -> {
                if (spec != null && afterDiveArm < 0) {
                    surface = SurfaceState.RISING;
                    surfaceTicks = 0;
                    actions.surface(surfacePart, true);
                    if (!surfacedOnce) {
                        surfacedOnce = true;
                        if (keys.release().isPresent() && firing) {
                            actions.release(keys.release().get());
                        }
                    }
                }
            }
            case RISING -> {
                surfaceTicks++;
                if (2 * surfaceTicks >= rise) {
                    layers[surfacePart] = Layer.GROUND;
                }
                if (surfaceTicks >= rise) {
                    surface = SurfaceState.UP;
                    surfaceTicks = 0;
                    fanTicks = Math.max(1, SimStep.ticks(lastSurface.glowSeconds()));
                }
            }
            case UP -> {
                surfaceTicks++;
                boolean stays = spec != null && spec.stay();
                if (!stays && (spec == null || surfaceTicks >= SimStep.ticks(spec.openSeconds()))) {
                    surface = SurfaceState.DIVING;
                    surfaceTicks = 0;
                    actions.surface(surfacePart, false);
                    return false;
                }
                if (!phase.attacks().isEmpty() && --fanTicks <= 0) {
                    fanTicks = Math.max(
                            1,
                            SimStep.ticks(boss.attacks()
                                    .get(phase.attacks().getFirst())
                                    .gun()
                                    .intervalSeconds()));
                    return true;
                }
            }
            case DIVING -> {
                surfaceTicks++;
                if (2 * surfaceTicks >= dive) {
                    layers[surfacePart] = Layer.SUB;
                }
                if (surfaceTicks >= dive) {
                    surface = SurfaceState.DOWN;
                    surfaceTicks = 0;
                    if (keys.slamming() == BossSpec.Slamming.AFTER_DIVE) {
                        afterDiveArm = single(shipX, actions);
                    }
                }
            }
        }
        return false;
    }

    /** A single slam by the alternate rule; returns the arm that slams, -1 for none (both severed). */
    private int single(double shipX, SetPiece.BossActions actions) {
        int player = laneOf(shipX);
        int ship = nearestShip(shipX, actions.shipLanes(), 0);
        boolean toShip = (choices & 1) == 1 && ship > 0;
        choices++;
        int first = toShip ? ship : player;
        int other = toShip ? player : ship;
        int arm = livingOwner(first);
        int lane = first;
        if (arm < 0 && other > 0 && livingOwner(other) >= 0) {
            arm = livingOwner(other);
            lane = other;
        }
        if (arm < 0) {
            arm = livingArm();
            if (arm < 0) {
                return -1;
            }
            lane = nearestOwned(arm, shipX, 0);
        }
        start(arm, lane, 0, actions);
        return arm;
    }

    /**
     * A volley: the ship's lane, the nearest convoy ship's and (hard) a third (the next-nearest
     * ship's, else the lane beside the ship's), each slammed by its half's living arm; an arm with no
     * target of its own slams its half's lane nearest the ship while the other arm lives; an arm with
     * two targets slams its second one {@link BossSpec.Slam#secondSeconds()} later when the volley has
     * three lanes. A busy arm sits the volley out.
     */
    private void volley(double shipX, SetPiece.BossActions actions) {
        int shipsMask = actions.shipLanes();
        int player = laneOf(shipX);
        int first = nearestShip(shipX, shipsMask, 0);
        int targets = 1 << player;
        int order0 = player;
        int order1 = first > 0 && first != player ? first : 0;
        int order2 = 0;
        if (order1 > 0) {
            targets |= 1 << order1;
        }
        if (slam.volleyLanes() >= 3) {
            int next = nearestShip(shipX, shipsMask & ~targets, 0);
            int third = next > 0 ? next : beside(player, shipX);
            if (third > 0 && (targets & (1 << third)) == 0) {
                order2 = third;
                targets |= 1 << third;
            }
        }
        int living = 0;
        for (int a = 0; a < arms; a++) {
            living += piece.partWrecked(armPart[a]) ? 0 : 1;
        }
        boolean twice = slam.volleyLanes() >= 3;
        for (int a = 0; a < arms; a++) {
            if (piece.partWrecked(armPart[a]) || cycle[a] >= 0) {
                continue;
            }
            int mine = armLanes[a];
            int one = 0;
            int two = 0;
            for (int t = 0; t < 3; t++) {
                int lane = t == 0 ? order0 : t == 1 ? order1 : order2;
                if (lane > 0 && (mine & (1 << lane)) != 0) {
                    if (one == 0) {
                        one = lane;
                    } else if (two == 0) {
                        two = lane;
                    }
                }
            }
            if (one == 0) {
                one = nearestOwned(a, shipX, 0);
            }
            if (twice && two == 0 && living == 1) {
                two = nearestOwned(a, shipX, one);
            }
            start(a, one, twice ? two : 0, actions);
        }
    }

    /** Arm {@code a} starts its cycle on {@code first} (and {@code second}, 0 for none): their telegraphs. */
    private void start(int a, int first, int second, SetPiece.BossActions actions) {
        cycle[a] = 0;
        lane1[a] = first;
        lane2[a] = second;
        actions.telegraph(first);
        if (second > 0) {
            actions.telegraph(second);
        }
    }

    /** The lane (1-based) the x lies in, the edges' lanes beyond them. */
    public int laneOf(double x) {
        return Math.clamp((int) Math.floor(x / lanes.width()) + 1, 1, lanes.count());
    }

    /**
     * Of the lanes in {@code mask}, other than {@code not}, the one whose centre is nearest {@code x}
     * (ties to the lower lane); 0 for none.
     */
    private int nearestShip(double x, int mask, int not) {
        int best = 0;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int lane = 1; lane <= lanes.count(); lane++) {
            if ((mask & (1 << lane)) == 0 || lane == not) {
                continue;
            }
            double distance = Math.abs(lanes.centre(lane) - x);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = lane;
            }
        }
        return best;
    }

    /** The lane beside {@code lane} on the side of x within it (the lower one on a tie or at the edge); 0 with one lane. */
    private int beside(int lane, double x) {
        if (lanes.count() < 2) {
            return 0;
        }
        boolean right = x > lanes.centre(lane);
        if (lane == 1) {
            return 2;
        }
        if (lane == lanes.count()) {
            return lane - 1;
        }
        return right ? lane + 1 : lane - 1;
    }

    /** Arm {@code a}'s lane nearest {@code x}, other than {@code not}; 0 for none. */
    private int nearestOwned(int a, double x, int not) {
        return nearestShip(x, armLanes[a], not);
    }

    /** The living arm that owns {@code lane}; -1 for none. */
    private int livingOwner(int lane) {
        for (int a = 0; a < arms; a++) {
            if ((armLanes[a] & (1 << lane)) != 0 && !piece.partWrecked(armPart[a])) {
                return a;
            }
        }
        return -1;
    }

    /** The first living arm; -1 when every arm is severed. */
    private int livingArm() {
        for (int a = 0; a < arms; a++) {
            if (!piece.partWrecked(armPart[a])) {
                return a;
            }
        }
        return -1;
    }

    /** Whether at least one slam arm lives. */
    boolean armsLeft() {
        return livingArm() >= 0;
    }

    /** The impacts in the current phase so far. */
    int slams() {
        return slams;
    }

    /** Part {@code p}'s layer now. */
    Layer layer(int p) {
        return layers[p];
    }

    /**
     * The multiplier of a hit on part {@code p} at (x, y) by a shot flying along (dirX, dirY) (a unit
     * vector; 0, 0 for a blast): an open weak spot's when the shot's line ahead of it runs through the
     * spot (a shot from below meets the part's box at its lower edge and flies on to the eyes) or the
     * blast's centre lies in it, else the part's own {@code partMultiplier}.
     */
    double multiplier(
            int p, double x, double y, double dirX, double dirY, double partX, double partY, double partMultiplier) {
        for (int i = 0; i < spots.size(); i++) {
            BossSpec.Spot spot = spots.get(i);
            if (spot.part() == p
                    && (!spot.whileOpen() || open(p))
                    && ahead(
                            x - partX - spot.dx(),
                            y - partY - spot.dy(),
                            dirX,
                            dirY,
                            spot.box().width() / 2,
                            spot.box().height() / 2)) {
                return spot.multiplier();
            }
        }
        return partMultiplier;
    }

    /** A shot's line ahead of it runs this far through a part at most, px. */
    private static final double REACH = 400;

    /**
     * Whether the point (px, py) relative to a box's centre, or the line ahead of it along (dx, dy)
     * within {@link #REACH}, lies in the box of half sizes (hw, hh).
     */
    private static boolean ahead(double px, double py, double dx, double dy, double hw, double hh) {
        double enter = 0;
        double leave = REACH;
        for (int axis = 0; axis < 2; axis++) {
            double p = axis == 0 ? px : py;
            double d = axis == 0 ? dx : dy;
            double h = axis == 0 ? hw : hh;
            if (d == 0) {
                if (Math.abs(p) > h) {
                    return false;
                }
                continue;
            }
            double t1 = (-h - p) / d;
            double t2 = (h - p) / d;
            enter = Math.max(enter, Math.min(t1, t2));
            leave = Math.min(leave, Math.max(t1, t2));
        }
        return enter <= leave;
    }

    /** Whether part {@code p}'s surfacing window is open (it is up). */
    public boolean open(int p) {
        return p == surfacePart && surface == SurfaceState.UP;
    }

    /** Arm {@code a} of the chain {@code c}; -1 for a neck. */
    int chainArm(int c) {
        return chainArm[c];
    }

    /** The lane arm {@code a} lies along now (from its rise until it has sunk); 0 while at rest. */
    public int armLane(int a) {
        if (cycle[a] < telegraphTicks) {
            return 0;
        }
        return lane2[a] > 0 && cycle[a] >= telegraphTicks + riseTicks + secondTicks ? lane2[a] : lane1[a];
    }

    // --- the presentation's view (E3c) ---------------------------------------------------------

    /** How many slam arms it has. */
    public int armCount() {
        return arms;
    }

    /** Arm {@code a}'s part index. */
    public int armPart(int a) {
        return armPart[a];
    }

    /** Arm {@code a}'s chain index; -1 without one. */
    public int armChain(int a) {
        return armChain[a];
    }

    /** The lanes arm {@code a} owns, bit {@code n} for lane {@code n}. */
    public int armLanes(int a) {
        return armLanes[a];
    }

    /** Where arm {@code a} is in its cycle. */
    public ArmState armState(int a) {
        int t = cycle[a];
        if (t < 0) {
            return ArmState.IDLE;
        }
        if (t < telegraphTicks) {
            return ArmState.TELEGRAPH;
        }
        if (t < telegraphTicks + riseTicks) {
            return ArmState.RISE;
        }
        return t < sinkStart(a) ? ArmState.AWASH : ArmState.SINK;
    }

    /** How far arm {@code a} is through its state, 0–1 (0 when idle). */
    public double armShare(int a) {
        int t = cycle[a];
        if (t < 0) {
            return 0;
        }
        if (t < telegraphTicks) {
            return (double) t / telegraphTicks;
        }
        if (t < telegraphTicks + riseTicks) {
            return (double) (t - telegraphTicks) / riseTicks;
        }
        int sink = sinkStart(a);
        int impact = telegraphTicks + riseTicks;
        return t < sink ? (double) (t - impact) / (sink - impact) : Math.min(1, (double) (t - sink) / sinkTicks);
    }

    /** The lane arm {@code a} slams first in its cycle, and second (0 for none); 0 while idle. */
    public int armFirstLane(int a) {
        return lane1[a];
    }

    public int armSecondLane(int a) {
        return lane2[a];
    }

    /** The lanes telegraphed now (a lane's telegraph lasts until its impact), bit {@code n} for lane {@code n}. */
    public int telegraphed() {
        int mask = 0;
        int impact = telegraphTicks + riseTicks;
        for (int a = 0; a < arms; a++) {
            if (cycle[a] < 0) {
                continue;
            }
            if (cycle[a] < impact) {
                mask |= 1 << lane1[a];
            }
            if (lane2[a] > 0 && cycle[a] < impact + secondTicks) {
                mask |= 1 << lane2[a];
            }
        }
        return mask;
    }

    /** How long until lane {@code lane}'s next impact, s; infinite when it is not telegraphed. */
    public double untilImpact(int lane) {
        double soonest = Double.POSITIVE_INFINITY;
        int impact = telegraphTicks + riseTicks;
        for (int a = 0; a < arms; a++) {
            if (cycle[a] < 0) {
                continue;
            }
            if (lane1[a] == lane && cycle[a] < impact) {
                soonest = Math.min(soonest, (impact - cycle[a]) * SimStep.SECONDS);
            }
            if (lane2[a] == lane && cycle[a] < impact + secondTicks) {
                soonest = Math.min(soonest, (impact + secondTicks - cycle[a]) * SimStep.SECONDS);
            }
        }
        return soonest;
    }

    public int laneCount() {
        return lanes.count();
    }

    public double laneWidth() {
        return lanes.width();
    }

    /** The lanes' top edge, px above the bottom edge: they run from there down to the bottom edge. */
    public double laneTop() {
        return lanes.top();
    }

    /** The surfacing part (the head); -1 before a phase with a surfacing. */
    public int surfacePart() {
        return surfacePart;
    }

    public SurfaceState surfaceState() {
        return surface;
    }

    /** How far the surfacing part is through its rise or dive, 0–1 (1 when up, 0 when down). */
    public double surfaceShare() {
        if (lastSurface == null) {
            return 0;
        }
        return switch (surface) {
            case DOWN -> 0;
            case UP -> 1;
            case RISING -> Math.min(1, surfaceTicks / (double) Math.max(1, SimStep.ticks(lastSurface.riseSeconds())));
            case DIVING ->
                1
                        - Math.min(
                                1,
                                surfaceTicks
                                        / (double) Math.max(
                                                1,
                                                SimStep.ticks(
                                                        lastSurface.diveSeconds() > 0
                                                                ? lastSurface.diveSeconds()
                                                                : lastSurface.riseSeconds())));
        };
    }

    /** Whether the surfacing part's tell runs (the beak's glow before an attack). */
    public boolean glowing() {
        return surface == SurfaceState.UP
                && lastSurface != null
                && fanTicks <= SimStep.ticks(lastSurface.glowSeconds());
    }

    /** Whether the surfacing part has surfaced in this attempt (the release came then). */
    public boolean surfacedOnce() {
        return surfacedOnce;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(slams)
                .add(choices)
                .add(slamWait)
                .add(afterDiveArm)
                .add(surfacePart)
                .add(surface.ordinal())
                .add(surfaceTicks)
                .add(surfacedOnce ? 1 : 0)
                .add(fanTicks);
        for (int a = 0; a < arms; a++) {
            hash.add(cycle[a]).add(lane1[a]).add(lane2[a]);
        }
        for (Layer layer : layers) {
            hash.add(layer.ordinal());
        }
    }
}
