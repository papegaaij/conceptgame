package vanguard.sim;

import java.util.Arrays;
import java.util.List;

/**
 * A huge set-piece unit flying its passes (design/enemies/space/leviathan): its centre follows
 * each pass's path with the pass's fixed heading; its parts are hit boxes at fixed offsets around
 * it that take the hits, fire while it is on the player's layer and stay wrecked once destroyed.
 * Its position is a function of the level time, so a restart only resets the parts. One instance
 * per {@link LevelScript.SetPieceSpec}, made when the sortie is.
 *
 * <p>A boss ({@link BossSpec}, design/enemies/bosses) is a set piece without passes: it arrives on
 * the level clock, descends (taking no damage), settles and sways; its chains hang parts on necks
 * that bend toward the player, and its phases fire their attacks and streams through the
 * {@link BossActions} until its vital part is destroyed. Everything it needs is allocated here, so
 * stepping it does not allocate.
 */
public final class SetPiece implements Hashed {
    /** What a boss does to the rest of the level; the sortie implements it once. */
    interface BossActions {
        /** One aimed shot from (x, y) with {@code gun}'s speed and damage. */
        void aimed(double x, double y, EnemyGun gun);

        /** A ring of {@code count} bullets from (x, y). */
        void ring(double x, double y, int count, double speed, double damage);

        /** One bullet from (x, y) at {@code angle} radians (0 = right, y up). */
        void bullet(double x, double y, double angle, double speed, double damage);

        /** One unit of {@code stream} from the left or the right edge. */
        void release(BossSpec.Stream stream, boolean left);

        /** It entered phase {@code phase} (after the first). */
        void phase(int phase);

        /**
         * Phase {@code phase} ended on its timer (a timeout), just before the next one is entered
         * with {@link #phase(int)}.
         */
        default void timeout(int phase) {}

        /**
         * One unit of {@code spawn} leaves an open window part at (x, y), flying straight out at
         * {@code angle} radians (0 = right, y up) at the spawn's speed, gliding and holding as the
         * spawn says.
         */
        default void launch(BossSpec.Spawn spawn, double x, double y, double angle) {}
    }

    /** How a boss moves now (design/enemies/bosses, the poses of part G). */
    public enum Motion {
        /** Not arrived yet, or destroyed. */
        NONE,
        /** Its entrance: in from above the top edge to its hover height. */
        PASS,
        /** At its station, swaying. */
        HOLD,
        /** A phase's move: gliding to its new station and sinking (or rising) to its new layer. */
        DESCEND,
        /** A phase's move: turning in place into its new pose. */
        TURN
    }

    private final LevelScript.SetPieceSpec spec;
    private final double extent;
    private final double[] partHp;
    private final int[] partTicksSinceHit;
    private final int[] volleyTicks;
    private final int[] burstLeft;
    private final int[] burstTicks;

    private int pass;
    private boolean present;
    private boolean destroyed;
    private boolean escaped;
    private boolean onPlane;
    private Layer layer;
    private double x;
    private double y;
    private double prevX;
    private double prevY;
    private double altitude;
    private double prevAltitude;
    private double cos;
    private double sin;
    private int hitCooldown;

    // A boss's state; the arrays are empty for a set piece flying passes.
    private final BossSpec boss;
    private final double maxHp;
    private final int[] partChain;
    private final int[] exposedFrom;
    private final int[] chainStart;
    private final int[] segmentStart;
    private final double[] angles;
    private final double[] segmentX;
    private final double[] segmentY;
    private final double[] tipX;
    private final double[] tipY;
    private final int[] attackTicks;
    private final int[] attackBurstLeft;
    private final int[] attackBurstTicks;
    private final int[] attackPart;
    private final int[] attackNext;
    /** Per attack, a spiral's arm angle and its steps to the next bullet. */
    private final double[] spiralAngles;

    private final int[] spiralTicks;
    /** Per part, whether it is a fire-only part (never damaged, out of the bar). */
    private final boolean[] armoured;
    /** Per pose and part, its offset from the centre on the play plane. */
    private final double[][] poseDx;

    private final double[][] poseDy;
    private final Hitbox[] poseBody;
    /** Per phase and part, whether the phase's windows hold the part (it takes damage only while open). */
    private final boolean[][] windowed;
    /** Per part, the steps it stays open in its window; 0 closed. */
    private final int[] openTicks;

    private final double entryY;
    /** Steps since it arrived; -1 before. */
    private int bossTicks;
    /** Steps since it settled; -1 before. */
    private int swayTicks;
    /** The step of {@link #swayTicks} its sway (re)started from, at its station. */
    private int swayStart;
    /** Whether its phases run (from its arrival or its settle). */
    private boolean engaged;
    /** Steps into the phase's opening move; -1 when none runs. */
    private int moveTicks;

    private double moveFromX;
    private double moveFromY;
    private double moveFromAltitude;
    private Layer moveFromLayer;
    /** Its station: where it sways. */
    private double anchorX;

    private double anchorY;
    /** Its pose (an index into its part layouts) and the one a move turns it from. */
    private int pose;

    private int fromPose;
    /** Steps since the phase engaged (after its move): its timeout's clock. */
    private int phaseTicks;
    /** Bit f set: phase f ended on its timer. */
    private long timeouts;

    private int windowTicks;
    private int windowNext;
    private int windowOpenings;

    private int phase;
    private int turn;
    private int turnTicks;
    private int streamTicks;
    private int streamLeft;
    private int streamUnitTicks;
    private int streamUnits;
    /** The steps from its arrival (the bar appearing) to its kill; -1 while it lives. */
    private int killTicks;

