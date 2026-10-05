package vanguard.sim;

/**
 * A segment chain in flight (design/enemies/air/coilwyrm): a head, its segments and its tail,
 * each an {@link Enemy} with its own hit box and HP. The chain's head point flies the wave's path
 * (and its loop-back) and leaves a history of the points it passed; member {@code i} sits on that
 * history {@code offsets[i]} px behind the head point, facing along it. The head point flies on
 * when the head itself is destroyed, so the body keeps flowing while it pops.
 *
 * <p>A cut (a destroyed segment) of a chain that has not regrown yet splits it: the rear part
 * becomes a chain of its own that holds still while it grows a new head, then lunges at the ship
 * (the cut used the regrowth, for both parts). A cut of any other chain, and the head's death, let
 * the members behind it die one by one, from the cut backwards; while they wait for their burst
 * they are {@linkplain #doomed doomed}. Pooled, with fixed arrays, so the
 * chain never allocates while it flies.
 */
public final class Chain implements Hashed {
    /** History points kept: enough for the longest chain at the slowest sampling. */
    static final int HISTORY = 512;
    /** The most members a chain has: head, segments and tail. */
    static final int MAX_MEMBERS = 24;
    /** A prefilled history has a point every this many px. */
    private static final double PREFILL_STEP = 4;
    /** The prefilled history reaches this far behind the last member. */
    private static final double PREFILL_MARGIN = 64;
    /**
     * A headless body's speed is multiplied by this every step: at 180 px/s it drifts about 60 px
     * and comes to rest in about 1.5 s, so the bursts of its chained death happen in place, on the
     * play field (design/enemies/air/coilwyrm, Chained death).
     */
    private static final double DRIFT = 0.95;
    /** Below this speed, px/s, a drifting body is at rest. */
    private static final double STILL = 1;

    /** How the head point moves. */
    enum Mode {
        /** Along its path. */
        PATH,
        /** Straight on past its path's end (waiting for its loop-back, or leaving). */
        STRAIGHT,
        /** Holding still while its new head grows. */
        REGROW,
        /** Straight at where the ship was when its new head had grown. */
        LUNGE
    }

    private final double[] hx = new double[HISTORY];
    private final double[] hy = new double[HISTORY];
    /** The arc length flown at each history point. */
    private final double[] hs = new double[HISTORY];

    private int newest;
    private int count;

    private final Enemy[] members = new Enemy[MAX_MEMBERS];
    private final double[] offsets = new double[MAX_MEMBERS];
    private int size;

    private EnemySpec.ChainSpec spec;
    /** The kind of the head a cut part grows. */
    private int regrownKind;

    private int serial;
    private Mode mode;
    private FlightPath path;
    private int segment;
    private double distance;
    private double speed;
    private double vx;
    private double vy;
    private double headX;
    private double headY;
    /** The loop-back still to come; null when there is none (or it was flown). */
    private Spawn.Loop loop;
    /** Steps flown straight on past the path's end. */
    private int straightTicks;
    /** Whether it can still regrow (an uncut chain from a wave). */
    private boolean canRegrow;
    /** Whether it is a wave's chain rather than a regrown rear part. */
    private boolean original;
    /** Whether a cut split it (or killed a part of it). */
    private boolean cut;

    private int regrowTicks;
    /** Members from this index on die one by one; {@link #size} when none does. */
    private int dyingFrom;
    /** Steps until the next of them pops. */
    private int popTicks;
    /** Whether any member has been on the play field (a regrown part or a loop-back counts as on). */
    private boolean entered;
    /** Whether its head is dead: the body drifts to a halt while it bursts. */
    private boolean drifting;

    /** A wave's chain: the head point starts at its path's start, the members strung out behind it. */
    void start(EnemySpec.ChainSpec chainSpec, int chainSerial, Spawn plan, int regrownHeadKind) {
        clear(chainSpec, chainSerial);
        regrownKind = regrownHeadKind;
        original = true;
        canRegrow = true;
        mode = Mode.PATH;
        path = plan.path();
        speed = plan.speed();
        loop = plan.loop().orElse(null);
        headX = path.x(0, 0);
        headY = path.y(0, 0);
        size = spec.members();
        for (int i = 0; i < size; i++) {
            offsets[i] = spec.offsets().get(i);
        }
        prefill(path.dx(0), path.dy(0));
    }

    private void clear(EnemySpec.ChainSpec chainSpec, int chainSerial) {
        spec = chainSpec;
        serial = chainSerial;
        count = 0;
        newest = -1;
        size = 0;
        segment = 0;
        distance = 0;
        vx = 0;
        vy = 0;
        straightTicks = 0;
        loop = null;
        cut = false;
        regrowTicks = 0;
        dyingFrom = MAX_MEMBERS;
        popTicks = 0;
        entered = false;
        drifting = false;
        for (int i = 0; i < MAX_MEMBERS; i++) {
            members[i] = null;
        }
    }

