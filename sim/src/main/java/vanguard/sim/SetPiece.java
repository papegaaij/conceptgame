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
    }

    /** After its last head, the crown opens for this long before the core's first attack. */
    static final double CROWN_OPEN_SECONDS = 1;

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
    private final double entryY;
    /** Steps since it arrived; -1 before. */
    private int bossTicks;
    /** Steps since it settled; -1 before. */
    private int swayTicks;

    private int phase;
    private int turn;
    private int turnTicks;
    private double spiralAngle;
    private int spiralTicks;
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
        maxHp = spec.parts().stream().mapToDouble(LevelScript.PartSpec::hp).sum();
        partChain = new int[parts];
        exposedFrom = new int[parts];
        java.util.Arrays.fill(partChain, -1);
        int chains = boss == null ? 0 : boss.chains().size();
        chainStart = new int[chains];
        segmentStart = new int[chains];
        int angleCount = 0;
        int segmentCount = 0;
        double reach = spec.size().height() / 2;
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
            reach = Math.max(reach, Math.abs(part.dy()) + part.box().height() / 2);
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
        if (boss != null) {
            for (int f = 0; f < boss.phases().size(); f++) {
                for (int p : boss.phases().get(f).exposes()) {
                    exposedFrom[p] = Math.max(exposedFrom[p], f);
                }
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
            turn = -1;
            turnTicks = 0;
            spiralAngle = 0;
            spiralTicks = 0;
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
            altitude = prevAltitude = 0;
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

    /** A boss arrives at its time, descends to its height and then sways there. */
    private void moveBoss(int levelTick) {
        if (destroyed) {
            return;
        }
        if (bossTicks < 0) {
            if (levelTick < SimStep.ticks(boss.arriveSeconds())) {
                return;
            }
            present = true;
            onPlane = true;
            x = prevX = boss.x();
            y = prevY = entryY;
        }
        bossTicks++;
        if (swayTicks < 0) {
            y = Math.max(boss.hoverY(), entryY - bossTicks * boss.descentSpeed() * SimStep.SECONDS);
            if (y <= boss.hoverY()) {
                swayTicks = 0;
                startPhase();
            }
        } else {
            swayTicks++;
            x = boss.x()
                    + boss.sineAmplitude()
                            * Trig.sin(2 * StrictMath.PI * swayTicks * SimStep.SECONDS / boss.sinePeriod());
        }
    }

    /**
     * A boss's step after it moved: its necks bend toward the ship, its phase ends on its parts,
     * and once settled its attacks and streams run; they reach the level through {@code actions}
     * only while {@code firing}.
     */
    void act(double shipX, double shipY, BossActions actions, boolean firing) {
        if (boss == null || !present || destroyed) {
            return;
        }
        bendChains(shipX, shipY);
        if (swayTicks < 0) {
            return;
        }
        while (phase < boss.phases().size() - 1 && phaseOver(boss.phases().get(phase))) {
            phase++;
            startPhase();
            actions.phase(phase);
        }
        BossSpec.Phase current = boss.phases().get(phase);
        if (current.alternate()) {
            alternate(current, actions, firing);
        } else {
            for (int i = 0; i < current.attacks().size(); i++) {
                runAttack(current.attacks().get(i), current, actions, firing);
            }
        }
        if (current.stream().isPresent()) {
            stream(current.stream().get(), actions, firing);
        }
    }

    /** Whether at most the phase's {@code left} of its parts are alive. */
    private boolean phaseOver(BossSpec.Phase current) {
        int alive = 0;
        for (int i = 0; i < current.untilParts().size(); i++) {
            alive += partHp[current.untilParts().get(i)] > 0 ? 1 : 0;
        }
        return alive <= current.left();
    }

    /** A phase starts: its attacks on their intervals, its alternation after the crown opens, its first stream now. */
    private void startPhase() {
        BossSpec.Phase current = boss.phases().get(phase);
        for (int i = 0; i < current.attacks().size(); i++) {
            int a = current.attacks().get(i);
            attackTicks[a] = SimStep.ticks(boss.attacks().get(a).gun().intervalSeconds());
            attackBurstLeft[a] = 0;
        }
        turn = -1;
        turnTicks = phase == 0 ? 1 : SimStep.ticks(CROWN_OPEN_SECONDS);
        streamLeft = 0;
        streamTicks = 1;
    }

    /** The phase's attacks in turn: each runs its turn (a ring its interval, a spiral its duration), then hands over. */
    private void alternate(BossSpec.Phase current, BossActions actions, boolean firing) {
        if (--turnTicks <= 0) {
            turn = (turn + 1) % current.attacks().size();
            BossSpec.Attack attack = boss.attacks().get(current.attacks().get(turn));
            turnTicks = Math.max(1, SimStep.ticks(attack.turnSeconds()));
            spiralTicks = 0;
            if (attack.pattern() != BossSpec.Pattern.SPIRAL) {
                volley(current.attacks().get(turn), current, actions, firing);
            }
        }
        if (turn < 0) {
            return;
        }
        BossSpec.Attack attack = boss.attacks().get(current.attacks().get(turn));
        if (attack.pattern() == BossSpec.Pattern.SPIRAL) {
            spiral(attack, current, actions, firing);
        }
    }

    /** A spiral's step: its arms turn, and each fires a bullet every interval. */
    private void spiral(BossSpec.Attack attack, BossSpec.Phase current, BossActions actions, boolean firing) {
        spiralAngle =
                Math.IEEEremainder(spiralAngle + attack.turnRadiansPerSecond() * SimStep.SECONDS, 2 * StrictMath.PI);
        if (--spiralTicks > 0) {
            return;
        }
        spiralTicks = Math.max(1, SimStep.ticks(attack.gun().intervalSeconds()));
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
                        spiralAngle + 2 * StrictMath.PI * k / attack.arms(),
                        attack.gun().bulletSpeed(),
                        attack.gun().damage());
            }
        }
    }

    /** An attack fired on its own interval: a volley when it is due, then the rest of its burst. */
    private void runAttack(int a, BossSpec.Phase current, BossActions actions, boolean firing) {
        BossSpec.Attack attack = boss.attacks().get(a);
        if (attack.pattern() == BossSpec.Pattern.SPIRAL) {
            spiral(attack, current, actions, firing);
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
                attack.pattern() == BossSpec.Pattern.AIMED ? attack.gun().burst() - 1 : 0;
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
        double phaseBend = swayTicks < 0 ? Double.NaN : boss.phases().get(phase).bendRadians();
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
     * Whether part {@code p} takes no damage now: a boss's parts while it descends, and the parts a
     * later phase exposes before it. A shot on it glances off.
     */
    public boolean partShielded(int p) {
        return boss != null && (swayTicks < 0 || phase < exposedFrom[p]);
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

    /** Destroys part {@code p} outright (the vital part took the rest with it); returns whether it still lived. */
    boolean wreckPart(int p) {
        boolean alive = partHp[p] > 0;
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
                    .add(spiralAngle)
                    .add(spiralTicks)
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
                        .add(attackNext[a]);
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

    /** The boss bar: its parts' remaining HP as a share of their total, 0–1. */
    public double barShare() {
        double left = 0;
        for (double hp : partHp) {
            left += Math.max(0, hp);
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
     */
    public double partOffsetX(int p) {
        if (partChain[p] >= 0) {
            return tipX[partChain[p]];
        }
        LevelScript.PartSpec part = spec.parts().get(p);
        return part.dx() * cos + part.dy() * sin;
    }

    /** Part {@code p}'s offset from the centre on the screen, px up. */
    public double partOffsetY(int p) {
        if (partChain[p] >= 0) {
            return tipY[partChain[p]];
        }
        LevelScript.PartSpec part = spec.parts().get(p);
        return -part.dx() * sin + part.dy() * cos;
    }

    /** Whether every part is wrecked. */
    boolean allWrecked() {
        for (double hp : partHp) {
            if (hp > 0) {
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