    SetPiece(LevelScript.SetPieceSpec spec) {
        this.spec = spec;
        extent = Math.max(spec.size().width(), spec.size().height()) / 2;
        int parts = spec.parts().size();
        partHp = new double[parts];
        partTicksSinceHit = new int[parts];
        volleyTicks = new int[parts];
        burstLeft = new int[parts];
        burstTicks = new int[parts];
        boss = spec.boss().orElse(null);
        armoured = new boolean[parts];
        if (boss != null) {
            for (int p : boss.armoured()) {
                armoured[p] = true;
            }
        }
        double hp = 0;
        for (int p = 0; p < parts; p++) {
            hp += armoured[p] ? 0 : spec.parts().get(p).hp();
        }
        maxHp = hp;
        partChain = new int[parts];
        exposedFrom = new int[parts];
        java.util.Arrays.fill(partChain, -1);
        int poses = boss == null || boss.poses().isEmpty() ? 1 : boss.poses().size();
        poseDx = new double[poses][parts];
        poseDy = new double[poses][parts];
        poseBody = new Hitbox[poses];
        for (int k = 0; k < poses; k++) {
            BossSpec.Pose layout =
                    boss == null || boss.poses().isEmpty() ? null : boss.poses().get(k);
            if (layout != null && layout.offsets().size() != parts) {
                throw new IllegalArgumentException(spec.slug() + ": pose " + layout.name() + " places every part");
            }
            poseBody[k] = layout == null ? spec.body() : layout.body();
            for (int p = 0; p < parts; p++) {
                poseDx[k][p] = layout == null
                        ? spec.parts().get(p).dx()
                        : layout.offsets().get(p).dx();
                poseDy[k][p] = layout == null
                        ? spec.parts().get(p).dy()
                        : layout.offsets().get(p).dy();
            }
        }
        int chains = boss == null ? 0 : boss.chains().size();
        chainStart = new int[chains];
        segmentStart = new int[chains];
        int angleCount = 0;
        int segmentCount = 0;
        // A boss arriving on high air is drawn (and its parts placed) at the high-air scale.
        double scale = boss != null && boss.layer() == Layer.HIGH_AIR ? BossSpec.HIGH_AIR_SCALE : 1;
        double reach = spec.size().height() / 2 * scale;
        for (int c = 0; c < chains; c++) {
            BossSpec.Chain chain = boss.chains().get(c);
            partChain[chain.part()] = c;
            chainStart[c] = angleCount;
            segmentStart[c] = segmentCount;
            angleCount += chain.segments() + 1;
            segmentCount += chain.segments();
        }
        for (int p = 0; p < parts; p++) {
            LevelScript.PartSpec part = spec.parts().get(p);
            reach = Math.max(reach, (Math.abs(poseDy[0][p]) + part.box().height() / 2) * scale);
        }
        entryY = PlayField.HEIGHT + reach;
        angles = new double[angleCount];
        segmentX = new double[segmentCount];
        segmentY = new double[segmentCount];
        tipX = new double[chains];
        tipY = new double[chains];
        int attacks = boss == null ? 0 : boss.attacks().size();
        attackTicks = new int[attacks];
        attackBurstLeft = new int[attacks];
        attackBurstTicks = new int[attacks];
        attackPart = new int[attacks];
        attackNext = new int[attacks];
        spiralAngles = new double[attacks];
        spiralTicks = new int[attacks];
        int phases = boss == null ? 0 : boss.phases().size();
        if (phases > Long.SIZE - 1) {
            throw new IllegalArgumentException(spec.slug() + ": at most " + (Long.SIZE - 1) + " phases");
        }
        windowed = new boolean[phases][parts];
        openTicks = new int[parts];
        for (int f = 0; f < phases; f++) {
            BossSpec.Phase current = boss.phases().get(f);
            for (int p : current.exposes()) {
                exposedFrom[p] = Math.max(exposedFrom[p], f);
            }
            for (int p = 0; p < parts; p++) {
                windowed[f][p] =
                        current.windows().isPresent() && current.windows().get().holds(p);
            }
        }
        reset();
    }

    /** Back to the level start: before its first pass, every part whole. */
    void reset() {
        pass = 0;
        present = false;
        destroyed = false;
        escaped = false;
        onPlane = false;
        layer = Layer.HIGH_AIR;
        x = y = prevX = prevY = 0;
        altitude = prevAltitude = 1;
        cos = 1;
        sin = 0;
        hitCooldown = 0;
        for (int p = 0; p < partHp.length; p++) {
            partHp[p] = spec.parts().get(p).hp();
            partTicksSinceHit[p] = Integer.MAX_VALUE;
            volleyTicks[p] = 0;
            burstLeft[p] = 0;
            burstTicks[p] = 0;
        }
        if (boss != null) {
            layer = boss.layer();
            bossTicks = -1;
            swayTicks = -1;
            phase = 0;
            swayStart = 0;
            engaged = false;
            moveTicks = -1;
            moveFromX = moveFromY = moveFromAltitude = 0;
            moveFromLayer = layer;
            anchorX = boss.x();
            anchorY = boss.hoverY();
            pose = fromPose = 0;
            phaseTicks = 0;
            timeouts = 0;
            windowTicks = 0;
            windowNext = 0;
            windowOpenings = 0;
            java.util.Arrays.fill(openTicks, 0);
            java.util.Arrays.fill(spiralAngles, 0);
            java.util.Arrays.fill(spiralTicks, 0);
            turn = -1;
            turnTicks = 0;
            streamTicks = 0;
            streamLeft = 0;
            streamUnitTicks = 0;
            streamUnits = 0;
            killTicks = -1;
            java.util.Arrays.fill(angles, 0);
            java.util.Arrays.fill(attackTicks, 0);
            java.util.Arrays.fill(attackBurstLeft, 0);
            java.util.Arrays.fill(attackBurstTicks, 0);
            java.util.Arrays.fill(attackPart, -1);
            java.util.Arrays.fill(attackNext, 0);
            x = prevX = boss.x();
            y = prevY = entryY;
            altitude = prevAltitude = layer == Layer.HIGH_AIR ? 1 : 0;
            placeChains();
        }
    }