    /** A history straight back from the head point against the direction (dx, dy), long enough for every member. */
    private void prefill(double dx, double dy) {
        double length = Math.sqrt(dx * dx + dy * dy);
        double ux = length > 0 ? dx / length : 0;
        double uy = length > 0 ? dy / length : -1;
        double reach = offsets[Math.max(0, size - 1)] + PREFILL_MARGIN;
        count = 0;
        newest = -1;
        for (double back = reach; back > 0; back -= PREFILL_STEP) {
            push(headX - ux * back, headY - uy * back);
        }
        push(headX, headY);
    }

    private void push(double x, double y) {
        double s = 0;
        if (count > 0) {
            double dx = x - hx[newest];
            double dy = y - hy[newest];
            s = hs[newest] + Math.sqrt(dx * dx + dy * dy);
        }
        newest = (newest + 1) % HISTORY;
        hx[newest] = x;
        hy[newest] = y;
        hs[newest] = s;
        count = Math.min(count + 1, HISTORY);
    }

    /** Member {@code i} is {@code enemy}, linked by the caller. */
    void member(int i, Enemy enemy) {
        members[i] = enemy;
    }

    /**
     * One step: the head point flies (along its path, straight on, held while a head grows, or
     * lunging) and the members follow on its history. Returns false once it has flown its course
     * and no member is on the play field any more.
     */
    boolean advance() {
        if (drifting) {
            speed *= DRIFT;
            vx *= DRIFT;
            vy *= DRIFT;
            if (speed < STILL && mode != Mode.REGROW) {
                // At rest: the members stay where they are until they burst.
                return place() || alive();
            }
        }
        boolean moved = true;
        switch (mode) {
            case PATH -> {
                distance += speed * SimStep.SECONDS;
                if (distance >= path.length()) {
                    segment = path.segmentAt(path.length(), segment);
                    double dx = path.dx(segment);
                    double dy = path.dy(segment);
                    double length = Math.sqrt(dx * dx + dy * dy);
                    vx = length > 0 ? dx / length * speed : 0;
                    vy = length > 0 ? dy / length * speed : -speed;
                    double over = distance - path.length();
                    headX = path.x(segment, path.length()) + vx / speed * over;
                    headY = path.y(segment, path.length()) + vy / speed * over;
                    mode = Mode.STRAIGHT;
                    straightTicks = 0;
                } else {
                    segment = path.segmentAt(distance, segment);
                    headX = path.x(segment, distance);
                    headY = path.y(segment, distance);
                }
            }
            case STRAIGHT, LUNGE -> {
                headX += vx * SimStep.SECONDS;
                headY += vy * SimStep.SECONDS;
                straightTicks++;
                if (loop != null && straightTicks >= SimStep.ticks(loop.gapSeconds())) {
                    reenter();
                    moved = false;
                }
            }
            case REGROW -> moved = false;
        }
        if (moved) {
            push(headX, headY);
        }
        boolean onField = place();
        entered |= onField;
        if (onField || mode == Mode.PATH || mode == Mode.REGROW || loop != null) {
            return true;
        }
        return !entered && straightTicks < SimStep.ticks(1);
    }

    /** The loop-back: the head point re-enters on its second path, the members strung out behind it. */
    private void reenter() {
        path = loop.path();
        loop = null;
        mode = Mode.PATH;
        segment = 0;
        distance = 0;
        headX = path.x(0, 0);
        headY = path.y(0, 0);
        prefill(path.dx(0), path.dy(0));
    }

    /** Places the members on the history where they enter, without an in-between position. */
    void settle() {
        place();
        for (int i = 0; i < size; i++) {
            if (members[i] != null) {
                members[i].chainTo(members[i].x(), members[i].y(), members[i].facing());
            }
        }
    }

    /** Places the members on the history; returns whether any of them is on the play field. */
    private boolean place() {
        boolean onField = false;
        int at = newest;
        int walked = 0;
        double headS = hs[newest];
        for (int i = 0; i < size; i++) {
            double target = headS - offsets[i];
            while (walked < count - 1 && hs[at] > target) {
                at = (at - 1 + HISTORY) % HISTORY;
                walked++;
            }
            // Between the point at (older) and the next newer one.
            int newer = walked == 0 ? at : (at + 1) % HISTORY;
            double x;
            double y;
            if (newer == at || hs[newer] == hs[at]) {
                x = hx[at];
                y = hy[at];
            } else {
                double t = Math.clamp((target - hs[at]) / (hs[newer] - hs[at]), 0, 1);
                x = hx[at] + (hx[newer] - hx[at]) * t;
                y = hy[at] + (hy[newer] - hy[at]) * t;
            }
            Enemy member = members[i];
            if (member == null) {
                continue;
            }
            int older = walked == 0 ? (at - 1 + HISTORY) % HISTORY : at;
            int front = walked == 0 ? at : newer;
            double dx = hx[front] - hx[older];
            double dy = hy[front] - hy[older];
            double heading = dx == 0 && dy == 0 ? member.facing() : StrictMath.atan2(-dx, -dy);
            member.chainTo(x, y, heading);
            onField |= PlayField.overlaps(x, y, member.hitbox());
        }
        return onField;
    }

    /**
     * Member {@code i} was destroyed: it leaves the chain. The head's death lets the whole chain die
     * from the front; a segment's cut either splits off the rear part (returned, to grow its new
     * head: the caller fills {@code rear} from the pool, null when none is free) or lets the
     * members behind the cut die.
     *
     * @return whether the rear part was split off into {@code rear}
     */
    boolean destroyed(int i, Chain rear, int rearSerial) {
        members[i] = null;
        if (i == 0) {
            canRegrow = false;
            dyingFrom = Math.min(dyingFrom, 1);
            drift();
            popTicks = popTicks > 0 ? popTicks : SimStep.ticks(spec.popSeconds());
            return false;
        }
        boolean behind = false;
        for (int k = i + 1; k < size; k++) {
            behind |= members[k] != null;
        }
        if (!behind) {
            size = i;
            dyingFrom = Math.min(dyingFrom, size);
            return false;
        }
        if (canRegrow && dyingFrom >= size && rear != null) {
            split(i, rear, rearSerial);
            return true;
        }
        cut = true;
        canRegrow = false;
        if (i + 1 < dyingFrom) {
            dyingFrom = i + 1;
            popTicks = SimStep.ticks(spec.popSeconds());
        }
        return false;
    }

    /** The head is dead: no loop-back, and the body drifts to a halt while it bursts. */
    private void drift() {
        drifting = true;
        loop = null;
    }

    /** The members behind the cut at {@code i} become {@code rear}, which grows a new head at the cut. */
    private void split(int i, Chain rear, int rearSerial) {
        rear.clear(spec, rearSerial);
        rear.regrownKind = regrownKind;
        rear.mode = Mode.REGROW;
        rear.regrowTicks = SimStep.ticks(spec.regrowSeconds());
        rear.speed = spec.regrowSpeed();
        rear.entered = true;
        // The rear part's history: this one's, up to the cut point.
        double cutS = hs[newest] - offsets[i];
        int at = newest;
        int walked = 0;
        while (walked < count - 1 && hs[at] > cutS) {
            at = (at - 1 + HISTORY) % HISTORY;
            walked++;
        }
        int oldest = (newest - count + 1 + HISTORY) % HISTORY;
        for (int k = 0, p = oldest; k < count - walked; k++, p = (p + 1) % HISTORY) {
            rear.push(hx[p], hy[p]);
        }
        rear.headX = cutX(i);
        rear.headY = cutY(i);
        rear.push(rear.headX, rear.headY);
        rear.size = size - i;
        rear.offsets[0] = 0;
        for (int k = i + 1; k < size; k++) {
            int index = k - i;
            rear.members[index] = members[k];
            rear.offsets[index] = offsets[k] - offsets[i];
            if (members[k] != null) {
                members[k].relink(rear, index);
            }
            members[k] = null;
        }
        size = i;
        cut = true;
        canRegrow = false;
        dyingFrom = Math.min(dyingFrom, size);
    }

    /** Where member {@code i} sits on the history: the cut point when it is destroyed. */
    private double cutX(int i) {
        return pointAt(hs[newest] - offsets[i], true);
    }

    private double cutY(int i) {
        return pointAt(hs[newest] - offsets[i], false);
    }

    /** The history's x (or y) at arc length {@code s}. */
    private double pointAt(double s, boolean wantX) {
        int at = newest;
        int walked = 0;
        while (walked < count - 1 && hs[at] > s) {
            at = (at - 1 + HISTORY) % HISTORY;
            walked++;
        }
        int newer = walked == 0 ? at : (at + 1) % HISTORY;
        double[] values = wantX ? hx : hy;
        if (newer == at || hs[newer] == hs[at]) {
            return values[at];
        }
        double t = Math.clamp((s - hs[at]) / (hs[newer] - hs[at]), 0, 1);
        return values[at] + (values[newer] - values[at]) * t;
    }

    /**
     * Counts down the regrowth; returns true in the step its new head has grown: the caller links
     * the head as member 0 ({@link #grown}) or, when the rear part died meanwhile, lets it end.
     */
    boolean regrowDue() {
        return mode == Mode.REGROW && --regrowTicks <= 0;
    }