    /**
     * Moves it to where its pass has it at {@code levelTick}; returns whether its last pass ended
     * with it alive this step (it escaped). A boss arrives at its time and then flies on its own
     * clock (the level clock may halt in its arena); it never escapes.
     */
    boolean update(int levelTick) {
        if (boss != null) {
            prevX = x;
            prevY = y;
            for (int p = 0; p < partTicksSinceHit.length; p++) {
                if (partTicksSinceHit[p] < Integer.MAX_VALUE) {
                    partTicksSinceHit[p]++;
                }
            }
            if (hitCooldown > 0) {
                hitCooldown--;
            }
            moveBoss(levelTick);
            return false;
        }
        prevX = x;
        prevY = y;
        prevAltitude = altitude;
        if (hitCooldown > 0) {
            hitCooldown--;
        }
        for (int p = 0; p < partTicksSinceHit.length; p++) {
            if (partTicksSinceHit[p] < Integer.MAX_VALUE) {
                partTicksSinceHit[p]++;
            }
        }
        if (destroyed || escaped) {
            return false;
        }
        double t = levelTick * SimStep.SECONDS;
        List<LevelScript.Pass> passes = spec.passes();
        while (pass < passes.size()) {
            LevelScript.Pass current = passes.get(pass);
            if (t < current.start()) {
                present = false;
                return false;
            }
            if (place(current, t)) {
                if (!present) {
                    // Entering a pass: it appears at its first point, without a jump from the last one.
                    present = true;
                    prevX = x;
                    prevY = y;
                    prevAltitude = altitude;
                    cos = Trig.cos(current.headingRadians());
                    sin = Trig.sin(current.headingRadians());
                }
                boolean plane = layer.collidesWithPlayer();
                if (plane && !onPlane) {
                    arm();
                }
                onPlane = plane;
                return false;
            }
            present = false;
            onPlane = false;
            pass++;
        }
        escaped = true;
        return true;
    }

    /** Places it on {@code pass} at {@code t}; returns false once the pass is over. */
    private boolean place(LevelScript.Pass current, double t) {
        List<LevelScript.Waypoint> path = current.path();
        if (!current.descends()) {
            if (t > path.getLast().t()) {
                return false;
            }
            along(path, t);
            layer = current.layer();
            altitude = layer == Layer.HIGH_AIR ? 1 : 0;
            return true;
        }
        if (t < current.leaveAt()) {
            along(path, t);
            if (t < current.descendAt() + current.descentSeconds()) {
                layer = Layer.HIGH_AIR;
                altitude = t < current.descendAt() ? 1 : 1 - (t - current.descendAt()) / current.descentSeconds();
            } else {
                layer = current.layer();
                altitude = 0;
            }
            return true;
        }
        along(path, current.leaveAt());
        y += (t - current.leaveAt()) * current.leaveSpeed();
        layer = Layer.HIGH_AIR;
        altitude = Math.min(1, (t - current.leaveAt()) / current.descentSeconds());
        return y - extent < PlayField.HEIGHT;
    }

    /** Its centre on the path at {@code t}: between the waypoints around it, at the last one after it. */
    private void along(List<LevelScript.Waypoint> path, double t) {
        LevelScript.Waypoint last = path.getLast();
        if (t >= last.t()) {
            x = last.x();
            y = last.y();
            return;
        }
        for (int i = 1; i < path.size(); i++) {
            LevelScript.Waypoint to = path.get(i);
            if (t < to.t()) {
                LevelScript.Waypoint from = path.get(i - 1);
                double p = Math.max(0, (t - from.t()) / (to.t() - from.t()));
                x = from.x() + (to.x() - from.x()) * p;
                y = from.y() + (to.y() - from.y()) * p;
                return;
            }
        }
    }

    /** It reached the player's layer: every living part's gun starts on its first-shot time. */
    private void arm() {
        for (int p = 0; p < partHp.length; p++) {
            LevelScript.PartSpec part = spec.parts().get(p);
            volleyTicks[p] = SimStep.ticks(part.firstShotSeconds()) + 1;
            burstLeft[p] = 0;
            burstTicks[p] = 0;
        }
    }

    /** Whether part {@code p} fires a shot this step: it lives, has a gun and the unit is on the player's layer. */
    boolean trigger(int p) {
        LevelScript.PartSpec part = spec.parts().get(p);
        if (!present || !onPlane || partHp[p] <= 0 || part.gun().isEmpty()) {
            return false;
        }
        EnemyGun gun = part.gun().get();
        if (burstLeft[p] > 0) {
            if (--burstTicks[p] > 0) {
                return false;
            }
            burstLeft[p]--;
            burstTicks[p] = SimStep.ticks(EnemyGun.BURST_GAP_SECONDS);
            return true;
        }
        if (--volleyTicks[p] > 0) {
            return false;
        }
        volleyTicks[p] = SimStep.ticks(gun.intervalSeconds());
        burstLeft[p] = gun.burst() - 1;
        burstTicks[p] = SimStep.ticks(EnemyGun.BURST_GAP_SECONDS);
        return true;
    }

    /**
     * A boss arrives at its time, descends to its height and then sways there; a phase's move
     * glides it to a new station and layer and turns it into a new pose first.
     */
    private void moveBoss(int levelTick) {
        if (destroyed) {
            return;
        }
        prevAltitude = altitude;
        if (bossTicks < 0) {
            if (levelTick < SimStep.ticks(boss.arriveSeconds())) {
                return;
            }
            present = true;
            layer = boss.layer();
            onPlane = layer.collidesWithPlayer();
            altitude = prevAltitude = layer == Layer.HIGH_AIR ? 1 : 0;
            x = prevX = boss.x();
            y = prevY = entryY;
            if (boss.engagesOnArrival()) {
                engaged = true;
                startPhase();
            }
        }
        bossTicks++;
        if (moveTicks >= 0) {
            stepMove();
        } else if (swayTicks < 0) {
            y = Math.max(anchorY, entryY - bossTicks * boss.descentSpeed() * SimStep.SECONDS);
            if (y <= anchorY) {
                swayTicks = 0;
                if (!engaged) {
                    engaged = true;
                    startPhase();
                }
            }
        } else {
            swayTicks++;
            x = anchorX
                    + boss.sineAmplitude()
                            * Trig.sin(
                                    2 * StrictMath.PI * (swayTicks - swayStart) * SimStep.SECONDS / boss.sinePeriod());
        }
    }

    /**
     * One step of the phase's move: the glide to its station and layer, then the turn in place
     * (the pose switching half-way); at its end the phase engages.
     */
    private void stepMove() {
        BossSpec.Move move = boss.phases().get(phase).move().orElseThrow();
        moveTicks++;
        int descend = SimStep.ticks(move.descendSeconds());
        int turnSteps = SimStep.ticks(move.turnSeconds());
        double share = descend == 0 ? 1 : Math.min(1, (double) moveTicks / descend);
        double toAltitude = move.layer() == Layer.HIGH_AIR ? 1 : 0;
        x = moveFromX + (move.x() - moveFromX) * share;
        y = moveFromY + (move.y() - moveFromY) * share;
        altitude = moveFromAltitude + (toAltitude - moveFromAltitude) * share;
        if (share >= 1) {
            layer = move.layer();
        } else if (moveFromLayer == Layer.HIGH_AIR || move.layer() == Layer.HIGH_AIR) {
            layer = Layer.HIGH_AIR;
        } else {
            layer = move.layer();
        }
        onPlane = layer.collidesWithPlayer();
        if (moveTicks >= descend && 2 * (moveTicks - descend) >= turnSteps) {
            pose = move.pose();
        }
        if (moveTicks >= descend + turnSteps) {
            pose = fromPose = move.pose();
            anchorX = move.x();
            anchorY = move.y();
            moveTicks = -1;
            if (swayTicks < 0) {
                // A move cut its entrance short: it holds at the new station without settling again.
                swayTicks = 1;
            }
            swayStart = swayTicks;
            armPhase();
        }
    }

    /**
     * A boss's step after it moved: its necks bend toward the ship, its windows close in time, its
     * phase ends on its parts or its timer, and once engaged (and not moving) its attacks, windows
     * and streams run; they reach the level through {@code actions} only while {@code firing}.
     */
    void act(double shipX, double shipY, BossActions actions, boolean firing) {
        if (boss == null || !present || destroyed) {
            return;
        }
        bendChains(shipX, shipY);
        if (!engaged) {
            return;
        }
        for (int p = 0; p < openTicks.length; p++) {
            if (openTicks[p] > 0) {
                openTicks[p]--;
            }
        }
        if (moveTicks >= 0) {
            return;
        }
        phaseTicks++;
        while (phase < boss.phases().size() - 1 && phaseOver(boss.phases().get(phase))) {
            if (!partsDown(boss.phases().get(phase))) {
                timeouts |= 1L << phase;
                actions.timeout(phase);
            }
            phase++;
            startPhase();
            actions.phase(phase);
            if (moveTicks >= 0) {
                return;
            }
        }
        BossSpec.Phase current = boss.phases().get(phase);
        if (current.alternate()) {
            alternate(current, actions, firing);
        } else {
            for (int i = 0; i < current.attacks().size(); i++) {
                runAttack(current.attacks().get(i), current, actions, firing);
            }
        }
        if (current.windows().isPresent()) {
            windows(current.windows().get(), shipX, shipY, actions, firing);
        }
        if (current.stream().isPresent()) {
            stream(current.stream().get(), actions, firing);
        }
    }

    /** Whether the phase is over: its parts are down, or its timer ran out. */
    private boolean phaseOver(BossSpec.Phase current) {
        return partsDown(current) || (current.timed() && phaseTicks >= SimStep.ticks(current.seconds()));
    }

    /** Whether at most the phase's {@code left} of its parts are alive (never for a phase without parts). */
    private boolean partsDown(BossSpec.Phase current) {
        if (current.untilParts().isEmpty()) {
            return false;
        }
        int alive = 0;
        for (int i = 0; i < current.untilParts().size(); i++) {
            alive += partHp[current.untilParts().get(i)] > 0 ? 1 : 0;
        }
        return alive <= current.left();
    }

    /** A phase starts: its windows close; it begins its move, or engages at once. */
    private void startPhase() {
        java.util.Arrays.fill(openTicks, 0);
        BossSpec.Phase current = boss.phases().get(phase);
        if (current.move().isPresent()) {
            moveTicks = 0;
            moveFromX = x;
            moveFromY = y;
            moveFromAltitude = altitude;
            moveFromLayer = layer;
            fromPose = pose;
            return;
        }
        armPhase();
    }

    /**
     * A phase engages: its timer starts; after its delay its attacks fire (each a volley one
     * interval later), its alternation takes its first turn, its windows open and its stream sends.
     */
    private void armPhase() {
        BossSpec.Phase current = boss.phases().get(phase);
        int delay = SimStep.ticks(current.delaySeconds());
        for (int i = 0; i < current.attacks().size(); i++) {
            int a = current.attacks().get(i);
            attackTicks[a] = delay + SimStep.ticks(boss.attacks().get(a).gun().intervalSeconds());
            attackBurstLeft[a] = 0;
            spiralTicks[a] = delay;
        }
        turn = -1;
        turnTicks = Math.max(1, delay);
        streamLeft = 0;
        streamTicks = Math.max(1, delay);
        int offset = current.windows().isPresent()
                ? SimStep.ticks(current.windows().get().offsetSeconds())
                : 0;
        windowTicks = Math.max(1, delay + offset);
        windowNext = 0;
        windowOpenings = 0;
        phaseTicks = 0;
    }

    /** The phase's attacks in turn: each runs its turn (a ring its interval, a spiral its duration), then hands over. */
    private void alternate(BossSpec.Phase current, BossActions actions, boolean firing) {
        if (--turnTicks <= 0) {
            turn = (turn + 1) % current.attacks().size();
            BossSpec.Attack attack = boss.attacks().get(current.attacks().get(turn));
            turnTicks = Math.max(1, SimStep.ticks(attack.turnSeconds()));
            spiralTicks[current.attacks().get(turn)] = 0;
            if (attack.pattern() != BossSpec.Pattern.SPIRAL) {
                volley(current.attacks().get(turn), current, actions, firing);
            }
        }
        if (turn < 0) {
            return;
        }
        int a = current.attacks().get(turn);
        BossSpec.Attack attack = boss.attacks().get(a);
        if (attack.pattern() == BossSpec.Pattern.SPIRAL) {
            spiral(a, attack, current, actions, firing);
        }
    }