    /** The new head has grown (or not, {@code head} null): it lunges at (shipX, shipY). */
    void grown(Enemy head, double shipX, double shipY) {
        members[0] = head;
        double dx = shipX - headX;
        double dy = shipY - headY;
        double length = Math.sqrt(dx * dx + dy * dy);
        vx = length > 0 ? dx / length * speed : 0;
        vy = length > 0 ? dy / length * speed : -speed;
        mode = Mode.LUNGE;
        straightTicks = 0;
        if (head == null) {
            canRegrow = false;
            dyingFrom = Math.min(dyingFrom, 1);
            drift();
            popTicks = SimStep.ticks(spec.popSeconds());
        }
    }

    /**
     * Counts down the next pop of the dying members; returns the member to pop now (the first alive
     * one from the cut backwards), or null.
     */
    Enemy popDue() {
        if (dyingFrom >= size) {
            return null;
        }
        if (--popTicks > 0) {
            return null;
        }
        popTicks = SimStep.ticks(spec.popSeconds());
        for (int k = dyingFrom; k < size; k++) {
            if (members[k] != null) {
                Enemy member = members[k];
                members[k] = null;
                dyingFrom = k + 1;
                return member;
            }
        }
        dyingFrom = size;
        return null;
    }

    /**
     * Whether {@code enemy} is a chain member waiting for its burst in a chained death: it flies on
     * with the chain and is drawn intact, but nothing hits it, it rams nothing and it pays nothing
     * until it bursts (design/enemies/air/coilwyrm, Chained death).
     */
    public static boolean doomed(Enemy enemy) {
        Chain chain = enemy.chain();
        return chain != null && enemy.link() >= chain.dyingFrom && enemy.link() < chain.size;
    }

    /** Whether any member is alive. */
    public boolean alive() {
        for (int k = 0; k < size; k++) {
            if (members[k] != null) {
                return true;
            }
        }
        return false;
    }

    /** The kind of the head a cut part of it grows. */
    public int regrownKind() {
        return regrownKind;
    }

    /** Whether its regrowth is under way: the rear part holds while its head grows. */
    public boolean regrowing() {
        return mode == Mode.REGROW;
    }

    /** The regrowth's progress, 0 to 1 (the new head's scale while it grows). */
    public double regrowth() {
        int total = SimStep.ticks(spec.regrowSeconds());
        return mode == Mode.REGROW ? 1 - (double) Math.max(0, regrowTicks) / total : 1;
    }

    /** The head point: where a new head grows. */
    public double headX() {
        return headX;
    }

    public double headY() {
        return headY;
    }

    /** The head point's heading, radians clockwise from straight down (a growing head faces it). */
    public double headFacing() {
        int older = (newest - 1 + HISTORY) % HISTORY;
        double dx = hx[newest] - hx[older];
        double dy = hy[newest] - hy[older];
        return count < 2 || (dx == 0 && dy == 0) ? 0 : StrictMath.atan2(-dx, -dy);
    }

    /** Whether destroying member {@code i} now pays the first bonus: the tail of an uncut wave's chain. */
    boolean firstBonusDue(int i) {
        return original && !cut && canRegrow && i == size - 1 && i == spec.members() - 1;
    }

    /** Whether it is a wave's chain (its head is the unit that counts as the kill). */
    boolean original() {
        return original;
    }

    /** The member slots, head first; a slot of a destroyed member is empty. */
    public int size() {
        return size;
    }

    /** Member {@code i}, or null when it was destroyed. */
    public Enemy member(int i) {
        return members[i];
    }

    /** Unique among the chains of an attempt. */
    int serial() {
        return serial;
    }

    public EnemySpec.ChainSpec spec() {
        return spec;
    }

    @Override
    public void addTo(StateHash hash) {
        hash.add(serial)
                .add(mode.ordinal())
                .add(segment)
                .add(distance)
                .add(speed)
                .add(vx)
                .add(vy)
                .add(headX)
                .add(headY)
                .add(loop == null ? 0 : 1)
                .add(straightTicks)
                .add(canRegrow ? 1 : 0)
                .add(original ? 1 : 0)
                .add(cut ? 1 : 0)
                .add(regrowTicks)
                .add(dyingFrom)
                .add(popTicks)
                .add(entered ? 1 : 0)
                .add(drifting ? 1 : 0)
                .add(size)
                .add(count)
                .add(newest);
        for (int k = 0; k < size; k++) {
            hash.add(members[k] == null ? -1 : members[k].serial()).add(offsets[k]);
        }
        for (int k = 0, p = newest; k < count; k++, p = (p - 1 + HISTORY) % HISTORY) {
            hash.add(hx[p]).add(hy[p]).add(hs[p]);
        }
    }
}