    /** A spiral's step: its arms turn, and each fires a bullet every interval (each spiral on its own state). */
    private void spiral(int a, BossSpec.Attack attack, BossSpec.Phase current, BossActions actions, boolean firing) {
        spiralAngles[a] = Math.IEEEremainder(
                spiralAngles[a] + attack.turnRadiansPerSecond() * SimStep.SECONDS, 2 * StrictMath.PI);
        if (--spiralTicks[a] > 0) {
            return;
        }
        spiralTicks[a] = Math.max(1, SimStep.ticks(attack.gun().intervalSeconds()));
        if (!firing) {
            return;
        }
        List<Integer> parts = firingParts(attack, current);
        for (int i = 0; i < parts.size(); i++) {
            int p = parts.get(i);
            if (partHp[p] <= 0) {
                continue;
            }
            for (int k = 0; k < attack.arms(); k++) {
                actions.bullet(
                        partX(p),
                        partY(p),
                        spiralAngles[a] + 2 * StrictMath.PI * k / attack.arms(),
                        attack.gun().bulletSpeed(),
                        attack.gun().damage());
            }
        }
    }

    /**
     * The phase's windows: when one is due, the next group in order with a living part on the field
     * opens (every such group when {@code all}), and each opened group releases the spawn of this
     * opening from its open parts.
     */
    private void windows(BossSpec.Windows windows, double shipX, double shipY, BossActions actions, boolean firing) {
        if (--windowTicks > 0) {
            return;
        }
        windowTicks = Math.max(1, SimStep.ticks(windows.everySeconds()));
        int open = Math.max(1, SimStep.ticks(windows.openSeconds()));
        BossSpec.Spawn spawn = windows.spawns().isEmpty()
                ? null
                : windows.spawns().get(windowOpenings % windows.spawns().size());
        int groups = windows.groups().size();
        boolean opened = false;
        for (int i = 0; i < groups; i++) {
            int g = (windowNext + i) % groups;
            List<Integer> group = windows.groups().get(g);
            int alive = 0;
            int ready = 0;
            for (int k = 0; k < group.size(); k++) {
                int p = group.get(k);
                if (partHp[p] > 0) {
                    alive++;
                    ready += partOnField(p) ? 1 : 0;
                }
            }
            if (ready == 0) {
                continue;
            }
            for (int k = 0; k < group.size(); k++) {
                int p = group.get(k);
                if (partHp[p] > 0 && partOnField(p)) {
                    openTicks[p] = open;
                }
            }
            if (spawn != null && firing) {
                release(spawn, group, alive, ready, shipX, shipY, actions);
            }
            opened = true;
            if (!windows.all()) {
                windowNext = (g + 1) % groups;
                break;
            }
        }
        if (opened) {
            windowOpenings++;
        }
    }

    /**
     * An opened group releases its share of {@code spawn}: the count times its living share,
     * rounded up, dealt out among its open parts, each part's units spread over the arc centred on
     * the direction from it to the ship.
     */
    private void release(
            BossSpec.Spawn spawn,
            List<Integer> group,
            int alive,
            int ready,
            double shipX,
            double shipY,
            BossActions actions) {
        int units = (spawn.count() * alive + group.size() - 1) / group.size();
        int j = 0;
        for (int k = 0; k < group.size(); k++) {
            int p = group.get(k);
            if (partHp[p] <= 0 || !partOnField(p)) {
                continue;
            }
            int mine = units / ready + (j < units % ready ? 1 : 0);
            j++;
            double px = partX(p);
            double py = partY(p);
            double toShip = StrictMath.atan2(shipY - py, shipX - px);
            for (int u = 0; u < mine; u++) {
                double angle =
                        mine == 1 ? toShip : toShip - spawn.arcRadians() / 2 + u * spawn.arcRadians() / (mine - 1);
                actions.launch(spawn, px, py, angle);
            }
        }
    }

    /** Whether part {@code p}'s hit box overlaps the play field. */
    private boolean partOnField(int p) {
        return PlayField.overlaps(partX(p), partY(p), spec.parts().get(p).box());
    }

    /** An attack fired on its own interval: a volley when it is due, then the rest of its burst. */
    private void runAttack(int a, BossSpec.Phase current, BossActions actions, boolean firing) {
        BossSpec.Attack attack = boss.attacks().get(a);
        if (attack.pattern() == BossSpec.Pattern.SPIRAL) {
            spiral(a, attack, current, actions, firing);
            return;
        }
        if (attackBurstLeft[a] > 0) {
            if (--attackBurstTicks[a] <= 0) {
                attackBurstLeft[a]--;
                attackBurstTicks[a] = Math.max(1, SimStep.ticks(attack.burstGapSeconds()));
                shoot(a, attack, current, actions, firing);
            }
        }
        if (--attackTicks[a] > 0) {
            return;
        }
        attackTicks[a] = Math.max(1, SimStep.ticks(attack.gun().intervalSeconds()));
        volley(a, current, actions, firing);
    }

    /** The first shot of a volley: a rotating attack picks the next living part; a burst follows. */
    private void volley(int a, BossSpec.Phase current, BossActions actions, boolean firing) {
        BossSpec.Attack attack = boss.attacks().get(a);
        List<Integer> parts = firingParts(attack, current);
        attackPart[a] = -1;
        if (attack.rotate()) {
            for (int i = 0; i < parts.size(); i++) {
                int candidate = (attackNext[a] + i) % parts.size();
                if (partHp[parts.get(candidate)] > 0) {
                    attackPart[a] = parts.get(candidate);
                    attackNext[a] = (candidate + 1) % parts.size();
                    break;
                }
            }
            if (attackPart[a] < 0) {
                return;
            }
        }
        attackBurstLeft[a] =
                attack.pattern() != BossSpec.Pattern.RING ? attack.gun().burst() - 1 : 0;
        attackBurstTicks[a] = Math.max(1, SimStep.ticks(attack.burstGapSeconds()));
        shoot(a, attack, current, actions, firing);
    }

    /** One shot (or ring) of attack {@code a} from its part, or from every living firing part. */
    private void shoot(int a, BossSpec.Attack attack, BossSpec.Phase current, BossActions actions, boolean firing) {
        if (!firing) {
            return;
        }
        if (attackPart[a] >= 0) {
            if (partHp[attackPart[a]] > 0) {
                fireFrom(attackPart[a], attack, actions);
            }
            return;
        }
        List<Integer> parts = firingParts(attack, current);
        for (int i = 0; i < parts.size(); i++) {
            int p = parts.get(i);
            if (partHp[p] > 0) {
                fireFrom(p, attack, actions);
            }
        }
    }

    private void fireFrom(int p, BossSpec.Attack attack, BossActions actions) {
        if (attack.pattern() == BossSpec.Pattern.RING) {
            actions.ring(
                    partX(p),
                    partY(p),
                    attack.count(),
                    attack.gun().bulletSpeed(),
                    attack.gun().damage());
        } else {
            actions.aimed(partX(p), partY(p), attack.gun());
        }
    }

    /** The parts that fire {@code attack}: its own, or else the parts the phase ends on. */
    private static List<Integer> firingParts(BossSpec.Attack attack, BossSpec.Phase current) {
        return attack.parts().isEmpty() ? current.untilParts() : attack.parts();
    }

    /** The phase's stream: the first at its start, then every {@code every} s, one unit per interval. */
    private void stream(BossSpec.Stream stream, BossActions actions, boolean firing) {
        if (--streamTicks <= 0) {
            streamTicks = Math.max(1, SimStep.ticks(stream.everySeconds()));
            streamLeft = stream.count();
            streamUnitTicks = 1;
        }
        if (streamLeft > 0 && --streamUnitTicks <= 0) {
            streamLeft--;
            streamUnitTicks = Math.max(1, SimStep.ticks(stream.intervalSeconds()));
            boolean left =
                    switch (stream.edge()) {
                        case LEFT -> true;
                        case RIGHT -> false;
                        case NONE, ALTERNATING -> streamUnits % 2 == 0;
                    };
            streamUnits++;
            if (firing) {
                actions.release(stream, left);
            }
        }
    }

    /**
     * The necks turn toward the ship: each chain's anchor piece eases toward the angle to the
     * ship (at most its bend, the phase's in an enraged phase), every later piece toward the one
     * before it, {@code lag} s late; a chain whose part is destroyed goes limp toward its rest.
     */
    private void bendChains(double shipX, double shipY) {
        double phaseBend = !engaged ? Double.NaN : boss.phases().get(phase).bendRadians();
        for (int c = 0; c < chainStart.length; c++) {
            BossSpec.Chain chain = boss.chains().get(c);
            LevelScript.PartSpec end = spec.parts().get(chain.part());
            double restX = end.dx() - chain.fromDx();
            double restY = end.dy() - chain.fromDy();
            double target = 0;
            if (partHp[chain.part()] > 0) {
                double toX = shipX - (x + chain.fromDx());
                double toY = shipY - (y + chain.fromDy());
                double bend = Double.isNaN(phaseBend) ? chain.bendRadians() : phaseBend;
                target =
                        Math.clamp(StrictMath.atan2(restX * toY - restY * toX, restX * toX + restY * toY), -bend, bend);
            }
            double ease = Math.min(1, SimStep.SECONDS / chain.lagSeconds());
            int first = chainStart[c];
            angles[first] += (target - angles[first]) * ease;
            for (int k = 1; k <= chain.segments(); k++) {
                angles[first + k] += (angles[first + k - 1] - angles[first + k]) * ease;
            }
        }
        placeChains();
    }

    /** The chains' segment and tip offsets from their angles: each piece is the rest line's share, turned. */
    private void placeChains() {
        for (int c = 0; c < chainStart.length; c++) {
            BossSpec.Chain chain = boss.chains().get(c);
            LevelScript.PartSpec end = spec.parts().get(chain.part());
            int pieces = chain.segments() + 1;
            double stepX = (end.dx() - chain.fromDx()) / pieces;
            double stepY = (end.dy() - chain.fromDy()) / pieces;
            double px = chain.fromDx();
            double py = chain.fromDy();
            for (int k = 0; k < pieces; k++) {
                double angle = angles[chainStart[c] + k];
                double cosA = Trig.cos(angle);
                double sinA = Trig.sin(angle);
                px += stepX * cosA - stepY * sinA;
                py += stepX * sinA + stepY * cosA;
                if (k < chain.segments()) {
                    segmentX[segmentStart[c] + k] = px;
                    segmentY[segmentStart[c] + k] = py;
                }
            }
            tipX[c] = px;
            tipY[c] = py;
        }
    }

    /**
     * Whether part {@code p} takes no damage now: a boss's parts before it engages (its descent)
     * and during a phase's move, its fire-only parts, the parts a later phase exposes before it,
     * and a window part while its window is shut. A shot on it glances off.
     */
    public boolean partShielded(int p) {
        if (boss == null) {
            return false;
        }
        if (!engaged || moveTicks >= 0 || armoured[p] || phase < exposedFrom[p]) {
            return true;
        }
        return windowed[phase][p] && openTicks[p] == 0;
    }

    /** Whether part {@code p} is fire-only: it fires, never takes damage, is not in the bar and pays nothing. */
    public boolean partArmoured(int p) {
        return armoured[p];
    }

    /** Whether the current phase's windows hold part {@code p} (it opens and shuts). */
    public boolean partWindowed(int p) {
        return boss != null && windowed[phase][p];
    }

    /** Whether part {@code p}'s window is open now and the part lives (it takes damage, unless the boss is moving). */
    public boolean partOpen(int p) {
        return openTicks[p] > 0 && partHp[p] > 0;
    }

    /** Seconds part {@code p}'s window stays open; 0 shut. */
    public double partOpenSeconds(int p) {
        return openTicks[p] * SimStep.SECONDS;
    }

    /** The current phase's window open time, s (0 without windows): with {@link #partOpenSeconds} the opening's progress. */
    public double windowOpenSeconds() {
        if (boss == null) {
            return 0;
        }
        var windows = boss.phases().get(phase).windows();
        return windows.isPresent() ? windows.get().openSeconds() : 0;
    }

    /** Whether a shot of {@code size} at (sx, sy) touches one of its armoured neck segments. */
    boolean neckTouches(Hitbox size, double sx, double sy) {
        for (int c = 0; c < chainStart.length; c++) {
            Hitbox box = boss.chains().get(c).box();
            for (int k = 0; k < boss.chains().get(c).segments(); k++) {
                int i = segmentStart[c] + k;
                if (size.overlaps(sx, sy, box, x + segmentX[i], y + segmentY[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Part {@code p} takes damage, times its multiplier; returns whether that destroyed it. */
    boolean damagePart(int p, double amount) {
        partTicksSinceHit[p] = 0;
        boolean alive = partHp[p] > 0;
        partHp[p] -= amount * spec.parts().get(p).multiplier();
        return alive && partHp[p] <= 0;
    }

    /** Destroys part {@code p} outright (the vital part took the rest with it); returns whether it still lived (and pays). */
    boolean wreckPart(int p) {
        // A fire-only part goes down with the boss but pays nothing.
        boolean alive = partHp[p] > 0 && !armoured[p];
        partHp[p] = Math.min(partHp[p], 0);
        return alive;
    }

    /** The unit is destroyed: it leaves the play field at once (its death plays where it was). */
    void destroy() {
        if (boss != null && killTicks < 0) {
            killTicks = Math.max(0, bossTicks);
        }
        destroyed = true;
        present = false;
        onPlane = false;
    }

    /** The body touching the ship: whether it deals its contact damage now (at most once per its interval). */
    boolean strike() {
        if (hitCooldown > 0) {
            return false;
        }
        hitCooldown = SimStep.ticks(LevelScript.SetPieceSpec.HIT_INTERVAL_SECONDS);
        return true;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(pass)
                .add(present ? 1 : 0)
                .add(destroyed ? 1 : 0)
                .add(escaped ? 1 : 0)
                .add(onPlane ? 1 : 0)
                .add(layer.ordinal())
                .add(x)
                .add(y)
                .add(altitude)
                .add(hitCooldown);
        for (int p = 0; p < partHp.length; p++) {
            hash.add(partHp[p])
                    .add(partTicksSinceHit[p])
                    .add(volleyTicks[p])
                    .add(burstLeft[p])
                    .add(burstTicks[p]);
        }
        if (boss != null) {
            hash.add(bossTicks)
                    .add(swayTicks)
                    .add(phase)
                    .add(turn)
                    .add(turnTicks)
                    .add(swayStart)
                    .add(engaged ? 1 : 0)
                    .add(moveTicks)
                    .add(moveFromX)
                    .add(moveFromY)
                    .add(moveFromAltitude)
                    .add(moveFromLayer.ordinal())
                    .add(anchorX)
                    .add(anchorY)
                    .add(pose)
                    .add(fromPose)
                    .add(phaseTicks)
                    .add(timeouts)
                    .add(windowTicks)
                    .add(windowNext)
                    .add(windowOpenings)
                    .add(prevAltitude)
                    .add(streamTicks)
                    .add(streamLeft)
                    .add(streamUnitTicks)
                    .add(streamUnits)
                    .add(killTicks);
            for (double angle : angles) {
                hash.add(angle);
            }
            for (int a = 0; a < attackTicks.length; a++) {
                hash.add(attackTicks[a])
                        .add(attackBurstLeft[a])
                        .add(attackBurstTicks[a])
                        .add(attackPart[a])
                        .add(attackNext[a])
                        .add(spiralAngles[a])
                        .add(spiralTicks[a]);
            }
            for (int ticks : openTicks) {
                hash.add(ticks);
            }
        }
    }

    double x() {
        return x;
    }

    double y() {
        return y;
    }

    public LevelScript.SetPieceSpec spec() {
        return spec;
    }

    /** Its enemy's slug ({@code leviathan}): the sprites and the death effect go by it. */
    public String slug() {
        return spec.slug();
    }

    /** Whether it is on screen in a pass (not before, between or after its passes, nor once destroyed). */
    public boolean present() {
        return present;
    }

    public boolean destroyed() {
        return destroyed;
    }

    /** Whether it ended its last pass alive. */
    public boolean escaped() {
        return escaped;
    }

    /** The index of the pass it flies (or will fly next) in {@link LevelScript.SetPieceSpec#passes()}. */
    public int pass() {
        return pass;
    }

    /** The name of that pass ({@code cross}, {@code descend}); empty after the last. */
    public String passName() {
        return pass < spec.passes().size() ? spec.passes().get(pass).name() : "";
    }

    /** The layer it flies on now: {@code high-air} out of reach, {@code air} once descended. */
    public Layer layer() {
        return layer;
    }

    /** Whether it is on the player's layer: its guns fire and its body collides. */
    public boolean onPlane() {
        return onPlane;
    }

    /** Where it faces, radians clockwise from straight down the screen, as {@link Enemy#facing()}; fixed per pass. */
    public double facing() {
        return present && boss == null ? spec.passes().get(pass).headingRadians() : 0;
    }

    /** Its boss script; empty for a set piece flying passes. */
    public java.util.Optional<BossSpec> boss() {
        return spec.boss();
    }

    /** Seconds since a boss arrived (its bar appeared); negative before. */
    public double bossSeconds() {
        return bossTicks * SimStep.SECONDS;
    }

    /** Steps since a boss arrived; -1 before. */
    int bossTicks() {
        return bossTicks;
    }

    /** Whether a boss has settled after its descent (it takes damage and attacks). */
    public boolean settled() {
        return swayTicks >= 0;
    }

    /** Steps since a boss settled; -1 before. */
    int swayTicks() {
        return swayTicks;
    }

    /** A boss's phase, 0 for its first. */
    public int phase() {
        return phase;
    }

    /** The seconds from a boss's arrival to its kill; negative while it lives (or before it arrived). */
    public double killSeconds() {
        return killTicks < 0 ? -1 : killTicks * SimStep.SECONDS;
    }

    /** The boss bar: its parts' remaining HP as a share of their total, 0–1 (fire-only parts left out). */
    public double barShare() {
        double left = 0;
        for (int p = 0; p < partHp.length; p++) {
            left += armoured[p] ? 0 : Math.max(0, partHp[p]);
        }
        return left / maxHp;
    }

    /** A boss's neck segments, all chains in order. */
    public int segmentCount() {
        return segmentX.length;
    }

    /** Neck segment {@code i}'s centre offset from its centre, px to the right. */
    public double segmentOffsetX(int i) {
        return segmentX[i];
    }

    /** Neck segment {@code i}'s centre offset from its centre, px up. */
    public double segmentOffsetY(int i) {
        return segmentY[i];
    }

    /** The chain neck segment {@code i} belongs to. */
    public int segmentChain(int i) {
        int c = 0;
        while (c + 1 < segmentStart.length && segmentStart[c + 1] <= i) {
            c++;
        }
        return c;
    }

    /** Chain {@code c}'s angle at its piece {@code k} (0 at the anchor; its segments, then its tip), radians counter-clockwise. */
    public double chainAngle(int c, int k) {
        return angles[chainStart[c] + k];
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public double renderX(double alpha) {
        return prevX + (x - prevX) * alpha;
    }

    /** Position between the previous and the current step; {@code alpha} in [0, 1]. */
    public double renderY(double alpha) {
        return prevY + (y - prevY) * alpha;
    }

    /** How high above the player's layer it flies: 1 on {@code high-air}, 0 on {@code air}, in between while it descends or rises. */
    public double altitude(double alpha) {
        return prevAltitude + (altitude - prevAltitude) * alpha;
    }

    public int partCount() {
        return partHp.length;
    }

    /** Whether part {@code p} is destroyed: it no longer fires or takes hits and is drawn wrecked. */
    public boolean partWrecked(int p) {
        return partHp[p] <= 0;
    }

    /** Part {@code p}'s HP left (at most its spec's, 0 or less once wrecked). */
    public double partHp(int p) {
        return partHp[p];
    }

    /** Simulation steps since part {@code p} was last hit, for its hit flash; {@link Integer#MAX_VALUE} before. */
    public int partTicksSinceHit(int p) {
        return partTicksSinceHit[p];
    }

    /** Part {@code p}'s centre on the screen now: its offset turned with the heading. */
    public double partX(int p) {
        return x + partOffsetX(p);
    }

    /** Part {@code p}'s centre on the screen now (y up). */
    public double partY(int p) {
        return y + partOffsetY(p);
    }

    /**
     * Part {@code p}'s offset from the centre on the screen, px to the right: its data offset turned
     * with the pass's heading (fixed per pass), so {@code renderX(alpha) + partOffsetX(p)} draws it.
     * A boss's is its pose's offset, grown by the high-air scale with its altitude ({@link #scale()}):
     * already the screen offset, not to be scaled again.
     */
    public double partOffsetX(int p) {
        if (partChain[p] >= 0) {
            return tipX[partChain[p]];
        }
        if (boss != null) {
            return poseDx[pose][p] * scale();
        }
        LevelScript.PartSpec part = spec.parts().get(p);
        return part.dx() * cos + part.dy() * sin;
    }

    /** Part {@code p}'s offset from the centre on the screen, px up. */
    public double partOffsetY(int p) {
        if (partChain[p] >= 0) {
            return tipY[partChain[p]];
        }
        if (boss != null) {
            return poseDy[pose][p] * scale();
        }
        LevelScript.PartSpec part = spec.parts().get(p);
        return -part.dx() * sin + part.dy() * cos;
    }

    /** A boss's size factor now: {@link BossSpec#HIGH_AIR_SCALE} on high air, 1 on the play plane, between them as it descends. */
    public double scale() {
        return 1 + (BossSpec.HIGH_AIR_SCALE - 1) * altitude;
    }

    /** Its armoured body's hit box now: a boss's pose's, else its spec's. */
    public Hitbox body() {
        return boss == null ? spec.body() : poseBody[pose];
    }

    /** How a boss moves now: its entrance, holding, or a phase's descent or turn. */
    public Motion motion() {
        if (boss == null || !present || destroyed || bossTicks < 0) {
            return Motion.NONE;
        }
        if (moveTicks >= 0) {
            int descend =
                    SimStep.ticks(boss.phases().get(phase).move().orElseThrow().descendSeconds());
            return moveTicks < descend ? Motion.DESCEND : Motion.TURN;
        }
        return swayTicks < 0 ? Motion.PASS : Motion.HOLD;
    }

    /** A boss's pose: an index into {@link BossSpec#poses()} (0, the arrival pose, without poses); it switches half-way through a turn. */
    public int pose() {
        return pose;
    }

    /** The pose a move turns it from (its pose when it holds). */
    public int turnFromPose() {
        return fromPose;
    }

    /** The pose a move turns it into (its pose when it holds). */
    public int turnToPose() {
        return moveTicks >= 0 ? boss.phases().get(phase).move().orElseThrow().pose() : pose;
    }

    /** The name of its pose; empty without poses. */
    public String poseName() {
        return boss == null || boss.poses().isEmpty()
                ? ""
                : boss.poses().get(pose).name();
    }

    /**
     * How far through its turn it is between the previous and the current step: 0 before the turn
     * (and when not moving), 1 at its end, between them the turn's frames.
     */
    public double turnShare(double alpha) {
        if (moveTicks < 0) {
            return 0;
        }
        BossSpec.Move move = boss.phases().get(phase).move().orElseThrow();
        int descend = SimStep.ticks(move.descendSeconds());
        int turnSteps = SimStep.ticks(move.turnSeconds());
        if (turnSteps == 0) {
            return moveTicks >= descend ? 1 : 0;
        }
        return Math.clamp((moveTicks - 1 + alpha - descend) / turnSteps, 0, 1);
    }

    /** Whether its phases run (from its arrival or its settle). */
    public boolean engaged() {
        return engaged;
    }

    /** Whether phase {@code phase} ended on its timer (a timeout) rather than on its parts. */
    public boolean endedOnTimeout(int phase) {
        return (timeouts & (1L << phase)) != 0;
    }

    /** Seconds since the current phase engaged (after its move): its timeout's clock. */
    public double phaseSeconds() {
        return phaseTicks * SimStep.SECONDS;
    }

    /** Whether every part is wrecked. */
    boolean allWrecked() {
        for (int p = 0; p < partHp.length; p++) {
            if (partHp[p] > 0 && !armoured[p]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return spec.slug() + " pass " + pass + " at " + x + ", " + y + " parts " + Arrays.toString(partHp);
    }
}
